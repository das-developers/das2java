/*
 * TableDataSet.java
 *
 * Created on October 24, 2003, 11:29 AM
 */

package edu.uiowa.physics.pw.das.dataset;

import edu.uiowa.physics.pw.das.datum.*;

/**
 *
 * @author  jbf
 */
public class TableDataSet extends DataSet {
    
    /** Creates a new instance of TableDataSet */
    public TableDataSet(DataSetDescriptor dsd) {
        super(dsd);
    }
    public TableDataSet(DataSetDescriptor dsd, Datum startTime, Datum endTime, Datum resolution ) {
        super(dsd, startTime, endTime, resolution );
    }
    
    public TableDataSet( Units xUnits ) {
        super( xUnits );
    }
}
