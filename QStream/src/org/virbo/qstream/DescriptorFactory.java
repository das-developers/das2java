/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.qstream;

import org.w3c.dom.Element;

/**
 *
 * @author jbf
 */
interface DescriptorFactory {
    Descriptor create( Element element );
}
