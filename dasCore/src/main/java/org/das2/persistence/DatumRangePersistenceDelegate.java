/*
 * DatumRangePersistenceDelegate.java
 *
 * Created on August 8, 2007, 10:43 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.das2.persistence;

import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import java.beans.DefaultPersistenceDelegate;
import java.beans.Encoder;
import java.beans.Expression;

/**
 *
 * @author jbf
 */
public class DatumRangePersistenceDelegate extends DefaultPersistenceDelegate {
    
    /** Creates a new instance of DatumRangePersistenceDelegate */
    public DatumRangePersistenceDelegate()  {
    }

    protected Expression instantiate(Object oldInstance, Encoder out) {
        Expression retValue;
        
        DatumRange field= (DatumRange)oldInstance;
        Units u= field.getUnits();
        
        return new Expression( field, this.getClass(), "newDatumRange", new Object[] { field.min().doubleValue(u), field.max().doubleValue(u), u.toString() } );
        
    }
    
    public static DatumRange newDatumRange( double min, double max, String units ) {
        Units u= Units.lookupUnits(units);
        return DatumRange.newDatumRange( min, max, u );
    }

    protected void initialize(Class<?> type, Object oldInstance, Object newInstance, Encoder out) {
        super.initialize(type, oldInstance, newInstance, out);
    }
}
