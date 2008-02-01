/*
 * DataSetOps.java
 *
 * Created on January 29, 2007, 9:48 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.virbo.dataset;

import edu.uiowa.physics.pw.das.datum.Units;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Useful operations for QDataSets
 * @author jbf
 */
public class DataSetOps {

    /**
     *slice on the first dimension
     */
    public static QDataSet slice0(final QDataSet ds, final int index) {
        return new Slice0DataSet(ds, index);
    }

    /**
     * this strange dataset operator assumes a square or qube dataset
     * by picking the index-th element of dataset's second dimension, without
     * regard to tags.
     */
    public static MutablePropertyDataSet slice1(final QDataSet ds, final int index) {
        if (ds.rank() > 2) {
            throw new IllegalArgumentException("rank limit > 2");
        }

        MutablePropertyDataSet result = new AbstractDataSet() {

            public int rank() {
                return ds.rank() - 1;
            }

            public double value(int i) {
                return ds.value(i, index);
            }

            public double value(int i0, int i1) {
                return ds.value(i0, index, i1);
            }

            public Object property(String name) {
                Object p = properties.get(name);
                return (p != null) ? p : ds.property(name);
            }

            public int length() {
                return ds.length();
            }
        };

        result.putProperty(QDataSet.QUBE, null);

        return result;
    }

    /**
     * this strange dataset operator assumes a square or qube dataset
     * by picking the index-th element of dataset's second dimension, without
     * regard to tags.
     */
    public static MutablePropertyDataSet slice2(final QDataSet ds, final int index) {
        if (ds.rank() != 3 ) {
            throw new IllegalArgumentException("rank must = 3");
        }

        MutablePropertyDataSet result = new AbstractDataSet() {

            public int rank() {
                return ds.rank() - 1;
            }

            public double value(int i0, int i1) {
                return ds.value(i0, i1, index );
            }

            public Object property(String name) {
                Object p = properties.get(name);
                return (p != null) ? p : ds.property(name);
            }

            public Object property(String name, int i) {
                Object p = properties.get(name);
                return (p != null) ? p : ds.property(name,i);
            }
            
            public int length() {
                return ds.length();
            }
            
            public int length(int i0) {
                return ds.length(i0);
            }
            
        };

        
        int[] qube= (int[]) ds.property( QDataSet.QUBE );
        if ( qube!=null ) {
            result.putProperty(QDataSet.QUBE, removeElement(qube,2) );
        }
        return result;
    }
    
    /**
     * removes the index-th element from the array.
     * @param array
     * @param index
     * @return
     */
    private static int[] removeElement(int[] array, int index) {
        int[] result = new int[array.length - 1];
        for (int i = 0; i < index; i++) {
            result[i] = array[i];
        }
        for (int i = index + 1; i < array.length; i++) {
            result[i - 1] = array[i];
        }
        return result;
    }

    /**
     * pull out a subset of the dataset by reducing the number of columns in the
     * last dimension.  This does not reduce rank.  This assumes the dataset has no
     * row with length>end.
     */
    public static MutablePropertyDataSet leafTrim(final QDataSet ds, final int start, final int end) {

        if (ds.rank() > 3) {
            throw new IllegalArgumentException("rank limit > 3");
        }

        MutablePropertyDataSet result = new AbstractDataSet() {

            public int rank() {
                return ds.rank();
            }

            public double value(int i) {
                return ds.value(i + start);
            }

            public double value(int i0, int i1) {
                return ds.value(i0, i1 + start);
            }

            public double value(int i0, int i1, int i2) {
                return ds.value(i0, i1, i2 + start);
            }

            public Object property(String name) {
                Object p = properties.get(name);
                return (p != null) ? p : ds.property(name);
            }

            public Object property(String name, int i) {
                Object p = properties.get(name);
                return (p != null) ? p : ds.property(name, i + start);
            }

            public Object property(String name, int i0, int i1) {
                Object p = properties.get(name);
                return (p != null) ? p : ds.property(name, i0, i1 + start);
            }

            public int length() {
                return ds.rank() == 1 ? end - start : ds.length();
            }

            public int length(int i) {
                return ds.rank() == 2 ? end - start : ds.length();
            }

            public int length(int i, int j) {
                return ds.rank() == 3 ? end - start : ds.length();
            }
        };

        String depNName = "DEPEND_" + (ds.rank() - 1);
        QDataSet depN = (QDataSet) ds.property(depNName);
        if (depN != null) {
            depN = leafTrim(depN, start, end);
        }
        result.putProperty(depNName, depN);

        int[] qube= (int[]) ds.property(QDataSet.QUBE);
        if ( qube!=null ) {
            qube[ds.rank()-1]= end - start;
            result.putProperty(QDataSet.QUBE, qube);
        }
        
        return result;

    }

    /**
     * returns a list of indeces that sort the dataset.  I don't like this implementation, because
     * it requires that an array of Integers (not int[]) be created.  Invalid measurements are not indexed in
     * the returned dataset.
     */
    public static QDataSet sort(final QDataSet ds) {
        if (ds.rank() > 1) {
            throw new IllegalArgumentException();
        }
        Integer[] indeces = new Integer[ds.length()];
        int i0 = 0;
        final Units u = (Units) ds.property(QDataSet.UNITS);
        for (int i = 0; i < ds.length(); i++) {
            if (u == null || !u.isFill(ds.value(i))) {
                indeces[i0] = new Integer(i);
                i0++;
            }
        }
        Comparator c = new Comparator() {

            public int compare(Object o1, Object o2) {
                int i1 = ((Integer) o1).intValue();
                int i2 = ((Integer) o2).intValue();
                return Double.compare(ds.value(i1), ds.value(i2));
            }
        };
        Arrays.sort(indeces, 0, i0, c);
        final int[] data = new int[i0];
        for (int i = 0; i < i0; i++) {
            data[i] = indeces[i].intValue();
        }
        return new IndexGenDataSet(i0) {

            public double value(int i) {
                return data[i];
            }
        };
    }

    /**
     * returns a rank 1 dataset that is a histogram of the data.
     * @param ds rank N dataset
     * @min the min of the first bin.
     * @max the max of the last bin.
     * @binsize the size of each bin.
     * @return a rank 1 dataset with each bin's count.  DEPEND_0 indicates the bin locations.
     */
    public static QDataSet histogram(QDataSet ds, final double min, final double max, final double binsize) {
        int n = (int) Math.ceil((max - min) / binsize);
        QDataSet tags = DataSetUtil.tagGenDataSet(n, min, binsize);
        final Units u = (Units) ds.property(QDataSet.UNITS);
        final int[] hits = new int[n];

        DataSetIterator iter = DataSetIterator.create(ds);

        for (; iter.hasNext();) {
            double d = iter.next();
            if (!Double.isNaN(d) && (u == null || u.isValid(d))) {
                int ibin = (int) ((d - min) / binsize);
                if (ibin >= 0 && ibin < n) {
                    hits[ibin]++;
                }
            }
        }

        IDataSet result = IDataSet.wrap(hits);
        result.putProperty(QDataSet.DEPEND_0, tags);

        return result;
    }

    /**
     * transpose the rank 2 qube dataset so the rows are columns and the columns are rows.
     */
    public static QDataSet transpose2(QDataSet ds) {
        return new TransposeRank2DataSet(ds);
    }
}
