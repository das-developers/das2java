/* 
 * DasSerializeable.java
 *
 * Created on November 4, 2004, 5:35 PM
 */

package edu.uiowa.physics.pw.das.graph;

import edu.uiowa.physics.pw.das.*;
import edu.uiowa.physics.pw.das.dasml.*;
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
