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
import java.beans.PropertyEditorSupport;

/**
 *
 * @author Jeremy
 */
public class UnitsEditor extends PropertyEditorSupport {
    
    @Override    
    public void setAsText(String str) throws IllegalArgumentException {
        setValue( Units.getByName(str) );
    }

}
