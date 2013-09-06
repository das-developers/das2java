/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.qstream.filter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.das2.datum.CacheTag;
import org.das2.datum.Datum;
import org.das2.datum.InconvertibleUnitsException;
import org.das2.datum.Units;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SemanticOps;
import org.virbo.qstream.CacheTagSerializeDelegate;
import org.virbo.qstream.FormatStreamHandler;
import org.virbo.qstream.PacketDescriptor;
import org.virbo.qstream.PlaneDescriptor;
import org.virbo.qstream.Rank0DataSetSerializeDelegate;
import org.virbo.qstream.SerializeDelegate;
import org.virbo.qstream.StreamDescriptor;
import org.virbo.qstream.StreamException;
import org.virbo.qstream.StreamTool;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Reduce packets of the same type, providing min and max channels.
 * @author jbf
 */
public class MinMaxReduceFilter extends QDataSetsFilter {

    class Accum {
        PacketDescriptor pd; // All planes should have the same value.
        int id;       // the packetId.
        //int capacity; // total capacity of the packet.  All planes should have the same number.
        String dsid;  // the dataset id, so we can remove this later.
        int N;
        double[] Smin;
        double[] Smax;
        double B; // base offset for S.  We remove this before putting data into the accumulation.
    }
    
    ByteOrder byteOrder;

    double lengthSeconds;
    double length; // in the stream units.

    Map<String, Accum> accum;
    Map<PacketDescriptor, Boolean> skip;
    Map<PacketDescriptor, Double> nextTags;

    public MinMaxReduceFilter() {
        accum= new HashMap();
        skip= new HashMap();
        lengthSeconds= 60;
        nextTags= new HashMap();
    }

    @Override
    public void streamDescriptor(StreamDescriptor sd) throws StreamException {
        this.sd= sd;
        sink.streamDescriptor(sd);
        byteOrder= sd.getByteOrder();
    }

    @Override
    public void packetDescriptor(PacketDescriptor pd) throws StreamException {
        Element ele= pd.getDomElement();

        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();

        unload( sd.descriptorId(pd) );
        clear( sd.descriptorId(pd) );

        try {
            XPathExpression expr;
            Node xp;

            boolean skipit= false;

            // figure out units.
            Units xunits= null;
            expr=  xpath.compile("/packet/qdataset[1]/properties/property[@name='UNITS']/@value");
            xp= (Node)expr.evaluate( ele,XPathConstants.NODE);
            String sunits= xp==null ? null : xp.getNodeValue();
            if ( sunits!=null ) {
                try {
                    xunits= SemanticOps.lookupTimeUnits(sunits);
                    double secmult= Units.seconds.getConverter( xunits.getOffsetUnits() ).convert(1);
                    length= secmult * lengthSeconds;
                } catch ( InconvertibleUnitsException ex ) {
                    skipit= true;
                } catch ( ParseException ex ) {
                    skipit= true;
                }
            } else {
                skipit= true;
            }

            if ( !skipit ) {

                int n= pd.getPlanes().size();
                PlaneDescriptor min= pd.getPlanes().get(n-1);
                PlaneDescriptor max= pd.getPlanes().get(n-1);
                pd.addPlane( max );
                initAccumulators(pd);

                // reset/set the cadence.
                expr= xpath.compile("/packet/qdataset[1]/properties/property[@name='CADENCE']/@value");
                xp= (Node)expr.evaluate( ele,XPathConstants.NODE);
                if ( xp!=null ) {
                    String scadence= xp.getNodeValue();
                    double oldCadenceSeconds;
                    SerializeDelegate ser= new Rank0DataSetSerializeDelegate();
                    try {
                        QDataSet o = (QDataSet)ser.parse( "rank0dataset", scadence );
                        oldCadenceSeconds= DataSetUtil.asDatum(o).doubleValue( Units.seconds );
                        if ( lengthSeconds<oldCadenceSeconds ) {
                            lengthSeconds= oldCadenceSeconds;
                        }
                    } catch ( ParseException ex ) {
                        throw new StreamException( String.format( "unable to parse cadence \"%s\"", scadence ), ex );
                    }
                    xp.setNodeValue( ser.format( DataSetUtil.asDataSet( lengthSeconds, Units.seconds ) ) );
                } else {
                    // don't install the cadence when there wasn't one already.  Let the client guess as they would have
                    // if the cadence were not specified.
                }

                // reset/set the cache tag
                expr= xpath.compile("/packet/qdataset[1]/properties/property[@name='CACHE_TAG']/@value");
                xp= (Node)expr.evaluate( ele,XPathConstants.NODE);
                if ( xp!=null ) {
                    String scachetag= xp.getNodeValue();
                    SerializeDelegate ser= new CacheTagSerializeDelegate();
                    try {
                        CacheTag ct0= (CacheTag)( ser.parse( "cacheTag", scachetag ) );
                        CacheTag ct1= new CacheTag( ct0.getRange(), Units.seconds.createDatum(lengthSeconds) );
                        xp.setNodeValue( ser.format(ct1) );
                    } catch ( ParseException ex ) {
                        throw new StreamException( String.format( "unable to parse cacheTag \"%s\"", scachetag ), ex );
                    }
                } else {
                    // We can't install a cachetag, because we don't know the bounds of the data coming.
                    // Clients must make sure a cache tag is provided in the stream.
                    // I think this is where the code in Autoplot/Das2ServerDataSource that plugs in CacheTags must have come from--me serving my
                    // temperature sensor data, where I just start kicking out daily files.  The das2server should make sure that
                    // cacheTags are provided.

                    //TODO: untested CACHE_TAG code is not in production.
                }
                
            }

            this.skip.put(pd, skipit);
            this.nextTags.put( pd, 0. );

        } catch (XPathExpressionException ex) {
            logger.log(Level.SEVERE, null, ex);
        }

        sink.packetDescriptor(pd);

    }

    /**
     * initialize the accumulators to an empty state.
     * @param pd
     */
    private void initAccumulators( PacketDescriptor pd ) {
        List<PlaneDescriptor> planes = pd.getPlanes();
        for (int i = 0; i < planes.size(); i++) {
            PlaneDescriptor planed = planes.get(i);
            Accum ac1= new Accum();
            ac1.pd= pd;
            ac1.id= sd.descriptorId(pd);
            ac1.dsid= planed.getName();
            ac1.Smin= new double[DataSetUtil.product(planed.getQube())];
            ac1.Smax= new double[DataSetUtil.product(planed.getQube())];
            ac1.N= 0;
            ac1.B= -1e38;
            accum.put(planed.getName(), ac1 );
        }
    }

    final char CHAR_NEWLINE= '\n';

    /**
     * remove this id from the packet accumulators, because this ID is about to be recycled.
     * unload(id) should be called just prior to clearing it.
     * @param id the integer id 1-99.
     */
    private void clear( int id ) {
        List<String> remove= new ArrayList();
        for ( Entry<String,Accum> entry: accum.entrySet() ) {
            Accum a= entry.getValue();
            if ( a.id==id ) {
                remove.add(a.dsid);
            }
        }
        for ( String a: remove ) {
            accum.remove(a);
        }
    }
    /**
     * unload the packets associated with the packet id.  (The packet id is 1 for ":01:")
     * Note unload of a packet that is not loaded does not cause exception.
     * @param id the integer id 1-99.
     * @throws StreamException
     */
    private void unload( int id ) throws StreamException {
        for ( Entry<String,Accum> entry: accum.entrySet() ) {
            Accum a= entry.getValue();
            if ( a.id==id ) {
                unload(a.pd);
            }
        }
    }

    /**
     * unload all the packets for this interval on to the stream
     * @param pd
     */
    private void unload( PacketDescriptor pd ) throws StreamException {

        ByteBuffer data= ByteBuffer.allocate( pd.sizeBytes() );
        data.order( byteOrder );

        int np= pd.getPlanes().size();
        int ip= 0;

        for (PlaneDescriptor planed : pd.getPlanes() ) {

            Accum ac1= accum.get(planed.getName());

            if ( ac1==null ) {
                initAccumulators(pd); //this is the initial condition
                return;
            }

            double[] smin = ac1.Smin;
            double[] smax = ac1.Smax;

            int nn= ac1.N;

            if ( nn==0 ) {
                initAccumulators(pd); //this is the initial condition
                return;
            }

            if ( ip<np-1 ) { // Time, Min
                if (planed.getElements() > 1) {
                    for (int ii = 0; ii < planed.getElements(); ii++) {
                        planed.getType().write( smin[ii], data );
                    }
                } else {
                    planed.getType().write( smin[0], data );
                }
            } else {         // Max
                if (planed.getElements() > 1) {
                    for (int ii = 0; ii < planed.getElements(); ii++) {
                        planed.getType().write( smax[ii], data );
                    }
                } else {
                    planed.getType().write( smax[0], data );
                }
            }

            if ( ( ip==np-1 ) && planed.getType().isAscii() && Character.isWhitespace( data.get( data.capacity() - 1) ) ) {
                data.put( data.capacity() - 1, (byte) CHAR_NEWLINE );
            }

            ip++;
        }

        data.flip();

        sink.packet( pd, data );

    }



    /**
     * QDataSets or parts of datasets as they come in.
     * @param sd
     * @param ds
     */
    public void packetData( PacketDescriptor pd, PlaneDescriptor pld, QDataSet ds ) {

    }

    public static void doit( InputStream in, OutputStream out, Datum cadence ) throws StreamException {

        ReduceFilter pipe= new ReduceFilter();
        pipe.setCadence( cadence );

        ReadableByteChannel rin= java.nio.channels.Channels.newChannel( in );

        FormatStreamHandler fsh= new FormatStreamHandler();
        fsh.setOutputStream( out );

        pipe.sink= fsh;

        StreamTool.readStream( rin, pipe );

    }

    public static void main( String[] args ) throws StreamException, MalformedURLException, IOException, ParseException {
        //InputStream in= System.in;
        //OutputStream out= System.out;
        
        InputStream in = new java.net.URL("file:///home/jbf/ct/hudson/data.backup/qds/aggregation.qds").openStream();
        OutputStream out=  new java.io.FileOutputStream("/home/jbf/ct/hudson/data.backup/qds/aggregation.reduce.minmax.qds");
        
        args= new String[] { "360" };
        if ( args.length!=1 ) {
            if ( args.length==2 && args[1].startsWith("file:") ) {
                //in= new FileInputStream( args[1].substring(5) );
            } else {
                System.err.println("java -jar autoplot.jar org.qstream.filter.MinMaxFilter <seconds>");
                System.err.println(  "arg2 can be set to input file: file:...");
                System.exit(-1);
            }
        }
        Datum cadence= Units.seconds.parse(args[0]);

        System.err.println( "this does not appear to be implemented, testing with sftp://jbf@papco.org:/home/jbf/ct/hudson/data.backup/qds/aggregation.qds" );
        doit( in, out, cadence );
        //doit( in, System.out, cadence );
        
        //doit( new java.net.URL("file:///home/jbf/project/autoplot/data.nobackup/qds/fm2_jmp_2012_03_13_msim3.qds").openStream(), new java.io.FileOutputStream("/tmp/fm2_jmp_2012_03_13_msim3.qds"), cadence );
        //doit( new java.net.URL("file:///tmp/0B000800408DD710.20120302.qds").openStream(), new FileOutputStream("/tmp/0B000800408DD710.20120302.reduce.qds"), cadence );
        //doit( new java.net.URL("file:///tmp/po_h0_hyd_20000128.qds").openStream(), new FileOutputStream("/tmp/po_h0_hyd_20000128.reduce.qds"), cadence );
        //doit( new java.net.URL("file:///home/jbf/data.nobackup/qds/doesntReduce.ascii.qds").openStream(), new java.io.FileOutputStream("/home/jbf/data.nobackup/qds/doesntReduce.reduced.qds"), cadence );
    }
}
