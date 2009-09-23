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
import java.util.Scanner;
import org.virbo.dsops.Ops;

/**
 * Useful operations for QDataSets
 * @author jbf
 */
public class DataSetOps {

    /**
     * absolute length limit for plots.  This is used to limit the elements used in autoranging, etc.
     */
    public final static int DS_LENGTH_LIMIT= 10000000;
    
    /**
     * return a dataset that has mutable properties.  If the dataset parameter already has, then the 
     * dataset is returned.
     * @param dataset
     * @return a WritableDataSet that is either a copy of the read-only dataset provided, or the parameter writable dataset provided.
     */
    public static MutablePropertyDataSet makePropertiesMutable( final QDataSet dataset) {
        if ( dataset instanceof MutablePropertyDataSet ) {
            return (MutablePropertyDataSet) dataset;
        } else {
            return new DataSetWrapper(dataset);
        }
    }

    /**
     * return a dataset that is writable.  If the dataset parameter is already writable, then the 
     * dataset is returned.
     * @param dataset
     * @return a WritableDataSet that is either a copy of the read-only dataset provided, or the parameter writable dataset provided.
     */
    public static WritableDataSet makeWritable(QDataSet dataset) {
        if ( dataset instanceof WritableDataSet ) {
            return (WritableDataSet) dataset;
        } else {
            return DDataSet.copy(dataset);
        }
    }

    /**
     *slice on the first dimension
     */
    public static MutablePropertyDataSet slice0(final QDataSet ds, final int index) {
        return new Slice0DataSet(ds, index);
    }

    /**
     * slice dataset operator assumes a qube dataset
     * by picking the index-th element of dataset's second dimension, without
     * regard to tags.
     */
    public static MutablePropertyDataSet slice1(final QDataSet ds, final int index) {
        return new Slice1DataSet(ds, index);
    }

    /**
     * slice dataset operator assumes a qube dataset
     * by picking the index-th element of dataset's second dimension, without
     * regard to tags.
     */
    public static MutablePropertyDataSet slice2(final QDataSet ds, final int index) {
        return new Slice2DataSet(ds, index);
    }

    /**
     * slice dataset operator assumes a qube dataset
     * by picking the index-th element of dataset's second dimension, without
     * regard to tags.
     */
    public static MutablePropertyDataSet slice3(final QDataSet ds, final int index) {
        return new Slice3DataSet(ds, index);
    }
    
    /**
     * reduce the number of elements in the dataset to the dim 0 indeces specified.
     * This does not change the rank of the dataset.
     *
     * @param ds
     * @param offset
     * @param len
     * @return
     */
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
        MutablePropertyDataSet tags = DataSetUtil.tagGenDataSet(n, min, binsize, (Units)ds.property(QDataSet.UNITS) );
        
        tags.putProperty( QDataSet.NAME, ds.property(QDataSet.NAME) );
        tags.putProperty( QDataSet.LABEL, ds.property(QDataSet.LABEL) );
        tags.putProperty( QDataSet.TITLE, ds.property(QDataSet.TITLE) );
        tags.putProperty( QDataSet.TYPICAL_MAX, ds.property(QDataSet.TYPICAL_MAX) );
        tags.putProperty( QDataSet.TYPICAL_MIN, ds.property(QDataSet.TYPICAL_MIN) );
        
        final int[] hits = new int[n];
        int maxFreq= 0;
        
        QubeDataSetIterator iter = new QubeDataSetIterator(ds);
        QDataSet wds= DataSetUtil.weightsDataSet(ds);

        int count=0;
        for (; count<DS_LENGTH_LIMIT && iter.hasNext();) {
            iter.next();
            double d = iter.getValue(ds);
            double w = iter.getValue(wds);
            if ( w>0. ) {
                int ibin = (int) ((d - min) / binsize);
                if (ibin >= 0 && ibin < n) {
                    hits[ibin]++;
                    if ( hits[ibin]>maxFreq ) maxFreq= hits[ibin];
                }
                count++;
            }
        }

        IDataSet result = IDataSet.wrap(hits);
        result.putProperty( QDataSet.DEPEND_0, tags );
        result.putProperty( "count", count );
        result.putProperty( "max", maxFreq );

        return result;
    }
    
    /**
     * performs the moment (mean,variance,etc) on the dataset.
     * @param ds rank N QDataSet.
     * @return rank 0 dataset of the mean.  Properties contain other stats:
     *   stddev, RankZeroDataSet
     *   validCount, Integer, the number valid measurements
     *   invalidCount, Integer, the number of invalid measurements
     */
    public static RankZeroDataSet moment(QDataSet ds) {

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
                iter.next(); 
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

        DRank0DataSet result = DataSetUtil.asDataSet(moment[0]);
        result.putProperty( QDataSet.UNITS, u );
        DRank0DataSet stddevds= DataSetUtil.asDataSet(moment[1]);
        stddevds.putProperty( QDataSet.UNITS, u.getOffsetUnits() );
        result.putProperty("stddev", stddevds );
        result.putProperty("validCount", validCount);
        result.putProperty("invalidCount", invalidCount);

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

    /**
     * Extract a bundled dataset from a bundle of datasets.  The input should
     * be a rank 2 dataset with the property BUNDLE_1 set to a bundle descriptor
     * dataset.  See BundleDataSet for more semantics.
     *
     * @param aThis
     * @param ib index of the dataset to extract.
     * @return
     */
    public static QDataSet unbundle(QDataSet bundleDs, int ib) {
        QDataSet bundle1= (QDataSet) bundleDs.property(QDataSet.BUNDLE_1);
        String[] names= new String[bundle1.length()];
        int[] offsets= new int[bundle1.length()];
        int[] lens= new int[bundle1.length()];

        int idx=0;
        for ( int j=0; j<bundle1.length(); j++ ) {
            names[j]= (String) bundle1.property( QDataSet.NAME, j );
            offsets[j]= idx;
            int n= 1;
            for (int k = 0; k < bundle1.length(j); k++) {
                 n *= bundle1.value(j, k);
            }
            lens[j]= n;
            idx+= n;
        }

        int j= ib;
        int n= lens[j];

        if ( bundle1.length(j)==0 ) {
            MutablePropertyDataSet result= DataSetOps.slice1(bundleDs,offsets[j]);
            return result;
        } else if ( bundle1.length(j)==1 ) {
            TrimStrideWrapper result= new TrimStrideWrapper(bundleDs);
            result.setTrim( 1, offsets[j], offsets[j]+lens[j], 1 );
            Map<String,Object> props= DataSetUtil.getProperties( DataSetOps.slice0(bundle1,j) );
            DataSetUtil.putProperties( props, result );
            String[] testProps= new String[] { QDataSet.DEPEND_0, QDataSet.DELTA_MINUS, QDataSet.DELTA_PLUS, QDataSet.PLANE_0 };
            for ( int i=0; i<testProps.length; i++ ) {
                String prop= testProps[i];
                Object dep0= result.property(prop);
                if ( dep0!=null ) {
                    if ( dep0 instanceof String ) {
                        idx= Arrays.asList(names).indexOf(dep0);
                        if ( idx==-1 ) throw new IllegalArgumentException("unable to find DEPEND_0 reference to \""+dep0+"\"");
                        result.putProperty( prop, unbundle(bundleDs,idx) );
                    }
                }
            }
            return result;
        } else {
            throw new IllegalArgumentException("rank limit: >2 not supported");
        }

    }

    /**
     * see http://www.papco.org/wiki/index.php/DataReductionSpecs
     * @param c
     * @param fillDs
     * @return
     */
    public static QDataSet sprocess(String c, QDataSet fillDs) {
        int i=1;
        Scanner s= new Scanner( c );
        s.useDelimiter("[\\(\\),]");

        while ( s.hasNext() ) {
            String cmd= s.next();
            if ( cmd.startsWith("|slice") ) {
                int dim= cmd.charAt(6)-'0';
                int idx= s.nextInt();
                if ( dim==0 ) {
                    if ( idx>=fillDs.length() ) idx=fillDs.length()-1;
                    if ( idx<0 ) idx=0;
                    fillDs= slice0(fillDs, idx);
                } else if ( dim==1 ) {
                    if ( idx>=fillDs.length(0) ) idx=fillDs.length(0)-1;
                    if ( idx<0 ) idx=0;
                    fillDs= slice1(fillDs, idx);
                } else if ( dim==2 ) {
                    if ( idx>=fillDs.length(0,0) ) idx=fillDs.length(0,0)-1;
                    if ( idx<0 ) idx=0;
                    fillDs= slice2(fillDs, idx);
                } else if ( dim==3 ) {
                    if ( idx>=fillDs.length(0,0,0) ) idx=fillDs.length(0,0,0)-1;
                    if ( idx<0 ) idx=0;
                    fillDs= slice3(fillDs, idx);
                }
            } else if ( cmd.startsWith("|collapse") ) {
                int dim= cmd.charAt(9)-'0';
                fillDs= Ops.reduceMean(fillDs,dim);
            } else if ( cmd.equals("|autoHistogram") ) {
                fillDs= Ops.autoHistogram(fillDs);
            } else if ( cmd.equals("|transpose") ) {
                fillDs= Ops.transpose(fillDs);
            } else if ( cmd.startsWith("|fftWindow" ) ) {
                int size= s.nextInt();
                fillDs= Ops.fftWindow(fillDs, size);
            }
        }
        return fillDs;
    }

    /**
     * indicate if the operators change dimensions of the dataset.  Often
     * this will result in true when the dimensions do not change, this is the better way to err.
     * @param c process string like "slice0(0)"
     * @param c2 process string like "slice0(0)|slice1(0)"
     * @return
     */
    public static boolean changesDimensions( String c, String c2 ) {
        //if ( c.length()==0 && !c2.startsWith("|") ) return false;  //TODO: kludge to avoid true when adding component child.
        Scanner s= new Scanner( c );
        s.useDelimiter("[\\(\\),]");
        Scanner s2= new Scanner( c2 );
        s2.useDelimiter("[\\(\\),]");
        while ( s.hasNext() && s2.hasNext() ) {
            String cmd= s.next();
            if ( !s2.next().equals(cmd) ) return true;
            if ( cmd.startsWith("|slice") ) {
                s.nextInt();
                s2.nextInt();
            }
        }
        return s.hasNext() || s2.hasNext();
    }
}
