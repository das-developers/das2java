/*
 * Util.java
 *
 * Created on September 23, 2005, 1:01 PM
 *
 *
 */

package org.das2.stream;

import org.das2.DasException;
import org.das2.client.DataSetStreamHandler;
import org.das2.dataset.DataSet;
import org.das2.dataset.DataSetDescriptor;
import org.das2.dataset.TableDataSet;
import org.das2.util.monitor.NullProgressMonitor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;

/**
 *
 * @author Jeremy
 */
public class StreamUtil {
    
    private static final String DATA_SET_ID_PREFIX
            = "class:org.das2.stream.test.LocalFileStandardDataStreamSource?file=";
    
    public static TableDataSet loadTableDataSet( String filename ) {
        try {
            filename= URLEncoder.encode(filename,"UTF-8");
            DataSetDescriptor dsd= DataSetDescriptor.create( DATA_SET_ID_PREFIX+filename );
            dsd.setDefaultCaching(false);
            DataSet ds= dsd.getDataSet(null,null,null,null);
            return (TableDataSet)ds;
        } catch ( UnsupportedEncodingException e ) {
            throw new RuntimeException(e);
        } catch ( DasException e ) {
            throw new RuntimeException(e);
        }
    }
    
    public static DataSet loadDataSetNew( String filename ) throws IOException, StreamException {
        FileInputStream in= new FileInputStream( filename );
        ReadableByteChannel channel = in.getChannel();
                
        DataSetStreamHandler handler = new DataSetStreamHandler( new HashMap(), new NullProgressMonitor() );
             
        StreamTool.readStream(channel, handler);
        return handler.getDataSet();
    }
    
    public static DataSet loadDataSet( String filename ) {        
        try {
            filename= URLEncoder.encode(filename,"UTF-8");
            DataSetDescriptor dsd= DataSetDescriptor.create( DATA_SET_ID_PREFIX+filename );
            dsd.setDefaultCaching(false);
            DataSet ds= dsd.getDataSet(null,null,null,null);
            return ds;
        } catch ( UnsupportedEncodingException e ) {
            throw new RuntimeException(e);
        } catch ( DasException e ) {
            throw new RuntimeException(e);
        }
    }
}
