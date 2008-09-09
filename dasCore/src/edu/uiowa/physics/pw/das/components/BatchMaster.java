/*
 * BatchMaster.java
 *
 * Created on January 31, 2004, 3:03 PM
 */

package edu.uiowa.physics.pw.das.components;

import org.das2.util.DasExceptionHandler;
import org.das2.util.DasPNGEncoder;
import org.das2.util.DasPNGConstants;
import org.das2.DasApplication;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.event.*;
import edu.uiowa.physics.pw.das.graph.*;
import org.das2.system.DasLogger;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.List;


/**
 * BatchMaster is a object that runs through a batch file, controlling a time axis to produce a series of images.
 * @author Jeremy
 */
public class BatchMaster {
    
    /* TODO: create ExceptionHandler, set DasExceptionHandler to this handler
     * to collect all the messages, then display them at the end.  Also keep
     * list of completed/uncompleted tasks
     */
    
    java.util.List taskList;
    int itask;
    DasCanvas canvas;
    TaskOutputDescriptor tod;
    boolean exit= true;
    
    public static class Timer {
        long t0= System.currentTimeMillis();;
        DecimalFormat df= new DecimalFormat( "00000.000" );
        /**
         *
         * @param msg
         */
        public void reportTime( String msg ) {
            Thread thread= Thread.currentThread();
            long elapsed= System.currentTimeMillis() - t0;
            System.out.println( df.format(elapsed/1000.)+" "+msg + "("+ thread +")" );
        }
    }
    
    public static Timer timer= new Timer();
    
    private interface TaskOutputDescriptor {
        public void completeTask( DatumRange range );
    }
    
    /**
     *
     * @param pngFilenameTemplate  BEGIN,END,RANGE substituted to form name
     * @return TaskOutputDescriptor describing the task.
     */
    public TaskOutputDescriptor createPngsTaskOutputDescriptor( final String pngFilenameTemplate ) {
        return new TaskOutputDescriptor() {
            private String insertRange( String filenameTemplate, DatumRange range ) {
                String rangeString= range.toString().replaceAll(":","-").replaceAll(" ","_");
                String s= filenameTemplate
                        .replaceAll( "BEGIN", range.min().toString().replaceAll(":","-") )
                        .replaceAll( "END", range.max().toString().replaceAll(":","-") )
                        .replaceAll( "RANGE", rangeString );
                return s;
            }
            public void completeTask( DatumRange range ) {
                Image image= BatchMaster.this.canvas.getImage( canvas.getWidth(), canvas.getHeight() );
                String s= insertRange( pngFilenameTemplate, range );
                try {
                    OutputStream out= new FileOutputStream( s );
                    try {
                        DasPNGEncoder encoder = new DasPNGEncoder();
                        encoder.addText(DasPNGConstants.KEYWORD_CREATION_TIME, new Date().toString());
                        encoder.write((BufferedImage)image, out);
                    } finally {
                        out.close();
                    }
                } catch ( IOException e ) {
                    DasExceptionHandler.handle(e);
                }
            }
        };
    }

    private void readStartEndSpecFile( File specFile ) throws ParseException, IOException {
        BufferedReader r= new BufferedReader( new FileReader( specFile ) );
        String s= r.readLine();
        while ( s!=null ) {
            s= s.trim();
            if ( !( s.equals("") || s.startsWith("#" ) ) ) {
                DatumRange dr= DatumRangeUtil.parseTimeRange(s);
                Datum begin= dr.min();
                Datum end= dr.max();
                addTask( begin, end );
            }
            s= r.readLine();
        }
    }
    
    /**
     *
     * @param canvas
     * @param specFile flat text file containing one parsable time range per line. (For example, "1990-01-01T00:00 1990-01-02T00:00" or "1990-01-01")
     * @param pngFilenameTemplate (For example, "BEGIN_END.png")
     * @throws java.text.ParseException
     * @throws java.io.IOException
     * @return BatchMaster object.
     */
    public static BatchMaster createPngs( DasCanvas canvas, File specFile, String pngFilenameTemplate ) throws ParseException, IOException {
        List taskList= new ArrayList();
        BatchMaster result= new BatchMaster(canvas );
        result.setTaskOutputDescriptor( result.createPngsTaskOutputDescriptor( pngFilenameTemplate ) );
        result.readStartEndSpecFile( specFile );
        return result;
    }
    
    /** Creates a new instance of BatchMaster */
    public BatchMaster( DasCanvas canvas ) {
        this.canvas= canvas;
        taskList= new ArrayList();
        itask= 0;
    }
    
    /**
     * Starts the batch process.
     */
    public void start() {
        submitNextTask();
    }
    
    /**
     * @depricated use addTask( DatumRange )
     */
    void addTask( Datum begin, Datum end ) {
        taskList.add( new DataRangeSelectionEvent( this, begin, end ) );
    }
    
    /**
     *
     * @param range
     */
    void addTask( DatumRange range ) {
        taskList.add( new DataRangeSelectionEvent( this, range.min(), range.max() ) );
    }
    
    /**
     *
     * @param tod
     */
    void setTaskOutputDescriptor( TaskOutputDescriptor tod ) {
        this.tod= tod;
    }
    
    /**
     *
     * @param val If true, then System.exit is called after running batch.
     */
    void setExitAfterCompletion( boolean val ) {
        this.exit= val;
    }
    
    void submitNextTask() {
        Thread thread= new Thread( new Runnable() {
            public void run() {
                if ( itask>=taskList.size() ) {
                    if ( exit ) System.exit(0);
                } else {
                    DasLogger.getLogger(DasLogger.SYSTEM_LOG).info( "itask="+taskList.get(itask) );
                    DataRangeSelectionEvent ev=  (DataRangeSelectionEvent) taskList.get(itask++);
                    fireDataRangeSelectionListenerDataRangeSelected( ev );
                    try {
                        canvas.waitUntilIdle();
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    tod.completeTask( ev.getDatumRange() );
                    submitNextTask();
                }
            }
        });
        thread.start();
    }
    
    /** Utility field used by event firing mechanism. */
    private javax.swing.event.EventListenerList listenerList =  null;
    
    /** Registers DataRangeSelectionListener to receive events.
     * @param listener The listener to register.
     */
    public synchronized void addDataRangeSelectionListener(edu.uiowa.physics.pw.das.event.DataRangeSelectionListener listener) {
        if (listenerList == null ) {
            listenerList = new javax.swing.event.EventListenerList();
        }
        listenerList.add(edu.uiowa.physics.pw.das.event.DataRangeSelectionListener.class, listener);
    }
    
    /** Removes DataRangeSelectionListener from the list of listeners.
     * @param listener The listener to remove.
     */
    public synchronized void removeDataRangeSelectionListener(edu.uiowa.physics.pw.das.event.DataRangeSelectionListener listener) {
        listenerList.remove(edu.uiowa.physics.pw.das.event.DataRangeSelectionListener.class, listener);
    }
    
    
    /** Notifies all registered listeners about the event.
     *
     * @param event The event to be fired
     */
    private void fireDataRangeSelectionListenerDataRangeSelected(DataRangeSelectionEvent event) {
        if (listenerList == null) return;
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==edu.uiowa.physics.pw.das.event.DataRangeSelectionListener.class) {
                ((edu.uiowa.physics.pw.das.event.DataRangeSelectionListener)listeners[i+1]).dataRangeSelected(event);
            }
        }
    }
    
    
}
