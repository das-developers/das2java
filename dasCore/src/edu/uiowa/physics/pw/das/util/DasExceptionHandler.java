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

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 *
 * @author  jbf
 */
public final class DasExceptionHandler {
    
    private DasExceptionHandler() {
    }
    
    public static void handle(Throwable t) {
        if (System.getProperty("java.awt.headless", "").equals("true")) {
            edu.uiowa.physics.pw.das.util.DasDie.println(edu.uiowa.physics.pw.das.util.DasDie.CRITICAL, t.toString());
        }
        else {
            showExceptionDialog(t);
        }
    }
    
    public static void showExceptionDialog(final Throwable t) {
        final JDialog dialog = new JDialog();
        dialog.setTitle("Error");
        dialog.setModal(true);
        dialog.setResizable(false);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        JComponent message;
        String errorMessage = t.getMessage();

        if (errorMessage == null) {
            JTextField field = new JTextField(t.toString());
            field.setEditable(false);
            field.setOpaque(false);
            message = field;
        }
        else if (java.util.regex.Pattern.matches(".*<[Hh][Tt][Mm][Ll]>.*</[Hh][Tt][Mm][Ll]>.*", errorMessage)) {
            JEditorPane pane = new JEditorPane("text/html", errorMessage);
            pane.setEditable(false);
            message = new JScrollPane(pane);
        }
        else {
            JTextArea area = new JTextArea(t.getMessage());
            area.setLineWrap(true);
            area.setEditable(false);
            area.setOpaque(false);
            message = new JScrollPane(area);
        }
        
        message.setBorder(new javax.swing.border.EmptyBorder(10, 10, 10, 10));
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(message, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton ok = new JButton("Ok");
        final JToggleButton details = new JToggleButton("Show Details");
        buttonPanel.add(ok);
        buttonPanel.add(details);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.getContentPane().add(mainPanel, BorderLayout.CENTER);
        
        JTextArea traceArea = new JTextArea(10, 40);
        traceArea.setLineWrap(false);
        traceArea.setEditable(false);

        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        t.printStackTrace(writer);
        writer.close();
        traceArea.setText(stringWriter.toString());
        traceArea.setTabSize(4);
        
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
                t.printStackTrace(System.err);
            }
        });
        
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }
    
}
