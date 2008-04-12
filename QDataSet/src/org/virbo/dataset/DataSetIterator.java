/*
 * DataSetIterator.java
 *
 * Created on November 11, 2007, 6:29 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.virbo.dataset;

/**
 * Iterator that provides access to each dataset point, hiding rank when
 * when it is not needed.
 *
 * @author jbf
 */
public abstract class DataSetIterator  {
    
    QDataSet ds;
    
    private DataSetIterator() {
    }
    
    /**
     * true if more data is available.
     */
    public abstract boolean hasNext();
    
    /**
     * return the next point.
     */
    public abstract double next();
    
    /**
     * returns the idimth index that the cursor is pointing at, that
     * the next() call will return.
     * @param idim
     * @return
     */
    public abstract int getIndex( int idim );
   
    static class Rank1 extends DataSetIterator {
        int len;
        int index;
        Rank1( QDataSet ds ) {
            this.ds= ds;
            len= ds.length();
            index= 0;
        }
        public boolean hasNext() {
            return index<len;
        }
        public double next() {
            return ds.value(index++);
        }

        @Override
        public int getIndex(int idim) {
            return index;
        }
    }
    
    static class Rank2 extends DataSetIterator {
        int len0,len1;
        int index0,index1;
        int lastIndex0;
        Rank2( QDataSet ds ) {
            this.ds= ds;
            len0= ds.length();
            lastIndex0= len0-1;
            index0= 0;
            len1= ds.length(index0);
            index1= 0;
        }
        
        public boolean hasNext() {
            return index0<=lastIndex0 && ( index1<len1 || index0<lastIndex0 );
        }
        
        private void carry() {
            index1=0;
            index0++;
            len1= ds.length(index0);
        }
        
        public double next() {
            if ( index1==len1 ) carry();
            return ds.value(index0,index1++);
        }

        @Override
        public int getIndex(int idim) {
            switch (idim) {
                case 0: return index0; 
                case 1: return index1; 
                default: throw new IllegalArgumentException("rank limit");
            }
        }
    }

    static class Rank3 extends DataSetIterator {
        int len0,len1,len2;
        int index0,index1,index2;
        int lastIndex0, lastIndex1;
        
        Rank3( QDataSet ds ) {
            this.ds= ds;
            len0= ds.length();
            lastIndex0= len0-1;
            index0= 0;
            len1= ds.length(index0);
            index1= 0;
            len2= ds.length(index0,index1);
            index2= 0;
        }
        
        public boolean hasNext() {
            // TODO: check rank 3 when first dim has no elements
            return index2<len2 || index1<lastIndex1 || index0<lastIndex0;
        }
        
        private void carry() {
            index2=0;
            index1++;
            if ( index1==len1 ) {
                index1=0;
                index0++;
                len1= ds.length(index0);
                lastIndex1= len1-1;
            } 
            len2= ds.length(index0,index1);
        }
        
        public double next() {
            if ( index2==len2 ) carry();
            return ds.value(index0,index1,index2++);
        }
        
        @Override
        public int getIndex(int idim) {
            switch (idim) {
                case 0: return index0;
                case 1: return index1;
                case 2: return index2;
                default: throw new IllegalArgumentException("rank limit");
            }
        }
        
    }

    public static DataSetIterator create( QDataSet ds ) {
        switch (ds.rank()) {
            case 1: return new Rank1(ds); 
            case 2: return new Rank2(ds);
            case 3: return new Rank3(ds);
            default: throw new IllegalArgumentException("rank not supported: "+ds.rank()+", must be 1,2,or 3." );
        }
    }
    
    public static void main(String[] args ) {
        double[] back= new double[20];
        for ( int i=0; i<back.length; i++ ) back[i]=i;
        //QDataSet ds= new IndexGenDataSet(20);
        QDataSet ds= DDataSet.wrapRank2(back,4);
        //QDataSet ds= DDataSet.wrapRank3(back,2,2);
        for ( DataSetIterator it= DataSetIterator.create(ds); it.hasNext(); ) {
            System.err.println(it.next());
        }
        
    }
    
}
