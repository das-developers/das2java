/* File: DasStackedHistogramPlot.java
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

package org.das2.graph;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import org.das2.event.DasMouseInputAdapter;
import org.das2.event.MouseModule;
import org.das2.event.LengthDragRenderer;
import org.das2.event.CrossHairMouseModule;
import org.das2.components.propertyeditor.Displayable;
import org.das2.dataset.DataSetRebinner;
import org.das2.dataset.TableDataSetConsumer;
import org.das2.dataset.DataSetDescriptor;
import org.das2.dataset.RebinDescriptor;
import org.das2.datum.DatumRange;
import org.das2.util.monitor.ProgressMonitor;
import org.das2.DasException;
import org.das2.components.HorizontalSpectrogramSlicer;
import org.das2.components.VerticalSpectrogramSlicer;
import org.das2.event.HorizontalSlicerMouseModule;
import org.das2.event.VerticalSlicerMouseModule;
import org.das2.datum.Units;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.das2.dataset.AveragePeakTableRebinner;
import org.das2.dataset.AverageTableRebinner;
import org.das2.datum.Datum;
import org.das2.datum.LocationUnits;
import org.das2.qds.DataSetUtil;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;
import org.das2.qds.ops.Ops;

/**
 *
 * @author  jbf
 */
public class StackedHistogramRenderer extends org.das2.graph.Renderer implements TableDataSetConsumer, PropertyChangeListener, Displayable  {
    
    private DasAxis yAxis= null;
    private DasAxis zAxis= null;
    private RowRowConnector zAxisConnector= null;
    private DasRow littleRow=null;
        
    private PeaksIndicator peaksIndicator= PeaksIndicator.MaxDots;
    
    /** Holds value of property sliceRebinnedData. */
    private boolean sliceRebinnedData;
    
    Image plotImage;
    DatumRange imageXRange, imageYRange;
    
    final static Color GREY_PEAKS_COLOR = Color.lightGray.brighter();
	 final static Color BLUE_PEAKS_COLOR = Color.BLUE;
	 final static Color RED_PEAKS_COLOR = Color.RED;

    public enum PeaksIndicator {
        NoPeaks, GrayPeaks, BlackPeaks, MaxDots, PeakLine, BluePeaks, RedPeaks
    }
    
    protected class RebinListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent e) {
            update();
        }
    }
    
    RebinListener rebinListener= new RebinListener();
    
    public StackedHistogramRenderer( DasAxis zAxis ) {
        this.zAxis= zAxis;
        zAxis.addPropertyChangeListener("dataMinimum", rebinListener);
        zAxis.addPropertyChangeListener("dataMaximum", rebinListener);
        zAxis.addPropertyChangeListener("log", rebinListener);
        zAxis.addPropertyChangeListener("flipped", rebinListener);    
    }
    
    public StackedHistogramRenderer( DasPlot parent, DataSetDescriptor dsd, DasAxis zAxis, DasAxis yAxis ) {
        this( zAxis );        
        this.yAxis= yAxis;
    }
    

    @Override
    public void setControl(String s) {
        super.setControl(s);
        setPeaksIndicator( PeaksIndicator.valueOf( getControl("peaksIndicator","GrayPeaks") ) );
    }

    @Override
    public String getControl() {
        Map<String,String> controls= new LinkedHashMap();
        controls.put( "peaksIndicator", this.peaksIndicator.toString() );
        return Renderer.formatControl(controls);
    }    
    
    @Override
    public void render(Graphics g, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {
        
        Graphics2D g2= (Graphics2D)g;
        
        QDataSet xtysData= getDataSet();
        
        if ( xtysData==null ) {
            getParent().postMessage(this, "null data set", DasPlot.WARNING, null, null);
            return;
        }
        
        if ( xtysData.length()==0 ) {
            getParent().postMessage(this, "empty data set", DasPlot.WARNING, null, null);
            return;
        }

        if ( xtysData.rank()!=2 ) {
            getParent().postMessage(this, "dataset is not rank 2", DasPlot.WARNING, null, null);
            return;
        }
        
        Point2D p;
        if (getDataSet()==null && lastException!=null ) {
            renderException(g2,xAxis,yAxis,lastException);
        } else if (plotImage!=null) {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR );
            p= new Point2D.Float( xAxis.getColumn().getDMinimum(), yAxis.getRow().getDMinimum() );
            
            g2.drawImage( plotImage,(int)(p.getX()+0.5),(int)(p.getY()+0.5), getParent() );
            
        }
        g2.dispose();
    }
    
    @Override
    protected void installRenderer() {
        DasPlot parent= getParent();
        DasCanvas canvas= parent.getCanvas();
        littleRow= new DasRow( canvas, 0.5,0.6 );
        zAxisConnector= new RowRowConnector( canvas, littleRow, zAxis.getRow(), parent.getColumn(), zAxis.getColumn() );
        zAxisConnector.setVisible(false);
        canvas.add(zAxisConnector);
        
        if ( parent.getCanvas()!=zAxis.getParent() ) {
            parent.getCanvas().add( zAxis, parent.getRow(), zAxis.getColumn() );
        }
                
        if ( zAxis instanceof DasColorBar ) {
            ((DasColorBar)zAxis).setShowColorBar( false );
        }
        
        if ( yAxis==null ) {
            this.yAxis= parent.getYAxis();
        }
        
        if ( yAxis!=null && yAxis instanceof DasLabelAxis ) {
            DasLabelAxis dlAxis= (DasLabelAxis)yAxis;
            dlAxis.setFloppyItemSpacing(true);
            dlAxis.setOutsidePadding(1);
        }
        
        DasMouseInputAdapter mouseAdapter = parent.getDasMouseInputAdapter();
        
        //TODO: consider delaying construction of slicers until first event
        VerticalSpectrogramSlicer vSlicer = VerticalSpectrogramSlicer.createSlicer( parent, this );
        VerticalSlicerMouseModule vsl = VerticalSlicerMouseModule.create(this);        
        vsl.addDataPointSelectionListener(vSlicer);
        mouseAdapter.addMouseModule(vsl);
        
        HorizontalSpectrogramSlicer hSlicer = HorizontalSpectrogramSlicer.createSlicer( parent, this);
        HorizontalSlicerMouseModule hsl = HorizontalSlicerMouseModule.create(this);
        hsl.addDataPointSelectionListener(hSlicer);
        mouseAdapter.addMouseModule(hsl);
        
        MouseModule ch= new CrossHairMouseModule(parent,this,parent.getXAxis(), parent.getYAxis());
        mouseAdapter.addMouseModule(ch);
        
        DasPlot p= parent;
        mouseAdapter.addMouseModule( new MouseModule( p, new LengthDragRenderer(p,p.getXAxis(),p.getYAxis()), "Length" ) );
    }
    
    @Override
    protected void uninstallRenderer() {
        DasCanvas c= getParent().getCanvas();
        c.remove(zAxisConnector);
        if ( zAxis instanceof DasColorBar ) {
            ((DasColorBar)zAxis).setShowColorBar( true );
        }
        
    }
    
    
    public void setZAxis(DasAxis zAxis) {
        this.zAxis= zAxis;
        throw new IllegalStateException("not supported");
    }
    
    @Override
    public void propertyChange(java.beans.PropertyChangeEvent e) {
        // this code was intended to make it so the zaxis component would move up and down with the labelAxis.
      /*  DasLabelAxis axis= (DasLabelAxis)getYAxis();
       
        if ( axis!=null ) {
            if ( getRow()!=DasRow.NULL ) {
                if ( axis.getInterItemSpace() > getRow().getHeight()/3.5 ) {
                    System.out.println("axis spacing exceeds zAxis spacing");
                    int[] labelPositions= axis.getLabelPositions();
                    zAxisComponent.getAxis().getRow().setDPosition( labelPositions[0], labelPositions[1] );
                } else {
                    int xx2= getRow().getDMaximum();
                    int xx1= getRow().getDMinimum();
                    zAxisComponent.getAxis().getRow().setDPosition( xx1, xx2 );
                }
            }
        } */
    }
    
    /**
     * This sets the yAxis and adds itself to the axis property change listener.
     * TODO: why must this Renderer be special and have a separate set y axis?
     * @param yAxis the new yAxis
     * @throws IllegalArgumentException if the yAxis is not an instanceof DasLabelAxis
     */
    public void setYAxis(DasAxis yAxis) {
        if (yAxis instanceof DasLabelAxis) {
            this.yAxis= (DasLabelAxis)yAxis;
            yAxis.addPropertyChangeListener(this);
        } else {
            throw new IllegalArgumentException("You can't call setYAxis for stackedHistogramPlot");
        }
    }
    
    
    @Override
    public void updatePlotImage( DasAxis xAxis, DasAxis yAxis_1, ProgressMonitor monitor ) throws DasException {
        super.updatePlotImage( xAxis, yAxis_1, monitor );
        final Color BAR_COLOR= Color.BLACK;
                
        DasColumn column= xAxis.getColumn();
        DasRow row= yAxis_1.getRow();
        
        int w= column.getWidth();
        int h= row.getHeight();
        
        if ( w==0 ) return;
        
        //plotImage = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        BufferedImage plotImage1 = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        
        Graphics2D g = plotImage1.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, plotImage1.getWidth(), plotImage1.getHeight());
        g.translate(-column.getDMinimum(),-row.getDMinimum());
        
        g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
          
        //Dimension d;
        
        //double iMin= column.getDMinimum();
        //double jMin= row.getDMinimum();
        
        RebinDescriptor xbins= new RebinDescriptor(xAxis.getDataMinimum(), xAxis.getDataMaximum(), (int)(Math.abs(column.getWidth())/1)+1, (xAxis.isLog()));
        
        imageXRange= xAxis.getDatumRange();
        imageYRange= yAxis_1.getDatumRange();
        
        int xDMax= column.getDMaximum();
        int xDMin= column.getDMinimum();
        
        QDataSet xtysData= (QDataSet)getDataSet();
        
        if ( xtysData==null ) {
            this.plotImage= null;
            getParent().postMessage(this, "null data set", DasPlot.WARNING, null, null);
            return;
        }
        
        if ( xtysData.length()==0 ) {
            getParent().postMessage(this, "empty data set", DasPlot.WARNING, null, null);
            return;
        }

        if ( xtysData.rank()!=2 ) {
            getParent().postMessage(this, "dataset is not rank 2", DasPlot.WARNING, null, null);
            return;
        }
        
        DataSetRebinner rebinner = new Rebinner();
        
        QDataSet data=  rebinner.rebin(xtysData, xbins, null);
        QDataSet peaks= (QDataSet) data.property(QDataSet.BIN_MAX); // can be null for NN.
        if ( peaks==null ) {
            peaks= (QDataSet) data.property(QDataSet.BIN_PLUS);
            if ( peaks==null ) {
                peaks= data;
            } else {
                peaks= Ops.add( data, peaks );
            }
        }
        QDataSet weights= SemanticOps.weightsDataSet(data);
        
        DasAxis yAxis1= yAxis_1;

        int zmid= zAxis.getRow().getDMiddle();
        boolean haveLittleRow= false;

        QDataSet yds= SemanticOps.ytagsDataSet( data );
        Units yunits= SemanticOps.getUnits(yds);
        Units zunits= SemanticOps.getUnits(data);
        
        if ( !zunits.isConvertibleTo( zAxis.getUnits() ) ) {
            String msg= "dataset z units are \""+zunits+"\" while z axis are \"" + zAxis.getUnits() + "\"";
            zunits= zAxis.getUnits();
            getParent().postMessage(this, msg, DasPlot.WARNING, null, null);
        }

        int[] yBases= new int[ data.length(0) ];
        for ( int j=0; j<data.length(0); j++ ) {
            if ( yAxis1 instanceof DasLabelAxis ) {
                yBases[j]= ((DasLabelAxis)yAxis1).getItemMax(yunits.createDatum(yds.value(j)));
            } else {
                yBases[j]= (int)yAxis1.transform( yunits.createDatum(yds.value(j)) );
            }
        }
        int yRowHeight= yBases[0]- yBases[1];
        
        
        
        for (int j = 0; j < data.length(0); j++) {
            
            int yBase;
            //Line2D.Float lBase;
            
            yBase= yBases[j];
            
            g.setColor(Color.lightGray);
            g.drawLine(xDMin, yBase, xDMax, yBase );
            g.setColor(BAR_COLOR);
            
            int yBaseTop= yBase - yRowHeight;
            
            if ( !haveLittleRow && yBaseTop <= zmid  ) {
                littleRow.setDPosition(yBaseTop,yBase);
                haveLittleRow= true;
                this.zAxisConnector.setVisible(true);
                this.zAxisConnector.repaint();
            } 
            
            double [] binStarts= xbins.binStarts();
            //double [] binStops= xbins.binStops();
            
            int y0= yBase;
            
            int littleRowHeight= yBase - yBaseTop;
            double zAxisMax= zAxis.getDataMaximum().doubleValue(zunits);
            double zAxisMin= zAxis.getDataMinimum().doubleValue(zunits);
            
            boolean visible;
            if ( yAxis1 instanceof DasLabelAxis ) {
                visible= yBaseTop >= row.getDMinimum() && yBase <= row.getDMaximum();
            } else {
                visible= yBase >= row.getDMinimum() && yBaseTop <= row.getDMaximum();
            }
            if ( visible ) {
                if ( peaksIndicator==PeaksIndicator.PeakLine && peaks!=null ) {
                    GeneralPath p= new GeneralPath();
                    boolean lastWasFill=true;
                    for (int ibin=0; ibin < data.length(); ibin++) {
                        int x0= (int)xAxis.transform(binStarts[ibin],xbins.getUnits());
                        double zz= peaks.value( ibin, j );
                        if ( !( weights.value( ibin, j )== 0 ) ) {
                            int yMax= (int)zAxis.transform( zz, zunits, yBase, yBaseTop );
                            if ( lastWasFill ) p.moveTo( x0, Math.min( yMax, y0 ) );
                            p.lineTo( x0, Math.min( yMax, y0 ) );
                            lastWasFill= false;
                        } else {
                            lastWasFill= true;
                        }
                    }
                    g.draw(p);
                }
                
                for (int ibin=0; ibin < data.length(); ibin++) {
                    int x0= (int)xAxis.transform(binStarts[ibin],xbins.getUnits());
                    //int x1;
                    //x1=x0+1; // 1 pixel wide
                    double zz= data.value( ibin, j );
                    if ( !( weights.value(ibin,j)==0 ) ) {
                        int yAvg= (int)zAxis.transform( zz, zunits, yBase, yBaseTop );
                        yAvg= yAvg > ( y0 - littleRowHeight ) ? yAvg : ( y0 - littleRowHeight );
                        int yHeight= (y0-yAvg)>(0) ? (y0-yAvg) : 0;
                        //yHeight= yHeight < littleRowHeight ? yHeight : littleRowHeight;
                        if ( peaks!=null ) {
                            double peakValue = peaks.value( ibin, j );
                            if (peakValue >= zAxisMin) {
                                int yMax= (int)zAxis.transform( peakValue, zunits, yBase, yBaseTop );
                                yMax= (y0-yMax)>(0) ? yMax : (y0);
                                yMax= (yMax<yBaseTop) ? yBaseTop : yMax;
                                if (null!=peaksIndicator) switch(peaksIndicator){
										 case MaxDots:
											 g.drawLine(x0,yMax,x0,yMax);
											 break;
										 // do nothing
										 case PeakLine:
											 break;
										 case GrayPeaks:
											 g.setColor(Color.gray);
											 g.drawLine(x0,yMax,x0,y0);
											 g.setColor(BAR_COLOR);
											 break;
										 case BlackPeaks:
											 g.setColor(BAR_COLOR);
											 g.drawLine(x0,yMax,x0,y0);
											 break;
										 case BluePeaks:
											 g.setColor(BLUE_PEAKS_COLOR);
											 g.drawLine(x0,yMax,x0,y0);
											 g.setColor(BAR_COLOR);
											 break;
										 case RedPeaks:
											 g.setColor(RED_PEAKS_COLOR);
											 g.drawLine(x0,yMax,x0,y0);
											 g.setColor(BAR_COLOR);
											 break;
										 default:
											 break;
										 }
                            }
                        }
                        if ( zz>=zAxisMin ) {
                            g.drawLine(x0, yAvg, x0, yAvg+yHeight );
                        }
                    }
                }
            }
        }
        
        g.dispose();
        this.plotImage = plotImage1;
        
        if ( sliceRebinnedData ) super.ds= data;
        
    }
    
    @Override
    public DasAxis getZAxis() {
        return zAxis;
    }
    
    public void setZTitle(String title) {
        getZAxis().setLabel(title);
    }
    
    public static class Rebinner implements DataSetRebinner {
        AverageTableRebinner highResRebinner;
        DataSetRebinner lowResRebinner;
        Rebinner() {
            highResRebinner= new AverageTableRebinner();
            highResRebinner.setInterpolateType( AverageTableRebinner.Interpolate.NearestNeighbor );
            //highResRebinner= new AveragePeakTableRebinner();
            lowResRebinner= new AveragePeakTableRebinner();
            //Plasma Wave Group will have to update this
        }
        
        @Override
        public QDataSet rebin(QDataSet ds, RebinDescriptor x, RebinDescriptor y) throws IllegalArgumentException, DasException {
            QDataSet xds= SemanticOps.xtagsDataSet(ds);
            Datum xwidth= SemanticOps.guessXTagWidth(xds,null);
            if ( xwidth==null ) xwidth= DataSetUtil.asDatum(DataSetUtil.guessCadenceNew(xds,null));

            try {
                QDataSet result;
                QDataSet binMax= (QDataSet) ds.property(QDataSet.BIN_MAX);
                QDataSet binPlus= (QDataSet) ds.property(QDataSet.BIN_PLUS);
                if ( binPlus==null && binMax==null && x.binWidthDatum().lt( xwidth ) ) {
                    logger.log(Level.FINE, "using rebinner {0}", highResRebinner);
                    result= highResRebinner.rebin( ds, x, y ); //Plasma Wave Group will have to update this
                } else {
                    logger.log(Level.FINE, "using rebinner {0}", lowResRebinner);
                    result= lowResRebinner.rebin( ds, x, y ); //Plasma Wave Group will have to update this
                }

                return result;
            } catch ( Exception e ) {
                throw new DasException(e);
            }
        }
        
    }
    
    /** Getter for property peaksIndicator.
     * @return Value of property peaksIndicator.
     */
    public PeaksIndicator getPeaksIndicator() {
        return this.peaksIndicator;
    }
    
    /** Setter for property peaksIndicator.
     * @param peaksIndicator New value of property peaksIndicator.
     */
    public void setPeaksIndicator(PeaksIndicator peaksIndicator) {
        this.peaksIndicator= peaksIndicator;
        updateCacheImage();
    }    
    
    /** Getter for property sliceRebinnedData.
     * @return Value of property sliceRebinnedData.
     *
     */
    public boolean isSliceRebinnedData() {
        return this.sliceRebinnedData;
    }
    
    /** Setter for property sliceRebinnedData.
     * @param sliceRebinnedData New value of property sliceRebinnedData.
     *
     */
    public void setSliceRebinnedData(boolean sliceRebinnedData) {
        this.sliceRebinnedData = sliceRebinnedData;
    }
        
    @Override
    public String getListLabel() {
        return "stacked histogram";
    }
    
    @Override
    public Icon getListIcon() {
        return new ImageIcon(SpectrogramRenderer.class.getResource("/images/icons/stackedHistogram.png"));
    }

}
