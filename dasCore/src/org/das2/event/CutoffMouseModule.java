/*
 * Cutoff2MouseModule.java
 *
 * Created on November 10, 2005, 1:41 PM
 *
 *
 */

package org.das2.event;

import org.das2.DasApplication;
import org.das2.DasException;
import org.das2.dataset.AverageTableRebinner;
import org.das2.dataset.ClippedTableDataSet;
import org.das2.dataset.DataSetConsumer;
import org.das2.dataset.DataSetRebinner;
import org.das2.dataset.DataSetUpdateEvent;
import org.das2.dataset.RebinDescriptor;
import org.das2.dataset.TableDataSetConsumer;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import org.das2.graph.DasAxis;
import org.das2.graph.DasCanvas;
import org.das2.graph.DasColumn;
import org.das2.graph.DasPlot;
import org.das2.graph.DasRow;
import org.das2.graph.SymbolLineRenderer;
import org.das2.util.monitor.ProgressMonitor;
import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.util.Collections;
import javax.swing.JFrame;
import org.das2.qds.DataSetOps;
import org.das2.qds.DataSetUtil;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;
import org.das2.qds.ops.Ops;
import org.das2.qds.util.DataSetBuilder;

/**
 *
 * @author Jeremy
 */
public class CutoffMouseModule extends BoxSelectorMouseModule {
    
    DatumRange xrange;
    DatumRange yrange;
    String lastComment;
    CutoffSlicer cutoffSlicer;
    DasApplication application;
    
    public CutoffMouseModule( DasPlot parent, DataSetConsumer consumer ) {
        super( parent, parent.getXAxis(), parent.getYAxis(), consumer, new BoxRenderer(parent,true), "Cutoff" );
        application= parent.getCanvas().getApplication();
        this.dataSetConsumer= consumer;
    }
    
    @Override
    protected void fireBoxSelectionListenerBoxSelected(BoxSelectionEvent event) {
        
        DatumRange xrange0= xrange;
        DatumRange yrange0= yrange;
        
        xrange= event.getXRange();
        yrange= event.getYRange();
        synchronized (this) {
            if ( event.getPlane("keyChar")!=null ) {
                lastComment= (String)event.getPlane("keyChar");
            } else {
                lastComment= null;
            }
        }
        
        try {
            recalculateSoon( );
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
    
    private void recalculateSoon(  ) {
        Runnable run= new Runnable() {
            public void run() {
                ProgressMonitor mon= application.getMonitorFactory().getMonitor( parent, "calculating cutoffs", "calculating cutoffs" );
                recalculate( mon );
            }
        };
        new Thread( run, "digitizer recalculate" ).start();
    }
    
    private synchronized void recalculate( ProgressMonitor mon) {
        QDataSet tds= (QDataSet)dataSetConsumer.getConsumedDataSet();
        if ( tds==null ) return;
        if ( xrange==null ) return;
        
        tds= new ClippedTableDataSet( tds, xrange, yrange );
        QDataSet yds= SemanticOps.ytagsDataSet(tds);
        QDataSet xds= SemanticOps.xtagsDataSet(tds);

        // average the data down to xResolution
        DataSetRebinner rebinner= new AverageTableRebinner();
        
        DatumRange range= DataSetUtil.asDatumRange( SemanticOps.bounds(tds).slice(0), true );

        RebinDescriptor ddx= getRebinDescriptor( range );
        
        try {
            //TODO: why does rebin throw DasException?
            tds= (QDataSet)rebinner.rebin( tds, ddx, null );
        } catch ( DasException e ) {
            throw new RuntimeException(e);
        }

        double fill= SemanticOps.getUnits(yds).getFillDouble();

        DataSetBuilder builder= new DataSetBuilder( 1, 100 );
        builder.putProperty( QDataSet.UNITS, SemanticOps.getUnits(yds) );
        builder.putProperty( QDataSet.FILL_VALUE, fill );
        DataSetBuilder xbuilder= new DataSetBuilder( 1, 100 );
        xbuilder.putProperty( QDataSet.UNITS, SemanticOps.getUnits(xds) );

        mon.setTaskSize( tds.length() );
        mon.started();

        for ( int i=0; i<tds.length(); i++ ) {
            mon.setTaskProgress( i );
            if ( mon.isCancelled() ) break;
            QDataSet spec= Ops.log10( tds.slice(i) );
            int icutoff= cutoff( spec, slopeMin, nave, isLowCutoff() ? 1 : -1, levelMin );
            if ( icutoff>-1 ) {
                xbuilder.putValue( -1, xds.value(i) );
                builder.putValue( -1, yds.value(icutoff) );
            } else {
                xbuilder.putValue( -1, xds.value(i) );
                builder.putValue( -1, fill );
            }
            builder.nextRecord();
            xbuilder.nextRecord();
        }

        mon.finished();
        
        if ( mon.isCancelled() ) return;
        
        String comment= "Ondrej:"+levelMin+":"+slopeMin+":"+nave;
        if ( lastComment!=null ) {
            comment= lastComment + " "+comment;
        }

        builder.putProperty( QDataSet.USER_PROPERTIES, Collections.singletonMap("comment",comment) );
        xbuilder.putProperty( QDataSet.CADENCE, DataSetUtil.asDataSet(this.xResolution ) );

        builder.putProperty( QDataSet.DEPEND_0, xbuilder.getDataSet() );
        QDataSet vds= builder.getDataSet();
        
        fireDataSetUpdateListenerDataSetUpdated( new DataSetUpdateEvent( vds ) );
        
    }
    
    /**
     * slopeMin in the y units of ds.
     * levelMin in the y units of ds.
     * mult=-1 high cutoff, =1 low cutoff
     */
    public int cutoff( QDataSet ds, Datum slopeMin, int nave, int mult, Datum levelMin ) {
        int nfr= ds.length();
        if ( nfr < (nave+1) ) {
            throw new IllegalArgumentException("DataSet doesn't contain enough elements");
        }
        double[] cumul= new double[nfr];
        Units units= SemanticOps.getUnits(ds);

        double level= levelMin.doubleValue( units );
        double slope= slopeMin.doubleValue( units );
        
        cumul[0]= ds.value(0);
        for ( int i=1; i<nfr; i++ ) {
            cumul[i]= cumul[i-1] + ds.value(i);
        }
        boolean[] icof= new boolean[nfr];

        QDataSet xds= SemanticOps.xtagsDataSet(ds);
        Units xunits= SemanticOps.getUnits(xds);
        
        DataSetBuilder levelBuilder= new DataSetBuilder( 1, 100 );
        DataSetBuilder xlevelBuilder= new DataSetBuilder( 1, 100 );
        xlevelBuilder.putProperty( QDataSet.UNITS,xunits );
        DataSetBuilder slopeBuilder= new DataSetBuilder( 1, 100 );
        DataSetBuilder xslopeBuilder= new DataSetBuilder( 1, 100 );
        xslopeBuilder.putProperty( QDataSet.UNITS,xunits );
        DataSetBuilder icofBuilder= new DataSetBuilder( 1, 100 );
        DataSetBuilder xicofBuilder= new DataSetBuilder( 1, 100 );
        xicofBuilder.putProperty( QDataSet.UNITS,xunits );

        for ( int i=1; i<nfr; i++ ) icof[i]=true;
        icof[0]= false;  // let's be explicit
        icof[nfr-1]= false; // the tests can't reach this one as well.

        for ( int k=1; k<=nave; k++ ) {
            double[] ave= new double[nfr];
            ave[0]= cumul[k-1]/k;
            for ( int j=0; j<nfr-k; j++ ) {
                ave[j+1]= ( cumul[j+k] - cumul[j] ) / k;
                levelBuilder.putValue( -1, ave[j+1] );
                levelBuilder.nextRecord();
                xlevelBuilder.putValue( -1, xds.value(j+1) );
                xlevelBuilder.nextRecord();
            }
            for ( int j=k; j<nfr-k; j++ ) {
                double slopeTest= ( ave[j+1]-ave[j-k] ) / k;
                slopeBuilder.putValue( -1, slopeTest );
                slopeBuilder.nextRecord();
                xslopeBuilder.putValue( -1, xds.value(j+1) );
                xslopeBuilder.nextRecord();
                if ( slopeTest*mult <= slope*mult ) icof[j]= false;
                double uave= mult>0 ? ave[j+k] :  ave[j];
                if ( uave <= level ) icof[j]=false;
                icofBuilder.putValue( -1, icof[j] ? 1: 0 );
                icofBuilder.nextRecord();
                xicofBuilder.putValue( -1, xds.value(j) );
                xicofBuilder.nextRecord();
                
            }
        }
        
        if ( cutoffSlicer!=null ) {
            slopeBuilder.putProperty( QDataSet.DEPEND_0, xslopeBuilder.getDataSet() );
            cutoffSlicer.slopeRenderer.setDataSet( slopeBuilder.getDataSet() );
            levelBuilder.putProperty( QDataSet.DEPEND_0, xlevelBuilder.getDataSet() );
            cutoffSlicer.levelRenderer.setDataSet( levelBuilder.getDataSet() );
            icofBuilder.putProperty( QDataSet.DEPEND_0, xicofBuilder.getDataSet() );
            cutoffSlicer.icofRenderer.setDataSet( icofBuilder.getDataSet() );
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
        DasPlot slopePlot;
        
        JFrame frame;
        
        CutoffSlicer( DasPlot parent, DasAxis xaxis ) {
            frame= new JFrame("Cutoff Slice");
            DasCanvas canvas= new DasCanvas( 300, 600 );
            
            frame.getContentPane().add( canvas );
            frame.pack();
            frame.setVisible(false);
            frame.setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
            
            DasColumn col= new DasColumn( canvas, null, 0, 1, 5, -1.5, 0, 0 );
            DasRow row1= new DasRow( canvas, null, 0/3., 1/3., 2, -1, 0, 0 );
            DasRow row2= new DasRow( canvas, null, 1/3., 2/3., 1.5, -1.5, 0, 0 );
            DasRow row3= new DasRow( canvas, null, 2/3., 3/3., 1, -2, 0, 0 );
            
            DasPlot plot= new DasPlot( xaxis, new DasAxis( new DatumRange( -18,-10,Units.dimensionless ), DasAxis.VERTICAL ) ) {
                @Override
                protected void drawContent(java.awt.Graphics2D g) {
                    super.drawContent(g);
                    
                    int iy;
                    int ix= getColumn().getDMinimum();
                    
                    g.setColor( Color.GRAY );
                    
                    iy= (int)getYAxis().transform( levelMin );
                    g.drawLine( ix, iy, ix+getWidth(), iy );
                    
                    ix= (int)getXAxis().transform( cutoff );
                    g.drawLine( ix, 0, ix, getHeight() );
                    
                    g.setColor( Color.pink );
                    ix= (int)getXAxis().transform( yValue );
                    g.drawLine( ix, 0, ix, getHeight() );
                    
                }
                
            };
            
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
                    throw new IllegalArgumentException("Not implemented, since DataSetUpdateEvents take QDataSets.");
//                    Datum x= e.getX();
//                    HashMap properties= new HashMap();
//                    if ( e.getPlane("keyChar")!=null ) {
//                        properties.put("comment",e.getPlane("keyChar"));
//                    } else {
//                        properties.put("comment","tweak");
//                    }
//                    fireDataSetUpdateListenerDataSetUpdated(
//                            new DataSetUpdateEvent(this,
//                            new SingleVectorDataSet( xValue, e.getX(), properties ) ) );
                }
            } );
            topPlot.addMouseModule( tweakSlicer );
            topPlot.getDasMouseInputAdapter().setPrimaryModule(tweakSlicer);
            
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
            
            plot= new DasPlot( xaxis.createAttachedAxis(), new DasAxis( new DatumRange( -0.3,.3,Units.dimensionless ), DasAxis.VERTICAL )  ) {
                @Override
                protected void drawContent(java.awt.Graphics2D g) {
                    super.drawContent(g);
                    
                    int iy;
                    
                    iy= (int)getYAxis().transform( slopeMin );
                    int ix= getColumn().getDMinimum();
                    g.setColor( Color.lightGray);
                    if ( lowCutoff ) {
                        g.drawString( "slope greater than", ix+3, iy );
                    } else {
                        g.drawString( "slope less than", ix+3, iy );
                    }
                    
                    g.setColor( Color.GRAY );
                    g.drawLine( ix, iy, ix+getWidth(), iy );
                    
                    ix= (int)getXAxis().transform( cutoff );
                    g.drawLine( ix, 0, ix, getHeight() );
                    
                    g.setColor( Color.pink );
                    ix= (int)getXAxis().transform( yValue );
                    g.drawLine( ix, 0, ix, getHeight() );
                    
                    
                }
                
            };
            
            slopePlot= plot;
            
            plot.getYAxis().setLabel("slope");
            slopeRenderer= new SymbolLineRenderer();
            contextSlopeRenderer= new SymbolLineRenderer();
            contextSlopeRenderer.setColor( Color.GRAY );
            //plot.addRenderer(contextSlopeRenderer);
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
            
            plot= new DasPlot( xaxis.createAttachedAxis(), new DasAxis( new DatumRange( -0.3,1.3,Units.dimensionless ), DasAxis.VERTICAL )  );
            plot.getYAxis().setLabel("icof");
            icofRenderer= new SymbolLineRenderer();
            plot.addRenderer(icofRenderer);
            canvas.add( plot, row3, col );
            
        }
        
        private void recalculate( ) {
            dataPointSelected(lastSelectedPoint);
        }
        
        public void dataPointSelected(org.das2.event.DataPointSelectionEvent event) {
            this.lastSelectedPoint= event;
            QDataSet tds= (QDataSet)dataSetConsumer.getConsumedDataSet();
            
            this.xValue= event.getX();
            this.yValue= event.getY();
            
            if ( xrange==null ) return;
            
            // average the data down to xResolution
            DataSetRebinner rebinner= new AverageTableRebinner();
            
            DatumRange range= DataSetUtil.asDatumRange( DataSetOps.dependBounds( tds ).slice(0), true );
            RebinDescriptor ddx= getRebinDescriptor( range );
            
            try {
                tds= (QDataSet)rebinner.rebin( tds, ddx, null );
            } catch ( DasException e ) {
                throw new RuntimeException(e);
            }

            QDataSet xds= SemanticOps.xtagsDataSet(tds);

            int i= DataSetUtil.closestIndex( xds, event.getX() );
            
            QDataSet contextDs= tds.slice(i);
            contextLevelRenderer.setDataSet( Ops.log10( contextDs ) );
            
            //VectorDataSet slopeDs= VectorUtil.finiteDerivative( contextDs, nave );
            //contextSlopeRenderer.setDataSet( slopeDs );

            DatumRange xrange= DataSetUtil.asDatumRange( DataSetOps.dependBounds( tds ).slice(0), true );
            tds= new ClippedTableDataSet( tds, xrange, yrange );
            
            this.xValue= SemanticOps.getDatum( xds, xds.value(i) );

            topPlot.setTitle( "" +  xValue + " " + yValue);
            
            QDataSet spec= Ops.log10( tds.slice(i) );
            QDataSet xspec= SemanticOps.xtagsDataSet(spec);

            int icutoff= cutoff( spec, slopeMin, nave, isLowCutoff() ? 1 : -1, levelMin );
            if ( icutoff==-1 ) {
                cutoff= SemanticOps.getUnits(xspec).getFillDatum();
            } else {
                cutoff= SemanticOps.getDatum(xspec,xspec.value(icutoff));
            }
            
            showPopup();
        }
        
        private void showPopup() {
            if ( !frame.isVisible() ) frame.setVisible(true);
        }
        
    }
    
    public  DataPointSelectionListener getSlicer( DasPlot plot, TableDataSetConsumer consumer ) {
        DasAxis sourceYAxis = plot.getYAxis();        
        DasAxis xAxis = sourceYAxis.createAttachedAxis( DasAxis.HORIZONTAL );
        cutoffSlicer= new CutoffSlicer( plot, xAxis );
        return cutoffSlicer;
        
    }

//    private void testCutoff() {
//        // see /home/jbf/voyager/cutoff/input.txt
//        double[] spec= new double[] {
//            -12.7093, -12.8479, -13.0042, -13.1509, -13.3007, -13.4671,
//            -13.5536, -13.6603, -13.8000, -13.8873, -13.9908, -14.1162,
//            -14.2016, -14.2694, -14.2844, -14.3126, -14.3507, -14.3841,
//            -14.4252, -14.4779, -14.4972, -14.5226, -14.6059, -14.6517,
//            -14.6545, -14.2863, -13.9616, -13.6898, -13.7407, -13.8821,
//            -14.1541, -14.4287, -14.6663, -14.8647, -15.0540, -15.0863,
//            -15.1190, -15.1464, -15.1479, -15.1399, -15.1284, -15.2001,
//            -15.2780, -15.3611, -15.3976, -15.4230, -15.4467, -15.4879,
//            -15.5437, -15.6058, -15.6501, -15.6606, -15.6737, -15.6867,
//            -15.6955, -15.7425, -15.8222, -15.9376, -16.0174, -16.0091,
//        };
//        double[] tags= new double[ spec.length ];
//        for ( int i=0; i< tags.length; i++ ) { tags[i]= i+1; }
//        double slope= 0.266692;
//        int nave=3;
//        boolean isLowCutoff= true;
//        int mult= isLowCutoff ? 1 : -1;
//        double level= -14;
//
//        DDataSet test= DDataSet.wrap(spec);
//        test.putProperty( QDataSet.UNITS, Units.v2pm2Hz );
//        test.putProperty( QDataSet.DEPEND_0, DDataSet.wrap(tags) );
//
//        int icut= cutoff( test, Units.v2pm2Hz.createDatum(slope), nave, mult, Units.v2pm2Hz.createDatum(level) );
//        System.out.println("icut="+icut+"  should be 25");
//    }
    
    
    private transient java.util.ArrayList dataSetUpdateListenerList; //TODO: can we not use javax.swing.event.EventListenerList?
    
    public synchronized void addDataSetUpdateListener(org.das2.dataset.DataSetUpdateListener listener) {
        if (dataSetUpdateListenerList == null ) {
            dataSetUpdateListenerList = new java.util.ArrayList();
        }
        dataSetUpdateListenerList.add(listener);
    }
    
    public synchronized void removeDataSetUpdateListener(org.das2.dataset.DataSetUpdateListener listener) {
        if (dataSetUpdateListenerList != null ) {
            dataSetUpdateListenerList.remove(listener);
        }
    }
    
    private void fireDataSetUpdateListenerDataSetUpdated(org.das2.dataset.DataSetUpdateEvent event) {
        java.util.ArrayList list;
        synchronized (this) {
            if (dataSetUpdateListenerList == null) return;
            list = (java.util.ArrayList)dataSetUpdateListenerList.clone();
        }
        for (int i = 0; i < list.size(); i++) {
            ((org.das2.dataset.DataSetUpdateListener)list.get(i)).dataSetUpdated(event);
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
        Datum oldVal= this.slopeMin;
        if ( !this.slopeMin.equals( slopeMin ) ) {
            this.slopeMin = slopeMin;
            PropertyChangeEvent e= new PropertyChangeEvent( this, "slope", oldVal, slopeMin );
            firePropertyChangeListenerPropertyChange( e );
            recalculateSoon();
        }
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
        Datum oldVal= this.levelMin;
        if ( !this.levelMin.equals( levelMin ) ) {
            this.levelMin = levelMin;
            PropertyChangeEvent e= new PropertyChangeEvent( this, "level", oldVal, levelMin );
            firePropertyChangeListenerPropertyChange( e );
            
            levelMin.getFormatter().format(levelMin);
            
            recalculateSoon();
            
        }
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
        int oldVal= this.nave;
        if ( this.nave!=nave ) {
            this.nave = nave;
            PropertyChangeEvent e= new PropertyChangeEvent( this, "nave", Integer.valueOf(oldVal), Integer.valueOf(nave) );
            firePropertyChangeListenerPropertyChange( e );
            recalculateSoon();
        }
    }
    
    private Datum xResolution= Units.milliseconds.createDatum(1000);
    
    public Datum getXResolution() {
        return this.xResolution;
    }
    
    public void setXResolution(Datum xResolution) {
        Datum oldVal= this.xResolution;
        if ( !this.xResolution.equals( xResolution ) ) {
            this.xResolution = xResolution;
            PropertyChangeEvent e= new PropertyChangeEvent( this, "timeResolution", oldVal, this.xResolution );
            firePropertyChangeListenerPropertyChange( e );
            recalculateSoon();
        }
    }
    
    /**
     * Holds value of property lowCutoff.  true indicates this is a low
     * cutoff (look for peak in slope).  false indicates this is a
     * high cutoff (look for valley in slope).
     */
    private boolean lowCutoff;
    
    /**
     * Getter for property lowCutoff.
     * @return Value of property lowCutoff.
     */
    public boolean isLowCutoff() {
        return this.lowCutoff;
    }
    
    /**
     * Setter for property lowCutoff.
     * @param lowCutoff New value of property lowCutoff.
     */
    public void setLowCutoff(boolean lowCutoff) {
        Boolean oldVal= this.lowCutoff;
        if ( this.lowCutoff!=lowCutoff ) {
            this.lowCutoff = lowCutoff;
            PropertyChangeEvent e= new PropertyChangeEvent( this, "lowCutoff", oldVal, Boolean.valueOf(lowCutoff) );
            firePropertyChangeListenerPropertyChange( e );
            recalculateSoon();
            if ( this.cutoffSlicer != null ) this.cutoffSlicer.slopePlot.repaint();
        }
    }
    
    /**
     * Utility field used by event firing mechanism.
     */
    private javax.swing.event.EventListenerList listenerList =  new javax.swing.event.EventListenerList();
    
    /**
     * Registers PropertyChangeListener to receive events.
     * @param listener The listener to register.
     */
    public void addPropertyChangeListener(java.beans.PropertyChangeListener listener) {
        listenerList.add(java.beans.PropertyChangeListener.class, listener);
    }
    
    /**
     * Removes PropertyChangeListener from the list of listeners.
     * @param listener The listener to remove.
     */
    public void removePropertyChangeListener(java.beans.PropertyChangeListener listener) {
        listenerList.remove(java.beans.PropertyChangeListener.class, listener);
    }
    
    /**
     * Notifies all registered listeners about the event.
     *
     * @param event The event to be fired
     */
    private void firePropertyChangeListenerPropertyChange(java.beans.PropertyChangeEvent event) {
        Object[] listeners;
        listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i]==java.beans.PropertyChangeListener.class) {
                ((java.beans.PropertyChangeListener)listeners[i+1]).propertyChange(event);
            }
        }
    }
    
}
