/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.qstream;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Writes the stream based on the messages sent to it.  This overlaps with the SimpleStreamFormatter,
 * but was needed to support streams.  The SimpleStreamFormatter took a QDataSet and formatted it.  This formats
 * based on the callbacks.
 *
 * Note the library was poorly designed, and this is pretty simple because most of the hard work is buried within
 * the StreamDescriptor.  StreamDescriptor should be simplified, and the code should be moved to here.
 *
 * @author jbf
 */
public class FormatStreamHandler implements StreamHandler {

    WritableByteChannel out;

    StreamDescriptor sd;

    public void setOutputStream( OutputStream outs ) {
        this.out= Channels.newChannel(outs);
    }
    
    public void streamDescriptor(StreamDescriptor sd) throws StreamException {
        this.sd= sd;
        this.sd.setFactory( DocumentBuilderFactory.newInstance() );
        try {
            this.sd.addDescriptor(sd); // allocate [00] for itself.  This is very goofy, I realize now...
            this.sd.newDocument(sd);
            sd.send( sd, out );
        } catch ( IOException ex ) {
            throw new StreamException(ex);
        } catch ( ParserConfigurationException ex ) {
            throw new StreamException(ex);
        }
    }

    public void packetDescriptor(PacketDescriptor pd) throws StreamException {
        this.sd.addDescriptor(pd);
        try {
            sd.newDocument(pd);
            this.sd.send(pd,out);
        } catch ( IOException ex ) {
            throw new StreamException(ex);
        } catch ( ParserConfigurationException ex ) {
            throw new StreamException(ex);
        }
    }

    public void packet(PacketDescriptor pd, ByteBuffer data) throws StreamException {
        try {
            out.write( ByteBuffer.wrap( String.format( ":%02d:", sd.descriptorId(pd) ).getBytes("US-ASCII")) );
            out.write(data);
        } catch ( IOException ex ) {
            throw new StreamException(ex);
        }


    }

    public void streamClosed(StreamDescriptor sd) throws StreamException {
        
    }

    public void streamException(StreamException se) throws StreamException {
        String msg= String.format("<exception type='%s' message='%s'/>", se.getClass().toString(), se.getMessage() );
        try {
            out.write( ByteBuffer.wrap( String.format( "[xx]%06i", msg.length() ).getBytes("US-ASCII") ) );
            out.write( ByteBuffer.wrap( msg.getBytes("US-ASCII") ) );
        } catch ( IOException ex ) {
            throw new StreamException(ex);
        }
    }

}
