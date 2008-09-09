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

import org.das2.DasApplication;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
/**
 * simply cache data by storing one per DataSetDescriptor.
 *
 * @author  jbf
 */
public class SimpleDataSetCache extends AbstractDataSetCache {    
    
    protected Map buffer;    
    
    /** Creates a new instance of StandardDataStreamCache */
    public SimpleDataSetCache() {
        buffer= new HashMap();
    }
    
    public void store( DataSetDescriptor dsd, CacheTag cacheTag, DataSet data ) {                        
        Entry entry= new Entry( dsd, cacheTag, data );
        buffer.put( dsd, entry );
    };
    
    public boolean haveStoredImpl( DataSetDescriptor dsd, CacheTag cacheTag ) {        
        Entry haveEntry= (Entry)buffer.get(dsd);
        if ( haveEntry==null ) {            
            return false;
        } else {
            Entry entry= new Entry( dsd, cacheTag, null );
            return haveEntry.satifies(entry);                         
        }
    }
    
    public DataSet retrieveImpl( DataSetDescriptor dsd, CacheTag cacheTag ) {
        Entry haveEntry= (Entry)buffer.get(dsd);
        if ( haveEntry==null ) {
            throw new IllegalArgumentException("Data not found in cache");
        } else {
            Entry entry= new Entry( dsd, cacheTag, null );
            if ( haveEntry.satifies(entry) ) {
                return haveEntry.getData();
            } else {
                throw new IllegalArgumentException("Data not found in cache");
            }
        }
    }
    
    public void reset() {
        buffer= new HashMap();
    }
            
    public String toString() {
        StringBuffer result= new StringBuffer("\n---SimpleDataSetCache---\n");
        for ( Iterator i= buffer.keySet().iterator(); i.hasNext(); ) {
            Object key= i.next();           
            result.append( " " );
            result.append( buffer.get(key).toString() );
            result.append( "\n" );
        } 
        result.append( "------------------------\n" );
        return result.toString();
    }
    
    
}
