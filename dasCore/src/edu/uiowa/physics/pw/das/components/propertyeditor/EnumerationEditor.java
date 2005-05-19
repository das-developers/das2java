/*
 * EnumerationEditor.java
 *
 * Created on April 14, 2005, 9:18 AM
 */

package edu.uiowa.physics.pw.das.components.propertyeditor;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.EventListenerList;
import javax.swing.table.TableCellEditor;

/**
 *
 * @author eew
 */
public class EnumerationEditor implements java.beans.PropertyEditor, TableCellEditor {
    
    private JComboBox editor;
    private Model model;
    private Object selected;
    private Class type;
    private boolean guessType;
    private PropertyChangeSupport pcSupport;
    private EventListenerList listeners = new EventListenerList();
    
    private Map valueMap;
    private Map nameMap;
    private Map toStringMap;
    
    /** Creates a new instance of EnumerationEditor */
    public EnumerationEditor() {
        guessType = true;
    }
    
    protected EnumerationEditor(Class c) {
        setClass(c);
        guessType = false;
    }
    
    private void initEditor() {
        if (editor == null) {
            model = new Model();
            editor = new JComboBox(model) {
                public void setBounds(int x, int y, int width, int height) {
                    Dimension preferred = getPreferredSize();
                    super.setBounds(x, y, width, preferred.height);
                }
            };
            editor.setRenderer(new Renderer());
        }
    }
    
    private static final int PUBLIC_STATIC_FINAL
            = Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL;
    
    private void setClass(Class c) {
        type = c;
        Field[] fields = type.getDeclaredFields();
        Set psf = new HashSet();
        Map valueM = new HashMap();
        Map nameM = new IdentityHashMap();
        Map toStringM = new HashMap();
        for (int i = 0; i < fields.length; i++) {
            int modifiers = fields[i].getModifiers();
            if ((modifiers & PUBLIC_STATIC_FINAL) == PUBLIC_STATIC_FINAL) {
                psf.add(fields[i]);
            }
        }
        for (Iterator i = psf.iterator(); i.hasNext();) {
            try {
                Field f = (Field)i.next();
                String name = f.getName();
                Object value = f.get(null);
                nameM.put(name, value);
                toStringM.put(value.toString(), value);
                valueM.put(value, name);
            }
            catch (IllegalAccessException iae) {
                IllegalAccessError err = new IllegalAccessError(iae.getMessage());
                err.initCause(iae);
                throw err;
            }
        }
        nameMap = nameM;
        valueMap = valueM;
        toStringMap = toStringM;
    }

    public String getAsText() {
        return selected.toString();
    }

    public Object getValue() {
        return selected;
    }

    public void setAsText(String str) throws IllegalArgumentException {
        Object oldValue;
        Object value = nameMap.get(str);
        if (value == null) {
            value = toStringMap.get(str);
        }
        if (value == null) {
            throw new IllegalArgumentException(str);
        }
        if (selected != value) {
            oldValue = selected;
            selected = value;
            pcSupport.firePropertyChange("value", oldValue, selected);
        }
    }

    public void setValue(Object obj) {
        Class c = getTypeClass(obj);
        if (type != c) {
            int size = 0;
            if (model !=  null) { size = model.getSize(); }
            setClass(c);
            if (model != null) {
                model.fireIntervalRemoved(model, 0, size - 1);
                model.fireIntervalAdded(model, 0, model.getSize() - 1);
            }
        }
        selected = obj;
    }
    
    private Class getTypeClass(Object obj) {
        Class c = obj.getClass();
        String name = c.getName();
        if (name.matches(".+?\\$\\d+")) {
            c = c.getSuperclass();
        }
        return c;
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
        return selected;
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
    
    private class Renderer extends DefaultListCellRenderer {
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean hasFocus) {
            String s = (String)valueMap.get(value);
            return super.getListCellRendererComponent(list, s, index, isSelected, hasFocus);
        }
        
    }
    
    private class Model extends AbstractListModel implements ComboBoxModel {
        private List list;
        
        private Model() {
            list = new ArrayList(nameMap.keySet());
            Collections.sort(list, String.CASE_INSENSITIVE_ORDER);
        }
        
        public Object getSelectedItem() {
            return selected;
        }
        
        public void setSelectedItem(Object o) {
            setValue(o);
            stopCellEditing();
        }
        
        public Object getElementAt(int index) {
            return nameMap.get(list.get(index));
        }
        
        public int getSize() {
            return list.size();
        }

        protected void fireIntervalRemoved(Object source, int index0, int index1) {
            super.fireIntervalRemoved(source, index0, index1);
        }

        protected void fireIntervalAdded(Object source, int index0, int index1) {
            super.fireIntervalAdded(source, index0, index1);
        }

        protected void fireContentsChanged(Object source, int index0, int index1) {
            super.fireContentsChanged(source, index0, index1);
        }
    }

}
