/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.qstream;

/**
 * Handles the base types, Number, etc.
 * @author jbf
 */
public abstract class DefaultSerializeDelegate implements SerializeDelegate {

    static void registerDelegates() {
        SerializeRegistry.register( java.lang.Boolean.class, new Boolean() );
        SerializeRegistry.register( java.lang.Integer.class, new Integer() );
        SerializeRegistry.register( java.lang.Long.class, new Long() );
        SerializeRegistry.register( java.lang.Float.class, new Float() );
        SerializeRegistry.register( java.lang.Double.class, new Double() );
        SerializeRegistry.register( java.lang.Short.class, new Short() );
        SerializeRegistry.register( java.lang.Byte.class, new Byte() );
        SerializeRegistry.register( java.lang.Number.class, new Number() );
        SerializeRegistry.register( boolean.class, new Boolean() );
        SerializeRegistry.register( int.class, new Integer() );
        SerializeRegistry.register( long.class, new Long() );
        SerializeRegistry.register( float.class, new Float() );
        SerializeRegistry.register( double.class, new Double() );
        SerializeRegistry.register( short.class, new Short() );
        SerializeRegistry.register( byte.class, new Byte() );

    }

    public String format(Object o) {
        return o.toString();
    }

    public abstract Object parse(String typeId, String s);

    public String typeId(Class clas) {
        String s = clas.getName();
        if (s.startsWith("java.lang.")) s = s.substring(10);
        return s;
    }

    public static class Boolean extends DefaultSerializeDelegate {
        @Override
        public Object parse(String typeId, String s) {
            return java.lang.Boolean.parseBoolean(s);
        }
    }
    
    public static class Integer extends DefaultSerializeDelegate {
        @Override
        public Object parse(String typeId, String s) {
            return java.lang.Integer.parseInt(s);
        }
    }
    public static class Long extends DefaultSerializeDelegate {
        @Override
        public Object parse(String typeId, String s) {
            return java.lang.Long.parseLong(s);
        }
    }
    public static class Float extends DefaultSerializeDelegate {
        @Override
        public Object parse(String typeId, String s) {
            return java.lang.Float.parseFloat(s);
        }
    }
    public static class Double extends DefaultSerializeDelegate {
        @Override
        public Object parse(String typeId, String s) {
            return java.lang.Double.parseDouble(s);
        }
    }
    public static class Number extends DefaultSerializeDelegate {
        @Override
        public Object parse(String typeId, String s) {
            return java.lang.Double.parseDouble(s);
        }
    }
    public static class Short extends DefaultSerializeDelegate {
        @Override
        public Object parse(String typeId, String s) {
            return java.lang.Short.parseShort(s);
        }
    }
    public static class Byte extends DefaultSerializeDelegate {
        @Override
        public Object parse(String typeId, String s) {
            return java.lang.Byte.parseByte(s);
        }
    }
    
}
