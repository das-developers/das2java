/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.qstream;

import java.text.ParseException;
import org.das2.datum.DatumRangeUtil;
import org.das2.datum.Units;
import org.virbo.dataset.SemanticOps;

/**
 * This serialize delegate assumes the formatted object is a time range,
 * and if it doesn't parse then it's a datum range.
 * @author jbf
 */
public class DatumRangeSerializeDelegate implements SerializeDelegate {

    public String format(Object o) {
        return o.toString();
    }

    public Object parse(String typeId, String s) throws ParseException {
        try {
            return DatumRangeUtil.parseTimeRange(s);
        } catch ( ParseException e ) {
            int i = s.trim().lastIndexOf(" ");
            if ( i==-1 ) {
                return DatumRangeUtil.parseDatumRange(s, Units.dimensionless );
            } else {
                String sunits= s.substring(i).trim();
                if ( Character.isLetter(sunits.charAt(0) ) ) {
                    Units u= SemanticOps.lookupUnits(sunits);
                    return DatumRangeUtil.parseDatumRange(s, u );
                } else {
                    return DatumRangeUtil.parseDatumRange(s, Units.dimensionless );
                }
            }
        }
    }

    public String typeId(Class clas) {
        return "datumRange";
    }

}
