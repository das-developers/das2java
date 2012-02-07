/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.util.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.zip.GZIPInputStream;

/**
 *
 * @author jbf
 */
public class FileSystemUtil {

    /**
     * Dump the contents of the inputstream into a file.  If the inputStream comes
     * from a file, then java.nio is used to transfer the data quickly.
     * @param in
     * @param f
     * @throws java.io.FileNotFoundException
     * @throws java.io.IOException
     */
    public static void dumpToFile( InputStream in, File f ) throws FileNotFoundException, IOException {

        ReadableByteChannel ic = Channels.newChannel(in);
        FileChannel oc=null;
        try {
            oc= new FileOutputStream(f).getChannel();
            if ( ic instanceof FileChannel ) {
                FileChannel fic= (FileChannel)ic;
                fic.transferTo(0, fic.size(), oc);
                fic.close();
                oc.close();
            } else {
                ByteBuffer buf= ByteBuffer.allocateDirect( 16*1024 );
                while ( ic.read(buf) >= 0 || buf.position() != 0 ) {
                    buf.flip();
                    oc.write(buf);
                    buf.compact();
                }
            }
        } finally {
            if ( oc!=null ) oc.close();
        }
    }


    /**
     * un-gzip the file.  This is similar to the unix gunzip command.
     * @param fz zipped input file
     * @param file unzipped destination file
     */
    public static void unzip( File fz, File file) throws IOException {
        GZIPInputStream in= null;
        OutputStream out= null;
        try {
            in = new GZIPInputStream(new FileInputStream(fz));
            out = new FileOutputStream(file);

            byte[] buf = new byte[1024];  //TODO: use FileChannel
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } finally {
            try {
                if ( in!=null ) in.close();
            } finally {
                if ( out!=null ) out.close();
            }
        }
    }

    /**
     * return null if the URI is not cacheable, or the URI of the parent if it is.
     *
     * For example,
     * <pre>
     * {@code
     * URI uri= new URL("http://autoplot.org/data/demos2011.xml").toURI();
     * URI parentUri= FileSystemUtil.isCacheable( uri );
     * if ( parentUri ) {
     *     FileSystem fd= FileSystem.create(parentUri);
     *     FileObject fo= fd.getFileObject( ruri.relativize(parentUri).toString() );
     *     in= fo.getInputStream();
     * }
     * }
     * </pre>
     * @param rurl
     * @return
     */
    public static URI isCacheable(URI ruri) {
        if ( ruri.getQuery()==null && ruri.getPath().length()>1 && !ruri.getPath().endsWith("/") ) {
            String s= ruri.toString();
            int i= s.lastIndexOf("/");
            String folder= s.substring(0,i);
            try {
                //TODO: actually list the parent to make sure it contains the child.
                return new URL(folder).toURI();
            } catch (URISyntaxException ex) {
                System.err.println("couldn't create URI from parent URL: " + ex);
                return null;
            } catch (MalformedURLException ex) {
                System.err.println("url caused malformed URL exception when creating parent URL: "+ex);
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * create the file folder if it does not exist.  Throw an IOException if it failed.
     * @param file
     * @throws IOException
     */
    public static void maybeMkdirs( File file ) throws IOException {
        if ( file.exists() ) return;
        if ( !file.mkdirs() ) {
            if ( file.exists() ) {
                return; // somebody else made the file.
            } else {
                System.err.println("Unable to mkdirs "+file ); // print it in case the IOException is misinterpretted.
                throw new IOException( "Unable to mkdirs "+file );
            }
        }
        return;
    }
}
