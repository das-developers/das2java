/* File: DasServer.java
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

package org.das2.client;

import org.das2.stream.StreamDescriptor;
import org.das2.stream.StreamException;
import org.das2.stream.DasStreamFormatException;
import org.das2.util.URLBuddy;
import org.das2.DasIOException;
import org.das2.system.DasLogger;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Logger;

import org.das2.DasException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Represents a remote Das 2.1 compliant server.  
 * 
 * Use the create() method to instantiate new Das 2 server objects.  Each call to
 * create() will only allocate a new server instance if no server matching the given URL
 * has already been created.  
 * 
 * @author  jbf
 */

public class DasServer {

    private String sProto;
    private String host;
    private String path;
    private int port;
	 
	 @Deprecated
    private HashMap keys; // Holds a list of all non-http auth das2 server keys
	 
	 //Probably dead code, let's see
    //private Key key;

    private static final Logger logger= DasLogger.getLogger( DasLogger.DATA_TRANSFER_LOG );

	 /* Holds the global list of Das2 Server objects */
    private static HashMap instanceHashMap= new HashMap();

	 @Deprecated
    public static DasServer plasmaWaveGroup;
	 
	 @Deprecated
    public static DasServer sarahandjeremy;
	 
	 static {
        try {
            plasmaWaveGroup= DasServer.create(new URL("http://www-pw.physics.uiowa.edu/das/das2Server"));
            sarahandjeremy= DasServer.create(new URL("http://www.sarahandjeremy.net/das/dasServer.cgi"));
        } catch ( java.net.MalformedURLException e ) {
            org.das2.util.DasExceptionHandler.handle(e);
        }
    }

    private DasServer(String sProto, String host, String path) {
        String[] s= host.split(":");
        if ( s.length>1 ) {
            this.port= Integer.parseInt(s[1]);
            host= s[0];
        } else {
            port= -1;
        }
		  this.sProto = sProto;
        this.host= host;
        this.path= path;
        this.keys= new HashMap();
    }
    
	 /** Provide the Das2 Server location.  
	  * Note that more than one Das2 server may be present on a single host web-site.  
	  * @return A URL string containing protocol, host and path information.
	  */
    public String getURL() {
        if ( port==-1 ) {
            return sProto+"://"+host+path;
        } else {
            return sProto+"://"+host+":"+port+path;
        }
    }

	 /** Get a Das2 server instance.
	  * 
	  * @param url A Das2 resource URL.  Only the protocol, host, port and path information
	  *            are used.  All other items, such as a GET query are ignored.
	  * 
	  * @return If a server matching the given url's protocol, host and path has already
	  * been created, that instance is returned, otherwise a new instance is created.
	  */
    public static DasServer create( URL url ) {
		  String proto = url.getProtocol();
        String host= url.getHost();
        int port = url.getPort();
        if ( port!=-1 ) {
            host+= ":"+port;
        }
        String key= proto+"://" + host + url.getPath();
        if ( instanceHashMap.containsKey( key ) ) {
            logger.fine( "Using existing DasServer for "+url);
            return (DasServer) instanceHashMap.get( key );
        } else {
            String path= url.getPath();
            logger.fine( "Creating DasServer for "+url);
            DasServer result= new DasServer(proto, host, path);
            instanceHashMap.put(key,result);
            return result;
        }
    }
    
	 /** Query the remote DasServer for it's id string.
	  * For Das 2.1 servers this is handled by sending the GET query ?server=id.
	  * 
	  * @return A string containing the id, or the empty string if the query failed
	  */
    public String getName() {
        String formData= "server=id";

        try {
            URL server= new URL("http",host,port,path+"?"+formData);

            logger.fine( "connecting to "+server);
            URLConnection urlConnection = server.openConnection();
            urlConnection.connect();

            String contentType = urlConnection.getContentType();
            InputStream in= urlConnection.getInputStream();

            String result= new String(  read(in) );
            logger.fine( "response="+result);

            return result;
        } catch (IOException e) {
            return "";
        }
    }

	 /** Query the remote DasServer for it's image icon.
	  * For Das 2.1 servers this is handled by sending the GET query ?server=logo.
	  * 
	  * @return An ImageIcon with the server logo, or an empty ImageIcon if the request
	  *         failed
	  */
    public ImageIcon getLogo() {
        String formData= "server=logo";

        try {
            URL server= new URL("http",host,port,path+"?"+formData);

            logger.fine( "connecting to "+server);
            URLConnection urlConnection = server.openConnection();
            urlConnection.connect();

            String contentType = urlConnection.getContentType();
            InputStream in= urlConnection.getInputStream();

            byte[] data= read(in);
            logger.fine( "response="+data.length+" bytes");
            return new ImageIcon(data);

        } catch (IOException e) {
            return new ImageIcon();
        }
    }

	 /** Query the remote DasServer for a hierarchical tree of all data sources on 
	  * the server.
	  * For Das 2.1 servers this is handled by sending the GET query ?server=list.
	  * 
	  * @return 
	  */
    public TreeModel getDataSetList() throws org.das2.DasException {
        String formData= "server=list";

        try {
            URL server= new URL("http",host,port,path+"?"+formData);

            logger.fine( "connecting to "+server);

            URLConnection urlConnection = server.openConnection();
            urlConnection.connect();

            String contentType = urlConnection.getContentType();
            InputStream in= urlConnection.getInputStream();

            TreeModel result= createModel(in);
            logger.fine( "response->"+result);
            return result;

        } catch (IOException e) {
            throw new DasIOException( e.getMessage() );
        }
    }

    private TreeModel createModel(InputStream uin) throws IOException {

        BufferedReader in = new BufferedReader( new InputStreamReader(uin) );

        DefaultMutableTreeNode root =
        new DefaultMutableTreeNode( getURL(), true );
        DefaultTreeModel model = new DefaultTreeModel(root, true);
        String line = in.readLine();

        while (line != null) {
            DefaultMutableTreeNode current = root;
            StringTokenizer tokenizer = new StringTokenizer(line, "/");
            token: while (tokenizer.hasMoreTokens()) {
                String tok = tokenizer.nextToken();
                for (int index = 0; index < current.getChildCount(); index++) {
                    String str = current.getChildAt(index).toString();
                    if (str.equals(tok)) {
                        current =
                        (DefaultMutableTreeNode)current.getChildAt(index);
                        continue token;
                    }
                }
                DefaultMutableTreeNode node =
                new DefaultMutableTreeNode(tok,
                (tokenizer.hasMoreElements()
                ? true
                : line.endsWith("/")));
                current.add(node);
                current = node;
            }
            line = in.readLine();
        }
        return model;
    }

    public StandardDataStreamSource getStandardDataStreamSource(URL url) {
        return new WebStandardDataStreamSource(this, url);
    }

    public StreamDescriptor getStreamDescriptor( URL dataSetID ) throws DasException {
        try {
            String dsdf = dataSetID.getQuery().split("&")[0];
            URL url = new URL(sProto, host, port, path+"?server=dsdf&dataset=" + dsdf);

            logger.fine( "connecting to "+url);
            URLConnection connection = url.openConnection();
            connection.connect();
            String contentType = connection.getContentType();
            String[] s1= contentType.split(";"); // dump charset info
            contentType= s1[0];
				
				InputStream inStream = null;
				if(connection instanceof HttpURLConnection){
					HttpURLConnection httpConn = (HttpURLConnection) connection;
					int nStatus = httpConn.getResponseCode();
					if(nStatus >= 400)
						inStream = httpConn.getErrorStream();
				}
				if(inStream == null) inStream = connection.getInputStream();

            if (contentType.equalsIgnoreCase("text/plain") ||
					 contentType.equalsIgnoreCase("text/vnd.das2.das2stream") ) {
                PushbackReader reader = new PushbackReader(new InputStreamReader(inStream), 4);
                char[] four = new char[4];
                reader.read(four);
                if (new String(four).equals("[00]")) {
                    logger.fine("response is a das2Stream");
                    reader.skip(6);
                    Document header = StreamDescriptor.parseHeader(reader);
                    Element root = header.getDocumentElement();
						 switch(root.getTagName()){
						 case "stream":
							 return new StreamDescriptor(root);
						 case "exception":
							 logger.fine("response is an exception");
							 String type= root.getAttribute("type");
							 String sMsg = root.getAttribute("message");
							 String sExceptMsg = "stream exception: "+type;
							 if(!sMsg.isEmpty())
								 sExceptMsg = sExceptMsg + ", " + sMsg;
							 StreamException se= new StreamException( sExceptMsg );
							 throw new DasException(sExceptMsg, se);
						 case "":
							 throw new DasStreamFormatException();
						 default:
							 throw new DasStreamFormatException();
						 }
					 }
                else {
                    logger.fine("response is a legacy descriptor");
                    reader.unread(four);
                    BufferedReader in = new BufferedReader(reader);
                    StreamDescriptor result = StreamDescriptor.createLegacyDescriptor(in);
                    return result;
                }
            }
            else {
                BufferedReader in = new BufferedReader(new InputStreamReader(inStream));
                StringBuilder message = new StringBuilder();
                for (String line = in.readLine(); line != null; line = in.readLine()) {
                    message.append(line).append('\n');
                }
                throw new IOException(message.toString());
            }
        } catch ( MalformedURLException e ) {
            throw new DataSetDescriptorNotAvailableException("malformed URL");
        } catch ( FileNotFoundException e ) {
            throw new DasServerNotFoundException( e.getMessage() );
        } catch ( IOException e ) {
            throw new DasIOException(e.toString());
        }
    }
	 
	 /** Handles key based authentication */
	 @Deprecated
    public Key authenticate( String user, String passCrypt) {
        try {
            Key result= null;

            String formData= "server=authenticator";
            formData+= "&user="+URLBuddy.encodeUTF8(user);
            formData+= "&passwd="+URLBuddy.encodeUTF8(passCrypt);

            URL server= new URL("http",host,port,path+"?"+formData);

            logger.fine( "connecting to "+server);

            InputStream in= server.openStream();
            BufferedInputStream bin= new BufferedInputStream(in);

            String serverResponse= readServerResponse(bin);

            String errTag= "error";
            String keyTag= "key";

            if ( serverResponse.substring(0,keyTag.length()+2).equals("<"+keyTag+">")) {
                int index= serverResponse.indexOf("</"+keyTag+">");
                String keyString= serverResponse.substring(keyTag.length()+2,index);
                result= new Key(keyString);
            } else {
                result= null;
            }
            return result;
        } catch (UnsupportedEncodingException uee) {
            throw new AssertionError("UTF-8 not supported");
        } catch ( IOException e ) {
            return null;
        }
    }

    /** returns a List<String> of resource Id's available with this key */
	 @Deprecated
    public List groups( Key key ) {
        try {
            String formData= "server=groups";
            formData+= "&key="+URLBuddy.encodeUTF8(key.toString());

            URL server= new URL("http",host,port,path+"?"+formData);

            logger.fine( "connecting to "+server);

            InputStream in= server.openStream();
            BufferedInputStream bin= new BufferedInputStream(in);

            String serverResponse= readServerResponse(bin);

            String[] groups= serverResponse.split(",");
            ArrayList result= new ArrayList();
            for ( int i=0; i<groups.length; i++ ) {
                groups[i]= groups[i].trim();
                if ( !"".equals(groups[i]) ) result.add( groups[i] );
            }

            return result;
        } catch ( IOException e ) {
            throw new RuntimeException(e);
        } 
    }

	 @Deprecated
    public void changePassword( String user, String oldPass, String newPass ) throws DasServerException {
        try {
            String formData= "server=changePassword";
            formData+= "&user="+URLBuddy.encodeUTF8(user);
            String cryptPass= org.das2.util.Crypt.crypt(oldPass);
            formData+= "&passwd="+URLBuddy.encodeUTF8(cryptPass);
            String cryptNewPass= org.das2.util.Crypt.crypt(newPass);
            formData+= "&newPasswd="+URLBuddy.encodeUTF8(cryptNewPass);

            URL server= new URL("http",host,port,path+"?"+formData);
            logger.fine( "connecting to "+server);

            InputStream in= server.openStream();
            BufferedInputStream bin= new BufferedInputStream(in);

            String serverResponse= readServerResponse(bin);

            String errTag= "error";
            String keyTag= "key";

            if ( serverResponse.substring(0,errTag.length()+2).equals("<"+errTag+">")) {
                int index= serverResponse.indexOf("</"+errTag+">");
                String errString= serverResponse.substring(errTag.length()+2,index);
                if (errString.equals("<badAuthentication/>")) {
                    throw new DasServerException("Bad User/Pass");
                }
            }
        } catch (UnsupportedEncodingException uee) {
            throw new AssertionError("UTF-8 not supported");
        } catch ( IOException e ) {
            throw new DasServerException("Failed Connection");
        }


    }

	 @Deprecated
    public String readServerResponse( BufferedInputStream in ) {
        // Read <dasResponse>...</dasResponse>, leaving the InputStream immediately after //

        in.mark(Integer.MAX_VALUE);

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
                while ( !new String( data,0,offset,"UTF-8" ).contains("</"+das2ResponseTag+">") &&
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

        logger.fine( "response="+das2Response);

        return das2Response;
    }

	 // Utility function to handle reading data off the HTTP stream.  Used by functions
	 // such as getName and getLogo that don't expect to receive a Das2 Stream
    private byte[] read(InputStream uin) throws IOException {
        LinkedList list = new LinkedList();
        byte[] data;
        int bytesRead=0;
        int totalBytesRead=0;

        //BufferedInputStream in= new BufferedInputStream(uin,4096*2);
        InputStream in= uin;

        long time = System.currentTimeMillis();
        //        fireReaderStarted();

        //FileOutputStream out= new FileOutputStream("x."+time+".dat");

        data = new byte[4096];

        int lastBytesRead = -1;

        String s;

        int offset=0;

        //        if (requestor != null) {
        //            requestor.totalByteCount(-1);
        //        }

        bytesRead= in.read(data,offset,4096-offset);

        while (bytesRead != -1) {

            int bytesSoFar = totalBytesRead;
            //            fireReaderUpdate(bytesSoFar);
            //            if (requestor != null) {
            //                requestor.currentByteCount(bytesSoFar);
            //            }

            offset+=bytesRead;
            lastBytesRead= offset;

            if (offset==4096) {
                list.addLast(data);
                data = new byte[4096];
                offset=0;
            }

            totalBytesRead+= bytesRead;

            bytesRead= in.read(data,offset,4096-offset);

        }

        if (lastBytesRead<4096) {
            list.addLast(data);
        }

        if (list.size()== 0) {
            return new byte[0];
        }

        int dataLength = (list.size()-1)*4096 + lastBytesRead;

        data = new byte[dataLength];

        Iterator iterator = list.iterator();
        int i;
        for (i = 0; i < list.size()-1; i++) {
            System.arraycopy(iterator.next(), 0, data, i*4096, 4096);
        }
        System.arraycopy(iterator.next(), 0, data, i*4096, lastBytesRead);

        return data;
    }

	 public String getProto() {
	     return sProto;	 
	 }
	 
    public String getHost() {
        return host;
    }

    public int getPort() {
        // returns -1 if the port was not specified, which can be used with URL constructor
        return port;
    }

    public String getPath() {
        return path;
    }

    public URL getURL( String formData ) throws MalformedURLException {
        return new URL( sProto, host, port, path+"?"+formData );
    }

    public Key getKey( String resource ) {
        synchronized (this) {
            if ( keys.get(resource)==null ) {
                Authenticator authenticator;
                authenticator= new Authenticator(this,resource);
                Key key= authenticator.authenticate();
                if ( key!=null ) keys.put( resource, key);
            }
        }
        return (Key)keys.get(resource);
    }

	 //Let's see if this is actually used anywhere
    //public void setKey( Key key ) {
    //    this.key= key;
    //}

	 @Override
    public String toString() {
        return this.getURL();
    }
}
