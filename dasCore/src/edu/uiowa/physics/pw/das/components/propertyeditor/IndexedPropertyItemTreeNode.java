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

class IndexedPropertyItemTreeNode extends PropertyTreeNode {
    
    private IndexedPropertyDescriptor indexedPropertyDescriptor;
    
    private int index;
    
    IndexedPropertyItemTreeNode(PropertyTreeNode parent, IndexedPropertyDescriptor indexedPropertyDescriptor, int index) {
        super(Array.get(parent.value, index));
        this.index = index;
        this.propertyDescriptor = indexedPropertyDescriptor;
        this.indexedPropertyDescriptor = indexedPropertyDescriptor;
    }
    
    public boolean getAllowsChildren() {
        if (value instanceof Editable) {
            return true;
        }
        else {
            return false;
        }
    }
    
    String getDisplayName() {
        return propertyDescriptor.getName() + "[" + index + "]";
    }
    
    void flush() throws InvocationTargetException {
        try {
            if (dirty) {
                Method writeMethod = indexedPropertyDescriptor.getIndexedWriteMethod();
                writeMethod.invoke(parent.parent.value, new Object[]{new Integer(index), value} );
                dirty = false;
            }
            if (childDirty) {
                for (Iterator i = children.iterator(); i.hasNext(); ) {
                    PropertyTreeNode child = (PropertyTreeNode)i.next();
                    child.flush();
                }
                childDirty = false;
            }
        }
        catch (IllegalAccessException iae) {
            throw new RuntimeException(iae);
        }
    }
    
}

