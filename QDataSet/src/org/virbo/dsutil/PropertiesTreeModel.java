/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.dsutil;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
    
    public PropertiesTreeModel( QDataSet ds ,int valuesSizeLimit) {
        this( null, ds ,valuesSizeLimit);
    }
    
    /**
     * 
     * @param prefix String to prefix the root label.
     * @param ds the dataset source of the metadata.
     */
    public PropertiesTreeModel( String prefix, QDataSet ds ,int valuesSizeLimit) {
        
        super( new DefaultMutableTreeNode( ( prefix==null ? "" : prefix ) + DataSetUtil.toString(ds) ) );
        this.ds = ds;
        
        Map properties= DataSetUtil.getProperties(ds);
        
        for ( Object key: properties.keySet() ) {
            Object value= properties.get(key);
            MutableTreeNode nextChild;
            if ( value instanceof QDataSet ) {
                PropertiesTreeModel model= new PropertiesTreeModel( key + "=", (QDataSet)value,valuesSizeLimit);
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
            } else if ( Map.class.isAssignableFrom( value.getClass() ) ) {
                nextChild= (MutableTreeNode) new MapTreeModel( key + " (map)", (Map)value ).getRoot();
            } else {
                nextChild= new DefaultMutableTreeNode(""+key+"="+value);
            }
            mroot.insert( nextChild, mroot.getChildCount() ); 
        }

        if ( ds.rank()>0 ) {
            MutableTreeNode values= new DefaultMutableTreeNode("values");
            ValuesTreeModel.valuesTreeNode( "value(", values, ds ,valuesSizeLimit);
            mroot.insert( values, mroot.getChildCount() );
        }
    }

    class MapTreeModel extends DefaultTreeModel {
        MapTreeModel( Object root, Map values ) {
            super( new DefaultMutableTreeNode(root) );
            MutableTreeNode mrt= ((MutableTreeNode)getRoot());
            for ( Object o : values.entrySet() ) {
                Entry val= (Entry)o;
                mrt.insert( new DefaultMutableTreeNode(""+val.getKey()+"="+val.getValue() ),mrt.getChildCount() );
            }
        }

    }
    
}
