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
        try {
            String s= c.getName().substring(c.getPackage().getName().length()+1);
            
            Class maybeClass=null;
            try {
                maybeClass= Class.forName( c.getPackage() + "." + s + "BeanInfo" );
            } catch ( ClassNotFoundException e ) {
                maybeClass= Class.forName( "edu.uiowa.physics.pw.das.beans."+  s + "BeanInfo" );
            }
            
            Object object= maybeClass.newInstance();
            BeanInfo beanInfo= (BeanInfo)maybeClass.newInstance();
            
            List propertyList= new ArrayList();
            
            PropertyDescriptor[] pdthis= beanInfo.getPropertyDescriptors();
            propertyList.addAll( Arrays.asList(pdthis) );
                        
            if ( beanInfo.getAdditionalBeanInfo()!=null ) {
                List additionalBeanInfo= new ArrayList( Arrays.asList( beanInfo.getAdditionalBeanInfo() ) );
                while( additionalBeanInfo.size()>0 ) {
                    BeanInfo aBeanInfo= (BeanInfo)additionalBeanInfo.remove(0);
                    System.out.println(aBeanInfo);
                    propertyList.addAll( Arrays.asList( aBeanInfo.getPropertyDescriptors() ) );
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
            
            
        } catch ( InstantiationException e ) {
            return null;
        } catch ( ClassNotFoundException e ) {
            return null;
        } catch ( IllegalAccessException e ) {
            return null;
        }
    }
    
    
}
