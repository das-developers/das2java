/*
 * Auralizor.java
 *
 * Created on April 2, 2004, 3:31 PM
 */

package org.das2.graph;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.WritableByteChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.dataset.VectorDataSet;
import org.das2.datum.Units;
import org.das2.system.DasLogger;
import javax.sound.sampled.*;
import org.das2.dataset.DataSetUtil;
import org.das2.datum.DatumRange;

/**
 *
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
    
    VectorDataSet ds;
    
    void setDataSet( VectorDataSet ds ) {
        this.ds= ds;
    }
    
    public void playSound() {
        float sampleRate=   (float) ( 1. / ds.getXTagDatum(1).subtract(ds.getXTagDatum(0)).doubleValue(Units.seconds)  ) ;
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

        while ( i<ds.getXLength() ) {
            double d= ds.getDouble(i++,yUnits);
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
                    Logger.getLogger(Auralizor.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(Auralizor.class.getName()).log(Level.SEVERE, null, ex);
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
    
    public Auralizor( VectorDataSet ds ) {
        min= -1;
        max= 1;
        DatumRange yrange= DataSetUtil.yRange(ds);
        yUnits= Units.dimensionless;
        min= yrange.min().doubleValue(yUnits);
        max= yrange.max().doubleValue(yUnits);
        this.ds= ds;
    }
    

    
}
