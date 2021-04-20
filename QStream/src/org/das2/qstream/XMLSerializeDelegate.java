/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.qstream;

import java.text.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Extension to SerializeDelegate that allows for embedding object within XML.
 * @author jbf
 */
public interface XMLSerializeDelegate {
    Element xmlFormat( Document doc, Object o );
    Object xmlParse( Element e ) throws ParseException ;
}
