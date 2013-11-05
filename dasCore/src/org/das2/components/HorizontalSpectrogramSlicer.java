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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.das2.components.propertyeditor.PropertyEditor;
import org.das2.datum.Units;
import org.das2.event.MouseModule;
import org.das2.event.PointSlopeDragRenderer;
import org.das2.graph.Renderer;
import org.das2.graph.SpectrogramRenderer;
import org.das2.util.monitor.ProgressMonitor;
import org.virbo.dataset.ArrayDataSet;
import org.virbo.dataset.DataSetOps;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.IDataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SemanticOps;
import org.virbo.dsops.Ops;
import org.virbo.dsutil.DataSetBuilder;


public class HorizontalSpectrogramSlicer implements DataPointSelectionListener {
    
    private JDialog popupWindow;
    private DasPlot parentPlot;
    private DasPlot myPlot;
    private DasAxis sourceZAxis;
    private DasAxis sourceXAxis;

    protected Datum xValue;
    protected Datum yValue;

    //private long eventBirthMilli;
    private SymbolLineRenderer renderer;
    private Color markColor = new Color(230,230,230);
    
    private HorizontalSpectrogramSlicer(DasPlot parent, DasAxis sourceXAxis, DasAxis sourceZAxis) {
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
                int ix= (int)myPlot.getXAxis().transform(xValue);
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

    protected void setDataSet( QDataSet ds ) {
       renderer.setDataSet(ds);
    }

    public static HorizontalSpectrogramSlicer createSlicer( DasPlot plot, TableDataSetConsumer dataSetConsumer) {
        DasAxis sourceXAxis = plot.getXAxis();
        DasAxis sourceZAxis = dataSetConsumer.getZAxis();
        return new HorizontalSpectrogramSlicer(plot, sourceXAxis, sourceZAxis );
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
        popupWindow.setTitle("Horizontal Slicer");
        popupWindow.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        popupWindow.setContentPane(content);
        popupWindow.pack();
                
        Point parentLocation = new Point( 0, parentPlot.getY() );
        parentLocation.translate( parentPlot.getX()/20, -1 * myPlot.getRow().getDMinimum() );
        SwingUtilities.convertPointToScreen(parentLocation, parentPlot.getCanvas());
        popupWindow.setLocation(parentLocation.x + parentPlot.getCanvas().getWidth(),parentLocation.y + height);
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

        QDataSet yds= SemanticOps.ytagsDataSet(tds1);
        int iy;
        Datum yy;
        
        QDataSet sliceDataSet;
        
        if ( yds.rank()==2 ) {
            QDataSet xds= SemanticOps.xtagsDataSet(tds1);
            int ix= org.virbo.dataset.DataSetUtil.closestIndex( xds, xValue );
            QDataSet yds1= yds.slice(ix);
            iy= org.virbo.dataset.DataSetUtil.closestIndex( yds1, yValue );
            yy= DataSetUtil.asDatum(yds.slice(ix).slice(iy));
            IDataSet eqdep1= IDataSet.createRank1(yds.length());
            eqdep1.putValue(ix,1);
            
            DataSetBuilder bz= new DataSetBuilder(1,yds.length());
            bz.putProperty( QDataSet.UNITS, tds1.property(QDataSet.UNITS ) );
            DataSetBuilder bx= new DataSetBuilder(1,yds.length());
            
            int lastIndex= iy;
            
            //int st1=0, st2=0, st3=0;
            
            for ( int i=0; i<yds.length(); i++ ) {
                if ( yds.value(i,lastIndex)==yy.value() ) {
                    bz.putValue(-1,tds1.value(i,lastIndex));
                    bx.putValue(-1,xds.value(i));
                    bx.nextRecord(); bz.nextRecord();
                    //st1++;
                } else {
                    //st3++;
                    try {
                        int j= DataSetUtil.closestIndex( yds.slice(i), yValue );
                        if ( yds.value( i,j )==yy.value() ) {
                            lastIndex= j;
                            bz.putValue(-1,tds1.value(i,lastIndex));
                            bx.putValue(-1,xds.value(i));
                            bx.nextRecord(); bz.nextRecord();
                            //st2++;                    
                        }
                    } catch ( IllegalArgumentException ex ) {
                        // do nothing, there's no valid data.
                    }
                }
            }
            //System.err.println("st1="+st1+" st2="+st2+" st3="+st3 );
            ArrayDataSet s1= bz.getDataSet();
            ArrayDataSet sx= bx.getDataSet();
            sx.putProperty( QDataSet.UNITS, xds.property(QDataSet.UNITS ) );
            s1.putProperty( QDataSet.DEPEND_0, sx );
            sliceDataSet= s1;
            
        } else {
            iy= org.virbo.dataset.DataSetUtil.closestIndex( yds, yValue );
            yy= DataSetUtil.asDatum(yds.slice(iy));
            sliceDataSet= DataSetOps.slice1( tds1, iy );
            
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
        myPlot.setTitle( "x: " + formatter.format(xValue) + " y: " + yy );
        //eventBirthMilli= e.birthMilli;
        return true;
    }

    public Color getMarkColor() {
        return markColor;
    }

    public void setMarkColor(Color markColor) {
        this.markColor = markColor;
    }
}
