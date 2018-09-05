/*
 * Auralizor.java
 *
 * Created on April 2, 2004, 3:31 PM
 */

package org.das2.graph;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.datum.Units;
import javax.sound.sampled.*;
import javax.swing.JButton;
import javax.swing.JPanel;
import org.das2.datum.Datum;
import org.das2.datum.LoggerManager;
import org.das2.datum.UnitsConverter;
import org.das2.qds.DataSetUtil;
import org.das2.qds.FlattenWaveformDataSet;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;
import org.das2.qds.ops.Ops;
import org.das2.util.DebugPropertyChangeSupport;

/**
 * Stream QDataSet to the sound system, using DEPEND_0 to control
 * sampling rate.  Note this does not support the new waveform packet 
 * scheme introduced a few years ago, and can easily be made to support 
 * it.
 * @author  Owner
 */
public class Auralizor {
    
    private static final Logger logger= LoggerManager.getLogger("das2.graph");
    
    private static final int	EXTERNAL_BUFFER_SIZE = 100000;
    ByteBuffer buffer;
    byte[] buf;
    SourceDataLine line = null;
    double min;
    double max;
    
    QDataSet ds;
    QDataSet dep0;
    
    int currentRecord= 0;
    boolean playing= false;
    
    PropertyChangeSupport pcs= new DebugPropertyChangeSupport(this);
    
    /**
     * set the dataset to stream.  The dataset should be 
     * rank 1 or a rank 2 waveform, and have DEPEND_0 which is convertible
     * to seconds or be a time location unit.
     * @param ds the rank 1 dataset with DEPEND_0 convertible to seconds or be a time location unit, or rank 2 waveform.
     */
    public final void setDataSet( QDataSet ds ) {        
        switch (ds.rank()) {
            case 2:
                this.ds= new FlattenWaveformDataSet(ds);
                break;
            case 1:
                this.ds= ds;
                break;
            default:
                throw new IllegalArgumentException("dataset must be rank 1 or rank 2 waveform");
        }
        this.dep0= (QDataSet) ds.property(QDataSet.DEPEND_0);
        min= -1;
        max= 1;
        QDataSet yrange= Ops.extent(this.ds);
        min= yrange.value(0);
        max= yrange.value(1);        
        if ( min==max ) {
            max= Math.abs(min);
            if ( max==min ) max= max * 10;
            if ( max==0 ) max = max + 10;
        }
    }
    
    
    public JPanel getControlPanel() {
        AuralizorControlPanel acp= new AuralizorControlPanel();
        acp.setAuralizor(this);
        return acp;
    }
    
    public void reset() {
        currentRecord= 0;
        playing= false;
        setPosition(Units.seconds.createDatum(0));
    }
    
    private Datum position = Units.seconds.createDatum(0);
    private Datum lastAnnouncedPosition= null;
    private Datum limit= Units.milliseconds.createDatum(100);
    
    public static final String PROP_POSITION = "position";

    public Datum getPosition() {
        return DataSetUtil.asDatum( dep0.slice(currentRecord) );
    }

    public void setPosition(Datum position) {
        if ( position!=this.position ) {
            this.currentRecord= DataSetUtil.closestIndex( dep0, position );
            if ( lastAnnouncedPosition==null || lastAnnouncedPosition.subtract(position).abs().gt(limit) ) {
                Datum newPosition= getPosition();
                pcs.firePropertyChange( PROP_POSITION, lastAnnouncedPosition, newPosition );
                lastAnnouncedPosition= newPosition;
            }
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }
        
    public void addPropertyChangeListener(String propname,PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(propname,listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }
        
    public void removePropertyChangeListener(String propname,PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(propname,listener);
    }    
    
    /**
     * begin streaming the sound.  This will block the current
     * thread until complete.
     */
    public void playSound() {
        playing= true;
        
        QDataSet dep0= (QDataSet) ds.property(QDataSet.DEPEND_0);
        UnitsConverter uc= UnitsConverter.getConverter( SemanticOps.getUnits(dep0).getOffsetUnits(), Units.seconds );
        float sampleRate=   (float) ( 1. / uc.convert( dep0.value(1)-dep0.value(0) )  ) ;
        logger.log(Level.FINE, "sampleRate= {0}", sampleRate);
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
        catch (LineUnavailableException e) {
            throw new RuntimeException(e);            
        }
        
        line.start();
        
        int i=currentRecord;
        int ibuf=0;

        while ( currentRecord<dep0.length() ) {
            if ( playing==false ) {
                break;
            }
            i= currentRecord;
            
            double d= ds.value(i++);
            int b= (int) ( 65536 * ( d - min ) / ( max-min ) ) - 32768;
            try {
                buffer.putShort( ibuf, (short)b );

            } catch ( IndexOutOfBoundsException ex ) {
                logger.log( Level.WARNING, ex.getMessage(), ex );
            }
            ibuf+=2;
            if ( ibuf==EXTERNAL_BUFFER_SIZE ) {
                line.write(buf, 0, ibuf );
                ibuf=0;
            }
            currentRecord= i;
            setPosition(getPosition());
        }
        line.write(buf, 0, ibuf );
        
        line.drain();
        line.close();
        
    }
    
    LineListener getLineListener( ) {
        return new LineListener() {
            @Override
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
