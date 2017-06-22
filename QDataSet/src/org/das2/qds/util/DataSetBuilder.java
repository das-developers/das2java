/*
 * DataSetBuilder.java
 *
 * Created on May 25, 2007, 7:04 AM
 *
 */

package org.das2.qds.util;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.datum.Datum;
import org.das2.datum.EnumerationUnits;
import org.das2.datum.Units;
import org.das2.util.LoggerManager;
import org.das2.qds.ArrayDataSet;
import org.das2.qds.DDataSet;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;
import org.das2.qds.ops.Ops;

/**
 * allows dataset of unknown length to be built. Presently, this only builds QUBES, but should allow for geometry changes.
 * TODO: consider using WritableDataSet interface.
 * The guessRecCount parameter is the initial number of allocated records, and is also the extension when this number of
 * records is passed.  The final physical dataset size is not affected by this, because the data is copied.
 * @author jbf
 */
public class DataSetBuilder { 
    
    private static final Logger logger= LoggerManager.getLogger("qdataset.util.dsb");
            
    int rank;
    
    ArrayList<DDataSet> finished;
    DDataSet current;
    int recCount;
    int dim1, dim2, dim3;
    int recElements; // number of elements per record
    int index;
    int offset;
    int length;  // number of records or partial records written
    HashMap<String,Object> properties;
    private HashMap<String,String> unresolvedPropertyTypes;
    private HashMap<String,String> unresolvedPropertyValues;

    Units u= null;
    Units[] us= null; // for Rank 1 bundles
    String[] labels= null; // for Rank 1 bundles
    String[] names= null; // for Rank 1 bundles
    boolean isBundle= false;

    public static final String UNRESOLVED_PROP_QDATASET= "qdataset";
    
    /**
     * Create a new builder for a rank 0 dataset.
     * @param rank the number of indeces of the result dataset.
     */
    public DataSetBuilder( int rank ) {
        this( rank, 0, 1, 1 );
        if ( rank!=0 ) throw new IllegalArgumentException( "rank must be 0 for one-arg DataSetBuilder call");
    }
    
    /**
     * Create a new builder for a rank 1 dataset.
     * guessRecCount is the guess of dim0 size.  Bad guesses will simply result in an extra array copy.
     * @param rank must be 1.
     * @param guessRecCount initial allocation for the first dimension.
     */
    public DataSetBuilder( int rank, int guessRecCount ) {
        this( rank, guessRecCount, 1, 1 );
        if ( rank>1 ) throw new IllegalArgumentException( String.format( "rank %d dataset when dim1 not specified.", rank ) );
        if ( rank!=1 ) throw new IllegalArgumentException( "rank must be 1 for two-arg DataSetBuilder call");
    }
    
    /**
     * Create a new builder for a rank 2 dataset.
     * guessRecCount is the guess of dim0 size.  Bad guesses will simply result in an extra array copy.
     * @param rank must be 2.
     * @param guessRecCount initial allocation for the first dimension.
     * @param dim1 fixed size of the second index.
     */
    public DataSetBuilder( int rank, int guessRecCount, int dim1 ) {
        this( rank, guessRecCount, dim1, 1 );
        if ( rank>2 ) throw new IllegalArgumentException(String.format( "rank %d dataset when dim2 not specified.", rank ) );
        if ( rank!=2 ) throw new IllegalArgumentException( "rank must be 2 for three-arg DataSetBuilder call");        
    }
    
    /**
     * Create a new builder for a rank 3 dataset.
     * guessRecCount is the guess of dim0 size.  Bad guesses will simply result in an extra array copy.
     * @param rank must be 3.
     * @param guessRecCount initial allocation for the first dimension.
     * @param dim1 fixed size of the second index.
     * @param dim2 fixed size of the third index.
     */
    public DataSetBuilder( int rank, int guessRecCount, int dim1, int dim2 ) {
        this.rank= rank;
        this.recCount= guessRecCount;
        this.dim1= dim1;
        this.dim2= dim2;
        this.recElements= dim1 * dim2;
        newCurrent();
        index=0;
        properties= new HashMap<>();
        unresolvedPropertyValues= new HashMap<>();
        unresolvedPropertyTypes= new HashMap<>();
    }

    /**
     * Create a new builder for a rank 3 dataset.
     * guessRecCount is the guess of dim0 size.  Bad guesses will simply result in an extra array copy.
     * @param rank must be 3.
     * @param guessRecCount initial allocation for the first dimension.
     * @param dim1 fixed size of the second index.
     * @param dim2 fixed size of the third index.
     * @param dim3 fixed size of the fourth index.
     */
    public DataSetBuilder( int rank, int guessRecCount, int dim1, int dim2, int dim3 ) {
        this.rank= rank;
        this.recCount= guessRecCount;
        this.dim1= dim1;
        this.dim2= dim2;
        this.dim3= dim3;
        this.recElements= dim1 * dim2 * dim3;
        newCurrent();
        index=0;
        properties= new HashMap<String,Object>();
    }
    
    /**
     * check the stream index specified.  If it's -1, that indicates that the builder
     * should keep track of the index and nextRecord() will be used to explicitly
     * increment the index.  If it is not -1, then it must either be equal to the
     * current position, or equal to the position + 1, which is implicitly a nextRecord().
     * @param index0
     * @throws java.lang.IllegalArgumentException if the index doesn't follow these rules.
     */
    private void checkStreamIndex(int index0) throws IllegalArgumentException {
        if (index0 > -1) {
            if (index0 == index + offset) {
                
            } else if ( index0 == index + offset + 1 ) {
                nextRecord();
            } else {
                throw new IllegalArgumentException("index0 must only increment by one");
            }
        }
        length= index + offset + 1;
    }
    
    private void newCurrent() {
        logger.log(Level.FINE, "creating rank {0} receiver for next {1} records", new Object[] { rank, recCount } );
        if ( rank==1 ) {
            current= DDataSet.createRank1( recCount );
        } else if ( rank==2 ) {
            current= DDataSet.createRank2( recCount, dim1 );
        } else if ( rank==3 ) {
            current= DDataSet.createRank3( recCount, dim1, dim2 );
        } else if ( rank==4 ) {
            current= DDataSet.createRank4( recCount, dim1, dim2, dim3 );
        }
    }


    /**
     * for index0==-1, return the last value entered into the rank 1 dataset.
     * @param index0
     * @throws IllegalArgumentException if the index is not -1
     * @throws IllegalArgumentException if nothing is yet written to the builder.
     * @return
     */

    public double getValue( int index0 ) {
        if ( index0==-1 ) { // returns the last value
            if ( this.index==0 ) {
                throw new IllegalArgumentException("nothing written to builder yet");
            }
            return current.value(this.index-1);
        } else {
            throw new IllegalArgumentException("index must be -1");
        }
    }
    /**
     * insert a value into the builder.
     * @param index0 The index to insert the data, or if -1, ignore and nextRecord() should be used.
     * @param d the value to insert.
     */
    public void putValue( int index0, double d ) {
        checkStreamIndex(index0);
        current.putValue( this.index, d );
    }
    
    /**
     * insert a value into the builder.
     * @param index0 The index to insert the data, or if -1, ignore and nextRecord() should be used.
     * @param index1 the second index
     * @param d the value to insert.
     */
    public void putValue( int index0, int index1, double d ) {
        checkStreamIndex(index0);
        current.putValue( this.index, index1, d );
    }
    
    /**
     * insert a value into the builder.
     * @param index0 The index to insert the data, or if -1, ignore and nextRecord() should be used.
     * @param index1 the second index
     * @param index2 the third index
     * @param d the value to insert.
     */
    public void putValue( int index0, int index1, int index2, double d ) {
        checkStreamIndex(index0);
        current.putValue( this.index, index1, index2, d );
    }

    /**
     * insert a value into the builder.
     * @param index0 The index to insert the data, or if -1, ignore and nextRecord() should be used.
     * @param index1 the second index
     * @param index2 the third index
     * @param index3 the third index
     * @param d the value to insert.
     */
    public void putValue( int index0, int index1, int index2, int index3, double d ) {
        checkStreamIndex(index0);
        current.putValue( this.index, index1, index2, index3, d );
    }
    
    /**
     * insert a value into the builder.
     * @param index0 The index to insert the data, or if -1, ignore and nextRecord() should be used.
     * @param d the value to insert.
     */
    public void putValue( int index0, Datum d ) {
        checkStreamIndex(index0);
        if ( rank!=1 ) throw new IllegalArgumentException("rank 1 putValue used with rank "+rank+" dataset");
        if ( u==null ) u= d.getUnits();
        current.putValue( this.index, d.doubleValue(u) );
    }
    
    /**
     * insert a value into the builder.
     * @param index0 The index to insert the data, or if -1, ignore and nextRecord() should be used.
     * @param index1 the second index
     * @param d the value to insert.
     */
    public void putValue( int index0, int index1, Datum d ) {
        checkStreamIndex(index0);
        if ( rank!=2 ) throw new IllegalArgumentException("rank 2 putValue used with rank "+rank+" dataset");
        if ( us==null || us[index1]==null ) {
            setUnits(index1, d.getUnits());
        }
        current.putValue( this.index, index1, d.doubleValue(us[index1]) );
    }
    
    /**
     * insert a value into the builder.
     * @param index0 The index to insert the data, or if -1, ignore and nextRecord() should be used.
     * @param index1 the second index
     * @param index2 the third index
     * @param d the value to insert.
     */
    public void putValue( int index0, int index1, int index2, Datum d ) {
        checkStreamIndex(index0);
        if ( rank!=3 ) throw new IllegalArgumentException("rank 3 putValue used with rank "+rank+" dataset");
        if ( u==null ) u= d.getUnits();
        current.putValue( this.index, index1, index2, d.doubleValue(u) );
    }    

    /**
     * insert a value into the builder.
     * @param index0 The index to insert the data, or if -1, ignore and nextRecord() should be used.
     * @param index1 the second index
     * @param index2 the third index
     * @param index3 the fourth index
     * @param d the value to insert.
     */
    public void putValue( int index0, int index1, int index2, int index3, Datum d ) {
        checkStreamIndex(index0);
        if ( rank!=4 ) throw new IllegalArgumentException("rank 4 putValue used with rank "+rank+" dataset");
        if ( u==null ) u= d.getUnits();
        current.putValue( this.index, index1, index2, index3, d.doubleValue(u) );
    }    
        
    /**
     * insert a value into the builder.  Note these do Units checking and are therefore less efficient
     * @param index0 The index to insert the data, or if -1, ignore and nextRecord() should be used.
     * @param d the value to insert.
     */
    public void putValue( int index0, QDataSet d ) {
        checkStreamIndex(index0);
        if ( u==null ) u= SemanticOps.getUnits(d);
        if ( d.rank()!=0 ) throw new IllegalArgumentException("data must be rank 0");
        if ( rank!=1 ) throw new IllegalArgumentException("rank 1 putValue used with rank "+rank+" dataset");        
        double v= d.value();        
        Units lu= SemanticOps.getUnits(d);
        if ( lu!=u ) {
            v= lu.convertDoubleTo( us[index], v );
        }
        current.putValue( this.index, v );
    }
    
    /**
     * insert a value into the builder.  Note these do Units checking and are therefore less efficient
     * @param index0 The index to insert the data, or if -1, ignore and nextRecord() should be used.
     * @param index1 the second index
     * @param d the rank 0 dataset value to insert.
     */
    public void putValue( int index0, int index1, QDataSet d ) {
        checkStreamIndex(index0);
        Units lu= SemanticOps.getUnits(d);
        if ( us==null || us[index1]==null ) { 
            setUnits( index1, lu );
        }
        if ( d.rank()!=0 ) throw new IllegalArgumentException("data must be rank 0");
        if ( rank!=2 ) throw new IllegalArgumentException("rank 2 putValue used with rank "+rank+" dataset");        
        double v= d.value();        
        if ( lu!=us[index1] ) {
            v= lu.convertDoubleTo( us[index1], v );
        }
        String label= (String)d.property(QDataSet.LABEL);
        if ( label!=null && ( labels==null || labels[index1]==null ) ) {
            setLabel(index1,label);
        }
        String name= (String)d.property(QDataSet.NAME);
        if ( name!=null && ( names==null || names[index1]==null ) ) {
            setName(index1,name);
        }

        current.putValue( this.index, index1, v );
    }
    
    /**
     * insert a value into the builder.  Note these do Units checking and are therefore less efficient
     * @param index0 The index to insert the data, or if -1, ignore and nextRecord() should be used.
     * @param index1 the second index
     * @param index2 the third index
     * @param d the value to insert.
     */
    public void putValue( int index0, int index1, int index2, QDataSet d ) {
        checkStreamIndex(index0);
        if ( u==null ) {
            u= SemanticOps.getUnits(d);
        } 
        if ( d.rank()!=0 ) throw new IllegalArgumentException("data must be rank 0");
        if ( rank!=3 ) throw new IllegalArgumentException("rank 3 putValue used with rank "+rank+" dataset");        
        double v= d.value();
        Units lu= SemanticOps.getUnits(d);
        if ( lu!=u ) {
            v= lu.convertDoubleTo( u, v );
        }
        current.putValue( this.index, index1, index2, v );
    }

    /**
     * insert a value into the builder.  Note these do Units checking and are therefore less efficient
     * @param index0 The index to insert the data, or if -1, ignore and nextRecord() should be used.
     * @param index1 the second index
     * @param index2 the third index
     * @param index3 the fourth index
     * @param d the value to insert.
     */
    public void putValue( int index0, int index1, int index2, int index3, QDataSet d ) {
        checkStreamIndex(index0);
        if ( u==null ) {
            u= SemanticOps.getUnits(d);
        } 
        if ( d.rank()!=0 ) throw new IllegalArgumentException("data must be rank 0");
        if ( rank!=4 ) throw new IllegalArgumentException("rank 4 putValue used with rank "+rank+" dataset");        
        double v= d.value();
        Units lu= SemanticOps.getUnits(d);
        if ( lu!=u ) {
            v= lu.convertDoubleTo( u, v );
        }
        current.putValue( this.index, index1, index2, index3, v );
    }
    
    /**
     * insert a value into the builder, parsing the string with the column units.
     * @param index0 The index to insert the data, or if -1, ignore and nextRecord() should be used.
     * @param index1 the second index
     * @param s the a string representation of the value parse and insert.
     * @throws java.text.ParseException
     * @see Ops#dataset(java.lang.Object) for the logic interpreting Strings.
     */
    public void putValue( int index0, int index1, String s ) throws ParseException {
        checkStreamIndex(index0);
        if ( us==null || us[index1]==null ) {
            QDataSet ds1= Ops.dataset(s);
            Units units= SemanticOps.getUnits(ds1);
            setUnits(index1, units );
        }
        if ( us[index1] instanceof EnumerationUnits ) {
            current.putValue( this.index, index1, ((EnumerationUnits)us[index1]).createDatum(s).doubleValue(us[index1]) );            
        } else {
            current.putValue( this.index, index1, us[index1].parse(s).doubleValue(us[index1]) );
        }
    }
    
    
    /**
     * copy the elements from one DDataSet into the builder (which can be done with
     * a system call), ignoring dataset geometry.  TODO: since the element count
     * allows for putting multiple records in at once, an index out of bounds may 
     * occur after the last record of current is written.
     * @param index0 The index to put the values, or -1 for the current position.
     * @param values rank 1 dataset.
     * @param count the number of elements to copy
     */
    public void putValues( int index0, QDataSet values, int count ) {
        DDataSet ddvalues;
        if ( values instanceof DDataSet ) {
            ddvalues= (DDataSet) values;
        } else {
            ddvalues= (DDataSet) ArrayDataSet.copy( double.class, values );
        }
        checkStreamIndex(index0);
        DDataSet.copyElements( ddvalues, 0, current, this.index, count, false );
    }
    
    /**
     * This must be called each time a record is complete.  Note this
     * currently advances to the next record and at this point the next record
     * exists.  In other words, the last call to nextRecord is not required.
     * This logic may change, so that any fields written would be dropped unless 
     * nextRecord is called to commit the record.
     * When -1 is used for the indexes of the streaming dimension, then this
     * will increment the internal counter.
     * TODO: Check for unspecified entries.
     * @see #nextRecord(java.lang.Object...) which inserts all values at once.
     */
    public void nextRecord() {
        index++;
        if ( index == current.length() ) {
            if ( finished==null ) finished= new ArrayList<DDataSet>(4);
            finished.add( current );
            offset += current.length();
            index -= current.length();
            newCurrent();
        }
    }
    
    /**
     * In one step, specify all the values of the record and advance the counter.
     * This is intended to reduce the number of lines needed in scripts, and
     * to support Jython where a double array would not be cast to an Object array.
     * Also, columns 1..N-1 are declared dependent on column 0, when column 0 is UT times.
     * @param values the record values.
     * @see #nextRecord(java.lang.Object...) 
     */
    public void nextRecord( double[] values ) {
        if ( values.length>this.dim1 ) {
            throw new IllegalArgumentException("Too many values provided: got "+values.length+", expected "+this.dim1 );
        }
        if ( this.rank!=2 ) {
            throw new IllegalArgumentException("nextRecord called with array but builder is not rank 2");
        }
        for ( int i=0; i<values.length; i++ ) { 
            double v1= values[i];
            putValue( -1, i, v1 );
        } 
        nextRecord();        
    }
    
    /**
     * In one step, specify all the values of the record and advance the counter.
     * This is intended to reduce the number of lines needed in scripts.  In Jython:
     *<blockquote><pre>
     *dsb= DataSetBuilder(2,100,2)
     *dsb.nextRecord( [ '2014-001T00:00', 20. ] )
     *dsb.nextRecord( [ '2014-002T00:00', 21. ] )
     *dsb.nextRecord( [ '2014-003T00:00', 21.4 ] )
     *dsb.nextRecord( [ '2014-004T00:00', 19.7 ] ) 
     *ds= dsb.getDataSet()
     *</pre></blockquote>
     * Also, columns 1..N-1 are declared dependent on column 0, when column 0 is UT times.
     * @param values the record values, in an String, Datum, Rank 0 QDataSet, or Number.
     */
    public void nextRecord( Object ... values ) {
        if ( values.length>this.dim1 ) {
            throw new IllegalArgumentException("Too many values provided: got "+values.length+", expected "+this.dim1 );
        }
        //if ( this.rank!=2 ) {
        //    throw new IllegalArgumentException("nextRecord called with array but builder is not rank 2");
        //}
        for ( int i=0; i<values.length; i++ ) { 
            Object v1= values[i];
            if ( v1 instanceof Number ) {
                putValue( -1, i, ((Number)v1).doubleValue() );
            } else if ( v1 instanceof String ) {
                try {
                    putValue( -1, i, ((String)v1) );
                } catch ( ParseException ex ) {
                    throw new IllegalArgumentException(ex);
                }
            } else if ( v1 instanceof Datum ) {
                putValue( -1, i, (Datum)v1 );
            } else if ( v1 instanceof QDataSet ) {
                putValue( -1, i, (QDataSet)v1 );
            } else {
                throw new IllegalArgumentException("expected String, Datum, or double");
            }
        } 
        nextRecord();
    }
    
    /**
     * rank 0 datasets can be used to build the rank 1 datasets
     * @param v rank 0 dataset
     */
    public void nextRecord( QDataSet v ) {
        if ( this.rank()!=1 ) {
            throw new IllegalArgumentException("must be rank 1");
        }
        if ( v.rank()!=0 ) {
            throw new IllegalArgumentException("argument must be rank 0");
        }
        putValue( -1, (QDataSet)v );
        nextRecord();
    }

    /**
     * add the double to the rank 1 builder.
     * @param v the value
     */
    public void nextRecord( double v ) {
        if ( this.rank()!=1 ) {
            throw new IllegalArgumentException("must be rank 1");
        }
        putValue( -1, v );
        nextRecord();
    }
    
    /**
     * add the double to the rank 1 builder.
     * @param v the value
     */
    public void nextRecord( Datum v ) {
        if ( this.rank()!=1 ) {
            throw new IllegalArgumentException("rank 1");
        }
        putValue( -1, v );
        nextRecord();
    }
    
    /**
    * return the number of records added to the builder.
     * @return the number of records added to the builder.
    */
    public int getLength() {
        return length;
    }
    
    /**
     * return the number of elements in each record.
     * @return the number of elements in each record.
     */
    public int getRecordElements() {
        return this.recElements;
    }
    
    /**
     * returns the result dataset, concatenating all the datasets it has built thus far.
     * @return the result dataset
     */
    public DDataSet getDataSet() {
        DDataSet result;

        //TODO: Consider https://sourceforge.net/p/autoplot/bugs/1469/:
        //if ( (index+offset)==(length-1) ) { // the last record was never committed with "nextRecord"
        //    length= index;
        //}
        
        switch (rank ) {
            case 1: result= DDataSet.createRank1(length); break;
            case 2: result= DDataSet.createRank2(length,dim1); break;
            case 3: result= DDataSet.createRank3(length,dim1,dim2); break;
            case 4: result= DDataSet.createRank4(length,dim1,dim2,dim3); break;
            default: throw new RuntimeException("bad rank");
        }
        
        int dsindex=0; // dim0 index to copy dataset
        if ( finished!=null ) {
            for (DDataSet f1 : finished) {
                DDataSet.copyElements( f1, 0, result, dsindex, f1.length() );
                dsindex+= f1.length();
            }
            DDataSet.copyElements( current, 0, result, dsindex, length-dsindex );
        } else {
            result= (DDataSet) ArrayDataSet.copy(double.class,current);
        }
        result.putLength( length );
        
        if ( u!=null ) {
            result.putProperty( QDataSet.UNITS,u );
        } 
        if ( us!=null ) {
            if ( isBundle ) {
                BundleBuilder bb= new BundleBuilder(dim1);
                if ( us[0]!=null && Units.us2000.isConvertibleTo(us[0]) && names[0]==null ) { 
                    names[0]= "UTC";
                }
                for ( int i=0; i<dim1; i++ ) {
                    if ( us[i]!=null ) bb.putProperty( QDataSet.UNITS, i, us[i] );
                    if ( labels[i]!=null ) bb.putProperty( QDataSet.LABEL, i, labels[i] );
                    if ( names[i]!=null ) bb.putProperty( QDataSet.NAME, i, names[i] );
                }
                if ( us[0]!=null && Units.us2000.isConvertibleTo(us[0]) && names[0]!=null ) {
                    for ( int i=1; i<dim1; i++ ) {
                        bb.putProperty( QDataSet.DEPENDNAME_0, i, names[0] );
                    }
                }
                if ( rank==2 ) {
                    result.putProperty( QDataSet.BUNDLE_1, bb.getDataSet() );
                }
            } else {
                result.putProperty( QDataSet.UNITS, us[0] );
            }
        }
        
        if ( fillValueUsed ) {
            result.putProperty( QDataSet.FILL_VALUE, fillValue );
        }
        
        for (String key : properties.keySet()) {
            if ( key.startsWith("BUNDLE_") && dataSetResolver!=null ) {
                Object okey= properties.get(key);
                if ( okey instanceof String ) {
                    okey= dataSetResolver.resolve((String)properties.get(key));
                } else if ( okey==null ) {
                    logger.log(Level.WARNING, "unable to resolve key: {0}", key);
                }
                result.putProperty( key, okey );
            } else if ( key.startsWith("WEIGHTS" ) || key.startsWith("DEPEND_")  // The QStream parser stores strings temporarily.
                || key.startsWith("DELTA_") || key.startsWith("BIN_")) {
                Object ods= properties.get(key);
                if ( ods!=null && ods instanceof QDataSet ) {
                    result.putProperty( key, ods );
                }
            } else {
                result.putProperty( key, properties.get(key) );
            }
        }
        
        for ( Entry<String,String> key: unresolvedPropertyTypes.entrySet() ) {
            String type= key.getValue();
            if ( type.equals(UNRESOLVED_PROP_QDATASET) ) {
                String svalue= unresolvedPropertyValues.get(key.getKey());
                QDataSet value= dataSetResolver.resolve(svalue);
                if ( value!=null ) result.putProperty( key.getKey(), value );
            }
        }
        
        return result;
    }
    
    /**
     * add the property to the dataset
     * @param string name like QDataSet.UNITS 
     * @param o the value
     */
    public void putProperty( String string, Object o ) {
        properties.put( string, o );
    }
    
    /**
     * mark the property as unresolved, for reference later.  This was
     * added for the QStream reader, which doesn't resolve
     * @param type the property type, if qdataset this is resolved with dataSetResolver.
     * @param pname the property name ("gain")
     * @param svalue the arbitrary reference ("gain_04")
     */
    public void putUnresolvedProperty( String type, String pname, String svalue) {
        unresolvedPropertyTypes.put( pname, type );
        unresolvedPropertyValues.put( pname, svalue );
    }
    
    /**
     * we now know the value, so resolve any unresolved properties containing the
     * string representation.  Note
     * the entry is left in the unresolved properties.
     * @param svalue the string reference
     * @param value the object value
     */
    public void resolveProperty( String svalue, Object value ) {
        for ( Entry<String,String> e: unresolvedPropertyValues.entrySet() ) {
            if ( e.getValue().equals(svalue) ) {
                properties.put( e.getKey(), value );
            }
        }
    }
    
    /**
     * set the units for the dataset. 
     * @param u 
     */    
    public void setUnits( Units u ) {
        this.u= u;
    }
    
    /**
     * the user has specified a Datum or QDataSet, or called setUnits(i,..._), etc
     * to initialize the bundle mode.
     */
    private void maybeInitializeBundle() {
        if ( !isBundle ) { 
            logger.fine("initializeBundle");
            this.us= new Units[dim1];
            for ( int j=0; j<dim1; j++ ) {
                this.us[j]= null;
            }
            this.labels= new String[dim1];
            for ( int j=0; j<dim1; j++ ) {
                this.labels[j]= null;
            }        
            this.names= new String[dim1];
            for ( int j=0; j<dim1; j++ ) {
                this.names[j]= null;
            }
            isBundle= true;
        }   
    }
    /**
     * set the units for column i.  This is only used with rank 2 (2-index) datasets.
     * @param i the column number
     * @param u the units for the column
     */
    public void setUnits( int i, Units u ) {
        maybeInitializeBundle();
        this.us[i]= u;
    }
    
    /**
     * set the label (short, descriptive label for human consumption) 
     * for the column, for rank 2 bundle datasets.
     * @param i the column number
     * @param label the label for the column
     */
    public void setLabel( int i, String label ) {
        maybeInitializeBundle();
        this.labels[i]= label;
    }
    
    /**
     * set the name (valid Jython identifier) for the column.
     * @param i the column number
     * @param name the name (valid Jython identifier) for the column.
     */
    public void setName( int i, String name ) {
        maybeInitializeBundle();
        this.names[i]= name;
    }
    
    /**
     * get a map of all the properties set thus far.
     * @return
     */
    public Map<String,Object> getProperties() {
        return Collections.unmodifiableMap(properties);
    }
    
    /**
     * Holds value of property fillValue.
     */
    private double fillValue= -1e31;
    
    /**
     * true if the fill value was read or written.
     */
    private boolean fillValueUsed= false;
    
    /**
     * Utility field used by bound properties.
     */
    private final java.beans.PropertyChangeSupport propertyChangeSupport =  new java.beans.PropertyChangeSupport(this);
    
    /**
     * Adds a PropertyChangeListener to the listener list.
     * @param l The listener to add.
     */
    public void addPropertyChangeListener(java.beans.PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(l);
    }
    
    /**
     * Removes a PropertyChangeListener from the listener list.
     * @param l The listener to remove.
     */
    public void removePropertyChangeListener(java.beans.PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener(l);
    }
    
    /**
     * Getter for property fillValue.
     * @return Value of property fillValue.
     */
    public double getFillValue() {
        fillValueUsed= true;
        return this.fillValue;
    }
    
    /**
     * Setter for property fillValue.
     * @param fillValue New value of property fillValue.
     */
    public void setFillValue(double fillValue) {
        fillValueUsed= true;
        double oldFillValue = this.fillValue;
        this.fillValue = fillValue;
        if ( !Double.isNaN(fillValue) ) properties.put( QDataSet.FILL_VALUE, fillValue );
        propertyChangeSupport.firePropertyChange("fillValue", oldFillValue, fillValue);
    }

    protected double validMin = Double.NEGATIVE_INFINITY;
    public static final String PROP_VALIDMIN = "validMin";

    public double getValidMin() {
        return validMin;
    }

    public void setValidMin(double validMin) {
        double oldValidMin = this.validMin;
        this.validMin = validMin;
        if ( validMin>Double.NEGATIVE_INFINITY ) properties.put( QDataSet.VALID_MIN, validMin );
        propertyChangeSupport.firePropertyChange(PROP_VALIDMIN, oldValidMin, validMin);
    }
    
    protected double validMax = Double.POSITIVE_INFINITY;
    public static final String PROP_VALIDMAX = "validMax";

    public double getValidMax() {
        return validMax;
    }

    /**
     * set the valid max property.
     * @param validMax 
     */
    public void setValidMax(double validMax) {
        double oldValidMax = this.validMax;
        this.validMax = validMax;
        if ( validMax<Double.POSITIVE_INFINITY ) properties.put( QDataSet.VALID_MAX, validMax );
        propertyChangeSupport.firePropertyChange(PROP_VALIDMAX, oldValidMax, validMax);
    }
    
    
    @Override
    public String toString() {
        String dims=null;
        switch ( rank ) {
            case 1: dims= "*"; break;
            case 2: dims= "*,"+this.dim1; break;
            case 3: dims= "*,"+this.dim1+","+this.dim2; break;
        }
        return "DataSetBuilder rank=" + this.rank + " dims=[" + dims + "] reccount="+(this.index+this.offset);
    }

    /**
     * return the rank of the dataset we are building.
     * @return the rank of the dataset we are building
     */
    public int rank() {
        return rank;
    }

    /**
     * this was introduced to avoid properties where BUNDLE_1 would temporarily hold the name.
     */
    public static interface DataSetResolver {
        /**
         * return the dataset for this name, if available, or null.
         * @param name
         * @return the dataset for this name, if available, or null.
        */ 
        public QDataSet resolve( String name );
    }
    
    private DataSetResolver dataSetResolver=null;
    
    /**
     * add dataset resolved.
     * @param dataSetResolver 
     */
    public void setDataSetResolver(DataSetResolver dataSetResolver) {
        this.dataSetResolver= dataSetResolver;
    }
}
