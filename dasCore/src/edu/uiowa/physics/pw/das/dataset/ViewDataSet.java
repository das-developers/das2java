package edu.uiowa.physics.pw.das.dataset;

import edu.uiowa.physics.pw.das.datum.*;

/** A DataSet implementation that share properties, yUnits and
 * yUnits with the instance of AbstractDataSet it is associated with.
 * This class is provided so that sub-classes of AbstractDataSet can
 * extend this class when creating views of their data without having
 * to copy the immutable data AbstractDataSet contains.
 *
 */
public abstract class ViewDataSet implements DataSet {
    
    DataSet source;
    
    protected ViewDataSet(DataSet source) {
        this.source= source;
    }
    
    /** Returns the value of the property that <code>name</code> represents
     * @param name String name of the property requested
     * @return the value of the property that <code>name</code> represents
     *
     */
    public Object getProperty(String name) {
        return source.getProperty(name);
    }
    
    public int getXLength() {
        return source.getXLength();
    }
    
    public Datum getXTagDatum(int i) {
        return source.getXTagDatum(i);
    }
    
    public double getXTagDouble(int i, Units units) {
        return source.getXTagDouble(i,units);
    }
    
    public int getXTagInt(int i, Units units) {
        return source.getXTagInt(i,units);
    }
    
    /** Returns the Units object representing the unit type of the x tags
     * for this data set.
     * @return the x units
     *
     */
    public Units getXUnits() {
        return source.getXUnits();
    }
    
    /** Returns the Units object representing the unit type of the y tags
     * or y values for this data set.
     * @return the y units
     *
     */
    public Units getYUnits() {
        return source.getYUnits();
    }
    
}

