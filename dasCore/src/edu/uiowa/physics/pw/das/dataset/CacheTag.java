package edu.uiowa.physics.pw.das.dataset;

import edu.uiowa.physics.pw.das.datum.*;

public class CacheTag {
    
    Datum start;
    
    Datum end;
    
    Datum resolution;
    
    public CacheTag(Datum start, Datum end, Datum resolution) {
        this.start= start;
        this.end= end;
        this.resolution= resolution;
    }
    
    public String toString() {
        return start + " " + end + " @ " + DatumUtil.asOrderOneUnits(resolution);
    }
}

