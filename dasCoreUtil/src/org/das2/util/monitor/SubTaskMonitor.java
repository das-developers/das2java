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

import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.util.LoggerManager;


/**
 * creates a ProgressMonitor that maps its progress to a parent's progress.
 * For example, if a process takes a progress monitor, but is implemented in
 * two steps that each take a progress monitor, then two subtask monitors can
 * be created to monitor each step, and the client who passed in the monitor 
 * will see the whole deal as one process.
 *
 * Note we would often abuse monitors, handing them off to several processes
 * that would each take the monitor through its lifecycle.  Now we require the
 * lifecycle be performed once, so that machines can use the monitors to monitor
 * processes.  In this case, SubTaskMonitors should be used, and getSubtaskMonitor
 * was added.  Note too that this class should not be called directly, because
 * getSubtaskMonitor allows the source class to return a monitor of its own design.
 * 
 * @see ProgressMonitor#getSubtaskMonitor(int, int, java.lang.String) 
 * @see ProgressMonitor#getSubtaskMonitor(java.lang.String) 
 * @author Jeremy
 */
public class SubTaskMonitor implements ProgressMonitor {

    private static final Logger logger = LoggerManager.getLogger("das2.system");
    
    ProgressMonitor parent;
    long min, max, progress, size;
    String label;
    
    /**
     * echo progress messages up to parent to mimic old behavior.
     */
    boolean doEchoToParent= false;

    /**
     * true if we should check the parent cancel status.
     */
    boolean cancelCheck;
    
    /**
     * create the subtask monitor
     * @param parent parent monitor
     * @param min minium
     * @param max maximum
     * @param cancelChecked true if we should check the parent's cancel status
     */
    private SubTaskMonitor( ProgressMonitor parent, long min, long max, boolean cancelChecked ) {
        this.parent= parent;
        this.min= min;
        this.max= max;
        this.size= -1;
        this.cancelCheck= cancelChecked;
    }

    /**
     * create the subtask monitor.
     * @param parent parent monitor
     * @param min minium
     * @param max maximum
     * @return SubTaskMonitor
     */
    public static SubTaskMonitor create( ProgressMonitor parent, long min, long max ) {
        return new SubTaskMonitor( parent, min, max, false );
    }
    
    /**
     * create the subtask monitor.
     * @param parent parent monitor
     * @param min minium
     * @param max maximum
     * @param cancelChecked true if we should check the parent's cancel status
     * @return SubTaskMonitor
     */
    public static SubTaskMonitor create( ProgressMonitor parent, long min, long max, boolean cancelChecked ) {
        return new SubTaskMonitor( parent, min, max, cancelChecked );
    }

    /**
     * mode for when parent is indeterminate
     * @param parent
     * @param cancelChecked 
     * @return 
     */
    public static SubTaskMonitor create( ProgressMonitor parent, boolean cancelChecked ) {
        SubTaskMonitor result= new SubTaskMonitor( parent, -1, -1, cancelChecked );
        result.doEchoToParent= true;  // See sftp://klunk.physics.uiowa.edu:/home/jbf/project/autoplot/script/bugs/1251_subtask_monitor/demo2.jy
        return result;
    }

    @Override
    public void cancel() {
        if ( parent.canBeCancelled() ) {
            parent.cancel();
        }
    }

    private boolean finished= false;

    private StackTraceElement[] sts;
    
    @Override
    public void finished() {
        if ( finished ) {
            Thread.dumpStack(); // DEBUG IDL BRIDGE
            logger.warning("XXX SubTaskMonitor.finished called twice, which could cause problems in the future");
            logger.warning("Here is sts:");
            for ( StackTraceElement el: sts ) {
                logger.warning("xxx " + el.toString());
            }
        } else {
            logger.fine("enter monitor finished");
            sts= Thread.currentThread().getStackTrace();
        }
        this.finished= true; // only to support the bean property
    }

    @Override
    public boolean isFinished() {
        return this.finished;
    }

    @Override
    public long getTaskProgress() {
        return progress;
    }

    @Override
    public boolean isCancelled() {
        return parent.isCancelled();
    }

    @Deprecated
    @Override
    public void setAdditionalInfo(String s) {
        // ignore
    }

    @Override
    public void setTaskProgress(long position) throws IllegalArgumentException {
        this.progress= position;
        if ( cancelCheck ) {
            parent.isCancelled(); // so there is one setTaskProgress for each isCancelled
        }
        if ( max==min && min==-1 ) {
            // parent is indeterminate
        } else {
            if ( size==-1 ) {
                parent.setTaskProgress( min );
            } else {
                parent.setTaskProgress( min + ( max - min ) * position / size );
            }
        }
    }

    @Override
    public void setTaskSize(long taskSize) {
        if ( taskSize<1 ) {
            logger.log(Level.FINER, "taskSize set to {0}, resetting", taskSize);
            taskSize= -1;
        }
        this.size= taskSize;
        if ( max==min && min==-1 && doEchoToParent ) {
            min= 0;
            max= taskSize;
            parent.setTaskSize(taskSize);
        }
    }

    @Override
    public long getTaskSize() {
        return this.size;
    }

    boolean started= false;

    @Override
    public void started() {
        this.started= true;
        if ( parent.isStarted()==false ) parent.started();
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public void setLabel(String label) {
        this.label= label;
        if ( this.doEchoToParent ) {
            parent.setLabel(label);
        }
    }

    @Override
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
     * these messages are lost, unless doEchoToParent is set.
     * @param message 
     */
    @Override
    public void setProgressMessage(String message) {
        if( this.doEchoToParent ) {
            parent.setProgressMessage(message);
        }
        //parent.setProgressMessage(message);
    }

    @Override
    public ProgressMonitor getSubtaskMonitor(int start, int end, String label) {
        //setProgressMessage(label);
        if ( this.min==-1 && this.max==-1 ) {
            return SubTaskMonitor.create( this, cancelCheck );            
        } else {
            return SubTaskMonitor.create( this, start, end, cancelCheck );
        }
    }

    @Override
    public ProgressMonitor getSubtaskMonitor(String label) {
        return SubTaskMonitor.create( this, cancelCheck );
    }

    
    @Override
    public boolean canBeCancelled() {
        return cancelCheck;
    }
    
}
