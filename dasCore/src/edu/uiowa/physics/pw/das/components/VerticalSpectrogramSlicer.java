/* File: VerticalSpectrogramSlicer.java
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
import edu.uiowa.physics.pw.das.event.DataPointSelectionEvent;
import edu.uiowa.physics.pw.das.event.DataPointSelectionListener;
import edu.uiowa.physics.pw.das.graph.*;
import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.dataset.DataSet;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class VerticalSpectrogramSlicer
extends DasPlot implements DataPointSelectionListener {
    
    private JFrame popupWindow;
    
    private Datum yValue;
    
    private int lastTagIndex=1;
    
    private long eventBirthMilli;
    private edu.uiowa.physics.pw.das.graph.Renderer renderer;
    
    private VerticalSpectrogramSlicer(DasAxis xAxis, DasAxis yAxis, DasRow row, DasColumn column) {
        super( xAxis, yAxis, row, column );
        renderer= new SymbolLineRenderer((DataSetDescriptor)null);
        addRenderer(renderer);                
    }
    
    public static VerticalSpectrogramSlicer createSlicer( DasPlot plot, TableDataSetConsumer dataSetConsumer,
    DasRow row, DasColumn column) {
        DasAxis sourceYAxis = plot.getYAxis();
        DasAxis sourceZAxis = dataSetConsumer.getZAxis();
        
        DasAxis xAxis = sourceYAxis.createAttachedAxis(row, column, DasAxis.HORIZONTAL);
        DasAxis yAxis = sourceZAxis.createAttachedAxis(row, column, DasAxis.VERTICAL);
        
        return new VerticalSpectrogramSlicer(xAxis, yAxis, row, column);
    }
    
    
    public static VerticalSpectrogramSlicer createPopupSlicer( DasZAxisPlot plot,
    int width, int height) {
        return createPopupSlicer( (DasPlot)plot, (TableDataSetConsumer)plot, width, height);
    }
    
    public static VerticalSpectrogramSlicer createPopupSlicer( DasPlot plot, TableDataSetConsumer dataSetConsumer,
    int width, int height) {
        DasCanvas canvas = new DasCanvas(width, height);
        DasRow row = new DasRow(canvas, 0.1, 0.9);
        DasColumn column = new DasColumn(canvas, 0.1, 0.9);
        final VerticalSpectrogramSlicer slicer = createSlicer(plot, dataSetConsumer, row, column);
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
        
        slicer.popupWindow = new JFrame("Vertical Slicer");
        slicer.popupWindow.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        slicer.popupWindow.setContentPane(content);
        slicer.popupWindow.pack();
                
        slicer.popupWindow.setLocation(600,0);
        
        return slicer;
    }
    
    protected void drawContent(Graphics2D g) {
        long x;
        x= System.currentTimeMillis()-eventBirthMilli;
        //edu.uiowa.physics.pw.das.util.DasDie.println("event handled in "+x+" milliseconds");
        
        int ix= (int)this.getXAxis().transform(yValue);
        DasRow row= this.getRow();
        int iy0= (int)row.getDMinimum();
        int iy1= (int)row.getDMaximum();
        g.drawLine(ix+3,iy0,ix,iy0+3);
        g.drawLine(ix-3,iy0,ix,iy0+3);
        g.drawLine(ix+3,iy1,ix,iy1-3);
        g.drawLine(ix-3,iy1,ix,iy1-3);
        
        g.setColor( new Color(230,230,230) );
        g.drawLine( ix, iy0+4, ix, iy1-4 );
        
        super.drawContent(g);
        x= System.currentTimeMillis()-eventBirthMilli;
        //edu.uiowa.physics.pw.das.util.DasDie.println("event handled in "+x+" milliseconds");
    }
    
    public void DataPointSelected(DataPointSelectionEvent e) {    
        long xxx[]= { 0,0,0,0 };
        xxx[0] = System.currentTimeMillis()-e.birthMilli;    
        
        DataSet ds = e.getDataSet();
        if (ds==null || !(ds instanceof TableDataSet))
            return;
        
        TableDataSet tds = (TableDataSet)ds;
        
        VectorDataSet sliceDataSet= tds.getXSlice( TableUtil.closestColumn( tds, e.getX() ) );
               
        xxx[1]= System.currentTimeMillis()-e.birthMilli;                
        
        renderer.setDataSet(sliceDataSet);
        
        if (popupWindow != null && !popupWindow.isVisible()) {
            popupWindow.setVisible(true);
        }
        else {
            repaint();
        }
        
        xxx[3]=  System.currentTimeMillis()-e.birthMilli;
        
        //edu.uiowa.physics.pw.das.util.DasDie.println(""+xxx[0]+" "+xxx[1]+" "+xxx[2]+" "+xxx[3]+" ");
        
        yValue= e.getY();
        Datum xValue = e.getX();
        
        setTitle("x: "+xValue);
        
        eventBirthMilli= e.birthMilli;
    }
    
}
