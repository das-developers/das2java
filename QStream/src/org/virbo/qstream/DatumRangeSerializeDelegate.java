/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.qstream;

import java.text.ParseException;
import org.das2.datum.DatumRange;
import org.das2.datum.DatumRangeUtil;
import org.das2.datum.Units;
import org.das2.datum.UnitsUtil;
import org.virbo.dataset.SemanticOps;

/**
 * This serialize delegate assumes the formatted object is a time range,
 * and if it doesn't parse then it's a datum range.  Time Ranges
 * may be explicitly declared with "time:" prefix.
 *
 * @author jbf
 */
public class DatumRangeSerializeDelegate implements SerializeDelegate {

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

    public Object parse(String typeId, String s) throws ParseException {
        if ( s.startsWith("time:") ) {
            return DatumRangeUtil.parseTimeRange(s.substring(5));
        }
        try {
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

    public String typeId(Class clas) {
        return "datumRange";
    }

}
