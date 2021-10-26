/*
 * PropertyEditorAdapter.java
 *
 * Created on April 14, 2005, 2:53 PM
 */

package org.das2.components.propertyeditor;

import org.das2.beans.BeansUtil;
import org.das2.components.treetable.TreeTableModel;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.IndexedPropertyDescriptor;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.util.EventObject;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.EventListenerList;
import javax.swing.table.TableCellEditor;

/**
 *
 * @author eew
 */
public class PropertyEditorAdapter implements TableCellEditor {
    
    private EventListenerList listenerList = new EventListenerList();
    private PropertyEditor editor;
    private EditorState state;
    
    /* Possible states */
    private EditorState simple = new SimpleEditor();
    private EditorState custom = new CustomEditor();
    private EditorState cellEditor = new CustomTableCellEditor();
    
    /** Creates a new instance of PropertyEditorAdapter */
    public PropertyEditorAdapter() {
    }

    public void addCellEditorListener(CellEditorListener l) {
        listenerList.add(CellEditorListener.class, l);
    }

    public void cancelCellEditing() {
        if (state != null) state.cancel();
        state = null;
        fireEditingCanceled();
    }

    public Object getCellEditorValue() {
        return editor.getValue();
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean selected, int rowIndex, int columnIndex) {
        TreeTableModel model = (TreeTableModel)table.getModel();
        PropertyTreeNodeInterface node = (PropertyTreeNodeInterface)model.getNodeForRow(rowIndex);
        PropertyDescriptor pd = node.getPropertyDescriptor();
        editor = getEditor(pd);
        if ( editor instanceof StringWithSchemeEditor ) {
            String propertyName=null;
            if ( node instanceof PropertyTreeNode ) {
                propertyName= ((PropertyTreeNode)node).getDisplayName();
            } else if ( node instanceof PeerPropertyTreeNode ) {
                propertyName= ((PeerPropertyTreeNode)node).getDisplayName();
            }
            
            if ( propertyName.equals("tickValues") ) {
                ((StringWithSchemeEditor)editor).setCustomEditor( new TickValuesStringSchemeEditor() );
            } else {
                ((StringWithSchemeEditor)editor).setCustomEditor( new DefaultStringSchemeEditor() );
            }            
        }
        if (editor == null) {
            cancelCellEditing();
            return new JLabel();
        }
        else {
            if ( value==org.das2.components.propertyeditor.PropertyEditor.MULTIPLE ) {
                editor.setValue(((PeerPropertyTreeNode)node).leader.getValueAt(1));
            } else {
                editor.setValue(value);
            }
        }
        
        if (editor instanceof TableCellEditor) {
            state = cellEditor;
        }
        else if (editor.supportsCustomEditor()) {
            state = custom;
        }
        else {
            state = simple;
        }
        return state.getEditorComponent(table, selected, rowIndex, columnIndex);
    }
    
    private static PropertyEditor getEditor(PropertyDescriptor pd) {
        PropertyEditor ed;
        if (pd.getPropertyEditorClass() != null) {
            try {
                Object instance = pd.getPropertyEditorClass().newInstance();
                if (instance instanceof PropertyEditor) {
                    ed = (PropertyEditor)instance;
                }
                else {
                    ed = null;
                }
            }
            catch (InstantiationException ie) {
                ed = null;
            }
            catch (IllegalAccessException iae) {
                ed = null;
            }
        }
        else {
            ed = BeansUtil.findEditor(pd instanceof IndexedPropertyDescriptor
                    ? ((IndexedPropertyDescriptor)pd).getIndexedPropertyType()
                    : pd.getPropertyType());
        }
        return ed;
    }

    public boolean isCellEditable(EventObject eventObject) {
        return true;
    }

    public void removeCellEditorListener(CellEditorListener l) {
        listenerList.remove(CellEditorListener.class, l);
    }

    public boolean shouldSelectCell(EventObject eventObject) {
        return true;
    }

    public boolean stopCellEditing() {
        if (state == null) return false;
        boolean stopped = state.stop();
        if (stopped) {
            fireEditingStopped();
            state = null;
        }
        return stopped;
    }
    
    private void fireEditingCanceled() {
        Class clazz = CellEditorListener.class;
        ChangeEvent e = null;
        Object[] listeners = listenerList.getListenerList();
        for (int i = 0; i < listeners.length; i+=2) {
            if (listeners[i] == clazz) {
                CellEditorListener l = (CellEditorListener)listeners[i+1];
                if (e == null) { e = new ChangeEvent(this); }
                l.editingCanceled(e);
            }
        }
    }
    
    private void fireEditingStopped() {
        Class clazz = CellEditorListener.class;
        ChangeEvent e = null;
        Object[] listeners = listenerList.getListenerList();
        for (int i = 0; i < listeners.length; i+=2) {
            if (listeners[i] == clazz) {
                CellEditorListener l = (CellEditorListener)listeners[i+1];
                if (e == null) { e = new ChangeEvent(this); }
                l.editingStopped(e);
            }
        }
    }
    
    private static interface EditorState {
        void cancel();
        boolean stop(); //TODO: document me--return value is rarely used
        Component getEditorComponent(JTable table, boolean selected, int rowIndex, int columnIndex);
    }
    
    private class SimpleEditor implements EditorState, ActionListener {
        private JTextField textField;
        public void cancel() {}
        public boolean stop() {
            String text = textField.getText();
            try {
                editor.setAsText(text);
                return true;
            }
            catch (IllegalArgumentException iae) {
                return false;
            }
        }
        public Component getEditorComponent(JTable table, boolean selected, int rowIndex, int columnIndex) {
            initTextField();
            textField.setText(editor.getAsText());
            return textField;
        }
        public void actionPerformed(ActionEvent e) {
            stopCellEditing();
        }
        private void initTextField() {
            if (textField == null) {
                textField = new JTextField();
                textField.setBorder(null);
                textField.addActionListener(this);
            }
        }
    }
    
    private class CustomEditor implements EditorState, ActionListener {
        private JButton button;
        
        public boolean stop() {
            return true;
        }
        
        public void cancel() {
        }
        
        public Component getEditorComponent(JTable table, boolean selected, int rowIndex, int columnIndex) {
            init();
            String s = editor.getAsText();
            if (s == null) {
                s = String.valueOf(editor.getValue());
            }
            button.setText(s);
            return button;
        }
        
        public void actionPerformed(ActionEvent e) {
            Component customEditor = editor.getCustomEditor();
            int result = JOptionPane.showConfirmDialog(button, customEditor, "", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.CLOSED_OPTION || result == JOptionPane.CANCEL_OPTION) {
                cancelCellEditing();
            }
            else {
                stopCellEditing();
            }
        }
        
        private void init() {
            if (button == null) {
                button = new JButton();
                button.setBorder(null);
                button.addActionListener(this);
            }
        }
    }
    
    private class CustomTableCellEditor implements EditorState, CellEditorListener {
        public Component getEditorComponent(JTable table, boolean selected, int rowIndex, int columnIndex) {
            TableCellEditor tce = (TableCellEditor)editor;
            Component c = tce.getTableCellEditorComponent(table, editor.getValue(), selected, rowIndex, columnIndex);
            tce.addCellEditorListener(this);
            return c;
        }
        public boolean stop() {
            TableCellEditor tce = (TableCellEditor)editor;
            return tce.stopCellEditing();
        }
        public void cancel() {
            TableCellEditor tce = (TableCellEditor)editor;
            tce.cancelCellEditing();
        }
        
        public void editingStopped(ChangeEvent e) {
            TableCellEditor tce = (TableCellEditor)editor;
            tce.removeCellEditorListener(this);
            PropertyEditorAdapter.this.fireEditingStopped();
        }
        
        public void editingCanceled(ChangeEvent e) {
            TableCellEditor tce = (TableCellEditor)editor;
            tce.removeCellEditorListener(this);
            PropertyEditorAdapter.this.fireEditingCanceled();
        }
    }
}
