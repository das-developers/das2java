/*
 * UnitsEditor.java
 *
 * Created on June 30, 2005, 10:23 AM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package org.das2.beans;

import org.das2.datum.Units;
import java.awt.Rectangle;
import java.beans.PropertyEditorSupport;
import java.beans.XMLEncoder;

/**
 *
 * @author Jeremy
 */
public class RectangleEditor extends PropertyEditorSupport {
    
    public void setAsText(String str) throws IllegalArgumentException {        
        setValue( Units.getByName(str) );
    }

    public String getAsText() {
        return String.valueOf( getValue() );
    }
}
