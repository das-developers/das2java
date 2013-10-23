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
import java.util.LinkedHashMap;
import java.util.Map;
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
import org.virbo.qstream.TransferType;
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
            pdouts= new LinkedHashMap<Integer, PacketDescriptor>();
            newEncodings= new LinkedHashMap<String,TransferType>();
            newEncodings.put( "double",new AsciiTransferType(20,false) );
            newEncodings.put( "float",new AsciiTransferType(10,false) );
            newEncodings.put( "int8",new AsciiTransferType(20,false) );
            newEncodings.put( "int4",new AsciiTransferType(10,false) );
            newEncodings.put( "int2",new AsciiTransferType(7,false) ); // 7 is the shortest supported.
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ToAsciiStreamHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    FormatStreamHandler format;
    
    int newPacketSize;
    StreamDescriptor sdout;
    Map<Integer,PacketDescriptor> pdouts;
    Map<String,TransferType> newEncodings;
    
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
                TransferType newTT= newEncodings.get( p.getType().name() );
                p.setType( newTT );
                newPacketSize+= 10 * p.getElements();
            }
            sdout.addDescriptor(pdout);
            format.packetDescriptor(pdout);
            this.pdouts.put( pd.getPacketId(), pdout);
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(ToAsciiStreamHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void packet(PacketDescriptor pd, ByteBuffer data) throws StreamException {
        ByteBuffer dataOut= ByteBuffer.allocate(newPacketSize);
        data.flip();
        PacketDescriptor pdout= pdouts.get(pd.getPacketId());
        int np= pd.getPlanes().size();
        for ( int ip=0; ip<np; ip++ ) {
            PlaneDescriptor pout= pdout.getPlanes().get(ip);
            PlaneDescriptor pin= pd.getPlanes().get(ip);
            data.limit( data.limit() + pin.getElements() * pin.getType().sizeBytes() );
            for ( int i=0; i<pin.getElements(); i++ ) {
                double d= pin.getType().read(data);
                if ( ip==np-1 && i==pin.getElements()-1 ) {
                    pout.getType().write( d, dataOut );
                    //dataOut.put( s.substring(0,9).getBytes() );
                    //dataOut.put( (byte)'\n' );
                } else {
                    pout.getType().write( d, dataOut );
                    //String s= String.format( "%9.3f", d );
                    //dataOut.put( s.substring(0,9).getBytes() );
                    //dataOut.put( (byte)' ' );
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
        InputStream in= new FileInputStream("/home/jbf/temp/autoplot/QStream/src/test/binary.qds");
        StreamTool st= new StreamTool();
        StreamHandler sink= new ToAsciiStreamHandler();
        st.readStream( Channels.newChannel(in), sink );
    }    
}
