/* File: StandardDataStreamCache.java
 * Copyright (C) 2002-2003 University of Iowa
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

package edu.uiowa.physics.pw.das.client;

import edu.uiowa.physics.pw.das.dataset.DataSetDescriptor;
import edu.uiowa.physics.pw.das.util.DasDate;
/**
 *
 * @author  jbf
 */
public class StandardDataStreamCache {
    
    protected class Tag {
        
        edu.uiowa.physics.pw.das.dataset.DataSetDescriptor dsd;
        DasDate start;
        DasDate end;
        double resolution;
        Object params;
        byte[] data;
        int nhits;
        long birthTime;
        long lastAccess;
        
        Tag() {
            this( null, null, null, 1e31, null, null );
        }
        
        Tag( edu.uiowa.physics.pw.das.dataset.DataSetDescriptor dsd, DasDate start, DasDate end, double resolution, Object params, byte[] data ) {
            this.dsd= dsd;
            this.start= start;
            this.end= end;
            this.resolution= resolution;
            this.params= params;
            this.data= data;
            this.nhits= 0;
            this.birthTime= System.currentTimeMillis();
            this.lastAccess= birthTime;
        }
        
        protected boolean isWithin( Tag tag ) {
            boolean result= ( this.dsd!=null) && ( tag.dsd!=null);
            result= result && ( tag.dsd.toString().equals(this.dsd.toString()) );
            result= result && ( tag.start.compareTo( this.start) >= 0 )  &&
            ( tag.end.compareTo(this.end) <= 0 );
            result= result && ( tag.resolution >= this.resolution );
            result= result &&
            ( ( tag.params==this.params ) || ( tag.params.toString().equals(this.params.toString()) ) );
            return result;
        }
        
        public String toString() {
            return dsd.toString() + " " + start.toString() + " - " +
            end.toString() + " @ " +resolution + "s  ["+nhits+" hits]";
        }
    }
    
    private Tag[] buffer;
    public int hits=0;
    public int misses=0;
    
    boolean disabled=false;
    
    /** Creates a new instance of StandardDataStreamCache */
    public StandardDataStreamCache() {
        buffer= new Tag[2];
    }
    
    public void store( edu.uiowa.physics.pw.das.dataset.DataSetDescriptor dsd, DasDate start, DasDate end, double resolution, Object params, byte[] data ) {

        if (disabled) return;
        
        Tag tag= new Tag( dsd, start, end, resolution, params, data );
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
        
        buffer[iMin]= tag;
    };
    
    private int findStored( edu.uiowa.physics.pw.das.dataset.DataSetDescriptor dsd, DasDate start, DasDate end, double resolution, Object params ) {
        Tag tag= new Tag( dsd, start, end, resolution, params, null );
        
        int iHit=-1;
        for (int i=0; i<buffer.length; i++) {
            if (buffer[i]!=null) {
                if (buffer[i].isWithin(tag)) {
                    iHit=i;
                }
            }
        }
        
        return iHit;
    };
    
    public boolean haveStored( edu.uiowa.physics.pw.das.dataset.DataSetDescriptor dsd, DasDate start, DasDate end, double resolution, Object params ) {
        Tag tag= new Tag( dsd, start, end, resolution, params, null );
        
        edu.uiowa.physics.pw.das.util.DasDie.println(toString());
        edu.uiowa.physics.pw.das.util.DasDie.println("    need: "+tag.toString());
        
        if ( disabled ) return false;
        
        int iHit= findStored( dsd, start, end, resolution, params );
        
        if (iHit!=-1) {
            hits++;
            return true;
        } else {
            misses++;
            return false;
        }
    };
    
    public byte[] retrieve( edu.uiowa.physics.pw.das.dataset.DataSetDescriptor dsd, DasDate start, DasDate end, double resolution, Object params ) {
        int iHit= findStored( dsd, start, end, resolution, params );
        if (iHit!=-1) {
            edu.uiowa.physics.pw.das.util.DasDie.println(" time offset= "+buffer[iHit].start.subtract(start) );
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
    
    public long calcTotalSize() {
        long total=0;
        for (int i=0; i<buffer.length; i++) {
            if ( buffer[i]!=null) {
                total+= buffer[i].data.length;
            }
        }
        return total;
    }
    
    public double calcHitRate() {
        return hits*100./(hits+misses);
    }
    
    public String toString() {
        String result="\n-------------------\n";
        for (int i=0; i<buffer.length; i++) {
            result+= "Buffer "+i+": ";
            if (buffer[i]!=null) {
                result+= buffer[i].toString();
            } else {
                result+= "";
            }
            result+="\n";
        }
        if ( disabled ) result+="DISABLED\n";
        result+= "-------------------";
        return result;
    }
    
    public void setDisabled(boolean disabled) {
        this.disabled= disabled;
    }
    
}
