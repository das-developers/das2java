/*
 * DatumRangePersistenceDelegate.java
 *
 * Created on August 8, 2007, 10:43 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.das2.persistence;

import org.das2.datum.Datum;
import org.das2.datum.Units;
import java.beans.DefaultPersistenceDelegate;
import java.beans.Encoder;
import java.beans.Expression;

/**
 *
 * @author jbf
 */
public class DatumPersistenceDelegate extends DefaultPersistenceDelegate {
    
    /** Creates a new instance of DatumRangePersistenceDelegate */
    public DatumPersistenceDelegate()  {
    }

    protected Expression instantiate(Object oldInstance, Encoder out) {
        Expression retValue;
        
        Datum field= (Datum)oldInstance;
        Units u= field.getUnits();
        
        return new Expression( field, this.getClass(), "newDatum", new Object[] { field.doubleValue(u), u.toString() } );
        
    }
    
    public static Datum newDatum( double val, String units ) {
        Units u= Units.lookupUnits(units);
        return u.createDatum( val );
    }

    protected void initialize(Class<?> type, Object oldInstance, Object newInstance, Encoder out) {
        super.initialize(type, oldInstance, newInstance, out);
    }

    public void writeObject(Object oldInstance, Encoder out) {
        super.writeObject(oldInstance, out);
    }

    protected boolean mutatesTo(Object oldInstance, Object newInstance) {
        boolean retValue;
        
        retValue = super.mutatesTo(oldInstance, newInstance);
        return retValue;
    }
}
