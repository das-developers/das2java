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
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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

    public void setWritableByteChannel( WritableByteChannel outs ) {
        this.out= outs;
    }

    /**
     * create a stream descriptor packet.  TODO: createPacketDescriptor.  See SerialStreamFormatter for examples of how this
     * would be done.
     * @param name
     * @param asciiTypes
     * @param isBigEndian
     * @return
     */
    public StreamDescriptor createStreamDescriptor( String name, boolean asciiTypes, boolean isBigEndian ) {
        try {
            StreamDescriptor sd = new StreamDescriptor(DocumentBuilderFactory.newInstance());
            Document document = sd.newDocument(sd);

            Element streamElement = document.createElement("stream");

            streamElement.setAttribute("dataset_id", name);
            if (asciiTypes == false) {
                streamElement.setAttribute("byte_order", isBigEndian ? "big_endian" : "little_endian");
            }
            return sd;

        } catch ( ParserConfigurationException ex ) {
            throw new RuntimeException(ex);
        }
        
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

    private String xmlSafe( String s ) {
        s= s.replaceAll("\'", "");
        return s;
    }
    
    public void streamException(StreamException se) throws StreamException {
        String msg= String.format("<exception type='%s' message='%s'/>\n", se.getClass().toString(), xmlSafe(se.getMessage()) );
        try {
            out.write( ByteBuffer.wrap( String.format( "[xx]%06d", msg.length() ).getBytes("US-ASCII") ) );
            out.write( ByteBuffer.wrap( msg.getBytes("US-ASCII") ) );
        } catch ( IOException ex ) {
            throw new StreamException(ex);
        }
    }

    public void streamComment(StreamComment se) throws StreamException {
        String msg= String.format("<comment type='%s' message='%s'/>\n", se.getType(), xmlSafe(se.getMessage()) );
        try {
            out.write( ByteBuffer.wrap( String.format( "[xx]%06d", msg.length() ).getBytes("US-ASCII") ) );
            out.write( ByteBuffer.wrap( msg.getBytes("US-ASCII") ) );
        } catch ( IOException ex ) {
            throw new StreamException(ex);
        }
    }

}
