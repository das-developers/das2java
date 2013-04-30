/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.dataset;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author jbf
 */
public class QubeDataSetIterator implements DataSetIterator {

    public interface DimensionIterator {

        boolean hasNext();

        int nextIndex();

        int index();

        int length();
    }

    public interface DimensionIteratorFactory {

        DimensionIterator newIterator(int len);
    }

    public static class StartStopStepIterator implements DimensionIterator {

        int start;
        int stop;
        int step;
        int index;
        boolean all; // just for toString

        public StartStopStepIterator(int start, int stop, int step, boolean all) {
            this.start = start;
            this.stop = stop;
            this.step = step;
            this.index = start - step;
            this.all = all;
        }

        public boolean hasNext() {
            return index + step < stop;
        }

        public int nextIndex() {
            index += step;
            return index;
        }

        public int index() {
            return index;
        }

        public int length() {
            int remainder= (stop - start) % step;
            return (stop - start) / step + ( remainder>0 ? 1 :0 );
        }

        @Override
        public String toString() {
            return all ? ":" : "" + start + ":" + stop + (step == 1 ? "" : ":" + step);
        }
    }

    public static class StartStopStepIteratorFactory implements DimensionIteratorFactory {

        Number start;
        Number stop;
        Number step;

        public StartStopStepIteratorFactory(Number start, Number stop, Number step) {
            this.start = start;
            this.stop = stop;
            this.step = step;
        }

        public DimensionIterator newIterator(int length) {
            int start1 = start == null ? 0 : start.intValue();
            int stop1 = stop == null ? length : stop.intValue();
            int step1 = step == null ? 1 : step.intValue();
            if (start1 < 0) {
                start1 = length + start1;
            }
            if (stop1 < 0) {
                stop1 = length + stop1;
            }

            return new StartStopStepIterator(start1, stop1, step1, start == null && stop == null && step == null);
        }
    }

    public static class IndexListIterator implements DimensionIterator {

        QDataSet ds;
        int index;

        public IndexListIterator(QDataSet ds) {
            this.ds = ds;
            this.index = -1;
        }

        public boolean hasNext() {
            return index < ds.length()-1;
        }

        public int nextIndex() {
            index++;
            return (int) ds.value(index);
        }

        public int index() {
            return (int) ds.value(index);
        }

        public int length() {
            return ds.length();
        }

        @Override
        public String toString() {
            String dstr= ds.toString();
            dstr= dstr.replace("(dimensionless)", "");
            return "[" + dstr  + " @ " +index + "]";
        }
    }

    public static class IndexListIteratorFactory implements DimensionIteratorFactory {

        QDataSet ds;

        public IndexListIteratorFactory(QDataSet ds) {
            this.ds = ds;
        }

        public DimensionIterator newIterator(int length) {
            ArrayDataSet list= ArrayDataSet.copy(ds);
            for ( int i=0; i<list.length(); i++ ) {
                if ( list.value(i)<0 ) list.putValue(i,list.value(i)+length);
            }
            return new IndexListIterator(list);
        }
    }

    public static class SingletonIterator implements DimensionIterator {

        int index;
        boolean hasNext = true;

        public SingletonIterator(int index) {
            this.index = index;
        }

        public boolean hasNext() {
            return hasNext;
        }

        public int nextIndex() {
            hasNext = false;
            return index;
        }

        public int index() {
            return index;
        }

        public int length() {
            return 1;
        }

        @Override
        public String toString() {
            return "" + index;
        }
    }

    public static class SingletonIteratorFactory implements DimensionIteratorFactory {

        int index;

        public SingletonIteratorFactory(int index) {
            this.index = index;
        }

        public DimensionIterator newIterator(int length) {
            if ( index<0 ) {
                return new SingletonIterator(length+index);
            } else {
                return new SingletonIterator(index);
            }
            
        }
    }
    private DimensionIterator[] it = new DimensionIterator[4];
    private DimensionIteratorFactory[] fit = new DimensionIteratorFactory[4];
    private boolean isAllIndexLists;

    private int rank;
    private int[] qube;
    private QDataSet ds;
    private boolean allnext = true;  // we'll have to do a borrow to get started.

    /**
     * dataset iterator to help in implementing the complex indexing
     * types of python.  
     * 
     * create a new iterator, set the index iterator factories, iterate.
     * @param ds
     */
    public QubeDataSetIterator(QDataSet ds) {
        List<String> problems= new ArrayList();
        if ( ! DataSetUtil.validate(ds,problems) ) {
            throw new IllegalArgumentException("data doesn't validate: "+problems );
        }
        if (Boolean.TRUE.equals(ds.property(QDataSet.QUBE))) {
            this.qube = DataSetUtil.qubeDims(ds);
            this.ds = ds;
        } else {
            this.ds = ds;
        }
        this.rank = ds.rank();
        for (int i = 0; i < ds.rank(); i++) {
            fit[i] = new StartStopStepIteratorFactory(0, null, 1);
        }
        initialize();
    }

    /**
     * internal constructor.  Note we have to have both constructors, which must
     * be kept in sync, because of initialize().
     * @param ds
     * @param fits
     */
    private QubeDataSetIterator(QDataSet ds, DimensionIteratorFactory[] fits) {
        if (Boolean.TRUE.equals(ds.property(QDataSet.QUBE))) {
            this.qube = DataSetUtil.qubeDims(ds);
            this.ds = ds;
        } else {
            this.ds = ds;
        }
        this.rank = ds.rank();
        this.ds = ds;
        this.fit = fits;
        initialize();
    }

    /**
     * return an iterator for the slice of a dataset.  This is introduced to improve
     * performance by reducing the number of bounds checks, etc from the general
     * case.
     * @param ds
     * @param sliceIndex
     * @return
     */
    public static QubeDataSetIterator sliceIterator(QDataSet ds, int sliceIndex) {
        QubeDataSetIterator result;
        if (ds.rank() == 0) {
            throw new IllegalArgumentException("can't slice rank 0");
        } else if (ds.rank() == 1) {
            throw new IllegalArgumentException("can't slice rank 1");
        } else if (ds.rank() == 2) {
            result = new QubeDataSetIterator(ds, 
                    new DimensionIteratorFactory[]{new SingletonIteratorFactory(sliceIndex),
                    new StartStopStepIteratorFactory(0, ds.length(sliceIndex), 1)});
        } else if (ds.rank() == 3) {
            result = new QubeDataSetIterator(ds,
                    new DimensionIteratorFactory[]{new SingletonIteratorFactory(sliceIndex),
                        new StartStopStepIteratorFactory(0, ds.length(sliceIndex), 1),
                        new StartStopStepIteratorFactory(0, null, 1),});
        } else if (ds.rank() == 4) {
            result = new QubeDataSetIterator( ds,
                    new DimensionIteratorFactory[]{new SingletonIteratorFactory(sliceIndex),
                        new StartStopStepIteratorFactory(0, ds.length(sliceIndex), 1),
                        new StartStopStepIteratorFactory(0, null, 1),
                        new StartStopStepIteratorFactory(0, null, 1), } );
        } else {
            throw new IllegalArgumentException("rank limit");
        }
        return result;
    }

    /**
     * reinitializes the iterator.
     * @param dim
     * @param fit
     */
    public void setIndexIteratorFactory(int dim, DimensionIteratorFactory fit) {
        if ( dim >= this.rank ) {
            throw new IllegalArgumentException( String.format( "rank limit: rank %d dataset %s has no index %d", ds.rank(), ds, dim ) );
        }
        this.fit[dim] = fit;
        initialize();
    }

    /**
     * now that the factories are configured, initialize the iterator to
     * begin iterating.
     */
    private void initialize() {
        boolean allLi= true;
        for (int i = 0; i < rank; i++) {
            int dimLength= dimLength(i);
            it[i] = fit[i].newIterator(dimLength);
            if ( !( it[i] instanceof IndexListIterator ) ) allLi= false;
        }
        this.isAllIndexLists= allLi;
    }

    /**
     * return the length of the dimension.  This is introduced with the intention
     * that this can support non-qube datasets as well.
     * @param idim
     * @return
     */
    private int dimLength(int idim) {
        if (qube != null) {
            return qube[idim];
        } else {
            int result = 0;
            switch (idim) {
                case 0:
                    result = ds.length();
                    break;
                case 1:
                    result = ds.length(Math.max(0, index(0)));
                    break;
                case 2:
                    result = ds.length(Math.max(0, index(0)), Math.max(0, index(1)));
                    break;
                case 3:
                    result = ds.length(Math.max(0, index(0)), Math.max(0, index(1)), Math.max(0, index(2)));
                    break;
                default:
                    throw new IllegalArgumentException("dimension not supported: " + idim);
            }
            return result;
        }
    }

    public boolean hasNext() {
        if (rank == 0) {
            return this.allnext;
        } else {
            if ( it[0].length()==0 ) return false; // check for empty datasets.
            if ( qube!=null ) {
                for ( int i=1; i<rank; i++ ) {
                    if ( it[i].length()==0 ) return false; // this is true only for Qubes.
                }
            }
            int i = rank - 1;
            if (it[i].hasNext()) {
                return true;
            } else {
                if (i > 0) {
                    for (int j = i - 1; j >= 0; j--) {
                        if (it[j].hasNext()) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    public void next() {

        if (this.allnext) {
            for (int i = 0; i < (rank - 1); i++) {
                if ( !( isAllIndexLists && ( it[i] instanceof IndexListIterator ) ) ) it[i].nextIndex();
            }
            allnext = false;
            if (rank == 0) {
                return;
            }
        }

        // implement borrow logic
        int i = rank - 1;
        if (it[i].hasNext()) {
            it[i].nextIndex();
            if ( it[i] instanceof IndexListIterator ) { // all index lists need to be incremented together.
                for ( int k=0; k<i; k++ ) {
                    if ( isAllIndexLists && ( it[k] instanceof IndexListIterator ) ) {
                        it[k].nextIndex();
                    }
                }
            }
        } else {
            if (i > 0) {
                for (int j = i - 1; j >= 0; j--) {
                    if (it[j].hasNext()) {
                        it[j].nextIndex();
                        if ( it[j] instanceof IndexListIterator ) { // all index lists need to be incremented together.
                            for ( int k=0; k<j; k++ ) {
                                if ( isAllIndexLists && ( it[k] instanceof IndexListIterator ) ) {
                                    it[k].nextIndex();
                                }
                            }
                        }
                        for (int k = j + 1; k <= i; k++) {
                            it[k] = fit[k].newIterator(dimLength(k));
                            it[k].nextIndex();
                        }
                        break;
                    }
                }
            } else {
                throw new IllegalArgumentException("no more elements");
            }
        }
    }

    public int index(int dim) {
        return it[dim].index();
    }

    /**
     * returns the length reported by this iterator.  Use caution, because this
     * does not imply that the result is a qube and does not account for slices.
     */
    public int length(int dim) {
        return it[dim].length();
    }

    public int rank() {
        int result= rank;
        for ( int i=0; i<it.length; i++ ) {
            if ( it[i] instanceof QubeDataSetIterator.SingletonIterator ) {
                result--;
            }
        }
        return result;
    }

    @Override
    public String toString() {
        if (rank == 0) {
            return "Iter hasNext=" + hasNext();
        } else {
            String its = it[0].toString();
            String ats = "" + it[0].index();
            for (int i = 1; i < rank; i++) {
                its = its + "," + it[i].toString();
                ats = ats + "," + it[i].index();
            }
            return "Iter [" + its + "] @ [" + ats + "] ";
        }
    }

    /**
     * return a dataset that will have the same geometry at the
     * dataset implied by each dimension iterator.  This is
     * introduced to encapsulate this dangerous code to here where it could
     * be done correctly.  Right now this assumes QUBES.
     *
     * Do not pass the result of this into the putValue of this iterator,
     * the result should have its own iterator.
     *
     * 20101006 jbf: copy dimensional metadata DEPEND_0, etc where possible
     * 20120718 jbf: bugfix with ds[7,16,4,:]. Support BINS.
     * @return empty dataset with structural properties set.
     */
    public DDataSet createEmptyDs() {
        List<Integer> qqube = new ArrayList<Integer>();
        List<Integer> dimMap= new ArrayList<Integer>();  //e.g. [0,1,2,3]
        for (int i = 0; i < this.it.length; i++) {
            if ( this.it[i]==null ) {
                continue;
            }
            boolean reform=  this.it[i] instanceof SingletonIterator;
            if (!reform) {
                qqube.add( this.it[i].length() );
                dimMap.add( i );
            }
        }
        
        int[] qube1;
        if (isAllIndexLists) {  // a=[1,2,3]; b=[2,3,1]; Z[a,b] is rank 1
            qube1=  new int[] { this.it[0].length() };
        } else {
            qube1 = new int[qqube.size()];
            for (int i = 0; i < qqube.size(); i++) {
                qube1[i] = qqube.get(i);
            }
        }
        DDataSet result = DDataSet.create(qube1);

        // move the structural properties like DEPEND_i and BUNDLE_i.  idim refers to the source, i refers to the result.
        for ( int i=0; i<dimMap.size(); i++ ) {
            int idim= dimMap.get(i);
            QDataSet dep= (QDataSet) this.ds.property("DEPEND_"+idim);
            QDataSet bund= (QDataSet) this.ds.property("BUNDLE_"+idim);
            String bins= (String) this.ds.property("BINS_"+idim);
            if ( fit[idim] instanceof StartStopStepIteratorFactory ) {
                if ( dep!=null && dep.rank()==1 ) {
                    StartStopStepIterator sssi= (StartStopStepIterator)it[idim];
                    if ( sssi.step==1 && sssi.start==0 && sssi.stop==dep.length() ) {
                        result.putProperty( "DEPEND_"+i, dep );
                    } else if ( sssi.step==1 ) {
                        result.putProperty( "DEPEND_"+i, dep.trim( sssi.start, sssi.stop ) );
                    } else {
                        QDataSet depSlice= DataSetOps.trim( dep, sssi.start, sssi.stop, sssi.step );
                        result.putProperty( "DEPEND_"+i, depSlice );
                    }
                }
                if ( bund!=null && it[idim].length()==bund.length() ) { 
                    result.putProperty( "BUNDLE_"+i, bund );
                }
                if ( bins!=null && it[idim].length()==bins.split(",").length ) { //TODO: verify this
                    result.putProperty( "BINS_"+i, bins );
                }
            }
        }
        return result;

    }

    /**
     * get the value from ds at the current iterator position.
     * @param ds a dataset with compatible geometry as the iterator's geometry.
     * @return the value of ds at the current iterator position.
     */
    public final double getValue(QDataSet ds) {
        switch (rank) {
            case 0:
                return ds.value();
            case 1:
                return ds.value(index(0));
            case 2:
                return ds.value(index(0), index(1));
            case 3:
                return ds.value(index(0), index(1), index(2));
            case 4:
                return ds.value(index(0), index(1), index(2), index(3) );
            default:
                throw new IllegalArgumentException("rank limit");
        }
    }

    /**
     * replace the value in ds at the current iterator position.
     * @param ds a writable dataset with capatible geometry as the iterator's geometry.
     * @param v the value to insert.
     */
    public final void putValue(WritableDataSet ds, double v) {
        switch (rank) {
            case 0:
                ds.putValue(v);
                return;
            case 1:
                ds.putValue(index(0), v);
                return;
            case 2:
                ds.putValue(index(0), index(1), v);
                return;
            case 3:
                ds.putValue(index(0), index(1), index(2), v);
                return;
            case 4:
                ds.putValue(index(0), index(1), index(2), index(3), v);
                return;
            default:
                throw new IllegalArgumentException("rank limit");
        }
    }
}
