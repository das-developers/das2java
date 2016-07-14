/* File: DataRequestThread.java
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

package org.das2.dataset;

import org.das2.util.monitor.ProgressMonitor;
import org.das2.DasException;
import org.das2.dataset.DataRequestor;
import org.das2.datum.Datum;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Edward West
 */
public class DataRequestThread extends Thread {
    
    private static int threadCount = 0;
    
    private Object lock = new String("DATA_REQUEST_LOCK");
    
    private DataRequest currentRequest;
    
    private List queue = Collections.synchronizedList(new LinkedList());
    
    /** Creates a new instance of DataRequestThread */
    public DataRequestThread() {
        //Set the name for debugging purposes.
        setName("DataRequest_" + threadCount++);
        setDaemon(true);
        start();  //Start it up
    }
    
    /**
     * Begins a data reqest operation that will be executed
     * in a separate thread and pass the resulting DataSet to the
     * org.das2.event.DataRequestListener#finished(org.das2.event.DataRequestEvent)
     * finished() method of the <code>DataRequestor</code>
     * specified.
     * 
     * @param dsd the <code>DataSetDescriptor</code> used to obtain
     *      the <code>DataSet</code>
     * @param start the start of the requested time interval
     * @param end the end of the requested time interval
     * @param resolution the requested resolution of the data set
     * @param requestor <code>DataRequestor</code> that is notified
     *      when the data loading operation is complete.
     */
    public void request(DataSetDescriptor dsd, 
        Datum start, Datum end, Datum resolution, DataRequestor requestor, ProgressMonitor monitor)
        throws InterruptedException {

        requestInternal(new DataRequest(dsd, start, end, resolution, requestor, monitor));
        
    }
    /**
     * Begins a data reqest operation that will be executed
     * in a separate thread and pass the resulting DataSet to the
     * org.das2.event.DataRequestListener#finished(org.das2.event.DataRequestEvent)
     * finished() method of the <code>DataRequestor</code>
     * specified.
     *
     * This method does not return until after the data loading is complete
     * or the request had been canceled.
     * 
     * @param dsd the <code>DataSetDescriptor</code> used to obtain
     *      the <code>DataSet</code>
     * @param params extra parameters passed to the 
     *      DataSetDescriptor#getDataSet(org.das2.util.Datum,org.das2.util.Datum,org.das2.util.Datum,org.das2.util.monitor.ProgressMonitor)
     *      getDataSet() method.  (TODO: these are ignored)
     * @param start the start of the requested time interval
     * @param end the end of the requested time interval
     * @param resolution the requested resolution of the data set
     * @param requestor <code>DataRequestor</code> that is notified
     *      when the data loading operation is complete.
     */
    public void requestAndWait(DataSetDescriptor dsd, Object params,
        Datum start, Datum end, Datum resolution, DataRequestor requestor, ProgressMonitor monitor)
        throws InterruptedException {

        DataRequest request = new DataRequest(dsd, start, end, resolution, requestor, monitor);

        //Wait till thread is done loading
        synchronized (request) {
            requestInternal(request);
            request.wait();
        }
    }
    
    private void requestInternal(DataRequest request) {
        queue.add(request);

        //Notify loading thread if it is waiting on object lock.
        synchronized (lock) {
            lock.notify();
        }
    }
    
    public void cancelCurrentRequest() throws InterruptedException {
        synchronized (lock) {
            if (this.isAlive()) {
                this.interrupt();
                lock.wait();
            }
        }
    }
    
    public void run() {
        if (Thread.currentThread() != this) {
            throw new IllegalStateException(
                "This method should not be invoked directly");
        }
        
        while (true) {
            while (!queue.isEmpty()) {
                currentRequest = (DataRequest)queue.remove(0);
                try {
                    DataSet ds = currentRequest.dsd.getDataSet(
                        currentRequest.start,
                        currentRequest.end,
                        currentRequest.resolution,
                        currentRequest.monitor);
                    currentRequest.requestor.finished(ds);
                }
                catch (DasException e) {
                    currentRequest.requestor.exception(e);
                }
                finally {
                    synchronized (currentRequest) {
                        currentRequest.notifyAll();
                    }
                }
            }
            synchronized (lock) {
                try {
                    lock.wait();
                }
                catch(InterruptedException ie) {
                    return;
                }
            }
        }
    }

    private static class DataRequest {
        DataSetDescriptor dsd;
        Datum start;
        Datum end;
        Object params;
        Datum resolution;
        DataRequestor requestor;
        ProgressMonitor monitor;
        DataRequest(DataSetDescriptor dsd, Datum start,
                    Datum end, Datum resolution,
                    DataRequestor requestor, ProgressMonitor monitor) {
            this.dsd = dsd;
            this.start = start;
            this.end = end;
            this.resolution = resolution;
            this.requestor = requestor;
            this.monitor = monitor;
        }
    }
    
}
