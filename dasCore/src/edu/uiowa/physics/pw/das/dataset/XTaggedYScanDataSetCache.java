/* File: XTaggedYScanDataSetCache.java
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

import edu.uiowa.physics.pw.das.util.DasDate;
import edu.uiowa.physics.pw.das.dataset.DataSet;
import edu.uiowa.physics.pw.das.dataset.DataSetCache;
import edu.uiowa.physics.pw.das.dataset.DataSetDescriptor;
import edu.uiowa.physics.pw.das.dataset.XTaggedYScanDataSet;
/**
 *
 * @author  jbf
 */
public class XTaggedYScanDataSetCache extends DataSetCache {
        
    /** Creates a new instance of StandardDataStreamCache */
    public XTaggedYScanDataSetCache() {
        super();        
    }
        
    public long calcTotalSize() {
        long total=0;
        for (int i=0; i<buffer.length; i++) {
            if ( buffer[i]!=null) {
                XTaggedYScanDataSet xtysds= (XTaggedYScanDataSet) buffer[i].data;
                int dataSize= xtysds.data[0].z.length;
                int s1= 4 * ( dataSize + 2 );
                total+= xtysds.data.length * s1;
            }
        }
        return total;        
    }    
    
//  This method will allow there to be some overlap of the requested start,end interval.  The gaps at
// the start or end will be filled with two reads.
    
    public boolean haveStored( DataSetDescriptor dsd, DasDate start, DasDate end, double resolution, Object params ) {
        double tenpercent= end.subtract(start)/5;
        DasDate start1= start.add(tenpercent);
        DasDate end1= end.subtract(tenpercent);
        
        DataSetCache.Tag tag= new DataSetCache.Tag( dsd, start, end, resolution, params, null );
        
        edu.uiowa.physics.pw.das.util.DasDie.println(toString());
        edu.uiowa.physics.pw.das.util.DasDie.println("    need: "+tag.toString());
        
        int iHit= findStored( dsd, start1, end1, resolution, params );
        
        if (iHit!=-1) {
            hits++;
            return true;
        } else {
            misses++;
            return false;
        }
    };
    
        
    public DataSet retrieve( DataSetDescriptor dsd, DasDate start, DasDate end, double resolution, Object params ) {
        double tenpercent= end.subtract(start)/5;
        DasDate start1= start.add(tenpercent);
        DasDate end1= end.subtract(tenpercent);

        int iHit= findStored( dsd, start1, end1, resolution, params );
        if (iHit!=-1) {
            buffer[iHit].nhits++;
            buffer[iHit].lastAccess= System.currentTimeMillis();
            XTaggedYScanDataSet ds= (XTaggedYScanDataSet)buffer[iHit].data;
            double res= resolution;
            DasDate dataSetStartTime= ds.getStartTime();
            if ( start.lt(dataSetStartTime) ) {
                try {                    
                    XTaggedYScanDataSet appendBefore= (XTaggedYScanDataSet)dsd.getDataSet(params,start,dataSetStartTime,res);
                    ds= appendBefore.append(ds);
                } catch ( edu.uiowa.physics.pw.das.DasException e ) {
                }
            }
            DasDate dataSetEndTime= ds.getEndTime();
            if ( dataSetEndTime.lt(end) ) {
                try {
                    XTaggedYScanDataSet appendAfter= (XTaggedYScanDataSet)dsd.getDataSet(params,dataSetEndTime,end,res);
                    ds= ds.append(appendAfter);
                } catch ( edu.uiowa.physics.pw.das.DasException e ) {
                }
            }
            DataSetCache.Tag tag= new DataSetCache.Tag( dsd, start, end, res, params, ds );
            buffer[iHit]= tag;
            return ds;
        } else {
            throw new IllegalArgumentException("Data not found in buffer");
        }
    }
}
