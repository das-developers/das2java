/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.dataset;

/**
 * Iterator that uses a rank 2 list of indeces.  For example,
 * to iterate over 10 points of a rank 2 dataset, this would be constructed
 * with a dataset[10,2].
 * @author jbf
 */
public class IndexListDataSetIterator implements DataSetIterator {

    /* rank 2 dataset of indeces */
    QDataSet indeces;
    int dsrank; // source dataset rank
    int index;
    
    public IndexListDataSetIterator( QDataSet indeces ) {
        if ( indeces.rank()!=2 ) throw new IllegalArgumentException("indeces must be rank 2.");
        this.indeces= indeces;
        index= -1;
        if ( indeces.length()>0 ) {
            this.dsrank= indeces.length(0);
        }
    }

    @Override
    public boolean hasNext() {
        return (index+1)< indeces.length();
    }

    @Override
    public int index(int dim) {
        return (int)indeces.value(index, dim);
    }

    @Override
    public int length(int dim) {
        return indeces.length();
    }

    @Override
    public void next() {
        index++;
    }

    @Override
    public int rank() {
        return 1;
    }

    /**
     * get the value from ds at the current iterator position.
     * @param ds a dataset with compatible geometry as the iterator's geometry.
     * @return the value of ds at the current iterator position.
     */
    @Override
    public final double getValue( QDataSet ds ) {
        switch ( dsrank ) {
            case 1:
                return ds.value( index(0) );
            case 2:
                return ds.value( index(0), index(1) );
            case 3:
                return ds.value( index(0), index(1), index(2) );
            default:
                throw new IllegalArgumentException("rank limit");
        }        
    }
    
    /**
     * replace the value in ds at the current iterator position.
     * @param ds a writable dataset with compatible geometry as the iterator's geometry.
     * @param v the value to insert.
     */
    @Override
    public final void putValue( WritableDataSet ds, double v ) {
        switch ( dsrank ) {
            case 1:
                ds.putValue( index(0), v );
                return;
            case 2:
                ds.putValue( index(0), index(1), v );
                return;
            case 3:
                ds.putValue( index(0), index(1), index(2), v );
                return;
            default:
                throw new IllegalArgumentException("rank limit");
        }        
    }
    
    @Override
    public String toString() {
        String its=""+index+" of "+indeces.length();
        StringBuilder ats;
        
        if ( index==-1 ) {
            ats= new StringBuilder("-1");
            for ( int i=1; i<dsrank; i++ ) {
                ats.append(",").append(index(i));
            }
        } else {
            ats= new StringBuilder( String.valueOf(index(0)) );
        }
        
        return "ListIter [" + its + "] @ ["+  ats + "] ";
    }

    @Override
    public DDataSet createEmptyDs() {
        return DDataSet.createRank1(indeces.length());
    }
}
