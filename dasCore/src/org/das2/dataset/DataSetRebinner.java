/* File: DataSetRebinner.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on November 5, 2003, 10:28 AM
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

import org.das2.DasException;
import org.das2.qds.QDataSet;

/**
 *
 * @author  Edward West
 */
public interface DataSetRebinner {

    /**
     * create a new QDataSet in a rank 2 table with x and y tags described by x and y.
     * @param ds The input dataset, either a rank 2 or rank 3 dataset.  Note this may include rank 1 dataset and rank 2 bundles at some point.
     * @param x describes the column labels.  (Note this may become a QDataSet at some point).
     * @param y describes the row labels.
     * @return a rank 2 QDataSet with the given rows and columns.
     * @throws IllegalArgumentException
     * @throws DasException
     */
    QDataSet rebin( QDataSet ds, RebinDescriptor x, RebinDescriptor y ) throws IllegalArgumentException, DasException;
    
}
