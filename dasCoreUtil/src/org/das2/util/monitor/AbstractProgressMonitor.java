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

/**
 * This is a progress monitor to use when we don't care about the progress,
 * this doesn't provide a view of the progress to the client.
 *
 * Further, this can act as a base class for other monitor types.
 *
 * @author jbf
 */
public class AbstractProgressMonitor implements ProgressMonitor {
    
    public AbstractProgressMonitor() {
    }
    
    private long taskSize=-1 ;
    
    private int cancelCheck= 0;
    
    public void setTaskSize(long taskSize) {
        this.taskSize= taskSize;
    }
    
    public long getTaskSize( ) { 
        return taskSize; 
    }

    private String progressMessage;

    public void setProgressMessage( String message ) {
        this.progressMessage= message;
    }

    public String getProgressMessage() {
        return this.progressMessage;
    }
        
    private long position=0;
    
    public void setTaskProgress(long position) throws IllegalArgumentException {
        this.position= position;
    }
        
    public long getTaskProgress() { 
        return position; 
    }
    
    private boolean started= false;
    
    public void started() {  
        this.started= false;
    }
    
    public boolean isStarted() {
        return started;
    }
    
    private boolean finished= false;
    
    public void finished() {
        finished= true;
    }
    
    public boolean isFinished() {
        return finished;
    }
    
    private boolean cancelled= false;
    
    public void cancel() {
        cancelled= true;
    }
    
    public boolean isCancelled() { 
        cancelCheck++;
        return cancelled; 
    }

    @Deprecated
    public void setAdditionalInfo(String s) { };
    
    private String label;
    
    public void setLabel( String s ) { 
        this.label= s;
    }
    
    public String getLabel() { 
        return label; 
    }
    
    @Override
    public String toString() {
        return "" + this.position + " of "+ this.taskSize;
    }

    /**
     * @param start start index
     * @param end end index, exclusive
     * @param label label or null.
     * @return 
     */    
    public ProgressMonitor getSubtaskMonitor(int start, int end, String label) {
        if ( label!=null ) setProgressMessage(label);
        return SubTaskMonitor.create( this, start, end );
    }

    public ProgressMonitor getSubtaskMonitor(String label) {
        if ( label!=null ) setProgressMessage(label);
        return SubTaskMonitor.create( this, true );
    }

    
    public boolean canBeCancelled() {
        return cancelCheck>0;
    }
}
