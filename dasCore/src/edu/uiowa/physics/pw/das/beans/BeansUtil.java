/*
 * BeanInfoUtil.java
 *
 * Created on May 31, 2005, 11:49 AM
 */

package edu.uiowa.physics.pw.das.beans;

import java.beans.*;
import java.lang.reflect.*;
import java.util.*;

/**
 *
 * @author Jeremy
 */
public class BeansUtil {
    /**
     * Use reflection to get a list of all the property names for the class.
     * The properties are returned in the order specified, and put inherited properties
     * at the end of the list.
     */
    public static String[] getPropertyNames( Class c ) {
        Set excludePropertyNames;    
        excludePropertyNames= new HashSet();
        excludePropertyNames.add("class");
        excludePropertyNames.add("listLabel");
        excludePropertyNames.add("listIcon");
    
        try {
            
            // goal: get BeanInfo for the class, the AccessLevelBeanInfo if it exists.
            BeanInfo beanInfo= null;
                        
            String s= c.getName().substring(c.getPackage().getName().length()+1);
            
            Class maybeClass=null;
            try {
                maybeClass= Class.forName( c.getPackage() + "." + s + "BeanInfo" );
            } catch ( ClassNotFoundException e ) {
                try {
                    maybeClass= Class.forName( "edu.uiowa.physics.pw.das.beans."+  s + "BeanInfo" );
                } catch ( ClassNotFoundException e2 ) {
                    beanInfo= Introspector.getBeanInfo(c);
                }
            }
                        
            if ( beanInfo==null ) beanInfo= (BeanInfo)maybeClass.newInstance();
            
            List propertyList= new ArrayList();
            
            PropertyDescriptor[] pdthis;
            try {
              pdthis=   beanInfo.getPropertyDescriptors();
            } catch ( IllegalStateException e ) {
                throw new RuntimeException(e);
            }
            
            for ( int i=0; i<pdthis.length; i++ ) {                
                boolean isWriteable= pdthis[i].getWriteMethod()!=null;
                boolean isUseable= pdthis[i].getReadMethod()!=null && !excludePropertyNames.contains(pdthis[i].getName());
                if ( isUseable || ( pdthis[i] instanceof IndexedPropertyDescriptor ) ) {                
                    propertyList.add( pdthis[i] );
                } 
            }
            
            if ( beanInfo.getAdditionalBeanInfo()!=null ) {
                List additionalBeanInfo= new ArrayList( Arrays.asList( beanInfo.getAdditionalBeanInfo() ) );
                while( additionalBeanInfo.size()>0 ) {
                    BeanInfo aBeanInfo= (BeanInfo)additionalBeanInfo.remove(0);
                    pdthis=  aBeanInfo.getPropertyDescriptors();
                    for ( int i=0; i<pdthis.length; i++ ) {
                        boolean isWriteable= pdthis[i].getWriteMethod()!=null;                        
                        boolean isUseable= pdthis[i].getReadMethod()!=null && !excludePropertyNames.contains(pdthis[i].getName());
                        if ( isUseable || ( pdthis[i] instanceof IndexedPropertyDescriptor  )) {                        
                            propertyList.add( pdthis[i] );
                        }
                    }                    
                    if ( aBeanInfo.getAdditionalBeanInfo()!=null ) {
                        additionalBeanInfo.addAll( Arrays.asList( aBeanInfo.getAdditionalBeanInfo() ) );
                    }
                }
            }
            
            String[] result= new String[ propertyList.size() ];
            for ( int i=0; i<result.length; i++ ) {
                result[i]= ((PropertyDescriptor)propertyList.get(i)).getName();
            }
            return result;
            
        } catch ( IntrospectionException e ) {
            return null;
        } catch ( InstantiationException e ) {
            return null;
        } catch ( IllegalAccessException e ) {
            return null;
        }
    }
    
    
}
