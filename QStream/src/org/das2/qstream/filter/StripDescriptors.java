package org.das2.qstream.filter;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.text.ParseException;
import org.das2.qstream.FormatStreamHandler;
import org.das2.qstream.PacketDescriptor;
import org.das2.qstream.StreamComment;
import org.das2.qstream.StreamDescriptor;
import org.das2.qstream.StreamException;
import org.das2.qstream.StreamHandler;
import org.das2.qstream.StreamTool;

/**
 * Remove the packets and retain only the descriptors, which is useful for debugging.
 * @author jbf
 */
public class StripDescriptors implements StreamHandler {

    StreamHandler sink;
    int[] count= new int[100];
    
    @Override
    public void streamDescriptor(StreamDescriptor sd) throws StreamException {        
        sink.streamDescriptor(sd);
    }

    @Override
    public void packetDescriptor(PacketDescriptor pd) throws StreamException {
        for ( int i=0; i<100; i++ ) {
            if ( count[i]>0 ) {
                StreamComment sc= new StreamComment("log:INFO", String.format( "%d type [%02d] packet%s", count[i], i, count[i]==1 ? "s" : "" ) );
                sink.streamComment( sc );
                count[i]=0;
            }
        }
        sink.packetDescriptor(pd);
    }

    @Override
    public void packet(PacketDescriptor pd, ByteBuffer data) throws StreamException {
        count[pd.getPacketId()]++; 
    }

    @Override
    public void streamClosed(StreamDescriptor sd) throws StreamException {
        for ( int i=0; i<100; i++ ) {
            if ( count[i]>0 ) {
                StreamComment sc= new StreamComment("log:INFO", String.format( "%d type [%02d] packet%s", count[i], i, count[i]==1 ? "s" : "" ) );
                sink.streamComment( sc );
                count[i]=0;
            }
        }
        sink.streamClosed(sd);
    }

    
    @Override
    public void streamException(StreamException se) throws StreamException {
        sink.streamException(se);
    }

    @Override
    public void streamComment(StreamComment sd) throws StreamException {
        sink.streamComment(sd);
    }
    
    public static void main( String[] args ) throws StreamException, MalformedURLException, IOException, ParseException {
        //File f = new File( "/home/jbf/ct/hudson/data/qds/proton_density.qds" );
        //File f = new File( "/home/cwp/tmp/juno_test/missing_freq.qds" );
        //InputStream in = new FileInputStream(f);
        
        InputStream in= System.in;
        
        FormatStreamHandler fsh= new FormatStreamHandler();
        fsh.setOutputStream( System.out );

        StripDescriptors filter= new StripDescriptors();
        filter.sink= fsh;

        StreamTool.readStream( Channels.newChannel(in), filter );

    }    
}
