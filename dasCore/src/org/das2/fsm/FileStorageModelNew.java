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
import org.das2.datum.CacheTag;
import org.das2.util.monitor.ProgressMonitor;
import org.das2.util.monitor.NullProgressMonitor;
import org.das2.util.monitor.SubTaskMonitor;
import org.das2.datum.TimeParser;
import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URI;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.*;
import org.das2.util.LoggerManager;
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

    static final Logger logger= LoggerManager.getLogger("das2.system.fsm");
    
    HashMap fileNameMap=null;
    private boolean allowGz= true;  // if true, the getFile can use a .gz version to retrieve a file.

    /**
     * Versioning types 
     */
    static enum VersioningType {
        none(null),
        /**
         * simple floating point numeric comparisons.
         */
        numeric( new Comparator() {       // 4.10 > 4.01
                public int compare(Object o1, Object o2) {
                    Double d1= Double.parseDouble((String)o1);
                    Double d2= Double.parseDouble((String)o2);
                    return d1.compareTo(d2);
                }
            } ),
        /**
         * comparison by lexical sort v2013a>v2012b.
         */
        alphanumeric(new Comparator() {   // a001
                public int compare(Object o1, Object o2) {
                    return ((String)o1).compareTo((String)o2);
                }
            } ),
        /**
         * comparison of numbers split by decimal points and dashes, so 1.20 > 1.3.
         */
        numericSplit( new Comparator() {  // 4.3.23   // 1.1.3-01 for RBSP (rbspice lev-2 isrhelt)
               public int compare(Object o1, Object o2) {
                    String[] ss1= o1.toString().split("[\\.-]",-2);
                    String[] ss2= o2.toString().split("[\\.-]",-2);
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
    String versionGe= null; // the version must be greater than or equal to this if non-null. 
    String versionLt= null; // the version must be less than this if non-null. 


    public FileSystem getFileSystem() {
        return this.root;
    }

    public String getRepresentativeFile( ProgressMonitor monitor ) throws IOException {
        return getRepresentativeFile( monitor, null );
    }
    
    public String getRepresentativeFile( ProgressMonitor monitor, String childRegex ) throws IOException {
        return getRepresentativeFile( monitor, childRegex, null );
    }
    /**
     * this is introduced to support discovery, where we just need one file to
     * get started.  Before, there was code that would list all files, then use
     * just the first one.  This may return a skeleton file, but getFileFor() must
     * return a result.
     * This implementation does the same as getNames(), but stops after finding a file.
     * @param childRegex the parent must contain a file/folder matching childRegex
     * @return null if no file is found
     */
    public String getRepresentativeFile( ProgressMonitor monitor, String childRegex, DatumRange range ) throws IOException {
        String listRegex;

        FileSystem[] fileSystems;
        String[] names;
        String parentRegex=null;
        DatumRange range1;  // the range from the parent we are looking for.  This is to limit searches...
        
        if ( parent!=null ) {
            parentRegex= getParentRegex(regex);
            String one= parent.getRepresentativeFile( monitor,regex.substring(parentRegex.length()+1), range );
            if ( one==null ) return null;
            names= new String[] { one }; //parent.getNamesFor(null);
            fileSystems= new FileSystem[names.length];
            for ( int i=0; i<names.length; i++ ) {
                try {
                    fileSystems[i]= FileSystem.create( root.getRootURI().resolve(names[i]), monitor ); // 3523492: allow the FS type to change; eg to zip.
                    //fileSystems[i]= root.createFileSystem( names[i] );
                } catch ( Exception e ) {
                    throw new RuntimeException(e);
                }
            }
            listRegex= regex.substring( parentRegex.length()+1 );
        } else {
            fileSystems= new FileSystem[] { root };
            names= new String[] {""};
            listRegex= regex;
        }
        
        String result= null;

        while ( result==null ) {
            for ( int i=fileSystems.length-1; result==null && i>=0; i-- ) {
                String[] files1= fileSystems[i].listDirectory( "/", listRegex );
                int j= files1.length-1;
                while ( j>=0 && result==null ) {
                    String ff= names[i].equals("") ? files1[ j ] : names[i]+"/"+files1[ j ];
                    if ( ff.endsWith("/") ) ff=ff.substring(0,ff.length()-1);
                    //try {
                        HashMap<String,String> extra= new HashMap();
                        DatumRange tr= getDatumRangeFor( ff, extra );
                        boolean versionOk= true;
                        if ( versionGe!=null && versioningType.comp.compare( extra.get("v"), versionGe )<0 ) versionOk=false;
                        if ( versionLt!=null && versioningType.comp.compare( extra.get("v"), versionLt )>=0 ) versionOk=false;
                        if ( versionOk && timeParser.getValidRange().contains( tr ) && ( range==null || range.intersects(tr) ) ) {
                            if ( childRegex!=null ) {
                                String[] kids= fileSystems[i].listDirectory( files1[ j ],childRegex);
                                if ( kids.length>0 ) {
                                    result= ff;
                                }
                            } else {
                                result= ff;
                            }
                        }
                    //} catch ( ParseException ex ) {
                    // 
                    //}
                    if ( result==null ) j--;
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
            
            if ( result==null ) {
                if ( parent==null  ) {
                    return null;
                } else { // fall back to old code that would list everything.
                    range1= parent.getRangeFor(names[0]);
                    range1= range1.previous();
                    if ( range!=null && !range.intersects(range1) ) {
                        return null;
                    }
                    String one= parent.getRepresentativeFile( monitor,regex.substring(parentRegex.length()+1), range1 );
                    if ( one==null ) return null;
                    names= new String[] { one }; //parent.getNamesFor(null);
                    fileSystems= new FileSystem[names.length];
                    for ( int i=0; i<names.length; i++ ) {
                        try {
                            fileSystems[i]= FileSystem.create( root.getRootURI().resolve(names[i]), monitor ); // 3523492: allow the FS type to change; eg to zip.
                        } catch ( Exception e ) {
                            throw new RuntimeException(e);
                        }
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
     *'.../FULL1/T'YYMM_MM/TYYMMDD'.DAT'  TODO: verify this.
     * .../FULL1/T$y$m_$m/T$y$m$d.dat
     *
     * @param filename the filename, within the filesystem.
     * @param extra an empty map where extra fields (such as version) are put.
     * @throws IllegalArgumentException when a filename does not match the model specification.
     */
    private synchronized DatumRange getDatumRangeFor( String filename, Map<String,String> extra ) {
        try {
            extra.clear();
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
        logger.log( Level.FINE, "getNamesFor {0}", this.root);
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
            logger.log( Level.FINER, "listDirectory({0})->{1}", new Object[]{theListRegex, files1.length});
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
                        logger.log( Level.FINER, "  add {0}", ff);
                    }
                } catch ( IllegalArgumentException e ) {
                    //logger.log( Level.WARNING, "", e ); // this just means file doesn't match template.
                    //System.err.println("ignoring file "+ff +" because of error when parsing as "+template);
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
                        if ( ( versionGe==null || comp.compare( thss, versionGe )>=0 )
                                && ( versionLt==null || comp.compare( thss, versionLt )<0 ) ){
                            bestVersions.put( key, thss );
                            bestFiles.put( key,ff );
                        }
                    } catch ( Exception ex ) {
                        logger.log( Level.WARNING, "", ex );
                        // doesn't match if comparator (e.g. version isn't a decimal number)
                    }
                } else {
                    try {
                        if ( ( ( versionGe==null || comp.compare( thss, versionGe )>=0 ) )
                                && ( versionLt==null || comp.compare( thss, versionLt )<0 )
                                && comp.compare( thss, best ) > 0 ) {
                            bestVersions.put( key,thss );
                            bestFiles.put( key,ff );
                        }
                    } catch ( Exception ex ) {
                        logger.log( Level.WARNING, "", ex );
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

    /**
     * return the time range represented by this name.
     * @param name like 2013-10-31
     * @return the timerange representing the day 2013-10-31
     * @throws IllegalArgumentException if the name is not part of the FileStorageModel.
     */
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
     * hide the contents of parameters as in
     *   product_%(o,id=ftp://stevens.lanl.gov/pub/projects/rbsp/autoplot/orbits/rbspa_pp).png -->
     *   product_%(______________________________________________________________________).png
     * @param template
     * @return
     */
    private static String hideParams( String template ) {
        StringBuilder result= new StringBuilder();
        boolean withinArg= false;
        for ( int i=0; i<template.length(); i++ ) {
            if ( withinArg ) {
                if ( template.charAt(i)==')' ) withinArg= false;
                if ( withinArg ) result.append("_"); else result.append(template.charAt(i) );
            } else {
                if ( template.charAt(i)=='(' ) withinArg= true;
                result.append(template.charAt(i) );
            }

        }
        return result.toString();
    }

    /**
     * This returns the index splitting the static part of the filename from the templated part.
     * For example, http://autoplot.org/data/C1_CP_EDI_EGD__$Y$m$d_V$v.cef is split into:<br>
     * <pre>http://autoplot.org/data/
     *C1_CP_EDI_EGD__$Y$m$d_V$v.cef</pre> and
     * http://emfisis.physics.uiowa.edu/Flight/RBSP-A/Quick-Look/$Y/$m/$d/rbsp-a_magnetometer_4sec-gsm_emfisis-Quick-Look_$Y$m$d_v$(v,sep).cdf:<br>
     * <pre>http://emfisis.physics.uiowa.edu/Flight/RBSP-A/Quick-Look/
     *$Y/$m/$d/rbsp-a_magnetometer_4sec-gsm_emfisis-Quick-Look_$Y$m$d_v$(v,sep).cdf</pre>
     * <tt></tt><br>
     *
     * <p>
     * This new version uses regexs and is more complete than versions found in Autoplot, and they should
     * eventually use this instead.  Note the Autoplot one returns the index of the last /, whereas this
     * returns that index plus one.
     * </p>
     * 
     * <p>Taken from Autoplot's AggregatingDataSourceFactory, where Autoplot just has a URI and needs to get a file list.
     * See also org/autoplot/pngwalk/WalkUtil.java splitIndex, which also allows wildcards like *.</p>
     * @param surl a string like http://autoplot.org/data/C1_CP_EDI_EGD__$Y$m$d_V$v.cef
     * @return an integer indicating the split index, so that surl.substring(0,i) returns the slash.
     */
    public static int splitIndex(String surl) { 
        String regex= "([\\$\\%][yY\\(\\{])";
        Matcher m= Pattern.compile(regex).matcher(surl);
        if ( m.find() ) {
            int i= m.start();
            i = surl.lastIndexOf('/', i);
            return i+1;
        } else {
            return -1;
        }
    }

    /**
     * creates a FileStorageModel for the given template, which uses:
     * <pre>%Y-%m-%dT%H:%M:%S.%{milli}Z";
     *    %Y  4-digit year
     *    %m  2-digit month
     *    %d  2-digit day of month
     *    %j  3-digit day of year
     *    %H  2-digit Hour
     *    %M  2-digit Minute
     *    %S  2-digit second
     *    %v  best version by number  Also %(v,sep) for 4.3.2  or %(v,alpha)
     *    %{milli}  3-digit milliseconds </pre>
     * product_$(o,id=ftp://stevens.lanl.gov/pub/projects/rbsp/autoplot/orbits/rbspa_pp).png
     * @param root FileSystem source of the files.
     * @param template describes how filenames are constructed.  This is converted to a regular expression and may contain regex elements without groups.  The
     *   string may contain $ instead of percents as long as there are no percents in the string.
     *
     * @return a newly-created FileStorageModelNew.
     */
    public static FileStorageModelNew create( FileSystem root, String template ) {
        template= makeCanonical(template);
        String templatebr= hideParams( template );
        int i= templatebr.lastIndexOf("/");

        if ( template.contains("%") && !template.contains("$") ) { //TODO: makeCanonical should do this.
            template= template.replaceAll("\\%", "\\$");
            templatebr= templatebr.replaceAll("\\%", "\\$");
        }
        
        int i2= templatebr.lastIndexOf("$",i);
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
        String templatebr= hideParams( template );
        int i= templatebr.lastIndexOf("/");

        if ( template.contains("%") && !template.contains("$") ) {
            template= template.replaceAll("\\%", "\\$");
            templatebr= templatebr.replaceAll("\\%", "\\$");
        }
        
        int i2= templatebr.lastIndexOf("$",i);
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
                if ( sep==null && args.containsKey("dotnotation")) {
                    sep= "T";
                }
                String alpha= args.get( "alpha" );
                if ( alpha==null && args.containsKey("alphanumeric") ) {
                    alpha="T";
                }
                String type= args.get("type");
                if ( type!=null ) {
                    if ( type.equals("sep") || type.equals("dotnotation") ) {
                        sep= "T";
                    } else if (type.equals("alpha") || type.equals("alphanumeric") ) {
                        alpha="T"; 
                    }
                }
                if ( args.get("gt")!=null ) {
                    throw new IllegalArgumentException("gt specified but not supported: must be ge or lt");
                }
                if ( args.get("le")!=null ) {
                    throw new IllegalArgumentException("le specified but not supported: must be ge or lt");
                }
                String ge= args.get("ge");
                if ( ge!=null ) {
                    versionGe= ge;
                }
                String lt= args.get("lt");
                if ( lt!=null ) {
                    versionLt= lt;
                }
                if ( alpha!=null ) {  // netbeans suggestion is incorrect. alpha can be null.
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
            /**
             * this contains a dangerous kludge for $v.$v.$v.
             */
            public void parse( String fieldContent, TimeStruct startTime, TimeStruct timeWidth, Map<String,String> extra ) {
                String v= extra.get("v");
                if ( v!=null ) {
                    versioningType= VersioningType.numericSplit; 
                    fieldContent= v+"."+fieldContent; // Support $v.$v.$v
                } 
                extra.put( "v", fieldContent );                    
            }

            public String getRegex() {
                return ".*";
            }

            public String format( TimeStruct startTime, TimeStruct timeWidth, int length, Map<String, String> extra ) {
                return extra.get("v"); //TODO: length
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
        return String.valueOf(root) + template;
    }


}
