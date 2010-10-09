/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test.stream;

import org.das2.client.DataSetStreamHandler;
import org.das2.dataset.VectorDataSet;
import org.das2.stream.StreamException;
import org.das2.util.DasProgressMonitorInputStream;
import org.das2.stream.StreamTool;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import org.das2.util.monitor.NullProgressMonitor;
import org.das2.util.monitor.ProgressMonitor;
import org.das2.components.DasProgressPanel;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import org.das2.dataset.DataSet;

/**
 *
 * @author jbf
 */
public class TestDas2StreamRead {

    public static void main(String[] args) throws Exception {
        //final URL dataUrl = TestDas2StreamRead.class.getResource("/epupdate4.20071130.d2s");
        //final URL dataUrl = new URL("file:///home/jbf/sun/media/d2s/AllCassiniDensity.20071204.v2.d2s");
        final URL dataUrl = new URL("http://www-wbd.physics.uiowa.edu/das/das2Server?server=dataset&start_time=2007-04-17T08%3A40%3A00.000Z&end_time=2007-04-17T08%3A50%3A00.000Z&resolution=1.1695906432748537&dataset=das2_1%2Fcluster%2Fwbd%2Fr_wbd_wf&params=sc%3D2%26ant%3DEz%26fo%3Dx");
        long t0;
        

        {
            t0 = System.currentTimeMillis();
            ProgressMonitor mon = DasProgressPanel.createFramed("reading data");
            DataSet ds = getDataSet(dataUrl, mon);

            long dt = (System.currentTimeMillis() - t0);
            System.err.println("done in " + dt + " millis");


            File f = new File(dataUrl.getPath());
            long taskSize = f.length() > 0 ? f.length() : -1;

            System.err.println("  " + (taskSize / dt) + " Kb/s");

        }

        {
            t0 = System.currentTimeMillis();
            ProgressMonitor mon = DasProgressPanel.createFramed("reading data");
            
            eatData(dataUrl, mon);

            long dt = (System.currentTimeMillis() - t0);
            System.err.println("done in " + dt + " millis");


            File f = new File(dataUrl.getPath());
            long taskSize = f.length() > 0 ? f.length() : -1;

            System.err.println("  " + (taskSize / dt) + " Kb/s");
        }
    }

    /**
     * eats up the data, ignoring the content.  This is the optimal speed.
     * @param url
     * @param mon
     * @throws java.io.IOException
     */
    public static void eatData(URL url, ProgressMonitor mon) throws IOException {
        long taskSize = -1;

        URLConnection connect = url.openConnection();
        InputStream in = connect.getInputStream();

        System.err.println("reading data from " + url);

        if (connect instanceof HttpURLConnection) {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            taskSize = connection.getContentLength();

        } else if (url.getProtocol().equals("file")) {
            File f = new File(url.getPath());
            taskSize = f.length() > 0 ? f.length() : -1;
        } else {
            taskSize = -1;
        }

        System.err.println("  taskSize=" + taskSize);

        DasProgressMonitorInputStream min = new DasProgressMonitorInputStream(in, mon);
        min.setStreamLength(taskSize);
        in = min;

        DataSetStreamHandler handler = new DataSetStreamHandler(new HashMap(), new NullProgressMonitor());

        ReadableByteChannel channel = Channels.newChannel(in);

        ByteBuffer buf = ByteBuffer.allocateDirect(84);

        int bytesRead = 84;
        while (bytesRead == 84 ) {
            bytesRead = channel.read(buf);
            buf.position(4);
            DoubleBuffer dbuf= buf.slice().asDoubleBuffer();
            for ( int i=0; i<10; i++ ) {
                double d= dbuf.get(i);
            }
            buf.position(0);
        }

        in.close();
    }

    public static DataSet getDataSet(URL url, ProgressMonitor mon) throws IOException, StreamException {

        long taskSize = -1;

        URLConnection connect = url.openConnection();
        InputStream in = connect.getInputStream();

        System.err.println("reading data from " + url);

        if (connect instanceof HttpURLConnection) {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            taskSize = connection.getContentLength();

        } else if (url.getProtocol().equals("file")) {
            File f = new File(url.getPath());
            taskSize = f.length() > 0 ? f.length() : -1;
        } else {
            taskSize = -1;
        }

        System.err.println("  taskSize=" + taskSize);

        DasProgressMonitorInputStream min = new DasProgressMonitorInputStream(in, mon);
        min.setStreamLength(taskSize);
        in = min;

        DataSetStreamHandler handler = new DataSetStreamHandler(new HashMap(), new NullProgressMonitor());

        ReadableByteChannel channel = Channels.newChannel(in);

        StreamTool.readStream(channel, handler);

        in.close();

        return handler.getDataSet();
    }
}
