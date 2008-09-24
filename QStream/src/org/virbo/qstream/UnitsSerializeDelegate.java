/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.qstream;

import org.das2.datum.NumberUnits;
import org.das2.datum.Units;

/**
 *
 * @author jbf
 */
public class UnitsSerializeDelegate implements SerializeDelegate {

    public String format(Object o) {
        return o.toString();
    }

    public Object parse(String typeId,String s) {
        Units u;
        try {
            u= Units.getByName(s);
        } catch ( IllegalArgumentException ex ) {
            u= new NumberUnits(s);
        }
        return u;
    }

    public String typeId(Class clas) {
        return "units";
    }
    
    
}
