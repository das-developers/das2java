package edu.uiowa.physics.pw.das.components.propertyeditor;

import edu.uiowa.physics.pw.das.components.DatumEditor;
import edu.uiowa.physics.pw.das.components.treetable.TreeTableModel;
import org.das2.dasml.CommandBlock;
import org.das2.dasml.CommandBlockEditor;
import org.das2.dasml.ListOption;
import org.das2.dasml.OptionListEditor;
import edu.uiowa.physics.pw.das.datum.Datum;
import org.das2.util.DasExceptionHandler;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;

class PropertyCellEditor extends AbstractCellEditor implements TableCellEditor, ActionListener, javax.swing.event.CellEditorListener {
    
    private static Map enumerationMap = new HashMap();
    
    static Object[] getEnumerationList(Class type) {
        
        Object[] list = (Object[])enumerationMap.get(type);
        if (list != null) return list;
        
        java.lang.reflect.Field[] fields = type.getFields();
        int count = 0;
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].getType() == type &&
            java.lang.reflect.Modifier.isStatic(fields[i].getModifiers())) {
                count++;
            }
        }
        list = new Object[count];
        int index = 0;
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].getType() == type &&
            java.lang.reflect.Modifier.isStatic(fields[i].getModifiers())) {
                try {
                    list[index] = fields[i].get(null);
                    index++;
                }
                catch (IllegalAccessException iae) {
                }
            }
        }
        enumerationMap.put(type, list);
        return list;
    }
        
    private JFormattedTextField integerField;
    
    private JFormattedTextField floatField;
    
    private JTextField stringField;
    
    private DatumEditor datumEditor;
    
    private JCheckBox booleanBox;
    
    private JComboBox enumerationChoice;
    
    private CommandBlockEditor commandBlockEditor;
    
    private OptionListEditor optionListEditor;
    
    private static final int INTEGER = 0x001;
    
    private static final int LONG = 0x002;
    
    private static final int FLOAT = 0x004;
    
    private static final int DOUBLE = 0x008;
    
    private static final int STRING = 0x010;
    
    private static final int BOOLEAN = 0x020;
    
    private static final int EDITABLE = 0x040;
    
    private static final int ENUMERATION = 0x080;
    
    private static final int DATUM = 0x100;
    
    private static final int COMMAND_BLOCK = 0x200;
    
    private static final int LIST_OPTION = 0x400;
    
    private int editorState = INTEGER;
    
    private Object currentValue = null;
    
    private int currentRow;
    
    private JTree propertyTree;
    
    public PropertyCellEditor(JTree propertyTree) {
        integerField = new JFormattedTextField(new Integer(0));
        integerField.addActionListener(this);
        
        FloatingPointFormatter floatFormatter = new FloatingPointFormatter();
        floatField = new JFormattedTextField(floatFormatter);
        floatField.addActionListener(this);
        
        stringField = new JTextField();
        stringField.addActionListener(this);
        datumEditor = new DatumEditor();
        datumEditor.addActionListener(this);
        booleanBox = new JCheckBox();
        booleanBox.addActionListener(this);
        
        this.propertyTree = propertyTree;
        enumerationChoice = new JComboBox();
        enumerationChoice.setRenderer(new PropertyCellRenderer());
        enumerationChoice.addActionListener(this);
        
        commandBlockEditor = new CommandBlockEditor();
        commandBlockEditor.addCellEditorListener(this);
        optionListEditor = new OptionListEditor();
        optionListEditor.addCellEditorListener(this);
    }
    
    public void actionPerformed(ActionEvent e) {
        
        fireEditingStopped();
        
        if (e.getSource() instanceof JButton) {
            if (propertyTree.isCollapsed(currentRow)) {
                propertyTree.expandRow(currentRow);
            }
            else {
                propertyTree.collapseRow(currentRow);
            }
        }
    }
    
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        currentValue = value;
        currentRow = row;
        TreeTableModel model = (TreeTableModel)table.getModel();
        PropertyTreeNodeInterface node = (PropertyTreeNodeInterface)model.getNodeForRow(row);
        PropertyDescriptor pd = node.getPropertyDescriptor();
        if (value instanceof Enumeration) {
            editorState = ENUMERATION;
            Object[] list = getEnumerationList(pd.getPropertyType());
            enumerationChoice.setModel(new DefaultComboBoxModel(list));
            enumerationChoice.setSelectedItem(value);
            return enumerationChoice;
        }
        else if (value instanceof Integer) {
            editorState = INTEGER;
            integerField.setValue(value);
            return integerField;
        }
        else if (value instanceof Long) {
            editorState = LONG;
            integerField.setValue(value);
            return integerField;
        }
        else if (value instanceof Float) {
            editorState = FLOAT;
            floatField.setValue(value);
            return floatField;
        }
        else if (value instanceof Double) {
            editorState = DOUBLE;
            floatField.setValue(value);
            return floatField;
        }
        else if (value instanceof Boolean) {
            editorState = BOOLEAN;
            booleanBox.setSelected(((Boolean)value).booleanValue());
            booleanBox.setText(value.toString());
            return booleanBox;
        }
        else if (value instanceof Datum) {
            editorState = DATUM;
            Datum d = (Datum)value;
            datumEditor.setValue(d);
            return datumEditor.getCustomEditor();
        }
        else if (pd.getPropertyType().equals(CommandBlock.class)) {
            editorState = COMMAND_BLOCK;
            return commandBlockEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
        }
        else if (pd.getPropertyType().equals(ListOption[].class)) {
            editorState = LIST_OPTION;
            return optionListEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
        }
        else{
            editorState = STRING;
            stringField.setText(value.toString());
            return stringField;
        }
    }
    
    public Object getCellEditorValue() {
        if (editorState == ENUMERATION) {
            return enumerationChoice.getSelectedItem();
        }
        else if (editorState == INTEGER) {
            Object value = integerField.getValue();
            if (value instanceof Integer) return value;
            if (value instanceof Number) {
                return new Integer(((Number)value).intValue());
            }
            throw new IllegalStateException("Value from textfield is not of type java.lang.Number");
        }
        else if (editorState == LONG) {
            Object value = integerField.getValue();
            if (value instanceof Long) return value;
            if (value instanceof Number) {
                return new Long(((Number)value).longValue());
            }
            throw new IllegalStateException("Value from textfield is not of type java.lang.Number");
        }
        else if (editorState == FLOAT) {
            Object value = floatField.getValue();
            if (value instanceof Float) return value;
            if (value instanceof Number) {
                return new Float(((Number)value).floatValue());
            }
            throw new IllegalStateException("Value from textfield is not of type java.lang.Number");
        }
        else if (editorState == DOUBLE) {
            Object value = floatField.getValue();
            if (value instanceof Double) return value;
            if (value instanceof Number) {
                return new Double(((Number)value).doubleValue());
            }
            throw new IllegalStateException("Value from textfield is not of type java.lang.Number");
        }
        else if (editorState == EDITABLE) {
            return currentValue;
        }
        else if (editorState == BOOLEAN) {
            return (booleanBox.isSelected() ? Boolean.TRUE : Boolean.FALSE);
        }
        else if (editorState == DATUM) {
            try {
                return datumEditor.getValue();
            }
            catch (IllegalArgumentException iae) {
                DasExceptionHandler.handle(iae);
                return currentValue;
            }
        }
        else if (editorState == COMMAND_BLOCK) {
            return commandBlockEditor.getCellEditorValue();
        }
        else if (editorState == LIST_OPTION) {
            return optionListEditor.getCellEditorValue();
        }
        else {
            return stringField.getText();
        }
    }
    
    public void editingCanceled(ChangeEvent e) {
        fireEditingCanceled();
    }
    
    public void editingStopped(ChangeEvent e) {
        fireEditingStopped();
    }
    
}

