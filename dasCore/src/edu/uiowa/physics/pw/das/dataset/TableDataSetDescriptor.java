/*
 * TableDataSetDescriptor.java
 *
 * Created on October 23, 2003, 12:32 PM
 */

package edu.uiowa.physics.pw.das.dataset;

import edu.uiowa.physics.pw.das.datum.*;

/**
 *
 * @author  jbf
 */
public abstract class TableDataSetDescriptor {
    
    public abstract Units getYUnits();
    public abstract Units getZUnits();
    
    /** Creates a new instance of TableDataSetDescriptor */
    public TableDataSetDescriptor() {
    }
    
}
