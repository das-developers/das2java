/*
 * EnumerationEditor.java
 *
 * Created on April 14, 2005, 9:18 AM
 */
package org.das2.components.propertyeditor;

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
    private PropertyChangeSupport pcSupport;
    private EventListenerList listeners = new EventListenerList();
    private Map valueMap;
    private Map nameMap;
    private Map toStringMap;

    /** Creates a new instance of EnumerationEditor */
    public EnumerationEditor() {
        pcSupport = new PropertyChangeSupport(this);
    }

    /**
     * create an editor for the class.
     * @param c 
     */
    public EnumerationEditor(Class c) {
        this();
        setClass(c);
    }

    /**
     * create an editor for o's class, with o as the initial value.
     * @param o initial value, like AnchorPosition.NORTH
     */
    public EnumerationEditor(Object o) {
        this();
        setClass(o.getClass());
        setValue(o);
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
            editor.setSelectedItem(selected);
        }
    }
    private static final int PUBLIC_STATIC_FINAL = Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL;

    private void setClass(Class c) {
        type = c;
        Map valueM = new HashMap();
        Map nameM = new IdentityHashMap();
        Map toStringM = new HashMap();

        if (c.isEnum()) {
            Object[] vals = c.getEnumConstants();
            for (Object o : vals) {
                Enum e = (Enum) o;
                nameM.put(e.name(), e);
                toStringM.put(e.toString(), e);
                valueM.put(e, e.name());
            }

        } else {

            Field[] fields = type.getDeclaredFields();
            Set psf = new HashSet();
            for (int i = 0; i < fields.length; i++) {
                int modifiers = fields[i].getModifiers();
                if ((modifiers & PUBLIC_STATIC_FINAL) == PUBLIC_STATIC_FINAL) {
                    psf.add(fields[i]);
                }
            }
            for (Iterator i = psf.iterator(); i.hasNext();) {
                try {
                    Field f = (Field) i.next();
                    String name = f.getName();
                    Object value = f.get(null);
                    nameM.put(name, value);
                    toStringM.put(value.toString(), value);
                    valueM.put(value, name);
                } catch (IllegalAccessException iae) {
                    IllegalAccessError err = new IllegalAccessError(iae.getMessage());
                    err.initCause(iae);
                    throw err;
                }
            }
        }
        nameMap = nameM;
        valueMap = valueM;
        toStringMap = toStringM;
    }

    /** remove the item from the list of selections
     * 
     * @param j
     */
    public void removeItem( Object j ) {
        Field[] fields = type.getDeclaredFields();
        for ( Field f: fields ) {
            try {
                if ( f.get(selected).equals(j) ) {
                    nameMap.remove( f.getName() );
                }
            } catch ( IllegalAccessException ex ) {
                
            }
        }
        if ( editor!=null ) {
            model = new Model();
            editor.setModel(model);
        }
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
            if ( editor!=null ) {
                editor.setSelectedItem(selected);
                editor.repaint();
            }
            pcSupport.firePropertyChange("value", oldValue, selected);
        }
    }

    public void setValue(Object obj) {
        Class c = getTypeClass(obj);
        if (type != c) {
            int size = 0;
            if (model != null) {
                size = model.getSize();
            }
            setClass(c);
            if (model != null) {
                model.fireIntervalRemoved(model, 0, size - 1);
                model.fireIntervalAdded(model, 0, model.getSize() - 1);
            }
        }
        Object oldValue = selected;
        selected = obj;
        if (oldValue != obj) {
            pcSupport.firePropertyChange("value", oldValue, selected);
            if ( editor!=null ) {
                editor.setSelectedItem(selected);
                editor.repaint();
            }
        }
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

    public String getJavaInitializationString() {
        return "???";
    }

    public String[] getTags() {
        return null;
    }

    public boolean isPaintable() {
        return false;
    }

    public void paintValue(Graphics g, Rectangle r) {
    }

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

    public boolean isCellEditable(EventObject evt) {
        return true;
    }

    public boolean shouldSelectCell(EventObject evt) {
        return true;
    }

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

    private class Renderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean hasFocus) {
            String s = (String) valueMap.get(value);
            super.getListCellRendererComponent(list, s, index, isSelected, hasFocus);
            if (value instanceof Enumeration) {
                setText(value.toString());
                setIcon(((Enumeration) value).getListIcon());
            }
            return this;
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

        //TODO: remove?
        @Override
        protected void fireIntervalRemoved(Object source, int index0, int index1) {
            super.fireIntervalRemoved(source, index0, index1);
        }

        //TODO: remove?
        @Override
        protected void fireIntervalAdded(Object source, int index0, int index1) {
            super.fireIntervalAdded(source, index0, index1);
        }

        //TODO: remove?
        @Override
        protected void fireContentsChanged(Object source, int index0, int index1) {
            super.fireContentsChanged(source, index0, index1);
        }
    }
}
