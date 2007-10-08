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

import edu.uiowa.physics.pw.das.DasException;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.event.DataRangeSelectionEvent;
import edu.uiowa.physics.pw.das.event.DataRangeSelectionListener;
import edu.uiowa.physics.pw.das.graph.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;


public class VerticalSpectrogramAverager extends DasPlot implements DataRangeSelectionListener {
    
    private JDialog popupWindow;
    
    private Datum yValue;
    
    private DasPlot parentPlot;
    private SymbolLineRenderer renderer;
    
    protected VerticalSpectrogramAverager(DasPlot plot, DasAxis xAxis, DasAxis yAxis) {
        super(xAxis, yAxis);
        parentPlot = plot;
        renderer= new SymbolLineRenderer((DataSetDescriptor)null);
        addRenderer(renderer);
    }
    
    public static VerticalSpectrogramAverager createAverager(DasPlot plot, TableDataSetConsumer dataSetConsumer) {
        DasAxis sourceYAxis = plot.getYAxis();
        DasAxis xAxis = sourceYAxis.createAttachedAxis(DasAxis.HORIZONTAL);
        DasAxis yAxis = dataSetConsumer.getZAxis().createAttachedAxis(DasAxis.VERTICAL);
        return new VerticalSpectrogramAverager(plot, xAxis, yAxis);
    }
    
    public void showPopup() {
        if (SwingUtilities.isEventDispatchThread()) {
            showPopupImpl();
        } else {
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
        } else if (parentWindow instanceof Dialog) {
            popupWindow = new JDialog((Dialog)parentWindow);
        } else {
            popupWindow = new JDialog();
        }
        popupWindow.setTitle("Vertical Slicer");
        popupWindow.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        popupWindow.setContentPane(content);
        popupWindow.pack();
        
        Point parentLocation = new Point();
        SwingUtilities.convertPointToScreen(parentLocation, parentPlot.getCanvas());
        popupWindow.setLocation(parentLocation.x + parentPlot.getCanvas().getWidth(),parentLocation.y);
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
    
    public void dataRangeSelected(DataRangeSelectionEvent e) {
        DataSet ds = e.getDataSet();
        if (ds==null || !(ds instanceof TableDataSet))
            return;
        TableDataSet xtys = (TableDataSet)ds;
        Datum xValue1 = e.getMinimum();
        Datum xValue2 = e.getMaximum();
        
        if ( xValue2.equals(xValue1) ) {
            return;
        }
        
        this.setTitle( new DatumRange( xValue1, xValue2 ).toString() );
        
        RebinDescriptor ddX = new RebinDescriptor(xValue1, xValue2, 1, false);
        ddX.setOutOfBoundsAction(RebinDescriptor.MINUSONE);
        AverageTableRebinner rebinner = new AverageTableRebinner();
        try {
            TableDataSet rebinned = (TableDataSet)rebinner.rebin(xtys, ddX, null);
            VectorDataSet ds1 = rebinned.getXSlice(0);
            renderer.setDataSet(ds1);
        } catch (DasException de) {
            //Do nothing.
        }
        
        if (!(popupWindow == null || popupWindow.isVisible()) || getCanvas() == null) {
            showPopup();
        } else {
            repaint();
        }
    }
    
    protected void uninstallComponent() {
        super.uninstallComponent();
    }
    
    protected void installComponent() {
        super.installComponent();
        getCanvas().getGlassPane().setVisible(false);
    }
    
}
