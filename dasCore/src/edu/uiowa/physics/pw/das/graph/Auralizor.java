/*
 * Auralizor.java
 *
 * Created on April 2, 2004, 3:31 PM
 */

package edu.uiowa.physics.pw.das.graph;

import org.das2.dataset.VectorDataSet;
import org.das2.datum.Units;
import edu.uiowa.physics.pw.das.*;
import edu.uiowa.physics.pw.das.graph.*;
import org.das2.system.DasLogger;
import java.io.*;
import javax.sound.sampled.*;
import javax.swing.*;

/**
 *
 * @author  Owner
 */
public class Auralizor {
    
    private static final int	EXTERNAL_BUFFER_SIZE = 1000;
    byte[] buffer;
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
        AudioFormat audioFormat= new AudioFormat( sampleRate, 8, 1, true, false );
        
        DataLine.Info info = new DataLine.Info(SourceDataLine.class,  audioFormat);
        try {
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(audioFormat);
            line.addLineListener(getLineListener());
        }
        catch (Exception e) {
            throw new RuntimeException(e);            
        }
        buffer = new byte[EXTERNAL_BUFFER_SIZE];
        bufferInputIndex= 0;
        line.start();
        
        int i=0;
        int ibuf=0;
        while ( i<ds.getXLength() ) {
            double d= ds.getDouble(i++,yUnits);
            int b= (int) ( 256 * ( d - min ) / ( max-min ) );
            buffer[ibuf++]= (byte)b;
            if ( ibuf==EXTERNAL_BUFFER_SIZE ) {
                line.write(buffer, 0, ibuf );
                ibuf=0;                
            }
        }
        line.write(buffer, 0, ibuf );
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
        yUnits= Units.dimensionless;
        this.ds= ds;
    }
    

    
}
