/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.qds.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import org.das2.datum.Units;
import org.das2.datum.UnitsUtil;
import org.das2.util.DasMath;
import org.das2.qds.DataSetUtil;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;

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
     * TreeModel representing the dataset.  This is really for human consumption and may change.
     * @param prefix String to prefix the root label.
     * @param ds the dataset source of the metadata.
     * @param valuesSizeLimit the number of values in a dataset to represent, e.g. 20, and ellipses (...) will represent the values not shown.
     */
    public PropertiesTreeModel( String prefix, QDataSet ds ,int valuesSizeLimit) {
        
        super( new DefaultMutableTreeNode( ( prefix==null ? "" : prefix ) + DataSetUtil.toString(ds) ) );
        this.ds = ds;
        
        Map<String,Object> properties= DataSetUtil.getProperties(ds);
        
        for ( Entry<String,Object> e: properties.entrySet() ) {
            String key= e.getKey();
            Object value= e.getValue();
            if ( key.equals(QDataSet.QUBE) && ( ds.rank()<2 || ds.property(QDataSet.BUNDLE_1)!=null ) ) {
                continue;
            }
            MutableTreeNode nextChild;
            if ( key.startsWith("BUNDLE_") && ( value instanceof QDataSet ) ) {
                QDataSet bdsd= (QDataSet)value;
                StringBuilder svalue= new StringBuilder();
                svalue.append(key).append("=");
                if ( bdsd.length()>0 ) svalue.append( bdsd.property( QDataSet.NAME, 0 ) );
                for ( int i=1; i<bdsd.length(); i++ ) {
                    svalue.append( "," ).append( bdsd.property( QDataSet.NAME, i ) );
                }
                BundleDescriptorTreeModel rm= new BundleDescriptorTreeModel(svalue.toString(),(QDataSet)value);
                nextChild= (MutableTreeNode) rm.getRoot();
            } else if ( value instanceof QDataSet ) {
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
                String svalue= String.valueOf(value);
                if ( value instanceof Number
                        && ( key.equals(QDataSet.VALID_MIN) || key.equals(QDataSet.VALID_MAX)
                        || key.equals(QDataSet.TYPICAL_MIN ) || key.equals(QDataSet.TYPICAL_MAX ) ) ) {
                    Units u= (Units) properties.get(QDataSet.UNITS);
                    if ( u!=null && UnitsUtil.isTimeLocation(u) ) {
                        if ( u.isValid( ((Number)value).doubleValue() ) ) {
                            svalue= u.createDatum((Number)value).toString() + " (" + svalue + ")";
                        } else {
                            svalue= "fill" + " (" + svalue + ")";
                        }
                    }
                } else if ( key.equals(QDataSet.FILL_VALUE ) && value instanceof Number ) { // round N digits.
                    if ( value instanceof Long ||value instanceof Integer || value instanceof Short ) {
                        svalue= String.valueOf( value ); 
                    } else {
                        svalue= String.valueOf( DasMath.roundNSignificantDigits( ((Number)value).doubleValue(), 6 ) ); 
                    }
                }
                nextChild= new DefaultMutableTreeNode(""+key+"="+svalue);
            }
            mroot.insert( nextChild, mroot.getChildCount() );
        }

        if ( ds.rank()>0 ) {
            if ( SemanticOps.isJoin(ds) ) {
                int lin=19;
                for ( int i=0; i<ds.length(); i++ ) {
                    if ( i<lin || i>=ds.length()-3 ) { 
                        QDataSet ds1= ds.slice(i);
                        MutableTreeNode values= (MutableTreeNode) new PropertiesTreeModel(String.format("slice(%d)= ",i), ds1,valuesSizeLimit).getRoot();
                        mroot.insert( values, mroot.getChildCount() );        
                    } else if ( i==lin ) {
                        mroot.insert( new DefaultMutableTreeNode("..."), mroot.getChildCount() );  
                    }
                }
            } else {
                MutableTreeNode values= new DefaultMutableTreeNode("values");
                ValuesTreeModel.valuesTreeNode( "value(", values, ds ,valuesSizeLimit);
                mroot.insert( values, mroot.getChildCount() );
            }
        }
    }

    private static class BundleDescriptorTreeModel extends DefaultTreeModel {
        public BundleDescriptorTreeModel( String label, QDataSet root ) {
            super(new DefaultMutableTreeNode(root));
            MutableTreeNode mrt= ((MutableTreeNode)getRoot());
            ((DefaultMutableTreeNode)mrt).setUserObject(label);
            for ( int i=0; i<root.length(); i++ ) {
                PropertiesTreeModel m= new PropertiesTreeModel(root.slice(i),20);
                mrt.insert( (MutableTreeNode)m.getRoot(), mrt.getChildCount() );
            }
        }
        
    }
    
    private static class ArrayTreeModel extends DefaultTreeModel {
        ArrayTreeModel( Object root, Object values ) {
            super( new DefaultMutableTreeNode(root) );
            int i= root.toString().indexOf("[");
            String name="";
            if ( i>-1 ) {
                name= root.toString().substring(0,i);
            }
            MutableTreeNode mrt= ((MutableTreeNode)getRoot());
            for ( i=0; i<Array.getLength(values); i++ ){
                MutableTreeNode nextChild= (MutableTreeNode) new DefaultMutableTreeNode( "["+i + "]=" + Array.get( values, i ) ).getRoot();
                mrt.insert( nextChild, mrt.getChildCount() );
            }
        }
    }
    
    private static class MapTreeModel extends DefaultTreeModel {
        MapTreeModel( Object root, Map values ) {
            super( new DefaultMutableTreeNode(root) );
            MutableTreeNode mrt= ((MutableTreeNode)getRoot());
            for ( Object o : values.entrySet() ) {
                Entry val= (Entry)o;
                Object value= val.getValue();
                if ( value!=null && value.getClass().isArray() ) {
                    DefaultTreeModel nextChild= new ArrayTreeModel( val.getKey() + "["+ Array.getLength(value)+"]", value );
                    mrt.insert(  (MutableTreeNode) nextChild.getRoot(), mrt.getChildCount() );
                } else if ( value!=null && Map.class.isAssignableFrom( value.getClass() ) ) {
                    MutableTreeNode nextChild= (MutableTreeNode) new MapTreeModel( val.getKey(), (Map)value ).getRoot();
                    mrt.insert( nextChild, mrt.getChildCount() );
                } else {
                    mrt.insert( new DefaultMutableTreeNode(""+val.getKey()+"="+val.getValue() ),mrt.getChildCount() );
                }
            }
        }

    }
    
}
