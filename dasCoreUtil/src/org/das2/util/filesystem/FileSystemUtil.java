/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.util.filesystem;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.das2.util.LoggerManager;
import org.das2.util.monitor.NullProgressMonitor;
import org.das2.util.monitor.ProgressMonitor;

/**
 *
 * @author jbf
 */
public class FileSystemUtil {

    private final static Logger logger= LoggerManager.getLogger( "das2.filesystem" );
    /**
     * Dump the contents of the InputStream into a file.  If the inputStream comes
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
            } else {
                ByteBuffer buf= ByteBuffer.allocateDirect( 16*1024 );
                while ( ic.read(buf) >= 0 || buf.position() != 0 ) {
                    buf.flip();
                    oc.write(buf);
                    buf.compact();
                }
            }
        } finally {
            closeResources( oc, ic );
        }
    }

    /**
     * encapsulate the logic that cleanly closes both channels.  
     * @param chout channel that needs closing
     * @param chin channel that needs closing.
     * @throws java.io.IOException
     */
    private static void closeResources( Channel chout, Channel chin ) throws IOException {
        if ( chout!=null && chout.isOpen() ) {
            try { 
                chout.close(); 
            } finally {
            }
        }        
        if ( chin!=null && chin.isOpen() ) {
            try { 
                chin.close(); 
            } finally {
            }
        } 
    }
    
    /**
     * un-gzip the file.  This is similar to the unix gunzip command.
     * To unzip a file, see fileUnzip
     * @param fz zipped input file
     * @param file unzipped destination file
     * @throws java.io.IOException
     * @deprecated use gunzip instead.
     */
    public static void unzip( File fz, File file) throws IOException {
        gunzip( fz, file );
    }
    
    /**
     * un-gzip the file.  This is similar to the unix gunzip command.
     * @param fz zipped input file
     * @param file unzipped destination file
     * @throws java.io.IOException
     */
    public static void gunzip( File fz, File file) throws IOException {
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
     * create a zip file of everything with and under the directory.
     * @param fz the output 
     * @param dir the root directory.
     * @throws java.io.IOException 
     */
    public static void zip( File fz, File dir ) throws IOException {
        if ( !dir.isDirectory() ) throw new IllegalArgumentException("dir should be a directory");
        //if ( fz.exists() ) throw new IllegalArgumentException("output file "+fz+ " exists");
        new ZipFiles().zipDirectory( dir, fz.getAbsolutePath() );
    }
    
    /**
     * Size of the buffer to read/write data
     */
    private static final int BUFFER_SIZE = 4096;
    
    /**
     * Extracts a zip file specified by the zipFilePath to a directory specified by
     * destDirectory (will be created if does not exists).
     * From http://www.codejava.net/java-se/file-io/programmatically-extract-a-zip-file-using-java
     * @param zipFilePath
     * @param destDir
     * @throws IOException
     */
    public static void unzipFile( File zipFilePath, File destDir) throws IOException {
        
        if (!destDir.exists()) {
            destDir.mkdir();
        }
        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry = zipIn.getNextEntry();
            // iterates over entries in the zip file
            while (entry != null) {
                String filePath = destDir.getAbsolutePath() + File.separator + entry.getName();
                if (!entry.isDirectory()) {
                    // if the entry is a file, extracts it
                    extractFile(zipIn, filePath);
                } else {
                    // if the entry is a directory, make the directory
                    File dir = new File(filePath);
                    dir.mkdir();
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        }
    }
    
    /**
     * Extracts a zip entry (file entry)
     * @param zipIn
     * @param filePath
     * @throws IOException
     */
    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath))) {
            byte[] bytesIn = new byte[BUFFER_SIZE];
            int read;
            while ((read = zipIn.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
        }
    }        

    /**
     * delete all files under the directory, with names matching the regex.
     * @param dir the directory
     * @param regex the regular expression, like ".*\.png"
     * @see Glob#getRegex(java.lang.String) 
     */
    public static void deleteAllFiles( File dir, String regex ) {
        File[] ff= dir.listFiles();
        for ( File f : ff ) {
            if ( f.isDirectory() ) {
                deleteAllFiles(f,regex);
            } else if ( f.isFile() ) {
                if ( f.getName().matches( regex ) ) {
                    if ( !f.delete() ) {
                        logger.log(Level.WARNING, "failed to delete: {0}", f);
                    }
                }
            }
        }
    }
    
    /**
     * copies data from in to out, sending the number of bytesTransferred to the monitor.
     * The input and output are not closed.
     * @param is the input stream, which will not be closed.
     * @param out the output stream, which will not be closed.
     * @param monitor a monitor, or null.
     * @throws java.io.IOException
     */
    public static void copyStream( InputStream is, OutputStream out, ProgressMonitor monitor ) throws IOException {
        if ( monitor==null ) monitor= new NullProgressMonitor();
        byte[] buffer = new byte[2048];
        int bytesRead = is.read(buffer, 0, 2048);
        long totalBytesRead = bytesRead;
        while (bytesRead > -1) {
            if (monitor.isCancelled()) {
                throw new InterruptedIOException();
            }
            monitor.setTaskProgress(totalBytesRead);
            out.write(buffer, 0, bytesRead);
            bytesRead = is.read(buffer, 0, 2048);
            totalBytesRead += bytesRead;
            logger.finest("transferring data");
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
     * @param ruri
     * @return the URI of the parent, or null.
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
                logger.log( Level.SEVERE, "couldn't create URI from parent URL", ex);
                return null;
            } catch (MalformedURLException ex) {
                logger.log( Level.SEVERE, "url caused malformed URL exception when creating parent URL: ", ex);
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * return the parent of the URI, or null if this is not possible.
     * @param ruri
     * @return
     */
    public static URI getParentUri( URI ruri ) {
         if ( ruri.getQuery()==null && ruri.getPath().length()>1 ) {
            String s= ruri.toString();
            int i= s.length();
            if ( s.charAt(i-1)=='/') {
                i=i-1;
            }
            i= s.lastIndexOf("/",i-1);
            String folder= s.substring(0,i);
            try {
                return new URL(folder).toURI();
            } catch (URISyntaxException ex) {
                logger.log( Level.SEVERE, "couldn't create URI from parent URL", ex);
                return null;
            } catch (MalformedURLException ex) {
                logger.log( Level.SEVERE, "url caused malformed URL exception when creating parent URL: ", ex);
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
                // somebody else made the file.
            } else {
                logger.log( Level.SEVERE, "Unable to mkdirs {0}", file); // print it in case the IOException is misinterpretted.
                throw new IOException( "Unable to mkdirs "+file );
            }
        }
    }
    
    /**
     * convert " " to "%20", etc, by looking for and encoding illegal characters.
     * We can't just aggressively convert...
     * @param surl 
     * @return
     */
    private static String uriEncode(String surl) {

        //surl = surl.replaceAll("#", "%23" );
        surl = surl.replaceAll("%", "%25" ); // see above
        surl = surl.replaceAll(" ", "%20" );
        //surl = surl.replaceAll("&", "%26" );
        //surl = surl.replaceAll("\\+", "%2B" );
        //surl = surl.replaceAll("/", "%2F" );
        //surl = surl.replaceAll(":", "%3A" );
        //surl = surl.replaceAll(";", "%3B" );
        surl = surl.replaceAll("<", "%3C");
        surl = surl.replaceAll(">", "%3E");
        //surl = surl.replaceAll("\\?", "%3F" );
        surl = surl.replaceAll("\\[", "%5B"); // Windows appends these in temporary downloadf rte_1495358356
        surl = surl.replaceAll("\\]", "%5D");

        return surl;
    }

    /**
     * convert "%20" to " ", etc, by using URLDecoder and catching the UnsupportedEncodingException that will never occur.
     * @param s
     * @return
     */
    private static String uriDecode(String s) {
        String surl= s;
//        if ( surl.contains("+") && !surl.contains("%20") ) { // legacy
//            surl = surl.replaceAll("+", " " );
//        }
        surl = surl.replaceAll("%20", " " );
        //surl = surl.replaceAll("%23", "#" );
        //surl = surl.replaceAll("%26", "&" );
        //surl = surl.replaceAll("%2B", "+" );
        //surl = surl.replaceAll("%2F", "/" );
        //surl = surl.replaceAll("%3A", ":" );
        //surl = surl.replaceAll("%3B", ";" );
        surl = surl.replaceAll("%3C", "<" );
        surl = surl.replaceAll("%3E", ">" );
        //surl = surl.replaceAll("%3F", "?" );
        surl = surl.replaceAll("%5B", "\\[" ); // Windows appends these in temporary downloadf rte_1495358356
        surl = surl.replaceAll("%5D", "\\]" );
        surl = surl.replaceAll("%25", "%" );

        return surl;
    }    
    
    /**
     * canonical method for converting URI to human-readable string, containing
     * spaces and other illegal characters.  Note pluses in the query part
     * are interpreted as spaces.
     * This was borrowed from Autoplot's URISplit code.
     * @param uri URI like URI("file:/home/jbf/ct/autoplot/bugs/1830227625/%5BajbTest%5D/")
     * @return string representation of a path like file:/home/jbf/ct/autoplot/bugs/1830227625/[ajbTest]/
     */
    public static String fromUri( URI uri ) {
        String surl= uri.toString();
        int i= surl.indexOf("?");
        String query= i==-1 ? "" : surl.substring(i);
        if ( i!=-1 ) {
            return uriDecode(surl.substring(0,i)) + query;
        } else {
            return uriDecode(surl);
        }
        
    }
    
    /**
     * encode the string as a URI.  The following characters are encoded:
     * " " % &lt; &gt; [ ]
     * @param s string representation of a path like file:/home/jbf/ct/autoplot/bugs/1830227625/[ajbTest]/
     * @return URI like URI("file:/home/jbf/ct/autoplot/bugs/1830227625/%5BajbTest%5D/")
     */
    public static URI toUri( String s ) {
        try {
            return new URI( uriEncode(s) );
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * create a temporary file, based on the name of the localFile.  This will
     * be a different name, and may exist already.  The problem with using 
     * deleteOnExit, is that Autoplot may be running within a webserver that
     * doesn't exit.  Java7 nio2 introduces more methods for cleaning up temporary
     * files, but even delete-on-close doesn't work because often a file is opened
     * and closed several times.
     * @param localFile which need not exist.
     * @param timeoutSeconds the minimum number of seconds this file will exist.
     * @return a new file that is a different but predictable name.
     */
    public static File createTempFile( File localFile, int timeoutSeconds ) {
        File f= FileSystem.settings().getLocalCacheDir();
        f= new File( f, "temp" );
        f= new File( f, localFile.toString() ); // TODO: check Windows.
        return f;
    }
    
}
