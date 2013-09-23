/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.util.monitor;

/**
 * Like the NullProgressMonitor, but print to stderr when the task is
 * taking a non-trivial amount of time.  The NullProgressMonitor is used
 * when the developer thinks that a task is trivial, so this should be used
 * to verify.
 * @author jbf
 */
public class AlertNullProgressMonitor extends NullProgressMonitor {
    long t0= System.currentTimeMillis();
    long lastAlert= 0;

    @Override
    public void setTaskProgress(long position) throws IllegalArgumentException {
        super.setTaskProgress(position); //To change body of generated methods, choose Tools | Templates.
        long t= System.currentTimeMillis();
        if ( ( t-t0 > 1000 ) && ( t-lastAlert > 250 ) ) {
            //TODO: on 20130923, this was failing to output on either the console or the Netbeans stderr console.
            System.err.print( String.format( "%s: %d of %d...",this.getLabel(),this.getTaskProgress(),this.getTaskSize()) );
            lastAlert= t;
        }
    }
    
    
}
