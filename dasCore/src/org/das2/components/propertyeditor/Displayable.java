package org.das2.components.propertyeditor;

import java.awt.Graphics2D;
import javax.swing.Icon;

/** 
 * Type-safe enumerations that are used as property types
 * that are editable with a PropertyEditor should
 * implement this interface.
 */
public interface Displayable {
    
    /** 
     * return a <code>String</code> that will help the user
     * identify this item when choosing from a list.
     * @return the list label.
     */
    String getListLabel();
    
    /** 
     * An icon can be provided that will be shown in a list
     * along with the textual description of the element.
     * This method should return <code>null</code> if there
     * is no icon available.
     *
     * @return the icon.
     */
    Icon getListIcon();
    
    /**
     * implement this to provide nice drawing of icon on printing graphics context.
     * @param g the graphics context.
     * @param x the x position, typically 0.
     * @param y the y position, typically 0.
     */
    void drawListIcon( Graphics2D g, int x, int y );
}

