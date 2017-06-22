/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.qstream;

import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.das2.datum.Datum;
import org.das2.datum.Units;
import org.das2.qds.DRank0DataSet;
import org.das2.qds.DataSetUtil;
import org.das2.qds.QDataSet;
import org.das2.qds.RankZeroDataSet;

/**
 *
 * @author jbf
 */
public class Rank0DataSetSerializeDelegate implements SerializeDelegate {

    public String format(Object o) {
        QDataSet ds= (QDataSet)o;
        if ( ds.rank()>0 ) {
            throw new IllegalArgumentException("rank>0 in Rank0DataSetSerializeDelegate");
        }
        Map<String,Object> props= DataSetUtil.getProperties(ds);
        Units u= (Units) ds.property(QDataSet.UNITS);
        if ( u==null ) u= Units.dimensionless;
        Datum d= DataSetUtil.asDatum(ds);
        String svalue= d.getFormatter().format(d, u); // we'll provide units context
        if ( svalue.contains(" ") ) {
            throw new RuntimeException("formatted value contains string");
        }
        StringBuilder sb= new StringBuilder( d.getFormatter().format( d, d.getUnits() ) );
        for ( Entry<String,Object> e: props.entrySet() ) {
            Object value= e.getValue();
            SerializeDelegate sd= SerializeRegistry.getDelegate(value.getClass());
            if ( sd!=null && !(value instanceof RankZeroDataSet ) ) { // TODO: review this--why should it now be a rank 0 dataset?
                sb.append(" ").append(sd.typeId(value.getClass())).append(":").append(e.getKey()).append("=").append(sd.format(value));
            }
        }
        return sb.toString(); // make sure time units are included with formatted time.
    }

    public Object parse(String typeId, String s) throws ParseException {
        s = s.trim();
        int i = s.indexOf(" ");
        if (i == -1) {
            return DataSetUtil.asDataSet(Double.parseDouble(s));
        } else {
            String svalue = s.substring(0, i);
            String smeta = s.substring(i + 1);
            Pattern p = Pattern.compile("\\s*(\\S+)\\:([A-Z]+)=(\\S+)");
            Matcher m = p.matcher(smeta);
            Map<String, Object> props = new LinkedHashMap<String, Object>();
            while (m.find()) {
                String proptype = m.group(1);
                String propname = m.group(2);
                String propsvalue = m.group(3);
                SerializeDelegate sd = SerializeRegistry.getByName(proptype);
                Object value = sd.parse(proptype, propsvalue);
                props.put(propname, value);
            }
            Units u= (Units) props.get(QDataSet.UNITS);
            if ( u==null ) u= Units.dimensionless;
            DRank0DataSet result;
            if ( u==Units.dimensionless ) {
                result= DRank0DataSet.create(u.parse(svalue));
            } else {
                result= DRank0DataSet.create(u.parse(svalue).doubleValue(u),u);
            }
            for ( Entry<String,Object> e:props.entrySet() ) {
                if ( e.getKey().equals(QDataSet.UNITS) ) {
                    result.putProperty( e.getKey(), e.getValue() );
                }
            }
            return result;
        }
    }

    public String typeId(Class clas) {
        return "rank0dataset";
    }

}
