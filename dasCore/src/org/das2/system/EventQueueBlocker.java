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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/**
 * Tool for emptying the event queue.  This will post an event and wait until
 * the event clears the event queue.
 * @author Jeremy
 */
public final class EventQueueBlocker {
    
    private static final Logger logger= Logger.getLogger( "das2.system" );
    
    private static final Object lockObject= new Object();
    
    /** Creates a new instance of EventQueueBlocker */
    private EventQueueBlocker() {
    }
    
    private static final Runnable clearEventQueueImmediatelyRunnable= new Runnable() {
        @Override
        public void run() {
            clearEventQueueImmediately();
        }
    };
    
    private static void clearEventQueueImmediately() {
        DasUpdateEvent evt;
        synchronized(lockObject) {
            evt= (DasUpdateEvent) Toolkit.getDefaultToolkit().getSystemEventQueue().peekEvent(DasUpdateEvent.DAS_UPDATE_EVENT_ID)  ;
            if ( evt != null ) {
                logger.log(Level.FINER, "pending update event: {0}", evt);
                EventQueue.invokeLater( clearEventQueueImmediatelyRunnable );
            } else {
                logger.finer("no update events found ");
                lockObject.notify();  // findbugs NN_NAKED_NOTIFY okay
            }
        }
    }
    
    /**
     * block until the event queue is cleared.
     */
    public static void clearEventQueue() {
        
        if ( SwingUtilities.isEventDispatchThread() ) {
            throw new IllegalStateException( "must not be called on the EventQueue");
        }
        synchronized(lockObject) {
            if ( Toolkit.getDefaultToolkit().getSystemEventQueue().peekEvent(DasUpdateEvent.DAS_UPDATE_EVENT_ID) != null ) {
                EventQueue.invokeLater( clearEventQueueImmediatelyRunnable );
                logger.finer("waiting for lockObject to indicate eventQueue is clear");
                try {
                    lockObject.wait();  // findbugs WA_NOT_IN_LOOP okay
                } catch (InterruptedException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            } else {
                logger.finer("no update events found, no runnable submitted ");
            }
        }
    }
    
    /**
     * method for inspecting the event queue.
     * @param out
     */
    public static void dumpEventQueue( PrintStream out ) {
        
        EventQueue eventQueue= Toolkit.getDefaultToolkit().getSystemEventQueue();
        
        Queue queue= new LinkedList();
        AWTEvent evt=null;
        DasUpdateEvent result=null;
        while ( eventQueue.peekEvent()!=null ) {
            try {
                evt= eventQueue.getNextEvent();
                out.println( String.format( "%6d %s",evt.getID(), evt.toString() ) );
            } catch (InterruptedException ex) {
            }
            queue.add(evt);
        }
        while ( queue.size() > 0 ) {
            eventQueue.postEvent((AWTEvent)queue.remove());
        }
    }
}
