/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.qstream;

import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    protected static final Logger logger= Logger.getLogger("qstream");
    
    @Override
    public String format(Object o) {
        EnumerationUnits eu= (EnumerationUnits)o;
        Map<Integer,Datum> values= eu.getValues();
        StringBuilder buf= new StringBuilder();
        buf.append("").append(eu.getId()).append("[");
        boolean useSemiSpaceDelimiters= false;
        for ( Entry<Integer,Datum> e: values.entrySet() ) {
            Integer i= e.getKey();
            String s= e.getValue().toString();
            if ( s.trim().length()==0 ) useSemiSpaceDelimiters= true; // we need an alternate delimiter, because confusion with delimiters.  Note space delimiters are easier to read anyway.
        }
        for ( Entry<Integer,Datum> e: values.entrySet() ) {
            Integer i= e.getKey();
            String s= e.getValue().toString();
            s= s.replaceAll("::", ":"); // :: is my delimiter
            s= s.replaceAll("; ", ";"); // "; " is an alternate delimiter
            if ( s.length()>0 ) {
                buf.append("").append(i).append(":").append(s);
                if ( i<values.size() ) buf.append( useSemiSpaceDelimiters ? "; " : "::");
            }
        }
        buf.append("]");
        return buf.toString();
    }

    @Override
    public Object parse( String typeId, String s ) {
        Pattern p= Pattern.compile("(.+?)(\\[(.*)\\])?");
        Matcher m= p.matcher(s);
        if ( !m.matches() ) {
            throw new IllegalArgumentException("bad format!");
        } else {
            String id= m.group(1);
            EnumerationUnits u;
            try {
                u= (EnumerationUnits) Units.getByName(s); //TODO: why???
            } catch ( IllegalArgumentException ex ) {
                u= new EnumerationUnits(id);   // getByName always fails, and then this is invoked, creating multiple units with the same name.  This happens to work, but it's not clean...
                // http://www-pw.physics.uiowa.edu/~jbf/autoplot/data/qds/agg/demoEventsInlineEnumeration.vap  shows why this needs to be cleaned up.
            }
            String values= m.group(3);
            String[] ss;
            if ( values==null ) {
                ss= new String[0];
            } else {
                ss= values.split("::",-2);
            }
            if ( ss.length==1 ) {
                ss= values.split("; ",-2);
            }
            for ( String nv: ss ) {
                if ( nv.trim().length()>0 ) {
                    int idx= nv.indexOf(":");
                    if ( idx>-1 ) {
                        try {
                            int ival= Integer.parseInt(nv.substring(0,idx));
                            String sval= nv.substring(idx+1);
                            u.createDatum(ival,sval);
                        } catch ( NumberFormatException ex ) {
                            logger.log(Level.WARNING, "NumberFormatException caught: {0}", nv);
                        }
                    } else {
                        logger.log(Level.WARNING, "Bad index caught: {0}", nv);
                    }
                }
            }
            return u;
        }
    }

    @Override
    public String typeId( Class clas ) {
        return "enumerationUnit";
    }

}
