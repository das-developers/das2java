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
import edu.uiowa.physics.pw.das.datum.Units;
import edu.uiowa.physics.pw.das.stream.*;
import java.nio.ByteBuffer;

/**
 *
 * @author Edward E. West
 */
public class DataSetStreamHandler implements StreamHandler {
    
    StreamHandlerDelegate delegate;
    StreamDescriptor sd;
    
    /** Creates a new instance of DataSetStreamHandler */
    public DataSetStreamHandler() {
    }
    
    public void packet(PacketDescriptor pd, ByteBuffer buffer) throws StreamException {
        ensureNotNullDelegate();
        delegate.packet(pd, buffer);
    }
    
    public void packetDescriptor(PacketDescriptor pd) throws StreamException {
        SkeletonDescriptor descriptor = pd.getYDescriptor(0);
        if (descriptor instanceof StreamMultiYDescriptor) {
            delegate = new VectorDataSetStreamHandler(pd);
        }
        else if (descriptor instanceof StreamYScanDescriptor) {
            delegate = new TableDataSetStreamHandler(pd);
        }
    }
    
    public void streamClosed(StreamDescriptor sd) throws StreamException {
        ensureNotNullDelegate();
        delegate.streamClosed(sd);
    }
    
    public void streamDescriptor(StreamDescriptor sd) throws StreamException {
        this.sd = sd;
    }
    
    public void streamException(StreamException se) throws StreamException {
    }
    
    public DataSet getDataSet() {
        return delegate.getDataSet();
    }
    
    private void ensureNotNullDelegate() {
        if (delegate == null) {
            throw new IllegalStateException("Null delegate");
        }
    }
    
    private static double getXWithBase(double x, Units xUnits, Datum base) {
        if (base == null) {
            return x;
        }
        else {
            x = xUnits.convertDoubleTo(base.getUnits().getOffsetUnits(), x);
            return base.doubleValue(base.getUnits()) + x;
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
            Units xUnits = pd.getXDescriptor().getUnits();
            Units yUnits = y.getUnits();
            builder = new VectorDataSetBuilder(xUnits,yUnits);                        
            this.packetDescriptor(pd);
        }
        
        public void packet(PacketDescriptor pd, ByteBuffer buffer) throws StreamException {
            Datum base = pd.getXDescriptor().getBase();
            for (int i = 0; i < pd.getYCount(); i++) {
                if (pd.getYDescriptor(i) instanceof StreamMultiYDescriptor) {
                    StreamMultiYDescriptor my = (StreamMultiYDescriptor)pd.getYDescriptor(i);
                    pd.getXDescriptor().read(buffer, doubles, 0);
                    double x = getXWithBase(doubles[0], pd.getXDescriptor().getUnits(), base);
                    my.read(buffer, doubles, 0);
                    double y = doubles[0];
                    builder.insertY(x, y);
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
            return builder.toVectorDataSet();
        }
        
    }
    
    private class TableDataSetStreamHandler implements StreamHandlerDelegate {
        
        private TableDataSetBuilder builder;
        
        private double[] doubles;
        
        private TableDataSetStreamHandler(PacketDescriptor pd) throws StreamException {
            StreamYScanDescriptor y = (StreamYScanDescriptor)pd.getYDescriptor(0);
            Datum base = pd.getXDescriptor().getBase();
            Units xUnits = base != null ? base.getUnits() : pd.getXDescriptor().getUnits();
            Units yUnits = y.getYUnits();
            Units zUnits = y.getZUnits();
            builder = new TableDataSetBuilder(xUnits, yUnits, zUnits);
            this.packetDescriptor(pd);
        }

        public void packet(PacketDescriptor pd, ByteBuffer buffer) throws StreamException {
            StreamYScanDescriptor yscan = (StreamYScanDescriptor)pd.getYDescriptor(0);
            Datum base = pd.getXDescriptor().getBase();
            for (int i = 0; i < pd.getYCount(); i++) {
                if (pd.getYDescriptor(i) instanceof StreamYScanDescriptor) {
                    yscan = (StreamYScanDescriptor)pd.getYDescriptor(i);
                    if (i != 0) {
                        builder.addPlane(yscan.getName(), yscan.getZUnits());
                    }
                    pd.getXDescriptor().read(buffer, doubles, 0);
                    double x = getXWithBase(doubles[0], pd.getXDescriptor().getUnits(), base);
                    yscan.read(buffer, doubles, 0);
                    builder.insertYScan(x, yscan.getYTags(), doubles);
                }
                else {
                    throw new StreamException("Mixed data sets are not currently supported");
                }
            }
        }
        
        public void packetDescriptor(PacketDescriptor pd) throws StreamException {
            StreamYScanDescriptor y = (StreamYScanDescriptor)pd.getYDescriptor(0);
            if (doubles == null || doubles.length < y.getNItems()) {
                doubles = new double[y.getNItems()];
            }
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
            return builder.toTableDataSet();
        }
        
    }
    
}
