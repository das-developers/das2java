/* File: ProgressMonitor.java
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

package org.das2.util.monitor;

/** <code>ProgressMonitor</code> defines a set of methods that are useful for
 * keeping track of the progress of an operation.  This interface also allows
 * the operation being tracked to be notified if the user wishes to cancel the
 * operation.  Code using this interface to track progress should call
 * {@link #isCancelled()} prior to calling {@link #setTaskProgress(long)}.
 * Implementations of this interface should throw an
 * <code>IllegalArgumentException</code> when <code>setTaskProgress(int)</code>
 * is called after the operation has been cancelled.
 * <p>
 * Code using the <code>ProgressMonitor</code> should call {@link #started()}
 * before <code>setTaskProgress(long)</code> is called for the first time.
 * <code>setTaskProgress()</code> should not be called after
 * <code>cancel()</code> or <code>finished()</code> has been called.  Therefore,
 * monitored processes should check isCancelled() before setTaskProgress(long)
 * is called.  An
 * implementation may throw an <code>IllegalArgumentException</code> if
 * <code>setTaskProgress(int)</code> is called before <code>started()</code> or
 * after <code>finished()</code> is called.  Note if isCancelled is not called
 * by the process, then the cancel button will become disabled.
 * 
 * <p>A client codes receiving a monitor must do one of two things.
 * It should either call setTaskSize(long), started(), setTaskProgress(long) zero or more times, then
 * finished(); or it should do nothing with the monitor, possibly passing the
 * monitor to a subprocess.  This is to ensure that it's easy to see that
 * the monitor lifecycle is properly performed. </p>
 *
 * @author  jbf
 */
public interface ProgressMonitor {

    public final static long SIZE_INDETERMINATE= -1;
    
    /** Sets the maximum value for the task progress of this
     * <code>ProgressMonitor</code>.
     * @param taskSize maximum value for the task progress.  A taskSize of -1 indicates the taskSize is indeterminate.
     */
    void setTaskSize(long taskSize);
    
    /** Notifies the ProgressMonitor of a change in the progress
     * of the task.  
     * @param position the current task position
     * @throws IllegalArgumentException if {@link #isCancelled()} returns true or,
     * possibly if started() has not been called or
     * finished() has been called.
     */
    void setTaskProgress(long position) throws IllegalArgumentException;
    
    /**
     * Provides additional feedback as to what's going on in the process. 
     * This message should be set by the service provider, not the client,
     * and refer to the implementation of the task.  e.g. "Reading file myData.dat"
     * @param message the message describing the state of progress.
     */
    void setProgressMessage( String message );
    
    /**
     * Returns the current progress of the monitored task.
     * @return the current progress of the monitored task.
     */
    long getTaskProgress();
    
    /**
     * Set a concise string that describes the task being performed.  Monitors
     * don't necessarily need to display this label, and this request may be
     * ignored.  It is only provided so a process can describe the task that
     * is going on.  This is usually set by the client of the process to indicate
     * what service we are waiting for.  e.g. "Loading Data"
     * @param label the label describing the task.
     */
    public void setLabel( String label );
    
    /**
     * Return the label string displayed, which is a concise string that 
     * describes the task being performed.  This is primarily to aid in debugging,
     * and this method need not return the string set by setLabel.
     * @return the label.
     */
    public String getLabel();
    
    /**
     * Return the size of the task.  The units are arbitrary
     * @return the size of the task.
     */
    long getTaskSize();
    
    /** Notifies the <code>ProgressMonitor</code> that the task
     * being monitored has started.  If the <code>ProgressMonitor</code>
     * is in a cancelled state when this method is called, that <code>
     * ProgressMonitor</code> should be 'uncancelled'.
     */
    void started();
    
    /** Notifies the <code>ProgressMonitor</code> that the task
     * being monitored has finished.  This must only be called once, and note that isFinished() must return
     * the state of this monitor.
     */
    void finished();
    
    /** 
     * Notifies the <code>ProgressMonitor</code> that the task
     * being monitored should be canceled.  After this method is
     * called, implementations should return <code>true</code> on
     * any subsequent calls to {@link #isCancelled()} and should
     * throw an IllegalStateException on any subsequent calls to
     * {@link #setTaskProgress(long)}.
     */
    void cancel();
    
    /** 
     * Returns <code>true</code> if the operation being tracked
     * should be cancelled.  For example, the human operator has pressed
     * the cancel button indicating that the process should be stopped.  Note
     * that if the process is not checking the cancel status, the cancel button
     * should be disabled.
     * 
     * @return <code>true</code> if the operation being tracked
     * should be cancelled.
     */
    boolean isCancelled();
    
    /**
     * return true if the process appears to support cancel.  Many
     * processes use a monitor to provide status feedback, but do not check
     * if the human operator has pressed cancel.
     * @return 
     */
    boolean canBeCancelled();
    
    /** additional information to be displayed alongside the progress.  That
     * might be of interest.
     * "85 of 100 (50KB/s)"
     * @param s the message, such as (50KB/s)
     * @deprecated setProgressMessage should be used by the service provider 
     *    to indicate how the process is being implemented.
     */
    public void setAdditionalInfo(String s);

    /** 
     * true if the process has indicated that it has started.
     * @return true if the process has indicated that it has started.
     */
    boolean isStarted();
    
    /**
     * true if the process has indicated that it is finished
     * @return true if the process has indicated that it is finished
     */
    boolean isFinished();
    
    /**
     * return a monitor to use for a subtask.  This is provided mostly as
     * a convenience.  setTaskProgress calls to the subtask monitor are mapped to
     * this monitor.  A label can also be specified for the subtask to improve
     * user experience.
     * 
     * If the parent process is not supporting cancel, then subprocesses cannot support cancel.
     *
     * @param start start position on this monitor.
     * @param end end position on this monitor (exclusive).
     * @param label a label for the subtask, often this is handled as progress message; or null.
     * @return a new progress monitor.  (generally type SubTaskMonitor)
     */
    public ProgressMonitor getSubtaskMonitor( int start, int end, String label);
    
    /**
     * get the subtask monitor when the current task length is indeterminate.  This avoids clients having to
     * put in dummy numbers that will cause problems in the future.
     * 
     * The subtask can set the taskSize and taskProgress and these should be conveyed to the parent.
     * 
     * @param label a label for the subtask, often this is handled as progress message; or null.
     * @return a new progress monitor.  (generally type SubTaskMonitor)
     */
    public ProgressMonitor getSubtaskMonitor( String label );
}
