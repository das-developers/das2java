/*
 * DataSetIterator.java
 *
 * Created on November 11, 2007, 6:29 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.das2.qds;

/**
 * Iterator that provides access to each dataset point, hiding rank when
 * when it is not needed.
 * 
 * TODO: Rank2 and Rank3 have problems with zero length indeces.
 *
 * @author jbf
 */
public abstract class OldDataSetIterator  {
    
    QDataSet ds;
    
    private OldDataSetIterator() {
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
     * returns the idimth index0 that the cursor is pointing at, after 
     * the next() was called.
     * @param idim
     * @return
     */
    public abstract int getIndex( int idim );
   
    static class Rank1 extends OldDataSetIterator {
        int len0;
        int index0;
        Rank1( QDataSet ds ) {
            this.ds= ds;
            len0= ds.length();
            index0= -1;
        }
        public boolean hasNext() {
            return ! ( index0==len0-1 );
        }
        public double next() {
            return ds.value(++index0);
        }

        @Override
        public int getIndex(int idim) {
            return idim==0 ? index0 : 0;
        }
    }
    
    static class Rank2 extends OldDataSetIterator {
        int len0,len1;
        int index0,index1;
        Rank2( QDataSet ds ) {
            this.ds= ds;
            len0= ds.length();
            index0= 0;
            len1= ds.length(index0);
            index1= -1;
        }
        
        public boolean hasNext() {
            return index0<len0 && ! ( index0==len0-1 && index1==len1-1 );
        }
        
        private void carry() {
            index1=0;
            len1= ds.length(++index0);
        }
        
        public double next() {
            if ( ++index1==len1 ) carry();
            return ds.value(index0,index1);
        }

        @Override
        public int getIndex(int idim) {
            switch (idim) {
                case 0: return index0;
                case 1: return index1; 
                default: return 0;
            }
        }
    }

    static class Rank3 extends OldDataSetIterator {
        int len0,len1,len2;
        int index0,index1,index2;
        
        Rank3( QDataSet ds ) {
            this.ds= ds;
            len0= ds.length();
            len1= ds.length(0);
            len2= ds.length(0,0);
            index0= 0;
            index1= 0;
            index2= -1;
        }
        
        public boolean hasNext() {
            boolean lastIndex= index0==len0-1 && index1==len1-1 && index2==len2-1;
            return index0<len0 && !lastIndex;
        }
        

        private void carry() {
            index2=0;
            if ( ++index1==len1 ) {
                index1=0;
                len1= ds.length(++index0);
            } 
            len2= ds.length(index0,index1);
        }
        
        public double next() {
            if ( ++index2==len2 ) carry();
            return ds.value(index0,index1,index2);
        }
        
        @Override
        public int getIndex(int idim) {
            switch (idim) {
                case 0: return index0;
                case 1: return index1;
                case 2: return index2;
                default: return 0;
            }
        }
        
    }

    /**
     * untested, unused code with optimizations for Qubes.
     * TODO: break into separate classes for each rank.
     * TODO: DDataSet should have a protected accessor.
     */
    static class Qube extends OldDataSetIterator {
        int len0,len1,len2;
        int n;
        int i;
        
        Qube( QDataSet ds ) {
            int[] dims= DataSetUtil.qubeDims(ds);
            n= ds.length();
            for ( int idim=1; idim<dims.length; idim++ ) {
                n*= dims[idim];
            }
            i=0;
        }
        
        @Override
        public boolean hasNext() {
            return i<n;
        }

        @Override
        public double next() {
            i++;
            switch ( ds.rank() ) {
                case 1: return ds.value(i);
                case 2: return ds.value(i/len0,i%len0);
                case 3: return ds.value(i/(len0*len1),i%(len0*len1)/len1,i%len2);
                default: throw new IllegalArgumentException("rank limit");
            }
        }

        @Override
        public int getIndex(int idim) {
            switch ( ds.rank() ) {
                case 1: return i;
                case 2: return idim==0 ? i/len0 : i%len0;
                case 3: switch ( idim ) {
                    case 0: return i/(len0*len1);
                    case 1: return i%(len0*len1)/len1;
                    case 2: return i%len2;
                }
                default: throw new IllegalArgumentException("rank limit");
            }            
        }
        
    }
    
    public static OldDataSetIterator create( QDataSet ds ) {
        switch (ds.rank()) {
            case 1: return new Rank1(ds); 
            case 2: return new Rank2(ds);
            case 3: return new Rank3(ds);
            default: throw new IllegalArgumentException("rank not supported: "+ds.rank()+", must be 1,2,or 3." );
        }
    }
    
    public static final void putValue( WritableDataSet ds, OldDataSetIterator it, double v  ) {
        switch ( ds.rank() ) {
            case 1: ds.putValue( it.getIndex(0), v ); return;
            case 2: ds.putValue( it.getIndex(0), it.getIndex(1), v ); return;
            case 3: ds.putValue( it.getIndex(0), it.getIndex(1), it.getIndex(2), v ); return;
            default: throw new IllegalArgumentException("rank limit");
        }
    }
            
    public static void main(String[] args ) {
        double[] back= new double[20];
        for ( int i=0; i<back.length; i++ ) back[i]=i;
        //QDataSet ds= new IndexGenDataSet(20);
        QDataSet ds= DDataSet.wrapRank2(back,4);
        //QDataSet ds= DDataSet.wrapRank3(back,2,2);
        for ( OldDataSetIterator it= OldDataSetIterator.create(ds); it.hasNext(); ) {
            System.err.println(it.next());
        }
        
    }
    
}
