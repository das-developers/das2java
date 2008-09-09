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

import org.das2.DasApplication;
/**
 *
 * @author  jbf
 */
public class LimitCountDataSetCache extends AbstractDataSetCache {
        
    protected Entry[] buffer;    
    
    /** Creates a new instance of StandardDataStreamCache */
    public LimitCountDataSetCache( int count ) {
        buffer= new Entry[count];
    }
    
    public void store( DataSetDescriptor dsd, CacheTag cacheTag, DataSet data ) {                        
        Entry entry= new Entry( dsd, cacheTag, data );
        
        int iMin=-1;
        for (int i=buffer.length-1; i>=0; i--) {
            if (buffer[i]==null) {
                iMin= i;
            }
        }
        if ( iMin==-1 ) {
            long oldestAccess= Long.MAX_VALUE;
            int oldest= -1;
            for (int i=buffer.length-1; i>=0; i--) {
                if ( buffer[i].lastAccess < oldestAccess ) {
                    oldest= i;
                    oldestAccess= buffer[i].lastAccess;
                }
            }
            iMin= oldest;
        }
        
        buffer[iMin]= entry;
    };
    
    private int findStored( DataSetDescriptor dsd, CacheTag cacheTag ) {
        Entry entry= new Entry( dsd, cacheTag, null );
        
        int iHit=-1;
        for (int i=0; i<buffer.length; i++) {
            if (buffer[i]!=null) {
                if (buffer[i].satifies(entry)) {
                    iHit=i;
                }
            }
        }
        
        return iHit;
    };
    
    public boolean haveStoredImpl( DataSetDescriptor dsd, CacheTag cacheTag ) {
        Entry entry= new Entry( dsd, cacheTag, null );
        
        int iHit= findStored( dsd, cacheTag );
        
        return iHit!=-1;
    };
    
    public DataSet retrieveImpl( DataSetDescriptor dsd, CacheTag cacheTag ) {
        int iHit= findStored( dsd, cacheTag );
        if (iHit!=-1) {            
            buffer[iHit].nhits++;
            buffer[iHit].lastAccess= System.currentTimeMillis();
            return buffer[iHit].data;
        } else {
            throw new IllegalArgumentException("Data not found in buffer");
        }
    }
    
    public void reset() {
        for (int i=0; i<buffer.length; i++) {
            buffer[i]= null;
        }
    }
        
    public double calcHitRate() {
        return hits*100./(hits+misses);
    }
    
    public String toString() {
        String result="\n---DataSetCache---\n";
        for (int i=0; i<buffer.length; i++) {
            result+= "Buffer "+i+": ";
            if (buffer[i]!=null) {
                result+= buffer[i].toString();
            } else {
                result+= "";
            }
            result+="\n";
        }
        result+= "-------------------";
        return result;
    }    
    
}
