/*
 * Auralizor.java
 *
 * Created on April 2, 2004, 3:31 PM
 */

package org.das2.graph;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.WritableByteChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.datum.Units;
import org.das2.system.DasLogger;
import javax.sound.sampled.*;
import org.das2.datum.UnitsConverter;
import org.virbo.dataset.FlattenWaveformDataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SemanticOps;
import org.virbo.dsops.Ops;

/**
 * Stream QDataSet to the sound system, using DEPEND_0 to control
 * sampling rate.  Note this does not support the new waveform packet 
 * scheme introduced a few years ago, and can easily be made to support 
 * it.
 * @author  Owner
 */
public class Auralizor {
    
    private static final int	EXTERNAL_BUFFER_SIZE = 100000;
    ByteBuffer buffer;
    byte[] buf;
    SourceDataLine line = null;
    int bufferInputIndex;
    double min;
    double max;
    Units yUnits;
    
    QDataSet ds;
    
    /**
     * set the dataset to stream.  The dataset should be 
     * rank 1 or a rank 2 waveform, and have DEPEND_0 which is convertible
     * to seconds or be a time location unit.
     * @param ds the rank 1 dataset with DEPEND_0 convertible to seconds or be a time location unit, or rank 2 waveform.
     */
    public final void setDataSet( QDataSet ds ) {        
        if ( ds.rank()==2 ) {
            this.ds= new FlattenWaveformDataSet(ds);
        } else if ( ds.rank()==1 ) {
            this.ds= ds;
        } else {
            throw new IllegalArgumentException("dataset must be rank 1 or rank 2 waveform");
        }
        min= -1;
        max= 1;
        QDataSet yrange= Ops.extent(this.ds);
        min= yrange.value(0);
        max= yrange.value(1);        
    }
    
    /**
     * begin streaming the sound.  This will block the current
     * thread until complete.
     */
    public void playSound() {
        QDataSet dep0= (QDataSet) ds.property(QDataSet.DEPEND_0);
        UnitsConverter uc= UnitsConverter.getConverter( SemanticOps.getUnits(dep0).getOffsetUnits(), Units.seconds );
        float sampleRate=   (float) ( 1. / uc.convert( dep0.value(1)-dep0.value(0) )  ) ;
        DasLogger.getLogger(DasLogger.GRAPHICS_LOG).fine("sampleRate= "+sampleRate);
        AudioFormat audioFormat= new AudioFormat( sampleRate, 16, 1, true, true );

        buf= new byte[EXTERNAL_BUFFER_SIZE];
        buffer= ByteBuffer.wrap(buf);

        buffer.order( ByteOrder.BIG_ENDIAN );
        
        DataLine.Info info = new DataLine.Info(SourceDataLine.class,  audioFormat);
        try {
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(audioFormat);
            line.addLineListener(getLineListener());
        }
        catch (Exception e) {
            throw new RuntimeException(e);            
        }
        
        bufferInputIndex= 0;
        line.start();
        
        int i=0;
        int ibuf=0;

        while ( i<dep0.length() ) {
            double d= ds.value(i++);
            int b= (int) ( 65536 * ( d - min ) / ( max-min ) ) - 32768;
            try {
                buffer.putShort( ibuf, (short)b );

            } catch ( IndexOutOfBoundsException ex ) {
                ex.printStackTrace();
            }
            ibuf+=2;
            if ( ibuf==EXTERNAL_BUFFER_SIZE ) {
                line.write(buf, 0, ibuf );
                try {
                    WritableByteChannel out = new FileOutputStream("/home/jbf/tmp/foo.bin").getChannel();
                    out.write(buffer);
                    buffer.flip();
                } catch (IOException ex) {
                    Logger.getLogger(Auralizor.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
                }
                ibuf=0;
            }
        }
        line.write(buf, 0, ibuf );
        try {
            WritableByteChannel out = new FileOutputStream("/home/jbf/tmp/foo.bin").getChannel();
            out.write(buffer);
            buffer.flip();
        } catch (IOException ex) {
            Logger.getLogger(Auralizor.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
        ibuf=0;
        
        line.drain();
        line.close();
        
    }
    
    LineListener getLineListener( ) {
        return new LineListener() {
            public void update( LineEvent e ) {
                if ( e.getType().equals( LineEvent.Type.CLOSE ) ) {
                    
                }
            }
        };
    }
    
    /**
     * create an Auralizor for rendering the dataset to the sound system.
     * @param ds rank 1 dataset with DEPEND_0 convertible to seconds.
     */
    public Auralizor( QDataSet ds ) {
        setDataSet(ds);
    }
    

    
}
