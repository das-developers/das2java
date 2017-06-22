/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.qstream;

import org.w3c.dom.Element;

/**
 * model elements of the QStream, such as StreamDescriptor, PacketDescriptor,
 * and EnumerationUnitDescriptor.
 * @author jbf
 */
public interface Descriptor {
    Element getDomElement();
}
