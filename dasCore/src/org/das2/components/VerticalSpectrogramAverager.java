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

package org.das2.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Window;
import org.das2.graph.SymbolLineRenderer;
import org.das2.graph.DasColumn;
import org.das2.graph.DasCanvas;
import org.das2.graph.DasRow;
import org.das2.graph.DasPlot;
import org.das2.graph.DasAxis;
import org.das2.dataset.TableDataSetConsumer;
import org.das2.dataset.AverageTableRebinner;
import org.das2.dataset.RebinDescriptor;
import org.das2.datum.DatumRange;
import org.das2.DasException;
import org.das2.datum.Datum;
import org.das2.event.DataRangeSelectionEvent;
import org.das2.event.DataRangeSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.das2.graph.Renderer;
import org.das2.graph.SeriesRenderer;
import org.das2.util.monitor.ProgressMonitor;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;

/**
 * show the average of the data over an interval
 * @author jbf
 */
public class VerticalSpectrogramAverager implements DataRangeSelectionListener {
    
    private JDialog popupWindow;
    private DasPlot parentPlot;
    private DasPlot myPlot;
    private DasAxis sourceZAxis;
    private DasAxis sourceXAxis;
    protected Datum value;
    private SeriesRenderer renderer;
    private Color markColor = new Color(230,230,230);
    
    protected VerticalSpectrogramAverager( DasPlot parent, DasAxis sourceXAxis, DasAxis sourceZAxis ) {
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
                if ( value!=null ) {
                    int ix= (int)myPlot.getXAxis().transform(value);
                    DasRow row= myPlot.getRow();
                    int iy0= (int)row.getDMinimum();
                    int iy1= (int)row.getDMaximum();
                    g.drawLine(ix+3,iy0,ix,iy0+3);
                    g.drawLine(ix-3,iy0,ix,iy0+3);
                    g.drawLine(ix+3,iy1,ix,iy1-3);
                    g.drawLine(ix-3,iy1,ix,iy1-3);

                    g.setColor(markColor);
                    g.drawLine( ix, iy0+4, ix, iy1-4 );
                }
            }
        } );
    }

    protected void setDataSet( QDataSet ds ) {
       renderer.setDataSet(ds);
    }

    
    public static VerticalSpectrogramAverager createAverager(DasPlot plot, TableDataSetConsumer dataSetConsumer) {
        DasAxis sourceYAxis = plot.getYAxis();
        DasAxis sourceZAxis = dataSetConsumer.getZAxis();
        return new VerticalSpectrogramAverager(plot, sourceYAxis, sourceZAxis);
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
    
    /**
     * dispose of the popup slicer.
     */
    public void dispose() {
        if ( popupWindow!=null ) {
            popupWindow.setVisible(false);
            popupWindow.dispose();
        }
    }
    
    /**
     * clear the current dataset to avoid units errors.
     */
    public void clear( ) {
        if ( renderer!=null ) this.renderer.setDataSet(null);
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
        canvas.add(myPlot, row, column);
        
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
        popupWindow = new JDialog(parentWindow);
        popupWindow.setTitle("Vertical Spectrogram Averager");
        popupWindow.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        popupWindow.setContentPane(content);
        popupWindow.pack();
        
        Point parentLocation = new Point( 0, parentPlot.getY() );
        parentLocation.translate( parentPlot.getX()/20, -1 * myPlot.getRow().getDMinimum() );
        SwingUtilities.convertPointToScreen(parentLocation, parentPlot.getCanvas());
        
        //make sure some portion of the slice window is visible on the screen.
        int xx= parentLocation.x + parentPlot.getCanvas().getWidth();
        int yy= parentLocation.y;
        
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int totalwidth = gd.getDisplayMode().getWidth();
        int totalheight = gd.getDisplayMode().getHeight();
        
        if ( xx>totalwidth-100 ) {
            xx= totalwidth-100;
        }
        if ( yy>totalheight-100 ) {
            yy= totalheight-100;
        }
        popupWindow.setLocation(xx,yy);
    }
    
    protected boolean isPopupVisible() {
        return ( popupWindow != null && popupWindow.isVisible()) && myPlot.getCanvas() != null;
    }
    
    public void dataRangeSelected(DataRangeSelectionEvent e) {
        QDataSet ds = e.getDataSet();

        if (ds==null || ! SemanticOps.isTableDataSet(ds) ) {
            return;
        }

        QDataSet xtys = (QDataSet)ds;
        Datum xValue1 = e.getMinimum();
        Datum xValue2 = e.getMaximum();
        
        if ( xValue2.equals(xValue1) ) {
            return;
        }

        if (!isPopupVisible()) {
            showPopup();
        }
        
        RebinDescriptor ddX = new RebinDescriptor(xValue1, xValue2, 1, false);
        ddX.setOutOfBoundsAction(RebinDescriptor.MINUSONE);
        AverageTableRebinner rebinner = new AverageTableRebinner();
        try {
            if ( xtys.rank()==3 ) {
                QDataSet jds= null;
                for ( int i=0; i<xtys.length(); i++ ) {
                    QDataSet rebinned = (QDataSet)rebinner.rebin(xtys.slice(i), ddX, null);
                    QDataSet ds1 = rebinned.slice(0);
                    jds= org.das2.qds.ops.Ops.concatenate( jds, ds1 );
                }
                renderer.setDataSet(jds);                
            } else {
                QDataSet rebinned = (QDataSet)rebinner.rebin(xtys, ddX, null);
                QDataSet ds1 = rebinned.slice(0);
                renderer.setDataSet(ds1);
            }
        } catch (DasException de) {
            //Do nothing.
        }

        value= e.getReference();

        String title= parentPlot.getTitle().trim();
        if ( title.length()>0 ) {
            title= "Averaged " + title+"!c";
        }
        
        myPlot.setTitle( title + new DatumRange( xValue1, xValue2 ).toString() );
        if ( !myPlot.getXAxis().getLabel().equals( sourceXAxis.getLabel() ) ) {
            myPlot.getXAxis().setLabel( sourceXAxis.getLabel() );
        }
        if ( !myPlot.getYAxis().getLabel().equals( sourceZAxis.getLabel() ) ) {
            myPlot.getYAxis().setLabel( sourceZAxis.getLabel() );
        }
        //eventBirthMilli= e.birthMilli;
    }
    

}
