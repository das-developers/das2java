/*
 * EventQueueBlocker.java
 *
 * Created on May 25, 2006, 8:31 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.uiowa.physics.pw.das.util.awt;

import edu.uiowa.physics.pw.das.event.DasUpdateEvent;
import edu.uiowa.physics.pw.das.system.DasLogger;
import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.lang.reflect.InvocationTargetException;
import java.util.EmptyStackException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/**
 *
 * @author Jeremy
 */
public final class EventQueueBlocker_1 {
    
    private static final Logger logger= DasLogger.getLogger(DasLogger.SYSTEM_LOG);
    
    private static Object lockObject= new String("EQB_1");
    
    /** Creates a new instance of EventQueueBlocker */
    private EventQueueBlocker_1() {
    }
    
    private static Runnable clearEventQueueImmediatelyRunnable= new Runnable() {
        public void run() {
            clearEventQueueImmediately();
        }
    };
    
    private static void clearEventQueueImmediately() {
        DasUpdateEvent evt;
        evt= (DasUpdateEvent) Toolkit.getDefaultToolkit().getSystemEventQueue().peekEvent(DasUpdateEvent.DAS_UPDATE_EVENT_ID)  ;
        if ( evt != null ) {
            logger.finer("pending update event: "+evt);
            EventQueue.invokeLater( clearEventQueueImmediatelyRunnable );
        } else {
            logger.finer("no update events found ");
            synchronized(lockObject) {
                lockObject.notify();
            }
        }
    }
    
    public static void clearEventQueue() throws InterruptedException {
        
        if ( SwingUtilities.isEventDispatchThread() ) {
            throw new IllegalStateException( "must not be called on the EventQueue");
        }
        synchronized(lockObject) {
            if ( Toolkit.getDefaultToolkit().getSystemEventQueue().peekEvent(DasUpdateEvent.DAS_UPDATE_EVENT_ID) != null ) {
                EventQueue.invokeLater( clearEventQueueImmediatelyRunnable );
                logger.finer("waiting for lockObject to indicate eventQueue is clear");
                lockObject.wait();
            } else {
                logger.finer("no update events found, no runnable submitted ");
            }
        }
    }
}
