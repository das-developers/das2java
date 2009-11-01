/*
 * BooleanEditor.java
 *
 * Created on April 14, 2005, 9:18 AM
 */

package org.das2.components.propertyeditor;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.EventObject;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.EventListenerList;
import javax.swing.table.TableCellEditor;

/**
 *
 * @author eew
 */
public class BooleanEditor implements java.beans.PropertyEditor, TableCellEditor {
    
    private JCheckBox editor;
    private Model model;
    private boolean selected;
    private Class type;
    private PropertyChangeSupport pcSupport;
    private EventListenerList listeners = new EventListenerList();
    
    /** Creates a new instance of BooleanEditor */
    public BooleanEditor() {
        pcSupport = new PropertyChangeSupport(this);
    }
    
    private void initEditor() {
        if (editor == null) {
            model = new Model();
            editor = new JCheckBox();
            editor.setModel(model);
        }
    }
    
    public String getAsText() {
        return String.valueOf(selected);
    }

    public Object getValue() {
        return selected ? Boolean.TRUE : Boolean.FALSE;
    }

    public void setAsText(String str) throws IllegalArgumentException {
        Boolean value;
        if ("true".equalsIgnoreCase(str)) {
            value = Boolean.TRUE;
        }
        else if ("false".equalsIgnoreCase(str)) {
            value = Boolean.FALSE;
        }
        else {
            throw new IllegalArgumentException(str);
        }
        setValue(value);
    }

    public void setValue(Object obj) {
        Boolean oldValue;
        Boolean value = (Boolean)obj;
        
        if (selected ^ value.booleanValue()) {
            oldValue = selected ? Boolean.TRUE : Boolean.FALSE;
            selected = value.booleanValue();
            pcSupport.firePropertyChange("value", oldValue, value);
        }
    }
    
    public boolean supportsCustomEditor() {
        return true;
    }
    
    public Component getCustomEditor() {
        initEditor();
        return editor;
    }
    
    public String getJavaInitializationString() { return "???"; }

    public String[] getTags() { return null; }

    public boolean isPaintable() { return false; }

    public void paintValue(Graphics g, Rectangle r) {}

    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcSupport.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcSupport.removePropertyChangeListener(l);
    }
    
    /*TableCellEditor stuff*/
    
    public Object getCellEditorValue() {
        return selected ? Boolean.TRUE : Boolean.FALSE;
    }
    
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        initEditor();
        editor.setForeground(table.getForeground());
        editor.setBackground(table.getBackground());
        setValue(value);
        return editor;
    }
    
    public boolean isCellEditable(EventObject evt) { return true; }
    
    public boolean shouldSelectCell(EventObject evt) { return true; }
    
    public boolean stopCellEditing() { 
        fireEditingStopped();
        return true;
    }
    
    public void cancelCellEditing() {
        fireEditingCanceled();
    }
    
    public void addCellEditorListener(CellEditorListener l) {
        listeners.add(CellEditorListener.class, l);
    }
            
    public void removeCellEditorListener(CellEditorListener l) {
        listeners.add(CellEditorListener.class, l);
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
    
    private class Model extends JToggleButton.ToggleButtonModel {
        private Model() {}
        public void setSelected(boolean b) {
            setValue(b ? Boolean.TRUE : Boolean.FALSE);
            fireEditingStopped();
        }
        public boolean isSelected() {
            return ((Boolean)getValue()).booleanValue();
        }
    }

}
