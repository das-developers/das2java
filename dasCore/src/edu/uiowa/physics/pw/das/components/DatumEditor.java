/* File: DatumEditor.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on October 1, 2003, 9:20 AM
 *      by Edward West <eew@space.physics.uiowa.edu>
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

import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.datum.format.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


/**
 *
 * @author  Edward West
 */
public class DatumEditor extends JComponent {
    
    private JTextField editor;
    private JButton unitsButton;
    private Units units = Units.dimensionless;
    private ActionListener actionListener;
    
    /** Creates a new instance of DatumEditor */
    public DatumEditor() {
        initComponents();
        installListeners();
        initToolTips();
        setFocusable(true);
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        
        editor = new JTextField(8);
        editor.setFocusable(false);
        add(editor, BorderLayout.CENTER);
        
        unitsButton = new JButton();
        unitsButton.setFocusable(false);
        add(unitsButton, BorderLayout.EAST);
    }
    
    private void installListeners() {
        UniversalListener ul = new UniversalListener();
        editor.addMouseListener(ul);
        unitsButton.addMouseListener(ul);
        addKeyListener(ul);
        addFocusListener(ul);
    }
    
    private void initToolTips() {
        ToolTipManager.sharedInstance().registerComponent(this);
    }
    
    public void setColumns(int columns) {
        editor.setColumns(columns);
    }
    
    public void setValue(Datum datum) {
        if (datum.getUnits() instanceof TimeLocationUnits) {
            editor.setText(TimeDatumFormatter.DEFAULT.format(datum));
        }
        else {
            editor.setText(datum.getFormatter().format(datum));
        }
        setUnits(datum.getUnits());
    }
    
    public Datum getValue() throws java.text.ParseException {
        return units.parse(editor.getText());
    }
    
    public void setUnits(Units units) {
        if (units instanceof TimeLocationUnits) {
            unitsButton.setVisible(false);
        }
        else {
            unitsButton.setVisible(true);
            unitsButton.setText(units.toString());
            unitsButton.setToolTipText(units.toString());
        }
        this.units = units;
    }
    
    public Units getUnits() {
        return units;
    }
    
    private void fireActionPerformed() {
        if (actionListener != null) {
            actionListener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "DatumEditor"));
        }
    }
    
    public void addActionListener(ActionListener al) {
        actionListener = AWTEventMulticaster.add(actionListener, al);
    }
    
    public void removeActionListener(ActionListener al) {
        actionListener = AWTEventMulticaster.remove(actionListener, al);
    }
    
    /* Why all one class?  To reduce the number of object created.
     * The purposes of the listeners are not necessarily related and it is
     * generally not a good idea to implement multiple interfaces unless there
     * is some sort of correlation between them.  It is okay to do that here
     * since this class is not a part of the public interface.
     */
    private class UniversalListener implements MouseListener, KeyListener, FocusListener {
        
        /** Listens for focus events on the DatumEditor and sets the editor
         * caret visible when focus is gained.
         */
        public void focusGained(FocusEvent e) {
            editor.getCaret().setVisible(true);
            editor.getCaret().setSelectionVisible(true);
        }
        
        /** Listens for focus events on the DatumEditor and sets the editor
         * caret invisible when focus is lost.
         */
        public void focusLost(FocusEvent e) {
            editor.getCaret().setVisible(false);
            editor.getCaret().setSelectionVisible(false);
        }
        
        /** All key events are forwarded to the editor.  Except for keyPresses
         * for VK_ENTER
         */
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                fireActionPerformed();
            }
            else {
                forwardKeyEvent(e);
            }
        }
        public void keyReleased(KeyEvent e) { forwardKeyEvent(e); }
        public void keyTyped(KeyEvent e) { forwardKeyEvent(e); }
        private void forwardKeyEvent(KeyEvent e) {
            e.setSource(editor);
            editor.dispatchEvent(e);
        }
        
        /** Request focus when sub-components are clicked */
        public void mousePressed(MouseEvent e) {
            requestFocusInWindow();
        }
        
        /** Unused */
        public void mouseEntered(MouseEvent e) {}
        public void mouseExited(MouseEvent e) {}
        public void mouseClicked(MouseEvent e) {}
        public void mouseReleased(MouseEvent e) {}
        
    }
    
    public static void main(String[] args) {
        
        DatumEditor de1 = new DatumEditor();
        de1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("actionPerformed");
            }
        });
        de1.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        de1.setValue(TimeUtil.now());
        
        DatumEditor de2 = new DatumEditor();
        de2.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("actionPerformed");
            }
        });
        de2.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        de2.setValue(Datum.create(1.0, Units.celcius));
        
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(de1);
        content.add(de2);
        
        JFrame frame = new JFrame("The Window");
        frame.setContentPane(content);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
    
    public String getToolTipText(MouseEvent event) {
        if (unitsButton.getBounds().contains(event.getX(), event.getY())) {
            return unitsButton.getToolTipText();
        }
        else {
            return null;
        }
    }
    
}
