/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.dsutil;

import edu.uiowa.physics.pw.das.datum.Units;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import org.virbo.dataset.DataSetOps;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.QDataSet;
import org.virbo.dsops.Ops;

/**
 * provides a TreeModel representation of the dataset's properties.
 * @author jbf
 */
public class ValuesTreeModel extends DefaultTreeModel {

    QDataSet ds;
    String dsLabel;
 
    MutableTreeNode mroot= (MutableTreeNode)root;
    
    public ValuesTreeModel( QDataSet ds ) {
        this( null, ds );
    }
    
    /**
     * 
     * @param prefix String to prefix the root label.
     * @param ds the dataset source of the metadata.
     */
    public ValuesTreeModel( String prefix, QDataSet ds ) {
        super( new DefaultMutableTreeNode((  prefix==null ? "" : prefix ) + DataSetUtil.toString(ds) ) );
        valuesTreeNode( "HELP", mroot, ds );
    }
    
    public static MutableTreeNode valuesTreeNode( String prefix, MutableTreeNode aroot, QDataSet ds ) {
        
        if ( ds.rank()==1 ) {
            Units units= (Units) ds.property(QDataSet.UNITS);
            if ( units==null ) units= Units.dimensionless;
            for ( int i=0; i<Math.min( ds.length(), 10 ); i++ ) {
                aroot.insert(  new DefaultMutableTreeNode( prefix+""+i+")="+units.createDatum(ds.value(i))), aroot.getChildCount() ); 
            }
            if ( ds.length()>=10 ) {
                aroot.insert( new DefaultMutableTreeNode( "..." ), aroot.getChildCount() );
            }
        } else {
            QDataSet dep0= (QDataSet) ds.property(QDataSet.DEPEND_0);
            if ( dep0==null ) dep0= Ops.dindgen(10);
            Units depu= (Units) dep0.property(QDataSet.UNITS);
            if ( depu==null ) depu= Units.dimensionless;
            for ( int i=0; i<Math.min( ds.length(), 10 ); i++ ) {
                MutableTreeNode sliceNode= new DefaultMutableTreeNode( "values @ "+depu.createDatum(dep0.value(i)) ); 
                MutableTreeNode atree= valuesTreeNode( prefix + i+",", sliceNode, DataSetOps.slice0(ds, i) );
                aroot.insert( sliceNode, aroot.getChildCount() );
            }
            if ( ds.length()>=10 ) {
                aroot.insert( new DefaultMutableTreeNode( "..." ), aroot.getChildCount() );
            }
        }
        return aroot;
    }
    
    
}
