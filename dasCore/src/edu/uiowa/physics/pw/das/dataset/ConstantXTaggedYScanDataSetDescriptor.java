/* File: ConstantXTaggedYScanDataSetDescriptor.java
 * Copyright (C) 2002-2003 The University of Iowa
 * Created by: Jeremy Faden <jbf@space.physics.uiowa.edu>
 *             Jessica Swanner <jessica@space.physics.uiowa.edu>
 *             Edward E. West <eew@space.physics.uiowa.edu>
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

import edu.uiowa.physics.pw.das.DasException;
import edu.uiowa.physics.pw.das.datum.*;

import java.io.InputStream;

/**
 *
 * @author  eew
 */
public class ConstantXTaggedYScanDataSetDescriptor extends XTaggedYScanDataSetDescriptor {
    
    XTaggedYScanDataSet xtys;
    
    /** Creates a new instance of ConstantXTaggedYScanDataSetDescriptor */
    public ConstantXTaggedYScanDataSetDescriptor(XTaggedYScanDataSet xtys) {
        if (xtys == null) throw new NullPointerException("XTaggedYScanDataSet parameter cannot be null");
        this.xtys = xtys;
    }
    
    public DataSet getDataSet(Object params, Datum start, Datum end, Datum resolution) throws DasException {
        return xtys;
    }
    
    public DataSet getDataSet(InputStream in, Datum start, Datum end) throws DasException {
        return xtys;
    }
    
    public DataSet getDataSet(Object params, Datum start, Datum end) throws DasException {
        return xtys;
    }
    
}
