/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.qstream;

import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.datum.DatumRange;
import org.das2.datum.DatumRangeUtil;
import org.das2.datum.Units;
import org.das2.datum.UnitsUtil;
import org.das2.util.LoggerManager;
import org.das2.qds.SemanticOps;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This serialize delegate assumes the formatted object is a time range,
 * and if it doesn't parse then it's a datum range.  Time Ranges
 * may be explicitly declared with "time:" prefix.
 *
 * @author jbf
 */
public class DatumRangeSerializeDelegate implements SerializeDelegate, XMLSerializeDelegate {

    private static final Logger logger= LoggerManager.getLogger("qstream");
            
    @Override
    public String format(Object o) {
        DatumRange dr= (DatumRange)o;
        Units u= dr.getUnits();
        if ( UnitsUtil.isTimeLocation(u) ) {
            // because of ambiguity of "1990 to 2000"
            return "time:"+ o;
        } else {
            return o.toString();
        }
    }

    @Override
    public Object parse(String typeId, String s) throws ParseException {
        if ( s.startsWith("time:") ) {
            return DatumRangeUtil.parseTimeRange(s.substring(5));
        }
        try {
            if ( !s.contains(" ") && !s.contains("E+") && !s.contains("e+") ) {
                s= s.replaceAll("\\+", " "); //TODO: verify 2E+13
            }
            return DatumRangeUtil.parseTimeRange(s);
        } catch ( ParseException e ) {
            int i = s.trim().lastIndexOf(" ");
            if ( i==-1 ) {
                return DatumRangeUtil.parseDatumRange(s, Units.dimensionless );
            } else {
                String sunits= s.substring(i).trim();
                try {
                    // if the last thing is a double, then assume dimensionless units.
                    Double d= Double.parseDouble(sunits);
                    return DatumRangeUtil.parseDatumRange(s, Units.dimensionless );
                } catch ( NumberFormatException ex ) {
                    Units u= SemanticOps.lookupUnits(sunits);
                    return DatumRangeUtil.parseDatumRange(s, u );
                }
            }
        }
    }

    @Override
    public String typeId(Class clas) {
        return "datumRange";
    }

    @Override
    public Element xmlFormat(Document doc, Object o) {
        DatumRange dr= (DatumRange)o;
        Element result= doc.createElement( typeId( o.getClass() ) );
        result.setAttribute( "units",dr.getUnits().toString() );
        if ( !UnitsUtil.isRatioMeasurement(dr.getUnits()) ) {
            result.setAttribute( "value",dr.toString() );
        } else {
            DatumRange test;
            try {
                test= DatumRangeUtil.parseDatumRange(dr.toString(), dr );
                if ( test.equals(dr) ) {
                    result.setAttribute( "value",dr.toString() );
                } else {
                    String svalue= String.format( "%s to %s", dr.min().value(), dr.max().value() );
                    result.setAttribute( "value", svalue );
                }
            } catch ( ParseException ex ) {
                logger.log( Level.WARNING, ex.getMessage(), ex );
                result.setAttribute( "value",dr.toString() );
            }
            
        }
        return result;
    }

    @Override
    public Object xmlParse(Element e) throws ParseException {
        String sunits= e.getAttribute("units");
        Units u= SemanticOps.lookupUnits(sunits);
        String s= e.getAttribute("value");
        return DatumRangeUtil.parseDatumRange(s, u );
    }

}
