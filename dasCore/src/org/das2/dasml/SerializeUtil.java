/*
 * SerializeUtil.java
 *
 * Created on June 21, 2005, 9:55 AM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package org.das2.dasml;

import org.das2.graph.DasCanvasComponent;
import org.das2.beans.AccessLevelBeanInfo;
import org.das2.beans.BeansUtil;
import org.das2.system.DasLogger;
import org.das2.util.monitor.ProgressMonitor;
import org.das2.util.monitor.NullProgressMonitor;
import java.beans.*;
import java.beans.PropertyDescriptor;
import java.lang.reflect.*;
import java.text.*;
import java.util.*;
import java.util.logging.*;
import org.w3c.dom.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author Jeremy
 */
public class SerializeUtil {
    
    static Set toStringSet;
    static {
        toStringSet= new HashSet();
        toStringSet.add( Boolean.class );
        toStringSet.add( Short.class );
        toStringSet.add( Integer.class );
        toStringSet.add( Long.class );
        toStringSet.add( Float.class );
        toStringSet.add( Double.class );
        toStringSet.add( String.class );
    }
    
    public static org.w3c.dom.Element getDOMElement( Document document, Object object ) {
        return getDOMElement( document, object, new NullProgressMonitor() );
    }
    
    public static org.w3c.dom.Element getDOMElement( Document document, Object object, ProgressMonitor monitor ) {
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
            
            HashMap serializedObjects= new HashMap();
            
            if ( propertyNameList.length>0 ) monitor.setTaskSize( propertyNameList.length );
            monitor.started();
            
            for ( int i=0; i<propertyNameList.length; i++ ) {
                monitor.setTaskProgress(i);
                
                String propertyName= propertyNameList[i];
                
                log.log( Level.FINE, "serializing property {0} of {1}", new Object[]{propertyName, elementName});
                
                if ( propertyName.equals("parent") ) {
                    log.info( "kludge to avoid cycles in bean graph due to parent property, ignoring");
                }
                
                PropertyDescriptor pd= (PropertyDescriptor)nameMap.get(propertyName);
                
                if ( pd==null ) {
                    log.log(Level.WARNING, "unable to locate property: {0}, ignoring", propertyName);
                    continue;
                }
                
                Method readMethod= pd.getReadMethod();
                
                if ( readMethod==null ) {
                    // note this happens with the indexed property getRBG of ColorBar.Type
                    log.log( Level.INFO, "skipping property {0} of {1}, failed to find read method.", new Object[]{propertyName, elementName});
                    continue;
                }
                
                Method writeMethod= pd.getWriteMethod();
                
                Object value;
                
                value= readMethod.invoke( object, new Object[0] );
                
                if ( value==null ) {
                    log.log( Level.INFO, "skipping property {0} of {1}, value is null.", new Object[]{propertyName, elementName});
                    continue;
                }
                
                java.beans.PropertyEditor editor= BeansUtil.getEditor( pd );
                
                String textValue= null;
                
                if ( editor!=null ) {
                    editor.setValue( value );
                    textValue= editor.getAsText();
                }
                
                if ( textValue!=null ) {
                    if ( writeMethod!=null ) element.setAttribute( propertyName, textValue );
                } else if ( value instanceof DasCanvasComponent ) {
                    // special optimization, only serialize at the first reference to DCC, afterwards just use name
                    DasCanvasComponent dcc= (DasCanvasComponent)value;
                    if ( serializedObjects.containsKey( dcc.getDasName() ) ) {
                        element.setAttribute( propertyName, dcc.getDasName() );
                        continue;
                    } else {
                        Element propertyElement= document.createElement( propertyName );
                        Element child= getDOMElement( document, value, new NullProgressMonitor() );
                        propertyElement.appendChild(child);
                        element.appendChild(propertyElement);
                        serializedObjects.put( dcc.getDasName(), null );
                    }
                } else if ( value.getClass().isArray() ) {
                    // serialize each element of the array.  Assumes order doesn't change
                    Element propertyElement= document.createElement( propertyName );
                    for ( int j=0; j<Array.getLength(value); j++ ) {
                        Object value1= Array.get( value, j );
                        Element child= getDOMElement( document, value1, new NullProgressMonitor() );
                        propertyElement.appendChild(child);
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
        }
    }
    
    private static void processNode( Node node, Object object, String className, Map nameMap ) throws IllegalAccessException, ParseException, InvocationTargetException {
        Logger log= DasLogger.getLogger( DasLogger.SYSTEM_LOG );
        
        String propertyName= node.getNodeName();
        
        PropertyDescriptor pd= (PropertyDescriptor)nameMap.get(propertyName);
        
        if ( pd==null ) {
            log.log(Level.WARNING, "unable to locate property: {0} of {1}, ignoring", new Object[]{propertyName, className});
            return;
        }
        
        Method readMethod= pd.getReadMethod();
        Object value;
        
        value= readMethod.invoke( object, new Object[0] );
        
        java.beans.PropertyEditor editor= BeansUtil.getEditor( pd );
        
        String textValue= null;
        
        if ( editor!=null ) {
            editor.setValue( value );
            textValue= editor.getAsText(); // getAsText implies that setAsText is supported--see docs.
        }
        
        if ( textValue!=null ) {
            Attr attr= (Attr)node;
            String newTextValue= attr.getValue();
            editor.setAsText( newTextValue );
            
            Method writeMethod= pd.getWriteMethod();
            if ( writeMethod==null ) {
                log.log(Level.WARNING, "read-only property \"{0}\" of {1} ignored", new Object[]{propertyName, className});
                return;
            }
            
            Object newValue= editor.getValue();
            
            if ( propertyName.equals("dataSetID" )  ) {
                log.info( "kludge to avoid setting dataSetID to null, ignoring" );
            }
            
            if ( propertyName.equals("dataSetID" ) && ( value==null || value.equals("") ) ) {
                log.info( "kludge to avoid setting dataSetID to null, ignoring" );
                return;
            }
            
            writeMethod.invoke( object, new Object[] { newValue } );
            
        } else if ( value instanceof DasCanvasComponent ) {
            DasCanvasComponent dcc= (DasCanvasComponent)value;
            Node propertyNode= node.getFirstChild();
            while ( propertyNode!=null &&
                    ( propertyNode.getNodeType()!=org.w3c.dom.Node.ELEMENT_NODE ) &&
                    ( propertyNode.getNodeType()!=org.w3c.dom.Node.TEXT_NODE ) )   propertyNode= propertyNode.getNextSibling();
            if ( propertyNode==null ) {
                throw new IllegalStateException( "expected element node under "+propertyName );
            }
            if ( propertyNode.getNodeType()==Node.TEXT_NODE ) {
                // name dereference--ignore
                return;
            }
            Element propertyElement= (Element)propertyNode;
            processElement( propertyElement, value );
        } else if ( value.getClass().isArray() ) {
            int j=0;
            Node propertyNode= node.getFirstChild();
            while ( propertyNode!=null ) {
                while ( propertyNode!=null && propertyNode.getNodeType()!=Node.ELEMENT_NODE ) propertyNode= propertyNode.getNextSibling();
                if ( propertyNode==null ) break;
                Element propertyElement= (Element)propertyNode;
                Object value1= Array.get( value, j );
                processElement( propertyElement, value1 );
                j= j+1;
                propertyNode= propertyNode.getNextSibling();
            }
        } else {
            Node propertyNode= node.getFirstChild();
            while ( propertyNode!=null && propertyNode.getNodeType()!=Node.ELEMENT_NODE && propertyNode.getNodeType()!=org.w3c.dom.Node.TEXT_NODE ) propertyNode= propertyNode.getNextSibling();
            if ( propertyNode==null ) throw new IllegalStateException( "expected element node under "+propertyName );
            if ( propertyNode.getNodeType()==Node.TEXT_NODE ) {
                // name dereference--ignore
                return;
            }
            Element propertyElement= (Element)propertyNode;
            processElement( propertyElement, value );
        }
    }
    
    /* set the properties of the object by
     * reading each property state from the element, and setting the property.
     */
    public static void processElement( Element element, Object object ) {
        Logger log= DasLogger.getLogger( DasLogger.SYSTEM_LOG );
        try {
            String elementName= element.getTagName();
            elementName= elementName.replaceAll( "\\_dollar_", "\\$" );
            
            log.log(Level.FINE, "handling {0}", elementName);
            
            if ( !object.getClass().getName().equals( elementName ) ) {
                throw new IllegalArgumentException("class name doesn't match: expected "+
                        object.getClass().getName()+", got "+elementName );
            }
            
            AccessLevelBeanInfo info = BeansUtil.asAccessLevelBeanInfo( BeansUtil.getBeanInfo(object.getClass()), object.getClass() );
            PropertyDescriptor[] properties =
                    info.getPropertyDescriptors( AccessLevelBeanInfo.PersistenceLevel.PERSISTENT ) ;
            String[] propertyNameList= BeansUtil.getPropertyNames(object.getClass());
            
            HashMap nameMap= new HashMap();
            
            for ( int i=0; i<properties.length; i++ ) {
                nameMap.put( properties[i].getName(), properties[i] );
            }
            
            NamedNodeMap attrs= element.getAttributes();
            for ( int i=0; i<attrs.getLength(); i++ ) {
                Node node= attrs.item(i);
                log.log( Level.FINER, "attr: {0}  {1}", new Object[]{node.getNodeType(), node.getNodeName()});
                processNode( node, object, elementName, nameMap );
            }
            
            NodeList children= element.getChildNodes();
            
            for ( int i=0; i<children.getLength(); i++ ) {
                Node node= children.item(i);
                
                //log.finest( "got node: "+node.getNodeName() + " " + node.getNodeType() +"  "+node.getNodeName() );
                if ( node.getNodeType()!=Node.ELEMENT_NODE ) continue;
                
                processNode( node, object, elementName, nameMap );
            }
            
        } catch ( IntrospectionException e ) {
            throw new RuntimeException(e);
        } catch ( IllegalAccessException e ) {
            throw new RuntimeException(e);
        } catch ( InvocationTargetException e ) {
            throw new RuntimeException(e);
        } catch ( ParseException e ) {
            throw new RuntimeException(e);
        }
    }
}
