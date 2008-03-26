/* File: ConstantDataSetDescriptor.java
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
import edu.uiowa.physics.pw.das.datum.Datum;
import org.das2.util.monitor.ProgressMonitor;

/**
 * This class wraps a DataSet to use where DataSetDescriptors are required.  This 
 * trivially returns the wrapped DataSet regardless of the getDataSet parameters.
 * Originally this class was used with Renderers, which needed a DataSetDescriptor 
 * for their operation.  Now that they interact only with DataSets, and this
 * class should not be used with them, use Renderer.setDataSet instead.
 *
 * @author eew
 * 
 */
public class ConstantDataSetDescriptor extends DataSetDescriptor {
    
    DataSet ds;
    
    /** Creates a new instance of ConstantDataSetDescriptor */
    public ConstantDataSetDescriptor(DataSet ds) {
        super();
        if (ds == null) throw new NullPointerException("DataSet parameter cannot be null");
        this.ds = ds;
    }
    
    // this is never called because we override getDataSet
    public DataSet getDataSetImpl(Datum start, Datum end, Datum resolution, ProgressMonitor monitor) throws DasException {
        return ds;
    }    
    
    public DataSet getDataSet( Datum start, Datum end, Datum resolution, ProgressMonitor monitor) throws DasException {
        return ds;
    }    
    
    public edu.uiowa.physics.pw.das.datum.Units getXUnits() {
        return ds.getXUnits();
    }
    
    public void requestDataSet(Datum start, Datum end, Datum resolution, ProgressMonitor monitor, Object lockObject) {
        DataSetUpdateEvent dsue= null;
        try {
            DataSet ds= getDataSet(start, end, resolution, monitor);
            dsue= new DataSetUpdateEvent( this, ds );
            fireDataSetUpdateEvent(dsue);
        } catch ( DasException e ) {
            dsue= new DataSetUpdateEvent( this,e);
            fireDataSetUpdateEvent(dsue);
        }
        return;
        
    }
    
}
