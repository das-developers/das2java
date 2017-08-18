
package org.das2.util;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.swing.AbstractButton;
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
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;

/**
 * Central place that keeps track of loggers.  Note that both org.das.datum 
 * and org.das2.datum have this same class, which is there to avoid coupling between the 
 * packages.
 * @author jbf
 */
public final class LoggerManager {

    private static final Set<String> loggers= new HashSet();
    private static final Map<String,Logger> log= new HashMap();
    private static final Set<Handler> extraHandlers= new HashSet();

    /**
     * There's something I (jbf) don't get about logging, so I'll abandon this for today.
     */
    private static boolean isTimeTaggingLoggers=false;
    
    /**
     *
     * @param t
     */
    public synchronized static void setUseTimeTaggingLoggers(boolean t ) {
        isTimeTaggingLoggers= t;
        loggers.clear();
        extraHandlers.clear();
        log.clear();
    }
    
    /**
     * are we keeping track of log message times, so we can sort loggers by 
     * how recently messages were posted?
     * @return true if we are keeping track of log message times.
     */
    public synchronized static boolean isUseTimeTaggingLoggers( )  {
        return isTimeTaggingLoggers;
    }
    
    private static final Level[] levels= new Level[] { Level.ALL, Level.FINEST, Level.FINER, Level.FINE, Level.CONFIG, Level.INFO, Level.WARNING, Level.SEVERE, Level.OFF };
    
    /**
     * Logger keeps track of the last record timetag.
     */
    public static final class TimeTaggingLogger extends Logger {
        
        public long lastTime= 0;
        
        private TimeTaggingLogger( String id ) {
            super(id,null);
        }
        
        @Override
        public String toString() {
//            Level l=null;
//            for ( Level thel: levels ) {
//                if ( this.isLoggable(thel) ) {
//                    l= thel;
//                    break;
//                }
//            }
            return this.getName(); // + " @ " + l;
        }

        //TODO: Note this is not a complete list of all the log methods which need to be overriden.
        
        @Override
        public void log(Level level, String msg) {
            super.log(level, msg);
            this.lastTime= System.currentTimeMillis();
        }

        @Override
        public void log(Level level, String msg, Object param1) {
            super.log(level, msg, param1); //To change body of generated methods, choose Tools | Templates.
            this.lastTime= System.currentTimeMillis();
        }
        
        
        @Override
        public void log(LogRecord record) {
            super.log(record);
            this.lastTime= record.getMillis();
        }
        
        /**
         * return the timestamp of the last log message.
         * @return 
         */
        public long getLastTime() {
            return lastTime;
        }
        
    }
    
    /**
     * return the requested logger, but add it to the list of known loggers.
     * @param id the name
     * @return the Logger
     */
    public synchronized static Logger getLogger( String id ) {
        Logger result= log.get(id);
        if ( result!=null ) {
            return result;
        } else {
            if ( isTimeTaggingLoggers ) {
                result= new TimeTaggingLogger(id);
            } else {
                result= Logger.getLogger(id);
            }
            log.put( id, result );
            for ( Handler h: extraHandlers ) {
                result.addHandler(h);
            }
        }
        loggers.add(id);
        return result;
    }

    /**
     * return the list of known loggers.
     * @return the list of known loggers.
     */
    public static Set<String> getLoggers() {
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
    
    private static int timerCycleCount=0;
    
    private static PrintStream timerLogger= System.err;

    /**
     * return enableTimers property.
     * @return enableTimers property.
     */
    public static boolean isEnableTimers() {
        return !disableTimers;
    }

    /**
     * if enableTimers is true, then resetTimer and markTime have 
     * an effect.  Each thread can have a timer to measure the execution 
     * time for a process.
     * @param enableTimers true to enable timers
     */
    public static void setEnableTimers(boolean enableTimers) {
        disableTimers = !enableTimers;
        timerCycleCount= 0;
        if ( !enableTimers ) {
            if ( timerLogger!=System.err ) {
                timerLogger.close();
            }
        }
    }
    
    /**
     * channel the logging information to here, setEnableTimers(false) to close.
     * @param f
     * @throws FileNotFoundException 
     */
    public static void setTimerLogfile(String f) throws FileNotFoundException {
        timerLogger= new PrintStream(f);
    }

    // clean up code that times things by keeping track of timer...
    private static final Map<Thread,TimerInfo> timers= new WeakHashMap<>();
    
    private static class TimerInfo {
        private TimerInfo( long birthNs ) {
            this.lastNs= this.birthNs= birthNs;
        }
        long birthNs;  // birth time
        long lastNs;   // time since last
        long count;    // increment the number of times incremented
    }
    
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
     * reset the timer for this thread.  The lifecycle is like so:<ul>
     * <li>LoggerManager.resetTimer("big task");
     * <li>LoggerManager.markTime("done loading");
     * <li>LoggerManager.markTime("calculated data");
     * <li>LoggerManager.clearTimer();  
     * </ul>
     * 
     * Note the timers are stored with weak references to the threads, so 
     * clearTimer needn't be called.
     * @param task 
     * @see #setEnableTimers(boolean) 
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
        timerLogger.println( String.format( "== %s ==", task ) );
        timers.put( Thread.currentThread(), new TimerInfo( System.nanoTime() ) );
        timerLogger.println( String.format( "Cycle Count Time(ms) TimeSinceLast(ms) Message" ) );
        timerCycleCount++;
    }
    
    /**
     * mark the time using the thread name.
     */
    public static void markTime() {
        if ( disableTimers ) return;
        markTime(null);
    }
    
    /**
     * mark the time that this occurred.
     * @param message message to accompany
     * @see #setEnableTimers(boolean) to turn on timers.
     * @see #resetTimer(java.lang.String) to reset timers.
     */
    public static void markTime( String message ) {
        if ( disableTimers ) return;
        TimerInfo timerInfo= timers.get( Thread.currentThread() );
        if ( timerInfo!=null ) {
            if ( message==null ) message= Thread.currentThread().getName();
            long t= System.nanoTime();
            timerLogger.println( String.format( "%d %d %.6f %.6f %s", timerCycleCount, timerInfo.count, (t-timerInfo.birthNs)/1e6, (t-timerInfo.lastNs)/1e6, message ) );
            timerInfo.lastNs= t;
            timerInfo.count++;
        }
    }
    
    /**
     * explicitly remove this timer.  
     * @see #resetTimer() 
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
        return child;
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
    
    // keep track of responsiveness of GUI event handling.  
    // This is the start of the event, and clients should call logExitGuiEvent to log time.
    private static long lastEventTime=0;
    
    private static void logGuiEvent( Object source, String thisRef ) {
        if ( !EventQueue.isDispatchThread() ) {
            return;
        }
        lastEventTime= System.currentTimeMillis();
        
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
    
    /**
     * call this at the end of the GUI event to measure time to respond.
     * @param e the focus event.
     */
    public static void logExitGuiEvent( ActionEvent e ) {
        long t1= System.currentTimeMillis();
        getLogger("gui").log( Level.FINE, "handled \"{0}\" in (ms): {1}", new Object[]{ "actionEvent", t1-lastEventTime });
    }    
    
    /**
     * log the GUI ChangeEvent
     * @param e
     */
    public static void logGuiEvent( ChangeEvent e ) {
        if ( !( getLogger("gui").isLoggable(Level.FINE ) ) ) {
            return;
        }
        logGuiEvent( e.getSource(), "changeEvent" );
    }
    
    /**
     * call this at the end of the GUI event to measure time to respond.
     * @param e the focus event.
     */
    public static void logExitGuiEvent( ChangeEvent e ) {
        long t1= System.currentTimeMillis();
        getLogger("gui").log( Level.FINE, "handled \"{0}\" in (ms): {1}", new Object[]{ "changeEvent", t1-lastEventTime });
    }    

    /**
     * log the GUI ItemEvent
     * @param e 
     */    
    public static void logGuiEvent( ItemEvent e ) {
        if ( !( getLogger("gui").isLoggable(Level.FINE ) ) ) {
            return;
        }
        logGuiEvent( e.getSource(), "itemEvent" );
    }
    
    /**
     * call this at the end of the GUI event to measure time to respond.
     * @param e the focus event.
     */
    public static void logExitGuiEvent( ItemEvent e ) {
        long t1= System.currentTimeMillis();
        getLogger("gui").log( Level.FINE, "handled \"{0}\" in (ms): {1}", new Object[]{ "itemEvent", t1-lastEventTime });
    }    
                
    /**
     * log the GUI FocusEvent
     * @param e 
     */
    public static void logGuiEvent( FocusEvent e ) {
        if ( !( getLogger("gui").isLoggable(Level.FINE ) ) ) {
            return;
        }
        logGuiEvent( e.getSource(), "focusEvent" );
    }
    
    /**
     * call this at the end of the GUI event to measure time to respond.
     * @param e the focus event.
     */
    public static void logExitGuiEvent( FocusEvent e ) {
        long t1= System.currentTimeMillis();
        getLogger("gui").log( Level.FINE, "handled \"{0}\" in (ms): {1}", new Object[]{ "focusEvent", t1-lastEventTime });
    }
    
    /**
     * log property change events.  (I realized I spend a lot of time debugging walking 
     * through the property change fire event code, and I should just add a 
     * log message to all propertyChange codes.)
     * @param e 
     */
    public static void logPropertyChangeEvent( PropertyChangeEvent e ) {
        getLogger("gui").log(Level.FINE, "PropertyChange {0}={1}", new Object[]{e.getPropertyName(), e.getNewValue()});
    }
            
    /**
     * log property change events.  (I realized I spend a lot of time debugging walking 
     * through the property change fire event code, and I should just add a 
     * log message to all propertyChange codes.)
     * @param e 
     * @param source comment on the source
     */
    public static void logPropertyChangeEvent( PropertyChangeEvent e, String source ) {
        getLogger("gui").log(Level.FINE, "PropertyChange {0}={1} {2}", new Object[]{e.getPropertyName(), e.getNewValue(), source});
    }
            
//    
//    public static void main( String[] args ) {
//        Logger l= LoggerManager.getLogger("test");
//        Exception e= new java.lang.Exception("this is the problem") ;
//        l.log( Level.WARNING, null, e ); // BUG 1119 DEMONSTRATION PURPOSES
//        l.log( Level.WARNING, e.getMessage(), e );
//        l.log( Level.WARNING, "Exception: {0}", e );
//        l.log( Level.INFO, "hello there..." );
//    }

    /**
     * A slightly more transparent logging configuration would provide feedback
     * about what configuration file it's loading.  This will echo
     * when the configuration file would be.
     * 
     * The idea is when you are completely frustrated with not getting the logger
     * to behave, you can add:
     * org.das2.util.LoggerManager.readConfiguration() 
     * to your code.
     * 
     */
    public static void readConfiguration() {
        String configfile= System.getProperty("java.util.logging.config.file");
        if ( configfile==null ) {
            System.err.println("no config file, set java property java.util.logging.config.file like so:");
            System.err.println("-Djava.util.logging.config.file=/tmp/logging.properties");
        } else {
            File ff= new File( configfile );
            if ( ff.isAbsolute() ) {
                System.err.println("loading logging configuration from "+ff);
            } else {
                ff= ff.getAbsoluteFile();
                System.err.println("loading logging configuration from "+ff);
            }
        }
        try {
            LogManager.getLogManager().readConfiguration();
        } catch ( IOException ex ) {
            ex.printStackTrace();
        }
    }
}
