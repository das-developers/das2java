package org.das2.components.propertyeditor;

import javax.swing.Icon;

/** Type-safe enumerations that are used as property types
 * that are editable with a PropertyEditor should
 * implement this interface.
 *
 */
public interface Enumeration {
    
    /** Type-safe Enumerations implementing this interface
     * should override the toString() method to return a
     * <code>String</code> that will be helpful to the user
     * when choosing this as an option from a list.
     *
     * @return a concise string representation
     */
    @Override
    String toString();
    //TODO: getListLabel() better, because toString should be reserved for programmers.
    
    /** An icon can be provided that will be shown in a list
     * along with the textual description of the element.
     * This method should return <code>null</code> if there
     * is no icon available.
     *
     * @return the icon
     */
    Icon getListIcon();
    
}

