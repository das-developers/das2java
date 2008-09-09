/*
 * BeanInfoUtil.java
 *
 * Created on May 31, 2005, 11:49 AM
 */
package org.das2.beans;

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
import edu.uiowa.physics.pw.das.graph.*;
import org.das2.system.DasLogger;
import org.das2.util.ClassMap;
import java.awt.Color;
import java.beans.*;
import java.util.*;
import java.util.logging.Logger;

/**
 *
 * @author Jeremy
 */
public class BeansUtil {

    static Logger log = DasLogger.getLogger(DasLogger.SYSTEM_LOG);
    

    static {
        String[] beanInfoSearchPath = {"edu.uiowa.physics.pw.das.beans", "sun.beans.infos"};
        if (DasApplication.hasAllPermission()) {
            Introspector.setBeanInfoSearchPath(beanInfoSearchPath);
        }
    }
    static ClassMap editorRegistry = new ClassMap();

    private static void registerEditor(Class beanClass, Class editorClass) {
        PropertyEditorManager.registerEditor(beanClass, editorClass);
        editorRegistry.put(beanClass, editorClass);
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
    static HashSet nullPropertyEditors = new HashSet();

    public static java.beans.PropertyEditor findEditor(Class propertyClass) {
        java.beans.PropertyEditor result = null;
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
                        ex.printStackTrace();
                    } catch (IllegalAccessException ex) {
                        ex.printStackTrace();
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
     */
    public static PropertyDescriptor[] getPropertyDescriptors(Class c) {
        Set excludePropertyNames;
        excludePropertyNames = new HashSet();
        excludePropertyNames.add("class");
        excludePropertyNames.add("listLabel");
        excludePropertyNames.add("listIcon");

        try {
            BeanInfo beanInfo = getBeanInfo(c);

            List propertyList = new ArrayList();

            PropertyDescriptor[] pdthis;
            try {
                pdthis = beanInfo.getPropertyDescriptors();
            } catch (IllegalStateException e) {
                throw new RuntimeException(e);
            }

            for (int i = 0; i < pdthis.length; i++) {
                boolean isWriteable = pdthis[i].getWriteMethod() != null;
                boolean isUseable = pdthis[i].getReadMethod() != null && !excludePropertyNames.contains(pdthis[i].getName());
                if (isUseable || (pdthis[i] instanceof IndexedPropertyDescriptor)) {
                    propertyList.add(pdthis[i]);
                }
            }

            if (beanInfo.getAdditionalBeanInfo() != null) {
                List additionalBeanInfo = new ArrayList(Arrays.asList(beanInfo.getAdditionalBeanInfo()));
                while (additionalBeanInfo.size() > 0) {
                    BeanInfo aBeanInfo = (BeanInfo) additionalBeanInfo.remove(0);
                    pdthis = aBeanInfo.getPropertyDescriptors();
                    for (int i = 0; i < pdthis.length; i++) {
                        String name = pdthis[i].getName();
                        boolean isWriteable = pdthis[i].getWriteMethod() != null;
                        boolean isUseable = pdthis[i].getReadMethod() != null && !excludePropertyNames.contains(name);
                        if (isUseable || (pdthis[i] instanceof IndexedPropertyDescriptor)) {
                            propertyList.add(pdthis[i]);
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

    public static BeanInfo getBeanInfo(final Class c) throws IntrospectionException {

        // goal: get BeanInfo for the class, or the AccessLevelBeanInfo if it exists.
        BeanInfo beanInfo = null;

        if (c.getPackage() == null) { // e.g. String array
            beanInfo = Introspector.getBeanInfo(c);

            log.finer("using BeanInfo " + beanInfo.getClass().getName() + " for " + c.getName());
        } else {

            String s;
            try {
                s = c.getName().substring(c.getPackage().getName().length() + 1);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            Class maybeClass = null;
            String beanInfoClassName = null;

            try {
                beanInfoClassName = c.getPackage() + "." + s + "BeanInfo";
                maybeClass = Class.forName(beanInfoClassName);
            } catch (ClassNotFoundException e) {
                try {
                    beanInfoClassName = "edu.uiowa.physics.pw.das.beans." + s + "BeanInfo";
                    maybeClass = Class.forName(beanInfoClassName);
                } catch (ClassNotFoundException e2) {
                    beanInfo = Introspector.getBeanInfo(c);
                    beanInfoClassName = beanInfo.getClass().getName();
                }
            }

            log.finer("using BeanInfo " + beanInfoClassName + " for " + c.getName());

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
     * at the end of the list.  This is motivated by the arbitary order that the
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
