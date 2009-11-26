package org.das2;


import java.util.HashMap;
import java.util.Map;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author jbf
 */
public class DebuggerVars {
    static Map<Object,Object> vars= new HashMap();
    public static void put(String name,Object val) { vars.put(name, val); }
    public static Object get( String name ) { return vars.get(name); }
    public static Object remove( String name ) { return vars.remove(name); }
}
