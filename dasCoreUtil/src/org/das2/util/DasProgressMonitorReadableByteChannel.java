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
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.text.DecimalFormat;
import org.das2.util.monitor.ProgressMonitor;

/**
 *
 * @author  Edward West
 */
public class DasProgressMonitorReadableByteChannel implements ReadableByteChannel {

    private ProgressMonitor monitor;
    private boolean started = false;
    private int bytesRead = 0;
    long birthTimeMilli;
    long deathTimeMilli;
    DecimalFormat transferRateFormat;
    boolean enableProgressPosition = true;
    private long streamLength = 1000000; // this is usually close because of server side averaging.
    private long taskSize = streamLength / 1000;
    // we're wrapping this channel
    ReadableByteChannel in;

    /** Creates a new instance of DasProgressMonitorInputStream */
    public DasProgressMonitorReadableByteChannel(ReadableByteChannel in, ProgressMonitor monitor) {
        this.monitor = monitor;
        this.birthTimeMilli = System.currentTimeMillis();
        this.deathTimeMilli = -1;
        this.in = in;
    }

    private void reportTransmitSpeed() {
        if (transferRateFormat == null) {
            transferRateFormat = new DecimalFormat();
            transferRateFormat.setMaximumFractionDigits(2);
            transferRateFormat.setMinimumFractionDigits(2);
        }
        monitor.setProgressMessage("(" + transferRateFormat.format(calcTransmitSpeed() / 1024) + "kB/s)");
        if (enableProgressPosition) {
            monitor.setTaskProgress(bytesRead / 1000);
        }
    }

    private double calcTransmitSpeed() {
        // return speed in bytes/second.
        long totalBytesRead = bytesRead;
        long timeElapsed;
        if (deathTimeMilli > -1) {
            timeElapsed = deathTimeMilli - birthTimeMilli;
        } else {
            timeElapsed = System.currentTimeMillis() - birthTimeMilli;
        }
        if (timeElapsed == 0) {
            return Double.POSITIVE_INFINITY;
        }
        return 1000. * totalBytesRead / timeElapsed;
    }

    private void checkCancelled() throws IOException {
        if (monitor != null && monitor.isCancelled()) {
            close();
            throw new InterruptedIOException("Operation cancelled");
        }
    }

    public void close() throws IOException {
        in.close();
        deathTimeMilli = System.currentTimeMillis();
        if (monitor != null) {
            monitor.finished();
            started = false;
        }
    }

    /**
     * disable/enable setting of progress position, true by default.  Transfer 
     * rate will still be reported. This is introduced in case another agent 
     * (the das2Stream reader, in particular) can set the progress position 
     * more accurately.
     */
    public void setEnableProgressPosition(boolean value) {
        this.enableProgressPosition = value;
    }
    /**
     * Utility field used by bound properties.
     */
    private java.beans.PropertyChangeSupport propertyChangeSupport = new java.beans.PropertyChangeSupport(this);

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
     * Getter for property taskSize.
     * @return Value of property taskSize.
     */
    public long getStreamLength() {
        return this.streamLength;
    }

    /**
     * Setter for property taskSize.
     * @param taskSize New value of property taskSize.
     */
    public void setStreamLength(long taskSize) {
        long oldTaskSize = this.streamLength;
        this.streamLength = taskSize;
        this.taskSize = taskSize == -1 ? taskSize : streamLength / 1000;
        propertyChangeSupport.firePropertyChange("streamLength", oldTaskSize, taskSize);
    }

    public int read(ByteBuffer dst) throws IOException {
        int result = in.read(dst);

        if (!started) {
            started = true;
            monitor.setTaskSize(taskSize);
            monitor.started();
        }
        if (bytesRead == -1) {
            monitor.finished();
        } else {
            bytesRead += result;
            checkCancelled();
            reportTransmitSpeed();
        }

        return result;
    }

    public boolean isOpen() {
        return in.isOpen();
    }
}
