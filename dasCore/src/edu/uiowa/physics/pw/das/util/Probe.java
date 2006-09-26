/*
 * Probe.java
 *
 * Created on September 22, 2004, 1:21 PM
 */

package edu.uiowa.physics.pw.das.util;

import edu.uiowa.physics.pw.das.*;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.event.*;
import edu.uiowa.physics.pw.das.graph.*;
import edu.uiowa.physics.pw.das.system.DasLogger;
import java.awt.*;
import java.awt.event.*;
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
    
    boolean ignoring=false;
    String triggerName=null;
    double triggerMin;
    double triggerMax;
    
    //DasPlot plot;
    Legend legend;
    Leveler leveler;
    DasCanvas canvas;
    DasAnnotation titleAnnotation;
    
    DatumRange xrange;
    DatumRange yrange;
    
    DasColumn column;
    DasAxis xAxis;
    
    boolean isWidgetCreated;
    JFrame frame;
    
    String title;
    
    boolean isNull;
    
    long t0millis;
    boolean needUpdate=false;
    boolean updating=true;
    
    /**
     * Holds value of property xsize.
     */
    private int xsize;
    
    /**
     * Holds value of property ysize.
     */
    private int ysize;
    
    /* agent has responsibility of conveying info */
    private class Agent {
        VectorDataSetBuilder builder;
        SymbolLineRenderer renderer;
        DasPlot plot;
        DatumRange xrange, yrange;
        boolean yrangeSet= false;
        boolean histogram= false;
        
        int hits;
        Legend legend;
        String name;
        
        Agent( String name, int index, Agent underplotAgent ) {
            builder= new VectorDataSetBuilder(Units.dimensionless,Units.dimensionless);
            renderer= new SymbolLineRenderer((DataSet)null);
            this.name= name;
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
            
            if ( underplotAgent==null ) {
                //plot= edu.uiowa.physics.pw.das.graph.Util.newDasPlot(canvas, xrange, yrange );
                plot= new DasPlot( thisXAxis, new DasAxis( yrange, DasAxis.VERTICAL ) );
                plot.addMouseModule( new DumpToFileMouseModule( plot, renderer, thisXAxis, plot.getYAxis() ) );
            } else {
                plot= underplotAgent.plot;
            }
            
            renderer.setPsym(Psym.NONE);
            // renderer.setPsymConnector(PsymConnector.NONE);
            //renderer.setSymSize(2.0);
            
            final SymColor[] symColors= { SymColor.black, SymColor.blue, SymColor.lightRed, SymColor.red, SymColor.darkGreen, SymColor.gray };
            renderer.setColor(symColors[index%symColors.length]);
            
            plot.getXAxis().setAnimated(true);
            plot.getYAxis().setAnimated(true);
            plot.getYAxis().setLabel(name);
            plot.addRenderer(renderer);
            
            if ( underplotAgent==null ) {
                DasRow row= leveler.addRow(0.);
                canvas.add( plot, row, column );                
            } else {
                underplotAgent.addToLegend(this);
            }
            
            hits= 0;
        }
        
        void addToLegend( Agent overplotAgent ) {
            if ( legend==null ) {
                DasCanvas c= plot.getCanvas();
                legend= new Legend();
                legend.add( renderer, name );
                c.add( legend, plot.getRow().createAttachedRow(0.2,0.95), plot.getColumn().createAttachedColumn(0.7,0.99) );
            }
            legend.add( overplotAgent.renderer, overplotAgent.name );
        }
        
        void add( double value ) {
            double xvalue= ++hits;
            synchronized( builder ) {
                builder.insertY(xvalue,value);
            }
            if ( !xrange.contains( Units.dimensionless.createDatum(xvalue)) ) {
                xrange= include( xrange, Units.dimensionless.createDatum(xvalue)) ;
                if ( !xrange.width().isFinite() ) {
                    throw new IllegalStateException();
                }
            }

            if ( !yrangeSet ) {
                yrange= new DatumRange( value, value+1e-7, Units.dimensionless );
                yrangeSet= true;
            }
            if ( !yrange.contains( Units.dimensionless.createDatum(value) ) ) {
                yrange= include( yrange, Units.dimensionless.createDatum(value) );
                if ( !yrange.width().isFinite() ) {
                    throw new IllegalStateException();
                }
            }            
            
            needUpdate=true;
        }
        
        void add( int value ) {
            histogram= true;
            this.add((float)value);
        }
        
        void update() {
            DataSet ds;
            synchronized( builder ) {
                ds= builder.toVectorDataSet();
            }
            plot.getXAxis().setDatumRange( xrange );
            plot.getYAxis().setDatumRange( yrange );
            renderer.setHistogram(histogram);
            renderer.setDataSet(ds);
            
        }
        
        void destroy() {
            if ( isWidgetCreated ) {
                plot.removeRenderer(renderer);
            }
        }
        
    }
    
    public DasCanvas getCanvas() {
        if ( isNull ) throw new IllegalArgumentException("getCanvas called for null canvas");
        return this.canvas;
    }
    
    public void reset() {
        if ( isNull ) return;
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
        } 
        
        Datum w= dr.width();
        if ( w.doubleValue( w.getUnits() ) == 0. ) {
            dr= dr.include(d);
            return dr;
        } else if ( d.lt(dr.min()) ) {
            while ( !dr.contains(d) ) dr= dr.rescale(-1,1);
        } else {
            while ( !dr.contains(d) ) dr= dr.rescale(0,2);
        }
        DasLogger.getLogger( DasLogger.SYSTEM_LOG ).info( "dr="+dr );
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
    
    private void checkTrigger( String name, double value ) {
        if ( this.triggerName==null ) return;
        if ( this.triggerName.equals(name) ) {
            boolean notIgnoring= this.triggerMin < value && value < this.triggerMax;
            ignoring= !notIgnoring;
        }
    }
        
    public boolean isTriggered() {
        return !ignoring;
    }
    
    public synchronized void add( String name, int value ) {
        if ( isNull ) return;
        checkTrigger( name, value );
        if ( ignoring ) return;
        if ( Double.isInfinite(value) ) {
            throw new IllegalStateException("value is not finite: "+name);
        }
        VectorDataSetBuilder builder;
        edu.uiowa.physics.pw.das.graph.SymbolLineRenderer renderer;
        Agent a;
        if ( ! agents.containsKey(name) ) {
            maybeCreateWidget();
            a= new Agent( name, agents.size(), null );
            agents.put( name, a );
        } else {
            a= (Agent)agents.get(name);
        }
        
        a.add(value);
        
    }
    
    public synchronized void add( String name, double value ) {
        if ( isNull ) return;
        checkTrigger( name, value );
        if ( ignoring ) return;
        if ( Double.isInfinite(value) ) {
            throw new IllegalStateException("value is not finite: "+name);
        }        
        VectorDataSetBuilder builder;
        Agent a;
        if ( ! agents.containsKey(name) ) {
            maybeCreateWidget();
            a= new Agent( name, agents.size(), null );
            agents.put( name, a );
        } else {
            a= (Agent)agents.get(name);
        }
        
        a.add(value);
    }
    
    public synchronized void addOverplot( String overplotName, String underplotName, double value ) {
        if ( isNull ) return;
        checkTrigger( overplotName, value );
        if ( ignoring ) return;
        if ( Double.isInfinite(value) ) {
            throw new IllegalStateException("value is not finite: "+overplotName);
        }
        VectorDataSetBuilder builder;
        Agent a;
        if ( ! agents.containsKey(overplotName) ) {
            maybeCreateWidget();
            Agent underplotAgent= (Agent)agents.get(underplotName);
            if ( underplotAgent==null ) {
                a= new Agent( overplotName, agents.size(), null );
            } else {
                a= new Agent( overplotName, agents.size(), underplotAgent );
            }
            agents.put( overplotName, a );
        } else {
            a= (Agent)agents.get(overplotName);
        }
        
        a.add(value);
    }
    
    public synchronized void addOverplot( String overplotName, String underplotName, int value ) {
        if ( isNull ) return;
        checkTrigger( overplotName, value );
        if ( ignoring ) return;        
        VectorDataSetBuilder builder;
        Agent a;
        if ( ! agents.containsKey(overplotName) ) {
            maybeCreateWidget();
            Agent underplotAgent= (Agent)agents.get(underplotName);
            if ( underplotAgent==null ) {
                a= new Agent( overplotName, agents.size(), null );
            } else {
                a= new Agent( overplotName, agents.size(), underplotAgent );
            }
            agents.put( overplotName, a );
        } else {
            a= (Agent)agents.get(overplotName);
        }
        
        a.add(value);
    }
    
    public void setTrigger( String name, double min, double max ) {
        if ( name!=this.triggerName || min!=this.triggerMin || max!=this.triggerMax ) {
            this.triggerName= name;
            this.triggerMin= min;
            this.triggerMax= max; 
            this.ignoring= true;
        }
    }
    
    private void startUpdateThread() {
        if ( updating ) {
            new Thread( new Runnable() {
                public void run() {
                    while ( true ) {
                        try { Thread.sleep(refreshRateMillis); } catch ( InterruptedException e ) { throw new RuntimeException(e); }
                        if ( updating & isWidgetCreated ) update();
                    }
                }
            }, "probeUpdateThread" ).start();
        }
    }
    
    public synchronized void update() {
        if ( isNull ) return;
        if ( isWidgetCreated && needUpdate ) {
            for (Iterator i=agents.keySet().iterator(); i.hasNext(); ) {
                Object name= i.next();
                Agent a= (Agent) agents.get(name);
                a.update();
            }
        }
        needUpdate= false;
    }
    
    public synchronized void pause() {
        updating= false;
    }
    
    private Action getUpdateAction() {
        return new AbstractAction("Update") {
            public void actionPerformed( ActionEvent e ) {
                update();
            }
        };
    }

    private Action getPauseAction() {
        return new AbstractAction("Pause") {
            public void actionPerformed( ActionEvent e ) {
                pause();
            }
        };
    }
    
    private void maybeCreateWidget() {
        if ( isWidgetCreated ) {
            return;
        } else {
            
            frame= DasApplication.getDefaultApplication().createMainFrame();
            JPanel panel= new JPanel();
            panel.setLayout( new BorderLayout());
            
            canvas= new DasCanvas(xsize,ysize);
            
            panel.add(canvas, BorderLayout.CENTER );
            
            Box box= Box.createHorizontalBox();
            box.add( new JButton( getUpdateAction() ) );
            box.add( new JButton( getPauseAction() ) );
            
            panel.add( box, BorderLayout.NORTH );
            
            column= new DasColumn( canvas, 0.15, 0.9 );
            
            frame.setContentPane(panel);
            
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
            titleAnnotation= new DasAnnotation( title );
            canvas.add( titleAnnotation, new DasRow(canvas,0.,0.05), new DasColumn( canvas, 0., 1. ) );
            canvas.revalidate();
            
            isWidgetCreated= true;
            startUpdateThread();
        }
        
    }
    
    public void setUpdating( boolean updating ) {
        this.updating= updating;
    }
    
    private Probe( String title, boolean notNull, int xsize, int ysize ) {
        this.title= title;
        this.isWidgetCreated= false;
        this.updating= notNull;
        this.isNull= !notNull;
        this.xsize= xsize;
        this.ysize= ysize;
        reset();
    }
    
    public static Probe newProbe( String title ) {
        return new Probe(title,true, 400, 600 );
    }
    
    public static Probe newProbe( int xsize, int ysize ) {
        return new Probe( "Probe", true, xsize, ysize );
    }
    
    public static Probe nullProbe() {
        return new Probe("",false, 400,400 );
    }
    
    public static void main(String[] args) throws Exception {
        Probe p= new Probe("Test of Probe with fake data",true,400,600);
        Thread.sleep(500);
        for ( int i=0; i<500; i++ ) {
            Thread.sleep(200);
            p.add("i", i);
            p.add("j", i % 10 );
        }
    }
    
    public int getXsize() {
        return this.xsize;
    }
    
    public void setXsize(int xsize) {
        this.xsize = xsize;
    }
    
    public int getYsize() {
        return this.ysize;
    }
    
    public void setYsize(int ysize) {
        this.ysize = ysize;
    }
    
    public String getTitle() {
        return this.title;
    }
    
    public void setTitle(String title) {
        this.title = title;
        if ( titleAnnotation!=null ) titleAnnotation.setText(title);
    }
    
}
