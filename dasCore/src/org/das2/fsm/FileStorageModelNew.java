/*
 * FileStorageModel.java
 *
 * Created on March 31, 2004, 9:52 AM
 */

package org.das2.fsm;

import org.das2.datum.DatumRange;
import org.das2.datum.Datum;
import org.das2.datum.TimeUtil.TimeStruct;
import org.das2.util.filesystem.FileObject;
import org.das2.util.filesystem.FileSystem;
import org.das2.dataset.CacheTag;
import org.das2.system.DasLogger;
import org.das2.util.monitor.ProgressMonitor;
import org.das2.util.monitor.NullProgressMonitor;
import org.das2.util.monitor.SubTaskMonitor;
import org.das2.datum.TimeParser;
import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URI;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.*;
import org.das2.util.filesystem.FileSystemUtil;

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
    private String regex;
    private Pattern gzpattern;

    FileStorageModelNew parent;
    FileSystem root;

    TimeParser timeParser;

    String template;

    static Logger logger= DasLogger.getLogger( DasLogger.SYSTEM_LOG );
    
    HashMap fileNameMap=null;
    private boolean allowGz= true;  // if true, the getFile can use a .gz version to retrieve a file.

    enum VersioningType {
        none(null),
        numeric( new Comparator() {       // 4.01
                public int compare(Object o1, Object o2) {
                    Double d1= Double.parseDouble((String)o1);
                    Double d2= Double.parseDouble((String)o2);
                    return d1.compareTo(d2);
                }
            } ),
        alphanumeric(new Comparator() {   // a001
                public int compare(Object o1, Object o2) {
                    return ((String)o1).compareTo((String)o2);
                }
            } ),
        numericSplit( new Comparator() {  // 4.3.23
               public int compare(Object o1, Object o2) {
                    String[] ss1= o1.toString().split("\\.",-2);
                    String[] ss2= o2.toString().split("\\.",-2);
                    int n= Math.min( ss1.length, ss2.length );
                    for ( int i=0; i<n; i++ ) {
                        double d1= Double.parseDouble(ss1[i]);
                        double d2= Double.parseDouble(ss2[i]);
                        if ( d1!=d2 ) {
                            return d1 < d2 ? -1 : 1;
                        }
                    }
                    return ss1.length - ss2.length;  // the longer version wins (3.2.1 > 3.2)
                } 
            });

            Comparator<String> comp;
            VersioningType( Comparator<String> comp ) {
                this.comp= comp;
            }
    };
    VersioningType versioningType;

    
    /* need to map back to TimeUtil's enums, note that we have an extra for the 2 digit year */
    private int toTimeUtilEnum( int i ) {
        if ( i<100 || i > 300 ) {
            throw new IllegalArgumentException( "enumeration is not of the correct type");
        }
        i= i % 100;
        if ( i==0 ) i=1;
        return i;
    }


    public FileSystem getFileSystem() {
        return this.root;
    }


    /**
     * this is introduced to support discovery, where we just need one file to
     * get started.  Before, there was code that would list all files, then use
     * just the first one.  This may return a skeleton file, but getFileFor() must
     * return a result.
     * This implementation does the same as getNames(), but stops after finding a file.
     * @return null if no file is found
     */
    public String getRepresentativeFile( ProgressMonitor monitor ) throws IOException {
        String listRegex;

        FileSystem[] fileSystems;
        String name;
        String[] names;
        if ( parent!=null ) {
            names= parent.getNamesFor(null);
            fileSystems= new FileSystem[names.length];
            for ( int i=0; i<names.length; i++ ) {
                try {
                    fileSystems[i]= root.createFileSystem( names[i] );
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
        
        String result= null;

        for ( int i=fileSystems.length-1; result==null && i>=0; i-- ) {
            String[] files1= fileSystems[i].listDirectory( "/", listRegex );
            if ( files1.length>0 ) {
                int last= files1.length-1;
                String ff= names[i].equals("") ? files1[ last ] : names[i]+"/"+files1[ last ];
                if ( ff.endsWith("/") ) ff=ff.substring(0,ff.length()-1);
                result= ff;
            }
        }

        if ( allowGz ) {
            if ( result==null) {
                for ( int i=fileSystems.length-1; result==null && i>=0; i-- ) {
                    String[] files1= fileSystems[i].listDirectory( "/", listRegex + ".gz" );
                    if ( files1.length>0 ) {
                        int last= files1.length-1;
                        String ff= names[i].equals("") ? files1[ last ] : names[i]+"/"+files1[ last ];
                        if ( ff.endsWith("/") ) ff=ff.substring(0,ff.length()-1);
                        result= ff.substring( 0,ff.length()-3 );
                    }
                }
            }
        }

        return result;
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
     *
     * @param args an empty map where extra fields (such as version) are put.
     */
    private synchronized DatumRange getDatumRangeFor( String filename, Map<String,String> extra ) {
        try {
            if ( pattern.matcher(filename).matches() ) {
                timeParser.parse( filename, extra );
                return timeParser.getTimeRange();
            } else {
                if ( gzpattern!=null && gzpattern.matcher(filename).matches() ) {
                    timeParser.parse( filename.substring(0,filename.length()-3), extra );
                    return timeParser.getTimeRange();
                } else {
                    throw new IllegalArgumentException( "file name ("+filename+") doesn't match model specification ("+regex+")");
                }
            }
        } catch ( ParseException e ) {
            IllegalArgumentException e2=new IllegalArgumentException( "file name ("+filename+") doesn't match model specification ("+regex+"), parse error in field",e);
            throw e2;
        } catch ( NumberFormatException e ) {
            IllegalArgumentException e2=new IllegalArgumentException( "file name ("+filename+") doesn't match model specification ("+regex+"), parse error in field",e);
            throw e2;
        }
    }

    /**
     * return a filename that would intersect the range.  Note this file
     * may not actually exist.  This may be used to quantize the range.
     * The template may not contain versions.
     * @param start
     * @param end
     * @return
     */
    public String getFilenameFor( Datum start, Datum end ) {
        return timeParser.format( start, end );
    }

    /**
     * return the names in the range, or all names if the range is null.
     * @param targetRange range limit, or null.
     * @param monitor
     * @return
     * @throws java.io.IOException
     */
    public String[] getNamesFor( final DatumRange targetRange ) throws IOException {
        return getNamesFor( targetRange, false, new NullProgressMonitor() );
    }

    /**
     * return the names in the range, or all names if the range is null.
     * @param targetRange range limit, or null.
     * @param monitor
     * @return
     * @throws java.io.IOException
     */
    public String[] getNamesFor( final DatumRange targetRange, ProgressMonitor monitor ) throws IOException {
        return getNamesFor( targetRange, false, monitor );
    }

    /**
     * return the names in the range, minding version numbers, or all names if the range is null.
     * @param targetRange range limit, or null.
     * @param monitor
     * @return
     * @throws java.io.IOException
     */
    public String[] getBestNamesFor( final DatumRange targetRange, ProgressMonitor monitor ) throws IOException {
        return getNamesFor( targetRange, true, monitor );
    }



    /**
     * return the names in the range, maybe with versioning.  
     * @param targetRange range limit, or null if no constraint used here.
     * @param versioning true means check versions.
     * @param monitor
     * @return
     * @throws IOException
     */
    private String[] getNamesFor( final DatumRange targetRange, boolean versioning, ProgressMonitor monitor ) throws IOException {
        String listRegex;

        FileSystem[] fileSystems;
        String[] names;

        if ( parent!=null ) {
            names= parent.getNamesFor(targetRange,versioning,new NullProgressMonitor());   // note recursive call
            fileSystems= new FileSystem[names.length];
            for ( int i=0; i<names.length; i++ ) {
                try {
                    URI url= root.getRootURI().resolve(names[i]);  //TODO: test
                    fileSystems[i]= FileSystem.create( url );
                } catch ( Exception e ) {
                    throw new RuntimeException(e);
                }
            }
            String parentRegex= getParentRegex(regex);  //vap:ftp://virbo.org/POES/n15/$Y/poes_n15_$Y$m$d.cdf.zip/poes_n15_$Y$m$d.cdf?minute&timerange=1998-07-01
            listRegex= regex.substring( parentRegex.length()+1 );
        } else {
            fileSystems= new FileSystem[] { root };
            names= new String[] {""};
            listRegex= regex;
        }

        List<String> list= new ArrayList();
        List<String> versionList= new ArrayList();
        List<DatumRange> rangeList= new ArrayList();

        monitor.setTaskSize( fileSystems.length*10 );
        monitor.started();
        
        Map<String,String> extra= new HashMap();

        for ( int i=0; i<fileSystems.length; i++ ) {
            if ( monitor.isCancelled() ) {
                throw new InterruptedIOException("cancel pressed");
            }
            monitor.setTaskProgress(i*10);
//            This is a nice trick for debugging when there is recursion and NullProgressMonitor is not passed in.
//            if ( !( monitor instanceof NullProgressMonitor ) ) {
//                System.err.println("here");
//            }
            String theListRegex= listRegex;
            if ( this.allowGz ) {
                theListRegex= theListRegex+"(.gz)?";
            }
            String[] files1= fileSystems[i].listDirectory( "/", theListRegex );
            for ( int j=0; j<files1.length; j++ ) {
                String ff= names[i].equals("") ? files1[j] : names[i]+"/"+files1[j];
                if ( ff.endsWith("/") ) ff=ff.substring(0,ff.length()-1);
                try {
                    DatumRange dr= getDatumRangeFor( ff, extra );
                    if ( targetRange==null || dr.intersects(targetRange) ) {
                        if ( ff.endsWith(".gz") && this.allowGz ) { //TODO: make sure allowgz is off when we really want gz files.
                            ff= ff.substring(0,ff.length()-3);
                        }
                        list.add(ff);
                        rangeList.add(dr);
                        if ( versioningType!=VersioningType.none ) {
                            if ( extra.get("v")==null ) throw new RuntimeException("expected version");
                            versionList.add( extra.get("v") );
                        }
                    }
                } catch ( IllegalArgumentException e ) {
                    if ( !e.getMessage().contains("invalid time before year 0001") ) {
                        System.err.println(e);
                    }
                    System.err.println("ignoring file "+ff +" because of error when parsing as "+template);
                }
                monitor.setTaskProgress( i*10 + j * 10 / files1.length );
            }
        }

        if ( versioning && versioningType!=VersioningType.none) {

            Comparator<String> comp= versioningType.comp;

            Map<String,String> bestVersions= new HashMap();
            Map<String,String> bestFiles= new HashMap();

            for ( int j=0; j<list.size(); j++ ) {
                String ff= list.get(j);
                String key= rangeList.get(j).toString();
                String thss= versionList.get(j);
                String best= bestVersions.get(key);
                if ( best==null ) {
                    try {
                        comp.compare( thss, thss ); // check for format exception
                        bestVersions.put( key, thss );
                        bestFiles.put( key,ff );
                    } catch ( Exception ex ) {
                        ex.printStackTrace();
                        // doesn't match if comparator (e.g. version isn't a decimal number)
                    }
                } else {
                    try {
                        if ( comp.compare( best, thss ) < 0 ) {
                            bestVersions.put( key,thss );
                            bestFiles.put( key,ff );
                        }
                    } catch ( Exception ex ) {
                        ex.printStackTrace();
                        // doesn't match
                    }
                }
            }
            list= Arrays.asList( bestFiles.values().toArray( new String[ bestFiles.size()] ) );
        }

        Collections.sort( list, new Comparator() {
            public int compare( Object o1, Object o2 ) {
                DatumRange dr1= getRangeFor( (String)o1 );
                DatumRange dr2= getRangeFor( (String)o2 );
                return dr1.compareTo( dr2 );
            }
        } );

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
    
    public File[] getFilesFor( final DatumRange targetRange ) throws IOException {
        return getFilesFor( targetRange, new NullProgressMonitor() );
    }

    public File[] getBestFilesFor( final DatumRange targetRange ) throws IOException {
        return getBestFilesFor( targetRange, new NullProgressMonitor() );
    }

    public DatumRange getRangeFor( String name ) {
        return getDatumRangeFor( name, new HashMap() );
    }

    /**
     * @return true if the file came (or could come) from this FileStorageModel.
     */
    public boolean containsFile( File file ) {
        if ( !fileNameMap.containsKey(file) ) {
            return false;
        } else {
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
     * check to see if "NAME.gz" exists
     * @param name name of uncompressed file
     * @param mon progress monitor
     * @return null or the uncompressed file.
     * @throws IOException
     */
    private File maybeGetGzFile( String name, ProgressMonitor mon) throws IOException {
        File f0 = null;
        FileObject oz = root.getFileObject(name + ".gz"); 
        if (oz.exists()) {
            File fz = oz.getFile(mon);
            String sfz = fz.getPath().substring(0, fz.getPath().length() - 3);
            f0 = new File(sfz);
            FileSystemUtil.unzip(fz, f0);
            if ( !f0.setLastModified(fz.lastModified()) ) {
                throw new IllegalArgumentException("failed to set last modified");
            }
        }
        return f0;
    }

    /**
     * @return a list of files that can be used
     */
    public File[] getFilesFor( final DatumRange targetRange, ProgressMonitor monitor ) throws IOException {
        String[] names= getNamesFor( targetRange );
        File[] files= new File[names.length];

        if ( fileNameMap==null ) fileNameMap= new HashMap();

        if ( names.length>0 ) monitor.setTaskSize( names.length * 10 );
        monitor.started();
        for ( int i=0; i<names.length; i++ ) {
            if ( monitor.isCancelled() ) {
                throw new InterruptedIOException("cancel pressed");
            }
            try {
                FileObject o= root.getFileObject( names[i] );
                if ( o.exists() ) {
                    files[i]= o.getFile( SubTaskMonitor.create( monitor, i*10, (i+1)*10 ));
                } else if ( allowGz ) {
                    File f0 = maybeGetGzFile( names[i], SubTaskMonitor.create(monitor, i * 10, (i + 1) * 10) );
                    files[i]= f0;
                }
                fileNameMap.put( files[i], names[i] );
            } catch ( Exception e ) {
                throw new RuntimeException(e);
            }
        }
        monitor.finished();
        return files;
    }

    /**
     * Get the files for the range, using versioning info ($v,etc).
     * @param targetRange
     * @param monitor
     * @return
     * @throws IOException
     */
    public File[] getBestFilesFor( final DatumRange targetRange, ProgressMonitor monitor ) throws IOException {
        String[] names= getNamesFor( targetRange, true, monitor );
        File[] files= new File[names.length];

        if ( fileNameMap==null ) fileNameMap= new HashMap();

        if ( names.length>0 ) monitor.setTaskSize( names.length * 10 );
        monitor.started();
        for ( int i=0; i<names.length; i++ ) {
            if ( monitor.isCancelled() ) {
                throw new InterruptedIOException("cancel pressed");
            }
            try {
                FileObject o= root.getFileObject( names[i] );
                if ( o.exists() ) {
                    files[i]= o.getFile( SubTaskMonitor.create( monitor, i*10, (i+1)*10 ));
                } else if ( allowGz ) {
                    File f0 = maybeGetGzFile( names[i], SubTaskMonitor.create(monitor, i * 10, (i + 1) * 10) );
                    files[i]= f0;                  
                }
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
     * Split off the end of the regex to get a regex for use in the parent system.  Returns null
     * if there is no parent portion of the regex.
     */
    private static String getParentRegex( String regex ) {
        String[] s= regex.split( "/" );
        StringBuilder dirRegex;
        if ( s.length>1 ) {
            dirRegex= new StringBuilder( s[0] );
            for ( int i=1; i<s.length-1; i++ ) {
                dirRegex.append( "/" ) .append( s[i] );
            }
        } else {
            dirRegex= null;
        }
        return dirRegex==null ? null : dirRegex.toString();
    }

    /**
     * returns the parent or null if none exists.  None will exist when there
     * are no more wildcards. (/home/foo/$Y/$m-$d.dat has parent /home/foo/$Y
     * which has parent null.)
     * 
     * @return
     */
    public FileStorageModelNew getParent() {
        return this.parent;
    }


    /**
     * Autoplot introduced the dollar sign instead of the percent, because $ is
     * more URI-friendly.  Switch to this if it looks appropriate.
     * @param template
     * @return
     */
    protected static String makeCanonical( String template ) {
        String result;
        if ( template.contains("$Y") || template.contains("$y" ) ) {
            result= template.replaceAll("\\$", "%");
        } else {
            result= template;
        }
        int i=result.indexOf("/");
        if ( i>-1 && result.indexOf("%")>i ) {
            System.err.println("static folder in template not allowed: "+ result.substring(0,i) );
        }
        return result;
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
     *    %v  best version by number  Also %(v,sep) for 4.3.2  or %(v,alpha)
     *    %{milli}  3-digit milliseconds
     *
     * @param root FileSystem source of the files.
     * @param template describes how filenames are constructed.  This is converted to a regular expression and may contain regex elements without groups.  The
     *   string may contain $ instead of percents as long as there are no percents in the string.
     *
     * @return a newly-created FileStorageModelNew.
     */
    public static FileStorageModelNew create( FileSystem root, String template ) {
        template= makeCanonical(template);
        int i= template.lastIndexOf("/");

        if ( template.contains("$") && !template.contains("%") ) {
            template= template.replaceAll("\\$", "%");
        }
        
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
     * @param template describes how filenames are constructed.  This is converted to a regular expression and may contain regex elements without groups.  The
     *   string may contain $ instead of percents as long as there are no percents in the string.
     * @param fieldName custom field name
     * @param fieldHandler TimeParser.FieldHandler to call with the field contents.
     * @return a newly-created FileStorageModelNew.
     */
    public static FileStorageModelNew create( FileSystem root, String template, String fieldName, TimeParser.FieldHandler fieldHandler ) {
        template= makeCanonical(template);
        int i= template.lastIndexOf("/");

        if ( template.contains("$") && !template.contains("%") ) {
            template= template.replaceAll("\\$", "%");
        }
        
        int i2= template.lastIndexOf("%",i);
        if ( i2 != -1 ) {
            String parentTemplate= template.substring(0,i);
            FileStorageModelNew parentFSM= FileStorageModelNew.create( root, parentTemplate, fieldName, fieldHandler );
            return new FileStorageModelNew( parentFSM, root, template, fieldName, fieldHandler );
        } else {
            return new FileStorageModelNew( null, root, template, fieldName, fieldHandler );
        }
    }

    private FileStorageModelNew( FileStorageModelNew parent, FileSystem root, String template, String fieldName, TimeParser.FieldHandler fieldHandler, Object ... moreHandler   ) {
        this.root= root;
        this.parent= parent;
        this.template= template.replaceAll("\\+", "\\\\+");

        String f="v";
        versioningType= VersioningType.none;
        TimeParser.FieldHandler vh= new TimeParser.FieldHandler() {
            public String configure( Map<String,String> args ) {
                String sep= args.get( "sep" );
                String alpha= args.get( "alpha" );
                if ( alpha!=null ) {
                    if ( sep!=null ) {
                        return "alpha with split not supported";
                    } else {
                        versioningType= VersioningType.alphanumeric;
                    }
                } else {
                    if ( sep!=null ) {
                        versioningType= VersioningType.numericSplit;
                    } else {
                        versioningType= VersioningType.numeric;
                    }
                }
                return null;
            }
            public void handleValue( String fieldContent, TimeStruct startTime, TimeStruct timeWidth, Map<String,String> extra ) {
                extra.put( "v", fieldContent );
            }
        };

        if ( fieldName==null ) {
            this.timeParser= TimeParser.create( template, f, vh );
        } else {
            if ( moreHandler==null || moreHandler.length==0 ) { //TODO: check if it can ever be null.
                this.timeParser= TimeParser.create( template, fieldName, fieldHandler, f, vh );
            } else {
                this.timeParser= TimeParser.create( template, fieldName, fieldHandler, f, vh, moreHandler );
            }
        }

        if ( this.timeParser.isStartTimeOnly() ) {
            this.startTimeOnly= true;
        }

        this.regex= timeParser.getRegex();
        this.pattern= Pattern.compile(regex);
        if ( template.endsWith(".gz") ) {
            allowGz= false; // turn off automatic uncompressing GZ files.
        }
        if ( allowGz ) {
            this.gzpattern= Pattern.compile(regex+"\\.gz");
        }
    }

    /**
     * The filename time only contains the start time of the interval, the end of the interval
     * is only bounded by the next file.
     */
    protected boolean startTimeOnly = false;

    /**
     * limit on the length of files with startTimeOnly set.
     * e.g. $Y$m$d_$(H,startTimeOnly)$M means that the files should be much less that one hour long
     *
     */
    protected Datum implicitTimeDelta= null;

    private FileStorageModelNew( FileStorageModelNew parent, FileSystem root, String template ) {
        this( parent, root, template, null, null );
    }
 
    @Override
    public String toString() {
        return String.valueOf(root) + regex;
    }


}
