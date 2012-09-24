/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.dataset;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.das2.datum.CacheTag;
import org.das2.datum.Units;
import org.das2.datum.UnitsConverter;
import org.das2.util.LoggerManager;

/**
 * A number of static methods were initially defined in DDataSet, then
 * copied into FDataSet and others when they were made.  This super implementation
 * will parent all such datasets and provide common methods.
 * @author jbf
 */
public abstract class ArrayDataSet extends AbstractDataSet implements WritableDataSet {

    private static final Logger logger= LoggerManager.getLogger("qdataset.array");

    int rank;

    int len0;
    int len1;
    int len2;
    int len3;

    float fill= Float.NaN;
    double dfill= Double.NaN;

    private static final boolean RANGE_CHECK = false;

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

    public static ArrayDataSet createRank1( Class c, int len0 ) {
        if ( c==double.class ) return new DDataSet( 1, len0, 1, 1, 1 );
        if ( c==float.class ) return new FDataSet( 1, len0, 1, 1, 1 );
        if ( c==long.class ) return new LDataSet( 1, len0, 1, 1, 1 );
        if ( c==int.class ) return new IDataSet( 1, len0, 1, 1, 1 );
        if ( c==short.class ) return new SDataSet( 1, len0, 1, 1, 1 );
        if ( c==byte.class ) return new BDataSet( 1, len0, 1, 1, 1 );
        throw new IllegalArgumentException("class not supported: "+c);
    }

    public static ArrayDataSet createRank2( Class c, int len0, int len1 ) {
        if ( c==double.class ) return new DDataSet( 2, len0, len1, 1, 1 );
        if ( c==float.class ) return new FDataSet( 2, len0, len1, 1, 1 );
        if ( c==long.class ) return new LDataSet( 2, len0, len1, 1, 1 );
        if ( c==int.class ) return new IDataSet( 2, len0, len1, 1, 1 );
        if ( c==short.class ) return new SDataSet( 2, len0, len1, 1, 1 );
        if ( c==byte.class ) return new BDataSet( 2, len0, len1, 1, 1 );
        throw new IllegalArgumentException("class not supported: "+c);
    }

    public static ArrayDataSet createRank3( Class c, int len0, int len1, int len2 ) {
        if ( c==double.class ) return new DDataSet( 3, len0, len1, len2, 1 );
        if ( c==float.class ) return new FDataSet( 3, len0, len1, len2, 1 );
        if ( c==long.class ) return new LDataSet( 3, len0, len1, len2, 1 );
        if ( c==int.class ) return new IDataSet( 3, len0, len1, len2, 1 );
        if ( c==short.class ) return new SDataSet( 3, len0, len1, len2, 1 );
        if ( c==byte.class ) return new BDataSet( 3, len0, len1, len2, 1 );
        throw new IllegalArgumentException("class not supported: "+c);
    }

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

    public int rank() {
        return rank;
    }

    @Override
    public final int length() {
        return len0;
    }

    @Override
    public final int length(int i) {
        if ( RANGE_CHECK && i>=len0 ) throw new IndexOutOfBoundsException("length("+i+") when dim 0 length="+len0); //TODO: allow disable with RANGE_CHECK for performance
        return len1;
    }

    @Override
    public final int length( int i0, int i1 ) {
        if ( RANGE_CHECK ) {
            if ( i0>=len0 ) throw new IndexOutOfBoundsException("length("+i0+","+i1+") when dim 0 length="+len0);
            if ( i1>=len1 ) throw new IndexOutOfBoundsException("length("+i0+","+i1+") when dim 1 length="+len1);
        }
        return len2;
    }

    @Override
    public final int length( int i0, int i1, int i2) {
        if ( RANGE_CHECK ) {
            if ( i0>=len0 ) throw new IndexOutOfBoundsException("length("+i0+","+i1+","+i2+") when dim 0 length="+len0);
            if ( i1>=len1 ) throw new IndexOutOfBoundsException("length("+i0+","+i1+","+i2+") when dim 1 length="+len1);
            if ( i2>=len2 ) throw new IndexOutOfBoundsException("length("+i0+","+i1+","+i2+") when dim 2 length="+len2);
        }
        return len3;
    }

    public Class getComponentType() {
        return getBack().getClass().getComponentType();
    }

    /**
     * Shorten the dataset by changing it's dim 0 length parameter.  The same backing array is used,
     * so the element that remain will be the same.
     * can only shorten!
     */
    public void putLength( int len ) {
        int limit= Array.getLength( getBack() ) / ( len1*len2*len3 );
        if ( len>limit ) throw new IllegalArgumentException("dataset cannot be lengthened");
        len0= len;
    }

    /**
     * grow the internal store so that append may be used to resize the dataset.
     * @param newRecCount
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
        
        this.len0= this.len0 + ds.len0;

        properties.putAll( joinProperties( this, ds ) ); //TODO: verify

    }

    /**
     * return true if the dataset can be appended.  Note this assumes that the
     * same length, etc.  This just checks that we have the number of spare records
     * in the backing store.
     * @param ds
     * @return
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
     * copies the properties, copying depend datasets as well.
     */
    private static Map copyProperties( QDataSet ds ) {
        Map result = new HashMap();
        Map srcProps= DataSetUtil.getProperties(ds);

        result.putAll(srcProps);

        for ( int i=0; i < ds.rank(); i++) {
            QDataSet dep = (QDataSet) ds.property("DEPEND_" + i);
            if (dep == ds) {
                throw new IllegalArgumentException("dataset is dependent on itsself!");
            }
            if (dep != null) {
                result.put("DEPEND_" + i, copy(dep) ); // for timetags
            }
        }

        for (int i = 0; i < QDataSet.MAX_PLANE_COUNT; i++) {
            QDataSet plane0 = (QDataSet) ds.property("PLANE_" + i);
            if (plane0 != null) {
                result.put("PLANE_" + i, copy(plane0));
            } else {
                break;
            }
        }

        return result;
    }

    /**
     * Copy the dataset to an ArrayDataSet only if the dataset is not already an ArrayDataSet.
     * @param ds
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
     * particular instance of ArrayDataSet.
     * @param ds
     * @return ArrayDayaSet of component type c.
     */
    public static ArrayDataSet maybeCopy( Class c, QDataSet ds ) {
        if ( ds instanceof ArrayDataSet && ((ArrayDataSet)ds).getComponentType()==c ) {
            return (ArrayDataSet)ds;
        } else {
            return copy(ds);
        }
    }

    /**
     * provide access to the backing array.
     * @return
     */
    protected abstract Object getBack();
    protected abstract void setBack(Object back);

    private static ArrayDataSet ddcopy(ArrayDataSet ds) {
        int dsLength = ds.len0 * ds.len1 * ds.len2 * ds.len3;

        Object newback = Array.newInstance( ds.getBack().getClass().getComponentType(), dsLength);

        System.arraycopy( ds.getBack(), 0, newback, 0, dsLength );

        ArrayDataSet result = ArrayDataSet.create(ds.rank, ds.len0, ds.len1, ds.len2, ds.len3, newback);
        result.properties.putAll(copyProperties(ds)); // TODO: problems...

        return result;
    }

    /**
     * Copy to array of specific type.  For example, copy( double.class, ds ) would return a copy
     * in a DDataSet.
     * @param c  the primitive type to use (e.g. double.class).
     * @param ds the data to copy.
     * @return ArrayDataSet of specific type.
     */
    public static ArrayDataSet copy( Class c, QDataSet ds ) {

        if ( ds instanceof ArrayDataSet && ((ArrayDataSet)ds).getBack().getClass().getComponentType()==c ) return ddcopy( (ArrayDataSet)ds );
        
        int rank= ds.rank();
        ArrayDataSet result;

        switch (rank) {
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

            default: throw new IllegalArgumentException("bad rank");
        }
        result.properties.putAll( copyProperties(ds) );
        result.checkFill();

        return result;

    }

    /**
     * copies the dataset into a writable ArrayDataSet, and all of its depend datasets as well.
     */
    public static ArrayDataSet copy( QDataSet ds ) {
        if ( ds instanceof ArrayDataSet ) {
            return ddcopy( (ArrayDataSet)ds );
        } else if ( ds instanceof JoinDataSet && ds.length()>0 ) {
            QDataSet ds1= ds.slice(0);
            if ( ds1 instanceof ArrayDataSet ) { // Juno/Waves needed to save memory and avoid converting everything to doubles
                Class c= ((ArrayDataSet)ds1).getBack().getClass().getComponentType();
                return copy( c, ds );
            } else {
                return copy( double.class, ds );
            }
        } else {
            return copy( double.class, ds ); // strange type does legacy behavior.
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
     * append the second dataset onto this dataset.  The two datasets need only have convertible units, so for example
     * two time arrays may be appended even if their units don't have the same base.  
     * Only properties of the two datasets that do not change are preserved.
     * @param ths rank N dataset
     * @param ds rank N dataset of the same type and geometry as ths.
     */
    public static ArrayDataSet append( ArrayDataSet ths, ArrayDataSet ds ) {
        if ( ds.rank()!=ths.rank ) throw new IllegalArgumentException("rank mismatch");
        if ( ds.len1!=ths.len1 ) throw new IllegalArgumentException("len1 mismatch");
        if ( ds.len2!=ths.len2 ) throw new IllegalArgumentException("len2 mismatch");
        if ( ds.len3!=ths.len3 ) throw new IllegalArgumentException("len3 mismatch");
        if ( ths.getBack().getClass()!=ds.getBack().getClass() ) throw new IllegalArgumentException("backing type mismatch");

        int myLength= ths.len0 * ths.len1 * ths.len2 * ths.len3;
        int dsLength= ds.len0 * ds.len1 * ds.len2 * ds.len3;

        Object newback= Array.newInstance( ths.getBack().getClass().getComponentType(), myLength + dsLength );

        System.arraycopy( ths.getBack(), 0, newback, 0, myLength );
        System.arraycopy( ds.getBack(), 0, newback, myLength, dsLength );

        Units u1= SemanticOps.getUnits(ths);
        Units u2= SemanticOps.getUnits(ds);
        if ( u1!=u2 ) {
            UnitsConverter uc= UnitsConverter.getConverter(u2,u1);
            Class backClass= ths.getBack().getClass().getComponentType();
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

        int len0= ths.len0 + ds.len0;
        
        ArrayDataSet result= create( ths.rank, len0, ths.len1, ths.len2, ths.len3, newback );

        result.properties.putAll( joinProperties( ths, ds ) );
        result.properties.put( QDataSet.UNITS, u1 ); // since we resolve units when they change (bug 3469219)

        return result;
    }


    /**
     * join the properties of the two datasets.
     * Note MONOTONIC assumes the ds will be added after ths.
     * @param ds
     */
    protected static Map joinProperties( ArrayDataSet ths, ArrayDataSet ds ) {
        Map result= new HashMap();
        for ( int i=0; i<ds.rank(); i++ ) {
            QDataSet thatDep= (QDataSet) ds.property( "DEPEND_"+i );
            if ( thatDep!=null && ( i==0 || thatDep.rank()>1 ) ) {
                QDataSet thisDep= (QDataSet) ths.property( "DEPEND_"+i );
                ArrayDataSet djoin= copy( thisDep ); //TODO: reconcile types
                ArrayDataSet ddep1= thatDep instanceof ArrayDataSet ? (ArrayDataSet) thatDep : maybeCopy( thatDep );
                djoin= append( djoin, ddep1 );
                result.put( "DEPEND_"+i, djoin );

            } else if ( thatDep!=null && thatDep.rank()==1 ) {
                //TODO: check properties equal.
                result.put( "DEPEND_"+i, thatDep );
            }
        }
        QDataSet dep1= (QDataSet) ds.property( QDataSet.PLANE_0 );
        if ( dep1!=null ) {
            QDataSet dep0= (QDataSet) ths.property( QDataSet.PLANE_0 );
            ArrayDataSet djoin=  copy( dep0 );
            ArrayDataSet dd1=  maybeCopy(dep1);
            djoin= append( djoin, dd1 );
            result.put( QDataSet.PLANE_0, djoin );
        }
        String[] props= DataSetUtil.dimensionProperties();
        for ( int i=0; i<props.length; i++ ) {
            String prop= props[i];
            Object value= ths.property(prop);
            if ( value!=null && value.equals(ds.property(prop) ) ) {
                result.put( prop, ths.property(prop) );
            }
        }
        // special handling for QDataSet.CADENCE, and QDataSet.MONOTONIC
        RankZeroDataSet o= (RankZeroDataSet) ths.property(QDataSet.CADENCE);
        if ( o!=null && o.equals( ds.property(QDataSet.CADENCE) ) ) {
            result.put( QDataSet.CADENCE, o );
        }

        // special handling for monotonic property.
        Boolean m= (Boolean) ths.property( QDataSet.MONOTONIC );
        if ( m!=null && m.equals(Boolean.TRUE) && m.equals( ds.property( QDataSet.MONOTONIC ) ) ) {
            // check to see that result would be monotonic
            int[] fl1= DataSetUtil.rangeOfMonotonic( ths );
            int[] fl2= DataSetUtil.rangeOfMonotonic( ds );
            Units u1= SemanticOps.getUnits(ds);
            Units u2= SemanticOps.getUnits(ths);
            UnitsConverter uc= u2.getConverter(u1);
            if ( ds.value(fl2[0]) -  uc.convert( ths.value(fl1[1]) ) >= 0 ) { 
                result.put( QDataSet.MONOTONIC, Boolean.TRUE );
            }
        }

        // special handling for cacheTag property.
        org.das2.datum.CacheTag ct0= (CacheTag) ths.property( QDataSet.CACHE_TAG );
        org.das2.datum.CacheTag ct1= (CacheTag) ds.property( QDataSet.CACHE_TAG );
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
        Double dmin0= (Double) ths.property(QDataSet.TYPICAL_MIN );
        Double dmax0= (Double) ths.property(QDataSet.TYPICAL_MAX );
        Double dmin1= (Double) ths.property(QDataSet.TYPICAL_MIN );
        Double dmax1= (Double) ths.property(QDataSet.TYPICAL_MAX );
        if ( dmin0!=null && dmin1!=null ) result.put( QDataSet.TYPICAL_MIN, Math.min( dmin0, dmin1 ) );
        if ( dmax0!=null && dmax1!=null ) result.put( QDataSet.TYPICAL_MAX, Math.max( dmin0, dmin1 ) );

        return result;
    }

    public QDataSet setUnits( Units units ) {
        this.putProperty( QDataSet.UNITS, units );
        return this;
    }

    @Override
    public String toString( ) {
        return DataSetUtil.toString( this );
    }

    /**
     * returns the size of the dataset in bytes.
     * @return
     */
    public int jvmMemory() {
        int sizePer;
        Class component= this.getComponentType();
        if ( component==double.class ) {
            sizePer= 8;
        } else if ( component==float.class ) {
            sizePer= 4;
        } else if ( component==long.class ) {
            sizePer= 8;
        } else if ( component==int.class ) {
            sizePer= 4;
        } else if ( component==short.class ) {
            sizePer= 2;
        } else if ( component==byte.class ) {
            sizePer= 1;
        } else {
            throw new IllegalArgumentException("not supported "+component );
        }
        return Array.getLength( this.getBack() ) * sizePer;
    }
}
