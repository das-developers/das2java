/* File: MeteredInputStream.java
 * Copyright (C) 2002-2003 The University of Iowa
 * Created by: Jeremy Faden <jbf@space.physics.uiowa.edu>
 *             Jessica Swanner <jessica@space.physics.uiowa.edu>
 *             Edward E. West <eew@space.physics.uiowa.edu>
 *
 * This file is part of the das2 library.
 *
 * das2 is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.uiowa.physics.pw.das.client;

import java.io.IOException;
import java.io.InputStream;

/** MeteredInputStream monitors the rate at which data is read from the
 * stream, and can also limit the maximum data rate.  (Limit is useful
 * for simulating slow links.)
 * @author jbf
 */
public class MeteredInputStream extends InputStream {
    
    InputStream in;
    int totalBytesRead;
    long birthTimeMilli;
    long deathTimeMilli;
    double speedLimit;
    
    /** Creates a new instance of MeteredInputStream */
    public MeteredInputStream(InputStream in) {
        this.speedLimit=0;
        this.deathTimeMilli=0; // -1 indicates metering is in progress.
        setInputStream(in);
    }
    
    public void reset() {
        this.totalBytesRead= 0;
        this.birthTimeMilli= System.currentTimeMillis();
        this.deathTimeMilli= -1;
    }
    
    public void stop() {
        this.deathTimeMilli= System.currentTimeMillis();
    }
    
    public void setInputStream( InputStream in ) {
        if ( this.in!=null ) {
            throw new IllegalArgumentException( "already metering another inputStream." );
        }
        this.in= in;
        reset();
    }
    
    public int read( byte[] b, int off, int len ) throws IOException {
        try {
            int bytesRead= in.read(b,off,len);
            if ( deathTimeMilli==-1 ) totalBytesRead+= bytesRead;
            if ( speedLimit>0 ) {
                try {
                    while ( calcTransmitSpeed() > speedLimit ) {
                        Thread.sleep(50);
                    }
                } catch ( InterruptedException ex ) { }
            }
            return bytesRead;
        } catch ( IOException e ) {
            this.in= null;
            throw e;
        }
    }
    
    public int read() throws IOException {
        try {
            int byteRead= in.read();
            totalBytesRead++;
            return byteRead;
        } catch ( IOException e ) {
            this.in= null;
            throw e;
        }
    }
    
    public void close() throws IOException {
        deathTimeMilli= System.currentTimeMillis();
        InputStream in= this.in;
        this.in= null;
        in.close();

    }
    
    /**
     * @return the transmit speed in bytes/second.
     */
    public double calcTransmitSpeed() {
        long timeElapsed;
        if ( deathTimeMilli>-1 ) {
            timeElapsed= deathTimeMilli-birthTimeMilli;
        } else {
            timeElapsed= System.currentTimeMillis()-birthTimeMilli;
        }
        if ( timeElapsed==0 ) return Double.POSITIVE_INFINITY;
        return 1000. * totalBytesRead / timeElapsed;
    }
    
    public double calcTransmitTime() {
        long timeElapsed;
        if ( deathTimeMilli>-1 ) {
            timeElapsed= deathTimeMilli-birthTimeMilli;
        } else {
            timeElapsed= System.currentTimeMillis()-birthTimeMilli;
        }
        return timeElapsed/1000.;
    }
    
    public long totalBytesTransmitted() {
        return totalBytesRead;
    }
    
    public double getSpeedLimit() {
        return this.speedLimit;
    }
    
    public void setSpeedLimit(double speedLimit) {
        this.speedLimit = speedLimit;
    }
    
}
