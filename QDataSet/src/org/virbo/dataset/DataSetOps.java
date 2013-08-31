/*
 * DataSetOps.java
 *
 * Created on January 29, 2007, 9:48 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.virbo.dataset;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.datum.Datum;
import org.das2.datum.Units;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.InputMismatchException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.regex.Pattern;
import org.das2.datum.DatumRange;
import org.das2.datum.DatumUtil;
import org.das2.datum.EnumerationUnits;
import org.das2.datum.TimeUtil;
import org.das2.datum.UnitsConverter;
import org.das2.datum.UnitsUtil;
import org.das2.util.LoggerManager;
import org.das2.util.monitor.NullProgressMonitor;
import org.das2.util.monitor.ProgressMonitor;
import org.virbo.dsops.Ops;
import org.virbo.dsops.Ops.FFTFilterType;
import org.virbo.dsutil.DataSetBuilder;
import org.virbo.dsutil.Reduction;

/**
 * Useful operations for QDataSets
 * @author jbf
 */
public class DataSetOps {

    private static final Logger logger= LoggerManager.getLogger("qdataset");

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
     * DO NOT try to optimize this by calling native trim, some native trim implementations call this.
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
        DataSetUtil.putProperties( DataSetUtil.getDimensionProperties(ds,null), zds );

        if ( dep1!=null && dep0!=null ) {
            DDataSet xds= xbuilder.getDataSet();
            DataSetUtil.putProperties( DataSetUtil.getDimensionProperties(dep0,null), xds );
            DDataSet yds= ybuilder.getDataSet();
            DataSetUtil.putProperties( DataSetUtil.getDimensionProperties(dep1,null), yds );
            return Ops.link( xds, yds, zds );
        } else  {
            return zds;
        }
        
    }

    /**
     * flatten a rank 2 dataset where the y depend variable is just an offset from the xtag.  This is
     * a nice example of the advantage of using a class to represent the data: this requires no additional
     * storage to handle the huge waveform.
     * @param ds
     * @return
     */
    public static QDataSet flattenWaveform( final QDataSet ds ) {
        final int n= ds.length(0);
        System.err.println("flattenWaveform v2.0");
        MutablePropertyDataSet result= new AbstractDataSet() {
            @Override
            public int rank() {
                return 1;
            }

            @Override
            public int length() {
                return n*ds.length();
            }


            @Override
            public double value(int i0) {
                return ds.value(i0/n,i0%n);
            }

            @Override
            public Object property(String name) {
                Object v= super.property(name);
                if ( v==null ) v= ds.property(name);
                if ( v==null ) {
                    if ( DataSetUtil.isInheritedProperty(name)) {
                        return ds.property(name);
                    } else {
                        return null;
                    }
                } else {
                    return v;
                }
            }

        };
        final QDataSet dsdep0 = (QDataSet) ds.property(QDataSet.DEPEND_0);
        final QDataSet dsdep1 = (QDataSet) ds.property(QDataSet.DEPEND_1);
        final UnitsConverter uc= UnitsConverter.getConverter( SemanticOps.getUnits(dsdep1), SemanticOps.getUnits(dsdep0).getOffsetUnits() );
        MutablePropertyDataSet dep0= new AbstractDataSet() {
            @Override
            public int rank() {
                return 1;
            }
            @Override
            public int length() {
                return n*ds.length();
            }
            @Override
            public double value(int i0) {
                return dsdep0.value(i0/n)+uc.convert(dsdep1.value(i0%n));
            }

            @Override
            public Object property(String name) {
                if ( name.equals(QDataSet.CADENCE) ) {
                    return dsdep1.property(QDataSet.CADENCE);
                } else {
                    if ( DataSetUtil.isInheritedProperty(name) ) {
                        return dsdep0.property(name);
                    } else {
                        return null;
                    }
                }
            }


        };
        result.putProperty( QDataSet.DEPEND_0, dep0 );
        return result;
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
        System.arraycopy(array, 0, result, 0, index);
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
                indeces[i0] = i;
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
     * @param min the min of the first bin.  If min=-1 and max=-1, then automatically set the min and max.
     * @param max the max of the last bin.
     * @param binsize the size of each bin.
     * @return a rank 1 dataset with each bin's count.  DEPEND_0 indicates the bin locations.
     */
    public static QDataSet histogram(QDataSet ds, double min, double max, final double binsize) {

        if ( min==-1 && max==-1 ) {
            QDataSet range= Ops.extent(ds);
            min= (Math.floor(range.value(0)/binsize)) * binsize;
            max= (Math.ceil(range.value(1)/binsize)) * binsize;
        }

        int n = (int) Math.ceil((max - min) / binsize);
        //TODO: half-bin offset needs to be corrected.  Correcting it by binsize/2  caused problems with autoranging and hudson.
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
        result.putProperty( QDataSet.RENDER_TYPE, "stairSteps" );  
        
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
        Object sbundle= props.get( QDataSet.BUNDLE_1 );
        QDataSet bundle1= ( sbundle instanceof QDataSet ) ? (QDataSet) sbundle : null; // kludge to handle where QStream reader hasn't resolved this.
        sbundle= props.get( QDataSet.BUNDLE_0 );
        QDataSet bundle0= ( sbundle instanceof QDataSet ) ? (QDataSet) sbundle : null;

        if ( dep0!=null && dep1!=null && dep0.rank()>1 && dep1.rank()>1 ) {
            throw new IllegalArgumentException("both DEPEND_0 and DEPEND_1 have rank>1");
        }

        if ( props.containsKey(QDataSet.CONTEXT_0) ) {
            for ( int i=0; i<QDataSet.MAX_RANK; i++ ) {
                QDataSet con= (QDataSet) props.get("CONTEXT_"+i);
                if ( con!=null ) {
                    result.put("CONTEXT_"+i, con);
                }
            }
        }
        if ( dep1!=null ) { //DEPEND_1 rank 1 implies qube
            if ( dep1.rank()==2 ) {
                result.put( QDataSet.DEPEND_0, dep1.slice( index ) );
            } else {
                result.put( QDataSet.DEPEND_0, dep1 );
            }
        }

        if ( dep0!=null && dep0.rank()==1 ) { //TODO: find documentation for rank 2 depend_0...
            DataSetUtil.addContext( result, dep0.slice( index ) );
        } else {
            if ( dep1==null && props.get( "DEPEND_0__"+index )==null ) { // bundle dataset  //TODO: this needs more review
                result.put( QDataSet.DEPEND_0, null );    // DANGER--uses indexed property convention.
            }
        }

        // UNITS__2_3=foo   property( UNITS, 2, 3 ) = foo
        for ( Map.Entry<String, Object> sse: props.entrySet() ) {
            String ss= sse.getKey();
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
                        result.put(slicePropName,sse.getValue());
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

        String[] p= DataSetUtil.correlativeProperties();
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
     * we've sliced a dataset, removing an index.  move the properties.  This was Ops.sliceProperties
     * @see MetadataUtil.sliceProperties
     * @param result
     * @param removeDim
     * @return
     */
    public static Map<String,Object> sliceProperties( Map<String,Object> properties, int sliceDimension ) {
        Map<String,Object> result = new LinkedHashMap();
        String[] ss= DataSetUtil.dimensionProperties();
        for ( String s: ss ) {
            Object val= properties.get(s);
            if ( val!=null ) result.put( s, val );
        }

        List<Object> deps = new ArrayList(QDataSet.MAX_RANK);
        List<Object> bund = new ArrayList(QDataSet.MAX_RANK);
        List<Object> bins = new ArrayList(QDataSet.MAX_RANK);
        for (int i = 0; i < QDataSet.MAX_RANK; i++) {
            deps.add(i, properties.get("DEPEND_" + i));
            bund.add(i, properties.get("BUNDLE_" + i));
            bins.add(i, properties.get("BINS_" + i));
        }

        deps.remove(sliceDimension);
        bund.remove(sliceDimension);
        bins.remove(sliceDimension);

        for (int i = 0; i < QDataSet.MAX_RANK-1; i++) {
            if ( deps.get(i)!=null ) result.put("DEPEND_" + i, deps.get(i));
            if ( bund.get(i)!=null ) result.put("BUNDLE_" + i, bund.get(i));
            if ( bins.get(i)!=null ) result.put("BINS_" + i, bins.get(i));
        }

        if ( properties.containsKey(QDataSet.CONTEXT_0) ) {
            for ( int i=0; i<QDataSet.MAX_RANK; i++ ) {
                QDataSet con= (QDataSet) properties.get("CONTEXT_"+i);
                if ( con!=null ) {
                    result.put("CONTEXT_"+i, con);
                }
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
     * @param name the name of the bundled dataset, or "ch_<i>" where i is the dataset number
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
            name= Ops.saferName(name);
        }

        if ( name.matches("ch_\\d+") ) {
            int ich= Integer.parseInt(name.substring(3) );
            return new Slice1DataSet( bundleDs, ich, true, true );
            //return DataSetOps.slice1( bundleDs, ich );
        }

        if ( bundle1==null ) {
            bundle1= (QDataSet) bundleDs.property(QDataSet.DEPEND_1); //simple legacy bundle was once DEPEND_1.
            if ( bundle1!=null && bundle1.rank()>1 ) {
                throw new IllegalArgumentException("high rank DEPEND_1 found where rank 1 was expected");
            } else if ( bundle1!=null ) {
                Units u= SemanticOps.getUnits( bundle1 );
                for ( int i2=0; i2<bundle1.length(); i2++ ) {
                    if ( name.equals( Ops.saferName( u.createDatum( bundle1.value(i2) ).toString() ) ) ) {
                        return unbundle( bundleDs, i2 );
                    }
                }
                throw new IllegalArgumentException("unable to find dataset with name \""+name+"\" in bundle "+bundleDs );
            } else {
                throw new IllegalArgumentException("expected to find BUNDLE_1 or DEPEND_1 with ordinal units." );
            }
        }

        boolean highRank= false;
        for ( int j=0; j<bundle1.length(); j++ ) {
            String n1= (String) bundle1.property( QDataSet.NAME, j );
            if ( n1!=null ) n1= Ops.saferName(n1);
            if ( n1!=null && n1.equals(name) ) {
                ib= j;
            }
            if ( bundle1.length(j)>0 ) {
                n1= (String) bundle1.property( QDataSet.ELEMENT_NAME, j );
                if ( n1!=null ) n1= Ops.saferName(n1);
                if ( n1!=null && n1.equals(name) ) {
                    ib= j;
                    highRank= true;
                    break;
                }
            }
        }

        for ( int j=0; j<bundle1.length(); j++ ) { // allow label to be used to access data, since old vap files may contain these.
            String n1= (String) bundle1.property( QDataSet.LABEL, j );
            if ( n1!=null ) n1= Ops.saferName(n1);
            if ( n1!=null && n1.equals(name) ) {
                ib= j;
            }
            if ( bundle1.length(j)>0 ) {
                n1= (String) bundle1.property( QDataSet.ELEMENT_LABEL, j );
                if ( n1!=null ) n1= Ops.saferName(n1);
                if ( n1!=null && n1.equals(name) ) {
                    ib= j;
                    highRank= true;
                    break;
                }
            }
        }

        if ( ib==-1 ) {
            if ( name.matches("ch_\\d+") ) {
                int ich= Integer.parseInt(name.substring(3) );
                if ( bundle1!=null ) {
                    return DataSetOps.unbundle(bundleDs, ich, false);
                } else {
                    return DataSetOps.slice1( bundleDs, ich );
                }
            } else {
                throw new IllegalArgumentException("unable to find dataset with name \""+name+"\" in bundle "+bundleDs );
            }
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
        if ( bundle1!=null ) {
            for ( int i=0; i<bundle1.length(); i++ ) {
                if ( bundle1.property(QDataSet.DEPEND_0,i)!=null ) ids=i;
                if ( bundle1.property(QDataSet.CONTEXT_0,i)!=null ) ids=i;
            }
            if ( ids==-1 ) ids= bundle1.length()-1;
        } else {
            ids= bundleDs.length(0)-1; // this would have been a NullPointerException before, thanks findbugs
        }
        
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
        return unbundle( bundleDs, ib, false );
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

        if ( bundleDs.rank()>=2 ) { // unbundle now allows rank >2 ...
            QDataSet bundle1= (QDataSet) bundleDs.property(QDataSet.BUNDLE_1);
            if ( bundle1==null ) {
                bundle1= (QDataSet) bundleDs.property(QDataSet.DEPEND_1); //simple legacy bundle was once DEPEND_1.
                if ( bundle1==null ) {
                    return new Slice1DataSet( bundleDs, ib ); //TODO: this was   throw new IllegalArgumentException( "Neither BUNDLE_1 nor DEPEND_1 found on dataset passed to unbundle command.");
                }
                if ( bundle1.rank()>1 ) {
                    throw new IllegalArgumentException("high rank DEPEND_1 found where rank 1 was expected");
                } else {
//                    Units u= SemanticOps.getUnits( bundle1 );
//                    if ( !( u instanceof EnumerationUnits ) ) {
//                        throw new IllegalArgumentException("dataset is not a bundle, and units of DEPEND_1 are not enumeration");
//                    }
                }
            }
            bundle= bundle1;
        } else if ( bundleDs.rank()==1 ) {
            QDataSet bundle0= (QDataSet) bundleDs.property(QDataSet.BUNDLE_0);
            if ( bundle0==null ) {
                bundle0= (QDataSet) bundleDs.property(QDataSet.DEPEND_0); //simple legacy bundle was once DEPEND_1.
                if ( bundle0==null ) {
                    return new Slice0DataSet( bundleDs, ib );
                }
                if ( bundle0.rank()>1 ) {
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
            throw new IndexOutOfBoundsException("in "+bundleDs+" no such data set at index="+ib +" bundle.length()="+bundle.length() );
        }

        if ( bundle.rank()==1 ) { //simple legacy bundle was once DEPEND_1.
            MutablePropertyDataSet result= bundleDs.rank()==2 ? DataSetOps.slice1(bundleDs,ib) : DataSetOps.slice0(bundleDs,ib);
            Units enumunits= (Units) bundle.property(QDataSet.UNITS);
            if ( enumunits==null ) enumunits= Units.dimensionless;
            String label=  String.valueOf(enumunits.createDatum(bundle.value(ib)));
            result.putProperty(QDataSet.NAME, Ops.safeName(label) ); //TODO: make safe java-identifier eg: org.virbo.dsops.Ops.safeName(label)
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
                QDataSet dep0= (QDataSet) bundleDs.property( QDataSet.DEPEND_0 );
                if ( dep0!=null && r.property(QDataSet.DEPEND_0)==null ) {
                    MutablePropertyDataSet rc= new DataSetWrapper(r);
                    rc.putProperty( QDataSet.DEPEND_0, dep0 );
                    return rc;
                } else {
                    if ( r.property(QDataSet.BUNDLE_1)!=null ) {
                        logger.warning("unbundled dataset still has BUNDLE_1");
                    }
                    return r;
                }

            } else {
                MutablePropertyDataSet result=null;
                // DataSetOps.slice1(bundleDs,offsets[j]); // this results in error message saying "we're not going to do this correctly, use unbundle instead", oops...
                if ( bundleDs.rank()==1 ) {
                    result= DataSetOps.makePropertiesMutable( bundleDs.slice(j) );
                } else if ( bundleDs.rank()>=2 ) {
                    result= new Slice1DataSet( bundleDs, j, true );
                } else {
                    throw new IllegalArgumentException("BundleDs must be rank 1 or rank 2"); // this is handled above and findbugs doesn't see that we can't get here.
                }

                String[] names1= DataSetUtil.dimensionProperties();
                for ( int i=0; i<names1.length; i++ ) {
                    Object v= bundle.property( names1[i], j );
                    if ( v!=null ) {
                        result.putProperty( names1[i], v );
                    }
                }
                // allow unindexed properties to define property for all bundled datasets, for example USER_PROPERTIES or FILL
                Map<String,Object> props3= DataSetUtil.getProperties(bundle, DataSetUtil.globalProperties(), null );
                for ( Map.Entry<String, Object> ss1: props3.entrySet() ) {
                    String ss= ss1.getKey();
                    Object vv= result.property( ss );
                    if ( vv==null ) {
                        result.putProperty( ss, ss1.getValue() );
                    }
                }

                if ( result.property(QDataSet.DEPEND_0)==null ) { // last make the default DEPEND_0 be the first column, if it is a UT time.
                    if ( ib>0 ) {
                        Units u= (Units) bundle.property(QDataSet.UNITS,0);
                        if ( u!=null && UnitsUtil.isTimeLocation(u) ) {
                            result.putProperty( QDataSet.DEPEND_0, unbundle(bundleDs,0,false) );
                        }
                    }
                }

                return result;
            }
        } else if ( bundle.length(j)==1 ) {
            if ( bundleDs.rank()==1 ) throw new IllegalArgumentException("not implemented for rank 0, slice is rank 1");
            TrimStrideWrapper result= new TrimStrideWrapper(bundleDs);
            result.setTrim( 1, is, is+len, 1 );
            Integer ifirst= (Integer) bundle.property( QDataSet.START_INDEX, j  );
            int first,last;
            if ( ifirst!=null ) {
                first= ifirst.intValue();
                last= first+len-1;
            } else {
                first= j; // I don't think this should happen, but...
                last= j;
            }
            Map<String,Object> props= DataSetUtil.getProperties( DataSetOps.slice0(bundle,first) );
            Map<String,Object> props2= DataSetUtil.getProperties( DataSetOps.slice0(bundle,last) );
            for ( Entry<String,Object> e: props2.entrySet() ) { // remove the properties that are not constant within the bundle by checking first against last.
                String ss= e.getKey();
                Object vv= props.get(ss);
                if ( vv!=null && !vv.equals( e.getValue() ) ) {
                    props.put(ss,null);
                }
            }
            if ( last!=first ) {
                props.put( QDataSet.BUNDLE_1, bundle.trim(first,last+1) );
            }

            if ( bundleDs.rank()>1 ) {
                if ( bundle.property(QDataSet.DEPEND_1,first)!=null && bundle.property(QDataSet.DEPEND_1,first)==bundle.property(QDataSet.DEPEND_1,last) ) {
                    props.put( QDataSet.DEPEND_1, bundle.property(QDataSet.DEPEND_1,first) );
                }
                if ( bundle.property(QDataSet.BINS_1,first)!=null && bundle.property(QDataSet.BINS_1,first).equals(bundle.property(QDataSet.BINS_1,last)) ) {
                    props.put( QDataSet.BINS_1, bundle.property(QDataSet.BINS_1,first) );
                    props.remove( QDataSet.BUNDLE_1 );
                }
                if ( bundle.property(QDataSet.BUNDLE_1,first)!=null  && bundle.property(QDataSet.BUNDLE_1,first)==(bundle.property(QDataSet.BUNDLE_1,last) ) ) {
                    props.put( QDataSet.BUNDLE_1, bundle.property(QDataSet.BUNDLE_1,first) );
                }
            }

            // allow unindexed properties to define property for all bundled datasets, for example USER_PROPERTIES or FILL
            Map<String,Object> props3= DataSetUtil.getProperties(bundle, DataSetUtil.globalProperties(), null );
            for ( String ss: props3.keySet() ) {
                Object vv= props.get( ss );
                if ( vv==null ) {
                    props.put( ss, props3.get(ss) );
                }
            }

            Object o;
            o= bundle.property(QDataSet.ELEMENT_NAME,j);
            if ( o!=null ) props.put( QDataSet.NAME, o );
            o= bundle.property(QDataSet.ELEMENT_LABEL,j);
            if ( o!=null ) props.put( QDataSet.LABEL, o );
            DataSetUtil.putProperties( props, result );
            String[] testProps= DataSetUtil.correlativeProperties();
            for ( int i=-1; i<testProps.length; i++ ) {
                String prop= ( i==-1 ) ? "DEPEND_0" : testProps[i];
                Object dep0= result.property(prop);
                if ( dep0!=null ) {
                    if ( dep0 instanceof String ) { //TODO: we can get rid of this.  DEPEND_0 must never be a string.
                        try {
                            QDataSet dep0ds= unbundle( bundleDs, (String)dep0 );
                            result.putProperty( prop, dep0ds );
                        } catch ( IllegalArgumentException ex ) {
                            throw new IllegalArgumentException("unable to find DEPEND_0 reference to \""+dep0+"\"");
                        }
                    }
                }
            }
            if ( result.property(QDataSet.DEPEND_0)==null ) { // last make the default DEPEND_0 be the first column, if it is a UT time.
                if ( ib>0 ) {
                    Units u= (Units) bundle.property(QDataSet.UNITS,0);
                    if ( u!=null && UnitsUtil.isTimeLocation(u) ) {
                        result.putProperty( QDataSet.DEPEND_0, unbundle(bundleDs,0,false) );
                    }
                }
            }
            return result;
        } else {
            throw new IllegalArgumentException("rank limit: >2 not supported");
        }

    }

    /**
     * given the bundle descriptor bds, return the dataset to be put in the context property.
     * @param bundle1 rank 2 bundle descriptor of length n with indexed properties.  This was introduced
     * when sloppy slice code was using the NAME and dropping the LABEL.
     * @param index 0<=index<n index of the unbundle
     * @return rank 0 QDataSet.
     */
    protected static QDataSet getContextForUnbundle( QDataSet bundle1, int index ) {
        String tname= (String) bundle1.property(QDataSet.NAME);
        if ( tname==null ) tname=(String) bundle1.property(QDataSet.NAME,index);
        String tlabel= (String) bundle1.property(QDataSet.LABEL,index);
        tname= String.valueOf(tname); // guard against null
        tlabel= String.valueOf(tlabel);
        MutablePropertyDataSet context= (MutablePropertyDataSet) ( Ops.labels( new String[] { tlabel } )).slice(0);
        if ( !Ops.safeName(tlabel).equals(tname) ) {
            context.putProperty( QDataSet.NAME, tname );
        }
        return context;
    }

    /**
     * returns the value from within a distribution that is the nth percentile division.  This
     * returns a fill dataset (Units.dimensionless.getFillDouble()) when the data is all fill.
     * @param ds
     * @param n
     * @return
     */
    public static QDataSet getNthPercentileSort( QDataSet ds, double n ) {

        if ( n<0 ) throw new IllegalArgumentException("n<0");
        if ( n>100 ) throw new IllegalArgumentException("n>=100");

        QDataSet sort= Ops.sort(ds);
        if ( sort.length()==0 ) {
            return DataSetUtil.asDataSet( Units.dimensionless.getFillDatum() );
        }
        
        int idx;
        if ( n==100 ) {
            idx= (int) sort.value( sort.length()-1 );
        } else {
            idx= (int) sort.value( (int)( sort.length() * n / 100 ) );
        }

        return ds.slice(idx);
    }


    /**
     * Get the background level by sorting the data.  The result is rank one less than the input rank.
     * @param ds
     * @param level
     * @return
     */
    public static QDataSet getBackgroundLevel( QDataSet ds, double level ) {
        if ( ds.rank()==1 ) {
            return getNthPercentileSort( ds, level );
        } else if ( ds.rank()==2 ) {
            DDataSet result= DDataSet.createRank1( ds.length(0) );
            result.putProperty( QDataSet.DEPEND_0, ds.property(QDataSet.DEPEND_1) );
            for ( int jj=0; jj<ds.length(0); jj++ ) {
                QDataSet b1= getNthPercentileSort( DataSetOps.slice1(ds,jj), level );
                result.putValue(jj, b1.value() );
            }
            result.putProperty(QDataSet.FILL_VALUE,Units.dimensionless.getFillDouble());
            return result;
        } else if ( ds.rank()>2 ) {
            JoinDataSet result= new JoinDataSet(ds.rank()-1);
            for ( int i=0; i<ds.length(); i++ ) {
                QDataSet ds1= ds.slice(i);
                QDataSet r1= getBackgroundLevel( ds1, level );
                result.join(r1);
            }
            return result;
        } else {
            throw new IllegalArgumentException("rank 0 dataset");
        }
    }


    /**
     * normalize the nth-level percentile from:
     *   rank 1: each element 
     *   rank 2: each row of the dataset
     *   rank 3: each row of each rank 2 dataset slice.
     * There must be at least 10 elements.  If the data is already in dB, then the result is a difference.
     * @param ds
     * @param level the percentile level, e.g. 10= 10%
     * @return the result dataset, in dB above background.
     */
    public static QDataSet dbAboveBackgroundDim1( QDataSet ds, double level ) {
    
        if ( ds.rank()<3 && ds.length()<10 ) {
            throw new IllegalArgumentException("not enough elements: "+ds);
        }

        MutablePropertyDataSet result;

        double fill= -1e31;
        boolean hasFill= false;
        if ( ds.rank()==1 ) {
            QDataSet back= getBackgroundLevel( ds, level );
            result= Ops.copy(ds);
            boolean db= ds.property(QDataSet.UNITS)==Units.dB;

            WritableDataSet wds= (WritableDataSet)result;
            QDataSet validDs= Ops.valid(back);
            QDataSet vds2= DataSetUtil.weightsDataSet(ds);
            if ( validDs.value()>0 ) {
                for ( int ii=0; ii<ds.length(); ii++ ) {
                    if ( vds2.value(ii)>0 ) {
                        double v= db ? ds.value(ii) - back.value() : 20 * Math.log10( ds.value(ii) / back.value() );
                        wds.putValue( ii,Math.max( 0,v ) );
                    } else {
                        wds.putValue( ii, fill );
                    }
                }
            } else {
                for ( int ii=0; ii<ds.length(); ii++ ) {
                    wds.putValue( ii, fill );
                }
                hasFill= true;
            }
            result.putProperty( QDataSet.USER_PROPERTIES,Collections.singletonMap("background", back) );

        } else if ( ds.rank()==2 ) {
            QDataSet back= getBackgroundLevel( ds, level );
            result= Ops.copy(ds);
            boolean db= ds.property(QDataSet.UNITS)==Units.dB;

            WritableDataSet wds= (WritableDataSet)result;
            QDataSet validDs= Ops.valid(back);
            QDataSet vds2= DataSetUtil.weightsDataSet(ds);
            for ( int jj=0; jj<ds.length(0); jj++ ) {
                for ( int ii=0; ii<ds.length(); ii++ ) {
                    if ( validDs.value(jj)>0 && vds2.value(ii,jj)>0 ) {
                        double v= db ? ds.value(ii,jj) - back.value(jj) : 20 * Math.log10( ds.value(ii,jj) / back.value(jj) );
                        wds.putValue( ii,jj, Math.max( 0,v ) );
                    } else {
                        wds.putValue( ii,jj, fill );
                        hasFill= true;
                    }
                }
            }
            result.putProperty( QDataSet.USER_PROPERTIES,Collections.singletonMap("background", back) );
            
        } else {
            JoinDataSet result1= new JoinDataSet(ds.rank());
            for ( int i=0; i<ds.length(); i++ ) {
                QDataSet ds1= ds.slice(i);
                QDataSet r1= dbAboveBackgroundDim1( ds1, level );
                result1.join(r1);
                if ( r1.property( QDataSet.FILL_VALUE )!=null ) {
                    hasFill= true;
                }
            }
            result= result1;
        }

        result.putProperty( QDataSet.UNITS, Units.dB );
        result.putProperty( QDataSet.TYPICAL_MIN, 0 );
        result.putProperty( QDataSet.TYPICAL_MAX, 120 );
        result.putProperty( QDataSet.SCALE_TYPE, "linear" );
        if ( hasFill ) result.putProperty( QDataSet.FILL_VALUE, fill );

        return result;
    }

    /**
     * normalize the level-th percentile from:
     *   rank 1: each element (same as removeBackground1)
     *   rank 2: each column of the dataset
     *   rank 3: each column of each rank 2 dataset slice.
     * There must be at least 10 elements.  If the data is already in dB, then the result is a difference.
     * @param ds
     * @param level the percentile level, e.g. 10= 10%
     * @return the result dataset, in dB above background.
     */
    public static QDataSet dbAboveBackgroundDim0( QDataSet ds, double level ) {

        MutablePropertyDataSet result;

        double fill= -1e31;
        boolean hasFill= false;

        if ( ds.rank()==1 ) {
            QDataSet back= getBackgroundLevel( ds, level );
            result= Ops.copy(ds);
            boolean db= ds.property(QDataSet.UNITS)==Units.dB;

            QDataSet validDs= Ops.valid(back);
            QDataSet vds2= DataSetUtil.weightsDataSet(ds);
            WritableDataSet wds= (WritableDataSet) result;
            if ( validDs.value()>0 ) {
                for ( int ii=0; ii<ds.length(); ii++ ) {
                    if ( vds2.value(ii)>0 ) {
                        double v= db ? wds.value(ii) - back.value() : 20 * Math.log10( wds.value(ii) / back.value() );
                        wds.putValue( ii,Math.max( 0,v ) );
                    } else {
                        wds.putValue( ii, fill );
                    }
                }
            } else {
                for ( int ii=0; ii<ds.length(); ii++ ) {
                    wds.putValue( ii, fill );
                }
                hasFill= true;
            }
            result.putProperty( QDataSet.USER_PROPERTIES,Collections.singletonMap("background", back) );
        } else if ( ds.rank()==2 ) {
            boolean db= ds.property(QDataSet.UNITS)==Units.dB;

            JoinDataSet result1= new JoinDataSet(ds.rank());
            for ( int ii=0; ii<ds.length(); ii++ ) {
                QDataSet ds1= ds.slice(ii);
                QDataSet back= getBackgroundLevel( ds1, level );
                QDataSet validDs= Ops.valid(back);
                QDataSet vds2= DataSetUtil.weightsDataSet(ds1);
                ds1= Ops.copy(ds1);
                WritableDataSet wds= (WritableDataSet) ds1;
                if ( validDs.value()>0 ) {
                    for ( int jj=0; jj<ds1.length(); jj++ ) {
                        if ( vds2.value(jj)>0 ) {
                            double v= db ? wds.value(jj) - back.value() : 20 * Math.log10( wds.value(jj) / back.value() );
                            wds.putValue( jj, Math.max( 0,v ) );
                        } else {
                            wds.putValue( jj, fill );
                        }
                    }
                } else {
                    for ( int jj=0; jj<ds1.length(); jj++ ) {
                        wds.putValue( jj, fill );
                    }
                    hasFill= true;
                }
                result1.join(wds);
            }
            result1.putProperty(QDataSet.DEPEND_0,ds.property(QDataSet.DEPEND_0));
            result= result1;
            
        } else {
            JoinDataSet result1= new JoinDataSet(ds.rank());
            for ( int i=0; i<ds.length(); i++ ) {
                QDataSet ds1= ds.slice(i);
                QDataSet r1= dbAboveBackgroundDim0( ds1, level );
                result1.join(r1);
                if ( r1.property( QDataSet.FILL_VALUE )!=null ) {
                    hasFill= true;
                }
            }
            result= result1;
        }

        result.putProperty( QDataSet.UNITS, Units.dB );
        result.putProperty( QDataSet.TYPICAL_MIN, 0 );
        result.putProperty( QDataSet.TYPICAL_MAX, 120 );
        result.putProperty( QDataSet.SCALE_TYPE, "linear" );
        if ( hasFill ) result.putProperty( QDataSet.FILL_VALUE, fill );

        return (MutablePropertyDataSet)result;
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
        return c.contains("copy") || c.contains("fft") || c.contains("contour") 
                || c.contains("dbAboveBackgroundDim") || c.contains("reducex")
                || c.contains("total") || c.contains("collapse");
    }

    /**
     * pop off the single or double quotes delimiting a string, if found.
     * @param s a string argument, possibly surrounded with quotes.
     * @return the string without the quotes.
     */
    private static String getStringArg( String s ) {
        String comp= s.trim();
        if ( comp.startsWith("'") && comp.endsWith("'") ) {
            comp= comp.substring(1,comp.length()-1);
        } else if ( comp.startsWith("\"") && comp.endsWith("\"") ) {
            comp= comp.substring(1,comp.length()-1);
        }
        return comp;
    }

    /**
     * apply process to the data.  This is like sprocess, except that the component can be extracted as the first step.
     * In general these can be done on the same thread (like
     * slice1), but some are slow (like fftPower).  This is a copy of PlotElementController.processDataSet.
     *
     * @param c
     * @param fillDs
     * @return
     * @throws RuntimeException
     * @throws Exception when the processStr cannot be processed.
     */
    public static QDataSet processDataSet( String c, QDataSet fillDs, ProgressMonitor mon ) throws RuntimeException, Exception {
        c= c.trim();
        if ( c.length()>0 && !c.startsWith("|") ) {  // grab the component, then apply processes after the pipe.
            if (!c.equals("") && fillDs.length() > 0 && fillDs.rank() == 2) {
                String[] labels = SemanticOps.getComponentNames(fillDs);
                String comp= c;
                int ip= comp.indexOf("|");
                if ( ip!=-1 ) {
                    comp= comp.substring(0,ip);
                }
                comp= Ops.saferName(comp);
                if ( fillDs.property(QDataSet.BUNDLE_1)!=null ) {
                    fillDs= DataSetOps.unbundle( fillDs,comp ); //TODO: illegal argument exception
                } else {
                    boolean found= false;
                    for (int i = 0; i < labels.length; i++) {
                        if ( Ops.saferName(labels[i]).equals(comp)) {
                            fillDs = DataSetOps.slice1(fillDs, i);
                            found= true;
                            break;
                        }
                    }
                    if ( !found ) {
                        throw new IllegalArgumentException("component not found: "+comp );
                    }
                }
            }
            int idx= c.indexOf("|");
            if ( idx==-1 ) {
                c="";
            } else {
                c= c.substring(idx);
            }
        }
        if (c.length() > 5 && c.startsWith("|")) {
            fillDs = DataSetOps.sprocess(c, fillDs, mon );
        }
        return fillDs;
    }

    /**
     * container for the logic for slicing at an index vs slicing at a datum.  If the string is
     * an integer, then we return the index.  If the index is a string, then we need to 
     * find the corresponding index.
     * @param dep
     * @param integer or rank 0 dataset.
     * @return 
     */
    public static Object getArgumentIndex( String arg ) {
        try {
            int idx= Integer.parseInt(arg);
            return idx;
        } catch ( NumberFormatException ex ) {
            arg= arg.trim();
            if ( arg.length()>2 && arg.startsWith("'") && arg.endsWith("'") ) {
                arg= arg.substring(1,arg.length()-1);
            }
            if ( arg.length()>2 && arg.startsWith("\"") && arg.endsWith("\"") ) {
                arg= arg.substring(1,arg.length()-1);
            }
            QDataSet ds= Ops.dataset( arg );
            return ds;
        }
    }

    /**
     * see http://www.papco.org/wiki/index.php/DataReductionSpecs.  There's a big problem here:
     * if the command is not recognized, then it is ignored.  We should probably change this,
     * but the change should be at a major version change in case it breaks things.
     * @param c
     * @param fillDs The dataset loaded from the data source controller, with initial filters (like fill) applied.
     * @throws ParseException when the string cannot be parsed
     * @throws Exception when a function cannot be processed (e.g. index out of bounds)
     * @return the dataset after the process string is applied.
     * @see http://autoplot.org/developer.dataset.filters
     * @see http://autoplot.org/developer.panel_rank_reduction
     * @see org.virbo.metatree.MetadataUtil.java which also handles the metadata (for now).
     */
    public static QDataSet sprocess( String c, QDataSet fillDs, ProgressMonitor mon ) throws Exception {

        logger.log(Level.FINE, "sprocess({0},{1})", new Object[] { c, fillDs } );

        if ( mon==null ) mon= new NullProgressMonitor();
        
        QDataSet ds0= fillDs;
        
        int i=1;
        Scanner s= new Scanner( c );
        s.useDelimiter("[\\(\\),]");

        String cmd="";
        try {
            mon.started();
            while ( s.hasNext() ) {
                cmd= s.next();
                cmd= cmd.replaceAll( "\\|\\s*", "|" ); // https://sourceforge.net/tracker/?func=detail&aid=3586477&group_id=199733&atid=970685
                i= c.indexOf(cmd,i);
                logger.log(Level.FINER, "  cmd \"{0}\"", cmd );

                if ( cmd.length()==0 ) continue;
                mon.setProgressMessage("performing "+cmd.substring(1));
                
                if ( logger.isLoggable(Level.FINEST) ) { // this has proved useful for debugging.
                    System.err.println( "---------------------" );
                    System.err.println( fillDs );
                    System.err.println( "dep0=" + fillDs.property(QDataSet.DEPEND_0) );
                    System.err.println( "bundle0=" + fillDs.property(QDataSet.BUNDLE_0) );
                    System.err.println( "dep1=" + fillDs.property(QDataSet.DEPEND_1) );
                    System.err.println( "bundle1=" + fillDs.property(QDataSet.BUNDLE_1) );
                    System.err.println( "  the next command is "+ cmd );
                }

                if ( cmd.startsWith("|slices") && cmd.length()==7 ) { // multi dimensional slice
                    int[] dims= DataSetUtil.qubeDims(fillDs);
                    Pattern skipPattern= Pattern.compile("\\'\\:?\\'");
                    List<Object> args= new ArrayList();
                    while ( s.hasNextInt() || s.hasNext( skipPattern ) ) {
                        if ( s.hasNextInt() ) {
                            args.add( s.nextInt() );
                        } else {
                            args.add( s.next() );
                        }
                    }
                    if ( dims!=null ) {
                        for ( int j=0; j<dims.length; j++ ) {
                            if ( args.get(j) instanceof Integer ) {
                                int dim= ((Integer)args.get(j) );
                                if ( dim<0 ) dim=0;
                                if ( dim>=dims[j] ) dim= dims[j]-1;
                                args.set(j,dim);
                            }
                        }
                    }
                    fillDs= Ops.slices( fillDs, args.toArray() );

                } else if(cmd.startsWith("|slice") && cmd.length()>6 ) {
                    int dim= cmd.charAt(6)-'0';
                    Object arg= getArgumentIndex( s.next() );
                    if ( arg instanceof Integer ) {
                        int idx= (Integer)arg;
                        if ( dim==0 ) {
                            if ( idx>=fillDs.length() ) idx=fillDs.length()-1;
                            if ( idx<0 ) idx=0;
                            //fillDs= slice0(fillDs, idx); //TODO: use fillDs.slice
                            fillDs= fillDs.slice(idx);
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
                    } else {
                        if ( dim==0 ) {
                            fillDs= Ops.slice0( fillDs, (QDataSet)arg );
                        } else {
                            throw new IllegalArgumentException("only slice0 works with strings");
                        }
                    }
                } else if ( cmd.equals("|reducex") ) {
                    String arg= getStringArg( s.next() );
                    try {
                        Datum r = DatumUtil.parse(arg);
                        fillDs= Reduction.reducex( fillDs, DataSetUtil.asDataSet(r) );
                    } catch (ParseException ex) {
                        logger.log(Level.SEVERE, null, ex);
                    }

                } else if ( cmd.equals("|diff") ) {
                    fillDs= Ops.diff(fillDs);
                } else if ( cmd.equals("|accum") ) {
                    fillDs= Ops.accum(fillDs);
                } else if ( cmd.equals("|log10") ) {
                    fillDs= Ops.log10(fillDs);
                } else if ( cmd.equals("|exp10") ) {
                    fillDs= Ops.exp10(fillDs);
                } else if ( cmd.equals("|trim") ) {
                    int d0= s.nextInt();
                    int d1= s.nextInt();
                    fillDs= fillDs.trim(d0,d1);
                } else if ( cmd.startsWith("|collapse") && cmd.length()>9 ) {
                    int dim= cmd.charAt(9)-'0';
                    if ( s.hasNextInt() ) {
                        if ( dim==0 ) {
                            int st= s.nextInt();
                            int en= s.nextInt();
                            fillDs= fillDs.trim(st,en);
                        } else {
                            throw new IllegalArgumentException("trim is only allowed with collapse0");
                        }
                    }
                    fillDs= Ops.reduceMean(fillDs,dim);
                } else if ( cmd.startsWith("|total") && cmd.length()>6 ) {
                    int dim= cmd.charAt(6)-'0';
                    if ( s.hasNextInt() ) {
                        if ( dim==0 ) {
                            int st= s.nextInt();
                            int en= s.nextInt();
                            fillDs= fillDs.trim(st,en);
                        } else {
                            throw new IllegalArgumentException("trim is only allowed with total0");
                        }
                    }
                    fillDs= Ops.total(fillDs,dim);
                } else if ( cmd.equals("|autoHistogram") ) {
                    fillDs= Ops.autoHistogram(fillDs);
                } else if ( cmd.equals("|histogram") ) { // 0=auto, 1=binsize
                    if ( s.hasNextDouble() ) {
                        double binSize= s.nextDouble();
                        if ( s.hasNextDouble() ) {
                            double min= binSize;
                            double max= s.nextDouble();
                            binSize= s.nextDouble();
                            fillDs= Ops.histogram(fillDs,min,max,binSize);
                        } else {
                            fillDs= Ops.histogram(fillDs,-1,-1,binSize);
                        }
                    } else {
                        fillDs= Ops.autoHistogram(fillDs);
                    }
                } else if ( cmd.equals("|extent") ) {
                    fillDs= Ops.extent(fillDs);
                } else if ( cmd.equals("|logHistogram") ) {
                    fillDs= Ops.autoHistogram(Ops.log10(fillDs));
                    MutablePropertyDataSet dep0= DDataSet.copy( (QDataSet) fillDs.property(QDataSet.DEPEND_0));
                    QDataSet cadence= (QDataSet) dep0.property( QDataSet.CADENCE );
                    dep0= (MutablePropertyDataSet) Ops.pow( Ops.replicate(10,dep0.length()), dep0 );
                    dep0.putProperty( QDataSet.SCALE_TYPE, "log" );
                    dep0.putProperty( QDataSet.CADENCE, cadence );
                    ((MutablePropertyDataSet)fillDs).putProperty( QDataSet.DEPEND_0, dep0 );
                } else if ( cmd.equals("|transpose") ) {
                    if ( fillDs.rank()==2 ) {
                        fillDs= Ops.transpose(fillDs);
                    } else {
                        System.err.println("unable to transpose dataset, not rank 2"); //TODO: error handling
                    }
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
                } else if ( cmd.equals("|abs") ) {
                    fillDs= Ops.abs(fillDs);
                } else if ( cmd.equals("|pow")) {
                    int idx= s.nextInt();
                    fillDs= Ops.pow(fillDs,idx);
                } else if ( cmd.equals("|total")) {
                    int idx= s.nextInt();
                    fillDs= Ops.total(fillDs, idx);
                } else if ( cmd.equals("|valid")) {
                    fillDs= Ops.valid(fillDs);
                } else if ( cmd.equals("|sqrt")) {
                    fillDs= Ops.sqrt(fillDs);
                } else if ( cmd.equals("|fftPower" ) ) {
                    if ( fillDs.length()>0 ) {
                        if ( s.hasNextInt() ) {
                            int len= s.nextInt();
                            if ( s.hasNextInt() ) {
                                int step= s.nextInt();
                                String window= getStringArg( s.next() );
                                if ( window.length()==0 ) window= "Unity";
                                FFTFilterType ff= Ops.FFTFilterType.valueOf(window);
                                QDataSet wqds= Ops.windowFunction( ff, len );
                                fillDs= Ops.fftPower( fillDs, wqds, step, mon);
                            } else {
                                fillDs= Ops.fftPower(fillDs,len, mon);
                            }
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
                    String comp= getStringArg( s.next() );
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
                } else if ( cmd.equals("|detrend") ) {
                    String comp= s.next();
                    int icomp= Integer.parseInt(comp);
                    fillDs= Ops.detrend(fillDs, icomp);
                } else if ( cmd.equals("|contour") ) {

                    List<Double> args= new ArrayList();

                    args.add( s.nextDouble() );
                    while ( s.hasNextDouble() ) {
                        args.add( s.nextDouble() );
                    }
                    double[] aa= new double[args.size()];
                    for ( int j=0; j<aa.length; j++ ) aa[j]= args.get(j).doubleValue();
                    fillDs= Ops.contour( fillDs, DataSetUtil.asDataSet( aa ) );

                } else if ( cmd.equals("|dbAboveBackgroundDim1") ) { // remove the background across slices
                    String qrg= s.next();
                    int iarg= Integer.parseInt(qrg.trim());
                    fillDs= DataSetOps.dbAboveBackgroundDim1( fillDs, iarg );

                } else if ( cmd.equals("|dbAboveBackgroundDim0") ) { // remove the background within slices
                    String qrg= s.next();
                    int iarg= Integer.parseInt(qrg.trim());
                    fillDs= DataSetOps.dbAboveBackgroundDim0( fillDs, iarg );

                } else if ( cmd.equals("|setUnits" ) ) {
                    String arg= getStringArg( s.next() );
                    Units newu= SemanticOps.lookupUnits(arg);
                    fillDs= ArrayDataSet.copy(fillDs).setUnits(newu);
                } else if ( cmd.equals("|add") ) { 
                    String arg= getStringArg( s.next() );
                    Datum d= SemanticOps.getUnits(fillDs).parse(arg);
                    fillDs= Ops.add( fillDs, DataSetUtil.asDataSet(d) );

                } else if ( cmd.equals("|multiply") ) { 
                    String arg= getStringArg( s.next() );
                    Datum d= DatumUtil.parse(arg);
                    fillDs= Ops.multiply( fillDs, DataSetUtil.asDataSet(d) );

                } else if ( cmd.equals("|divide") ) { 
                    String arg= getStringArg( s.next() );
                    Datum d= DatumUtil.parse(arg);
                    fillDs= Ops.divide( fillDs, DataSetUtil.asDataSet(d) );

                } else if ( cmd.equals("|nop") ) { // no operation, for testing.
                    //fillDs= fillDs;

                } else if ( cmd.equals("|copy") ) { // force a copy of the dataset.
                    //fillDs= fillDs;
                    fillDs= Ops.copy(fillDs);

                } else {
                    if ( !cmd.equals("") ) throw new ParseException( c + " (command not recognized: \""+cmd +"\")", i );
                }
            }
        } catch ( InputMismatchException ex ) {
            String msg= ex.getLocalizedMessage();
            if ( msg==null ) msg= ex.toString();
            ParseException ex2;
            if ( c.length()>cmd.length() ) {
                ex2= new ParseException( c + " at "+cmd+" ("+msg+")", i );
            } else {
                ex2= new ParseException( c + " ("+msg+")", i );
            }
            throw ex2;
        } finally {
            mon.finished();
        }
        logger.log(Level.FINE, "{0}->sprocess(\"{1}\")->{2}", new Object[] { ds0, c, fillDs } );
        return fillDs;
    }

    /**
     * indicate if the operators change dimensions of the dataset.  Often
     * this will result in true when the dimensions do not change, this is the better way to err.
     * @param c0 process string like "slice0(0)"
     * @param c1 process string like "slice0(0)|slice1(0)"
     * @return
     */
    public static boolean changesDimensions( String c0, String c1 ) {
        //if ( c.length()==0 && !c2.startsWith("|") ) return false;  //TODO: kludge to avoid true when adding component child.
        Scanner s0= new Scanner( c0 );
        s0.useDelimiter("[\\(\\),]");
        Scanner s1= new Scanner( c1 );
        s1.useDelimiter("[\\(\\),]");
        boolean slicesChangesDim= false;
        while ( s0.hasNext() && s1.hasNext() ) {
            String cmd0= s0.next();
            String cmd1= s1.next();
            if ( !cmd1.equals(cmd0) ) {
                return true;
            }
            if ( cmd0.startsWith("|slices") && cmd0.length()==7 ) { // multi dimensional slice
                Pattern skipPattern= Pattern.compile("\\'\\:?\\'");
                while ( s0.hasNextInt() || s0.hasNext( skipPattern ) ) {
                    if ( s0.hasNextInt() && s1.hasNextInt() ) {
                        s0.nextInt();
                        s1.nextInt();
                    } else if ( s0.hasNext( skipPattern ) && s1.hasNext( skipPattern ) ) {
                        s0.next();
                        s1.next();
                    } else {
                        slicesChangesDim= true;
                        s0.next();
                        s1.next();
                    }
                }
                if ( s0.hasNext() ) s0.next(); // ?? why TODO: verify this
            } else if (cmd0.startsWith("|slice") && cmd0.length() > 6) {
                s0.next();
                s1.next();
            } else if (cmd0.equals("|smooth") ) {
                s0.nextInt();
                s1.nextInt();
            } else if (cmd0.equals("|hanning") ) {
                s0.nextInt();
                s1.nextInt();
            } else if (cmd0.equals("|fftPower") ) {
                s0.nextInt();
                s1.nextInt();
                if ( s0.hasNextInt() && s1.hasNextInt() ) {
                    s0.nextInt();  // don't care if they change the slide
                    s1.nextInt();            
                }
                Pattern p= Pattern.compile("\'[a-zA-Z0-9_]*\'");
                if ( s0.hasNext(p) && s1.hasNext(p) ) { // don't care if they change the window
                    s0.next();
                    s1.next();    
                }
            }
        }
        boolean res= slicesChangesDim || s0.hasNext() || s1.hasNext();
        logger.log(Level.FINE, "  changesDimensions {0} , {1} ->{2}", new Object[]{c0, c1, res});
        return res;
    }

    /**
     * return a bounding qube of the independent dimensions containing
     * the dataset.  If r is the result of the function, then for
     *   rank 1: r.slice(0) x bounds, r.slice(1) y bounds
     *   rank 2 waveform: r.slice(0) x bounds, r.slice(1) y bounds
     *   rank 2 table:r.slice(0) x bounds  r.slice(1)  DEPEND_0 bounds.
     *   rank 3 table:r.slice(0) x bounds  r.slice(1)  DEPEND_0 bounds.
     * @param ds
     * @return
     */
    public static QDataSet dependBounds( QDataSet ds ) {
        QDataSet xrange;
        QDataSet yrange;

        if ( ds.rank()==1 ) {
            xrange= Ops.extent( SemanticOps.xtagsDataSet(ds) );
            yrange= Ops.extent( ds );
        } else if( ds.rank() == 2 ) {
            if ( SemanticOps.isRank2Waveform(ds) ) {
                xrange= Ops.extent( SemanticOps.xtagsDataSet(ds) );
                yrange= Ops.extent( ds );
            //} else if ( SemanticOps.isBundle(ds) ) { //bug: spectrogram rend of rbspb_pre_ect-mageisM75-sp-L1_20120908_v1.0.0.cdf?Count_Rate_SpinSetAvg
            //    xrange= Ops.extent( SemanticOps.xtagsDataSet(ds) );
            //    yrange= null;
            //    for ( int i=0; i<ds.length(0); i++ ) {
            //        yrange= Ops.extent( DataSetOps.unbundle( ds, i ), yrange );
            //    }
            } else {
                xrange= Ops.extent( SemanticOps.xtagsDataSet(ds) );
                yrange= Ops.extent( SemanticOps.ytagsDataSet(ds) );
            }
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

        QDataSet result= makePropertiesMutable( Ops.join( xrange, yrange ) );
        ((MutablePropertyDataSet)result).putProperty( QDataSet.BINS_1, QDataSet.VALUE_BINS_MIN_MAX );

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
