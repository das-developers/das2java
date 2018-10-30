/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.qstream;

import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.datum.Datum;
import org.das2.datum.DatumUtil;
import org.das2.datum.Units;
import org.das2.datum.UnitsUtil;
import org.das2.util.LoggerManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This serialize delegate handles Datums.
 *
 * @author jbf
 */
public class DatumSerializeDelegate implements SerializeDelegate, XMLSerializeDelegate {

    private static final Logger logger= LoggerManager.getLogger("qstream");
            
    @Override
    public String format(Object o) {
        return o.toString();
    }

    @Override
    public Object parse(String typeId, String s) throws ParseException {
        return DatumUtil.parse(s);
    }

    @Override
    public String typeId(Class clas) {
        return "datum";
    }

    @Override
    public Element xmlFormat(Document doc, Object o) {
        Datum d= (Datum)o;
        Element result= doc.createElement( typeId( o.getClass() ) );
        result.setAttribute( "units",d.getUnits().toString() );
        if ( !UnitsUtil.isRatioMeasurement(d.getUnits()) ) {
            result.setAttribute( "value",d.toString() );
        } else {
            Datum test;
            try {
                test= DatumUtil.parse( d.toString() );
                if ( test.equals(d) ) {
                    result.setAttribute( "value",d.toString() );
                } else {
                    logger.log( Level.WARNING, "parse of format doesn't check out" );
                    result.setAttribute( "value",d.toString() );   
                }
            } catch ( ParseException ex ) {
                logger.log( Level.WARNING, ex.getMessage(), ex );
                result.setAttribute( "value",d.toString() );
            }
            
        }
        return result;
    }

    @Override
    public Object xmlParse(Element e) throws ParseException {
        String sunits= e.getAttribute("units");
        Units u= Units.lookupUnits(sunits);
        String s= e.getAttribute("value");
        return u.parse(s);
    }

}
