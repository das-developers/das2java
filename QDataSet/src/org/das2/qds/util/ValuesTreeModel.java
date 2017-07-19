/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.qds.util;

import java.util.Enumeration;
import org.das2.datum.Units;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import org.das2.datum.EnumerationUnits;
import org.das2.qds.DataSetOps;
import org.das2.qds.DataSetUtil;
import org.das2.qds.QDataSet;
import org.das2.qds.RankZeroDataSet;
import org.das2.qds.ops.Ops;

/**
 * provides a TreeModel representation of the dataset's properties.
 * @author jbf
 */
public class ValuesTreeModel extends DefaultTreeModel {

    // number of trailing elements to show
    private static final int TAIL_COUNT = 3;

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
        valuesTreeNode( "HELP", mroot, ds ,10);
    }

    /**
     * The valuesTreeNode implementation that uses MutableTreeNodes cannot scale to very large datasets, since all
     * nodes are created immediately.  This implementation only queries the dataset as children are opened, and
     * should eventually allow for exploration of any dataset.  This is not used right now in the metadata
     * tab of Autoplot because the root node of the tree is a MutableTreeNode and all children must therefore
     * be MutableTreeNodes.
     */
    public static TreeNode valuesTreeNode2( final String prefix, final TreeNode parent, final QDataSet ds ) {
        final Units units= (Units) ds.property( QDataSet.UNITS );
        final QDataSet wds= DataSetUtil.weightsDataSet(ds);
        final QDataSet dep0= (QDataSet) ds.property(QDataSet.DEPEND_0);
        final Units depu= dep0==null ? null : (Units) dep0.property(QDataSet.UNITS);

        return new TreeNode() {

            public TreeNode getChildAt(int i) {
                if ( ds.rank()==1 ) {
                    String sval= units==null ? String.valueOf(ds.value(i)) : String.valueOf(units.createDatum(ds.value(i)));
                    if ( wds.value(i) > 0. ) sval= "fill ("+ds.value(i)+")"; //TODO: future datum class may allow for toString to return nominal data for invalid data.
                    return new DefaultMutableTreeNode( prefix+""+i+")="+sval);
                } else {
                    String sdepu= dep0==null ? String.valueOf(i) : String.valueOf(depu.createDatum(dep0.value(i)));
                    MutableTreeNode sliceNode= new DefaultMutableTreeNode( "values @ "+ sdepu );
                    return sliceNode;
                }
            }

            public int getChildCount() {
                return Math.min( ds.length(), 10 ); // TODO: "..."
            }

            public TreeNode getParent() {
                return parent;
            }

            public int getIndex(TreeNode node) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public boolean getAllowsChildren() {
                return true;
            }

            public boolean isLeaf() {
                return false;
            }

            public Enumeration children() {
                return new Enumeration() {
                    int i=0;
                    public boolean hasMoreElements() {
                        return i<getChildCount();
                    }

                    public Object nextElement() {
                        return getChildAt(i++);
                    }
                };
            }
        };
    }

    private static String svalRank1( QDataSet wds, QDataSet ds, int i ) {
        if ( ds.rank()==2 && ds.property(QDataSet.BINS_1).equals(QDataSet.VALUE_BINS_MIN_MAX) ) {
            if ( wds.value(i,0)==0 || wds.value(i,1)==0 ) {
               return "fill";
            } else {
                return DataSetUtil.asDatumRange( ds.slice(i), true ).toString();
            }
        }
        try {
            if ( wds.value(i) > 0. ) {
                QDataSet bds= (QDataSet)ds.property(QDataSet.BUNDLE_0);
                if ( bds!=null ) {
                    String alt= ds.slice(i).toString();
                    String result= DataSetUtil.getStringValue( ds, ds.value(i), i );
                    if ( alt.endsWith("=fill") ) {
                        result= result + "(fill)";
                    }
                    return result;
                } else {
                    return DataSetUtil.getStringValue( ds, ds.value(i) );
                }
            } else {
                return "fill ("+ds.value(i)+")";
            }
        } catch ( IllegalArgumentException ex ) {
            return "Error: "+ex;
        }
    }

    /**
     * return a tree node for the values of a dataset.
     * @param prefix prefix added to the each node, e.g. "value("
     * @param aroot the parent to which the nodes are added.
     * @param ds the dataset to represent.
     * @param sizeLimit the number of nodes to represent, e.g. 20, and ellipses (...) will represent the values not shown.
     * @return the node (aroot) is returned.
     */
    public static MutableTreeNode valuesTreeNode( String prefix, MutableTreeNode aroot, QDataSet ds, int sizeLimit ) {
        QDataSet wds= DataSetUtil.weightsDataSet(ds);

        QDataSet bundle= (QDataSet)ds.property( QDataSet.BUNDLE_0 );

        QDataSet dep0= (QDataSet) ds.property(QDataSet.DEPEND_0);
        QDataSet wdsDep0= null;
        if ( dep0!=null ) wdsDep0= DataSetUtil.weightsDataSet(dep0);

        Units depu= dep0==null ? Units.dimensionless : (Units) dep0.property(QDataSet.UNITS);
        if ( depu==null ) depu= Units.dimensionless;

        if ( ds.rank()==0 ) {
            String sval= DataSetUtil.asDatum(ds).toString();
            aroot.insert(  new DefaultMutableTreeNode( prefix+")="+sval), aroot.getChildCount() );
        } else if ( ds.rank()==1 ) {
            Units units= (Units) ds.property(QDataSet.UNITS);
            if ( units==null ) units= Units.dimensionless;
            for ( int i=0; i<Math.min( ds.length(), sizeLimit ); i++ ) {
                Units u= units;
                if ( bundle!=null ) {
                    u= (Units)bundle.property( QDataSet.UNITS, i );
                    if ( u==null ) u= Units.dimensionless;
                }
                String sval;
                if ( u instanceof EnumerationUnits ) {
                    try {
                        sval= svalRank1( wds, ds, i );
                    } catch ( IllegalArgumentException ex ) {
                        sval= "" + ds.value(i) + " (error)";
                    }
                } else {
                    sval= svalRank1( wds, ds, i );
                }
                //TODO: future datum class may allow for toString to return nominal data for invalid data.
                if ( dep0!=null ) {
                    sval += " @ " +( svalRank1( wdsDep0, dep0, i ) );
                }
                if ( bundle!=null ) {
                    sval = bundle.property(QDataSet.NAME,i)+" = " + ( svalRank1( wds, ds, i ) );
                    //sval = bundle.property(QDataSet.NAME,i)+" = " + ( wds.value(i) > 0. ? DataSetUtil.getStringValue( bundle.slice(i), ds.value(i) ) : "fill" ); //TODO: check this
                    aroot.insert(  new DefaultMutableTreeNode( sval), aroot.getChildCount() );
                } else {
                    aroot.insert(  new DefaultMutableTreeNode( prefix+""+i+")="+sval), aroot.getChildCount() );
                }
            }
            if ( ds.length()>=sizeLimit ) {
                aroot.insert( new DefaultMutableTreeNode( "..." ), aroot.getChildCount() );
            }
            // insert last N values
            for ( int i=Math.max( ds.length()-TAIL_COUNT,sizeLimit ); i<ds.length(); i++ ) {
                Units u= units;
                if ( bundle!=null ) {
                    u= (Units)bundle.property( QDataSet.UNITS, i );
                    if ( u==null ) u= Units.dimensionless;
                }
                String sval;
                if ( u instanceof EnumerationUnits ) {
                    try {
                        sval= svalRank1( wds, ds, i );
                    } catch ( IllegalArgumentException ex ) {
                        sval= "" + ds.value(i) + " (error)";
                    }
                } else {
                    sval= svalRank1( wds, ds, i );
                }
                //TODO: future datum class may allow for toString to return nominal data for invalid data.
                if ( dep0!=null ) {
                    sval += " @ " +( svalRank1( wdsDep0, dep0, i ) );
                }
                if ( bundle!=null ) {
                    sval = bundle.property(QDataSet.NAME,i)+" = " + ( svalRank1( wds, ds, i ) );
                    //sval = bundle.property(QDataSet.NAME,i)+" = " + ( wds.value(i) > 0. ? DataSetUtil.getStringValue( bundle.slice(i), ds.value(i) ) : "fill" ); //TODO: check this
                    aroot.insert(  new DefaultMutableTreeNode( sval), aroot.getChildCount() );
                } else {
                    aroot.insert(  new DefaultMutableTreeNode( prefix+""+i+")="+sval), aroot.getChildCount() );
                }
            }
        } else {
            if ( dep0==null ) dep0= Ops.dindgen(ds.length());
            if ( depu==null ) depu= Units.dimensionless;
        
            for ( int i=0; i<Math.min( ds.length(), sizeLimit ); i++ ) {
                if ( dep0.rank()==1 ) { //TODO: what should this do for rank>1?
                    MutableTreeNode sliceNode= new DefaultMutableTreeNode( "values @ "+depu.createDatum(dep0.value(i)) );
                    aroot.insert( sliceNode, aroot.getChildCount() );
                    valuesTreeNode( prefix + i+",", sliceNode, ds.slice(i), 20 );
                }
            }
            if ( ds.length()>=sizeLimit ) {
                aroot.insert( new DefaultMutableTreeNode( "..." ), aroot.getChildCount() );
            }
            for ( int i=Math.max( ds.length()-TAIL_COUNT,sizeLimit ); i<ds.length(); i++  ) {
                if ( dep0.rank()==1 ) { //TODO: what should this do for rank>1?
                    MutableTreeNode sliceNode= new DefaultMutableTreeNode( "values @ "+depu.createDatum(dep0.value(i)) );
                    aroot.insert( sliceNode, aroot.getChildCount() );
                    valuesTreeNode( prefix + i+",", sliceNode, ds.slice(i), 20 );
                }
            }
        }
        return aroot;
    }
    
    
}
