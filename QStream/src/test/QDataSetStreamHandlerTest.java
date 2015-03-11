/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import org.virbo.dataset.QDataSet;
import org.virbo.qstream.QDataSetStreamHandler;
import org.virbo.qstream.StreamException;
import org.virbo.qstream.StreamTool;

/**
 *
 * @author jbf
 */
public class QDataSetStreamHandlerTest {
    
    private static void test1() throws FileNotFoundException, StreamException {
        long t0= System.currentTimeMillis();
        ReadableByteChannel stream= new FileInputStream("/home/jbf/ct/hudson/data.backup/hyd_20000101.qds").getChannel();
        //ReadableByteChannel stream= new FileInputStream("/home/jbf/ct/hudson/data.backup/qds/junoRank3_20120923.qds").getChannel();
        QDataSetStreamHandler handler = new QDataSetStreamHandler();
        StreamTool.readStream( stream, handler );
        //System.err.println( handler.getDataSet("ds_99") );
        System.err.println( handler.getDataSet("ENERGY_ELE") );
        System.err.println( handler.getDataSet("ELECTRON_DIFFERENTIAL_ENERGY_FLUX") );
        System.err.println( String.format( "read in %d ms", ( System.currentTimeMillis()-t0 ) ) );        
    }

    private static void test2() throws FileNotFoundException, StreamException {
        long t0= System.currentTimeMillis();
        ReadableByteChannel stream= new FileInputStream("/home/jbf/ct/hudson/data.backup/qds/2014294.qds").getChannel();
        //ReadableByteChannel stream= new FileInputStream("/home/jbf/ct/hudson/data.backup/qds/junoRank3_20120923.qds").getChannel();
        QDataSetStreamHandler handler = new QDataSetStreamHandler();
        StreamTool.readStream( stream, handler );
        //System.err.println( handler.getDataSet("ds_99") );
        System.err.println( handler.getDataSet("TimeStop") );
        System.err.println( handler.getDataSet("Time") );
        System.err.println( String.format( "read in %d ms", ( System.currentTimeMillis()-t0 ) ) );
        QDataSet dts= handler.getDataSet("TimeStop") ;
        QDataSet t= handler.getDataSet("Time");
        for ( int i=0; i<dts.length(); i++ ) {
            System.err.printf("%s %s\n", t.slice(i).slice(0), dts.value(i,0) );
        }
        
    }
        
    public static void main( String[] args ) throws StreamException, IOException {
        //Logger.getLogger("qstream").setLevel(Level.FINER);
        //Handler h= new ConsoleHandler();
        //h.setLevel(Level.ALL);
        //Logger.getLogger("qstream").addHandler( h );
        //test1();
        test2();
        
    }
}
