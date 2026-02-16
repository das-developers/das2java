/*
 * GraphicalLogFormatter.java
 *
 * Created on December 8, 2005, 2:32 PM
 *
 *
 */

package org.das2.graph.util;
import org.das2.util.monitor.ProgressMonitor;
import org.das2.DasApplication;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import org.das2.event.BoxRenderer;
import org.das2.event.BoxSelectionEvent;
import org.das2.event.BoxSelectionListener;
import org.das2.event.BoxSelectorMouseModule;
import org.das2.event.LabelDragRenderer;
import org.das2.event.MouseModule;
import org.das2.graph.DasAxis;
import org.das2.graph.DasCanvas;
import org.das2.graph.DasColumn;
import org.das2.graph.DasPlot;
import org.das2.graph.DasRow;
import org.das2.graph.Legend;
import org.das2.graph.Renderer;
import org.das2.system.DasLogger;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import org.das2.util.DenseConsoleFormatter;
import org.das2.util.GrannyTextRenderer;
import org.das2.util.ObjectLocator;



/**
 * Class attempts to visualize messages sent to the loggers.
 * @author Jeremy
 */
public class GraphicalLogHandler extends Handler {
    
    List records= new ArrayList();
    List yAxisValuesThread= new ArrayList();
    List yAxisValuesClass= new ArrayList();
    List yAxisValuesLogger= new ArrayList();
    List times= new ArrayList();
    
    Renderer renderer;
    boolean updating= false;
    Thread updateThread;
    
    long time0;
    
    HashMap loggerMap= new HashMap();
    //HashMap yaxisMap= new HashMap();
    HashMap<String,Integer> yaxisMapThread=  new HashMap();
    HashMap<String,Integer> yaxisMapClass=  new HashMap();
    HashMap<String,Integer> yaxisMapLogger=  new HashMap();
    
    private static final int YAXIS_THREAD = -199;
    private static final int YAXIS_CLASS = -198;
    private static final int YAXIS_LOGNAME = -197;
    private int yaxisDimension = YAXIS_LOGNAME;
    
    DasAxis xaxis;
    Legend legend;
    
    JFrame frame;
    
    // this is to avoid initialization failures
    long sleepInitiallyTime= 2000; // milliseconds
    
    private GraphicalLogHandler() {
        time0= System.currentTimeMillis();
    }
    
    private static GraphicalLogHandler instance= null;
    
    public static GraphicalLogHandler getInstance() {
        if ( instance==null ) {
            instance= new GraphicalLogHandler();
        }
        return instance;
    }
    
    private void createCanvas() {
        if  ( loggerMap.isEmpty() ) {
            loggerMap.put( DasLogger.getLogger(DasLogger.APPLICATION_LOG).getName(), Color.black );
            loggerMap.put( DasLogger.getLogger(DasLogger.DATA_OPERATIONS_LOG).getName(), Color.blue );
            loggerMap.put( DasLogger.getLogger(DasLogger.DATA_TRANSFER_LOG).getName(), Color.YELLOW );
            loggerMap.put( DasLogger.getLogger(DasLogger.GRAPHICS_LOG ).getName(), Color.PINK );
            loggerMap.put( DasLogger.getLogger(DasLogger.SYSTEM_LOG ).getName(), Color.gray );
            loggerMap.put( DasLogger.getLogger(DasLogger.GUI_LOG ).getName(), Color.green );
            loggerMap.put( DasLogger.getLogger(DasLogger.DASML_LOG).getName(), Color.LIGHT_GRAY );
        }
        
        DasCanvas canvas= new DasCanvas(800,400);
        DasPlot plot= DasPlot.createPlot( new DatumRange( 0, 10, Units.seconds ) ,
                new DatumRange( 0, 10, Units.dimensionless ) );
        xaxis= plot.getXAxis();
        xaxis.setAnimated(false);
        
        renderer.setDataSetLoader(null);
        plot.addRenderer( renderer );
        
        canvas.add( plot, new DasRow( canvas, 0.1, 0.9 ), DasColumn.create(canvas) );
        
        MouseModule mm=  getMouseModule();
        plot.getDasMouseInputAdapter().addMouseModule( mm );
        plot.getDasMouseInputAdapter().setPrimaryModule( mm );
        
        mm= getShowLogMouseModule( plot );
        plot.getDasMouseInputAdapter().addMouseModule( mm );
        plot.getDasMouseInputAdapter().setSecondaryModule( mm );
        
        legend= new Legend();
        canvas.add( legend, new DasRow( canvas, 0.1, 0.5 ), new DasColumn( canvas, 0.65, 0.98 ) );
        
        for ( Iterator i= loggerMap.keySet().iterator(); i.hasNext(); ) {
            Object key= i.next();
            String name= String.valueOf(key);
            if ( name.equals("") ) name="<default>";
            legend.add( Legend.getIcon( (Color)loggerMap.get(key) ), name );
        }
        
        frame= DasApplication.getDefaultApplication().createMainFrame( "GraphicalLogHandler" );
        JPanel appPanel= new JPanel( new BorderLayout() );
        appPanel.add( canvas, BorderLayout.CENTER );
        
        JPanel controlPanel= new JPanel();
        controlPanel.setLayout( new BoxLayout( controlPanel, BoxLayout.X_AXIS ) );
        
        JCheckBox jcb= new JCheckBox( getUpdatingAction() );
        jcb.setSelected(updating);
        
        startUpdateThread();
        
        controlPanel.add( jcb );
        
        JButton x= new JButton( getUpdateAction() );
        controlPanel.add( x );
        
        appPanel.add( controlPanel, BorderLayout.SOUTH );
        
        
        
        frame.getContentPane().add( appPanel );
        frame.setVisible( true );
        frame.pack();
        frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
    }
    
    private Action getUpdatingAction() {
        return new AbstractAction( "Updating" ) {
            public void actionPerformed( ActionEvent e ) {
                JCheckBox source= (JCheckBox)e.getSource();
                updating= source.isSelected();
                if ( updating ) startUpdateThread();
            }
        };
    }
    
    private Action getUpdateAction() {
        return new AbstractAction( "Update" ) {
            public void actionPerformed( ActionEvent e ) {
                update();
            }
        };
    }
    
    public void setYAxisType( int type ) {
        this.yaxisDimension= type;
    }
    
    public int getYAxisType() {
        return this.yaxisDimension;
    }
    
    private void update() {
        long endMillis= System.currentTimeMillis() - time0 + 2000;
        if ( endMillis < 10000 ) endMillis= 10000;
        Datum end= Units.seconds.createDatum( endMillis/1000. );
        DatumRange range= new DatumRange( end.subtract( xaxis.getDatumRange().width() ), end );
        xaxis.setDatumRange( range );
    }
    
    private void startUpdateThread() {
        if ( updateThread==null ) {
            updateThread= new Thread( new Runnable() {
                public void run() {
                    while ( true ) {
                        try { Thread.sleep(500); } catch ( InterruptedException e ) { }
                        if ( updating ) update();
                    }
                }
            }, "graphicalHandlerUpdateThread" );
            updateThread.start();
        }
    }
    
    /**
     * check that the logger is not listening to itself.
     * @param st
     * @return 
     */
    private boolean checkMyMessages( StackTraceElement[] st ) {
        String myName= this.getClass().getName();
        boolean result= false;
        for ( int i=1; i<st.length; i++ ) {
            if ( st[i].getClassName().equals(myName) ) {
                result= true;
            }
            if ( st[i].getClassName().contains("DasLogger") ) result=true;
        }
        return result;
    }
    
    public void publish( LogRecord rec ) {
        StackTraceElement[] st= new Throwable().getStackTrace();
        
        if ( checkMyMessages(st) ) return;
        if ( Thread.currentThread().getName().equals( "graphicalHandlerUpdateThread" ) ) return;
        
        if ( DasApplication.getDefaultApplication().isHeadless() ) {
            // simply disable this when there is no head available.
            return;
        }
        
        if ( renderer==null &&
                ( System.currentTimeMillis() - this.time0 ) > sleepInitiallyTime ) getRenderer();
        
        String yAxisName;
        if ( yaxisDimension==YAXIS_THREAD ) {
            yAxisName= Thread.currentThread().getName() ;
        } else if ( yaxisDimension==YAXIS_CLASS ) {
            yAxisName= rec.getSourceClassName();
        } else if ( yaxisDimension==YAXIS_LOGNAME ) {
            yAxisName= rec.getLoggerName();
        } else {
            throw new IllegalArgumentException("bad yAxisName");
        }
        
        Integer yValue;
        yValue= yaxisMapClass.get( rec.getSourceClassName() );
        if ( yValue==null ) {
            yValue= yaxisMapClass.size();
            yaxisMapClass.put( yAxisName, yValue );
        }
        
        yValue= yaxisMapThread.get( rec.getSourceClassName() );
        if ( yValue==null ) {
            yValue= yaxisMapThread.size();
            yaxisMapThread.put( yAxisName, yValue );
        }
        
        yValue= yaxisMapLogger.get( rec.getLoggerName() );
        if ( yValue==null ) {
            yValue= yaxisMapLogger.size();
            yaxisMapLogger.put( yAxisName, yValue );
        }
        
        
        synchronized (this) {
            Long time= rec.getMillis() - time0;
            int index= Collections.binarySearch(times, time );
            if ( index<0 ) {
                index= -1-index;
            } else {
                int fudge=0;
                while ( index>=0 ) {
                    fudge++;
                    time= rec.getMillis() - time0 + fudge;
                    index= Collections.binarySearch(times, time );
                }
                index= -1-index;
            }
            records.add( index, rec );
            yAxisValuesClass.add( index, yaxisMapClass.get( rec.getSourceClassName() ) );
            yAxisValuesThread.add( index, yaxisMapThread.get( rec.getSourceClassName() ) );
            yAxisValuesLogger.add( index, yaxisMapLogger.get( rec.getLoggerName() ) );
            
            times.add( index, time );
            
        }
        
        // consider how to not record it's own messages
    }
    
    public void flush() {
        if ( renderer==null ) getRenderer();
        renderer.update();
    }
    
    public void close() {
    }
    
    private String formatMessage(LogRecord r) {
        try {
            Formatter f = getFormatter();
            // If you later swap in a formatter that applies parameters, this will honor it.
            String s = (f != null) ? f.formatMessage(r) : r.getMessage();
            return s == null ? "" : s;
        } catch (Exception e) {
            // Fall back to raw message
            String s = r.getMessage();
            return s == null ? "" : s;
        }
    }
    
    ObjectLocator objectLocator;
    private synchronized void setObjectLocator( ObjectLocator o ) {
        this.objectLocator= o;
    }

    private synchronized ObjectLocator getObjectLocator() {
        return this.objectLocator;
    }
    
    public class LogRenderer extends Renderer {
        String searchRegex="";
        
        public String getSearchRegex() {
            return searchRegex;
        }
        public void setSearchRegex( String regex ) {
            this.searchRegex= regex;
            super.update();
        }
        
        public void render(Graphics2D g1, DasAxis xAxis, DasAxis yAxis ) {
            
            Graphics2D g= (Graphics2D)g1;
            g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
            
            int ix0= (int) xAxis.transform( xAxis.getDataMinimum() );
            g.setColor( Color.lightGray );
            
            HashMap<String,Integer> yaxisMap;
            List<Integer> yAxisValues;
            if ( yaxisDimension==YAXIS_CLASS ) {
                yaxisMap= yaxisMapClass;
                yAxisValues= yAxisValuesClass;
            } else if ( yaxisDimension==YAXIS_THREAD ) {
                yaxisMap= yaxisMapThread;
                yAxisValues= yAxisValuesThread;
            } else {
                yaxisMap= yaxisMapLogger;
                yAxisValues= yAxisValuesLogger;
            }
            
            for ( Entry<String,Integer> e: yaxisMap.entrySet() ) {
                Object name= e.getKey();
                Integer ithread= e.getValue();
                int iy= (int)yAxis.transform( Units.dimensionless.createDatum(ithread.intValue()) );
                g.drawString( String.valueOf(name), ix0+2, iy );
            }
            
            ObjectLocator objectLocator= new ObjectLocator();

            long minMilli= (long)xAxis.getDataMinimum().doubleValue( Units.milliseconds );
            long maxMilli= (long)xAxis.getDataMaximum().doubleValue( Units.milliseconds );

            int firstIndex= Collections.binarySearch( times, Long.valueOf( minMilli ) );
            if ( firstIndex<0 ) firstIndex= -1 - firstIndex;
            int lastIndex= Collections.binarySearch( times, Long.valueOf( maxMilli ) );
            if ( lastIndex<0 ) {
                lastIndex= -1 - lastIndex;
            } else {
                lastIndex++;
            }

            int lastX=-999;
            int lastY=-999;
            int collisionCount=0;

            DasPlot parent= getParent();
            if ( !searchRegex.equals("") ) {
                for ( int i=firstIndex; i<lastIndex; i++ ) {
                    LogRecord record= (LogRecord) records.get(i);
                    if ( record.getMessage().matches( searchRegex ) ) {
                        int ix= (int)xAxis.transform( Units.milliseconds.createDatum( ((Long)times.get(i)).longValue() ) );
                        g.setColor( Color.lightGray );
                        g.fillRect( ix-2, parent.getY(), 5, parent.getHeight() );
                        objectLocator.addObject( new Rectangle( ix-2, parent.getY(), 5, parent.getHeight() ),
                                record );
                    }
                }
            }

            for ( int i=firstIndex; i<lastIndex; i++ ) {

                LogRecord record= (LogRecord) records.get(i);

                int ithread= yAxisValues.get(i);

                int iy= (int)yAxis.transform( Units.dimensionless.createDatum(ithread) );
                int ix= (int)xAxis.transform( Units.milliseconds.createDatum( ((Long)times.get(i)).longValue() ) );

                if ( ix==lastX && iy==lastY ) {
                    collisionCount++;
                } else {
                    lastX= ix;
                    lastY= iy;
                    collisionCount=0;
                }

                if ( !searchRegex.isEmpty() ) {
                    if ( record.getMessage().matches( searchRegex ) ) {
                        g.setColor( Color.lightGray );
                        g.fillRect( ix-2, 0, 5, 100 );
                    }
                }

                Color color= (Color)loggerMap.get( record.getLoggerName() );
                if ( color==null ) {
                    Object key= record.getLoggerName();
                    loggerMap.put( key, Color.ORANGE );
                    legend.add( Legend.getIcon( (Color)loggerMap.get(key) ), String.valueOf( key ) );
                    legend.repaint();
                }
                g.setColor( color );

                int height= record.getLevel().intValue() / 100;
                g.fillRect( ix-2, iy-height-2*collisionCount, 5, height );
                objectLocator.addObject( new Rectangle( ix-2, iy-height-2*collisionCount, 5, height ), record );
            }
            
            setObjectLocator( objectLocator );
        }
        
    }
    
    Renderer getRenderer() {
        if ( renderer==null ) {
            renderer= new LogRenderer();
            createCanvas();
        }
        return renderer;
    }
    
    private class LookupDragRenderer extends LabelDragRenderer {
        private static final String LABEL_NOT_AVAILABLE = "n/a";
        DasAxis xaxis, yaxis;
        
        LookupDragRenderer( DasPlot parent ) {
            super( parent );
            this.xaxis= parent.getXAxis();
            this.yaxis= parent.getYAxis();
        }
        
        @Override
        public Rectangle[] renderDrag( Graphics g, Point p1, Point p2 ) {
            
            LogRecord select= (LogRecord)getObjectLocator().closestObject( p2 );
            int iclosest= records.indexOf( select );
            
            String label;
            Rectangle[] myDirtyBounds;
            
            List yAxisValues;
            switch (yaxisDimension) {
                case YAXIS_CLASS:
                    yAxisValues= yAxisValuesClass;
                    break;
                case YAXIS_THREAD:
                    yAxisValues= yAxisValuesThread;
                    break;
                default:
                    yAxisValues= yAxisValuesLogger;
                    break;
            }
            
            if ( select==null ) {
                label= LABEL_NOT_AVAILABLE;
                myDirtyBounds= new Rectangle[] { new Rectangle( 0,0,0,0 ), new Rectangle( 0,0,0,0 ) };
                
            } else {
                String message= formatMessage(select);
                
                label= select.getLoggerName()+":"+select.getLevel()+":!c"+message;
                
                int ix= (int)xaxis.transform( Units.milliseconds.createDatum( ((Long)times.get(iclosest)).longValue() ) );
                int iy= (int)yaxis.transform( Units.dimensionless.createDatum( ((Integer)yAxisValues.get(iclosest)).intValue() ) );
                g.drawOval( ix-5,  iy-5, 10, 10 );
                GrannyTextRenderer gtr= new GrannyTextRenderer();
                gtr.setString(g, label);
                gtr.draw( g, 5, g.getFontMetrics().getHeight() );
                Rectangle gtrBounds= gtr.getBounds();
                gtrBounds.translate(5,g.getFontMetrics().getHeight());
                myDirtyBounds= new Rectangle[] {
                    new Rectangle( ix-5,  iy-5, 11, 11 ),
                            gtrBounds };
            }
            
            super.setLabel(label);
            Rectangle[] dirtyBounds= super.renderDrag( g, p1, p2 );
            if ( dirtyBounds.length > 0 ) {
                return new Rectangle[] { dirtyBounds[0], myDirtyBounds[0], myDirtyBounds[1] } ;
            } else {
                return new Rectangle[] { myDirtyBounds[0], myDirtyBounds[1] } ;
            }
        }
        
    }
    
    
    public MouseModule getMouseModule( ) {
        DasPlot parent= renderer.getParent();
        LabelDragRenderer dr= new LookupDragRenderer( parent );
        MouseModule mouseModule= new MouseModule( parent, dr, "DataSetMonitor" );
        return mouseModule;
    }
    
    public MouseModule getShowLogMouseModule( DasPlot plot2 ) {
        BoxSelectorMouseModule result= new BoxSelectorMouseModule( plot2, plot2.getXAxis(), plot2.getYAxis(),
                plot2.getRenderer(0), new BoxRenderer( plot2 ), "View Messages" );
        result.setDragEvents( false );
        result.setReleaseEvents( true );
        result.addBoxSelectionListener( new BoxSelectionListener() {
            public void boxSelected( BoxSelectionEvent e ) {
                StringBuilder buf= new StringBuilder(1000);
                
                //Handler h= new ConsoleHandler();
                //Formatter f= h.getFormatter();
                Formatter f= new DenseConsoleFormatter();
                
                DatumRange threadsRange= e.getYRange();
                DatumRange timeRange= e.getXRange();

                List yAxisValues;
                if ( yaxisDimension==YAXIS_CLASS ) {
                    yAxisValues= yAxisValuesClass;
                } else if ( yaxisDimension==YAXIS_THREAD ) {
                    yAxisValues= yAxisValuesThread;
                } else {
                    yAxisValues= yAxisValuesLogger;
                }
            
                int messageCount=0;
                for ( int i=0; i<records.size(); i++ ) {
                    double time= ((Long)times.get( i )).doubleValue();
                    if ( timeRange.contains( Units.milliseconds.createDatum( time ) ) ) {
                        if ( threadsRange.contains( Units.dimensionless.createDatum( (Number)yAxisValues.get(i) ) ) ) {
                            buf.append( f.format( (LogRecord)records.get(i) ) );
                            messageCount++;
                        }
                    }
                }
                
                JDialog dialog= new JDialog( frame, "Log messages" );
                JTextArea pane= new JTextArea( );
                pane.insert( buf.toString(), 0 );
                pane.insert( ""+messageCount+" messages: \n\n", 0 );
                
                JScrollPane spane= new JScrollPane( pane );
                spane.setPreferredSize( new Dimension( 800, 600 ) );
                dialog.getContentPane().add( spane );
                dialog.pack();
                dialog.setVisible(true);
            }
        } );
        return result;
    }
    
    public static void main( String[] args ) {
        
        GraphicalLogHandler p= new GraphicalLogHandler();
                
        DasLogger.getLogger( DasLogger.DATA_TRANSFER_LOG ).addHandler(p);
        // set up your logging to use this.
        DasLogger.getLogger( DasLogger.DATA_TRANSFER_LOG ).warning("warning");
        DasLogger.getLogger( DasLogger.DATA_TRANSFER_LOG ).info("info");
        
        JButton b= new JButton( new AbstractAction("pushme") {
            public void actionPerformed( ActionEvent e ) {
                 DasLogger.getLogger( DasLogger.GRAPHICS_LOG ).warning("*giggle*");
            }
        });
        
        JFrame f= new JFrame();
        f.getContentPane().add( b );
        f.pack();
        f.setVisible(true);
        
        
    }
    
}