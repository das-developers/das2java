/*
 * DataSetOps.java
 *
 * Created on January 29, 2007, 9:48 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.virbo.dataset;

import org.das2.datum.Units;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

/**
 * Useful operations for QDataSets
 * @author jbf
 */
public class DataSetOps {

    /**
     *slice on the first dimension
     */
    public static MutablePropertyDataSet slice0(final QDataSet ds, final int index) {
        return new Slice0DataSet(ds, index);
    }

    /**
     * this strange dataset operator assumes a square or qube dataset
     * by picking the index-th element of dataset's second dimension, without
     * regard to tags.
     */
    public static MutablePropertyDataSet slice1(final QDataSet ds, final int index) {
        return new Slice1DataSet(ds, index);
    }

    /**
     * this strange dataset operator assumes a square or qube dataset
     * by picking the index-th element of dataset's second dimension, without
     * regard to tags.
     */
    public static MutablePropertyDataSet slice2(final QDataSet ds, final int index) {
        if (ds.rank() != 3) {
            throw new IllegalArgumentException("rank must = 3");
        }

        MutablePropertyDataSet result = new AbstractDataSet() {

            public int rank() {
                return ds.rank() - 1;
            }

            public double value(int i0, int i1) {
                return ds.value(i0, i1, index);
            }

            public Object property(String name) {
                if (properties.containsKey(name)) {
                    return properties.get(name);
                } else {
                    return ds.property(name);
                }
            }

            public Object property(String name, int i) {
                Object p = properties.get(name);
                return (p != null) ? p : ds.property(name, i);
            }

            public int length() {
                return ds.length();
            }

            public int length(int i0) {
                return ds.length(i0);
            }
        };

        result.putProperty(QDataSet.DEPEND_2, null);

        return result;
    }

    public static MutablePropertyDataSet trim(final QDataSet ds, final int offset, final int len) {

        MutablePropertyDataSet result = new AbstractDataSet() {

            public int rank() {
                return ds.rank();
            }

            @Override
            public Object property(String name, int i0, int i1) {
                return super.property(name, i0, i1);
            }

            @Override
            public double value(int i) {
                return ds.value(offset + i);
            }

            @Override
            public double value(int i0, int i1) {
                return ds.value(i0 + offset, i1);
            }

            @Override
            public double value(int i0, int i1, int i2) {
                return ds.value(i0 + offset, i1, i2);
            }

            public Object property(String name) {
                if (properties.containsKey(name)) {
                    return properties.get(name);
                } else {
                    return ds.property(name);
                }
            }

            public Object property(String name, int i) {
                Object p = properties.get(name);
                return (p != null) ? p : ds.property(name, i);
            }

            public int length() {
                return len;
            }

            public int length(int i0) {
                return ds.length(i0);
            }

            public int length(int i0, int i1) {
                return ds.length(i0, i1);
            }
        };

        QDataSet dep0 = (QDataSet) ds.property(QDataSet.DEPEND_0);
        if (dep0 != null) {
            result.putProperty(QDataSet.DEPEND_0, trim(dep0, offset, len));
        }
        return result;

    }

    /**
     * removes the index-th element from the array.
     * @param array
     * @param index
     * @return
     */
    public static int[] removeElement(int[] array, int index) {
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
     * @param start first index to include.
     * @param end last index, exclusive
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
                if (properties.containsKey(name)) {
                    return properties.get(name);
                } else {
                    return ds.property(name);
                }
            }

            public Object property(String name, int i) {
                if (properties.containsKey(name)) {
                    return properties.get(name);
                } else {
                    return ds.property(name);
                }
            }

            public Object property(String name, int i0, int i1) {
                if (properties.containsKey(name)) {
                    return properties.get(name);
                } else {
                    return ds.property(name);
                }
            }

            public int length() {
                return ds.rank() == 1 ? end - start : ds.length();
            }

            public int length(int i) {
                return ds.rank() == 2 ? end - start : ds.length(i);
            }

            public int length(int i, int j) {
                return ds.rank() == 3 ? end - start : ds.length(i, j);
            }
        };

        String depNName = "DEPEND_" + (ds.rank() - 1);
        QDataSet depN = (QDataSet) ds.property(depNName);
        if (depN != null) {
            depN = leafTrim(depN, start, end);
        }
        result.putProperty(depNName, depN);

        return result;

    }

    /**
     * returns a list of indeces that sort the dataset.  I don't like this implementation, because
     * it requires that an array of Integers (not int[]) be created.  Invalid measurements are not indexed in
     * the returned dataset.
     */
    public static QDataSet sort(final QDataSet ds) {
        if (ds.rank() > 1) {
            throw new IllegalArgumentException( "dataset must be rank 1");
        }
        Integer[] indeces = new Integer[ds.length()];
        int i0 = 0;
        
        QDataSet wds= DataSetUtil.weightsDataSet(ds);
        
        for (int i = 0; i < ds.length(); i++) {
            if ( wds.value(i)>0. ) {
                indeces[i0] = new Integer(i);
                i0++;
            }
        }
        Comparator<Integer> c = new Comparator<Integer>() {
            public int compare(Integer o1, Integer o2) {
                int i1 = o1.intValue();
                int i2 = o2.intValue();
                return Double.compare(ds.value(i1), ds.value(i2));
            }
        };
        Arrays.sort(indeces, 0, i0, c);
        final int[] data = new int[i0];
        for (int i = 0; i < i0; i++) {
            data[i] = indeces[i].intValue();
        }
        MutablePropertyDataSet result = IDataSet.wrap(data);
        result.putProperty(QDataSet.NAME, "sort" + ds.length());
        return result;
    }

    /**
     * Applies the sort index to the idim-th dimension of the qube dataset ds.
     * TODO: consider sorting multiple dimensions at once, to reduce excessive copying.
     * @param ds rank 1,2, or 3 qube dataset
     * @param idim the dimension being sorted.
     * @param sort rank 1 dataset of new indeces, needn't be same size as index.
     * @param deps do dependencies as well. Note this does not rearrange planes!
     * @return new dataset that is a copy of the first, resorted.
     * @see  org.virbo.dataset.SortDataSet for similar functionality
     */
    public static WritableDataSet applyIndex( QDataSet ds, int idim, QDataSet sort, boolean deps ) {

        if (idim > 2) {
            throw new IllegalArgumentException("idim must be <=2 ");
        }
        if (ds.rank() > 3) {
            throw new IllegalArgumentException("rank limit");
        }

        if ( idim==0 ) {
            return DDataSet.copy( new SortDataSet( ds, sort ) );
        }
        
        int[] qube = DataSetUtil.qubeDims( ds );
        if ( qube==null ) throw new IllegalArgumentException("dataset is not a qube and index is not on first dimension");
        
        qube[idim] = sort.length();

        DDataSet cds = DDataSet.create(qube);
        org.virbo.dataset.DataSetUtil.putProperties(org.virbo.dataset.DataSetUtil.getProperties(ds), cds);
        
        if (deps) {
            String depprop = "DEPEND_" + idim;

            QDataSet depds = (QDataSet) ds.property(depprop);
            if (depds != null) {
                depds = applyIndex(depds, 0, sort, false);
                cds.putProperty(depprop, depds);
            }
        }

        if (idim == 0) {
            for (int i = 0; i < qube[0]; i++) {
                if (ds.rank() > 1) {
                    for (int j = 0; j < qube[1]; j++) {
                        if (ds.rank() > 2) {
                            for (int k = 0; k < qube[2]; k++) {
                                double d = ds.value((int) sort.value(i), j, k);
                                cds.putValue(i, j, k, d);
                            }
                        } else {
                            double d = ds.value((int) sort.value(i), j);
                            cds.putValue(i, j, d);
                        }
                    }
                } else {
                    double d = ds.value((int) sort.value(i));
                    cds.putValue(i, d);
                }
            }
        } else if (idim == 1) {
            for (int i = 0; i < qube[0]; i++) {
                for (int j = 0; j < qube[1]; j++) {
                    if (ds.rank() > 2) {
                        for (int k = 0; k < qube[2]; k++) {
                            double d = ds.value(i, (int) sort.value(j), k);
                            cds.putValue(i, j, k, d);
                        }
                    } else {
                        double d = ds.value(i, (int) sort.value(j));
                        cds.putValue(i, j, d);
                    }
                }
            }
        } else if (idim == 2) {
            for (int i = 0; i < qube[0]; i++) {
                for (int j = 0; j < qube[1]; j++) {
                    for (int k = 0; k < qube[2]; k++) {
                        double d = ds.value(i, j, (int) sort.value(k));
                        cds.putValue(i, j, k, d);
                    }
                }
            }
        }

        return cds;
    }

    /**
     * returns a rank 1 dataset that is a histogram of the data.
     * @param ds rank N dataset
     * @param min the min of the first bin.
     * @param max the max of the last bin.
     * @param binsize the size of each bin.
     * @return a rank 1 dataset with each bin's count.  DEPEND_0 indicates the bin locations.
     */
    public static QDataSet histogram(QDataSet ds, final double min, final double max, final double binsize) {
        int n = (int) Math.ceil((max - min) / binsize);
        MutablePropertyDataSet tags = DataSetUtil.tagGenDataSet(n, min, binsize);
        
        tags.putProperty( QDataSet.NAME, ds.property(QDataSet.NAME) );
        tags.putProperty( QDataSet.LABEL, ds.property(QDataSet.LABEL) );
        tags.putProperty( QDataSet.TITLE, ds.property(QDataSet.TITLE) );
        tags.putProperty( QDataSet.TYPICAL_MAX, ds.property(QDataSet.TYPICAL_MAX) );
        tags.putProperty( QDataSet.TYPICAL_MIN, ds.property(QDataSet.TYPICAL_MIN) );
        tags.putProperty( QDataSet.CADENCE, binsize );
        
        final int[] hits = new int[n];

        QubeDataSetIterator iter = new QubeDataSetIterator(ds);
        QDataSet wds= DataSetUtil.weightsDataSet(ds);

        int count=0;
        for (; iter.hasNext();) {
            iter.next();
            double d = iter.getValue(ds);
            double w = iter.getValue(wds);
            if ( w>0. ) {
                int ibin = (int) ((d - min) / binsize);
                if (ibin >= 0 && ibin < n) {
                    hits[ibin]++;
                }
                count++;
            }
        }

        IDataSet result = IDataSet.wrap(hits);
        result.putProperty(QDataSet.DEPEND_0, tags);
        result.putProperty("count",count);

        return result;
    }

    /**
     * performs the moment (mean,variance,etc) on the dataset.
     * @param ds rank N QDataSet.
     * @return QDataSet rank 1 dataset with one tag, the mean.  Properties
     *   contain other stats.
     */
    public static QDataSet moment(QDataSet ds) {

        double[] moment = new double[2];

        Units u = (Units) ds.property(QDataSet.UNITS);
        if (u == null) {
            u = Units.dimensionless;
        }
        int validCount = 0;
        int invalidCount = 0;

        double approxMean = 0.;

        QDataSet wds= DataSetUtil.weightsDataSet(ds);
        QubeDataSetIterator iter= new QubeDataSetIterator(ds);
        
        while (iter.hasNext()) {
            iter.next();
            double d = iter.getValue(ds);
            double w = iter.getValue(wds);
            if ( w==0.0 ) {
                invalidCount++;
            } else {
                validCount++;
                approxMean += d;
            }
        }

        if (validCount > 0) {
            approxMean /= validCount; // approximate--suseptible to number error.
        }

        double mean = 0;
        double stddev = 0;

        if (validCount > 0) {
            iter= new QubeDataSetIterator(ds);
            while (iter.hasNext()) {
                double d = iter.getValue(ds);
                double w = iter.getValue(wds);
                if ( w>0.0 ) {
                    mean += (d - approxMean);
                    stddev += Math.pow(d - approxMean, 2);
                }
            }

            mean /= validCount;
            mean += approxMean;

            moment[0] = mean;

            if (validCount > 1) {
                stddev /= (validCount - 1); // this will be very close to result, even though correction should be made since approxMean != mean.
                stddev = Math.sqrt(stddev);
                moment[1] = stddev;
            } else {
                moment[1] = u.getFillDouble();
            }

        } else {
            moment[0] = u.getFillDouble();
        }

        DDataSet result = DDataSet.createRank1(1);
        result.putValue(0, moment[0]);
        result.putProperty("stddev", moment[1]);
        result.putProperty("validCount", validCount);
        result.putProperty("invalidCount", invalidCount);
        result.putProperty("rank", ds.rank());

        return result;
    }

    /**
     * transpose the rank 2 qube dataset so the rows are columns and the columns are rows.
     * @param ds rank 2 Qube DataSet.
     * @return rank 2 Qube DataSet
     */
    public static QDataSet transpose2(QDataSet ds) {
        return new TransposeRank2DataSet(ds);
    }
}
