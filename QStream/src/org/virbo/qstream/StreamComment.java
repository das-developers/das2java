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
public class StreamComment implements Descriptor {
    private Element element;
    
    StreamComment( Element element ) {
        this.element= element;
    }

    public Element getDomElement() {
        return element;
    }
}
