/*
 * FileStorageModel.java
 *
 * Created on March 31, 2004, 9:52 AM
 */

package org.das2.util.filesystem;

import edu.uiowa.physics.pw.das.dataset.CacheTag;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.system.DasLogger;
import org.das2.util.monitor.ProgressMonitor;
import org.das2.util.monitor.NullProgressMonitor;
import org.das2.util.monitor.SubTaskMonitor;
import edu.uiowa.physics.pw.das.util.TimeParser;
import java.io.File;
import java.net.URL;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.*;

/**
 * Represents a method for storing data sets in a set of files by time.  The
 * client provides a regex for the files and how each group of the regex is
 * interpreted as a time digit.  The model can then be used to provide the set
 * of files that cover a time range, etc.
 *
 * This new implementation uses a TimeParser object to more quickly process 
 * file names.
 *
 * @author  Jeremy
 */
public class FileStorageModelNew {

    private Pattern pattern;
    private Pattern absPattern;
    private String regex;

    FileStorageModelNew parent;
    FileSystem root;

    TimeParser timeParser;

    String template;

    static Logger logger= DasLogger.getLogger( DasLogger.SYSTEM_LOG );
    
    HashMap fileNameMap=null;

    
    /* need to map back to TimeUtil's enums, note that we have an extra for the 2 digit year */
    private int toTimeUtilEnum( int i ) {
        if ( i<100 || i > 300 ) {
            throw new IllegalArgumentException( "enumeration is not of the correct type");
        }
        i= i % 100;
        if ( i==0 ) i=1;
        return i;
    }


    /**
     * extract time range for file or directory from its name.
     * The least significant time digit is considered to be the implicitTimeWidth,
     * and if the width is not stated explicitly, it will be used.  When
     * a set timeDigits are encountered twice, then the second occurrence
     * is considered be the end time.
     *
     * .../FULL1/T8709_12/T871118.DAT
     *'.../FULL1/T'YYMM_MM/TYYMMDD'.DAT'
     */
    private DatumRange getDatumRangeFor( String filename ) {
        try {
            if ( pattern.matcher(filename).matches() ) {
                timeParser.parse( filename );
                return timeParser.getTimeRange();
            } else {
                throw new IllegalArgumentException( "file name ("+filename+") doesn't match model specification ("+regex+")");
            }
        } catch ( ParseException e ) {
            IllegalArgumentException e2=new IllegalArgumentException( "file name ("+filename+") doesn't match model specification ("+regex+"), parse error in field",e);
            throw e2;
        } catch ( NumberFormatException e ) {
            IllegalArgumentException e2=new IllegalArgumentException( "file name ("+filename+") doesn't match model specification ("+regex+"), parse error in field",e);
            throw e2;
        }
    }

    public String getFilenameFor( Datum start, Datum end ) {
        return timeParser.format( start, end );
    }

    public String[] getNamesFor( final DatumRange targetRange ) {
        return getNamesFor( targetRange, new NullProgressMonitor() );
    }

    public String[] getNamesFor( final DatumRange targetRange, ProgressMonitor monitor ) {

        String listRegex;

        FileSystem[] fileSystems;
        String[] names;

        if ( parent!=null ) {
            names= parent.getNamesFor(targetRange);
            fileSystems= new FileSystem[names.length];
            for ( int i=0; i<names.length; i++ ) {
                try {
                    URL url= new URL( root.getRootURL(), names[i] );
                    fileSystems[i]= FileSystem.create( url );
                } catch ( Exception e ) {
                    throw new RuntimeException(e);
                }
            }
            String parentRegex= getParentRegex(regex);
            listRegex= regex.substring( parentRegex.length()+1 );
        } else {
            fileSystems= new FileSystem[] { root };
            names= new String[] {""};
            listRegex= regex;
        }

        List list= new ArrayList();

        monitor.setTaskSize( fileSystems.length*10 );
        monitor.started();
        
        for ( int i=0; i<fileSystems.length; i++ ) {
            monitor.setTaskProgress(i*10);
            String[] files1= fileSystems[i].listDirectory( "/", listRegex );
            for ( int j=0; j<files1.length; j++ ) {
                String ff= names[i].equals("") ? files1[j] : names[i]+"/"+files1[j];
                if ( ff.endsWith("/") ) ff=ff.substring(0,ff.length()-1);
                try { 
                    if ( getDatumRangeFor( ff ).intersects(targetRange) ) list.add(ff);
                } catch ( IllegalArgumentException e ) {
                    logger.fine("ignoring file "+ff);
                }
                monitor.setTaskProgress( i*10 + j * 10 / files1.length );
            }
        }
        
        monitor.finished();
        return (String[])list.toArray(new String[list.size()]);
    }

    public static CacheTag getCacheTagFor( FileStorageModelNew fsm, DatumRange range, String[] names ) {
        Datum min= range.min();
        Datum max= range.max();
        for ( int i=0; i<names.length; i++ ) {
            DatumRange r= fsm.getRangeFor( names[i] );
            min= min.gt(range.min()) ? r.min() : min;
            max= max.lt(range.max()) ? r.max() : max;
        }
        return new CacheTag( min, max, null );
    }
    
    public static CacheTag getCacheTagFor( FileStorageModelNew fsm, DatumRange range, File[] files ) {
        String[] names= new String[files.length];
        for ( int i=0; i<files.length; i++ ) {
            names[i]= fsm.getNameFor(files[i]);
        }
        return getCacheTagFor( fsm, range, names );
    }
    
    public File[] getFilesFor( final DatumRange targetRange ) {
        return getFilesFor( targetRange, new NullProgressMonitor() );
    }

    public DatumRange getRangeFor( String name ) {
        return getDatumRangeFor( name );
    }

    /**
     * @return true if the file came (or could come) from this FileStorageModel.
     */
    public boolean containsFile( File file ) {
        if ( !fileNameMap.containsKey(file) ) {
            return false;
        } else {
            String result= (String)fileNameMap.get(file);
            String name= getNameFor( file );
            Matcher m= pattern.matcher( name );
            return m.matches();
        }
    }

    /**
     * Provides a way to recover the model name of a file.  The returned File from getFilesFor can be anywhere,
     * so it would be good to provide a way to get it back into a FSM name.  For example, a filesystem
     * might download the remote file to a cache directory, which is the File that is provided to the
     * client, sometimes the client will need to recover the name of the corresponding FileObject, so
     * this maps the File back to the name.
     *
     * @return the canonical name of the file.
     */
    public String getNameFor( File file ) {
        String result= (String)fileNameMap.get(file);
        if ( result==null ) {
            throw new IllegalArgumentException( "File didn't come from this FileStorageModel" );
        } else {
            return result;
        }
    }

    /**
     * @return a list of files that can be used
     */
    public File[] getFilesFor( final DatumRange targetRange, ProgressMonitor monitor ) {
        String[] names= getNamesFor( targetRange );
        File[] files= new File[names.length];

        if ( fileNameMap==null ) fileNameMap= new HashMap();

        if ( names.length>0 ) monitor.setTaskSize( names.length * 10 );
        monitor.started();
        for ( int i=0; i<names.length; i++ ) {
            try {
                FileObject o= root.getFileObject( names[i] );
                files[i]= o.getFile( SubTaskMonitor.create( monitor, i*10, (i+1)*10 ));
                fileNameMap.put( files[i], names[i] );
            } catch ( Exception e ) {
                throw new RuntimeException(e);
            }
        }
        monitor.finished();
        return files;
    }


    private static int countGroups( String regex ) {
        int result=0;
        Pattern p= Pattern.compile( regex );
        Matcher m= p.matcher("");
        return m.groupCount();
    }

    /**
     * Split off the end of the regex to get a regex for use in the parent system.
     * Note: changed from public to private as no one is using this internal routine.
     */
    private static String getParentRegex( String regex ) {
        String[] s= regex.split( "/" );
        String dirRegex;
        if ( s.length>1 ) {
            dirRegex= s[0];
            for ( int i=1; i<s.length-2; i++ ) {
                dirRegex+= "/"+s[i];
            }
        } else {
            dirRegex= null;
        }
        String fileRegex= s[s.length-1];
        return dirRegex;
    }

    /**
     * creates a FileStorageModel for the given template, which uses:
     *    %Y-%m-%dT%H:%M:%S.%{milli}Z";
     *    %Y  4-digit year
     *    %m  2-digit month
     *    %d  2-digit day of month
     *    %j  3-digit day of year
     *    %H  2-digit Hour
     *    %M  2-digit Minute
     *    %S  2-digit second
     *    %{milli}  3-digit milliseconds
     *
     * @param root FileSystem source of the files.
     * @param template describes how filenames are constructed.
     * @return a newly-created FileStorageModelNew.
     */
    public static FileStorageModelNew create( FileSystem root, String template ) {
        int i= template.lastIndexOf("/");
        int i2= template.lastIndexOf("%",i);
        if ( i2 != -1 ) {
            String parentTemplate= template.substring(0,i);
            FileStorageModelNew parentFSM= FileStorageModelNew.create( root, parentTemplate );
            return new FileStorageModelNew( parentFSM, root, template );
        } else {
            return new FileStorageModelNew( null, root, template );
        }
    }

    
    /**
     * creates a FileStorageModel for the given template, but with a custom FieldHandler and
     * field.
     *
     * @param root FileSystem source of the files.
     * @param template describes how filenames are constructed.
     * @param fieldName custom field name
     * @param fieldHandler TimeParser.FieldHandler to call with the field contents.
     * @return a newly-created FileStorageModelNew.
     */
    public static FileStorageModelNew create( FileSystem root, String template, String fieldName, TimeParser.FieldHandler fieldHandler ) {
        int i= template.lastIndexOf("/");
        int i2= template.lastIndexOf("%",i);
        if ( i2 != -1 ) {
            String parentTemplate= template.substring(0,i);
            FileStorageModelNew parentFSM= FileStorageModelNew.create( root, parentTemplate );
            return new FileStorageModelNew( parentFSM, root, template, fieldName, fieldHandler );
        } else {
            return new FileStorageModelNew( null, root, template, fieldName, fieldHandler );
        }
    }
    
    private FileStorageModelNew( FileStorageModelNew parent, FileSystem root, String template, String fieldName, TimeParser.FieldHandler fieldHandler  ) {
        this.root= root;
        this.parent= parent;
        this.template= template;
        this.timeParser= TimeParser.create( template, fieldName, fieldHandler );
        this.regex= timeParser.getRegex();
        this.pattern= Pattern.compile(regex);
    }

    private FileStorageModelNew( FileStorageModelNew parent, FileSystem root, String template ) {
        this.root= root;
        this.parent= parent;
        this.template= template;
        this.timeParser= TimeParser.create( template );
        this.regex= timeParser.getRegex();
        this.pattern= Pattern.compile(regex);
    }
 
    @Override
    public String toString() {
        return String.valueOf(root) + regex;
    }

}
