/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

/**
 * Central place that keeps track of loggers.  Note that both org.das.datum 
 * and org.das2.datum have this same class, which is there to avoid coupling between the 
 * packages.
 * @author jbf
 */
public final class LoggerManager {

    private static Set<String> loggers= new HashSet();
    private static Map<String,Logger> log= new HashMap();
    private static Set<Handler> extraHandlers= new HashSet();

    /**
     * return the requested logger, but add it to the list of known loggers.
     * @param id
     * @return
     */
    public synchronized static Logger getLogger( String id ) {
        Logger result= log.get(id);
        if ( result!=null ) {
            return result;
        } else {
            result= Logger.getLogger(id);
            log.put( id, result );
            for ( Handler h: extraHandlers ) {
                result.addHandler(h);
            }
        }
        loggers.add(id);
        return Logger.getLogger(id);
    }

    /**
     * return the list of known loggers.
     * @return
     */
    public static Set getLoggers() {
        return loggers;
    }
    
    /**
     * Add the handler to all loggers created by this manager, and all
     * future.  Only call this once!  I would think that adding a handler to 
     * the root would essentially add the handler to all loggers, but it doesn't 
     * seem to. 
     * 
     * @param handler e.g. GraphicalLogHandler
     */
    public static void addHandlerToAll( Handler handler ) {
        for ( String l: loggers ) {
            log.get(l).addHandler(handler);
        }
        extraHandlers.add(handler);
    }
    
    private static boolean disableTimers = true;

    public static boolean isEnableTimers() {
        return !disableTimers;
    }

    /**
     * if enableTimers is true, then resetTimer and markTime have 
     * an effect.  Each thread can have a timer to measure the execution 
     * time for a process.
     */
    public static void setEnableTimers(boolean enableTimers) {
        disableTimers = !enableTimers;
    }

    // clean up code that times things by keeping track of timer...
    private static Map<Thread,Long> timers= new WeakHashMap<Thread, Long>();
    
    /**
     * reset the timer.  The lifecycle is like so:
     *  LoggerManager.resetTimer("big task");
     *  LoggerManager.markTime("done loading");
     *  LoggerManager.markTime("calculated data");
     *  LoggerManager.clearTimer();  
     * Note the timers are stored with weak references to the threads, so 
     * clearTimer needn't be called.
     */
    public static void resetTimer() {
        if ( disableTimers ) return;
        resetTimer(null);
    }
    
    /**
     * reset the timer for this thread.  The lifecycle is like so:
     *  LoggerManager.resetTimer("big task");
     *  LoggerManager.markTime("done loading");
     *  LoggerManager.markTime("calculated data");
     *  LoggerManager.clearTimer();  
     * Note the timers are stored with weak references to the threads, so 
     * clearTimer needn't be called.
     * @param task 
     */
    public static void resetTimer( String task ) {
        if ( disableTimers ) return;
        if ( task==null ) {
            task= Thread.currentThread().getName();
        } else {
            if ( EventQueue.isDispatchThread() ) {
                task= task + " (GUI)";
            }
        }
        System.err.println( String.format( "== %s ==", task ) );
        timers.put( Thread.currentThread(), System.currentTimeMillis() );
    }
    
    /**
     * mark the time using the thread name.
     * @param message 
     */
    public static void markTime() {
        if ( disableTimers ) return;
        markTime(null);
    }
    
    /**
     * mark the time that this occurred.
     * @param message message to accompany
     */
    public static void markTime( String message ) {
        if ( disableTimers ) return;
        Long t0= timers.get( Thread.currentThread() );
        if ( t0!=null ) {
            if ( message==null ) message= Thread.currentThread().getName();
            System.err.println( String.format( "%05d: %s", System.currentTimeMillis()-t0, message ) );
        }
    }
    
    /**
     * explicitly remove this timer.  @see resetTimer.
     */
    public static void clearTimer() {
        if ( disableTimers ) return;
        timers.remove( Thread.currentThread() );
    }
    
    private static Container findReferenceComponent( Container c ) {
        Container child=null;
        while ( c!=null ) {
            child= c;
            c= c.getParent();
            if ( c instanceof JTabbedPane ) {
                return child;
            } else if ( c instanceof JDialog ) {
                return c;
            } else if ( c!=null && c.getClass().getName().startsWith("org") ) {
                return c;
            }
        }
        if ( c==null ) {
            return (child);
        } else {
            return c;
        }
    }
    
    /**
     * provide easy way to log all GUI events.
     * @param e 
     */
    public static void logGuiEvent( ActionEvent e ) {
        if ( !( getLogger("gui").isLoggable(Level.FINE ) ) ) {
            return;
        }
        if ( !EventQueue.isDispatchThread() ) {
            return;
        }
        String ssrc= e.getSource().toString();
        if ( ssrc.length()>10 ) {
            int i=ssrc.indexOf("[");
            if ( i>-1 ) ssrc= ssrc.substring(0,i);
            if ( e.getSource() instanceof JComponent ) {
                Container cc= findReferenceComponent((JComponent)e.getSource());
                Container w= cc.getParent();
                if ( cc instanceof Dialog ) {
                    ssrc= ssrc + " of \"" + ((Dialog)cc).getTitle() + "\"";
                } else if ( cc instanceof JFrame ) {
                    ssrc= ssrc + " of \"" + ((JFrame)cc).getTitle()+ "\"";;
                } else if ( w instanceof JTabbedPane ) {
                    JTabbedPane p= (JTabbedPane)cc.getParent();
                    String title= p.getTitleAt( p.indexOfComponent(cc) );
                    ssrc= ssrc + " of tab \"" + title+ "\"";
                } else {
                    String ofsrc= cc.toString();
                    int i2=ofsrc.indexOf("[");
                    if ( i2>-1 ) ofsrc= ofsrc.substring(0,i2);
                    ssrc= ssrc + " of " + ofsrc;
                }
                
            }
        }
        getLogger("gui").log( Level.FINE, "\"{0}\" from {1}", new Object[]{e.getActionCommand(), ssrc });
    }
            
    public static void main( String[] args ) {
        Logger l= LoggerManager.getLogger("test");
        Exception e= new java.lang.Exception("this is the problem") ;
        l.log( Level.WARNING, null, e ); // BUG 1119 DEMONSTRATION PURPOSES
        l.log( Level.WARNING, e.getMessage(), e );
        l.log( Level.WARNING, "Exception: {0}", e );
        l.log( Level.INFO, "hello there..." );
    }
}
