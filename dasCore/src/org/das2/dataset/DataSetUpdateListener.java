/*
 * DataSetUpdateListener.java
 *
 * Created on October 23, 2003, 10:20 AM
 */

package org.das2.dataset;

/**
 *
 * @author  jbf
 */
public interface DataSetUpdateListener extends java.util.EventListener {    
    public void dataSetUpdated( DataSetUpdateEvent e);
    //public void dataSetException( DataSetUpdateEvent e);
}
