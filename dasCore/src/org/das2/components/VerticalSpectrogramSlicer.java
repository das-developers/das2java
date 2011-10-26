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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Window;
import org.das2.graph.SymbolLineRenderer;
import org.das2.graph.DasColumn;
import org.das2.graph.DasCanvas;
import org.das2.graph.DasRow;
import org.das2.graph.DasPlot;
import org.das2.graph.DasAxis;
import org.das2.dataset.TableDataSetConsumer;
import org.das2.datum.format.DatumFormatter;
import org.das2.datum.format.TimeDatumFormatter;
import org.das2.datum.TimeLocationUnits;
import org.das2.system.DasLogger;
import org.das2.datum.Datum;
import org.das2.event.DataPointSelectionEvent;
import org.das2.event.DataPointSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.virbo.dataset.DataSetOps;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SemanticOps;


public class VerticalSpectrogramSlicer
extends DasPlot implements DataPointSelectionListener {
    
    private JDialog popupWindow;
    private DasPlot parentPlot;
    protected Datum yValue;    
    private long eventBirthMilli;
    private SymbolLineRenderer renderer;
    private Color yMarkColor = new Color(230,230,230);
    
    protected VerticalSpectrogramSlicer(DasPlot parent, DasAxis xAxis, DasAxis yAxis) {
        super( xAxis, yAxis);
        this.parentPlot = parent;
        renderer= new SymbolLineRenderer();
        addRenderer(renderer);                
    }
        
    protected void setDataSet( QDataSet ds ) {
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
    
    /** This method should ONLY be called by the AWT event thread */
    private void createPopup() {
        int width = parentPlot.getCanvas().getWidth() / 2;
        int height = parentPlot.getCanvas().getHeight() / 2;
        final DasCanvas canvas = new DasCanvas(width, height);
        DasRow row = new DasRow(canvas, 0.1, 0.9);
        DasColumn column = new DasColumn(canvas, 0.1, 0.9);
        canvas.add(this, row, column);
        
        JPanel content = new JPanel(new BorderLayout());
        
        JPanel buttonPanel = new JPanel();
        BoxLayout buttonLayout = new BoxLayout(buttonPanel, BoxLayout.X_AXIS);
        buttonPanel.setLayout(buttonLayout);

        buttonPanel.add(Box.createHorizontalGlue());

        JButton printButton= new JButton( new AbstractAction("Print...") {
            public void actionPerformed( ActionEvent e ) {
                canvas.makeCurrent();
                DasCanvas.PRINT_ACTION.actionPerformed(e);
            }
        });
        buttonPanel.add( printButton );

        
        JButton close = new JButton("Hide Window");
        close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                popupWindow.setVisible(false);
            }
        });
        
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
        popupWindow.setTitle("Vertical Slicer");
        popupWindow.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        popupWindow.setContentPane(content);
        popupWindow.pack();
                
        Point parentLocation = new Point();
        SwingUtilities.convertPointToScreen(parentLocation, parentPlot.getCanvas());
        popupWindow.setLocation(parentLocation.x + parentPlot.getCanvas().getWidth(),parentLocation.y);
    }
    
    @Override
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
        
        QDataSet ds = e.getDataSet();
        if (ds==null || ! SemanticOps.isTableDataSet(ds) )
            return;
        
        QDataSet tds = (QDataSet)ds;
        QDataSet tds1=null;

        if ( tds.rank()==3 ) { // slice to get the correct table;
            for ( int i=0; i<tds.length(); i++ ) {
                QDataSet bounds= DataSetOps.dependBounds(tds.slice(i));
                if ( DataSetOps.boundsContains( bounds, e.getX(), e.getY() ) ) {
                    tds1= tds.slice(i);
                    break;
                }
            }
        } else {
            QDataSet bounds= DataSetOps.dependBounds(tds);
            if ( DataSetOps.boundsContains( bounds, e.getX(), e.getY() ) ) {
                tds1= tds;
            }
        }

        if ( tds1==null ) return;
        
        QDataSet xds= SemanticOps.xtagsDataSet(tds1);
        QDataSet sliceDataSet= tds1.slice( org.virbo.dataset.DataSetUtil.closestIndex( xds, e.getX() ) );
                      
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
    
    @Override
    protected void uninstallComponent() {
        super.uninstallComponent();
    }
    
    @Override
    protected void installComponent() {
        super.installComponent();
        getCanvas().getGlassPane().setVisible(false);
    }
    
    @Override
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
