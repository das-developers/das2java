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
    private boolean isCancelled = false;
    
    /** Creates new form DasProgressPanel */
    public DasProgressPanel() {
        setOpaque(false);
        initComponents();
        transferRateFormat= new DecimalFormat();
        transferRateFormat.setMaximumFractionDigits(2);
        maximumTaskPosition = -1;
    }
    
    private void initComponents() {
        JPanel mainPanel, buttonPanel;
        JButton cancelButton;

        messageLabel = new JLabel();
        messageLabel.setOpaque(false);
        messageLabel.setFont(new Font("Dialog", 1, 18));
        messageLabel.setHorizontalAlignment(JLabel.CENTER);
        messageLabel.setText("Loading Data Set");
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
        setVisible(false);
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
            double transferRate = (double)(position * 1000) / (1024 * elapsedTimeMs);
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
        maximumTaskPosition = taskSize;
    }
    
    public void started() {
        taskStartedTime= System.currentTimeMillis();
        currentTaskPosition = 0;
        isCancelled = false;
    }
    
    public void cancel() {
        isCancelled = true;
        setVisible(false);
    }
    
    public boolean isCancelled() {
        return isCancelled;
    }
    
}
