/*
 * LoggingEventQueue.java
 *
 * Created on October 31, 2005, 1:02 PM
 *
 *
 */

package org.das2.util.awt;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.event.InvocationEvent;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

/**
 * Tool for debugging event queue stuff.  This can be used to log the event
 * queue, or insert breakpoints, etc.
 * Toolkit.getDefaultToolkit().getSystemEventQueue().push(new LoggingEventQueue());
 *
 * @author Jeremy
 *
 */
public class LoggingEventQueue extends EventQueue {
    
    final static Logger logger= Logger.getLogger( "das2.gui" );
    
    private LoggingEventQueue() {
        
    }
    
    @Override
    public void postEvent(java.awt.AWTEvent theEvent) {
        if (theEvent instanceof InvocationEvent) {
         //   logger.info("XXX: "+theEvent);
        } else {
        //    logger.info(""+theEvent);
        }
        super.postEvent(theEvent);
    }
    
    private static LoggingEventQueue instance;
    public static LoggingEventQueue getInstance() {
        if ( instance==null ) {
            instance= new LoggingEventQueue();
        }
        return instance;
    }
    
    public static synchronized void dumpPendingEvents() {
        System.err.println("---------------------------------------------------------------");
        Queue queue= new LinkedList();
        AWTEvent evt;
        while ( instance.peekEvent()!=null ) {
            try {
                evt= instance.getNextEvent();
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            System.err.println("[ "+evt);
            queue.add(evt);
        }
        System.err.println("-----e--n--d-----------------------------------------------------");
        while ( queue.size() > 0 ) {
            instance.postEvent((AWTEvent)queue.remove());
        }
    }
    
}
