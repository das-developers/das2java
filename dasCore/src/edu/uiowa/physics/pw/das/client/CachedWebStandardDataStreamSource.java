/* File: CachedWebStandardDataStreamSource.java
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

import edu.uiowa.physics.pw.das.DasException;
import edu.uiowa.physics.pw.das.DasIOException;
import edu.uiowa.physics.pw.das.dataset.DataSetDescriptor;
import edu.uiowa.physics.pw.das.dataset.XTaggedYScanDataSetDescriptor;
import edu.uiowa.physics.pw.das.util.DasDate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

/**
 *
 * @author  jbf
 */
public class CachedWebStandardDataStreamSource extends WebStandardDataStreamSource {
    
    private StandardDataStreamCache dataCache;
    private double transferSeconds=0.;
    private long bytesTransferred=0;
    private long uncompressedBytesTransferred=0;
    private long sessionBirthTime;
    private boolean compress;
    
    public CachedWebStandardDataStreamSource(edu.uiowa.physics.pw.das.server.DasServer server) {
        super(server);
        dataCache= new StandardDataStreamCache();
        dataCache.setDisabled(true);
        sessionBirthTime= System.currentTimeMillis();
        compress= false;
    }
    
    public InputStream getInputStream(edu.uiowa.physics.pw.das.dataset.DataSetDescriptor dsd, Object params, DasDate start, DasDate end) throws DasException{
        
        InputStream result= null;
        if ( dataCache.haveStored(dsd,start,end,dsd.x_sample_width,params) ) {
            edu.uiowa.physics.pw.das.util.DasDie.println("----- Buffer Hit ----\n");
            byte [] b= dataCache.retrieve(dsd,start,end,dsd.x_sample_width,params);
            result= new ByteArrayInputStream(b);
        } else {
            edu.uiowa.physics.pw.das.util.DasDie.println("------- Miss --------\n");
            String formData= "server=dataset";
            double docResolution= dsd.x_sample_width;
            try {
                result= transferURLInputStream( dsd, params, start, end, docResolution, formData );
            } catch ( IOException e ) {
                throw new DasIOException(e.getMessage());
            }
        }
        selfAdjustParameters();
        return result;
    }
    
    public InputStream getReducedInputStream(edu.uiowa.physics.pw.das.dataset.XTaggedYScanDataSetDescriptor dsd, Object params, DasDate start, DasDate end, double resolution) throws DasException {
        
        InputStream result= null;
        
        double res= ( resolution > dsd.x_sample_width ) ? resolution : dsd.x_sample_width;
        
        if ( dataCache.haveStored(dsd,start,end,res,params) ) {
            //edu.uiowa.physics.pw.das.util.DasDie.println("----- Buffer Hit ----");
            byte [] b= dataCache.retrieve(dsd,start,end,res,params);
            result= new ByteArrayInputStream(b);
        } else {
            //edu.uiowa.physics.pw.das.util.DasDie.println("------- Miss --------");
            
            // "dumb" smart buffering--simply get a bit more than they asked for
            // to increase chance of future hit.
            double sbResolution= resolution ;
            double delta= end.subtract(start);
            //DasDate sbStart= start.subtract(delta/10);
            //DasDate sbEnd= end.add(delta/10);
            DasDate sbStart= start;
            DasDate sbEnd= end;
            sbResolution= sbResolution > dsd.x_sample_width ? sbResolution : dsd.x_sample_width;
            
            String serverType= "compactdataset";
            String formData= "server="+serverType;
            formData+= "&resolution="+sbResolution;
            formData+= "&nitems="+(dsd.y_coordinate.length+1);
            
            double docResolution= sbResolution;
            
            try {
                result= transferURLInputStream( dsd, params, sbStart, sbEnd, docResolution, formData );
            } catch ( IOException e ) {
                throw new DasIOException( e.getMessage() );
            }
        }
        selfAdjustParameters();
        return result;
    }
    
    private InputStream transferURLInputStream( edu.uiowa.physics.pw.das.dataset.DataSetDescriptor dsd, Object params, DasDate start, DasDate end, double docResolution, String additionalFormData )
    throws IOException, DasException {
        //  docResolution is a kludge to store (document) the resolution in the cache.  It must be consistent with the
        // resolution requested in additionalFormData.
        if (compress) additionalFormData+= "&compress=true";
        
        InputStream in;
         
        in= openURLConnection( dsd, params, start, end, additionalFormData );
        
        long startTime= System.currentTimeMillis();
        byte [] b= dsd.readBytes( in, params, start, end);
        transferSeconds+= ( System.currentTimeMillis() - startTime ) / 1000;
        bytesTransferred+= b.length;
        
        if (compress) {
            byte [] bz= b;
            ZipInputStream zin= new ZipInputStream( new ByteArrayInputStream(b) );
            try {
                edu.uiowa.physics.pw.das.util.DasDie.println(zin.getNextEntry().getName());
                b= dsd.readBytes( zin, params, start, end);
            } catch ( ZipException e ) {
                throw new IOException("Error in compressed stream");
            }
            
        }
        
        uncompressedBytesTransferred+= b.length;
        
        dataCache.store( dsd, start, end, docResolution, params, b );
        InputStream result= new ByteArrayInputStream(b);
        return result;
    }
    
    public void printStats() {
        edu.uiowa.physics.pw.das.util.DasDie.println("------ Cached Web Data Source ------");
        edu.uiowa.physics.pw.das.util.DasDie.println("Average Transfer Rate: "+getAverageTransferRate()+ " bytes/second");
        edu.uiowa.physics.pw.das.util.DasDie.println("Total Transfer Time: "+transferSeconds);
        edu.uiowa.physics.pw.das.util.DasDie.println("Total Bytes Transferred: "+bytesTransferred);
        edu.uiowa.physics.pw.das.util.DasDie.println("Total Uncompressed Bytes Transferred: "+uncompressedBytesTransferred);
        edu.uiowa.physics.pw.das.util.DasDie.println("Average Compression: "+(uncompressedBytesTransferred-bytesTransferred)*100/uncompressedBytesTransferred+"%");
        edu.uiowa.physics.pw.das.util.DasDie.println("Compression: "+(compress?"On":"Off"));
        //edu.uiowa.physics.pw.das.util.DasDie.println("Cache Hit Rate: "+dataCache.calcHitRate()+"%");
        //edu.uiowa.physics.pw.das.util.DasDie.println("Cache Hits: "+dataCache.hits);
        //edu.uiowa.physics.pw.das.util.DasDie.println("Cache Misses: "+dataCache.misses);
        //edu.uiowa.physics.pw.das.util.DasDie.println("Cache Size: "+dataCache.calcTotalSize()/1024+" KBytes");
        edu.uiowa.physics.pw.das.util.DasDie.println("------------------------------------");
    }
    
    private void selfAdjustParameters() {
        // make adjustments to cache and compress parameters.
        compress= getAverageTransferRate()<100000; // bytes/sec, between dsl and LAN
        printStats();
    }
    
    public double getAverageTransferRate() {
        return bytesTransferred / transferSeconds;
    }
    
    public void reset() {
        edu.uiowa.physics.pw.das.util.DasDie.println("Resetting buffer");
        dataCache.reset();
    }
    
}
