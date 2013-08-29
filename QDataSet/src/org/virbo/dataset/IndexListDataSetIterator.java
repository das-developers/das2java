/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.dataset;

/**
 *
 * @author jbf
 */
public class IndexListDataSetIterator implements DataSetIterator {

    /* rank 2 dataset of indeces */
    QDataSet indeces;
    int rank;
    int dsrank;
    int index;
    
    public IndexListDataSetIterator( QDataSet indeces ) {
        this.indeces= indeces;
        index= -1;
        if ( indeces.length()>0 ) {
            this.dsrank= indeces.length(0);
        }
        this.rank= 1;
    }

    public boolean hasNext() {
        return (index+1)< indeces.length();
    }

    public int index(int dim) {
        return (int)indeces.value(index, dim);
    }

    public int length(int dim) {
        return indeces.length();
    }

    public void next() {
        index++;
    }

    public int rank() {
        return 1;
    }

    /**
     * get the value from ds at the current iterator position.
     * @param ds a dataset with capatible geometry as the iterator's geometry.
     * @return the value of ds at the current iterator position.
     */
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
     * @param ds a writable dataset with capatible geometry as the iterator's geometry.
     * @param v the value to insert.
     */
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

    public DDataSet createEmptyDs() {
        return DDataSet.createRank1(indeces.length());
    }
}
