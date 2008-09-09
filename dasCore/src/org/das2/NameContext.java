/* File: NameContext.java
 * Copyright (C) 2002-2003 The University of Iowa
 * Created by: Jeremy Faden <jbf@space.physics.uiowa.edu>
 *             Jessica Swanner <jessica@space.physics.uiowa.edu>
 *             Edward E. West <eew@space.physics.uiowa.edu>
 *
 * This file is part of the das2 library.
 *
 * das2 is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.das2;

import org.das2.beans.BeansUtil;
import edu.uiowa.physics.pw.das.dasml.ParsedExpression;
import edu.uiowa.physics.pw.das.dasml.ParsedExpressionException;
import edu.uiowa.physics.pw.das.datum.*;

import java.beans.*;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** An instance of <code>NameContext</code> defines the name space for a
 * dasml/das2 application.  Methods for querying values of properties are
 * also provided.
 *
 * @author  eew
 */
public class NameContext {
    
    private static final String SIMPLE_NAME_STRING = "[A-Za-z][A-Za-z0-9_]*";
    private static final String INDEX_STRING = "0|[1-9][0-9]*";
    private static final String INDEXED_NAME_STRING
    = "(" + SIMPLE_NAME_STRING + ")" + "\\[(" + INDEX_STRING + ")\\]";
    private static final String QUALIFIED_NAME_STRING
    = SIMPLE_NAME_STRING + "(\\." + SIMPLE_NAME_STRING + "|\\." + INDEXED_NAME_STRING + ")*";
    
    public static final Pattern SIMPLE_NAME = Pattern.compile(SIMPLE_NAME_STRING);
    public static final Pattern INDEXED_NAME = Pattern.compile(INDEXED_NAME_STRING);
    public static final Pattern QUALIFIED_NAME = Pattern.compile(QUALIFIED_NAME_STRING);
    public static final Pattern refPattern = Pattern.compile("\\$\\{([^\\}]+)\\}");
    public static final Pattern intPattern = Pattern.compile("-?(0|[1-9][0-9]*)");
    public static final Pattern floatPattern = Pattern.compile("-?[0-9]*(\\.[0-9]*)?([eE]-?[0-9]+)?");
    
    
    private Map nameMap;
    private Map propertyMap;
    
    /** Creates a new instance of NameContext */
    NameContext() {
        nameMap = new HashMap();
        propertyMap = new HashMap();
    }
    
    /** Associates a value with a name in this context.  The <code>name</code>
     * parameter must being with a letter and can only consist of alphanumeric
     * characters and '_'.
     * @param name the name for the value to be associated with
     * @param value the value being named
     */
    public void put(String name, Object value) throws DasNameException {
        Matcher m = SIMPLE_NAME.matcher(name);
        if (m.matches()) {
            nameMap.put(name, new WeakReference( value ) );
        }
        else {
            throw new DasNameException(name + " must match " + SIMPLE_NAME_STRING);
        }
    }
    
    public Object get(String name) throws DasPropertyException, InvocationTargetException {
        if (name == null) {
            throw new NullPointerException("name");
        }
        Matcher m = SIMPLE_NAME.matcher(name);
        if (m.matches()) {
            return ((WeakReference)nameMap.get(name)).get();
        }
        int index = name.lastIndexOf('.');
        Object obj = get(name.substring(0, index));
        String property = name.substring(index + 1);
        m = INDEXED_NAME.matcher(property);
        if (m.matches()) {
            property = m.group(1);
            index = Integer.parseInt(m.group(2));
            return getIndexedPropertyValue(obj, property, index);
        }
        else {
            return getPropertyValue(obj, property);
        }
    }
    
    public void set(String name, Object value) throws InvocationTargetException, ParsedExpressionException, DasPropertyException, DasNameException {
        if (name == null) {
            throw new NullPointerException("name");
        }
        Matcher m = SIMPLE_NAME.matcher(name);
        if (m.matches()) {
            put(name, value);
        }
        int index = name.lastIndexOf('.');
        Object obj = get(name.substring(0, index));
        String property = name.substring(index + 1);
        m = INDEXED_NAME.matcher(property);
        if (m.matches()) {
            property = m.group(1);
            index = Integer.parseInt(m.group(2));
            setIndexedPropertyValue(obj, property, index, value);
        }
        else {
            setPropertyValue(obj, property, value);
        }
    }
    
    public Object getPropertyValue(Object obj, String property) throws DasPropertyException, InvocationTargetException {
        try {
            Class type = obj.getClass();
            maybeLoadPropertiesForClass(type);
            Map map = (Map)propertyMap.get(type);
            PropertyDescriptor pd = (PropertyDescriptor)map.get(property);
            if (pd == null) {
                throw new DasPropertyException(DasPropertyException.NOT_DEFINED, null, property);
            }
            Method readMethod = pd.getReadMethod();
            return readMethod.invoke(obj);
        }
        catch (IllegalAccessException iae) {
            throw new RuntimeException(iae);
        }
    }
    
    public Object getIndexedPropertyValue(Object obj, String property, int index) throws DasPropertyException, InvocationTargetException {
        try {
            Class type = obj.getClass();
            maybeLoadPropertiesForClass(type);
            Map map = (Map)propertyMap.get(type);
            PropertyDescriptor pd = (PropertyDescriptor)map.get(property);
            if (pd == null) {
                throw new DasPropertyException(DasPropertyException.NOT_DEFINED, null, property);
            }
            if (!(pd instanceof IndexedPropertyDescriptor)) {
                throw new DasPropertyException(DasPropertyException.NOT_INDEXED, null, property);
            }
            IndexedPropertyDescriptor ipd = (IndexedPropertyDescriptor)pd;
            Method readMethod = ipd.getIndexedReadMethod();
            return readMethod.invoke(obj, new Object[] { new Integer(index) });
        }
        catch (IllegalAccessException iae) {
            throw new RuntimeException(iae);
        }
    }
    
    public void setPropertyValue(Object obj, String property, Object value) throws InvocationTargetException, ParsedExpressionException, DasPropertyException {
        try {
            Class type = obj.getClass();
            maybeLoadPropertiesForClass(type);
            Map map = (Map)propertyMap.get(type);
            PropertyDescriptor pd = (PropertyDescriptor)map.get(property);
            if (pd == null) {
                throw new DasPropertyException(DasPropertyException.NOT_DEFINED, null, property);
            }
            Method writeMethod = pd.getWriteMethod();
            if (writeMethod == null) {
                throw new DasPropertyException(DasPropertyException.READ_ONLY, null, property);
            }
            Class propertyType = pd.getPropertyType();
            if (value instanceof String) {
                value = parseValue((String)value, propertyType);
            }
            if (!propertyType.isInstance(value)
            && !(propertyType == boolean.class && value instanceof Boolean)
            && !(propertyType == char.class && value instanceof Character)
            && !(propertyType == double.class && value instanceof Double)
            && !(propertyType == short.class && value instanceof Short)
            && !(propertyType == int.class && value instanceof Integer)
            && !(propertyType == float.class && value instanceof Float)
            && !(propertyType == byte.class && value instanceof Byte)
            && !(propertyType == long.class && value instanceof Long)) {
                throw new DasPropertyException(DasPropertyException.TYPE_MISMATCH, null, property);
            }
            writeMethod.invoke(obj, new Object[] { value } );
        }
        catch (IllegalAccessException iae) {
            throw new RuntimeException(iae);
        }
    }
    
    public void setIndexedPropertyValue(Object obj, String property, int index, Object value) throws InvocationTargetException, ParsedExpressionException, DasPropertyException {
        try {
            Class type = obj.getClass();
            maybeLoadPropertiesForClass(type);
            Map map = (Map)propertyMap.get(type);
            PropertyDescriptor pd = (PropertyDescriptor)map.get(property);
            if (pd == null) {
                throw new DasPropertyException(DasPropertyException.NOT_DEFINED, null, property);
            }
            if (!(pd instanceof IndexedPropertyDescriptor)) {
                throw new DasPropertyException(DasPropertyException.NOT_INDEXED, null, property);
            }
            IndexedPropertyDescriptor ipd = (IndexedPropertyDescriptor)pd;
            Method writeMethod = ipd.getIndexedWriteMethod();
            if (writeMethod == null) {
                throw new DasPropertyException(DasPropertyException.READ_ONLY, null, property);
            }
            Class propertyType = pd.getPropertyType();
            if (value instanceof String) {
                value = parseValue((String)value, propertyType);
            }
            if (!propertyType.isInstance(value)
            && !(propertyType == boolean.class && value instanceof Boolean)
            && !(propertyType == char.class && value instanceof Character)
            && !(propertyType == double.class && value instanceof Double)
            && !(propertyType == short.class && value instanceof Short)
            && !(propertyType == int.class && value instanceof Integer)
            && !(propertyType == float.class && value instanceof Float)
            && !(propertyType == byte.class && value instanceof Byte)
            && !(propertyType == long.class && value instanceof Long)) {
                throw new DasPropertyException(DasPropertyException.TYPE_MISMATCH, null, property);
            }
            writeMethod.invoke(obj, new Object[] { new Integer(index), value });
        }
        catch (IllegalAccessException iae) {
            throw new RuntimeException(iae);
        }
    }
    
    private void maybeLoadPropertiesForClass(Class cl) {
        try {
            if (propertyMap.get(cl) == null) {
                BeanInfo info = BeansUtil.getBeanInfo(cl);
                HashMap map = new HashMap();
                PropertyDescriptor[] properties = info.getPropertyDescriptors();
                for (int i = 0; i < properties.length; i++) {
                    if (properties[i].getReadMethod() == null) {
                        continue;
                    }
                    if (properties[i] instanceof IndexedPropertyDescriptor) {
                        IndexedPropertyDescriptor ipd = (IndexedPropertyDescriptor)properties[i];
                        if (ipd.getIndexedReadMethod() == null) {
                            continue;
                        }
                    }
                    map.put(properties[i].getName(), properties[i]);
                }
                propertyMap.put(cl, map);
            }
        }
        catch (IntrospectionException ie) {
        }
    }
    
    public void remove(String name) {
        nameMap.remove(name);
    }
    
    /**
     * Parses the given <code>String</code> object in an attempt to
     * produce the an object of the given type.
     *
     * @param valueString the given <code>String</code>
     * @param type the given type
     */
    public Object parseValue(String valueString, Class type) throws edu.uiowa.physics.pw.das.dasml.ParsedExpressionException, InvocationTargetException, DasPropertyException {
        Object parsedValue;
        valueString = replaceReferences(valueString);
        if (type == String.class) {
            return valueString;
        }
        else if (type == boolean.class || type == Boolean.class) {
            if (valueString.equals("true")) return Boolean.TRUE;
            if (valueString.equals("false")) return Boolean.FALSE;
            ParsedExpression exp = new ParsedExpression(valueString);
            Object o = exp.evaluate(this);
            if (!(o instanceof Boolean)) throw new ParsedExpressionException("'" + valueString + "' does not evaluate to a boolean value");
            return o;
        }
        else if (type == int.class || type == Integer.class) {
            if (intPattern.matcher(valueString).matches()) {
                return new Integer(valueString);
            }
            ParsedExpression exp = new ParsedExpression(valueString);
            Object o = exp.evaluate(this);
            if (!(o instanceof Number)) throw new ParsedExpressionException("'" + valueString + "' does not evaluate to a numeric value");
            return (o instanceof Integer ? (Integer)o : new Integer(((Number)o).intValue()));
        }
        else if (type == long.class || type == Long.class) {
            if (intPattern.matcher(valueString).matches()) {
                parsedValue = new Long(valueString);
            }
            ParsedExpression exp = new ParsedExpression(valueString);
            Object o = exp.evaluate(this);
            if (!(o instanceof Number)) throw new ParsedExpressionException("'" + valueString + "' does not evaluate to a numeric value");
            return new Long(((Number)o).longValue());
        }
        else if (type == float.class || type == Float.class) {
            if (floatPattern.matcher(valueString).matches()) {
                parsedValue = new Float(valueString);
            }
            ParsedExpression exp = new ParsedExpression(valueString);
            Object o = exp.evaluate(this);
            if (!(o instanceof Number)) throw new ParsedExpressionException("'" + valueString + "' does not evaluate to a numeric value");
            return new Float(((Number)o).floatValue());
        }
        else if (type == double.class || type == Double.class) {
            if (floatPattern.matcher(valueString).matches()) {
                parsedValue = new Double(valueString);
            }
            ParsedExpression exp = new ParsedExpression(valueString);
            Object o = exp.evaluate(this);
            if (!(o instanceof Number)) throw new ParsedExpressionException("'" + valueString  + "' does not evaluate to a numeric value");
            return (o instanceof Double ? (Double)o : new Double(((Number)o).doubleValue()));
        }
        else if (type == Datum.class) {
            try {
                return TimeUtil.create(valueString);
            }
            catch ( java.text.ParseException ex ) {
                try {
                    return Datum.create(Double.parseDouble(valueString));
                }
                catch (NumberFormatException iae) {
                    throw new ParsedExpressionException(valueString + " cannot be parsed as a Datum");
                }
            }
            
        }
        else {
            throw new IllegalStateException(type.getName() + " is not a recognized type");
        }
    }
    
    protected String replaceReferences(String str) throws DasPropertyException, InvocationTargetException {
        Matcher matcher = refPattern.matcher(str);
        while (matcher.find()) {
            String name = matcher.group(1).trim();
            Object value = get(name);
            str = matcher.replaceFirst(value.toString());
            matcher.reset(str);
        }
        return str;
    }
    
    public String toString() {
        return getClass().getName() + nameMap.keySet().toString();
    }
}
