/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.dsutil;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.QDataSet;

/**
 * provides a TreeModel representation of the dataset's properties.
 * @author jbf
 */
public class PropertiesTreeModel extends DefaultTreeModel {

    QDataSet ds;
    String dsLabel;
 
    MutableTreeNode mroot= (MutableTreeNode)root;
    
    public PropertiesTreeModel( QDataSet ds ) {
        this( null, ds );
    }
    
    /**
     * 
     * @param prefix String to prefix the root label.
     * @param ds the dataset source of the metadata.
     */
    public PropertiesTreeModel( String prefix, QDataSet ds ) {
        
        super( new DefaultMutableTreeNode( ( prefix==null ? "" : prefix ) + DataSetUtil.toString(ds) ) );
        this.ds = ds;
        
        Map properties= DataSetUtil.getProperties(ds,new HashMap());
        
        for ( Object key: properties.keySet() ) {
            Object value= properties.get(key);
            MutableTreeNode nextChild;
            if ( value instanceof QDataSet ) {
                PropertiesTreeModel model= new PropertiesTreeModel( key + "=", (QDataSet)value);
                nextChild= (MutableTreeNode) model.getRoot();
            } else if ( value.getClass().isArray() ) {
                value.getClass().getComponentType();
                List list= new ArrayList();
                int nn= Math.min( Array.getLength(value), 5 );
                for ( int i=0; i<nn; i++ ){
                    list.add( Array.get( value, i) );
                }
                if ( Array.getLength(value)>5 ) {
                    list.add("...");
                }
                nextChild= new DefaultMutableTreeNode(""+key+"="+list);
            } else {
                nextChild= new DefaultMutableTreeNode(""+key+"="+value);
            }
            mroot.insert( nextChild, mroot.getChildCount() ); 
        }
    }
    
    
}
