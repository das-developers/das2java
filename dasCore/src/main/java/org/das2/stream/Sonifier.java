package org.das2.stream;

import org.das2.util.StreamTool;
import java.io.*;
import java.io.File;
import java.io.IOException;
import java.nio.channels.*;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class Sonifier implements StreamHandler {
    private static final int	EXTERNAL_BUFFER_SIZE = 128000;
    byte[] buffer;
    SourceDataLine line = null;
    int bufferInputIndex;        
    
    
    public void packet(PacketDescriptor pd, org.das2.datum.Datum xTag, org.das2.datum.DatumVector[] vectors) throws StreamException {
        double max= 1.0;
        buffer[bufferInputIndex++]= (byte) ( 256 * vectors[0].doubleValue(0, vectors[0].getUnits() ) );  
        if ( bufferInputIndex==100 ) {
            line.write(buffer, 0, bufferInputIndex);
            bufferInputIndex=0;
        }
    }
    
    public void packetDescriptor(PacketDescriptor pd) throws StreamException {        
    }
    
    public void streamClosed(StreamDescriptor sd) throws StreamException {
        line.drain();
        line.close();        
    }
    
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
    
    public void streamException(StreamException se) throws StreamException {
        se.printStackTrace();
        System.exit(0);
    }
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


