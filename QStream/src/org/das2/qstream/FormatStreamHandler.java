
package org.das2.qstream;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.das2.dataset.NoDataInIntervalException;
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
            StreamDescriptor lsd = new StreamDescriptor(DocumentBuilderFactory.newInstance());
            Document document = lsd.newDocument(lsd);

            Element streamElement = document.createElement("stream");

            streamElement.setAttribute("dataset_id", name);
            if (asciiTypes == false) {
                streamElement.setAttribute("byte_order", isBigEndian ? "big_endian" : "little_endian");
            }
            return lsd;

        } catch ( ParserConfigurationException ex ) {
            throw new RuntimeException(ex);
        }
        
    }

    @Override
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

    @Override
    public void packetDescriptor(PacketDescriptor pd) throws StreamException {
        this.sd.addDescriptor(pd);
        try {
            Document d= sd.newDocument(pd);
            if ( pd.getDomElement()==null ) {
                Element ele= d.createElement("packet");
                for ( PlaneDescriptor pld: pd.getPlanes() ) { // all this begs the question, where is the information stored?  Is it in the PlaneDescriptor, or is it in the Document???  Executive decision 2013-10-23: it is in the document, and xpath should be used to process.
                    Element qdatasetElement= d.createElement("qdataset");
                    qdatasetElement.setAttribute( "id", pld.getName() );
                    qdatasetElement.setAttribute( "rank", String.valueOf(pld.getRank()) );
                    ele.appendChild(qdatasetElement);
                }
                pd.setDomElement( ele );
                throw new IllegalStateException("this implementation is not complete.  See SimpleStreamFormatter");
            }
            this.sd.send(pd,out);
        } catch ( IOException ex ) {
            throw new StreamException(ex);
        } catch ( ParserConfigurationException ex ) {
            throw new StreamException(ex);
        }
    }

    @Override
    public void packet(PacketDescriptor pd, ByteBuffer data) throws StreamException {
        try {
            out.write( ByteBuffer.wrap( String.format( ":%02d:", sd.descriptorId(pd) ).getBytes("US-ASCII")) );
            out.write(data);
        } catch ( IOException ex ) {
            throw new StreamException(ex);
        }


    }

    @Override
    public void streamClosed(StreamDescriptor sd) throws StreamException {
        
    }

    private String xmlSafe( String s ) {
        s= s.replaceAll("\'", "");
        return s;
    }
    
    @Override
    public void streamException(StreamException se) throws StreamException {
        String type= "StreamException";
        if ( se.getCause() instanceof NoDataInIntervalException ) {
            type= "NoDataInInterval";
        }
        String msg= String.format("<exception type='%s' message='%s'/>\n", type, xmlSafe(se.getMessage()) );
        try {
            out.write( ByteBuffer.wrap( String.format( "[xx]%06d", msg.length() ).getBytes("US-ASCII") ) );
            out.write( ByteBuffer.wrap( msg.getBytes("US-ASCII") ) );
        } catch ( IOException ex ) {
            throw new StreamException(ex);
        }
    }

    @Override
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
