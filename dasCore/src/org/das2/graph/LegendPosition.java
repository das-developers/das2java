/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.graph;

import javax.swing.Icon;
import org.das2.components.propertyeditor.Enumeration;

/**
 * enumeration of legend positions
 * @author jbf
 */
public enum LegendPosition implements Enumeration {
    // no NW because of error messages.
    NE, // corner of plot
    SW,
    SE;
    // bugs with bounds prevent OutsideNE; // outside right position on plot

    public Icon getListIcon() {
        return null;
    }
    

}
