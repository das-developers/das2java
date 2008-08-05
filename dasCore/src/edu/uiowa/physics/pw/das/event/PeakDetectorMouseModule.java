/*
 * Cutoff2MouseModule.java
 *
 * Created on November 10, 2005, 1:41 PM
 *
 *
 */

package edu.uiowa.physics.pw.das.event;

import edu.uiowa.physics.pw.das.DasException;
import edu.uiowa.physics.pw.das.dataset.AverageTableRebinner;
import edu.uiowa.physics.pw.das.dataset.ClippedTableDataSet;
import edu.uiowa.physics.pw.das.dataset.DataSet;
import edu.uiowa.physics.pw.das.dataset.DataSetConsumer;
import edu.uiowa.physics.pw.das.dataset.DataSetDescriptor;
import edu.uiowa.physics.pw.das.dataset.DataSetRebinner;
import edu.uiowa.physics.pw.das.dataset.DataSetUpdateEvent;
import edu.uiowa.physics.pw.das.dataset.DataSetUtil;
import edu.uiowa.physics.pw.das.dataset.NoDataInIntervalException;
import edu.uiowa.physics.pw.das.dataset.RebinDescriptor;
import edu.uiowa.physics.pw.das.dataset.SingleVectorDataSet;
import edu.uiowa.physics.pw.das.dataset.TableDataSet;
import edu.uiowa.physics.pw.das.dataset.TableDataSetConsumer;
import edu.uiowa.physics.pw.das.dataset.VectorDataSet;
import edu.uiowa.physics.pw.das.dataset.VectorDataSetBuilder;
import edu.uiowa.physics.pw.das.dataset.test.PolynomialDataSetDescriptor;
import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.datum.DatumRange;
import edu.uiowa.physics.pw.das.datum.Units;
import edu.uiowa.physics.pw.das.graph.DasAxis;
import edu.uiowa.physics.pw.das.graph.DasCanvas;
import edu.uiowa.physics.pw.das.graph.DasColumn;
import edu.uiowa.physics.pw.das.graph.DasPlot;
import edu.uiowa.physics.pw.das.graph.DasRow;
import edu.uiowa.physics.pw.das.graph.Psym;
import edu.uiowa.physics.pw.das.graph.PsymConnector;
import edu.uiowa.physics.pw.das.graph.SymColor;
import edu.uiowa.physics.pw.das.graph.SymbolLineRenderer;
import edu.uiowa.physics.pw.das.math.QuadFitUtil;
import edu.uiowa.physics.pw.das.system.DasLogger;
import edu.uiowa.physics.pw.das.util.DasMath;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 *
 * @author Jeremy
 */
public class PeakDetectorMouseModule extends BoxSelectorMouseModule {
    
    DasAxis xaxis, yaxis;
    DataSetConsumer dataSetConsumer;
    DatumRange xrange;
    DatumRange yrange;
    String lastComment;
    PeakSlicer peakSlicer;
    
    
    static Logger logger= DasLogger.getLogger( DasLogger.GUI_LOG );
    
    public PeakDetectorMouseModule( DasPlot parent, DataSetConsumer consumer ) {
        super( parent, parent.getXAxis(), parent.getYAxis(), consumer, new BoxRenderer(parent,true), "Peak Detector" );
        this.dataSetConsumer= consumer;
    }
    
    protected void fireBoxSelectionListenerBoxSelected(BoxSelectionEvent event) {
        
        final DatumRange xrange0= xrange;
        final DatumRange yrange0= yrange;
        
        xrange= event.getXRange();
        yrange= event.getYRange();
        if ( event.getPlane("keyChar")!=null ) {
            lastComment= (String)event.getPlane("keyChar");
        } else {
            lastComment= null;
        }
        
        Runnable run= new Runnable() {
            public void run() {
                try {
                    recalculate();
                } catch ( RuntimeException ex ) {
                    xrange= xrange0;
                    yrange= yrange0;
                    throw ex;
                }
            }
        };
        new Thread(run).start();
    }
    
    /**
     * return RebinDescriptor that is on descrete, repeatable boundaries.
     * get us2000, divide by resolution, truncate, multiply by resolution.
     */
    private RebinDescriptor getRebinDescriptor( DatumRange range ) {
        double res= xResolution.doubleValue(Units.microseconds);
        double min= range.min().doubleValue(Units.us2000);
        min= Math.floor( min / res );
        double max= range.max().doubleValue(Units.us2000);
        max= Math.ceil( max / res );
        int nbin= (int)(max-min);
        
        RebinDescriptor ddx= new RebinDescriptor( min*res, max*res, Units.us2000, nbin, false );
        return ddx;
    }
    
    private VectorDataSet toDb( VectorDataSet ds, Datum reference ) {
        Units refUnits= reference.getUnits();
        double refValue= reference.doubleValue(refUnits);
        Units yunits= Units.dB;
        Units xunits= ds.getXUnits();
        VectorDataSetBuilder builder= new VectorDataSetBuilder( xunits, yunits );
        
        for ( int i=0; i<ds.getXLength(); i++ ) {
            builder.insertY( ds.getXTagDouble(i,xunits), 10 * DasMath.log10( ds.getDouble(i,refUnits) / refValue ));
        }
        return builder.toVectorDataSet();
    }
    
    class FitDescriptor {
        Datum time;
        Datum peakX;
        Datum halfWidth;
        double[] fitCoef; // fit parameters
        int[] fitPoints;
        VectorDataSet digitizedDataSet;
    }
    
    private FitDescriptor doDigitize( TableDataSet tds, int i ) {
        VectorDataSet slice=  tds.getXSlice(i);
        int peakIndex= peakIndex( slice, yrange.min(), yrange.max() );
        
        
        if ( peakIndex != -1 ) {
            VectorDataSet spec= toDb( slice, slice.getDatum(peakIndex) );
            
            double[] fit= getFit( spec, peakIndex );
            if ( fit!=null ) {
                double peakx = QuadFitUtil.quadPeak(fit);
                double peaky= fit[0] + fit[1]*peakx + fit[2]*peakx*peakx;
                double halfWidth= QuadFitUtil.quadHalfWidth( fit, 3 );
                FitDescriptor result= new FitDescriptor();
                result.halfWidth= tds.getYUnits().getOffsetUnits().createDatum(halfWidth);
                result.peakX= tds.getYUnits().createDatum(peakx);
                result.time=  tds.getXTagDatum( i );
                result.digitizedDataSet= spec;
                result.fitCoef= fit;
                // result.peakY= slice.getDatum(peakIndex) * // convert from dB to physical unit
                return result;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
    
    private TableDataSet conditionData( TableDataSet tds, DatumRange range ) throws NoDataInIntervalException {
        // average the data down to xResolution
        DataSetRebinner rebinner= new AverageTableRebinner();
        
        RebinDescriptor ddx= getRebinDescriptor( range );
        
        try {
            //TODO: why does rebin throw DasException?
            tds= (TableDataSet)rebinner.rebin( tds, ddx, null );
        } catch ( NoDataInIntervalException e ) {
            throw e;
        } catch ( DasException e ) {
            throw new RuntimeException(e);
        }
        return tds;
    }
    
    private void recalculate( ) {
        TableDataSet tds= (TableDataSet)dataSetConsumer.getConsumedDataSet();
        tds= new ClippedTableDataSet( tds, xrange, yrange );
        
        DatumRange range= DataSetUtil.xRange( tds );
        try {
            tds= conditionData( tds, range );
        } catch (NoDataInIntervalException ex) {
            return;
        }
        
        VectorDataSetBuilder builder= new VectorDataSetBuilder( tds.getXUnits(), tds.getYUnits() );
        builder.addPlane( "halfWidth", tds.getYUnits() );
        for ( int i=0; i<tds.getXLength(); i++ ) {
            FitDescriptor fit= doDigitize( tds, i );
            if ( fit!=null ) {
                builder.insertY( fit.time, fit.peakX );
                builder.insertY( fit.time, fit.halfWidth, "halfWidth" );
            }
        }
        
        String comment= "West:"+levelMin;
        if ( lastComment!=null ) {
            comment= lastComment + " "+comment;
        }
        builder.setProperty( "comment", comment );
        builder.setProperty( DataSet.PROPERTY_X_TAG_WIDTH, this.xResolution );
        VectorDataSet vds= builder.toVectorDataSet();
        
        fireDataSetUpdateListenerDataSetUpdated( new DataSetUpdateEvent( this,vds ) );
        
    }
    
    /**
     * finds the index of the highest point where min>=xtag>max, or -1 if no peak is found.
     */
    private static int peakIndex(VectorDataSet ds, Datum min, Datum max) {
        Datum yMax = ds.getYUnits().createDatum(Double.NEGATIVE_INFINITY);
        int iMax = -1;
        int i0 = -1; //The first x index in range
        int i1 = -1; //The last x index in range
        for (int i = 0; i < ds.getXLength(); i++) {
            Datum y = ds.getDatum(i);
            Datum x = ds.getXTagDatum(i);
            if ( x.ge(min) && x.lt(max)) {
                if (i0 == -1) i0 = i;
                i1 = i;
                if (y.gt(yMax)) {
                    yMax = y;
                    iMax = i;
                }
            }
        }
        
        // We don't want to record this value if it is the first or
        // last within range, since this inidicates that the value
        // probably isn't really a maximum.
        if (iMax == i0 || iMax == i1) {
            iMax = -1;
        }
        return iMax;
    }
    
    private static int[] findFive(VectorDataSet ds, int peakIndex) {
        int[] indices, result;
        int nIndex;
        double yLowPrev, yHighPrev;
        Units yUnits = ds.getYUnits();
        
        indices = new int[7];
        indices[0] = peakIndex;
        nIndex = 1;
        
        yHighPrev = yLowPrev = ds.getDouble(peakIndex, yUnits);
        
        for (int i = 1; i <= 3 && nIndex < 5; i++) {
            double yLow = (peakIndex - i) >= 0 ? ds.getDouble(peakIndex - i, yUnits) : Double.POSITIVE_INFINITY;
            double yHigh = (peakIndex + i) < ds.getXLength() ? ds.getDouble(peakIndex + i, yUnits) : Double.POSITIVE_INFINITY;
            if (yLow < yLowPrev) {
                indices[nIndex++] = peakIndex - i;
                yLowPrev = yLow;
            } else {
                yLowPrev = Double.NEGATIVE_INFINITY;
            }
            if (yHigh < yHighPrev) {
                indices[nIndex++] = peakIndex + i;
                yHighPrev = yHigh;
            } else {
                yHighPrev = Double.NEGATIVE_INFINITY;
            }
        }
        
        Arrays.sort(indices, 0, nIndex);
        result = new int[nIndex];
        System.arraycopy(indices, 0, result, 0, nIndex);
        
        return result;
    }
    
    /**
     * returns a double[2] if a fit is possible, null otherwise.
     * @param slice a VectorDataSet with yUnits convertable to Units.dB.
     */
    private double[] getFit( VectorDataSet slice, int jMax) {
        double[] px, py, w;
        double[] c;
        int [] indices;
        Datum y = slice.getXTagDatum(jMax);
        Datum z = slice.getDatum(jMax);
        Units xUnits = slice.getXUnits();
        Units yUnits = Units.dB;
        
        indices = findFive(slice, jMax);
        int peakOfFive= Arrays.binarySearch(indices, jMax);
        px = new double[indices.length];
        py = new double[indices.length];
        w = new double[indices.length];
        for (int iIndex = 0; iIndex < indices.length; iIndex++) {
            px[iIndex] = slice.getXTagDouble(indices[iIndex], xUnits);
            py[iIndex] = slice.getDouble(indices[iIndex], yUnits);
            w[iIndex] = Units.dB.convertDoubleTo(Units.dimensionless, py[iIndex]);
        }
        
        double threeDown = py[peakOfFive] - levelMin.doubleValue(Units.dB);
        if (threeDown < py[0] && threeDown < py[py.length - 1]) {
            return null;
        }
        
        //Arrays.fill(w, 1.0);
        c = QuadFitUtil.quadfit(px, py, w);
        
        if (c[2] >= -0.0) {
            return null;
        }
        
        double peak = QuadFitUtil.quadPeak(c);
        if (peak < slice.getXTagDouble(0, xUnits) || peak > slice.getXTagDouble(slice.getXLength() - 1, xUnits)) {
            return null;
        }
        
        return c;
    }
    
    
    private class PeakSlicer implements  DataPointSelectionListener {
        
        DataPointSelectionEvent lastSelectedPoint;
        FitDescriptor fit;
        Datum yValue;
        Datum xValue;
        int selectedRecord; //
        
        SymbolLineRenderer levelRenderer;
        SymbolLineRenderer fitRenderer;
        SymbolLineRenderer fitPointRenderer;
        
        DasPlot topPlot;
        JFrame frame;
        
        Action prevAction= new AbstractAction("<< Prev") {
            public void actionPerformed( ActionEvent e ) {
                Datum xnew= xValue.subtract( xResolution );
                DataPointSelectionEvent evNew= new DataPointSelectionEvent( this, xnew, yValue );
                PeakSlicer.this.dataPointSelected(evNew);
            }
        } ;
        
        Action nextAction= new AbstractAction("Next >>") {
            public void actionPerformed( ActionEvent e ) {
                Datum xnew= xValue.add( xResolution );
                DataPointSelectionEvent evNew= new DataPointSelectionEvent( this, xnew, yValue );
                PeakSlicer.this.dataPointSelected(evNew);
            }
        } ;
        
        
        PeakSlicer( DasPlot parent, DasAxis xaxis ) {
            frame= new JFrame("Peak Slice");
            JPanel contentPanel= new JPanel();
            contentPanel.setLayout( new BorderLayout() );
            DasCanvas canvas= new DasCanvas( 300, 300 );
            contentPanel.add( canvas, BorderLayout.CENTER );
            Box npBox= Box.createHorizontalBox();
            npBox.add( new JButton( prevAction ) );
            npBox.add( new JButton( nextAction ) );
            contentPanel.add( npBox, BorderLayout.NORTH );
            
            frame.getContentPane().add( contentPanel );
            frame.pack();
            frame.setVisible(false);
            frame.setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
            
            DasColumn col= DasColumn.create( canvas );
            DasRow row1= DasRow.create( canvas, 0, 1 );
            
            DasAxis yaxis= new DasAxis( new DatumRange( 0.001, 1,Units.dimensionless ), DasAxis.VERTICAL ) ;
            yaxis.setLog(true);
            DasPlot plot= new PeakDasPlot( xaxis, yaxis );
            plot.getYAxis().setLabel("level");
            plot.getXAxis().setTickLabelsVisible(false);
            levelRenderer= new SymbolLineRenderer();
            
            fitRenderer= new SymbolLineRenderer();
            fitRenderer = new SymbolLineRenderer();
            fitRenderer.setColor(SymColor.blue);
            
            fitPointRenderer = new SymbolLineRenderer();
            fitPointRenderer.setColor(SymColor.red);
            fitPointRenderer.setPsymConnector(PsymConnector.NONE);
            fitPointRenderer.setPsym(Psym.TRIANGLES);
            fitPointRenderer.setSymSize(3.0);
            
            plot.addRenderer(fitRenderer);
            plot.addRenderer(levelRenderer);
            
            topPlot= plot;
            
            DataPointSelectorMouseModule tweakSlicer=
                    new DataPointSelectorMouseModule( topPlot, levelRenderer,
                    new VerticalSliceSelectionRenderer(topPlot), "tweak cutoff" ) {
                public void keyPressed( KeyEvent event ) {
                    System.err.print(event);
                    if ( event.getKeyCode()==KeyEvent.VK_DOWN ) {
                    } else if ( event.getKeyCode()==KeyEvent.VK_UP ) {
                        Datum xnew= xValue.subtract( xResolution );
                        DataPointSelectionEvent evNew= new DataPointSelectionEvent( this, xnew, yValue );
                        PeakSlicer.this.dataPointSelected(evNew);
                    }
                }
            };
            tweakSlicer.setDragEvents(true); // only key events fire
            tweakSlicer.addDataPointSelectionListener( new DataPointSelectionListener() {
                public void dataPointSelected( DataPointSelectionEvent e ) {
                    Datum x= e.getX();
                    HashMap properties= new HashMap();
                    if ( e.getPlane("keyChar")!=null ) {
                        properties.put("comment",e.getPlane("keyChar"));
                    } else {
                        properties.put("comment","tweak");
                    }
                    fireDataSetUpdateListenerDataSetUpdated(
                            new DataSetUpdateEvent(this,
                            new SingleVectorDataSet( xValue, e.getX(), properties ) ) );
                }
            } );
            topPlot.addMouseModule( tweakSlicer );
            topPlot.getMouseAdapter().setPrimaryModule(tweakSlicer);
            
            DataPointSelectorMouseModule levelSlicer=
                    new DataPointSelectorMouseModule( topPlot, levelRenderer,
                    new HorizontalSliceSelectionRenderer(topPlot), "peak S/N level" );
            levelSlicer.addDataPointSelectionListener( new DataPointSelectionListener() {
                public void dataPointSelected( DataPointSelectionEvent e ) {
                    Datum y= e.getY();
                    PeakDetectorMouseModule.this.setLevelMin( y );
                }
            } );
            levelSlicer.setDragEvents(false);
            levelSlicer.setKeyEvents(false);
            levelSlicer.setReleaseEvents(true);
            topPlot.addMouseModule( levelSlicer );
            
            canvas.add( plot, row1, col );
            
        }
        
        
        public void dataPointSelected(edu.uiowa.physics.pw.das.event.DataPointSelectionEvent event) {
            logger.fine("got DataPointSelectionEvent: "+event.getX() );
            this.lastSelectedPoint= event;
            
            TableDataSet tds= (TableDataSet)dataSetConsumer.getConsumedDataSet();
            
            this.xValue= event.getX();
            this.yValue= event.getY();
            
            if ( xrange==null ) return;
            
            DatumRange range= new DatumRange( event.getX(), event.getX() );
            try {
                tds= conditionData( tds, range );
            } catch (NoDataInIntervalException ex) {
                return;
            }
            
            logger.fine("find closest column " );
            int i= DataSetUtil.closestColumn( tds, event.getX() );
            
            
            this.xValue= tds.getXTagDatum(i);
            topPlot.setTitle( "" +  xValue );
            
            logger.info("doDigitize");
            fit= doDigitize( tds, i );
            if ( fit!=null ) {
                levelRenderer.setDataSet( fit.digitizedDataSet );
                Datum resLimit= topPlot.getXAxis().invTransform(1) .subtract( topPlot.getXAxis().invTransform(0) );
                PolynomialDataSetDescriptor dsd= new PolynomialDataSetDescriptor( fit.fitCoef, fit.peakX.getUnits(),
                        fit.digitizedDataSet.getYUnits(), resLimit ) ;
                dsd.setYMin(fit.digitizedDataSet.getYUnits().createDatum( -5 ));
                fitRenderer.setDataSetDescriptor( dsd );
            }
            
            showPopup();
        }
        
        private void showPopup() {
            if ( !frame.isVisible() ) frame.setVisible(true);
        }
        
        class PeakDasPlot extends DasPlot {
            PeakDasPlot( DasAxis x, DasAxis y ) {
                super(x,y);
            }
            protected void drawContent(java.awt.Graphics2D g) {
                super.drawContent(g);
                
                if ( fit!=null ) {
                    g.setColor( Color.GRAY );
                    int ix= (int)this.getXAxis().transform( fit.peakX );
                    g.drawLine( ix, 0, ix, getHeight() );
                    int iy= (int)this.getYAxis().transform( Units.dB.createDatum(-3) );
                    g.drawLine( 0, iy, getWidth(), iy );
                    
                    g.setColor( Color.pink );
                    ix= (int)getXAxis().transform( yValue );
                    g.drawLine( ix, 0, ix, getHeight() );
                }
                
            }
        }
    }
    
    public  DataPointSelectionListener getSlicer( DasPlot plot, TableDataSetConsumer consumer ) {
        DasAxis sourceYAxis = plot.getYAxis();
        DasAxis sourceZAxis = consumer.getZAxis();
        
        DatumRange range= sourceYAxis.getDatumRange();
        DasAxis xAxis = sourceYAxis.createAttachedAxis( DasAxis.HORIZONTAL );
        peakSlicer= new PeakSlicer( plot, xAxis );
        return peakSlicer;
        
    }
    
    
    private transient java.util.ArrayList dataSetUpdateListenerList;
    
    public synchronized void addDataSetUpdateListener(edu.uiowa.physics.pw.das.dataset.DataSetUpdateListener listener) {
        if (dataSetUpdateListenerList == null ) {
            dataSetUpdateListenerList = new java.util.ArrayList();
        }
        dataSetUpdateListenerList.add(listener);
    }
    
    public synchronized void removeDataSetUpdateListener(edu.uiowa.physics.pw.das.dataset.DataSetUpdateListener listener) {
        if (dataSetUpdateListenerList != null ) {
            dataSetUpdateListenerList.remove(listener);
        }
    }
    
    private void fireDataSetUpdateListenerDataSetUpdated(edu.uiowa.physics.pw.das.dataset.DataSetUpdateEvent event) {
        java.util.ArrayList list;
        synchronized (this) {
            if (dataSetUpdateListenerList == null) return;
            list = (java.util.ArrayList)dataSetUpdateListenerList.clone();
        }
        for (int i = 0; i < list.size(); i++) {
            ((edu.uiowa.physics.pw.das.dataset.DataSetUpdateListener)list.get(i)).dataSetUpdated(event);
        }
    }
    
    /**
     * Holds value of property levelMin.
     */
    private Datum levelMin= Units.dB.createDatum(-3.);
    
    /**
     * Getter for property levelMin.
     * @return Value of property levelMin.
     */
    public Datum getLevelMin() {
        return this.levelMin;
    }
    
    /**
     * Setter for property levelMin.
     * @param levelMin New value of property levelMin.
     */
    public void setLevelMin(Datum levelMin) {
        this.levelMin = levelMin;
        recalculate();
    }
    
    private Datum xResolution= Units.milliseconds.createDatum(500);
    
    public Datum getXResolution() {
        return this.xResolution;
    }
    
    public void setXResolution(Datum xResolution) {
        this.xResolution = xResolution;
    }
    
}
