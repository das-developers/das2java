/* Copyright (C) 2003-2008 The University of Iowa 
 *
 * This file is part of the Das2 <www.das2.org> utilities library.
 *
 * Das2 utilities are free software: you can redistribute and/or modify them
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Das2 utilities are distributed in the hope that they will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * as well as the GNU General Public License along with Das2 utilities.  If
 * not, see <http://www.gnu.org/licenses/>.
 *
 * SubTaskMonitor.java
 *
 * Created on August 18, 2005, 4:01 PM
 */

package org.das2.util.monitor;


/**
 * creates a ProgressMonitor that maps its progress to a parent's progress.
 * For example, if a process takes a progress monitor, but is implemented in
 * two steps that each take a progress monitor, then two subtask monitors can
 * be created to monitor each step, and the client who passed in the monitor 
 * will see the whole deal as one process.
 *
 * @author Jeremy
 */
public class SubTaskMonitor implements ProgressMonitor {

    ProgressMonitor parent;
    long min, max, progress, size;
    String label;

    boolean cancelCheck;
    
    private SubTaskMonitor( ProgressMonitor parent, long min, long max, boolean cancelChecked ) {
        this.parent= parent;
        this.min= min;
        this.max= max;
        this.size= -1;
        this.cancelCheck= cancelChecked;
    }

    public static SubTaskMonitor create( ProgressMonitor parent, long min, long max ) {
        return new SubTaskMonitor( parent, min, max, false );
    }
    
    public static SubTaskMonitor create( ProgressMonitor parent, long min, long max, boolean cancelChecked ) {
        return new SubTaskMonitor( parent, min, max, cancelChecked );
    }

    public void cancel() {
        if ( parent.canBeCancelled() ) {
            parent.cancel();
        }
    }

    private boolean finished= false;

    public void finished() {
        this.finished= true; // only to support the bean property
    }

    public boolean isFinished() {
        return this.finished;
    }

    public long getTaskProgress() {
        return progress;
    }

    public boolean isCancelled() {
        return parent.isCancelled();
    }

    @Deprecated
    public void setAdditionalInfo(String s) {
        // ignore
    }

    public void setTaskProgress(long position) throws IllegalArgumentException {
        this.progress= position;
        if ( cancelCheck ) {
            parent.isCancelled(); // so there is one setTaskProgress for each isCancelled
        }
        if ( size==-1 ) {
            parent.setTaskProgress( min );
        } else {
            parent.setTaskProgress( min + ( max - min ) * position / size );
        }
    }

    public void setTaskSize(long taskSize) {
        this.size= taskSize;
    }

    public long getTaskSize() {
        return this.size;
    }

    boolean started= false;

    public void started() {
        this.started= true;
        if ( parent.isStarted()==false ) parent.started();
    }

    public boolean isStarted() {
        return started;
    }

    public void setLabel(String label) {
        this.label= label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        if (label == null) {
            return parent.toString();
        } else {
            return parent.toString() + ">" + label;
        }
    }

    /**
     * these messages are lost.
     * @param message 
     */
    public void setProgressMessage(String message) {
        //parent.setProgressMessage(message);
    }

    /**
     * @param start start index
     * @param end end index, exclusive
     * @param label label or null.
     * @return 
     */
    public ProgressMonitor getSubtaskMonitor(int start, int end, String label) {
        //setProgressMessage(label);
        return SubTaskMonitor.create( this, start, end, cancelCheck );
    }

    public boolean canBeCancelled() {
        return cancelCheck;
    }
    
}
