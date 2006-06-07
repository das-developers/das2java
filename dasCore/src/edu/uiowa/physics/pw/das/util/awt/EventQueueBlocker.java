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
public class EventQueueBlocker {
    private static final Logger logger= DasLogger.getLogger(DasLogger.SYSTEM_LOG);

    /** Creates a new instance of EventQueueBlocker */
    public EventQueueBlocker() {
    }

    class MyEventQueue extends EventQueue {
        protected void dispatchEvent(AWTEvent event) {
            super.dispatchEvent(event);
        }
        protected void pop() throws EmptyStackException {
        }
    }

    private void clearEventQueueImmediately()  {
        MyEventQueue eventQueue=  new MyEventQueue();
        Toolkit.getDefaultToolkit().getSystemEventQueue().push( eventQueue );

        /*
        DasUpdateEvent evt= findUpdateEvent( eventQueue );
        while ( evt!=null ) {
            AWTEvent evt2;
            try {
                evt2 = eventQueue.getNextEvent(); //TODO: improve by poping all the non-update events until the update event is found               
                eventQueue.dispatchEvent(evt2);
                evt= findUpdateEvent( eventQueue );
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
         */
        while (eventQueue.peekEvent(DasUpdateEvent.DAS_UPDATE_EVENT_ID) != null) {
            try {
                Thread.sleep(10);
            }
            catch (InterruptedException ie) {
                throw new RuntimeException(ie);
            }
        }

        eventQueue.pop();
        logger.info("no more pending events");
    }


    private DasUpdateEvent findUpdateEvent( EventQueue eventQueue ) {
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

    public synchronized void clearEventQueue() throws InterruptedException {
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
