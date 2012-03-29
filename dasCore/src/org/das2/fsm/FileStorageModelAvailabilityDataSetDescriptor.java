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
import java.net.URI;
import java.util.Map;
import org.das2.datum.TimeUtil.TimeStruct;
import org.das2.util.filesystem.FileSystem;
import org.das2.DasException;
import org.das2.dataset.CacheTag;
import org.das2.dataset.DataSet;
import org.das2.dataset.DataSetDescriptor;
import org.das2.dataset.VectorDataSetBuilder;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.TimeUtil;
import org.das2.datum.Units;
import org.das2.util.monitor.ProgressMonitor;
import org.das2.datum.TimeParser;

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
        fs = FileSystem.create(new URI("http://www-pw.physics.uiowa.edu/~jbf/cluster/obtdata/"));
        
        TimeParser.FieldHandler hexHandler= new TimeParser.FieldHandler() {
            
            public String configure( Map<String,String> args ) { return null; }

            public void parse(String fieldContent, TimeUtil.TimeStruct startTime, TimeUtil.TimeStruct timeWidth, Map<String,String> extra ) {
                int i= Integer.decode("0x"+fieldContent).intValue();
                double seconds= 86400 * i / 256;
                startTime.seconds= seconds;
                timeWidth.seconds= 86400 / 256.;
            }

            public String getRegex() {
                return "..";
            }

            public String format(TimeStruct startTime, TimeStruct timeWidth, int length, Map<String, String> extra) throws IllegalArgumentException {
                if ( length!=2 ) {
                    throw new IllegalArgumentException("length must be 2");
                }
                double seconds= startTime.seconds;
                if ( seconds<0 || seconds>=86400 ) throw new IllegalArgumentException("seconds of startTime must be between 0 and 86400");
                int i= (int)( seconds * 256 / 86400 );
                String r= String.format( "%2X", i );
                return r;
            }
            
        };
        
        int cl= 4;
        FileStorageModelNew fsm= FileStorageModelNew.create( fs, "%y%2m/%y%2m%2d%2{hex}\\..C"+cl, "hex", hexHandler );

    }
}
