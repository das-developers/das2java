
package org.das2.graph;

/**
 * Anchor Type
 * @author jbf
 */
public enum AnchorType {
    /**
     * anchored by row and column
     */
    CANVAS, 
    /**
     * anchored by datum ranges
     */
    PLOT,   
    /**
     * anchored to the data (not implemented, same as PLOT for now).
     */
    DATA,   
}
