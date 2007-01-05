/*
 * VectorDataSetParser.java
 *
 * Created on December 4, 2004, 12:54 PM
 */

package edu.uiowa.physics.pw.das.dataset.parser;

import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.datum.*;
import java.io.*;
import java.util.regex.*;

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
    
    final static String decimalRegex= "\\S*\\d+\\S*";
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
            regexBuf.append("("+decimalRegex+")\\s+");
        }
        regexBuf.append("("+decimalRegex+")\\s*");        
        recordPattern= Pattern.compile(regexBuf.toString());
        this.recordPattern= recordPattern;
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
     * internal implementation of the parser.
     */
    private VectorDataSet readStream( InputStream in ) throws IOException {
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
            if ( iline<skipLines ) break;
            if ( (m=recordPattern.matcher(line)).matches() ) {
                for ( int i=0; i<fieldCount; i++ ) {
                    builder.insertY( irec, Double.parseDouble(m.group(i+1)), fieldNames[i] );
                }
                irec++;
            } else if ( propertyPattern!=null && ( m=propertyPattern.matcher(line) ).matches()) {
                builder.setProperty( m.group(1).trim(), m.group(2).trim() );
            } else {
                //System.out.println(line);
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
        VectorDataSetParser parser= VectorDataSetParser.newParser(5);        
        parser.setPropertyPattern(Pattern.compile("\\s*(.+)\\s*\\:\\s*(.+)\\s*"));
        long t0= System.currentTimeMillis();
        VectorDataSet ds= parser.readFile( "j:/ct/ncvs/sarahFFT/lintest10/2490lintest100002.raw" );        
        System.out.println(""+(System.currentTimeMillis()-t0));
        System.out.println(ds.getProperty("Frequency"));
        System.out.flush();
    }
    
}
