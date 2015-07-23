package org.das2.stream;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

/**
 * demo code that shows how a das2stream can be piped to an audiosystem.
 * @author jbf
 */
public class Sonifier implements StreamHandler {
    private static final int	EXTERNAL_BUFFER_SIZE = 128000;
    byte[] buffer;
    SourceDataLine line = null;
    int bufferInputIndex;        
    
    @Override
    public void packet(PacketDescriptor pd, org.das2.datum.Datum xTag, org.das2.datum.DatumVector[] vectors) throws StreamException {
        buffer[bufferInputIndex++]= (byte) ( 256 * vectors[0].doubleValue(0, vectors[0].getUnits() ) );  
        if ( bufferInputIndex==100 ) {
            line.write(buffer, 0, bufferInputIndex);
            bufferInputIndex=0;
        }
    }
    
    @Override
    public void packetDescriptor(PacketDescriptor pd) throws StreamException {        
    }
    
    @Override
    public void streamClosed(StreamDescriptor sd) throws StreamException {
        line.drain();
        line.close();        
    }
    
    @Override
    public void streamDescriptor(StreamDescriptor sd) throws StreamException {
        AudioFormat audioFormat= new AudioFormat( 8000, 8, 1, true, false );
        
        DataLine.Info	info = new DataLine.Info(SourceDataLine.class,  audioFormat);
        try {
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(audioFormat);
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        buffer = new byte[EXTERNAL_BUFFER_SIZE];
        bufferInputIndex= 0;        
        line.start();
    }
    
    @Override
    public void streamException(StreamException se) throws StreamException {
        se.printStackTrace();
        System.exit(0);
    }
    @Override
    public void streamComment( StreamComment sc ) throws StreamException {
        
    }
    
    public static void main( String[] args ) throws Exception {
        InputStream in;
        if ( args.length==0 ) {
            in= System.in;
        } else {
            in= new FileInputStream(args[0]);
        }
        ReadableByteChannel channel= Channels.newChannel(in);
        StreamTool.readStream(channel, new Sonifier() );
    }
}


