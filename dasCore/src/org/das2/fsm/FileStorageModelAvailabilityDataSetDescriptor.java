/*
 * FileStorageModelAvailabilityDataSetDescriptor.java
 *
 * Created on May 10, 2006, 4:23 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.das2.fsm;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.util.filesystem.FileSystem;
import org.das2.DasException;
import org.das2.dataset.CacheTag;
import org.das2.dataset.DataSet;
import org.das2.dataset.DataSetDescriptor;
import org.das2.dataset.VectorDataSetBuilder;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import org.das2.util.monitor.ProgressMonitor;
import org.das2.util.TimeParser;
import java.net.URL;
import org.das2.datum.CalendarTime;

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
    
    protected DataSet getDataSetImpl(Datum start, Datum end, Datum resolution, ProgressMonitor monitor) throws DasException {
        try {
            String[] names = fsm.getNamesFor(new DatumRange(start, end), monitor);

            String planeId = "xTagWidth";
            Units xUnits = getXUnits();
            Units offsetUnits = xUnits.getOffsetUnits();

            VectorDataSetBuilder builder = new VectorDataSetBuilder(xUnits, offsetUnits);
            builder.addPlane(planeId, offsetUnits);
            builder.setProperty(DataSet.PROPERTY_CACHE_TAG, new CacheTag(start, end, null));
            for (int i = 0; i < names.length; i++) {
                DatumRange range = fsm.getRangeFor(names[i]);
                builder.insertY(range.min().doubleValue(xUnits), range.width().doubleValue(offsetUnits), planeId, range.width().doubleValue(offsetUnits));
            }

            return builder.toVectorDataSet();
        } catch (IOException ex) {
            throw new DasException(ex);
        }
    }
    
    public Units getXUnits() {
        return Units.us2000;
    }
    
    public static void main( String[] args ) throws Exception {
        
        FileSystem fs;
        fs = FileSystem.create(new URL("http://www-pw.physics.uiowa.edu/~jbf/cluster/obtdata/"));
        
        TimeParser.FieldHandler hexHandler= new TimeParser.FieldHandler() {
            public void handleValue(String fieldContent, CalendarTime startTime, int[] timeWidth) {
                int i= Integer.decode("0x"+fieldContent).intValue();
                double seconds = (86400 * i) / 256;
                startTime.nanosecond= (long) seconds * 1000000000;
					 startTime.normalize();
					 startTime = startTime.step(CalendarTime.MILLISEC, (int)(86400000 / 256.));
            }
        };
        
        int cl= 4;
        FileStorageModelNew fsm= FileStorageModelNew.create(fs, "%y%2m/%y%2m%2d%2{hex}\\..C"+cl,
			                                                   "hex", hexHandler );

    }
}
