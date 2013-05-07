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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.virbo.dataset.QDataSet;
import org.virbo.qstream.PacketDescriptor;
import org.virbo.qstream.QDataSetStreamHandler;
import org.virbo.qstream.SimpleStreamFormatter;
import org.virbo.qstream.StreamComment;
import org.virbo.qstream.StreamDescriptor;
import org.virbo.qstream.StreamException;
import org.virbo.qstream.StreamHandler;
import org.virbo.qstream.StreamTool;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Demonstrate how a node is added to a packet descriptor.
 * @author jbf
 */
public class RenderTypeFilter implements StreamHandler {

    private static final Logger logger= Logger.getLogger("qstream");
    
    StreamHandler sink;
    ByteOrder byteOrder;

    double lengthSeconds;
    double length; // in the stream units.
    double nextTag;

    StreamDescriptor sd;

    int size;
    org.virbo.dsops.Ops.FFTFilterType filterType;

    public RenderTypeFilter() {
        size= 2048;
        filterType= org.virbo.dsops.Ops.FFTFilterType.Hanning;
    }

    public void streamDescriptor(StreamDescriptor sd) throws StreamException {
        this.sd= sd;
        sink.streamDescriptor(sd);
        byteOrder= sd.getByteOrder();
    }

    public void packetDescriptor(PacketDescriptor pd) throws StreamException {

        try {
            Element ele= pd.getDomElement(); // This should be a document --Ed

            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();

            NodeList nl= (NodeList) xpath.evaluate( "/packet/qdataset", ele, XPathConstants.NODESET );
            if ( nl.getLength()==2 ) {
                XPathExpression expr= xpath.compile("/packet/qdataset[2]/properties");
                Element props= (Element) expr.evaluate(ele,XPathConstants.NODE);
                Element rt= ele.getOwnerDocument().createElement("property");
                rt.setAttribute("name", QDataSet.RENDER_TYPE );
                rt.setAttribute("value", "nnSpectrogram" );
                rt.setAttribute("type", "String");

                props.appendChild(rt);
            }

            sink.packetDescriptor(pd);

        } catch (XPathExpressionException ex) {
            logger.log(Level.SEVERE, null, ex);
            throw new StreamException(ex);
        }

    }

    public void streamClosed(StreamDescriptor sd) throws StreamException {
        sink.streamClosed(sd);
    }

    public void streamException(StreamException se) throws StreamException {
        sink.streamException(se);
    }

    public void streamComment(StreamComment se) throws StreamException {
        sink.streamComment(se);
    }
    
    final char CHAR_NEWLINE= '\n';

    public void packet(PacketDescriptor pd, ByteBuffer data) throws StreamException {
        sink.packet(pd, data);
    }

    public static void main( String[] args ) throws StreamException, FileNotFoundException, IOException {
        File f = new File( "/home/jbf/data.nobackup/qds/waveformTable.qds" );


        InputStream in = new FileInputStream(f);
        QDataSetStreamHandler handler = new QDataSetStreamHandler();

        RenderTypeFilter filter= new RenderTypeFilter();
        
        filter.sink= handler;

        StreamTool.readStream( Channels.newChannel(in), filter );
        //StreamTool.readStream( Channels.newChannel(in), handler ); // test without filter.

        QDataSet qds = handler.getDataSet();

        System.err.println( "result= "+qds );

        SimpleStreamFormatter format= new SimpleStreamFormatter();

        format.format( qds, new FileOutputStream("/tmp/plotme.fftd.qds"), false );

    }
}
