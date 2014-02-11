/*
 * ImplicitAccessLevelBeanInfo.java
 *
 * Created on April 21, 2006, 3:58 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.das2.beans;

import org.das2.beans.AccessLevelBeanInfo.AccessLevel;
import org.das2.beans.AccessLevelBeanInfo.PersistenceLevel;
import org.das2.beans.AccessLevelBeanInfo.Property;
import java.beans.BeanInfo;
import java.beans.IndexedPropertyDescriptor;
import java.beans.PropertyDescriptor;

/**
 * ImplicitAccessLevelBeanInfo makes any BeanInfo class look like an AccessLevelBeanInfo by implementing
 * the default access level and persistence level settings.
 *
 * @author Jeremy
 */
public class ImplicitAccessLevelBeanInfo extends AccessLevelBeanInfo {
    
    //BeanInfo beanInfo;
    
    /** Creates a new instance of ImplicitAccessLevelBeanInfo */
    private ImplicitAccessLevelBeanInfo( BeanInfo beanInfo, Class beanClass, Property[] properties ) {
        super( properties, beanClass );
        //this.beanInfo= beanInfo;
    }
    
    public static ImplicitAccessLevelBeanInfo create( BeanInfo beanInfo, Class beanClass ) {
        Property[] properties;
        PropertyDescriptor[] pds = BeansUtil.getPropertyDescriptors( beanClass );
        String[] propertyNameList= BeansUtil.getPropertyNames( pds );

        properties= new Property[propertyNameList.length];
        for ( int i=0; i<properties.length; i++ ) {
            PropertyDescriptor pd= pds[i];
            String setter= pd.getWriteMethod()==null ? null : pd.getWriteMethod().getName();
            String getter= pd.getReadMethod()==null ? null : pd.getReadMethod().getName();
            if ( pd instanceof IndexedPropertyDescriptor ) {
                IndexedPropertyDescriptor ipd= (IndexedPropertyDescriptor)pd;
                String isetter= ipd.getIndexedWriteMethod()==null ? null : ipd.getIndexedWriteMethod().getName();
                String igetter= ipd.getIndexedReadMethod()==null ? null : ipd.getIndexedReadMethod().getName();
                properties[i]= new Property( pd.getName(), AccessLevel.DASML, PersistenceLevel.TRANSIENT, 
                        setter, getter, isetter, igetter, pd.getPropertyEditorClass() );
            } else {
                properties[i]= new Property( pd.getName(), AccessLevel.DASML, PersistenceLevel.TRANSIENT, 
                        setter, getter, pd.getPropertyEditorClass() );
            }
        }
        return new ImplicitAccessLevelBeanInfo( beanInfo, beanClass, properties );
    }
    
}
