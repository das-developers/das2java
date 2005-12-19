/*
 * DataSetUpdateEvent.java
 *
 * Created on October 23, 2003, 10:22 AM
 */

package edu.uiowa.physics.pw.das.dataset;

import edu.uiowa.physics.pw.das.event.*;
import edu.uiowa.physics.pw.das.util.DasProgressMonitor;

/**
 *
 * @author  jbf
 */
public class DataSetUpdateEvent extends DasEvent {
    
    private DataSet dataSet;
    private Exception exception;
    private DasProgressMonitor monitor;
    
    /**
     * @deprecated use {link
     * #DataSetUpdateEvent(Object)}
     */
    public DataSetUpdateEvent(DataSetDescriptor source) {
        this((Object)source);
    }
    
    /**
     * @deprecated use {link
     * #DataSetUpdateEvent(Object,DataSet);
     */
    public DataSetUpdateEvent(DataSetDescriptor source, DataSet dataSet) {
        this((Object)source,dataSet);        
    }
    
    /**
     * @deprecated use {link
     * #DataSetUpdateEvent(Object,Exception);
     */
    public DataSetUpdateEvent(DataSetDescriptor source, Exception exception) {
        this((Object)source,exception);        
    }
    
    public DataSetUpdateEvent( Object source) {
        // null indicates that the state has changed and the consumer needs to read.
        super(source);
    }
    
    /** Creates a new instance of DataSetUpdateEvent */
    public DataSetUpdateEvent( Object source, DataSet dataSet) {
        super(source);
        this.dataSet = dataSet;
    }
    
    public DataSetUpdateEvent( Object source, Exception exception) {
        super(source);
        this.exception = exception;
    }    
    
    public DataSet getDataSet() {
        return dataSet;
    }
    
    public Exception getException() {
        return exception;
    }
    
    /**
     * temporary kludge that allows for identification of event to request
     */
    public void setMonitor( DasProgressMonitor monitor ) {
        this.monitor= monitor;
    }
    
    public DasProgressMonitor getMonitor() {
        return monitor;
    }
}
