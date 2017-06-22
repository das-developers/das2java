/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.qstream.filter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.das2.qds.QDataSet;
import org.das2.qstream.PacketDescriptor;
import org.das2.qstream.StreamComment;
import org.das2.qstream.StreamDescriptor;
import org.das2.qstream.StreamException;
import org.das2.qstream.StreamHandler;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Demonstrate how a node is added to a packet descriptor.
 * @author jbf
 */
public class RenderTypeFilter implements StreamHandler {

    private static final Logger logger= Logger.getLogger("qstream");
    
    StreamHandler sink;

    public RenderTypeFilter() {
    }

    public void setSink( StreamHandler sink ) {
        this.sink= sink;
    }
    
    @Override
    public void streamDescriptor(StreamDescriptor sd) throws StreamException {
        sink.streamDescriptor(sd);
    }

    @Override
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
            logger.log(Level.SEVERE, ex.getMessage(), ex);
            throw new StreamException(ex);
        }

    }

    @Override
    public void streamClosed(StreamDescriptor sd) throws StreamException {
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
    
    @Override
    public void packet(PacketDescriptor pd, ByteBuffer data) throws StreamException {
        sink.packet(pd, data);
    }

//    public static void main( String[] args ) throws StreamException, FileNotFoundException, IOException {
//        File f = new File( "/home/jbf/data.nobackup/qds/waveformTable.qds" );
//
//
//        InputStream in = new FileInputStream(f);
//        QDataSetStreamHandler handler = new QDataSetStreamHandler();
//
//        RenderTypeFilter filter= new RenderTypeFilter();
//        
//        filter.sink= handler;
//
//        StreamTool.readStream( Channels.newChannel(in), filter );
//        //StreamTool.readStream( Channels.newChannel(in), handler ); // test without filter.
//
//        QDataSet qds = handler.getDataSet();
//
//        System.err.println( "result= "+qds );
//
//        SimpleStreamFormatter format= new SimpleStreamFormatter();
//
//        format.format( qds, new FileOutputStream("/tmp/plotme.fftd.qds"), false );
//
//    }
}
