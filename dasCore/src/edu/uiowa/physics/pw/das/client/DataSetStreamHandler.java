/* File: DataSetStreamHandler.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on February 11, 2004, 4:26 PM
 *      by Edward E. West <eew@space.physics.uiowa.edu>
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

package edu.uiowa.physics.pw.das.client;

import edu.uiowa.physics.pw.das.dataset.DataSet;
import edu.uiowa.physics.pw.das.dataset.TableDataSetBuilder;
import edu.uiowa.physics.pw.das.dataset.VectorDataSetBuilder;
import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.datum.DatumVector;
import edu.uiowa.physics.pw.das.datum.Units;
import edu.uiowa.physics.pw.das.stream.*;
import edu.uiowa.physics.pw.das.util.*;
import java.nio.ByteBuffer;
import java.util.*;

/**
 *
 * @author Edward E. West
 */
public class DataSetStreamHandler implements StreamHandler {
    
    StreamHandlerDelegate delegate;
    StreamDescriptor sd;
    Map extraProperties;
    
    DasProgressMonitor monitor;
    static final int ntasks= 100;
    boolean [] taskStarted= new boolean[ntasks];
    Datum start, taskWidth;
    
    /** Creates a new instance of DataSetStreamHandler */
    public DataSetStreamHandler( Map extraProperties, DasProgressMonitor _monitor, Datum _start, Datum end ) {
        //this.monitor= _monitor;
        //this.start= _start;
        //this.taskWidth= end.subtract(_start).divide(ntasks);
        //monitor.setTaskSize(ntasks);
        for ( int i=0; i<taskStarted.length; i++ ) taskStarted[i]= false;
        this.extraProperties = new HashMap(extraProperties);
    }
    
    public void packet(PacketDescriptor pd, Datum xTag, DatumVector[] vectors) throws StreamException {                
        ensureNotNullDelegate();
        delegate.packet(pd, xTag, vectors);
        //Units u= taskWidth.getUnits();
        //int itask= (int)( xTag.subtract(start).doubleValue(u) / taskWidth.doubleValue(u) );
        //if ( itask<0 ) itask=0;
        //if ( itask>=ntasks ) itask=ntasks-1;
        //boolean monitorUpdateNeeded= taskStarted[itask]==false;
        //taskStarted[itask]= true;
        //monitor.setTaskProgress( itask );
        //if ( monitorUpdateNeeded ) {
        //    updateMonitor();
        //}
    }
    
    private void updateMonitor() {
        int tasksStarted=0;
        for ( int i=0; i<ntasks; i++ ) {
            if ( taskStarted[i] ) tasksStarted++;
        }
        monitor.setTaskProgress(tasksStarted);
    }
    
    public void packetDescriptor(PacketDescriptor pd) throws StreamException {        
        if (delegate == null) {
            SkeletonDescriptor descriptor = pd.getYDescriptor(0);
            if (descriptor instanceof StreamMultiYDescriptor) {
                delegate = new VectorDataSetStreamHandler(pd);
            }
            else if (descriptor instanceof StreamYScanDescriptor) {
                delegate = new TableDataSetStreamHandler(pd);
            }
        }
        else {
            delegate.packetDescriptor(pd);
        }
    }
    
    public void streamClosed(StreamDescriptor sd) throws StreamException {
        if (delegate != null) {
            delegate.streamClosed(sd);
        }
    }
    
    public void streamDescriptor(StreamDescriptor sd) throws StreamException {
        this.sd = sd;
    }
    
    public void streamException(StreamException se) throws StreamException {
    }
    
    public DataSet getDataSet() {
        if (delegate == null) {
            return null;
        }
        else {
            return delegate.getDataSet();
        }
    }
    
    private void ensureNotNullDelegate() {
        if (delegate == null) {
            throw new IllegalStateException("Null delegate");
        }
    }
    
    private static double getXWithBase(Datum base, Datum x) {
        if (base == null) {
            return x.doubleValue(x.getUnits());
        }
        else {
            return base.doubleValue(base.getUnits()) + x.doubleValue(base.getUnits().getOffsetUnits());
        }
    }
    
    private static interface StreamHandlerDelegate extends StreamHandler {
        DataSet getDataSet();
    }
    
    private class VectorDataSetStreamHandler implements StreamHandlerDelegate {
        
        private VectorDataSetBuilder builder;
        
        private double[] doubles = new double[1];
        
        private VectorDataSetStreamHandler(PacketDescriptor pd) throws StreamException {
            StreamMultiYDescriptor y = (StreamMultiYDescriptor)pd.getYDescriptor(0);
            Datum base = pd.getXDescriptor().getBase();
            Units xUnits = base == null ? pd.getXDescriptor().getUnits() : base.getUnits();
            Units yUnits = y.getUnits();
            builder = new VectorDataSetBuilder(xUnits,yUnits);
            this.packetDescriptor(pd);
        }
        
        public void packet(PacketDescriptor pd, Datum xTag, DatumVector[] vectors) throws StreamException {
            Datum base = pd.getXDescriptor().getBase();
            double x = getXWithBase(base, xTag);
            for (int i = 0; i < pd.getYCount(); i++) {
                if (pd.getYDescriptor(i) instanceof StreamMultiYDescriptor) {
                    StreamMultiYDescriptor my = (StreamMultiYDescriptor)pd.getYDescriptor(i);
                    double y = vectors[i].doubleValue(0, my.getUnits());
                    if (i != 0) {
                        builder.insertY(x, y, my.getName());
                    }
                    else {
                        builder.insertY(x, y);
                    }
                }
                else {
                    throw new StreamException("Mixed data sets are not currently supported");
                }
            }
        }
        
        public void packetDescriptor(PacketDescriptor pd) throws StreamException {
            for (int i = 1; i < pd.getYCount(); i++) {
                StreamMultiYDescriptor y = (StreamMultiYDescriptor)pd.getYDescriptor(i);
                builder.addPlane(y.getName(),y.getUnits());
            }
        }
        
        public void streamClosed(StreamDescriptor sd) throws StreamException {}
        
        public void streamDescriptor(StreamDescriptor sd) throws StreamException {}
        
        public void streamException(StreamException se) throws StreamException {}
        
        public DataSet getDataSet() {
            builder.addProperties(sd.getProperties());
            builder.addProperties(extraProperties);
            return builder.toVectorDataSet();
        }
        
    }
    
    private class TableDataSetStreamHandler implements StreamHandlerDelegate {
        
        private TableDataSetBuilder builder;
        
        private TableDataSetStreamHandler(PacketDescriptor pd) throws StreamException {
            StreamYScanDescriptor y = (StreamYScanDescriptor)pd.getYDescriptor(0);
            Datum base = pd.getXDescriptor().getBase();
            Units xUnits = base != null ? base.getUnits() : pd.getXDescriptor().getUnits();
            Units yUnits = y.getYUnits();
            Units zUnits = y.getZUnits();
            builder = new TableDataSetBuilder(xUnits, yUnits, zUnits);
            this.packetDescriptor(pd);
        }
        
        public void packet(PacketDescriptor pd, Datum xTag, DatumVector[] vectors) throws StreamException {
            StreamYScanDescriptor yscan = (StreamYScanDescriptor)pd.getYDescriptor(0);
            Datum base = pd.getXDescriptor().getBase();
            Datum x = base == null ? xTag : base.add(xTag);
            DatumVector y = DatumVector.newDatumVector(yscan.getYTags(), yscan.getYUnits());
            String[] planeIDs = new String[pd.getYCount()];
            for (int i = 0; i < pd.getYCount(); i++) {
                planeIDs[i] = ((StreamYScanDescriptor)pd.getYDescriptor(i)).getName();
            }
            builder.insertYScan(x, y, vectors, planeIDs);
        }
        
        public void packetDescriptor(PacketDescriptor pd) throws StreamException {
            StreamYScanDescriptor y = (StreamYScanDescriptor)pd.getYDescriptor(0);
            for (int i = 1; i < pd.getYCount(); i++) {
                y = (StreamYScanDescriptor)pd.getYDescriptor(i);
                builder.addPlane(y.getName(), y.getZUnits());
            }
        }
        
        public void streamClosed(StreamDescriptor sd) throws StreamException {
        }
        
        public void streamDescriptor(StreamDescriptor sd) throws StreamException {}
        
        public void streamException(StreamException se) throws StreamException {}
        
        public DataSet getDataSet() {
            builder.addProperties(sd.getProperties());
            builder.addProperties(extraProperties);
            return builder.toTableDataSet();
        }
        
    }
    
}
