/*
 * WebFileSystem.java
 *
 * Created on May 13, 2004, 1:22 PM
 *
 * A WebFileSystem allows web files to be opened just as if they were
 * local files, since it manages the transfer of the file to a local
 * file system.
 */

package edu.uiowa.physics.pw.das.util.fileSystem;

import edu.uiowa.physics.pw.das.*;
import edu.uiowa.physics.pw.das.util.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import javax.swing.*;

/**
 *
 * @author  Jeremy
 */
public class HttpFileSystem extends FileSystem {
    
    final File localRoot;
    final URL root;
    
    /** Creates a new instance of WebFileSystem */
    private HttpFileSystem(URL root, File localRoot) {
        /*try {
            root.openConnection().getContentLength();            
        } catch ( FileNotFoundException e ) {
            throw new RuntimeException( "URL doesn't appear to exist: "+root );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }*/
        if ( !root.toString().endsWith("/" ) ) {
            String s= root.toString();
            try {
                root= new URL( s+"/" );
            } catch ( MalformedURLException e ) {
                throw new RuntimeException(e);
            }
        }
        this.root= root;
        this.localRoot= localRoot;
    }
    
    public static HttpFileSystem createHttpFileSystem( URL root ) throws IOException {
        File x= File.createTempFile("WebFileSystem","~~~");
        File local= x.getParentFile();  // name of a local, large, writable directory
        local= new File( local, "das2/WebFileSystem/" );
        local= new File( local, root.getHost() );
        local= new File( local, root.getFile() );
        local.mkdirs();
        return new HttpFileSystem( root, local );
    }
    
    protected void transferFile( String filename, File f ) throws IOException {
        DasApplication.getDefaultApplication().getLogger().fine("create file "+filename);
        URL remoteURL= new URL( root.toString()+filename );
        InputStream in= remoteURL.openStream();
        if ( !f.getParentFile().exists() ) {
            f.getParentFile().mkdirs();
        }
        if ( f.createNewFile() ) {
            FileOutputStream out= new FileOutputStream( f );
            byte[] buf= new byte[2048];
            int br= in.read(buf);
            DasApplication.getDefaultApplication().getLogger().fine("transferring file "+filename);
            while ( br!=-1 ) {
                out.write( buf, 0, br );
                br= in.read(buf);
            }
            out.close();
        } else {
            handleException( new RuntimeException( "couldn't create local file: "+f ) );
        }
        in.close();
    }
    
    protected File getLocalRoot() {
        return this.localRoot;
    }
    
    protected URL getRoot() {
        return this.root;
    }
    
    public boolean isDirectory( String filename ) {
        File f= new File( localRoot, filename );
        if ( f.exists() ) {
            return f.isDirectory();
        } else {
            if ( filename.endsWith("/") ) {
                return true;
            } else {
                try {
                    File parentFile= f.getParentFile();
                    URL[] urls= HtmlUtil.getDirectoryListing( getURL( getLocalName( parentFile ) ) );
                    URL remoteUrl;
                    if ( filename.startsWith("/") ) {
                        remoteUrl= new URL( root+filename.substring(1)+"/" );
                    } else {
                        remoteUrl= new URL( root+filename+"/" );
                    }
                    for ( int i=0; i<urls.length; i++ ) {
                        if ( urls[i].equals( remoteUrl ) ) {
                            return true;
                        }
                    }
                    return false;
                } catch ( IOException e ) {
                    handleException(e);
                    return false;
                }
            }
        }
    }
    
    public String[] listDirectory( String directory ) {
        if ( ! isDirectory( directory ) ) {
            throw new IllegalArgumentException( "is not a directory: "+directory );
        }
        try {
            URL[] list= HtmlUtil.getDirectoryListing( getURL(directory ) );
            if ( list.length>100 ) {
                throw new IllegalStateException( "URL list is very long, refusing to transfer" );
            }
            String[] result= new String[list.length];
            for ( int i=0; i<list.length; i++ ) {
                URL url= list[i];
                result[i]= getLocalName(url);
            }
            return result;
        } catch ( IOException e ) {
            handleException(e);
            return new String[0];
        }
    }
    
    public String[] listDirectory( String directory, String regex ) {
        directory= toCanonicalFilename(directory);
        if ( ! isDirectory( directory ) ) {
            throw new IllegalArgumentException( "is not a directory: "+directory );
        }
        try {
            Pattern pattern= Pattern.compile(regex);
            URL[] list= HtmlUtil.getDirectoryListing( getURL(directory ) );
            if ( list.length>100 ) {
                throw new IllegalStateException( "URL list is very long, refusing to transfer" );
            }
            ArrayList result= new ArrayList();
            for ( int i=0; i<list.length; i++ ) {
                URL url= list[i];
                String r1= getLocalName(url).substring(directory.length());
                if ( pattern.matcher(r1).matches() ) {
                    result.add(r1);
                }
            }
            return (String[])result.toArray(new String[result.size()]);
            
        } catch ( IOException e ) {
            handleException(e);
            return new String[0];
        }
    }
    
    public URL getURL( String filename ) {
        try {
            filename= FileSystem.toCanonicalFilename(filename);
            return new URL( root+filename.substring(1) );
        } catch ( MalformedURLException e ) {
            throw new RuntimeException(e);
        }
    }
    
    public String getLocalName( File file ) {
        if ( !file.toString().startsWith(localRoot.toString() ) ) {
            throw new IllegalArgumentException( "file \""+file+"\"is not of this web file system" );
        }
        String filename= file.toString().substring(localRoot.toString().length() );
        filename= filename.replaceAll( "\\\\", "/" );
        return filename;
    }
    
    public String getLocalName( URL url ) {
        if ( !url.toString().startsWith(root.toString() ) ) {
            throw new IllegalArgumentException( "url \""+url+"\"is not of this web file system" );
        }
        String filename= FileSystem.toCanonicalFilename( url.toString().substring(root.toString().length() ) );
        return filename;
    }
    
    public FileObject getFile( String filename ) {
        HttpFileObject f= new HttpFileObject( this, filename, new Date(System.currentTimeMillis()) );        
        if ( f.canRead() ) {
            return f;
        } else {
            try {
                transferFile( filename, f.getLocalFile() );
                return f;
            } catch ( Exception e ) {
                throw new RuntimeException(e);
            }
        }
    }
    
    public String toString() {
        return "wfs: "+root;
    }
    
}
