/*
 * BatchMaster.java
 *
 * Created on January 31, 2004, 3:03 PM
 */

package edu.uiowa.physics.pw.das.components;

import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.event.*;
import edu.uiowa.physics.pw.das.graph.*;
import edu.uiowa.physics.pw.das.util.*;
import java.io.*;
import java.text.*;
import java.util.*;
import javax.swing.*;
import testNew.*;

/**
 *
 * @author  Jeremy
 */
public class BatchMaster {
    
    List taskList;
    int itask;
    DasCanvas canvas;
        
    public static class Timer {
        long t0= System.currentTimeMillis();;
        DecimalFormat df= new DecimalFormat( "00000.000" );
        public void reportTime( String msg ) {
            Thread thread= Thread.currentThread();
            long elapsed= System.currentTimeMillis() - t0;
            System.out.println( df.format(elapsed/1000.)+" "+msg + "("+ thread +")" );
        }
    }
    
    public static Timer timer= new Timer();
    
    public static BatchMaster create( DasCanvas canvas, File specFile ) throws ParseException, IOException {
        List taskList= new ArrayList();
        BufferedReader r= new BufferedReader( new FileReader( specFile ) );
        String s= r.readLine();
        BatchMaster result= new BatchMaster(canvas);
        while ( s!=null ) {
            String[] s1= s.split(" ");
            Datum begin= TimeUtil.create(s1[0]);
            Datum end= TimeUtil.create(s1[1]);            
            result.addTask( begin, end );
            s= r.readLine();
        }        
        return result;
    }
    
    /** Creates a new instance of BatchMaster */
    public BatchMaster( DasCanvas canvas ) {
        this.canvas= canvas;
        taskList= new ArrayList();
        itask= 0;
    }
    
    public void start() {
        submitNextTask();
    }
    
    void addTask( Datum begin, Datum end ) {
        taskList.add( new DataRangeSelectionEvent( this, begin, end ) );
    }    
    
    /*void submitFirstTask() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                submitNextTask();
            }
        });
    }*/
    
    void submitNextTask() {
        Thread thread= new Thread( new Runnable() {
            public void run() {
                if ( itask>=taskList.size() ) {
                    DasExceptionHandler.handle( new RuntimeException( "done!" ) );
                } else {
                    System.out.println( "itask="+taskList.get(itask) );
                    fireDataRangeSelectionListenerDataRangeSelected( (DataRangeSelectionEvent) taskList.get(itask++) );                    
                    try {
                        timer.reportTime("call to writePng");
                        canvas.writeToPng("J:/temp/"+itask+".png");
                    } catch ( IOException e ) {
                        DasExceptionHandler.handle(e);
                    }
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
                ((edu.uiowa.physics.pw.das.event.DataRangeSelectionListener)listeners[i+1]).DataRangeSelected(event);
            }
        }
    }

    /**
     * @param args the command line arguments
     */
    /*
    public static void main(String[] args) throws Exception {
        TimeAxisDemo t= new TimeAxisDemo();
        t.getAxis().setAnimated(false);
        BatchMaster bm= create( t.getAxis().getCanvas(), new File( "J:/das/working/batchMasterTest.txt" ) );        
        bm.addDataRangeSelectionListener( t.getAxis() );
        bm.start();
    }
     */
    
}
