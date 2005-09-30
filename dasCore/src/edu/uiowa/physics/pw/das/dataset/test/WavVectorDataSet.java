/*
 * AudioFileReader.java
 *
 * Created on April 15, 2004, 11:39 AM
 */

package edu.uiowa.physics.pw.das.dataset.test;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.datum.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel.MapMode;
import javax.sound.sampled.*;


/**
 *
 * @author  Jeremy
 */
public abstract class WavVectorDataSet implements VectorDataSet {
    
    AudioFormat audioFormat;
    int samples;
    ByteBuffer byteBuffer;
    ShortBuffer shortBuffer;
    
    int frameCount;
    int channelCount;
    int channel;
    float sampleRate;
    int frameSize;
    boolean unsigned;
    
    // note that the dataBuf should have the position at the beginning of the wav data, after the header.
    //  The limit marks the end of the data set.
    private WavVectorDataSet( ByteBuffer dataBuf, AudioFormat format ) throws IOException {
        this.audioFormat= format;
        this.byteBuffer= dataBuf;
        
        channelCount= audioFormat.getChannels();
        channel=0;
        
        sampleRate= audioFormat.getSampleRate();
        frameSize= audioFormat.getFrameSize();
        unsigned= audioFormat.getEncoding().equals(AudioFormat.Encoding.PCM_UNSIGNED );
        
        frameCount= ( byteBuffer.limit()-byteBuffer.position() ) / frameSize;
        
        if ( audioFormat.getSampleSizeInBits()==16 ) {
            if ( audioFormat.isBigEndian() ) {
                byteBuffer.order( ByteOrder.BIG_ENDIAN );
            } else {
                byteBuffer.order( ByteOrder.LITTLE_ENDIAN );
            }
            shortBuffer= byteBuffer.asShortBuffer();
        }
        
    }
    
    abstract int indexOf( int frame );
    
    static class Wav8bitMonoSigned extends WavVectorDataSet {
        Wav8bitMonoSigned( ByteBuffer dataBuf, AudioFormat format ) throws IOException {
            super( dataBuf, format );
        }
        int indexOf( int frame ) { return frame * channelCount + channel; }
        public double getDouble( int i, Units units ) { return (double)( byteBuffer.get(indexOf(i)) ) / 256 + 0.5; }
    }
    
    static class Wav16bitMonoUnsigned extends WavVectorDataSet {
        Wav16bitMonoUnsigned( ByteBuffer dataBuf, AudioFormat format ) throws IOException {
            super( dataBuf, format );
        }
        int indexOf( int frame ) { return frame * channelCount + channel; }
        public double getDouble( int i, Units units ) { return (double)( shortBuffer.get(indexOf(i)) ) / 65536; }
    }
    
    static class Wav8bitMonoUnsigned extends WavVectorDataSet {
        Wav8bitMonoUnsigned( ByteBuffer dataBuf, AudioFormat format ) throws IOException {
            super( dataBuf, format );
        }
        int indexOf( int frame ) { return frame * channelCount + channel; }
        public double getDouble( int i, Units units ) { return (double)( 0x00ff & byteBuffer.get(indexOf(i)) ) / 256; }
    }
    
    static class Wav16bitMonoSigned extends WavVectorDataSet {
        Wav16bitMonoSigned( ByteBuffer dataBuf, AudioFormat format ) throws IOException {
            super( dataBuf, format );
        }
        int indexOf( int frame ) { return frame * channelCount + channel; }
        public double getDouble( int i, Units units ) { return (double)( shortBuffer.get(indexOf(i) )) / 65536 + 0.5; }
        
    }
    
    public DataSet getPlanarView(String planeID) {
        int ch= Integer.parseInt(planeID.substring(7));
        if ( ch==channel ) {
            return this;
        } else {
            try {
                WavVectorDataSet result= createWavVectorDataSet( this.byteBuffer, this.audioFormat );
                result.channel= ch;
                return result;
            } catch ( IOException e ) {
                throw new RuntimeException(e);
            }
        }
    }
    
    public String[] getPlaneIds() {
        String[] result= new String[ channelCount ];
        for ( int i=0; i<channelCount; i++ ) {
            result[i]= "channel"+i;
        }
        return result;
    }
    
    public static WavVectorDataSet createWavVectorDataSet( ByteBuffer buf, AudioFormat audioFormat ) throws IOException {
        int bits= audioFormat.getSampleSizeInBits();
        boolean unsigned= audioFormat.getEncoding().equals(AudioFormat.Encoding.PCM_UNSIGNED );
        if ( bits==16 ) {
            if ( unsigned ) {
                return new Wav16bitMonoUnsigned(buf, audioFormat);
            } else {
                return new Wav16bitMonoSigned(buf,audioFormat);
            }
        } else {
            if ( unsigned ) {
                return new Wav8bitMonoUnsigned(buf,audioFormat);
            } else {
                return new Wav8bitMonoSigned(buf,audioFormat);
            }
        }
    }
    
    public static WavVectorDataSet createFromFile( File wavFile ) throws FileNotFoundException, IOException {
        try {
            AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(wavFile);
            AudioFormat audioFormat= fileFormat.getFormat();
            FileInputStream fin= new FileInputStream( wavFile );
            ByteBuffer buf= fin.getChannel().map( MapMode.READ_ONLY, 64, wavFile.length()-64 );
            return createWavVectorDataSet( buf, audioFormat );
        } catch ( UnsupportedAudioFileException e ) {
            // it's supposed to be a wav file.
            throw new RuntimeException(e);
        }
    }
    
    public int getXLength() {
        return frameCount;
    }
    
    public double getXTagDouble(int i, Units units) {
        return ((double)i)/sampleRate;
    }
    
    public Units getXUnits() {
        return Units.seconds;
    }
    
    public Units getYUnits() {
        return Units.dimensionless;
    }
    
    java.util.Map properties= new java.util.HashMap();
    String[] planeIds= new String[0];
    
    public Datum getDatum(int i) {
        Units yUnits= getYUnits();
        return yUnits.createDatum( getDouble(i,yUnits) );
    }
    
    public int getInt(int i, Units units) {
        return (int) getDouble( i,units );
    }
    
    public java.util.Map getProperties() {
        return null;
    }
    
    public Object getProperty(String name) {
        return null;
    }
    
    public Datum getXTagDatum(int i) {
        Units xUnits= getXUnits();
        return xUnits.createDatum( getXTagDouble(i,xUnits) );
    }
    
    public int getXTagInt(int i, Units units) {
        return (int)getXTagDouble( i, units );
    }
    
}
