/* File: PropertyEditor.java
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

package edu.uiowa.physics.pw.das.components;

import edu.uiowa.physics.pw.das.dasml.*;
import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.util.DasDate;
import edu.uiowa.physics.pw.das.util.DasExceptionHandler;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;

import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.text.*;
import javax.swing.text.DocumentFilter.FilterBypass;
import javax.swing.tree.*;

import java.lang.reflect.*;
import java.text.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

/**
 * This class implements a Hierarchical property editor
 *
 * @author Edward West
 */
public class PropertyEditor extends JPanel {
    
    private static DecimalFormat expFormat = new DecimalFormat("0.#######E0");
    
    private static Map enumerationMap = new HashMap();
    
    private static Object[] getEnumerationList(Class type) {
        
        Object[] list = (Object[])enumerationMap.get(type);
        if (list != null) return list;
        
        Field[] fields = type.getFields();
        int count = 0;
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].getType() == type &&
            Modifier.isStatic(fields[i].getModifiers())) {
                count++;
            }
        }
        list = new Object[count];
        int index = 0;
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].getType() == type &&
            Modifier.isStatic(fields[i].getModifiers())) {
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
        
    static {
        String[] beanInfoSearchPath = { "edu.uiowa.physics.pw.das.beans", "sun.beans.infos" };
        Introspector.setBeanInfoSearchPath(beanInfoSearchPath);
    }
    
    
    /**
     * Objects that are instances of classes that implement
     * this interface can be expanded in a <code>PropertyEditor</code>
     * property list.
     */
    public static interface Editable {}
    
    /**
     * Type-safe enumerations that are used as property types
     * that are editable with a PropertyEditor should
     * implement this interface.
     */
    public static interface Enumeration {
        /**
         * Type-safe Enumerations implementing this interface
         * should override the toString() method to return a
         * <code>String</code> that will be helpful to the user
         * when choosing this as an option from a list.
         */
        String toString();
        
        /**
         * An icon can be provided that will be shown in a list
         * along with the textual description of the element.
         * This method should return <code>null</code> if there
         * is no icon available.
         */
        Icon getListIcon();
    }
    
    PropertyTable table;
    
    JButton closeButton;
    
    JDialog dialog;
    
    boolean globalHasChanged;
    
    public PropertyEditor(Editable e) {
        super(new BorderLayout());
        PropertyTreeTableModel model = new PropertyTreeTableModel(e);
        table = new PropertyTable(model);
        add(new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);
        initButtonPanel();
        PropertyValueCellRenderer valueRenderer = new PropertyValueCellRenderer();
        PropertyTreeRenderer nameRenderer = getPropertyTreeTableModel().getRenderer();
        TableCellEditor editor = new PropertyCellEditor(nameRenderer);
        Component c = valueRenderer.getTableCellRendererComponent(null, "XXX", false, false,  0, 0);
        table.setRowHeight(c.getPreferredSize().height);
        nameRenderer.setRowHeight(c.getPreferredSize().height);
        nameRenderer.setCellRenderer(valueRenderer);
        ((JComponent)nameRenderer.getCellRenderer()).setOpaque(false);
        table.getColumnModel().getColumn(0).setCellRenderer(nameRenderer);
        table.getColumnModel().getColumn(1).setCellRenderer(valueRenderer);
        table.setDefaultEditor(Object.class, editor);
        table.addMouseListener(new PropertyTableMouseListener());
    }
    
    private void initButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        final JButton apply = new JButton("Apply Changes");
        closeButton = new JButton("Dismiss");
        ActionListener al = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == apply) {
                    globalApplyChanges();
                }
                else if (e.getSource() == closeButton) {
                    dismissDialog();
                }
            }
        };
        apply.addActionListener(al);
        closeButton.addActionListener(al);
        buttonPanel.add(apply);
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void globalApplyChanges() {
        ((PropertyTreeNode)getPropertyTreeTableModel().getRoot()).flushChanges();
        globalHasChanged = false;
    }
    
    private void dismissDialog() {
        if (globalHasChanged) {
            String[] message = new String[] {
                "You have unsaved changes",
                "Would you like to apply them?"
            };
            int result = JOptionPane.showConfirmDialog(this, message, "", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (result == JOptionPane.CANCEL_OPTION) {
                return;
            }
            if (result == JOptionPane.YES_OPTION) {
                globalApplyChanges();
            }
        }
        dialog.setVisible(false);
        dialog.dispose();
    }
    
    public void showDialog(Component c) {
        if (dialog == null) {
            Container top = ( c == null ? null : SwingUtilities.getAncestorOfClass(Window.class, c));
            if (top instanceof JFrame) {
                dialog = new JDialog((JFrame)top);
            }
            else if (top instanceof JDialog) {
                dialog = new JDialog((JDialog)top);
            }
            else { 
                dialog = new JDialog();
            }
            
            dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            dialog.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    dismissDialog();
                }
            });
            dialog.setContentPane(this);
            dialog.pack();
        }
        if (c != null) {
            dialog.setLocationRelativeTo(c);
        }
        dialog.setVisible(true);
    }
    
    public void doLayout() {
        if (SwingUtilities.isDescendingFrom(this, dialog)) {
            closeButton.setVisible(true);
        }
        else {
            closeButton.setVisible(false);
        }
        super.doLayout();
    }
    
    private PropertyTreeTableModel getPropertyTreeTableModel() {
        return (PropertyTreeTableModel)table.getModel();
    }
    
    private OptionListEditor optionListEditor;
    private CommandBlockEditor commandBlockEditor;
    
    private class PropertyTableMouseListener extends MouseAdapter {
        public void mouseClicked(MouseEvent e) {
            Point p = e.getPoint();
            int row = table.rowAtPoint(p);
            int column = table.columnAtPoint(p);
            PropertyTreeTableModel ptm = getPropertyTreeTableModel();
            if (!ptm.isLeaf(row)) {
                ptm.toggleExpanded(row);
            }
        }
    }
    
    private class PropertyTable extends JTable {
        PropertyTable(PropertyTreeTableModel model) {
            super(model);
        }
        
        public TableCellEditor getCellEditor(int row, int column) {
            if (column == 0) {
                return super.getCellEditor(row, column);
            }
            else {
                PropertyTreeTableModel model = getPropertyTreeTableModel();
                Class type = model.getPropertyType(row);
                if (type.equals(ListOption[].class)) {
                    if (optionListEditor == null) {
                        optionListEditor = new OptionListEditor();
                    }
                    return optionListEditor;
                }
                else if (type.equals(CommandBlock.class)) {
                    if (commandBlockEditor == null) {
                        commandBlockEditor = new CommandBlockEditor();
                    }
                    return commandBlockEditor;
                }
                return super.getCellEditor(row, column);
            }
        }    
    }
    
    /**
     * Cell renderer.
     */
    public static class PropertyValueCellRenderer extends JLabel implements TableCellRenderer, TreeCellRenderer, ListCellRenderer {
        
        JCheckBox booleanRenderer;
        
        public PropertyValueCellRenderer() {
            super("Label");
            setFont(getFont().deriveFont(Font.PLAIN));
            setOpaque(true);
            setBorder(new EmptyBorder(5,5,5,5));
            booleanRenderer = new JCheckBox();
            booleanRenderer.setBorder(new EmptyBorder(5,5,5,5));
        }
        public Component getTableCellRendererComponent(JTable table,
        Object value,
        boolean isSelected,
        boolean hasFocus,
        int row,
        int column) {
            PropertyTreeTableModel model = null;
            if (table != null) {
                model = (PropertyTreeTableModel)table.getModel();
            }
            if (value instanceof Enumeration) {
                Enumeration enum = (Enumeration)value;
                setIcon(enum.getListIcon());
            }
            else {
                setIcon(null);
            }
            if (value instanceof Boolean) {
                booleanRenderer.setSelected(((Boolean)value).booleanValue());
                booleanRenderer.setText(value.toString());
                booleanRenderer.setBackground(isSelected ? Color.gray : Color.lightGray);
                return booleanRenderer;
            }
            else if ((value instanceof Double) || (value instanceof Float)) {
                double doubleValue = ((Number)value).doubleValue();
                if (doubleValue < 0.01 || doubleValue >= 100.0) {
                    value = expFormat.format(doubleValue);
                }
            }
            if (model == null) {
                setText(value==null ? "null" : (value instanceof Editable ? "Click to expand/collapse" : value.toString()));
            }
            else {
                setText(value==null ? "null" : (model.isLeaf(row) ? value.toString() : "Click to expand/collapse"));
            }
            setOpaque(true);
            setBackground(isSelected ? Color.gray : Color.lightGray);
            return this;
        }
        public Component getTreeCellRendererComponent(JTree tree,
        Object value,
        boolean isSelected,
        boolean isExpanded,
        boolean isLeaf,
        int row,
        boolean hasFocus) {
            setText(value==null ? "null" : value.toString());
            setIcon(null);
            setOpaque(false);
            return this;
        }
        
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }
        
        /** Return a component that has been configured to display the specified
         * value. That component's <code>paint</code> method is then called to
         * "render" the cell.  If it is necessary to compute the dimensions
         * of a list because the list cells do not have a fixed size, this method
         * is called to generate a component on which <code>getPreferredSize</code>
         * can be invoked.
         *
         * @param list The JList we're painting.
         * @param value The value returned by list.getModel().getElementAt(index).
         * @param index The cells index.
         * @param isSelected True if the specified cell was selected.
         * @param cellHasFocus True if the specified cell has the focus.
         * @return A component whose paint() method will render the specified value.
         *
         * @see JList
         * @see ListSelectionModel
         * @see ListModel
         */
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value instanceof Enumeration) {
                Enumeration enum = (Enumeration)value;
                setIcon(enum.getListIcon());
            }
            else {
                setIcon(null);
            }
            setText(value.toString());
            setOpaque(true);
            setBackground(isSelected ? Color.gray : Color.lightGray);
            return this;
        }
        
    }

    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    private class PropertyTreeTableModel extends DefaultTreeModel implements TableModel, TreeExpansionListener, TreeModelListener {
        
        PropertyTreeRenderer propertyTree;
        
        EventListenerList listenerList = new EventListenerList();
        
        PropertyTreeTableModel(Editable e) {
            super(null);
            PropertyTreeNode root = new PropertyTreeNode(e);
            root.setTreeModelListener(this);
            setRoot(root);
            propertyTree = new PropertyTreeRenderer(this);
            propertyTree.addTreeExpansionListener(this);
        }
        
        public PropertyTreeRenderer getRenderer() {
            return propertyTree;
        }
        
        public int getRowCount() {
            return propertyTree.getRowCount();
        }
        
        public int getColumnCount() {
            return 2;
        }
        
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            PropertyTreeNode node = (PropertyTreeNode)propertyTree.getPathForRow(rowIndex).getLastPathComponent();
            return columnIndex == 1 && node.isLeaf();
        }
        
        public String getColumnName(int columnIndex) {
            return (columnIndex==0 ? "Property" : "Value");
        }
        
        public Class getColumnClass(int columnIndex) {
            return Object.class;
        }
        
        public Object getValueAt(int row, int column) {
            if (column == 0) return getPropertyName(row);
            return getPropertyValue(row);
        }
        
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                throw new IllegalArgumentException("Can't edit property names");
            }
            PropertyTreeNode node = (PropertyTreeNode)propertyTree.getPathForRow(rowIndex).getLastPathComponent();
            node.setValue(value);
        }
        
        public void treeCollapsed(TreeExpansionEvent e) {
            TreePath path = e.getPath();
            int row = propertyTree.getRowForPath(path);
            int count = propertyTree.getModel().getChildCount(path.getLastPathComponent());
            fireTableRowsDeleted(row+1, row+count);
        }
        
        public void treeExpanded(TreeExpansionEvent e) {
            TreePath path = e.getPath();
            int row = propertyTree.getRowForPath(path);
            int count = propertyTree.getModel().getChildCount(path.getLastPathComponent());
            fireTableRowsInserted(row+1, row+count);
        }
        
        public void toggleExpanded(int row) {
            if (propertyTree.isCollapsed(row)) {
                propertyTree.expandRow(row);
            }
            else {
                propertyTree.collapseRow(row);
            }
        }
        
        /** Adds a listener to the list that is notified each time a change
         * to the data model occurs.
         *
         * @param	l		the TableModelListener
         */
        public void addTableModelListener(TableModelListener l) {
            listenerList.add(TableModelListener.class, l);
        }
        
        /** Removes a listener from the list that is notified each time a
         * change to the data model occurs.
         *
         * @param	l		the TableModelListener
         */
        public void removeTableModelListener(TableModelListener l) {
            listenerList.remove(TableModelListener.class, l);
        }
        
        private void fireTableRowsInserted(int start, int end) {
            fireTableChanged(new TableModelEvent(this, start, end, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT));
        }
        
        private void fireTableRowsDeleted(int start, int end) {
            fireTableChanged(new TableModelEvent(this, start, end, TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE));
        }
        
        private void fireTableChanged(TableModelEvent e) {
            Object[] listeners = listenerList.getListenerList();
        	for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i]==TableModelListener.class) {
                    ((TableModelListener)listeners[i+1]).tableChanged(e);
                }
            }
        }

        /** <p>Invoked after a node (or a set of siblings) has changed in some
         * way. The node(s) have not changed locations in the tree or
         * altered their children arrays, but other attributes have
         * changed and may affect presentation. Example: the name of a
         * file has changed, but it is in the same location in the file
         * system.</p>
         * <p>To indicate the root has changed, childIndices and children
         * will be null. </p>
         *
         * <p>Use <code>e.getPath()</code>
         * to get the parent of the changed node(s).
         * <code>e.getChildIndices()</code>
         * returns the index(es) of the changed node(s).</p>
         */
        public void treeNodesChanged(TreeModelEvent e) {
            this.fireTreeNodesChanged(this, e.getPath(), e.getChildIndices(), e.getChildren());
        }
        
        /** <p>Invoked after nodes have been inserted into the tree.</p>
         *
         * <p>Use <code>e.getPath()</code>
         * to get the parent of the new node(s).
         * <code>e.getChildIndices()</code>
         * returns the index(es) of the new node(s)
         * in ascending order.</p>
         */
        public void treeNodesInserted(TreeModelEvent e) {
            this.fireTreeNodesInserted(this, e.getPath(), e.getChildIndices(), e.getChildren());
        }
        
        /** <p>Invoked after nodes have been removed from the tree.  Note that
         * if a subtree is removed from the tree, this method may only be
         * invoked once for the root of the removed subtree, not once for
         * each individual set of siblings removed.</p>
         *
         * <p>Use <code>e.getPath()</code>
         * to get the former parent of the deleted node(s).
         * <code>e.getChildIndices()</code>
         * returns, in ascending order, the index(es)
         * the node(s) had before being deleted.</p>
         */
        public void treeNodesRemoved(TreeModelEvent e) {
            this.fireTreeNodesRemoved(this, e.getPath(), e.getChildIndices(), e.getChildren());
        }
        
        /** <p>Invoked after the tree has drastically changed structure from a
         * given node down.  If the path returned by e.getPath() is of length
         * one and the first element does not identify the current root node
         * the first element should become the new root of the tree.<p>
         *
         * <p>Use <code>e.getPath()</code>
         * to get the path to the node.
         * <code>e.getChildIndices()</code>
         * returns null.</p>
         */
        public void treeStructureChanged(TreeModelEvent e) {
            this.fireTreeStructureChanged(this, e.getPath(), e.getChildIndices(), e.getChildren());
        }
        
        public boolean isLeaf(int row) {
            PropertyTreeNode node = (PropertyTreeNode)propertyTree.getPathForRow(row).getLastPathComponent();
            return node.isLeaf();
        }
        
        public String getPropertyName(int row) {
            PropertyTreeNode node = (PropertyTreeNode)propertyTree.getPathForRow(row).getLastPathComponent();
            return node.getName();
        }
        
        public Object getPropertyValue(int row) {
            PropertyTreeNode node = (PropertyTreeNode)propertyTree.getPathForRow(row).getLastPathComponent();
            return node.getValue();
        }
        
        public Class getPropertyType(int row) {
            PropertyTreeNode node = (PropertyTreeNode)propertyTree.getPathForRow(row).getLastPathComponent();
            return node.getPropertyType();
        }
        
        public PropertyDescriptor getPropertyDescriptor(int row){
            PropertyTreeNode node = (PropertyTreeNode)propertyTree.getPathForRow(row).getLastPathComponent();
            return node.getPropertyDescriptor();
        }
    }
    
    protected static class PropertyCellEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {
        
        private JFormattedTextField integerField;
        private JFormattedTextField floatField;
        private JTextField stringField;
        private DatumEditor datumEditor;
        private JCheckBox booleanBox;
        private JButton editableButton;
        private JComboBox enumerationChoice;
        
        private static final int INTEGER      = 0x001;
        private static final int LONG         = 0x002;
        private static final int FLOAT        = 0x004;
        private static final int DOUBLE       = 0x008;
        private static final int STRING       = 0x010;
        private static final int DATE         = 0x020;
        private static final int BOOLEAN      = 0x040;
        private static final int EDITABLE     = 0x080;
        private static final int ENUMERATION  = 0x100;
        private static final int DATUM        = 0x200;
        
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
            
            /*
            DateFormatter dateFormatter= new DateFormatter(
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS"));
            dateField = new JFormattedTextField(dateFormatter);
            dateField.addActionListener(this);
             */
            
            editableButton = new JButton("Edit");
            editableButton.addActionListener(this);
            this.propertyTree = propertyTree;
            enumerationChoice = new JComboBox();
            enumerationChoice.setRenderer(new PropertyValueCellRenderer());
            enumerationChoice.addActionListener(this);
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
        
        public Component getTableCellEditorComponent(JTable table,
        Object value,
        boolean isSelected,
        int row, int column) {
            currentValue = value;
            currentRow = row;
            PropertyTreeTableModel model = (PropertyTreeTableModel)table.getModel();
            if (!model.isLeaf(row)) {
                editorState = EDITABLE;
                return editableButton;
            }
            else if (value instanceof Enumeration) {
                editorState = ENUMERATION;
                Object[] list = getEnumerationList(value.getClass());
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
            else if (value instanceof Editable) {
                editorState = EDITABLE;
                return editableButton;
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
                return datumEditor;
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
                catch ( java.text.ParseException ex ) {
                    DasExceptionHandler.handle(ex);
                    return currentValue;
                }
            }
            else {
                return stringField.getText();
            }
        }
        
    }
    
    
    static class PropertyTreeRenderer extends JTree implements TableCellRenderer {
        private JTable table;
        private int visibleRow;
        public PropertyTreeRenderer(PropertyTreeTableModel model) {
            super(model);
            setRootVisible(false);
            setShowsRootHandles(true);
        }
        
        public void setBounds(int x, int y, int w, int h) {
            super.setBounds(x, 0, w, table.getRowHeight()*table.getRowCount());
        }
        
        public void paint(Graphics g) {
            g.translate(0, -visibleRow * getHeight()/getRowCount());
            super.paint(g);
        }
        
        public Component getTableCellRendererComponent(JTable table,
        Object value,
        boolean isSelected,
        boolean hasFocus,
        int row,
        int column) {
            this.table = table;
            this.visibleRow = row;
            setBackground(isSelected ? Color.gray : Color.lightGray);
            return this;
        }
    }
        
    private class PropertyTreeNode implements TreeNode {
        
        /**
         * The parent node.
         */
        protected PropertyTreeNode parent;
        
        /**
         * The PropertyDescriptor for this node.
         */
        protected PropertyDescriptor property;
        
        boolean hasChanged;
        
        /**
         * The value of the property
         */
        protected Object value;
        
        /**
         *
         */
        protected boolean valueInitialized = false;
        
        /**
         * List of the children of this node.
         */
        protected List childList;
        
        /**
         * Event listener for this node.  Only one listener allowed.
         */
        protected TreeModelListener listener;
        
        protected boolean isIndexed = false;
        
        protected int index;
        
        /**
         * Construct a root node for the property tree for the given Editable.
         */
        PropertyTreeNode(Editable e) {
            this.parent = null;
            this.property = null;
            this.listener = null;
            this.value = e;
            this.valueInitialized = true;
        }
        
        PropertyTreeNode(PropertyDescriptor property, PropertyTreeNode parent, TreeModelListener listener) {
            this.parent = parent;
            this.property = property;
            this.listener = listener;
        }
        
        PropertyTreeNode(PropertyDescriptor property, PropertyTreeNode parent, int index, TreeModelListener listener) {
            this.parent = parent;
            this.property = property;
            this.listener = listener;
            this.index = index;
            isIndexed = true;
        }
        
        private void maybeInitialize() {
            if (childList != null || isLeaf()) return;
            if (property instanceof IndexedPropertyDescriptor && !isIndexed) {
                int childCount = ((Object[])value).length;
                childList = new ArrayList(childCount);
                for (int index = 0; index < childCount; index++) {
                    childList.add(new PropertyTreeNode(property, this, index, listener));
                }
            }
            else {
                try {
                    BeanInfo info = Introspector.getBeanInfo(getValue().getClass());
                    PropertyDescriptor[] properties = info.getPropertyDescriptors();
                    childList = new ArrayList(properties.length);
                    for (int index = 0; index < properties.length; index++) {
                        childList.add(new PropertyTreeNode(properties[index], this, listener));
                    }
                }
                catch (IntrospectionException ie) {
                    throw new RuntimeException(ie);
                }
            }
        }
        
        public Class getPropertyType() {
            if (property == null) {
                return Void.TYPE;
            }
            else if (isIndexed) {
                return ((IndexedPropertyDescriptor)property).getIndexedPropertyType();
            }
            else {
                return property.getPropertyType();
            }
        }
        
        public PropertyDescriptor getPropertyDescriptor() {
            return property;
        }
        
        /**
         * Returns the value this node represents.
         */
        public Object getValue() {
            if (valueInitialized) {
                return value;
            }
            try {
                if (isIndexed) {
                    IndexedPropertyDescriptor iProperty = (IndexedPropertyDescriptor)property;
                    PropertyTreeNode pp = (PropertyTreeNode)parent.getParent();
                    Method readMethod = iProperty.getIndexedReadMethod();
                    Object[] args = {new Integer(index)};
                    value = readMethod.invoke(pp.getValue(), args);
                }
                else {
                    Method readMethod = property.getReadMethod();
                    Object parentValue = parent.getValue();
                    value = readMethod.invoke(parentValue, null);
                }
                valueInitialized = true;
                return value;
            }
            catch (IllegalAccessException iae) {
                throw new RuntimeException(iae);
            }
            catch (InvocationTargetException ite) {
                throw new RuntimeException(ite);
            }
        }
        
        /** Returns the children of the receiver as an <code>Enumeration</code>.
         */
        public java.util.Enumeration children() {
            maybeInitialize();
            return Collections.enumeration(childList);
        }
        
        /** Returns true if the receiver allows children.
         */
        public boolean getAllowsChildren() {
            return isLeaf();
        }
        
        /** Returns the child <code>TreeNode</code> at index
         * <code>childIndex</code>.
         */
        public TreeNode getChildAt(int childIndex) {
            maybeInitialize();
            if (isLeaf()) {
                return null;
            }
            return (PropertyTreeNode)childList.get(childIndex);
        }
        
        /** Returns the number of children <code>TreeNode</code>s the receiver
         * contains.
         */
        public int getChildCount() {
            maybeInitialize();
            if (isLeaf()) {
                return 0;
            }
            return childList.size();
        }
        
        /** Returns the index of <code>node</code> in the receivers children.
         * If the receiver does not contain <code>node</code>, -1 will be
         * returned.
         */
        public int getIndex(TreeNode node) {
            if (isLeaf()) {
                return -1;
            }
            for (int index = 0; index < childList.size(); index++) {
                if (node == childList.get(index)) {
                    return index;
                }
            }
            return -1;
        }
        
        /** Returns the parent <code>TreeNode</code> of the receiver.
         */
        public TreeNode getParent() {
            return parent;
        }
        
        /** Returns true if the receiver is a leaf.
         */
        public boolean isLeaf() {
            if (value == null) {
                return true;
            }
            if (value instanceof Editable) {
                return false;
            }
            if (property instanceof IndexedPropertyDescriptor && !isIndexed) {
                return false;
            }
            return true;
        }
        
        /**
         * Returns the name of this property.
         */
        public String getName() {
            if (property == null) {
                return "root";
            }
            if (isIndexed) {
                return property.getName() + "[" + index + "]";
            }
            return property.getName();
        }
        
        /**
         * Sets the valueof this node.
         */
        public void setValue(Object obj) {
            this.value = obj;
            hasChanged = true;
            globalHasChanged = true;
        }
        
        public void flushChanges() {
            try {
                if (hasChanged) {
                    if (parent == null) return;
                    if (isIndexed) {
                        PropertyTreeNode pp = (PropertyTreeNode)parent.getParent();
                        IndexedPropertyDescriptor iProperty = (IndexedPropertyDescriptor)property;
                        Object parentValue = pp.getValue();
                        Object[] args = {new Integer(index), value};
                        Method writeMethod = iProperty.getIndexedWriteMethod();
                        if (writeMethod == null) {
                            DasExceptionHandler.handle(new NullPointerException(
                                "<html><body>'" + getName() + "' is a read-only property<br>"
                                + "Ed, find a better way to handle this!!!"));
                        }
                        writeMethod.invoke(parentValue, args);
                    }
                    else {
                        Object parentValue = parent.getValue();
                        Object[] args = {value};
                        Method writeMethod = property.getWriteMethod();
                        if (writeMethod == null) {
                            DasExceptionHandler.handle(new NullPointerException(
                                "<html><body>'" + getName() + "' is a read-only property<br>"
                                + "Ed, find a better way to handle this!!!"));
                        }
                        writeMethod.invoke(parentValue, args);
                    }
                }
                if (childList != null) {
                    for (Iterator i = childList.iterator(); i.hasNext();) {
                        PropertyTreeNode node = (PropertyTreeNode)i.next();
                        node.flushChanges();
                    }
                }
                childList = null;
                maybeInitialize();
                hasChanged = false;
                fireTreeStructureChanged();
            }
            catch (IllegalAccessException iae) {
                throw new RuntimeException(iae);
            }
            catch (InvocationTargetException ite) {
                ite.getTargetException().printStackTrace();
                DasExceptionHandler.handle((Exception)ite.getTargetException());
            }
        }
        
        /**
         * Returns the name of this property.
         */
        public String toString() {
            return getName();
        }
        
        public boolean isEditable() {
            return property.getWriteMethod() != null;
        }
        
        private void setTreeModelListener(TreeModelListener listener) {
            this.listener = listener;
        }
        
        private void fireTreeStructureChanged() {
            TreeModelEvent evt = new TreeModelEvent(this, getPath());
            listener.treeStructureChanged(evt);
        }
        
        Object[] getPath() {
            return getPath(1);
        }
        
        Object[] getPath(int count) {
            if (parent == null) {
                Object[] path = new Object[count];
                path[0] = this;
                return path;
            }
            Object[] path = parent.getPath(count + 1);
            path[path.length - count] = this;
            return path;
        }
        
    }
    
    protected static class FloatingPointFormatter extends AbstractFormatter {
                
        /** Parses <code>text</code> returning an arbitrary Object. Some
         * formatters may return null.
         *
         * @throws ParseException if there is an error in the conversion
         * @param text String to convert
         * @return Object representation of text
         */
        public Object stringToValue(String text) throws ParseException {
            try {
                Double d = new Double(text);
                if (d.doubleValue() == Double.NEGATIVE_INFINITY ||
                    d.doubleValue() == Double.POSITIVE_INFINITY ||
                    d.doubleValue() == Double.NaN) {
                    throw new ParseException("+/-infinity and NaN are not allowed", 0);
                }
                return d;
            }
            catch (NumberFormatException nfe) {
                throw new ParseException(nfe.getMessage(), 0);
            }
        }
        
        /** Returns the string value to display for <code>value</code>.
         *
         * @throws ParseException if there is an error in the conversion
         * @param value Value to convert
         * @return String representation of value
         */
        public String valueToString(Object value) throws ParseException {
            if (value instanceof Number) {
                double doubleValue = ((Number)value).doubleValue();
                if (doubleValue < 0.01 || doubleValue > 99)
                    return expFormat.format(doubleValue);
                return value.toString();
            }
            else throw new ParseException("value must be of type Number", 0);
        }
        
        protected DocumentFilter getDocumentFilter() {
            return new FloatingPointDocumentFilter();
        }
        
    }
    
    private static class FloatingPointDocumentFilter extends DocumentFilter {
        
        public void insertString(FilterBypass fb, int offset, String text, AttributeSet atts) throws BadLocationException {
            if (text.length() == 1) {
                if (text.charAt(0) == '-') {
                    String content = fb.getDocument().getText(0, fb.getDocument().getLength());
                    int eIndex = Math.max(content.indexOf('e'), content.indexOf('E'));
                    if (content.length() == 0) {
                        super.insertString(fb, 0, text, atts);
                    }
                    else if (eIndex < 0 || offset <= eIndex) {
                        if (content.charAt(0) == '-') {
                            super.remove(fb, 0, 1);
                        }
                        else {
                            super.insertString(fb, 0, text, atts);
                        }
                    }
                    else {
                        if (content.length() == eIndex+1) {
                            super.insertString(fb, eIndex+1, text, atts);
                        }
                        else if (content.charAt(eIndex+1) == '-') {
                            super.remove(fb, eIndex+1, 1);
                        }
                        else {
                            super.insertString(fb, eIndex+1, text, atts);
                        }
                    }
                }
                else if (text.charAt(0) == '.') {
                    String content = fb.getDocument().getText(0, fb.getDocument().getLength());
                    int dotIndex = content.indexOf('.');
                    if (offset <= dotIndex) {
                        super.replace(fb, offset, dotIndex-offset+1, text, atts);
                    }
                    else if (dotIndex < 0) {
                        super.insertString(fb, offset, text, atts);
                    }
                }
                else if (text.charAt(0) == 'e' || text.charAt(0) == 'E') {
                    String content = fb.getDocument().getText(0, fb.getDocument().getLength());
                    int eIndex = Math.max(content.indexOf('e'), content.indexOf('E'));
                    if (offset <= eIndex) {
                        super.replace(fb, offset, eIndex-offset+1, text, atts);
                    }
                    else if (eIndex < 0) {
                        super.insertString(fb, offset, text, atts);
                    }
                }
                else if (Character.isDigit(text.charAt(0))) {
                    super.insertString(fb, offset, text, atts);
                }
            }
        }
        
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet atts) throws BadLocationException {
            remove(fb, offset, length);
            insertString(fb, offset, text, atts);
        }
        
    }
    
}
