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

import edu.uiowa.physics.pw.das.*;
import edu.uiowa.physics.pw.das.dataset.DataSet;
import edu.uiowa.physics.pw.das.dataset.TableDataSetBuilder;
import edu.uiowa.physics.pw.das.dataset.VectorDataSetBuilder;
import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.datum.DatumVector;
import edu.uiowa.physics.pw.das.datum.Units;
import edu.uiowa.physics.pw.das.stream.*;
import edu.uiowa.physics.pw.das.system.DasLogger;
import edu.uiowa.physics.pw.das.util.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Edward E. West
 */
public class DataSetStreamHandler implements StreamHandler {
    
    StreamHandlerDelegate delegate;
    StreamDescriptor sd;
    Map extraProperties;
    DasProgressMonitor monitor;
    int totalPacketCount= -1;   
    int taskSize= -1;
    int packetCount= 0;
       
    private static final Logger logger= DasLogger.getLogger(DasApplication.DATA_TRANSFER_LOG);
    
    public DataSetStreamHandler( Map extraProperties, DasProgressMonitor monitor ) {        
        this.extraProperties = new HashMap(extraProperties);
        this.monitor= monitor==null ? DasProgressMonitor.NULL : monitor;        
    }
    
    public void streamDescriptor(StreamDescriptor sd) throws StreamException {
        logger.finest("got stream descriptor");
        this.sd = sd;
        Object o;
        if ( ( o= sd.getProperty("taskSize") )!=null ) {
            this.taskSize= ((Integer)o).intValue();
            monitor.setTaskSize( taskSize );
        } else if ( ( o= sd.getProperty("packetCount" ) )!=null ) {
            this.totalPacketCount= ((Integer)o).intValue();
            monitor.setTaskSize( totalPacketCount );
        } 
        if ( ( o=sd.getProperty("pid") )!=null ) {
            logger.fine("stream pid="+o);
        }
    }
    
    public void packetDescriptor(PacketDescriptor pd) throws StreamException {               
        logger.finest("got packet descriptor");
        if (delegate == null) {
            SkeletonDescriptor descriptor = pd.getYDescriptor(0);
            if (descriptor instanceof StreamMultiYDescriptor) {
                logger.fine("using VectorDS delegate");
                delegate = new VectorDataSetStreamHandler(pd);
            }
            else if (descriptor instanceof StreamYScanDescriptor) {
                logger.fine("using TableDS delegate");
                delegate = new TableDataSetStreamHandler(pd);
            }
        }
        else {
            delegate.packetDescriptor(pd);
        }
    }
    
    public void packet(PacketDescriptor pd, Datum xTag, DatumVector[] vectors) throws StreamException {                
        logger.finest("got packet");
        ensureNotNullDelegate();
        delegate.packet(pd, xTag, vectors);
        packetCount++;
        if ( totalPacketCount!=-1 ) {
            monitor.setTaskProgress(packetCount);
        }
    }
    
    public void streamClosed(StreamDescriptor sd) throws StreamException {
        logger.finest("got streamClosed");
        if (delegate != null) {
            delegate.streamClosed(sd);
        }
    }
    
    public void streamException(StreamException se) throws StreamException {
        logger.finest("got stream exception");
    }
    
    public void streamComment(StreamComment sc) throws StreamException {
        logger.finest("got stream comment: "+sc);
        if ( sc.getType().equals(sc.TYPE_TASK_PROGRESS) && taskSize!=-1 ) {
            if ( !monitor.isCancelled() ) monitor.setTaskProgress( Long.parseLong(sc.getValue() ) );            
        } else if ( sc.getType().matches(sc.TYPE_LOG) ) {
            String level= sc.getType().substring(4);
            Level l= Level.parse(level.toUpperCase());
            logger.log(l,sc.getValue());
            monitor.setProgressMessage(sc.getValue());
        }
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
            logger.fine("got packet descriptor: "+pd);
            for (int i = 1; i < pd.getYCount(); i++) {
                StreamMultiYDescriptor y = (StreamMultiYDescriptor)pd.getYDescriptor(i);
                builder.addPlane(y.getName(),y.getUnits());
            }
        }
        
        public void streamClosed(StreamDescriptor sd) throws StreamException {}
        
        public void streamDescriptor(StreamDescriptor sd) throws StreamException {}
        
        public void streamException(StreamException se) throws StreamException {}
        
        public void streamComment(StreamComment sc) throws StreamException {}
        
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
            Map p= pd.getProperties();
            for ( Iterator i=p.keySet().iterator(); i.hasNext(); ) {
                String key= (String)i.next();
                builder.setProperty( key, p.get(key) );
            }
        }
        
        public void streamClosed(StreamDescriptor sd) throws StreamException {}
        
        public void streamDescriptor(StreamDescriptor sd) throws StreamException {}
        
        public void streamException(StreamException se) throws StreamException {}
        
        public void streamComment(StreamComment sc) throws StreamException {}
        
        public DataSet getDataSet() {
            builder.addProperties(sd.getProperties());
            builder.addProperties(extraProperties);
            return builder.toTableDataSet();
        }
        
    }
    
}
