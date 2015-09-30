
package org.das2.graph;

import javax.swing.Icon;
import org.das2.components.propertyeditor.Enumeration;

/**
 * enumeration of legend positions
 * @author jbf
 */
public enum LegendPosition implements Enumeration {
    NW, 
    NE, // corner of plot
    SW,
    SE,
    OutsideNE;

    @Override
    public Icon getListIcon() {
        return null;
    }
    

}
