/*
 * LoggingEventQueue.java
 *
 * Created on October 31, 2005, 1:02 PM
 *
 *
 */

package edu.uiowa.physics.pw.das.util.awt;

import java.awt.EventQueue;
import java.awt.event.InvocationEvent;

/**
 * Tool for debugging event queue stuff.  This can be used to log the event 
 * queue, or insert breakpoints, etc.
 * Toolkit.getDefaultToolkit().getSystemEventQueue().push(new LoggingEventQueue());
 *
 * @author Jeremy
 * 
 */
public class LoggingEventQueue extends EventQueue {
    public void postEvent(java.awt.AWTEvent theEvent) {
        if (theEvent instanceof InvocationEvent) {
            System.out.print("XXX");
        }
        System.out.println(""+theEvent);
        super.postEvent(theEvent);
    }
    
}
