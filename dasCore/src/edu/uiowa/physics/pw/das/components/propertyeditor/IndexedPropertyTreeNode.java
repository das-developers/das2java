package edu.uiowa.physics.pw.das.components.propertyeditor;

import edu.uiowa.physics.pw.das.util.DasExceptionHandler;
import java.beans.*;
import java.beans.IndexedPropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeNode;

class IndexedPropertyTreeNode extends PropertyTreeNode {
    
    private IndexedPropertyDescriptor indexedPropertyDescriptor;
    
    IndexedPropertyTreeNode(PropertyTreeNode parent, IndexedPropertyDescriptor indexedPropertyDescriptor) throws InvocationTargetException {
        super(parent, indexedPropertyDescriptor);
        this.indexedPropertyDescriptor = indexedPropertyDescriptor;
    }
    
    public boolean getAllowsChildren() {
        return true;
    }
    
    protected void maybeLoadChildren() {
        if (children == null) {
            children = new ArrayList();
            int childCount = Array.getLength(value);
            for (int i = 0; i < childCount; i++) {
                children.add(new IndexedPropertyItemTreeNode(this, indexedPropertyDescriptor, i));
            }
        }
    }
    
    String getDisplayName() {
        return propertyDescriptor.getName() + "[]";
    }
    
    void flush() throws InvocationTargetException {
        if (childDirty) {
            for (Iterator i = children.iterator(); i.hasNext(); ) {
                PropertyTreeNode child = (PropertyTreeNode)i.next();
                child.flush();
            }
            childDirty = false;
        }
    }

    public boolean isCellEditable(int column) {
        return false;
    }
    
}

