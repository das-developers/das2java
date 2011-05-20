/*
 * DataSetOps.java
 *
 * Created on January 29, 2007, 9:48 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.virbo.dataset;

import java.util.ArrayList;
import org.das2.datum.Datum;
import org.das2.datum.Units;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import org.das2.datum.DatumRange;
import org.das2.datum.EnumerationUnits;
import org.das2.util.monitor.ProgressMonitor;
import org.virbo.dsops.Ops;
import org.virbo.dsutil.DataSetBuilder;

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
        return new Slice0DataSet(ds, index,true);
    }

    /**
     * slice dataset operator assumes a qube dataset
     * by picking the index-th element of dataset's second dimension, without
     * regard to tags.
     */
    public static MutablePropertyDataSet slice1(final QDataSet ds, final int index) {
        return new Slice1DataSet(ds, index, true, false);
    }

    /**
     * slice dataset operator assumes a qube dataset
     * by picking the index-th element of dataset's second dimension, without
     * regard to tags.
     */
    public static MutablePropertyDataSet slice2(final QDataSet ds, final int index) {
        return new Slice2DataSet(ds, index, true);
    }

    /**
     * slice dataset operator assumes a qube dataset
     * by picking the index-th element of dataset's second dimension, without
     * regard to tags.
     */
    public static MutablePropertyDataSet slice3(final QDataSet ds, final int index) {
        return new Slice3DataSet(ds, index, true );
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
        return new TrimDataSet( ds, offset, offset+len );
    }

    public static MutablePropertyDataSet trim( final QDataSet dep, final int start, final int stop, final int stride  ) {
        if ( dep.rank()!=1 ) throw new IllegalArgumentException("only rank 1 supported");
        QubeDataSetIterator itIn= new QubeDataSetIterator(dep);
        itIn.setIndexIteratorFactory( 0, new QubeDataSetIterator.StartStopStepIteratorFactory(start, stop, stride ) );
        DDataSet depSlice= itIn.createEmptyDs();
        QubeDataSetIterator itOut= new QubeDataSetIterator(depSlice);
        while ( itIn.hasNext() ) {
            itIn.next();
            itOut.next();
            itOut.putValue( depSlice, itIn.getValue(dep) );
        }
        String[] names = DataSetUtil.dimensionProperties();
        for (int i = 0; i < names.length; i++) {
            if ( dep.property(names[i]) != null) {
                depSlice.putProperty(names[i], dep.property(names[i]));
            }
        }
        return depSlice;
    }
    
    /**
     * flatten a rank 2 dataset.  The result is a n,3 dataset
     * of [x,y,z], or if there are no tags, just [z].
     * history:
     *    modified for use in PW group.
     * @param ds
     * @return
     */
    public static QDataSet flattenRank2( final QDataSet ds ) {
        QDataSet dep0= (QDataSet) ds.property(QDataSet.DEPEND_0);
        QDataSet dep1= (QDataSet) ds.property(QDataSet.DEPEND_1);
        DataSetBuilder builder= new DataSetBuilder( 1, 100 );
        DataSetBuilder xbuilder= new DataSetBuilder( 1, 100 );
        DataSetBuilder ybuilder= new DataSetBuilder( 1, 100 );
        for ( int i=0; i<ds.length(); i++ ) {
            for ( int j=0; j<ds.length(i); j++ ) {
                if (dep0!=null) {
                    xbuilder.putValue(-1, dep0.value(i) );
                    xbuilder.nextRecord();
                }
                if (dep1!=null) {
                    ybuilder.putValue(-1, dep1.value(j) );
                    ybuilder.nextRecord();
                }
                builder.putValue(-1, ds.value(i,j) );
                builder.nextRecord();
            }
        }

        DDataSet zds= builder.getDataSet();
        DataSetUtil.putProperties( DataSetUtil.getProperties(ds), zds );

        if ( dep1!=null && dep0!=null ) {
            DDataSet xds= xbuilder.getDataSet();
            DataSetUtil.putProperties( DataSetUtil.getProperties(dep0), xds );
            DDataSet yds= ybuilder.getDataSet();
            DataSetUtil.putProperties( DataSetUtil.getProperties(dep1), yds );
            return Ops.link( xds, yds, zds );
        } else  {
            return zds;
        }
        
    }

    /**
     * takes rank 2 link (x,y,z) and makes a table from it z(x,y)
     * @param ds
     * @return
     */
    public static QDataSet grid( final QDataSet ds ) {
        GridDataSet result= new GridDataSet();
        result.add( ds );
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
     * This is extended to support rank 4 datasets.
     * TODO: This probably doesn't handle bundles property.
     * TODO: slice and trim should probably be implemented here for efficiently.
     * @param start first index to include.
     * @param end last index, exclusive
     */
    public static MutablePropertyDataSet leafTrim(final QDataSet ds, final int start, final int end) {
        return new LeafTrimDataSet( ds, start, end );
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
     * TODO: this should probably (and would easily) be redone by using dataset implementation that applies the sort on the ith index when read.
     *   See SubsetDataSet which would do this nicely.
     * TODO: note the Jython stuff does this to, using a different implementation.  Reconcile these...
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
            return DDataSet.copy( new SortDataSet( ds, sort ) ); // this was presumably for efficiency
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
     * returns a rank 1 dataset that is a histogram of the data.  Note there
     * will also be in the properties:
     *   count, the total number of valid values.
     *   nonZeroMin, the smallest non-zero, positive number
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
        
        QubeDataSetIterator iter = new QubeDataSetIterator(ds);
        QDataSet wds= DataSetUtil.weightsDataSet(ds);

        double positiveMin= Double.MAX_VALUE;

        int count=0;
        for (; count<DS_LENGTH_LIMIT && iter.hasNext();) {
            iter.next();
            double d = iter.getValue(ds);
            double w = iter.getValue(wds);
            if ( w>0. ) {
                int ibin = (int) ((d - min) / binsize);
                if (ibin >= 0 && ibin < n) {
                    hits[ibin]++;
                }
                if ( d>0 && d<positiveMin ) positiveMin=d;
                count++;
            }
        }

        IDataSet result = IDataSet.wrap(hits);
        result.putProperty( QDataSet.DEPEND_0, tags );
        result.putProperty( "count", count );
        result.putProperty( "positiveMin", positiveMin );
        
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
     * method to help dataset implementations implement slice.
     * 2010-09-23: support rank 2 DEPEND_2 and DEPEND_3
     * 2010-09-23: add BINS_1 and BUNDLE_1, Slice0DataSet calls this.
     * 2010-02-24: BUNDLE_0 handled.
     * 2011-03-25: add WEIGHTS_PLANE
     * @param sliceIndex
     * @param props
     */
    public static Map<String,Object> sliceProperties0( int index, Map<String,Object> props ) {
        Map<String,Object> result= new LinkedHashMap();

        QDataSet dep0= (QDataSet) props.get( QDataSet.DEPEND_0 );
        QDataSet dep1= (QDataSet) props.get( QDataSet.DEPEND_1 );
        QDataSet dep2= (QDataSet) props.get( QDataSet.DEPEND_2 );
        QDataSet dep3= (QDataSet) props.get( QDataSet.DEPEND_3 );
        String bins1= (String) props.get( QDataSet.BINS_1 );
        QDataSet bundle1= (QDataSet) props.get( QDataSet.BUNDLE_1 );
        QDataSet bundle0= (QDataSet) props.get( QDataSet.BUNDLE_0 );

        if ( dep0!=null && dep1!=null && dep0.rank()>1 && dep1.rank()>1 ) {
            throw new IllegalArgumentException("both DEPEND_0 and DEPEND_1 have rank>1");
        }

        if ( dep1!=null ) { //DEPEND_1 rank 1 implies qube
            if ( dep1!=null && dep1.rank()==2 ) {
                result.put( QDataSet.DEPEND_0, dep1.slice( index ) );
            } else {
                result.put( QDataSet.DEPEND_0, dep1 );
            }
        }

        if ( dep0!=null && dep0.rank()==1 ) { //TODO: find documentation for rank 2 depend_0...
            if ( dep0!=null ) DataSetUtil.addContext( result, dep0.slice( index ) );
        } else {
            if ( dep1==null && props.get( "DEPEND_0__"+index )==null ) { // bundle dataset  //TODO: this needs more review
                result.put( QDataSet.DEPEND_0, null );    // DANGER--uses indexed property convention.
            }
        }

        // UNITS__2_3=foo   property( UNITS, 2, 3 ) = foo
        for ( String ss: props.keySet() ) {
            int ii= ss.indexOf("__");
            if ( ii>-1 ) {
                String hd= ss.substring(ii+2);
                int iii=0;
                while ( iii<hd.length() && Character.isDigit( hd.charAt(iii) ) ) iii++;
                if ( iii>0 ) {
                    int islice= Integer.parseInt( hd.substring(0,iii) );
                    if ( islice==index ) {
                        String slicePropName;
                        if ( iii<hd.length() ) {
                            slicePropName= ss.substring(0,ii)+"__"+ hd.substring(iii + 1 ); // +1 is for _ in _3
                        } else {
                            slicePropName= ss.substring(0,ii);
                        }
                        result.put(slicePropName,props.get(ss));
                    }
                }
            }
        }

        if ( dep2!=null ) {
            if ( dep2.rank()==2 ) {
                result.put( QDataSet.DEPEND_1, dep2.slice( index ) );
            } else {
                result.put( QDataSet.DEPEND_1, dep2 );
            }
        }

        if ( dep3!=null ) { 
            if ( dep3.rank()==2 ) {
                result.put( QDataSet.DEPEND_2, dep3.slice( index ) );
            } else {
                result.put( QDataSet.DEPEND_2, dep3 );
            }
        }

        if ( bins1!=null ) {
            result.put( QDataSet.BINS_0, bins1 );
        }

        if ( bundle1!=null ) {
            result.put( QDataSet.BUNDLE_0, bundle1 );
        }

        if ( bundle0!=null ) { //TODO: what if BUNDLE_0 bundles a high rank dataset?  This assumes they are all rank 0.
            QDataSet bundle0ds= bundle0.slice(index);
            result.putAll( DataSetUtil.getProperties(bundle0ds) );
        }

        //TODO: verify that we needn't put null in for JOIN_0.

        for ( int i=0; i<QDataSet.MAX_PLANE_COUNT; i++ ) {
            String prop= "PLANE_"+i;
            QDataSet plane= (QDataSet) props.get( prop );
            if ( plane!=null ) {
                if ( plane.rank()<1 ) {
                    result.put( prop, plane );
                } else {
                    result.put( prop, plane.slice(index) );
                }
            } else {
                break;
            }
        }

        String[] p= new String[] { QDataSet.DELTA_MINUS, QDataSet.DELTA_PLUS, QDataSet.WEIGHTS_PLANE };

        for ( int i=0; i<p.length; i++ ) {
            QDataSet delta= (QDataSet) props.get( p[i] );
            if ( delta!=null && delta.rank()>0 ) {
                result.put( p[i], delta.slice(index) );
            }
        }

        String[] ss= DataSetUtil.dimensionProperties();
        for ( int i=0; i<ss.length; i++ ) {
            Object o= props.get( ss[i] );
            if ( o!=null ) {
                result.put( ss[i], o );
            }
        }
        return result;
    }

    /**
     * returns a bundle descriptor roughly equivalent to the BundleDescriptor
     * passed in, but will describe each dataset as if it were rank 1.  This
     * is useful for when the client can't work with mixed rank bundles anyway
     * (like display data).
     * @param bundle1
     * @return
     */
    public static QDataSet flattenBundleDescriptor( QDataSet bundle1 ) {

        int nr1= 0;
        final List<String> names= new ArrayList();
        final List<Units> units= new ArrayList();

        for ( int j=0; j<bundle1.length(); j++ ) {

            int rank= bundle1.length(j);
            int n=1;

            for (int k = 0; k < rank; k++) {
                 n *= bundle1.value(j, k);
            }
            nr1+= n;

            String name= (String) bundle1.property(QDataSet.NAME,j);
            Units unit= (Units) bundle1.property(QDataSet.UNITS,j);
            String bins= (String) bundle1.property(QDataSet.BINS_1,j);

            for ( int i=0; i<n; i++ ) {
                String binName= null;
                if ( bins!=null ) {
                    String[] ss= bins.split(",",-2);
                    binName= ss[i];
                }
                String theName= name;
                if ( theName!=null && binName!=null ) {
                    theName= theName + "_"+ binName;
                }
                if ( theName!=null ) names.add( theName ); else names.add("");
                if ( unit!=null ) units.add( unit ); else units.add(Units.dimensionless);

            }
        }
        final int fnr1= nr1;

        QDataSet bundleDescriptor= new AbstractDataSet() {
            @Override
            public int rank() {
                return 2;
            }

            @Override
            public int length() {
                return fnr1;
            }
            @Override
            public int length(int i) {
                return 0;
            }
            @Override
            public Object property(String name, int i) {
                if ( i>names.size() ) throw new IllegalArgumentException("index too large:"+i );
                if ( name.equals( QDataSet.NAME ) ) {
                    return names.get(i);
                } else if ( name.equals( QDataSet.UNITS ) ) {
                    return units.get(i);
                } else {
                    return null;
                }
            }
        };

        return bundleDescriptor;
    }

    /**
     * Extract the named bundled dataset.  For example, extract B_x from bundle of components.
     * @param bundleDs
     * @param name
     * @see unbundle( QDataSet bundleDs, int ib )
     * @throws IllegalArgumentException if no named dataset is found.
     * @return
     */
    public static QDataSet unbundle( QDataSet bundleDs, String name ) {
        QDataSet bundle1= (QDataSet) bundleDs.property(QDataSet.BUNDLE_1);

        int ib= -1;
        int i= name.indexOf("["); // allow name to be "Flux[Time=1440,en=10]"
        if ( i>0 ) {
            name= name.substring(i);
        }

        if ( bundle1==null ) {
            bundle1= (QDataSet) bundleDs.property(QDataSet.DEPEND_1); //simple legacy bundle was once DEPEND_1.
            if ( bundle1!=null && bundle1.rank()>1 ) {
                throw new IllegalArgumentException("high rank DEPEND_1 found where rank 1 was expected");
            } else if ( bundle1!=null ) {
                Units u= (Units) bundle1.property(QDataSet.UNITS);
                for ( int i2=0; i2<bundle1.length(); i2++ ) {
                    if ( name.equals( u.createDatum( bundle1.value(i2) ).toString() ) ) {
                        return unbundle( bundleDs, i2 );
                    }
                }
                throw new IllegalArgumentException("unable to find dataset with name \""+name+"\" in bundle "+bundleDs );
            } else if ( bundle1==null && name.matches("ch_\\d+") ) {
                int ich= Integer.parseInt(name.substring(3) );
                return DataSetOps.slice1( bundleDs, ich );
            }
        }

        if ( bundle1==null ) {
            throw new IllegalArgumentException("unbundle called but no bundle dataset found in BUNDLE_1 or DEPEND_1");
        }

        boolean highRank= false;
        for ( int j=0; j<bundle1.length(); j++ ) {
            String n1= (String) bundle1.property( QDataSet.NAME, j );
            if ( n1!=null && n1.equals(name) ) {
                ib= j;
            }
            if ( bundle1.length(j)>0 ) {
                n1= (String) bundle1.property( QDataSet.ELEMENT_NAME, j );
                if ( n1!=null && n1.equals(name) ) {
                    ib= j;
                    highRank= true;
                    break;
                }
            }
        }
        if ( ib==-1 ) {
            throw new IllegalArgumentException("unable to find dataset with name \""+name+"\" in bundle "+bundleDs );
        } else {
            return unbundle(bundleDs,ib,highRank);
        }
    }

    /**
     * extract the dataset that is dependent on others, or the last one.  
     * For example, the dataset ds[:,"x,y"] -> y[:]
     * @param bundleDs
     * @return
     */
    public static QDataSet unbundleDefaultDataSet( QDataSet bundleDs ) {
        QDataSet bundle1= (QDataSet) bundleDs.property(QDataSet.BUNDLE_1);

        if ( bundle1==null ) {
            bundle1= (QDataSet) bundleDs.property(QDataSet.DEPEND_1); //simple legacy bundle was once DEPEND_1.
            if ( bundle1!=null && bundle1.rank()>1 ) {
                throw new IllegalArgumentException("high rank DEPEND_1 found where rank 1 was expected");
            }
        }

        int ids= -1;
        for ( int i=0; i<bundle1.length(); i++ ) {
            if ( bundle1.property(QDataSet.DEPEND_0,i)!=null ) ids=i;
            if ( bundle1.property(QDataSet.CONTEXT_0,i)!=null ) ids=i;
        }
        if ( ids==-1 ) ids= bundle1.length()-1;
        return unbundle(bundleDs,ids);
    }

    /**
     * Extract a bundled dataset from a bundle of datasets.  The input should
     * be a rank 2 dataset with the property BUNDLE_1 set to a bundle descriptor
     * dataset.  See BundleDataSet for more semantics.  Note we support the case
     * where DEPEND_1 has EnumerationUnits, and this is the same as slice1.
     *
     *
     * @param aThis
     * @param ib index of the dataset to extract. If the index is within a dataset,
     *   then the entire dataset is returned.
     * @throws IndexOutOfBoundsException if the index is invalid.
     * @throws IllegalArgumentException if the dataset is not a bundle dataset, with either BUNDLE_1 or DEPEND_1 set.
     * @return
     */
    public static QDataSet unbundle(QDataSet bundleDs, int ib) {
        return unbundle( bundleDs, ib, true );
    }

    /**
     * Extract a bundled dataset from a bundle of datasets.  The input should
     * be a rank 2 dataset with the property BUNDLE_1 set to a bundle descriptor
     * dataset.  See BundleDataSet for more semantics.  Note we support the case
     * where DEPEND_1 has EnumerationUnits, and this is the same as slice1.
     *
     *
     * @param aThis
     * @param ib index of the dataset to extract. If the index is within a dataset,
     *   then the entire dataset is returned.
     * @param highRank if true, then if the dataset at ib is rank 2 or greater, then
     *   then the entire dataset is returned.  If false, only the slice of the dataset is
     *   returned.
     * @throws IndexOutOfBoundsException if the index is invalid.
     * @throws IllegalArgumentException if the dataset is not a bundle dataset, with either BUNDLE_1 or DEPEND_1 set.
     * @return
     */
    public static QDataSet unbundle(QDataSet bundleDs, int ib, boolean highRank ) {
        
        QDataSet bundle=null;

        if ( bundleDs.rank()==2 ) {
            QDataSet bundle1= (QDataSet) bundleDs.property(QDataSet.BUNDLE_1);
            if ( bundle1==null ) {
                bundle1= (QDataSet) bundleDs.property(QDataSet.DEPEND_1); //simple legacy bundle was once DEPEND_1.
                if ( bundle1==null ) {
                    throw new IllegalArgumentException( "Neither BUNDLE_1 nor DEPEND_1 found on dataset passed to unbundle command.");
                }
                if ( bundle1!=null && bundle1.rank()>1 ) {
                    throw new IllegalArgumentException("high rank DEPEND_1 found where rank 1 was expected");
                } else {
                    Units u= SemanticOps.getUnits( bundle1 );
                    if ( !( u instanceof EnumerationUnits ) ) {
                        throw new IllegalArgumentException("dataset is not a bundle, and units of DEPEND_1 are not enumeration");
                    }
                }
            }
            bundle= bundle1;
        } else if ( bundleDs.rank()==1 ) {
            QDataSet bundle0= (QDataSet) bundleDs.property(QDataSet.BUNDLE_0);
            if ( bundle0==null ) {
                bundle0= (QDataSet) bundleDs.property(QDataSet.DEPEND_0); //simple legacy bundle was once DEPEND_1.
                if ( bundle0==null ) {
                    throw new IllegalArgumentException( "Neither BUNDLE_0 nor DEPEND_0 found on dataset passed to unbundle command.");
                }
                if ( bundle0!=null && bundle0.rank()>1 ) {
                    throw new IllegalArgumentException("high rank DEPEND_0 found where rank 1 was expected");
                } else {
                    Units u= SemanticOps.getUnits( bundle0 );
                    if ( !( u instanceof EnumerationUnits ) ) {
                        throw new IllegalArgumentException("dataset is not a bundle, and units of DEPEND_0 are not enumeration");
                    }
                }
            }
            bundle= bundle0;
        } else {
            throw new IllegalArgumentException("bundle must be rank 1 or rank 2");
        }


        if ( ib<0 || ib>=bundle.length() ) {
            throw new IndexOutOfBoundsException("no such data set");
        }

        if ( bundle.rank()==1 ) { //simple legacy bundle was once DEPEND_1.
            MutablePropertyDataSet result= bundleDs.rank()==2 ? DataSetOps.slice1(bundleDs,ib) : DataSetOps.slice0(bundleDs,ib);
            Units enumunits= (Units) bundle.property(QDataSet.UNITS);
            if ( enumunits==null ) enumunits= Units.dimensionless;
            String label=  String.valueOf(enumunits.createDatum(bundle.value(ib)));
            result.putProperty(QDataSet.NAME, label ); //TODO: make safe java-identifier eg: org.virbo.dsops.Ops.safeName(label)
            result.putProperty(QDataSet.LABEL, label );
            return result;
        } else if ( bundle.rank()==2 ) {

        } else {
            throw new IllegalArgumentException("rank limit: >2 not supported");
        }

        int len=1;  // total number of elements per record of the dataset
        int j=ib;   // column requested
        int is= ib; // start index of the high-rank dataset

        if ( highRank ) {
            Integer s= (Integer)bundle.property(QDataSet.START_INDEX,ib);
            if ( s==null ) s= ib;
            is= s.intValue();
            int n=1;
            for (int k = 0; k < bundle.length(is); k++) {
                 n *= bundle.value(is, k);
            }
            len= n;
            j= ib;
        }
        
        if ( bundle.length(j)==0 || !highRank ) {
            if ( bundleDs instanceof BundleDataSet ) {
                QDataSet r= ((BundleDataSet)bundleDs).unbundle(j);
                return r;
            } else {
                MutablePropertyDataSet result=null;
                // DataSetOps.slice1(bundleDs,offsets[j]); // this results in error message saying "we're not going to do this correctly, use unbundle instead", oops...
                if ( bundleDs.rank()==1 ) {
                    result= DataSetOps.makePropertiesMutable( bundleDs.slice(j) );
                } else if ( bundleDs.rank()==2 ) {
                    result= new Slice1DataSet( bundleDs, j, true );
                }

                String[] names1= DataSetUtil.dimensionProperties();
                for ( int i=0; i<names1.length; i++ ) {
                    Object v= bundle.property( names1[i], j );
                    if ( v!=null ) {
                        result.putProperty( names1[i], v );
                    }
                }
                return result;
            }
        } else if ( bundle.length(j)==1 ) {
            if ( bundleDs.rank()==1 ) throw new IllegalArgumentException("not implemented for rank 0, slice is rank 1");
            TrimStrideWrapper result= new TrimStrideWrapper(bundleDs);
            result.setTrim( 1, is, is+len, 1 );
            Map<String,Object> props= DataSetUtil.getProperties( DataSetOps.slice0(bundle,j) );
            Object o;
            o= bundle.property(QDataSet.ELEMENT_NAME,j);
            if ( o!=null ) props.put( QDataSet.NAME, o );
            o= bundle.property(QDataSet.ELEMENT_LABEL,j);
            if ( o!=null ) props.put( QDataSet.LABEL, o );
            DataSetUtil.putProperties( props, result );
            String[] testProps= new String[] { QDataSet.DEPEND_0, QDataSet.DELTA_MINUS, QDataSet.DELTA_PLUS, QDataSet.PLANE_0 };
            for ( int i=0; i<testProps.length; i++ ) {
                String prop= testProps[i];
                Object dep0= result.property(prop);
                if ( dep0!=null ) {
                    if ( dep0 instanceof String ) {
                        try {
                            QDataSet dep0ds= unbundle( bundleDs, (String)dep0 );
                            result.putProperty( prop, dep0ds );
                        } catch ( IllegalArgumentException ex ) {
                            throw new IllegalArgumentException("unable to find DEPEND_0 reference to \""+dep0+"\"");
                        }
                    }
                }
            }
            return result;
        } else {
            throw new IllegalArgumentException("rank limit: >2 not supported");
        }

    }

    /**
     * return true if the process described in c is probably a slow
     * process that should be done asynchronously.  For example, do
     * a long fft on a different thread and use a progress monitor.  Processes
     * that take a trivial, constant amount of time should return false, and
     * may be completed on the event thread,etc.
     * 
     * @param c, process string, as in sprocess.
     * @return
     */
    public static boolean isProcessAsync(String c) {
        return c.contains("fft");
    }

    /**
     * see http://www.papco.org/wiki/index.php/DataReductionSpecs
     * @param c
     * @param fillDs
     * @return
     */
    public static QDataSet sprocess( String c, QDataSet fillDs , ProgressMonitor mon ) {
        int i=1;
        Scanner s= new Scanner( c );
        s.useDelimiter("[\\(\\),]");

        while ( s.hasNext() ) {
            String cmd= s.next();
            if ( cmd.startsWith("|slice") ) {
                if  ( cmd.length()<=6 ) {
                    throw new IllegalArgumentException("need DIM on sliceDIM(INDEX)");
                }
                int dim= cmd.charAt(6)-'0';
                int idx= s.nextInt();
                if ( dim==0 ) {
                    if ( idx>=fillDs.length() ) idx=fillDs.length()-1;
                    if ( idx<0 ) idx=0;
                    fillDs= slice0(fillDs, idx); //TODO: use fillDs.slice
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
            } else if ( cmd.equals("|nop") ) {
                //fillDs= fillDs;
            } else if ( cmd.equals("|diff") ) {
                fillDs= Ops.diff(fillDs);
            } else if ( cmd.equals("|accum") ) {
                fillDs= Ops.accum(fillDs);
            } else if ( cmd.equals("|log10") ) {
                fillDs= Ops.log10(fillDs);
            } else if ( cmd.equals("|exp10") ) {
                fillDs= Ops.exp10(fillDs);
            } else if ( cmd.startsWith("|collapse") ) {
                int dim= cmd.charAt(9)-'0';
                fillDs= Ops.reduceMean(fillDs,dim);
            } else if ( cmd.equals("|autoHistogram") ) {
                fillDs= Ops.autoHistogram(fillDs);
            } else if ( cmd.equals("|histogram") ) {
                fillDs= Ops.autoHistogram(fillDs);
            } else if ( cmd.equals("|logHistogram") ) {
                fillDs= Ops.autoHistogram(Ops.log10(fillDs));
                MutablePropertyDataSet dep0= DDataSet.copy( (QDataSet) fillDs.property(QDataSet.DEPEND_0));
                QDataSet cadence= (QDataSet) dep0.property( QDataSet.CADENCE );
                dep0= (MutablePropertyDataSet) Ops.pow( Ops.replicate(10,dep0.length()), dep0 );
                dep0.putProperty( QDataSet.SCALE_TYPE, "log" );
                dep0.putProperty( QDataSet.CADENCE, cadence );
                ((MutablePropertyDataSet)fillDs).putProperty( QDataSet.DEPEND_0, dep0 );
            } else if ( cmd.equals("|transpose") ) {
                fillDs= Ops.transpose(fillDs);
            } else if ( cmd.startsWith("|fftWindow" ) ) {
                int size= s.nextInt();
                fillDs= Ops.fftWindow(fillDs, size);
            } else if ( cmd.equals("|flatten" ) ) {
                if ( fillDs.rank()!=2 ) throw new IllegalArgumentException("only rank2 supported");
                fillDs= flattenRank2(fillDs);
            } else if ( cmd.equals("|grid" ) ) {
                if ( fillDs.rank()!=2 ) throw new IllegalArgumentException("only rank2 supported");
                fillDs= grid(fillDs);
            } else if ( cmd.equals("|magnitude") ) {
                fillDs= Ops.magnitude(fillDs);
            } else if ( cmd.equals("|pow")) {
                int idx= s.nextInt();
                fillDs= Ops.pow(fillDs,idx);
            } else if ( cmd.equals("|total")) {
                int idx= s.nextInt();
                fillDs= Ops.total(fillDs, idx);
            } else if ( cmd.equals("|sqrt")) {
                fillDs= Ops.sqrt(fillDs);
            } else if ( cmd.equals("|fftPower" ) ) {
                if ( fillDs.length()>0 ) {
                    if ( s.hasNextInt() ) {
                        int len= s.nextInt();
                        fillDs= Ops.fftPower(fillDs,len, mon);
                    } else {
                        fillDs= Ops.fftPower(fillDs);
                    }
                } else {
                    fillDs= Ops.fftPower(fillDs);
                }
            } else if ( cmd.equals("|hanning") ) {
                if ( fillDs.length()>0 ) {
                    if ( s.hasNextInt() ) {
                        int len= s.nextInt();
                        fillDs= Ops.fftFilter( fillDs, len, Ops.FFTFilterType.Hanning );
                    } else {
                        throw new IllegalArgumentException("expected argument to hanning filter");
                    }
                }

            } else if ( cmd.equals("|unbundle" ) ) {
                String comp= s.next();
                if ( comp.startsWith("'") && comp.endsWith("'") ) {
                    comp= comp.substring(1,comp.length()-1);
                }
                try {
                    int icomp= Integer.parseInt(comp);
                    fillDs= DataSetOps.unbundle( fillDs, icomp );
                } catch ( NumberFormatException ex ) {
                    fillDs= DataSetOps.unbundle( fillDs, comp );
                }
            } else if ( cmd.equals("|negate") ) {
                fillDs= Ops.negate(fillDs);
            } else if ( cmd.equals("|cos") ) {
                fillDs= Ops.cos(fillDs);
            } else if ( cmd.equals("|sin") ) {
                fillDs= Ops.sin(fillDs);
            } else if ( cmd.equals("|toRadians") ) {
                fillDs= Ops.toRadians(fillDs);
            } else if ( cmd.equals("|toDegrees") ) {
                fillDs= Ops.toDegrees(fillDs);
            } else if ( cmd.equals("|smooth") ) {
                String comp= s.next();
                int icomp= Integer.parseInt(comp);
                fillDs= Ops.smooth(fillDs, icomp);
            } else {
                if ( !cmd.equals("") ) System.err.println( "command not recognized: \""+cmd +"\"" );
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

    /**
     * return a bounding qube of the independent dimensions containing
     * the dataset.
     * Only for tables right now.
     * @param ds
     * @return
     */
    public static QDataSet dependBounds( QDataSet ds ) {
        QDataSet xrange;
        QDataSet yrange;

        if( ds.rank() == 2) {
            xrange= Ops.extent( SemanticOps.xtagsDataSet(ds) );
            yrange= Ops.extent( SemanticOps.ytagsDataSet(ds) );
        } else if ( ds.rank()==3 ) {
            QDataSet ds1= ds.slice(0);
            xrange= Ops.extent( SemanticOps.xtagsDataSet(ds1) );
            yrange= Ops.extent( SemanticOps.ytagsDataSet(ds1) );
            for ( int i=1; i<ds.length(); i++ ) {
                ds1= ds.slice(i);
                xrange= Ops.extent( SemanticOps.xtagsDataSet(ds1), xrange );
                yrange= Ops.extent( SemanticOps.ytagsDataSet(ds1), yrange );
            }
        } else {
            throw new IllegalArgumentException("bad rank");
        }

        QDataSet result= Ops.join( xrange, yrange );

        return result;
    }

    public static boolean boundsContains(QDataSet bounds, Datum xValue, Datum yValue) {
        if ( bounds.property(QDataSet.BINS_1)==null ) {
            if ( bounds.property(QDataSet.BINS_0,0)==null ) {
                throw new IllegalArgumentException("expected BINS_1");
            }
        }
        DatumRange xrange= DataSetUtil.asDatumRange( bounds.slice(0), true );
        DatumRange yrange= DataSetUtil.asDatumRange( bounds.slice(1), true );
        return xrange.contains(xValue) && yrange.contains(yValue);
    }
    
}
