package edu.uiowa.physics.pw.das.components.propertyeditor;

import edu.uiowa.physics.pw.das.components.treetable.TreeTableModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.beans.PropertyDescriptor;
import java.text.DecimalFormat;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.TreeCellRenderer;

/** Cell renderer.
 *
 */
class PropertyCellRenderer extends JLabel implements TableCellRenderer, TreeCellRenderer, ListCellRenderer {
    
    private static DecimalFormat expFormat = new DecimalFormat("0.#######E0");
    
    JCheckBox booleanRenderer;
    
    public PropertyCellRenderer() {
        super("Label");
        setFont(getFont().deriveFont(Font.PLAIN));
        //setOpaque(true);
        //setBorder(new EmptyBorder(5,5,5,5));
        booleanRenderer = new JCheckBox();
        booleanRenderer.setBorder(new EmptyBorder(5,5,5,5));
    }
    
    private TableCellRenderer colorRenderer = new ColorCellRenderer();
    
    private boolean isWritable(JTable table, int row) {
        TreeTableModel model = (TreeTableModel)table.getModel();
        PropertyTreeNodeInterface node = (PropertyTreeNodeInterface)model.getNodeForRow(row);
        PropertyDescriptor pd = node.getPropertyDescriptor();
        return pd.getWriteMethod() != null;
    }
    
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        boolean writable = table == null || isWritable(table, row);
        setEnabled(writable);
        Component c;
        if (value instanceof java.awt.Color) {
            c = colorRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        } else if (value instanceof Boolean) {
            booleanRenderer.setSelected(((Boolean)value).booleanValue());
            booleanRenderer.setText(value.toString());
            booleanRenderer.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            c = booleanRenderer;
        } else {
            if ((value instanceof Double) || (value instanceof Float)) {
                double doubleValue = ((Number)value).doubleValue();
                double mag= Math.abs(doubleValue);
                if ( doubleValue!=0.0 && mag < 0.0001 || mag >= 10000.0) {
                    value = expFormat.format(doubleValue);
                }
            }
            if ( value instanceof Displayable ) {
                setText( ((Displayable)value).getListLabel() );
            } else {
                setText(String.valueOf(value));
            }
            setOpaque(true);
            if (table != null) {
                setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            }
            if (value instanceof Displayable) {
                Displayable enm = (Displayable)value;
                setIcon(enm.getListIcon());
                setDisabledIcon(enm.getListIcon());
            } else {
                setIcon(null);
                setDisabledIcon(null);
            }
            c = this;
            
        }
        
        c.setEnabled(writable);
        return c;        
    }
    
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean isSelected, boolean isExpanded, boolean isLeaf, int row, boolean hasFocus) {
        setText(String.valueOf(value));
        setIcon(null);
        setOpaque(false);
        setEnabled(true);
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
     *
     */
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        if (value instanceof Displayable ) {
            Displayable enm = (Displayable)value;
            setIcon(enm.getListIcon());
            setText(enm.getListLabel());
        } else {
            setIcon(null);
            setText(String.valueOf(value));
        }
        setOpaque(true);
        setBackground(isSelected ? Color.gray : Color.lightGray);
        return this;
    }
    
}

