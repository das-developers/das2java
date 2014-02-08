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
import org.das2.datum.Units;
import org.das2.datum.UnitsUtil;
import org.das2.util.DasMath;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SemanticOps;

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
     * This is really for human consumption and may change.
     * @param prefix String to prefix the root label.
     * @param ds the dataset source of the metadata.
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
                if ( bdsd.length()>0 ) svalue.append( bdsd.property( QDataSet.NAME, 0 ) );
                for ( int i=1; i<bdsd.length(); i++ ) {
                    svalue.append( "," ).append( bdsd.property( QDataSet.NAME, i ) );
                }
                nextChild= new DefaultMutableTreeNode(""+key+"="+svalue);
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
                        MutableTreeNode values= new DefaultMutableTreeNode( String.format( "slice(%d): %s",i, ds1 ) );
                        ValuesTreeModel.valuesTreeNode( "value(", values, ds1, valuesSizeLimit );
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

    private static class MapTreeModel extends DefaultTreeModel {
        MapTreeModel( Object root, Map values ) {
            super( new DefaultMutableTreeNode(root) );
            MutableTreeNode mrt= ((MutableTreeNode)getRoot());
            for ( Object o : values.entrySet() ) {
                Entry val= (Entry)o;
                Object value= val.getValue();
                if ( value.getClass().isArray() ) {
                    value.getClass().getComponentType();
                    List list= new ArrayList();
                    int nn= Math.min( Array.getLength(value), 5 );
                    for ( int i=0; i<nn; i++ ){
                        list.add( Array.get( value, i) );
                    }
                    if ( Array.getLength(value)>5 ) {
                        list.add("...");
                    }
                    DefaultMutableTreeNode nextChild= new DefaultMutableTreeNode(""+val.getKey()+"="+list);
                    mrt.insert( nextChild, mrt.getChildCount() );
                } else {
                    mrt.insert( new DefaultMutableTreeNode(""+val.getKey()+"="+val.getValue() ),mrt.getChildCount() );
                }
            }
        }

    }
    
}
