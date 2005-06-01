package edu.uiowa.physics.pw.das.components.propertyeditor;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
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
    
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof java.awt.Color) {
            return colorRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
        if (value instanceof Enumeration) {
            Enumeration enm = (Enumeration)value;
            setIcon(enm.getListIcon());
        }
        else {
            setIcon(null);
        }
        if (value instanceof Boolean) {
            booleanRenderer.setSelected(((Boolean)value).booleanValue());
            booleanRenderer.setText(value.toString());
//            booleanRenderer.setBackground(isSelected ? Color.gray : Color.lightGray);
            booleanRenderer.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            return booleanRenderer;
        }
        else if ((value instanceof Double) || (value instanceof Float)) {
            double doubleValue = ((Number)value).doubleValue();
            if (doubleValue < 0.01 || doubleValue >= 100.0) {
                value = expFormat.format(doubleValue);
            }
        }
        setText(String.valueOf(value));
        setOpaque(true);
        //setBackground(isSelected ? Color.gray : Color.lightGray);
        if (table != null) {
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
        }
        return this;
    }
    
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean isSelected, boolean isExpanded, boolean isLeaf, int row, boolean hasFocus) {
        setText(String.valueOf(value));
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
     *
     */
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        if (value instanceof Enumeration) {
            Enumeration enum = (Enumeration)value;
            setIcon(enum.getListIcon());
        }
        else {
            setIcon(null);
        }
        setText(String.valueOf(value));
        setOpaque(true);
        setBackground(isSelected ? Color.gray : Color.lightGray);
        return this;
    }
    
}

