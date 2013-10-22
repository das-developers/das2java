/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.qstream.filter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import org.virbo.qstream.AsciiTransferType;
import org.virbo.qstream.FormatStreamHandler;
import org.virbo.qstream.PacketDescriptor;
import org.virbo.qstream.PlaneDescriptor;
import org.virbo.qstream.StreamComment;
import org.virbo.qstream.StreamDescriptor;
import org.virbo.qstream.StreamException;
import org.virbo.qstream.StreamHandler;
import org.virbo.qstream.StreamTool;

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
                //newPacketSize+= p.getElements() * p.getType().sizeBytes();
            }
            sdout.addDescriptor(pdout);
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
        for ( int ip=0; ip<pd.getPlanes().size(); ip++ ) {
            PlaneDescriptor pout= pdout.getPlanes().get(ip);
            PlaneDescriptor pin= pd.getPlanes().get(ip);
            data.limit( data.limit() + pin.getElements() * pin.getType().sizeBytes() );
            for ( int i=0; i<pin.getElements(); i++ ) {
                double d= pin.getType().read(data);
                dataOut.put( String.format( "f10.3", d ).getBytes() );
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
        InputStream in= new FileInputStream("/home/jbf/project/autoplot/autoplot-code/QStream/src/test/binary.qds");
        StreamTool st= new StreamTool();
        StreamHandler sink= new ToAsciiStreamHandler();
        st.readStream( Channels.newChannel(in), sink );
    }    
}
