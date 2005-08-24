package edu.uiowa.physics.pw.das.dataset;

import edu.uiowa.physics.pw.das.datum.*;

public class CacheTag {
    
    DatumRange range;    
    Datum resolution;
    
    public static CacheTag append( CacheTag tag1, CacheTag tag2 ) {
        Datum res;
        if ( tag1.resolution==null && tag2.resolution==null ) {
            res= null;
        } else {
            if ( tag1.resolution!=null && tag2.resolution!=null ) {
                res= tag1.resolution.gt( tag2.resolution ) ? tag1.resolution : tag2.resolution;                
            } else {
                res= tag1.resolution==null ? tag2.resolution : tag1.resolution;
            }
        }        
        
        Datum min;
        Datum max;
        if ( !tag1.range.intersects(tag2.range) ) {
            if ( tag1.range.min().lt( tag2.range.min() ) ) {
                CacheTag temp= tag1;
                tag1= tag2;
                tag2= temp;
            }
            if ( tag1.range.max().lt(tag2.range.min()) ) {
                throw new IllegalArgumentException("cache tags cannot be appended, they are not adjacent");
            }
            min= tag1.range.min();
            max= tag1.range.max();
        } else {
            min= tag1.range.min().lt( tag2.range.min() ) ? tag1.range.min() : tag2.range.min();
            max= tag1.range.max().gt( tag2.range.max() ) ? tag1.range.max() : tag2.range.max();
        }
        
        DatumRange range= new DatumRange( min, max );
        return new CacheTag( range, res );
    }
    
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

