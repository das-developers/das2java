/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package test;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.virbo.qstream.QDataSetStreamHandler;
import org.virbo.qstream.StreamException;
import org.virbo.qstream.StreamTool;

/**
 *
 * @author jbf
 */
public class QDataSetStreamHandlerTest {
    public static void main( String[] args ) throws StreamException, IOException {
        //Logger.getLogger("qstream").setLevel(Level.FINER);
        //Handler h= new ConsoleHandler();
        //h.setLevel(Level.ALL);
        //Logger.getLogger("qstream").addHandler( h );
        long t0= System.currentTimeMillis();
        ReadableByteChannel stream= new FileInputStream("/home/jbf/ct/hudson/data.backup/qds/aggregation.qds").getChannel();
        //ReadableByteChannel stream= new FileInputStream("/home/jbf/ct/hudson/data.backup/qds/junoRank3_20120923.qds").getChannel();
        QDataSetStreamHandler handler = new QDataSetStreamHandler();
        StreamTool.readStream( stream, handler );
        //System.err.println( handler.getDataSet("ds_99") );
        System.err.println( handler.getDataSet("ds_1") );
        System.err.println( handler.getDataSet("ds_0") );
        System.err.println( String.format( "read in %d ms", ( System.currentTimeMillis()-t0 ) ) );
    }
}
