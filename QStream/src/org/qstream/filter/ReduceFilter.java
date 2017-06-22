/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.qstream.filter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.das2.datum.CacheTag;
import org.das2.datum.Datum;
import org.das2.datum.InconvertibleUnitsException;
import org.das2.datum.Units;
import org.das2.qds.DataSetUtil;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;
import org.virbo.qstream.CacheTagSerializeDelegate;
import org.virbo.qstream.PacketDescriptor;
import org.virbo.qstream.PlaneDescriptor;
import org.virbo.qstream.Rank0DataSetSerializeDelegate;
import org.virbo.qstream.SerializeDelegate;
import org.virbo.qstream.StreamComment;
import org.virbo.qstream.StreamDescriptor;
import org.virbo.qstream.StreamException;
import org.virbo.qstream.StreamHandler;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Reduce packets of the same type by combining packets together.  Currently this
 * just does linear averages of the data, but this can easily be extended to support
 * other combinations, such as min and max.
 * @author jbf
 */
public class ReduceFilter implements StreamHandler {

    protected static final Logger logger= Logger.getLogger("qstream");

    StreamHandler sink;
    ByteOrder byteOrder;

    double lengthSeconds;
    double reportCadenceSeconds; // this is the cadence we'll report.  It cannot be less than the cadence in the input stream.
    double length; // in the stream units.
    //double nextTag;

    private static class Accum {
        PacketDescriptor pd; // All planes should have the same value.
        int id;       // the packetId.
        //int capacity; // total capacity of the packet.  All planes should have the same number.
        String dsid;  // the dataset id, so we can remove this later.
        double[] S;
        int N;
        double B; // base offset for S.  We remove this before putting data into the accumulation.
        //double nextTag;
    }

    Map<String, Accum> accum;
    Map<PacketDescriptor, Boolean> skip;
    Map<PacketDescriptor, Double> nextTags;

    StreamDescriptor sd;

    public ReduceFilter() {
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

        try {
            int id= sd.descriptorId(pd); 
            unload( id );
            clear( id );
        } catch ( IllegalArgumentException ex ) {
            //old id has already been cleared
            // vap+das2server:http://emfisis.physics.uiowa.edu/das/das2Server?dataset=rbsp/RBSP-A/HFR_spectra.dsdf&start_time=2012-11-01T23:17&end_time=2012-11-02T08:16
        }

        try {
            XPathExpression expr;
            Node xp;

            boolean lskip= false;

            // figure out units.
            expr=  xpath.compile("/packet/qdataset[1]/properties/property[@name='UNITS']/@value");
            xp= (Node)expr.evaluate( ele,XPathConstants.NODE);
            String sunits= xp==null ? null : xp.getNodeValue(); 
            if ( sunits!=null ) {
                try {
                    Units xunits;
                    xunits= Units.lookupTimeUnits(sunits);
                    double secmult= Units.seconds.getConverter( xunits.getOffsetUnits() ).convert(1);
                    length= secmult * lengthSeconds;
                } catch ( InconvertibleUnitsException ex ) {
                     lskip= true;
                } catch ( ParseException ex ) {
                    lskip= true;
                }
            } else {
                lskip= true;
            }

            if ( !lskip ) {

                initAccumulators(pd);

                // reset/set the cadence.
                expr= xpath.compile("/packet/qdataset[1]/properties/property[@name='CADENCE']/@value");
                xp= (Node)expr.evaluate( ele,XPathConstants.NODE);
                if ( xp!=null ) {
                    String scadence= xp.getNodeValue();
                    double oldCadenceSeconds;
                    SerializeDelegate ser= new Rank0DataSetSerializeDelegate();
                    try {
                        QDataSet o = (QDataSet) ser.parse( "rank0dataset", scadence );
                        oldCadenceSeconds= DataSetUtil.asDatum(o).doubleValue( Units.seconds );
                        if ( lengthSeconds<oldCadenceSeconds ) {
                            reportCadenceSeconds= oldCadenceSeconds;
                            //lengthSeconds= oldCadenceSeconds;
                        }
                    } catch ( ParseException ex ) {
                        throw new StreamException( String.format( "unable to parse cadence \"%s\"", scadence ), ex );
                    }
                    xp.setNodeValue( ser.format( DataSetUtil.asDataSet( reportCadenceSeconds, Units.seconds ) ) );
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
                        //CacheTag ct1= new CacheTag( ct0.getRange(), Units.seconds.createDatum(reportCadenceSeconds) );
                        CacheTag ct1= new CacheTag( ct0.getRange(), Units.seconds.createDatum( lengthSeconds) );
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
                }
            }

            this.skip.put(pd, lskip);
            this.nextTags.put( pd, 0. );

        } catch (XPathExpressionException ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }

        sink.packetDescriptor(pd);
    }

    @Override
    public void streamClosed(StreamDescriptor sd) throws StreamException {
        Set<String> keys= accum.keySet();
        Set<PacketDescriptor> unloaded= new HashSet<PacketDescriptor>();
        for ( String key: keys ) {
            Accum ac1= accum.get(key);
            if ( unloaded.contains(ac1.pd) ) {

            } else {
                unload( ac1.pd );
                unloaded.add(ac1.pd);
            }
        }
        sink.streamClosed(sd);
    }

    @Override
    public void streamException(StreamException se) throws StreamException {
        sink.streamException(se);
    }
    
    @Override
    public void streamComment(StreamComment se) throws StreamException {
        sink.streamComment(se);
    }
        
    /**
     * initialize the accumulators to an empty state.
     * @param pd
     */
    private void initAccumulators(PacketDescriptor pd) {
        List<PlaneDescriptor> planes = pd.getPlanes();
        for (int i = 0; i < planes.size(); i++) {
            PlaneDescriptor planed = planes.get(i);
            double[] ss = new double[DataSetUtil.product(planed.getQube())];
            Accum ac1= new Accum();
            ac1.pd= pd;
            ac1.id= sd.descriptorId(pd);
            ac1.dsid= planed.getName();
            ac1.S= ss;
            ac1.N= 0;
            ac1.B= -1e38;
            accum.put(planed.getName(), ac1 );
        }
    }

    private static final char CHAR_NEWLINE= '\n';

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

            double[] ss = ac1.S;  // Netbeans claims that this is never modified, see line 363!
            int nn= ac1.N;
            double bb= ac1.B;

            if ( nn==0 ) {
                initAccumulators(pd); //this is the initial condition
                return;
            }

            if (planed.getElements() > 1) {
                for (int ii = 0; ii < planed.getElements(); ii++) {
                    double avg= ss[ii]/nn + bb;
                    planed.getType().write( avg, data );
                }
            } else {
                double avg= ss[0]/nn + bb;
                planed.getType().write( avg, data );
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
     * accumulate data into packets by reducing them.
     * @param pd
     * @param data
     * @throws StreamException
     */
    @Override
    public void packet(PacketDescriptor pd, ByteBuffer data) throws StreamException {

        boolean lskip= this.skip.get(pd);
        if ( lskip ) {
            sink.packet( pd, data );

        } else {

            List<PlaneDescriptor> planes= pd.getPlanes();

            PlaneDescriptor t0= planes.get(0);
            double ttag= t0.getType().read(data);

            double nextTag= nextTags.get( pd );

            if ( ttag>nextTag ) {
                unload( pd );
                initAccumulators(pd);
                nextTag= ( 1 + Math.floor( ttag/length ) ) * length;
                nextTags.put( pd, nextTag );
            }

            data.rewind();

            for (PlaneDescriptor planed : pd.getPlanes() ) {

                Accum ac1= accum.get(planed.getName());

                double[] ss = ac1.S; // copy reference to array.
                double bb= ac1.B;
                //ac1.capacity= data.limit();

                if ( bb==-1e38 ) {
                    int pos= data.position();
                    bb= planed.getType().read(data);
                    data.position(pos);
                    ac1.B= bb;
                }

                //TODO: look out for fill...
                if (planed.getElements() > 1) {
                    for (int ii = 0; ii < planed.getElements(); ii++) {
                        ss[ii]+= ( planed.getType().read(data)-bb ); // java nio keeps track of index
                    }
                } else {
                    ss[0]+= planed.getType().read(data)-bb;
                }
                ac1.N+= 1;

            }
        }

    }

    /**
     * set the cadence to reduce to target the data.  This should be convertible to seconds.
     * Note we assume all packets have the same offset units (e.g. microseconds).
     * @param cadence
     */
    public void setCadence(Datum cadence) {
        lengthSeconds= cadence.doubleValue( Units.seconds );
        reportCadenceSeconds= lengthSeconds;
    }

    /**
     * set the sink for the stream components as they are processed.
     * @param sink 
     */
    public void setSink( StreamHandler sink ) {
        this.sink= sink;
    }
    
//    public static void main( String[] args ) throws StreamException, FileNotFoundException, IOException {
//        File f = new File( "/home/jbf/ct/hudson/data/qds/proton_density.qds" );
//
//        InputStream in = new FileInputStream(f);
//        QDataSetStreamHandler handler = new QDataSetStreamHandler();
//
//        ReduceFilter filter= new ReduceFilter();
//        filter.lengthSeconds= 3600; //TODO: client must know the units of the qstream.
//        
//        filter.sink= handler;
//
//        StreamTool.readStream( Channels.newChannel(in), filter );
//        //StreamTool.readStream( Channels.newChannel(in), handler ); // test without filter.
//
//        QDataSet qds = handler.getDataSet();
//
//        System.err.println( "result= "+qds ); // logger okay
//
//        SimpleStreamFormatter format= new SimpleStreamFormatter();
//        format.format( qds, new FileOutputStream("/tmp/proton_density.reduced.qds"), true );
//
//    }
}
