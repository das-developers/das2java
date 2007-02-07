/*
 * FileStorageModel.java
 *
 * Created on March 31, 2004, 9:52 AM
 */

package edu.uiowa.physics.pw.das.util.fileSystem;

import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.util.DasExceptionHandler;
import edu.uiowa.physics.pw.das.util.DasProgressMonitor;
import edu.uiowa.physics.pw.das.util.SubTaskMonitor;
import java.io.File;
import java.text.*;
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
public class FileStorageModel {
    
    private Pattern pattern;
    private Pattern absPattern;
    private String regex;
    
    private FieldHandler[] fieldHandlers;
    private int timeWidth; /* in TimeUtil enum */
    private boolean[] copyToEndTime; /* indexed by TimeUtil enum */
    private boolean useEndTime;
    FileStorageModel parent;
    FileSystem root;
    
    public static final int StartYear4=100;
    public static final int StartYear2=101;
    public static final int StartMonth=102;
    public static final int StartMonthName=108;
    public static final int StartDay=103;
    public static final int StartDoy=104;
    public static final int StartHour=105;
    public static final int StartMinute=106;
    public static final int StartSecond=107;
    
    public static final int EndYear4=200;
    public static final int EndYear2=201;
    public static final int EndMonth=202;
    public static final int EndMonthName=208;
    public static final int EndDay=203;
    public static final int EndDoy=204;
    public static final int EndHour=205;
    public static final int EndMinute=206;
    public static final int EndSecond=207;
    
    public static final int Ignore=300;
    
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
    
    //TODO: add 
    //  public string format( DatumRange dr );
    //
    private interface FieldHandler {
        public void handle( String s, TimeUtil.TimeStruct ts1, TimeUtil.TimeStruct ts2 );
    }
    
    public static abstract class IntegerFieldHandler implements FieldHandler {
        public void handle( String s, TimeUtil.TimeStruct ts1, TimeUtil.TimeStruct ts2 ) {
            handleInt( Integer.parseInt(s), ts1, ts2 );
        }
        abstract void handleInt( int i, TimeUtil.TimeStruct ts1, TimeUtil.TimeStruct ts2 );
        abstract String format( TimeUtil.TimeStruct ts1, TimeUtil.TimeStruct ts2 );
    }
    
    static final NumberFormat nf4= new DecimalFormat("0000");
    static final NumberFormat nf3= new DecimalFormat("000");
    static final NumberFormat nf2= new DecimalFormat("00");
        
    private static final FieldHandler StartMonthNameHandler= new FieldHandler() {
        public void handle( String s, TimeUtil.TimeStruct ts1, TimeUtil.TimeStruct ts2 ) {
            try {
                ts1.month= TimeUtil.monthNumber( s );
            } catch ( ParseException e ) {
                DasExceptionHandler.handle(e);
            }
        }
    };

    private static final FieldHandler EndMonthNameHandler= new FieldHandler() {
        public void handle( String s, TimeUtil.TimeStruct ts1, TimeUtil.TimeStruct ts2 ) {
            try {
                ts2.month= TimeUtil.monthNumber( s );
            } catch ( ParseException e ) {
                DasExceptionHandler.handle(e);
            }
        }
    };


    private static final FieldHandler StartYear4Handler= new IntegerFieldHandler() {
        public void handleInt( int i, TimeUtil.TimeStruct ts1, TimeUtil.TimeStruct ts2 ) { ts1.year= i;  }
        public String format( TimeUtil.TimeStruct ts1, TimeUtil.TimeStruct ts2 ) { return nf4.format(ts1.year); }
    };
    
    private static final FieldHandler StartYear2Handler= new IntegerFieldHandler() {
        public void handleInt( int i, TimeUtil.TimeStruct ts1, TimeUtil.TimeStruct ts2 ) { ts1.year= i<58 ? i+2000 : i+1900;  }
        public String format( TimeUtil.TimeStruct ts1, TimeUtil.TimeStruct ts2 ) { return nf2.format( ts1.year % 100 ); }
    };
    
    private static final FieldHandler StartMonthHandler= new IntegerFieldHandler() {        
        public void handleInt( int i, TimeUtil.TimeStruct ts1, TimeUtil.TimeStruct ts2 ) { ts1.month= i;  }
        public String format( TimeUtil.TimeStruct ts1, TimeUtil.TimeStruct ts2 ) { return nf2.format( ts1.month ); }
    };
    
    private static final FieldHandler StartDayHandler= new IntegerFieldHandler() {
        public void handleInt( int i, TimeUtil.TimeStruct ts1, TimeUtil.TimeStruct ts2 ) { ts1.day= i;  }
        public String format( TimeUtil.TimeStruct ts1, TimeUtil.TimeStruct ts2 ) { return nf2.format( ts1.day ); }
    };
    private static final FieldHandler StartDoyHandler= new IntegerFieldHandler() {
        public void handleInt( int i, TimeUtil.TimeStruct ts1, TimeUtil.TimeStruct ts2 ) { ts1.doy= i;  }
        public String format( TimeUtil.TimeStruct ts1, TimeUtil.TimeStruct ts2 ) { return nf3.format( ts1.doy ); }
    };
    private static final FieldHandler StartHourHandler= new IntegerFieldHandler() {
        public void handleInt( int i, TimeUtil.TimeStruct ts1, TimeUtil.TimeStruct ts2 ) { ts1.hour= i;  }
        public String format( TimeUtil.TimeStruct ts1, TimeUtil.TimeStruct ts2 ) { return nf2.format( ts1.hour ); }
    };
    private static final FieldHandler StartMinuteHandler= new IntegerFieldHandler() {
        public void handleInt( int i, TimeUtil.TimeStruct ts1, TimeUtil.TimeStruct ts2 ) { ts1.minute= i;  }
        public String format( TimeUtil.TimeStruct ts1, TimeUtil.TimeStruct ts2 ) { return nf2.format( ts1.minute ); }
    };
    private static final FieldHandler StartSecondHandler= new IntegerFieldHandler() {
        public void handleInt( int i, TimeUtil.TimeStruct ts1, TimeUtil.TimeStruct ts2 ) { ts1.seconds= i;  }
        public String format( TimeUtil.TimeStruct ts1, TimeUtil.TimeStruct ts2 ) { return nf2.format( ts1.seconds ); }
    };
    
    private static final FieldHandler EndYear4Handler= new IntegerFieldHandler() {
        public void handleInt( int i, TimeUtil.TimeStruct ts1, TimeUtil.TimeStruct ts2 ) { ts2.year= i;  }
        public String format( TimeUtil.TimeStruct ts1, TimeUtil.TimeStruct ts2 ) { return nf4.format( ts2.year ); }
    };
    private static final FieldHandler EndYear2Handler= new IntegerFieldHandler() {
        public void handleInt( int i, TimeUtil.TimeStruct ts1, TimeUtil.TimeStruct ts2 ) { ts2.year= i<58 ? i+2000 : i+1900;  }
        public String format( TimeUtil.TimeStruct ts1, TimeUtil.TimeStruct ts2 ) { return nf2.format( ts2.year ); }
    };
    
    private static final FieldHandler EndMonthHandler= new IntegerFieldHandler() {
        public void handleInt( int i, TimeUtil.TimeStruct ts1, TimeUtil.TimeStruct ts2 ) { ts2.month= i;  }
        public String format( TimeUtil.TimeStruct ts1, TimeUtil.TimeStruct ts2 ) { return nf2.format( ts2.month ); }
    };
    private static final FieldHandler EndDayHandler= new IntegerFieldHandler() {
        public void handleInt( int i, TimeUtil.TimeStruct ts1, TimeUtil.TimeStruct ts2 ) { ts2.day= i;  }
        public String format( TimeUtil.TimeStruct ts1, TimeUtil.TimeStruct ts2 ) { return nf2.format( ts2.day ); }
    };
    private static final FieldHandler EndDoyHandler= new IntegerFieldHandler() {
        public void handleInt( int i, TimeUtil.TimeStruct ts1, TimeUtil.TimeStruct ts2 ) { ts2.doy= i;  }
        public String format( TimeUtil.TimeStruct ts1, TimeUtil.TimeStruct ts2 ) { return nf3.format( ts2.doy ); }
    };
    private static final FieldHandler EndHourHandler= new IntegerFieldHandler() {
        public void handleInt( int i, TimeUtil.TimeStruct ts1, TimeUtil.TimeStruct ts2 ) { ts2.hour= i;  }
        public String format( TimeUtil.TimeStruct ts1, TimeUtil.TimeStruct ts2 ) { return nf2.format( ts2.hour ); }
    };
    private static final FieldHandler EndMinuteHandler= new IntegerFieldHandler() {
        public void handleInt( int i, TimeUtil.TimeStruct ts1, TimeUtil.TimeStruct ts2 ) { ts2.minute= i;  }
        public String format( TimeUtil.TimeStruct ts1, TimeUtil.TimeStruct ts2 ) { return nf2.format( ts2.minute ); }
    };
    private static final FieldHandler EndSecondHandler= new IntegerFieldHandler() {
        public void handleInt( int i, TimeUtil.TimeStruct ts1, TimeUtil.TimeStruct ts2 ) { ts2.seconds= i;  }
        public String format( TimeUtil.TimeStruct ts1, TimeUtil.TimeStruct ts2 ) { return nf2.format( ts2.seconds ); }
    };
    
    private static final FieldHandler IgnoreHandler= new FieldHandler() {
        public void handle( String s, TimeUtil.TimeStruct ts1, TimeUtil.TimeStruct ts2 ) {  }
        public String format( TimeUtil.TimeStruct ts1, TimeUtil.TimeStruct ts2 ) { return "*"; }
    };
    
    
    private void checkArgs( String regex, int[] digitList ) {
        int startLsd=0, endLsd=0;
        int[] startDigits= new int[7];
        int[] endDigits= new int[7];
        copyToEndTime= new boolean[8]; /* indexed by TimeUtil enum */
        int startBase=100;
        int endBase=200;
        int ignoreBase=300;
        for ( int i=0; i<digitList.length; i++ ) {
            if ( digitList[i]==StartMonthName ) digitList[i]= StartMonth;
            if ( digitList[i]==EndMonthName ) digitList[i]= EndMonth;
        }
        
        for ( int i=0; i<digitList.length; i++ ) {
            if ( digitList[i]==StartDoy ) {
                startDigits[1]=1; startDigits[2]=1;
                if ( 103>startLsd ) startLsd= 103;
            } else if ( digitList[i]==EndDoy ) {
                endDigits[1]=1; endDigits[2]=1;
                if ( 203>endLsd ) endLsd= 203;
            } else if ( digitList[i]>=startBase && digitList[i]<endBase ) {
                startDigits[digitList[i]-startBase]= 1;
                if ( digitList[i]>startLsd ) startLsd= digitList[i];
            } else if ( digitList[i]>=endBase && digitList[i]<ignoreBase ) {
                endDigits[digitList[i]-endBase]= 1;
                if ( digitList[i]>endLsd ) endLsd= digitList[i];
            }
        }
        if ( startDigits[StartYear2-startBase]==1 ) startDigits[StartYear4-startBase]=1;
        if ( startDigits[StartYear4-startBase]==1 ) startDigits[StartYear2-startBase]=1;
        
        if ( startDigits[StartDoy-startBase]==1 ) {
            startDigits[StartMonth-startBase]=1;
            startDigits[StartDay-startBase]=1;
        }
        if ( endDigits[EndYear2-endBase]==1 ) endDigits[EndYear4-endBase]=1;
        if ( startDigits[EndYear4-endBase]==1 ) startDigits[EndYear2-endBase]=1;        
        if ( endDigits[EndDoy-endBase]==1 ) {
            endDigits[EndMonth-endBase]=1;
            endDigits[EndDay-endBase]=1;
        }
        for ( int i=0; i<startDigits.length; i++ ) {
            if ( i>0 && startDigits[i]==1 && startDigits[i-1]!=1 ) {
                throw new IllegalArgumentException( "more significant digits missing in startTime");
            }
            if ( i>0 && startDigits[i]==0 && startDigits[i-1]==1 ) {
                timeWidth= toTimeUtilEnum( startLsd );
            }
        }
        
        boolean canUse= true;
        for ( int i=startLsd-startBase; i>=0; i-- ) {
            if ( endDigits[i]==0 ) canUse=false;
            if ( !canUse ) endDigits[i]= 0;
        }
        
        for ( int i=0; i<endDigits.length; i++ ) {
            copyToEndTime[toTimeUtilEnum(i+endBase)]= endDigits[i]==0;
        }
        
        if ( countGroups( regex ) != digitList.length ) {
            throw new IllegalArgumentException( "number of groups in regular expression ("+countGroups(regex)+") doesn't equal the length of digitList ("+digitList.length+")." );
        }
    }
    
    
    private static FieldHandler[] getHandlers( int [] digitList ) {
        FieldHandler[] startHandlers= new FieldHandler[] {
            StartYear4Handler, StartYear2Handler, StartMonthHandler,  StartDayHandler,
                    StartDoyHandler ,
                    StartHourHandler,     StartMinuteHandler,  StartSecondHandler, StartMonthNameHandler,
        };
        
        FieldHandler[] endHandlers= new FieldHandler[] {
            EndYear4Handler, EndYear2Handler, EndMonthHandler,  EndDayHandler,
                    EndDoyHandler ,
                    EndHourHandler,     EndMinuteHandler,  EndSecondHandler, EndMonthNameHandler,
        };
        
        ArrayList fieldHandlerList= new ArrayList();
        for ( int i=0; i<digitList.length; i++ ) {
            if ( digitList[i]>=100 && digitList[i]<200 ) {
                fieldHandlerList.add(i,startHandlers[digitList[i]-100]);
            } else if (digitList[i]>=200 && digitList[i]<300 ) {
                fieldHandlerList.add(i,endHandlers[digitList[i]-200]);
            } else if ( digitList[i]==300 ) {
                fieldHandlerList.add(i,IgnoreHandler);
            } else {
                throw new IllegalArgumentException("unknown field handler: "+digitList[i]);
            }
        }
        return (FieldHandler[])fieldHandlerList.toArray( new FieldHandler[fieldHandlerList.size()] );
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
        
        TimeUtil.TimeStruct ts1= new TimeUtil.TimeStruct();
        ts1.year=0;
        ts1.day=1;
        ts1.month=1;
        ts1.doy=1;
        ts1.hour=0;
        ts1.minute=0;
        ts1.seconds=0;
        
        TimeUtil.TimeStruct ts2= new TimeUtil.TimeStruct();
        if ( File.separatorChar=='\\' ) filename= filename.replaceAll("\\\\", "/");
        
        Matcher m= pattern.matcher(filename);
        if ( m.matches() ) {
            for ( int i=0; i<fieldHandlers.length; i++ ) {
                String s= m.group(i+1);
                fieldHandlers[i].handle(s,ts1,ts2);
            }
            TimeUtil.normalize(ts1);
            if ( copyToEndTime[1] ) ts2.year= ts1.year;
            if ( copyToEndTime[2] ) ts2.month= ts1.month;
            if ( copyToEndTime[3] ) ts2.day= ts1.day;
            if ( copyToEndTime[4] ) ts2.doy= ts1.doy;
            if ( copyToEndTime[5] ) ts2.hour= ts1.hour;
            if ( copyToEndTime[6] ) ts2.minute= ts1.minute;
            if ( copyToEndTime[7] ) ts2.seconds= ts1.seconds;
            
            Datum s1= TimeUtil.toDatum(ts1);
            Datum s2= TimeUtil.next(timeWidth, TimeUtil.toDatum(ts2));
            
            DatumRange dr= new DatumRange(s1,s2);
            return dr;
        } else {
            throw new IllegalArgumentException( "file name ("+filename+") doesn't match model specification ("+regex+")");
        }
    }
    
    public String getFilenameFor( Datum start, Datum end ) {
        TimeUtil.TimeStruct ts1= TimeUtil.toTimeStruct(start);
        TimeUtil.TimeStruct ts2= TimeUtil.toTimeStruct(end);
        
        // the following code does not work, but serves as a reminder of what this was going to do.
        StringBuffer result= new StringBuffer(30);
        result.append(root);
        for ( int i=0; i<fieldHandlers.length; i++ ) {
            //result.append( fieldHandlerList.
        }
        return null;
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
    
    public static FileStorageModel create( FileSystem root, String regex, int[] digitList )  {
        String parentRegex= getParentRegex( regex );
        FileStorageModel parentModel;
        if ( parentRegex!=null ) {
            int parentGroupsCount= countGroups(parentRegex);
            int[] parentDigitList= new int[parentGroupsCount];
            for ( int i=0; i<parentGroupsCount; i++ ) {
                parentDigitList[i]= digitList[i];
            }
            parentModel= create( root, parentRegex, parentDigitList );
        } else {
            parentModel= null;
        }
        return FileStorageModel.create( parentModel, root, regex, digitList );
    }
    
    
    public static FileStorageModel create( FileSystem root, String template ) {
        String[] s= template.split("%");
        char[] valid_formatCodes= new char[] { 'Y', 'y', 'j', 'm', 'd', 'H', 'M', 'S', 'v', 'V', 'x', 'b' };
        String[] formatName= new String[] { "Year", "2-digit-year", "day-of-year", "month", "day", "Hour", "Minute", "Second", "version", "Version", "date", "month-name" };
        int[] formatCode_lengths= new int[] { 4, 2, 3, 2, 2, 2, 2, 2, -1, -1, -1, -1 };
        int[] formatDigit= new int[] { StartYear4, StartYear2, StartDoy, StartMonth, StartDay, StartHour, StartMinute, StartSecond, Ignore, Ignore, Ignore, StartMonthName };
        
        int n= s.length;
        
        StringBuffer regex= new StringBuffer(100);
        regex.append( s[0] );
        
        int[] positions= new int[20];
        positions[0]= 0;
        int[] dateFormat= new int[n-1];
        
        int [] p= new int[20];  // position of each % specifier
        p[0]= 0;
        p[1]= s[0].length();
        
        boolean versioning=false;
        
        for ( int i= 1; i<n; i++ ) {
            char firstChar= s[i].charAt(0);
            int len= -1;
            
            char fc= s[i].charAt(0);
            
            int index=-1; 
            for ( int j=0; j<valid_formatCodes.length; j++ ) if ( valid_formatCodes[j] == fc ) index=j;
            
            String cc= s[i].substring(1);
            
            if ( index==-1 ) {
                throw new IllegalArgumentException("invalid format code: "+fc );
            } else {
                String fieldName= formatName[index];
            }
            if ( len == -1 ) len= formatCode_lengths[index];
            
            
            if ( len == -1 && cc.equals("") && i<n-1 ) {
                throw new IllegalArgumentException( "invalid variable specification, need non-null constant string to delineate" );
            }                        
            
            if ( fc == 'v' || fc == 'V' ) versioning= true;
            
            String dots=".........";
            regex.append( "("+dots.substring(0,len)+")" );
            regex.append( cc );
            
            dateFormat[i-1]= formatDigit[index];
            
            p[i+1]= p[i] + s[i].length() + 1;
            positions[i]= p[i+1] - cc.length();
        }
                
        return FileStorageModel.create( root, regex.toString(), dateFormat );
    }
    
    public FileStorageModel( FileStorageModel parent, FileSystem root, String regex, FieldHandler[] handlers ) {
        this.root= root;
        this.parent= parent;
        this.regex= regex;        
        this.pattern= Pattern.compile(regex);        
        this.fieldHandlers= handlers;
    }
        
    /** Creates a new instance of FileStorageModel */
    private static FileStorageModel create( FileStorageModel parent, FileSystem root, String regex, int[] digitList ) {
        FieldHandler[] handlers= getHandlers(digitList);
        
        FileStorageModel result= new FileStorageModel( parent, root, regex, handlers );
        result.checkArgs( regex, digitList );
        return result;
    }
    
    public String toString() {
        return String.valueOf(root) + regex;
    }
    
    public FileSystem getFileSystem() {
        return this.root;
    }

}
