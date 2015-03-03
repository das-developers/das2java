/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test.system;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.das2.graph.DasCanvas;
import org.das2.graph.DasColumn;
import org.das2.graph.DasPlot;
import org.das2.graph.DasRow;
import org.das2.system.DasLogger;

/**
 * This shows that the isDirty flag is still set on the 
 * @author jbf
 */
public class TestDirtyEventQueueBlocker {
    public static void main( String[] args ) throws IOException {
        
        DasLogger.getLogger(DasLogger.GRAPHICS_LOG).setLevel( Level.ALL );
        DasLogger.getLogger(DasLogger.GRAPHICS_LOG).addHandler( new Handler() {
            @Override
            public void publish(LogRecord record) {
                System.err.println( MessageFormat.format( record.getMessage(), record.getParameters() ) );
            }
            @Override
            public void flush() {
            }
            @Override
            public void close() throws SecurityException {
            }
        });
         
        DasCanvas c= new DasCanvas(300,200);
        
        DasPlot p= DasPlot.createDummyPlot();
        DasRow row= new DasRow( c, 0.1, 0.9 );
        DasColumn column= new DasColumn( c, 0.1, 0.9 );
        c.add( p, row, column );
        c.setSize(640,480);
        row.setDMinimum(10);
        
        c.writeToPng("/tmp/ap/canvasTestDirtyEventQueueBlocker.png");
    }
}
