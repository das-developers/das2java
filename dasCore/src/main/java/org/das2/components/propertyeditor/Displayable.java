package org.das2.components.propertyeditor;

import javax.swing.Icon;

/** Type-safe enumerations that are used as property types
 * that are editable with a PropertyEditor should
 * implement this interface.
 *
 */
public interface Displayable {
    
    /** return a <code>String</code> that will help the user
     * identify this item when choosing from a list.
     */
    String getListLabel();
    
    /** An icon can be provided that will be shown in a list
     * along with the textual description of the element.
     * This method should return <code>null</code> if there
     * is no icon available.
     *
     */
    Icon getListIcon();
    
}

