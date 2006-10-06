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

package edu.uiowa.physics.pw.das.util;

/** <code>ProgressMonitor</code> defines a set of methods that are useful for
 * keeping track of the progress of an operation.  This interface also allows
 * the operation being tracked to be notified if the user wishes to cancel the
 * operation.  Code using this interface to track progress should call
 * {@link #isCancelled()} prior to calling {@link #setTaskProgress(int)}.
 * Implementations of this interface should throw an
 * <code>IllegalArgumentException</code> when <code>setTaskProgress(int)</code>
 * is called after the operation has been cancelled.
 * <p>
 * Code using the <code>ProgressMonitor</code> should call {@link started()}
 * before <code>setTaskProgress()</code> is called for the first time.
 * <code>setTaskProgress()</code> should not be called after
 * <code>cancel()</code> or <code>finisheded</code> has been called.  An
 * implementation may throw an <code>IllegalArgumentException</code> if
 * <code>setTaskProgress(int)</code> is called before <code>started()</code> or
 * after <code>finished()</code> is called.
 *
 * @author  jbf
 */
public interface DasProgressMonitor {
    
    /**
     * Use NULL when you do not need or wish to use a progressMonitor.  It simply
     * ignores the progress messages.
     */    
    public static final DasProgressMonitor NULL= new DasProgressMonitor() {
        public void setTaskSize(long taskSize) {} ;
        public long getTaskSize( ) { return 1; }
        public void setTaskProgress(long position) throws IllegalArgumentException {};
        public void setProgressMessage( String message ) {} ;
        public long getTaskProgress() { return 0; };
        public void started() {};
        public void finished() {};
        public void cancel() {};
        public boolean isCancelled() { return false; };        
        public void setAdditionalInfo(String s) { };
        public void setLabel( String s ) { };
        public String getLabel() { return ""; }
    };
    
    /** Sets the maximum value for the task progress of this
     * <code>ProgressMonitor</code>.
     * @param taskSize maximum value for the task progress.  A taskSize of -1 indicates the taskSize is indeterminate.
     */
    void setTaskSize(long taskSize);
    
    /** Notifies the ProgressMonitor of a change in the progress
     * of the task.
     * @param position the current task position
     * @throws IllegalArgumentException if {@link isCancelled()} returns true or,
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
    
    /** Returns the current progress of the monitored task.
     * @return the current progress of the monitored task.
     */
    long getTaskProgress();
    
    /**
     * Set a consise string that describes the task being performed.  Monitors
     * don't necessarily need to display this label, and this request may be
     * ignored.  It is only provided so a process can describe the task that
     * is going on.  This is usually set by the client of the process to indicate
     * what service we are waiting for.  e.g. "Loading Data"
     */
    public void setLabel( String label );
    
    /**
     * Return the label string displayed.  This is primarily to aid in debugging,
     * and this method need not return the string set by setLabel.
     */
    public String getLabel();
    
    long getTaskSize();
    
    /** Notifies the <code>ProgressMonitor</code> that the task
     * being monitored has started.  If the <code>ProgressMonitor</code>
     * is in a cancelled state when this method is called, that <code>
     * ProgressMonitor</code> should be 'uncancelled'.
     */
    void started();
    
    /** Notifies the <code>ProgressMonitor</code> that the task
     * being monitored has finished.
     */
    void finished();
    
    /** Notifies the <code>ProgressMonitor</code> that the task
     * being monitored should be canceled.  After this method is
     * called, implementations should return <code>true</code> on
     * any subsequent calls to {@link #isCancelled()} and should
     * throw an IllegalStateException on any subsequent calls to
     * {@link #setTaskProgress(int)}.
     */
    void cancel();
    
    /** Returns <code>true</code> if the operation being tracked
     * should be cancelled.
     * @return <code>true</code> if the operation being tracked
     * should be cancelled.
     */
    boolean isCancelled();
    
    /** additional information to be displayed alongside the progress.  That
     * might be of interest.
     * "85 of 100 (50KB/s)"
     */
    public void setAdditionalInfo(String s);
    
}
