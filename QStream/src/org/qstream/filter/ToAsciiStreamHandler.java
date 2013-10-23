/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.qstream.filter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.virbo.qstream.AsciiTransferType;
import org.virbo.qstream.FormatStreamHandler;
import org.virbo.qstream.PacketDescriptor;
import org.virbo.qstream.PlaneDescriptor;
import org.virbo.qstream.StreamComment;
import org.virbo.qstream.StreamDescriptor;
import org.virbo.qstream.StreamException;
import org.virbo.qstream.StreamHandler;
import org.virbo.qstream.StreamTool;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * This is not complete.  It needs to be finished off.
 * @author jbf
 */
public class ToAsciiStreamHandler implements StreamHandler {

    public ToAsciiStreamHandler() {
        try {
            format= new FormatStreamHandler();
            format.setOutputStream( new FileOutputStream("/tmp/foo.qds") );
            //format.setOutputStream( System.out );
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ToAsciiStreamHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    FormatStreamHandler format;
    
    int newPacketSize;
    PacketDescriptor pdout;
    StreamDescriptor sdout;
    
    @Override
    public void streamDescriptor(StreamDescriptor sd) throws StreamException {
        sdout= new StreamDescriptor(DocumentBuilderFactory.newInstance());
        format.streamDescriptor(sd);
    }

    @Override
    public void packetDescriptor(PacketDescriptor pd) throws StreamException {
        newPacketSize= 0;
        try {
            PacketDescriptor pdout= (PacketDescriptor) pd.clone();
            for ( PlaneDescriptor p: pdout.getPlanes() ) {
                p.setType( new AsciiTransferType(10,true) );
                newPacketSize+= 10 * p.getElements();
            }
            sdout.addDescriptor(pdout);
            try {
                XPath xpath= XPathFactory.newInstance().newXPath();
                Element e = (Element)pd.getDomElement();
                XPathExpression expr = xpath.compile("/packet/qdataset/values");
                Object o = expr.evaluate(e, XPathConstants.NODESET);
                NodeList nodes = (NodeList) o;

                for ( int i=0; i<nodes.getLength(); i++ ) {
                        Element n= (Element)nodes.item(i);
                        if ( n.hasAttribute("encoding") ) {
                            n.setAttribute("encoding","ascii10");
                        }
                }
                pdout.setDomElement( e );
            } catch ( XPathExpressionException ex ) {
                
            }
            format.packetDescriptor(pdout);
            this.pdout= pdout;
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(ToAsciiStreamHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void packet(PacketDescriptor pd, ByteBuffer data) throws StreamException {
        ByteBuffer dataOut= ByteBuffer.allocate(newPacketSize);
        data.flip();
        int np= pd.getPlanes().size();
        for ( int ip=0; ip<np; ip++ ) {
            PlaneDescriptor pout= pdout.getPlanes().get(ip);
            PlaneDescriptor pin= pd.getPlanes().get(ip);
            data.limit( data.limit() + pin.getElements() * pin.getType().sizeBytes() );
            for ( int i=0; i<pin.getElements(); i++ ) {
                double d= pin.getType().read(data);
                if ( ip==np-1 && i==pin.getElements()-1 ) {
                    String s= String.format( "%9.3f", d );
                    dataOut.put( s.substring(0,9).getBytes() );
                    dataOut.put( (byte)'\n' );
                } else {
                    String s= String.format( "%9.3f", d );
                    dataOut.put( s.substring(0,9).getBytes() );
                    dataOut.put( (byte)' ' );
                }
            }            
        }
        dataOut.flip();
        format.packet(pdout,dataOut);
    }

    @Override
    public void streamClosed(StreamDescriptor sd) throws StreamException {
        format.streamClosed(sd);
    }

    @Override
    public void streamException(StreamException se) throws StreamException {
        format.streamException(se);
    }

    @Override
    public void streamComment(StreamComment sd) throws StreamException {
        format.streamComment(sd);
    }
    
    public static void main( String[] args ) throws FileNotFoundException, StreamException {
        InputStream in= new FileInputStream("/Users/jbf/NetBeansProjects/autoplot/QStream/src/test/binary.qds");
        StreamTool st= new StreamTool();
        StreamHandler sink= new ToAsciiStreamHandler();
        st.readStream( Channels.newChannel(in), sink );
    }    
}
