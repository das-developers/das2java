/* File: FakeStandardDataStreamSource.java
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

package edu.uiowa.physics.pw.das.client;

import edu.uiowa.physics.pw.das.DasException;
import edu.uiowa.physics.pw.das.dataset.DataSetDescriptor;
import edu.uiowa.physics.pw.das.dataset.XTaggedYScanDataSetDescriptor;
import edu.uiowa.physics.pw.das.datum.*;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 *
 * @author  jbf
 */
public class FakeStandardDataStreamSource implements StandardDataStreamSource {
    
    class FakeInputStream extends InputStream {
        int nitems;
        long nRecs;
        double recsPerSecond;
        long floatCount;
        long byteCount;
        long recCount;
        long recSize;
        float currentFloat;
        byte[] iCurrentFloat;
        ByteBuffer buff;
        FloatBuffer fbuff;      
        double transferRateBps;
        long transferBirthMilli;
        
        FakeInputStream( long nRecs, double recsPerSecond, int nitems ) {
            this.nitems= nitems;
            this.nRecs= nRecs;
            this.recsPerSecond= recsPerSecond;
            buff= ByteBuffer.allocate(4*(nitems+1));
            fbuff = buff.asFloatBuffer();
            buff.position(buff.limit());
            floatCount=0;
            byteCount=0;
            recCount=0;
            recSize= (nitems+1)*4;
            iCurrentFloat= new byte[4];
            transferRateBps= 2.1e5;
            transferBirthMilli= System.currentTimeMillis();
        }
        
        public int read() throws java.io.IOException {            
            try {
                while( ( byteCount * 1000 / ( 1 + ( System.currentTimeMillis() - transferBirthMilli ) ) ) > transferRateBps ) {
                    Thread.sleep(10);
                }
            } catch ( InterruptedException e ) {
            }
            
            int result;
            if ( ! buff.hasRemaining() ) {                
                buff.position(0);
                fbuff.put(0, (float) ( recCount / recsPerSecond ));
                for (int i=1; i<=nitems; i++) {
                    float f= (float)Math.random();
                    fbuff.put(i,f);
                }
                recCount++;
            }

            if (recCount>nRecs) {
                return -1;
            }   else {            
                result= buff.get() & 0xff;            
                byteCount++;
                return result;
            }
        }
        
    }
    
    /** Creates a new instance of FakeStandardDataStreamSource */
    public FakeStandardDataStreamSource() {
    }
    
    public InputStream getInputStream(edu.uiowa.physics.pw.das.dataset.DataSetDescriptor dsd, Object params, Datum start, Datum end) throws DasException {
        
        double recsPerSecond= 1 / dsd.x_sample_width;
        int nRec= (int) ( end.subtract(start).doubleValue(Units.seconds) * recsPerSecond );
        
        int nitems;
        if ( dsd instanceof edu.uiowa.physics.pw.das.dataset.XTaggedYScanDataSetDescriptor ) {
            nitems= ((edu.uiowa.physics.pw.das.dataset.XTaggedYScanDataSetDescriptor)dsd).y_coordinate.length;
        } else {
            throw new IllegalArgumentException("Only XTYSDSD's please!");
        }
        
        InputStream result= new FakeStandardDataStreamSource.FakeInputStream(nRec,recsPerSecond,nitems);
        return result;
    }
    
    public InputStream getReducedInputStream(edu.uiowa.physics.pw.das.dataset.XTaggedYScanDataSetDescriptor dsd, Object params, Datum start, Datum end, Datum timeResolution) throws DasException {
        return getInputStream( dsd, params, start, end );
    }
    
    public void reset() {
    }
    
}
