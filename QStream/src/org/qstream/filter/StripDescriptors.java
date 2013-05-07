package org.qstream.filter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.text.ParseException;
import org.virbo.qstream.FormatStreamHandler;
import org.virbo.qstream.PacketDescriptor;
import org.virbo.qstream.QDataSetStreamHandler;
import org.virbo.qstream.StreamComment;
import org.virbo.qstream.StreamDescriptor;
import org.virbo.qstream.StreamException;
import org.virbo.qstream.StreamHandler;
import org.virbo.qstream.StreamTool;
import org.w3c.dom.Element;

/**
 * Remove the packets and retain only the descriptors, which is useful for debugging.
 * @author jbf
 */
public class StripDescriptors implements StreamHandler {

    StreamHandler sink;
    int[] count= new int[100];
    
    public void streamDescriptor(StreamDescriptor sd) throws StreamException {        
        sink.streamDescriptor(sd);
    }

    public void packetDescriptor(PacketDescriptor pd) throws StreamException {
        for ( int i=0; i<100; i++ ) {
            if ( count[i]>0 ) {
                StreamComment sc= new StreamComment("log:INFO", String.format( "%d type %d packets", count[i], i ) );
                sink.streamComment( sc );
                count[i]=0;
            }
        }
        sink.packetDescriptor(pd);
    }

    public void packet(PacketDescriptor pd, ByteBuffer data) throws StreamException {
        count[0]++; //TODO: why can't we get the packet ID?
    }

    public void streamClosed(StreamDescriptor sd) throws StreamException {
        for ( int i=0; i<100; i++ ) {
            if ( count[i]>0 ) {
                StreamComment sc= new StreamComment("log:INFO", String.format( "%d type %d packets", count[i], i ) );
                sink.streamComment( sc );
                count[i]=0;
            }
        }
        sink.streamClosed(sd);
    }

    
    public void streamException(StreamException se) throws StreamException {
        sink.streamException(se);
    }

    public void streamComment(StreamComment sd) throws StreamException {
        sink.streamComment(sd);
    }
    
    public static void main( String[] args ) throws StreamException, MalformedURLException, IOException, ParseException {
        //File f = new File( "/home/jbf/ct/hudson/data/qds/proton_density.qds" );
        File f = new File( "/home/cwp/tmp/juno_test/missing_freq.qds" );
        InputStream in = new FileInputStream(f);
        
        //InputStream in= System.in;
        
        FormatStreamHandler fsh= new FormatStreamHandler();
        fsh.setOutputStream( System.out );

        StripDescriptors filter= new StripDescriptors();
        filter.sink= fsh;

        StreamTool.readStream( Channels.newChannel(in), filter );

    }    
}
