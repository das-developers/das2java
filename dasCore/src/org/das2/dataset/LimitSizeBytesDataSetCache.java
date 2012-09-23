/*
 * LimitSizeBytesDataSetCache.java
 *
 * Created on June 9, 2005, 1:23 PM
 */

package org.das2.dataset;

import org.das2.datum.CacheTag;
import org.das2.datum.Units;
import org.das2.datum.Datum;
import java.util.*;

/**
 * DataCache that attempts to limit the amount of memory it consumes,
 * by using DataSetUtil.guessSizeBytes(DataSet)
 *
 * @author Jeremy
 */
public class LimitSizeBytesDataSetCache extends AbstractDataSetCache {
    
    List entries;
    long totalSize;
    long totalSizeLimit;
    
    /** Creates a new instance of LimitSizeBytesDataSetCache */
    public LimitSizeBytesDataSetCache( long totalSizeLimitBytes ) {
        entries= new ArrayList();
        totalSize= 0;
        this.totalSizeLimit= totalSizeLimitBytes;
    }
    
    /* return a list of all the datasets that together cover the cacheTag */
    private Entry findStored( DataSetDescriptor dsd, CacheTag cacheTag ) {
        Entry entry= new Entry( dsd, cacheTag, null );
        
        Entry iHit=null;
        synchronized (entries) {
            for ( Iterator i= entries.iterator(); i.hasNext(); ) {
                Entry testEntry= (Entry)i.next();
                if (testEntry.satifies(entry)) {
                    iHit= testEntry;
                }
            }
        }
        return iHit;
    };
    
    boolean haveStoredImpl(DataSetDescriptor dsd, CacheTag cacheTag) {
        Entry entry= findStored( dsd, cacheTag );
        return ( entry!=null );
    }
    
    public void reset() {
        synchronized(entries) {
            entries.clear();
            this.totalSize= 0;
        }
    }
    
    DataSet retrieveImpl(DataSetDescriptor dsd, CacheTag cacheTag) {
        Entry entry= findStored( dsd, cacheTag );
        if ( entry!=null ) {
            entry.nhits++;
            return entry.getData();
        } else {
            throw new IllegalArgumentException("not found in cache");
        }
    }
    
    private Entry leastValuableEntry() {
        Entry result= (Entry)entries.get(0);
        long value= this.cacheValue(result);
        for ( Iterator i=entries.iterator(); i.hasNext(); ) {
            Entry test= (Entry)i.next();
            long testValue= cacheValue(test);
            if ( testValue < value ) {
                result= test;
                value= testValue;
            }
        }
        return result;
    }
    
    public void store(DataSetDescriptor dsd, CacheTag cacheTag, DataSet data) {
        long sizeBytes= DataSetUtil.guessSizeBytes( data );
        if ( sizeBytes > this.totalSizeLimit ) return;
        
        synchronized (entries) {
            while ( sizeBytes + this.totalSize > this.totalSizeLimit ) {
                Entry e= leastValuableEntry();
                long s= DataSetUtil.guessSizeBytes( e.data );
                entries.remove(e);
                totalSize-= s;
            }
        
            entries.add( new Entry( dsd, cacheTag, data ) );
            totalSize+= sizeBytes;
        }
    }
    
    public Entry[] getEntries() {
        return (Entry[]) entries.toArray( new Entry[ entries.size() ] );
    }
    
    public Entry getEntries( int i ) {
        return (Entry)entries.get(i);
    }
    
    public Datum getTotalSize() {
        return Units.kiloBytes.createDatum( this.totalSize/1000., 0.1 );
    }
    
    public Datum getTotalSizeLimit() {
        return Units.kiloBytes.createDatum( this.totalSizeLimit/1000., 0.1 );
    }
    
    public void setTotalSizeLimit( Datum d ) {
        this.totalSizeLimit= (long)( d.doubleValue( Units.kiloBytes ) * 1000 );
    }
    
    public Datum getHitRate() {
        //return ( this.hits + this.misses == 0 ) ? Units.percent.getFillDatum() : Units.percent.createDatum( this.hits * 100. / ( this.hits + this.misses ), 0.1 );
        return Units.percent.createDatum( this.hits * 100. / ( this.hits + this.misses ), 0.1 );
    }
    
    public String getDisplayString() {
        StringBuffer result= new StringBuffer( "LimitSizeBytesDataSetCache with "+entries.size()+" datasets" );
        for ( int i=0; i<entries.size(); i++ ) {
            result.append( "\n"+entries.get(i) );
        }
        result.append("\n");
        return result.toString();
    }
}
