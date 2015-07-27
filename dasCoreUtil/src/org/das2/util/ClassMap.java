/*
 * ClassMap.java
 *
 * Created on April 28, 2006, 2:11 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.das2.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

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
    
    /**
     * return the object or null for this string  "RED" -&gt; Color.RED
     * @param c the class defining the target type
     * @param ele the string representation to be interpreted for this type.
     * @return the instance of the type.
     */
    public static Object getEnumElement( Class c, String ele ) {
        int PUBLIC_STATIC_FINAL = Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL;
        //List lvals;
        if (c.isEnum()) {
            Object[] vals = c.getEnumConstants();
            for (Object o : vals) {
                Enum e = (Enum) o;
                if ( e.toString().equalsIgnoreCase(ele) ) return e;
            }
            //lvals= Arrays.asList(vals);
        } else {
            Field[] fields = c.getDeclaredFields();
            //lvals= new ArrayList();
            for ( Field f: fields ) {
                try {
                    String name = f.getName();
                    if ( ( ( f.getModifiers() & PUBLIC_STATIC_FINAL) == PUBLIC_STATIC_FINAL ) ) {
                        Object value = f.get(null);
                        if ( value!=null && c.isInstance(value) ) {
                            //lvals.add(value);
                            if ( name.equalsIgnoreCase(ele) || value.toString().equalsIgnoreCase(ele) ) {
                               return value;
                            }
                        }
                    }
                } catch (IllegalAccessException iae) {
                    IllegalAccessError err = new IllegalAccessError(iae.getMessage());
                    err.initCause(iae);
                    throw err;
                }
            }
        }
        //logger.log( Level.INFO, "looking for {0}, found {1}\n", new Object[]{ele, lvals.toString()});
        return null;
    }

}
