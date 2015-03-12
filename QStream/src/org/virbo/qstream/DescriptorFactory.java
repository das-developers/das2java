/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.qstream;

import org.w3c.dom.Element;

/**
 * Factor for converting the XML packet into the Descriptor that implements.
 * @author jbf
 */
public interface DescriptorFactory {
    /**
     * create the Descriptor from the XML element.
     * @param element
     * @return the Descriptor.
     */
    Descriptor create( Element element );
}
