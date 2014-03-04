/* File: AccessLevelBeanInfo.java
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

package org.das2.beans;

import org.das2.DasApplication;
import java.beans.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class is designed to implement access levels for bean properties.
 * The system property "edu.uiowa.physics.das.beans.AccessLevelBeanInfo.AccessLevel" will
 * determine the access level of the bean.  The access levels that are currently supported
 * are "ALL" and "END_USER".  The access level must be set prior to this class being loaded.
 *
 * @author Edward West
 */
public abstract class AccessLevelBeanInfo extends SimpleBeanInfo {
    
    /**
     * Type-safe enumeration class used to specify access levels
     * for bean properties.
     *
     * **NOTE TO DEVELOPERS**
     * The order parameter for the constructor should not be specified as a negative number
     */
    public static class AccessLevel implements Comparable {
        public static final AccessLevel ALL = new AccessLevel("ALL", 0);
        public static final AccessLevel DASML = new AccessLevel("DASML", 1000);
        public static final AccessLevel END_USER = new AccessLevel("END_USER", 0x7FFF0000);
        private String str;
        private int order;
        private AccessLevel(String str, int order) {
            this.str = str; this.order = order;
        }
        public int compareTo(Object o) {
            return order - ((AccessLevel)o).order;
        }
        @Override
        public boolean equals(Object o) {
            if ( !( o instanceof PersistenceLevel ) ) return false;
            return order==((PersistenceLevel)o).order;
        }
        @Override
        public int hashCode() {
            return this.order;
        }        
        @Override
        public String toString() {
            return str;
        }
    }
    
    /**
     * this level indicates what persistence is allowed.
     */
    public static class PersistenceLevel implements Comparable {
        public static final PersistenceLevel NONE = new PersistenceLevel( "NONE", 0);
        public static final PersistenceLevel TRANSIENT = new PersistenceLevel( "TRANSIENT", 1000 );
        public static final PersistenceLevel PERSISTENT = new PersistenceLevel( "PERSISTENT", 2000);
        
        private String str;
        private int order;
        private PersistenceLevel(String str, int order) {
            this.str = str; this.order = order;
        }
        public int compareTo(Object o) {
            return order - ((PersistenceLevel)o).order;
        }
        @Override
        public boolean equals(Object o) {
            if ( !( o instanceof PersistenceLevel ) ) return false;
            return order==((PersistenceLevel)o).order;
        }
        @Override
        public int hashCode() {
            return this.order;
        }
        @Override
        public String toString() {
            return str;
        }
    }
    
    private Property[] properties;
    private Class beanClass;
    private static AccessLevel accessLevel;
    private static Object lockObject = new Object();
    
    static {
        String level = DasApplication.getProperty("edu.uiowa.physics.das.beans.AccessLevelBeanInfo.AccessLevel",null);
        if (level==null) {
            accessLevel = AccessLevel.ALL;
        } else if (level.equals("ALL")) {
            accessLevel = AccessLevel.ALL;
        } else if (level.equals("DASML")) {
            accessLevel = AccessLevel.DASML;
        } else if (level.equals("END_USER")) {
            accessLevel = AccessLevel.END_USER;
        } else {
            accessLevel = AccessLevel.ALL;
        }
    }
    
    /**
     * Returns the access level for AccessLevelBeanInfo objects.
     */
    public static AccessLevel getAccessLevel() {
        return accessLevel;
    }
    
    /**
     * Sets the access level for AccessLevelBeanInfo objects.
     */
    public static void setAccessLevel(AccessLevel level) {
        synchronized (lockObject) {
            accessLevel = level;
        }
    }
    
    public static Object getLock() {
        return lockObject;
    }
    
    /**
     * Creates and instance of AccessLevelBeanInfo.
     * Each element of the <code>properties</code> array must be of the type
     * <code>Object[]</code> with the following format:
     * <code>{ propertyName, accessorMethod, mutatorMethod, accessLevel}</code>
     * where the elements have the following meaning.
     * <ul>
     * <li><code>propertyName</code> - A <code>String</code> naming the property being specified.</li>
     * <li><code>accessorMethod</code> - A <code>String</code> specifying the name of the read method
     * for this property.</li>
     * <li><code>mutatorMethod</code> - A <code>String</code> specifying the name of the write method
     * for this property</li>
     * <li><code>accessLevel</code> - A <code>org.das2.beans.AccessLevelBeanInfo.AccessLevel</code> instance specifying
     * the access level for this property.</li>
     * </ul>
     */
    protected AccessLevelBeanInfo(Property[] properties, Class beanClass) {
        this.properties = properties;
        this.beanClass = beanClass;
    }
    
    /**
     * convenient method that only returns the descriptors for the specified persistence level.
     * Also implements the property inheritance.
     */
    public PropertyDescriptor[] getPropertyDescriptors( PersistenceLevel persistenceLevel ) {
        synchronized (lockObject) {
            try {
                ArrayList result= new ArrayList();
                int propertyIndex = 0;
                for (int index = 0; index < properties.length; index++) {
                    if (persistenceLevel.compareTo(properties[index].getPersistenceLevel()) <= 0) {
                        result.add( properties[index].getPropertyDescriptor(beanClass) );
                    }
                }
                BeanInfo[] moreBeanInfos= getAdditionalBeanInfo() ;
                if ( moreBeanInfos!=null ) {
                    for ( int i=0; i<moreBeanInfos.length; i++ ) {
                        result.addAll( Arrays.asList( moreBeanInfos[i].getPropertyDescriptors() ) );
                    }
                }
                return (PropertyDescriptor[]) result.toArray( new PropertyDescriptor[ result.size() ] );
                
            } catch (IntrospectionException ie) {
                throw new IllegalStateException(ie.getMessage());
            }
        }
    }

    public PropertyDescriptor[] getPropertyDescriptors() {
        synchronized (lockObject) {
            try {
                int count = 0;
                for (int index = 0; index < properties.length; index++) {
                    if (accessLevel.compareTo(properties[index].getLevel()) <= 0) {
                        count++;
                    }
                }
                PropertyDescriptor[] descriptors = new PropertyDescriptor[count];
                int propertyIndex = 0;
                for (int index = 0; index < properties.length; index++) {
                    if (accessLevel.compareTo(properties[index].getLevel()) <= 0) {
                        descriptors[propertyIndex] = properties[index].getPropertyDescriptor(beanClass);
                        propertyIndex++;
                    }
                }
                return descriptors;
            } catch (IntrospectionException ie) {
                throw new IllegalStateException(ie.getMessage());
            }
        }
    }
    
    public Property getProperty( PropertyDescriptor pd ) {
        String name= pd.getName();
        for ( int i=0; i<properties.length; i++ ) {
            if ( properties[i].name.equals(name) ) {
                return properties[i];
            }
        }
        BeanInfo[] additional= getAdditionalBeanInfo();
        if ( additional!=null && additional.length>0) {
            for ( int i=0; i<additional.length; i++ ) {
                BeanInfo b= additional[i];
                if ( b instanceof AccessLevelBeanInfo ) {
                    Property p= ((AccessLevelBeanInfo)b).getProperty(pd);
                    if ( p!=null ) return p;
                }
            }
        }
        return null;
    }
    
    public BeanDescriptor getBeanDescriptor() {
        return new BeanDescriptor(beanClass);
    }
    
    /** AccessLevelBeanInfo.Property is a helper class for subclasses of
     * AccessLevelBeanInfo to specify a list of properties in a way that
     * is independant of the underlying Bean/PropertyDescriptor implementation.
     *
     * @author Edward West
     */
    protected static class Property {
        
        /** The name the user will see for this property */
        private final String name;
        
        /** The AccessLevel associated with this property */
        private final AccessLevel level;
        
        /** The PersistenceLevel associated with this property */
        private final PersistenceLevel persistenceLevel;
        
        /** The name of the accessor method for this property */
        private final String getter;
        
        /** The name of the mutator method for this property */
        private final String setter;
        
        /** The name of the indexed accessor method for this property */
        private final String igetter;
        
        /** The name of the indexed mutator method for this property */
        private final String isetter;
        
        /** The class of the graphical editor for this property */
        private final Class editor;
        
        /** flag indicated whether of not this property is indexed */
        private final boolean indexed;
        
        /** Creates a new Property object.
         * @param name the name the user will see for this property
         * @param level the AccessLevel associated with this property
         * @param getter the name of the accessor method for this property
         * @param setter the name of the mutator method for this property
         * @param editor the class name of the graphical editor for this property
         */
        public Property(String name, AccessLevel level,  PersistenceLevel persistenceLevel, String getter, String setter, Class editor) {
            this.name = name;
            this.level = level;
            this.persistenceLevel= persistenceLevel;
            this.getter = getter;
            this.setter = setter;
            this.igetter = null;
            this.isetter = null;
            this.editor = editor;
            this.indexed = false;
        }
        
        /** Creates a new Property object.
         * @param name the name the user will see for this property
         * @param level the AccessLevel associated with this property
         * @param getter the name of the accessor method for this property
         * @param setter the name of the mutator method for this property
         * @param editor the class name of the graphical editor for this property
         */
        public Property(String name, AccessLevel level, String getter, String setter, Class editor) {
            this( name, level, PersistenceLevel.TRANSIENT, getter, setter, editor );
        }
        
        /** Creates a new Property object that is indexed.
         * @param name the name the user will see for this property
         * @param level the AccessLevel associated with this property
         * @param getter the name of the accessor method for this property
         * @param setter the name of the mutator method for this property
         * @param igetter the name of the indexed accessor method for this property
         * @param isetter the name of the indexed mutator method for this property
         * @param editor the class name of the graphical editor for this property
         */
        public Property(String name, AccessLevel level, PersistenceLevel persistenceLevel, String getter, String setter, String igetter, String isetter, Class editor) {
            this.name = name;
            this.level = level;
            this.persistenceLevel= persistenceLevel;
            this.getter = getter;
            this.setter = setter;
            this.igetter = igetter;
            this.isetter = isetter;
            this.editor = editor;
            this.indexed = true;
        }
        
        /** Creates a new Property object that is indexed.
         * @param name the name the user will see for this property
         * @param level the AccessLevel associated with this property
         * @param getter the name of the accessor method for this property
         * @param setter the name of the mutator method for this property
         * @param igetter the name of the indexed accessor method for this property
         * @param isetter the name of the indexed mutator method for this property
         * @param editor the class name of the graphical editor for this property
         */
        public Property(String name, AccessLevel level, String getter, String setter, String igetter, String isetter, Class editor) {
            this( name, level, PersistenceLevel.TRANSIENT, getter, setter, igetter, isetter, editor );
        }
        
        /** Returns the access level for this property */
        public AccessLevel getLevel() {
            return level;
        }
        
        /** Returns a PropertyDescriptor for this property that is associated
         * with the specified bean class.
         */
        public PropertyDescriptor getPropertyDescriptor(Class beanClass) throws IntrospectionException {
            PropertyDescriptor pd;
            if (indexed) {
                pd = new IndexedPropertyDescriptor(name, beanClass, getter, setter, igetter, isetter);
            } else {
                pd = new PropertyDescriptor(name, beanClass, getter, setter);
            }
            if (editor != null) {
                pd.setPropertyEditorClass(editor);
            }
            return pd;
        }
        
        public AccessLevelBeanInfo.PersistenceLevel getPersistenceLevel() {
            return persistenceLevel;
        }
    }
}
