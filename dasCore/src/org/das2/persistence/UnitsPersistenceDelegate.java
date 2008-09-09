/*
 * UnitsPersistenceDelegate.java
 *
 * Created on August 8, 2007, 11:04 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.das2.persistence;

import edu.uiowa.physics.pw.das.datum.Units;
import java.beans.DefaultPersistenceDelegate;
import java.beans.Encoder;
import java.beans.Expression;

/**
 *
 * @author jbf
 */
public class UnitsPersistenceDelegate extends DefaultPersistenceDelegate {
    
    /** Creates a new instance of UnitsPersistenceDelegate */
    public UnitsPersistenceDelegate() {
        
    }
    
    protected Expression instantiate(Object oldInstance, Encoder out) {
        Expression retValue;
        
        Units u= (Units)oldInstance;
        
        return new Expression( u, u.getClass(), "getByName", new Object[] { u.toString() } );
        
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
