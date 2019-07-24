
package org.das2.graph;

/**
 * control for the error bar rendering.
 * @author jbf
 */
public enum ErrorBarType {
    /**
     * vertical or horizontal bar showing the extent of the error level.
     */
    BAR, 
    
    /**
     * vertical or horizontal bar but with perpendicular serif bar highlighting the extent.
     */
    SERIF_BAR, 
    
    /**
     * shade the error region rather than using a bar.
     */
    SHADE
}
