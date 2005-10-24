/*
 * InputStreamMeter.java
 *
 * Created on October 21, 2005, 5:35 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package edu.uiowa.physics.pw.das.client;

import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.datum.Units;
import edu.uiowa.physics.pw.das.system.DasLogger;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author Jeremy
 */
public class InputStreamMeter {
    
    long totalBytesRead;
    long millisElapsed;
    double speedLimit;
    long meterCount;  // only clock while there are > 0 inputStreams out there
    long startTime;
    
    /** Creates a new instance of InputStreamMeter */
    public InputStreamMeter() {
        this.speedLimit= 0;
        this.totalBytesRead= 0;
        this.millisElapsed= 0;
        this.startTime= -1;
    }
    
    /** MeteredInputStream is used to monitor the read.  It is required to notify
     * the InputStreamMeter of the number of bytesRead, and allow the InputStreamMeter
     * to govern download speed by inserting blocking sleeps.
     *
     * @author jbf
     */
    private class MeteredInputStream extends InputStream {
        
        InputStream in;
        InputStreamMeter meter;
        long lastTimeMilli;
        
        /** Creates a new instance of MeteredInputStream */
        public MeteredInputStream( InputStream in, InputStreamMeter meter ) {
            this.lastTimeMilli= System.currentTimeMillis();
            this.meter= meter;
            this.in= in;
        }
        
        public int read( byte[] b, int off, int len ) throws IOException {
            try {
                int bytesRead= in.read(b,off,len);                
                meter.addBytes(bytesRead,this);
                meter.governSpeed(this);
                long t= System.currentTimeMillis()-lastTimeMilli;
                if ( t>100 ) {
                    meter.addMillis(t, this);
                    lastTimeMilli += t;
                }
                return bytesRead;
            } catch ( IOException e ) {
                meter.exception(this);
                throw e;
            }
        }
        
        public int read() throws IOException {
            try {
                int byteRead= in.read();
                meter.addBytes(1,this);
                meter.governSpeed(this);
                return byteRead;
            } catch ( IOException e ) {
                meter.exception(this);
                throw e;
            }
        }
        
        public void close() throws IOException {
            meter.addMillis( System.currentTimeMillis()-lastTimeMilli, this );
            meter.closing(this);
            in.close();
        }
        
    }
    
    
    public InputStream meterInputStream( InputStream in ) {
        meterCount++;
        if ( meterCount==1 ) {
            startTime= System.currentTimeMillis();
            totalBytesRead= 0;
            millisElapsed= 0;
        }
        return new MeteredInputStream( in, this );
    }
    
    /* limit total speed, possibly balancing load btw streams */
    final protected void governSpeed( MeteredInputStream mis ) {
        if ( speedLimit>0 ) {
            if ( calcTransmitSpeed() > speedLimit ) {
                long targetMillis= (long) ( ( totalBytesRead ) / ( speedLimit / 1000. ) );
                System.out.println( "target: " +  targetMillis + " have: " + calcMillisElapsed() );
                long waitMs= Math.min( 1000, targetMillis - calcMillisElapsed() );
                DasLogger.getLogger(DasLogger.DATA_TRANSFER_LOG).fine("limiting speed by waiting "+waitMs+" ms");
                try { Thread.sleep(waitMs); } catch ( InterruptedException ex ) { }
            }
        }
    }
    
    /* add these bytes to the meter, on behalf of mis. */
    final protected void addBytes( long bytes, MeteredInputStream mis ) {
        totalBytesRead+= bytes;
    }
    
    final protected void addMillis( long millis, MeteredInputStream mis ) {
    }
    
    final protected void closing( MeteredInputStream mis ) {
        meterCount--;
        if ( meterCount==0 ) {
            this.millisElapsed+= System.currentTimeMillis() - startTime;
            this.startTime=-1;
        }
    }
    
    final protected void exception( MeteredInputStream mis ) {
        meterCount--;
        if ( meterCount==0 ) {
            this.millisElapsed+= System.currentTimeMillis() - startTime;
            this.startTime=-1;
        }
    }
    
    private long calcMillisElapsed() {
        long millis= this.millisElapsed;
        if ( startTime!=-1 ) {
            millis+= System.currentTimeMillis() - startTime;
        }
        return millis;
    }
    
    private double calcTransmitSpeed() {
        long millis= calcMillisElapsed();
        if ( millis==0 ) {
            return Units.bytesPerSecond.getFillDouble();
        } else {
            return 1000 * totalBytesRead / millis;
        }
    }
    
    public Datum getTransmitSpeed() {
        return Units.bytesPerSecond.createDatum( calcTransmitSpeed(), 100 );
    }
    
    public long getBytesTransmitted() {
        return totalBytesRead;
    }
    
    public Datum getSpeedLimit() {
        return Units.bytesPerSecond.createDatum( this.speedLimit );
    }
    
    public void setSpeedLimit( Datum speedLimit) {
        this.speedLimit = speedLimit.doubleValue( Units.bytesPerSecond );
    }
}
