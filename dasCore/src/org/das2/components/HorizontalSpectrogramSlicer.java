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
import java.awt.Graphics2D;
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
import org.das2.datum.format.DatumFormatter;
import org.das2.datum.format.TimeDatumFormatter;
import org.das2.datum.TimeLocationUnits;
import org.das2.system.DasLogger;
import org.das2.datum.Datum;
import org.das2.event.DataPointSelectionEvent;
import org.das2.event.DataPointSelectionListener;
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
import org.das2.datum.InconvertibleUnitsException;
import org.das2.datum.Units;
import org.das2.datum.UnitsUtil;
import org.das2.event.MouseModule;
import org.das2.event.PointSlopeDragRenderer;
import org.das2.graph.Painter;
import org.das2.graph.Renderer;
import org.das2.graph.SpectrogramRenderer;
import org.das2.qds.ArrayDataSet;
import org.das2.qds.DataSetOps;
import org.das2.qds.DataSetUtil;
import org.das2.qds.IDataSet;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;
import org.das2.qds.ops.Ops;
import org.das2.qds.util.DataSetBuilder;


public class HorizontalSpectrogramSlicer implements DataPointSelectionListener {
    
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
    
    private HorizontalSpectrogramSlicer(DasPlot parent, DasAxis sourceXAxis, DasAxis sourceZAxis) {
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
    
    /**
     * provide the Y position of the data.  Note this may be different 
     * than where the user requested because the nearest channel is provided.
     * @return the slice position.
     */
    public Datum getSliceY() {
        return ySlice;
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
        popupWindow.setTitle("Horizontal Slicer");
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
        if (ds==null || ! SemanticOps.isTableDataSet(ds) ) {
            System.err.println("dataset scheme is not supported: "+ds );
            return;
        }
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
                Units xunits= SemanticOps.getUnits(bounds.slice(0));
                if ( !xunits.isConvertibleTo(xValue.getUnits()) 
                    && UnitsUtil.isRatioMeasurement(xunits) 
                    && ( xunits==Units.dimensionless || xValue.getUnits()==Units.dimensionless ) ) {
                    xValue= xunits.createDatum(xValue.value());
                }
            }
        } else {
            // QDataSet bounds = DataSetOps.dependBounds(tds);  cannot be used because interpolation might result in waveform-type.
            QDataSet xrange= Ops.extent( SemanticOps.xtagsDataSet(tds) );
            QDataSet yrange= Ops.extent( SemanticOps.ytagsDataSet(tds) );
            QDataSet bounds= Ops.join( xrange, yrange );
            Units xunits= SemanticOps.getUnits(xrange);
            Units yunits= SemanticOps.getUnits(yrange);
            if ( !xunits.isConvertibleTo(xValue.getUnits()) 
                && UnitsUtil.isRatioMeasurement(xunits) 
                && ( xunits==Units.dimensionless || xValue.getUnits()==Units.dimensionless ) ) {
                xValue= xunits.createDatum(xValue.value());
            }
            if ( !yunits.isConvertibleTo(yValue.getUnits()) 
                && UnitsUtil.isRatioMeasurement(yunits) 
                && ( yunits==Units.dimensionless || yValue.getUnits()==Units.dimensionless ) ) {
                yValue= yunits.createDatum(yValue.value());
            }
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
            int ix= org.das2.qds.DataSetUtil.closestIndex( xds, xValue );
            QDataSet yds1= yds.slice(ix);
            iy= org.das2.qds.DataSetUtil.closestIndex( yds1, yValue );
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
            iy= org.das2.qds.DataSetUtil.closestIndex( yds, yValue );
            yy= DataSetUtil.asDatum(yds.slice(iy));  //TODO: https://bugs-pw.physics.uiowa.edu/mantis/view.php?id=455
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
        
        DatumFormatter yformatter= yy.getFormatter();
        
        String title= parentPlot.getTitle().trim();
        if ( title.length()>0 ) title= title+"!c";
        myPlot.setTitle( title +  "x: " + formatter.format(xValue)  + " y: " + yformatter.format(yy) );
        ySlice= yy;
            
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
