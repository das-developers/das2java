package edu.uiowa.physics.pw.das.components.propertyeditor;

import edu.uiowa.physics.pw.das.beans.*;
import edu.uiowa.physics.pw.das.components.treetable.TreeTableNode;
import edu.uiowa.physics.pw.das.system.DasLogger;
import edu.uiowa.physics.pw.das.util.DasExceptionHandler;
import java.beans.*;
import java.beans.IndexedPropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Enumeration;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeModel;
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
    
    protected DefaultTreeModel treeModel;
    
    protected Object value;
    
    protected boolean dirty;
    
    protected boolean childDirty;
    
    PropertyTreeNode( Object value ) {
        this.value = value;
    }

    /**
     * Used to put the tree model into the root tree node so that it can be 
     * passed down into the tree.
     */
    protected void setTreeModel( DefaultTreeModel treeModel ) {
        if ( this.treeModel!=null ) throw new IllegalArgumentException("Improper use, see documentation");
        this.treeModel= treeModel;
    }
    
     PropertyTreeNode(PropertyTreeNode parent, PropertyDescriptor propertyDescriptor ) throws InvocationTargetException {
        this.parent = parent;
        this.propertyDescriptor = propertyDescriptor;
        this.treeModel= parent.treeModel;
        if ( treeModel==null ) {
            throw new IllegalArgumentException("null treeModel in parent");
        }
        try {
            if ( propertyDescriptor.getReadMethod()==null ) {
                throw new RuntimeException("read method not defined for "+propertyDescriptor.getName());
            }
            value = propertyDescriptor.getReadMethod().invoke(parent.value, NULL_ARGS);
        } catch (IllegalAccessException iae) {
            throw new RuntimeException(iae);
        }
    }
    
    public Enumeration children() {
        maybeLoadChildren();
        return Collections.enumeration(children);
    }
    
    public boolean getAllowsChildren() {
        
        //No propertyDescriptor indicates the root node.
        if (propertyDescriptor == null) {
            return true;
        }
        //Properties that define an editor should not be expanded
        else if (propertyDescriptor.getPropertyEditorClass() != null) {
            return false;
        } else {
            Class type;
            if (propertyDescriptor instanceof IndexedPropertyDescriptor) {
                IndexedPropertyDescriptor ipd = (IndexedPropertyDescriptor)propertyDescriptor;
                type = ipd.getIndexedPropertyType();
            } else {
                type = propertyDescriptor.getPropertyType();
            }
            
            //Types with identified as editable by PropertyEditor and
            //types with registered PropertyEditors should not be expanded.
            return !PropertyEditor.editableTypes.contains(type)
            && PropertyEditorManager.findEditor(type) == null;
        }
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
        if ( value==null ) return true;
        maybeLoadChildren();
        return children.isEmpty();
    }
    
    PropertyDescriptor getPropertyDescriptor() {
        return propertyDescriptor;
    }
    
    
    protected void maybeLoadChildren() {
        if (children == null) {
            ArrayList children = new ArrayList();
            if (getAllowsChildren()) {
                try {
                    BeanInfo info = Introspector.getBeanInfo(value.getClass());
                    PropertyDescriptor[] properties= info.getPropertyDescriptors();
                    String[] propertyNameList= BeansUtil.getPropertyNames(value.getClass());
                    if ( propertyNameList==null ) {
                        propertyNameList= new String[ properties.length ];
                        for ( int i=0; i<properties.length; i++ ) {
                            propertyNameList[i]= properties[i].getName();
                        }
                    }
                    
                    HashMap nameMap= new HashMap();
                    for ( int i=0; i<properties.length; i++ ) {
                        nameMap.put( properties[i].getName(), properties[i] );
                    }
                    
                    for (int j = 0; j < propertyNameList.length; j++) {
                        PropertyDescriptor pd= (PropertyDescriptor)nameMap.get( propertyNameList[j] );
                        if ( pd==null ) {
                            throw new IllegalArgumentException( "property not found: "+propertyNameList[j] );
                        }
                        if (pd.getReadMethod() != null) {
                            if (pd instanceof IndexedPropertyDescriptor) {
                                children.add( new IndexedPropertyTreeNode(this, (IndexedPropertyDescriptor)pd) );
                            } else {
                                children.add( new PropertyTreeNode(this, pd ));
                            }
                        }
                    }
                } catch (InvocationTargetException ite) {
                    DasExceptionHandler.handle(ite.getCause());
                } catch (IntrospectionException ie) {
                    throw new RuntimeException(ie);
                }
            }
            this.children= children;
        }
    }
    
    Object getValue() {
        return value;
    }
    
    Object getDisplayValue() {
        if ( value instanceof Displayable ) {
            return value;
        }
        boolean allowsChildren= getAllowsChildren();
        
        if ( allowsChildren ) {
            String ss= String.valueOf(value);
            if ( ss.length()<100 ) {
                return ss;
            } else {
                return ss.substring(0,100) + "...";
            }
        } else {
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
        } else if (obj != null && obj.equals(value)) {
            return;
        } else {
            value = obj;
            setDirty();
        }
    }
    
    protected String absPropertyName() {
        if ( this.propertyDescriptor==null ) {
            return "";
        } else {
            String s= parent.absPropertyName();
            return ( s=="" ? "" : s+"." ) + propertyDescriptor.getName();
        }
    }
    
    protected Object[] getTreePath( ) {
        ArrayList list= new ArrayList();
        list.add(this);
        TreeNode node= this.getParent();
        while ( node!=null ) {
            list.add( 0, node );
            node= node.getParent();
        }        
        return (Object[]) list.toArray( new Object[list.size()] );
    }
    
    void flush() throws InvocationTargetException {
        try {
            if (dirty) {
                DasLogger.getLogger( DasLogger.DASML_LOG).fine("flushing property "+absPropertyName()+"="+value );
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
        } catch (IllegalAccessException iae) {
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
        boolean isWritable= propertyDescriptor.getWriteMethod()!=null;
        return isWritable && column == 1 && !(getAllowsChildren());
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
                return "Property Name";
            case 1:
                return "Value";
            default: throw new IllegalArgumentException("No such column: " + columnIndex);
        }
    }
    
    public String toString() {
        return getDisplayName();
    }
    
    public void refresh( ) {
        if (getAllowsChildren()) {
            if (children != null) {
                for (Iterator i = children.iterator(); i.hasNext();) {
                    PropertyTreeNode child = (PropertyTreeNode)i.next();
                    child.refresh( );
                }
            }
        } else {
            Object newValue = read();
            if (newValue != value && newValue != null && !newValue.equals(value)) {
                value = newValue;
                //TODO: update table somehow.
            }
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
}

