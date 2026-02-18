
package org.das2.graph.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import org.das2.datum.DatumRange;
import org.das2.datum.EnumerationUnits;
import org.das2.datum.Units;
import org.das2.event.BoxRenderer;
import org.das2.event.BoxSelectionEvent;
import org.das2.event.BoxSelectorMouseModule;
import org.das2.event.LabelDragRenderer;
import org.das2.event.MouseModule;
import org.das2.graph.DasAxis;
import org.das2.graph.DasPlot;
import org.das2.graph.Legend;
import org.das2.graph.Renderer;
import org.das2.qds.QDataSet;
import org.das2.qds.ops.Ops;
import org.das2.qds.util.DataSetBuilder;
import org.das2.system.DasLogger;
import org.das2.util.DenseConsoleFormatter;
import org.das2.util.GrannyTextRenderer;
import org.das2.util.ObjectLocator;

/**
 *
 * @author jbf
 */
public class GraphicalLogRenderer {
    
    QDataSet records= null;
    List yAxisValuesThread= new ArrayList();
    List yAxisValuesClass= new ArrayList();
    List yAxisValuesLogger= new ArrayList();
    List times= new ArrayList();
    
    LogRenderer renderer;
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
    
    private GraphicalLogRenderer() {
        time0= System.currentTimeMillis();
    }
    
    private static GraphicalLogRenderer instance= null;
    
    public static GraphicalLogRenderer getInstance() {
        if ( instance==null ) {
            instance= new GraphicalLogRenderer();
        }
        return instance;
    }
    
//    private void createCanvas() {
//        if  ( loggerMap.isEmpty() ) {
//            loggerMap.put( DasLogger.getLogger(DasLogger.APPLICATION_LOG).getName(), Color.black );
//            loggerMap.put( DasLogger.getLogger(DasLogger.DATA_OPERATIONS_LOG).getName(), Color.blue );
//            loggerMap.put( DasLogger.getLogger(DasLogger.DATA_TRANSFER_LOG).getName(), Color.YELLOW );
//            loggerMap.put( DasLogger.getLogger(DasLogger.GRAPHICS_LOG ).getName(), Color.PINK );
//            loggerMap.put( DasLogger.getLogger(DasLogger.SYSTEM_LOG ).getName(), Color.gray );
//            loggerMap.put( DasLogger.getLogger(DasLogger.GUI_LOG ).getName(), Color.green );
//            loggerMap.put( DasLogger.getLogger(DasLogger.DASML_LOG).getName(), Color.LIGHT_GRAY );
//        }
//        
//        DasCanvas canvas= new DasCanvas(800,400);
//        DasPlot plot= DasPlot.createPlot( new DatumRange( 0, 10, Units.seconds ) ,
//                new DatumRange( 0, 10, Units.dimensionless ) );
//        xaxis= plot.getXAxis();
//        xaxis.setAnimated(false);
//        
//        renderer.setDataSetLoader(null);
//        plot.addRenderer( renderer );
//        
//        canvas.add( plot, new DasRow( canvas, 0.1, 0.9 ), DasColumn.create(canvas) );
//        
//        MouseModule mm=  getMouseModule();
//        plot.getDasMouseInputAdapter().addMouseModule( mm );
//        plot.getDasMouseInputAdapter().setPrimaryModule( mm );
//        
//        mm= getShowLogMouseModule( plot );
//        plot.getDasMouseInputAdapter().addMouseModule( mm );
//        plot.getDasMouseInputAdapter().setSecondaryModule( mm );
//        
//        legend= new Legend();
//        canvas.add( legend, new DasRow( canvas, 0.1, 0.5 ), new DasColumn( canvas, 0.65, 0.98 ) );
//        
//        for ( Iterator i= loggerMap.keySet().iterator(); i.hasNext(); ) {
//            Object key= i.next();
//            String name= String.valueOf(key);
//            if ( name.equals("") ) name="<default>";
//            legend.add( Legend.getIcon( (Color)loggerMap.get(key) ), name );
//        }
//        
//        frame= DasApplication.getDefaultApplication().createMainFrame( "GraphicalLogHandler" );
//        JPanel appPanel= new JPanel( new BorderLayout() );
//        appPanel.add( canvas, BorderLayout.CENTER );
//        
//        JPanel controlPanel= new JPanel();
//        controlPanel.setLayout( new BoxLayout( controlPanel, BoxLayout.X_AXIS ) );
//        
//        JCheckBox jcb= new JCheckBox( getUpdatingAction() );
//        jcb.setSelected(updating);
//        
//        startUpdateThread();
//        
//        controlPanel.add( jcb );
//        
//        JButton x= new JButton( getUpdateAction() );
//        controlPanel.add( x );
//        
//        appPanel.add( controlPanel, BorderLayout.SOUTH );
//        
//        
//        
//        frame.getContentPane().add( appPanel );
//        frame.setVisible( true );
//        frame.pack();
//        frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
//    }
    
//    private Action getUpdatingAction() {
//        return new AbstractAction( "Updating" ) {
//            @Override
//            public void actionPerformed( ActionEvent e ) {
//                JCheckBox source= (JCheckBox)e.getSource();
//                updating= source.isSelected();
//                if ( updating ) startUpdateThread();
//            }
//        };
//    }
//    
//    private Action getUpdateAction() {
//        return new AbstractAction( "Update" ) {
//            @Override
//            public void actionPerformed( ActionEvent e ) {
//                update();
//            }
//        };
//    }
    
    public void setYAxisType( int type ) {
        this.yaxisDimension= type;
    }
    
    public int getYAxisType() {
        return this.yaxisDimension;
    }
                    
    private String formatMessage(LogRecord r) {
        String s = r.getMessage();
        return s == null ? "" : s;
    }
    
    private static String[] guardedSplit( String line, String[] fields ) {
        Pattern p= Pattern.compile("\\s*(?:\"((?:[^\"]|\"\")*)\"|([^,]*))\\s*(?:,|$)");
        if ( fields==null ) {
            List<String> fieldz= new ArrayList<>();
            Matcher m= p.matcher(line);
            while ( m.find() ) {
                String field= m.group(2);
                if (field==null) field=""; // sometimes logger message is null not "".
                fieldz.add(field);
            }
            fields= fieldz.toArray(new String[0]);
        } else {
            Matcher m= p.matcher(line);
            int i=0;
            while ( m.find() && i<fields.length ) {
                fields[i]= m.group(2);
                if (fields[i]==null) fields[i]=""; // sometimes logger message is null not "".
                i=i+1;
            }
        }
        return fields;
    }
    
    public QDataSet readRecords( File f ) throws FileNotFoundException, ParseException {
        DataSetBuilder dsb= new DataSetBuilder(2,1000,9);
        dsb.setNameLabelUnits( 0, "timestamp_iso", "ISO Time", Units.us2000 );
        dsb.setNameLabelUnits( 1, "elapsed_seconds", "Elapsed Seconds", Units.seconds );
        dsb.setNameLabelUnits( 2, "level", "Log Level", Units.dimensionless );
        dsb.setNameLabelUnits( 3, "thread", "Thread", EnumerationUnits.create("thread") );
        dsb.setNameLabelUnits( 4, "logger", "Logger Name", EnumerationUnits.create("loggerName") );
        dsb.setNameLabelUnits( 5, "source_class", "Source Class", EnumerationUnits.create("sourceClass") );
        dsb.setNameLabelUnits( 6, "source_method", "Source Method", EnumerationUnits.create("sourceMethod") );
        dsb.setNameLabelUnits( 7, "message", "message", EnumerationUnits.create("message") );
        dsb.setNameLabelUnits( 8, "thrown", "Thrown", EnumerationUnits.create("thrown") );
        
        Map<String,String> threadNameMap= new HashMap<>();
                
        BufferedReader reader= new BufferedReader( new FileReader(f) );
        String line;
        int iline=0;
        try {
            String[] fields=null;
            line= reader.readLine();
            iline++;
            while ( line!=null ) {
                fields= guardedSplit( line, fields );
                if ( fields.length==2 ) {
                    String[] ss= line.split("=");
                    if ( ss[0].startsWith("thread.") ) {
                        threadNameMap.put( ss[0].substring(7).trim(), ss[1].trim() );
                    }
                    fields= null;
                } else if ( fields.length>2 && fields[0]!=null && fields[0].startsWith("20")) {
                    String threadName= threadNameMap.get(fields[3]);
                    if ( threadName!=null ) fields[3]=threadName;
                    dsb.nextRecord( (Object[])fields );
                } else {
                    fields= null;
                }
                line= reader.readLine();
                iline++;
            }
        } catch (IOException ex) {
            Logger.getLogger(GraphicalLogRenderer.class.getName()).log(Level.SEVERE, null, ex);
        }
        QDataSet ds= dsb.getDataSet();
        QDataSet tt= Ops.unbundle(ds,"elapsed_seconds");
        QDataSet s= Ops.sort(tt);
        ds= Ops.applyIndex(ds, s);
        
        if ( ds.length()==0 ) throw new IllegalArgumentException("must have some records");
        
        this.records= ds;
        
        QDataSet sort;
        QDataSet vv;
            
        QDataSet classNames= Ops.unbundle(ds,"source_class");
        sort= Ops.sort(classNames);
        vv= Ops.uniqValues(classNames,sort);
        
        for ( int i=0; i<vv.length(); i++ ) {
            yaxisMapClass.put(vv.slice(i).svalue(),i);
        }

        QDataSet threads= Ops.unbundle(ds,"thread");
        sort= Ops.sort(threads);
        vv= Ops.uniqValues(threads,sort);
        for ( int i=0; i<vv.length(); i++ ) {
            yaxisMapThread.put(vv.slice(i).svalue(),i);
        }

        QDataSet loggers= Ops.unbundle(ds,"logger");
        sort= Ops.sort(loggers);
        vv= Ops.uniqValues(loggers,sort);
        for ( int i=0; i<vv.length(); i++ ) {
            yaxisMapLogger.put(vv.slice(i).svalue(),i);
        }
        return ds;
    }
    
    
    /**
     * Renderer expects a QDataSet bundle with datasets:<ul>
     * <li>time
     * <li>threadId
     * <li>loggername
     * <li>class
     * <li>message
     * </ul>
     */
    public static class LogRenderer extends Renderer {
        String searchRegex="";
        
        int yaxisDimension;
        Map<String,Integer> yaxisMapClass= new HashMap<>();
        Map<String,Integer> yaxisMapThread= new HashMap<>();
        Map<String,Integer> yaxisMapLogger= new HashMap<>();
        Map<String,Color> loggerNameColorMap= new HashMap<>();
            
        public LogRenderer( int yaxisDimension ) {
            this.yaxisDimension= yaxisDimension;
            loggerNameColorMap.put( DasLogger.getLogger(DasLogger.APPLICATION_LOG).getName(), Color.black );
            loggerNameColorMap.put( DasLogger.getLogger(DasLogger.DATA_OPERATIONS_LOG).getName(), Color.blue );
            loggerNameColorMap.put( DasLogger.getLogger(DasLogger.DATA_TRANSFER_LOG).getName(), Color.YELLOW );
            loggerNameColorMap.put( DasLogger.getLogger(DasLogger.GRAPHICS_LOG ).getName(), Color.PINK );
            loggerNameColorMap.put( DasLogger.getLogger(DasLogger.SYSTEM_LOG ).getName(), Color.gray );
            loggerNameColorMap.put( DasLogger.getLogger(DasLogger.GUI_LOG ).getName(), Color.green );
            loggerNameColorMap.put( DasLogger.getLogger(DasLogger.DASML_LOG).getName(), Color.LIGHT_GRAY );            
        }
        
        ObjectLocator objectLocator;
        
        protected void setObjectLocator( ObjectLocator o ) {
            this.objectLocator= o;
        }

        protected ObjectLocator getObjectLocator() {
            return this.objectLocator;
        }
            
        
        public String getSearchRegex() {
            return searchRegex;
        }
        public void setSearchRegex( String regex ) {
            this.searchRegex= regex;
            super.update();
        }

        @Override
        public void setDataSet(QDataSet ds) {
            super.setDataSet(ds);
            
            QDataSet s,vv;
            
            QDataSet classNames= Ops.unbundle(ds,"source_class");
            s= Ops.sort(classNames);
            vv= Ops.uniqValues(classNames,s);
            for ( int i=0; i<vv.length(); i++ ) {
                yaxisMapClass.put(vv.slice(i).svalue(),i);
            }
            
            QDataSet threads= Ops.unbundle(ds,"thread");
            s= Ops.sort(threads);
            vv= Ops.uniqValues(threads,s);
            for ( int i=0; i<vv.length(); i++ ) {
                yaxisMapThread.put(vv.slice(i).svalue(),i);
            }
            
            QDataSet loggers= Ops.unbundle(ds,"logger");
            s= Ops.sort(loggers);
            vv= Ops.uniqValues(loggers,s);
            for ( int i=0; i<vv.length(); i++ ) {
                yaxisMapLogger.put(vv.slice(i).svalue(),i);
            }
                        
        }
        
        
        
        public QDataSet doAutorange( QDataSet ds ) {
            if ( ds.length()==0 ) {
                return null;
            }
            if ( ds.rank()!=2 ) {
                return null;
            }
            QDataSet times= Ops.unbundle(ds, "elapsed_seconds");

            return Ops.join( Ops.extent(times), Ops.dataset(Ops.datumRange("0 to 100")) );

        }
        
        @Override
        public void render(Graphics2D g1, DasAxis xAxis, DasAxis yAxis ) {
            
            Graphics2D g= (Graphics2D)g1;
            g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
            
            int ix0= (int) xAxis.transform( xAxis.getDataMinimum() );
            g.setColor( Color.lightGray );
            
            Map<String,Integer> yaxisMap;
            QDataSet yAxisValues;
            switch (yaxisDimension) {
                case YAXIS_CLASS:
                    yaxisMap= yaxisMapClass;
                    yAxisValues= Ops.unbundle(ds, "source_class");
                    break;
                case YAXIS_THREAD:
                    yaxisMap= yaxisMapThread;
                    yAxisValues= Ops.unbundle(ds, "thread");
                    break;
                default:
                    yaxisMap= yaxisMapLogger;
                    yAxisValues= Ops.unbundle(ds, "logger");
                    break;
            }
            
            for ( Map.Entry<String,Integer> e: yaxisMap.entrySet() ) {
                Object name= e.getKey();
                Integer ithread= e.getValue();
                int iy= (int)yAxis.transform( Units.dimensionless.createDatum(ithread.intValue()) );
                g.drawString( String.valueOf(name), ix0+2, iy );
            }
            
            ObjectLocator objectLocator= new ObjectLocator();

            QDataSet times= Ops.unbundle(ds, "elapsed_seconds");
            int firstIndex= (int)( Ops.floor( Ops.findex( times, xAxis.getDataMinimum() ) ).value() );
            if ( firstIndex<0 ) firstIndex= -1 - firstIndex;
            int lastIndex= (int)( Ops.floor( Ops.findex( times, xAxis.getDataMaximum() ) ).value() );
            if ( lastIndex<0 ) {
                lastIndex= -1 - lastIndex;
            } else {
                lastIndex++;
            }

            int lastX=-999;
            int lastY=-999;
            int collisionCount=0;

            QDataSet messages= Ops.unbundle(ds, "message");
                    
            DasPlot parent= getParent();
            if ( !searchRegex.equals("") ) {
                for ( int i=firstIndex; i<lastIndex; i++ ) {
                    String recordMessage= messages.slice(i).svalue();
                    if ( recordMessage.matches( searchRegex ) ) {
                        int ix= (int)xAxis.transform( times.slice(i) );
                        g.setColor( Color.lightGray );
                        g.fillRect( ix-2, parent.getY(), 5, parent.getHeight() );
                        objectLocator.addObject( new Rectangle( ix-2, parent.getY(), 5, parent.getHeight() ), i );
                    }
                }
            }
            
            QDataSet loggerNames= Ops.copy(Ops.unbundle(ds, "logger"));
            QDataSet levels= Ops.copy(Ops.unbundle(ds, "level"));  // copy makes debugging easier

            for ( int i=firstIndex; i<lastIndex; i++ ) {
                if ( i%100==0 ) System.err.println(i);
                String sthread= yAxisValues.slice(i).svalue();  // note might be thread, or classname, or loggername.
                int ithread= yaxisMap.get(sthread);

                int iy= (int)yAxis.transform( Units.dimensionless.createDatum(ithread) );
                int ix= (int)xAxis.transform( times.slice(i) );

                if ( ix==lastX && iy==lastY ) {
                    collisionCount++;
                } else {
                    lastX= ix;
                    lastY= iy;
                    collisionCount=0;
                }

                if ( !searchRegex.isEmpty() ) {
                    if ( messages.slice(i).svalue().matches( searchRegex ) ) {
                        g.setColor( Color.lightGray );
                        g.fillRect( ix-2, 0, 5, 100 );
                    }
                }
                
                String loggerName= loggerNames.slice(i).svalue();

                Color color= (Color)loggerNameColorMap.get( loggerName );
                if ( color==null ) {
                    String key= loggerName;
                    loggerNameColorMap.put( key, Color.ORANGE );
                    //legend.add( Legend.getIcon( (Color)loggerMap.get(key) ), String.valueOf( key ) );
                    //legend.repaint();
                }
                g.setColor( color );

                int height= (int)( levels.value(i) / 100 );
                g.fillRect( ix-2, iy-height-2, 5, height );
                objectLocator.addObject( new Rectangle( ix-2, iy-height-2, 5, height ), i );
            }
            
            this.objectLocator= objectLocator;
            
        }
        
    }
    
    public Renderer getRenderer() {
        if ( renderer==null ) {
            renderer= new LogRenderer(YAXIS_THREAD);
            //createCanvas();
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
            
            Integer select= (Integer)GraphicalLogRenderer.this.renderer.getObjectLocator().closestObject( p2 );
            
            if ( select==null ) {
                return null;
            }
            
            int iclosest= select;
            
            QDataSet record= records.slice(iclosest);
            
            String label;
            Rectangle[] myDirtyBounds;
            
            QDataSet yAxisValues;
            Map<String,Integer> map;
            
            switch (yaxisDimension) {
                case YAXIS_CLASS:
                    yAxisValues= Ops.unbundle( record, "source_class" );
                    map= yaxisMapClass;
                    break;
                case YAXIS_THREAD:
                    yAxisValues= Ops.unbundle( record, "thread" );
                    map= yaxisMapThread;
                    break;
                case YAXIS_LOGNAME:
                    yAxisValues= Ops.unbundle( record, "logger" );
                    map= yaxisMapLogger;
                    break;
                default:
                    throw new IllegalArgumentException("bad state");
            }
            
            if ( select==null ) {
                label= LABEL_NOT_AVAILABLE;
                myDirtyBounds= new Rectangle[] { new Rectangle( 0,0,0,0 ), new Rectangle( 0,0,0,0 ) };
                
            } else {
                QDataSet messages= Ops.unbundle( GraphicalLogRenderer.this.records, "message" );
                QDataSet times= Ops.unbundle( GraphicalLogRenderer.this.records, "elapsed_seconds" );
                String message= messages.slice(select).svalue();
                
                QDataSet loggerNames= Ops.unbundle( GraphicalLogRenderer.this.records, "logger" );
                QDataSet levels= Ops.unbundle( GraphicalLogRenderer.this.records, "level" );
                
                label= loggerNames.slice(select).svalue()+":"+levels.slice(select).svalue()+":!c"+message;
                
                QDataSet t= times.slice(select);
                
                int ix= (int)xaxis.transform( t );
                int iy= (int)yaxis.transform( map.get(yAxisValues.svalue()), yaxis.getUnits() );
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
        result.addBoxSelectionListener((BoxSelectionEvent e) -> {
            StringBuilder buf= new StringBuilder(1000);
            
            //Handler h= new ConsoleHandler();
            //Formatter f= h.getFormatter();
            Formatter f= new DenseConsoleFormatter();
            
            DatumRange threadsRange= e.getYRange();
            DatumRange timeRange= e.getXRange();
            
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
            
            int messageCount=0;
            QDataSet messages= Ops.unbundle( records, "message" );
            for ( int i=0; i<records.length(); i++ ) {
                double time= ((Long)times.get( i )).doubleValue();
                if ( timeRange.contains( Units.milliseconds.createDatum( time ) ) ) {
                    if ( threadsRange.contains( Units.dimensionless.createDatum( (Number)yAxisValues.get(i) ) ) ) {
                        buf.append( messages.slice(i).svalue() );
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
        });
        return result;
    }
        
}