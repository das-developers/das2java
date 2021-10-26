/*
 * PeerPropertyTreeNode.java
 *
 * Created on December 20, 2005, 11:15 AM
 *
 *
 */
package org.das2.components.propertyeditor;

import java.beans.PropertyDescriptor;
import java.util.Arrays;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

/**
 *
 * @author Jeremy
 */
public class PeerPropertyTreeNode implements PropertyTreeNodeInterface {

    PeerPropertyTreeNode parent;
    PeerPropertyTreeNode[] children = null;
    PropertyTreeNode leader;
    PropertyTreeNode[] peers;
    private DefaultTreeModel treeModel;

    public PeerPropertyTreeNode(PeerPropertyTreeNode parent, PropertyTreeNode leader, PropertyTreeNode[] peers) {
        this.parent = parent;
        this.leader = leader;
        this.peers = Arrays.copyOf( peers, peers.length );
    }

    public java.util.Enumeration children() {
        return new java.util.Enumeration() {

            int index = 0;

            public boolean hasMoreElements() {
                return index < getChildCount();
            }

            public Object nextElement() {
                return getChildAt(index++);
            }
        };
    }

    public boolean getAllowsChildren() {
        return leader.getAllowsChildren();
    }

    private synchronized PeerPropertyTreeNode[] maybeCreateChildren() {
        if (children == null) {
            children = new PeerPropertyTreeNode[leader.getChildCount()];
            for (int childIndex = 0; childIndex < children.length; childIndex++) {
                // TODO: handle tree structure change
                PropertyTreeNode[] peerChildren = new PropertyTreeNode[peers.length];
                for (int i = 0; i < peers.length; i++) {
                    peerChildren[i] = (PropertyTreeNode) peers[i].getChildAt(childIndex);
                }
                PeerPropertyTreeNode child =
                        new PeerPropertyTreeNode(this, (PropertyTreeNode) leader.getChildAt(childIndex), peerChildren);
                child.treeModel = treeModel;
                children[childIndex] = child;
            }
        }
        return children;
    }

    public TreeNode getChildAt(int childIndex) {
        PeerPropertyTreeNode[] kids= maybeCreateChildren();
        return kids[childIndex];
    }

    public int getChildCount() {
        return leader.getChildCount();
    }

    public Class getColumnClass(int columnIndex) {
        return leader.getColumnClass(columnIndex);
    }

    public int getColumnCount() {
        return 2 + peers.length;
    }

    public String getColumnName(int columnIndex) {
        if (columnIndex > 1) {
            return String.valueOf(peers[columnIndex - 2].getDisplayValue());
        } else {
            return leader.getColumnName(columnIndex);
        }
    }

    public int getIndex(TreeNode node) {
        PeerPropertyTreeNode[] kids= maybeCreateChildren();
        for (int i = 0; i < kids.length; i++) {
            if (node == kids[i]) {
                return i;
            }
        }
        return -1;
    }

    public TreeNode getParent() {
        return parent;
    }

    public Object getValueAt(int column) {
        if (column > 1) {
            return peers[column - 2].getValueAt(1);
        } else {
            switch (column) {
                case 0:
                    return leader.getDisplayName();
                case 1:
                    return getDisplayValue();
                default:
                    throw new IllegalArgumentException("No such column: " + column);
            }
        }
    }

    public boolean isCellEditable(int column) {
        return column > 0;
    }

    public boolean isDirty() {
        return leader.isDirty();
    }

    public boolean isLeaf() {
        return leader.isLeaf();
    }

    public void refresh() {
        leader.refresh();
        for (int i = 0; i < peers.length; i++) {
            peers[i].refresh();
        }
    }

    public void setValueAt(Object value, int column) {
        if (column > 1) {
            peers[column - 2].setValueAt(value, 1);
        } else {
            switch (column) {
                case 0:
                    throw new IllegalArgumentException("Cell is not editable");
                case 1:
                    if (value != PropertyEditor.MULTIPLE) {
                        setValue(value);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("No such column: " + column);
            }
        }
        //treeModel.nodeStructureChanged( this );
        treeModel.nodeChanged(this); //TODO why not this?
    }

    public void setTreeModel(DefaultTreeModel treeModel) {
        this.treeModel = treeModel;
        for (int i = 0; i < peers.length; i++) {
            peers[i].setTreeModel(treeModel);
        }
        leader.setTreeModel(treeModel);
    }

    public Object getValue() {
        return leader.getValue();
    }

    public Object getDisplayValue() {
        Object value = leader.getDisplayValue();
        boolean warn = false;
        for (int i = 0; i < peers.length; i++) {
            Object peerValue = peers[i].getDisplayValue();
            if (peerValue != null) {
                if (!peers[i].getDisplayValue().equals(value)) {
                    warn = true;
                }
            } else {
                if (value != null) {
                    warn = true;
                }
            }
        }
        return warn ? PropertyEditor.MULTIPLE : value;
    }

    public String getDisplayName() {
        return leader.getDisplayName();
    }
    
    public void flush() {
        for (int i = 0; i < peers.length; i++) {
            peers[i].flush();
        }
        leader.flush();
    }

    public PropertyDescriptor getPropertyDescriptor() {
        return leader.getPropertyDescriptor();
    }

    public String toString() {
        return leader.getDisplayName() + "";
    }

    public void setValue(Object value) {
        for (int i = 0; i < peers.length; i++) {
            peers[i].setValue(value);
        }
        leader.setValue(value);
        treeModel.nodeChanged(this);
    }
}
