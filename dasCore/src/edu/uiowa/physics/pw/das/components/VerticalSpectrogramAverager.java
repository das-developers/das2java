/* File: VerticalSpectrogramAverager.java
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

import edu.uiowa.physics.pw.das.client.*;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.event.DataRangeSelectionEvent;
import edu.uiowa.physics.pw.das.event.DataRangeSelectionListener;
import edu.uiowa.physics.pw.das.graph.*;
import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.dataset.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class VerticalSpectrogramAverager extends DasSymbolPlot implements DataRangeSelectionListener {
    
    private JFrame popupWindow;
    
    private Datum yValue;
    
    private VerticalSpectrogramAverager(DasAxis xAxis, DasAxis yAxis, DasRow row, DasColumn column) {
        super((XMultiYDataSet)null, xAxis, yAxis, row, column);
    }
    
    public static VerticalSpectrogramAverager createAverager(DasPlot plot, TableDataSetConsumer dataSetConsumer, DasRow row, DasColumn column) {
        DasAxis sourceYAxis = plot.getYAxis();
        
        DasAxis xAxis = sourceYAxis.createAttachedAxis(row, column, DasAxis.HORIZONTAL);
        DasAxis yAxis = dataSetConsumer.getZAxis().createAttachedAxis(row, column, DasAxis.VERTICAL);
        
        return new VerticalSpectrogramAverager(xAxis, yAxis, row, column);
    }
    
    public static VerticalSpectrogramAverager createPopupAverager(DasPlot plot, TableDataSetConsumer dataSetConsumer, int width, int height) {
        DasCanvas canvas = new DasCanvas(width, height);
        DasRow row = new DasRow(canvas, 0.1, 0.9);
        DasColumn column = new DasColumn(canvas, 0.1, 0.9);
        final VerticalSpectrogramAverager averager = createAverager(plot, dataSetConsumer, row, column);
        canvas.add(averager);
        canvas.add(averager.getXAxis());
        canvas.add(averager.getYAxis());
        
        JPanel content = new JPanel(new BorderLayout());
        
        JPanel buttonPanel = new JPanel();
        BoxLayout buttonLayout = new BoxLayout(buttonPanel, BoxLayout.X_AXIS);
        JButton close = new JButton("Hide Window");
        close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                averager.popupWindow.setVisible(false);
            }
        });
        buttonPanel.setLayout(buttonLayout);
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(close);
        
        content.add(canvas, BorderLayout.CENTER);
        content.add(buttonPanel, BorderLayout.SOUTH);
        
        averager.popupWindow = new JFrame("Vertical Averager");
        averager.popupWindow.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        averager.popupWindow.setContentPane(content);
        averager.popupWindow.pack();
        
        averager.popupWindow.setLocation(600, 20);
        
        return averager;
    }
    
    protected void drawContent(Graphics2D g) {
        super.drawContent(g);
        /*int ix= (int)this.getXAxis().transform(yValue);
        DasRow row= this.getRow();
        int iy0= (int)row.getDMinimum();
        int iy1= (int)row.getDMaximum();
        g.drawLine(ix+3,iy0,ix,iy0+3);
        g.drawLine(ix-3,iy0,ix,iy0+3);
        g.drawLine(ix+3,iy1,ix,iy1-3);
        g.drawLine(ix-3,iy1,ix,iy1-3);*/
    }
    
    public void DataRangeSelected(DataRangeSelectionEvent e) {
        DataSet ds = e.getDataSet();
        if (ds==null || !(ds instanceof XTaggedYScanDataSet))
            return;
        XTaggedYScanDataSet xtys = (XTaggedYScanDataSet)ds;
        double[] x = new double[xtys.y_coordinate.length];
        System.arraycopy(xtys.y_coordinate, 0, x, 0, xtys.y_coordinate.length);
        double[] y = new double[xtys.y_coordinate.length];
        double[] w = new double[xtys.y_coordinate.length];
        Datum xValue1 = e.getMinimum();
        Datum xValue2 = e.getMaximum();
    
        this.setTitle( ""+xValue1+" - "+xValue2 );
        
        if (xtys.getXUnits()!=xValue1.getUnits()) {
            xValue1.convertTo(xtys.getXUnits());
        }
        if (xtys.getXUnits()!=xValue2.getUnits()) {
            xValue2.convertTo(xtys.getXUnits());
        }
        
        double x1= xValue1.doubleValue(xtys.getXUnits());
        double x2= xValue2.doubleValue(xtys.getXUnits());
        
        double dx = Double.MAX_VALUE;
        int tagIndex = 0;
        XTaggedYScan[] weights= xtys.getWeights();
        for (int i = 0; i < xtys.data.length; i++) {
            if ( x1 < xtys.data[i].x && xtys.data[i].x < x2 ) {
                for (int j = 0; j < y.length; j++) {
                    y[j] += xtys.data[i].z[j] * weights[i].z[j];
                    w[j] += weights[i].z[j];
                }
            }
        }
        
        for (int j = 0; j < y.length; j++) {
            if ( w[j] > 0. ) {
                y[j]/= w[j];
            } else {
                y[j]= -1e31;
            }
        }
        
        //yValue= e.getY();
        
        XMultiYDataSet ds1= XMultiYDataSet.create(x,xtys.getYUnits(),y,xtys.getZUnits());
        ds1.y_fill= -1e31;
 
        addData(ds1);
        
        if (popupWindow != null && !popupWindow.isVisible()) {
            popupWindow.setVisible(true);
        }
        else {
            repaint();
        }
    }
    
}
