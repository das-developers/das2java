/*
 * FileStorageModel.java
 *
 * Created on March 31, 2004, 9:52 AM
 */

package org.das2.fsm;

import java.awt.EventQueue;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URI;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.*;
import org.das2.DasApplication;
import org.das2.datum.CacheTag;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.DatumRangeUtil;
import org.das2.datum.TimeParser;
import org.das2.datum.TimeUtil.TimeStruct;
import org.das2.datum.Units;
import org.das2.util.LoggerManager;
import org.das2.util.filesystem.FileObject;
import org.das2.util.filesystem.FileSystem;
import org.das2.util.filesystem.FileSystemUtil;
import org.das2.util.filesystem.LocalFileSystem;
import org.das2.util.filesystem.WebFileSystem;
import org.das2.util.monitor.NullProgressMonitor;
import org.das2.util.monitor.ProgressMonitor;
import org.das2.util.monitor.SubTaskMonitor;

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
public class FileStorageModel {

    private Pattern pattern;
    private String regex;
    private Pattern gzpattern;

    FileStorageModel parent;
    FileSystem root;

    TimeParser timeParser;

    String template;

    static final Logger logger= LoggerManager.getLogger("das2.system.fsm");
    
    HashMap fileNameMap=null;
    private boolean allowGz= true;  // if true, the getFile can use a .gz version to retrieve a file.

    List<String> oldVersions= new ArrayList();

    /**
     * Versioning types 
     */
    static enum VersioningType {
        none(null),
        /**
         * simple floating point numeric comparisons.
         */
        numeric( new Comparator() {       // 4.10 > 4.01
                @Override
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
                @Override
                public int compare(Object o1, Object o2) {
                    return ((String)o1).compareTo((String)o2);
                }
            } ),
        /**
         * comparison of numbers split by decimal points and dashes, so 1.20 > 1.3.
         */
        numericSplit( new Comparator() {  // 4.3.23   // 1.1.3-01 for RBSP (rbspice lev-2 isrhelt)
               @Override
               public int compare(Object o1, Object o2) {
                    String[] ss1= o1.toString().split("[\\.-]",-2);
                    String[] ss2= o2.toString().split("[\\.-]",-2);
                    int n= Math.min( ss1.length, ss2.length );
                    for ( int i=0; i<n; i++ ) {
                        int d1= Integer.parseInt(ss1[i]);
                        int d2= Integer.parseInt(ss2[i]);
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

    /**
     * return the filesystem used to implement this.
     * @return filesystem
     */
    public FileSystem getFileSystem() {
        return this.root;
    }

    /**
     * return a child filesystem, with special code for LocalFileSystems to support
     * Windows.  TODO: look into .zip child.
     * @param root
     * @param child
     * @param monitor
     * @return the FileSystem
     * @see https://sourceforge.net/p/autoplot/bugs/2132
     * @throws org.das2.util.filesystem.FileSystem.FileSystemOfflineException
     * @throws UnknownHostException
     * @throws FileNotFoundException 
     */
    public static FileSystem getChildFileSystem( FileSystem root, String child, ProgressMonitor monitor ) throws FileSystem.FileSystemOfflineException, UnknownHostException, FileNotFoundException {
        FileSystem result; 
        if ( root.getRootURI().getScheme().equals("file") ) {
            File localRoot= ((LocalFileSystem)root).getLocalRoot();
            result= FileSystem.create( new File( localRoot, child ).toURI(), monitor.getSubtaskMonitor("create") );
        } else {
            result= FileSystem.create( root.getRootURI().resolve( child ), monitor.getSubtaskMonitor("create") ); // 3523492: allow the FS type to change; eg to zip.
        }
        return result;
        //fileSystems[i]= root.createFileSystem( names[i] ); //TODO: look into this.
        // 3523492: allow the FS type to change; eg to zip.
    }
    
    /**
     * return a random file from the FSM, which can be used to represent a typical file.  For
     * example, we need to look at metadata to see what is available.
     * @param monitor progress monitor in case a file must be downloaded.
     * @return a reference to the file within the FileSystem, or null if one is not found.
     * @throws IOException 
     */
    public String getRepresentativeFile( ProgressMonitor monitor ) throws IOException {
        return getRepresentativeFile( this, monitor, null, null, 0 );
    }
    
    /**
     * return a random file from the FSM, which can be used to represent a typical file.  For
     * example, we need to look at metadata to see what is available.
     * @param monitor progress monitor in case a file must be downloaded.
     * @param childRegex the parent must contain a file/folder matching childRegex
     * @return a reference to the file within the FileSystem, or null if one is not found.
     * @throws IOException 
     */  
    public String getRepresentativeFile( ProgressMonitor monitor, String childRegex ) throws IOException {     
        return getRepresentativeFile( this, monitor, childRegex, null, 0 );
    }
    /**
     * Return a random file from the FSM, which can be used to represent a typical file.  For
     * example, we need to look at metadata to see what is available.  This is introduced 
     * to support discovery, where we just need one file to
     * get started.  Before, there was code that would list all files, then use
     * just the first one.  This may return a skeleton file, but getFileFor() must
     * return a result.
     * This implementation does the same as getNames(), but stops after finding a file.
     * @param monitor progress monitor in case a file must be downloaded.
     * @param childRegex the parent must contain a file/folder matching childRegex, or null if there is no child.
     * @param range hint at the range where we are looking, or null if there is not time range constraint.
     * @return null if no file is found
     * @throws java.io.IOException if the file cannot be downloaded.
     */
    public String getRepresentativeFile( ProgressMonitor monitor, String childRegex, DatumRange range ) throws IOException {
        return getRepresentativeFile( this, monitor,childRegex,range,0 );
    }

    /**
     * Return a random file from the FSM, which can be used to represent a typical file.  For
     * example, we need to look at metadata of a given file to see what is available inside.  
     * This is introduced to support discovery, where we just need one file to
     * get started.  Before, there was code that would list all files, then use
     * just the first one.  This may return a skeleton file, but getFileFor() must
     * return a result.
     * This implementation does the same as getNames(), but stops after finding a file.
     * @param ths
     * @param monitor progress monitor in case a file must be downloaded.
     * @param childRegex the parent must contain a file/folder matching childRegex, or null if there is no child.
     * @param range hint at the range where we are looking.  
     * @param depth the recursion depth, useful for debugging.
     * @return null if no file is found
     * @throws java.io.IOException if the file cannot be downloaded.
     */
    public static String getRepresentativeFile( FileStorageModel ths, ProgressMonitor monitor, String childRegex, DatumRange range, int depth ) throws IOException {
            
        logger.log(Level.FINE, "get representative from {0} {1} range: {2}", new Object[]{ths.getFileSystem(), childRegex, range});
        
        //if ( depth==1 ) { //TODO: recusion makes this really hard to debug, and this should be rewritten to remove recursion.
        //    System.err.println("here at depth 1: "+ths.toString() );
        //}
        if ( monitor==null ) monitor= new NullProgressMonitor();
        
        String listRegex;

        FileSystem[] fileSystems;
        String[] names;
        String parentRegex=null;
        DatumRange range1;  // the range from the parent we are looking for.  This is to limit searches...
        
        if ( EventQueue.isDispatchThread() ) {
            logger.info("FileSystem use on the GUI event thread will often cause problems.");
            //new Exception("FileSystem uses event thread stack trace").printStackTrace();
        }
        
        String result= null;

        try {
            if ( ths.parent!=null ) {
                parentRegex= getParentRegex(ths.regex);
                String one= getRepresentativeFile( ths.parent, monitor.getSubtaskMonitor("get representative file"),ths.regex.substring(parentRegex.length()+1), range, depth+1 );
                if ( one==null ) return null;
                names= new String[] { one }; //parent.getNamesFor(null);
                fileSystems= new FileSystem[names.length];
                for ( int i=0; i<names.length; i++ ) {
                    try {
                        fileSystems[i]= getChildFileSystem( ths.root, names[i], monitor.getSubtaskMonitor("create") );
                    } catch ( FileSystem.FileSystemOfflineException | UnknownHostException | FileNotFoundException e ) {
                        throw new RuntimeException(e);
                    }
                }
                listRegex= ths.regex.substring( parentRegex.length()+1 );
            } else {
                fileSystems= new FileSystem[] { ths.root };
                names= new String[] {""};
                listRegex= ths.regex;
            }

            while ( result==null ) {
                for ( int i=fileSystems.length-1; result==null && i>=0; i-- ) {
                    String[] files1= fileSystems[i].listDirectory( "/", listRegex, monitor.getSubtaskMonitor("create") );
                    //to avoid the case where the latest folder is empty, check n-1 down to 0, then n.
                    //int n= files1.length;
                    //String[] resort= new String[n];
                    //for ( int j=0; j<n-1; j++ ) {
                    //    resort[j]= files1[n-2-j];
                    //}
                    //resort[n-1]= files1[n-1];
                    //files1= resort;
                    //for ( int j=0; j<files1.length && result==null; j++ ) {
                    int j= files1.length-1;
                    while ( j>=0 && result==null ) {                        
                        String ff= names[i].equals("") ? files1[ j ] : names[i]+"/"+files1[ j ];
                        if ( ff.endsWith("/") ) ff=ff.substring(0,ff.length()-1);
                        try {
                            HashMap<String,String> extra= new HashMap();
                            DatumRange tr= getDatumRangeFor( ths, ff, extra );
                            boolean versionOk= true;
                            if ( ths.versionGe!=null && ths.versioningType.comp.compare( extra.get("v"), ths.versionGe )<0 ) versionOk=false;
                            if ( ths.versionLt!=null && ths.versioningType.comp.compare( extra.get("v"), ths.versionLt )>=0 ) versionOk=false;
                            if ( versionOk && ths.timeParser.getValidRange().contains( tr ) && ( range==null || range.intersects(tr) ) ) {
                                if ( childRegex!=null ) {
                                    String[] kids= fileSystems[i].listDirectory( files1[ j ],childRegex, monitor.getSubtaskMonitor("list directory") );
                                    if ( kids.length>0 ) {
                                        result= ff;
                                    }
                                } else {
                                    result= ff;
                                }
                            }
                        } catch ( IllegalArgumentException ex ) {
                            logger.log( Level.FINER, null, ex );
                        }
                        if ( result==null ) j--;
                    }
                }

                if ( ths.allowGz ) {
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
                    if ( ths.parent==null  ) {
                        return null;
                    } else { // fall back to old code that would list everything.
                        logger.fine("fall back to old code that would list everything");
                        range1= ths.parent.getRangeFor(names[0]);
                        range1= range1.previous();
                        if ( range!=null && !range.intersects(range1) ) {
                            return null;
                        }
                        String one= getRepresentativeFile( 
                                ths.parent,
                                monitor.getSubtaskMonitor("getRepresentativeFile"),
                                ths.regex.substring(parentRegex.length()+1), 
                                range1, 
                                depth+1 );
                        if ( one==null ) return null;
                        names= new String[] { one }; //parent.getNamesFor(null);
                        fileSystems= new FileSystem[names.length];
                        for ( int i=0; i<names.length; i++ ) {
                            try {
                                fileSystems[i]= getChildFileSystem( ths.root, names[i], monitor.getSubtaskMonitor("create") ); 
                            } catch ( Exception e ) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            }
        } finally {
            monitor.finished();
        }        

        return result;
    }
    
    /**
     * set the datum range giving context to the files.  For example,
     * filenames are just $H$M$S.dat, and the context is "Jan 17th, 2015"
     * @param trdr the context
     */
    public void setContext( DatumRange trdr ) {
        timeParser.setContext(trdr);
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
                    throw new IllegalArgumentException( "file name \""+filename+"\" doesn't match model specification ("+template+")");
                }
            }
        } catch ( ParseException | NumberFormatException e ) {
            IllegalArgumentException e2=new IllegalArgumentException( "file name \""+filename+"\" doesn't match model specification ("+template+"), parse error in field",e);
            throw e2;
        }
    }

    private static DatumRange getDatumRangeFor( FileStorageModel ths, String filename, Map<String,String> extra ) {
        try {
            extra.clear();
            if ( ths.pattern.matcher(filename).matches() ) {
                ths.timeParser.parse( filename, extra );
                return ths.timeParser.getTimeRange();
            } else {
                if ( ths.gzpattern!=null && ths.gzpattern.matcher(filename).matches() ) {
                    ths.timeParser.parse( filename.substring(0,filename.length()-3), extra );
                    return ths.timeParser.getTimeRange();
                } else {
                    throw new IllegalArgumentException( "file name \""+filename+"\" doesn't match model specification ("+ths.template+")");
                }
            }
        } catch ( ParseException | NumberFormatException e ) {
            IllegalArgumentException e2=new IllegalArgumentException( "file name \""+filename+"\" doesn't match model specification ("+ths.template+"), parse error in field",e);
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
     * generate the names of the files that would cover this range.  This was
     * taken from Autoplot's org.virbo.jythonsupport.Util.  
     * TODO: versioning, etc.
     * @param range the time range to cover.
     * @return the string names, each in the context of the filesystem.
     */
    public String[] generateNamesFor( DatumRange range ) {
        String sstart;
        TimeParser tp= timeParser;
        try {
            sstart= tp.format( range.min(), null );
        } catch ( Exception ex ) { // orbit files have limited range
            DatumRange dr= tp.getValidRange();
            DatumRange dd= DatumRangeUtil.sloppyIntersection(range, dr);
            if ( dd.width().value()==0 ) {
                return new String[0]; // no intersection
            }
            sstart= tp.format( dd.min(), null );
        }

        
        try {
            tp.parse(sstart);
        } catch ( ParseException ex ) {
            throw new RuntimeException(ex);
        }
        DatumRange curr= tp.getTimeRange();
        
        int countLimit= 1000000;
        int approxCount= (int)( 1.01 * range.width().divide(curr.width()).value() ); // extra 1% for good measure.

        if ( approxCount>countLimit*1.03 ) {
            throw new IllegalArgumentException("too many intervals would be created, this is limited to about 1000000 intervals.");
        }
        
        List<String> result= new ArrayList<>( approxCount );
        
        if ( !range.intersects(curr) ) { // Sebastian has a strange case that failed, see 
            curr= curr.next();
        }
        
        while ( range.intersects(curr) ) {
            String scurr= tp.format( curr.min(), curr.max() );
            result.add( scurr );
            DatumRange oldCurr= curr;
            curr= curr.next();
            if ( oldCurr.equals(curr) ) { // orbits return next() that is this at the ends.
                break;
            }
        }
        return result.toArray( new String[result.size()] );
        
    }
        
    /**
     * return the timerange that contains the given timerange and
     * exactly contains a set of granules.
     * 
     * This needs to be synchronized because the timeParser.
     * @param timeRange arbitrary time range
     * @return list of file timeranges covering file input timeRange.
     */
    public synchronized DatumRange quantize(DatumRange timeRange) {
                
        try {
            String tf1= timeParser.format( timeRange.min(), timeRange.min() );
            String tf2= timeParser.format( timeRange.max(), timeRange.max() );
            
            DatumRange dr1,dr2;
            try {
                dr1= timeParser.parse(tf1).getTimeRange();
                dr2= timeParser.parse(tf2).getTimeRange();
//                if ( true ) {
//                    TimeParser tp1= timeParser.parse(tf1);
//                    logger.log( Level.WARNING, "tp1 start {0}", tp1.getTime(Units.us2000) );
//                    logger.log( Level.WARNING, "tp1 end {0}", tp1.getEndTime(Units.us2000) );
//                    logger.log( Level.WARNING, "tp1 tr {0}", tp1.getTimeRange() );
//                    TimeParser tp2= timeParser.parse(tf2);
//                    logger.log( Level.WARNING, "tp2 start {0}", tp2.getTime(Units.us2000) );
//                    logger.log( Level.WARNING, "tp2 end {0}", tp2.getEndTime(Units.us2000) );
//                    logger.log( Level.WARNING, "tp2 tr {0}", tp2.getTimeRange() );
//                }
            } catch ( IllegalArgumentException ex ) {
                logger.log(Level.WARNING, "Strange bug shown in test033: {2}\n>>{0}<<\n>>{1}<<", new Object[]{tf1, tf2,this.timeParser});
                TimeParser tp1= timeParser.parse(tf1);
                logger.log( Level.WARNING, "tp1 start {0}", tp1.getTime(Units.us2000) );
                logger.log( Level.WARNING, "tp1 end {0}", tp1.getEndTime(Units.us2000) );
                logger.log( Level.WARNING, "tp1 tr {0}", tp1.getTimeRange() );
                TimeParser tp2= timeParser.parse(tf2);
                logger.log( Level.WARNING, "tp2 start {0}", tp2.getTime(Units.us2000) );
                logger.log( Level.WARNING, "tp2 end {0}", tp2.getEndTime(Units.us2000) );
                logger.log( Level.WARNING, "tp2 tr {0}", tp2.getTimeRange() );
                throw ex;
            }
            
            if ( dr2.min().equals(timeRange.max() ) ) {
                return DatumRangeUtil.union( dr1, dr2.min() );
            } else {
                return DatumRangeUtil.union( dr1, dr2 );
            }
        } catch (ParseException ex) {
            throw new RuntimeException("this shouldn't happen");
        }
    }

    
    /**
     * return the names in the range, or all names if the range is null.
     * @param targetRange range limit, or null.
     * @return array of names within the system.
     * @throws java.io.IOException
     */
    public String[] getNamesFor( final DatumRange targetRange ) throws IOException {
        return getNamesFor( targetRange, false, new NullProgressMonitor() );
    }

    /**
     * return the names in the range, or all available names if the range is null.
     * This will list directories.
     * @param targetRange range limit, or null.
     * @param monitor
     * @return array of names within the system.
     * @throws java.io.IOException
     */
    public String[] getNamesFor( final DatumRange targetRange, ProgressMonitor monitor ) throws IOException {
        return getNamesFor( targetRange, false, monitor );
    }

    /**
     * return the names in the range, minding version numbers, or all available 
     * names if the range is null.  This will list directories.
     * @param targetRange range limit, or null.
     * @param monitor
     * @return array of names within the system.
     * @throws java.io.IOException
     */
    public String[] getBestNamesFor( final DatumRange targetRange, ProgressMonitor monitor ) throws IOException {
        return getNamesFor( targetRange, true, monitor );
    }

    /**
     * return the names in the range, minding version numbers, or all available 
     * names if the range is null.  This will list directories.
     * @param targetRange range limit, or null.
     * @return array of names within the system.
     * @throws java.io.IOException
     */
    public String[] getBestNamesFor( final DatumRange targetRange ) throws IOException {
        return getNamesFor( targetRange, true, new NullProgressMonitor() );
    }

    /**
     * return the names in the range, maybe with versioning.  This will 
     * list directories.
     * @param targetRange range limit, or null if no constraint used here.
     * @param versioning true means check versions.
     * @param monitor progress monitor (or null)
     * @return array of names within the system.
     * @throws IOException
     * @see generateNamesFor
     */
    private String[] getNamesFor( final DatumRange targetRange, boolean versioning, ProgressMonitor monitor ) throws IOException {
        logger.log( Level.FINE, "getNamesFor {0}", this.root);
        
        if ( monitor==null ) monitor= new NullProgressMonitor();
        
        String listRegex;

        FileSystem[] fileSystems;
        String[] names;

        oldVersions.clear();
        
        if ( parent!=null ) {
            names= parent.getNamesFor(targetRange,versioning,new NullProgressMonitor());   // note recursive call
            logger.log(Level.FINE, "parent {0} yields: {1}", new Object[]{parent.toString(), names.length});
            fileSystems= new FileSystem[names.length];
            for ( int i=0; i<names.length; i++ ) {
                try {
                    fileSystems[i]= getChildFileSystem( root, names[i], monitor );
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
            
            Arrays.sort(files1);
            
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
                    logger.log( Level.FINER, "  skip {0} because it does not parse properly", ff ); // this just means file doesn't match template.
                }
                monitor.setTaskProgress( i*10 + j * 10 / files1.length );
            }
            if ( fileSystems[i] instanceof WebFileSystem ) {
                WebFileSystem wfs= (WebFileSystem)fileSystems[i];
                File f= wfs.getLocalRoot();
                if ( f!=null ) { // e.g. Applet support, where applets have no local root.
                    FileSystem lfs= FileSystem.create( f.toURI() );
                    String[] files2= lfs.listDirectory( "/", theListRegex );
                    List<String> deleteRemote= new ArrayList();
                    List<String> remoteFiles= Arrays.asList( files1 );
                    for ( String s: files2 ) {
                        if ( remoteFiles.indexOf(s)==-1 ) {
                            deleteRemote.add(s);
                        }
                    }
                    logger.log(Level.FINE, "local files that do not exist on remote: {0}", deleteRemote);
                    oldVersions.addAll(deleteRemote);
                }
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
                        logger.log(Level.FINER, "check format exception: {0}", comp.compare( thss, thss )); // check for format exception
                        if ( ( versionGe==null || comp.compare( thss, versionGe )>=0 )
                                && ( versionLt==null || comp.compare( thss, versionLt )<0 ) ){
                            bestVersions.put( key, thss );
                            bestFiles.put( key,ff );
                        }
                    } catch ( Exception ex ) {
                        logger.log( Level.WARNING, ex.getMessage(), ex );
                        // doesn't match if comparator (e.g. version isn't a decimal number)
                    }
                } else {
                    try {
                        if ( ( ( versionGe==null || comp.compare( thss, versionGe )>=0 ) )
                                && ( versionLt==null || comp.compare( thss, versionLt )<0 )
                                && comp.compare( thss, best ) > 0 ) {
                            bestVersions.put( key,thss );
                            String rm= bestFiles.put( key,ff );
                            if ( !oldVersions.contains(rm) ) {
                                oldVersions.add(rm);
                            }
                        }
                    } catch ( Exception ex ) {
                        logger.log( Level.WARNING, ex.getMessage(), ex );
                        // doesn't match
                    }
                }
            }
            
            list= Arrays.asList( bestFiles.values().toArray( new String[ bestFiles.size()] ) );
        }

        Collections.sort( list, new Comparator() {
            @Override
            public int compare( Object o1, Object o2 ) {
                DatumRange dr1= getRangeFor( (String)o1 );
                DatumRange dr2= getRangeFor( (String)o2 );
                return dr1.compareTo( dr2 );
            }
        } );

        logger.log( Level.FINE, "getNamesFor {0} -> {1}", new Object[] { this.root, list.size() } );
        monitor.finished();
        return (String[])list.toArray(new String[list.size()]);
    }

    public static CacheTag getCacheTagFor( FileStorageModel fsm, DatumRange range, String[] names ) {
        Datum min= range.min();
        Datum max= range.max();
        for (String name : names) {
            DatumRange r = fsm.getRangeFor(name);
            min= min.gt(range.min()) ? r.min() : min;
            max= max.lt(range.max()) ? r.max() : max;
        }
        return new CacheTag( min, max, null );
    }
    
    public static CacheTag getCacheTagFor( FileStorageModel fsm, DatumRange range, File[] files ) {
        String[] names= new String[files.length];
        for ( int i=0; i<files.length; i++ ) {
            names[i]= fsm.getNameFor(files[i]);
        }
        return getCacheTagFor( fsm, range, names );
    }
    
    public File[] getFilesFor( final DatumRange targetRange ) throws IOException {
        return getFilesFor( targetRange, new NullProgressMonitor() );
    }

    /**
     * return the best files found for the range, without progress feedback.
     * @param targetRange
     * @return
     * @throws IOException 
     */
    public File[] getBestFilesFor( final DatumRange targetRange ) throws IOException {
        return getBestFilesFor( targetRange, new NullProgressMonitor() );
    }
    
    /**
     * remove files that have been identified as old versions.
     */
    public void cacheCleanup() {
        if ( getFileSystem() instanceof LocalFileSystem ) {
            logger.fine("local filesystems do not cache");
        } else {
            for ( String s: oldVersions ) {
                if ( getFileSystem().getFileObject(s).removeLocalFile()==false ) {
                    logger.log(Level.FINER, "removeLocalFile returned false: {0}", s);
                }
            }
        }
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
     * returns true if the parser has the field.
     * @param field e.g. "x"
     * @return true if the parser has the field.
     */
    public boolean hasField( String field ) {
        return timeParser.hasField( field );
    }
    
    /**
     * return the field value for the given name.  For example, if the spec
     * is $Y/$m/$d/$Y$m$d_v$(v,sep).dat and the name matched is 2014/04/04/20140404_v2.3.dat
     * then calling this for the field "v" would result in "2.3"  This should not
     * be used to retrieve fields that are components of the time range, such as 
     * $Y or $m.
     * @param field field, for example "v"
     * @param name name, for example 2014/04/04/20140404_v2.3.dat
     * @return the field value, for example, "2.3" when the spec is $Y/$m/$d/$Y$m$d_v$v.dat
     */
    public String getField( String field, String name ) {
        HashMap<String,String> hh= new HashMap();
        getDatumRangeFor( name, hh );
        if ( hh.containsKey(field) ) {
            return hh.get(field);
        } else {
            throw new IllegalArgumentException("field is not in template: "+field);
        }
    }
    
    /**
     * return true if the file came (or could come) from this FileStorageModel.
     * @param file the file
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
     * @param file the file
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
     * return the root of the filesystem as a string.
     * @return the root of the filesystem as a string.
     */
    public String getRoot() {
        return root.getRootURI().toString();
    }
    
    /**
     * check to see if "NAME.gz" exists
     * @param name name of uncompressed file
     * @param mon progress monitor (or null)
     * @return null or the uncompressed file.
     * @throws IOException
     */
    private File maybeGetGzFile( String name, ProgressMonitor mon) throws IOException {
        File f0 = null;
        if ( mon==null ) mon= new NullProgressMonitor();
        FileObject oz = root.getFileObject(name + ".gz"); 
        if (oz.exists()) {
            File fz = oz.getFile(mon);
            String sfz = fz.getPath().substring(0, fz.getPath().length() - 3);
            f0 = new File(sfz);
            FileSystemUtil.gunzip(fz, f0);
            if ( !f0.setLastModified(fz.lastModified()) ) {
                throw new IllegalArgumentException("failed to set last modified");
            }
        }
        return f0;
    }

    /**
     * download the file for the given name within the filesystem.
     * @param name the name within the filesystem.
     * @return null or a local file which can be opened.
     * @throws IOException 
     */
    public File getFileFor( String name ) throws IOException {
        File[] ff= getFilesFor( new String[] { name }, new NullProgressMonitor() );
        if ( ff.length>0 ) {
            return ff[0];
        } else {
            return null;
        }
    }
    
    /**
     * download the file for the given name within the filesystem.
     * @param name the name within the filesystem.
     * @param monitor monitor for the download.
     * @return null or a local file which can be opened.
     * @throws IOException 
     */
    public File getFileFor( String name, ProgressMonitor monitor ) throws IOException {
        File[] ff= getFilesFor( new String[] { name }, monitor );
        if ( ff.length>0 ) {
            return ff[0];
        } else {
            return null;
        }
    }
    
    /**
     * download the files for each of the given names within the filesystem.
     * @param names array of names within the filesystem
     * @param monitor monitor for the downloads (or null).
     * @return local files that can be opened.
     * @throws java.io.IOException during the transfer
     */
    public File[] getFilesFor( String [] names, ProgressMonitor monitor ) throws IOException {

        if ( monitor==null ) monitor= new NullProgressMonitor();
        
        File[] files= new File[names.length];

        if ( fileNameMap==null ) fileNameMap= new HashMap();

        int numwarn= 0;
        
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
                if ( files[i]==null && numwarn<3 ) {
                    logger.log(Level.WARNING, "listing returns result that cannot be resolved file file (e.g. bad link): {0}", names[i]);
                    numwarn++;
                }
                fileNameMap.put( files[i], names[i] );
            } catch ( IOException e ) {
                throw new RuntimeException(e);
            }
        }
        monitor.finished();

        // remove nulls that come from bad references.
        ArrayList<File> result= new ArrayList(files.length);
        int i=0;
        for (File file : files) {
            if (file != null) {
                result.add(file);
                i++;
            }
        }
        return result.toArray( new File[i] );
        
    }
    
    /**
     * Download the files within the range.
     * This might catch a bad link where getNamesFor does not.
     * @param targetRange range limit, or null if no constraint used here.
     * @param monitor the monitor
     * @return a list of files that can be opened.  
     * @throws java.io.IOException 
     */
    public File[] getFilesFor( final DatumRange targetRange, ProgressMonitor monitor ) throws IOException {
        String[] names= getNamesFor( targetRange );
        File[] ff= getFilesFor( names, monitor );
        return ff;
    }
    
    /**
     * Get the files for the range, using versioning info ($v,etc).
     * @param targetRange range limit, or null if no constraint used here.
     * @param monitor the monitor
     * @return a list of files that can be opened.  
     * @throws IOException
     */
    public File[] getBestFilesFor( final DatumRange targetRange, ProgressMonitor monitor ) throws IOException {
        
        if ( monitor==null ) monitor= new NullProgressMonitor();
        
        String[] names= getNamesFor( targetRange, true, monitor.getSubtaskMonitor("get names") );
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
            } catch ( IOException e ) {
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
     * @return parent or null.
     */
    public FileStorageModel getParent() {
        return this.parent;
    }


    /**
     * Autoplot introduced the dollar sign instead of the percent, because $ is
     * more URI-friendly.  Switch to this if it looks appropriate.
     * 
     * NOTE: this does not correct commas in the qualifiers section.  TODO: fix this!
     * 
     * @param template, e.g "/%Y/%m%d.dat"
     * @return "$Y/$m$d.dat"
     */
    protected static String makeCanonical( String template ) {
        String result;
        if ( template.contains("$Y") || template.contains("$y" ) ) {
            result= template.replaceAll("\\$", "%");
        } else {
            result= template;
        }
        result= result.replaceAll("//+","/");
        int i=result.indexOf("/");
        if ( i>-1 && result.indexOf("%")>i ) {
            System.err.println("each folder of template must have fields marked by $ or %: "+ result.substring(0,i) );
        }
        if ( result.startsWith("/") ) {
            result= result.substring(1);
        }
        return result;
    }

    /**
     * hide the contents of parameters as in<pre>
     *   product_%(o,id=ftp://stevens.lanl.gov/pub/projects/rbsp/autoplot/orbits/rbspa_pp).png -->
     *   product_%(______________________________________________________________________).png
     * </pre>
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
     * @return an integer indicating the split index, so that surl.substring(0,i) includes the slash.
     */
    public static int splitIndex(String surl) { 
        String regex= "([\\$\\%][yYxv\\(\\{])";
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
     * @return a newly-created FileStorageModel.
     */
    public static FileStorageModel create( FileSystem root, String template ) {
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
            FileStorageModel parentFSM= FileStorageModel.create( root, parentTemplate );
            return new FileStorageModel( parentFSM, root, template );
        } else {
            return new FileStorageModel( null, root, template );
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
     * @return a newly-created FileStorageModel.
     */
    public static FileStorageModel create( FileSystem root, String template, String fieldName, TimeParser.FieldHandler fieldHandler ) {
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
            FileStorageModel parentFSM= FileStorageModel.create( root, parentTemplate, fieldName, fieldHandler );
            return new FileStorageModel( parentFSM, root, template, fieldName, fieldHandler );
        } else {
            return new FileStorageModel( null, root, template, fieldName, fieldHandler );
        }
    }

    private FileStorageModel( FileStorageModel parent, FileSystem root, String template, String fieldName, TimeParser.FieldHandler fieldHandler, Object ... moreHandler   ) {
        this.root= root;
        this.parent= parent;
        
        if ( template.startsWith("/") ) { // clean up double slashes immediately.  (/home/jbf//data/)
            template= template.substring(1);
        }
        
        while ( template.contains("//") ) {
            template= template.replaceAll("\\/\\/", "/");
        }

        this.template= template.replaceAll("\\+", "\\\\+");
        
        String f="v";
        versioningType= VersioningType.none;
        
        TimeParser.FieldHandler vh= new TimeParser.FieldHandler() {
            @Override
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

            @Override
            public void parse( String fieldContent, TimeStruct startTime, TimeStruct timeWidth, Map<String,String> extra ) {
                String v= extra.get("v");
                if ( v!=null ) {
                    versioningType= VersioningType.numericSplit; 
                    fieldContent= v+"."+fieldContent; // Support $v.$v.$v
                } 
                extra.put( "v", fieldContent );                    
            }

            @Override
            public String getRegex() {
                return ".*";
            }

            @Override
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

        //if ( this.timeParser.isStartTimeOnly() ) {
        //    this.startTimeOnly= true;
        //}

        this.regex= timeParser.getRegex();
        this.pattern= Pattern.compile(regex);
        if ( template.endsWith(".gz") ) {
            allowGz= false; // turn off automatic uncompressing GZ files.
        }
        if ( allowGz ) {
            this.gzpattern= Pattern.compile(regex+"\\.gz");
        }
    }

//    /**
//     * The filename time only contains the start time of the interval, the end of the interval
//     * is only bounded by the next file.
//     */
//    protected boolean startTimeOnly = false;
//
//    /**
//     * limit on the length of files with startTimeOnly set.
//     * e.g. $Y$m$d_$(H,startTimeOnly)$M means that the files should be much less that one hour long
//     *
//     */
//    protected Datum implicitTimeDelta= null;

    private FileStorageModel( FileStorageModel parent, FileSystem root, String template ) {
        this( parent, root, template, null, null );
    }
 
    @Override
    public String toString() {
        return String.valueOf(root) + template;
    }


}
