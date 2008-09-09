/* File: DataSetCache.java
 * Copyright (C) 2002-2003 The University of Iowa
 * Created by: Jeremy Faden <jbf@space.physics.uiowa.edu>
 *             Jessica Swanner <jessica@space.physics.uiowa.edu>
 *             Edward E. West <eew@space.physics.uiowa.edu>
 *
 * This file is part of the das2 library.
 *
 * das2 is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.das2.dataset;

import org.das2.datum.Datum;
import org.das2.DasApplication;
import edu.uiowa.physics.pw.das.components.propertyeditor.*;
import org.das2.system.DasLogger;
import java.text.*;
import java.util.*;
import java.util.logging.Logger;
/**
 * Keeps keep track of cache statistics and to give consistent
 * log messages, and provides the Entry class.
 *
 * @author  jbf
 */
public abstract class AbstractDataSetCache implements DataSetCache {
    
    protected class Entry implements Displayable {
        
        protected DataSetDescriptor dsd;
        protected CacheTag cacheTag;
        protected DataSet data;
        protected int nhits;
        protected long birthTime;
        protected long lastAccess;

        Entry() {
            this( null, null, null );
        }
            
        Entry( DataSetDescriptor dsd, CacheTag cacheTag, DataSet data ) {
            this.dsd= dsd;
            this.cacheTag= cacheTag;
            this.data= data;
            this.nhits= 0;
            this.birthTime= System.currentTimeMillis();
            this.lastAccess= birthTime;
        }
        
        /*
         * returns the dataSet attached to the Entry, and updates the lastAccess time
         */
        protected DataSet getData() {
            this.lastAccess= System.currentTimeMillis();
            return data;
        }
        
        protected boolean satifies( Entry entry ) {
            boolean result= ( this.dsd!=null) && ( entry.dsd!=null);
            result= result && ( this.dsd.equals( entry.dsd ) );
            result= result && ( cacheTag.contains(entry.cacheTag) );
            return result;
        }
        
        public String toString() {
            long sizeBytes= DataSetUtil.guessSizeBytes(this.data);
            String sizeBytesString= " ("+NumberFormat.getIntegerInstance().format(sizeBytes)+" bytes)";
            return dsd.toString() + " " + cacheTag + " ["+nhits+" hits]"+sizeBytesString;
        }

        public javax.swing.Icon getListIcon() {
            return null;
        }

        public String getListLabel() {
            return toString();
        }

        public CacheTag getCacheTag() {
            return this.cacheTag;            
        }

    }
        
    private static final Logger logger= DasLogger.getLogger(DasLogger.SYSTEM_LOG);
            
    public int hits=0;
    public int misses=0;
    
    abstract public void store( DataSetDescriptor dsd, CacheTag cacheTag, DataSet data );
    
    abstract boolean haveStoredImpl( DataSetDescriptor dsd, CacheTag cacheTag );
    
    public boolean haveStored( DataSetDescriptor dsd, CacheTag cacheTag ) {
        boolean result= haveStoredImpl( dsd, cacheTag );
        if ( result ) {
            logger.fine("cache hit "+dsd+" "+cacheTag);            
            hits++;
        } else {
            logger.fine("cache miss "+dsd+" "+cacheTag);
            misses++;
        }
        return result;
    }
    
    /**
     * return a measure of the utility of the entry, presumably so that it may be
     * swapped out when a resource limit is met.
     */    
    protected long cacheValue( Entry e ) {
        return e.lastAccess;
    }
        
    abstract DataSet retrieveImpl( DataSetDescriptor dsd, CacheTag cacheTag );
    
    public DataSet retrieve( DataSetDescriptor dsd, CacheTag cacheTag ) {
        return retrieveImpl( dsd, cacheTag );
    }
    
    /**
     * reset the internal state of the cache 
     */
    abstract public void reset();
    
    /**
     * return the DataSet described by the set of DataSets if possible.  
     * @throws IllegalArgumentException if a subset is not continous, 
     * non-overlapping, and of the same resolution.  Removes 
     * elements from the list that are not needed for the set.
     */
    public DataSet coalese( List result ) {
       Collections.sort( result, new Comparator() {
            public int compare( Object o1, Object o2 ) {
                return ((Entry)o1).cacheTag.range.compareTo( ((Entry)o2).cacheTag.range );
            }
        } );
        
        Entry e0= (Entry)result.get(0);
        CacheTag ct= e0.cacheTag;
        Datum t1= ct.range.max();        
        Datum resolution= ct.resolution;

        DataSet ds= e0.data;

        // check for continuity and non-overlap
        for ( int i=1; i<result.size(); i++ ) {
            Entry entryTest= (Entry)result.get(i);
            CacheTag ctTest= entryTest.cacheTag;
            if ( ctTest.range.min().equals(t1) && 
                 ( ( ct.resolution==null && ctTest.resolution==null ) || 
                    ct.resolution.equals( ctTest.resolution ) ) ) {
                ds= DataSetUtil.append( ds, entryTest.data );
                t1= ctTest.range.max();
            }
        }
        
        return ds;
    }

    /**
     * Getter for property resetCache.
     * @return Value of property resetCache.
     */
    public boolean isResetCache() {        
        return false;
    }

    /**
     * Setter for property resetCache.
     * @param resetCache New value of property resetCache.
     */
    public void setResetCache(boolean resetCache) {
        if ( resetCache ) this.reset();        
    }
    
}
