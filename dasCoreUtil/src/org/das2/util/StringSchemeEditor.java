
package org.das2.util;

import java.awt.Component;

/**
 * Where a String holds a state, and an editor is provided to edit
 * the state.
 * @author jbf
 */
public interface StringSchemeEditor {
    public void setValue( String v );
    public String getValue();
    public Component getComponent();
}
