/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.qstream;

import org.w3c.dom.Element;

/**
 *
 * @author jbf
 */
public class ExceptionDescriptor implements Descriptor {

    /**
     * will be called when writing streams.
     */
    public ExceptionDescriptor() {
    }

    /**
     * called when reading streams.
     * @param element
     */
    ExceptionDescriptor(Element element) {
        this.type= element.getAttribute("type");
        this.message= element.getAttribute("message");
        this.element= element;
    }

    private String type;
    
    String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    private String message;

    String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    Element element;
    
    public Element getDomElement() { //TODO: this needs some work.  It should be able create a DomElement, given the Document.
        return element;
    }

}
