/* File: AbstractDataSetDescriptor.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on October 22, 2003, 12:49 PM by __FULLNAME__ <__EMAIL__>
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

import edu.uiowa.physics.pw.das.*;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.graph.*;
import edu.uiowa.physics.pw.das.util.*;
import javax.swing.event.*;

/**
 *
 * @author  jbf
 */
public abstract class DataSetDescriptor {
    /* defaultCaching means that the abstract DataSetDescriptor is allowed to handle
     * repeat getDataSet calls by returning a cached dataset.  If a dataSetUpdate event
     * is thrown, the defaultCache is reset.
     */
    private boolean defaultCaching;
    private DataSet cacheDataSet;
    private class CacheTag {
        Datum start;
        Datum end;
        Datum resolution;
        CacheTag( Datum start, Datum end, Datum resolution ) {
            this.start= start;
            this.end= end;
            this.resolution= resolution;
        }
    }
    private CacheTag cacheTag;
    
    protected DataSetDescriptor(String dataSetId) {
        cacheTag=null;
        this.dataSetId= dataSetId;
    }
    protected DataSetDescriptor() {
        this("");
    }
    
    /* getDataSetImpl implements the getDataSet for this DataSetDescriptor implementation.  The
     * getDataSet call of the abstract DataSetDescriptor uses this routine to satisfy requests and
     * fill its cache.  This caching may be disabled via setDefaultCaching;
     */
    public abstract DataSet getDataSetImpl( Datum start, Datum end, Datum resolution, DasProgressMonitor monitor ) throws DasException ;
    
    public abstract Units getXUnits();
    
    /* Retrieve the dataset for this interval and resolution.  The contract for this function is that
     * identical start,end,resolution parameters will yield an identical dataSet, except for when an
     * DataSetUpdate has been fired in the meantime.
     */
    public DataSet getDataSet(Datum start, Datum end, Datum resolution, DasProgressMonitor monitor ) throws DasException {
        if ( cacheTag!=null &&
        cacheTag.start.equals(start) &&
        cacheTag.end.equals(end) &&
        cacheTag.resolution.equals(resolution) ) {
            return cacheDataSet;
        } else {
            cacheDataSet= getDataSetImpl( start, end, resolution, monitor );
            cacheTag= new CacheTag(start,end,resolution);
            return cacheDataSet;
        }
    }
        
    protected void setDefaultCaching( boolean value ) {
        defaultCaching= true;
    }
    
    EventListenerList listenerList;      
    
    
    public void addDataSetUpdateListener( DataSetUpdateListener listener ) {
         if (listenerList == null ) {
            listenerList = new EventListenerList();
        }
        listenerList.add( DataSetUpdateListener.class, listener);            
    }
     
    protected void fireDataSetUpdateEvent( DataSetUpdateEvent event ) {
        if (listenerList == null) return;
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==DataSetUpdateListener.class) {
                ((DataSetUpdateListener)listeners[i+1]).DataSetUpdated(event);
            }
        }
    }

    String dataSetId;

    public String getDataSetId() {
        return this.dataSetId;
    }
    
    public void setDataSetId(String dataSetId) {
        this.dataSetId= dataSetId;
    }
    
}
