
package org.das2.util;

import java.awt.Component;

/**
 * Where a String holds a state, and an editor is provided to edit
 * the state.
 * @author jbf
 */
public interface StringSchemeEditor {
    
    /**
     * set the current string value, representing a state.
     * @param v the state
     */
    public void setValue( String v );
    
    /**
     * return the string represented by the GUI state
     * @return the string represented by the GUI state 
     */
    public String getValue();
    
    /**
     * return the editor for the string
     * @return the editor for the string
     */
    public Component getComponent();
    
    /**
     * some clients of this code may provide a context for the string, so for example
     * a preview could be created or units detected.  This method can be ignored,
     * and the code should assume a context will be provided.  This is typically
     * the object containing the property.
     * @param o 
     */
    public void setContext( Object o );
}
