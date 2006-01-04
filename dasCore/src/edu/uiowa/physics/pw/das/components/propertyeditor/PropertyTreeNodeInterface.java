/*
 * PropertyTreeNodeInterface.java
 *
 * Created on December 20, 2005, 12:35 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.uiowa.physics.pw.das.components.propertyeditor;

import edu.uiowa.physics.pw.das.components.treetable.TreeTableNode;
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
