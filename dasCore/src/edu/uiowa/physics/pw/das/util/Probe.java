/*
 * Probe.java
 *
 * Created on September 22, 2004, 1:21 PM
 */

package edu.uiowa.physics.pw.das.util;

import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.event.*;
import edu.uiowa.physics.pw.das.graph.*;
import java.util.*;
import javax.swing.*;

/**
 *
 * @author  Jeremy
 */
public class Probe {
    
    final long refreshRateMillis=1000;
    
    HashMap histograms;
    
    HashMap agents;
    
    //DasPlot plot;
    Legend legend;
    Leveler leveler;
    DasCanvas canvas;
    
    DatumRange xrange;
    DatumRange yrange;
    
    DasColumn column;
    DasAxis xAxis;
    
    boolean isWidgetCreated;
    JFrame frame;
    
    String title;
    
    long t0millis;
    boolean needUpdate=false;
    boolean updating=true;
    
    /* agent has responsibility of conveying info */
    private class Agent {
        VectorDataSetBuilder builder;
        SymbolLineRenderer renderer;
        DasPlot plot;
        DatumRange xrange, yrange;
        
        int hits;
        
        Agent( String name, int index ) {
            builder= new VectorDataSetBuilder(Units.dimensionless,Units.dimensionless);
            renderer= new SymbolLineRenderer((DataSet)null);
            
            xrange= new DatumRange( Datum.create(0), Datum.create(1) );
            yrange= DatumRangeUtil.newDimensionless(0,0.000001);
            
            DasAxis thisXAxis;
            if ( index==0 ) {
                xAxis= new DasAxis( xrange, DasAxis.HORIZONTAL);
                thisXAxis= xAxis;
            } else {
                thisXAxis= xAxis.createAttachedAxis();
                thisXAxis.setTickLabelsVisible(false);
            }
            
            //plot= edu.uiowa.physics.pw.das.graph.Util.newDasPlot(canvas, xrange, yrange );
            plot= new DasPlot( thisXAxis, new DasAxis( yrange, DasAxis.VERTICAL ) );
            
            renderer.setPsym(Psym.CIRCLES);
            // renderer.setPsymConnector(PsymConnector.NONE);
            renderer.setSymSize(2.0);
            final SymColor[] symColors= { SymColor.black, SymColor.blue, SymColor.lightRed, SymColor.red, SymColor.darkGreen, SymColor.gray };
            renderer.setColor(symColors[index%symColors.length]);
            
            plot.getXAxis().setAnimated(true);
            plot.getYAxis().setAnimated(true);
            plot.getYAxis().setLabel(name);
            plot.addRenderer(renderer);
            
            DasRow row= leveler.getRow();
            
            canvas.add( plot, row, column );
            
            hits= 0;
        }
        
        void add( double value ) {
            //double seconds= ( System.currentTimeMillis() - t0millis ) / 1000.;
            double xvalue= ++hits;
            builder.insertY(xvalue,value);
            
            
            if ( !xrange.contains( Units.dimensionless.createDatum(xvalue)) ) {
                xrange= include( xrange, Units.dimensionless.createDatum(xvalue)) ;
            }
            if ( !yrange.contains( Units.dimensionless.createDatum(value) ) ) {
                yrange= include( yrange, Units.dimensionless.createDatum(value) );
            }
            
            
            needUpdate=true;
        }
        
        void update() {
            DataSet ds= builder.toVectorDataSet();
            plot.getXAxis().setDataRange( xrange );
            plot.getYAxis().setDataRange( yrange );
            renderer.setDataSet(ds);
        }
        
        void destroy() {
            if ( isWidgetCreated ) {
                plot.removeRenderer(renderer);
            }
        }
    }
    
    public void reset() {
        if ( agents==null ) agents= new HashMap();
        
        for ( Iterator i=agents.keySet().iterator(); i.hasNext(); ) {
            Object key= i.next();
            Agent a= (Agent)agents.get(key);
            a.destroy();
        }
        
        agents= new HashMap();
        
        t0millis= System.currentTimeMillis();
        needUpdate= true;
    }
    
    private DatumRange include( DatumRange dr, Datum d ) {
        if ( dr.contains(d) ) {
            return dr;
        } else if ( d.lt(dr.min()) ) {
            while ( !dr.contains(d) ) dr= dr.rescale(-1,1);
        } else {
            while ( !dr.contains(d) ) dr= dr.rescale(0,2);
        }
        return dr;
    }
    
   /* public synchronized void add( String name, double value, String name2, double value2 ) {
        // this doesn't work.
        VectorDataSetBuilder builder;
        edu.uiowa.physics.pw.das.graph.CurveRenderer renderer;
        if ( ! builders.containsKey(name) ) {
            maybeCreateWidget();
            builder= new VectorDataSetBuilder(Units.seconds,Units.dimensionless);
            builder.addPlane( name2, Units.dimensionless );
            builders.put(name, builder);
            renderer= new CurveRenderer( null, "", name2 );
    
            renderers.put( name, renderer );
    
            plot.addRenderer(renderer);
    
        } else {
            builder= (VectorDataSetBuilder)builders.get(name);
        }
    
        //long c= ((Long)currentCount.get(name)).longValue();
        double seconds= ( System.currentTimeMillis() - t0millis ) / 1000.;
        builder.insertY(seconds,value);
        builder.insertY( seconds, value2, name2 );
        //currentCount.put(name,new Long(++c));
    
        if ( !xrange.contains( Units.seconds.createDatum(seconds)) ) {
            xrange= include( xrange, Units.seconds.createDatum(seconds)) ;
            plot.getXAxis().setDataRange( xrange );
        }
        if ( !yrange.contains( Units.dimensionless.createDatum(value) ) ) {
            yrange= include( yrange, Units.dimensionless.createDatum(value) );
            plot.getYAxis().setDataRange( yrange );
        }
    
        needUpdate=true;
    }*/
    
  /*  public synchronized void addHistogram( String name, double value ) {
        int[] histogram;
        if ( !histograms.containsKey(name) ) {
            maybeCreateWidget();
            histogram= new int[50];
            histograms.put(name,histogram);
            edu.uiowa.physics.pw.das.graph.SymbolLineRenderer renderer;
            renderer= new SymbolLineRenderer((DataSet)null);
            renderer.setHistogram(true);
            renderer.setPsym( Psym.NONE );
            renderers.put( name, renderer );
            plot.addRenderer(renderer);
            legend.add( renderer, name );
        } else {
            histogram= (int[])histograms.get(name);
        }
        int ivalue= (int)value;
        if ( ivalue < 0 ) ivalue=0;
   
        if ( (int)value > histogram.length ) {
            int[] histNew= new int[ 2*(int)value ];
            System.arraycopy(histogram, 0, histNew, 0, histogram.length );
            histogram= histNew;
            histograms.put( name, histogram );
        }
        histogram[ivalue]++;
   
        if ( !xrange.contains( Units.seconds.createDatum(histogram.length)) ) {
            xrange= include( xrange, Units.seconds.createDatum(histogram.length)) ;
            plot.getXAxis().setDataRange( xrange );
        }
        if ( !yrange.contains( Units.dimensionless.createDatum(histogram[ivalue]) ) ) {
            yrange= include( yrange, Units.dimensionless.createDatum(histogram[ivalue]) );
            plot.getYAxis().setDataRange( yrange );
        }
   
        needUpdate= true;
   
    } */
    
    public synchronized void add( String name, double value ) {
        VectorDataSetBuilder builder;
        edu.uiowa.physics.pw.das.graph.SymbolLineRenderer renderer;
        Agent a;
        if ( ! agents.containsKey(name) ) {
            maybeCreateWidget();
            a= new Agent( name, agents.size() );
            agents.put( name, a );
        } else {
            a= (Agent)agents.get(name);
        }
        
        a.add(value);
        
        
    }
    
    private void startUpdateThread() {
        if ( updating ) {
            new Thread( new Runnable() {
                public void run() {
                    try { Thread.sleep(refreshRateMillis); } catch ( InterruptedException e ) { throw new RuntimeException(e); }
                    update();
                }
            }, "probeUpdateThread" ).start();
        }
    }
    
    private synchronized void update() {
        if ( isWidgetCreated && needUpdate ) {
            for (Iterator i=agents.keySet().iterator(); i.hasNext(); ) {
                Object name= i.next();
                Agent a= (Agent) agents.get(name);
                a.update();
            }
            /*for ( Iterator i=histograms.keySet().iterator(); i.hasNext(); ) {
                Object name= i.next();
                int[] histogram= (int[])histograms.get(name);
                edu.uiowa.physics.pw.das.graph.Renderer renderer= (edu.uiowa.physics.pw.das.graph.Renderer)renderers.get(name);
                VectorDataSetBuilder builder= new VectorDataSetBuilder( Units.seconds, Units.dimensionless ); // seconds is a kludge
                for ( int j=0; j<histogram.length; j++ ) {
                    builder.insertY(j,histogram[j]);
                }
                renderer.setDataSet(builder.toVectorDataSet());
            }*/
        }
        needUpdate= false;
        if ( isWidgetCreated ) startUpdateThread();
    }
    
    private void maybeCreateWidget() {
        if ( isWidgetCreated ) {
            return;
        } else {
            
            frame= new JFrame( title );
            
            canvas= new DasCanvas(300,300);
            column= new DasColumn( canvas, 0.15, 0.9 );
            
            frame.setContentPane(canvas);
            
            frame.setVisible(true);
            frame.pack();
            
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            //xrange= new DatumRange( Datum.create(0), Datum.create(1) );
            //yrange= DatumRangeUtil.newDimensionless(0,0.000001);
            
            //plot= edu.uiowa.physics.pw.das.graph.Util.newDasPlot(canvas, xrange, yrange );
            //            MouseModule mm=  new MouseModule( plot, new PointSlopeDragRenderer( plot, plot.getXAxis(),  plot.getYAxis() ), "Point,Slope" );
            //            plot.addMouseModule( mm );
            
            //            plot.addMouseModule( new BoxRangeSelectorMouseModule( plot, plot.getXAxis(),  plot.getYAxis() ) );
            //            plot.getMouseAdapter().setPrimaryModule(mm);
            
            //            plot.getMouseAdapter().addMouseModule( new MouseModule( plot, new LengthDragRenderer( plot, plot.getXAxis(),  plot.getYAxis() ), "Length" ) );
            
            //            plot.getXAxis().setAnimated(true);
            //            plot.getYAxis().setAnimated(true);
            
            //  legend= new Legend();
            // no attachedRow method!
            //  canvas.add( legend, plot.getRow().createSubRow(0.95,0.80), plot.getColumn().createAttachedColumn( 0.5, 0.8 ) );
            leveler= new Leveler(canvas);
            isWidgetCreated= true;
            startUpdateThread();
        }
        
    }
    
    public void setUpdating( boolean updating ) {
        this.updating= updating;
        if ( isWidgetCreated && updating ) {
            startUpdateThread();
        }
    }
    
    private Probe( String title, boolean active ) {
        this.title= title;
        this.isWidgetCreated= false;
        this.updating= active;
        reset();
    }
    
    public static Probe newProbe( String title ) {
        return new Probe(title,true);
    }
    
    public static Probe nullProbe( String title ) {
        return new Probe(title,false);
    }
    
    public static void main(String[] args) throws Exception {
        Probe p= new Probe("",true);
        Thread.sleep(2000);
        for ( int i=0; i<100; i++ ) {
            Thread.sleep(200);
            p.add("i", i);
            p.add("j", i % 10 );
        }
    }
    
}
