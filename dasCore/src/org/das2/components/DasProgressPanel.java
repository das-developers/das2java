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
import java.awt.Dialog;
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
import org.das2.graph.DasCanvas;

/**
 * ProgressMonitor component used throughout das2.
 *
 * Here's an Autoplot script demoing its operation:
 * monitor.setTaskSize( 100 )
 * monitor.started()
 *
 * for i in range(100):
 *   if ( i>50 and monitor.isCancelled() ):
 *     raise Exception('cancel pressed')
 *   print i
 *   java.lang.Thread.sleep(100)
 *   monitor.setTaskProgress(i)
 *
 * monitor.finished()
 *
 * @author  eew
 */
public class DasProgressPanel implements ProgressMonitor {

    private static final Logger logger = DasLogger.getLogger(DasLogger.SYSTEM_LOG);

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
    private int cancelCheckFailures = 0; // number of times client codes failed to check cancelled before setTaskProgress.
    private boolean cancelChecked = false;
    private String label;
    private static final int hideInitiallyMilliSeconds = 300;
    private static final int refreshPeriodMilliSeconds = 500;
    private boolean running = false;
    private boolean finished = false;
    private Thread updateThread;
    private boolean showProgressRate;
    private JPanel thePanel;
    private boolean componentsInitialized;
    private DasCanvasComponent parentComponent;
    private DasCanvas parentCanvas;
    private Container removeFromComponent= null; // this is the parent we need to remove the monitor from when finished.
    private static int createComponentCount = 0;

    /**
     * string representing the units, such as "M" or "K"
     */
    private String units;

    /**
     * factor for units
     */
    private int unitsf=1;

    class MyPanel extends JPanel {

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
    }    // provides details button, which shows who creates and who consumes the ProgressPanel
    Exception source;
    Exception consumer;

    ImageIcon cancel= new ImageIcon( DasProgressPanel.class.getResource("/images/cancel14.png") );
    ImageIcon cancelGrey= new ImageIcon( DasProgressPanel.class.getResource("/images/cancelGrey14.png") );
    
    public DasProgressPanel(String label) {

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
    }

    /**
     * returns the JPanel component.
     */
    public Component getComponent() {
        if (!componentsInitialized)
            initComponents();
        return this.thePanel;
    }

    public static DasProgressPanel createComponentPanel(DasCanvasComponent component, String initialMessage) {
        DasProgressPanel progressPanel = new DasProgressPanel(initialMessage);
        progressPanel.parentComponent = component;
        return progressPanel;
    }
    
    public static DasProgressPanel createComponentPanel(DasCanvas canvas, String initialMessage) {
        DasProgressPanel progressPanel = new DasProgressPanel(initialMessage);
        progressPanel.parentCanvas = canvas;
        return progressPanel;
    }
    

    /** Returning true here keeps the progress bar from forcing the whole canvas
     * to repaint when the label of the progress bar changes.
     */
    public boolean isValidateRoot() {
        return true;
    }

    /**
     * this may be called from off the event thread, but it does assume that the
     * event thread is free.
     * @param label
     * @return
     */
    public static DasProgressPanel createFramed( final String label) {
        final DasProgressPanel result;
        result = new DasProgressPanel(label);
        Runnable run= new Runnable() {
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
     * @param parent
     * @param label
     * @return
     */
    public static DasProgressPanel createFramed( final Window parent, String label) {
        final DasProgressPanel result;
        result = new DasProgressPanel(label);
        Runnable run= new Runnable() {
            public void run() {
                if ( parent instanceof JFrame ) {
                    result.jframe = new JDialog((JFrame)parent,"Progress Monitor");
                } else if ( parent instanceof Dialog ) {
                    result.jframe = new JDialog((Dialog)parent,"Progress Monitor");
                }

                result.initComponents();
                result.jframe.add(result.thePanel);
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

    public void setLabel(String label) {
        label= abbrevateStringEllipsis( label, LABEL_LEN_LIMIT);
        this.label = label;
        this.labelDirty = true;
        if (thePanel != null)
            thePanel.repaint();
    }

    public String getLabel() {
        return label;
    }

    private void initComponents() {
        // get a stack trace so we can see what caused this.

        createComponentCount++;
        logger.log(Level.FINER, "createComponentCount={0}", createComponentCount);
        JPanel mainPanel, buttonPanel;

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
        progressBar.setPreferredSize( new Dimension(150,14) );
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

        //cancelButton = new JButton("cancel");
        cancelButton = new JButton( cancelGrey );
        Dimension cs= new Dimension(20,20);
        cancelButton.setMaximumSize( cs );
        cancelButton.setMinimumSize( cs );
        cancelButton.setPreferredSize( cs );
        cancelButton.setMargin( new Insets( 2,2,2,2 ) );
        cancelButton.setToolTipText(MSG_TASK_CANNOT_BE_CANCELED);
        cancelButton.setEnabled(false);
        cancelButton.setVerticalAlignment( SwingConstants.CENTER );
        cancelButton.setOpaque(false);
        //cancelButton.setBorder(border);
        cancelButton.setFocusPainted(false);
        cancelButton.addActionListener(new ActionListener() {
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

//        buttonPanel = new JPanel( new FlowLayout(FlowLayout.LEFT) );
//        buttonPanel.setOpaque(false);
//
//        if (useDetails)
//            buttonPanel.add(detailsButton);
//        buttonPanel.add(cancelButton);

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
        }

        componentsInitialized = true;
    }

    public synchronized void finished() {
        running = false;
        finished = true;
        Runnable run= new Runnable() {
            public void run() {
                if ( removeFromComponent!=null ) {
                    removeFromComponent.remove(thePanel);
                }
                if (jframe == null) {
                    setVisible(false);
                } else {
                    jframe.dispose();
                }

            }
        };
        SwingUtilities.invokeLater(run);
    }

    /* ProgressMonitor interface */
    public void setTaskProgress(long position) throws IllegalStateException {
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "progressPosition={0}", position);
        }

        if (isCancelled) {
            // note that the monitored process should check isCancelled before setTaskProgress, but this is no longer required.
            // If this is not done, we throw a IllegalStateException to kill the thread, and the monitored process is killed that way.
            logger.fine("setTaskProgress called when isCancelled true. consider checking isCancelled before calling setTaskProgress.");
            throw new IllegalStateException("Operation cancelled: developers: consider checking isCancelled before calling setTaskProgress.");
        }

        if (!running) {
            throw new IllegalStateException("setTaskProgress called before started");
        }

        if (position != 0 && position < currentTaskPosition) {
            logger.finest("progress position goes backwards, this is allowed.");
        }

        if (!cancelChecked) {
            // cancelCheckFailures is used to detect when if the monitored process is not checking cancelled.  If it is not, then we
            // disable the cancel button.  Note the cancel() method can still be called from elsewhere, killing the process.
            cancelCheckFailures++;
        }
        cancelChecked = false;  // reset for next time, isCancelled will set true.

        if (maximumTaskPosition == 0) {
            throw new IllegalArgumentException("when taskSize is 0, setTaskProgress must not be called.");
        }

        currentTaskPosition = position;

        long elapsedTimeMs = System.currentTimeMillis() - taskStartedTime;
        if (elapsedTimeMs > hideInitiallyMilliSeconds && !isVisible()) {
            setVisible(true);
        }
    }

    private void startUpdateThread() {
        Runnable run = new Runnable() {

            public void run() {
                while (!DasProgressPanel.this.finished) {
                    try {
                        SwingUtilities.invokeAndWait(new Runnable() {
                            public void run() {
                                updateUIComponents();
                                thePanel.repaint();
                            }
                        });
                        Thread.sleep(refreshPeriodMilliSeconds);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(DasProgressPanel.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (InvocationTargetException ex) {
                        Logger.getLogger(DasProgressPanel.class.getName()).log(Level.SEVERE, null, ex);
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
            bytesReadLabel = "" + (kb/unitsf) + "/" + (maximumTaskPosition/unitsf) + "" + units;
        } else {
            bytesReadLabel = "" + kb + "";
        }

        if (progressMessageDirty) {
            progressMessageLabel.setToolTipText( progressMessageString );
            String s= abbrevateStringEllipsis( progressMessageString, PROGRESS_MESSAGE_LEN_LIMIT);
            progressMessageLabel.setText(s);
            progressMessageDirty = false;
        }

        if (labelDirty) {
            taskLabel.setText(label);
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
                cancelButton.setIcon( cancel );
                cancelButton.setToolTipText(MSG_CANCEL_TASK);
            } else {
                cancelButton.setIcon( cancelGrey );
                cancelButton.setToolTipText(MSG_TASK_CANNOT_BE_CANCELED);
            }
        }
    }

	@Deprecated
    public void setAdditionalInfo(String s) {
        transferRateString = s;
    }

    public long getTaskProgress() {
        return currentTaskPosition;
    }

    public long getTaskSize() {
        return maximumTaskPosition;
    }

    public void setTaskSize(long taskSize) {
        if (taskSize < -1) {
            throw new IllegalArgumentException("taskSize must be positive, -1, or 0, not " + taskSize);
        } else {
            if (componentsInitialized) {
                SwingUtilities.invokeLater( new Runnable() {
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
        }
        maximumTaskPosition = taskSize;
    }

    /**
     * the the progressPanel visible or hide it.  This
     * @param visible
     */
    public synchronized void setVisible(final boolean visible) {
        if (!componentsInitialized && !visible)
            return;
        Runnable run= new Runnable() {
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

    public void started() {
        taskStartedTime = System.currentTimeMillis();
        running = true;

        if (hideInitiallyMilliSeconds > 0) {
            setVisible(false);
            new Thread(new Runnable() {

                public void run() {
                    try {
                        Thread.sleep(hideInitiallyMilliSeconds);
                    } catch (InterruptedException e) {
                    }
                    if (running) {
                        logger.log(Level.FINE, "hide time={0}", (System.currentTimeMillis() - taskStartedTime));
                        setVisible(true);
                    }
                }
            }, "progressPanelUpdateThread").start();
        } else {
            setVisible(true);
        }

        // cancel() might have been called before we got here, so check it.
        if (isCancelled)
            return;
        if (maximumTaskPosition > 0)
            setTaskProgress(0);
    }

    public void cancel() {
        isCancelled = true;
        finished();
    }

    public boolean isCancelled() {
        cancelCheckFailures = 0;
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

    public void setProgressMessage(String message) {
        this.progressMessageString = message;
        this.progressMessageDirty = true;
    }

    public boolean isStarted() {
        return running;
    }

    public boolean isFinished() {
        return finished;
    }
}
