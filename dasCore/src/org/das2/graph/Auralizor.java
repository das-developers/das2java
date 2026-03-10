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
 * sampling rate.  
 * @author  Owner
 */
public final class Auralizor {
    
    private static final Logger logger= LoggerManager.getLogger("das2.graph.auralizor");
    
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
    
    boolean hasListeners= false;
    
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
    
    public static final String PROP_POSITION = "position";

    public Datum getPosition() {
        return DataSetUtil.asDatum( dep0.slice(currentRecord) );
    }

    public void setPosition(Datum position) {
        if ( position!=this.position ) {
            this.position= position;
            if ( lastAnnouncedPosition==null || lastAnnouncedPosition.subtract(position).abs().gt(reportPeriod) ) {
                this.currentRecord= DataSetUtil.closestIndex( dep0, position );
                Datum newPosition= getPosition();
                pcs.firePropertyChange( PROP_POSITION, lastAnnouncedPosition, newPosition );
                lastAnnouncedPosition= newPosition;
            }
        }
    }
    
    private boolean scale = true;

    public static final String PROP_SCALE = "scale";

    public boolean isScale() {
        return scale;
    }

    public void setScale(boolean scale) {
        boolean oldScale = this.scale;
        this.scale = scale;
        pcs.firePropertyChange(PROP_SCALE, oldScale, scale);
    }

    private double timeScale = 1.0;

    public static final String PROP_TIMESCALE = "timeScale";

    public double getTimeScale() {
        return timeScale;
    }

    /**
     * Speed or slow the sound by this much, greater than one speeds up, less than one slows down.
     * @param timeScale 
     */
    public void setTimeScale(double timeScale) {
        double oldTimeScale = this.timeScale;
        this.timeScale = timeScale;
        pcs.firePropertyChange(PROP_TIMESCALE, oldTimeScale, timeScale);
    }

    private Datum reportPeriod = Datum.create(100, Units.milliseconds);

    public static final String PROP_REPORT_PERIOD = "reportPeriod";

    public Datum getReportPeriod() {
        return reportPeriod;
    }

    /**
     * frequency reports are issued.  Note this may be ignored.
     * @param reportPeriod 
     */
    public void setReportPeriod(Datum reportPeriod) {
        Datum oldReportPeriod = this.reportPeriod;
        this.reportPeriod = reportPeriod;
        pcs.firePropertyChange(PROP_REPORT_PERIOD, oldReportPeriod, reportPeriod);
    }


    public void addPropertyChangeListener(PropertyChangeListener listener) {
        hasListeners= true;
        pcs.addPropertyChangeListener(listener);
    }
        
    public void addPropertyChangeListener(String propname,PropertyChangeListener listener) {
        hasListeners= true;
        pcs.addPropertyChangeListener(propname,listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }
        
    public void removePropertyChangeListener(String propname,PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(propname,listener);
    }    
    
    /**
     * begin streaming the sound.  This will start the process on a separate thread and
     * not block the current thread.
     * @see #playSound() 
     */
    public void start() {
        Runnable playSoundRunnable= () -> {
            playSound();
        };
        new Thread( playSoundRunnable ).start();
    }
    
    /**
     * begin streaming the sound.  This will block the current
     * thread until complete.
     * @see #start()
     */
    public void playSound() {
        playing= true;
        
        UnitsConverter uc= UnitsConverter.getConverter( SemanticOps.getUnits(dep0).getOffsetUnits(), Units.seconds );
        float sampleRate=   (float) ( 1. / uc.convert( dep0.value(1)-dep0.value(0) ) * timeScale );
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
        
        int ibuf=0;
        logger.fine("feeding audiosystem...");
        
        while ( currentRecord<dep0.length() ) {
            if ( playing==false ) {
                break;
            }
            
            logger.finest("   feeding audiosystem...");
            
            double d= ds.value(currentRecord);
            int b= scale ? ( (int) ( 65536 * ( d - min ) / ( max-min ) ) - 32768 ) : (int)d ;
        
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
            currentRecord++;
            
            if ( hasListeners ) {
                if ( currentRecord<dep0.length() ) {
                    setPosition(getPosition());
                }
            }
            
        }
        logger.fine("done feeding audiosystem");
        
        line.write(buf, 0, ibuf );
        
        line.drain();
        line.close();
        
    }
    
    private LineListener getLineListener( ) {
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
