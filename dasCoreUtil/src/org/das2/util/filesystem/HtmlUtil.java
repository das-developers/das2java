/* Copyright (C) 2003-2008 The University of Iowa 
 *
 * This file is part of the Das2 <www.das2.org> utilities library.
 *
 * Das2 utilities are free software: you can redistribute and/or modify them
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Das2 utilities are distributed in the hope that they will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * as well as the GNU General Public License along with Das2 utilities.  If
 * not, see <http://www.gnu.org/licenses/>.
 *
 * HtmlUtil.java
 *
 * Created on May 14, 2004, 9:06 AM
 */

package org.das2.util.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.logging.Level;
import org.das2.util.monitor.CancelledOperationException;
import org.das2.util.Base64;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.das2.util.FileUtil;
import org.das2.util.LoggerManager;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * HTML utilities, such as getting a directory listing, where a "file" is a link
 * below the directory we are listing, and read a URL into a String.
 * @author  Jeremy
 */
public class HtmlUtil {

    private final static Logger logger= LoggerManager.getLogger( "das2.filesystem.htmlutil" );
    /**
     * this logger is for opening connections to remote sites.
     */
    protected static final Logger loggerUrl= org.das2.util.LoggerManager.getLogger( "das2.url" );
    
    public static boolean isDirectory( URL url ) {
        String file= url.getFile();
        return file.charAt(file.length()-1) != '/';
    }

    /**
     * nice clients consume both the stderr and stdout coming from websites.
     * This reads everything off of the stream and closes it.
     * http://docs.oracle.com/javase/1.5.0/docs/guide/net/http-keepalive.html suggests that you "do not abandon connection"
     * @param err the input stream
     * @throws IOException 
     * @see HttpUtil#consumeStream(java.io.InputStream) 
     */
    @Deprecated
    public static void consumeStream( InputStream err ) throws IOException {
        HttpUtil.consumeStream(err);
    }
    
    private static URL[] getDirectoryListingAmazonS3( URL root, String content ) {
        try {
            Reader reader = new StringReader(content);
            DocumentBuilder builder= DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputSource source = new InputSource(reader);
            Document document= builder.parse(source);
            
            XPathFactory factory= XPathFactory.newInstance();
            javax.xml.xpath.XPath xpath= factory.newXPath();
            NodeList fs= (NodeList) xpath.evaluate( "/ListBucketResult/Contents/Key", document, XPathConstants.NODESET );
            
            int n= fs.getLength();
            URL[] result= new URL[n];
            for ( int i=0; i<n; i++ ) {
                org.w3c.dom.Node nn= fs.item(i);
                try {
                    result[i]= new URL( root, nn.getTextContent() );
                } catch ( Exception e ) {
                    
                }
            }
            return result;
        } catch (SAXException | IOException | XPathExpressionException | ParserConfigurationException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * Get the listing of the web directory, returning links that are "under" the given URL.
     * Note this does not handle off-line modes where we need to log into
     * a website first, as is often the case for a hotel.
     *
     * This was refactored to support caching of listings by simply writing the content to disk.
     *
     * @param url the address.
     * @param urlStream stream containing the URL content, which must be UTF-8 (or US-ASCII)
     * @return list of URIs referred to in the page.
     * @throws IOException
     * @throws CancelledOperationException
     */
    public static URL[] getDirectoryListing( URL url, InputStream urlStream ) throws IOException, CancelledOperationException {    
        return getDirectoryListing( url, urlStream, true );
    }
    
    /**
     * Get the listing of the web directory, returning links that are "under" the given URL.
     * Note this does not handle off-line modes where we need to log into
     * a website first, as is often the case for a hotel.
     *
     * This was refactored to support caching of listings by simply writing the content to disk.
     *
     * @param url the address.
     * @param urlStream stream containing the URL content, which must be UTF-8 (or US-ASCII)
     * @param childCheck only return links to URLs "under" the url.
     * @return list of URIs referred to in the page.
     * @throws IOException
     * @throws CancelledOperationException
     */
    public static URL[] getDirectoryListing( URL url, InputStream urlStream, boolean childCheck ) throws IOException, CancelledOperationException {
        // search the input stream for links
        // first, read in the entire URL

        long t0= System.currentTimeMillis();
        byte b[] = new byte[10000];
        int numRead = urlStream.read(b);  // i18n  also decorator in script to make plot
        StringBuilder contentBuffer = new StringBuilder( 10000 );

        if ( numRead!=-1 ) contentBuffer.append( new String( b, 0, numRead, "UTF-8" ) );
        while (numRead != -1) {
            logger.finest("download listing");
            numRead = urlStream.read(b);
            if (numRead != -1) {
                String newContent = new String(b, 0, numRead, "UTF-8");
                contentBuffer.append( newContent );
            }
        }
        urlStream.close();

        logger.log(Level.FINER, "read listing data in {0} millis", (System.currentTimeMillis() - t0));
        String content= contentBuffer.toString();

        //logger.log(Level.WARNING, "listing length (bytes): {0} {1}", new Object[]{content.length(), url});
        
        if ( content.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<ListBucketResult")) {
            return getDirectoryListingAmazonS3( url, content );
        }
        //TODO: use getLinks to get all the links.
        
        String hrefRegex= "(?i)href\\s*=\\s*([\"'])(.+?)\\1";
        Pattern hrefPattern= Pattern.compile( hrefRegex );

        Matcher matcher= hrefPattern.matcher( content );

        ArrayList urlList= new ArrayList();

        String surl= url.toString();

        while ( matcher.find() ) {
            
            String strLink= matcher.group(2);
            logger.log(Level.FINEST, "parse listing {0}", strLink);
            URL urlLink;

            try {
                urlLink = new URL(url, URLDecoder.decode(strLink,"UTF-8") );
                strLink = urlLink.toString();
                if ( strLink.contains("data-item-type=") ) { // kludge for https://github.com/autoplot/dev/master/bugs/sf/2376/
                    continue;
                }
                if ( strLink.contains("#") ) {
                    continue;   // get rid of many https://abbith.physics.uiowa.edu/assets/icons-2cb47a6dce56387af715816406f3f0d5d68651436bd5c96807123fcf421ad07d.svg#chevron-down
                }
            } catch (MalformedURLException e) {
                logger.log(Level.SEVERE, "bad URL: {0} {1}", new Object[]{url, strLink});
                continue;
            }

            if ( childCheck ) {
                if ( strLink.startsWith(surl) && strLink.length() > surl.length() && null==urlLink.getQuery() ) {
                    String file= strLink.substring( surl.length() );
                    if ( !file.startsWith("../") ) {
                        urlList.add( urlLink );
                    }
                }
            } else {
                urlList.add( urlLink );
            }
        }

        return (URL[]) urlList.toArray( new URL[urlList.size()] );

    }

    /**
     * Get the listing of the web directory, returning links that are "under" the given URL.
     * Note this does not handle off-line modes where we need to log into
     * a website first, as is often the case for a hotel.
     * @param url
     * @return list of URIs referred to in the page.
     * @throws IOException
     * @throws CancelledOperationException
     */
    public static URL[] getDirectoryListing( URL url ) throws IOException, CancelledOperationException {

        logger.log(Level.FINER, "listing {0}", url);
        
        String file= url.getFile();
        if ( file.charAt(file.length()-1)!='/' ) {
            url= new URL( url.toString()+'/' );
        }
        
        InputStream urlStream= getInputStream(url);
        
        return getDirectoryListing( url, urlStream );
    }
    
    /**
     * get the inputStream, following redirects if a 301 or 302 is encountered.  
     * The scientist may be prompted for a password, but only if "user@" is
     * in the URL.
     * 
     * Note this does not explicitly close the connections
     * to the server, and Java may not know to release the resources.  
     * TODO: fix this by wrapping the input stream and closing the connection
     * when the stream is closed.  This was done in Autoplot's DataSetURI.downloadResourceAsTempFile
     * @see org.autoplot.datasource.DataSetURI#downloadResourceAsTempFile
     * 
     * @param url
     * @return input stream
     * @throws IOException 
     * @throws org.das2.util.monitor.CancelledOperationException 
     */
    public static InputStream getInputStream( URL url ) throws IOException, CancelledOperationException {

        loggerUrl.log(Level.FINE, "getInputStream {0}", new Object[] { url } );
        
        long t0= System.currentTimeMillis();
        
        String userInfo= KeyChain.getDefault().getUserInfo(url);

        //long t0= System.currentTimeMillis();
        loggerUrl.log(Level.FINE, "openConnect {0}", new Object[] { url } );
        URLConnection urlConnection = url.openConnection();

        urlConnection.setAllowUserInteraction(false);
        urlConnection.setConnectTimeout(FileSystem.settings().getConnectTimeoutMs() );
        
        logger.log(Level.FINER, "connected in {0} millis", (System.currentTimeMillis() - t0));
        if ( userInfo != null) {
            String encode = Base64.getEncoder().encodeToString( userInfo.getBytes());
            urlConnection.setRequestProperty("Authorization", "Basic " + encode);
        }
                
        urlConnection= HttpUtil.checkRedirect( urlConnection );
        InputStream ins= urlConnection.getInputStream();
        
        boolean keepResponseForDebugging=false;
        
        if ( !keepResponseForDebugging ) {
            return ins;
        } else {
            StringBuilder keep= new StringBuilder();
            byte[] buf= new byte[4096];
            int bytesRead=ins.read(buf);
            int totalBytesRead=0;
            while ( bytesRead>0 ) {
                totalBytesRead+=bytesRead;
                for ( int i=0; i<bytesRead; i++ ) {
                    keep.append((char)buf[i]);
                }
                bytesRead=ins.read(buf);
            }
            long t02= (System.currentTimeMillis()-1718399881869L);
            File file= new File( "/tmp/ap/"+url.getFile().hashCode()+"."+ String.format("%09d",t02) +".html");
            FileUtil.writeStringToFile( file, keep.toString() );
            logger.log(Level.INFO, "writing html listing to {0}", file);
            return new FileInputStream(file);
        }
        
    }
    
    /**
     * read the contents of the URL into a string, assuming UTF-8 encoding.
     * @param url
     * @return
     * @throws IOException
     * @throws CancelledOperationException 
     */
    public static String readToString( URL url ) throws IOException, CancelledOperationException {
        InputStream ins= getInputStream( url );
        StringBuilder build= new StringBuilder();
        byte[] buf= new byte[2048];
        int i= ins.read(buf);
        Charset charset= Charset.forName("UTF-8");
        while ( i>-1 ) {
            build.append( new String( buf, 0, i, charset ) );
            i= ins.read(buf);
        }
        return build.toString();
    }

    /**
     * return the metadata about a URL.  This will support http, https,
     * and ftp, and will check for redirects.  This will 
     * allow caching of head requests.
     * @param url ftp,https, or http URL
     * @param props, if non-null, may be a map containing cookie.
     * @return the metadata
     * @throws java.io.IOException when HEAD requests are made.
     * @see HttpUtil#getMetadata(java.net.URL, java.util.Map) 
     */
    @Deprecated
    public static Map<String,String> getMetadata( URL url, Map<String,String> props ) throws IOException {
        return HttpUtil.getMetadata(url, props);
    }
    
    /**
     * check for 301, 302 or 303 redirects, and return a new connection in this case.
     * This should be called immediately before the urlConnection.connect call,
     * as this must connect to get the response code.
     * @param urlConnection if an HttpUrlConnection, check for 301 or 302; return connection otherwise.
     * @return a connection, typically the same one as passed in.
     * @throws IOException 
     * @see HttpUtil#checkRedirect(java.net.URLConnection) 
     */
    @Deprecated
    public static URLConnection checkRedirect( URLConnection urlConnection ) throws IOException {
        return HttpUtil.checkRedirect(urlConnection);
    }

    /**
     * return the links found in the content, using url as the context.
     * @param url null or the url for the context.
     * @param content the html content.
     * @return a list of URLs.
     */
    public static List<URL> getLinks( URL url, String content ) {
        String hrefRegex= "(?i)href\\s*=\\s*([\"'])(.+?)\\1";
        Pattern hrefPattern= Pattern.compile( hrefRegex );

        Matcher matcher= hrefPattern.matcher( content );

        ArrayList urlList= new ArrayList();

        while ( matcher.find() ) {
            String strLink= matcher.group(2);
            logger.log(Level.FINEST, "parse listing {0}", strLink);
            URL urlLink;

            try {
                urlLink = new URL(url, URLDecoder.decode(strLink,"UTF-8") );
                urlList.add( urlLink );
            } catch (MalformedURLException e) {
                logger.log(Level.SEVERE, "bad URL: {0} {1}", new Object[]{url, strLink});
                continue;
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(HtmlUtil.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        return urlList;
    }
    
}
