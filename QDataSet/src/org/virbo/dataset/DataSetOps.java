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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
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
        return new TrimDataSet( ds, offset, offset+len );
    }

    /**
     * flatten a rank 2 dataset.
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

        if ( dep1!=null && dep0!=null ) {
            return Ops.link( xbuilder.getDataSet(), ybuilder.getDataSet(), builder.getDataSet() );
        } else  {
            return builder.getDataSet();
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
     * @param sliceIndex
     * @param props
     */
    public static Map<String,Object> sliceProperties0( int index, Map<String,Object> props ) {
        Map<String,Object> result= new LinkedHashMap();

        QDataSet dep0= (QDataSet) props.get( QDataSet.DEPEND_0 );
        QDataSet dep1= (QDataSet) props.get( QDataSet.DEPEND_1 );
        QDataSet dep2= (QDataSet) props.get( QDataSet.DEPEND_2 );
        QDataSet dep3= (QDataSet) props.get( QDataSet.DEPEND_3 );
        QDataSet bins1= (QDataSet) props.get( QDataSet.BINS_1 );
        QDataSet bundle1= (QDataSet) props.get( QDataSet.BUNDLE_1 );

        if ( dep0!=null && dep1!=null && dep0.rank()>1 && dep1.rank()>1 ) {
            throw new IllegalArgumentException("both DEPEND_0 and DEPEND_1 have rank>1");
        }

        if ( dep1!=null ) { //DEPEND_1 rank 1 implies qube
            if ( dep0!=null ) DataSetUtil.addContext( result, dep0.slice( index ) );
            if ( dep1!=null && dep1.rank()==2 ) {
                result.put( QDataSet.DEPEND_0, dep1.slice( index ) );
            } else {
                result.put( QDataSet.DEPEND_0, dep1 );
            }
        } else {
            if ( dep0!=null && dep0.rank()==1 ) { //TODO: find documentation for rank 2 depend_0...
                if ( dep0!=null ) DataSetUtil.addContext( result, dep0.slice( index ) );
            } else {
                if ( props.get( "DEPEND_0["+index+"]" )==null ) { // bundle dataset  //TODO: this needs more review
                    result.put( QDataSet.DEPEND_0, null );    // DANGER--uses indexed property convention.
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

        String[] p= new String[] { QDataSet.DELTA_MINUS, QDataSet.DELTA_PLUS };

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
     * Extract the named bundled dataset.
     * @param bundleDs
     * @param name
     * @see unbundle( QDataSet bundleDs, int ib )
     * @return
     */
    public static QDataSet unbundle( QDataSet bundleDs, String name ) {
        QDataSet bundle1= (QDataSet) bundleDs.property(QDataSet.BUNDLE_1);
        int ib= -1;
        int i= name.indexOf("["); // allow name to be "Flux[Time=1440,en=10]"
        if ( i>0 ) {
            name= name.substring(i);
        }
        for ( int j=0; j<bundle1.length(); j++ ) {
            String n1= (String) bundle1.property( QDataSet.NAME, j );
            if ( n1!=null && n1.equals(name) ) {
                ib= j;
            }
        }
        if ( ib==-1 ) {
            throw new IllegalArgumentException("unable to find dataset with name \""+name+"\"");
        } else {
            return unbundle(bundleDs,ib);
        }
    }

    /**
     * Extract a bundled dataset from a bundle of datasets.  The input should
     * be a rank 2 dataset with the property BUNDLE_1 set to a bundle descriptor
     * dataset.  See BundleDataSet for more semantics.
     *
     * @param aThis
     * @param ib index of the dataset to extract.
     * @throws IndexOutOfBoundsException if the index is invalid.
     * @return
     */
    public static QDataSet unbundle(QDataSet bundleDs, int ib) {
        QDataSet bundle1= (QDataSet) bundleDs.property(QDataSet.BUNDLE_1);

        if ( bundle1==null ) {
            bundle1= (QDataSet) bundleDs.property(QDataSet.DEPEND_1); //simple legacy bundle was once DEPEND_1.
            if ( bundle1!=null && bundle1.rank()>1 ) {
                throw new IllegalArgumentException("high rank DEPEND_1 found where rank 1 was expected");
            }
        }

        if ( ib<0 || ib>=bundle1.length() ) {
            throw new IndexOutOfBoundsException("no such data set");
        }

        if ( bundle1.rank()==1 ) { //simple legacy bundle was once DEPEND_1.
            MutablePropertyDataSet result= DataSetOps.slice1(bundleDs,ib);
            Units enumunits= (Units) bundle1.property(QDataSet.UNITS);
            if ( enumunits==null ) enumunits= Units.dimensionless;
            String label=  String.valueOf(enumunits.createDatum(bundle1.value(ib)));
            result.putProperty(QDataSet.NAME, label ); //TODO: make safe java-identifier
            result.putProperty(QDataSet.LABEL, label );
            return result;
        }

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
            result.putProperty(QDataSet.UNITS, bundle1.property( QDataSet.UNITS, j ) ); //TODO: underimplementation
            result.putProperty(QDataSet.NAME, bundle1.property( QDataSet.NAME, j ) );
            result.putProperty(QDataSet.LABEL, bundle1.property( QDataSet.LABEL, j ) );
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
