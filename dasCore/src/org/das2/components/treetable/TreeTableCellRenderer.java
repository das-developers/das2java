package org.das2.components.treetable;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.TreeModel;

public class TreeTableCellRenderer extends JTree implements TableCellRenderer {
    
    private JTable table;
    
    private int visibleRow;
    
    public TreeTableCellRenderer(TreeModel model) {
        super(model);
    }
    
    public void setBounds(int x, int y, int w, int h) {
        super.setBounds(x, 0, w, table.getRowHeight()*table.getRowCount());
    }
    
    public void paint(Graphics g) {
        g.translate(0, -visibleRow * getHeight()/getRowCount());
        super.paint(g);
    }
    
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        this.table = table;
        this.visibleRow = row;
        if (table != null) {
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
        }
        return this;
    }
    
}

