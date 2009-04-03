/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.qstream;

import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.das2.datum.Datum;
import org.das2.datum.DatumUtil;
import org.das2.datum.NumberUnits;
import org.das2.datum.Units;
import org.virbo.dataset.DRank0DataSet;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.RankZeroDataSet;

/**
 *
 * @author jbf
 */
public class Rank0DataSetSerializeDelegate implements SerializeDelegate {

    public String format(Object o) {
        RankZeroDataSet ds= (RankZeroDataSet)o;
        Map<String,Object> props= DataSetUtil.getProperties(ds);
        Units u= (Units) ds.property(QDataSet.UNITS);
        Datum d= DataSetUtil.asDatum((RankZeroDataSet)o);
        String svalue= d.getFormatter().format(d, u); // we'll provide units context
        if ( svalue.contains(" ") ) {
            throw new RuntimeException("formatted value contains string");
        }
        StringBuffer sb= new StringBuffer( d.getFormatter().format( d, d.getUnits() ) );
        for ( Entry<String,Object> e: props.entrySet() ) {
            Object value= e.getValue();
            SerializeDelegate sd= SerializeRegistry.getDelegate(value.getClass());
            if ( sd!=null && !(value instanceof RankZeroDataSet ) ) {
                sb.append(" "+sd.typeId(value.getClass()) + ":" + e.getKey() + "=" + sd.format(value) );
            }
        }
        return sb.toString(); // make sure time units are included with formatted time.
    }

    public Object parse(String typeId, String s) throws ParseException {
        int i= s.indexOf(" ");
        String svalue= s.substring(0,i);
        String smeta= s.substring(i+1);
        Pattern p= Pattern.compile("\\s*(\\S+)\\:([A-Z]+)=(\\S+)");
        Matcher m= p.matcher(smeta);
        Map<String,Object> props= new LinkedHashMap<String,Object>();
        while ( m.find() ) {
            String proptype= m.group(1);
            String propname= m.group(2);
            String propsvalue= m.group(3);
            SerializeDelegate sd= SerializeRegistry.getByName(proptype);
            Object value= sd.parse(proptype, propsvalue);
            props.put(propname, value);
        }
        Units u= (Units) props.get(QDataSet.UNITS);
        if ( u==null ) u= Units.dimensionless;
        DRank0DataSet result= DRank0DataSet.create(u.parse(svalue));
        for ( Entry<String,Object> e:props.entrySet() ) {
            result.putProperty( e.getKey(), e.getValue() );
        }
        return result;
    }

    public String typeId(Class clas) {
        return "rank0dataset";
    }

}
