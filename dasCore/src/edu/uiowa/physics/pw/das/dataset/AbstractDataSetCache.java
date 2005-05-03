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

package edu.uiowa.physics.pw.das.dataset;

import edu.uiowa.physics.pw.das.DasApplication;
import java.text.*;
/**
 * Keeps keep track of cache statistics and to give consistent
 * log messages, and provides the Entry class.
 *
 * @author  jbf
 */
public abstract class AbstractDataSetCache implements DataSetCache {
    
    protected class Entry {
        
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
        
        protected DataSet getData() {
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
    }
        
    public int hits=0;
    public int misses=0;    
    
    abstract public void store( DataSetDescriptor dsd, CacheTag cacheTag, DataSet data );
    
    abstract boolean haveStoredImpl( DataSetDescriptor dsd, CacheTag cacheTag );
    
    public boolean haveStored( DataSetDescriptor dsd, CacheTag cacheTag ) {
        boolean result= haveStoredImpl( dsd, cacheTag );
        if ( result ) {
            DasApplication.getDefaultApplication().getLogger(DasApplication.SYSTEM_LOG).fine("cache hit "+dsd+" "+cacheTag);            
            hits++;
        } else {
            DasApplication.getDefaultApplication().getLogger(DasApplication.SYSTEM_LOG).fine("cache miss "+dsd+" "+cacheTag);
            misses++;
        }
        return result;
    }
    
    abstract DataSet retrieveImpl( DataSetDescriptor dsd, CacheTag cacheTag );
    
    public DataSet retrieve( DataSetDescriptor dsd, CacheTag cacheTag ) {
        return retrieveImpl( dsd, cacheTag );
    }
    
    abstract public void reset();
}
