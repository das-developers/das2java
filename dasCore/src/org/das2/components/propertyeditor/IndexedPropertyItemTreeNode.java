package org.das2.components.propertyeditor;
import org.das2.util.DasExceptionHandler;
import java.beans.IndexedPropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

class IndexedPropertyItemTreeNode extends PropertyTreeNode {
    
    private IndexedPropertyDescriptor indexedPropertyDescriptor;
    
    private int index;
    
    IndexedPropertyItemTreeNode(PropertyTreeNode parent, IndexedPropertyDescriptor indexedPropertyDescriptor, int index) {
        super(Array.get(parent.value, index));
        setTreeModel(parent.treeModel);
        this.index = index;
        this.parent = parent;
        this.propertyDescriptor = indexedPropertyDescriptor;
        this.indexedPropertyDescriptor = indexedPropertyDescriptor;
    }
    
    public boolean getAllowsChildren() {
        return indexedPropertyDescriptor.getPropertyEditorClass() == null;
    }
    
    public String getDisplayName() {
        return propertyDescriptor.getName() + "[" + index + "]";
    }
    
    public void flush()  {
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
        } catch (IllegalAccessException iae) {
            throw new RuntimeException(iae);
        } catch ( InvocationTargetException e ) {
            DasExceptionHandler.handle(e);
        }
    }
    
    protected Object read() {
        Object[] parentValue= (Object[])parent.read();
        return parentValue[this.index];
    }
    
    public void refresh( ) {
        Object newValue = read();
        boolean foldMe= false;
        if ( newValue!=value ) {
            boolean allowsChildren= getAllowsChildren();
            if ( allowsChildren ) {
                foldMe= true;
            }
        }
        if (newValue != value && newValue != null && !newValue.equals(value)) {
            value = newValue;
        }
        if ( foldMe ) {
            children= null;
            treeModel.nodeStructureChanged( this );
        } else {
            if (getAllowsChildren()) {
                if (children != null) {
                    for (Iterator i = children.iterator(); i.hasNext();) {
                        PropertyTreeNode child = (PropertyTreeNode)i.next();
                        child.refresh( );
                    }
                }
            }
        }
        
    }
    
}

