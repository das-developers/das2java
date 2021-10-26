
package org.das2.components.propertyeditor;

import java.awt.Component;

/**
 * something that can hold a string.
 * @author jbf
 */
public interface StringSchemeEditor {
    public void setValue( String v );
    public String getValue();
    public Component getComponent();
}
