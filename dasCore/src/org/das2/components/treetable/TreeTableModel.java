package org.das2.components.treetable;

import javax.swing.JTree;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

public class TreeTableModel extends AbstractTableModel implements TableModel {
    
    private TreeTableNode root;
    
    private JTree tree;
    
    public TreeTableModel(TreeTableNode root, JTree tree) {
        this.root = root;
        this.tree = tree;
        tree.addTreeExpansionListener(new TreeTableTreeListener());
        tree.getModel().addTreeModelListener(new TreeTableTreeModelListener());
    }
    
    @Override
    public Class getColumnClass(int columnIndex) {
        return root.getColumnClass(columnIndex);
    }
    
    public int getColumnCount() {
        return root.getColumnCount();
    }
    
    @Override
    public String getColumnName(int columnIndex) {
        return root.getColumnName(columnIndex);
    }
    
    public int getRowCount() {
        return tree.getRowCount();
    }
    
    public Object getValueAt(int rowIndex, int columnIndex) {
        return getNodeForRow(rowIndex).getValueAt(columnIndex);
    }
    
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return getNodeForRow(rowIndex).isCellEditable(columnIndex);
    }
    
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        getNodeForRow(rowIndex).setValueAt(aValue, columnIndex);
    }
    
    public void toggleExpanded(int rowIndex) {
        if (tree.isExpanded(rowIndex)) {
            tree.collapseRow(rowIndex);
        }
        else {
            tree.expandRow(rowIndex);
        }
    }
    
    public void expand( int rowIndex ) {
        if ( tree.isCollapsed(rowIndex) ) tree.expandRow(rowIndex);
    }
    
    public void collapse( int rowIndex ) {
        if ( tree.isExpanded(rowIndex) ) tree.collapseRow(rowIndex);
    }
    
    public TreeTableNode getNodeForRow(int rowIndex) {
        TreePath path = tree.getPathForRow(rowIndex);
        return (TreeTableNode)path.getLastPathComponent();
    }
    
    public TreeTableNode getRoot() {
        return root;
    }
    
    public void setRoot(TreeTableNode node) {
        if (node == null) {
            throw new NullPointerException("null root node not allowed");
        }
        tree.setModel(new DefaultTreeModel(node, true));
    }
    
    private class TreeTableTreeModelListener implements TreeModelListener {
        
        public void treeNodesChanged(TreeModelEvent e) {
            TreePath path = new TreePath(e.getPath());
            int row = tree.getRowForPath(path);
            TreeTableNode node = (TreeTableNode)path.getLastPathComponent();
            int count = node.getChildCount();
            if (row != -1 && tree.isExpanded(row)) {
                TreeTableModel.this.fireTableRowsUpdated(row + 1, row + count);
            }
        }
        
        public void treeNodesInserted(TreeModelEvent e) {
            TreePath path = e.getTreePath();
            int row = tree.getRowForPath(path);
            if (row != -1 && tree.isExpanded(row)) {
                int[] indices = e.getChildIndices();
                java.util.Arrays.sort(indices);
                for (int i = 0; i < indices.length; i++) {
                    TreeTableModel.this.fireTableRowsInserted(indices[i], indices[i]);
                }
            }
        }
        
        public void treeNodesRemoved(TreeModelEvent e) {
            TreePath path = e.getTreePath();
            int row = tree.getRowForPath(path);
            if (row != -1 && tree.isExpanded(row)) {
                int[] indices = e.getChildIndices();
                java.util.Arrays.sort(indices);
                for (int i = indices.length - 1; i >= 0; i++) {
                    TreeTableModel.this.fireTableRowsDeleted(indices[i], indices[i]);
                }
            }
        }
        
        public void treeStructureChanged(TreeModelEvent e) {
            TreeTableModel.this.fireTableStructureChanged();
        }
        
    }
    
    private class TreeTableTreeListener implements TreeExpansionListener {
        
        public void treeCollapsed(TreeExpansionEvent event) {
            TreePath path = event.getPath();
            int row = tree.getRowForPath(path);
            TreeTableNode node = (TreeTableNode)path.getLastPathComponent();
            int count = node.getChildCount();
            if (count != 0) {
                TreeTableModel.this.fireTableRowsDeleted(row + 1, row + count);
            }
        }
        
        public void treeExpanded(TreeExpansionEvent event) {
            TreePath path = event.getPath();
            int row = tree.getRowForPath(path);
            TreeTableNode node = (TreeTableNode)path.getLastPathComponent();
            int count = node.getChildCount();
            if (count != 0) {
                TreeTableModel.this.fireTableRowsInserted(row + 1, row + count);
            }
        }
        
    }
    
}

