/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.qstream;

import java.text.ParseException;
import org.das2.datum.CacheTag;
import org.das2.datum.DatumRangeUtil;
import org.das2.datum.Units;

/**
 * Serialize CacheTags, which have pretty much always been for time tags.
 * TODO: handle non-time cache tags.
 * @author jbf
 */
public class CacheTagSerializeDelegate implements SerializeDelegate {

    public String format(Object o) {
        CacheTag tag= (CacheTag)o;
        return tag.toString();
    }

    public Object parse(String typeId, String s) throws ParseException {
        int i= s.indexOf("@");
        String sres= s.substring(i+1);
        String srange= s.substring(0,i);
        
        return new CacheTag(DatumRangeUtil.parseTimeRange(srange), sres.trim().equals(CacheTag.INTRINSIC) ? null : Units.seconds.parse(sres));
        
    }

    public String typeId(Class clas) {
        return "cacheTag";
    }

}
