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
public class ClassMap<T> implements Map<Class,T> {
    HashMap<Class,T> map;
    
    /** Creates a new instance of ClassMap */
    public ClassMap() {
        map= new HashMap<Class,T>();
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public boolean containsKey(Object key) {
        Object close= closestKey((Class)key);
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
    
    public T get(Object key) {
        Object close= closestKey(key);
        return ( close==null ) ? null : map.get(close);
    }

    public T put(Class key, T value) {
        if ( key.isInterface() ) {
           // System.err.println("interfaces not supported");
        }
        T result= map.get(key);
        map.put( key, value );
        return result;
    }

    public T remove(Object key) {
        return map.remove(key);
    }

    public void putAll(Map t) {
        if ( t instanceof ClassMap ) map.putAll(t);
    }

    public void clear() {
        map.clear();
    }

    public Set<Class> keySet() {
        return map.keySet();
    }

    public Collection values() {
        return map.values();
    }

    public Set entrySet() {
        return map.entrySet();
    }
    
}
