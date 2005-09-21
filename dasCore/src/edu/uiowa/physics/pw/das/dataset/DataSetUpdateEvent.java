/*
 * DataSetUpdateEvent.java
 *
 * Created on October 23, 2003, 10:22 AM
 */

package edu.uiowa.physics.pw.das.dataset;

import edu.uiowa.physics.pw.das.event.*;

/**
 *
 * @author  jbf
 */
public class DataSetUpdateEvent extends DasEvent {
    
    private DataSet dataSet;
    private Exception exception;
    
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
    
}
