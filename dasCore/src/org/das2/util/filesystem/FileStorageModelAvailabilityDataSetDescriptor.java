/*
 * FileStorageModelAvailabilityDataSetDescriptor.java
 *
 * Created on May 10, 2006, 4:23 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.das2.util.filesystem;

import edu.uiowa.physics.pw.das.DasException;
import edu.uiowa.physics.pw.das.dataset.CacheTag;
import edu.uiowa.physics.pw.das.dataset.DataSet;
import edu.uiowa.physics.pw.das.dataset.DataSetDescriptor;
import edu.uiowa.physics.pw.das.dataset.VectorDataSetBuilder;
import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.datum.DatumRange;
import edu.uiowa.physics.pw.das.datum.TimeUtil;
import edu.uiowa.physics.pw.das.datum.Units;
import org.das2.util.monitor.DasProgressMonitor;
import edu.uiowa.physics.pw.das.util.TimeParser;
import java.net.URL;

/**
 *
 * @author Jeremy
 */
public class FileStorageModelAvailabilityDataSetDescriptor extends DataSetDescriptor {
    
    FileStorageModelNew fsm;
    
    /** Creates a new instance of FileStorageModelAvailabilityDataSetDescriptor */
    public FileStorageModelAvailabilityDataSetDescriptor( FileStorageModelNew fsm ) {
        this.fsm= fsm;
    }
    
    protected DataSet getDataSetImpl(Datum start, Datum end, Datum resolution, DasProgressMonitor monitor) throws DasException {
        String[] names= fsm.getNamesFor( new DatumRange( start, end ), monitor );
        
        String planeId= "xTagWidth";
        Units xUnits= getXUnits();
        Units offsetUnits= xUnits.getOffsetUnits();
        
        VectorDataSetBuilder builder= new VectorDataSetBuilder( xUnits, offsetUnits );
        builder.addPlane( planeId, offsetUnits );
        builder.setProperty( DataSet.PROPERTY_CACHE_TAG, new CacheTag( start, end, null )  );
        for ( int i=0; i<names.length; i++ ) {
            DatumRange range= fsm.getRangeFor(names[i]);
            builder.insertY( range.min().doubleValue(xUnits),
                    range.width().doubleValue(offsetUnits),
                    planeId,
                    range.width().doubleValue(offsetUnits) );
        }
        
        return builder.toVectorDataSet();
    }
    
    public Units getXUnits() {
        return Units.us2000;
    }
    
    public static void main( String[] args ) throws Exception {
        
        FileSystem fs;
        fs = FileSystem.create(new URL("http://www-pw.physics.uiowa.edu/~jbf/cluster/obtdata/"));
        
        TimeParser.FieldHandler hexHandler= new TimeParser.FieldHandler() {
            public void handleValue(String fieldContent, TimeUtil.TimeStruct startTime, TimeUtil.TimeStruct timeWidth) {
                int i= Integer.decode("0x"+fieldContent).intValue();
                double seconds= 86400 * i / 256;
                startTime.seconds= seconds;
                timeWidth.seconds= 86400 / 256.;
            }
        };
        
        int cl= 4;
        FileStorageModelNew fsm= FileStorageModelNew.create( fs, "%y%2m/%y%2m%2d%2{hex}\\..C"+cl, "hex", hexHandler );

    }
}
