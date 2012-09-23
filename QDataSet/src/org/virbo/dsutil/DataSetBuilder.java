/*
 * DataSetBuilder.java
 *
 * Created on May 25, 2007, 7:04 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.virbo.dsutil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.virbo.dataset.ArrayDataSet;
import org.virbo.dataset.DDataSet;
import org.virbo.dataset.QDataSet;

/**
 * allows dataset of unknown length to be built. Presently, this only builds QUBES, but should allow for geometry changes.
 * TODO: consider using WritableDataSet interface.
 * The guessRecCount parameter is the initial number of allocated records, and is also the extension when this number of
 * records is passed.  The final physical dataset size is not affected by this, because the data is copied.
 * @author jbf
 */
public class DataSetBuilder { 
    
    int rank;
    
    ArrayList<DDataSet> finished;
    DDataSet current;
    int recCount;
    int dim1, dim2;
    int recElements; // number of elements per record
    int index;
    int offset;
    int length;  // number of records or partial records written
    HashMap<String,Object> properties;
    
    /**
     * recCount is the guess of dim0 size.  Bad guesses will result in an extra copy.
     * @param guessRecCount initial allocation for the first dimension.
     */
    public DataSetBuilder( int rank, int guessRecCount ) {
        this( rank, guessRecCount, 1, 1 );
    }
    
    /**
     * recCount is the guess of dim0 size.  Bad guesses will result in an extra copy.
     * @param guessRecCount initial allocation for the first dimension.
     * @param dim1 when rank 2 or greater is used.
     */
    public DataSetBuilder( int rank, int guessRecCount, int dim1 ) {
        this( rank, guessRecCount, dim1, 1 );
    }
    
    /**
     * recCount is the guess of dim0 size.  Bad guesses may result in an extra copy.
     * @param guessRecCount initial allocation for the first dimension.
     * @param dim1 when rank 2 or greater is used.
     * @param dim2 when rank 3 or greater is used.
     */
    public DataSetBuilder( int rank, int guessRecCount, int dim1, int dim2 ) {
        this.rank= rank;
        this.recCount= guessRecCount;
        this.dim1= dim1;
        this.dim2= dim2;
        this.recElements= dim1 * dim2;
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
        if ( rank==1 ) {
            current= DDataSet.createRank1( recCount );
        } else if ( rank==2 ) {
            current= DDataSet.createRank2( recCount, dim1 );
        } else if ( rank==3 ) {
            current= DDataSet.createRank3( recCount, dim1, dim2 );
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
     * This must be called each time a record is complete.  
     * TODO:  I always forget to call this, find another way to do this.  Check
     * for unspecified entries.
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
    * return the number of records added to the builder.
    */
    public int getLength() {
        return length;
    }
    
    /**
     * return the number of elements in each record.
     * @return
     */
    public int getRecordElements() {
        return this.recElements;
    }
    
    /**
     * returns the result dataset, concatenating all the datasets it's built
     * thus far.
     */
    public DDataSet getDataSet() {
        DDataSet result;
        int len;
        
        switch (rank ) {
            case 1: result= DDataSet.createRank1(length); break;
            case 2: result= DDataSet.createRank2(length,dim1); break;
            case 3: result= DDataSet.createRank3(length,dim1,dim2); break;
            default: throw new RuntimeException("bad rank");
        }
        
        int dsindex=0; // dim0 index to copy dataset
        if ( finished!=null ) {
            for ( int i=0; i<finished.size(); i++ ) {
                DDataSet f1= finished.get(i);
                DDataSet.copyElements( f1, 0, result, dsindex, f1.length() );
                dsindex+= f1.length();
            }
            DDataSet.copyElements( current, 0, result, dsindex, length-dsindex );
        } else {
            result= (DDataSet) ArrayDataSet.copy(double.class,current);
        }
        result.putLength( length );
        
        for ( Iterator<String> i= properties.keySet().iterator(); i.hasNext();  ) {
            String key= i.next();
            result.putProperty( key, properties.get(key) );
        }
        
        return result;
    }
    
    public void putProperty( String string, Object o ) {
        properties.put( string, o );
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
     * Utility field used by bound properties.
     */
    private java.beans.PropertyChangeSupport propertyChangeSupport =  new java.beans.PropertyChangeSupport(this);
    
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
        return this.fillValue;
    }
    
    /**
     * Setter for property fillValue.
     * @param fillValue New value of property fillValue.
     */
    public void setFillValue(double fillValue) {
        double oldFillValue = this.fillValue;
        this.fillValue = fillValue;
        if ( !Double.isNaN(fillValue) ) properties.put( QDataSet.FILL_VALUE, fillValue );
        propertyChangeSupport.firePropertyChange("fillValue", new Double(oldFillValue), new Double(fillValue));
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
     */
    public int rank() {
        return rank;
    }
}
