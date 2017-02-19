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
 * NullProgressMonitor.java
 *
 * Created on October 23, 2007, 10:11 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.das2.util.monitor;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.util.LoggerManager;

/**
 * This is a progress monitor to use when we don't care about the progress,
 * this doesn't provide a view of the progress to the client.
 *
 * Further, this can act as a base class for other monitor types.
 *
 * @author jbf
 */
public class AbstractProgressMonitor implements ProgressMonitor {
    
    private static final Logger logger = LoggerManager.getLogger("das2.system");
    
    public AbstractProgressMonitor() {
    }
    
    private long taskSize=-1 ;
    
    private int cancelCheck= 0;
    
    @Override
    public void setTaskSize(long taskSize) {
        if ( taskSize<1 ) {
            logger.log(Level.FINER, "taskSize set to {0}, resetting", taskSize);
            this.taskSize= -1;
        } else {
            this.taskSize= taskSize;
        }
    }
    
    @Override
    public long getTaskSize( ) { 
        return taskSize; 
    }

    private String progressMessage;

    @Override
    public void setProgressMessage( String message ) {
        this.progressMessage= message;
    }

    /**
     * provide access to the last progress message setting.
     * @return the last progress message setting.
     */
    public String getProgressMessage() {
        return this.progressMessage;
    }
        
    private long position=0;
    
    @Override
    public void setTaskProgress(long position) throws IllegalArgumentException {
        this.position= position;
    }
        
    @Override
    public long getTaskProgress() { 
        return position; 
    }
    
    private boolean started= false;
    
    @Override
    public void started() {  
        this.started= false;
    }
    
    @Override
    public boolean isStarted() {
        return started;
    }
    
    private boolean finished= false;
    
    @Override
    public void finished() {
        if ( finished ) {
            logger.warning("AbstractProgressMonitor.finished called twice, which could cause problems in the future");
        } else {
            logger.fine("enter monitor finished");
        }
        finished= true;
    }
    
    @Override
    public boolean isFinished() {
        return finished;
    }
    
    private boolean cancelled= false;
    
    @Override
    public void cancel() {
        cancelled= true;
    }
    
    @Override
    public boolean isCancelled() { 
        cancelCheck++;
        return cancelled; 
    }

    @Deprecated
    public void setAdditionalInfo(String s) { };
    
    private String label;
    
    @Override
    public void setLabel( String s ) { 
        this.label= s;
    }
    
    @Override
    public String getLabel() { 
        return label; 
    }
    
    /**
     * return a human-readable representation of the monitor, which is
     * currently position + "of" + taskSize.
     * @return return a human-readable representation of the monitor
     */
    @Override
    public String toString() {
        return "" + this.position + " of "+ this.taskSize;
    }

    @Override
    public ProgressMonitor getSubtaskMonitor(int start, int end, String label) {
        if ( label!=null ) setProgressMessage(label);
        return SubTaskMonitor.create( this, start, end );
    }

    @Override
    public ProgressMonitor getSubtaskMonitor(String label) {
        if ( label!=null ) setProgressMessage(label);
        return SubTaskMonitor.create( this, true );
    }

    @Override
    public boolean canBeCancelled() {
        return cancelCheck>0;
    }
}
