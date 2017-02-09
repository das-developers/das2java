/*
 * Cutoff2MouseModule.java
 *
 * Created on November 10, 2005, 1:41 PM
 *
 *
 */

package org.das2.event;

import java.text.ParseException;
import org.das2.DasApplication;
import org.das2.DasException;
import org.das2.dataset.AverageTableRebinner;
import org.das2.dataset.ClippedTableDataSet;
import org.das2.dataset.DataSet;
import org.das2.dataset.DataSetConsumer;
import org.das2.dataset.DataSetRebinner;
import org.das2.dataset.DataSetUpdateEvent;
import org.das2.dataset.DataSetUtil;
import org.das2.dataset.DefaultVectorDataSet;
import org.das2.dataset.RebinDescriptor;
import org.das2.dataset.SingleVectorDataSet;
import org.das2.dataset.TableDataSet;
import org.das2.dataset.TableDataSetConsumer;
import org.das2.dataset.VectorDataSet;
import org.das2.dataset.VectorDataSetBuilder;
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
import java.awt.Font;
import java.awt.FontMetrics;
import java.beans.PropertyChangeEvent;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JFrame;
import org.das2.util.monitor.NullProgressMonitor;

/**
 * CutoffMouseModule contains Ondrej's code for selecting the cutoff, and allows
 * operator to graphically adjust the control parameters.
 * @author Jeremy
 */
public class CutoffMouseModule extends BoxSelectorMouseModule {
    
    DasAxis xaxis, yaxis;
    DataSetConsumer dataSetConsumer;
    DatumRange xrange;
    DatumRange yrange;
    String lastComment;
    CutoffSlicer cutoffSlicer;
    DasApplication application;
    
    private static final Logger logger= org.das2.system.DasLogger.getLogger( 
		 org.das2.system.DasLogger.DATA_OPERATIONS_LOG );
    
    public CutoffMouseModule( DasPlot parent, DataSetConsumer consumer ) {
        super( parent, parent.getXAxis(), parent.getYAxis(), consumer, 
			      new BoxRenderer(parent,true), "Cutoff" );
        application= parent.getCanvas().getApplication();
        this.dataSetConsumer= consumer;
    }

	 public static final String ALGO_ONDREJ = "Ondrej: ";
	 
    /** Set the configuration of slope calculation and digitizer output using an
	  * algorithm selection and configuration string.  At present only one algorithm is
	  * supported:  
	  * 
	  *     ALGO_ONDREJ
	  * 
	  * Parameter settings for the Ondrej algorithm are:
	  *    min: (TODO explain)
	  *    slopeMin (TODO explain)
	  *    nave     (TODO explain)
	  *    cutoff   (TODO explain)
	  *    xres     (TODO explain)
	  * 
	  * Example configuration strings known to work are:
	  *   
	  *  For Voyager FFT'ed Waveforms:  min=-4. slopeMin=0.26 nave=3 cutoff=lower xres=1s
	  *  For Galileo Survey Spectra:    min=1.78 slopeMin=0.072 nave=3 cutoff=lower xres=120s
	  *                                 min=1.78 slopeMin=0.072 nave=3 cutoff=lower xres=60s
     *                                 min=1.78 slopeMin=0.072 nave=3 cutoff=lower xres=30s
     * @param config
     */
    public void setConfig(String algo, String config ) throws ParseException {
        if ( !algo.equals(ALGO_ONDREJ) )
            throw new IllegalArgumentException("Only Ondrej's cutoff algorithim has been"
					                                + " implemented at this time");

        Pattern p= Pattern.compile("(\\S+)=(\\S+)");

        Matcher m= p.matcher(config);

        while ( m.find() ) {
            String name= m.group(1);
            String sval= m.group(2);
            if ( name.equals("min") ) {
                setLevelMin(Units.dimensionless.parse(sval));
            } else if ( name.equals("slopeMin") ) {
                setSlopeMin(Units.dimensionless.parse(sval));
            } else if ( name.equals("nave") ) {
                setNave( Integer.parseInt(sval) );
            } else if (name.equals("cutoff") ) {
                setLowCutoff( "lower".equals(sval) );
            } else if (name.equals("xres") ) {
                setXResolution( Units.seconds.parse(sval) );
            }
        }
    }
    
	 @Override
    protected void fireBoxSelectionListenerBoxSelected(BoxSelectionEvent event) {
        
        DatumRange xrange0= xrange;
        DatumRange yrange0= yrange;
        
        xrange= event.getXRange();
        yrange= event.getYRange();
        if ( event.getPlane("keyChar")!=null ) {
            lastComment= (String)event.getPlane("keyChar");
        } else {
            if ( xrange.width().lt( getXResolution().multiply(5) ) ) {
                super.fireBoxSelectionListenerBoxSelected(event);
            }
            return;
        }
        
        String keyChar= String.valueOf( event.getPlane("keyChar") );
        if ( keyChar.equals("!") ) {  // note null becomes "null"
            assertChannel(new NullProgressMonitor());
        } else {
            try {
                recalculateSoon( );
            } catch ( RuntimeException ex ) {
                xrange= xrange0;
                yrange= yrange0;
                throw ex;
            }
        }
    }
    
    /**
     * return RebinDescriptor that is on discrete, repeatable boundaries.
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
				@Override
            public void run() {
                ProgressMonitor mon= application.getMonitorFactory().getMonitor( 
						                  parent, "calculating cutoffs", "calculating cutoffs" );
                recalculate( mon );
            }
        };
        new Thread( run, "digitizer recalculate" ).start();
    }
    
    private synchronized void assertChannel( ProgressMonitor mon ) {
        TableDataSet tds= (TableDataSet)dataSetConsumer.getConsumedDataSet();
        if ( tds==null ) return;
        if ( xrange==null ) return;
        
        tds= new ClippedTableDataSet( tds, xrange, yrange );
                
        if ( xResolution.value()>0. ) {
            // average the data down to xResolution
            DataSetRebinner rebinner= new AverageTableRebinner();
            DatumRange range= DataSetUtil.xRange( tds );
            RebinDescriptor ddx= getRebinDescriptor( range );
            try {
                tds= (TableDataSet)rebinner.rebin( tds, ddx, null, null );
            } catch (IllegalArgumentException | DasException ex) {
                Logger.getLogger(CutoffMouseModule.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        VectorDataSetBuilder builder= new VectorDataSetBuilder( tds.getXUnits(), tds.getYUnits() );
        mon.setTaskSize( tds.getXLength() );
        mon.started();
        
        Datum level= tds.getYTagDatum( 0, tds.getYLength(0)/2 );
        
        for ( int i=0; i<tds.getXLength(); i++ ) {
            builder.insertY( tds.getXTagDatum(i), level );
        }
        
        mon.finished();
        
        if ( mon.isCancelled() ) return;
        
        String comment= ( "fixed:"+yrange.min()+":"+yrange.max() ).replaceAll(" ","");
        
        builder.setProperty("comment",comment);
        if ( this.xResolution.value()>0 ) {
            builder.setProperty( DataSet.PROPERTY_X_TAG_WIDTH, this.xResolution );
        }
        VectorDataSet vds= builder.toVectorDataSet();
        
        fireDataSetUpdateListenerDataSetUpdated( new DataSetUpdateEvent( this,vds ) );
                

    }
    
    private synchronized void recalculate( ProgressMonitor mon) {
        TableDataSet tds= (TableDataSet)dataSetConsumer.getConsumedDataSet();
        if ( tds==null ) return;
        if ( xrange==null ) return;
        
        tds= new ClippedTableDataSet( tds, xrange, yrange );
        
        // average the data down to xResolution
        
        if ( xResolution.value()>0. ) {
            // average the data down to xResolution
            DataSetRebinner rebinner= new AverageTableRebinner();
            DatumRange range= DataSetUtil.xRange( tds );
            RebinDescriptor ddx= getRebinDescriptor( range );
            try {
                tds= (TableDataSet)rebinner.rebin( tds, ddx, null, null );
            } catch (IllegalArgumentException | DasException ex) {
                Logger.getLogger(CutoffMouseModule.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        VectorDataSetBuilder builder= new VectorDataSetBuilder( tds.getXUnits(), tds.getYUnits() );
        
        mon.setTaskSize( tds.getXLength() );
        mon.started();
        
        for ( int i=0; i<tds.getXLength(); i++ ) {
            mon.setTaskProgress( i );
            if ( mon.isCancelled() ) break;
            VectorDataSet spec= DataSetUtil.log10( tds.getXSlice(i) );
            int icutoff= cutoff( spec, slopeMin, nave, isLowCutoff() ? 1 : -1, levelMin, cutoffSlicer );
            if ( icutoff>-1 ) {
                builder.insertY( tds.getXTagDatum(i), tds.getYTagDatum( tds.tableOfIndex(i), icutoff ) );
            } else if ( icutoff<0 ) {
                Units yunits=tds.getYUnits();
                builder.insertY( tds.getXTagDatum(i), yunits.createDatum(yunits.getFillDouble()) );
            }
        }
        
        mon.finished();
        
        if ( mon.isCancelled() ) return;
        
        String comment= "Ondrej:"+levelMin+":"+slopeMin+":"+nave;
        if ( lastComment!=null ) {
            comment= lastComment + " "+comment;
        }
        builder.setProperty("comment",comment);
        if ( this.xResolution.value()>0 ) {
            builder.setProperty( DataSet.PROPERTY_X_TAG_WIDTH, this.xResolution );
        }
        VectorDataSet vds= builder.toVectorDataSet();
        
        fireDataSetUpdateListenerDataSetUpdated( new DataSetUpdateEvent( this,vds ) );
        
    }
    
    /**
     * @param ds  PSD vector (spectrum)
     * @param slopeMin required PSD slope per frequency bin
     * @param nave required bandwidth
     * @param mult -1 for lower cutoff, 1 for upper cutoff (check this, I think Ondrej's 
	  *        got this backwards.)
     * @param levelMin
     * @param cutoffSlicer if available, render data to here for diagnostics
     * @return the index of the cutoff.
     */
    public static int cutoff(
        VectorDataSet ds, Datum slopeMin, int nave, int mult, Datum levelMin, 
		  CutoffSlicer cutoffSlicer 
	 ) {    
        assert mult==-1 || mult==1;
        
        int nfr= ds.getXLength();
        if ( nfr < (nave+1) ) {
            logger.fine( "DataSet doesn't contain enough elements" );
            return 0;
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
        icof[nfr-1]= false; // the tests can't reach this one as well.
        
        //TODO: describe what is happening here!
        for ( int k=1; k<=nave; k++ ) { // TODO: rewrite.  This may be correct but it is opaque.
            double[] ave= new double[nfr];
            ave[0]= cumul[k-1]/k;
            for ( int j=0; j<nfr-k; j++ ) {
                ave[j+1]= ( cumul[j+k] - cumul[j] ) / k;
                levelBuilder.insertY( ds.getXTagDatum(j+1), units.createDatum(ave[j+1]) );
            }
            for ( int j=k; j<nfr-k; j++ ) {
                double slopeTest= ( ave[j+1]-ave[j-k] ) / k;
                slopeBuilder.insertY( ds.getXTagDatum(j+1), units.createDatum(slopeTest) );
                if ( slopeTest*mult <= slope*mult ) icof[j]= false;
                double uave= mult>0 ? ave[j+k] :  ave[j];
                if ( uave <= level ) icof[j]=false;
                icofBuilder.insertY( ds.getXTagDatum(j), 
						 icof[j] ? units.dimensionless.createDatum(1) : units.dimensionless.createDatum(0) );
            }
        }
        
        if ( cutoffSlicer!=null ) {
            cutoffSlicer.slopeRenderer.setDataSet( slopeBuilder.toVectorDataSet() );
            cutoffSlicer.levelRenderer.setDataSet( levelBuilder.toVectorDataSet() );
            cutoffSlicer.icofRenderer.setDataSet( icofBuilder.toVectorDataSet() );
        }
        
        int icutOff=-1;
        
        if ( mult<0 ) {
            for ( int j= nfr-1; j>=0; j-- ) {
                if ( icof[j] ) {
                    icutOff= j;
                    break;
                }
            }
            
        } else {
            for ( int j= 0; j<nfr; j++ ) {
                if ( icof[j] ) {
                    icutOff= j;
                    break;
                }
            }
        }
        
        return icutOff;
    }
    
    public class CutoffSlicer implements  DataPointSelectionListener {
        
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
            canvas.setFont( Font.decode("sans-14") );
            frame.getContentPane().add( canvas );
            frame.pack();
            frame.setVisible(false);
            frame.setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
            
            DasColumn col= new DasColumn( canvas, null, 0, 1, 5, -1.5, 0, 0 );
            DasRow row1= new DasRow( canvas, null, 0/3., 1/3., 2, -1, 0, 0 );
            DasRow row2= new DasRow( canvas, null, 1/3., 2/3., 1.5, -1.5, 0, 0 );
            DasRow row3= new DasRow( canvas, null, 2/3., 3/3., 1, -2, 0, 0 );
            
            // Autoplot community dasCore has decorators.
            DasPlot plot= new DasPlot( xaxis, new DasAxis( 
					new DatumRange( -18,-10,Units.dimensionless ), DasAxis.VERTICAL ) ) {
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

                    g.setColor( Color.lightGray );
                    
                    FontMetrics fm= g.getFontMetrics();
                    g.drawString( "gray is original data, black is averaged", 
							  getColumn().getDMinimum()+3, getRow().getDMinimum()+fm.getHeight() );
                    
                }
                
            };
            
            plot.getYAxis().setLabel("level");
            plot.getXAxis().setTickLabelsVisible(false);
            levelRenderer= new SymbolLineRenderer();
            levelRenderer.setAntiAliased(true);
            contextLevelRenderer= new SymbolLineRenderer();
            contextLevelRenderer.setColor( Color.GRAY );
            contextLevelRenderer.setAntiAliased(true);
            
            plot.addRenderer(contextLevelRenderer);
            plot.addRenderer(levelRenderer);
            
            topPlot= plot;
            
            DataPointSelectorMouseModule tweakSlicer=
                    new DataPointSelectorMouseModule( topPlot, levelRenderer,
                    new VerticalSliceSelectionRenderer(topPlot), "tweak cutoff" );
            tweakSlicer.setDragEvents(true); // only key events fire
            tweakSlicer.addDataPointSelectionListener( new DataPointSelectionListener() {
					 @Override
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
            topPlot.getDasMouseInputAdapter().setPrimaryModule(tweakSlicer);
            
            DataPointSelectorMouseModule levelSlicer=
                    new DataPointSelectorMouseModule( topPlot, levelRenderer,
                    new HorizontalSliceSelectionRenderer(topPlot), "cutoff level" );
            levelSlicer.addDataPointSelectionListener(new DataPointSelectionListener() {
					 @Override
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
            
            plot= new DasPlot( xaxis.createAttachedAxis(), new DasAxis( 
					  new DatumRange( -0.3,.3,Units.dimensionless ), DasAxis.VERTICAL )  ) {
					 @Override
                protected void drawContent(java.awt.Graphics2D g) {
                    super.drawContent(g);
                    
                    int iy;
                    
                    iy= (int)getYAxis().transform( slopeMin );
                    int ix= getColumn().getDMinimum();
                    g.setColor( Color.lightGray);
                    FontMetrics fm= g.getFontMetrics();
                    if ( lowCutoff ) {
                        g.drawString( "slope greater than", ix+3, getRow().getDMinimum()+fm.getHeight()  );
                    } else {
                        g.drawString( "slope less than", ix+3, getRow().getDMinimum()+fm.getHeight()  );
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
            slopeRenderer.setAntiAliased(true);
            contextSlopeRenderer= new SymbolLineRenderer();
            contextSlopeRenderer.setColor( Color.GRAY );
            contextSlopeRenderer.setAntiAliased(true);
            //plot.addRenderer(contextSlopeRenderer);
            plot.addRenderer(slopeRenderer);
            
            // TODO: here's a bug mode to check into (topPlot should be plot):
            //DataPointSelectorMouseModule slopeSlicer=
            //        new DataPointSelectorMouseModule( topPlot, levelRenderer,
            //        new HorizontalSliceSelectionRenderer( topPlot ), "slope level" );
            DataPointSelectorMouseModule slopeSlicer=
                    new DataPointSelectorMouseModule( plot, levelRenderer,
                    new HorizontalSliceSelectionRenderer( plot ), "slope level" );
            slopeSlicer.addDataPointSelectionListener(new DataPointSelectionListener() {
					 @Override
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
            
            plot= new DasPlot( xaxis.createAttachedAxis(), new DasAxis( 
					new DatumRange( -0.3,1.3,Units.dimensionless ), DasAxis.VERTICAL )  ) {
					  @Override
                 protected void drawContent(java.awt.Graphics2D g) {
                    super.drawContent(g);
                    
                    g.setColor( Color.lightGray );
                    
                    FontMetrics fm= g.getFontMetrics();
                    
                    String s= lowCutoff ? "lowest" : "highest";
                    g.drawString( s + " value equal to one is used", 
							  getColumn().getDMinimum()+3, getRow().getDMinimum()+fm.getHeight() );
                    
                    
                }
            };
            plot.getYAxis().setLabel("icof");
            icofRenderer= new SymbolLineRenderer();
            icofRenderer.setAntiAliased(true);
            plot.addRenderer(icofRenderer);
            canvas.add( plot, row3, col );
            
        }
        
        private void recalculate( ) {
            dataPointSelected(lastSelectedPoint);
        }
        
		  @Override
        public void dataPointSelected(org.das2.event.DataPointSelectionEvent event) {
            this.lastSelectedPoint= event;
            TableDataSet tds= (TableDataSet)dataSetConsumer.getConsumedDataSet();
            
            this.xValue= event.getX();
            this.yValue= event.getY();
            
            if ( xrange==null ) return;
            
            // average the data down to xResolution
            DataSetRebinner rebinner= new AverageTableRebinner();
                        
            if ( xResolution.value()>0 ) {
                DatumRange range= DataSetUtil.xRange( tds );
                RebinDescriptor ddx= getRebinDescriptor( range );

                try {
                    tds= (TableDataSet)rebinner.rebin( tds, ddx, null, null );
                } catch ( DasException e ) {
                    throw new RuntimeException(e);
                }
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
            
            int icutoff= cutoff( spec, slopeMin, nave, isLowCutoff() ? 1 : -1, levelMin, cutoffSlicer );
            if ( icutoff==-1 ) {
                cutoff= spec.getXUnits().getFillDatum();
            } else {
                cutoff= spec.getXTagDatum(icutoff);
            }
            
            showPopup();
        }
        
        private void showPopup() {
            if ( !frame.isVisible() ) frame.setVisible(true);
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
    
    private transient java.util.ArrayList dataSetUpdateListenerList;
    
    public synchronized void addDataSetUpdateListener(org.das2.dataset.DataSetUpdateListener listener)
	 {
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
		 for(Object listener : list){
			 ((org.das2.dataset.DataSetUpdateListener) listener).dataSetUpdated(event);
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
            PropertyChangeEvent e= new PropertyChangeEvent( 
					this, "nave", new Integer(oldVal), new Integer(nave) );
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
            PropertyChangeEvent e= new PropertyChangeEvent( 
					this, "timeResolution", oldVal, this.xResolution );
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
            PropertyChangeEvent e= new PropertyChangeEvent( 
					this, "lowCutoff", oldVal, Boolean.valueOf(lowCutoff) );
            firePropertyChangeListenerPropertyChange( e );
            recalculateSoon();
            if ( this.cutoffSlicer != null ) this.cutoffSlicer.slopePlot.repaint();
        }
    }
    
    /**
     * Utility field used by event firing mechanism.
     */
    private javax.swing.event.EventListenerList listenerList =  null;
    
    /**
     * Registers PropertyChangeListener to receive events.
     * @param listener The listener to register.
     */
    public synchronized void addPropertyChangeListener(java.beans.PropertyChangeListener listener) {
        if (listenerList == null ) {
            listenerList = new javax.swing.event.EventListenerList();
        }
        listenerList.add(java.beans.PropertyChangeListener.class, listener);
    }
    
    /**
     * Removes PropertyChangeListener from the list of listeners.
     * @param listener The listener to remove.
     */
    public synchronized void removePropertyChangeListener(java.beans.PropertyChangeListener listener) {
        listenerList.remove(java.beans.PropertyChangeListener.class, listener);
    }
    
    /**
     * Notifies all registered listeners about the event.
     *
     * @param event The event to be fired
     */
    private void firePropertyChangeListenerPropertyChange(java.beans.PropertyChangeEvent event) {
        if (listenerList == null) return;
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i]==java.beans.PropertyChangeListener.class) {
                ((java.beans.PropertyChangeListener)listeners[i+1]).propertyChange(event);
            }
        }
    }
    
    
    private static void testCutoff() {
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
        int mult= -1; // low cutoff
        double level= -14;
        int icut= cutoff(
                new DefaultVectorDataSet( spec, Units.hertz, spec, Units.v2pm2Hz, new HashMap() ),
                Units.v2pm2Hz.createDatum(slope), nave, mult, Units.v2pm2Hz.createDatum(level), null );
        System.out.println("icut="+icut+"  should be 25");
    }

}
