/*
 * MapEditor.java
 *
 * Created on September 27, 2005, 1:37 PM
 *
 *
 */

package org.das2.components.propertyeditor;

import java.util.Iterator;
import java.util.Map;
import javax.swing.JTextArea;

/**
 *
 * @author Jeremy
 */
public class MapEditor implements java.beans.PropertyEditor {
    Map value;
    
    public void addPropertyChangeListener(java.beans.PropertyChangeListener propertyChangeListener) {
    }

    public String getAsText() {
        return "lookup";
    }

    public java.awt.Component getCustomEditor() {
        JTextArea result= new JTextArea(20,6);
        for ( Iterator i= value.keySet().iterator(); i.hasNext(); ) {
            Object key= i.next();
            Object value= i.next();
            result.append(""+key+"=\t"+value+"\n");
        }
        return result;
    }

    public String getJavaInitializationString() {
        return "lookup";
    }

    public String[] getTags() {
        return null;
    }

    public Object getValue() {
        return value;
    }

    public boolean isPaintable() {
        return false;
    }

    public void paintValue(java.awt.Graphics graphics, java.awt.Rectangle rectangle) {
        
    }

    public void removePropertyChangeListener(java.beans.PropertyChangeListener propertyChangeListener) {
    }

    public void setAsText(String str) throws IllegalArgumentException {                
    }

    public void setValue(Object obj) {
        this.value= (Map)obj;
    }

    public boolean supportsCustomEditor() {
        return true;
    }
    
}
