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
import java.util.HashMap;
import java.util.Iterator;
import org.virbo.dataset.DDataSet;

/**
 * allows dataset of unknown length to be built. Presently, this only builds QUBES, but should allow for geometry changes.
 * TODO: consider using WritableDataSet interface.
 * @author jbf
 */
public class DataSetBuilder {
    
    int rank;
    
    ArrayList<DDataSet> finished;
    DDataSet current;
    int recCount;
    int dim1, dim2;
    int index;
    int offset;
    HashMap<String,Object> properties;
    
    /**
     * recCount is the guess of dim0 size.  Bad guesses will result in an extra copy.
     * @param recCount initial allocation for the first dimension.
     */
    public DataSetBuilder( int rank, int recCount ) {
        this( rank, recCount, 1, 1 );
    }
    
    /**
     * recCount is the guess of dim0 size.  Bad guesses will result in an extra copy.
     * @param recCount initial allocation for the first dimension.
     * @param dim1 when rank 2 or greater is used.
     */
    public DataSetBuilder( int rank, int recCount, int dim1 ) {
        this( rank, recCount, dim1, 1 );
    }
    
    /**
     * recCount is the guess of dim0 size.  Bad guesses may result in an extra copy.
     * @param recCount initial allocation for the first dimension.
     * @param dim1 when rank 2 or greater is used.
     * @param dim2 when rank 3 or greater is used.
     */
    public DataSetBuilder( int rank, int recCount, int dim1, int dim2 ) {
        this.rank= rank;
        this.recCount= recCount;
        this.dim1= dim1;
        this.dim2= dim2;
        newCurrent();
        index=0;
        properties= new HashMap<String,Object>();
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
     * index0 is ignored!!!
     */
    public void putValue( int index0, double d ) {
        current.putValue( this.index, d );
    }
    
    /**
     * index0 is ignored!!!
     */
    public void putValue( int index0, int index1, double d ) {
        current.putValue( this.index, index1, d );
    }
    
    /**
     * index0 is ignored!!!
     */
    public void putValue( int index0, int index1, int index2, double d ) {
        current.putValue( this.index, index1, index2, d );
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
    
    private final boolean isFill( double d ) {
        return fillValue==d || validMin>d || validMax<d;
    }
    
    /**
     * returns the result dataset, concatenating all the datasets it's built
     * thus far.
     */
    public DDataSet getDataSet() {
        DDataSet result;
        int len;
        
        switch (rank ) {
            case 1: result= DDataSet.createRank1(index+offset); break;
            case 2: result= DDataSet.createRank2(index+offset,dim1); break;
            case 3: result= DDataSet.createRank3(index+offset,dim1,dim2); break;
            default: throw new RuntimeException("bad rank");
        }
        
        int dsindex=0; // dim0 index to copy dataset
        if ( finished!=null ) {
            for ( int i=0; i<finished.size(); i++ ) {
                DDataSet f1= finished.get(i);
                DDataSet.copyElements( f1, 0, result, dsindex, f1.length() );
                dsindex+= f1.length();
            }
            DDataSet.copyElements( current, 0, result, dsindex, index );
        } else {
            result= DDataSet.copy(current);
        }
        result.putLength( index+offset );
        
        if ( fillValue!=-1e31 || validMax<Double.POSITIVE_INFINITY || validMin>Double.NEGATIVE_INFINITY ) {
            switch ( rank ) {
                case 1: {
                    for ( int i=0; i<result.length(); i++ ) {
                        if ( isFill( result.value(i) ) ) result.putValue(i,-1e31);
                    }
                } break;
                case 2: {
                    for ( int i=0; i<result.length(); i++ ) {
                        for ( int j=0; j<result.length(i); j++ ) {
                            if ( isFill( result.value(i,j) ) ) result.putValue(i,j,-1e31);
                        }
                    }
                } break;
                case 3: {
                    for ( int i=0; i<result.length(); i++ ) {
                        for ( int j=0; j<result.length(i); j++ ) {
                            for ( int k=0; k<result.length(i,j); k++ ) {
                                if ( isFill( result.value(i,j,k) ) ) result.putValue(i,j,k,-1e31);
                            }
                        }
                    }
                }
                default: throw new RuntimeException("bad rank");
            }
        }
        
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
        return "DataSetBuilder rank=" + this.rank + "(" + dims + ") reccount="+(this.index+this.offset);
    }
}
