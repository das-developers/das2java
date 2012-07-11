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
import java.awt.Graphics;
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
import org.das2.components.propertyeditor.PropertyEditor;
import org.das2.graph.Renderer;
import org.das2.graph.SpectrogramRenderer;
import org.das2.util.monitor.ProgressMonitor;
import org.virbo.dataset.DataSetOps;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SemanticOps;


public class VerticalSpectrogramSlicer implements DataPointSelectionListener {
    
    private JDialog popupWindow;
    private DasPlot parentPlot;
    private DasPlot myPlot;
    private DasAxis sourceZAxis;
    private DasAxis sourceXAxis;

    protected Datum xValue;
    protected Datum yValue;

    //private long eventBirthMilli;
    private SymbolLineRenderer renderer;
    private Color yMarkColor = new Color(230,230,230);
    
    protected VerticalSpectrogramSlicer( DasPlot parent, DasAxis sourceXAxis, DasAxis sourceZAxis ) {
        this.sourceZAxis= sourceZAxis;
        this.sourceXAxis= sourceXAxis;
        this.parentPlot= parent;
    }

    private void initPlot() {
        DasAxis xAxis= sourceXAxis.createAttachedAxis( DasAxis.HORIZONTAL );
        DasAxis yAxis = sourceZAxis.createAttachedAxis(DasAxis.VERTICAL);
        myPlot= new DasPlot( xAxis, yAxis);
        renderer= new SymbolLineRenderer();
        renderer.setAntiAliased(true);
        myPlot.addRenderer(renderer);
        myPlot.addRenderer( new Renderer() {
            @Override
            public void render(Graphics g, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {
                int ix= (int)myPlot.getXAxis().transform(yValue);
                DasRow row= myPlot.getRow();
                int iy0= (int)row.getDMinimum();
                int iy1= (int)row.getDMaximum();
                g.drawLine(ix+3,iy0,ix,iy0+3);
                g.drawLine(ix-3,iy0,ix,iy0+3);
                g.drawLine(ix+3,iy1,ix,iy1-3);
                g.drawLine(ix-3,iy1,ix,iy1-3);

                g.setColor(yMarkColor);
                g.drawLine( ix, iy0+4, ix, iy1-4 );
            }
        } );
    }
        
    protected void setDataSet( QDataSet ds ) {
       renderer.setDataSet(ds);
    }
            
    public static VerticalSpectrogramSlicer createSlicer( DasPlot plot, TableDataSetConsumer dataSetConsumer) {
        DasAxis sourceYAxis = plot.getYAxis();
        DasAxis sourceZAxis = dataSetConsumer.getZAxis();
        return new VerticalSpectrogramSlicer(plot, sourceYAxis, sourceZAxis);
    }
    
    public static VerticalSpectrogramSlicer createSlicer( DasPlot plot, DasAxis xAxis, TableDataSetConsumer dataSetConsumer) {
        DasAxis sourceZAxis = dataSetConsumer.getZAxis();
        return new VerticalSpectrogramSlicer(plot, xAxis, sourceZAxis );
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
            SwingUtilities.invokeLater(r);
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
        if ( myPlot==null ) {
            initPlot();
        }
        int width = parentPlot.getCanvas().getWidth() / 2;
        int height = parentPlot.getCanvas().getHeight() / 2;
        final DasCanvas canvas = new DasCanvas(width, height);
        DasRow row = new DasRow(canvas, null, 0, 1.0, 3, -5, 0, 0 );
        DasColumn column = new DasColumn(canvas, null, 0, 1.0, 7, -3, 0, 0 );
        canvas.add( myPlot, row, column);
        
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

        JButton settingsButton= new JButton( new AbstractAction("Settings...") {
            public void actionPerformed( ActionEvent e ) {
                SpectrogramRenderer rend=null;
                for ( Renderer r : parentPlot.getRenderers() ) {
                    if ( r instanceof SpectrogramRenderer ) {
                        rend= (SpectrogramRenderer) r;
                        break;
                    }
                }
                SliceSettings settings= new SliceSettings();
                settings.setSliceRebinnedData( rend.isSliceRebinnedData() );
                new PropertyEditor(settings).showModalDialog(canvas);
                rend.setSliceRebinnedData(settings.isSliceRebinnedData());

                QDataSet ds= rend.getConsumedDataSet();
                showSlice( ds, xValue, yValue );
                
            }
        });
        buttonPanel.add( settingsButton );
        
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
    
    protected boolean isPopupVisible() {
        return ( popupWindow != null && popupWindow.isVisible()) && myPlot.getCanvas() != null;
    }
    
    public void dataPointSelected(DataPointSelectionEvent e) {    
        long xxx[]= { 0,0,0,0 };
        xxx[0] = System.currentTimeMillis()-e.birthMilli;    

        yValue = e.getY();
        xValue = e.getX();

        QDataSet ds = e.getDataSet();
        if (ds==null || ! SemanticOps.isTableDataSet(ds) )
            return;
        
        QDataSet tds = (QDataSet)ds;
        
        showSlice( tds, xValue, yValue );

    }

    private boolean showSlice( QDataSet tds, Datum xValue, Datum yValue ) {
        QDataSet tds1 = null;
        if (tds.rank() == 3) {
            // slice to get the correct table;
            for (int i = 0; i < tds.length(); i++) {
                QDataSet bounds = DataSetOps.dependBounds(tds.slice(i));
                if (DataSetOps.boundsContains(bounds, xValue, yValue)) {
                    tds1 = tds.slice(i);
                    break;
                }
            }
        } else {
            QDataSet bounds = DataSetOps.dependBounds(tds);
            if (DataSetOps.boundsContains(bounds, xValue, yValue)) {
                tds1 = tds;
            }
        }
        if (tds1 == null) {
            return false;
        }
        QDataSet xds = SemanticOps.xtagsDataSet(tds1);
        int index;
        index = org.virbo.dataset.DataSetUtil.closestIndex(xds, xValue);
        QDataSet sliceDataSet = tds1.slice(index);
        DasLogger.getLogger(DasLogger.GUI_LOG).finest("setDataSet sliceDataSet");
        if (!isPopupVisible()) {
            showPopup();
        }
        renderer.setDataSet(sliceDataSet);
        DatumFormatter formatter;
        if (xValue.getUnits() instanceof TimeLocationUnits) {
            formatter = TimeDatumFormatter.DEFAULT;
        } else {
            formatter = xValue.getFormatter();
        }
        myPlot.setTitle("x: " + formatter.format(xValue) + " y: " + yValue);
        //eventBirthMilli= e.birthMilli;
        return true;
    }
    
    public Color getYMarkColor() {
        return yMarkColor;
    }

    public void setYMarkColor(Color yMarkColor) {
        this.yMarkColor = yMarkColor;
    }
}
