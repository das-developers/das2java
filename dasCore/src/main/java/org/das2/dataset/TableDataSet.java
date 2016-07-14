/* File: TableDataSet.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on October 24, 2003, 11:46 AM
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

package org.das2.dataset;

import org.das2.datum.Units;
import org.das2.datum.DatumVector;
import org.das2.datum.Datum;

/** A <code>DataSet</code> implementation for 3 dimensional z(x,y) data sets
 * where the data is arranged in a sequence of tables.  Each table will have
 * a set of monotonically increasing x tags and y tags.  The x tags for all
 * the tables, when taken together in the order that the table are in, will
 * be monotonically increasing over the whole data set.  The y tags are constant
 * over the y scans of each table, but may change, either in number or value,
 * from table to table.
 *
 * @author  Edward West
 */
public interface TableDataSet extends DataSet {
    
    /** Returns the Units object representing the unit type of the y values for
     * this data set.
     * @return the x units
     */
    Units getZUnits();
    
    /** Returns the Z value for the given indices into the x and y tags as a
     * <code>Datum</code>.
     * @param i index of the x tag for the requested value.
     * @param j index of the y tag for the requested value.
     * @return the value at index location (i, j) as a <code>Datum</code>
     */    
    Datum getDatum(int i, int j);
    
    /** Returns the Z value for the given indices into the x and y tags as a
     * <code>double</code> with the given units.
     * @param j index of the x tag for the requested value.
     * @param i index of the y tag for the requested value.
     * @param units the units the returned value should be coverted to.
     * @return the value at index location (i, j) as a <code>double</code>.
     */    
    double getDouble(int i, int j, Units units);
    
    DatumVector getScan(int i);
    
    double[] getDoubleScan(int i, Units units);
    
    /** Returns the Z value for the given indices into the x and y tags as a
     * <code>int</code> with the given units.
     * @param i index of the x tag for the requested value.
     * @param j index of the y tag for the requested value.
     * @param units the units the returned value should be coverted to.
     * @return the value at index location (i, j) as a <code>int</code>.
     */    
    int getInt(int i, int j, Units units);
    
    /** Returns the yTags for this data set as a <code>DatumVector</code>
     * @return the yTags for this data set as a <code>DatumVector</code>
     */
    DatumVector getYTags(int table);

    /** Returns the value of the y tag at the given index j as a
     *      <code>Datum</datum>.
     * @param j the index of the requested y tag
     * @return the value of the y tag at the given index j as a
     *      <code>Datum</code>.
     */
    Datum getYTagDatum(int table, int j);
    
    /** Returns the value of the y tag at the given index j as a
     *      <code>double</code> in the given units.  YTags must be
     *      monotonically increasing with j.
     * @return the value of the y tag at the given index j as a
     *      <code>double</code>.
     * @param units the units of the returned value
     * @param j the index of the requested y tag
     */
    double getYTagDouble(int table, int j, Units units);
    
    /** Returns the value of the y tag at the given index j as an
     *      <code>int</datum> in the given units.  YTags must be
     *      monotonically increasing with j.
     * @return the value of the y tag at the given index j as an
     *      <code>int</code>.
     * @param units the units of the returned value
     * @param j the index of the requested y tag
     */
    int getYTagInt(int table, int j, Units units);
    
    /** Returns the number of y tags in the specified table for this data set.
     *  YTags must be monotonically increasing with j.
     * @param table index of the table
     * @return the number of x tags in this data set.
     */
    int getYLength(int table);
    
    /** Returns the first x tag index of the specified table.
     * @param table the index of the table.
     * @return the first x tag index of the specified table
     */    
    int tableStart(int table);
    
    /** Returns the index after the last x tag index of the specified table
     * @param table the index of the table
     * @return the index after the last x tag index of the specified table
     */    
    int tableEnd(int table);
    
    /** Returns the number of tables in this data set
     * @return the number of tables in this data set
     */    
    int tableCount();
    
    /** Returns the table number that the specified index is in.
     * @param i x tag index
     * @return the table number that the specified index is in
     */
    int tableOfIndex(int i);
    
    /** Returns a slice view of this data set for a specific x value
     */
    VectorDataSet getXSlice(int i);
    
    /** Returns a slice view of this data set for a specific y value
     */
    VectorDataSet getYSlice(int j, int table);

    /**
     * Return the property value attached to the table.  This should 
     * simply return DataSet.getProperty() if the table has no special
     * value for the table.
     * @param table
     * @param name
     * @return
     */
    Object getProperty( int table, String name );
}
