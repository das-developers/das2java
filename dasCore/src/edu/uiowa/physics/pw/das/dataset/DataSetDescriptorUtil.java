/*
 * DataSetDescriptorUtil.java
 *
 * Created on October 23, 2003, 10:11 AM
 */

package edu.uiowa.physics.pw.das.dataset;

import edu.uiowa.physics.pw.das.*;
import edu.uiowa.physics.pw.das.client.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

/**
 *
 * @author  jbf
 */
public class DataSetDescriptorUtil {
    
    private static final Pattern CLASS_ID = Pattern.compile("class:([a-zA-Z\\.]+)(?:\\?(.*))?");
    private static final Pattern NAME_VALUE = Pattern.compile("([_0-9a-zA-Z%+.]+)=([_0-9a-zA-Z%+./]+)");
            
    public static DataSetDescriptor create( String dataSetID ) throws DasException {
        java.util.regex.Matcher classMatcher = CLASS_ID.matcher(dataSetID);
        if (classMatcher.matches()) {
            try {
                String className = classMatcher.group(1);
                String argString = classMatcher.group(2);
                String[] argList;
                if (argString != null && argString.length() > 0) {
                    argList = argString.split("&");
                }
                else {
                    argList = new String[0];
                }
                URLDecoder decoder = new URLDecoder();
                Map argMap = new HashMap();
                for (int index = 0; index < argList.length; index++) {
                    Matcher argMatcher = NAME_VALUE.matcher(argList[index]);
                    if (argMatcher.matches()) {
                        argMap.put(decoder.decode(argMatcher.group(1), "UTF-8"),
                        decoder.decode(argMatcher.group(2), "UTF-8"));
                    }
                    else {
                        throw new NoSuchDataSetException("Invalid argument: " + argList[index]);
                    }
                }
                Class dsdClass = Class.forName(className);
                Method method = dsdClass.getMethod("newDataSetDescriptor", new Class[]{java.util.Map.class});
                if (!Modifier.isStatic(method.getModifiers())) {
                    throw new NoSuchDataSetException("getDataSetDescriptor must be static");
                }
                return (DataSetDescriptor)method.invoke(null, new Object[]{argMap});
            }
            catch (ClassNotFoundException cnfe) {
                DataSetDescriptorNotAvailableException dsdnae =
                new DataSetDescriptorNotAvailableException(cnfe.getMessage());
                dsdnae.initCause(cnfe);
                throw dsdnae;
            }
            catch (NoSuchMethodException nsme) {
                DataSetDescriptorNotAvailableException dsdnae =
                new DataSetDescriptorNotAvailableException(nsme.getMessage());
                dsdnae.initCause(nsme);
                throw dsdnae;
            }
            catch (InvocationTargetException ite) {
                DataSetDescriptorNotAvailableException dsdnae =
                new DataSetDescriptorNotAvailableException(ite.getTargetException().getMessage());
                dsdnae.initCause(ite.getTargetException());
                throw dsdnae;
            }
            catch (IllegalAccessException iae) {
                DataSetDescriptorNotAvailableException dsdnae =
                new DataSetDescriptorNotAvailableException(iae.getMessage());
                dsdnae.initCause(iae);
                throw dsdnae;
            }
            catch (UnsupportedEncodingException uee) {
                throw new RuntimeException(uee);
            }
        }
        else if (!dataSetID.startsWith("http://")) {
            dataSetID = "http://www-pw.physics.uiowa.edu/das/dasServer?" + dataSetID;
        }
        try {
            DataSetDescriptor result = DasServer.createDataSetDescriptor(new URL(dataSetID));
            result.setDataSetId(dataSetID);
            return result;
        }
        catch (MalformedURLException mue) {
            throw new DasIOException(mue.getMessage());
        }
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
    }
    
}
