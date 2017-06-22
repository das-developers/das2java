/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.qstream;

import org.das2.datum.Units;
import org.das2.qds.SemanticOps;

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
        u= SemanticOps.lookupUnits(s);
        return u;
    }

    public String typeId(Class clas) {
        return "units";
    }
    
    
}
