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
import edu.uiowa.physics.pw.das.client.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class HorizontalSpectrogramSlicer
extends DasPlot implements DataPointSelectionListener {
    
    private JFrame popupWindow;
    private Datum xValue;
    private edu.uiowa.physics.pw.das.graph.Renderer renderer;
    
    private HorizontalSpectrogramSlicer(DasAxis xAxis, DasAxis yAxis, DasRow row, DasColumn column) {
        super(xAxis, yAxis, row, column);
        renderer= new SymbolLineRenderer((DataSetDescriptor)null);
        addRenderer(renderer);
    }
    
    public static HorizontalSpectrogramSlicer createSlicer(DasPlot plot, TableDataSetConsumer dataSetConsumer, DasRow row, DasColumn column) {
        DasAxis sourceXAxis = plot.getXAxis();
        
        DasAxis xAxis = sourceXAxis.createAttachedAxis(row, column, DasAxis.HORIZONTAL);
        DasAxis yAxis = dataSetConsumer.getZAxis().createAttachedAxis(row, column, DasAxis.VERTICAL);
        
        return new HorizontalSpectrogramSlicer(xAxis, yAxis, row, column);
    }
    
    public static HorizontalSpectrogramSlicer createPopupSlicer(DasPlot plot, TableDataSetConsumer dataSetConsumer, int width, int height) {
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
        
        long xxx[]= { 0,0,0,0 };
        xxx[0] = System.currentTimeMillis()-e.birthMilli;
        
        DataSet ds = e.getDataSet();
        if (ds==null || !(ds instanceof TableDataSet))
            return;
        
        Datum yValue = e.getY();
        xValue = e.getX();
        
        TableDataSet tds = (TableDataSet)ds;
        
        int itable= TableUtil.tableIndexAt( tds, TableUtil.closestColumn( tds, e.getX() ) );
        VectorDataSet sliceDataSet= tds.getYSlice( TableUtil.closestRow( tds, itable, e.getY() ), itable );
        
        xxx[1]= System.currentTimeMillis()-e.birthMilli;
        
        String xAsString;
        xAsString= ""+xValue;
        renderer.setDataSet(sliceDataSet);
        
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
        int ix= this.getXAxis().transform(xValue);
        DasRow row= this.getRow();
        int iy0= row.getDMinimum();
        int iy1= row.getDMaximum();
        g.drawLine(ix+3,iy0,ix,iy0+3);
        g.drawLine(ix-3,iy0,ix,iy0+3);
        g.drawLine(ix+3,iy1,ix,iy1-3);
        g.drawLine(ix-3,iy1,ix,iy1-3);
    }
    
    public static HorizontalSpectrogramSlicer createPopupSlicer(DasZAxisPlot plot, int width, int height) {
        return createPopupSlicer( (DasPlot)plot, (TableDataSetConsumer)plot, width, height);
    }
    
    protected void uninstallComponent() {
        super.uninstallComponent();
    }
    
    protected void installComponent() {
        super.installComponent();
        getCanvas().getGlassPane().setVisible(false);
    }
    
}
