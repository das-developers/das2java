/* 
 * DasSerializeable.java
 *
 * Created on November 4, 2004, 5:35 PM
 */

package org.das2.graph;

import org.das2.dasml.FormBase;
import org.das2.DasNameException;
import org.das2.DasPropertyException;
import java.text.*;
import org.w3c.dom.*;

/**
 *
 * @author  Jeremy
 */
public interface DasSerializeable {
    
    Element getDOMElement( Document document );
    Object processElement( Element element, DasPlot parent, FormBase form) throws DasPropertyException, DasNameException, ParseException;
}
