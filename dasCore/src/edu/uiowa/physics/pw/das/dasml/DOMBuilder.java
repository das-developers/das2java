/*
 * Serializer.java
 *
 * Created on April 28, 2006, 4:49 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.uiowa.physics.pw.das.dasml;

import edu.uiowa.physics.pw.das.DasApplication;
import edu.uiowa.physics.pw.das.NameContext;
import edu.uiowa.physics.pw.das.beans.AccessLevelBeanInfo;
import edu.uiowa.physics.pw.das.beans.BeansUtil;
import edu.uiowa.physics.pw.das.graph.*;
import edu.uiowa.physics.pw.das.system.DasLogger;
import org.das2.util.monitor.DasProgressMonitor;
import edu.uiowa.physics.pw.das.util.NullProgressMonitor;
import java.beans.*;
import java.beans.PropertyDescriptor;
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * New stateful serialize utility that uses names when available.
 * @author Jeremy
 */
public class DOMBuilder {
    
    Object bean;
    HashMap serializedObjects;
    NameContext nameContext;
    
    /** Creates a new instance of Serializer */
    public DOMBuilder( Object bean ) {
        this.bean= bean;
    }
    
    /**
     * returns name or null.
     */
    private String getBeanName( Object bean ) {
        try {
            PropertyDescriptor[] pds= BeansUtil.getPropertyDescriptors(bean.getClass());
            for ( int i=0; i<pds.length; i++ ) {
                PropertyDescriptor pd= pds[i];
                if ( pd.getName().equals("name") ) {
                    String name= (String)pd.getReadMethod().invoke( bean, new Object[0] );
                    return name;
                }
            }
        } catch ( IllegalAccessException ex ) {
        } catch ( InvocationTargetException ex ) {
        }
        
        return null;
    }
    
    private org.w3c.dom.Element getDOMElement( Document document, Object object, DasProgressMonitor monitor ) {
        Logger log= DasLogger.getLogger( DasLogger.SYSTEM_LOG );
        
        try {
            String elementName= object.getClass().getName();

            elementName= elementName.replaceAll("\\$", "\\_dollar_");
            
            Element element=null;
            try {
                element= document.createElement(elementName);
            } catch ( Exception e ) {
                System.err.println(e);
                throw new RuntimeException(e);
            }
            
            BeanInfo info = BeansUtil.getBeanInfo(object.getClass());
            
            AccessLevelBeanInfo alInfo= BeansUtil.asAccessLevelBeanInfo( info, object.getClass() );
            
            PropertyDescriptor[] properties = alInfo.getPropertyDescriptors( AccessLevelBeanInfo.PersistenceLevel.PERSISTENT );
            String[] propertyNameList= BeansUtil.getPropertyNames( properties );
            
            HashMap nameMap= new HashMap();
            
            for ( int i=0; i<properties.length; i++ ) {
                nameMap.put( properties[i].getName(), properties[i] );
            }
            
            if ( propertyNameList.length>0 ) monitor.setTaskSize( propertyNameList.length );
            monitor.started();
            
            for ( int i=0; i<propertyNameList.length; i++ ) {
                monitor.setTaskProgress(i);
                
                String propertyName= propertyNameList[i];
                
                log.fine( "serializing property "+propertyName + " of "+elementName );
                System.err.println("@@@: "+"serializing property "+propertyName + " of "+elementName );
                
                if ( propertyName.equals("parent" ) ) {
                    log.fine( "kludge to skip parents thus avoiding cycles." );
                    continue;
                }
                
                PropertyDescriptor pd= (PropertyDescriptor)nameMap.get(propertyName);
                
                if ( pd==null ) {
                    log.warning("unable to locate property: "+propertyName+", ignoring");
                    continue;
                }
                
                Method readMethod= pd.getReadMethod();
                
                if ( readMethod==null ) {
                    // note this happens with the indexed property getRBG of ColorBar.Type
                    log.info( "skipping property "+propertyName+" of "+elementName+", failed to find read method." );
                    continue;
                }
                
                Method writeMethod= pd.getWriteMethod();
                
                Object value;
                
                value= readMethod.invoke( object, new Object[0] );
                
                if ( value==null ) {
                    log.info( "skipping property "+propertyName+" of "+elementName+", value is null." );
                    continue;
                }
                
                // DEBUGGING
                if ( pd.getName().equals("dataMaximum" ) ) {
                    System.err.println(elementName);
                }
                if ( pd.getName().equals("baseFont" ) ) {
                    System.err.println(elementName);
                }
                
                java.beans.PropertyEditor editor= BeansUtil.getEditor( pd );
                
                String textValue= null;
                
                if ( editor!=null ) {
                    editor.setValue( value );
                    textValue= editor.getAsText();
                }
                
                String beanName= getBeanName( value );
                
                if ( textValue!=null ) {
                    if ( writeMethod!=null ) element.setAttribute( propertyName, textValue );
                } else if ( beanName!=null ) {
                    // special optimization, only serialize at the first reference to DCC, afterwards just use name
                    if ( serializedObjects.containsKey( beanName ) ) {
                        element.setAttribute( propertyName, beanName );
                        continue;
                    } else {
                        Element propertyElement= document.createElement( propertyName );
                        Element child= getDOMElement( document, value, new NullProgressMonitor() );
                        propertyElement.appendChild(child);
                        element.appendChild(propertyElement);
                        serializedObjects.put( beanName, value );
                    }
                } else if ( value.getClass().isArray() ) {
                    // serialize each element of the array.  Assumes order doesn't change
                    Element propertyElement= document.createElement( propertyName );
                    for ( int j=0; j<Array.getLength(value); j++ ) {
                        Object value1= Array.get( value, j );
                        Element child= getDOMElement( document, value1, new NullProgressMonitor() );
                        propertyElement.appendChild(child);
                        if ( value1 instanceof DasCanvasComponent ) {
                            DasCanvasComponent dcc= ( DasCanvasComponent)value1;
                            serializedObjects.put( dcc.getDasName(), value1 );
                        }
                    }
                    element.appendChild(propertyElement);
                    
                } else {
                    // catch-all for other beans.
                    Element propertyElement= document.createElement( propertyName );
                    Element child= getDOMElement( document, value, new NullProgressMonitor() );
                    propertyElement.appendChild(child);
                    element.appendChild(propertyElement);
                }
            }
            monitor.finished();
            
            return element;
        } catch ( IntrospectionException e ) {
            throw new RuntimeException(e);
        } catch ( IllegalAccessException e ) {
            throw new RuntimeException(e);
        } catch ( InvocationTargetException e ) {
            throw new RuntimeException(e);      
        } finally {
            monitor.finished();
        }
    }
    
    
    public synchronized Element serialize( Document document, DasProgressMonitor monitor ) {        
        nameContext= DasApplication.getDefaultApplication().getNameContext();
        serializedObjects= new HashMap();
        
        return getDOMElement( document, this.bean, monitor );
    }
}
