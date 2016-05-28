
package org.das2.util.monitor;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Example monitor that prints progress messages to the console (stderr).
 * TODO: It might be nice to find a library that supports terminals so that newlines
 * are not printed.
 * @author jbf
 */
public class ConsoleTextProgressMonitor extends NullProgressMonitor {

    Thread updateThread;
    
    private Runnable createRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(ConsoleTextProgressMonitor.class.getName()).log(Level.SEVERE, null, ex);
                }
                while ( !isFinished() ) {
                    StringBuilder s= new StringBuilder( getLabel() + ": " );
                    if ( getTaskSize()>-1 ) {
                        s.append(getTaskProgress()).append(" of ").append(getTaskSize()).append(": ");
                    }
                    s.append( getProgressMessage() );
                    System.err.println( s.toString() );
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ConsoleTextProgressMonitor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        };
    }
    @Override
    public void started() {
        updateThread= new Thread(createRunnable(),"consoleTextProgressMonitor");
        updateThread.start();
    }
    
    @Override
    public void finished() {
        super.finished(); 
    }
    
}
