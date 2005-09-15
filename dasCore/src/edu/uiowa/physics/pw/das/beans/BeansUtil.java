/*
 * BeanInfoUtil.java
 *
 * Created on May 31, 2005, 11:49 AM
 */

package edu.uiowa.physics.pw.das.beans;
import edu.uiowa.physics.pw.das.components.*;
import edu.uiowa.physics.pw.das.components.propertyeditor.*;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.graph.*;
import edu.uiowa.physics.pw.das.system.DasLogger;
import java.awt.Color;
import java.awt.Rectangle;
import java.beans.*;
import java.util.*;
import java.util.logging.Logger;

/**
 *
 * @author Jeremy
 */
public class BeansUtil {
    
    /**
     * See that the known editors are registered with the PropertyEditorManager
     */
    public static void registerPropertyEditors() {
        PropertyEditorManager.registerEditor(Color.class, ColorEditor.class);
        PropertyEditorManager.registerEditor(Datum.class, DatumEditor.class);
        PropertyEditorManager.registerEditor(Units.class, UnitsEditor.class );
        PropertyEditorManager.registerEditor(NumberUnits.class, UnitsEditor.class );
        PropertyEditorManager.registerEditor(Boolean.TYPE, BooleanEditor.class);
        PropertyEditorManager.registerEditor(Boolean.class, BooleanEditor.class);
        PropertyEditorManager.registerEditor(PsymConnector.class, EnumerationEditor.class);
        // PropertyEditorManager.registerEditor(Rectangle.class, RectangleEditor.class);
        //PropertyEditorManager.registerEditor(DasServer.class, DasServerEditor.class);
    }
    
    /**
     * Use reflection to get a list of all the property names for the class.
     * The properties are returned in the order specified, and put inherited properties
     * at the end of the list.
     */
    public static PropertyDescriptor[] getPropertyDescriptors( Class c ) {
        Set excludePropertyNames;
        excludePropertyNames= new HashSet();
        excludePropertyNames.add("class");
        excludePropertyNames.add("listLabel");
        excludePropertyNames.add("listIcon");
        
        try {
            Logger log= DasLogger.getLogger(DasLogger.SYSTEM_LOG);
            
            // goal: get BeanInfo for the class, the AccessLevelBeanInfo if it exists.
            BeanInfo beanInfo= null;
            
            if ( c.getPackage()==null ) { // e.g. String array
                beanInfo= Introspector.getBeanInfo(c);
                log.finer( "using BeanInfo "+ beanInfo.getClass().getName()+" for "+c.getName() );
            } else {
                
                String s;
                try {
                    s= c.getName().substring(c.getPackage().getName().length()+1);
                } catch ( Exception e ) {
                    throw new RuntimeException(e);
                }
                
                Class maybeClass=null;
                String beanInfoClassName=null;
                
                try {
                    beanInfoClassName= c.getPackage() + "." + s + "BeanInfo";
                    maybeClass= Class.forName( beanInfoClassName );
                } catch ( ClassNotFoundException e ) {
                    try {
                        beanInfoClassName= "edu.uiowa.physics.pw.das.beans."+  s + "BeanInfo";
                        maybeClass= Class.forName( beanInfoClassName );
                    } catch ( ClassNotFoundException e2 ) {
                        beanInfo= Introspector.getBeanInfo(c);
                        beanInfoClassName= beanInfo.getClass().getName();
                    }
                }
                
                log.finer( "using BeanInfo "+beanInfoClassName+" for "+c.getName() );
                
                if ( beanInfo==null ) {
                    beanInfo= (BeanInfo)maybeClass.newInstance();
                }
            }
            
            
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
                   /* This is commented to mimic the behavior of the Introspector, which doesn't climb up the bean inheritance
                     tree unless the additional are specified via the Introspector. */
                   // if ( aBeanInfo.getAdditionalBeanInfo()!=null ) {
                   //     additionalBeanInfo.addAll( Arrays.asList( aBeanInfo.getAdditionalBeanInfo() ) );
                   // }
                }
            }
            
            return (PropertyDescriptor[])propertyList.toArray(new PropertyDescriptor[ propertyList.size() ] );
            
        } catch ( IntrospectionException e ) {
            return null;
        } catch ( InstantiationException e ) {
            return null;
        } catch ( IllegalAccessException e ) {
            return null;
        }
    }
    
    public static String[] getPropertyNames( PropertyDescriptor[] propertyList ) {
        String[] result= new String[ propertyList.length ];
        for ( int i=0; i<result.length; i++ ) {
            result[i]= ((PropertyDescriptor)propertyList[i]).getName();
        }
        return result;
    }
    
    /**
     * Use reflection to get a list of all the property names for the class.
     * The properties are returned in the order specified, and put inherited properties
     * at the end of the list.  This is motivated by the arbitary order that the 
     * Introspector presents the properties, which is in conflict with our desire to control
     * the property order.
     */
    public static String[] getPropertyNames( Class c ) {
        PropertyDescriptor[] propertyList= getPropertyDescriptors( c );
        return getPropertyNames( propertyList );
    }
    
    
}
