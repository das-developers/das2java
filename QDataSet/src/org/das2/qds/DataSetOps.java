/*
 * DataSetOps.java
 *
 * Created on January 29, 2007, 9:48 AM
 */
package org.das2.qds;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.datum.Datum;
import org.das2.datum.Units;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.regex.Pattern;
import org.das2.qds.buffer.BufferDataSet;
import org.das2.datum.DatumRange;
import org.das2.datum.EnumerationUnits;
import org.das2.datum.UnitsUtil;
import org.das2.util.LoggerManager;
import org.das2.util.monitor.ProgressMonitor;
import org.das2.qds.examples.Schemes;
import org.das2.qds.ops.Ops;
import org.das2.qds.util.DataSetBuilder;

/**
 * Useful operations for QDataSets, such as slice2, leafTrim.
 * TODO: identify which functions appear here instead of Ops.java.
 * @author jbf
 */
public class DataSetOps {

    private static final Logger logger= LoggerManager.getLogger("qdataset.ops");
    
    /**
     * absolute length limit for plots.  This is used to limit the elements used in autoranging, etc.
     */
    public final static int DS_LENGTH_LIMIT= 10000000;
    
    /**
     * return a dataset that has mutable properties.  If the dataset parameter already has, then the 
     * dataset is returned.  If the dataset is a MutablePropertyDataSet but the immutable flag is
     * set, then the dataset is wrapped to make the properties mutable.  
     * @param dataset dataset
     * @return a MutablePropertyDataSet that is has a wrapper around the dataset, or the dataset.
     * @see DataSetWrapper
     */
    public static MutablePropertyDataSet makePropertiesMutable( final QDataSet dataset ) {
        if ( dataset instanceof MutablePropertyDataSet ) {
            MutablePropertyDataSet mpds= (MutablePropertyDataSet) dataset;
            if ( mpds.isImmutable() ) {
                return new DataSetWrapper(dataset);
            } else {
                return (MutablePropertyDataSet) dataset;
            }
        } else {
            return new DataSetWrapper(dataset);
        }
    }

    /**
     * return a dataset that is writable.  If the dataset parameter of this idempotent
     * function is already writable, then the 
     * dataset is returned.  If the dataset is a WritableDataSet but the immutable flag is
     * set, then the a copy is returned.
     * @param dataset
     * @return a WritableDataSet that is either a copy of the read-only dataset provided, or the parameter writable dataset provided.
     */
    public static WritableDataSet makeWritable(QDataSet dataset) {
        if ( dataset instanceof WritableDataSet ) {
            WritableDataSet wds= (WritableDataSet) dataset;
            if ( wds.isImmutable() ) {
                return ArrayDataSet.copy(dataset);
            } else {
                return (WritableDataSet) dataset;
            }
        } else {
            return ArrayDataSet.copy(dataset);
        }
    }

    /**
     * slice on the first dimension.  Note the function ds.slice(index) was
     * added later and will typically be more efficient.  
     * 
     * TODO: This actually needs a bit more study, because there are codes that
     * talk about not using the native slice because it copies data and they just
     * want metadata.  This probably is because Slice0DataSet doesn't check for 
     * immutability, and really should be copying.  This needs to be fixed, 
     * making sure the result of this call is immutable, and the native slice
     * really should be more efficient, always.
     * 
     * @param ds rank 1 or more dataset
     * @param index the index to slice at
     * @return rank 0 or more dataset.
     * @see QDataSet#slice(int) 
     */
    public static MutablePropertyDataSet slice0(final QDataSet ds, final int index) {
        return new Slice0DataSet(ds, index,true);
    }
    
    /**
     * slice dataset operator assumes a qube dataset
     * by picking the index-th element of dataset's second dimension, without
     * regard to tags.
     * @param ds rank 2 or more dataset
     * @param index the index to slice at
     * @return rank 1 or more dataset.
     */
    public static MutablePropertyDataSet slice1(final QDataSet ds, final int index) {
        return new Slice1DataSet(ds, index, true, false);
    }

    /**
     * slice dataset operator assumes a qube dataset
     * by picking the index-th element of dataset's second dimension, without
     * regard to tags.
     * @param ds rank 3 or more dataset
     * @param index the index to slice at.
     * @return rank 2 or more dataset.
     */
    public static MutablePropertyDataSet slice2(final QDataSet ds, final int index) {
        return new Slice2DataSet(ds, index, true);
    }

    /**
     * slice dataset operator assumes a qube dataset
     * by picking the index-th element of dataset's second dimension, without
     * regard to tags.
     * @param ds rank 4 or more dataset.
     * @param index index to slice at
     * @return rank 3 or more dataset.
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
     * @param ds the dataset
     * @param offset the offset
     * @param len the length, (not the stop index!)
     * @return trimmed dataset
     */
    public static MutablePropertyDataSet trim(final QDataSet ds, final int offset, final int len) {
        return new TrimDataSet( ds, offset, offset+len );
    }

    /**
     * reduce the number of elements in the dataset to the dim 0 indeces specified.
     * This does not change the rank of the dataset.
     * @param dep the dataset.
     * @param start first index to include
     * @param stop last index, exclusive
     * @param stride the step size, e.g. 2 is every other element.
     * @return trimmed dataset
     */
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
        for (String name : names) {
            if (dep.property(name) != null) {
                depSlice.putProperty(name, dep.property(name));
            }
        }
        return depSlice;
    }
    
    /**
     * flatten a rank 2 dataset.  The result is a n,3 dataset
     * of [x,y,f].
     * History:<ul>
     *   <li> modified for use in PW group.
     *   <li> missing DEPEND_1 resulted in NullPointerException, so just use 0,1,2,..,n instead and always have rank 2 result.
     * </ul>
     * @param ds rank 2 table dataset
     * @return rank 2 dataset that is that is array of (x,y,f).
     */
    public static QDataSet flattenRank2( final QDataSet ds ) {
        QDataSet dep0= (QDataSet) ds.property(QDataSet.DEPEND_0);
        QDataSet dep1= (QDataSet) ds.property(QDataSet.DEPEND_1);
        QDataSet dep0offset=  (QDataSet) ds.property("OFFSETS_1"); //kludge to experiment with this.
        if ( dep0==null ) dep0= Ops.findgen(ds.length(0));
        if ( dep1==null ) dep1= Ops.findgen(ds.length(0));
        DataSetBuilder builder= new DataSetBuilder( 1, 100 );
        DataSetBuilder xbuilder= new DataSetBuilder( 1, 100 );
        DataSetBuilder ybuilder= new DataSetBuilder( 1, 100 );
        if ( dep1.rank()==2 && Schemes.isRank2Bins(dep1) ) {
            dep1= Ops.reduceBins( dep1 );
        }
        boolean dep1rank2= dep1!=null && dep1.rank()==2;
        for ( int i=0; i<ds.length(); i++ ) {
            for ( int j=0; j<ds.length(i); j++ ) {
                if (dep0!=null) {
                    if ( dep0offset!=null ) {
                        xbuilder.putValue(-1, Ops.add( dep0.slice(i), dep0offset.slice(j) ) );
                    } else {
                        xbuilder.putValue(-1, dep0.value(i) );
                    }
                    xbuilder.nextRecord();
                }
                if (dep1!=null) {
                    ybuilder.putValue(-1, dep1rank2 ? dep1.value(i,j) : dep1.value(j) );
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
     * flatten a rank 3 dataset.  The result is a n,4 dataset
     * of [x,y,z,f], or if there are no tags just rank 1 f.
     * @param ds rank 3 table dataset
     * @return rank 2 dataset that is array of (x,y,z,f) or rank 1 f.
     */
    public static QDataSet flattenRank3( final QDataSet ds ) {
        QDataSet dep0= (QDataSet) ds.property(QDataSet.DEPEND_0);
        QDataSet dep1= (QDataSet) ds.property(QDataSet.DEPEND_1);
        QDataSet dep2= (QDataSet) ds.property(QDataSet.DEPEND_2);
        DataSetBuilder builder= new DataSetBuilder( 1, 100 );
        DataSetBuilder xbuilder= new DataSetBuilder( 1, 100 );
        DataSetBuilder ybuilder= new DataSetBuilder( 1, 100 );
        DataSetBuilder zbuilder= new DataSetBuilder( 1, 100 );
        boolean dep1rank2= dep1!=null && dep1.rank()==2;
        boolean dep2rank2= dep2!=null && dep2.rank()==2;
        for ( int i=0; i<ds.length(); i++ ) {
            for ( int j=0; j<ds.length(i); j++ ) {
                for ( int k=0; k<ds.length(i,j); k++ ) {
                    if (dep0!=null) {
                        xbuilder.nextRecord( dep0.value(i) );
                    }
                    if (dep1!=null) {
                        ybuilder.nextRecord( dep1rank2 ? dep1.value(i,j) : dep1.value(j) );
                    }
                    if (dep2!=null) {
                        zbuilder.nextRecord( dep2rank2 ? dep2.value(i,k) : dep2.value(k) );
                    }
                    builder.nextRecord( ds.value(i,j,k) );
                }
            }
        }

        DDataSet fds= builder.getDataSet();
        DataSetUtil.putProperties( DataSetUtil.getDimensionProperties(ds,null), fds );

        if ( dep1!=null && dep0!=null ) {
            DDataSet xds= xbuilder.getDataSet();
            DataSetUtil.putProperties( DataSetUtil.getDimensionProperties(dep0,null), xds );
            DDataSet yds= ybuilder.getDataSet();
            DataSetUtil.putProperties( DataSetUtil.getDimensionProperties(dep1,null), yds );
            DDataSet zds= zbuilder.getDataSet();
            DataSetUtil.putProperties( DataSetUtil.getDimensionProperties(dep2,null), zds );
            return Ops.link( xds, yds, zds, fds );
        } else  {
            return fds;
        }
        
    }
    
    /**
     * flatten a rank 2 dataset where the y depend variable is just an offset from the xtag.  This is
     * a nice example of the advantage of using a class to represent the data: this requires no additional
     * storage to handle the huge waveform.  Note the new DEPEND_0 may have different units from ds.property(DEPEND_0).
     * @param ds rank 2 waveform with tags for DEPEND_0 and offsets for DEPEND_1
     * @return rank 1 waveform
     */
    public static QDataSet flattenWaveform( final QDataSet ds ) {
        QDataSet dep1= (QDataSet) ds.property( QDataSet.DEPEND_1 );
        if ( dep1.rank()==1 ) {
            return new FlattenWaveformDataSet(ds);
        } else {
            QDataSet dep0= (QDataSet) ds.property(QDataSet.DEPEND_0);
            QDataSet result= null;
            for ( int i=0; i<ds.length(); i++ ) {
                QDataSet ds1= ds.slice(i);
                QDataSet xtags= (QDataSet)ds1.property(QDataSet.DEPEND_0);
                xtags= Ops.add( dep0.slice(i), xtags );
                result= Ops.append( result, Ops.link( xtags, ds1 ) );
            }
            return result;
        }
    }

    /**
     * takes rank 2 link (x,y,z) and makes a table from it z(x,y)
     * @param ds rank 2 link (x,y,z)
     * @return a table from it z(x,y)
     */
    public static QDataSet grid( final QDataSet ds ) {
        GridDataSet result= new GridDataSet();
        result.add( ds );
        return result;
    }

    /**
     * removes the index-th element from the array.
     * @param array length N array
     * @param index the index to remove
     * @return array without the element, length N-1.
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
     * row with length&gt;end.
     * This is extended to support rank 4 datasets.
     * TODO: This probably doesn't handle bundles property.
     * TODO: slice and trim should probably be implemented here for efficiently.
     * @param ds rank 1 or more dataset
     * @param start first index to include.
     * @param end last index, exclusive
     * @return dataset of the same rank.
     */
    public static MutablePropertyDataSet leafTrim(final QDataSet ds, final int start, final int end) {
        return new LeafTrimDataSet( ds, start, end );
    }

    /**
     * returns a list of indeces that sort the dataset.  I don't like this implementation, because
     * it requires that an array of Integers (not int[]) be created.  Invalid measurements are not indexed in
     * the returned dataset.
     * If the sort is monotonic, then the property MONOTONIC will be Boolean.TRUE.
     * @param ds rank 1 dataset, possibly containing fill.
     * @return indeces that sort the data.
     */
    public static QDataSet sort(final QDataSet ds) {
        logger.entering( "org.das2.qds.DataSetOps","sort",ds);
        if (ds.rank() != 1) {
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
        final Comparator<Integer> c = new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                int i1 = o1;
                int i2 = o2;
                return Double.compare(ds.value(i1), ds.value(i2));
            }
        };
        Arrays.sort(indeces, 0, i0, c);
        final int[] data = new int[i0];
        
        boolean monotonic= true;
        int lastData=0;
        if ( i0>0 ) {
            data[0] = indeces[0];
            lastData= data[0];
        }
        for (int i = 1; i < i0; i++) {
            data[i] = indeces[i];
            if ( monotonic && data[i]<lastData ) monotonic=false;
            lastData= data[i];
        }
        
        MutablePropertyDataSet result = IDataSet.wrap(data);
        result.putProperty(QDataSet.NAME, "sort" + ds.length());
        if ( monotonic ) result.putProperty( QDataSet.MONOTONIC, Boolean.TRUE );
        logger.exiting( "org.das2.qds.DataSetOps","sort",ds);
        return result;
    }

    /**
     * apply the sort to the data on the zeroth dimension.  The dataset
     * must be mutable, and the dataset itself is modified.  This was introduced
     * to support AggregatingDataSource but should be generally useful.
     * @param ds a writable dataset that is still mutable.
     * @param sort the new sort indeces.
     */
    public static void applyIndexInSitu( WritableDataSet ds, QDataSet sort ) {
        logger.entering("org.virbo.dataset","applyIndexInSitu");
        if ( ds.isImmutable() ) throw new IllegalArgumentException("ds is immutable: "+ds);
        WritableDataSet ssort= Ops.copy(sort);
        if ( ds.rank()==1 ) {
            for ( int i=0; i<ssort.length(); i++ ) {
                int j= (int)ssort.value(i);
                if ( j!=i ) {
                    double d= ds.value(i);
                    ds.putValue( i, ds.value( j ) );
                    ds.putValue( j, d );
                    ssort.putValue( j, j );
                }
            }
        } else if ( ds.rank()==2 ) {
            for ( int i=0; i<ssort.length(); i++ ) {
                int j= (int)ssort.value(i);
                if ( j!=i ) {
                    for ( int i2=0; i2<ds.length(0); i2++ ) {
                        double d= ds.value(i,i2);
                        ds.putValue( i, i2, ds.value(j,i2) );
                        ds.putValue( j, i2, d );    
                    }
                    ssort.putValue( j, j );
                }
            }
        } else if ( ds.rank()==3 ) {
            for ( int i=0; i<ssort.length(); i++ ) {
                int j= (int)ssort.value(i);
                if ( j!=i ) {
                    for ( int i2=0; i2<ds.length(0); i2++ ) {
                        for ( int i3=0; i3<ds.length(0); i3++ ) {
                            double d= ds.value(i,i2,i3);
                            ds.putValue( i, i2, i3, ds.value(j,i2,i3) );
                            ds.putValue( j, i2, i3, d ); 
                        }
                    }
                    ssort.putValue( j, j );
                }
            }            
        } else if ( ds.rank()==4 ) {
            for ( int i=0; i<ssort.length(); i++ ) {
                int j= (int)ssort.value(i);
                if ( j!=i ) {
                    for ( int i2=0; i2<ds.length(0); i2++ ) {
                        for ( int i3=0; i3<ds.length(0); i3++ ) {
                            for ( int i4=0; i4<ds.length(0); i4++ ) {
                                double d= ds.value(i,i2,i3,i4);
                                ds.putValue( i, i2, i3, i4, ds.value(j,i2,i3,i4) );
                                ds.putValue( j, i2, i3, i4, d ); 
                            }
                        }
                    }
                    ssort.putValue( j, j );
                }
            }
        }
        QDataSet dep0= (QDataSet)ds.property(QDataSet.DEPEND_0);
        if ( dep0!=null ) {
            if ( dep0 instanceof WritableDataSet ) {
                applyIndexInSitu( (WritableDataSet)dep0, sort );
            } else {
                throw new IllegalArgumentException("dep0 should be mutable");
            }
        }
        QDataSet m= (QDataSet)ds.property(QDataSet.BIN_PLUS);
        if ( m!=null ) {
            if ( m instanceof WritableDataSet ) {
                applyIndexInSitu( (WritableDataSet)m, sort );
            } else {
                throw new IllegalArgumentException("bin_plus should be mutable");
            }
        }
        m= (QDataSet)ds.property(QDataSet.BIN_MINUS);
        if ( m!=null ) {
            if ( m instanceof WritableDataSet ) {
                applyIndexInSitu( (WritableDataSet)m, sort );
            } else {
                throw new IllegalArgumentException("bin_plus should be mutable");
            }
        }
        m= (QDataSet)ds.property(QDataSet.BINS_0);
        if ( m!=null ) {
            if ( m instanceof WritableDataSet ) {
                applyIndexInSitu( (WritableDataSet)m, sort );
            } else {
                throw new IllegalArgumentException("bin_plus should be mutable");
            }
        }
        for ( int i=1; i<ds.rank(); i++ ) {
            QDataSet dep= (QDataSet)ds.property("DEPEND_"+i);
            if ( dep!=null && dep.rank()==2 ) {
                if ( dep instanceof WritableDataSet ) {
                    applyIndexInSitu( (WritableDataSet)dep, sort );
                } else {
                    throw new IllegalArgumentException("dep0 should be mutable");
                }
            }
        }
        logger.exiting("org.virbo.dataset","applyIndexInSitu");
    }
    
    /**
     * return the class type that can accurately store data in this 
     * dataset.  This was motivated by DDataSets and FDataSets, but also
     * IndexGenDataSets.
     * 
     * @param ds the dataset.
     * @return the class that can store this type.  double.class is returned when the class cannot be identified.
     * @see ArrayDataSet#create(java.lang.Class, int[]) 
     */
    public static Class getComponentType( QDataSet ds ) {
        if ( ds instanceof ArrayDataSet ) {
            return ((ArrayDataSet)ds).getComponentType();
        } else if ( ds instanceof BufferDataSet ) {
            return ((BufferDataSet)ds).getCompatibleComponentType();
        } else if ( ds instanceof IndexGenDataSet ) {
            return int.class;
        } else if ( ds instanceof JoinDataSet && ds.length()>0 ) {
            return getComponentType(ds.slice(0));
        } else {
            return double.class;
        }
    }
    
    /**
     * return a fill value that is representable by the type.
     * @param c the class type, including double.class, float.class, etc.
     * @return a fill value that is representable by the type.
     */
    public static double suggestFillForComponentType( Class c ) {
        if ( c==double.class ) {
            return -1e38;
        } else if ( c==float.class ) {
            return -1e38;
        } else if ( c==long.class ) {
            return Long.MIN_VALUE;
        } else if ( c==int.class ) {
            return Integer.MIN_VALUE;
        } else if ( c==short.class ) {
            return Short.MIN_VALUE;
        } else if ( c==byte.class ) {
            return Byte.MIN_VALUE;
        } else {
            return -1e38;
        }
    }
    
    /**
     * return the dataset with records rearranged according to indices.
     * @param ds rank N dataset, where N>0
     * @param indices rank 1 dataset, length m.
     * @return length m rank N dataset.
     * @see #applyIndex(org.das2.qds.QDataSet, int, org.das2.qds.QDataSet, boolean) 
     */
    public static QDataSet applyIndex( QDataSet ds, QDataSet indices ) {
        return DataSetOps.applyIndex( ds, 0, indices, true );
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
     * @see  org.das2.qds.SortDataSet for similar functionality
     * @see Ops#decimate(org.das2.qds.QDataSet, int, int) 
     */
    public static WritableDataSet applyIndex( QDataSet ds, int idim, QDataSet sort, boolean deps ) {

        if (idim > 2) {
            throw new IllegalArgumentException("idim must be <=2 ");
        }
                
        if ( idim==0 ) {
            return ArrayDataSet.copy( getComponentType(ds), new SortDataSet( ds, sort ) );
        }

        if (ds.rank() > 3) {
            throw new IllegalArgumentException("rank limit");
        }

        int[] qube = DataSetUtil.qubeDims( ds );
        if ( qube==null ) throw new IllegalArgumentException("dataset is not a qube and index is not on first dimension");
        
        qube[idim] = sort.length();
        
        ArrayDataSet cds= ArrayDataSet.create( getComponentType(ds), qube );
        
        Map<String,Object> props= org.das2.qds.DataSetUtil.getDimensionProperties(ds,null);
        org.das2.qds.DataSetUtil.putProperties(props, cds);
        
        if (deps) {
            String depprop = "DEPEND_" + idim;

            QDataSet depds = (QDataSet) ds.property(depprop);
            if (depds != null) {
                depds = applyIndex(depds, 0, sort, false);
                cds.putProperty(depprop, depds);
            }
            
            String bundleprop= "BUNDLE_"+idim;
            QDataSet bds= (QDataSet) ds.property( bundleprop );
            if ( bds!=null ) {
                JoinDataSet jds= new JoinDataSet(2);
                for ( int i=0; i<sort.length(); i++ ) {
                    jds.join( bds.slice((int)(sort.value(i))) );
                }
                cds.putProperty( bundleprop, jds );
            }
        }

        if (idim == 1) {
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
        
        MutablePropertyDataSet tags = DataSetUtil.tagGenDataSet(n, min + binsize/2 , binsize, (Units)ds.property(QDataSet.UNITS) );
        
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
                int ibin = (int) Math.floor((d - min) / binsize);
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
     * @param index the index to slice at in the zeroth index.
     * @param props the properties to slice.
     * @return the properties after the slice.
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

        for ( int i=0; i<QDataSet.MAX_PLANE_COUNT; i++ ) {
            String prop= "PLANE_"+i;
            Object o= props.get(prop);
            if ( o!=null ) {
                if ( o instanceof QDataSet ) {
                    QDataSet plane= (QDataSet) o;
                    if ( plane.rank()<1 ) {
                        result.put( prop, plane );
                    } else {
                        result.put( prop, plane.slice(index) );
                    }
                } else {
                    break;
                }
            } else {
                break;
            }
        }

        String[] p= DataSetUtil.correlativeProperties(); // DELTA_PLUS, BIN_MINUS, etc.
        for (String p1 : p) {
            Object o= props.get(p1);
            if ( o!=null ) {
                if ( o instanceof QDataSet ) {
                    QDataSet d = (QDataSet) o;
                    if ( d.rank()>0) {
                        result.put(p1, d.slice(index));
                    }
                } else {
                    logger.log(Level.INFO, "property is not a QDataSet: {0}", p1);
                }
            }
        }

        String[] dimprops= DataSetUtil.dimensionProperties(); // TITLE, UNITS, etc.
        for (String s : dimprops ) {
            Object o = props.get(s);
            
            if (o!=null) {
                result.put(s, o);
            }
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
            if ( dep1.rank()==2 && !Schemes.isRank2Bins(dep1)) {
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

        return result;
    }

    /**
     * we've sliced a dataset, removing an index.  move the properties.  This was Ops.sliceProperties
     * For example, after slicing the zeroth dimension (time), what was DEPEND_1 is
     * becomes DEPEND_0.
     * 
     * @param properties the properties to slice.
     * @param sliceDimension the dimension to slice at (0,1,2...QDataSet.MAX_HIGH_RANK)
     * @return the properties after the slice.
     */
    public static Map<String,Object> sliceProperties( Map<String,Object> properties, int sliceDimension ) {
        Map<String,Object> result = new LinkedHashMap();
        String[] ss= DataSetUtil.dimensionProperties();
        for ( String s: ss ) {
            Object val= properties.get(s);
            if ( val!=null ) result.put( s, val );
        }
        
        if ( sliceDimension>=QDataSet.MAX_HIGH_RANK ) {
            throw new IllegalArgumentException("sliceDimension > MAX_HIGH_RANK");
        }

        List<Object> deps = new ArrayList(QDataSet.MAX_HIGH_RANK);
        List<Object> bund = new ArrayList(QDataSet.MAX_HIGH_RANK);
        List<Object> bins = new ArrayList(QDataSet.MAX_HIGH_RANK);
        
        for (int i = 0; i < QDataSet.MAX_RANK; i++) {
            deps.add(i, properties.get("DEPEND_" + i));
            bund.add(i, properties.get("BUNDLE_" + i));
            bins.add(i, properties.get("BINS_" + i));
        }

        if ( sliceDimension<QDataSet.MAX_RANK ) {
            deps.remove(sliceDimension);
            bund.remove(sliceDimension);
            bins.remove(sliceDimension);
        }

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

            for (int k = 1; k < rank; k++) {
                 n *= bundle1.value(j, k-1);
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
     * create array of [ "ch_0", "ch_1", ... ]
     * @param len
     * @return string array containing the names that will always work.
     */
    private static String[] backupBundleNames( int len ) {
        String[] result= new String[len];
        for ( int i2=0; i2<len; i2++ ) {
            result[i2]= "ch_"+i2;
        }
        return result;
    }

    /**
     * return the names of the dataset that can be unbundled.
     * @param bundleDs
     * @return and array of the bundle names.
     * @throws IllegalArgumentException when bundleDs is not a bundle.
     * @see DataSetOps#unbundle(org.das2.qds.QDataSet, java.lang.String) 
     */
    public static String[] bundleNames( QDataSet bundleDs ) {
        
        List<String> result= new ArrayList(bundleDs.length(0));
        
        QDataSet bundle1= (QDataSet) bundleDs.property(QDataSet.BUNDLE_1);

        if ( bundle1==null ) {
            bundle1= (QDataSet) bundleDs.property(QDataSet.DEPEND_1); //simple legacy bundle was once DEPEND_1.
            if ( bundle1!=null && bundle1.rank()>1 ) {
                if ( bundle1.rank()!=2 ) {
                    throw new IllegalArgumentException("high rank DEPEND_1 found where rank 1 was expected");
                } else {
                    result= Arrays.asList( backupBundleNames( bundle1.length(0) ) );
                }
            } else if ( bundle1!=null ) {
                Units u= SemanticOps.getUnits( bundle1 );
                for ( int i2=0; i2<bundle1.length(); i2++ ) {
                    result.add( Ops.saferName( u.createDatum( bundle1.value(i2) ).toString() ) );
                }
            } else {
                result= Arrays.asList( backupBundleNames( bundleDs.length(0) ) );
            }
        } else {
            for ( int j=0; j<bundle1.length(); j++ ) { // allow label to be used to access data, since old vap files may contain these.
                String n1= (String) bundle1.property( QDataSet.NAME, j );
                if ( n1!=null ) {
                    n1= Ops.saferName(n1);
                    result.add(n1);
                } else {
                    n1= (String) bundle1.property( QDataSet.LABEL, j );
                    if ( n1!=null ) {
                        n1= Ops.saferName(n1);
                        result.add(n1);
                    } else {
                        n1= (String) bundle1.property( QDataSet.ELEMENT_LABEL, j );
                        if ( n1!=null ) {
                            n1= Ops.saferName(n1);
                            result.add(n1);
                        } else {
                            result.add("ch_"+j );
                        }
                    }
                    
                } 
            }
        }
        return result.toArray( new String[0] );
    }
    
    /**
     * return the index of the named bundled dataset.  This cleans up
     * the name so that is contains just a Java-style identifier.  Also, ch_1 is
     * always implicitly index 1.  
     * Last, if safe names created from labels match that this is used. For example,
     *<blockquote><pre>
     *bds=ripplesVectorTimeSeries(100)
     *2==indexOfBundledDataSet( bds, "Z" ) 
     *</pre></blockquote>
     * demonstrates its use.
     * 
     * Last, extraneous spaces and underscores are removed to see if this will result in a match.
     * 
     * @param bundleDs a bundle dataset with the property BUNDLE_1 or DEPEND_1 having EnumerationUnits, (or BUNDLE_0 for a rank 1 dataset).
     * @param name the named dataset.
     * @return the index or -1 if the name is not found.
     */
    public static int indexOfBundledDataSet( QDataSet bundleDs, String name ) {
        int rank= bundleDs.rank();
        QDataSet bundle1= (QDataSet) bundleDs.property( "BUNDLE_"+(rank-1) );
        
        int ib= -1;
        int i= name.indexOf("["); // allow name to be "Flux[Time=1440,en=10]"
        if ( i>0 ) {
            name= name.substring(i);
            name= Ops.saferName(name);
        } else {
            name= Ops.saferName(name);
        }

        if ( name.matches("ch_\\d+") ) {
            int ich= Integer.parseInt(name.substring(3) );
            return ich;
        }

        if ( bundle1==null ) {
            bundle1= (QDataSet) bundleDs.property( "DEPEND_"+(rank-1) ); //simple legacy bundle was once DEPEND_1.
            if ( bundle1!=null && bundle1.rank()>1 ) {
                throw new IllegalArgumentException("high rank DEPEND_1 found where rank 1 was expected");
            } else if ( bundle1!=null ) {
                Units u= SemanticOps.getUnits( bundle1 );
                for ( int i2=0; i2<bundle1.length(); i2++ ) {
                    if ( name.equals( Ops.saferName( u.createDatum( bundle1.value(i2) ).toString() ) ) ) {
                        return i2;
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
            int[] dims= (int[])bundle1.property( QDataSet.ELEMENT_DIMENSIONS, j );
            if ( bundle1.length(j)>0 || ( dims!=null && dims.length>0 ) ) {
                n1= (String) bundle1.property( QDataSet.ELEMENT_NAME, j );
                if ( n1!=null ) n1= Ops.saferName(n1);
                if ( n1!=null && n1.equals(name) ) {
                    ib= j;
                    highRank= true;
                    break;
                }
            }
        }

        //if ( ib==-1 ) {
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
        //}
        
        if ( ib==-1 ) {
            name= name.replaceAll("_| ","");
            for ( int j=0; j<bundle1.length(); j++ ) {
                String n1= (String) bundle1.property( QDataSet.NAME, j );
                if ( n1!=null ) n1= Ops.saferName(n1);
                if ( n1!=null ) n1= n1.replaceAll("_| ","");
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
        }
        
        if ( highRank ) {
            logger.log(Level.FINER, "index of bundled dataset \"{0}\" is {1} (highrank={2})", new Object[]{name, ib, highRank});
        } 
        

        return ib;
    }
    
    /**
     * Extract the named bundled dataset.  For example, extract B_x from bundle of components.
     * @param bundleDs a bundle of datasets
     * @param name the name of the bundled dataset, or "ch_&lt;i&gt;" where i is the dataset number
     * @see #unbundle(org.das2.qds.QDataSet, int) 
     * @throws IllegalArgumentException if no named dataset is found.
     * @return the named dataset
     */
    public static QDataSet unbundle( QDataSet bundleDs, String name ) {
        QDataSet bundle1= (QDataSet) bundleDs.property(QDataSet.BUNDLE_1);

        int ib= indexOfBundledDataSet( bundleDs, name );

        boolean highRank= false; // we have to see if they referred to the high-rank dataset, or the rank 1 dataset.  Chris, wouldn't it be nice if Java could return two things?
        
        int[] dims=null;
        if ( ib>-1 && bundle1!=null ) dims= (int[])bundle1.property( QDataSet.ELEMENT_DIMENSIONS, ib );
        if ( bundle1!=null && ( bundle1.length(ib)>0 || ( dims!=null && dims.length>0 ) ) ) {
            String n1= (String) bundle1.property( QDataSet.ELEMENT_NAME, ib );
            if ( n1!=null ) n1= Ops.saferName(n1);
            if ( n1!=null && n1.equals(name) ) {
                highRank= true;
            }
            if ( highRank==false ) {
                n1= (String) bundle1.property( QDataSet.ELEMENT_LABEL, ib );
                if ( n1!=null ) n1= Ops.saferName(n1);
                if ( n1!=null && n1.equals(name) ) {
                    highRank= true;
                }
            }
        }
                
        if ( ib==-1 ) {
            if ( name.matches("ch_\\d+") ) {
                int ich= Integer.parseInt(name.substring(3) );
                return DataSetOps.unbundle(bundleDs, ich, false);
            } else {
                throw new IllegalArgumentException("unable to find dataset with name \""+name+"\" in bundle "+bundleDs );
            }
        } else {
            return unbundle(bundleDs,ib,highRank);
        }
    }

    /**
     * extract the dataset that is dependent on others, or the last one.  
     * For example, the dataset ds[:,"x,y"] &rarr; y[:]
     * @param bundleDs a bundle of datasets
     * @return the default dataset
     * @see Schemes#bundleDataSet() 
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
     * @param bundleDs the bundle dataset.
     * @param ib index of the dataset to extract. If the index is within a dataset,
     *   then the entire dataset is returned.
     * @throws IndexOutOfBoundsException if the index is invalid.
     * @throws IllegalArgumentException if the dataset is not a bundle dataset, with either BUNDLE_1 or DEPEND_1 set.
     * @return the ib-th dataset from the bundle.
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
     * @param bundleDs the bundle dataset.
     * @param ib index of the dataset to extract. If the index is within a dataset,
     *   then the entire dataset is returned.
     * @param highRank if true, then if the dataset at ib is rank 2 or greater, then
     *   then the entire dataset is returned.  If false, only the slice of the dataset is
     *   returned.
     * @throws IndexOutOfBoundsException if the index is invalid.
     * @throws IllegalArgumentException if the dataset is not a bundle dataset, with either BUNDLE_1 or DEPEND_1 set.
     * @return the ib-th dataset from the bundle.
     */
    public static QDataSet unbundle(QDataSet bundleDs, int ib, boolean highRank ) {
        
        QDataSet bundle=null;

        if ( bundleDs.rank()>=2 ) { // unbundle now allows rank >2 ...
            QDataSet bundle1= (QDataSet) bundleDs.property(QDataSet.BUNDLE_1);
            if ( bundle1==null ) {
                bundle1= (QDataSet) bundleDs.property(QDataSet.DEPEND_1); //simple legacy bundle was once DEPEND_1.
                if ( bundle1==null ) {
                    if ( bundleDs.rank()==2 ) {
                        return new Slice1DataSet( bundleDs, ib ); //TODO: this was   throw new IllegalArgumentException( "Neither BUNDLE_1 nor DEPEND_1 found on dataset passed to unbundle command.");
                    } else if ( bundleDs.rank()==3 ) {
                        return new Slice2DataSet( bundleDs, ib ); //TODO: this was   throw new IllegalArgumentException( "Neither BUNDLE_1 nor DEPEND_1 found on dataset passed to unbundle command.");
                    } else {
                        throw new IllegalArgumentException("rank must be 2 or 3");
                    }
                }
                if ( bundle1.rank()==2 ) {                    
                    return new Slice1DataSet( bundleDs, ib );  // warning message removed, because rank 1 context is used.
                } else if ( bundle1.rank()>1 ) {
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

        // since 2016-09-27, the dimensions should be a property now, and the dataset should be [n,0].
        int[] dimensions= (int[]) bundle.property(QDataSet.ELEMENT_DIMENSIONS,ib);
        if ( dimensions==null && bundle.length(j)>0 ) {
            dimensions= new int[bundle.length(j)];
            for ( int ii=0; ii<bundle.length(j); ii++ ) {
                dimensions[ii]= (int)bundle.value( j, ii );
            }
        }
        if ( dimensions!=null && dimensions.length==0 ) {
            dimensions= null;
        }
        
        if ( highRank ) {
            Integer s= (Integer)bundle.property(QDataSet.START_INDEX,ib);
            if ( s==null ) s= ib;
            if ( dimensions!=null ) {
                is= s;
                int n=1;
                for (int k = 0; k < dimensions.length; k++) {
                     n *= dimensions[k];
                }
                len= n;
                j= ib;
            } 
        }
        
        if ( dimensions==null || !highRank ) {
            if ( bundleDs instanceof BundleDataSet ) {
                QDataSet r= ((BundleDataSet)bundleDs).unbundle(j);
                QDataSet dep0= (QDataSet) bundleDs.property( QDataSet.DEPEND_0 );
                String dependName= (String)r.property(QDataSet.DEPENDNAME_0);
                if ( dependName!=null ) {
                    dep0= unbundle( bundleDs, dependName );
                }
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
                for (String names11 : names1) {
                    Object v = bundle.property(names11, j);
                    if (v!=null) {
                        result.putProperty(names11, v);
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
        } else if ( dimensions.length==1 || dimensions.length==2 ) {
            if ( bundleDs.rank()==1 ) {
                return bundleDs.trim(is, is+len);
            }
            TrimStrideWrapper result= new TrimStrideWrapper(bundleDs);
            result.setTrim( 1, is, is+len, 1 );
            Integer ifirst= (Integer) bundle.property( QDataSet.START_INDEX, j  );
            int first,last;
            if ( ifirst!=null ) {
                first= ifirst;
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
                QDataSet bundleTrim= bundle.trim(first,last+1);
                MutablePropertyDataSet mds;
                mds= DataSetOps.makePropertiesMutable( bundleTrim );
                Ops.copyIndexedProperties( bundleTrim, mds );
                props.put( QDataSet.BUNDLE_1, mds );
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
            for ( Entry<String,Object> e: props3.entrySet() ) {
                String ss= e.getKey();
                Object vv= props.get( ss );
                if ( vv==null ) {
                    props.put( ss, e.getValue() );
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
            if ( dimensions.length==2 ) {
                int[] qube= new int[] { result.length(), dimensions[0], dimensions[1] };
                return Ops.reform( result, qube );
            } else {
                return result;
            }
        } else {
            throw new IllegalArgumentException("rank limit: >2 not supported");
        }

    }

    /**
     * given the bundle descriptor bds, return the dataset to be put in the context property.
     * @param bundle1 rank 2 bundle descriptor of length n with indexed properties.  This was introduced
     * when sloppy slice code was using the NAME and dropping the LABEL.
     * @param index 0&lt;=index&lt;n index of the unbundle
     * @return rank 0 QDataSet.
     */
    protected static QDataSet getContextForUnbundle( QDataSet bundle1, int index ) {
        String tname= (String) bundle1.property(QDataSet.NAME);
        if ( tname==null ) tname=(String) bundle1.property(QDataSet.NAME,index);
        String tlabel= (String) bundle1.property(QDataSet.LABEL,index);
        tname= String.valueOf(tname); // guard against null
        tlabel= String.valueOf(tlabel);
        MutablePropertyDataSet context= (MutablePropertyDataSet) ( Ops.labelsDataset( new String[] { tlabel } )).slice(0);
        if ( !Ops.safeName(tlabel).equals(tname) ) {
            context.putProperty( QDataSet.NAME, tname );
        }
        return context;
    }

    /**
     * returns the value from within a distribution that is the nth percentile division.  This
     * returns a fill dataset (Units.dimensionless.getFillDouble()) when the data is all fill.
     * @param ds the dataset
     * @param n percent between 0 and 100.
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
     * @param ds rank 1, 2, or rank 3 join.
     * @param level the level between 0 and 100.
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
     * normalize the nth-level percentile from:<ul>
     *   <li>rank 1: each element 
     *   <li>rank 2: each row of the dataset
     *   <li>rank 3: each row of each rank 2 dataset slice.
     * </ul>
     * If the data is already in dB, then the result is a difference.
     * This is assuming the units are similar to voltage, not a power, we think,
     * containing code like 20 * Math.log10( ds / background ).
     * @param ds
     * @param level the percentile level, e.g. 10= 10%
     * @return the result dataset, in dB above background.
     */
    public static QDataSet dbAboveBackgroundDim1( QDataSet ds, double level ) {
        return dbAboveBackgroundDim1( ds, level, false );
    }
    
    /**
     * normalize the nth-level percentile from:<ul>
     *   <li>rank 1: each element 
     *   <li>rank 2: each row of the dataset
     *   <li>rank 3: each row of each rank 2 dataset slice.
     * </ul>
     * If the data is already in dB, then the result is a difference.
     * This is assuming the units are similar to voltage, not a power,
     * containing code like 20 * Math.log10( ds / background ).
     * @param ds
     * @param level the percentile level, e.g. 10= 10%
     * @param power if true, return 10*Math.log10(ds / background ).
     * @return the result dataset, in dB above background.
     */
    public static QDataSet dbAboveBackgroundDim1( QDataSet ds, double level, boolean power ) {
    
        MutablePropertyDataSet result;

        double fill= -1e31;
        boolean hasFill= false;
        final double mult= power ? 10.0 : 20.0;
        switch (ds.rank()) {
            case 1:
                {
                    QDataSet back= getBackgroundLevel( ds, level );
                    result= Ops.copy(ds);
                    boolean db= ds.property(QDataSet.UNITS)==Units.dB;
                    WritableDataSet wds= (WritableDataSet)result;
                    QDataSet validDs= Ops.valid(back);
                    QDataSet vds2= DataSetUtil.weightsDataSet(ds);
                    if ( validDs.value()>0 ) {
                        for ( int ii=0; ii<ds.length(); ii++ ) {
                            if ( vds2.value(ii)>0 ) {
                                double v= db ? ds.value(ii) - back.value() : mult * Math.log10( ds.value(ii) / back.value() );
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
                    break;
                }
            case 2:
                {
                    QDataSet back= getBackgroundLevel( ds, level );
                    result= Ops.copy(ds);
                    boolean db= ds.property(QDataSet.UNITS)==Units.dB;
                    WritableDataSet wds= (WritableDataSet)result;
                    QDataSet validDs= Ops.copy(Ops.valid(back));
                    QDataSet vds2= DataSetUtil.weightsDataSet(ds);
                    for ( int jj=0; jj<ds.length(0); jj++ ) {
                        for ( int ii=0; ii<ds.length(); ii++ ) {
                            if ( validDs.value(jj)>0 && vds2.value(ii,jj)>0 ) {
                                double v= db ? ds.value(ii,jj) - back.value(jj) : mult * Math.log10( ds.value(ii,jj) / back.value(jj) );
                                wds.putValue( ii,jj, Math.max( 0,v ) );
                            } else {                            
                                wds.putValue( ii,jj, fill );
                                hasFill= true;
                            }
                        }
                    }       result.putProperty( QDataSet.USER_PROPERTIES,Collections.singletonMap("background", back) );
                    break;
                }
            default:
                JoinDataSet result1= new JoinDataSet(ds.rank());
                for ( int i=0; i<ds.length(); i++ ) {
                    QDataSet ds1= ds.slice(i);
                    QDataSet r1= dbAboveBackgroundDim1( ds1, level, power );
                    result1.join(r1);
                    if ( r1.property( QDataSet.FILL_VALUE )!=null ) {
                        hasFill= true;
                    }
                }   result= result1;
                break;
        }

        result.putProperty( QDataSet.UNITS, Units.dB );
        result.putProperty( QDataSet.TYPICAL_MIN, 0 );
        result.putProperty( QDataSet.TYPICAL_MAX, 120 );
        result.putProperty( QDataSet.SCALE_TYPE, "linear" );
        result.putProperty( QDataSet.VALID_MIN, null );
        result.putProperty( QDataSet.VALID_MAX, null );
        
        if ( hasFill ) result.putProperty( QDataSet.FILL_VALUE, fill );

        return result;
    }

    /**
     * normalize the level-th percentile from:
     *   rank 1: each element (same as removeBackground1)
     *   rank 2: each column of the dataset
     *   rank 3: each column of each rank 2 dataset slice.
     * There must be at least 10 elements.  If the data is already in dB, then the result is a difference.
     * This is assuming the units are similar to voltage, not a power, we think,
     * containing code like 20 * Math.log10( ds / background ).
     * @param ds
     * @param level the percentile level, e.g. 10= 10%
     * @return the result dataset, in dB above background.
     */
    public static QDataSet dbAboveBackgroundDim0( QDataSet ds, double level ) {

        MutablePropertyDataSet result;

        double fill= -1e31;
        boolean hasFill= false;

        switch (ds.rank()) {
            case 1:
                {
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
                    break;
                }
            case 2:
                {
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
                    break;
                }
            default:
                {
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
                    break;
                }
        }

        result.putProperty( QDataSet.UNITS, Units.dB );
        result.putProperty( QDataSet.TYPICAL_MIN, 0 );
        result.putProperty( QDataSet.TYPICAL_MAX, 120 );
        result.putProperty( QDataSet.SCALE_TYPE, "linear" );
        result.putProperty( QDataSet.VALID_MIN, null );
        result.putProperty( QDataSet.VALID_MAX, null );
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
     * @param c process string, as in sprocess.
     * @return true if the process described in c is probably a slow process
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
     * @param c the process string, like "bgsmx|slice0(9)|histogram()"
     * @param fillDs the input dataset.
     * @param mon a monitor for the processing
     * @return dataset resulting form filters.
     * @throws Exception when the processStr cannot be processed.
     * @throws RuntimeException when the component (e.g. bgsmx) is not found.
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
     * sprocess implements the poorly-named filters string / process string of Autoplot, allowing
     * clients to "pipe" data through a chain of operations.  For example, the filters string 
     * "|slice0(9)|histogram()" will slice on the ninth index and then take a histogram of that
     * result.  See http://www.papco.org/wiki/index.php/DataReductionSpecs (TODO: wiki page was lost,
     * which could probably be recovered.)  There's a big problem here:
     * if the command is not recognized, then it is ignored.  We should probably change this,
     * but the change should be at a major version change in case it breaks things.
     * @param c process string like "slice0(9)|histogram()"
     * @param fillDs The dataset loaded from the data source controller, with initial filters (like fill) applied.
     * @param mon monitor for the processing.
     * @throws ParseException when the string cannot be parsed
     * @throws Exception when a function cannot be processed (e.g. index out of bounds)
     * @return the dataset after the process string is applied.
     * @see <a href="http://autoplot.org/developer.dataset.filters">http://autoplot.org/developer.dataset.filters</a>
     * @see <a href="http://autoplot.org/developer.panel_rank_reduction">http://autoplot.org/developer.panel_rank_reduction</a>
     */
    public static QDataSet sprocess( String c, QDataSet fillDs, ProgressMonitor mon ) throws Exception {
        return OperationsProcessor.sprocess( c, fillDs, mon );
    }

    /**
     * indicate if this one operator changes the dimensions.  For example, 
     * |smooth doesn't change the dimensions, but fftPower and slice do.
     * @param p the filter, e.g. "|smooth"
     * @return true if the dimensions change. 
     */
    public static boolean changesDimensions( String p ) {
        int j= p.indexOf('(');
        if ( j>-1 ) {
            p= p.substring(0,j);
        }
        switch (p) {
            case "|smooth":
            case "|nop":
            case "|trim":
            case "|magnitude":
            case "|abs":
            case "|hanning":
            case "|butterworth":
            case "|detrend":
            case "|medianFilter":
            case "|copy":
            case "|setDepend0Cadence":
                return false;
            default:
                return true;
        }
    }
    
    /**
     * return the next command that changes dimensions.
     * @param s0 scanner 
     * @return the command, e.g. "|slice0"
     */
    private static String nextDimensionChangingCommand( Scanner s0 ) {
        while ( s0.hasNext() ) {
            String cmd= s0.next();
            if ( cmd.startsWith("|") ) {
                if ( changesDimensions(cmd) ) {
                    return cmd;
                }
            }
        }
        return null;
    }
    
    /**
     * indicate if the operators change dimensions of the dataset.  Often
     * this will result in true when the dimensions do not change, this is the better way to err.
     * @param c0 old value for the process string, e.g. "slice0(0)"
     * @param c1 new value for the process string, e.g. "slice0(0)|slice1(0)"
     * @return true if the dimensions would be different.
     */
    public static boolean changesDimensions( String c0, String c1 ) {
        //if ( c.length()==0 && !c2.startsWith("|") ) return false;  //TODO: kludge to avoid true when adding component child.
        if ( c0==null || c1==null ) return true;
        Scanner s0= new Scanner( c0 );
        s0.useDelimiter("[\\(\\),]");
        Scanner s1= new Scanner( c1 );
        s1.useDelimiter("[\\(\\),]");
        boolean slicesChangesDim= false;
        String cmd0= nextDimensionChangingCommand( s0 );
        String cmd1= nextDimensionChangingCommand( s1 );
        while ( cmd0!=null && cmd1!=null ) {
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
            }
            cmd0= nextDimensionChangingCommand( s0 );
            cmd1= nextDimensionChangingCommand( s1 );                
        }
        
        boolean res= slicesChangesDim || cmd0!=null || cmd1!=null;
        logger.log(Level.FINE, "  changesDimensions {0} , {1} ->{2}", new Object[]{c0, c1, res});
        return res;
    }

    /**
     * return a bounding qube of the independent dimensions containing
     * the dataset.  If r is the result of the function, then for<ul>
     *   <li>rank 1: r.slice(0) x bounds, r.slice(1) y bounds
     *   <li>rank 2 waveform: r.slice(0) x bounds, r.slice(1) y bounds
     *   <li>rank 2 table:r.slice(0) x bounds  r.slice(1)  DEPEND_0 bounds.
     *   <li>rank 3 table:r.slice(0) x bounds  r.slice(1)  DEPEND_0 bounds.
     * </ul>
     * This does not take DELTA_PLUS and DELTA_MINUS into account.
     * When all the data is fill, ds[0,0] will be positive infinity.
     * @param ds a rank 1,2, or 3 dataset.
     * @return a bounding qube of the independent dimensions 
     */
    public static QDataSet dependBoundsSimple( QDataSet ds ) {
        logger.entering( "org.das2.qds.DataSetOps", "dependBoundsSimple" );
        QDataSet xrange;
        QDataSet yrange;

        if ( ds.rank()==1 ) {
            xrange= Ops.extentSimple( SemanticOps.xtagsDataSet(ds), null );
            yrange= Ops.extentSimple( ds, null );
        } else if( ds.rank() == 2 ) {
            if ( SemanticOps.isRank2Waveform(ds) ) {
                xrange= Ops.extentSimple( SemanticOps.xtagsDataSet(ds), null );
                yrange= Ops.extentSimple( ds, null );
            //} else if ( SemanticOps.isBundle(ds) ) { //bug: spectrogram rend of rbspb_pre_ect-mageisM75-sp-L1_20120908_v1.0.0.cdf?Count_Rate_SpinSetAvg
            //    xrange= Ops.extent( SemanticOps.xtagsDataSet(ds) );
            //    yrange= null;
            //    for ( int i=0; i<ds.length(0); i++ ) {
            //        yrange= Ops.extent( DataSetOps.unbundle( ds, i ), yrange );
            //    }
            } else {
                xrange= Ops.extentSimple( SemanticOps.xtagsDataSet(ds), null );
                yrange= Ops.extentSimple( SemanticOps.ytagsDataSet(ds), null );
            }
        } else if ( ds.rank()==3 ) {
            QDataSet ds1= ds.slice(0);
            xrange= Ops.extentSimple( SemanticOps.xtagsDataSet(ds1), null );
            yrange= Ops.extentSimple( SemanticOps.ytagsDataSet(ds1), null );
            for ( int i=1; i<ds.length(); i++ ) {
                ds1= ds.slice(i);
                xrange= Ops.extentSimple( SemanticOps.xtagsDataSet(ds1), xrange );
                yrange= Ops.extentSimple( SemanticOps.ytagsDataSet(ds1), yrange );
            }
        } else {
            throw new IllegalArgumentException("bad rank");
        }

        QDataSet result= makePropertiesMutable( Ops.join( xrange, yrange ) );
        ((MutablePropertyDataSet)result).putProperty( QDataSet.BINS_1, QDataSet.VALUE_BINS_MIN_MAX );

        logger.exiting( "org.das2.qds.DataSetOps", "dependBoundsSimple" );
        return result;
        
    }
    
    /**
     * return a bounding qube of the independent dimensions containing
     * the dataset.  If r is the result of the function, then for<ul>
     *   <li>rank 1: r.slice(0) x bounds, r.slice(1) y bounds
     *   <li>rank 2 waveform: r.slice(0) x bounds, r.slice(1) y bounds
     *   <li>rank 2 table:r.slice(0) x bounds  r.slice(1)  DEPEND_0 bounds.
     *   <li>rank 3 table:r.slice(0) x bounds  r.slice(1)  DEPEND_0 bounds.
     * </ul>
     * @param ds rank 1,2, or 3 dataset.
     * @return a bounding qube of the independent dimensions 
     */
    public static QDataSet dependBounds( QDataSet ds ) {
        logger.entering( "org.das2.qds.DataSetOps", "dependBounds" );
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
        logger.exiting( "org.das2.qds.DataSetOps", "dependBounds" );
        return result;
    }

    /**
     * return true of the bounds overlaps with the x and y range.
     * @param bounds bounding box
     * @param xValue the x range
     * @param yValue the y range
     * @return true of the bounds overlap
     */
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
