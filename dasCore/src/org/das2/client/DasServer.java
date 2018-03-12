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

import java.util.logging.Level;
import org.das2.stream.StreamDescriptor;
import org.das2.stream.StreamException;
import org.das2.stream.DasStreamFormatException;
import org.das2.util.URLBuddy;
import org.das2.DasIOException;
import org.das2.system.DasLogger;
import org.das2.util.filesystem.FileSystem;

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
 * A DasServer is the object that holds the methods of a remote das server.
 * These include, for example, getLogo() which returns a graphical mnemonic
 * for the server, authenticate() and setPassword().
 */

public class DasServer {

    private String sProto;
    private String host;
    private String path;
    private int port;
    
	 @Deprecated
    private final HashMap keys; // Holds a list of all non-http auth das2 server keys
	 
	 //Probably dead code, let's see
    //private Key key;

    private static final Logger logger= DasLogger.getLogger( DasLogger.DATA_TRANSFER_LOG );

	 /* Holds the global list of Das2 Server objects */
    private static HashMap instanceHashMap= new HashMap();

//	 @Deprecated
//    public static DasServer plasmaWaveGroup;
//	 
//	 @Deprecated
//    public static DasServer sarahandjeremy;
//	 
//    static {
//        try {
//            plasmaWaveGroup= DasServer.create(new URL("http://www-pw.physics.uiowa.edu/das/das2Server"));
//        } catch ( java.net.MalformedURLException e ) {
//            throw new IllegalArgumentException(e);
//        }
//        try {
//            sarahandjeremy= DasServer.create(new URL("http://www.sarahandjeremy.net/das/dasServer.cgi"));
//        } catch ( java.net.MalformedURLException e ) {
//            throw new IllegalArgumentException(e);
//        }
//    }

    /**
     * return one DasServer to serve as an example.
     * @return 
     */
    public static DasServer createPlasmaWaveGroup() {
        try {
            return DasServer.create(new URL("http://www-pw.physics.uiowa.edu/das/das2Server"));
        } catch ( java.net.MalformedURLException e ) {
            throw new IllegalArgumentException(e);
        }
    }
    
	/** Class to represent know information about an item in a list of data sources.
	 * @author cwp
	 */
	public static class DataSrcListItem {
		private boolean bDirectory;
		private String sName;
		private String sDesc;
		/** Create a data source item with a description string */
		public DataSrcListItem(boolean bDirectory, String sName, String sDesc){
			this.bDirectory = bDirectory;
			this.sName = sName;
			this.sDesc = sDesc;
		}
		public boolean isDirectory(){return bDirectory;}
		public boolean isDataSource(){return !bDirectory;}
		public String name(){return sName;}
		public String description(){return sDesc;}
		@Override
		public String toString(){return sName;}
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
            logger.log( Level.FINE, "Using existing DasServer for {0}", url);
            return (DasServer) instanceHashMap.get( key );
        } else {
            String path= url.getPath();
            logger.log( Level.FINE, "Creating DasServer for {0}", url);
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
        InputStream in=null;
        try {
            URL server= new URL("http",host,port,path+"?"+formData);

            logger.log( Level.FINE, "connecting to {0}", server);
            URLConnection urlConnection = server.openConnection();
            urlConnection.connect();

            in= urlConnection.getInputStream();

            String result= new String(  read(in) );
            logger.log( Level.FINE, "response={0}", result);

            return result;
        } catch (IOException e) {

            return "";
        } finally {
            if ( in!=null ) try {
                in.close();
            } catch ( IOException ex ) {   
                logger.log( Level.WARNING, ex.toString(), ex );
            }
        }

    }

    public ImageIcon getLogo() {

        String formData= "server=logo";
        InputStream in=null;
        try {
            URL server= new URL("http",host,port,path+"?"+formData);

            logger.log( Level.FINE, "connecting to {0}", server);
            URLConnection urlConnection = server.openConnection();
            urlConnection.connect();

            in= urlConnection.getInputStream();

            byte[] data= read(in);
            logger.log( Level.FINE, "response={0} bytes", data.length);
            return new ImageIcon(data);

        } catch (IOException e) {

            return new ImageIcon();
        } finally {
            if ( in!=null ) try {
                in.close();
            } catch ( IOException ex ) {   
                logger.log( Level.WARNING, ex.toString(), ex );
            }
        }

    }

    public TreeModel getDataSetListWithDiscovery() throws org.das2.DasException {
        return getDataSetList("?server=discovery");
    }

    public TreeModel getDataSetList() throws org.das2.DasException {
        return getDataSetList("?server=list");
    }
    
    protected TreeModel getDataSetList(String sSuffix) throws DasIOException{
        InputStream in=null;
        try {
            URL server= new URL(sProto,host,port,path+sSuffix);

            logger.log( Level.FINE, "connecting to {0}", server);

            URLConnection conn = server.openConnection();
            conn.setConnectTimeout(FileSystem.settings().getConnectTimeoutMs());
            if(conn instanceof HttpURLConnection){
                HttpURLConnection httpConn = (HttpURLConnection) conn;
                int nStatus = httpConn.getResponseCode();
                    
                if(nStatus >= 400)   // Just fail on 400's and 500's
                    throw new java.io.IOException("Server returned HTTP response "
                           + "code:" + nStatus + " for URL: " + server);
            }
            in= conn.getInputStream();
            TreeModel result= createModel(in);
            logger.log( Level.FINE, "response->{0}", result);
            return result;

        } catch (IOException e) {
            throw new DasIOException( e.getMessage() );
        } finally {
            if ( in!=null ) try {
                in.close();
            } catch ( IOException ex ) {   
                logger.log( Level.WARNING, ex.toString(), ex );
            }
        } 
    }

    /**
     * sort the DefaultMutableTreeNodes so the directories are listed before
     * the files.
     * @param tn 
     */
    private void sortDirectories( DefaultMutableTreeNode tn ) {
        DefaultMutableTreeNode[] children= new DefaultMutableTreeNode[tn.getChildCount()];
        int ichild=0;
        for ( int i=0; i<tn.getChildCount(); i++ ) {
            if ( tn.getChildAt(i).getAllowsChildren() ) {
                DefaultMutableTreeNode childWithKids= (DefaultMutableTreeNode)tn.getChildAt(i);
                sortDirectories(childWithKids);
                children[ichild]= childWithKids;
                ichild++;
            }
        }
        for ( int i=0; i<tn.getChildCount(); i++ ) {
            if ( ! tn.getChildAt(i).getAllowsChildren() ) {
                children[ichild]= (DefaultMutableTreeNode)tn.getChildAt(i);
                ichild++;
            }
        }
        int kidCount= tn.getChildCount();
        for ( int i=0; i<kidCount; i++ ) {
            tn.remove(0);
        }
        for ( int i=0; i<kidCount; i++ ) {
            tn.insert( children[i], i);
        }
        
    }

    /**
     * Return a tree of the datasets and folders, listing the sub-folders 
     * first in a folder.  Node user objects are 
     * 
     * 2014-10-08: There can be pipe characters in the response. https://sourceforge.net/p/autoplot/feature-requests/393/
     * @param uin input stream 
     * @return treeModel of the data on the server.
     * @throws IOException 
     */
    private TreeModel createModel(InputStream uin) throws IOException {

        BufferedReader in = new BufferedReader( new InputStreamReader(uin) );

		  DataSrcListItem rootData = new DataSrcListItem(true, getURL(), null);
		  
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(rootData, true );
        DefaultTreeModel model = new DefaultTreeModel(root, true);
        String line = in.readLine();

        while (line != null) {
			  String sDesc = null;
            int ipipe= line.indexOf('|');
            if ( ipipe>-1 ) {
					sDesc = line.substring(ipipe+1).trim();
					if(sDesc.isEmpty()) sDesc = null;
               line = line.substring(0,ipipe).trim();
				}
				
            DefaultMutableTreeNode current = root;
            StringTokenizer tokenizer = new StringTokenizer(line, "/");
            token: while (tokenizer.hasMoreTokens()) {
                String tok = tokenizer.nextToken();
                for (int index = 0; index < current.getChildCount(); index++) {
                    String str = current.getChildAt(index).toString();
                    if (str.equals(tok)) {
                        current = (DefaultMutableTreeNode)current.getChildAt(index);
                        continue token;
                    }
                }
					 boolean bDir = tokenizer.hasMoreElements() ? true : line.endsWith("/");
					 DataSrcListItem dsItem = new DataSrcListItem(bDir, tok, sDesc);
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(dsItem, bDir);
                current.add(node);
                current = node;
            }
            line = in.readLine();
        }
        sortDirectories( root );        
        return model;
    }

    public StandardDataStreamSource getStandardDataStreamSource(URL url) {
        return new WebStandardDataStreamSource(this, url);
    }

    public StreamDescriptor getStreamDescriptor( URL dataSetID ) throws DasException {
        try {
            String dsdf = dataSetID.getQuery().split("&")[0];
            URL url = new URL(sProto, host, port, path+"?server=dsdf&dataset=" + dsdf);

            logger.log( Level.FINE, "connecting to {0}", url);
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
                int count= reader.read(four);
                if ( count!=4 ) throw new IllegalArgumentException("failed to read four characters");
                if (new String(four).equals("[00]")) {
                    logger.fine("response is a das2Stream");
                    if ( reader.skip(6)!=6 ) throw new IllegalArgumentException("expected to skip six characters");
                    Document header = StreamDescriptor.parseHeader(reader);
                    Element root = header.getDocumentElement();
                    if (root.getTagName().equals("stream")) {
                        return new StreamDescriptor(root);
                    }
                    else if ( root.getTagName().equals("exception") ) {
                        logger.info("response is an exception");
                        String type= root.getAttribute("type");
                        StreamException se= new StreamException( "stream exception: "+type );
                        throw new DasException("stream exception: " + type, se);
                    }
                    else if (root.getTagName().equals("")) {
                        throw new DasStreamFormatException();
                    }
                    else {
                        throw new DasStreamFormatException();
                    }
                }
                else {
                    logger.info("response is a legacy descriptor");
                    reader.unread(four);
                    BufferedReader in = new BufferedReader(reader);
                    StreamDescriptor result = StreamDescriptor.createLegacyDescriptor(in);
                    return result;
                }
            }
            else {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
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

            logger.log( Level.FINE, "connecting to {0}", server);

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

    /** 
     * returns a {@code List<String>} of resource Id's available with this key
     * @param key the key
     * @return a list of the groups.
     * @deprecated this is not used.
     */
    public List groups( Key key ) {
        try {
            String formData= "server=groups";
            formData+= "&key="+URLBuddy.encodeUTF8(key.toString());

            URL server= new URL("http",host,port,path+"?"+formData);

            logger.log( Level.FINE, "connecting to {0}", server);

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

    /**
     * old code for changing password
     * @param user
     * @param oldPass
     * @param newPass
     * @throws DasServerException
     * @deprecated
     */
    public void changePassword( String user, String oldPass, String newPass ) throws DasServerException {
        try {
            String formData= "server=changePassword";
            formData+= "&user="+URLBuddy.encodeUTF8(user);
            String cryptPass= org.das2.util.Crypt.crypt(oldPass);
            formData+= "&passwd="+URLBuddy.encodeUTF8(cryptPass);
            String cryptNewPass= org.das2.util.Crypt.crypt(newPass);
            formData+= "&newPasswd="+URLBuddy.encodeUTF8(cryptNewPass);

            URL server= new URL("http",host,port,path+"?"+formData);
            logger.log( Level.FINE, "connecting to {0}", server);

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

    /**
     * 
     * @param in
     * @return
     * @deprecated
     */
    public String readServerResponse( BufferedInputStream in ) {
        // Read <dasResponse>...</dasResponse>, leaving the InputStream immediately after //

        in.mark(Integer.MAX_VALUE);

        String das2Response;

        byte[] data = new byte[4096];

        //int lastBytesRead = -1;

        //String s;

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

                logger.log(Level.FINER, "das2Response={0}", das2Response);

                in.reset();
                long n= das2Response.length() + 2 * das2ResponseTag.length() + 5;
                while ( n>0 ) {
                    long k= in.skip( n );
                    n-= k;
                }

            } else {
                in.reset();

                das2Response="";
            }
        } catch ( IOException e ) {
            das2Response= "";
        }

        logger.log( Level.FINE, "response={0}", das2Response);

        return das2Response;
    }

    /**
     * Utility function to handle reading data off the HTTP stream.  Used by functions
     * such as getName and getLogo that don't expect to receive a Das2 Stream
     * 
     * @param uin
     * @return
     * @throws IOException 
     */
    private byte[] read(InputStream uin) throws IOException {
        LinkedList<byte[]> list = new LinkedList();
        byte[] data;
        int bytesRead=0;
        //int totalBytesRead=0;

        //BufferedInputStream in= new BufferedInputStream(uin,4096*2);
        InputStream in= uin;

        //long time = System.currentTimeMillis();
        //        fireReaderStarted();

        //FileOutputStream out= new FileOutputStream("x."+time+".dat");

        data = new byte[4096];

        int lastBytesRead = -1;

        int offset=0;

        //        if (requestor != null) {
        //            requestor.totalByteCount(-1);
        //        }

        bytesRead= in.read(data,offset,4096-offset);

        while (bytesRead != -1) {

            //int bytesSoFar = totalBytesRead;
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

            //totalBytesRead+= bytesRead;

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

        Iterator<byte[]> iterator = list.iterator();
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
                Key key1= authenticator.authenticate();
                if ( key1!=null ) keys.put( resource, key1);
            }
        }
        return (Key)keys.get(resource);
    }

    public void setKey( Key key ) {
        logger.info("this key is ignored");
    }

    @Override
    public String toString() {
        return this.getURL();
    }
}
