/* File: HorizontalSpectrogramSlicer.java
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
import edu.uiowa.physics.pw.das.event.DataPointSelectionEvent;
import edu.uiowa.physics.pw.das.event.DataPointSelectionListener;
import edu.uiowa.physics.pw.das.graph.*;
import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.dataset.DataSet;
import edu.uiowa.physics.pw.das.dataset.XMultiYDataSet;
import edu.uiowa.physics.pw.das.dataset.XTaggedYScanDataSet;
import edu.uiowa.physics.pw.das.dataset.XTaggedYScanDataSetConsumer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class HorizontalSpectrogramSlicer
extends DasSymbolPlot implements DataPointSelectionListener {
    
    private JFrame popupWindow;
            
    private Datum xValue;
    
    private HorizontalSpectrogramSlicer(DasAxis xAxis, DasAxis yAxis, DasRow row, DasColumn column) {
        super((XMultiYDataSet)null, xAxis, yAxis, row, column);
    }
    
    public static HorizontalSpectrogramSlicer createSlicer(DasPlot plot, XTaggedYScanDataSetConsumer dataSetConsumer, DasRow row, DasColumn column) {
        DasAxis sourceXAxis = plot.getXAxis();
        
        DasAxis xAxis = sourceXAxis.createAttachedAxis(row, column, DasAxis.HORIZONTAL);
        DasAxis yAxis = dataSetConsumer.getZAxis().createAttachedAxis(row, column, DasAxis.VERTICAL);
        
        return new HorizontalSpectrogramSlicer(xAxis, yAxis, row, column);
    }
    
    public static HorizontalSpectrogramSlicer createPopupSlicer(DasPlot plot, XTaggedYScanDataSetConsumer dataSetConsumer, int width, int height) {
        DasCanvas canvas = new DasCanvas(width, height);
        DasRow row = new DasRow(canvas, 0.1, 0.9);
        DasColumn column = new DasColumn(canvas, 0.1, 0.9);
        final HorizontalSpectrogramSlicer slicer = createSlicer(plot, dataSetConsumer, row, column);
        canvas.add(slicer);
        canvas.add(slicer.getXAxis());
        canvas.add(slicer.getYAxis());
        
        JPanel content = new JPanel(new BorderLayout());
        
        JPanel buttonPanel = new JPanel();
        BoxLayout buttonLayout = new BoxLayout(buttonPanel, BoxLayout.X_AXIS);
        JButton close = new JButton("Hide Window");
        close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                slicer.popupWindow.setVisible(false);
            }
        });
        buttonPanel.setLayout(buttonLayout);
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(close);
        
        content.add(canvas, BorderLayout.CENTER);
        content.add(buttonPanel, BorderLayout.SOUTH);
        
        slicer.popupWindow = new JFrame("Horizontal Slicer");
        slicer.popupWindow.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        slicer.popupWindow.setContentPane(content);
        slicer.popupWindow.pack();
        
        slicer.popupWindow.setLocation( 0,  600 );
        
        return slicer;
    }
    
    public void DataPointSelected(DataPointSelectionEvent e) {
	DataSet ds = e.getDataSet();
	if (ds==null || !(ds instanceof XTaggedYScanDataSet))
	    return;
	XTaggedYScanDataSet xtys = (XTaggedYScanDataSet)ds;
	double[] x = new double[xtys.data.length];
	double[] y = new double[xtys.data.length];
	Datum yValue = e.getY();
        xValue = e.getX();

        String xAsString;
        xAsString= ""+xValue;
        
        if ( xtys.getYUnits()!=yValue.getUnits() ) {
            yValue= yValue.convertTo(xtys.getYUnits());
        }
        
	double dy = Double.MAX_VALUE;
	int yIndex = 0;
	for (int i = 0; i < xtys.y_coordinate.length; i++) {
	    double delta = Math.abs(yValue.doubleValue(xtys.getYUnits())-xtys.y_coordinate[i]);
	    if (delta < dy) {
		dy = delta;
		yIndex = i;
	    }
	}
        
	for (int i = 0; i < x.length; i++) {
	    x[i] = xtys.data[i].x;
	}
	for (int i = 0; i < y.length; i++) {
	    y[i] = xtys.data[i].z[yIndex];
	}
	XMultiYDataSet xmy = XMultiYDataSet.create(x,xtys.getXUnits(),y,xtys.getZUnits());
	xmy.y_fill = xtys.getZFill();
	addData(xmy);
        setTitle("x: "+xAsString);
	if (popupWindow != null && !popupWindow.isVisible()) {
	    popupWindow.setVisible(true);
	}
	else {
	    repaint();
	}
    }
    
    public void drawContent(Graphics2D g) {
        super.drawContent(g);
        int ix= (int)this.getXAxis().transform(xValue);
        DasRow row= this.getRow();
        int iy0= (int)row.getDMinimum();
        int iy1= (int)row.getDMaximum();
        g.drawLine(ix+3,iy0,ix,iy0+3);
        g.drawLine(ix-3,iy0,ix,iy0+3);
        g.drawLine(ix+3,iy1,ix,iy1-3);
        g.drawLine(ix-3,iy1,ix,iy1-3);
    }
    
    public static HorizontalSpectrogramSlicer createPopupSlicer(DasZAxisPlot plot, int width, int height) {
        return createPopupSlicer( (DasPlot)plot, (XTaggedYScanDataSetConsumer)plot, width, height);
    }
    
}
