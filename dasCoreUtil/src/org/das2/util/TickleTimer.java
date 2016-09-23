/*
 * TickleTimer.java
 *
 * Created on July 28, 2006, 9:23 PM
 *
 */

package org.das2.util;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TickleTimer is a timer that fires once it's been left alone for 
 * a while.  The idea is the keyboard can be pecked away and 
 * the change event will not be fired until the keyboard is idle.
 * 
 * This is a copy of the TickleTimer used in Autoplot.
 *
 * java.util.Timer performs very similar functionality.
 * @author Jeremy Faden
 */
public class TickleTimer {
    long tickleTime;
    long delay;
    boolean running;
    boolean firing=false; // true when the listener is being invoked.
    List<String> messages;
    
    static final Logger log= org.das2.util.LoggerManager.getLogger("das2.util");
    
    /**
     * @param delay time in milliseconds to wait until firing off the change.  
     *   If delay is =&lt;0, then events will be fired off immediately.
     * @param listener provides the callback when the timer 
     *    runs out.  The listener is added as one of the bean's property
     *    change listeners.
     */
    public TickleTimer( long delay, PropertyChangeListener listener ) {
        this.tickleTime= System.currentTimeMillis();
        this.delay= delay;
        addPropertyChangeListener( listener );
        this.running= false;
        messages= new ArrayList<>();
    }
    
    private void startTimer() {
        running= true;
        if ( delay<=0 ) {
            newRunnable().run();
        } else {
            new Thread( newRunnable(), "tickleTimerThread" ).start();
            messages.clear();
        }
    }
    
    private Runnable newRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                long d=  System.currentTimeMillis() - tickleTime;
                while ( d < delay ) {
                    try {
                        log.log(Level.FINER, "tickleTimer sleep {0}", (delay - d));
                        Thread.sleep( delay-d );
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                    d= System.currentTimeMillis() - tickleTime;
                }
                log.log(Level.FINER, "tickleTimer fire after {0}", d );
                firing= true;
                running= false; //sometimes listeners need to retickle the timer...
                propertyChangeSupport.firePropertyChange("running",true,false);
                firing= false;
                messages= new ArrayList<>();
            }
        };
    }
    
    public synchronized void tickle(){
        tickle(null);
    }

    public synchronized void tickle( String message ) {
        if ( firing ) {
            log.log(Level.FINE, "tickle while firing, which may be result in bugs");
        }
        tickleTime= System.currentTimeMillis();
        if ( !running ) startTimer();
        if ( message!=null ) messages.add(message);
    }
    
    private final java.beans.PropertyChangeSupport propertyChangeSupport =  new java.beans.PropertyChangeSupport(this);

    public final void addPropertyChangeListener(java.beans.PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(l);
    }

    public final void removePropertyChangeListener(java.beans.PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener(l);
    }

    public boolean isRunning() {
        return this.running;
    }

    public void setRunning(boolean running) {
        boolean oldRunning = this.running;
        this.running = running;
        propertyChangeSupport.firePropertyChange ("running", Boolean.valueOf(oldRunning), Boolean.valueOf(running));
    }
    
    public List<String> getMessages() {
        return Collections.unmodifiableList(messages);
    }
    
}
