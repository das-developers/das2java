/* File: AbstractVectorDataSet.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on October 27, 2003, 10:31 AM
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

import java.util.*;

/** Abstract implementation of the VectorDataSet interface provided to make
 * implementation of concrete base classes easier.  Subclasses only need to
 * implement:<ul>
 * <li>{@link VectorDataSet#getDatum(int)}</li>
 * <li>{@link VectorDataSet#getDouble(int, org.das2.datum.Units)}</li>
 * <li>{@link VectorDataSet#getInt(int, org.das2.datum.Units)}</li>
 * <li>{@link DataSet#getPlanarView(java.lang.String)}</li>
 *
 * @author  Edward West
 */
public abstract class AbstractVectorDataSet extends AbstractDataSet implements DataSet, VectorDataSet {
    
    /** Creates a new instance of AbstractVectorDataSet
     * The properties map must only have keys of type String.
     * @param xTags values of the x tags for this data set in xUnits
     * @param xUnits the units of the x tags for this data set
     * @param yUnits the units of the y tags/values for this data set
     * @param properties map of property names and values
     * @throws IllegalArgumentException if properties has one or more keys
     *      that is not a String
     */
    protected AbstractVectorDataSet(double[] xTags, Units xUnits, Units yUnits, Map properties) throws IllegalArgumentException {
        super(xTags, xUnits, yUnits, properties);
    }
    
    public String toString() {
        return VectorUtil.toString(this);
    }
        
}
