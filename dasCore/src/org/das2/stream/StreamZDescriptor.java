/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.stream;

import org.w3c.dom.Element;

/**
 * Marker for Z.
 * @author jbf
 */
public class StreamZDescriptor extends StreamScalarDescriptor {
    public StreamZDescriptor(Element element) throws StreamException {
        super(element);
    }
}
