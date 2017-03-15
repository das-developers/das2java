/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.dataset;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.autoplot.bufferdataset.BufferDataSet;
import org.autoplot.bufferdataset.ByteDataSet;
import org.autoplot.bufferdataset.FloatDataSet;
import org.autoplot.bufferdataset.IntDataSet;
import org.autoplot.bufferdataset.ShortDataSet;
import org.das2.datum.CacheTag;
import org.das2.datum.EnumerationUnits;
import org.das2.datum.Units;
import org.das2.datum.UnitsConverter;
import org.das2.datum.UnitsUtil;
import org.das2.util.LoggerManager;
import org.virbo.dsops.Ops;

/**
 * A number of static methods were initially defined in DDataSet, then
 * copied into FDataSet and others when they were made.  This super implementation
 * will parent all such datasets and provide common methods.
 * @author jbf
 */
public abstract class ArrayDataSet extends AbstractDataSet implements WritableDataSet {

    protected static final Logger logger= LoggerManager.getLogger("qdataset.array");

    int rank;

    int len0;
    int len1;
    int len2;
    int len3;

    float fill= Float.NaN;
    double dfill= Double.NaN;

    private static final boolean RANGE_CHECK = "true".equals( System.getProperty("rangeChecking","true") );

    Class componentType;
    
    protected ArrayDataSet( Class componentType ) {
        this.componentType= componentType;
    }
    
    protected static ArrayDataSet create( int rank, int len0, int len1, int len2, int len3, Object back ) {
        Class c= back.getClass().getComponentType();
        if ( c==double.class ) return new DDataSet( rank, len0, len1, len2, len3, (double[])back );
        if ( c==float.class ) return new FDataSet( rank, len0, len1, len2, len3, (float[])back );
        if ( c==long.class ) return new LDataSet( rank, len0, len1, len2, len3, (long[])back );
        if ( c==int.class ) return new IDataSet( rank, len0, len1, len2, len3, (int[])back );
        if ( c==short.class ) return new SDataSet( rank, len0, len1, len2, len3, (short[])back );
        if ( c==byte.class ) return new BDataSet( rank, len0, len1, len2, len3, (byte[])back );
        throw new IllegalArgumentException("class not supported: "+c);
    }
    
    /**
     * create a dataset with the backing type and the dimensions given.
     * The following component types are supported: double.class, float.class,
     * long.class, int.class, short.class, and byte.class.  
     * @param c the backing type, such as double.class, float.class, etc.
     * @param qube dimensions of the dataset
     * @return the dataset
     * @see #getComponentType() 
     * @see BufferDataSet which also supports unsigned types.
     */
    public static ArrayDataSet create( Class c, int[] qube ) {
        if ( c==double.class ) return DDataSet.create( qube );
        if ( c==float.class ) return FDataSet.create( qube );
        if ( c==long.class ) return LDataSet.create( qube );
        if ( c==int.class ) return IDataSet.create( qube );
        if ( c==short.class ) return SDataSet.create( qube );
        if ( c==byte.class ) return BDataSet.create( qube );
        throw new IllegalArgumentException("class not supported: "+c);      
    }

    /**
     * create a rank 0 dataset of the class, which can be 
     * double.class, float.class, long.class, int.class, short.class and byte.class
     * @param c the primitive class of each element.
     * @return the ArrayDataSet
     */
    public static ArrayDataSet createRank0( Class c ) {
        if ( c==double.class ) return new DDataSet( 0, 1, 1, 1, 1 );
        if ( c==float.class ) return new FDataSet( 0, 1, 1, 1, 1 );
        if ( c==long.class ) return new LDataSet( 0, 1, 1, 1, 1 );
        if ( c==int.class ) return new IDataSet( 0, 1, 1, 1, 1 );
        if ( c==short.class ) return new SDataSet( 0, 1, 1, 1, 1 );
        if ( c==byte.class ) return new BDataSet( 0, 1, 1, 1, 1 );
        throw new IllegalArgumentException("class not supported: "+c);
    }
    
    /**
     * create a rank 1 dataset of the class, which can be 
     * double.class, float.class, long.class, int.class, short.class and byte.class
     * @param c the primitive class of each element.
     * @param len0 the length of the dimension
     * @return the ArrayDataSet
     */
    public static ArrayDataSet createRank1( Class c, int len0 ) {
        if ( c==double.class ) return new DDataSet( 1, len0, 1, 1, 1 );
        if ( c==float.class ) return new FDataSet( 1, len0, 1, 1, 1 );
        if ( c==long.class ) return new LDataSet( 1, len0, 1, 1, 1 );
        if ( c==int.class ) return new IDataSet( 1, len0, 1, 1, 1 );
        if ( c==short.class ) return new SDataSet( 1, len0, 1, 1, 1 );
        if ( c==byte.class ) return new BDataSet( 1, len0, 1, 1, 1 );
        throw new IllegalArgumentException("class not supported: "+c);
    }

    /**
     * create a rank 2 dataset of the class, which can be 
     * double.class, float.class, long.class, int.class, short.class and byte.class
     * @param c the primitive class of each element.
     * @param len0 the length of the dimension
     * @param len1 the length of the dimension
     * @return the ArrayDataSet
     */
    public static ArrayDataSet createRank2( Class c, int len0, int len1 ) {
        if ( c==double.class ) return new DDataSet( 2, len0, len1, 1, 1 );
        if ( c==float.class ) return new FDataSet( 2, len0, len1, 1, 1 );
        if ( c==long.class ) return new LDataSet( 2, len0, len1, 1, 1 );
        if ( c==int.class ) return new IDataSet( 2, len0, len1, 1, 1 );
        if ( c==short.class ) return new SDataSet( 2, len0, len1, 1, 1 );
        if ( c==byte.class ) return new BDataSet( 2, len0, len1, 1, 1 );
        throw new IllegalArgumentException("class not supported: "+c);
    }

    /**
     * create a rank 3 dataset of the class, which can be 
     * double.class, float.class, long.class, int.class, short.class and byte.class
     * @param c the primitive class of each element.
     * @param len0 the length of the dimension
     * @param len1 the length of the dimension
     * @param len2 the length of the dimension
     * @return the ArrayDataSet
     */
    public static ArrayDataSet createRank3( Class c, int len0, int len1, int len2 ) {
        if ( c==double.class ) return new DDataSet( 3, len0, len1, len2, 1 );
        if ( c==float.class ) return new FDataSet( 3, len0, len1, len2, 1 );
        if ( c==long.class ) return new LDataSet( 3, len0, len1, len2, 1 );
        if ( c==int.class ) return new IDataSet( 3, len0, len1, len2, 1 );
        if ( c==short.class ) return new SDataSet( 3, len0, len1, len2, 1 );
        if ( c==byte.class ) return new BDataSet( 3, len0, len1, len2, 1 );
        throw new IllegalArgumentException("class not supported: "+c);
    }

    /**
     * create a rank 4 dataset of the class, which can be 
     * double.class, float.class, long.class, int.class, short.class and byte.class
     * @param c the primitive class of each element.
     * @param len0 the length of the dimension
     * @param len1 the length of the dimension
     * @param len2 the length of the dimension
     * @param len3 the length of the dimension
     * @return the ArrayDataSet
     */
    public static ArrayDataSet createRank4( Class c, int len0, int len1, int len2, int len3 ) {
        if ( c==double.class ) return new DDataSet( 4, len0, len1, len2, len3 );
        if ( c==float.class ) return new FDataSet( 4, len0, len1, len2, len3 );
        if ( c==long.class ) return new LDataSet( 4, len0, len1, len2, len3 );
        if ( c==int.class ) return new IDataSet( 4, len0, len1, len2, len3 );
        if ( c==short.class ) return new SDataSet( 4, len0, len1, len2, len3 );
        if ( c==byte.class ) return new BDataSet( 4, len0, len1, len2, len3 );
        throw new IllegalArgumentException("class not supported: "+c);
    }

    /**
     * return the array as ArrayDataSet  The array must be a 1-D array and the
     * dimensions of the result are provided in qube.
     * @param array 1-D array
     * @param qube dimensions of the dataset
     * @param copy copy the data so that original data is not modified with putValue
     * @return ArrayDataSet
     * @see #create(java.lang.Class, int[]) 
     */
    public static ArrayDataSet wrap( Object array, int[] qube, boolean copy ) {
        Object arr;
        //check type
        if ( !array.getClass().isArray() ) throw new IllegalArgumentException("input must be an array");
        Class c= array.getClass().getComponentType();
        if ( c.isArray() ) throw new IllegalArgumentException("input must be 1-D array");
        if ( copy ) {
            arr= Array.newInstance( c, Array.getLength(array) );
            System.arraycopy( array, 0, arr, 0, Array.getLength(array) );
        } else {
            arr= array;
        }
        if ( c==double.class ) return DDataSet.wrap( (double[])arr, qube );
        if ( c==float.class ) return FDataSet.wrap( (float[])arr, qube );
        if ( c==long.class ) return LDataSet.wrap( (long[])arr, qube );
        if ( c==int.class ) return IDataSet.wrap( (int[])arr, qube );
        if ( c==short.class ) return SDataSet.wrap( (short[])arr, qube );
        if ( c==byte.class ) return BDataSet.wrap( (byte[])arr, qube );

        throw new IllegalArgumentException("component type not supported: "+c );

    }
    
    /**
     * ensure that there are no non-monotonic or repeat records, by starting at the middle
     * of the dataset and finding the monotonic subsection.  The input need not be writable.
     * @param ds ArrayDataSet, which must be writable.
     * @return dataset, possibly with records removed.
     */
    public static ArrayDataSet monotonicSubset2( ArrayDataSet ds ) {
        
        QDataSet ssdep0= (QDataSet)ds.property(QDataSet.DEPEND_0);
        if ( ssdep0==null && UnitsUtil.isTimeLocation( SemanticOps.getUnits(ds) ) ) {
            ssdep0= ds;
        } else if ( ssdep0==null ) {
            return ds;
        }
        
        int mid= ssdep0.length()/2;
        int start=0;
        for ( int i=mid-1; i>0; i-- ) { // trim the left side
            if ( ssdep0.value(i)>=ssdep0.value(i+1) ) {
                start= i+1;
                break;
            }
        }
        int stop= ssdep0.length();
        for ( int i=mid+1; i<ssdep0.length(); i++ ) { // trim the right side
            if ( ssdep0.value(i-1)>=ssdep0.value(i) ) {
                stop= i-1;
                break;
            }
        }
        if ( start>0 || stop<ssdep0.length() ) {
            ds= (ArrayDataSet)ds.trim(start,stop);
            return ds;
        } else {
            return ds;
        }
        
    }
    
    /**
     * ensure that there are no non-monotonic or repeat records, by removing
     * the first N-1 records of N repeated records.  
     * @param ds ArrayDataSet, which must be writable.
     * @return dataset, possibly with records removed.
     */
    public static ArrayDataSet monotonicSubset( ArrayDataSet ds ) {
        
        if ( ds.isImmutable() ) {
            ds= ArrayDataSet.copy(ds);
            logger.warning("immutabilty forced copy.");
        }
               
        QDataSet sdep0= (QDataSet)ds.property(QDataSet.DEPEND_0);
        if ( sdep0==null && UnitsUtil.isTimeLocation( SemanticOps.getUnits(ds) ) ) {
            sdep0= ds;
        } else if ( sdep0==null ) {
            return ds;
        }
        ArrayDataSet dep0= ArrayDataSet.maybeCopy( sdep0 ); // I don't think this will copy.
        if ( !UnitsUtil.isTimeLocation( SemanticOps.getUnits(dep0) ) ) {
            return ds;
        }
        if ( dep0.length()<2 ) return ds;
        if ( dep0.rank()!=1 ) return ds;
        QDataSet vdep0= Ops.valid(dep0);
        
        int[] rback= new int[dep0.length()];
        rback[dep0.length()/2]= dep0.length()/2;
        int rindex=dep0.length()/2+1;
        double a= dep0.value(rindex-1);
        for ( int i=rindex; i<dep0.length(); i++ ) {
            if ( vdep0.value(i)>0 ) {
                double a1=dep0.value(i);
                if ( a1>a ) {
                    rback[rindex]= i;
                    a= a1;
                    rindex++;
                } else {
                    logger.log(Level.FINER, "data point breaks monotonic rule: {0}", i);
                }
            }
        }
        int lindex=dep0.length()/2;
        a= dep0.value(lindex+1);
        for ( int i=lindex; i>=0; i-- ) {
            if ( vdep0.value(i)>0 ) {
                double a1=dep0.value(i);
                if ( a1<a ) {
                    rback[lindex]= i;
                    a= a1;
                    lindex--;
                } else {
                    logger.log(Level.FINER, "data point breaks monotonic rule: {0}", i);
                }
            }
        }
        lindex+=1;
        
        int nrm= dep0.length() - ( rindex-lindex );
        if ( nrm>0 ) {
            if ( rindex==1 ) {
                logger.log(Level.FINE, "ensureMono removes all points, assume it's monotonic decreasing" );
                return ds;
            }
            logger.log(Level.FINE, "ensureMono removes {0} points", nrm);
            Class c= ds.getComponentType();
            int[] idx= new int[rindex-lindex];
            System.arraycopy( rback, lindex, idx, 0, ( rindex-lindex ) );
            ds.putProperty( QDataSet.DEPEND_0, null );
            ds= ArrayDataSet.copy( c, new SortDataSet( ds, Ops.dataset(idx) ) );
            Class depclass= dep0.getComponentType();
            ArrayDataSet dep0copy= ArrayDataSet.copy( depclass, new SortDataSet( dep0, Ops.dataset(idx) ) );
            dep0copy.putProperty( QDataSet.MONOTONIC, Boolean.TRUE );
            ds.putProperty( QDataSet.DEPEND_0, dep0copy );
        }
        return ds;
    }
    
    @Override
    public int rank() {
        return rank;
    }

    @Override
    public final int length() {
        return len0;
    }

    @Override
    public final int length(int i) {
        //if ( RANGE_CHECK && i>=len0 ) throw new IndexOutOfBoundsException("length("+i+") when dim 0 length="+len0); //TODO: allow disable with RANGE_CHECK for performance
        return len1;
    }

    @Override
    public final int length( int i0, int i1 ) {
        //if ( RANGE_CHECK ) {
        //    if ( i0>=len0 ) throw new IndexOutOfBoundsException("length("+i0+","+i1+") when dim 0 length="+len0);
        //    if ( i1>=len1 ) throw new IndexOutOfBoundsException("length("+i0+","+i1+") when dim 1 length="+len1);
        //}
        return len2;
    }

    @Override
    public final int length( int i0, int i1, int i2) {
        //if ( RANGE_CHECK ) {
        //    if ( i0>=len0 ) throw new IndexOutOfBoundsException("length("+i0+","+i1+","+i2+") when dim 0 length="+len0);
        //    if ( i1>=len1 ) throw new IndexOutOfBoundsException("length("+i0+","+i1+","+i2+") when dim 1 length="+len1);
        //    if ( i2>=len2 ) throw new IndexOutOfBoundsException("length("+i0+","+i1+","+i2+") when dim 2 length="+len2);
        //}
        return len3;
    }

    /**
     * return the component type of the backing array, such as double.class.
     * @return the component type of the backing array.
     * @see #create(java.lang.Class, int[]) 
     */
    public Class getComponentType() {
        return componentType;
    }

    /**
     * Shorten the dataset by changing it's dim 0 length parameter.  
     * The same backing array is used, so the element that remain will be the same.
     * This can only be used to shorten datasets, see grow to lengthen.
     * @param len new number of records
     * @see #grow(int) 
     */
    public void putLength( int len ) {
        int limit= Array.getLength( getBack() ) / ( len1*len2*len3 );
        if ( len>limit ) throw new IllegalArgumentException("dataset cannot be lengthened");
        len0= len;
    }

    /**
     * grow the internal store so that append may be used to resize the dataset.
     * @param newRecCount new number of records, which includes the old records.
     * @see #putLength(int) 
     */
    public void grow( int newRecCount ) {
        if ( newRecCount < len0 ) throw new IllegalArgumentException("new recsize for grow smaller than old");
        int newSize= newRecCount * len1 * len2 * len3;
        Object back= getBack();
        int oldSize= Array.getLength(back);

        if ( newSize<oldSize ) { // it's possible that the dataset already has a backing that can support this.  Check for this.
            return;
        }

        Object newBack;
        if ( back instanceof double[] ) {
            newBack= new double[ newSize ];
        } else if ( back instanceof float[] ) {
            newBack= new float[ newSize ];
        } else if ( back instanceof long[] ) {
            newBack= new long[ newSize ];
        } else if ( back instanceof int[] ) {
            newBack= new int[ newSize ];
        } else if ( back instanceof short[] ) {
            newBack= new short[ newSize ];
        } else if ( back instanceof byte[] ) {
            newBack= new byte[ newSize ];
        } else {
            throw new IllegalArgumentException("shouldn't happen bad type");
        }
        System.arraycopy( back, 0, newBack, 0, Array.getLength(back) );
        setBack(newBack);
    }

    /**
     * append the dataset with the same geometry but different number of records (zeroth dim)
     * to this.  An IllegalArgumentException is thrown when there is not enough room.  
     * See grow(newRecCount).
     * Not thread safe--we need to go through and make it so...
     * @param ds
     */
    public synchronized void append( ArrayDataSet ds ) {
        if ( ds.rank()!=this.rank ) throw new IllegalArgumentException("rank mismatch");
        if ( ds.len1!=this.len1 ) throw new IllegalArgumentException("len1 mismatch");
        if ( ds.len2!=this.len2 ) throw new IllegalArgumentException("len2 mismatch");
        if ( ds.len3!=this.len3 ) throw new IllegalArgumentException("len3 mismatch");
        if ( this.getBack().getClass()!=ds.getBack().getClass() ) throw new IllegalArgumentException("backing type mismatch");

        int myLength= this.len0 * this.len1 * this.len2 * this.len3;
        int dsLength= ds.len0 * ds.len1 * ds.len2 * ds.len3;

        if ( Array.getLength(this.getBack()) < myLength + dsLength ) {
            throw new IllegalArgumentException("unable to append dataset, not enough room");
        }

        System.arraycopy( ds.getBack(), 0, this.getBack(), myLength, dsLength );

        Units u1= SemanticOps.getUnits(this);
        Units u2= SemanticOps.getUnits(ds);
        if ( u1!=u2 ) {
            if ( u1 instanceof EnumerationUnits && u2 instanceof EnumerationUnits ) {
                for ( int i=myLength; i<myLength+dsLength; i++ ) {
                    double d= Array.getDouble( this.getBack(),i );
                    d= ((EnumerationUnits)u1).createDatum( u2.createDatum(d).toString() ).doubleValue(u1);
                    Array.set( this.getBack(), i, d );
                }
            } else {
                UnitsConverter uc= UnitsConverter.getConverter(u2,u1);
                Class backClass= this.getBack().getClass().getComponentType();
                for ( int i=myLength; i<myLength+dsLength; i++ ) {
                    Number nv=  uc.convert(Array.getDouble( this.getBack(),i) ) ;
                    if ( backClass==double.class ) {
                        Array.set( this.getBack(), i, nv.doubleValue() );
                    } else if ( backClass==float.class ) {
                        Array.set( this.getBack(), i, nv.floatValue() );
                    } else if ( backClass==long.class ) {
                        Array.set( this.getBack(), i, nv.longValue() );
                    } else if ( backClass==int.class ) {
                        Array.set( this.getBack(), i, nv.intValue() );
                    } else if ( backClass==short.class ) {
                        Array.set( this.getBack(), i, nv.shortValue() );
                    } else if ( backClass==byte.class ) {
                        Array.set( this.getBack(), i, nv.byteValue() );
                    } else {
                        throw new IllegalArgumentException("unsupported type: "+backClass );
                    }
                }
            }
        }
        
        this.len0= this.len0 + ds.len0;

        properties.putAll( joinProperties( this, ds ) ); 

    }

    /**
     * return true if the dataset can be appended.  Note this assumes that the
     * same length, etc.  This just checks that we have the number of spare records
     * in the backing store.
     * @param ds the dataset to test
     * @return true if the dataset can be appended.
     */
    public boolean canAppend( ArrayDataSet ds ) {
        if ( ds.rank()!=this.rank ) throw new IllegalArgumentException("rank mismatch");
        if ( ds.len1!=this.len1 ) throw new IllegalArgumentException("len1 mismatch");
        if ( ds.len2!=this.len2 ) throw new IllegalArgumentException("len2 mismatch");
        if ( ds.len3!=this.len3 ) throw new IllegalArgumentException("len3 mismatch");
        if ( this.getBack().getClass()!=ds.getBack().getClass() ) {
            Class a1= ds.getBack().getClass();
            Class a2= this.getBack().getClass();
            String s1,s2;
            s1= "" + a1.getComponentType();
            s2= "" + a2.getComponentType();
            throw new IllegalArgumentException("backing type mismatch: "+ s2 + "["+ds.length()+",*] can't be appended to "+ s1 + "["+this.length()+",*]" );
        }
        int trec= Array.getLength(this.getBack()) / this.len1 / this.len2 / this.len3;
        return trec-this.len0 > ds.length();
    }

    /**
     * Copy the dataset to an ArrayDataSet only if the dataset is not 
     * already an ArrayDataSet.
     * @param ds the dataset to copy, which may be an ArrayDataSet
     * @return an ArrayDataSet.
     */
    public static ArrayDataSet maybeCopy( QDataSet ds ) {
        if ( ds instanceof ArrayDataSet ) {
            return (ArrayDataSet)ds;
        } else {
            return copy(ds);
        }
    }

    /**
     * Copy the dataset to a ArrayDataSet only if the dataset is not already a
     * particular instance of the given type of ArrayDataSet.
     * @param c component type, e.g. float.class double.class int.class, etc.
     * @param ds the dataset.
     * @return ArrayDayaSet of component type c, e.g. double.class, float.class, etc.
     */
    public static ArrayDataSet maybeCopy( Class c, QDataSet ds ) {
        if ( ds instanceof ArrayDataSet && ((ArrayDataSet)ds).getComponentType()==c ) {
            return (ArrayDataSet)ds;
        } else {
            return copy(c,ds);
        }
    }

    /**
     * provide access to the backing array.
     * @return the backing array for the data. 
     */
    protected abstract Object getBack();
    
    /**
     * return the size of the backing in JvmMemory.
     * @return the backing array for the data. 
     */
    protected abstract int getBackJvmMemory();
    
    /**
     * return a copy of the backing array, to support ArrayDataSet.copy.
     * @return a copy of the array for the data.
     */
    protected abstract Object getBackCopy();
    
    /**
     * reset the back to this new backing array.
     * @param back the new backing array.
     */
    protected abstract void setBack(Object back);

    /**
     * make a copy of the array dataset.
     * @param ds the dataset to copy.
     * @return a copy of the dataset.
     */
    private static ArrayDataSet internalCopy(ArrayDataSet ds) {

        Object newback = ds.getBackCopy();

        ArrayDataSet result = ArrayDataSet.create(ds.rank, ds.len0, ds.len1, ds.len2, ds.len3, newback);
        result.properties.putAll( Ops.copyProperties(ds) ); // TODO: problems...
        result.checkFill();

        return result;
    }

    /**
     * Copy to array of specific type.  For example, copy( double.class, ds ) 
     * would return a copy in a DDataSet.
     * @param c  the primitive type to use (e.g. double.class, float.class, int.class).
     * @param ds the data to copy.
     * @return ArrayDataSet of specific type.
     */
    public static ArrayDataSet copy( Class c, QDataSet ds ) {

        if ( ds instanceof ArrayDataSet && ((ArrayDataSet)ds).getBack().getClass().getComponentType()==c ) return internalCopy( (ArrayDataSet)ds );
        
        int rank= ds.rank();
        ArrayDataSet result;

        switch (rank) {
            case 0:
                result= createRank0( c );
                result.putValue( ds.value() );
                break;
            case 1:
                result= createRank1( c, ds.length() );
                for ( int i=0; i<ds.length(); i++ ) {
                    result.putValue( i, ds.value(i) );
                }
                break;
            case 2:
                result= createRank2( c, ds.length(), ds.length(0) );
                int i0= ds.length()>0 ? ds.length(0) : -1;
                for ( int i=0; i<ds.length(); i++ ) {
                    if ( ds.length(i)!=i0 ) throw new IllegalArgumentException("Attempt to copy non-qube into ArrayDataSet which must be qube: "+ds );
                    for ( int j=0; j<ds.length(i); j++ ) {
                        result.putValue( i, j, ds.value(i,j) );
                    }
                }
                break;
            case 3:
                result= createRank3( c, ds.length(), ds.length(0), ds.length(0,0) );
                int i0_= ds.length()>0 ? ds.length(0) : -1;
                for ( int i=0; i<ds.length(); i++ ) {
                    if ( ds.length(i)!=i0_ ) throw new IllegalArgumentException("Attempt to copy non-qube into ArrayDataSet which must be qube: "+ds );
                    for ( int j=0; j<ds.length(i); j++ ) {
                        for ( int k=0; k<ds.length(i,j); k++ ) {
                            result.putValue( i, j, k, ds.value(i,j,k) );
                        }
                    }
                }
                break;
            case 4:
                result = createRank4( c, ds.length(), ds.length(0), ds.length(0,0), ds.length(0,0,0));
                for ( int i=0; i<ds.length(); i++ )
                    for ( int j=0; j<ds.length(i); j++ )
                        for ( int k=0; k<ds.length(i,j); k++ )
                            for ( int l=0; l<ds.length(i,j,k); l++ )
                                result.putValue( i, j, k, l, ds.value(i,j,k,l));
                break;

            default: 
                throw new IllegalArgumentException("bad rank");
        }
        result.properties.putAll( Ops.copyProperties(ds) );
        result.checkFill();

        return result;

    }

    /**
     * copies the dataset into a writable ArrayDataSet, and all of its depend 
     * datasets as well. Note this does not verify that the data is a qube!!!
     * @param ds the data to be copied.
     * @return a copy of the data.
     */
    public static ArrayDataSet copy( QDataSet ds ) {
        //TODO: this should check that the data is a qube.
        if ( ds instanceof ArrayDataSet ) {
            return internalCopy( (ArrayDataSet)ds );
        } else if ( ds instanceof JoinDataSet && ds.length()>0 ) {
            QDataSet ds1= ds.slice(0);
            if ( ds1 instanceof ArrayDataSet ) { // Juno/Waves needed to save memory and avoid converting everything to doubles
                Class c= ((ArrayDataSet)ds1).getBack().getClass().getComponentType();
                return copy( c, ds );
            } else {
                return copy( guessBackingStore(ds), ds );
            }
        } else {
            return copy( guessBackingStore(ds), ds ); // strange type does legacy behavior.
        }
    }

    /**
     * guess the type of the backing store, returning double.class
     * if it cannot be determined.
     * @param ds the dataset
     * @return the backing store class, one of double.class, float.class, etc.
     */
    public static Class guessBackingStore( QDataSet ds ) {
        if ( ds instanceof BDataSet || ds instanceof ByteDataSet ) {
            return byte.class;
        } else if ( ds instanceof SDataSet || ds instanceof ShortDataSet ) {
            return short.class;
        } else if ( ds instanceof IDataSet || ds instanceof IntDataSet ) {
            return int.class;
        } else if ( ds instanceof FDataSet || ds instanceof FloatDataSet ) {
            return float.class;
        } else {
            return double.class;
        }
    }
    
    
    /**
     * check for fill property and set local variable.
     */
    protected void checkFill() {
        Number f= (Number) properties.get(QDataSet.FILL_VALUE);
        if ( f!=null ) {
            fill= f.floatValue();
            dfill= f.doubleValue();
        } else {
            fill= Float.NaN;
            dfill= Double.NaN;
        }
    }

    /**
     * append the second dataset onto this dataset.  The two datasets need 
     * only have convertible units, so for example two time arrays may be appended 
     * even if their units don't have the same base, and the times will be
     * converted.  Only properties of the two datasets that do not change are preserved.
     * @param ds1 rank N dataset
     * @param ds2 rank N dataset of the same type and compatible geometry as ds1.
     * @return dataset with the data appended.
     */
    public static ArrayDataSet append( ArrayDataSet ds1, ArrayDataSet ds2 ) {
        if ( ds1==null ) return ds2;
        if ( ds2==null ) throw new NullPointerException("ds is null");
        
        if ( ds1.rank()==ds2.rank()-1 ) {
            Units u= SemanticOps.getUnits(ds1);
            ds1= ArrayDataSet.create( ds1.rank()+1, 1, ds1.len0, ds1.len1, ds1.len2, ds1.getBack() );
            ds1.putProperty( QDataSet.UNITS,u);
        }
        if ( ds1.rank()-1==ds2.rank() ) {
            Units u= SemanticOps.getUnits(ds2);
            ds2= ArrayDataSet.create( ds2.rank()+1, 1, ds2.len0, ds2.len1, ds2.len2, ds2.getBack() );
            ds2.putProperty( QDataSet.UNITS,u);
        }
        if ( ds2.rank()!=ds1.rank ) throw new IllegalArgumentException("rank mismatch");
        if ( ds2.len1!=ds1.len1 ) throw new IllegalArgumentException("len1 mismatch");
        if ( ds2.len2!=ds1.len2 ) throw new IllegalArgumentException("len2 mismatch");
        if ( ds2.len3!=ds1.len3 ) throw new IllegalArgumentException("len3 mismatch");
        if ( ds1.getBack().getClass()!=ds2.getBack().getClass() ) {
            throw new IllegalArgumentException("backing type mismatch");
        }

        int myLength= ds1.len0 * ds1.len1 * ds1.len2 * ds1.len3;
        int dsLength= ds2.len0 * ds2.len1 * ds2.len2 * ds2.len3;

        Object newback= Array.newInstance( ds1.getBack().getClass().getComponentType(), myLength + dsLength );

        System.arraycopy( ds1.getBack(), 0, newback, 0, myLength );
        System.arraycopy( ds2.getBack(), 0, newback, myLength, dsLength );

        if ( SemanticOps.isBundle(ds1) && SemanticOps.isBundle(ds2) ) {
            QDataSet bds1= (QDataSet) ds1.property(QDataSet.BUNDLE_1);
            QDataSet bds2= (QDataSet) ds2.property(QDataSet.BUNDLE_1);
            for ( int j=0; j<ds1.length(0); j++ ) {
                Units u1= (Units) bds1.property(QDataSet.UNITS,j);
                if ( u1==null ) u1= Units.dimensionless;
                Units u2= (Units) bds2.property(QDataSet.UNITS,j);
                if ( u2==null ) u2= Units.dimensionless;
                if ( u1!=u2 ) { 
                    logger.log(Level.WARNING, "unable to properly append two bundle datasets with different units: \"{0}\" \"{1}\"", new Object[]{u1, u2});
                }
            }
        }
        
        Units u1= SemanticOps.getUnits(ds1);
        Units u2= SemanticOps.getUnits(ds2);
        if ( u1!=u2 ) {
            UnitsConverter uc= UnitsConverter.getConverter(u2,u1);
            Class backClass= ds1.getBack().getClass().getComponentType();
            for ( int i=myLength; i<myLength+dsLength; i++ ) { //TODO: this is going to be sub-optimal that its much slower than it needs to be because if statements.
                Number nv=  uc.convert(Array.getDouble( newback,i) ) ;
                if ( backClass==double.class ) {
                    Array.set( newback, i, nv.doubleValue() );
                } else if ( backClass==float.class ) {
                    Array.set( newback, i, nv.floatValue() );
                } else if ( backClass==long.class ) {
                    Array.set( newback, i, nv.longValue() );
                } else if ( backClass==int.class ) {
                    Array.set( newback, i, nv.intValue() );
                } else if ( backClass==short.class ) {
                    Array.set( newback, i, nv.shortValue() );
                } else if ( backClass==byte.class ) {
                    Array.set( newback, i, nv.byteValue() );
                } else {
                    throw new IllegalArgumentException("unsupported type: "+backClass );
                }
            }
        }

        int len0= ds1.len0 + ds2.len0;
        
        ArrayDataSet result= create( ds1.rank, len0, ds1.len1, ds1.len2, ds1.len3, newback );

        result.properties.putAll( joinProperties( ds1, ds2 ) );
        result.properties.put( QDataSet.UNITS, u1 ); // since we resolve units when they change (bug 3469219)

        return result;
    }


    /**
     * join the properties of the two datasets.  This was introduced to
     * support append, but may be useful in other contexts.  Note there is special
     * handling of properties, like CACHE_TAG and TYPICAL_MIN, TYPICAL_MAX.
     * Note MONOTONIC assumes the ds2 will be added after ds1.
     * @param ds1 dataset, typically a time series
     * @param ds2 dataset, typically a time serirs
     * @return the two sets combined.
     */
    protected static Map joinProperties( ArrayDataSet ds1, ArrayDataSet ds2 ) {
        Map result= new HashMap();
        for ( int i=0; i<ds2.rank(); i++ ) {
            QDataSet thatDep= (QDataSet) ds2.property( "DEPEND_"+i );
            if ( thatDep!=null && ( i==0 || thatDep.rank()>1 ) ) {
                QDataSet thisDep= (QDataSet) ds1.property( "DEPEND_"+i );
                ArrayDataSet djoin= copy( thisDep ); //TODO: reconcile types
                ArrayDataSet ddep1= thatDep instanceof ArrayDataSet ? (ArrayDataSet) thatDep : maybeCopy( thatDep );
                djoin= append( djoin, ddep1 );
                result.put( "DEPEND_"+i, djoin );

            } else if ( thatDep!=null && thatDep.rank()==1 ) {
                //TODO: check properties equal.
                result.put( "DEPEND_"+i, thatDep );
            }
            QDataSet thatBundle= (QDataSet) ds2.property( "BUNDLE_"+i );
            QDataSet thisBundle= (QDataSet) ds1.property("BUNDLE_"+i );
            if ( i>0 && thatBundle!=null && thisBundle!=null ) {
                if ( thisBundle.length()!=thatBundle.length() ) {
                    throw new IllegalArgumentException("BUNDLE_"+i+" should be the same length to append, but they are not");
                }
                for ( int j=0; j<thatBundle.length(); j++ ) {
                    Units thatu= (Units)thatBundle.property( QDataSet.UNITS, j );
                    Units thisu= (Units)thisBundle.property( QDataSet.UNITS, j );
                    if ( thisu!=thatu ) {
                        throw new IllegalArgumentException("units in BUNDLE_"+i+" change...");
                    }
                }
                //TODO: other safety checks...
                result.put( "BUNDLE_"+i, thatBundle );
            }
        }
        String[] props;
        props= DataSetUtil.correlativeProperties();
        for ( int iprop= -1; iprop<props.length; iprop++ ) {
            String prop= iprop==-1 ? QDataSet.PLANE_0 : props[iprop];
            QDataSet w1= (QDataSet) ds2.property( prop );
            if ( w1!=null ) {
                QDataSet dep0= (QDataSet) ds1.property( prop );
                if ( dep0!=null ) {
                    ArrayDataSet djoin=  copy( dep0 );
                    ArrayDataSet dd1=  maybeCopy(w1);
                    djoin= append( djoin, dd1 );
                    result.put( prop, djoin );
                } else {
                    logger.log(Level.INFO, "dataset doesn''t have property \"{0}\" but other dataset does: {1}", new Object[]{prop, ds1});
                }
            }
        }

        props= DataSetUtil.dimensionProperties();
        for (String prop : props) {
            Object value= ds1.property(prop);
            if ( value!=null && value.equals(ds2.property(prop) ) ) {
                result.put( prop, ds1.property(prop) );
            }
        }
        // special handling for QDataSet.CADENCE, and QDataSet.MONOTONIC
        props= new String[] { QDataSet.CADENCE, QDataSet.BINS_1 };
        for (String prop : props) {
            Object o = ds1.property(prop);
            if (o!=null && o.equals(ds2.property(prop))) {
                result.put(prop, o);
            }
        }

        // special handling for monotonic property.
        Boolean m= (Boolean) ds1.property( QDataSet.MONOTONIC );
        if ( m!=null && m.equals(Boolean.TRUE) && m.equals( ds2.property( QDataSet.MONOTONIC ) ) ) {
            // check to see that result would be monotonic
            try {
                int[] fl1= DataSetUtil.rangeOfMonotonic( ds1 );
                int[] fl2= DataSetUtil.rangeOfMonotonic( ds2 );
                Units u1= SemanticOps.getUnits(ds2);
                Units u2= SemanticOps.getUnits(ds1);
                UnitsConverter uc= u2.getConverter(u1);
                if ( ds2.value(fl2[0]) -  uc.convert( ds1.value(fl1[1]) ) >= 0 ) { 
                    result.put( QDataSet.MONOTONIC, Boolean.TRUE );
                }
            } catch ( IllegalArgumentException ex ) {
                logger.fine("rte_1282463981: can't show that result has monotonic timetags because each dataset is not monotonic.");
            }
        }

        // special handling for cacheTag property.
        org.das2.datum.CacheTag ct0= (CacheTag) ds1.property( QDataSet.CACHE_TAG );
        org.das2.datum.CacheTag ct1= (CacheTag) ds2.property( QDataSet.CACHE_TAG );
        if ( ct0!=null && ct1!=null ) {
            // If cache tags are not adjacent, the range between them is included in the new tag.
            CacheTag newTag= null;
            try {
                newTag= CacheTag.append(ct0, ct1);
            } catch ( IllegalArgumentException ex ) {
                logger.fine( "append of two datasets that have CACHE_TAGs and are not adjacent, dropping CACHE_TAG" );
            }
            if ( newTag!=null ) {
                result.put( QDataSet.CACHE_TAG, newTag );
            }
        }

        // special handling of TYPICAL_MIN _MAX properties
        Number dmin0= (Number) ds1.property(QDataSet.TYPICAL_MIN );
        Number dmax0= (Number) ds1.property(QDataSet.TYPICAL_MAX );
        Number dmin1= (Number) ds2.property(QDataSet.TYPICAL_MIN );
        Number dmax1= (Number) ds2.property(QDataSet.TYPICAL_MAX );
        if ( dmin0!=null && dmin1!=null ) result.put( QDataSet.TYPICAL_MIN, Math.min( dmin0.doubleValue(), dmin1.doubleValue() ) );
        if ( dmax0!=null && dmax1!=null ) result.put( QDataSet.TYPICAL_MAX, Math.max( dmax0.doubleValue(), dmax1.doubleValue() ) );

        return result;
    }

    /**
     * set the units for this dataset.  This is a convenience method
     * intended to encourage setting the data units.  For example in Jython:
     *<blockquote><pre>
     *ds= linspace(0.,10.,100).setUnits( Units.seconds )
     *</pre></blockquote>
     * @param units units object, like Units.seconds
     * @return this dataset.
     */
    public QDataSet setUnits( Units units ) {
        checkImmutable();        
        this.putProperty( QDataSet.UNITS, units );
        return this;
    }

    @Override
    public String toString( ) {
        return DataSetUtil.toString( this );
    }

    /**
     * returns the size of the dataset in bytes.
     * @return the size of the dataset in bytes.
     * @see org.autoplot.bufferdataset.BufferDataSet which stores data outside of the JVM.
     */
    public int jvmMemory() {
        return getBackJvmMemory();
    }
    
    
    /**
     * print some info about this ArrayDataSet.
     */
    public void about() {
        System.err.println("== "+this.toString() + "==");
        System.err.println("back is array of "+ this.getComponentType() );
        //QDataSet extent= Ops.extent(this);  // this is occasionally very slow. TODO: investigate
        //System.err.println("extent="+extent);
    }
    
}
