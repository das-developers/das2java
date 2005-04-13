package edu.uiowa.physics.pw.das.dataset;

import edu.uiowa.physics.pw.das.datum.*;

public class CacheTag {
    
    DatumRange range;    
    Datum resolution;
    
    public CacheTag(Datum start, Datum end, Datum resolution) {
        this( new DatumRange( start, end ), resolution );
    }
    
    public CacheTag( DatumRange range, Datum resolution) {
        this.range= range;
        this.resolution= resolution;
    }
    
    public String toString() {
        return range + " @ " + ( resolution==null ? "intrisic" : ""+DatumUtil.asOrderOneUnits(resolution) );
    }
    
    /**
     * @returns true if the tag has a lower resolution and its bounds are within
     * this.
     */
    public boolean contains( CacheTag tag ) {
        return ( this.range.contains( tag.range ) 
          && ( this.resolution==null 
              || ( tag.resolution!=null && this.resolution.le( tag.resolution ) ) 
             ) );            
    }
}

