/*
 * PropertyTreeNodeInterface.java
 *
 * Created on December 20, 2005, 12:35 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.das2.components.propertyeditor;

import org.das2.components.treetable.TreeTableNode;
import java.beans.PropertyDescriptor;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

/**
 *
 * @author Jeremy
 */
public interface PropertyTreeNodeInterface extends TreeNode, TreeTableNode {
    
    boolean isDirty();

    void refresh();

    void setTreeModel(DefaultTreeModel treeModel);
    
    Object getValue();
    
    void setValue( Object value ); 
    
    Object getDisplayValue();
    
    void flush();
    
    PropertyDescriptor getPropertyDescriptor();
}
