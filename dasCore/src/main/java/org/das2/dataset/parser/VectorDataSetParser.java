/*
 * VectorDataSetParser.java
 *
 * Created on December 4, 2004, 12:54 PM
 */

package org.das2.dataset.parser;

import org.das2.datum.Units;
import java.io.*;
import java.util.regex.*;
import org.das2.dataset.VectorDataSet;
import org.das2.dataset.VectorDataSetBuilder;

/**
 * Class for reading ascii tables into a VectorDataSet.  This parses a
 * file by looking at each line to see if it matches one of
 * two Patterns: one for properties and one for records.  If a record matched,
 * then the record is matched and fields pulled out, parsed and insered a
 * VectorDataSetBuilder.  If a property is matched, then the builder property
 * is set.  Two Patterns are provided NAME_COLON_VALUE_PATTERN and
 * NAME_EQUAL_VALUE_PATTERN for convenience.  The record pattern is currently
 * the number of fields identified with whitespace in between.  Note the X
 * tags are just the record numbers.
 *
 * @author  Jeremy
 */
public class VectorDataSetParser {
    
    Pattern propertyPattern;
    String commentRegex;
    Pattern recordPattern;
    String[] fieldNames;
    
    final static String numberPart= "[\\d\\.eE\\+\\-]+";
    final static String decimalRegex= numberPart;
    int skipLines;
    int recordCountLimit;
    int fieldCount;
    
    public final static Pattern NAME_COLON_VALUE_PATTERN= Pattern.compile("\\s*(.+?)\\s*\\:\\s*(.+)\\s*");
    public final static Pattern NAME_EQUAL_VALUE_PATTERN= Pattern.compile("\\s*(.+?)\\s*\\=\\s*(.+)\\s*");
    
    private VectorDataSetParser( String[] fieldNames ) {
        this.fieldCount= fieldNames.length;
        this.fieldNames= fieldNames;
        StringBuffer regexBuf= new StringBuffer();
        regexBuf.append("\\s*");
        for ( int i=0; i<fieldCount-1; i++ ) {
            regexBuf.append("("+decimalRegex+")[\\s+,+]\\s*");
        }
        regexBuf.append("("+decimalRegex+")\\s*");
        recordPattern= Pattern.compile(regexBuf.toString());
        this.recordPattern= recordPattern;
    }
    
    /**
     * return the field count that would result in the largest number of records parsed.  The 
     * entire file is scanned, and for each line the number of decimal fields is counted.  At the end
     * of the scan, the fieldCount with the highest record count is returned.
     */
    public static int guessFieldCount( String filename ) throws FileNotFoundException, IOException {
        
        final int maxFieldCount=8;  // can only identify maxFieldCount - 1.
        
        int[] recCount= new int[maxFieldCount];
        
        StringBuffer regexBuf= new StringBuffer();
        regexBuf.append("\\s*("+decimalRegex+")");
        for ( int i=1; i<maxFieldCount; i++ ) {
            regexBuf.append("([\\s+,+]\\s*("+decimalRegex+"))?");
        }
        regexBuf.append( "\\s*" );
        
        Pattern pat= Pattern.compile( regexBuf.toString() );
        
        BufferedReader reader= new LineNumberReader( new FileReader( filename ) );
        
        String line;
        while ( ( line=reader.readLine() ) != null ) {
            Matcher m= pat.matcher( line );
            if ( m.matches() ) {
                int j;
                for ( j=1; j<m.groupCount(); j+=2 ) {
                    if ( m.group(j)==null ) {
                        recCount[ (j-1) / 2 ] ++;
                        break;
                    }
                }
            }
        }
        int max=0;
        int imax=0;
        for ( int j=1; j<maxFieldCount; j++ ) {
            if ( recCount[j] > max ) {
                imax=j;
                max= recCount[j];
            }           
        }
        
        return imax;
    }
    
    /**
     * creates a parser with @param fieldCount fields, named "field0,...,fieldN"
     */
    public static VectorDataSetParser newParser( int fieldCount ) {
        String[] fieldNames= new String[ fieldCount ];
        for ( int i=0; i<fieldCount; i++ ) {
            fieldNames[i]= "field"+i;
        }
        return new VectorDataSetParser( fieldNames );
    }
    
    /**
     * creates a parser with the named fields.
     */
    public static VectorDataSetParser newParser( String[] fieldNames ) {
        return new VectorDataSetParser( fieldNames );
    }
    
    /**
     * skip a number of lines before trying to parse anything.
     */
    public void setSkipLines( int skipLines ) {
        this.skipLines= skipLines;
    }
    
    /**
     * limit the number of records read.  parsing will stop at this limit.
     */
    public void setRecordCountLimit( int recordCountLimit ) {
        this.recordCountLimit= recordCountLimit;
    }
    
    /**
     * specify the Pattern used to recognize properties.  Note property
     * values are not parsed, they are provided as Strings.
     */
    public void setPropertyPattern( Pattern propertyPattern ) {
        this.propertyPattern= propertyPattern;
    }
    
    /**
     * Parse the stream using the current settings.
     */
    public VectorDataSet readStream( InputStream in ) throws IOException {
        BufferedReader reader= new BufferedReader( new InputStreamReader(in ) );
        String line;
        int iline=0;
        int irec=0;
        
        VectorDataSetBuilder builder= new VectorDataSetBuilder( Units.dimensionless, Units.dimensionless );
        for ( int i=0; i<fieldCount; i++ ) {
            builder.addPlane( fieldNames[i], Units.dimensionless );
        }
        Matcher m;
        while ( (line=reader.readLine() )!=null ) {
            //System.err.println(line);
            try {
                if ( iline<skipLines ) break;
                if ( (m=recordPattern.matcher(line)).matches() ) {
                    boolean allInvalid= true;
                    for ( int i=0; i<fieldCount; i++ ) {
                        try {
                            double d= Double.parseDouble(m.group(i+1)); 
                            allInvalid= false;
                        } catch ( NumberFormatException e ) {}
                    }
                    if ( ! allInvalid ) {
                        for ( int i=0; i<fieldCount; i++ ) {
                            builder.insertY( irec, Double.parseDouble(m.group(i+1)), fieldNames[i] );
                        }
                        irec++;
                    }
                } else if ( propertyPattern!=null && ( m=propertyPattern.matcher(line) ).matches()) {
                    builder.setProperty( m.group(1).trim(), m.group(2).trim() );
                } else {
                    //System.out.println(line);
                }
            } catch ( NumberFormatException e ) {
                e.printStackTrace();
            }
        }
        
        return builder.toVectorDataSet();
    }
    
    /**
     * Parse the file using the current settings.
     */
    public VectorDataSet readFile( String filename ) throws IOException {
        return readStream( new FileInputStream(filename ) ) ;
    }
    
    public static void main(String[] args) throws Exception {
        
        String file= "L:/ct/virbo/autoplot/data/2490lintest90005.dat";
        System.err.println( VectorDataSetParser.guessFieldCount( file ) );
        
        VectorDataSetParser parser= VectorDataSetParser.newParser(5);
        parser.setPropertyPattern(Pattern.compile("\\s*(.+)\\s*\\:\\s*(.+)\\s*"));
        long t0= System.currentTimeMillis();
        VectorDataSet ds= parser.readFile( "j:/ct/ncvs/sarahFFT/lintest10/2490lintest100002.raw" );
        System.out.println(""+(System.currentTimeMillis()-t0));
        System.out.println(ds.getProperty("Frequency"));
        System.out.flush();
    }
    
}
