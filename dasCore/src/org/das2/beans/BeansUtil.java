/*
 * BeanInfoUtil.java
 *
 * Created on May 31, 2005, 11:49 AM
 */
package org.das2.beans;

import org.das2.graph.FillStyle;
import org.das2.graph.PsymConnector;
import org.das2.graph.Psym;
import org.das2.graph.PlotSymbol;
import org.das2.components.DatumEditor;
import org.das2.components.propertyeditor.BooleanEditor;
import org.das2.components.propertyeditor.EnumerationEditor;
import org.das2.components.propertyeditor.ColorEditor;
import org.das2.components.DatumRangeEditor;
import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import org.das2.datum.Datum;
import org.das2.datum.NumberUnits;
import org.das2.DasApplication;
import org.das2.util.ClassMap;
import java.awt.Color;
import java.beans.*;
import java.lang.reflect.Modifier;
import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.components.propertyeditor.StringWithSchemeEditor;
import org.das2.util.LoggerManager;


/**
 * Utilities for JavaBeans conventions, allowing custom editors to be 
 * used to edit JavaBeans with set/get properties.
 * @author Jeremy
 */
public class BeansUtil {

    private static final Logger logger = LoggerManager.getLogger("das2.system");

    static {
        String[] beanInfoSearchPath = {"org.das2.beans", "sun.beans.infos"};
        if (DasApplication.hasAllPermission()) {
            Introspector.setBeanInfoSearchPath(beanInfoSearchPath);
        }
    }
    static ClassMap editorRegistry = new ClassMap();
    
    /** 
     * see BeanBindingDemo2.java 
     * @param beanClass the bean class, e.g. Datum.class
     * @param editorClass the editor class, e.g. DatumEditor.class
     */
    public static void registerEditor(Class beanClass, Class editorClass) {
        if ( DasApplication.hasAllPermission() ) {
            PropertyEditorManager.registerEditor(beanClass, editorClass);
            editorRegistry.put(beanClass, editorClass);
        } else {
            
        }
    }

    /**
     * See that the known editors are registered with the PropertyEditorManager
     */
    static {
        if (DasApplication.hasAllPermission()) {
            registerEditor(Datum.class, DatumEditor.class);
            registerEditor(DatumRange.class, DatumRangeEditor.class);
            registerEditor(Color.class, ColorEditor.class);
            registerEditor(Units.class, UnitsEditor.class);
            registerEditor(NumberUnits.class, UnitsEditor.class);
            registerEditor(Boolean.TYPE, BooleanEditor.class);
            registerEditor(Boolean.class, BooleanEditor.class);
            registerEditor(PsymConnector.class, EnumerationEditor.class);
            registerEditor(Psym.class, EnumerationEditor.class);
            registerEditor(PlotSymbol.class, EnumerationEditor.class);
            registerEditor(FillStyle.class, EnumerationEditor.class);
            registerEditor(String.class, StringWithSchemeEditor.class);
            registerEditor(org.das2.graph.DigitalRenderer.Align.class, EnumerationEditor.class );
            registerEditor(org.das2.graph.DasColorBar.Type.class, EnumerationEditor.class );
            registerEditor(org.das2.graph.SpectrogramRenderer.RebinnerEnum.class, EnumerationEditor.class );
            registerEditor(org.das2.graph.LegendPosition.class, EnumerationEditor.class );
            registerEditor(org.das2.graph.AnchorPosition.class, EnumerationEditor.class );
            registerEditor(org.das2.graph.AnchorType.class,EnumerationEditor.class );
            registerEditor(org.das2.graph.BorderType.class,EnumerationEditor.class );
            registerEditor(org.das2.graph.ErrorBarType.class,EnumerationEditor.class );
            registerEditor(java.util.logging.Level.class, EnumerationEditor.class );
            
        // registerEditor(Rectangle.class, RectangleEditor.class);
        //registerEditor(DasServer.class, DasServerEditor.class);
        }
    }
    /**
     * There's an annoyance with PropertyEditorManager.findEditor, in that it
     * always goes looking for the editor with the classLoader.  This is annoying
     * for applets, because this causes an applet codebase hit each time its called,
     * even if it's already been called for the given class.  This is problematic
     * with the propertyEditor, which calls this for each property, making it
     * sub-interactive at best.  Here we keep track of the results, either in a
     * list of nullPropertyEditors or by registering the editor we just found.
     */
    private static final HashSet nullPropertyEditors = new HashSet();

    public static java.beans.PropertyEditor findEditor(Class propertyClass) {
        java.beans.PropertyEditor result;
        if (nullPropertyEditors.contains(propertyClass)) {
            result = null;
        } else {
            result = PropertyEditorManager.findEditor(propertyClass);

            if (result == null) {
                Class resultClass = (Class) editorRegistry.get(propertyClass);
                if (resultClass != null) {
                    try {
                        result = (java.beans.PropertyEditor) resultClass.newInstance(); // TODO: this branch will cause excessive lookups for applets.
                    } catch (InstantiationException ex) {
                        logger.log( Level.WARNING, ex.getMessage(), ex );
                    } catch (IllegalAccessException ex) {
                        logger.log( Level.WARNING, ex.getMessage(), ex );
                    }
                }
            }

            // if still null, then keep track of the null so we don't have to search again.
            if (result == null) {
                nullPropertyEditors.add(propertyClass);
		//result= new PropertyEditorSupport();
            } else {
                if ( DasApplication.hasAllPermission() ) PropertyEditorManager.registerEditor(propertyClass, result.getClass()); // TODO: this will cause problems when the super class comes before subclass
            }

        }
        return result;
    }

    /**
     * One-stop place to get the editor for the given propertyDescriptor.
     * @param pd the property descriptor
     * @return the editor 
     */
    public static java.beans.PropertyEditor getEditor(PropertyDescriptor pd) {
        java.beans.PropertyEditor editor = null;

        try {
            Class editorClass = pd.getPropertyEditorClass();
            if (editorClass != null) {
                editor = (java.beans.PropertyEditor) editorClass.newInstance();
            }
        } catch (InstantiationException ex) {
        } catch (IllegalAccessException ex) {
        }

        if (editor == null) {
            editor = BeansUtil.findEditor(pd.getPropertyType());
        }
        return editor;
    }

    /**
     * Use reflection to get a list of all the property names for the class.
     * The properties are returned in the order specified, and put inherited properties
     * at the end of the list.  Implement the include/exclude logic.  
     * 
     * This will not return write-only descriptors!!!
     * 
     */
    public static PropertyDescriptor[] getPropertyDescriptors(Class c) {
        Set excludePropertyNames;
        excludePropertyNames = new HashSet();
        excludePropertyNames.add("class");
        excludePropertyNames.add("listLabel");
        excludePropertyNames.add("listIcon");

        if ( !Modifier.isPublic(c.getModifiers() ) ) {
            System.err.println("cannot use class: "+c );
            return new PropertyDescriptor[0];
        }

        try {
            BeanInfo beanInfo = getBeanInfo(c);

            List propertyList = new ArrayList();

            PropertyDescriptor[] pdthis;
            try {
                pdthis = beanInfo.getPropertyDescriptors();
            } catch (IllegalStateException e) {
                throw new RuntimeException(e);
            }

            for (PropertyDescriptor pdthi : pdthis) {
                boolean isUseable = pdthi.getReadMethod() != null && !excludePropertyNames.contains(pdthi.getName());
                if (isUseable) {
                    propertyList.add(pdthi);
                }
            }

            if (beanInfo.getAdditionalBeanInfo() != null) {
                List additionalBeanInfo = new ArrayList(Arrays.asList(beanInfo.getAdditionalBeanInfo()));
                while (additionalBeanInfo.size() > 0) {
                    BeanInfo aBeanInfo = (BeanInfo) additionalBeanInfo.remove(0);
                    pdthis = aBeanInfo.getPropertyDescriptors();
                    for (PropertyDescriptor pdthi : pdthis) {
                        String name = pdthi.getName();
                        boolean isUseable = pdthi.getReadMethod() != null && !excludePropertyNames.contains(name);
                        if (isUseable) {
                            propertyList.add(pdthi);
                        }
                    }
                    /* This is commented to mimic the behavior of the Introspector, which doesn't climb up the bean inheritance
                    tree unless the additional are specified via the Introspector. */
                    // if ( aBeanInfo.getAdditionalBeanInfo()!=null ) {
                    //     additionalBeanInfo.addAll( Arrays.asList( aBeanInfo.getAdditionalBeanInfo() ) );
                    // }
                }
            }

            return (PropertyDescriptor[]) propertyList.toArray(new PropertyDescriptor[propertyList.size()]);

        } catch (IntrospectionException e) {
            return null;
        }
    }

    //private static final Map<Class,BeanInfo> beanInfoCache= new ConcurrentHashMap<Class, BeanInfo>();
    
    public static BeanInfo getBeanInfo(final Class c) throws IntrospectionException {

        long t0= System.currentTimeMillis();
        
        // goal: get BeanInfo for the class, or the AccessLevelBeanInfo if it exists.
        
        BeanInfo beanInfo = null; // beanInfoCache.get(c);
        //if ( beanInfo!=null ) {
        //    logger.log(Level.FINER, "class {0} found in cache in {1} millis", new Object[] { c.getName(), System.currentTimeMillis()-t0 } );
        //    return beanInfo;
        //}
        
        if (c.getPackage() == null) { // e.g. String array
            beanInfo = Introspector.getBeanInfo(c);

            logger.log(Level.FINER, "using BeanInfo {0} for {1}", new Object[]{beanInfo.getClass().getName(), c.getName()});
        } else {

            String s;
            try {
                s = c.getName().substring(c.getPackage().getName().length() + 1);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            Class maybeClass = null;
            String beanInfoClassName;

            try {
                beanInfoClassName = c.getPackage() + "." + s + "BeanInfo";
                maybeClass = Class.forName(beanInfoClassName);
                logger.log(Level.FINER, "class found in {0} millis", ( System.currentTimeMillis()-t0 ) );
            } catch (ClassNotFoundException e) {
                try {
                    beanInfoClassName = "org.das2.beans." + s + "BeanInfo";
                    maybeClass = Class.forName(beanInfoClassName);
                    logger.log(Level.FINER, "org.das2.beans class found in {0} millis", ( System.currentTimeMillis()-t0 ) );
                } catch (ClassNotFoundException e2) {
                    beanInfo = Introspector.getBeanInfo(c);
                    beanInfoClassName = beanInfo.getClass().getName();
                    logger.log(Level.FINER, "introspector found class {0} found in {1} millis", new Object[] { c.getName(), System.currentTimeMillis()-t0 } );
                }
            }

            long dt= System.currentTimeMillis()-t0;
            
            if ( dt>500 ) {
                logger.log(Level.INFO, "class {0} found in {1} millis", new Object[] { c.getName(), dt } );
                // weird case where suddenly it's taking forever to resolve these.
            }
            
            logger.log(Level.FINER, "using BeanInfo {0} for {1}", new Object[]{beanInfoClassName, c.getName()});

            if (beanInfo == null) {
                try {
                    beanInfo = (BeanInfo) maybeClass.newInstance();
                } catch (IllegalAccessException ex) {
                    throw new RuntimeException(ex);
                } catch (InstantiationException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        
        //beanInfoCache.put(c,beanInfo);
        
        return beanInfo;
    }

    public static String[] getPropertyNames(PropertyDescriptor[] propertyList) {
        String[] result = new String[propertyList.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = ((PropertyDescriptor) propertyList[i]).getName();
        }
        return result;
    }

    /**
     * Use reflection to get a list of all the property names for the class.
     * The properties are returned in the order specified, and put inherited properties
     * at the end of the list.  This is motivated by the arbitrary order that the
     * Introspector presents the properties, which is in conflict with our desire to control
     * the property order.
     */
    public static String[] getPropertyNames(Class c) {
        PropertyDescriptor[] propertyList = getPropertyDescriptors(c);
        return getPropertyNames(propertyList);
    }

    /**
     * Returns an AccessLevelBeanInfo for the BeanInfo class, implementing the logic
     * of how to handle implicit properties.
     */
    public static AccessLevelBeanInfo asAccessLevelBeanInfo(BeanInfo beanInfo, Class beanClass) {
        if (beanInfo instanceof AccessLevelBeanInfo) {
            return (AccessLevelBeanInfo) beanInfo;
        } else {
            return ImplicitAccessLevelBeanInfo.create(beanInfo, beanClass);
        }

    }
}
