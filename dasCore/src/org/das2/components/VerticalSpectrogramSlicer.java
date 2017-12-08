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
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.das2.components.propertyeditor.PropertyEditor;
import org.das2.dataset.TableDataSetConsumer;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.DatumRangeUtil;
import org.das2.datum.InconvertibleUnitsException;
import org.das2.datum.TimeLocationUnits;
import org.das2.datum.format.DatumFormatter;
import org.das2.datum.format.TimeDatumFormatter;
import org.das2.event.DataPointSelectionEvent;
import org.das2.event.DataPointSelectionListener;
import org.das2.event.MouseModule;
import org.das2.event.PointSlopeDragRenderer;
import org.das2.graph.DasAxis;
import org.das2.graph.DasCanvas;
import org.das2.graph.DasColumn;
import org.das2.graph.DasPlot;
import org.das2.graph.DasRow;
import org.das2.graph.Painter;
import org.das2.graph.Renderer;
import org.das2.graph.SpectrogramRenderer;
import org.das2.graph.SymbolLineRenderer;
import org.das2.system.DasLogger;
import org.das2.util.monitor.ProgressMonitor;
import org.das2.qds.DataSetOps;
import org.das2.qds.DataSetUtil;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;
import org.das2.qds.ops.Ops;


public class VerticalSpectrogramSlicer implements DataPointSelectionListener {
    
    private JDialog popupWindow;
    private final DasPlot parentPlot;
    private DasPlot myPlot;
    private final DasAxis sourceZAxis;
    private final DasAxis sourceXAxis;

    protected Datum xValue;
    protected Datum yValue;

    protected Datum ySlice;
    
    JPanel buttonPanel;
    List<Action> additionalActions= new ArrayList<>();
    
    //private long eventBirthMilli;
    private SymbolLineRenderer renderer;
    private Color markColor = new Color(230,230,230);
    
    protected VerticalSpectrogramSlicer( DasPlot parent, DasAxis sourceXAxis, DasAxis sourceZAxis ) {
        this.sourceZAxis= sourceZAxis;
        this.sourceXAxis= sourceXAxis;
        this.parentPlot= parent;
    }

    private void initPlot( DasCanvas canvas ) {
        DasAxis xAxis= sourceXAxis.createAttachedAxis( DasAxis.HORIZONTAL );
        DasAxis yAxis = sourceZAxis.createAttachedAxis(DasAxis.VERTICAL);
        myPlot= new DasPlot( xAxis, yAxis);
        renderer= new SymbolLineRenderer();
        renderer.setAntiAliased(true);
        myPlot.addRenderer(renderer);
        canvas.addTopDecorator( new Painter() {
            @Override
            public void paint(Graphics2D g) {
                int ix= (int)myPlot.getXAxis().transform(yValue);
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
        } );
        myPlot.getDasMouseInputAdapter().addMouseModule(new MouseModule(myPlot, new PointSlopeDragRenderer(myPlot, myPlot.getXAxis(), myPlot.getYAxis()), "Slope"));

    }
        
    /**
     * add a button
     * @param a the action for the button.
     */
    public void addAction( Action a ) {
        additionalActions.add(a);
        if ( buttonPanel!=null ) {
            JButton b= new JButton(a);
            buttonPanel.add(b,0);
        }
    }

    protected void setDataSet( QDataSet ds ) {
       renderer.setDataSet(ds);
    }
    
    /**
     * provide access to the dataset
     * @return the dataset.
     */
    public QDataSet getDataSet() {
        return renderer.getDataSet();
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
                @Override
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
     * clear the current dataset to avoid units errors.  If the
     * new dataset can be used, use it.
     * @param tds the new dataset
     */
    public void clear( QDataSet tds ) {
        if ( renderer!=null ) {
            if ( tds==null ) {
                this.renderer.setDataSet(null);
            } else {
                try {
                    if ( this.isPopupVisible() ) {
                        showSlice( tds, xValue, yValue );
                    }
                } catch ( InconvertibleUnitsException ex ) {
                    this.renderer.setDataSet(null);
                }
            }
        }
    }
        
    
    /** This method should ONLY be called by the AWT event thread */
    private void createPopup() {
        int width = parentPlot.getCanvas().getWidth() / 2;
        int height = parentPlot.getCanvas().getHeight() / 2;
        final DasCanvas canvas = new DasCanvas(width, height);
        if ( myPlot==null ) {
            initPlot(canvas);
        }
        DasRow row = new DasRow(canvas, null, 0, 1.0, 3, -5, 0, 0 );
        DasColumn column = new DasColumn(canvas, null, 0, 1.0, 7, -3, 0, 0 );
        canvas.add( myPlot, row, column);
        
        JPanel content = new JPanel(new BorderLayout());
        
        buttonPanel = new JPanel();
        BoxLayout buttonLayout = new BoxLayout(buttonPanel, BoxLayout.X_AXIS);
        buttonPanel.setLayout(buttonLayout);

        if ( additionalActions!=null && additionalActions.size()>0 ) {
            for ( Action a: additionalActions ) {
                buttonPanel.add( new JButton(a) );
            }
        }
        
        buttonPanel.add(Box.createHorizontalGlue());

        JButton printButton= new JButton( new AbstractAction("Print...") {
            @Override
            public void actionPerformed( ActionEvent e ) {
                canvas.makeCurrent();
                DasCanvas.PRINT_ACTION.actionPerformed(e);
            }
        });
        buttonPanel.add( printButton );

        JButton settingsButton= new JButton( new AbstractAction("Settings...") {
            @Override
            public void actionPerformed( ActionEvent e ) {
                SpectrogramRenderer rend=null;
                for ( Renderer r : parentPlot.getRenderers() ) {
                    if ( r instanceof SpectrogramRenderer ) {
                        rend= (SpectrogramRenderer) r;
                        break;
                    }
                }
                if ( rend==null ) {
                    JOptionPane.showMessageDialog( null, "Unable to find associated Spectrogram" );
                    return;
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
        
        JButton close = new JButton("Close");
        close.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                popupWindow.setVisible(false);
            }
        });
        
        buttonPanel.add(close);
        
        content.add(canvas, BorderLayout.CENTER);
        content.add(buttonPanel, BorderLayout.SOUTH);
        
        Window parentWindow = SwingUtilities.getWindowAncestor(parentPlot);
        popupWindow = new JDialog(parentWindow);
        popupWindow.setTitle("Vertical Slicer");
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
    
    /**
     * show the slice at the data point selected.
     * @param e the selection event containing the data point.
     */
    @Override
    public void dataPointSelected(DataPointSelectionEvent e) {    

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
        List<QDataSet> tdss= new ArrayList();
        if (tds.rank() == 3) {
            // slice to get the correct table;
            for (int i = 0; i < tds.length(); i++) {
                QDataSet bounds = DataSetOps.dependBounds(tds.slice(i));
                DatumRange xrange= DataSetUtil.asDatumRange( bounds.slice(0), true );
                if ( xrange.contains( xValue )) {
                    tdss.add(tds.slice(i));
                }
            }
        } else {
            QDataSet bounds = DataSetOps.dependBounds(tds);
            if (DataSetOps.boundsContains(bounds, xValue, yValue)) {
                tds1 = tds;
            }
        }
        
        int ix;
        Datum xx=null;
        DatumRange xdr= null;  // if the xx's aren't identical, show the range instead.
        
        QDataSet sliceDataSet;
        if (tds1 == null) {
            QDataSet jds= null;
            assert tdss.size()>0;
            for (QDataSet tds2 : tdss) {
                tds1 = tds2;
                QDataSet xds = SemanticOps.xtagsDataSet(tds1);
                ix = org.das2.qds.DataSetUtil.closestIndex(xds, xValue);
                QDataSet s1 = tds1.slice(ix);
                if ( xx!=null ) {
                    Datum xx1= DataSetUtil.asDatum(xds.slice(ix));
                    if ( !xx1.equals(xx) ) {
                        xdr= DatumRangeUtil.union( xx, xx1 );
                    }
                }
                xx= DataSetUtil.asDatum(xds.slice(ix));
                jds= org.das2.qds.ops.Ops.concatenate( jds, s1 );
            }
            sliceDataSet= jds;
            
        } else {
        
            QDataSet xds = SemanticOps.xtagsDataSet(tds1);
            ix = org.das2.qds.DataSetUtil.closestIndex(xds, xValue);
            xds= xds.slice(ix);
            if ( xds.rank()==0 ) {
                xx= DataSetUtil.asDatum(xds);
            } else {
                xx= DataSetUtil.asDatum(xds.slice(0));
                xx= xx.add( DataSetUtil.asDatum( Ops.divide( Ops.subtract( xds.slice(1), xds.slice(0) ), 2 ) ) );
            }
            sliceDataSet = tds1.slice(ix);
        }
        
        if ( sliceDataSet==null ) {
            return false;
        }
        
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
        String title= parentPlot.getTitle().trim();
        if ( title.length()>0 ) title= title+"!c";
        if ( xdr!=null ) {
            myPlot.setTitle( title +  "x: " + xdr + " y: " + yValue );
        } else {
            myPlot.setTitle( title + "x: " + formatter.format(xx) + " y: " + yValue );
        }
        if ( !myPlot.getXAxis().getLabel().equals( sourceXAxis.getLabel() ) ) {
            myPlot.getXAxis().setLabel( sourceXAxis.getLabel() );
        }
        if ( !myPlot.getYAxis().getLabel().equals( sourceZAxis.getLabel() ) ) {
            myPlot.getYAxis().setLabel( sourceZAxis.getLabel() );
        }
        //eventBirthMilli= e.birthMilli;
        return true;
    }
    
    /**
     * the color for the mark (vertical bar) indicating slice position
     * @return the mark color
     */
    public Color getMarkColor() {
        return markColor;
    }

    /**
     * set the color for the mark (vertical bar) indicating slice position
     * Color(230,230,230) is the default.
     * @param markColor the color
     */
    public void setMarkColor(Color markColor) {
        this.markColor = markColor;
    }
}
