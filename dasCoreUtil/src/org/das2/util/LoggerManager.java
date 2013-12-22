/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.util;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.text.JTextComponent;

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
            } else if ( c instanceof JPanel && ( ((JPanel)c).getBorder() instanceof TitledBorder ) ) {
                return c;
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
     * return a human-consumable label for the component.
     * @param c
     * @return 
     */
    private static String labelFor( Component c ) {
        if ( c instanceof JPopupMenu ) {
            return ((JPopupMenu)c).getLabel();
        } else {
            if ( c.getParent() instanceof JTabbedPane ) {
                JTabbedPane tp= (JTabbedPane)c.getParent();
                int itab= tp.indexOfComponent(c);
                return tp.getTitleAt(itab);
            } else if ( c instanceof JPanel && ( ((JPanel)c).getBorder() instanceof TitledBorder )) {
                TitledBorder tb= (TitledBorder)((JPanel)c).getBorder();
                String title= tb.getTitle();
                if ( title.endsWith(" [?]") ) {
                    title= title.substring(0,title.length()-4);
                }
                return title;
            } else {
                return c.getName();
            }
            
        }
    }
    
    private static int lastEvent= 0;
    
    private static void logGuiEvent( Object source, String thisRef ) {
        if ( !EventQueue.isDispatchThread() ) {
            return;
        }
        AWTEvent evt= EventQueue.getCurrentEvent();
        if ( evt!=null ) {
            int thisEvent= evt.hashCode();
            if ( thisEvent==lastEvent ) {  // avoid secondary messages.
                return;
            }
            lastEvent= thisEvent;
        } else {
            //System.err.println("wasn't expecting this..."); // this happens when plotting "http://jfaden.net/~jbf/autoplot/bugs/sf/1140/pngwalk/product.vap?timeRange=1996-04-04"
        }
        
        String ssrc= source.toString();
        if ( ssrc.length()>10 ) {
            int i=ssrc.indexOf("[");
            if ( i>-1 ) ssrc= ssrc.substring(0,i);
            if ( source instanceof JComponent ) {
                Container c= (JComponent)source;
                Container cc= findReferenceComponent(c);
                StringBuilder src;
                if ( cc instanceof JPanel && ( ((JPanel)cc).getBorder() instanceof TitledBorder ) ) {
                    String title= labelFor(c);
                    src= new StringBuilder( " of \""+title + "\"");
                } else {
                    src= new StringBuilder( );
                }
                
                Container w= cc.getParent();  //because findReferenceComponent might be a tab
                if ( !( w instanceof JTabbedPane ) ) {
                    w= findReferenceComponent(c.getParent());
                    if ( w!=null ) {
                        if ( w.getParent() instanceof JViewport ) {
                            w= w.getParent();
                        }
                        if ( w.getParent() instanceof JScrollPane ) {
                            w= w.getParent();
                        }
                        if ( w.getParent() instanceof JTabbedPane ) {
                            JTabbedPane tp= (JTabbedPane)w.getParent();
                            int itab= tp.indexOfComponent(w);
                            String n= tp.getTitleAt(itab);
                            src.append(" of \"").append(n).append("\"");
                        }
                    }
                }
                if ( src.length()==0 && cc instanceof JPopupMenu ) {
                    String t= ((JPopupMenu)cc).getLabel();
                    if ( t!=null && t.length()==0 ) t= cc.getName();
                    if ( t==null ) {
                         Component inv= ((JPopupMenu)cc).getInvoker();
                         while ( inv instanceof JMenu ) {
                             t= ((JMenu)inv).getText();
                             src.append(" of menu \"").append(t).append( "\"");
                             Component inv1= inv.getParent();
                             if ( inv1 instanceof JPopupMenu ) {
                                 inv= ((JPopupMenu)inv1).getInvoker();
                             } else {
                                 inv= null;
                             }
                         }
                         if ( inv!=null ) {
                             src.append(" of \"").append(labelFor(inv)).append( "\"");
                         }
                    } else {
                        src.append(" of menu \"").append(t).append( "\"");
                    }
                } else if ( c instanceof JComboBox ) {
                    src.append(c.getName());
                } else if ( c instanceof AbstractButton ) {
                    String text= ((AbstractButton)c).getText();
                    if ( text==null || text.length()==0 ) {
                        text= c.getName();
                    }
                    src.append("\"").append(text).append("\"");
                } else if ( c instanceof JTextField ) {
                    String text= ((JTextField)c).getText();
                    if ( text==null || text.length()==0 ) {
                        text= c.getName();
                    }
                    src.append("\"").append(text).append("\"");
                } else if ( c instanceof JTextField ) {
                    String text= ((JTextField)c).getText();
                    if ( text==null || text.length()==0 ) {
                        text= c.getName();
                    }
                    src.append("\"").append(text).append("\"");
                }
                
                Window h= SwingUtilities.getWindowAncestor(c);
                String htitle=null;
                if ( h instanceof Dialog ) {
                    src.append(" of \"").append( ((Dialog)h).getTitle()).append("\"");
                } else if ( h instanceof JFrame ) {
                    src.append(" of \"").append( ((JFrame)h).getTitle()).append("\"");
                }
                ssrc= src.toString();
                
            }
        }
        if ( thisRef.length()>30 ) { // pngwalk tool uses action commands to handle argument passing.
            getLogger("gui").log( Level.FINE, "{1}", new Object[]{ ssrc });
        } else {
            getLogger("gui").log( Level.FINE, "\"{0}\" from {1}", new Object[]{ thisRef, ssrc });
        }
        
    }
    /**
     * provide easy way to log all GUI events.
     * @param e 
     */
    public static void logGuiEvent( ActionEvent e ) {
        if ( e==null ) {
            getLogger("gui").log( Level.FINEST, "null ActionEvent");
            return;
        }
        if ( !( getLogger("gui").isLoggable(Level.FINE ) ) ) {
            return;
        }
        if ( e.getSource() instanceof JCheckBox ) {
            JCheckBox cb= (JCheckBox)e.getSource();
            logGuiEvent( e.getSource(), ( cb.isSelected() ? "select " : "deselect " ) + e.getActionCommand() );
        } else if ( e.getSource() instanceof JComboBox ) {
            JComboBox cb= (JComboBox)e.getSource();
            logGuiEvent( e.getSource(), cb.getEditor().getItem().toString() );
        } else {
            logGuiEvent( e.getSource(), e.getActionCommand() );
        }
    }
    
    public static void logGuiEvent( ChangeEvent e ) {
        if ( !( getLogger("gui").isLoggable(Level.FINE ) ) ) {
            return;
        }
        logGuiEvent( e.getSource(), "changeEvent" );
    }
    
    public static void logGuiEvent( ItemEvent e ) {
        if ( !( getLogger("gui").isLoggable(Level.FINE ) ) ) {
            return;
        }
        logGuiEvent( e.getSource(), "itemEvent" );
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
