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
package org.das2.components;

import org.das2.datum.Units;
import org.das2.datum.format.TimeDatumFormatter;
import org.das2.datum.Datum;
import org.das2.datum.TimeLocationUnits;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyEditor;
import java.text.ParseException;
import java.util.EventObject;
import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.EventListenerList;
import javax.swing.table.TableCellEditor;

/**
 *
 * @author  Edward West
 */
public class DatumEditor implements PropertyEditor, TableCellEditor {

    private JTextField editor;
    private JPanel panel;
    private JButton unitsButton;
    private Units units = Units.dimensionless;
    private ActionListener actionListener;
    private Datum value;
    private EventListenerList listeners;
    private PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public synchronized void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(propertyName, listener);
    }

    @Override
    public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    public synchronized void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(propertyName, listener);
    }

    @Override
    public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    /** Creates a new instance of DatumEditor */
    public DatumEditor() {
    }

    private void initGui() {
        initComponents();
        installListeners();
        initToolTips();
        unitsButton.setVisible(false);
        panel.setFocusable(true);
    }

    private void maybeInitGui() {
        if (panel == null) {
            initGui();
        }
    }

    private void initComponents() {
        panel = new JPanel();
        panel.setLayout(new BorderLayout());

        editor = new JTextField(8);
        if (value != null) setDatum(value);
        editor.setFocusable(false);
        panel.add(editor, BorderLayout.CENTER);

        unitsButton = new JButton();
        unitsButton.setFocusable(false);
        unitsButton.setToolTipText("units selection");
        if ( units!=null ) {
            setUnits(units); // set labels and such
        }
        panel.add(unitsButton, BorderLayout.EAST);
    }

    private void installListeners() {
        UniversalListener ul = new UniversalListener();
        editor.addMouseListener(ul);
        unitsButton.addMouseListener(ul);
        panel.addKeyListener(ul);
        panel.addFocusListener(ul);
    }

    private void initToolTips() {
        ToolTipManager.sharedInstance().registerComponent(panel);
    }

    public void setColumns(int columns) {
        editor.setColumns(columns);
    }

    /**
     * @param value the Datum object to be editted.
     * @throws IllegalArgumentException if value is not a Datum
     */
    @Override
    public void setValue(Object value) {
        if (value instanceof Datum) {
            setDatum((Datum) value);
        } else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * @param datum the Datum object to be editted.
     */
    private void setDatum(Datum datum) {
        Datum oldValue = value;
        value = datum;
        Units u = datum.getUnits();
        if (editor != null) {
            if (datum.getUnits() instanceof TimeLocationUnits) {
                editor.setText(TimeDatumFormatter.DEFAULT.format(datum, u));
            } else {
                editor.setText(datum.getFormatter().format(datum, u));
            }
        }
        setUnits(u);
        if (oldValue != value && oldValue != null && !oldValue.equals(value)) {
            pcs.firePropertyChange("value", oldValue, value);
        }
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        try {
            setDatum(units.parse(text));
        } catch (ParseException pe) {
            IllegalArgumentException iae = new IllegalArgumentException(pe);
            throw iae;
        }
    }

    @Override
    public Object getValue() {
        return getDatum();
    }

    public Datum getDatum() {
        try {
            Datum d;
            if (editor != null) {
                String text = editor.getText();
                d = units.parse(text);
                if (!d.equals(value)) {
                    Datum oldValue = value;
                    value = d;
                    pcs.firePropertyChange("value", oldValue, value);
                }
            }
            return value;
        } catch (ParseException e) {
            if (value != null) {
                setDatum(value); // cause reformat of old Datum
                return value;
            } else {
                return null;
            }
        }
    }

    @Override
    public String getAsText() {
        Datum v = getDatum();
        if (v == null) {
            return null;
        } else {
            return v.toString();
        }
    }

    public void setUnits(Units units) {
        if (unitsButton != null) {
//            if (units instanceof TimeLocationUnits) {
                unitsButton.setVisible(false);
//            } else {
//                unitsButton.setVisible(true);
//                String unitsStr= units.toString();
//                if ( unitsStr.length()>10 ) unitsStr= unitsStr.substring(0,9)+"...";
//                unitsButton.setText(unitsStr);
//                unitsButton.setToolTipText(units.toString()); // don't abbreviate
//            }
        }
        this.units = units;
    }

    public Units getUnits() {
        return units;
    }

    private void fireActionPerformed() {
        setDatum(getDatum());
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
        @Override
        public void focusGained(FocusEvent e) {
            editor.getCaret().setVisible(true);
            editor.getCaret().setSelectionVisible(true);
        }

        /** Listens for focus events on the DatumEditor and sets the editor
         * caret invisible when focus is lost.
         */
        @Override
        public void focusLost(FocusEvent e) {
            editor.getCaret().setVisible(false);
            editor.getCaret().setSelectionVisible(false);
        }

        /** All key events are forwarded to the editor.  Except for keyPresses
         * for VK_ENTER
         */
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                fireActionPerformed();
            } else {
                forwardKeyEvent(e);
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            forwardKeyEvent(e);
        }

        @Override
        public void keyTyped(KeyEvent e) {
            forwardKeyEvent(e);
        }

        private void forwardKeyEvent(KeyEvent e) {
            e.setSource(editor);
            editor.dispatchEvent(e);
        }

        /** Request focus when sub-components are clicked */
        @Override
        public void mousePressed(MouseEvent e) {
            panel.requestFocusInWindow();
        }

        /** Unused */
        @Override
        public void mouseEntered(MouseEvent e) {
        }

        @Override
        public void mouseExited(MouseEvent e) {
        }

        @Override
        public void mouseClicked(MouseEvent e) {
        }

        @Override
        public void mouseReleased(MouseEvent e) {
        }
    }

    public String getToolTipText(MouseEvent event) {
        if (unitsButton.getBounds().contains(event.getX(), event.getY())) {
            return unitsButton.getToolTipText();
        } else {
            return null;
        }
    }

    /** @see java.beans.PropertyEditor#supportsCustomEditor()
     * @return true
     */
    @Override
    public boolean supportsCustomEditor() {
        return true;
    }

    /** @see java.beans.PropertyEditor#getCustomEditor()
     * @return <code>this</code>
     */
    @Override
    public Component getCustomEditor() {
        maybeInitGui();
        return panel;
    }

    /**
     * This PropertyEditor implementation does not support generating java
     * code.
     * @return The string "???"
     */
    @Override
    public String getJavaInitializationString() {
        return "???";
    }

    /**
     * This PropertyEditor implementation does not support enumerated
     * values.
     * @return null
     */
    @Override
    public String[] getTags() {
        return null;
    }

    @Override
    public boolean isPaintable() {
        return false;
    }

    @Override
    public void paintValue(Graphics g, Rectangle r) {
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        setValue(value);
        maybeInitGui();
        return panel;
    }

    @Override
    public void addCellEditorListener(CellEditorListener l) {
        if (listeners == null) {
            listeners = new EventListenerList();
        }
        listeners.add(CellEditorListener.class, l);
    }

    @Override
    public void removeCellEditorListener(CellEditorListener l) {
        if (listeners != null) {
            listeners.remove(CellEditorListener.class, l);
        }
    }

    /** @see javax.swing.CellEditor#isCellEditable(java.util.EventObject)
     * @return <code>true</code>
     */
    @Override
    public boolean isCellEditable(EventObject anEvent) {
        return true;
    }

    /** @see javax.swing.CellEditor#shouldSelectCell(java.util.EventObject)
     * @return <code>true</code>
     */
    @Override
    public boolean shouldSelectCell(EventObject anEvent) {
        return true;
    }

    /** Returns the value stored in this editor.
     * @return the current value being edited
     */
    @Override
    public Object getCellEditorValue() {
        return getDatum();
    }

    @Override
    public boolean stopCellEditing() {
        if (getDatum() == null) {
            return false;
        }
        fireEditingStopped();
        return true;
    }

    @Override
    public void cancelCellEditing() {
        fireEditingCanceled();
    }
    private ChangeEvent evt;

    private void fireEditingStopped() {
        if ( listeners==null ) return;
        Object[] l = listeners.getListenerList();
        for (int i = 0; i < l.length; i += 2) {
            if (l[i] == CellEditorListener.class) {
                CellEditorListener cel = (CellEditorListener) l[i + 1];
                if (evt == null) {
                    evt = new ChangeEvent(this);
                }
                cel.editingStopped(evt);
            }
        }
    }

    private void fireEditingCanceled() {
        if ( listeners==null ) return;
        Object[] l = listeners.getListenerList();
        for (int i = 0; i < l.length; i += 2) {
            if (l[i] == CellEditorListener.class) {
                CellEditorListener cel = (CellEditorListener) l[i + 1];
                if (evt == null) {
                    evt = new ChangeEvent(this);
                }
                cel.editingCanceled(evt);
            }
        }
    }
}
