/* File: DasServer.java
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

import edu.uiowa.physics.pw.das.DasProperties;
import edu.uiowa.physics.pw.das.DasIOException;
import edu.uiowa.physics.pw.das.client.*;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import java.awt.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.StringTokenizer;
/**
 *
 * @author  jbf
 */
public class DasServer {
    
    private String host;
    private String path;
    private int port;
    private StandardDataStreamSource standardDataStreamSource;
    
    private static Hashtable instanceHashtable= new Hashtable(); 
    
    public static DasServer plasmaWaveGroup;
    public static DasServer sarahandjeremy;
    static {
        try {       
        plasmaWaveGroup= DasServer.create(new URL("http://www-pw.physics.uiowa.edu/das/dasServer"));
        sarahandjeremy= DasServer.create(new URL("http://www.sarahandjeremy.net/das/dasServer.cgi"));
        } catch ( java.net.MalformedURLException e ) {
            edu.uiowa.physics.pw.das.util.DasExceptionHandler.handle(e);
        }
    }
    
    /** Creates a new instance of DasServer */
    private DasServer(String host, String path) {
        String[] s= host.split(":");
        if ( s.length>1 ) {
            this.port= Integer.parseInt(s[1]);
            host= s[0];
        } else {
            port= -1;
        }
        this.host= host;        
        this.path= path;
        this.standardDataStreamSource = new WebStandardDataStreamSource(this);
    }
    
    public String getURL() {
        if ( port==-1 ) {
            return "http://"+host+path;
        } else {
            return "http://"+host+":"+port+path;
        }
    }
    
    public static DasServer create( URL url ) {
        String host= url.getHost();        
        int port = url.getPort();
        if ( port!=-1 ) {
            host+= ":"+port;
        }
        String key= "http://" + host + url.getPath();
        if ( instanceHashtable.containsKey( key ) ) {
            return (DasServer) instanceHashtable.get( key );
        } else {            
            String path= url.getPath();
            DasServer result= new DasServer(host,path);            
            instanceHashtable.put(key,result);
            return result;
        }
    }
    
    public String getName() {
        String formData= "server=id";
        
        try {
            URL server= new URL("http",host,port,path+"?"+formData);
            //edu.uiowa.physics.pw.das.util.DasDie.println(edu.uiowa.physics.pw.das.util.DasDie.VERBOSE,server.toString());
            
            URLConnection urlConnection = server.openConnection();
            urlConnection.connect();
            
            String contentType = urlConnection.getContentType();
            
            InputStream in= urlConnection.getInputStream();
            
            byte[] data= read(in);
            return new String(data);
        } catch (IOException e) {
            
            return "";
        }
        
    }
    
    public ImageIcon getLogo() {
        
        String formData= "server=logo";
        
        try {
            URL server= new URL("http",host,port,path+"?"+formData);
            //edu.uiowa.physics.pw.das.util.DasDie.println(edu.uiowa.physics.pw.das.util.DasDie.VERBOSE,server.toString());
            
            URLConnection urlConnection = server.openConnection();
            urlConnection.connect();
            
            String contentType = urlConnection.getContentType();
            
            InputStream in= urlConnection.getInputStream();
            
            byte[] data= read(in);
            return new ImageIcon(data);
            
        } catch (IOException e) {
            
            return new ImageIcon();
        }
        
    }
    
    public TreeModel getDataSetList() throws edu.uiowa.physics.pw.das.DasException {
        String formData= "server=list";
        
        try {
            URL server= new URL("http",host,port,path+"?"+formData);
            //edu.uiowa.physics.pw.das.util.DasDie.println(edu.uiowa.physics.pw.das.util.DasDie.VERBOSE,server.toString());
            
            URLConnection urlConnection = server.openConnection();
            urlConnection.connect();
            
            String contentType = urlConnection.getContentType();
            
            InputStream in= urlConnection.getInputStream();
            
            return createModel(in);
            
        } catch (IOException e) {
            
            throw new DasIOException( e.getMessage() );
        }
        
    }
    
    private TreeModel createModel(InputStream uin) throws IOException {
        
        BufferedReader in = new BufferedReader( new InputStreamReader(uin) );
        
        DefaultMutableTreeNode root =
        new DefaultMutableTreeNode(host, true);
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
    
    public StandardDataStreamSource getStandardDataStreamSource() {
        return standardDataStreamSource;
    }
    
    public String getDataSetDescriptor(String dsdf) throws edu.uiowa.physics.pw.das.DasException {
        URL url;
        
        try {
            url = new URL("http", host, port, path+"?server=dsdf&dataset=" + dsdf);
        } catch ( MalformedURLException e ) {
            throw new DataSetDescriptorNotAvailableException("malformed URL");
        } catch ( IOException e ) {
            throw new DataSetDescriptorNotAvailableException("I/O Error");
        }
        
        edu.uiowa.physics.pw.das.util.DasDie.println(edu.uiowa.physics.pw.das.util.DasDie.VERBOSE,"getting "+url.toString());
        
        try {
            URLConnection connection = url.openConnection();
            connection.connect();
            String contentType = connection.getContentType();
            String[] s1= contentType.split(";"); // dump charset info
            contentType= s1[0];
            
            if (!contentType.equalsIgnoreCase("text/plain")) {
                BufferedReader bin = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String message = "";
                String line = bin.readLine();
                while (line != null) {
                    message = message.concat(line);
                    line = bin.readLine();
                }
                throw new IOException(message);
            } else {
                byte [] b= read(connection.getInputStream());
                return new String(b);
            }
        } catch ( FileNotFoundException e ) {
            throw new DasServerNotFoundException( e.getMessage() );
        } catch ( IOException e ) {
            throw new DasIOException(e.getMessage());
        }
        
        
    }
    
    public Key authenticate( String user, String pass) {
        try {
            Key result= null;
        
            String formData= "server=authenticator";
            formData+= "&user="+URLEncoder.encode(user, "UTF-8");
            String cryptPass= edu.uiowa.physics.pw.das.util.crypt.crypt(pass);
        
            if (pass.equals("sendPropertyPassword")) {
                cryptPass= DasProperties.getInstance().getProperty("password");
            }
        
            formData+= "&passwd="+URLEncoder.encode(cryptPass, "UTF-8");

            URL server= new URL("http",host,port,path+"?"+formData);
            edu.uiowa.physics.pw.das.util.DasDie.println(edu.uiowa.physics.pw.das.util.DasDie.VERBOSE,server.toString());
            
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
    
    public void changePassword( String user, String oldPass, String newPass ) throws DasServerException {
        try {
            String formData= "server=changePassword";
            formData+= "&user="+URLEncoder.encode(user, "UTF-8");
            String cryptPass= edu.uiowa.physics.pw.das.util.crypt.crypt(oldPass);
            formData+= "&passwd="+URLEncoder.encode(cryptPass, "UTF-8");
            String cryptNewPass= edu.uiowa.physics.pw.das.util.crypt.crypt(newPass);
            formData+= "&newPasswd="+URLEncoder.encode(cryptNewPass, "UTF-8");
        
            URL server= new URL("http",host,port,path+"?"+formData);
            edu.uiowa.physics.pw.das.util.DasDie.println(edu.uiowa.physics.pw.das.util.DasDie.VERBOSE,server.toString());
            
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
    
    public byte[] read(InputStream uin) throws IOException {
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
    
    public static void main(String[] argv) {
        DasServer dasServer= DasServer.plasmaWaveGroup;
        
        JFrame f= new JFrame();
        JPanel jpanel= new JPanel();
        jpanel.setLayout(new BorderLayout());
        jpanel.add(new JLabel(dasServer.getLogo()));
        
        f.setContentPane(jpanel);
        f.pack();
        f.setVisible(true);
        
        edu.uiowa.physics.pw.das.util.DasDie.println(dasServer.getName());
        try {
            edu.uiowa.physics.pw.das.util.DasDie.println(dasServer.getDataSetDescriptor("galileo/pws/best-e"));
        } catch (Exception e ) {
            edu.uiowa.physics.pw.das.util.DasDie.println(e);
        }
        
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
        return new URL( "http", host, port, path+"?"+formData );
    }
    
}
