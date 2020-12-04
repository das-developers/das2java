
package org.das2.util.monitor;

/**
 * Like the NullProgressMonitor, but print to stderr when the task is
 * taking a non-trivial amount of time.  The NullProgressMonitor is used
 * when the developer thinks that a task is trivial, so this should be used
 * to verify.  After one second, this will start dumping messages to 
 * stderr.
 * @author jbf
 */
public class AlertNullProgressMonitor extends NullProgressMonitor {
    
    /**
     * the birth time for the monitor.
     */
    long t0= System.currentTimeMillis();
    
    /**
     * the time the lastAlert was issued.
     */
    long lastAlert= 0;

    /**
     * create a monitor.
     */
    public AlertNullProgressMonitor() {
    }
    
    /**
     * create a monitor with the label.
     * @param label the label
     */
    public AlertNullProgressMonitor(String label) {
        this();
        this.setLabel(label);
    }
    
    @Override
    public void setTaskProgress(long position) throws IllegalArgumentException {
        super.setTaskProgress(position); //To change body of generated methods, choose Tools | Templates.
        long t= System.currentTimeMillis();
        if ( ( t-t0 > 1000 ) && ( t-lastAlert > 500 ) ) {
            System.err.println( String.format( "%s: %d of %d... (trivial task is taking longer than expected)",this.getLabel(),this.getTaskProgress(),this.getTaskSize()) );
            if ( this.getLabel()==null ) {
                StackTraceElement[] sts= new Exception("getStackTrace").getStackTrace();
                System.err.println( sts[1] );
            }
            lastAlert= t;
        }
    }
    
    
}
