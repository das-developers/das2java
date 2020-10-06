/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.qstream;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import org.das2.datum.CacheTag;
import org.das2.datum.DatumRange;
import org.das2.datum.EnumerationUnits;
import org.das2.datum.Units;
import org.das2.util.ClassMap;
import org.das2.qds.RankZeroDataSet;

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
        register( AbstractMap.class, new MapSerializeDelegate() );
        register( RankZeroDataSet.class, new Rank0DataSetSerializeDelegate() );
        register( DatumRange.class, new DatumRangeSerializeDelegate() );
        DefaultSerializeDelegate.registerDelegates();
    }
    
    private static NumberArraySerializeDelegate numsd= new NumberArraySerializeDelegate();

    /**
     * Clients like SerializeUtil are able to register methods for
     * serializing additional classes.
     * @param clas the class
     * @param sd the delegate
     * @see org.autoplot.state.SerializeUtil in the Autoplot project.
     */
    public static void register( Class clas, SerializeDelegate sd ) {
        delegates.put(clas, sd);
        sdelegates.put(sd.typeId(clas), sd);
    }
    
    /**
     * returns a delegate or null if the class is not supported.
     * @param clas the class
     * @return the delegate which can be used to format and parse the class
     */
    public static SerializeDelegate getDelegate( Class clas ) {
        if ( clas.isArray() && clas.getComponentType().isPrimitive() ) {
            return numsd;
        } else {
            return delegates.get(clas);
        }
    }
    
    /**
     * return the delegate without using a class instance.
     * @param name class name like "org.das2.
     * @return 
     */
    public static SerializeDelegate getByName( String name ) {
        if ( name.equals(NumberArraySerializeDelegate.TYPE_NUMBER_ARRAY) ) {
            return numsd;
        } else {
            return sdelegates.get(name);
        }
    }
    
}
