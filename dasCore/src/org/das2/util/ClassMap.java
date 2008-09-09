/*
 * ClassMap.java
 *
 * Created on April 28, 2006, 2:11 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.das2.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Map that takes a Class for keys, and the get method finds the closest matching class.
 * @author Jeremy
 */
public class ClassMap implements Map {
    HashMap map;
    
    /** Creates a new instance of ClassMap */
    public ClassMap() {
        map= new HashMap();
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public boolean containsKey(Object key) {
        Object close= closestKey(key);
        return close!=null;
    }

    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    private Object closestKey( Object key ) {
        Object result= key;
        if ( map.containsKey(result) ) return result; 
        Class clas= (Class)key;
        while ( clas!=null ) {
            if ( map.containsKey(clas) ) return clas;
            clas= clas.getSuperclass();
        }
        return clas;
    }
    
    public Object get(Object key) {
        Object close= closestKey(key);
        return ( close==null ) ? null : map.get(close);
    }

    public Object put(Object key, Object value) {
        return map.put( key, value );
    }

    public Object remove(Object key) {
        return map.remove(key);
    }

    public void putAll(Map t) {
        if ( t instanceof ClassMap ) map.putAll(t);
    }

    public void clear() {
        map.clear();
    }

    public Set keySet() {
        return map.keySet();
    }

    public Collection values() {
        return map.values();
    }

    public Set entrySet() {
        return map.entrySet();
    }
    
}
