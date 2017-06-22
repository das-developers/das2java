package org.das2.qds;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.util.LoggerManager;
import org.das2.qds.ops.Ops;

/**
 * Extracts a subset of the source dataset by using a rank 1 subset of indeces on each index.
 * @author jbf
 */
public class SubsetDataSet extends AbstractDataSet {

    private static final Logger logger= LoggerManager.getLogger("qdataset");

    QDataSet source;

    QDataSet[] sorts;
    int[] lens;

    boolean nonQube=false;

    public SubsetDataSet( QDataSet source ) {
        this.source= source;
        sorts= new QDataSet[ QDataSet.MAX_RANK ];
        lens= new int[ QDataSet.MAX_RANK ];
        if ( !DataSetUtil.isQube(source) ) {
            nonQube= true;
        } else { // flatten the qube immediately, because we are seeing this with FFTPower output.
            QDataSet dep1= (QDataSet) source.slice(0).property(QDataSet.DEPEND_0);
            putProperty( QDataSet.DEPEND_1, dep1 );
        }
        int[] lenss= DataSetUtil.qubeDims(source);
        if ( nonQube ) {
            lens[0]= source.length();
            sorts[0]= new IndexGenDataSet(lens[0]);
            for ( int i=1; i<source.rank(); i++ ) {
                lens[i]= Integer.MAX_VALUE;
                sorts[i]= new IndexGenDataSet(lens[i]);
            }
        } else {
            for ( int i=0; i<lenss.length; i++ ) {
                lens[i]= lenss[i];
                sorts[i]= new IndexGenDataSet(lenss[i]);
            }
        }
    }

    /**
     * apply the subset indexes to a given dimension.  For example,
     * if a=[10,20,30,40] then applyIndex( 0, [1,2] ) would result in [20,30].
     * @param idim
     * @param idx the rank 1 index list, for example from where on a rank 1 dataset.
     */
    public void applyIndex( int idim, QDataSet idx ) {
        if ( nonQube && idim>0 ) throw new IllegalArgumentException("unable to applyIndex on non-qube source dataset");
        if ( idx.rank()==1 ) {
            QDataSet max= Ops.reduceMax( idx, 0 );
            if ( max.value()>=lens[idim] ) {
                logger.log(Level.WARNING, "idx dataset contains maximum that is out-of-bounds: {0}", max);
            }
        }
        sorts[idim]= idx;
        lens[idim]= idx.length();
        if ( idx.rank()>1 ) {
            throw new IllegalArgumentException("indexes must be rank 1");
        }
        QDataSet dep= (QDataSet)property( "DEPEND_"+idim );
        if ( dep==null ) {
            dep= (QDataSet) source.property( "DEPEND_"+idim );
        }
        if ( dep!=null ) {
            SubsetDataSet dim= new SubsetDataSet( dep );
            dim.applyIndex(0,idx);
            putProperty("DEPEND_"+idim,dim);
        }
        if ( idim==0 ) { // DEPEND_1-4 can be rank 2, where the 0th dimension corresponds to DEPEND_0.
            for ( int i=1; i<QDataSet.MAX_RANK; i++ ) {
                QDataSet depi= (QDataSet)source.property("DEPEND_"+i); // note this is not a qube...
                if ( depi!=null && depi.rank()>1 ) {
                    SubsetDataSet dim= new SubsetDataSet( depi );
                    dim.applyIndex( 0, idx );
                    putProperty("DEPEND_"+i, dim );
                }
            }
        }
    }

    @Override
    public int rank() {
        return source.rank();
    }

    @Override
    public int length() {
        return lens[0];
    }

    @Override
    public int length(int i) {
        return nonQube ? source.length(i) : lens[1];
    }

    @Override
    public int length(int i, int j) {
        return nonQube ? source.length(i,j) : lens[2];
    }

    @Override
    public int length(int i, int j, int k) {
        return nonQube ? source.length(i,j,k) : lens[3];
    }

    @Override
    public double value() {
        return source.value();
    }

    @Override
    public double value(int i) {
        return source.value((int)sorts[0].value(i));
    }

    @Override
    public double value(int i0, int i1) {
        return source.value((int)sorts[0].value(i0),(int)sorts[1].value(i1));
    }

    @Override
    public double value(int i0, int i1, int i2) {
        return source.value((int)sorts[0].value(i0),(int)sorts[1].value(i1),(int)sorts[2].value(i2));
    }

    @Override
    public double value(int i0, int i1, int i2, int i3) {
        return source.value((int)sorts[0].value(i0),(int)sorts[1].value(i1),(int)sorts[2].value(i2),(int)sorts[3].value(i3));
    }

    @Override
    public Object property(String name, int i) {
        Object v= properties.get(name);
        return v!=null ? v : source.property(name, i);
    }

    @Override
    public Object property(String name) {
        Object v= properties.get(name);
        return v!=null ? v : source.property(name);
    }

}
