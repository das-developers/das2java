/*
 * FileStorageModel.java
 *
 * Created on March 31, 2004, 9:52 AM
 */

package edu.uiowa.physics.pw.das.util.fileSystem;

import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.util.DasProgressMonitor;
import edu.uiowa.physics.pw.das.util.SubTaskMonitor;
import edu.uiowa.physics.pw.das.util.TimeParser;
import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.regex.*;

/**
 * Represents a method for storing data sets in a set of files by time.  The 
 * client provides a regex for the files and how each group of the regex is 
 * interpreted as a time digit.  The model can then be used to provide the set
 * of files that cover a time range, etc.
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
    
        
    /*
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
        if ( pattern.matcher(filename).matches() ) {
            timeParser.parse( filename );
            return timeParser.getTimeRange();
        } else {
            throw new IllegalArgumentException( "file name ("+filename+") doesn't match model specification ("+regex+")");
        }
    }
    
    public String getFilenameFor( Datum start, Datum end ) {
        return timeParser.format( start, end );
    }
    
    public String[] getNamesFor( final DatumRange targetRange ) {
        return getNamesFor( targetRange, DasProgressMonitor.NULL );
    }

    public String[] getNamesFor( final DatumRange targetRange, DasProgressMonitor monitor ) {
        
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
        
        for ( int i=0; i<fileSystems.length; i++ ) {
            String[] files1= fileSystems[i].listDirectory( "/", listRegex );
            for ( int j=0; j<files1.length; j++ ) {
                String ff= names[i].equals("") ? files1[j] : names[i]+"/"+files1[j];
                if ( ff.endsWith("/") ) ff=ff.substring(0,ff.length()-1);
                if ( getDatumRangeFor( ff ).intersects(targetRange) ) list.add(ff);
            }
        }
        return (String[])list.toArray(new String[list.size()]);
    }
    
    public File[] getFilesFor( final DatumRange targetRange ) {
        return getFilesFor( targetRange, DasProgressMonitor.NULL );
    }
    
    public DatumRange getRangeFor( String name ) {
        return getDatumRangeFor( name );
    }
    
    /**
     * returns true if the file came (or could come) from this FileStorageModel.  
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
     * Need a way to recover the model name of a file.  The returned File from getFilesFor can be anywhere,
     * so it would be good to provide a way to get it back into a FSM name.
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
     * returns a list of files that can be used 
     */
    public File[] getFilesFor( final DatumRange targetRange, DasProgressMonitor monitor ) {
        String[] names= getNamesFor( targetRange );
        File[] files= new File[names.length];
        
        if ( fileNameMap==null ) fileNameMap= new HashMap();
        
        if ( names.length>0 ) monitor.setTaskSize( names.length * 10 );
        for ( int i=0; i<names.length; i++ ) {
            try {
                FileObject o= root.getFileObject( names[i] );
                files[i]= o.getFile( SubTaskMonitor.create( monitor, i*10, (i+1)*10 ));                
                fileNameMap.put( files[i], names[i] );
            } catch ( Exception e ) {
                throw new RuntimeException(e);
            }
        }
        return files;
    }
    
    
    private static int countGroups( String regex ) {
        int result=0;
        Pattern p= Pattern.compile( regex );
        Matcher m= p.matcher("");
        return m.groupCount();
    }
    
    public static String getParentRegex( String regex ) {
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
  
    public FileStorageModelNew( FileStorageModelNew parent, FileSystem root, String template ) {
        this.root= root;
        this.parent= parent;
        this.template= template;
        this.timeParser= TimeParser.create( template );
        this.regex= timeParser.getRegex();
        this.pattern= Pattern.compile(regex); 
    }

    
    public String toString() {
        return String.valueOf(root) + regex;
    }
    
}
