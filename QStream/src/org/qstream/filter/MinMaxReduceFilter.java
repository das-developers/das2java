/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.qstream.filter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.das2.datum.InconvertibleUnitsException;
import org.das2.datum.Units;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SemanticOps;
import org.virbo.qstream.PacketDescriptor;
import org.virbo.qstream.PlaneDescriptor;
import org.virbo.qstream.QDataSetStreamHandler;
import org.virbo.qstream.Rank0DataSetSerializeDelegate;
import org.virbo.qstream.SimpleStreamFormatter;
import org.virbo.qstream.StreamDescriptor;
import org.virbo.qstream.StreamException;
import org.virbo.qstream.StreamTool;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 *
 * @author jbf
 */
public class MinMaxReduceFilter extends QDataSetsFilter {

    class Accum {
        PacketDescriptor pd; // All planes should have the same value.
        int id;       // the packetId.
        int capacity; // total capacity of the packet.  All planes should have the same number.
        String dsid;  // the dataset id, so we can remove this later.
        int N;
        double[] Smin;
        double[] Smax;
        double B; // base offset for S.  We remove this before putting data into the accumulation.
        double nextTag;
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

            boolean skip= false;

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
                    skip= true;
                } catch ( ParseException ex ) {
                    skip= true;
                }
            } else {
                skip= true;
            }

            if ( !skip ) {

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
                    double oldCadenceSeconds=0;
                    try {
                        QDataSet o = (QDataSet) new Rank0DataSetSerializeDelegate().parse( "rank0dataset", scadence );
                        oldCadenceSeconds= DataSetUtil.asDatum(o).doubleValue( Units.seconds );
                        if ( lengthSeconds<oldCadenceSeconds ) {
                            lengthSeconds= oldCadenceSeconds;
                        }
                    } catch ( ParseException ex ) {
                        System.err.println("unable to parse cadence");
                    }
                    xp.setNodeValue( String.format( "%f units:UNITS=s", lengthSeconds ) );
                } else {
                    // don't install the cadence when there wasn't one already.  Let the client guess as they would have
                    // if the cadence were not specified.
                }
            }

            this.skip.put(pd, skip);
            this.nextTags.put( pd, 0. );

        } catch (XPathExpressionException ex) {
            Logger.getLogger(ReduceFilter.class.getName()).log(Level.SEVERE, null, ex);
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

    public static void main( String[] args ) throws StreamException, IOException  {
        File f = new File( "/home/jbf/ct/hudson/data/qds/proton_density.qds" );

        InputStream in = new FileInputStream(f);
        QDataSetStreamHandler handler = new QDataSetStreamHandler();

        MinMaxReduceFilter filter= new MinMaxReduceFilter();
        filter.lengthSeconds= 3600; //TODO: client must know the units of the qstream.

        filter.sink= new QDataSetsFilter.QDataSetSink() {

            @Override
            public void packetData(PacketDescriptor pd, PlaneDescriptor pld, QDataSet ds) {
                System.err.println( "From "+pld.getName() + ": " + ds );
            }

        };

        StreamTool.readStream( Channels.newChannel(in), filter );
        //StreamTool.readStream( Channels.newChannel(in), handler ); // test without filter.

        QDataSet qds = handler.getDataSet();

        System.err.println( "result= "+qds );

        SimpleStreamFormatter format= new SimpleStreamFormatter();
        format.format( qds, new FileOutputStream("/tmp/proton_density.reduced.qds"), true );

    }
}
