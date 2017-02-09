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

package org.das2.components;

import org.das2.graph.SymbolLineRenderer;
import org.das2.graph.DasColumn;
import org.das2.graph.DasCanvas;
import org.das2.graph.DasRow;
import org.das2.graph.DasPlot;
import org.das2.graph.DasAxis;
import org.das2.dataset.TableDataSetConsumer;
import org.das2.dataset.TableDataSet;
import org.das2.dataset.DataSet;
import org.das2.dataset.VectorDataSet;
import org.das2.dataset.DataSetUtil;
import org.das2.datum.format.DatumFormatter;
import org.das2.datum.format.TimeDatumFormatter;
import org.das2.datum.TimeLocationUnits;
import org.das2.system.DasLogger;
import org.das2.datum.Datum;
import org.das2.event.DataPointSelectionEvent;
import org.das2.event.DataPointSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;


public class VerticalSpectrogramSlicer
extends DasPlot implements DataPointSelectionListener {
    
    private JDialog popupWindow;
    private DasPlot parentPlot;
    protected Datum yValue;    
    private long eventBirthMilli;
    private SymbolLineRenderer renderer;
    private Color yMarkColor = new Color(230,230,230);
	 private String frameTitle = "Vertical Slicer";
    
    protected VerticalSpectrogramSlicer(DasPlot parent, DasAxis xAxis, DasAxis yAxis) {
        super( xAxis, yAxis);
        this.parentPlot = parent;
        renderer= new SymbolLineRenderer();
        addRenderer(renderer);                
    }
        
    protected void setDataSet( VectorDataSet ds ) {
       renderer.setDataSet(ds);
    }
            
    public static VerticalSpectrogramSlicer createSlicer( DasPlot plot, TableDataSetConsumer dataSetConsumer) {
        DasAxis sourceYAxis = plot.getYAxis();
        DasAxis sourceZAxis = dataSetConsumer.getZAxis();
        DasAxis xAxis = sourceYAxis.createAttachedAxis(DasAxis.HORIZONTAL);
        DasAxis yAxis = sourceZAxis.createAttachedAxis(DasAxis.VERTICAL);
        return new VerticalSpectrogramSlicer(plot, xAxis, yAxis);
    }
    
    public static VerticalSpectrogramSlicer createSlicer( DasPlot plot, DasAxis xAxis, TableDataSetConsumer dataSetConsumer) {
        DasAxis sourceYAxis = plot.getYAxis();
        DasAxis sourceZAxis = dataSetConsumer.getZAxis();
        DasAxis yAxis = sourceZAxis.createAttachedAxis(DasAxis.VERTICAL);
        return new VerticalSpectrogramSlicer(plot, xAxis, yAxis);
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
	 
	 /* Would have done this in a base class but I didn't see one that handled the 
	  * popup window */
	 public void setFrameTitle(String s){  
		 frameTitle = s; 
		 if(popupWindow != null) popupWindow.setTitle(frameTitle);
	 }
	 public String getFrameTitle(){ return frameTitle; }
    
    /** This method should ONLY be called by the AWT event thread */
    private void createPopup() {
        int width = parentPlot.getCanvas().getWidth() / 2;
        int height = parentPlot.getCanvas().getHeight() / 2;
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
        
        Window parentWindow = SwingUtilities.getWindowAncestor(parentPlot);
        if (parentWindow instanceof Frame) {
            popupWindow = new JDialog((Frame)parentWindow);
        }
        else if (parentWindow instanceof Dialog) {
            popupWindow = new JDialog((Dialog)parentWindow);
        }
        else {
            popupWindow = new JDialog();
        }
        popupWindow.setTitle(frameTitle);
        popupWindow.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        popupWindow.setContentPane(content);
        popupWindow.pack();
                
        Point parentLocation = new Point();
        SwingUtilities.convertPointToScreen(parentLocation, parentPlot.getCanvas());
        popupWindow.setLocation(parentLocation.x + parentPlot.getCanvas().getWidth(),parentLocation.y);
    }
    
    protected void drawContent(Graphics2D g) {
        long x;
        x= System.currentTimeMillis()-eventBirthMilli;
        
        int ix= (int)this.getXAxis().transform(yValue);
        DasRow row= this.getRow();
        int iy0= (int)row.getDMinimum();
        int iy1= (int)row.getDMaximum();
        g.drawLine(ix+3,iy0,ix,iy0+3);
        g.drawLine(ix-3,iy0,ix,iy0+3);
        g.drawLine(ix+3,iy1,ix,iy1-3);
        g.drawLine(ix-3,iy1,ix,iy1-3);
        
        g.setColor(yMarkColor);
        g.drawLine( ix, iy0+4, ix, iy1-4 );
        
        super.drawContent(g);
        x= System.currentTimeMillis()-eventBirthMilli;
        //org.das2.util.DasDie.println("event handled in "+x+" milliseconds");
    }
    
    protected boolean isPopupVisible() {
        return ( popupWindow != null && popupWindow.isVisible()) && getCanvas() != null;
    }
    
    public void dataPointSelected(DataPointSelectionEvent e) {    
        long xxx[]= { 0,0,0,0 };
        xxx[0] = System.currentTimeMillis()-e.birthMilli;    
        
        DataSet ds = e.getDataSet();
        if (ds==null || !(ds instanceof TableDataSet))
            return;
        
        TableDataSet tds = (TableDataSet)ds;
        
        VectorDataSet sliceDataSet= tds.getXSlice( DataSetUtil.closestColumn( tds, e.getX() ) );
                      
        renderer.setDataSet(sliceDataSet);
        DasLogger.getLogger(DasLogger.GUI_LOG).finest("setDataSet sliceDataSet");        
        if (!isPopupVisible()) {
            showPopup();
        }
        
        yValue= e.getY();
        Datum xValue = e.getX();
        
        DatumFormatter formatter;
        if ( xValue.getUnits() instanceof TimeLocationUnits ) {
            formatter= TimeDatumFormatter.DEFAULT;
        } else {
            formatter= xValue.getFormatter();
        }
            
        setTitle("x: "+ formatter.format(xValue) + " y: "+yValue);
        
        eventBirthMilli= e.birthMilli;
    }
    
    protected void uninstallComponent() {
        super.uninstallComponent();
    }
    
    protected void installComponent() {
        super.installComponent();
        getCanvas().getGlassPane().setVisible(false);
    }
    
    protected void processDasUpdateEvent(org.das2.event.DasUpdateEvent e) {
        if (isDisplayable()) {
            updateImmediately();
            resize();
        }
    }

    public Color getYMarkColor() {
        return yMarkColor;
    }

    public void setYMarkColor(Color yMarkColor) {
        this.yMarkColor = yMarkColor;
    }
}
