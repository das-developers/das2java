/* File: AbstractDataSet.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on October 27, 2003, 9:32 AM
 *      by Edward West <eew@space.physics.uiowa.edu>
 *
 * This file is part of the das2 library.
 *
 * das2 is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.uiowa.physics.pw.das.dataset;

import edu.uiowa.physics.pw.das.datum.*;

import java.util.*;

/**
 *
 * @author  eew
 */
public abstract class AbstractDataSet implements DataSet {
    
    private Map properties;
    
    private double[] xTags;
    
    private Units xUnits;
    
    private Units yUnits;
    
    /** Creates a new instance of AbstractDataSet
     * The properties map must only have keys of type String.
     * @param xTags values of the x tags for this data set in xUnits
     * @param xUnits the units of the x tags for this data set
     * @param yUnits the units of the y tags/values for this data set
     * @param properties map of property names and values
     * @throws IllegalArgumentException if properties has one or more keys
     *      that is not a String
     */
    protected AbstractDataSet(double[] xTags, Units xUnits, Units yUnits, Map properties) throws IllegalArgumentException {
        for (Iterator i = properties.keySet().iterator(); i.hasNext();) {
            if (!(i.next() instanceof String)) {
                throw new IllegalArgumentException("Non-String key found in property map");
            }
        }
        this.properties = new HashMap(properties);
        this.xTags = (double[])xTags.clone();
        this.xUnits = xUnits;
        this.yUnits = yUnits;
    }
    
    private AbstractDataSet() {
    }
    
    /** Returns the value of the property that <code>name</code> represents
     * @param name String name of the property requested
     * @return the value of the property that <code>name</code> represents
     */
    public Object getProperty(String name) {
        return properties.get(name);
    }
    
    public int getXLength() {
        return xTags.length;
    }
    
    public Datum getXTagDatum(int i) {
        return Datum.create(xTags[i], getXUnits());
    }
    
    public double getXTagDouble(int i, Units units) {
        double xTag = xTags[i];
        xTag = getXUnits().getConverter(units).convert(xTag);
        return xTag;
    }
    
    public int getXTagInt(int i, Units units) {
        return (int)Math.round(getXTagDouble(i, units));
    }
    
    /** Returns the Units object representing the unit type of the x tags
     * for this data set.
     * @return the x units
     */
    public Units getXUnits() {
        return xUnits;
    }

    /** Returns the Units object representing the unit type of the y tags
     * or y values for this data set.
     * @return the y units
     */
    public Units getYUnits() {
        return yUnits;
    }

    /** A DataSet implementation that share properties, yUnits and
     * yUnits with the instance of AbstractDataSet it is associated with.
     * This class is provided so that sub-classes of AbstractDataSet can
     * extend this class when creating views of their data without having
     * to copy the immutable data AbstractDataSet contains.
     */
    protected abstract class ViewDataSet extends AbstractDataSet implements DataSet {
        
        protected ViewDataSet() {}
        
        /** Returns the value of the property that <code>name</code> represents
         * @param name String name of the property requested
         * @return the value of the property that <code>name</code> represents
         */
        public Object getProperty(String name) {
            return properties.get(name);
        }

        public int getXLength() {
            return xTags.length;
        }
    
        public Datum getXTagDatum(int i) {
            return Datum.create(xTags[i], getXUnits());
        }
    
        public double getXTagDouble(int i, Units units) {
            double xTag = xTags[i];
            xTag = getXUnits().getConverter(units).convert(xTag);
            return xTag;
        }

        public int getXTagInt(int i, Units units) {
            return (int)Math.round(getXTagDouble(i, units));
        }
    
        /** Returns the Units object representing the unit type of the x tags
         * for this data set.
         * @return the x units
         */
        public Units getXUnits() {
            return xUnits;
        }

        /** Returns the Units object representing the unit type of the y tags
         * or y values for this data set.
         * @return the y units
         */
        public Units getYUnits() {
            return yUnits;
        }

    }
    
}
