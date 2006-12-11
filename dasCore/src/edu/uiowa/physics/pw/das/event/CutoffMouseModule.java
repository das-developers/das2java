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
import edu.uiowa.physics.pw.das.dataset.DataSetRebinner;
import edu.uiowa.physics.pw.das.dataset.DataSetUpdateEvent;
import edu.uiowa.physics.pw.das.dataset.DataSetUtil;
import edu.uiowa.physics.pw.das.dataset.DefaultVectorDataSet;
import edu.uiowa.physics.pw.das.dataset.RebinDescriptor;
import edu.uiowa.physics.pw.das.dataset.SingleVectorDataSet;
import edu.uiowa.physics.pw.das.dataset.TableDataSet;
import edu.uiowa.physics.pw.das.dataset.TableDataSetConsumer;
import edu.uiowa.physics.pw.das.dataset.VectorDataSet;
import edu.uiowa.physics.pw.das.dataset.VectorDataSetBuilder;
import edu.uiowa.physics.pw.das.dataset.VectorUtil;
import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.datum.DatumRange;
import edu.uiowa.physics.pw.das.datum.Units;
import edu.uiowa.physics.pw.das.graph.DasAxis;
import edu.uiowa.physics.pw.das.graph.DasCanvas;
import edu.uiowa.physics.pw.das.graph.DasColumn;
import edu.uiowa.physics.pw.das.graph.DasPlot;
import edu.uiowa.physics.pw.das.graph.DasRow;
import edu.uiowa.physics.pw.das.graph.SymbolLineRenderer;
import java.awt.Color;
import java.util.HashMap;
import javax.swing.JFrame;

/**
 *
 * @author Jeremy
 */
public class CutoffMouseModule extends BoxSelectorMouseModule {
        
    DasAxis xaxis, yaxis;
    DataSetConsumer dataSetConsumer;
    DatumRange xrange;
    DatumRange yrange;
    String lastComment;
    CutoffSlicer cutoffSlicer;
    
     public CutoffMouseModule( DasPlot parent, DataSetConsumer consumer ) {
         super( parent, parent.getXAxis(), parent.getYAxis(), consumer, new BoxRenderer(parent,true), "Cutoff" );
         this.dataSetConsumer= consumer;
     }

    protected void fireBoxSelectionListenerBoxSelected(BoxSelectionEvent event) {
        
        DatumRange xrange0= xrange;
        DatumRange yrange0= yrange;
        
        xrange= event.getXRange();
        yrange= event.getYRange();
        if ( event.getPlane("keyChar")!=null ) {
            lastComment= (String)event.getPlane("keyChar");
        } else {
            lastComment= null;
        }
        
        try {
            recalculate();
        } catch ( RuntimeException ex ) {
            xrange= xrange0;
            yrange= yrange0;
            throw ex;
        }
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
    
    private void recalculate( ) {
        TableDataSet tds= (TableDataSet)dataSetConsumer.getConsumedDataSet();
        tds= new ClippedTableDataSet( tds, xrange, yrange );
        
        // average the data down to xResolution
        DataSetRebinner rebinner= new AverageTableRebinner();
        
        DatumRange range= DataSetUtil.xRange( tds );
        RebinDescriptor ddx= getRebinDescriptor( range );
        
        try {
            //TODO: why does rebin throw DasException?
            tds= (TableDataSet)rebinner.rebin( tds, ddx, null );
        } catch ( DasException e ) {
            throw new RuntimeException(e);
        }
        
        VectorDataSetBuilder builder= new VectorDataSetBuilder( tds.getXUnits(), tds.getYUnits() );
        for ( int i=0; i<tds.getXLength(); i++ ) {
            VectorDataSet spec= DataSetUtil.log10( tds.getXSlice(i) );
            int icutoff= cutoff( spec, slopeMin, nave, 1, levelMin );
            if ( icutoff>-1 ) {
                builder.insertY( tds.getXTagDatum(i), tds.getYTagDatum( tds.tableOfIndex(i), icutoff ) );
            } else {
                Units yunits=tds.getYUnits();
                builder.insertY( tds.getXTagDatum(i), yunits.createDatum(yunits.getFillDouble()) );
            }
        }
        
        String comment= "Ondrej:"+levelMin+":"+slopeMin+":"+nave;
        if ( lastComment!=null ) {
            comment= lastComment + " "+comment;
        }
        builder.setProperty("comment",comment);
        builder.setProperty( DataSet.PROPERTY_X_TAG_WIDTH, this.xResolution );
        VectorDataSet vds= builder.toVectorDataSet();
        
        fireDataSetUpdateListenerDataSetUpdated( new DataSetUpdateEvent( this,vds ) );
        
    }
    
    /**
     * slopeMin in the y units of ds.
     * levelMin in the y units of ds.
     * mult=-1 high cutoff, =1 low cutoff
     */
    public int cutoff( VectorDataSet ds, Datum slopeMin, int nave, int mult, Datum levelMin ) {
        int nfr= ds.getXLength();
        if ( nfr < (nave+1) ) {
            throw new IllegalArgumentException("DataSet doesn't contain enough elements");
        }
        double[] cumul= new double[nfr];
        Units units= ds.getYUnits();
        double level= levelMin.doubleValue( units );
        double slope= slopeMin.doubleValue( units );
        
        cumul[0]= ds.getDouble(0, units);
        for ( int i=1; i<nfr; i++ ) {
            cumul[i]= cumul[i-1] + ds.getDouble(i, units);
        }
        boolean[] icof= new boolean[nfr];
        
        VectorDataSetBuilder levelBuilder= new VectorDataSetBuilder( ds.getXUnits(), units );
        VectorDataSetBuilder slopeBuilder= new VectorDataSetBuilder( ds.getXUnits(), units );
        VectorDataSetBuilder icofBuilder= new VectorDataSetBuilder( ds.getXUnits(), Units.dimensionless );
        
        for ( int i=1; i<nfr; i++ ) icof[i]=true;
        icof[0]= false;  // let's be explicit
        
        for ( int k=1; k<=nave; k++ ) {
            double[] ave= new double[nfr];
            ave[0]= cumul[k-1]/k;
            for ( int j=0; j<nfr-k; j++ ) {
                ave[j+1]= ( cumul[j+k] - cumul[j] ) / k;
                levelBuilder.insertY( ds.getXTagDatum(j+1), units.createDatum(ave[j+1]) );
            }
            for ( int j=k; j<nfr-k; j++ ) {
                double slopeTest= ( ave[j+1]-ave[j-k] ) / k;
                slopeBuilder.insertY( ds.getXTagDatum(j+1), units.createDatum(slopeTest) );
                if ( slopeTest*mult <= slope ) icof[j]= false;
                double uave= mult>0 ? ave[j+k] :  ave[j];
                if ( uave <= level ) icof[j]=false;
                icofBuilder.insertY( ds.getXTagDatum(j), icof[j] ? units.dimensionless.createDatum(1) : units.dimensionless.createDatum(0) );
            }
        }
        
        if ( cutoffSlicer!=null ) {
            cutoffSlicer.slopeRenderer.setDataSet( slopeBuilder.toVectorDataSet() );
            cutoffSlicer.levelRenderer.setDataSet( levelBuilder.toVectorDataSet() );
            cutoffSlicer.icofRenderer.setDataSet( icofBuilder.toVectorDataSet() );
        }
        
        int icutOff=-1;
        
        for ( int j= ( mult<0 ? nfr-1 : 0 ); j>=0 && j<nfr; j+=mult ) {
            if ( icof[j] ) {
                icutOff= j;
                break;
            }
        }
        
        return icutOff;
    }
    
    private class CutoffSlicer implements  DataPointSelectionListener {
        
        DataPointSelectionEvent lastSelectedPoint;
        Datum cutoff;
        Datum yValue;
        Datum xValue;        
        
        SymbolLineRenderer levelRenderer;
        SymbolLineRenderer contextLevelRenderer;
        SymbolLineRenderer slopeRenderer;
        SymbolLineRenderer contextSlopeRenderer;
        SymbolLineRenderer icofRenderer;
        DasPlot topPlot;
        JFrame frame;
        
        CutoffSlicer( DasPlot parent, DasAxis xaxis ) {
            frame= new JFrame("Cutoff Slice");
            DasCanvas canvas= new DasCanvas( 300, 600 );
            frame.getContentPane().add( canvas );
            frame.pack();
            frame.setVisible(false);
            frame.setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
            
            DasColumn col= DasColumn.create( canvas );
            DasRow row1= DasRow.create( canvas, 0, 3 );
            DasRow row2= DasRow.create( canvas, 1, 3 );
            DasRow row3= DasRow.create( canvas, 2, 3 );
            
            DasPlot plot= new CutoffDasPlot( xaxis, new DasAxis( new DatumRange( -8,0,Units.dimensionless ), DasAxis.VERTICAL ) );
            plot.getYAxis().setLabel("level");
            plot.getXAxis().setTickLabelsVisible(false);
            levelRenderer= new SymbolLineRenderer();
            contextLevelRenderer= new SymbolLineRenderer();
            contextLevelRenderer.setColor( Color.GRAY );
            
            plot.addRenderer(contextLevelRenderer);
            plot.addRenderer(levelRenderer);
            
            topPlot= plot;
            
            DataPointSelectorMouseModule tweakSlicer=
                    new DataPointSelectorMouseModule( topPlot, levelRenderer,
                    new VerticalSliceSelectionRenderer(topPlot), "tweak cutoff" );
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
                    new HorizontalSliceSelectionRenderer(topPlot), "cutoff level" );
            levelSlicer.addDataPointSelectionListener( new DataPointSelectionListener() {
                public void dataPointSelected( DataPointSelectionEvent e ) {
                    Datum y= e.getY();
                    CutoffMouseModule.this.setLevelMin( y );                    
                }
            } );
            levelSlicer.setDragEvents(false);
            levelSlicer.setKeyEvents(false);
            levelSlicer.setReleaseEvents(true);
            topPlot.addMouseModule( levelSlicer );
            
            canvas.add( plot, row1, col );
            
            plot= new CutoffDasPlot( xaxis.createAttachedAxis(), new DasAxis( new DatumRange( -0.3,.3,Units.dimensionless ), DasAxis.VERTICAL )  );
            plot.getYAxis().setLabel("slope");
            plot.getXAxis().setTickLabelsVisible(false);
            slopeRenderer= new SymbolLineRenderer();
            contextSlopeRenderer= new SymbolLineRenderer();
            contextSlopeRenderer.setColor( Color.GRAY );
            plot.addRenderer(contextSlopeRenderer);
            plot.addRenderer(slopeRenderer);
            
            // TODO: here's a bug mode to check into (topPlot should be plot):
            //DataPointSelectorMouseModule slopeSlicer=
            //        new DataPointSelectorMouseModule( topPlot, levelRenderer,
            //        new HorizontalSliceSelectionRenderer( topPlot ), "slope level" );
            DataPointSelectorMouseModule slopeSlicer=
                    new DataPointSelectorMouseModule( plot, levelRenderer,
                    new HorizontalSliceSelectionRenderer( plot ), "slope level" );
            slopeSlicer.addDataPointSelectionListener( new DataPointSelectionListener() {
                public void dataPointSelected( DataPointSelectionEvent e ) {
                    Datum y= e.getY();
                    CutoffMouseModule.this.setSlopeMin( y );                    
                }
            } );
            slopeSlicer.setDragEvents(false);
            slopeSlicer.setKeyEvents(false);
            slopeSlicer.setReleaseEvents(true);
            plot.addMouseModule( slopeSlicer );
            
            canvas.add( plot, row2, col );
            
            plot= new CutoffDasPlot( xaxis.createAttachedAxis(), new DasAxis( new DatumRange( -0.3,1.3,Units.dimensionless ), DasAxis.VERTICAL )  );
            plot.getYAxis().setLabel("icof");
            icofRenderer= new SymbolLineRenderer();
            plot.addRenderer(icofRenderer);
            canvas.add( plot, row3, col );
            
        }
        
        private void recalculate( ) {
            dataPointSelected(lastSelectedPoint);
        }
        
        public void dataPointSelected(edu.uiowa.physics.pw.das.event.DataPointSelectionEvent event) {
            this.lastSelectedPoint= event;
            TableDataSet tds= (TableDataSet)dataSetConsumer.getConsumedDataSet();
            
            this.xValue= event.getX();
            this.yValue= event.getY();
            
            if ( xrange==null ) return;
            
            // average the data down to xResolution
            DataSetRebinner rebinner= new AverageTableRebinner();
            
            DatumRange range= DataSetUtil.xRange( tds );
            RebinDescriptor ddx= getRebinDescriptor( range );
            
            try {
                tds= (TableDataSet)rebinner.rebin( tds, ddx, null );
            } catch ( DasException e ) {
                throw new RuntimeException(e);
            }
            
            int i= DataSetUtil.closestColumn( tds, event.getX() );
            
            VectorDataSet contextDs= tds.getXSlice(i);
            contextLevelRenderer.setDataSet( DataSetUtil.log10( contextDs ) );
            
            //VectorDataSet slopeDs= VectorUtil.finiteDerivative( contextDs, nave );
            //contextSlopeRenderer.setDataSet( slopeDs );
            
            tds= new ClippedTableDataSet( tds, DataSetUtil.xRange(tds), yrange );
            
            this.xValue= tds.getXTagDatum(i);
            topPlot.setTitle( "" +  xValue + " " + yValue);
            
            VectorDataSet spec= DataSetUtil.log10( tds.getXSlice(i) );
            
            int icutoff= cutoff( spec, slopeMin, nave, 1, levelMin );
            cutoff= spec.getXTagDatum(icutoff);
            
            showPopup();
        }
        
        private void showPopup() {
            if ( !frame.isVisible() ) frame.setVisible(true);
        }
        
        class CutoffDasPlot extends DasPlot {
            CutoffDasPlot( DasAxis x, DasAxis y ) {
                super(x,y);
            }
            protected void drawContent(java.awt.Graphics2D g) {
                super.drawContent(g);
                
                int iy;
                
                g.setColor( Color.GRAY );
                
                iy= (int)getYAxis().transform( levelMin );
                g.drawLine( 0, iy, getWidth(), iy );
                
                iy= (int)getYAxis().transform( slopeMin );
                g.drawLine( 0, iy, getWidth(), iy );
                
                int ix= (int)getXAxis().transform( cutoff );
                g.drawLine( ix, 0, ix, getHeight() );
                
                g.setColor( Color.pink );
                ix= (int)getXAxis().transform( yValue );
                g.drawLine( ix, 0, ix, getHeight() );
                
                
            }
        }
    }
    
    public  DataPointSelectionListener getSlicer( DasPlot plot, TableDataSetConsumer consumer ) {
        DasAxis sourceYAxis = plot.getYAxis();
        DasAxis sourceZAxis = consumer.getZAxis();
        
        DatumRange range= sourceYAxis.getDatumRange();
        DasAxis xAxis = sourceYAxis.createAttachedAxis( DasAxis.HORIZONTAL );
        cutoffSlicer= new CutoffSlicer( plot, xAxis );
        return cutoffSlicer;
        
    }
    
    private void testCutoff() {
        // see /home/jbf/voyager/cutoff/input.txt
        double[] spec= new double[] {
            -12.7093, -12.8479, -13.0042, -13.1509, -13.3007, -13.4671,
                    -13.5536, -13.6603, -13.8000, -13.8873, -13.9908, -14.1162,
                    -14.2016, -14.2694, -14.2844, -14.3126, -14.3507, -14.3841,
                    -14.4252, -14.4779, -14.4972, -14.5226, -14.6059, -14.6517,
                    -14.6545, -14.2863, -13.9616, -13.6898, -13.7407, -13.8821,
                    -14.1541, -14.4287, -14.6663, -14.8647, -15.0540, -15.0863,
                    -15.1190, -15.1464, -15.1479, -15.1399, -15.1284, -15.2001,
                    -15.2780, -15.3611, -15.3976, -15.4230, -15.4467, -15.4879,
                    -15.5437, -15.6058, -15.6501, -15.6606, -15.6737, -15.6867,
                    -15.6955, -15.7425, -15.8222, -15.9376, -16.0174, -16.0091,
        };
        double[] tags= new double[ spec.length ];
        for ( int i=0; i< tags.length; i++ ) { tags[i]= i+1; }
        double slope= 0.266692;
        int nave=3;
        int mult= 1;
        double level= -14;
        int icut= cutoff(
                new DefaultVectorDataSet( spec, Units.hertz, spec, Units.v2pm2Hz, new HashMap() ),
                Units.v2pm2Hz.createDatum(slope), nave, mult, Units.v2pm2Hz.createDatum(level) );
        System.out.println("icut="+icut+"  should be 25");
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
     * Holds value of property slopeMin.
     */
    private Datum slopeMin= Units.dimensionless.createDatum( 0.26 );
    
    /**
     * Getter for property slopeMin.
     * @return Value of property slopeMin.
     */
    public Datum getSlopeMin() {
        return this.slopeMin;
    }
    
    /**
     * Setter for property slopeMin.
     * @param slopeMin New value of property slopeMin.
     */
    public void setSlopeMin(Datum slopeMin) {        
        this.slopeMin = slopeMin;
        recalculate();
    }
    
    /**
     * Holds value of property levelMin.
     */
    private Datum levelMin= Units.dimensionless.createDatum(-4.);
    
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
    
    /**
     * Holds value of property nave.
     */
    private int nave=3;
    
    /**
     * Getter for property nave.
     * @return Value of property nave.
     */
    public int getNave() {
        return this.nave;
    }
    
    /**
     * Setter for property nave.
     * @param nave New value of property nave.
     */
    public void setNave(int nave) {
        this.nave = nave;
        recalculate();
    }
    
    private Datum xResolution= Units.milliseconds.createDatum(1000);
    
    public Datum getXResolution() {
        return this.xResolution;
    }
    
    public void setXResolution(Datum xResolution) {
        this.xResolution = xResolution;
    }
     
}
