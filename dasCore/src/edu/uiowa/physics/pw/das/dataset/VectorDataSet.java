/* File: VectorDataSet.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on October 27, 2003, 8:55 AM
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

/** Interface definition for datasets comprized of a vector of y values
 * each with an associated x tag.
 *
 * @author  eew
 */
public interface VectorDataSet extends DataSet {

    /** Returns the Y value for the given index into the x tags as a
     * <code>Datum</code>.
     * @param i index of the x tag for the requested value.
     * @return the value at index location i as a <code>Datum</code>
     */    
    Datum getDatum(int i);
    
    /** Returns the Y value for the given index into the x tags as a
     * <code>double</code> with the given units.
     * @param i index of the x tag for the requested value.
     * @param units the units the returned value should be coverted to.
     * @return the value at index location i as a <code>double</code>.
     */    
    double getDouble(int j, Units units);
    
    /** Returns the Y value for the given index into the x tags as a
     * <code>int</code> with the given units.
     * @param i index of the x tag for the requested value.
     * @param units the units the returned value should be coverted to.
     * @return the value at index location i as a <code>int</code>.
     */    
    int getInt(int i, Units units);
}
