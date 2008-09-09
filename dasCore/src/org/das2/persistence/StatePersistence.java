/*
 * StatePersistence.java
 *
 * Created on August 8, 2007, 10:47 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.das2.persistence;

import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import java.beans.ExceptionListener;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Provides object serialization, using delegates to handle das2 immutable objects.
 * @author jbf
 */
public class StatePersistence {
    
    private StatePersistence() {
    }

    public static void saveState( OutputStream out, Object state ) throws IOException {
        XMLEncoder e = new XMLEncoder(
                new BufferedOutputStream(
                out ) );
        
        e.setPersistenceDelegate( DatumRange.class, new DatumRangePersistenceDelegate() );
        e.setPersistenceDelegate( Units.class, new UnitsPersistenceDelegate() );
        e.setPersistenceDelegate( Datum.class, new DatumPersistenceDelegate() );
        
        e.setExceptionListener( new ExceptionListener() {
            public void exceptionThrown(Exception e) {
                e.printStackTrace();
            }
        } );
        e.writeObject(state);
        e.close();                
    }
    
    public static Object restoreState( InputStream in )  throws IOException {
        XMLDecoder decode= new XMLDecoder( in );
        return decode.readObject();
    }
}
