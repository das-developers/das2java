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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import javax.swing.*;
import javax.swing.border.*;
import edu.uiowa.physics.pw.das.util.DasProgressMonitor;

/**
 *
 * @author  eew
 */
public class DasProgressPanel extends JPanel implements DasProgressMonitor {
    
    private long taskStartedTime;
    private int currentTaskPosition;
    private int maximumTaskPosition;
    private DecimalFormat transferRateFormat;
    private JLabel messageLabel;
    private JLabel kbLabel;
    private JProgressBar progressBar;
    private JFrame jframe;  // created when createFramed() is used.
    private boolean isCancelled = false;
    private String label;
    
    /** Creates new form DasProgressPanel */
    public DasProgressPanel() {
        this("Loading Data Set");
    }
    
    public DasProgressPanel(String label) {
        this.label = label;
        setOpaque(false);
        initComponents();
        transferRateFormat= new DecimalFormat();
        transferRateFormat.setMaximumFractionDigits(2);
        maximumTaskPosition = -1;
    }
    
    public static DasProgressPanel createFramed() {
        DasProgressPanel result;
        result= new DasProgressPanel();
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
        System.out.println(progressBar.getMaximum());
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
        if ( jframe==null ) {
            setVisible(false);
        } else {
            jframe.dispose();
        }
    }
    
    /* ProgressMonitor interface */
    public void setTaskProgress(int position) throws IllegalStateException {
        if (isCancelled) {
            throw new IllegalStateException("Operation cancelled");
        }
        long elapsedTimeMs= System.currentTimeMillis()-taskStartedTime;
        if ( elapsedTimeMs > 400 ) {
            setVisible(true);
        }
        
        currentTaskPosition = position;
        int kb = currentTaskPosition / 1024;
        
        
        if ( maximumTaskPosition > 0 ) {
            progressBar.setValue(kb * 100 / (maximumTaskPosition/1024));
        } else {
            progressBar.setValue(kb % 100);
        }
        
        String bytesReadLabel;
        if ( maximumTaskPosition > 0 ) {
            bytesReadLabel = "" + kb + "/" + maximumTaskPosition/1024 + "kb";
        } else {
            bytesReadLabel= "" + kb + "kb";
        }
        
        if ( elapsedTimeMs > 1000 ) {
            double transferRate = ((double)position * 1000) / (1024 * elapsedTimeMs);
            kbLabel.setText(bytesReadLabel+" ("+transferRateFormat.format(transferRate)+"kb/s)");
        } else {
            kbLabel.setText(bytesReadLabel);
        }
        
        if (Toolkit.getDefaultToolkit().getSystemEventQueue().isDispatchThread()) {
            paintImmediately(0, 0, getWidth(), getHeight());
        }
        else {
            repaint();
        }
    }
    
    public int getTaskProgress() {
        return currentTaskPosition;
    }
    
    public void setTaskSize(int taskSize) {
        if (taskSize == -1) {
            progressBar.setIndeterminate(true);
        }
        else {
            progressBar.setIndeterminate(false);
        }
        maximumTaskPosition = taskSize;
    }
    
    public void started() {
        taskStartedTime= System.currentTimeMillis();
        currentTaskPosition = 0;
        isCancelled = false;
        setVisible(true);
    }
    
    public void cancel() {
        isCancelled = true;
        setVisible(false);
    }
    
    public boolean isCancelled() {
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
    
}
