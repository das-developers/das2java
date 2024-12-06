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

import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import org.das2.datum.DatumRangeUtil;
import org.das2.datum.TimeLocationUnits;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyEditor;
import java.text.ParseException;
import java.util.EventObject;
import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.EventListenerList;
import javax.swing.table.TableCellEditor;
import org.das2.DasApplication;
import org.das2.datum.UnitsUtil;


/**
 *
 * @author  Edward West
 */
public class DatumRangeEditor extends JComponent implements PropertyEditor, TableCellEditor {
    
    private JTextField editor;
    private JButton unitsButton;
    private Units units = Units.dimensionless;
    private ActionListener actionListener;
    private DatumRange value;
    private EventListenerList listeners;
    
    /** Creates a new instance of DatumEditor */
    public DatumRangeEditor() {
        initComponents();
        installListeners();
        initToolTips();
        unitsButton.setVisible(false);
        setFocusable(true);
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        
        editor = new JTextField(8);
        editor.setFocusable(false);
        add(editor, BorderLayout.CENTER);
        
        unitsButton = new JButton();
        unitsButton.setFocusable(false);
        unitsButton.setToolTipText("units selection");
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
    
    /**
     * @param value the DatumRange to be editted.
     * @throws IllegalArgumentException if value is not a Datum
     */
    public void setValue(Object value) {
        if (value instanceof DatumRange) {
            setDatumRange((DatumRange)value);
        } else {
            throw new IllegalArgumentException();
        }
    }
    
    /**
     * @param datumRange the DatumRange to be edited.
     */
    private void setDatumRange(DatumRange datumRange ) {
        DatumRange oldValue = value;
//        if ( datumRange.min().gt( datumRange.max() ) ) {
//            System.err.println("how did we get here!");
//        }
        value = datumRange;
        Units u= datumRange.getUnits();
        String text= datumRange.toString();
        if ( !UnitsUtil.isTimeLocation(units) ) {
            if ( text.endsWith( u.toString() ) && u.toString().length()>7 ) {
                int n= text.length();
                text= text.substring(0,n-u.toString().length());
            }
        }
//        System.err.println("thetext="+text);
//        try {
//            DatumRangeUtil.parseTimeRange(text);
//        } catch ( ParseException ex ) {
//            ex.printStackTrace();;
//        } catch ( IllegalArgumentException ex ) {
//            ex.printStackTrace();;
//            datumRange.toString();
//        }
        editor.setText( text );
        setUnits(u);
        if (oldValue != value && oldValue != null && !oldValue.equals(value)) {
            firePropertyChange("value", oldValue, value);
        }
    }
    
    public void setAsText(String text) throws IllegalArgumentException {
        try {
            setDatumRange( parseText( text ) );
        } catch (ParseException pe) {
            IllegalArgumentException iae = new IllegalArgumentException(pe);
            throw iae;
        }
    }
    
    public Object getValue() {
        return getDatumRange();
    }
    
    private DatumRange parseText( String text ) throws ParseException {
        DatumRange result=null;
        if ( units instanceof TimeLocationUnits ) {
            result= DatumRangeUtil.parseTimeRange( text );
        } else {
            result= DatumRangeUtil.parseDatumRange( text, value );
        }
        return result;
    }

    /**
     * prevent displaying same message so many times...
     */
    private String lastErrorText= null;
    private long lastErrorTime= 0;

    private void showErrorUsage( String text, String why ) {
        if ( !DasApplication.getDefaultApplication().isHeadless() ) {
            if ( text!=null && text.equals(lastErrorText) 
                    && (System.currentTimeMillis()-lastErrorTime)<5000 ) {
                return;
            }
            if ( why!=null ) {
                JOptionPane.showMessageDialog( this, "<html>Unable to accept \""+text+"\"<br>"+why+"<html>" );
            } else {
                JOptionPane.showMessageDialog( this, "<html>Unable to accept \""+text+"\"</html>" );
            }
            lastErrorText= text;
            lastErrorTime= System.currentTimeMillis();
        }
    }

    /**
     * attempt to read the datum range in the TextField.  If it is not
     * parseable, then display an error message and return to the old value.
     * @return
     */
    public DatumRange getDatumRange() {
        String text = editor.getText();
        try {
            DatumRange dr = parseText(text);
            if ( !Double.isFinite(dr.min().doubleValue(dr.getUnits())) ) {
                if ( !Double.isFinite(value.min().doubleValue(value.getUnits())) ) {
                    editor.setText("1 to 100");
                    dr = parseText(text);
                } else {
                    editor.setText(value.toString());
                    return value;
                }
            }
            if ( !allowZeroWidth && dr.width().value()==0 ) {
                editor.setText(value.toString());
                return value;
            }
            if (!dr.equals(value) ) {
                DatumRange oldValue = value;
                value = dr;
                firePropertyChange("value", oldValue, value);
            }
            return value;
        } catch (ParseException e) {
            if ( value!=null ) {
                setDatumRange( value ); // cause reformat of old Datum
                if ( UnitsUtil.isTimeLocation(value.getUnits()) ) {
                    showErrorUsage( text, "String cannot be parsed to time range" );
                } else {
                    showErrorUsage( text, "String cannot be parsed to range with units \""+value.getUnits()+"\"" );
                }
                return value;
            } else {
                return null;
            }
        } catch ( IllegalArgumentException e ) {
            if ( value!=null ) {
                setDatumRange( value ); // cause reformat of old Datum
                if ( e.getMessage().contains("min > max") ) {
                    showErrorUsage( text, "Min cannot be greater than max" );
                } else {
                    showErrorUsage( text, e.getMessage() );
                }
                return value;
            } else {
                return null;
            }
        }
    }
    
    public String getAsText() {
        //String text= editor.getText();
        Object value = getDatumRange();
        if (value == null) {
            return null;
        }
        return editor.getText();
    }
    
    public void setUnits(Units units) {
//        if (units instanceof TimeLocationUnits) {
            unitsButton.setVisible(false);
//        } else {
//            unitsButton.setVisible(true);
//            String unitsStr= units.toString();
//            if ( unitsStr.length()>10 ) unitsStr= unitsStr.substring(0,9)+"...";
//            unitsButton.setText(unitsStr);
//            unitsButton.setToolTipText(units.toString()); // don't abbreviate
//        }
        this.units = units;
    }
    
    public Units getUnits() {
        return units;
    }
    
    private boolean allowZeroWidth = true;

    public static final String PROP_ALLOWZEROWIDTH = "allowZeroWidth";

    public boolean isAllowZeroWidth() {
        return allowZeroWidth;
    }

    public void setAllowZeroWidth(boolean allowZeroWidth) {
        boolean oldAllowZeroWidth = this.allowZeroWidth;
        this.allowZeroWidth = allowZeroWidth;
        firePropertyChange(PROP_ALLOWZEROWIDTH, oldAllowZeroWidth, allowZeroWidth);
    }

    private void fireActionPerformed() {
        setDatumRange(getDatumRange());
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
            } else {
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
    public boolean supportsCustomEditor() {
        return true;
    }
    
    /** @see java.beans.PropertyEditor#getCustomEditor()
     * @return <code>this</code>
     */
    public Component getCustomEditor() {
        return this;
    }
    
    /**
     * This PropertyEditor implementation does not support generating java
     * code.
     * @return The string "???"
     */
    public String getJavaInitializationString() {
        return "???";
    }
    
    /**
     * This PropertyEditor implementation does not support enumerated
     * values.
     * @return null
     */
    public String[] getTags() { return null; }
    
    /** @see java.beans.PropertyEditor#isPaintable()
     * @return false
     */
    public boolean isPaintable() {
        return false;
    }
    
    /** Does nothing.
     * @param g
     * @param r
     */
    public void paintValue(Graphics g, Rectangle r) {}
    
    /* CellEditor stuff */
    
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        setValue(value);
        return this;
    }
    
    /**
     */
    public void addCellEditorListener(CellEditorListener l) {
        if (listeners == null) {
            listeners = new EventListenerList();
        }
        listeners.add(CellEditorListener.class, l);
    }
    
    /**
     */
    public void removeCellEditorListener(CellEditorListener l) {
        if (listeners != null) {
            listeners.remove(CellEditorListener.class, l);
        }
    }
    
    /** @see javax.swing.CellEditor#isCellEditable(java.util.EventObject)
     * @return <code>true</code>
     */
    public boolean isCellEditable(EventObject anEvent) {
        return true;
    }
    
    /** @see javax.swing.CellEditor#shouldSelectCell(java.util.EventObject)
     * @return <code>true</code>
     */
    public boolean shouldSelectCell(EventObject anEvent) {
        return true;
    }
    
    /** Returns the value stored in this editor.
     * @return the current value being edited
     */
    public Object getCellEditorValue() {
        return getDatumRange();
    }
    
    public boolean stopCellEditing() {
        if (getDatumRange() == null) {
            return false;
        }
        fireEditingStopped();
        return true;
    }
    
    public void cancelCellEditing() {
        fireEditingCanceled();
    }
    
    private ChangeEvent evt;
    
    private void fireEditingStopped() {
        Object[] l = listeners.getListenerList();
        for (int i = 0; i < l.length; i+=2) {
            if (l[i] == CellEditorListener.class) {
                CellEditorListener cel = (CellEditorListener)l[i+1];
                if (evt == null) { evt = new ChangeEvent(this); }
                cel.editingStopped(evt);
            }
        }
    }
    
    private void fireEditingCanceled() {
        Object[] l = listeners.getListenerList();
        for (int i = 0; i < l.length; i+=2) {
            if (l[i] == CellEditorListener.class) {
                CellEditorListener cel = (CellEditorListener)l[i+1];
                if (evt == null) { evt = new ChangeEvent(this); }
                cel.editingCanceled(evt);
            }
        }
    }
}
