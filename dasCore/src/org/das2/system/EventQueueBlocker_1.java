/*
 * EventQueueBlocker.java
 *
 * Created on May 25, 2006, 8:31 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.das2.system;

import org.das2.event.DasUpdateEvent;
import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/**
 *
 * @author Jeremy
 */
public final class EventQueueBlocker_1 {
    
    private static final Logger logger= Logger.getLogger( "das2.system" );
    
    private static final Object lockObject= new Object();
    
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
                lockObject.notify();  // findbugs NN_NAKED_NOTIFY okay
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
                lockObject.wait();  // findbugs WA_NOT_IN_LOOP okay
            } else {
                logger.finer("no update events found, no runnable submitted ");
            }
        }
    }
    
    /**
     * method for inspecting the event queue.
     */
    public static void dumpEventQueue( PrintStream out ) {
        
        EventQueue eventQueue= Toolkit.getDefaultToolkit().getSystemEventQueue();
        
        Queue queue= new LinkedList();
        AWTEvent evt=null;
        DasUpdateEvent result=null;
        while ( eventQueue.peekEvent()!=null ) {
            try {
                evt= eventQueue.getNextEvent();
                out.println(evt);
            } catch (InterruptedException ex) {
            }
            queue.add(evt);
        }
        while ( queue.size() > 0 ) {
            eventQueue.postEvent((AWTEvent)queue.remove());
        }
    }
}
