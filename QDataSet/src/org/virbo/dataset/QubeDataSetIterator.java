/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.dataset;

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
            return (stop - start) / step;
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
            return index < ds.length();
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
            return "[" + ds.toString() + "]";
        }
    }

    public static class IndexListIteratorFactory implements DimensionIteratorFactory {

        QDataSet ds;

        public IndexListIteratorFactory(QDataSet ds) {
            this.ds = ds;
        }

        public DimensionIterator newIterator(int length) {
            return new IndexListIterator(ds);
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
            return new SingletonIterator(index);
        }
    }
    private DimensionIterator[] it = new DimensionIterator[4];
    private DimensionIteratorFactory[] fit = new DimensionIteratorFactory[4];
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
        if (Boolean.TRUE.equals(ds.property(QDataSet.QUBE))) {
            this.qube = DataSetUtil.qubeDims(ds);
        } else {
            this.ds = ds;
        }
        this.rank = ds.rank();
        for (int i = 0; i < ds.rank(); i++) {
            fit[i] = new StartStopStepIteratorFactory(0, null, 1);
        }
        initialize();
    }

    private QubeDataSetIterator(QDataSet ds, DimensionIteratorFactory[] fits) {
        if (Boolean.TRUE.equals(ds.property(QDataSet.QUBE))) {
            this.qube = DataSetUtil.qubeDims(ds);
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
        this.fit[dim] = fit;
        initialize();
    }

    /**
     * now that the factories are configured, initialize the iterator to
     * begin iterating.
     */
    private void initialize() {
        for (int i = 0; i < rank; i++) {
            it[i] = fit[i].newIterator(dimLength(i));
        }
    }

    /**
     * return the length of the dimension.  This is introduced with the intention
     * that this can support non-qube datasets as well.
     * @param idim
     * @return
     */
    private final int dimLength(int idim) {
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
                it[i].nextIndex();
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
        } else {
            if (i > 0) {
                for (int j = i - 1; j >= 0; j--) {
                    if (it[j].hasNext()) {
                        it[j].nextIndex();
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

    public int length(int dim) {
        return it[dim].length();
    }

    public int rank() {
        return rank;
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
     * get the value from ds at the current iterator position.
     * @param ds a dataset with capatible geometry as the iterator's geometry.
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
            default:
                throw new IllegalArgumentException("rank limit");
        }
    }
}
