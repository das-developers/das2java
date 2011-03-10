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
    NW, 
    NE, // corner of plot
    SW,
    SE,
    OutsideNE;
    // bugs with bounds prevent OutsideNE; // outside right position on plot

    public Icon getListIcon() {
        return null;
    }
    

}
