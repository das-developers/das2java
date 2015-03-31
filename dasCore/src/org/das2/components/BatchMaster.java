/*
 * BatchMaster.java
 *
 * Created on January 31, 2004, 3:03 PM
 */

package org.das2.components;

import org.das2.event.DataRangeSelectionEvent;
import org.das2.datum.DatumRange;
import org.das2.datum.DatumRangeUtil;
import org.das2.util.DasExceptionHandler;
import org.das2.util.DasPNGEncoder;
import org.das2.util.DasPNGConstants;
import org.das2.graph.DasCanvas;
import org.das2.system.DasLogger;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.logging.Level;


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
        public void reportTime( String msg ) {
            Thread thread= Thread.currentThread();
            long elapsed= System.currentTimeMillis() - t0;
            System.out.println( df.format(elapsed/1000.)+" "+msg + "("+ thread +")" );
        }
    }
    
    public static final Timer timer= new Timer();
    
    public interface TaskOutputDescriptor {
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
            @Override
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
                addTask( dr );
            }
            s= r.readLine();
        }
    }
    
    /**
     * create the pngwalk using the class.
     * @param canvas the source for images
     * @param specFile flat text file containing one parsable time range per line. (For example, "1990-01-01T00:00 1990-01-02T00:00" or "1990-01-01")
     * @param pngFilenameTemplate (For example, "BEGIN_END.png")
     * @throws java.text.ParseException with the specFile
     * @throws java.io.IOException with the specFile
     * @return BatchMaster object.
     */
    public static BatchMaster createPngs( DasCanvas canvas, File specFile, String pngFilenameTemplate ) throws ParseException, IOException {
        BatchMaster result= new BatchMaster(canvas );
        result.setTaskOutputDescriptor( result.createPngsTaskOutputDescriptor( pngFilenameTemplate ) );
        result.readStartEndSpecFile( specFile );
        return result;
    }
    
    /** 
     * Creates a new instance of BatchMaster
     * @param canvas the source for images
     */
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
     * add another range
     * @param range
     */
    void addTask( DatumRange range ) {
        taskList.add( new DataRangeSelectionEvent( this, range.min(), range.max() ) );
    }
    
    /**
     * The TaskOutputDescriptor is called as each task is completed.
     * @param tod
     */
    void setTaskOutputDescriptor( TaskOutputDescriptor tod ) {
        this.tod= tod;
    }
    
    /**
     * If true, then System.exit is called after running batch.
     * @param val If true, then System.exit is called after running batch.
     */
    void setExitAfterCompletion( boolean val ) {
        this.exit= val;
    }
    
    void submitNextTask() {
        Thread thread= new Thread( new Runnable() {
            @Override
            public void run() {
                if ( itask>=taskList.size() ) {
                    if ( exit ) System.exit(0);
                } else {
                    DasLogger.getLogger(DasLogger.SYSTEM_LOG).log(Level.INFO, "itask={0}", taskList.get(itask));
                    DataRangeSelectionEvent ev=  (DataRangeSelectionEvent) taskList.get(itask++);
                    fireDataRangeSelectionListenerDataRangeSelected( ev );
                    canvas.waitUntilIdle();
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
    public synchronized void addDataRangeSelectionListener(org.das2.event.DataRangeSelectionListener listener) {
        if (listenerList == null ) {
            listenerList = new javax.swing.event.EventListenerList();
        }
        listenerList.add(org.das2.event.DataRangeSelectionListener.class, listener);
    }
    
    /** Removes DataRangeSelectionListener from the list of listeners.
     * @param listener The listener to remove.
     */
    public synchronized void removeDataRangeSelectionListener(org.das2.event.DataRangeSelectionListener listener) {
        listenerList.remove(org.das2.event.DataRangeSelectionListener.class, listener);
    }
    
    
    /** Notifies all registered listeners about the event.
     *
     * @param event The event to be fired
     */
    private void fireDataRangeSelectionListenerDataRangeSelected(DataRangeSelectionEvent event) {
        if (listenerList == null) return;
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==org.das2.event.DataRangeSelectionListener.class) {
                ((org.das2.event.DataRangeSelectionListener)listeners[i+1]).dataRangeSelected(event);
            }
        }
    }
    
    
}
