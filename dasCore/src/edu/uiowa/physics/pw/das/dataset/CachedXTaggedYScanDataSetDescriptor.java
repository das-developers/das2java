/* File: CachedXTaggedYScanDataSetDescriptor.java
 * Copyright (C) 2002-2003 University of Iowa
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
import edu.uiowa.physics.pw.das.util.DasDate;

import java.util.Hashtable;

/**
 *
 * @author  jbf
 */
public class CachedXTaggedYScanDataSetDescriptor extends XTaggedYScanDataSetDescriptor {
    
    /** Creates a new instance of CachedXTaggedYScanDataSetDescriptor */
    protected CachedXTaggedYScanDataSetDescriptor() {
    }
    
    protected CachedXTaggedYScanDataSetDescriptor(Hashtable properties) {
        
        super(properties);
        dataCache= new XTaggedYScanDataSetCache();
        
    }
    
    private XTaggedYScanDataSetCache dataCache;
    
    public DataSet getDataSet(Object params, DasDate start, DasDate end, double resolution) throws DasException {
        double res= resolution;
        edu.uiowa.physics.pw.das.util.DasDie.println(""+dataCache);
        if ( dataCache.haveStored(this,start,end,res,params) ) {
            edu.uiowa.physics.pw.das.util.DasDie.println("----- Buffer Hit ----");
            return dataCache.retrieve(this,start,end,res,params);
            
        } else {
            edu.uiowa.physics.pw.das.util.DasDie.println("------- Miss --------");
            
            DataSet ds= super.getDataSet(params,start,end,resolution);
            
            dataCache.store( this, start, end, resolution, params, (XTaggedYScanDataSet)ds );
            
            return ds;
        }
    }    
    
    public void setEnabled(boolean enabled) {
        dataCache.setEnabled(enabled);
    }
    
}
