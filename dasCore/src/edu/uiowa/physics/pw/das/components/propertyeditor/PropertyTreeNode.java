package edu.uiowa.physics.pw.das.components.propertyeditor;

import edu.uiowa.physics.pw.das.components.treetable.TreeTableNode;
import edu.uiowa.physics.pw.das.util.DasExceptionHandler;
import java.beans.*;
import java.beans.IndexedPropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Enumeration;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeNode;

class PropertyTreeNode implements TreeNode, TreeTableNode {
    
    static {
        String[] beanInfoSearchPath = { "edu.uiowa.physics.pw.das.beans", "sun.beans.infos" };
        Introspector.setBeanInfoSearchPath(beanInfoSearchPath);
    }
    
    protected static final Object[] NULL_ARGS = new Object[0];
    
    protected List children;
    
    protected PropertyTreeNode parent;
    
    protected PropertyDescriptor propertyDescriptor;
    
    protected Object value;
    
    protected boolean dirty;
    
    protected boolean childDirty;
    
    PropertyTreeNode(Object value) {
        this.value = value;
    }
    
    PropertyTreeNode(PropertyTreeNode parent, PropertyDescriptor propertyDescriptor) throws InvocationTargetException {
        this.parent = parent;
        this.propertyDescriptor = propertyDescriptor;
        try {
            value = propertyDescriptor.getReadMethod().invoke(parent.value, NULL_ARGS);
        }
        catch (IllegalAccessException iae) {
            throw new RuntimeException(iae);
        }
    }
    
    public Enumeration children() {
        maybeLoadChildren();
        return Collections.enumeration(children);
    }
    
    public boolean getAllowsChildren() {
        return value instanceof Editable;
    }
    
    public TreeNode getChildAt(int childIndex) {
        maybeLoadChildren();
        return (TreeNode)children.get(childIndex);
    }
    
    public int getChildCount() {
        maybeLoadChildren();
        return children.size();
    }
    
    public int getIndex(TreeNode node) {
        maybeLoadChildren();
        return children.indexOf(node);
    }
    
    public TreeNode getParent() {
        return parent;
    }
    
    public boolean isLeaf() {
        maybeLoadChildren();
        return children.isEmpty();
    }
    
    PropertyDescriptor getPropertyDescriptor() {
        return propertyDescriptor;
    }
    
    protected void maybeLoadChildren() {
        if (children == null) {
            children = new ArrayList();
            if (value instanceof Editable) {
                try {
                    BeanInfo info = Introspector.getBeanInfo(value.getClass());
                    PropertyDescriptor[] properties = info.getPropertyDescriptors();
                    for (int i = 0; i < properties.length; i++) {
                        if (properties[i].getReadMethod() != null) {
                            if (properties[i] instanceof IndexedPropertyDescriptor) {
                                children.add(new IndexedPropertyTreeNode(this, (IndexedPropertyDescriptor)properties[i]));
                            }
                            else {
                                children.add(new PropertyTreeNode(this, properties[i]));
                            }
                        }
                    }
                }
                catch (IntrospectionException ie) {
                    throw new RuntimeException(ie);
                }
                catch (InvocationTargetException ite) {
                    DasExceptionHandler.handle(ite.getCause());
                }
            }
        }
    }
    
    Object getValue() {
        return value;
    }
    
    Object getDisplayValue() {
        if (getAllowsChildren()) {
            return "Click to expand/collapse";
        }
        else {
            return value;
        }
    }
    
    String getDisplayName() {
        if (propertyDescriptor == null) {
            return "root";
        }
        return propertyDescriptor.getName();
    }
    
    void setValue(Object obj) {
        if (obj == value) {
            return;
        }
        else if (obj != null && obj.equals(value)) {
            return;
        }
        else {
            value = obj;
            setDirty();
        }
    }
    
    void flush() throws InvocationTargetException {
        try {
            if (dirty) {
                Method writeMethod = propertyDescriptor.getWriteMethod();
                writeMethod.invoke(parent.value, new Object[]{value} );
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
    
    private void setDirty() {
        this.dirty = true;
        if (parent != null) {
            parent.setChildDirty();
        }
    }
    
    public boolean isDirty() {
        return dirty || childDirty;
    }
    
    private void setChildDirty() {
        childDirty = true;
        if (parent != null) {
            parent.setChildDirty();
        }
    }
    
    public Object getValueAt(int column) {
        switch (column) {
            case 0:
                return getDisplayName();
            case 1:
                return getDisplayValue();
            default: throw new IllegalArgumentException("No such column: " + column);
        }
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
    
    public boolean isCellEditable(int column) {
        return column == 1 && !(Editable.class.isAssignableFrom(propertyDescriptor.getPropertyType()));
    }
    
    public Class getColumnClass(int columnIndex) {
        return Object.class;
    }
    
    public int getColumnCount() {
        return 2;
    }
    
    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return "Propety name";
            case 1:
                return "Value";
            default: throw new IllegalArgumentException("No such column: " + columnIndex);
        }
    }
    
    public String toString() {
        return getDisplayName();
    }
}

