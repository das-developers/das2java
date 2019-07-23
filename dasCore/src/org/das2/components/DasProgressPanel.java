/* File: DasProgressPanel.java
 * Copyright (C) 2002-2003 The University of Iowa
 * Created by: Jeremy Faden <jbf@space.physics.uiowa.edu>
 *             Jessica Swanner <jessica@space.physics.uiowa.edu>
 *             Edward E. West <eew@space.physics.uiowa.edu>
 *
 * This file is part of the das2 library.
 *
 * das2 is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.das2.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import org.das2.graph.DasCanvasComponent;
import org.das2.system.DasLogger;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.util.logging.Level;
import org.das2.util.monitor.ProgressMonitor;
import org.das2.util.NumberFormatUtil;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import oracle.jrockit.jfr.JFR;
import org.das2.graph.DasCanvas;
import org.das2.util.LoggerManager;
import org.das2.util.monitor.NullProgressMonitor;
import org.das2.util.monitor.SubTaskMonitor;

/**
 * ProgressMonitor component used throughout das2.
 *
 * Here's an Autoplot script demoing its operation:
 *<blockquote><pre>{@code
 *monitor.setTaskSize( 100 )
 *monitor.started()
 *
 *for i in range(100):
 *  if ( i>50 and monitor.isCancelled() ):
 *    raise Exception('cancel pressed')
 *  print i
 *  java.lang.Thread.sleep(100)
 *  monitor.setTaskProgress(i)
 *
 *monitor.finished()
 *}</pre></blockquote>
 *
 * @author  eew
 */
public class DasProgressPanel implements ProgressMonitor {

    private static final Logger logger = LoggerManager.getLogger("das2.system.monitor");

    public static final String MSG_CANCEL_TASK = "cancel task";
    public static final String MSG_TASK_CANNOT_BE_CANCELED = "task cannot be cancelled";

    private static final int PROGRESS_MESSAGE_LEN_LIMIT = 40;
    private static final int LABEL_LEN_LIMIT = 34;

    private long taskStartedTime;
    private long currentTaskPosition;
    private long maximumTaskPosition;
    private DecimalFormat transferRateFormat;
    private String transferRateString;
    private JLabel taskLabel;
    private boolean labelDirty = false;
    private JLabel progressMessageLabel;
    private String progressMessageString;
    private boolean progressMessageDirty = false;
    private JLabel kbLabel;
    private JProgressBar progressBar;
    private Window jframe = null;  // created when createFramed() is used.
    private boolean isCancelled = false;
    private JButton cancelButton;
    private int cancelCheckFailures = 2; // number of times client codes failed to check cancelled before setTaskProgress.  Start with disabled.
    private boolean cancelChecked = false;
    private String label;
    private static final int hideInitiallyMilliSeconds = 300;
    private static final int refreshPeriodMilliSeconds = 500;
    private boolean running = false;
    private boolean finished = false;
    private Thread updateThread;
    private boolean showProgressRate;
    private JPanel thePanel;
    private Component contextComponent=null;
    private boolean componentsInitialized;
    private DasCanvasComponent parentComponent;
    private DasCanvas parentCanvas;
    private Container removeFromComponent= null; // this is the parent we need to remove the monitor from when finished.
    private static int createComponentCount = 0;
    private String currentThreadName= null;

    /**
     * string representing the units, such as "M" or "K"
     */
    private String units;

    /**
     * factor for units (e.g. 1000,1000000)
     */
    private int unitsf=1;

    @Override
    public ProgressMonitor getSubtaskMonitor(int start, int end, String label) {
        if ( label!=null ) setProgressMessage(label);
        if ( this.isFinished() ) {
            logger.info("getSubtaskMonitor called after finished");
            return new NullProgressMonitor();
        }
        return SubTaskMonitor.create( this, start, end, cancelCheckFailures < 2 );
    }
    
    @Override
    public ProgressMonitor getSubtaskMonitor( String label ) {
        if ( label!=null ) setProgressMessage(label);
        if ( this.isFinished() ) {
            logger.info("getSubtaskMonitor called after finished");
            return new NullProgressMonitor();
        }
        return SubTaskMonitor.create( this, cancelCheckFailures < 2 );
    }

    private static class MyPanel extends JPanel {

        @Override
        protected void paintComponent(Graphics g1) {
            Graphics2D g2 = (Graphics2D) g1;

            g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                    RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
            g2.setColor(new Color(0xdcFFFFFF, true));
            Rectangle rect = g2.getClipBounds();
            if (rect == null) {
                g2.fillRect(0, 0, getWidth(), getHeight());
            } else {
                g2.fillRect(rect.x, rect.y, rect.width, rect.height);
            }
            super.paintComponent(g1);
        }

        @Override
        public void print(Graphics g) {
            //System.err.println("this ought not be printed");  I think this is printed when Autoplot gets thumbnails.
            //super.print(g); //To change body of generated methods, choose Tools | Templates.
        }
        
        
    }    // provides details button, which shows who creates and who consumes the ProgressPanel
    Exception source;
    Exception consumer;

    ImageIcon cancel= new ImageIcon( DasProgressPanel.class.getResource("/images/icons/cancel14.png") );
    ImageIcon cancelGrey= new ImageIcon( DasProgressPanel.class.getResource("/images/icons/cancelGrey14.png") );
    
    protected DasProgressPanel(String label) {
        logger.log(Level.FINE, "create monitor: \"{0}\"", label);
        componentsInitialized = false;
        label= abbrevateStringEllipsis( label, LABEL_LEN_LIMIT);
        this.label = label;

        transferRateFormat = NumberFormatUtil.getDecimalFormat();
        transferRateFormat.setMaximumFractionDigits(2);
        maximumTaskPosition = -1;
        //lastRefreshTime = Integer.MIN_VALUE;
        showProgressRate = true;
        isCancelled = false;
        running = false;
        //System.err.println("here create \"" + label + "\" " + Integer.toHexString( this.hashCode() ) + " " + Thread.currentThread()  );
    }

    /**
     * returns the JPanel component.
     * @return the JPanel component.
     */
    public Component getComponent() {
        if (!componentsInitialized)
            initComponents();
        return this.thePanel;
    }
    
    /**
     * set the component where this progress monitor is understood.  This
     * is typically the DasCanvas.
     * @param window 
     */
    public void setContextComponent( Component window ) {
        this.contextComponent= window;
    }

    /**
     * provide convenient code which will center DasProgressMonitors on window.
     * @param mon the monitor, which may be a DasProgressPanel.
     * @param window the component about which to center the monitor.
     */
    public static void maybeCenter( ProgressMonitor mon, Component window ) {
        if ( mon instanceof DasProgressPanel ) {
            ((DasProgressPanel)mon).setContextComponent(window);
        }
    }
            
    /**
     * return a new progress panel with component indicating status for the component.
     * @param component the canvas component providing a context for the progress, for example a DasPlot which will load some data.
     * @param initialMessage initial message for the monitor
     * @return the new component
     */
    public static DasProgressPanel createComponentPanel(DasCanvasComponent component, String initialMessage) {
        DasProgressPanel progressPanel = new DasProgressPanel(initialMessage);
        progressPanel.parentComponent = component;
        return progressPanel;
    }
    
    /**
     * return a new progress panel with component indicating status for the canvas.
     * @param canvas the canvas client providing a context for the task.
     * @param initialMessage initial message for the monitor
     * @return 
     */
    public static DasProgressPanel createComponentPanel(DasCanvas canvas, String initialMessage) {
        DasProgressPanel progressPanel = new DasProgressPanel(initialMessage);
        progressPanel.parentCanvas = canvas;
        return progressPanel;
    }
    

    /** Returning true here keeps the progress bar from forcing the whole canvas
     * to repaint when the label of the progress bar changes.
     * @return this always returns true
     */
    public boolean isValidateRoot() {
        return true;
    }

    /**
     * this may be called from off the event thread, but it does assume that the
     * event thread is free.
     * @param label label describing the task.
     * @return a DasProgressPanel.
     */
    public static DasProgressPanel createFramed( final String label) {
        final DasProgressPanel result;
        result = new DasProgressPanel(label);
        Runnable run= new Runnable() {
            @Override
            public void run() {
                JFrame fr= new JFrame("Progress Monitor");
                result.jframe = fr;
                result.initComponents();
                fr.getContentPane().add(result.thePanel);
                fr.pack();
                fr.setVisible(false);
                fr.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            }
        };
        if ( SwingUtilities.isEventDispatchThread() ) {
            run.run();
        } else {
            SwingUtilities.invokeLater(run);
        }
        return result;
    }

    /**
     * creates a dialog object that follows a parent
     * @param parent the Window that owns this popup gui.
     * @param label label describing the task.
     * @return a DasProgressPanel.
     */
    public static DasProgressPanel createFramed( final Window parent, String label) {
        final DasProgressPanel result;
        result = new DasProgressPanel(label);
        Runnable run= new Runnable() {
            @Override
            public void run() {
                result.jframe = new JDialog(parent,"Progress Monitor");
                result.initComponents();
                JPanel lthePanel= result.thePanel;
                result.jframe.add(lthePanel);
                result.jframe.pack();
                result.jframe.setLocationRelativeTo(parent);
                result.jframe.setVisible(false);
            }
        };
        if ( SwingUtilities.isEventDispatchThread() ) {
            run.run();
        } else {
            SwingUtilities.invokeLater(run);
        }
        return result;
    }

    @Override
    public void setLabel(String label) {
        label= abbrevateStringEllipsis( label, LABEL_LEN_LIMIT);
        this.label = label;
        this.labelDirty = true;
        if (thePanel != null)
            thePanel.repaint();
    }

    @Override
    public String getLabel() {
        return label;
    }

    private void initComponents() {
        // get a stack trace so we can see what caused this.

        createComponentCount++;
        logger.log(Level.FINER, "createComponentCount={0}", createComponentCount);
        JPanel mainPanel;

        taskLabel = new JLabel();
        taskLabel.setOpaque(false);
        taskLabel.setFont(new Font("Dialog", 1, 14));
        taskLabel.setHorizontalAlignment(JLabel.CENTER);
        taskLabel.setText(label);
        logger.log( Level.FINE, "taskLabel: {0}", label);
        taskLabel.setAlignmentX(JComponent.CENTER_ALIGNMENT);

        progressMessageLabel = new JLabel() {

            @Override
            public void paint(Graphics g) {
                ((java.awt.Graphics2D) g).setRenderingHint(
                        java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                super.paint(g);
            }
        };
        progressMessageLabel.setOpaque(false);
        progressMessageLabel.setFont(new Font("Dialog", 1, 8));
        progressMessageLabel.setHorizontalAlignment(JLabel.CENTER);
        progressMessageLabel.setText(" ");
        progressMessageLabel.setAlignmentX(JComponent.CENTER_ALIGNMENT);

        progressBar = new JProgressBar();
        progressBar.setOpaque(false);
        progressBar.setPreferredSize( new Dimension(220,14) );
        progressBar.setMaximumSize(progressBar.getPreferredSize());
        progressBar.setMinimumSize(progressBar.getPreferredSize());
        progressBar.setAlignmentX(JComponent.CENTER_ALIGNMENT);

        kbLabel = new JLabel();
        kbLabel.setOpaque(false);
        kbLabel.setHorizontalAlignment(SwingConstants.CENTER);
        kbLabel.setText("0 kb");
        kbLabel.setAlignmentX(0.5F);
        kbLabel.setMaximumSize(progressBar.getPreferredSize());
        kbLabel.setMinimumSize(progressBar.getPreferredSize());
        kbLabel.setPreferredSize(progressBar.getPreferredSize());
        kbLabel.setAlignmentX(JComponent.CENTER_ALIGNMENT);

        JPanel progressBarPanel= new JPanel( new BorderLayout() );

        cancelButton = new JButton( cancelGrey );
        Dimension cs= new Dimension(20,20);
        cancelButton.setMaximumSize( cs );
        cancelButton.setMinimumSize( cs );
        cancelButton.setPreferredSize( cs );
        cancelButton.setMargin( new Insets( 2,2,2,2 ) );
        cancelButton.setToolTipText(MSG_TASK_CANNOT_BE_CANCELED);
        cancelButton.setEnabled(false);
        cancelButton.setIcon( cancelGrey );
        cancelButton.setVerticalAlignment( SwingConstants.CENTER );
        cancelButton.setOpaque(false);
        //cancelButton.setBorder(border);
        cancelButton.setFocusPainted(false);
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        mainPanel = new JPanel();
        mainPanel.setOpaque(false);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.add(taskLabel);
        mainPanel.add(progressMessageLabel);
        progressBarPanel.add(progressBar,BorderLayout.CENTER);
        progressBarPanel.add(cancelButton,BorderLayout.EAST);
        mainPanel.add(progressBarPanel);
        mainPanel.add(kbLabel);

        thePanel = new MyPanel();
        thePanel.setOpaque(false);
        thePanel.setLayout(new BorderLayout());
        thePanel.add(mainPanel, BorderLayout.CENTER);
        //thePanel.add(buttonPanel, BorderLayout.EAST);

        if (parentComponent != null) {
            thePanel.setSize(thePanel.getPreferredSize());
            int x = parentComponent.getColumn().getDMiddle();
            int y = parentComponent.getRow().getDMiddle();
            thePanel.setLocation(x - thePanel.getWidth() / 2, y - thePanel.getHeight() / 2);
            removeFromComponent= ((Container) (parentComponent.getCanvas().getGlassPane()));
            removeFromComponent.add(thePanel);
            thePanel.setVisible(false);
        } else if ( parentCanvas!=null ) {
            thePanel.setSize(thePanel.getPreferredSize());
            int x = parentCanvas.getWidth()/2;
            int y = parentCanvas.getHeight()/2;
            thePanel.setLocation(x - thePanel.getWidth() / 2, y - thePanel.getHeight() / 2);
            removeFromComponent= ((Container) (parentCanvas.getGlassPane()));
            removeFromComponent.add(thePanel);

            thePanel.setVisible(false);
        } else {
            thePanel.setSize(thePanel.getPreferredSize());
        }

        if ( this.contextComponent!=null ) {
            Component window= this.contextComponent;
            Window w= SwingUtilities.getWindowAncestor( thePanel );
            Window monitorWindow= SwingUtilities.getWindowAncestor( window );
            if ( w!=null && w!=monitorWindow ) {
                w.setLocationRelativeTo( window );
            } else if ( jframe!=null && window instanceof Window ) {
                jframe.setLocationRelativeTo( window );
            }
        }
        
        componentsInitialized = true;
    }

    StackTraceElement[] stackTrace= null;
    
    private void printStackTrace( StackTraceElement[] trace ) {
        PrintStream s= System.err;
        for (StackTraceElement traceElement : trace) {
            s.println("\tat " + traceElement);
        }
    }
    
    @Override
    public void finished() {
        //System.err.println("here finished " + Integer.toHexString( this.hashCode() ) + " " + Thread.currentThread() );
        //if ( this.progressMessageString.endsWith("t: 31" ) ) {
        //    System.err.println("*** here finished t31 " + Integer.toHexString( this.hashCode() )  );    
        //}
        if ( finished==true ) {
            logger.warning("monitor finished was called twice!");
            logger.warning("here was the first call:");
            printStackTrace( stackTrace );
            logger.warning("... and the second call:");
            new Exception().printStackTrace();
            
        } else {
            logger.fine("enter monitor finished");
            stackTrace= Thread.currentThread().getStackTrace();
        }
        running = false;
        finished = true;
        Runnable run= new Runnable() {
            @Override
            public void run() {
                if ( removeFromComponent!=null ) {
                    removeFromComponent.remove(thePanel);
                    removeFromComponent.invalidate();
                    removeFromComponent.validate();
                    Rectangle b= thePanel.getBounds();
                    removeFromComponent.repaint(b.x,b.y,b.width,b.height); 
                }
                if (jframe == null) {
                    setVisible(false);
                } else {
                    jframe.setVisible(false);
                }

            }
        };
        SwingUtilities.invokeLater(run);
        
        propertyChangeSupport.firePropertyChange( PROP_FINISHED, false, true );
    }

    /* ProgressMonitor interface */
    @Override
    public void setTaskProgress(long position) throws IllegalStateException {
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "progressPosition={0}", position);
        }

        if (isCancelled) {
            // note that the monitored process should check isCancelled before setTaskProgress, but this is no longer required.
            // If this is not done, we throw a IllegalStateException to kill the thread, and the monitored process is killed that way.
            logger.fine("setTaskProgress called when isCancelled true. consider checking isCancelled before calling setTaskProgress.");
            boolean cancelEnabled = cancelCheckFailures < 2;
            if ( !cancelEnabled ) {
                // this was introduced as a quick-n-dirty way to provide cancel, that messes up applications like
                // autoplot.  We need to be more careful about how this is used.
                throw new IllegalStateException("Operation cancelled: developers: consider checking isCancelled before calling setTaskProgress.");
            } else {
                logger.fine("setTaskProgress but isCancelled, assuming its okay to ignore.");
                // just ignore the progress update.  This should be a transitional state.
                return;
            }
        }

        if (!running) {
            if ( finished ) {
                throw new IllegalStateException("setTaskProgress called after finished");
            } else {
                throw new IllegalStateException("setTaskProgress called before started");
            }
        }

        if (position != 0 && position < currentTaskPosition) {
            logger.finest("progress position goes backwards, this is allowed.");
        }

        if (!cancelChecked) {
            // cancelCheckFailures is used to detect when if the monitored process is not checking cancelled.  If it is not, then we
            // disable the cancel button.  Note the cancel() method can still be called from elsewhere, killing the process.
            cancelCheckFailures++;
            logger.log(Level.FINER, "cancelCheckFailures={0}", cancelCheckFailures);
        }
        cancelChecked = false;  // reset for next time, isCancelled will set true.

        if (maximumTaskPosition == 0) {
            logger.fine("setTaskProgress called when taskSize is 0, just letting you know.");
        }

        currentTaskPosition = position;

        long elapsedTimeMs = System.currentTimeMillis() - taskStartedTime;
        if (elapsedTimeMs > hideInitiallyMilliSeconds && !isVisible()) {
            logger.finer("make hidden monitor visible");
            setVisible(true);
        }
    }

    @Override
    public boolean canBeCancelled() {
        return cancelChecked;
    }

    
    private void startUpdateThread() {
        Runnable run = new Runnable() {
            @Override
            public void run() {
                while (!DasProgressPanel.this.finished) {
                    try {
                        SwingUtilities.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                if ( progressBar!=null ) {
                                    updateUIComponents();
                                    thePanel.repaint();
                                }
                            }
                        });
                        Thread.sleep(refreshPeriodMilliSeconds);
                    } catch (InterruptedException ex) {
                        logger.log(Level.SEVERE, ex.getMessage(), ex);
                    } catch (InvocationTargetException ex) {
                        logger.log(Level.SEVERE, ex.getMessage(), ex);
                    } 
                }
            }
        };
        updateThread = new Thread(run, "progressMonitorUpdateThread");
        updateThread.start();
    }

    /**
     * return a string of no longer than s long.  Currently this just takes a bit of the front and end, but
     * a future version of this might look for the bit that's changing.
     * @param s the unabbreviated string.  This is any length, and contains a message
     * @param lenLimit
     * @return a string of no more that lenLimit+2 chars.  (why +2?  because the ellipse used is short.)
     */
    private String abbrevateStringEllipsis( String s, int lenLimit ) {
        if (s.length() > lenLimit ) {
            int n = s.length();
            s = s.substring(0, 10) + "..." + s.substring( n - (lenLimit-11), n );
        }
        return s;
    }

    /**
     * this must be run on the event thread
     */
    private void updateUIComponents() {
        //logger.finer("finished="+this.finished+ "  "+  Integer.toHexString( this.hashCode() ) );
        long elapsedTimeMs = System.currentTimeMillis() - taskStartedTime;

        long kb = currentTaskPosition;
        
        if (maximumTaskPosition == -1) {
            progressBar.setIndeterminate(true);
        } else {
            progressBar.setIndeterminate(false);
        }
        if (maximumTaskPosition > 0) {
            progressBar.setValue((int) (kb * 100 / (maximumTaskPosition)));
        } else {
            progressBar.setValue((int) kb % 100);
        }

        String bytesReadLabel;
        if (maximumTaskPosition > 0) {
            bytesReadLabel = "" + (kb/unitsf) + units + "/" + (maximumTaskPosition/unitsf) + "" + units;
        } else {
            bytesReadLabel = "" + kb + "";
        }

        if (progressMessageDirty) {
            progressMessageLabel.setToolTipText( "<html><b>" + label + "</b><br>" + progressMessageString + "<br>on thread " + currentThreadName );
            String s= abbrevateStringEllipsis( progressMessageString, PROGRESS_MESSAGE_LEN_LIMIT);
            progressMessageLabel.setText(s);
            progressMessageDirty = false;
        }

        if (labelDirty) {
            taskLabel.setText(label);
            taskLabel.setToolTipText( "<html><b>" + label + "</b><br>" + progressMessageString + "<br>on thread " + currentThreadName );
            labelDirty = false;
        }

        if (showProgressRate && elapsedTimeMs > 1000 && transferRateString != null) {
            kbLabel.setText(bytesReadLabel + " " + transferRateString);
        } else {
            kbLabel.setText(bytesReadLabel);
        }
        kbLabel.setToolTipText(""+kb+"/"+maximumTaskPosition);

        boolean cancelEnabled = cancelCheckFailures < 2;
        if (cancelEnabled != cancelButton.isEnabled()) {
            cancelButton.setEnabled(cancelEnabled);
            if ( cancelEnabled ) {
                logger.finer("cancel enabled");                        
                cancelButton.setIcon( cancel );
                cancelButton.setToolTipText(MSG_CANCEL_TASK);
            } else {
                logger.finer("cancel disabled");
                cancelButton.setIcon( cancelGrey );
                cancelButton.setToolTipText(MSG_TASK_CANNOT_BE_CANCELED);
            }
        }
    }

	@Deprecated
    @Override
    public void setAdditionalInfo(String s) {
        transferRateString = s;
    }

    @Override
    public long getTaskProgress() {
        return currentTaskPosition;
    }

    @Override
    public long getTaskSize() {
        return maximumTaskPosition;
    }

    @Override
    public void setTaskSize(long taskSize) {
        if (taskSize < -1) {
            throw new IllegalArgumentException("taskSize must be positive, -1, or 0, not " + taskSize);
        } else {
            if ( taskSize==0 ) taskSize=-1;
            if (componentsInitialized) {
                SwingUtilities.invokeLater( new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setIndeterminate(false);
                    }
                } );
            }
        }
        if ( taskSize>100000000 ) {
            units= "M";
            unitsf= 1000000;
        } else if ( taskSize>100000 ) {
            units= "K";
            unitsf= 1000;
        } else {
            units= "";
            unitsf= 1;
        }
        maximumTaskPosition = taskSize;
    }

    /**
     * make the progressPanel visible or hide it.  This
     * @param visible true if the progressPanel should be visible.
     */
    public void setVisible(final boolean visible) {
        if (!componentsInitialized && !visible) {
            return;
        }
        Runnable run= new Runnable() {
            @Override
            public void run() {
                if (!componentsInitialized && !finished)
                    initComponents();
                if (thePanel != null)
                    thePanel.setVisible(visible);
                if (DasProgressPanel.this.jframe != null)
                    DasProgressPanel.this.jframe.setVisible(visible);
            }
        };
        if ( SwingUtilities.isEventDispatchThread() ) {
            run.run();
        } else {
            SwingUtilities.invokeLater(run);
        }

        if ( visible ) {
            if ( updateThread==null ) startUpdateThread();
        }
    }

    public boolean isVisible() {
        return (!componentsInitialized || thePanel.isVisible());
    }
    
    public final static String PROP_STARTED="started";

    public final static String PROP_FINISHED="finished";
    
    /**
     * true if the task is cancelled.  Note cancelled will be set
     * before finished.
     */
    public final static String PROP_CANCELLED="cancelled";
    
    @Override
    public void started() {
        taskStartedTime = System.currentTimeMillis();
        running = true;
        currentThreadName= Thread.currentThread().getName(); 

        if (hideInitiallyMilliSeconds > 0) {
            setVisible(false);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(hideInitiallyMilliSeconds);
                    } catch (InterruptedException e) {
                    }
                    if (running) {
                        logger.log(Level.FINE, "hide time={0}", (System.currentTimeMillis() - taskStartedTime));
                        if ( contextComponent!=null ) {
                            setContextComponent(contextComponent); // reset the position.
                        }
                        setVisible(true);
                    }
                }
            }, "progressPanelUpdateThread").start();
        } else {
            setVisible(true);
        }

        propertyChangeSupport.firePropertyChange( PROP_STARTED, false, true );
        
        // cancel() might have been called before we got here, so check it.
        if (isCancelled)
            return;
        if (maximumTaskPosition > 0)
            setTaskProgress(0);
    }

    @Override
    public void cancel() {
        //logger.finest("cancel "+ Integer.toHexString( this.hashCode() ) );
        isCancelled = true;
        
        propertyChangeSupport.firePropertyChange( PROP_CANCELLED, false, true );
        
        finished();
    }

    @Override
    public boolean isCancelled() {
        cancelCheckFailures = 0;
        logger.log(Level.FINER, "cancelCheckFailures={0}", cancelCheckFailures);
        cancelChecked = true;
        return isCancelled;
    }

    public Exception getSource() {
        return source;
    }

    public Exception getConsumer() {
        return consumer;
    }

    /**
     * Setter for property showProgressRate.
     * @param showProgressRate New value of property showProgressRate.
     */
    public void setShowProgressRate(boolean showProgressRate) {
        this.showProgressRate = showProgressRate;
    }

    @Override
    public String toString() {
        if (isCancelled) {
            return "cancelled";
        } else if (finished) {
            return "finished";
        } else if (running) {
            return "" + currentTaskPosition + " of " + this.maximumTaskPosition;
        } else {
            return "waiting for start";
        }
    }

    @Override
    public void setProgressMessage(String message) {
        logger.finest("setProgressMessage");
        //if ( this.message.endsWith("t: 31") ) {
        //    System.err.println("setProgressMessage "+ Integer.toHexString( this.hashCode() ) );
        //}
        this.progressMessageString = message;
        this.progressMessageDirty = true;
    }

    @Override
    public boolean isStarted() {
        return running;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }
    
    private transient final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }
    
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
    }    

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
    }

}
