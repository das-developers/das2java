/*
 * VectorDataSet.java
 *
 * Created on October 24, 2003, 11:29 AM
 */

package edu.uiowa.physics.pw.das.dataset;

import edu.uiowa.physics.pw.das.datum.*;

/**
 *
 * @author  jbf
 */
public class VectorDataSet extends DataSet {
    
    /** Creates a new instance of VectorDataSet */
    public VectorDataSet(DataSetDescriptor dsd) {
        super(dsd);        
    }
    
    public VectorDataSet(DataSetDescriptor dsd, Datum startTime, Datum endTime, Datum resolution ) {
        super(dsd, startTime, endTime, resolution );
    }
    
    public VectorDataSet( Units xUnits ) {
        super( xUnits );
    }
  
}
