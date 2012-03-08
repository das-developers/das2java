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
import java.nio.channels.Channels;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.das2.datum.Datum;
import org.das2.datum.Units;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SemanticOps;
import org.virbo.qstream.PacketDescriptor;
import org.virbo.qstream.PlaneDescriptor;
import org.virbo.qstream.QDataSetStreamHandler;
import org.virbo.qstream.SimpleStreamFormatter;
import org.virbo.qstream.StreamDescriptor;
import org.virbo.qstream.StreamException;
import org.virbo.qstream.StreamHandler;
import org.virbo.qstream.StreamTool;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Reduce packets of the same type by combining packets together.  Currently this
 * just does linear averages of the data, but this can easily be extended to support
 * other combinations, such as min and max.
 * @author jbf
 */
public class ReduceFilter implements StreamHandler {

    StreamHandler sink;

    double lengthSeconds;
    double length; // in the stream units.
    double nextTag;

    class Accum {
        double[] S;
        int N;
        double B; // base offset for S.  We remove this before putting data into the accumulation.
    }

    Map<String, Accum> accum;

    public ReduceFilter() {
        accum= new HashMap();
        lengthSeconds= 60;
        nextTag= 0;
    }

    public void streamDescriptor(StreamDescriptor sd) throws StreamException {
        sink.streamDescriptor(sd);
    }

    public void packetDescriptor(PacketDescriptor pd) throws StreamException {
        initAccumulators(pd);
        Element ele= pd.getDomElement();

        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();

        
        try {
            XPathExpression expr;
            Node xp;

            // figure out units.
            Units xunits= null;
            expr=  xpath.compile("/packet/qdataset[1]/properties/property[@name='UNITS']/@value");
            xp= (Node)expr.evaluate( ele,XPathConstants.NODE);
            String sunits= xp.getNodeValue();
            try {
                xunits= SemanticOps.lookupTimeUnits(sunits);
                double secmult= Units.seconds.getConverter( xunits.getOffsetUnits() ).convert(1);
                length= secmult * lengthSeconds;

            } catch ( ParseException ex ) {
                throw new StreamException("Unable to parse time units", ex);
            }

            // reset/set the cadence.
            expr= xpath.compile("/packet/qdataset[1]/properties/property[@name='CADENCE']/@value");
            xp= (Node)expr.evaluate( ele,XPathConstants.NODE);
            if ( xp!=null ) {
                xp.setNodeValue( String.format( "%f units:UNITS=s", lengthSeconds ) );
            } else {
                XPathExpression  parentExpr= xpath.compile("/packet/qdataset[1]/properties"); // it should at least have a units, so it's safe to assume this node is to be found.
                Node props= (Node) parentExpr.evaluate(ele,XPathConstants.NODE);
                if ( props==null ) {
                    throw new StreamException("No properties node found under the first dataset, which should be time.");
                }
                Element propNode= ele.getOwnerDocument().createElement( "property" );
                propNode.setAttribute("name", "CADENCE");
                propNode.setAttribute("type", "Rank0DataSet" );
                propNode.setAttribute("value", String.format( "%f units:UNITS=s", lengthSeconds ) );
                props.appendChild( propNode );
            }

        } catch (XPathExpressionException ex) {
            Logger.getLogger(ReduceFilter.class.getName()).log(Level.SEVERE, null, ex);
        }

        sink.packetDescriptor(pd);
    }

    public void streamClosed(StreamDescriptor sd) throws StreamException {
        //TODO: flush remaining accum
        sink.streamClosed(sd);
    }

    public void streamException(StreamException se) throws StreamException {
        sink.streamException(se);
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
            ac1.S= ss;
            ac1.N= 0;
            ac1.B= -1e38;
            accum.put(planed.getName(), ac1 );
        }
    }
    
    /**
     * unload all the packets for this interval on to the stream
     * @param pd
     */
    private void unload( PacketDescriptor pd, int capacity ) throws StreamException {
        ByteBuffer data= ByteBuffer.allocate(capacity);

        for (PlaneDescriptor planed : pd.getPlanes() ) {

            Accum ac1= accum.get(planed.getName());

            if ( ac1==null ) {
                initAccumulators(pd); //this is the initial condition
                return;
            }

            double[] ss = ac1.S;
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
    public void packet(PacketDescriptor pd, ByteBuffer data) throws StreamException {

        //TODO: nextTag may be attached to packet ids.  This is coded with just one.

        List<PlaneDescriptor> planes= pd.getPlanes();

        PlaneDescriptor t0= planes.get(0);
        double ttag= t0.getType().read(data);

        if ( ttag>nextTag ) {
            unload( pd, data.limit() );
            initAccumulators(pd);
            nextTag= ( 1 + Math.floor( ttag/length ) ) * length;
        }

        data.rewind();

        for (PlaneDescriptor planed : pd.getPlanes() ) {

            Accum ac1= accum.get(planed.getName());

            double[] ss = ac1.S;
            int nn= ac1.N;
            double bb= ac1.B;

            if ( nn==0 ) {
                int pos= data.position();
                bb= planed.getType().read(data);
                data.position(pos);
                ac1.B= bb;
            }

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

    /**
     * set the cadence to reduce to target the data.  This should be convertible to seconds.
     * Note we assume all packets have the same offset units (e.g. microseconds).
     * @param cadence
     */
    void setCadence(Datum cadence) {
        lengthSeconds= cadence.doubleValue( Units.seconds );
    }

    public static void main( String[] args ) throws StreamException, FileNotFoundException, IOException {
        File f = new File( "/home/jbf/ct/hudson/data/qds/proton_density.qds" );

        InputStream in = new FileInputStream(f);
        QDataSetStreamHandler handler = new QDataSetStreamHandler();

        ReduceFilter filter= new ReduceFilter();
        filter.lengthSeconds= 3600; //TODO: client must know the units of the qstream.
        
        filter.sink= handler;

        StreamTool.readStream( Channels.newChannel(in), filter );
        //StreamTool.readStream( Channels.newChannel(in), handler ); // test without filter.

        QDataSet qds = handler.getDataSet();

        System.err.println( "result= "+qds );

        SimpleStreamFormatter format= new SimpleStreamFormatter();
        format.format( qds, new FileOutputStream("/tmp/proton_density.reduced.qds"), true );

    }
}
