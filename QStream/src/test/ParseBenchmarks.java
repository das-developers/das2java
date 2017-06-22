/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.util.HashMap;
import org.das2.client.DataSetStreamHandler;
import org.das2.dataset.DataSet;
import org.das2.util.monitor.NullProgressMonitor;
import org.das2.qds.QDataSet;
import org.das2.qstream.QDataSetStreamHandler;
import org.das2.qstream.StreamException;
import org.das2.qstream.StreamTool;

/**
 *
 * @author jbf
 */
public class ParseBenchmarks {

    public static void main(String[] args) throws FileNotFoundException, StreamException, org.das2.stream.StreamException {
        readAsciiQds();
        readAsciiD2s();
        readBinaryQds();
        readBinaryD2s();        
    }

    private static void readStream( File f ) throws FileNotFoundException, StreamException, org.das2.stream.StreamException {
        String ext= f.toString().substring(f.toString().lastIndexOf(".") ); // URI okay
        
        if ( ext.equals(".qds") ) {
            long t0 = System.currentTimeMillis();
            InputStream in = new FileInputStream(f);
            QDataSetStreamHandler handler = new QDataSetStreamHandler();
            StreamTool.readStream(Channels.newChannel(in), handler);
            QDataSet qds = handler.getDataSet();
            System.err.println("Time to read " + qds.length() + " records: " + (System.currentTimeMillis() - t0));
            
        } else {
            long t0 = System.currentTimeMillis();
            InputStream in = new FileInputStream(f);
            DataSetStreamHandler handler = new DataSetStreamHandler( new HashMap(), new NullProgressMonitor() );
            org.das2.stream.StreamTool.readStream(Channels.newChannel(in), handler);
            DataSet ds = handler.getDataSet();
            System.err.println("Time to read " + ds.getXLength() + " records: " + (System.currentTimeMillis() - t0));
        }
    }
    
    
    private static void readAsciiQds() throws FileNotFoundException, StreamException, org.das2.stream.StreamException {

        File f = new File(System.getProperty("user.home") + "/temp/benchmark1.qds");

        for (int i = 0; i < 5; i++) {
            readStream(f);
        }
    }
    
    private static void readAsciiD2s() throws FileNotFoundException, org.das2.stream.StreamException, StreamException {

        File f = new File(System.getProperty("user.home") + "/temp/benchmark1.d2s");

        for (int i = 0; i < 5; i++) {
            readStream(f);
        }
    }

    private static void readBinaryQds() throws FileNotFoundException, StreamException, org.das2.stream.StreamException {

        File f = new File(System.getProperty("user.home") + "/temp/benchmark1.binary.qds");

        for (int i = 0; i < 5; i++) {
            readStream(f);
        }
    }
    
    private static void readBinaryD2s() throws FileNotFoundException, org.das2.stream.StreamException, StreamException {

        File f = new File(System.getProperty("user.home") + "/temp/benchmark1.binary.d2s");

        for (int i = 0; i < 5; i++) {
            readStream(f);
        }
    }
    
}
