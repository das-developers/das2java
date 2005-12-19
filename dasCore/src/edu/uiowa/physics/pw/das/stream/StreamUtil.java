/*
 * Util.java
 *
 * Created on September 23, 2005, 1:01 PM
 *
 *
 */

package edu.uiowa.physics.pw.das.stream;

import edu.uiowa.physics.pw.das.DasException;
import edu.uiowa.physics.pw.das.dataset.DataSet;
import edu.uiowa.physics.pw.das.dataset.DataSetDescriptor;
import edu.uiowa.physics.pw.das.dataset.TableDataSet;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 *
 * @author Jeremy
 */
public class StreamUtil {
    
    private static final String DATA_SET_ID_PREFIX
            = "class:edu.uiowa.physics.pw.das.stream.test.LocalFileStandardDataStreamSource?file=";
    
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
