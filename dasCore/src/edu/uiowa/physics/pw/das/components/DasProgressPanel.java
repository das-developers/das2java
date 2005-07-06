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

package edu.uiowa.physics.pw.das.components;

import edu.uiowa.physics.pw.das.*;
import edu.uiowa.physics.pw.das.graph.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import javax.swing.*;
import javax.swing.border.*;
import edu.uiowa.physics.pw.das.util.DasProgressMonitor;
import java.util.*;

/**
 *
 * @author  eew
 */
public class DasProgressPanel extends JPanel implements DasProgressMonitor {
    
    private long taskStartedTime;
    private long currentTaskPosition;
    private long maximumTaskPosition;
    private DecimalFormat transferRateFormat;
    private String transferRateString;
    private JLabel messageLabel;
    private JLabel kbLabel;
    private JProgressBar progressBar;
    private JFrame jframe;  // created when createFramed() is used.
    private boolean isCancelled = false;
    private int cancelCheckFailures= 0; // number of times client codes failed to check cancelled before setTaskProgress.
    private boolean cancelChecked= false;  
    private String label;
    private static final int hideInitiallyMilliSeconds= 1500;
    private long lastTaskTime;
    private boolean running = false;
    private long lastRefreshTime;
    private ArrayList refreshTimeQueue;
    
    /**
     * Holds value of property showProgressRate.
     */
    private boolean showProgressRate;
    
    /** Creates new form DasProgressPanel */
    
    public DasProgressPanel(String label) {
        this.label = label;
        setOpaque(false);
        initComponents();
        transferRateFormat= new DecimalFormat();
        transferRateFormat.setMaximumFractionDigits(2);        
        maximumTaskPosition = -1;
        lastTaskTime= Integer.MAX_VALUE;
        lastRefreshTime= Integer.MIN_VALUE;
        showProgressRate= true;
    }
    
    public static DasProgressPanel createComponentPanel( DasCanvasComponent component, String initialMessage ) {
        DasProgressPanel progressPanel= new DasProgressPanel( initialMessage );
        
        progressPanel.setSize(progressPanel.getPreferredSize());
        
        int x= component.getColumn().getDMiddle();
        int y= component.getRow().getDMiddle();
        
        progressPanel.setLocation( x - progressPanel.getWidth()/2, y - progressPanel.getHeight()/2 );
        
        ((Container)(component.getCanvas().getGlassPane())).add(progressPanel);
        
        progressPanel.setVisible(false); 
        
        return progressPanel;
        
    }
    
    /** Returning true here keeps the progress bar from forcing the whole canvas
     * to repaint when the label of the progress bar changes.
     */
    public boolean isValidateRoot() {
        return true;
    }
    
    public static DasProgressPanel createFramed( String label ) {
        DasProgressPanel result;
        result= new DasProgressPanel( label );
        result.jframe= new JFrame("Das Progress Monitor");
        result.jframe.getContentPane().add( result );
        result.jframe.pack();
        result.jframe.setVisible(true);
        result.jframe.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        return result;
    }
    
    public void setLabel(String label) {
        messageLabel.setText(label);
        this.label = label;
        repaint();
    }
    
    public String getLabel() {
        return label;
    }
    
    private void initComponents() {
        JPanel mainPanel, buttonPanel;
        JButton cancelButton;
        
        messageLabel = new JLabel();
        messageLabel.setOpaque(false);
        messageLabel.setFont(new Font("Dialog", 1, 18));
        messageLabel.setHorizontalAlignment(JLabel.CENTER);
        messageLabel.setText(label);
        messageLabel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        
        progressBar = new JProgressBar();
        progressBar.setOpaque(false);
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
        
        mainPanel = new JPanel();
        mainPanel.setOpaque(false);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.add(messageLabel);
        mainPanel.add(progressBar);
        mainPanel.add(kbLabel);
        
        Border lineBorder = new LineBorder(Color.BLACK, 2);
        Border emptyBorder = new EmptyBorder(2, 2, 2, 2);
        CompoundBorder border = new CompoundBorder(lineBorder, emptyBorder);
        
        cancelButton = new JButton("cancel");
        cancelButton.setOpaque(false);
        cancelButton.setBorder(border);
        cancelButton.setFocusPainted(false);
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });
        
        buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        buttonPanel.add(cancelButton);
        
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    public void finished() {
        running = false;
        lastTaskTime= System.currentTimeMillis()-taskStartedTime;
        if ( jframe==null ) {
            setVisible(false);
        } else {
            jframe.dispose();
        }
    }
    
    /* ProgressMonitor interface */
    public void setTaskProgress(long position) throws IllegalStateException {
        DasApplication.getDefaultApplication().getLogger(DasApplication.SYSTEM_LOG).finest( "progressPosition="+position );
        
        if ( position!=0 && position<currentTaskPosition ) {
            DasApplication.getDefaultApplication().getLogger(DasApplication.SYSTEM_LOG).finest( "progress position goes backwards" );
        }

        if (!cancelChecked) {
            cancelCheckFailures++;
            if ( cancelCheckFailures>10 ) {
                DasApplication.getDefaultApplication().getLogger().fine("setTaskProgress called when isCancelled true, check isCancelled before calling setTaskProgress?");
                throw new IllegalStateException("Operation cancelled: developers: check isCancelled before calling setTaskProgress");
            }
        }
        cancelChecked= false;  // reset for next time, isCancelled will set true.
        
        long elapsedTimeMs= System.currentTimeMillis()-taskStartedTime;
        if ( elapsedTimeMs > hideInitiallyMilliSeconds && !isVisible()) {
            setVisible(true);
        }
        
        currentTaskPosition = position;
        long kb = currentTaskPosition ;
        
        
        if ( maximumTaskPosition > 0 ) {
            progressBar.setValue( (int) (kb * 100 / (maximumTaskPosition) ) );
        } else {
            progressBar.setValue( (int) kb % 100 );
        }
        
        String bytesReadLabel;
        if ( maximumTaskPosition > 0 ) {
            bytesReadLabel = "" + kb + "/" + maximumTaskPosition + "";
        } else {
            bytesReadLabel= "" + kb + "";
        }
        
        if ( showProgressRate && elapsedTimeMs > 1000 && transferRateString!=null ) {
            double transferRate = ((double)position * 1000) / ( elapsedTimeMs );
            kbLabel.setText(bytesReadLabel+" "+transferRateString );
        } else {
            kbLabel.setText(bytesReadLabel);
        }        
        
        long tnow;
        if ( (tnow=System.currentTimeMillis()) - lastRefreshTime > 100 ) {            
            if (Toolkit.getDefaultToolkit().getSystemEventQueue().isDispatchThread()) {
                paintImmediately(0, 0, getWidth(), getHeight());
            }
            else {
                repaint();
            }
            lastRefreshTime= tnow;
        }
    }
    
    public void setAdditionalInfo( String s ) {
        transferRateString= s;
    }
    
    public long getTaskProgress() {
        return currentTaskPosition;
    }
    
    public void setTaskSize(long taskSize) {
        if (taskSize == -1) {
            progressBar.setIndeterminate(true);            
        } else if ( taskSize<1 ) {
            throw new IllegalArgumentException( "taskSize must be positive, or -1" );
        } else {
            progressBar.setIndeterminate(false);
        }
        maximumTaskPosition = taskSize;
    }
    
    public void started() {
        taskStartedTime= System.currentTimeMillis();
        isCancelled = false;
        running = true;
        DasApplication.getDefaultApplication().getLogger(DasApplication.SYSTEM_LOG).fine("lastTaskTime="+lastTaskTime);
        setTaskProgress(0);
        if ( lastTaskTime>hideInitiallyMilliSeconds*2.0 ) {
            setVisible(true);
        } else {
            setVisible(false);
            new Thread( new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(hideInitiallyMilliSeconds);
                    } catch ( InterruptedException e ) { };
                    if (running) {
                        DasApplication.getDefaultApplication().getLogger(DasApplication.SYSTEM_LOG).fine("hide time="+(System.currentTimeMillis()-taskStartedTime) );
                        setTaskProgress(getTaskProgress());
                    }
                }
            } ).start();
        }
    }
    
    public void cancel() {
        isCancelled = true;
        finished();
    }
    
    public boolean isCancelled() {
        cancelCheckFailures=0;
        cancelChecked= true;
        return isCancelled;
    }
    
    protected void paintComponent(Graphics g1) {
        Graphics2D g2= ( Graphics2D) g1;
        
        g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
        RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
        g2.setColor(new Color(0xdcFFFFFF, true));
        Rectangle rect = g2.getClipBounds();
        if (rect == null) {
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
        else {
            g2.fillRect(rect.x, rect.y, rect.width, rect.height);
        }
        
        super.paintComponent(g1);
    }
    
    /**
     * Setter for property showProgressRate.
     * @param showProgressRate New value of property showProgressRate.
     */
    public void setShowProgressRate(boolean showProgressRate) {
        this.showProgressRate = showProgressRate;
    }
    
}
