/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.stream;

import org.w3c.dom.Element;

/**
 * Marker interface for Y
 * @author jbf
 */
public class StreamYDescriptor extends StreamScalarDescriptor {
    public StreamYDescriptor(Element element) throws StreamException {
        super(element);
    }
}
