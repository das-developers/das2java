/* File: DataSetDescriptor.java
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
import edu.uiowa.physics.pw.das.client.*;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.graph.*;
import edu.uiowa.physics.pw.das.util.*;
import edu.uiowa.physics.pw.das.stream.*;
import edu.uiowa.physics.pw.das.system.RequestProcessor;

import java.net.*;
import java.util.*;
import java.util.regex.*;
import java.lang.reflect.*;
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
    protected Map properties = new HashMap();
    private boolean defaultCaching= true;
    private DataSet cacheDataSet;
    private String dataSetID;
    private CacheTag cacheTag;
    private EventListenerList listenerList;
    
    protected DataSetDescriptor(final String dataSetID) {
        cacheTag=null;
        this.dataSetID= dataSetID;
    }
    protected DataSetDescriptor() {
        this("");
    }
    
    /* getDataSetImpl implements the getDataSet for this DataSetDescriptor implementation.  The
     * getDataSet call of the abstract DataSetDescriptor uses this routine to satisfy requests and
     * fill its cache.  This caching may be disabled via setDefaultCaching;
     */
    protected abstract DataSet getDataSetImpl( Datum start, Datum end, Datum resolution, DasProgressMonitor monitor ) throws DasException ;
    
    public abstract Units getXUnits();
    
    public void requestDataSet(final Datum start, final Datum end, final Datum resolution, final DasProgressMonitor monitor) {
        DataSetUpdateEvent dsue;
        try {
            DataSet ds = getDataSet(start, end, resolution, monitor);
            dsue = new DataSetUpdateEvent(DataSetDescriptor.this, ds);
        }
        catch (DasException de) {
            dsue = new DataSetUpdateEvent(DataSetDescriptor.this, de);
        }
        fireDataSetUpdateEvent(dsue);
    }
    
    /* Retrieve the dataset for this interval and resolution.  The contract for this function is that
     * identical start,end,resolution parameters will yield an identical dataSet, except for when an
     * DataSetUpdate has been fired in the meantime.
     *
     * null for the data resolution indicates that the data should be returned at its "intrinsic resolution"
     * if such a resolution exists.
     */
    public DataSet getDataSet(Datum start, Datum end, Datum resolution, DasProgressMonitor monitor ) throws DasException {
        if (monitor != null) {
            monitor.started();
        }
        if ( cacheTag!=null &&
        defaultCaching &&
        cacheTag.start.le(start) &&
        cacheTag.end.ge(end) &&
        ( cacheTag.resolution==null || (resolution != null && cacheTag.resolution.le(resolution)) ) ) {
            if (monitor != null) {
                monitor.finished();
            }
            return cacheDataSet;
        } else {
            try {
                cacheDataSet= getDataSetImpl( start, end, resolution, monitor );
                if ( cacheDataSet!=null ) {
                    cacheTag= (CacheTag)cacheDataSet.getProperty( "cacheTag" );
                    if ( cacheTag == null ) cacheTag= new CacheTag( start, end, resolution );
                }
                if (monitor != null) {
                    monitor.finished();
                }
                return cacheDataSet;
            } catch ( DasException e ) {
                throw e;
            } catch ( Exception e ) {
                DasExceptionHandler.handle(e);
                return null;
            }
        }
    }
    
    /*
     * clear any state that's developed, in particular any data caches 
     */
    public void reset() {
       cacheTag= null;        
    }
    
    protected void setDefaultCaching( boolean value ) {
        defaultCaching= value;
    }
    
    public void addDataSetUpdateListener( DataSetUpdateListener listener ) {
        if (listenerList == null ) {
            listenerList = new EventListenerList();
        }
        listenerList.add( DataSetUpdateListener.class, listener);
    }
    
    public void removeDataSetUpdateListener( DataSetUpdateListener listener ) {
        if (listenerList == null ) {
            listenerList = new EventListenerList();
        }
        listenerList.remove( DataSetUpdateListener.class, listener );
    }
    
    protected void fireDataSetUpdateEvent( DataSetUpdateEvent event ) {
        if (listenerList == null) return;
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==DataSetUpdateListener.class) {
                ((DataSetUpdateListener)listeners[i+1]).dataSetUpdated(event);
            }
        }        
    }
    
    public String getDataSetID() {
        return this.dataSetID;
    }
    
    private static final Pattern CLASS_ID = Pattern.compile("class:([a-zA-Z0-9_\\.]+)(?:\\?(.*))?");
    private static final Pattern NAME_VALUE = Pattern.compile("([_0-9a-zA-Z%+.-]+)=([_0-9a-zA-Z%+.-]+)");
    
    public static DataSetDescriptor create( final String dataSetID ) throws DasException {
        java.util.regex.Matcher classMatcher = CLASS_ID.matcher(dataSetID);
        DataSetDescriptor result;
        if (classMatcher.matches()) {
            result = createFromClassName(dataSetID, classMatcher);
        }
        else {
            try {
                result = createFromServerAddress(new URL(dataSetID));
                //result = DasServer.createDataSetDescriptor(new URL(dataSetID));
            }
            catch (MalformedURLException mue) {
                throw new DasIOException(mue.getMessage());
            }
        }
        result.dataSetID = dataSetID;
        return result;
    }
    
    private static DataSetDescriptor createFromServerAddress(final URL url) throws DasException {
        DasServer server = DasServer.create(url);
        StreamDescriptor sd = server.getStreamDescriptor(url);
        return new StreamDataSetDescriptor(sd, server.getStandardDataStreamSource(url));
    }
    
    private static DataSetDescriptor createFromClassName( final String dataSetID, final Matcher matcher) throws DasException {
        try {
            String className = matcher.group(1);
            String argString = matcher.group(2);
            Map argMap = argString == null ? Collections.EMPTY_MAP : URLBuddy.parseQueryString(argString);
            Class dsdClass = Class.forName(className);
            Method method = dsdClass.getMethod("newDataSetDescriptor", new Class[]{java.util.Map.class});
            if (!Modifier.isStatic(method.getModifiers())) {
                throw new NoSuchDataSetException("newDataSetDescriptor must be static");
            }
            return (DataSetDescriptor)method.invoke(null, new Object[]{argMap});
        }
        catch (ClassNotFoundException cnfe) {
            DataSetDescriptorNotAvailableException dsdnae =
            new DataSetDescriptorNotAvailableException(cnfe.getMessage());
            dsdnae.initCause(cnfe);
            throw dsdnae;
        }
        catch (NoSuchMethodException nsme) {
            DataSetDescriptorNotAvailableException dsdnae =
            new DataSetDescriptorNotAvailableException(nsme.getMessage());
            dsdnae.initCause(nsme);
            throw dsdnae;
        }
        catch (InvocationTargetException ite) {
            DataSetDescriptorNotAvailableException dsdnae =
            new DataSetDescriptorNotAvailableException(ite.getTargetException().getMessage());
            dsdnae.initCause(ite.getTargetException());
            throw dsdnae;
        }
        catch (IllegalAccessException iae) {
            DataSetDescriptorNotAvailableException dsdnae =
            new DataSetDescriptorNotAvailableException(iae.getMessage());
            dsdnae.initCause(iae);
            throw dsdnae;
        }
    }
    
    protected void setProperties( Map properties ) {
        this.properties.putAll(properties);
    }
    
    /**
     * Returns the value of the property with the specified name
     *
     * @param name The name of the property requested
     * @return The value of the requested property as an Object
     */
    public Object getProperty(String name) {
        return properties.get(name);
    }
    
    
}
