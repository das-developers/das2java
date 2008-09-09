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

import org.das2.DasApplication;
import org.das2.util.URLBuddy;
import edu.uiowa.physics.pw.das.*;
/**
 *
 * @author  jbf
 */

import org.das2.DasException;
import org.das2.DasIOException;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.stream.*;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.datum.format.DatumFormatter;
import edu.uiowa.physics.pw.das.system.DasLogger;
import java.io.*;
import java.net.*;

/*
 * Web standard data stream source */

/** */
public class WebStandardDataStreamSource implements StandardDataStreamSource {

    private DasServer server;
    private boolean legacyStream = true;
    private String extraParameters;

    /**
     * Holds value of property compress.
     */
    private boolean compress;

    /**
     * Holds value of property lastRequestURL.
     */
    private String lastRequestURL;

    public WebStandardDataStreamSource(DasServer server, URL url) {
        this.server = server;
        String[] query = url.getQuery() == null ? new String[0] : url.getQuery().split("&");
        if (query.length >= 2) {
            extraParameters = query[1];
        }
    }

    public boolean isLegacyStream() {
        return legacyStream;
    }


    public InputStream getInputStream(StreamDataSetDescriptor dsd, Datum start, Datum end) throws DasException {
        String serverType="dataset";

        StringBuffer formData = new StringBuffer();
        formData.append("server=").append(serverType);

        InputStream in= openURLConnection( dsd, start, end, formData );
        in= DasApplication.getDefaultApplication().getInputStreamMeter().meterInputStream(in);
        return in;

    }


    public InputStream getReducedInputStream( StreamDataSetDescriptor dsd, Datum start, Datum end, Datum timeResolution) throws DasException {

        StringBuffer formData = new StringBuffer();
        String form = (String)dsd.getProperty("form");

        if ("true".equals(dsd.getProperty("legacy")) && "x_tagged_y_scan".equals(form) ) {
            formData.append("server=compactdataset");
            StreamYScanDescriptor y = (StreamYScanDescriptor)dsd.getDefaultPacketDescriptor().getYDescriptors().get(0);
            formData.append("&nitems=").append(y.getNItems() + 1);
            if (timeResolution != null) {
                formData.append("&resolution=").append(timeResolution.doubleValue(Units.seconds));
            }
        } else if ( "x_multi_y".equals(form) && dsd.getProperty("items") != null) {
            formData.append("server=dataset");
            if (timeResolution != null) {
                formData.append("&interval=").append(timeResolution.doubleValue(Units.seconds));
            }
        } else {
            formData.append("server=compactdataset");
            if (timeResolution != null) {
                formData.append("&resolution=").append(timeResolution.doubleValue(Units.seconds));
            }
        }

        if (extraParameters != null) {
            formData.append("&params=").append(extraParameters);  //Should already be url encoded.
        }

        //if ( min!=null && min.calcTransmitSpeed()>30000 ) {
        //    compress= false;
        //}

        //compress= true;
        if (compress) {
            formData.append("&compress=true");
        }

        if ( !devel.equals("") ) {
            formData.append("&devel="+devel);
        }
        
        InputStream in= openURLConnection( dsd, start, end, formData );

        in= DasApplication.getDefaultApplication().getInputStreamMeter().meterInputStream(in);

        return in;

    }

    private String createFormDataString(String dataSetID, Datum start, Datum end, StringBuffer additionalFormData) throws UnsupportedEncodingException {
        DatumFormatter formatter = start.getUnits().getDatumFormatterFactory().defaultFormatter();
        String startStr = formatter.format(start);
        String endStr= formatter.format(end);
        StringBuffer formData= new StringBuffer("dataset=");
        formData.append(URLBuddy.encodeUTF8(dataSetID));
        formData.append("&start_time=").append(URLBuddy.encodeUTF8(startStr));
        formData.append("&end_time=").append(URLBuddy.encodeUTF8(endStr));
        formData.append("&").append(additionalFormData);
        return formData.toString();
    }

    protected synchronized InputStream openURLConnection( StreamDataSetDescriptor dsd, Datum start, Datum end, StringBuffer additionalFormData ) throws DasException {

        String[] tokens = dsd.getDataSetID().split("\\?|\\&");
        String dataSetID = tokens[1];

        try {
            String formData = createFormDataString(dataSetID, start, end, additionalFormData);
            if ( dsd.isRestrictedAccess() ) {
                key= server.getKey("");
                if (key!=null) {
                    formData+= "&key="+URLEncoder.encode(key.toString(),"UTF-8");
                }
            }

            if ( redirect ) {
                formData+= "&redirect=1";
            }

            URL serverURL= this.server.getURL(formData);

            this.lastRequestURL= String.valueOf( serverURL );

            DasLogger.getLogger(DasLogger.DATA_TRANSFER_LOG).info("opening "+serverURL.toString());

            URLConnection urlConnection = serverURL.openConnection();
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

            InputStream in= urlConnection.getInputStream();

            if (isLegacyStream()) {
                return processLegacyStream(in);
            } else {
                throw new UnsupportedOperationException();
            }
        } catch (IOException e) {
            throw new DasIOException(e);
        }
    }

    private InputStream processLegacyStream(InputStream in) throws IOException, DasException {
        /* advances the inputStream past the old das2server tags */
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

                org.das2.util.DasDie.println("error="+error);

                /* presume that the endUser has opted out */
                if (error.equals("<needKey/>")) {;
                throw new NoKeyProvidedException("");
                }

                if (error.equals("<accessDenied/>")) {
                    /* the server says the key used does not have access to the resouce requested. Allow the user to reauthenticate */
                    server.setKey(null);
                    server.getKey("");
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

                org.das2.util.DasDie.println("das2Response="+das2Response);

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

    public void authenticate( String restrictedResourceLabel ) {
        Authenticator authenticator;
        authenticator= new Authenticator(server,restrictedResourceLabel);
        Key key= authenticator.authenticate();
        if ( key!=null ) server.setKey(key);
    }

    /**
     * Getter for property compress.
     * @return Value of property compress.
     */
    public boolean isCompress() {
        return this.compress;
    }

    /**
     * Setter for property compress.
     * @param compress New value of property compress.
     */
    public void setCompress(boolean compress) {
        this.compress = compress;
    }

    /**
     * Getter for property lastRequestURL.
     * @return Value of property lastRequestURL.
     */
    public String getLastRequestURL() {
        return this.lastRequestURL;
    }

    public void setLastRequestURL( String url ) {
    }

    public DasServer getDasServer() {
        return this.server;
    }

    /**
     * Holds value of property redirect.
     */
    private boolean redirect= false;

    /**
     * Getter for property redirect.
     * @return Value of property redirect.
     */
    public boolean isRedirect() {
        return this.redirect;
    }

    /**
     * Setter for property redirect.
     * @param redirect New value of property redirect.
     */
    public void setRedirect(boolean redirect) {
        this.redirect = redirect;
    }

    /**
     * Holds value of property key.
     */
    private Key key=null;

    /**
     * Getter for property key.
     * @return Value of property key.
     */
    public Key getKey() {
        return this.key;
    }

    private String devel="";

    /**
     * use the develop version of the reader instead of the
     * production version.
     */
    public String getDevel() {
        return this.devel;
    }

    /**
     * use the develop version of the reader instead of the
     * production version.  If a reader was in dasHome/readers,
     * then use the one in /home/<devel>/readers/ instead.
     *
     */
    public void setDevel(String devel) {
        this.devel = devel;
    }

}
