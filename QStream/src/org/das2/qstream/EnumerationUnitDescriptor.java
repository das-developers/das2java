/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.qstream;

import org.w3c.dom.Element;

/**
 * This handles enumeration units that are defined within the stream (not within
 * a header).
 * @author jbf
 */
public class EnumerationUnitDescriptor implements Descriptor {

    /**
     * will be called when writing streams.
     */
    public EnumerationUnitDescriptor() {
    }

    /**
     * called when reading streams.
     * @param element
     */
    EnumerationUnitDescriptor(Element element) {
        this.name= element.getAttribute("name"); // the context for the value -> label.
        this.svalue= element.getAttribute("value");
        this.label= element.getAttribute("label");
        this.element= element;
    }

    private String name;
    
    String getName() {
        return name;
    }

    public void setName(String type) {
        this.name = type;
    }

    private String label;

    String getLabel() {
        return label;
    }

    public void setMessage(String label) {
        this.label = label;
    }
    
    private String svalue;

    public void setValue(double value) {
        this.svalue = String.valueOf(value);
    }

    public double getValue() {
        return Double.parseDouble(svalue);
    }

    Element element;
  
    @Override
    public Element getDomElement() {
        return element;
    }

}
