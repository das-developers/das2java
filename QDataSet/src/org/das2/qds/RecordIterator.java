
package org.das2.qds;

import java.util.Iterator;
import org.das2.datum.DatumRange;
import org.das2.qds.ops.Ops;

/**
 * Make any QDataSet into a table, then iterate over the records.  This
 * was introduced to support the first Autoplot HAPI server, but will be
 * useful elsewhere.
 * @author jbf
 */
public class RecordIterator implements Iterator<QDataSet> {

    QDataSet src;
    int index;
    int lastIndex;
    
    public static long TIME_STAMP= System.currentTimeMillis();
        
    /**
     * set up the iterator.  If the src is rank 1 with a DEPEND_0, then
     * the two are bundled together.
     * @param src 
     */
    public RecordIterator( QDataSet src ) {
        if ( src.rank()==0 ) {
            throw new IllegalArgumentException("rank 0 dataset");
        } else if ( src.rank()>2 ) {
            throw new IllegalArgumentException("rank is greater than 2");
        }
        QDataSet dep0= (QDataSet) src.property(QDataSet.DEPEND_0);
        if ( dep0!=null ) {
            this.src= Ops.bundle( dep0, src );
        } else {
            this.src= src;
        }
        this.index= 0;
        this.lastIndex= src.length();
    }
    
    /**
     * limit the data returned such that only data within the datum range
     * provided are returned.
     * @param dr 
     */
    public void constrainDepend0( DatumRange dr ) {
        QDataSet dep0= Ops.slice1( this.src, 0 );
        QDataSet findeces= Ops.findex( dep0, dr );
        this.index= (int)Math.ceil( findeces.value(0) );
        this.lastIndex= (int)Math.ceil( findeces.value(1) );
    }
    
    @Override
    public boolean hasNext() {
        return index<lastIndex;
    }

    @Override
    public QDataSet next() {
        return src.slice(index++);
    }
    
    @Override
    public void remove() {
        // do nothing
    }
    
}
