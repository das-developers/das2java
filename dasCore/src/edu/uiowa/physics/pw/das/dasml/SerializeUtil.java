/*
 * SerializeUtil.java
 *
 * Created on June 21, 2005, 9:55 AM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package edu.uiowa.physics.pw.das.dasml;

import edu.uiowa.physics.pw.das.beans.*;
import edu.uiowa.physics.pw.das.beans.BeansUtil;
import edu.uiowa.physics.pw.das.components.propertyeditor.*;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.graph.*;
import edu.uiowa.physics.pw.das.system.DasLogger;
import java.beans.*;
import java.beans.PropertyDescriptor;
import java.beans.XMLEncoder;
import java.lang.reflect.*;
import java.text.*;
import java.util.*;
import java.util.logging.*;
import org.w3c.dom.*;
import org.w3c.dom.DOMException;
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
        BeansUtil.registerPropertyEditors();
    }
    
    public static org.w3c.dom.Element getDOMElement( Document document, Object object ) {
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
            
            BeanInfo info = Introspector.getBeanInfo(object.getClass());
            PropertyDescriptor[] properties = info.getPropertyDescriptors();
            String[] propertyNameList= BeansUtil.getPropertyNames(object.getClass());
            
            HashMap nameMap= new HashMap();
            
            for ( int i=0; i<properties.length; i++ ) {
                nameMap.put( properties[i].getName(), properties[i] );
            }
            
            HashMap serializedObjects= new HashMap();
            
            for ( int i=0; i<propertyNameList.length; i++ ) {
                String propertyName= propertyNameList[i];
                       
                log.fine( "serializing property "+propertyName + " of "+elementName );
                
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
                
                java.beans.PropertyEditor editor= PropertyEditorManager.findEditor( pd.getPropertyType() );
                
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
                        Element child= getDOMElement( document, value );
                        propertyElement.appendChild(child);
                        element.appendChild(propertyElement);
                        serializedObjects.put( dcc.getDasName(), null );
                    }
                } else if ( value.getClass().isArray() ) {
                    // serialize each element of the array.  Assumes order doesn't change
                    Element propertyElement= document.createElement( propertyName );
                    for ( int j=0; j<Array.getLength(value); j++ ) {
                        Object value1= Array.get( value, j );
                        Element child= getDOMElement( document, value1 );
                        propertyElement.appendChild(child);
                    }
                    element.appendChild(propertyElement);
                    
                } else {
                    // catch-all for other beans.
                    Element propertyElement= document.createElement( propertyName );
                    Element child= getDOMElement( document, value );
                    propertyElement.appendChild(child);
                    element.appendChild(propertyElement);
                }
            }
            
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
            log.warning("unable to locate property: "+propertyName+" of "+className+", ignoring");
            return;
        }
        
        Method readMethod= pd.getReadMethod();
        Object value;
        
        value= readMethod.invoke( object, new Object[0] );
        
        java.beans.PropertyEditor editor= PropertyEditorManager.findEditor( pd.getPropertyType() );
        
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
                log.warning("read-only property \""+propertyName+"\" of "+className+" ignored" );
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
            while ( propertyNode!=null && propertyNode.getNodeType()!=Node.ELEMENT_NODE ) propertyNode= propertyNode.getNextSibling();
            if ( propertyNode==null ) throw new IllegalStateException( "expected element node under "+propertyName );
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
            while ( propertyNode!=null && propertyNode.getNodeType()!=Node.ELEMENT_NODE ) propertyNode= propertyNode.getNextSibling();
            if ( propertyNode==null ) throw new IllegalStateException( "expected element node under "+propertyName );
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
            
            log.fine("handling "+elementName);           
            
            if ( !object.getClass().getName().equals( elementName ) ) {
                throw new IllegalArgumentException("class name doesn't match!!!");
            }
            
            BeanInfo info = Introspector.getBeanInfo(object.getClass());
            PropertyDescriptor[] properties = info.getPropertyDescriptors();
            String[] propertyNameList= BeansUtil.getPropertyNames(object.getClass());
            
            HashMap nameMap= new HashMap();
            
            for ( int i=0; i<properties.length; i++ ) {
                nameMap.put( properties[i].getName(), properties[i] );
            }
            
            HashMap serializedObjects= new HashMap();
            
            NamedNodeMap attrs= element.getAttributes();
            for ( int i=0; i<attrs.getLength(); i++ ) {
                Node node= attrs.item(i);
                log.finer( "attr: "+ node.getNodeType() +"  "+node.getNodeName() );
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
