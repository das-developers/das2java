package org.das2.dataset;

import org.das2.datum.DatumRange;
import org.das2.datum.Datum;
import org.das2.datum.DatumUtil;

/**
 * CacheTags are used to represent the coverage of datasets stored in a cache, and are
 * the reference used to decide if a cache entry is capable of satisfying a data request.
 * 
 */
public class CacheTag {
    
    DatumRange range;    
    Datum resolution;
    
    /**
     * Appends two CacheTags, when possible.  The new range covers the ranges, and the resolution is the lower of the two.
     * @param tag1 a CacheTag
     * @param tag2 a CacheTag that is adjacent to tag1
     * @return a CacheTag that covers both CacheTags precisely.
     */
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
            if ( tag2.range.min().lt( tag1.range.min() ) ) {
                CacheTag temp= tag1;
                tag1= tag2;
                tag2= temp;
            }
            if ( tag1.range.max().lt(tag2.range.min()) ) {
                throw new IllegalArgumentException("cache tags cannot be appended, they are not adjacent");
            }
            min= tag1.range.min();
            max= tag2.range.max();
        } else {
            min= tag1.range.min().lt( tag2.range.min() ) ? tag1.range.min() : tag2.range.min();
            max= tag1.range.max().gt( tag2.range.max() ) ? tag1.range.max() : tag2.range.max();
        }
        
        DatumRange range= new DatumRange( min, max );
        return new CacheTag( range, res );
    }
    
    /**
     * Constructs a new CacheTag.
     * @param start the beginning of the interval.
     * @param end the end of the interval.
     * @param resolution the highest resolution request that can be provided.
     */
    public CacheTag(Datum start, Datum end, Datum resolution) {
        this( new DatumRange( start, end ), resolution );
    }
    
    /**
     * Constucts a new CacheTag.
     * @param range the interval covered by the CacheTag.
     * @param resolution  the highest resolution request that can be provided.
     */
    public CacheTag( DatumRange range, Datum resolution) {
        this.range= range;
        this.resolution= resolution;
    }
    
    /**
     * Returns a string representation of the object.
     * @return a human consumable representation of the cache tag.  This should fit on
     * one line as it is used to list cache contents.
     */
    public String toString() {
        return range + " @ " + ( resolution==null ? "intrinsic" : ""+DatumUtil.asOrderOneUnits(resolution) );
    }
    
    /**
     * Indicates if the cache tag the the capability of satifying the given cache tag.  If the tag has a lower (courser) resolution and its bounds are within
     * this CacheTag.
     * @return true if the tag has a lower resolution and its bounds are within
     * this CacheTag.
     * @param tag a CacheTag to test.
     */
    public boolean contains( CacheTag tag ) {
        return ( this.range.contains( tag.range ) 
          && ( this.resolution==null 
              || ( tag.resolution!=null && this.resolution.le( tag.resolution ) ) 
             ) );            
    }
    
    /**
     * the range covered by the cache tag.
     */
    public DatumRange getRange() {
        return this.range;
    }
    
    /**
     * the resolution of the cache tag, which may be null.
     */
    public Datum getResolution() {
        return this.resolution;
    }
}

