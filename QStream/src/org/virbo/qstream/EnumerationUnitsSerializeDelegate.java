/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.qstream;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.das2.datum.Datum;
import org.das2.datum.EnumerationUnits;
import org.das2.datum.Units;

/**
 *
 * @author jbf
 */
public class EnumerationUnitsSerializeDelegate implements SerializeDelegate {

    @Override
    public String format(Object o) {
        EnumerationUnits eu= (EnumerationUnits)o;
        Map<Integer,Datum> values= eu.getValues();
        StringBuilder buf= new StringBuilder();
        buf.append("").append(eu.getId()).append("[");
        for ( Integer i: values.keySet() ) {
            String s= values.get(i).toString();
            s= s.replaceAll("::", ":"); // :: is my delimiter
            buf.append("").append(i).append(":").append(s);
            if ( i<values.size() ) buf.append("::");
        }
        buf.append("]");
        return buf.toString();
    }

    @Override
    public Object parse( String typeId, String s ) {
        Pattern p= Pattern.compile("(.+?)\\[(.*)\\]");
        Matcher m= p.matcher(s);
        if ( !m.matches() ) {
            throw new IllegalArgumentException("bad format!");
        } else {
            String id= m.group(1);
            EnumerationUnits u;
            try {
                u= (EnumerationUnits) Units.getByName(s);
            } catch ( IllegalArgumentException ex ) {
                u= new EnumerationUnits(id);
            }
            String values= m.group(2);
            String[] ss= values.split("::");
            for ( String nv: ss ) {
                int idx= nv.indexOf(":");
                int ival= Integer.parseInt(nv.substring(0,idx));
                String sval= nv.substring(idx+1);
                u.createDatum(ival,sval);
            }
            return u;
        }
    }

    @Override
    public String typeId( Class clas ) {
        return "enumerationUnit";
    }

}
