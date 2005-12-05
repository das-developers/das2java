package edu.uiowa.physics.pw.das.components.propertyeditor;
import java.beans.IndexedPropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.MutableTreeNode;

class IndexedPropertyTreeNode extends PropertyTreeNode {
    
    private IndexedPropertyDescriptor indexedPropertyDescriptor;
    
    IndexedPropertyTreeNode(PropertyTreeNode parent, IndexedPropertyDescriptor indexedPropertyDescriptor) throws InvocationTargetException {
        super(parent, indexedPropertyDescriptor );
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
    
    protected Object read() {
        if (propertyDescriptor == null) {
            return value;
        } else {
            Method readMethod = propertyDescriptor.getReadMethod();
            if (readMethod == null) {
                String pName = propertyDescriptor.getName();
                String pId = value.getClass().getName() + "#" + pName;
                throw new IllegalStateException(
                        "Null read method for: " + pId);
            }
            try {
                return readMethod.invoke(parent.value, null);
            } catch (IllegalAccessException iae) {
                Error err = new IllegalAccessError(iae.getMessage());
                err.initCause(iae);
                throw err;
            } catch (InvocationTargetException ite) {
                Throwable cause = ite.getCause();
                if (cause instanceof Error) {
                    throw (Error)cause;
                } else if (cause instanceof RuntimeException) {
                    throw (RuntimeException)cause;
                } else {
                    throw new RuntimeException(cause);
                }
            }
        }
    }
    public boolean isCellEditable(int column) {
        return false;
    }
    
    public void refresh( ) {
        Object newValue = read();
        
        value= newValue;
        if ( children!=null ) {            
            if ( children.size()<((Object[])newValue).length ) {
                children=null;
                treeModel.nodeStructureChanged(this);          
            } else if ( children.size()>((Object[])newValue).length ) {                
                children= null;
                treeModel.nodeStructureChanged(this);  
            }
        }
        if (children != null) {
            for (Iterator i = children.iterator(); i.hasNext();) {
                PropertyTreeNode child = (PropertyTreeNode)i.next();
                child.refresh( );
            }
        }
        
    }
}

