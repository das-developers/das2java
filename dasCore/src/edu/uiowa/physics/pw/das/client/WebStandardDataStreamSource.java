/* File: WebStandardDataStreamSource.java
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

/**
 *
 * @author  jbf
 */

import edu.uiowa.physics.pw.das.DasException;
import edu.uiowa.physics.pw.das.DasIOException;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.client.DasServer;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.util.*;

import javax.swing.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Properties;

public class WebStandardDataStreamSource implements StandardDataStreamSource {
    
    private Key key;
    private DasServer server;
    private MeteredInputStream min;
    
    public WebStandardDataStreamSource(DasServer server) {
        this.server = server;
    }
    
    private Key getKey() {
        return getStoredKey();
    }
        
    private Key getStoredKey() {
        String keyfile= System.getProperty("user.home")+System.getProperty("file.separator")+".das2rc";
        File f= new File(keyfile);
        Properties dasProperties=new Properties();
        if (f.canRead()) {
            try {
                InputStream in= new FileInputStream(f);
                dasProperties.load(in);
                in.close();
            } catch (IOException e) {
                edu.uiowa.physics.pw.das.util.DasExceptionHandler.handle(e);
            }
        } else {
            String keyString= JOptionPane.showInputDialog(null,"Enter temporary key: ","Need a key", JOptionPane.QUESTION_MESSAGE);
            dasProperties.put("keyHack",keyString);
            try {
                OutputStream out= new FileOutputStream(f);
                dasProperties.store(out,"");
                out.close();
            } catch (IOException e) {
                edu.uiowa.physics.pw.das.util.DasExceptionHandler.handle(e);
            }
        }
        
        if (dasProperties.containsKey("keyHack")) {
            return new Key((String)dasProperties.get("keyHack"));
        } else {
            return null;
        }
    }
    
    public InputStream getInputStream(DataSetDescriptor dsd, Datum start, Datum end) throws DasException {
        String serverType;
        serverType="dataset";
        
        String formData= "server="+serverType;
        
        InputStream in= openURLConnection( dsd, start, end, formData );        
        MeteredInputStream min= new MeteredInputStream(in);
        min.setSpeedLimit(3300);
        return min;
        
    }
    
    
    public InputStream getReducedInputStream( DataSetDescriptor dsd, Datum start, Datum end, Datum timeResolution) throws DasException {
        // params is either String, or object with toString properly defined.
                
        String formData;
        
        if ( dsd instanceof XTaggedYScanDataSetDescriptor ) {            
            formData= "server=compactdataset";
            formData+= "&nitems="+(((XTaggedYScanDataSetDescriptor)dsd).y_coordinate.length+1);
            formData+= "&resolution="+timeResolution.doubleValue(Units.seconds);
        } else if ( dsd instanceof XMultiYDataSetDescriptor ) {            
            formData= "server=dataset";
            XMultiYDataSetDescriptor mdsd= (XMultiYDataSetDescriptor)dsd;
            if ( mdsd.isTCA() ) {
                formData+= "&interval="+timeResolution.doubleValue(Units.seconds);
            }
        } else {
            throw new IllegalStateException("dsd type not handled");
        }
        
        
        boolean compress=true;
        if ( min!=null ) {
            edu.uiowa.physics.pw.das.util.DasDie.println(edu.uiowa.physics.pw.das.util.DasDie.DEBUG, "last transfer speed (byte/sec)= "+min.calcTransmitSpeed());
            edu.uiowa.physics.pw.das.util.DasDie.println(edu.uiowa.physics.pw.das.util.DasDie.DEBUG, "   time to transfer (sec)= "+min.calcTransmitTime());
            edu.uiowa.physics.pw.das.util.DasDie.println(edu.uiowa.physics.pw.das.util.DasDie.DEBUG, "   total kbytes transferred= "+min.totalBytesTransmitted()/1024);
        }
        
        if ( min!=null && min.calcTransmitSpeed()>30000 ) {
            compress= false;
        }
            
        if ( dsd.isDas2Stream() && compress ) {
            edu.uiowa.physics.pw.das.util.DasDie.println(edu.uiowa.physics.pw.das.util.DasDie.DEBUG, "compressing data stream");
            formData+= "&compress=true";
        } else {
            edu.uiowa.physics.pw.das.util.DasDie.println(edu.uiowa.physics.pw.das.util.DasDie.DEBUG, "NOT compressing data stream");
        }            
        
        InputStream in= openURLConnection( dsd, start, end, formData );        
        min= new MeteredInputStream(in);
        return min;

    }
    
    protected synchronized InputStream openURLConnection( DataSetDescriptor dsd,
      Datum start, Datum end, String additionalFormData )
    throws DasException {
        
        String dataSetID = dsd.getDataSetID();
        String[] split = dataSetID.split("\\?", 2);
        if (split.length > 1) {
            dataSetID = split[1];
        }
        
        try {
            DasTimeFormatter formatter=null;
            if ( start.getUnits() instanceof TimeLocationUnits ) {
                formatter= new DasTimeFormatter(TimeContext.HOURS);
                formatter.setAlwaysShowDate(true);
            } else {
                throw new IllegalStateException( "start,end units are not TimeLocationUnits -- not supported" );
            }
            
            String formData= "dataset="+URLEncoder.encode(dataSetID,"UTF-8");
            String startStr= formatter.format(start).replace(' ','T');            
            formData+= "&start_time="+URLEncoder.encode(startStr,"UTF-8");
            String endStr= formatter.format(end).replace(' ','T');                        
            formData+= "&end_time="+URLEncoder.encode(endStr,"UTF-8");
            
            if (dsd.isRestrictedAccess() || key!=null ) {
                if (key==null) {
                    authenticate();
                }
                if (key!=null) {
                    formData+= "&key="+URLEncoder.encode(key.toString(),"UTF-8");
                }
            }
            
            formData+= "&"+additionalFormData;
            URL server= this.server.getURL(formData);
            DasDie.println(DasDie.VERBOSE,server.toString());
            
            URLConnection urlConnection = server.openConnection();
            urlConnection.connect();
            
            String contentType = urlConnection.getContentType();
            
            if (!contentType.equalsIgnoreCase("application/octet-stream")) {
                BufferedReader bin = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String line = bin.readLine();
                String message = "";
                while (line != null) {
                    message = message.concat(line);
                    line = bin.readLine();
                }
                throw new DasIOException(message);
            }
            
            InputStream in= server.openStream();
            
            BufferedInputStream bin= new BufferedInputStream(in);
            
            bin.mark(Integer.MAX_VALUE);
            String serverResponse= readServerResponse(bin);
            
            if ( serverResponse.equals("") ) {
                return bin;
                
            } else {
                
                if (serverResponse.equals("<noDataInInterval/>")) {
                    throw new NoDataInIntervalException("no data in interval");
                }
                
                String errorTag= "error";
                if (serverResponse.startsWith("<"+errorTag+">")) {
                    int index2= serverResponse.indexOf("</"+errorTag+">");
                    
                    String error= serverResponse.substring( errorTag.length()+2,
                    serverResponse.length()-(errorTag.length()+3));
                    
                    edu.uiowa.physics.pw.das.util.DasDie.println("error="+error);
                    
                    if (error.equals("<needKey/>")) {
                        authenticate();
                        throw new NoKeyProvidedException("");
                    }
                    
                    if (error.equals("<accessDenied/>")) {
                        throw new AccessDeniedException("");
                    }
                    
                    if (error.equals("<invalidKey/>" )) {
                        throw new NoKeyProvidedException("invalid Key");
                    }
                    
                    if (error.equals("<noSuchDataSet/>")) {
                        throw new NoSuchDataSetException("");
                    }
                    
                    else {
                        throw new DasServerException("Error response from server: "+error);
                    }
                }
                
                return bin;
            }
        } catch (IOException e) {
            throw new DasIOException(e.getMessage());
        }
    }
    
    private String readServerResponse(InputStream in) {
        
        // Read <dasResponse>...</dasResponse>, leaving the InputStream immediately after //
        
        String das2Response;
        
        byte[] data = new byte[4096];
        
        int lastBytesRead = -1;
        
        String s;
        
        int offset=0;
        
        try {
            int bytesRead= in.read(data,offset,4096-offset);
            
            String das2ResponseTag= "das2Response";
            // beware das2ResponseTagLength=14 assumed below!!!
            
            if (bytesRead<(das2ResponseTag.length()+2)) {
                offset+= bytesRead;
                bytesRead= in.read(data,offset,4096-offset);
            }
            
            if ( new String(data,0,14,"UTF-8").equals("<"+das2ResponseTag+">")) {
                while ( new String( data,0,offset,"UTF-8" ).indexOf("</"+das2ResponseTag+">")==-1 &&
                offset<4096 ) {
                    offset+= bytesRead;
                    bytesRead= in.read(data,offset,4096-offset);
                }
                
                int index= new String( data,0,offset,"UTF-8" ).indexOf("</"+das2ResponseTag+">");
                
                das2Response= new String(data,14,index-14);
                
                edu.uiowa.physics.pw.das.util.DasDie.println("das2Response="+das2Response);
                
                in.reset();
                in.skip( das2Response.length() + 2 * das2ResponseTag.length() + 5 );
                
            } else {
                in.reset();
                
                das2Response="";
            }
        } catch ( IOException e ) {
            das2Response= "";
        }
        
        return das2Response;
        
    }
    
    public void reset() {
    }
    
    public void authenticate() {
        Authenticator authenticator;
        authenticator= new Authenticator(server);
        key= authenticator.authenticate();
    }
    
}
