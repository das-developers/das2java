/*
 * BooleanEditor.java
 *
 * Created on May 4, 2005, 9:09 AM
 */

package edu.uiowa.physics.pw.das.components.propertyeditor;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyEditor;
import java.util.EventObject;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import javax.swing.table.TableCellEditor;

/**
 *
 * @author eew
 */
public class BooleanEditor implements PropertyEditor, TableCellEditor {
    
    private JCheckBox editor;
    private PropertyChangeSupport pcSupport;
    private Boolean value = Boolean.FALSE;
    private EventListenerList listeners = new EventListenerList();
    
    /** Creates a new instance of BooleanEditor */
    public BooleanEditor() {
        editor = new JCheckBox();
        editor.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                setValue(Boolean.valueOf(editor.isSelected()));
                editor.setText(value.toString());
                fireEditingStopped();
            }
        });
        pcSupport = new PropertyChangeSupport(this);
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcSupport.addPropertyChangeListener(l);
    }

    public String getAsText() {
        return value.toString();
    }

    public Component getCustomEditor() {
        return editor;
    }

    public String getJavaInitializationString() {
        return value.toString();
    }

    public String[] getTags() {
        return null;
    }

    public Object getValue() {
        return value;
    }

    public boolean isPaintable() {
        return false;
    }

    public void paintValue(Graphics g, Rectangle r) {
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcSupport.removePropertyChangeListener(l);
    }

    public void setAsText(String str) throws IllegalArgumentException {
        if ("true".equalsIgnoreCase(str)) {
            setValue(Boolean.TRUE);
        }
        else if ("false".equalsIgnoreCase(str)) {
            setValue(Boolean.FALSE);
        }
        else {
            throw new IllegalArgumentException(str);
        }
    }

    public void setValue(Object obj) {
        if (!value.equals(obj)) {
            Object oldValue = value;
            value = (Boolean)obj;
            editor.setSelected(value.booleanValue());
            pcSupport.firePropertyChange("value", oldValue, value);
        }
    }

    public boolean supportsCustomEditor() {
        return true;
    }

    public void addCellEditorListener(CellEditorListener l) {
        listeners.add(CellEditorListener.class, l);
    }

    public void cancelCellEditing() {
        fireEditingCanceled();
    }

    public Object getCellEditorValue() {
        return value;
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        editor.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
        editor.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
        editor.setFont(table.getFont());
        setValue(value);
        return editor;
    }

    public boolean isCellEditable(EventObject e) {
        return true;
    }

    public void removeCellEditorListener(CellEditorListener l) {
        listeners.remove(CellEditorListener.class, l);
    }

    public boolean shouldSelectCell(EventObject e) {
        return true;
    }

    public boolean stopCellEditing() {
        fireEditingStopped();
        return true;
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
