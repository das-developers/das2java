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

package org.das2.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.util.monitor.ProgressMonitor;

/**
 *
 * @author  Edward West
 */
public class DasProgressMonitorInputStream extends java.io.FilterInputStream {
    
    private static final Logger logger= LoggerManager.getLogger("das2.system.monitor");
    
    private final ProgressMonitor monitor;
    private boolean started = false;
    private int bytesRead = 0;
    private final long birthTimeMilli;
    private long deathTimeMilli;
    private DecimalFormat transferRateFormat;
    boolean enableProgressPosition= true; 

    private long streamLength= 1000000; // this is usually close because of server side averaging.
    private long taskSize= streamLength/1000;
    private final List<Runnable> runWhenClosedRunnables;
    
    /** Creates a new instance of DasProgressMonitorInputStream
     * @param in the InputStream to monitor as data comes in.
     * @param monitor the monitor to report feedback
     */
    public DasProgressMonitorInputStream( InputStream in, ProgressMonitor monitor ) {
        super(in);
        this.monitor = monitor;
        this.birthTimeMilli= System.currentTimeMillis();
        this.deathTimeMilli= -1;
        this.runWhenClosedRunnables= new ArrayList<>();
    }
    
    private void reportTransmitSpeed() {
        if (transferRateFormat==null ) {
            transferRateFormat= new DecimalFormat();
            transferRateFormat.setMaximumFractionDigits(2);
            transferRateFormat.setMinimumFractionDigits(2);
        }
        String s= transferRateFormat.format(calcTransmitSpeed()/1024);
        monitor.setProgressMessage("("+ s +"kB/s)");
        logger.log(Level.FINER, "transmit speed {0}", s );
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
        return 1000. * totalBytesRead / timeElapsed;
    }
    
    @Override
    public int read() throws IOException {
        checkCancelled();
        int result = super.read();
        if (monitor != null) {
            if (!started) {
                started = true;
                monitor.setTaskSize(taskSize);
                monitor.started();
            }
            if (bytesRead == -1) {
                if ( !monitor.isFinished() ) monitor.finished();
            }
            else {
                bytesRead++;
                checkCancelled();
                reportTransmitSpeed();
            }
        }
        return result;
    }
    
    @Override
    public int read(byte[] b) throws IOException {
        checkCancelled();
        int result = super.read(b);
        if (monitor != null) {
            if (!started) {
                started = true;
                monitor.setTaskSize(taskSize);
                monitor.started();
            }
            if (bytesRead == -1) {
                if ( !monitor.isFinished() ) monitor.finished();
            }
            else {
                bytesRead += result;
                checkCancelled();
                reportTransmitSpeed();
            }
        }
        return result;
    }
    
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        checkCancelled();
        int result = super.read(b, off, len);
        if (monitor != null) {
            if (!started) {
                started = true;
                monitor.setTaskSize( taskSize );
                monitor.started();
            }
            if (bytesRead == -1) {
                if ( !monitor.isFinished() ) {
                    monitor.finished();
                } else {
                    // Something fishy is happening.  See /home/jbf/project/autoplot/bugs/20141103
                }
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
    
    /**
     * should an action be needed when the transaction is complete, for example
     * closing a network connection, this can be used.
     * @param run 
     */
    public void addRunWhenClosedRunnable( Runnable run ) {
        runWhenClosedRunnables.add(run);
    }
    
    /**
     * close resources needed and set the monitor finished flag.  If
     * and runWhenClosedRunnables have been added, call them.
     * @throws IOException 
     */
    @Override
    public void close() throws IOException {
        logger.fine("close monitor");
        super.close();
        if ( deathTimeMilli>-1 ) {
            logger.fine("close called twice.");
        }
        boolean doRunWhenClosedRunnables= deathTimeMilli==-1;
        deathTimeMilli= System.currentTimeMillis();
        if (monitor != null) {
            if ( !monitor.isFinished() ) monitor.finished();  // it should be finished already from the read command.
            started= false;
        }
        if ( doRunWhenClosedRunnables ) {
            for ( Runnable run: runWhenClosedRunnables ) {
                try {
                    run.run();
                } catch ( RuntimeException ex ) {
                    logger.log(Level.WARNING,ex.getMessage(),ex);
                }
            }
        }
    }
    
    /**
     * disable/enable setting of progress position, true by default.  Transfer 
     * rate will still be reported. This is introduced in case another agent 
     * (the das2Stream reader, in particular) can set the progress position 
     * more accurately.
     * @param value
     */
    public void setEnableProgressPosition( boolean value ) {
        this.enableProgressPosition= value;
    }

    /**
     * Utility field used by bound properties.
     */
    private final java.beans.PropertyChangeSupport propertyChangeSupport =  new java.beans.PropertyChangeSupport(this);

    /**
     * Adds a PropertyChangeListener to the listener list.
     * @param l The listener to add.
     */
    public void addPropertyChangeListener(java.beans.PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(l);
    }

    /**
     * Removes a PropertyChangeListener from the listener list.
     * @param l The listener to remove.
     */
    public void removePropertyChangeListener(java.beans.PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener(l);
    }

    /**
     * the length of the stream in bytes.  Note often the length is not known,
     * and it is by default 1000000.
     * @return length of the stream in bytes, or 10000000.
     */
    public long getStreamLength() {
        return this.streamLength;
    }

    /**
     * set the length of the stream in bytes.
     * @param streamLength the length of the stream in bytes.
     */
    public void setStreamLength(long streamLength) {
        long oldTaskSize = this.streamLength;
        this.streamLength = streamLength;
        this.taskSize= streamLength < 1000 ? -1 : streamLength/1000;
        propertyChangeSupport.firePropertyChange ("streamLength", oldTaskSize, streamLength );
    }
    
}
