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

import org.das2.util.monitor.NullProgressMonitor;
import org.das2.util.monitor.DasProgressMonitor;
import edu.uiowa.physics.pw.das.*;
import edu.uiowa.physics.pw.das.client.*;
import edu.uiowa.physics.pw.das.components.propertyeditor.*;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.util.*;
import edu.uiowa.physics.pw.das.stream.*;
import edu.uiowa.physics.pw.das.system.DasLogger;
import edu.uiowa.physics.pw.das.system.RequestProcessor;

import java.net.*;
import java.util.*;
import java.util.regex.*;
import java.lang.reflect.*;
import java.util.logging.Logger;
import javax.swing.event.*;

/**
 * DataSetDescriptors are a source from where datasets are produced.  These uniquely identify a data set
 * that is parameteric.  Typically, the parameter is time, so for example, there might be a DataSetDescriptor
 * for "discharge of the Iowa River measured at Iowa City."  Clients of the class get
 * DataSets from the DataSetDescriptor via the getDataSet( Start, End, Resolution ) method.  So for
 * example, you might ask what is the discharge from June 1 to August 31st, 2005, at a resolution of
 * 1 day.  Presently, it's implicit that this means to give bin averages of the data.
 *
 * <p>DataSetDescriptors are identified with a URL-like string:
 * <pre>http://www-pw.physics.uiowa.edu/das/das2Server?das2_1/cluster/wbd/r_wbd_dsn_cfd&spacecraft%3Dc1%26antenna%3DEy</pre></p>
 *
 * <p>The protocol of the string indicates how the DataSetDescriptor is to be constructed, and presently
 * there are:
 *<pre>
 *   http     a das2Server provides the specification of the datasetdescriptor.
 *   class    refers to a loadable java class that is an instanceof DataSetDescriptor and
 *            has the method newDataSetDescriptor( Map params ) throws DasException
 *</pre>
 * </P>
 * @author jbf
 */
public abstract class DataSetDescriptor implements Displayable {

    protected Map properties = new HashMap();

    private boolean defaultCaching= true;
    
    private DataSetCache dataSetCache;
    
    private String dataSetID;
    
    private EventListenerList listenerList;
    
    protected DataSetDescriptor(final String dataSetID) {
        dataSetCache= DasApplication.getDefaultApplication().getDataSetCache();
        this.dataSetID= dataSetID;
    }
           
    protected DataSetDescriptor() {
        this("");
    }
    
    private static final Logger logger= DasLogger.getLogger(DasLogger.GRAPHICS_LOG);
    
    /**
     * getDataSetImpl implements the getDataSet for this DataSetDescriptor implementation.  The
     * getDataSet call of the abstract DataSetDescriptor uses this routine to satisfy requests and
     * fill its cache.  This caching may be disabled via setDefaultCaching.  To satisfy the request,
     * a DataSet should be returned with an x tag range that contains start and end, with a 
     * resolution finer than that requested.
     * 
     * @param start beginning of range for the request.
     * @param end end of the range for the request.
     * @param resolution the resolution requirement for the reqeust.  <code>null</code> may be used to request the finest resolution available or intrinic resolution.
     */
    protected abstract DataSet getDataSetImpl( Datum start, Datum end, Datum resolution, DasProgressMonitor monitor ) throws DasException ;
    
    /**
     * @return the x units of the DataSetDescriptor that parameterize the data.  This is used to identify dataSet requests.
     */
    public abstract Units getXUnits();
    
    /**
     * Requests that a dataSet be loaded, and that the dataSet be returned via a DataSetUpdate event.
     * The @param lockObject is an object that is dependent on the load, for example, the DasCanvas,
     * and will be passed in to the request processor.  If the dataSet is available in interactive time,
     * then the dataSetUpdate may be fired on the same thread as the request is made.
     */
    public void requestDataSet(final Datum start, final Datum end, final Datum resolution,
            final DasProgressMonitor monitor, Object lockObject ) {
        
        Runnable request = new Runnable() {
            public void run() {
                logger.info("requestDataSet: "+start+" "+end+" "+resolution);
                try {
                    DataSet ds= getDataSet( start, end, resolution, monitor );
                    if ( ds==null ) throw new NoDataInIntervalException( new DatumRange(start,end).toString() );
                    DataSetUpdateEvent dsue= new DataSetUpdateEvent(DataSetDescriptor.this,ds);
                    dsue.setMonitor( monitor );
                    fireDataSetUpdateEvent(dsue);
                } catch ( DasException e ) {
                    DataSetUpdateEvent dsue= new DataSetUpdateEvent(DataSetDescriptor.this,e);
                    dsue.setMonitor( monitor );
                    fireDataSetUpdateEvent(dsue);
                }
            }
            public String toString() {
                return "loadDataSet "+ new DatumRange( start, end );
            }
        };
        logger.info("submit data request");
        
        CacheTag tag= new CacheTag( start, end, resolution );
        if ( dataSetCache.haveStored( this, tag ) ) {
            request.run();
        } else {
            RequestProcessor.invokeLater( request, lockObject );
        }
        
    }
    
    /**
     * Request the dataset, and the dataset is returned only to the listener.
     *
     * @param lockObject object that is waiting for the result of this load, used to block other tasks which use that object.
     */
    public void requestDataSet(final Datum start, final Datum end, final Datum resolution,
            final DasProgressMonitor monitor, Object lockObject, final DataSetUpdateListener listener ) {
        
        if ( lockObject==null ) lockObject= listener;
        
        if ( this instanceof ConstantDataSetDescriptor ) {
            try {
                DataSet ds= getDataSet(null,null,null,null);
                DataSetUpdateEvent dsue= new DataSetUpdateEvent( this, ds );
                dsue.setMonitor(monitor);
            } catch ( DasException e ) {
                DataSetUpdateEvent dsue= new DataSetUpdateEvent(DataSetDescriptor.this,e);
                dsue.setMonitor(monitor);
                listener.dataSetUpdated(dsue);
            }
        } else {
            Runnable request = new Runnable() {
                public void run() {
                    logger.info("request data from dsd: "+start+" "+end+" "+resolution);
                    try {
                        DataSet ds= getDataSet( start, end, resolution, monitor );
                        DataSetUpdateEvent dsue= new DataSetUpdateEvent(DataSetDescriptor.this,ds);
                        dsue.setMonitor(monitor);
                        listener.dataSetUpdated(dsue);
                    } catch ( DasException e ) {
                        DataSetUpdateEvent dsue= new DataSetUpdateEvent(DataSetDescriptor.this,e);
                        dsue.setMonitor(monitor);
                        listener.dataSetUpdated(dsue);
                    }
                }
                public String toString() {
                    return "loadDataSet "+ new DatumRange( start, end );
                }
            };
            RequestProcessor.invokeLater( request, lockObject );
        }
        
    }
    
    /**
     * Retrieve the dataset for this interval and resolution.  The contract for this function is that
     * identical start,end,resolution parameters will yield an identical dataSet, except for when an
     * DataSetUpdate has been fired in the meantime.
     *
     * null for the data resolution indicates that the data should be returned at its "intrinsic resolution"
     * if such a resolution exists.
     */
    public DataSet getDataSet(Datum start, Datum end, Datum resolution, DasProgressMonitor monitor ) throws DasException {
        if ( monitor==null ) monitor=new NullProgressMonitor();
        
        CacheTag tag=null;
        if ( defaultCaching ) {
            tag= new CacheTag( start, end, resolution );
            DasLogger.getLogger(DasLogger.DATA_TRANSFER_LOG).info("getDataSet " + this +" " + tag);
        }
        
        if ( defaultCaching && dataSetCache.haveStored( this, tag ) ) {
            return dataSetCache.retrieve( this, tag );
        } else {
            try {
                DataSet ds = getDataSetImpl( start, end, resolution, monitor );
                if (ds != null) {
                    if ( ds.getProperty( "cacheTag" )!=null ) tag= (CacheTag)ds.getProperty( "cacheTag" );
                    if ( defaultCaching ) dataSetCache.store( this, tag, ds );
                }
                return ds;
            } catch ( DasException e ) {
                throw e;
            } finally {
                monitor.finished();
            }
        }
    }
    
    /**
     * clear any state that's developed, in particular any data caches.  Note
     * this currently deletes all cached datasets, regardless of the DataSetDescriptor
     * that produced them.
     */
    public void reset() {
        dataSetCache.reset();
    }
    
    /**
     * defaultCaching means that the abstract DataSetDescriptor is allowed to handle
     * repeat getDataSet calls by returning a cached dataset.  If a dataSetUpdate event
     * is thrown, the defaultCache is reset.
     *
     * Use caution when using this.  Note that caching may only be turned off
     * with this call.
     */
    public void setDefaultCaching( boolean value ) {
        if ( value==false ) defaultCaching= value;
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
    
    /**
     * @return the string that uniquely identifies this dataset.
     */
    public String getDataSetID() {
        return this.dataSetID;
    }
    
    private static final Pattern CLASS_ID = Pattern.compile("class:([a-zA-Z0-9_\\.]+)(?:\\?(.*))?");
    private static final Pattern NAME_VALUE = Pattern.compile("([_0-9a-zA-Z%+.-]+)=([_0-9a-zA-Z%+.-]+)");
    
    /**
     * creates a DataSetDescriptor for the given identification string.  The identification
     * string is a URL-like string, for example <code>http://www-pw.physics.uiowa.edu/das/das2Server?das2_1/cluster/wbd/r_wbd_dsn_cfd&spacecraft%3Dc1%26antenna%3DEy</code>
     * The protocol of the string indicates how the DataSetDescriptor is to be constructed, and presently
     * there are:
     *<pre>
     *   http     a das2Server provides the specification of the DataSetDescriptor, and a
     *            StreamDataSetDescriptor is created.
     *   class    refers to a loadable java class that is an instanceof DataSetDescriptor and
     *            has the method newDataSetDescriptor( Map params )
     *</pre>
     * Note that DataSetDescriptors are stateless, the same DataSetDescriptor object
     * may be returned to multiple clients.
     */
    public static DataSetDescriptor create( final String dataSetID ) throws DasException {
        java.util.regex.Matcher classMatcher = CLASS_ID.matcher(dataSetID);
        DataSetDescriptor result;
        if (classMatcher.matches()) {
            result = createFromClassName(dataSetID, classMatcher);
        } else {
            try {
                result = createFromServerAddress(new URL(dataSetID));
                //result = DasServer.createDataSetDescriptor(new URL(dataSetID));
            } catch (MalformedURLException mue) {
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
        } catch (ClassNotFoundException cnfe) {
            DataSetDescriptorNotAvailableException dsdnae =
                    new DataSetDescriptorNotAvailableException(cnfe.getMessage());
            dsdnae.initCause(cnfe);
            throw dsdnae;
        } catch (NoSuchMethodException nsme) {
            DataSetDescriptorNotAvailableException dsdnae =
                    new DataSetDescriptorNotAvailableException(nsme.getMessage());
            dsdnae.initCause(nsme);
            throw dsdnae;
        } catch (InvocationTargetException ite) {
            DataSetDescriptorNotAvailableException dsdnae =
                    new DataSetDescriptorNotAvailableException(ite.getTargetException().getMessage());
            dsdnae.initCause(ite.getTargetException());
            throw dsdnae;
        } catch (IllegalAccessException iae) {
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
    
    public javax.swing.Icon getListIcon() {
        return null;
    }
    
    public String getListLabel() {
        return this.dataSetID;
    }
    
    /**
     * @return the DataSetCache object used to store cached copies of the
     * DataSets created by this object.
     */
    public DataSetCache getDataSetCache() {
        return this.dataSetCache;
    }
    
}
