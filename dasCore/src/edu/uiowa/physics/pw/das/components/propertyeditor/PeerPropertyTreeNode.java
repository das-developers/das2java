/*
 * PeerPropertyTreeNode.java
 *
 * Created on December 20, 2005, 11:15 AM
 *
 *
 */

package edu.uiowa.physics.pw.das.components.propertyeditor;

import java.beans.PropertyDescriptor;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

/**
 *
 * @author Jeremy
 */
public class PeerPropertyTreeNode implements PropertyTreeNodeInterface {
    PeerPropertyTreeNode parent;
    PropertyTreeNode leader;
    PropertyTreeNode[] peers;
    
    public PeerPropertyTreeNode( PeerPropertyTreeNode parent, PropertyTreeNode leader, PropertyTreeNode[] peers ) {
        this.parent= parent;
        this.leader= leader;
        this.peers= peers;
    }
    
    public java.util.Enumeration children() {
        return new java.util.Enumeration() {
            int index=0;
            public boolean hasMoreElements() {
                return index<getChildCount();
            }
            public Object nextElement() {
                return getChildAt( index++ );
            }
        };
    }
    
    public boolean getAllowsChildren() {
        return leader.getAllowsChildren();
    }
    
    public TreeNode getChildAt(int childIndex) {
        // TODO: handle tree structure change
        PropertyTreeNode[] peerChildren= new PropertyTreeNode[ peers.length ];
        for ( int i=0; i<peers.length; i++ ) {
            peerChildren[i]= (PropertyTreeNode)peers[i].getChildAt( childIndex );
        }
        return new PeerPropertyTreeNode( this, (PropertyTreeNode)leader.getChildAt( childIndex ), peerChildren );
    }
    
    public int getChildCount() {
        return leader.getChildCount();
    }
    
    public Class getColumnClass(int columnIndex) {
        return leader.getColumnClass(columnIndex);
    }
    
    public int getColumnCount() {
        return leader.getColumnCount();
    }
    
    public String getColumnName(int columnIndex) {
        return leader.getColumnName(columnIndex);
    }
    
    public int getIndex(TreeNode node) {
        return leader.getIndex(node);
    }
    
    public TreeNode getParent() {
        return parent;
    }
    
    public Object getValueAt(int column) {
        switch (column) {
            case 0:
                return leader.getDisplayName();
            case 1:
                return getDisplayValue();
            default: throw new IllegalArgumentException("No such column: " + column);
        }
    }
    
    public boolean isCellEditable(int column) {
        return leader.isCellEditable(column);
    }
    
    public boolean isDirty() {
        return leader.isDirty();
    }
    
    public boolean isLeaf() {
        return leader.isLeaf();
    }
    
    public void refresh() {
        leader.refresh();
    }
    
    public void setValueAt(Object value, int column) {
        switch (column) {
            case 0:
                throw new IllegalArgumentException("Cell is not editable");
            case 1:
                setValue(value);
                break;
            default: throw new IllegalArgumentException("No such column: " + column);
        }
    }
    
    public void setTreeModel(DefaultTreeModel treeModel) {
        for ( int i=0; i<peers.length; i++ ) {
            peers[i].setTreeModel(treeModel);
        }
        leader.setTreeModel(treeModel);
    }
    
    public Object getValue() {
        return leader.getValue();
    }
    
    public Object getDisplayValue() {
        Object value= leader.getDisplayValue();
        boolean warn= false;
        for ( int i=0; i<peers.length; i++ ) {
            Object peerValue= peers[i].getDisplayValue();
            if ( peerValue!=null ) {
                if ( !peers[i].getDisplayValue().equals(value) ) {
                    warn = true;
                }
            } else {
                if ( value!=null ) {
                    warn = true;
                }
            }
        }
        return value;        
    }
    
    public void flush() {
        for ( int i=0; i<peers.length; i++ ) {
            peers[i].flush();
        }
        leader.flush();
    }
    
    public PropertyDescriptor getPropertyDescriptor() {
        return leader.getPropertyDescriptor();
    }
    
    public String toString() {
        return leader.getDisplayName()+" ++";
    }
    
    public void setValue(Object value) {
        for ( int i=0; i<peers.length; i++ ) {
            peers[i].setValue(value);
        }
        leader.setValue(value);
    }
    
}
