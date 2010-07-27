/* File: HorizontalSpectrogramSlicer.java
 * Copyright (C) 2002-2008 The University of Iowa
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
import org.das2.graph.GraphUtil;
import org.das2.graph.DasRow;
import org.das2.graph.DasPlot;
import org.das2.graph.DasAxis;
import org.das2.dataset.TableDataSetConsumer;
import org.das2.dataset.VectorDataSetBuilder;
import org.das2.dataset.TableDataSet;
import org.das2.dataset.DataSet;
import org.das2.dataset.VectorDataSet;
import org.das2.dataset.DataSetUtil;
import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import org.das2.datum.Datum;
import org.das2.event.BoxSelectionEvent;
import org.das2.event.BoxSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;


public class AngleSpectrogramSlicer extends DasPlot implements BoxSelectionListener {
    
    private JDialog popupWindow;
    
    private SymbolLineRenderer renderer;
    private DasPlot parentPlot;
    private TableDataSetConsumer consumer;
    private Datum xstart;    
    private Datum ystart;
    private DatumRange xrange;
    private DatumRange yrange;
    
    private int sliceDir;
    private final int SLICEDIR_HORIZ=0;
    private final int SLICEDIR_VERTICAL=1;
    
    private AngleSpectrogramSlicer(DasPlot plot, DasAxis xAxis, DasAxis yAxis, TableDataSetConsumer consumer ) {
        super(xAxis, yAxis);
        parentPlot = plot;
        renderer= new SymbolLineRenderer();
        this.consumer= consumer;
        addRenderer(renderer);
    }
    
    public static AngleSpectrogramSlicer createSlicer( DasPlot plot, TableDataSetConsumer dataSetConsumer ) {
        DasAxis sourceXAxis = plot.getXAxis();
        DasAxis xAxis = sourceXAxis.createAttachedAxis(DasAxis.HORIZONTAL);
        DasAxis yAxis = dataSetConsumer.getZAxis().createAttachedAxis(DasAxis.VERTICAL);
        
        return new AngleSpectrogramSlicer(plot, xAxis, yAxis, dataSetConsumer );
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
                canvas.PRINT_ACTION.actionPerformed(e);
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
        popupWindow.setTitle("Angle Slicer");
        popupWindow.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        popupWindow.setContentPane(content);
        popupWindow.pack();
                
        Point parentLocation = new Point();
        SwingUtilities.convertPointToScreen(parentLocation, parentPlot.getCanvas());
        popupWindow.setLocation(parentLocation.x + parentPlot.getCanvas().getWidth(),parentLocation.y + height);
    }

    
    private VectorDataSet angleSliceHoriz( TableDataSet tds, DatumRange xlimit, Datum xbase, Datum ybase, Datum slope ) {
        VectorDataSetBuilder builder= new VectorDataSetBuilder( tds.getXUnits(), tds.getZUnits() );
        int i0= DataSetUtil.closestColumn( tds, xlimit.min() );
        int i1= DataSetUtil.closestColumn( tds, xlimit.max() );
        
        int irow0=0;
        int irow1=0;
        
        Units zunits= tds.getZUnits();
        Units yunits= tds.getYUnits();
        
        for ( int i=i0; i<i1; i++ ) {
            Datum x= tds.getXTagDatum(i);
            Datum y= ybase.add( slope.multiply(x.subtract(xbase) ) );
            int itable= tds.tableOfIndex(i);
            while ( irow0>0 && tds.getYTagDatum( itable, irow0 ).gt( y ) ) irow0--;
            irow1= irow0+1;
            while ( (irow1+1)<tds.getYLength(itable) && tds.getYTagDatum(itable, irow1).lt( y ) ) irow1++;
            irow0= irow1-1;
            
            if ( irow0>0 ) {
                double z0= tds.getDouble( i, irow0, zunits );
                double z1= tds.getDouble( i, irow1, zunits );
                double y0= tds.getYTagDouble( itable, irow0, yunits );
                double y1= tds.getYTagDouble( itable, irow1, yunits );
                double yy= y.doubleValue(yunits);
                double alpha= ( yy-y0 ) / ( y1-y0 );
                double zinterp= z0 + (z1-z0) * alpha;
                builder.insertY( x, Datum.create(zinterp,zunits) );
            }
        }
        
        return builder.toVectorDataSet();
    }
    
    @Override
    public void drawContent(Graphics2D g) {
        
        double[] ixs;
        double ix;
        if ( sliceDir==SLICEDIR_HORIZ ) {
            ixs= GraphUtil.transformRange( this.getXAxis(), xrange );
            ix= this.getXAxis().transform( xstart );
        } else {
            ixs= GraphUtil.transformRange( this.getYAxis(), yrange );
            ix= this.getYAxis().transform( ystart );
        }
        
        DasRow row= getRow();
        
        g.setColor( new Color(230,230,230) );
        g.fillRect( (int)ixs[0], row.getDMinimum(), (int)(ixs[1]-ixs[0]), row.getHeight() );
        
        g.setColor( Color.LIGHT_GRAY );
        g.drawLine( (int)ix, row.getDMinimum(), (int)ix, row.getDMaximum() );
        
        super.drawContent(g);
        
    }
    
    protected void processDasUpdateEvent(org.das2.event.DasUpdateEvent e) {
        if (isDisplayable()) {
            updateImmediately();
            resize();
        }
    }

    public void BoxSelected(BoxSelectionEvent e) {
        Datum xbase= e.getStartX();
        Datum ybase= e.getStartY();

        xstart= e.getStartX();
        ystart= e.getStartY();
        
        xrange= e.getXRange();
        yrange= e.getYRange();
        
        Datum slope= e.getFinishY().subtract(ybase) .divide( e.getFinishX().subtract(xbase) );

        DataSet ds = consumer.getConsumedDataSet();

        if (ds==null || !(ds instanceof TableDataSet)) {
            return;
        }
        
        TableDataSet tds = (TableDataSet)ds;
        VectorDataSet sliceDataSet;
        
        sliceDataSet= angleSliceHoriz( tds, getXAxis().getDatumRange(), xbase, ybase, slope );
        sliceDir= SLICEDIR_HORIZ;
        
        renderer.setDataSet(sliceDataSet);            
        
        if (!(popupWindow == null || popupWindow.isVisible()) || getCanvas() == null) {
            showPopup();
        }        
        
    }
}
