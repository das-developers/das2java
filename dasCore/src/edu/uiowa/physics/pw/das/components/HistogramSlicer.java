/* File: HistogramSlicer.java
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

package edu.uiowa.physics.pw.das.components;

import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.datum.format.*;
import edu.uiowa.physics.pw.das.event.DataPointSelectionEvent;
import edu.uiowa.physics.pw.das.event.DataPointSelectionListener;
import edu.uiowa.physics.pw.das.graph.*;
import edu.uiowa.physics.pw.das.util.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import javax.swing.*;


public class HistogramSlicer extends DasPlot implements DataPointSelectionListener {
    
    private JDialog popupWindow;
    private Datum xValue;
    private SpectrogramRenderer renderer;
    
    private HistogramSlicer(SpectrogramRenderer parentRenderer, DasAxis xAxis, DasAxis yAxis) {
        super(xAxis, yAxis);
        renderer = parentRenderer;
        SymbolLineRenderer symLineRenderer= new SymbolLineRenderer((DataSetDescriptor)null);
        symLineRenderer.setPsymConnector(PsymConnector.PSYM10);
        symLineRenderer.setLineWidth(1.0f);
        symLineRenderer.setPsym(Psym.CROSS);
        addRenderer(symLineRenderer);
    }
    
    public static HistogramSlicer createSlicer(SpectrogramRenderer renderer) {
        DasAxis sourceZAxis = renderer.getColorBar();
        DasAxis xAxis = new DasAxis(sourceZAxis.getDataMinimum(), sourceZAxis.getDataMaximum(), DasAxis.HORIZONTAL, sourceZAxis.isLog());
        DasAxis yAxis = new DasAxis(Datum.create(0.0), Datum.create(1.0), DasAxis.VERTICAL);
        return new HistogramSlicer(renderer, xAxis, yAxis);
    }
    
    public void showPopup() {
        if (SwingUtilities.isEventDispatchThread()) {
            showPopupImpl();
        }
        else {
            Runnable r = new Runnable() {
                public void run() {
                    showPopupImpl();
                }
            };
        }
    }
    
    /** This method should ONLY be called by the AWT event thread */
    private void showPopupImpl() {
        if (popupWindow == null) {
            createPopup();
        }
        popupWindow.setVisible(true);
    }
    
    /** This method should ONLY be called by the AWT event thread */
    private void createPopup() {
        int width = renderer.getParent().getCanvas().getWidth() / 2;
        int height = renderer.getParent().getCanvas().getHeight() / 2;
        DasCanvas canvas = new DasCanvas(width, height);
        DasRow row = new DasRow(canvas, 0.1, 0.9);
        DasColumn column = new DasColumn(canvas, 0.1, 0.9);
        canvas.add(this, row, column);
        
        JPanel content = new JPanel(new BorderLayout());
        
        JPanel buttonPanel = new JPanel();
        BoxLayout buttonLayout = new BoxLayout(buttonPanel, BoxLayout.X_AXIS);
        JButton close = new JButton("Hide Window");
        close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                popupWindow.setVisible(false);
            }
        });
        buttonPanel.setLayout(buttonLayout);
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(close);
        
        content.add(canvas, BorderLayout.CENTER);
        content.add(buttonPanel, BorderLayout.SOUTH);
        
        Window parentWindow = SwingUtilities.getWindowAncestor(renderer.getParent());
        if (parentWindow instanceof Frame) {
            popupWindow = new JDialog((Frame)parentWindow);
        }
        else if (parentWindow instanceof Dialog) {
            popupWindow = new JDialog((Dialog)parentWindow);
        }
        else {
            popupWindow = new JDialog();
        }
        popupWindow.setTitle("Histogram Slicer");
        popupWindow.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        popupWindow.setContentPane(content);
        popupWindow.pack();
                
        Point parentLocation = new Point();
        SwingUtilities.convertPointToScreen(parentLocation, renderer.getParent().getCanvas());
        popupWindow.setLocation(parentLocation.x + renderer.getParent().getCanvas().getWidth(),parentLocation.y + height);
    }
    
    public void DataPointSelected(DataPointSelectionEvent e) {
                
        DataSet ds = e.getDataSet();
        if (ds==null || !(ds instanceof TableDataSet)) {
            return;
        }
        
        Datum yValue = e.getY();
        xValue = e.getX();
        
        TableDataSet tds = (TableDataSet)ds;
        //TableDataSet tds = (TableDataSet)renderer.getDataSet();
        
        int itable= TableUtil.tableIndexAt( tds, DataSetUtil.closestColumn( tds, e.getX() ) );
        VectorDataSet sliceDataSet= tds.getYSlice( TableUtil.closestRow( tds, itable, e.getY() ), itable );
        
        DasColorBar cb = renderer.getColorBar();
        DasAxis xAxis = getXAxis();
        if (!xAxis.getUnits().equals(cb.getUnits())) {
            xAxis.setUnits(cb.getUnits());
            xAxis.setDataRange(cb.getDataMinimum(), cb.getDataMaximum());
            xAxis.setLog(cb.isLog());
        }
        
        VectorDataSet hist = getHistogram(sliceDataSet);
        
        getRenderer(0).setDataSet(hist);

        DatumFormatter formatter;
        if ( xValue.getUnits() instanceof TimeLocationUnits ) {
            formatter= TimeDatumFormatter.DEFAULT;
        } else {
            formatter= xValue.getFormatter();
        }
            
        if (!(popupWindow == null || popupWindow.isVisible()) || getCanvas() == null) {
            showPopup();
        }
        else {
            repaint();
        }
    }
    
    /** This should handle non-log data too probably. */
    public VectorDataSet getHistogram(VectorDataSet vds) {
        final int BINS_PER_DECADE = 8;
        DasAxis zAxis = renderer.getColorBar();
        Units yUnits = zAxis.getUnits();
        double min = getXAxis().getDataMinimum(yUnits);
        double max = getXAxis().getDataMaximum(yUnits);
        int sampleCount = vds.getXLength();
        if (zAxis.isLog()) {
            double minLog = Math.floor(DasMath.log10(min));
            double maxLog = Math.ceil(DasMath.log10(max));
            int binCount = (int)(maxLog - minLog) * BINS_PER_DECADE;  //4 bins per decade
            double[] bins = new double[binCount];
            for (int i = 0; i < vds.getXLength(); i++) {
                double y = vds.getDouble(i, yUnits);
                if (yUnits.isFill(y) || Double.isNaN(y)) {
                    sampleCount--;
                    continue;
                }
                double yLog = DasMath.log10(y);
                if (yLog < minLog) {
                    double newMinLog = Math.floor(yLog);
                    int binCountDelta = (int)(minLog - newMinLog) * BINS_PER_DECADE;
                    binCount += binCountDelta;
                    double[] newBins = new double[binCount];
                    System.arraycopy(bins, 0, newBins, binCountDelta, bins.length);
                    minLog = newMinLog;
                    bins = newBins;
                }
                else if (yLog >= maxLog) {
                    double newMaxLog = Math.ceil(yLog+0.001);
                    int binCountDelta = (int)(newMaxLog - maxLog) * BINS_PER_DECADE;
                    binCount += binCountDelta;
                    double[] newBins = new double[binCount];
                    System.arraycopy(bins, 0, newBins, 0, bins.length);
                    maxLog = newMaxLog;
                    bins = newBins;
                }
                int index = (int)((yLog - minLog) * (double)BINS_PER_DECADE);
                if (index >= 0 && index < bins.length) {
                    bins[index] += 1.0;
                }
            }
            double[] x = new double[binCount];
            for (int index = 0; index < binCount; index++) {
                x[index] = DasMath.exp10(minLog + (index / (double)BINS_PER_DECADE));
                bins[index] = bins[index] / (double)sampleCount;
            }
            return new DefaultVectorDataSet(x, yUnits, bins, Units.dimensionless, Collections.EMPTY_MAP);
        }
        else {
            //Should add logic that determine bin size instead of just using 1.0
            min = Math.floor(min);
            max = Math.ceil(max);
            int binCount = (int)(max - min);
            double[] bins = new double[binCount];
            for (int i = 0; i < vds.getXLength(); i++) {
                double y = vds.getDouble(i, yUnits);
                if (yUnits.isFill(y) || Double.isNaN(y)) {
                    sampleCount--;
                    continue;
                }
                if (y < min) {
                    double newMin = Math.floor(y);
                    int binCountDelta = (int)(min - newMin);
                    binCount += binCountDelta;
                    double[] newBins = new double[binCount];
                    System.arraycopy(bins, 0, newBins, binCountDelta, bins.length);
                    min = newMin;
                    bins = newBins;
                }
                else if (y >= max) {
                    double newMax = Math.ceil(y+0.001);
                    int binCountDelta = (int)(newMax - max);
                    binCount += binCountDelta;
                    double[] newBins = new double[binCount];
                    System.arraycopy(bins, 0, newBins, 0, bins.length);
                    max = newMax;
                    bins = newBins;
                }
                int index = (int)(y - min);
                if (index >= 0 && index < bins.length) {
                    bins[index] += 1.0;
                }
            }
            double[] x = new double[binCount];
            for (int index = 0; index < binCount; index++) {
                x[index] = min + (double)index + 0.5;
                bins[index] = bins[index] / (double)sampleCount;
            }
            return new DefaultVectorDataSet(x, yUnits, bins, Units.dimensionless, Collections.EMPTY_MAP);
        }
    }
    
    protected void uninstallComponent() {
        super.uninstallComponent();
    }
    
    protected void installComponent() {
        super.installComponent();
    }
    
}
