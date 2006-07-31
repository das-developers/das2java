/* File: DasProgressMonitorInputStream.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on September 23, 2003, 5:00 PM
 *      by Edward West <eew@space.physics.uiowa.edu>
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

package edu.uiowa.physics.pw.das.util;

import edu.uiowa.physics.pw.das.client.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.text.*;

/**
 *
 * @author  Edward West
 */
public class DasProgressMonitorInputStream extends java.io.FilterInputStream {
    
    private DasProgressMonitor monitor;
    private boolean started = false;
    private int bytesRead = 0;
    long birthTimeMilli;
    long deathTimeMilli;
    DecimalFormat transferRateFormat;
    boolean enableProgressPosition; 
    
    /** Creates a new instance of DasProgressMonitorInputStream */
    public DasProgressMonitorInputStream( InputStream in, DasProgressMonitor monitor ) {
        super(in);
        this.monitor = monitor;
        this.birthTimeMilli= System.currentTimeMillis();
        this.deathTimeMilli= -1;
        enableProgressPosition= true; // TODO: we don't know the size, but we use 1000 just because it's usually close.
        if ( monitor!=null ) {
            transferRateFormat= new DecimalFormat();
            transferRateFormat.setMaximumFractionDigits(2);
            transferRateFormat.setMinimumFractionDigits(2);
            if ( enableProgressPosition ) monitor.setTaskSize(1000);
        }
    }
    
    private void reportTransmitSpeed() {
        monitor.setAdditionalInfo("("+ transferRateFormat.format(calcTransmitSpeed()/1024) +"kB/s)");
        if ( enableProgressPosition ) monitor.setTaskProgress(bytesRead/1000);
    }
    
    
    private double calcTransmitSpeed() {
        // return speed in bytes/second.
        long totalBytesRead= bytesRead;
        long timeElapsed;
        if ( deathTimeMilli>-1 ) {
            timeElapsed= deathTimeMilli-birthTimeMilli;
        } else {
            timeElapsed= System.currentTimeMillis()-birthTimeMilli;
        }
        if ( timeElapsed==0 ) return Double.POSITIVE_INFINITY;
        return 1000 * totalBytesRead / timeElapsed;
    }
    
    public int read() throws IOException {
        checkCancelled();
        int result = super.read();
        if (monitor != null) {
            if (!started) {
                started = true;
                monitor.started();
            }
            if (bytesRead == -1) {
                monitor.finished();
            }
            else {
                bytesRead++;
                checkCancelled();
                reportTransmitSpeed();
            }
        }
        return result;
    }
    
    public int read(byte[] b) throws IOException {
        checkCancelled();
        int result = super.read(b);
        if (monitor != null) {
            if (!started) {
                started = true;
                monitor.started();
            }
            if (bytesRead == -1) {
                monitor.finished();
            }
            else {
                bytesRead += result;
                checkCancelled();
                reportTransmitSpeed();
            }
        }
        return result;
    }
    
    public int read(byte[] b, int off, int len) throws IOException {
        checkCancelled();
        int result = super.read(b, off, len);
        if (monitor != null) {
            if (!started) {
                started = true;
                monitor.started();
            }
            if (bytesRead == -1) {
                monitor.finished();
            }
            else {
                bytesRead += result;
                checkCancelled();
                reportTransmitSpeed();
            }
        }
        return result;
    }
    
    private void checkCancelled() throws IOException {
        if (monitor != null && monitor.isCancelled()) {
            close();
            throw new InterruptedIOException("Operation cancelled");
        }
    }
    
    public void close() throws IOException {
        super.close();
        deathTimeMilli= System.currentTimeMillis();
        if (monitor != null) {
            monitor.finished();
            started= false;
        }
    }
    
    /**
     * disable/enable setting of progress position, true by default.  Transfer 
     * rate will still be reported. This is introduced in case another agent 
     * (the das2Stream reader, in particular) can set the progress position 
     * more accurately.
     */
    public void setEnableProgressPosition( boolean value ) {
        this.enableProgressPosition= value;
    }
    
}
