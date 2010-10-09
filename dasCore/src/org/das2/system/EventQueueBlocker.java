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
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/**
 *
 * @author Jeremy
 */
public final class EventQueueBlocker {

    private static final Logger logger= Logger.getLogger( "das2.system" );

    /** Creates a new instance of EventQueueBlocker */
    private EventQueueBlocker() {
    }


    static class MyEventQueue extends EventQueue {
        protected void dispatchEvent(AWTEvent event) {
            super.dispatchEvent(event);
            if (myEventQueue.peekEvent(DasUpdateEvent.DAS_UPDATE_EVENT_ID) == null) {
                synchronized (this) {
                    pop();
                    this.notifyAll();
                }
            }
        }
        /*
        protected void pop() throws EmptyStackException {
            super.pop();
        }
         */
    }

    static MyEventQueue myEventQueue= new MyEventQueue();

    private static void clearEventQueueImmediately()  {
        System.err.println( Thread.currentThread().getName() );

        synchronized (myEventQueue) {
            Toolkit.getDefaultToolkit().getSystemEventQueue().push( myEventQueue );
            try {
                myEventQueue.wait();
            } catch (InterruptedException ie) {
                throw new RuntimeException(ie);
            }
        }

        /*
        while (myEventQueue.peekEvent(DasUpdateEvent.DAS_UPDATE_EVENT_ID) != null) {
            try {
                Thread.sleep(40);
            }
            catch (InterruptedException ie) {
                throw new RuntimeException(ie);
            }
        }

        myEventQueue.pop();
         */
        logger.info("no more pending events");
    }


    private static DasUpdateEvent findUpdateEvent( EventQueue eventQueue ) {
        Queue queue= new LinkedList();
        AWTEvent evt=null;
        DasUpdateEvent result=null;
        while ( eventQueue.peekEvent()!=null ) {
            try {
                evt= eventQueue.getNextEvent();
                if ( evt instanceof DasUpdateEvent ) {
                    result= (DasUpdateEvent)evt;
                }
            } catch (InterruptedException ex) {
            }
            queue.add(evt);
        }
        while ( queue.size() > 0 ) {
            eventQueue.postEvent((AWTEvent)queue.remove());
        }
        return result;
    }

    public static synchronized void clearEventQueue() throws InterruptedException {

        if ( SwingUtilities.isEventDispatchThread() ) {
            clearEventQueueImmediately();
        } else {
            try {
                EventQueue.invokeAndWait(new Runnable() {
                    public void run() {
                        clearEventQueueImmediately();
                    }
                });
            } catch (InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
            logger.finer("waiting for lockObject to indicate eventQueue is clear");
            logger.finer("event queue task complete");
        }
    }
}
