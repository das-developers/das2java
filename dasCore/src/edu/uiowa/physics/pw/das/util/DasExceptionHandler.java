/* File: DasExceptionHandler.java
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

package edu.uiowa.physics.pw.das.util;

import edu.uiowa.physics.pw.das.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 *
 * @author  jbf
 */
public final class DasExceptionHandler {

    //private static JDialog dialog;
    //private static JTextArea messageArea;
    //private static JTextArea traceArea;
    private static final String UNCAUGHT = "An unexpected error has occurred.  " +
        "The system may not be able to recover properly.  Please report this " +
        "error to the Das2 bug database at http://bugs-pw.physics.uiowa.edu/." +
        "  Please include all error information and a description of how you" +
        " encountered the error.  For your convenience, you may click the " +
        "\"Show Details\" button then click the \"Save to file\" button to save" +
        " all the relevant error messages to a file.\n";
    
    private DasExceptionHandler() {
    }
    
    public static void handle(Throwable t) {
        if ( DasApplication.getDefaultApplication().isHeadless() ) {
            edu.uiowa.physics.pw.das.util.DasDie.println(edu.uiowa.physics.pw.das.util.DasDie.CRITICAL, t.toString());
        }
        else {
            showExceptionDialog(t, "");
        }
    }
    
    public static void handleUncaught(Throwable t) {
        if ( DasApplication.getDefaultApplication().isHeadless() ) {
            t.printStackTrace();
        }
        else {
            showExceptionDialog(t, UNCAUGHT);
        }
    }

    private static void showExceptionDialog(Throwable t, String extraInfo) {
        String errorMessage = extraInfo + t.getClass().getName() + "\n"
            + (t.getMessage() == null ? "" : t.getMessage());
        final JDialog dialog = new JDialog();
        dialog.setTitle("Error");
        dialog.setModal(false);
        dialog.setResizable(false);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        final JTextArea messageArea = new JTextArea(10, 40);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setEditable(false);
        messageArea.setText(errorMessage);
        JScrollPane message = new JScrollPane(messageArea);
        message.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(message, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton ok = new JButton("Ok");
        final JToggleButton details = new JToggleButton("Show Details");
        buttonPanel.add(ok);
        buttonPanel.add(details);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.getContentPane().add(mainPanel, BorderLayout.CENTER);
        
        final JTextArea traceArea = new JTextArea(10, 40);
        traceArea.setLineWrap(false);
        traceArea.setEditable(false);
        traceArea.setTabSize(4);
        
        StringWriter writer = new StringWriter();
        t.printStackTrace(new PrintWriter(writer));
        traceArea.setText(writer.toString());
        
        final JPanel stackPane = new JPanel(new BorderLayout());
        stackPane.add(new JScrollPane(traceArea), BorderLayout.NORTH);
        stackPane.setBorder(new javax.swing.border.EmptyBorder(10, 10, 10, 10));
        JPanel buttonPanel2 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel2.setBorder(new javax.swing.border.EmptyBorder(10, 0, 0, 0));
        JButton dump = new JButton("Dump to STDERR");
        buttonPanel2.add(dump);
        stackPane.add(dump, BorderLayout.SOUTH);
        Dimension size = message.getPreferredSize();
        size.width = stackPane.getPreferredSize().width;
        message.setPreferredSize(size);
        
        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });
        
        details.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (details.isSelected()) {
                    details.setText("Less Details");
                    dialog.getContentPane().add(stackPane, BorderLayout.SOUTH);
                    dialog.pack();
                }
                else {
                    details.setText("More Details");
                    dialog.getContentPane().remove(stackPane);
                    dialog.pack();
                }
            }
        });
        
        dump.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String text = traceArea.getText();
                System.err.print(text);
            }
        });

        /*
        save.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    JFileChooser chooser = new JFileChooser();
                    int result = chooser.showSaveDialog(dialog);
                    if (result == chooser.APPROVE_OPTION) {
                        File selected = chooser.getSelectedFile();
                        FileWriter writer = new FileWriter(selected);
                    }
                }
                catch (IOException ioe) {
                    handle(ioe);
                }
            }
        });
         */
        
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }
    
}
