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
    
    /** Sets the maximum value for the task progress of this
     * <code>ProgressMonitor</code>.
     * @param taskSize maximum value for the task progress.
     */
    void setTaskSize(int taskSize);    
    
    /** Notifies the ProgressMonitor of a change in the progress
     * of the task.
     * @param position the current task position
     * @throws IllegalArgumentException if {@link isCancelled()} returns true or,
     * possibly if started() has not been called or
     * finished() has been called.
     */    
    void setTaskProgress(int position) throws IllegalArgumentException;

    /** Returns the current progress of the monitored task.
     * @return the current progress of the monitored task.
     */    
    int getTaskProgress();

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
    
}
