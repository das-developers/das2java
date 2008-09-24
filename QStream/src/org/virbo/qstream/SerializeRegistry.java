/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.qstream;

import java.util.HashMap;
import java.util.Map;
import org.das2.dataset.CacheTag;
import org.das2.datum.EnumerationUnits;
import org.das2.datum.Units;
import org.das2.util.ClassMap;

/**
 *
 * @author jbf
 */
public class SerializeRegistry {
    static Map<Class,SerializeDelegate> delegates= new ClassMap<SerializeDelegate>();
    static Map<String,SerializeDelegate> sdelegates= new HashMap<String,SerializeDelegate>();
    
    static { // while testing, register stuff here.  Normally this is done by the client.
        register( Units.class, new UnitsSerializeDelegate() );
        register( EnumerationUnits.class, new EnumerationUnitsSerializeDelegate() );
        register( String.class, new StringSerializeDelegate() );
        register( CacheTag.class, new CacheTagSerializeDelegate() );
        DefaultSerializeDelegate.registerDelegates();
    }
    
    
    public static void register( Class clas, SerializeDelegate sd ) {
        delegates.put(clas, sd);
        sdelegates.put(sd.typeId(clas), sd);
    }
    
    public static SerializeDelegate getDelegate( Class clas ) {
        return delegates.get(clas);
    }
    
    public static SerializeDelegate getByName( String name ) {
        return sdelegates.get(name);
    }
    
}
