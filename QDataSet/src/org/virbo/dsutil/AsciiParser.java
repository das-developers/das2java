/*
 * AsciiParser.java
 *
 * Created on May 25, 2007, 7:01 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.virbo.dsutil;

import java.util.logging.Level;
import org.das2.datum.Units;
import org.das2.datum.UnitsUtil;
import org.das2.util.monitor.ProgressMonitor;
import java.io.*;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.regex.*;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.DatumRangeUtil;
import org.das2.datum.EnumerationUnits;
import org.das2.datum.InconvertibleUnitsException;
import org.das2.datum.TimeParser;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.WritableDataSet;
import org.virbo.dsops.Ops;

/**
 * Class for reading ASCII tables into a QDataSet.  This parses a file by breaking
 * it up into records, and passing the record off to a delegate record parser.
 * The record parser then breaks up the record into fields, and each field is 
 * parsed by a delegate field parser.  Each column of the table has a Unit, field name,
 * and field label associated with it.
 * 
 * Examples of record parsers include 
 * DelimParser, which splits the record by a delimiter such as a tab or comma,
 * RegexParser, which processes each record with a regular expression to get the fields,
 * and FixedColumnsParser, which splits the record by character positions.
 * Example of field parsers include DOUBLE_PARSER which parses the value
 * as a double, and UNITS_PARSER, which uses the Unit attached to the column
 * to interpret the value.
 *
 * When the first record with the correct number of fields is found but is not
 * parseable, we look for field labels and units.
 * 
 * The skipLines property tells the parser to skip a given number of header lines 
 * before attempting to parse the record.  Also, commentPrefix identifies lines to be 
 * ignored.  In either the header or in comments, we look for propertyPattern, and
 * if a property is matched, then the builder property
 * is set.  Two Patterns are provided NAME_COLON_VALUE_PATTERN and
 * NAME_EQUAL_VALUE_PATTERN for convenience.   
 *
 * Adapted to QDataSet model, Jeremy, May 2007.
 *
 * @author Jeremy
 */
public class AsciiParser {

    private static final Logger logger= Logger.getLogger("qdataset.ascii");

    Pattern propertyPattern = null;
    String commentPrefix = "#";

    /**
     * a java identifier that can be used to identify the column.
     */
    String[] fieldNames;

    /**
     * rich headers are put here.
     */
    AsciiHeadersParser.BundleDescriptor bundleDescriptor;

    /**
     * units for each column.
     */
    Units[] units;

    /** either the unit or depend 1 value associated with the column,
     * e.g. Density(cc**-3)  or  flux_C4(6.4).
     * @see #units
     */
    String[] fieldUnits;

    /**
     * the presentation label for the column.
     */
    String[] fieldLabels;

    FieldParser[] fieldParsers;
    final static String numberPart = "[\\d\\.eE\\+\\-]+";
    final static String decimalRegex = numberPart;
    int skipLines;
    
    int recordCountLimit = Integer.MAX_VALUE;
    int fieldCount;
    
    /**
     * pattern for name:value.  
     */
    public final static Pattern NAME_COLON_VALUE_PATTERN = Pattern.compile("\\s*([a-zA-Z_].*?)\\s*\\:\\s*(.+)\\s*");
    
    /**
     * pattern for name=value.  
     */
    public final static Pattern NAME_EQUAL_VALUE_PATTERN = Pattern.compile("\\s*([a-zA-Z_].*?)\\s*\\=\\s*(.+)\\s*");
    
    /**
     * detect identifiers for columns.
     */
    Pattern COLUMN_ID_HEADER_PATTERN = Pattern.compile("\\s*\"?([a-zA-Z][a-zA-Z _0-9]*)([\\(\\[]([a-zA-Z_\\.\\[\\-\\]0-9//\\*\\^]*)[\\)\\]])?\"?\\s*");
    /**
     * allow columns to be labeled with some datum ranges, such as 10.0-13.1.  We convert these into an identifier, but depend1labels will present as-is.
     * Note this pattern will match "-999.000" so check groups 2 and 4 for non null.
     */
    private final static Pattern COLUMN_CHANNEL_HEADER_PATTERN = Pattern.compile("\\s*\"?(([a-zA-Z_]*)(\\d*\\.?\\d*([eE]\\d+)?)\\-(\\d*\\.?\\d*([eE]\\d+)?))\"?\\s*");

    public final static String PROPERTY_FIELD_NAMES = "fieldNames";
    public static final String PROPERTY_FILE_HEADER = "fileHeader";
    public static final String PROPERTY_FIRST_RECORD = "firstRecord";
    public static final String PROPERTY_FIELD_PARSER = "fieldParser";
    public static final String DELIM_COMMA = ",";
    public static final String DELIM_TAB = "\t";
    public static final String DELIM_WHITESPACE = "\\s+";

    private static final int HEADER_LENGTH_LIMIT=1000;

    /**
     * Convenient unit for parsing UTC times.
     */
    public static final Units UNIT_UTC= Units.t2000;

    StringBuffer headerBuffer = new StringBuffer();

    private AsciiParser(String[] fieldNames) {
        setRegexParser(fieldNames);
    }

    /**
     * returns true if the line is a header or comment.
     * @param iline the line number in the file, starting with 0.
     * @param lastLine the last line read.
     * @param thisLine the line we are testing.
     * @param recCount the number of records successfully read.
     * @return true if the line is a header line.
     */
    public final boolean isHeader(int iline, String lastLine, String thisLine, int recCount) {
        return (iline < skipLines || (headerDelimiter != null && recCount == 0 && (lastLine == null || !Pattern.compile(headerDelimiter).matcher(lastLine).find())) || (commentPrefix != null && thisLine.startsWith(commentPrefix)));
    }

    /**
     * quick-n-dirty check to see if a string appears to be an ISO8601 time.
     * minimally 2000-002T00:00, but also 2000-01-01T00:00:00Z etc. 
     * Note that an external code may explicitly indicate that the field is a time,
     * This is just to catch things that are obviously times.
     * @param s
     * @return true if this is clearly an ISO time.
     */
    public final boolean isIso8601Time( String s ) {
        if ( s.length()>13 && s.contains("T") ) {
            if ( ( s.startsWith("20") || s.startsWith("19") || s.startsWith("18") ) && Character.isDigit(s.charAt(2)) && Character.isDigit(s.charAt(3)) ) {
                int charCount=4;
                for ( int i=4; i<s.length(); i++ ) {
                    if ( Character.isDigit( s.charAt(i) ) ) charCount++;
                }
                return charCount>10;
            } else {
                return false;
            }
        } else {
            return false;
        }
        
    }
    /**
     * return the first record that the parser would parse.  If skipLines is 
     * more than the total number of lines, or all lines are comments, then null
     * is returned.
     * 
     * @param filename
     * @return the first line after skip lines and comment lines.
     * @throws java.io.IOException
     */
    public String readFirstRecord(String filename) throws IOException {
        return readFirstRecord(new BufferedReader(new FileReader(filename)));
    }

    /**
     * return the first line of the freshly opened file.  The reader
     * is closed.
     * @param reader
     * @return
     * @throws java.io.IOException
     */
    public String readFirstRecord(BufferedReader reader) throws IOException {

        String line;
        String lastLine = null;

        int iline = 0;
        line = reader.readLine();

        while (line != null && isHeader(iline, lastLine, line, 0)) {
            lastLine = line;
            line = reader.readLine();
            iline++;
        }
        reader.close();

        return line;
    }

    /**
     * returns the first record that the record parser parses successfully.  The
     * recordParser should be set and configured enough to identify the fields.
     * If no records can be parsed, then null is returned.
     *
     * The first record should be in the first 1000 lines.
     * 
     * @param filename
     * @return the first parseable line, or null if no such line exists.
     * @throws java.io.IOException
     */
    public String readFirstParseableRecord(String filename) throws IOException {
        String line;
        try (BufferedReader reader = new LineNumberReader(new FileReader(filename))) {
            String lastLine = null;
            
            line = reader.readLine();
            int iline = 0;
            while (line != null && isHeader(iline, lastLine, line, 0)) {
                lastLine = line;
                line = reader.readLine();
                iline++;
            }
            
            DataSetBuilder builder = new DataSetBuilder(2, 100, recordParser.fieldCount() );
            
            while (line != null && iline<HEADER_LENGTH_LIMIT && this.recordParser.tryParseRecord(line, 0, builder) == false) {
                line = reader.readLine();
                iline++;
            }
            
            if ( iline==HEADER_LENGTH_LIMIT ) line= null;
        }

        return line;
    }

    /**
     * try to figure out how many lines to skip by looking for the line where
     * the number of fields becomes stable.
     * @param filename
     * @param recParser
     * @return
     * @throws java.io.IOException
     */
    public int guessSkipLines( String filename, RecordParser recParser ) throws IOException {
        BufferedReader reader = null;
        int currentFirstRecord=0;
        try {
            reader= new LineNumberReader(new FileReader(filename));

            String line;
            String lastLine = null;

            line = reader.readLine();
            int iline = 0;
            while (line != null && isHeader(iline, lastLine, line, 0)) {
                lastLine = line;
                line = reader.readLine();
                iline++;
            }

            int currentFieldCount=-1;
            currentFirstRecord=iline;
            int repeatCount=0;
            while ( line != null ) {
                int fc= recParser.fieldCount(line);
                if ( fc!=currentFieldCount ) {
                    currentFieldCount=fc;
                    currentFirstRecord= iline;
                    repeatCount=1;
                } else {
                    repeatCount++;
                }
                if ( repeatCount>50 ) {
                    return currentFirstRecord;
                }
                line = reader.readLine();
            }
        } finally {
            if ( reader!=null ) reader.close();
        }

        return currentFirstRecord;
    }

    /**
     * read in records, allowing for a header of non-records before
     * guessing the delim parser.  This will return a reference to the
     * DelimParser and set skipLines.  DelimParser header field is set as well.
     * @param filename
     * @return the record parser to use, or null if no records are found.
     * @throws java.io.IOException
     */
    public DelimParser guessSkipAndDelimParser( String filename ) throws IOException {
        BufferedReader reader = null;
        DelimParser result= null;

        try {
            reader = new BufferedReader( new FileReader(filename) );

            String line;
            String lastLine = null;

            line = reader.readLine();
            int iline = 0;

            headerBuffer= new StringBuffer();

            while (line != null && isHeader(iline, lastLine, line, 0)) {
                lastLine = line;
                if ( iline<HEADER_LENGTH_LIMIT ) {
                   headerBuffer.append(line).append("\n");
                }
                line = reader.readLine();
                iline++;
            }

            if ( line==null ) return null;

            DelimParser p= guessDelimParser(line,iline);

            List<String> lines= new LinkedList<>();

            int parseCount=0;

            while ( iline<HEADER_LENGTH_LIMIT && line != null && parseCount<5 ) {
                lines.add(line);
                line = reader.readLine();
                iline++;
                while ( lines.size()>10 ) {
                    lines.remove(0);
                }
                if ( line!=null ) {
                    p= guessDelimParser(line,iline);
                    p.showException= false;
                    parseCount= p.tryParseRecord(line, iline, null) ? 1 : 0;
                    for (String line1 : lines) {
                        if (p.tryParseRecord(line1, 0, null)) {
                            parseCount++;
                        } else if (iline==2) {
                            String[] ff= p.fields(line);
                            for ( int j=0; j<ff.length; j++ ) {
                                if ( TimeParser.isIso8601String(ff[j]) ) {
                                    setUnits(j,UNIT_UTC);
                                }
                            }
                            if (p.tryParseRecord(line1, 0, null)) {
                                parseCount++; 
                            }
                        }
                    }
                }
            }
            
            result= p;

            for (String line1 : lines) {
                if (p.fieldCount(line1) == p.fieldCount()) {
                    line = line1;
                    Units[] u= new Units[this.units.length];
                    System.arraycopy( this.units, 0, u, 0, this.units.length );
                    result = createDelimParser(line1, p.getDelim(), -1); // set column names
                    System.arraycopy( u, 0, this.units, 0, this.units.length );
                    break;
                }
            }
                      
            if ( line==null ) return null;
            
            // check for ISO8601 times.
            String[] fields= new String[result.fieldCount];
            if ( result.splitRecord(line, fields ) ) {
                for ( int i=0; i<fields.length; i++ ) {
                    if ( isIso8601Time(fields[i]) ) {
                        this.setFieldParser( i, UNITS_PARSER );
                        this.setUnits( i,Units.cdfTT2000 );
                    }
                }
            }
            
        } finally {
            if ( reader!=null ) reader.close();
        }

        String header= headerBuffer.toString();
        // look for rich headers, and update the column names.
        boolean isRichHeader= isRichHeader(header);
        if ( isRichHeader ) {
            try {
                int ii= header.indexOf("\n"); // check for lost header new line delimiter.
                if ( ii>0 ) {
                    String ss= header.substring(0,ii);
                    if ( ss.split("\\#").length>2 ) {
                        throw new IllegalArgumentException("rich header cannot contain more than two hashes (#) on the first line.  Maybe newlines were unintentionally removed");
                    }
                }

                bundleDescriptor = AsciiHeadersParser.parseMetadata(header, fieldNames, fieldLabels );
                if ( bundleDescriptor.length()==fieldNames.length ) {
                    for ( int j=0; j<bundleDescriptor.length(); j++ ) {
                        String n= (String)bundleDescriptor.property(QDataSet.NAME,j);
                        if ( n!=null ) fieldNames[j]= n;
                        n= (String)bundleDescriptor.property(QDataSet.LABEL,j);
                        if ( n!=null ) fieldLabels[j]= n;
                    }
                } else {
                    System.err.println( String.format("rich header buffer not the same length as the dataset (%d!=%d)", bundleDescriptor.length(), fieldNames.length) );
                }
            } catch (ParseException ex) {
                logger.log(Level.SEVERE, ex.getMessage(), ex);
            }
            result.header= header;
        }

        
        return result;
    }
    
    public DelimParser guessDelimParser(String line) throws IOException {
        return guessDelimParser( line, -1 );
    }

    /**
     * read in the first record, then guess the delimiter and possibly the column headers.
     * @param line a single record to attempt parsing.
     * @param lineNumber, useful for debugging.
     * @return RecordParser object that can be queried. 
     * @throws java.io.IOException
     */
    public DelimParser guessDelimParser(String line, int lineNumber ) throws IOException {

        String fieldSep;

        int tabDelimFieldCount= line.split("\t",-2 ).length;
        int semiColonDelimFieldCount= line.split(";",-2 ).length;
        int commaDelimFieldCount= line.split( ",",-2 ).length;
        int whitespaceDelimFieldCount= line.split("\\s+",-2 ).length;

        if ( semiColonDelimFieldCount > 1 && semiColonDelimFieldCount>=whitespaceDelimFieldCount/2 ) {
            fieldSep = ";";
        } else if ( tabDelimFieldCount > 1 && tabDelimFieldCount!=whitespaceDelimFieldCount ) {  // always use tabs over others, but only if other doesn't work
            fieldSep = "\t";
        } else if ( commaDelimFieldCount > 1 && commaDelimFieldCount>= whitespaceDelimFieldCount/2 ) { //TODO: improve this
            fieldSep = ",";
        } else {
            fieldSep = "\\s+";
        }

        DelimParser result = createDelimParser(line, fieldSep, lineNumber);
        this.recordParser = result;

        return result;

    }

    /**
     * The DelimParser splits each record into fields using a delimiter like ","
     * or "\\s+".
     *
     * @param filename filename to read in.
     * @param delim the delimiter, such as "," or "\t" or "\s+"
     * @return the record parser that will split each line into fields
     * @throws java.io.IOException
     */
    public DelimParser setDelimParser(String filename, String delim) throws IOException {
        FileReader r= null;
        DelimParser result=null;
        try {
            r= new FileReader(filename);
            result= setDelimParser(r, delim);
        } finally {
            if ( r!=null ) r.close();
        }
        return result;
    }

    /**
     * The DelimParser splits each record into fields using a delimiter like ","
     * or "\\s+".
     * @param in
     * @param delimRegex the delimiter, such as "," or "\t" or "\s+"
     * @return the record parser that will split each line into fields
     * @throws java.io.IOException
     */
    public DelimParser setDelimParser(Reader in, String delimRegex) throws IOException {

        String line;
        try (BufferedReader reader = new LineNumberReader(in)) {
            line = readFirstRecord(reader);
        }

        DelimParser result = createDelimParser(line, delimRegex, -1);
        this.recordParser = result;

        return result;

    }

    /**
     * The regex parser is a slow parser, but gives precise control.
     * @param fieldNames
     * @return the parser for each record.
     */
    public final RecordParser setRegexParser(String[] fieldNames) {
        initializeByFieldCount(fieldNames.length);
        this.fieldNames = Arrays.copyOf( fieldNames, fieldNames.length );

        StringBuilder regexBuf = new StringBuilder();
        regexBuf.append("\\s*");
        for (int i = 0; i < fieldCount - 1; i++) {
            regexBuf.append("(" + decimalRegex + ")[\\s+,+]\\s*");
        }

        regexBuf.append("(" + decimalRegex + ")\\s*");

        recordParser = new RegexParser(regexBuf.toString());
        return recordParser;
    }

    /**
     * looks at the first line after skipping, and splits it to calculate where 
     * the columns are.  The FixedColumnsParser is the fastest of the three parsers. 
     * 
     * @param filename filename to read in.
     * @param delim regex to split the initial line into the fixed columns.
     * @return the record parser that will split each line. 
     * @throws java.io.IOException
     */
    public FixedColumnsParser setFixedColumnsParser(String filename, String delim) throws IOException {
        Reader r=null;
        FixedColumnsParser result;
        try {
            r= new FileReader(filename);
            result= setFixedColumnsParser( r, delim);
        } finally {
            if ( r!=null ) r.close();
        }
        return result;
    }

    /**
     * looks at the first line after skipping, and splits it to calculate where 
     * the columns are.  
     * 
     * @param in the Reader to get lines from.
     * @param delim regex to split the initial line into the fixed columns.
     * @return the record parser that will split each line. 
     * @throws java.io.IOException
     */
    public FixedColumnsParser setFixedColumnsParser(Reader in, String delim) throws IOException {
        String line;
        int lineNumber;
        try (LineNumberReader reader = new LineNumberReader(in)) {
            line = readFirstRecord(reader);
            lineNumber= reader.getLineNumber();
        }

        int[] columnOffsets;
        int[] columnWidths;

        int col = 0;

        String[] ss = line.split(delim);
        columnOffsets = new int[ss.length];
        columnWidths = new int[ss.length - 1];
        
        initializeByFieldCount(ss.length);
        initializeUnitsByGuessing(ss,lineNumber);

        boolean rightJustified = false;
        if (ss[0].trim().length() == 0) {
            rightJustified = true;
            for (int i = 0; i < ss.length - 1; i++) {
                ss[i] = ss[i + 1];
            }
        }

        columnOffsets[0] = 0;

        if (rightJustified) {
            for (int i = 1; i < ss.length; i++) {
                col = line.indexOf(ss[i - 1], columnOffsets[i - 1]);
                columnOffsets[i] = col + ss[i - 1].length();
                columnWidths[i - 1] = columnOffsets[i] - columnOffsets[i - 1];
            }
        } else {
            for (int i = 1; i < ss.length; i++) {
                col = line.indexOf(ss[i], col + ss[i - 1].length()); // account for whitespace
                columnOffsets[i] = col;
                columnWidths[i - 1] = columnOffsets[i] - columnOffsets[i - 1];
            }
        }

        int[] co = new int[columnWidths.length];
        System.arraycopy(columnOffsets, 0, co, 0, columnWidths.length);

        FixedColumnsParser p = new FixedColumnsParser(co, columnWidths);
        this.recordParser = p;

        this.propertyPattern = null;

        return p;
    }

    /**
     * return the field count that would result in the largest number of records parsed.  The
     * entire file is scanned, and for each line the number of decimal fields is counted.  At the end
     * of the scan, the fieldCount with the highest record count is returned.
     * @param filename the file name, a local file opened with a FileReader
     * @return the apparent field count.
     * @throws java.io.FileNotFoundException
     */
    public static int guessFieldCount(String filename) throws FileNotFoundException, IOException {

        final int maxFieldCount = 10;  // can only identify maxFieldCount - 1.

        int[] recCount = new int[maxFieldCount];

        StringBuilder regexBuf = new StringBuilder();
        regexBuf.append("\\s*(" + decimalRegex + ")");
        for (int i = 1; i < maxFieldCount; i++) {
            regexBuf.append("([\\s+,+]\\s*(" + decimalRegex + "))?");
        }
        regexBuf.append("\\s*");

        Pattern pat = Pattern.compile(regexBuf.toString());

        try (BufferedReader reader = new LineNumberReader(new FileReader(filename))) {

            String line;
            while ((line = reader.readLine()) != null) {
                Matcher m = pat.matcher(line);
                if (m.matches()) {
                    int j;
                    for (j = 1; j < m.groupCount(); j += 2) {
                        if (m.group(j) == null) {
                            recCount[(j - 1) / 2]++;
                            break;
                        }
                    }
                }
            }
        }
        int max = 0;
        int imax = 0;
        for (int j = 1; j < maxFieldCount; j++) {
            if (recCount[j] > max) {
                imax = j;
                max = recCount[j];
            }
        }

        return imax;
    }

    /**
     * set the special parser for a field.
     * @param field the field number, 0 is the first column.
     * @param fp the parser
     */
    public void setFieldParser(int field, FieldParser fp) {
        FieldParser oldFp = this.fieldParsers[field];
        this.fieldParsers[field] = fp;
        if (fp == UNITS_PARSER && UnitsUtil.isTimeLocation(units[field])) {
            setPropertyPattern(null);
        }
        propertyChangeSupport.firePropertyChange(PROPERTY_FIELD_PARSER, oldFp, fp);
    }

    /**
     * creates a parser with @param fieldCount fields, named "field0,...,fieldN"
     * @param fieldCount the number of fields
     * @return the file parser
     */
    public static AsciiParser newParser(int fieldCount) {
        String[] fieldNames = new String[fieldCount];
        for (int i = 0; i < fieldCount; i++) {
            fieldNames[i] = "field" + i;
        }
        return new AsciiParser(fieldNames);
    }

    /**
     * creates a parser with the named fields.
     * @param fieldNames the names for each field
     * @return the file parser
     */
    public static AsciiParser newParser(String[] fieldNames) {
        return new AsciiParser(fieldNames);
    }

    /**
     * skip a number of lines before trying to parse anything.  This can be
     * set to point at the first valid line, and the RecordParser will be 
     * configured using that line.
     * @param skipLines
     */
    public void setSkipLines(int skipLines) {
        this.skipLines = skipLines;
    }

    /**
     * limit the number of records read.  parsing will stop once this number of
     * records is read.  This is Integer.MAX_VALUE by default.
     * @param recordCountLimit
     */
    public void setRecordCountLimit(int recordCountLimit) {
        this.recordCountLimit = recordCountLimit;
    }

    /**
     * specify the Pattern used to recognize properties.  Note property
     * values are not parsed, they are provided as Strings.  This is a regular
     * expression with two groups for the property name and value.
     * For example, (.+)=(.+)
     * @param propertyPattern regular expression Pattern with two groups.
     */
    public void setPropertyPattern(Pattern propertyPattern) {
        this.propertyPattern = propertyPattern;
    }

    /**
     * Records starting with this are not processed as data, for example "#".
     * This is initially "#".  Setting this to null disables this check.
     * @param comment the prefix
     */
    public void setCommentPrefix(String comment) {
        this.commentPrefix = comment;
    }
    protected String headerDelimiter = null;
    public static final String PROP_HEADERDELIMITER = "headerDelimiter";

    /**
     * get the header delimiter
     * @return the header delimiter.
     */
    public String getHeaderDelimiter() {
        return headerDelimiter;
    }

    /**
     * set the delimiter which explicitly separates header from the data.
     * For example "-------" could be used.  Normally the parser just looks at
     * the number of fields and this is sufficient.
     * @param headerDelimiter 
     */
    public void setHeaderDelimiter(String headerDelimiter) {
        String oldHeaderDelimiter = this.headerDelimiter;
        this.headerDelimiter = headerDelimiter;
        propertyChangeSupport.firePropertyChange(PROP_HEADERDELIMITER, oldHeaderDelimiter, headerDelimiter);
    }

    /**
     * Parse the stream using the current settings.
     * @param in the input stream
     * @param mon
     * @return 
     * @throws java.io.IOException
     */
    public WritableDataSet readStream(Reader in, ProgressMonitor mon) throws IOException {
        return readStream(in, null, mon);
    }

    /**
     * read in the stream, including the first record if non-null.
     * @param in the stream, which is not closed.
     * @param firstRecord, if non-null, parse this record first.  This allows information to be extracted about the
     * records, then fed into this loop.
     * @param mon a monitor
     * @return the dataset.
     * @throws java.io.IOException
     */
    public WritableDataSet readStream(Reader in, String firstRecord, ProgressMonitor mon) throws IOException {
        BufferedReader reader = new BufferedReader(in);
        String line = null;
        String lastLine;

        int iline = -1;
        int irec = 0;

        mon.started();

        DataSetBuilder builder = new DataSetBuilder(2, 100, recordParser.fieldCount());
        builder.setFillValue(fillValue);
        builder.setValidMax(validMax);
        builder.setValidMin(validMin);

        long bytesRead = 0;

        headerBuffer = new StringBuffer();
        
        //int skipInt = skipLines + (skipColumnHeader ? 1 : 0);

        lastLine = line;
        if (firstRecord != null) {
            line = firstRecord;
            firstRecord = null;
        } else {
            line = reader.readLine();
        }

        boolean parsedMeta= false;

        boolean acceptRecord= true; // true if the record meets where condition.
        
        while (line != null) {
            bytesRead += line.length() + 1; // +1 is for end-of-line
            iline++;

            if (irec == this.recordCountLimit || mon.isCancelled()) {
                break;
            }

            mon.setTaskProgress(bytesRead);
            if (iline % 100 == 0) { // decimate to avoid excessive String work
                mon.setProgressMessage("reading line " + iline);
            }

            if (isHeader(iline, lastLine, line, irec)) {
                if ((commentPrefix != null && line.startsWith(commentPrefix))) {
                    line = line.substring(commentPrefix.length());
                }
                if (keepFileHeader && iline<HEADER_LENGTH_LIMIT) {
                    headerBuffer.append(line).append("\n");
                }
                //properties will be picked up in post-processing
                
            } else {

                if ( parsedMeta==false ) { // this will attempt to parse the header to get units for parsing data.
                    String header = headerBuffer.toString();
                    parseMeta( header, builder );
                    if ( !isRichHeader( header ) ) {
                        builder.putProperty(PROPERTY_FILE_HEADER, header);
                    }
                    parsedMeta= true;
                }

                try {

                    if (firstRecord == null) {
                        int nonAsciiCount= 0;
                        if ( line.length()>3 ) {
                            for ( int i=0; i<line.length(); i++ ) {
                                char ch= line.charAt(i);
                                if ( ( ch<32 || ch>126 ) && ch!=9 ) nonAsciiCount++;
                            }
                            if ( nonAsciiCount>20 || nonAsciiCount*100/line.length()>20 ) {
                                throw new IOException("stream does not appear to be ascii");
                            }
                        }
                        firstRecord = line.length()>132 ? ( line.substring(0,132)+"..." ) : line;
                        builder.putProperty(PROPERTY_FIRST_RECORD, firstRecord);
                    }

                    // *** here's where we parse each record ***
                    if (recordParser.tryParseRecord(line, irec, builder)) {
                        acceptRecord= true;
                        if ( whereParm!=null ) {
                            String[] fields= new String[recordParser.fieldCount()];
                            if ( recordParser.splitRecord(line,fields) ) {
                                String field= fields[iwhereParm].trim();
                                int icomp= whereComp.compare( field, whereValue );
                                acceptRecord= false;
                                if ( whereEq && icomp==0 ) {
                                    acceptRecord= true;
                                } else if ( whereNe && icomp!=0 ) {
                                    acceptRecord= true;
                                } else if ( whereSign==icomp ) {
                                    acceptRecord= true;
                                }
                            }
                            
                        }
                        if ( acceptRecord ) {
                            irec++;
                            builder.nextRecord();
                        }

                    } else {
                        //System.out.println(line);
                    }
                } catch (NumberFormatException e) {
                    logger.log(Level.SEVERE, e.getMessage(), e);
                }

            }

            lastLine = line;
            line = reader.readLine();
        }

        mon.finished();

        builder.putProperty(QDataSet.USER_PROPERTIES, new HashMap(builder.properties)); // put discovered properties into

        WritableDataSet result= builder.getDataSet();
        
        if ( acceptRecord==false ) {
            result= (WritableDataSet)result.trim(0,result.length()-1);
        }
        
        return result;
        
    }

    /**
     * return true if the header appears to contain JSON code which could be
     * interpreted as a "Rich Header" (a.k.a. JSONHeadedASCII).  This is 
     * a very simple test, simply looking for <code>#{</code> and <code>#}</code>
     * with a colon contained within.
     * @see https://github.com/JSONheadedASCII/examples
     * @param header string containing the commented header.
     * @return true if parsing as a Rich Header should be attempted.
     */
    public static boolean isRichHeader( String header ) {
        if ( header.length()==0 ) return false;
        String hash= header.charAt(0)=='#' ? "\\#" : "";  // we might have popped off all the comment prefixes (#).
        
        Pattern p= Pattern.compile(hash+"\\s*\\{");
        Matcher m= p.matcher(header);
        if ( m.find() ) {
            int istart= m.start();
            int iend= m.end();
            p= Pattern.compile(hash+".*\\}");
            m= p.matcher(header);
            if ( m.find( iend ) ) {
                iend= m.end();
                String jsonSrc= header.substring(istart,iend);
                return jsonSrc.contains(":");
            }
        }
        return false;
    }

    /**
     * attempt to parse the metadata in the headers.  If the header contains
     * a pair of braces {}, then we assume it's a special JSON-formatted header
     * with QDataSet metadata for the UNITS and LABELs.  If not, then we
     * just look for name/value pairs as specified by the propertyPattern.
     *
     * In the JSON case, we form a bundle descriptor (a special QDataSet) which
     * contains the properties for each bundled dataset.  For the default case,
     * we assign the values to the USER_PROPERTIES.
     * @param header
     * @param builder
     */
    private void parseMeta( String header, DataSetBuilder builder ) {

        boolean doJSON= isRichHeader(header); 
        
        if ( doJSON ) {
            try {
                //System.err.println( "== JSON Header == \n"+header );
                logger.fine("Parsing Rich JSON Header...");
                bundleDescriptor = AsciiHeadersParser.parseMetadata(header, getFieldNames(), getFieldLabels() );
                builder.putProperty( QDataSet.BUNDLE_1, bundleDescriptor );
                bundleDescriptor.property(QDataSet.LABEL, 1);
                //move dimensionless properties to the dataset.
                Map<String,Object> props= DataSetUtil.getProperties( bundleDescriptor, DataSetUtil.globalProperties(), null );
                for ( Entry<String,Object> e: props.entrySet() ) {
                    String k= e.getKey();
                    builder.putProperty( k, e.getValue() );
                }

                for ( int j=0; j<bundleDescriptor.length(); j++ ) {
                    Units u= (Units) bundleDescriptor.property( QDataSet.UNITS, j );
                    if ( u!=null ) {
                        this.fieldParsers[j]= UNITS_PARSER;
                        this.units[j]= u;
                    }
                }
                if ( bundleDescriptor.length()!=this.fieldParsers.length ) {
                    logger.warning("lengths check didn't work out");
                }

            } catch (ParseException ex) {
                logger.log(Level.SEVERE, ex.getMessage(), ex);
                if ( propertyPattern!=null ) {
                Map<String,String> userProps= new LinkedHashMap();
                for ( String line2: header.split("\n") ) {
                    Matcher m2= propertyPattern.matcher(line2);
                    if ( m2.matches() ) {
                        userProps.put( m2.group(1).trim(), m2.group(2).trim() );
                    }
                }
                builder.putProperty( QDataSet.USER_PROPERTIES, userProps );
            }
            }
        } else {
            if ( propertyPattern!=null ) {
                Map<String,String> userProps= new LinkedHashMap();
                for ( String line2: header.split("\n") ) {
                    Matcher m2= propertyPattern.matcher(line2);
                    if ( m2.matches() ) {
                        userProps.put( m2.group(1).trim(), m2.group(2).trim() );
                    }
                }
                builder.putProperty( QDataSet.USER_PROPERTIES, userProps );
            }
        }

    }

    /**
     * returns the high rank rich fields in a map from NAME to LABEL.
     * NAME:&gt;fieldX&lt;  or NAME:&gt;fieldX-fieldY&lt;
     * @return the high rank rich fields in a map from NAME to LABEL.
     */
    public Map<String,String> getRichFields() {
        LinkedHashMap<String,String> result= new LinkedHashMap<>();
        if ( bundleDescriptor!=null ) {
            for ( int i=0; i<bundleDescriptor.length(); i++ ) {
                String name= (String) bundleDescriptor.property( QDataSet.ELEMENT_NAME, i);
                if ( name!=null && !result.containsKey(name) ) {
                    String label= (String) bundleDescriptor.property( QDataSet.ELEMENT_LABEL, i);
                    int rank= bundleDescriptor.length( i );
                    int len=0;
                    if ( rank>0 ) {
                        len=1;
                        for ( int j=0; j<rank; j++  ) {
                            len= len*(int)bundleDescriptor.value(i,j);
                        }
                    }
                    if ( len==0 ) {
                        result.put(name+": field"+i,label);
                    } else {
                        result.put(name+": field"+i+"-field"+(i+len-1),label);
                        i=i+len-1;
                    }
                }
            }
        }
        return result;
    }

    String whereParm= null;
    int iwhereParm= -1;
    boolean whereEq= false;
    boolean whereNe= false;
    int whereSign= 0; // zero means don't allow gt or lt
    String whereValue= null;
    Datum dwhereValue= null;
    DatumRange dwhereWithin= null;
    
    private Comparator whereComp= new Comparator() {
        @Override
        public int compare(Object o1, Object o2) {
            if ( o1.equals(o2) ) {
                return 0;
            } else {
                Datum d;
                try {
                    d= units[iwhereParm].parse((String)o1);
                } catch ( ParseException ex ) {
                    return 1-whereSign; // don't accept this value
                }
                if ( dwhereValue!=null ) {
                    try {
                        return d.compareTo(dwhereValue);
                    } catch ( InconvertibleUnitsException ex ) {
                        if ( UnitsUtil.isRatioMeasurement(units[iwhereParm]) ) {
                            dwhereValue= Datum.create( dwhereValue.value(),units[iwhereParm] );
                            return d.compareTo(dwhereValue);
                        } else {
                            throw ex;
                        }
                    }
                } else if ( dwhereWithin!=null ) {
                    try {
                        return dwhereWithin.contains(d) ? 1 : -1;
                    } catch ( InconvertibleUnitsException ex ) {
                        if ( UnitsUtil.isRatioMeasurement(units[iwhereParm]) ) {
                            dwhereWithin= DatumRange.newDatumRange( dwhereWithin.min().value(), dwhereWithin.max().value(), units[iwhereParm] );
                            return dwhereWithin.contains(d) ? 1 : -1;
                        } else {
                            throw ex;
                        }
                    }
                    
                } else {
                    return 1-whereSign; // don't accept this value
                }
            }
        }
    };
    
    /**
     * allow constraint for where condition is true.  This doesn't 
     * need the data to be interpreted for "eq", string equality is checked
     * for nominal data.  Note sval is compared after trimming outside spaces.
     * @param sparm column name, such as "field4"
     * @param op constraint, one of eq gt ge lt le ne
     * @param sval String value.  For nominal columns, String equality is used.
     */
    public void setWhereConstraint(String sparm, String op, String sval) {
        this.whereParm= sparm;
        this.iwhereParm= getFieldIndex(whereParm);
        if ( iwhereParm==-1 ) {
            throw new IllegalArgumentException("no such column: "+sparm);
        }
        switch (op) {
            case "eq":
                this.whereSign= 0;
                this.whereEq= true;
                this.whereNe= false;
                break;
            case "ne":
                this.whereSign= 0;
                this.whereEq= false;
                this.whereNe= true;
                break;
            case "gt":
                this.whereSign= 1;
                this.whereEq= false;
                this.whereNe= false;
                break;
            case "ge":
                this.whereSign= 1;
                this.whereEq= true;
                this.whereNe= false;
                break;
            case "lt":
                this.whereSign= -1;
                this.whereEq= false;
                this.whereNe= false;
                break;
            case "le":
                this.whereSign= -1;
                this.whereEq= true;
                this.whereNe= false;
                break;
            case "within":
                this.whereSign= 1;
                this.whereEq= true;
                this.whereNe= false;
                break;
            case "matches":
                this.whereSign= 0;
                this.whereEq= true;
                this.whereNe= false;
                final Pattern p= Pattern.compile(sval);
                this.whereComp= new Comparator() {
                    @Override
                    public int compare(Object o1, Object o2) {
                        String s1= o1.toString();
                        if ( p.matcher(s1).matches() ) {
                            return 0;
                        } else {
                            return 1;
                        }
                    }
                };
                break;
            default:
                throw new IllegalArgumentException("where constraint not supported: "+op);
        }
        this.whereValue= sval.trim();
        this.dwhereValue= null;
        if ( UnitsUtil.isOrdinalMeasurement(units[iwhereParm]) ) {
            logger.log( Level.FINE, "column {0} is ordinal data", sparm);
            // we just do string comparisons in this case.
        } else {
            try {
                if ( op.equals("within") ) {
                    this.dwhereWithin= DatumRangeUtil.parseDatumRange( this.whereValue, units[iwhereParm] );
                } else {
                    this.dwhereValue= units[iwhereParm].parse( this.whereValue );
                }
            } catch (ParseException ex) {
                logger.log( Level.FINE, "sval is not parseable, assuming it is ordinal data");
            }
        }
    }

    public static interface RecordParser {

        /**
         * returns true if the line appears to be a record.  If it is a record,
         * then the record is inserted into the builder.
         * @param line the line from the file.
         * @param irec the record number
         * @param builder the builder into which the data is inserted.
         * @return true if the line appeared to be a record.
         */
        boolean tryParseRecord(String line, int irec, DataSetBuilder builder);

        /**
         * indicate the number of fields this RecordParser is 
         * expecting on each line.
         * @return the field count.
         */
        int fieldCount();

        /**
         * return the number of fields in this line.  All records will have this
         * number of fields.  This is used for discovery and to configure the parser.
         * @param line the line from the file, to attempt parsing.
         * @return the number of fields found.
         */
        int fieldCount(String line);

        /**
         * attempts to extract fields from the record, returning true if
         * the record could be split.
         * @param line the line from the file.
         * @param fields array to store the fields.  fieldCount() should be used
         * to determine the length of the array.
         * @return true if the line is a record that can be split into fields.
         */
        boolean splitRecord( String line, String[] fields );
    }

    /**
     * A FieldParser takes character data and returns a number representing
     * the data.  The units of the field are often used with this when parsing.
     */
    public static interface FieldParser {
        /**
         * parse the field into a double representing
         * @param field the field
         * @param columnIndex the column index.
         * @return the double representing
         * @throws ParseException 
         */
        double parseField(String field, int columnIndex) throws ParseException;
    }
    
    /**
     * parses the field using Double.parseDouble, Java's double parser.
     */
    public static final FieldParser DOUBLE_PARSER = new FieldParser() {
        @Override
        public final double parseField(String field, int columnIndex) {
            if ( field.length()==1 ) {
                double d= field.charAt(0)-'0';  // bugfix '+' caused exception http://www.dartmouth.edu/~rdenton/Data/DentonTakahashiGOES1980-1991MassDensityWithHeader.txt?skipLines=91&depend0=FractionlYear&column=AE
                return ( d<0 || d>9 ) ? Double.NaN : d;
            } else {
                return Double.parseDouble(field);
            }
        }
        @Override
        public String toString() {
            return "doubleParser";
        }            
    };
    
    /**
     * delegates to the unit object set for this field to parse the data.
     */
    public final FieldParser UNITS_PARSER = new FieldParser() {
        @Override
        public final double parseField(String field, int columnIndex) throws ParseException {
            Units u = AsciiParser.this.units[columnIndex];
            if ( u instanceof EnumerationUnits ) {
                field= field.trim();
                if ( field.startsWith("\"") && field.endsWith("\"") ) {
                    return u.parse(field.substring(1,field.length()-1)).doubleValue(u);
                }
            }
            return u.parse(field).doubleValue(u);
        }
        @Override
        public String toString() {
            return "unitsParser";
        }
    };

    /**
     * uses the EnumerationUnits for the field to create a Datum.
     */
    public final FieldParser ENUMERATION_PARSER = new FieldParser() {
        @Override
        public final double parseField(String field, int columnIndex) throws ParseException {
            EnumerationUnits u = (EnumerationUnits)AsciiParser.this.units[columnIndex];
            if ( u==null ) throw new NullPointerException("ENUMERATION_PARSER used where we don't have a unit, at columnIndex="+columnIndex );
            field= field.trim();
            if ( field.startsWith("\"") && field.endsWith("\"") ) {
                return u.parse(field.substring(1,field.length()-1)).doubleValue(u); // TODO: untested..
            } else {
                try {
                    Datum d= u.createDatum(field); // rte_2038937185 that Ivar sees.
                    return d.doubleValue(u);            
                } catch ( NullPointerException ex ) {
                    throw ex; // w/Ivar
                }
            }
        }
        @Override
        public String toString() {
            return "enumerationParser";
        }        
    };

    /**
     * hide the nuances of Java's split function.  When the string endswith the
     * regex, add an empty field.  Also, trim the string so leading and trailing
     * whitespace is not treated as a delimiter.
     * @param string
     * @param regex regular expression like \\s+
     * @return string array containing the fields.
     */
    private static String[] split(String string, String regex) {
        String[] ss = string.trim().split(regex);
        if (string.endsWith(regex)) {
            String[] ss1 = new String[ss.length + 1];
            System.arraycopy(ss, 0, ss1, 0, ss.length);
            ss1[ss1.length - 1] = "";
            ss = ss1;
        }
        return ss;
    }


    /**
     * create a delimeter-based record parser by splitting the line and counting
     * the number of fields.  If the line appears to contain column headings,
     * then column names will be set as well.
     * This has the side effect of turning off property record pattern.
     * Trailing and leading whitespace is ignored.
     * @param line
     * @param fieldSep
     * @param lineNum the line number, 1 is first line.
     * @return
     */
    private DelimParser createDelimParser(String line, String fieldSep, int lineNum) {

        String[] ss = split(line.trim(), fieldSep);

        initializeByFieldCount(ss.length);
        initializeUnitsByGuessing(ss,lineNum);
        
        fieldLabels= new String[fieldCount];
        fieldUnits= new String[fieldCount];
        
        boolean isColumnHeaders = true;
        for (int i = 0; i < ss.length; i++) {
            Matcher m;
            if ((m = COLUMN_ID_HEADER_PATTERN.matcher(ss[i])).matches()) {
                String n= m.group(1).trim();
                if ( n.length()!=3 || !n.equalsIgnoreCase("nan") ) {
                    fieldLabels[i] = n;
                    fieldNames[i] = Ops.safeName( fieldLabels[i] );
                    fieldUnits[i]= m.group(3);
                    if (fieldUnits[i]!=null) {
                        fieldUnits[i]= fieldUnits[i].trim();
                        if ( fieldUnits[i].length()>2 ) {
                            char ch= fieldUnits[i].charAt(0);
                            if ( !Character.isLetter(ch) ) {
                                // this can't be turned into a unit, so just tack this on to the label.
                                fieldLabels[i]= fieldLabels[i] + m.group(2);
                                fieldUnits[i]= null;
                            }
                        }
                    }
                } else {
                    if (isColumnHeaders) {
                        logger.log(Level.FINEST, "parsed line appears to contain NaN''s, and is not a column header because of field #{0}: {1}", new Object[]{i, ss[i]});
                    }
                    isColumnHeaders = false;
                }
            } else if ((m=COLUMN_CHANNEL_HEADER_PATTERN.matcher(ss[i])).matches() && m.group(3).length()>0 && m.group(5).length()>0 ) {
                String n= m.group(1).trim();
                fieldLabels[i] = n;
                if ( m.group(2).length()>0 ) { // make valid java identifier
                    fieldNames[i] = n.replaceAll("-", "_");
                } else {
                    fieldNames[i] = "ch_"+n.replaceAll("-", "_");
                }
                fieldUnits[i]= null;
            } else {
                if (isColumnHeaders) {
                    logger.log(Level.FINEST, "first parsed line does not appear to be column header because of field #{0}: {1}", new Object[]{i, ss[i]});
                }
                isColumnHeaders = false;
            }
        }

        if (!isColumnHeaders) {
            for (int i = 0; i < fieldCount; i++) {
                if (fieldNames[i] == null) {
                    fieldNames[i] = "field" + i;
                }
            }
            //TODO: this will clean up cases where we get extraneous headers.
//            int fieldNameCount=0;
//            for (int i = 0; i < fieldCount; i++) {
//                if (fieldNames[i] != null) {
//                    fieldNameCount++;
//                }
//            }
//            if ( fieldNameCount<fieldCount) { // all or none
//                for ( int i=0; i<fieldCount; i++ ) {
//                    fieldNames[i]= "field" + i;
//                }
//            }
//                                    
        }

        DelimParser recordParser1 = new DelimParser(fieldParsers.length, fieldSep);

        this.propertyPattern = null;

        return recordParser1;

    }

    /**
     * DelimParser splits the line on a regex (like "," or "\\s+") to create the fields.
     * Trailing and leading whitespace is ignored.
     */
    public final class DelimParser implements RecordParser {

        int fieldCount;
        String delimRegex;
        Pattern delimPattern;
        boolean[] doParseField;
        public String header=null; // place to store the header.
        boolean showException= true;

        public DelimParser(int fieldCount, String delim) {
            this.fieldCount = fieldCount;
            this.doParseField = new boolean[fieldCount];
            for (int i = 0; i < fieldCount; i++) {
                this.doParseField[i] = true;
            }
            this.delimRegex = delim;
            this.delimPattern = Pattern.compile(delim);
            this.header= ""; // call guessParser to set
        }

        /**
         * returns the delimiter, which is a regex.  Examples include "," "\t", and "\s+"
         * @return
         */
        public String getDelim() {
            return delimRegex;
        }

        public void setShowException( boolean s ) {
            this.showException= s;
        }
        
        @Override
        public boolean tryParseRecord(String line, int irec, DataSetBuilder builder) {
            int j;
            int okayCount = 0;
            int failCount = 0;
            int tryCount= 0;

            if ( fieldCount!=fieldParsers.length ) {
                return false; //TODO: how do we get into this condition?
            } 
            String[] ss = new String[fieldCount];
            if ( !splitRecord(line, ss ) ) {
                return false;
            }

            Exception firstException= null;
            for (j = 0; j < fieldCount; j++) {
                tryCount++;
                if (doParseField[j]) {
                    String parseable = ss[j];
                    try {
                        double d= fieldParsers[j].parseField(parseable, j);
                        if ( builder!=null ) builder.putValue(irec, j, d );
                        okayCount++;
                    } catch (ParseException | NumberFormatException e) {
                        if ( firstException==null ) firstException= e;
                        failCount++;
                        if ( builder!=null ) builder.putValue(irec, j, -1e31 ); //TODO
                    }
                    //TODO
                    
                }
            }
            if ( firstException!=null && failCount>0 && failCount<fieldCount ) {
                if ( showException ) {
                    firstException.printStackTrace();
                    showException= false;
                }
            }
            
            // the record is parsable if there are two or more parsable fields.
            // it is not parsable if no fields can be parsed.
            return ( failCount < tryCount ) && ( okayCount > 1 || failCount < 3 );
        }

        @Override
        public int fieldCount() {
            return fieldCount;
        }

        @Override
        public int fieldCount(String line) {
            return fields(line).length;
        }

        public void setSkipField(int ifield, boolean skip) {
            this.doParseField[ifield] = !skip;
        }

        /**
         * return the string for each field.  This is useful
         * for discovery, and is not used in the bulk parsing.
         * @param line
         * @return
         */
        private String[] fields(String line) {
            String[] many= new String[1000];
            splitRecord(line, many);
            int count=0;
            for ( int i=0; i<many.length; i++ ) {
                if ( many[i]==null ) {
                    count=i;
                    break;
                }
            }
            String[] ss= new String[count];
            System.arraycopy( many, 0, ss, 0, count );
            
            return ss;
        }

        @Override
        public boolean splitRecord(String input, String[] fields) {

            int index = 0;
            int ifield = 0;

            Matcher m = delimPattern.matcher(input);

            boolean tabDelim= delimPattern.pattern().equals("\t");

            char quote='"';
            int len= input.length();
            
            int index0= index;
            int qend=-1; // end of the quoted
            while ( ifield<fields.length && index<len ) {
                while ( index<len && ( !tabDelim && Character.isWhitespace( input.charAt(index) ) ) ) index++;
                if ( index==len ) break;
                int i1;
                if ( input.charAt(index)==quote ) { // find closing quote
                    index= index+1;
                    index0= index;
                    i1= input.indexOf(quote,index);
                    if ( i1==-1 ) {
                        System.err.println("unclosed quote: " + input);
                        continue;
                    }
                    while ( i1+1<input.length() && input.charAt(i1+1)==quote ) {
                        i1= input.indexOf(quote,i1+2);
                        if ( i1==-1 ) throw new IllegalArgumentException("unclosed quote");
                    }
                    index= i1+1;
                    qend= i1;

                    if ( index==len || ( ifield==fields.length-1 && input.substring(index).trim().length()==0 ) ) { // allow for trailing whitespace 
                        fields[ifield]= input.substring(index0,qend);
                        if ( ifield==fields.length-1 && input.substring(index).trim().length()==0 ) {
                            index=len;
                        }
                        ifield++;
                    }
                } else {
                    if ( m.find(index) ) {
                        if ( qend==-1 ) index0= index;
                        index= m.start();
                        if ( qend==-1 ) {
                            fields[ifield]= input.substring(index0, index);
                        } else if ( qend<index0 ) {
                            return false; //TODO: when does this happen?  http://www.dartmouth.edu/~rdenton/Data/DentonTakahashiGOES1980-1991MassDensityWithHeader.txt?skipLines=91&depend0=FractionlYear&column=AE
                        } else {
                            fields[ifield]= input.substring(index0, qend).replaceAll("\"\"", "\"");
                            qend=-1;
                        }
                        index= m.end();
                        index0= index;
                        ifield++;
                    } else if ( ifield==fields.length-1 ) {
                        if ( qend==-1 ) {
                            fields[ifield]= input.substring(index0);
                        } else {
                            fields[ifield]= input.substring(index0,qend);
                        }
                        ifield++;
                        index=len;
                    } else {
                        fields[ifield]= input.substring(index0); // support fields() function
                        return false;
                    }
                }
            }
            if ( index==len && ifield==fields.length-1 && !delimPattern.toString().equals(" ") ) { // check for empty field at the end of the record, as in "1991-01-01 00:03,1490.0,I,"
                fields[ifield]="";
                ifield++;
            }

            return ( ifield == fields.length && index==len ) ;
        }

        @Override
        public String toString() {
            return "AsciiParser.DelimParser: regex="+this.delimRegex;
        }
    }

    /**
     * convert F77 style to C style.
     * X3,I3,F8 -> 3x,3i,8f
     * Repeats are not supported.
     * @param format
     * @return 
     */
    private static String[] f77FormatToCFormat( String[] format ) {
        String[] ss= new String[format.length+1];
        for ( int i=1;i<ss.length;i++ ) {
            String field= format[i-1];
            if ( field.length()>1 ) {// I3 -> 3i;
                Pattern p= Pattern.compile( "(\\d*)(\\D)(\\d*).*");  // the .* is for F8.3 so the .3 is ignored
                Matcher m= p.matcher(field);
                if ( m.matches() ) {
                    String type= m.group(2);
                    int repeat= !m.group(1).equals("") ? Integer.parseInt(m.group(1)) : 1;
                    int len= !m.group(3).equals("") ? Integer.parseInt(m.group(3)) : -1;
                    if ( type.toLowerCase().equals("x") ) {
                        if ( len==-1 ) len= repeat; else len= repeat*len;
                        ss[i]= String.valueOf(len) + type;
                    } else {
                        if ( repeat!=1 ) {
                            throw new IllegalArgumentException("repeats are only allowed for X: "+field);
                        } else {
                            ss[i]= String.valueOf(len) + type;
                        }
                    }
                    
                } else {
                    throw new IllegalArgumentException("unable to parse: "+field);
                }
            } else {
                ss[i]= field;
            }
        }
        ss[0]="";
        return ss;
    }
    /**
     * Convert FORTRAN (F77) style format to C-style format specifiers.
     * @param format for example "%5d%5d%9f%s"
     * @return for example "d5,d5,f9,a"
     */
    public static String getRegexForFormat( String format ) {
        String[] ss= format.split("%");
        if ( ss.length==1 ) { // support $ as well as %, since % is not nice in URIs.
            String[] ss1= format.split("\\$");
            if ( ss1.length>1 ) ss= ss1;
        }
        if ( ss.length==1 ) {
            String[] ss2= format.split(","); //FORTRAN style <repeat>F<len>
            if ( ss2.length>1 ) {
                ss= f77FormatToCFormat( ss2 );
            }        
        }
        
//        int count= 0;
//        for (String s : ss) {
//            if (!s.toLowerCase().endsWith("x")) {
//                count++;
//            }
//        }
        //String[] fc = new String[count];
        int[] lengths = new int[ss.length];
        for (int i = 0; i < lengths.length; i++) {
            lengths[i] = -1; // -1 indicates not known, but we'll figure out as many as we can.

        }
        //String[] delim = new String[count + 1];

        StringBuilder build = new StringBuilder(100);
        //delim[0] = ss[0];

        //int ifield= 0;
        for (int i = 1; i < ss.length; i++) {
            int pp = 0;
            while (Character.isDigit(ss[i].charAt(pp)) || ss[i].charAt(pp) == '-') {
                pp++;
            }
            if (pp > 0) {
                lengths[i] = Integer.parseInt(ss[i].substring(0, pp));
            } else {
                lengths[i] = -1; // determine later by field type
            }

            logger.log( Level.FINE, "ss[i]={0}", ss[i] );

            String fci;
            if ( ss[i].toLowerCase().endsWith("x") ) {
                if ( lengths[i]==-1 ) {
                    fci= "\\s*\\S+";
                } else {
                    //fc[i]= "(" + "...................................................................".substring(0,lengths[i]) + ")";
                    fci= "" + ".{"+lengths[i]+"}";
                }                
            } else {
                if ( lengths[i]==-1 ) {
                    fci= "\\s*(\\S+)";
                } else {
                    //fc[i]= "(" + "...................................................................".substring(0,lengths[i]) + ")";
                    fci= "(" + ".{"+lengths[i]+"})";
                }
                //fc[ifield++]= fci;
            }

            build.append(fci);
            if ( lengths[i]==-1 ) build.append("\\s*");
            
        }

        String regex= build.toString();
        //System.err.println( "regex= "+ regex );

        return regex;
    }

    /**
     * see {@code private TimeParser(String formatString, Map<String,FieldHandler> fieldHandlers)</tt>},
     * which is very similar.<ul>
     * <li>"%5d%5d%9f%s"
     * <li>"d5,d5,f9,a"
     * </ul>
     * @param format
     * @return
     * @see org.das2.datum.TimeParser
     */
    public RegexParser getRegexParserForFormat(String format) {
        String regex= getRegexForFormat(format);
        RegexParser rp= new RegexParser(regex);
        setRecordParser(rp);
        return rp;
    }

    /**
     * This initializes the parser, setting:
     * <li>fieldCount
     * <li>fieldNames to "field"+i
     * <li>fieldParsers to DOUBLE_PARSER
     * <li>units to Units.dimensionless.
     * @param count 
     */
    private void initializeByFieldCount( int count ) {
        fieldCount= count;
        fieldNames = new String[fieldCount];
        fieldParsers = new FieldParser[fieldCount];
        for (int i = 0; i < fieldCount; i++) {
            fieldParsers[i] = DOUBLE_PARSER;
            fieldNames[i] = "field" + (i);
        }
        units = new Units[fieldCount]; //this is the one place where units array is initialized
        fieldUnits= new String[fieldCount];
        for (int i = 0; i < fieldCount; i++) {
            units[i] = Units.dimensionless;
            fieldUnits[i] = "";
        }
    }
    
    /**
     * initialize the units by guessing at each field.  This will
     * only switch between dimensionless and UTC times.
     * @param ss the fields.
     */
    private void initializeUnitsByGuessing( String[] ss, int lineNumber ) {
        logger.log(Level.FINE, "guess units at line {0}", lineNumber);
        for (int i = 0; i < ss.length; i++) {
            if ( isIso8601Time(ss[i].trim()) ) {
                units[i]= Units.t2000;
                fieldParsers[i]= UNITS_PARSER;
            } else {
                units[i] = Units.dimensionless;
                fieldParsers[i] = DOUBLE_PARSER;
            }        
        }
    }

    /**
     * parser uses a regular expression to match each record.
     */
    public final class RegexParser implements RecordParser {

        Pattern recordPattern;
        int fieldCount;

        protected RegexParser(String regex) {
            recordPattern = Pattern.compile(regex);
            initializeByFieldCount(recordPattern.matcher("").groupCount());
        }

        @Override
        public int fieldCount() {
            return fieldCount;
        }

        @Override
        public final boolean tryParseRecord(String line, int irec, DataSetBuilder builder) {
            Matcher m;
            if (recordPattern != null && (m = recordPattern.matcher(line)).matches()) {
                try {
                    boolean allInvalid = true;
                    for (int i = 0; i < fieldCount; i++) {
                        try {
                            String parseable= m.group(i + 1);
                            double d= fieldParsers[i].parseField(parseable, i);
                            if ( builder!=null ) builder.putValue(irec, i, d );
                            allInvalid = false;
                        } catch (NumberFormatException e) {
                        }
                    }
                    return !allInvalid;
                    
                } catch (ParseException ex) {
                    return false;
                }

            } else {
                return false;
            }
        }

        /**
         * return what this would be if spitting by whitespace.
         */
        @Override
        public int fieldCount(String line) {
            return line.split("\\s*").length;
        }

        /**
         * show the fields found in the line.  This assumes the line 
         * will match.
         * @param line
         * @return the groups.
         */
        public String[] fields(String line) {
            Matcher m;
            m = recordPattern.matcher(line);
            String[] fields = new String[m.groupCount() - 1];
            for (int i = 0; i < fieldCount; i++) {
                fields[i] = m.group(i + 1);
            }
            return fields;
        }

        @Override
        public boolean splitRecord( String line, String[] fields ) {
            Matcher m;
            m = recordPattern.matcher(line);
            if ( m.matches() ) {
                for (int i = 0; i < fieldCount; i++) {
                    fields[i] = m.group(i + 1);
                }
                return true;
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            return "RegexParser regex="+this.recordPattern+"";
        }
    }

    /**
     * set the record parser to be a fixed columns parser
     * @param columnOffsets the start of each column
     * @param columnWidths the width of each column
     * @param parsers the parser for each column.
     * @return the FixedColumnsParser
     */
    public FixedColumnsParser setFixedColumnsParser(int[] columnOffsets, int[] columnWidths, FieldParser[] parsers) {
        FixedColumnsParser result = new FixedColumnsParser(columnOffsets, columnWidths);
        this.recordParser = result;
        initializeByFieldCount(parsers.length);
        this.fieldParsers = Arrays.copyOf( parsers, parsers.length );
        return result;
    }

    /**
     * Record parser looks at fixed column positions for each record.
     */
    public final class FixedColumnsParser implements RecordParser {

        int[] columnOffsets;
        int[] columnWidths;
        private final int fieldCount;

        public FixedColumnsParser(int[] columnOffsets, int[] columnWidths) {
            this.columnOffsets = Arrays.copyOf( columnOffsets, columnOffsets.length );
            this.columnWidths = Arrays.copyOf( columnWidths, columnWidths.length );
            this.fieldCount = columnOffsets.length;
        }

        @Override
        public int fieldCount() {
            return fieldCount;
        }

        @Override
        public final boolean tryParseRecord(String line, int irec, DataSetBuilder builder) {
            boolean[] fails = new boolean[fieldCount];
            int okayCount = 0;
            int failCount = 0;
            int tryCount= 0;

            for (int i = 0; i < fieldCount; i++) {
                tryCount++;
                try {
                    double d = fieldParsers[i].parseField(line.substring(columnOffsets[i], columnOffsets[i] + columnWidths[i]), i);
                    okayCount++;
                    if ( builder!=null ) builder.putValue(irec, i, d);
                } catch (NumberFormatException | ParseException ex) {
                    failCount++;
                    fails[i] = true;
                }
            }

            if (failCount > 0) {
                System.err.println("error(s) parsing record number " + (irec) + ": ");
                System.err.println(line);
                char[] lineMarker = new char[columnOffsets[fieldCount - 1] + columnWidths[fieldCount - 1]];
                for (int i = 0; i < fieldCount; i++) {
                    if (fails[i]) {
                        for (int j = 0; j < columnWidths[i]; j++) {
                            lineMarker[j + columnOffsets[i]] = '-';
                        }
                    }
                }
                System.err.println(new String(lineMarker));
            }
            // the record is parsable if there are two or more parsable fields.
            // it is not parsable if no fields can be parsed.
            return ( failCount < tryCount ) && ( okayCount > 1 || failCount < 3 );
            
        }

        @Override
        public int fieldCount(String line) {
            return line.split("\\s*").length;
        }

        public String[] fields(String line) {
            String[] result = new String[fieldCount];
            for (int i = 0; i < fieldCount; i++) {
                result[i] = line.substring(columnOffsets[i], columnOffsets[i] + columnWidths[i]);
            }
            return result;
        }

        @Override
        public boolean splitRecord(String line, String[] fields) {
            if ( line.length() >= columnOffsets[fieldCount-1] + columnWidths[fieldCount-1] ) {
                for (int i = 0; i < fieldCount; i++) {
                    fields[i] = line.substring(columnOffsets[i], columnOffsets[i] + columnWidths[i]);
                }
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * return the number of fields in each record.  Note the RecordParsers
     * also have a fieldCount, which should be equal to this.  This allows them
     * to be independent of the parser.
     * @return 
     */
    public int getFieldCount() {
        return fieldCount;
    }
    
    /**
     * return the name of each field.  field0, field1, ... are the default names when
     * names are not discovered in the table.  Changing the array will not affect
     * internal representation.
     * @return
     */
    public String[] getFieldNames() {
        return Arrays.copyOf( this.fieldNames, this.fieldNames.length );
    }

    /**
     * return the labels found for each field.  If a label wasn't found,
     * then the name is returned.
     * @return
     */
    public String[] getFieldLabels() {
        if ( fieldLabels==null ) {
            fieldLabels= new String[fieldNames.length];
        }
        for ( int i=0; i<fieldLabels.length; i++ ) {
            if ( fieldLabels[i]==null ) fieldLabels[i]= fieldNames[i];
        }
        return Arrays.copyOf( this.fieldLabels, this.fieldLabels.length );
    }


    /**
     * return the units that were associated with the field.  This might also be
     * the channel label for spectrograms.  
     * In "field0(str)" or "field0[str]" this is str.
     * elements may be null if not found.
     * @return
     */
    public String[] getFieldUnits() {
        return Arrays.copyOf( this.fieldUnits, this.fieldUnits.length );
    }

    /**
     * Parse the file using the current settings.
     * @param filename the file to read
     * @param mon a monitor
     * @return a rank 2 dataset.
     * @throws java.io.IOException
     */
    public WritableDataSet readFile(String filename, ProgressMonitor mon) throws IOException {
        long size = new File(filename).length();
        mon.setTaskSize(size);
        try (Reader in = new FileReader(filename)) {
            WritableDataSet result= readStream( in, null, mon );
            return result;
        }
    }

//    /**
//     * This was probably used for debugging
//     * @param dp
//     * @param parse
//     * @param fields 
//     */
//    public static void printAndResetMain( DelimParser dp, String parse, String[] fields) {
//        System.err.println(""+parse);
//        dp.splitRecord( parse, fields );
//        for ( int i=0; i<fields.length; i++ ) {
//            System.err.println( String.format( "%3d %s", i, fields[i]  ) );
//            fields[i]= null;
//        }
//    }

//    public static void main(String[] args) throws Exception {
//
//        TimeParser tp= TimeParser.create( "%{ignore} %y %m %d %{ignore} %H" );
//        System.err.println( tp.parse("JF 09 12 02 xxx 04").getTimeDatum() );
//
//        {
//            AsciiParser parser= AsciiParser.newParser(5);
//            DelimParser dp= parser.guessDelimParser("1,2,3,4,5");
//            String[] fields= new String[5];
//            boolean ok;
//            printAndResetMain(dp,"1, 2 ,3,4,\"154\"",fields);
//            printAndResetMain(dp,"1,2,3,4,5",fields);
//            printAndResetMain(dp,"\"foo\",1,3,4,5", fields);
//            printAndResetMain(dp,"1,\"foo\",3,4,5", fields);
//            printAndResetMain(dp,"1,\"fo,o\",3,4,5", fields);
//            printAndResetMain(dp,"1, \"fo,o\" ,3,4,5", fields);
//            printAndResetMain(dp,"1, \"he said \"\"boo\"\"!\" ,3,4,\"154\"", fields);
//            printAndResetMain(dp,"1, 2 ,3,4,\"154\"", fields);
//            System.err.println("great stuff");
//        }
//
//        String file = "/media/mini/data.backup/examples/dat/sarah/2490lintest90005.raw";
//
//        {
//            AsciiParser parser = AsciiParser.newParser(5);
//            parser.setPropertyPattern(Pattern.compile("\\s*(.+)\\s*\\:\\s*(.+)\\s*"));
//            long t0 = System.currentTimeMillis();
//            WritableDataSet ds = parser.readFile(file, new NullProgressMonitor());
//            System.out.println("" + (System.currentTimeMillis() - t0));
//            System.out.println(ds.property("Frequency"));
//            System.out.println(ds.value(0));
//            System.out.flush();
//        }
//        {
//            AsciiParser parser = AsciiParser.newParser(5);
//            parser.setSkipLines(30);
//            //RecordParser rec = parser.guessDelimParser(parser.readFirstRecord(file));
//
//            parser.setPropertyPattern(Pattern.compile("\\s*(.+)\\s*\\:\\s*(.+)\\s*"));
//            long t0 = System.currentTimeMillis();
//            WritableDataSet ds = parser.readFile(file, new NullProgressMonitor());
//            System.out.println("" + (System.currentTimeMillis() - t0));
//            System.out.println(ds.property("Frequency"));
//            for (int j = 0; j < parser.fieldCount; j++) {
//                System.out.print(ds.value(0, j) + " ");
//            }
//            System.out.println();
//            System.out.flush();
//        }
//
//    }

    /** 
     * Creates a new instance.  This is created and then 
     * configured before any files can be parsed.
     */
    public AsciiParser() {
        logger.fine("new ascii parser");
    }
    
    /**
     * Holds value of property keepFileHeader.
     */
    private boolean keepFileHeader;
    
    /**
     * Utility field used by bound properties.
     */
    private final java.beans.PropertyChangeSupport propertyChangeSupport = new java.beans.PropertyChangeSupport(this);

    /**
     * Adds a PropertyChangeListener to the listener list.
     * @param l The listener to add.
     */
    public void addPropertyChangeListener(java.beans.PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(l);
    }

    /**
     * Removes a PropertyChangeListener from the listener list.
     * @param l The listener to remove.
     */
    public void removePropertyChangeListener(java.beans.PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener(l);
    }

    /**
     * Getter for property keepHeader.
     * @return Value of property keepHeader.
     */
    public boolean isKeepFileHeader() {
        return this.keepFileHeader;
    }

    /**
     * Setter for property keepHeader.  By default false but if true, the file header
     * ignored by skipLines is put into the property PROPERTY_FILE_HEADER.
     *
     * @param keepHeader New value of property keepHeader.
     */
    public void setKeepFileHeader(boolean keepHeader) {
        boolean oldKeepHeader = this.keepFileHeader;
        this.keepFileHeader = keepHeader;
        propertyChangeSupport.firePropertyChange("keepHeader", oldKeepHeader, keepHeader);
    }
    /**
     * Holds value of property recordParser.
     */
    private RecordParser recordParser;

    /**
     * Getter for property recordParser.
     * @return Value of property recordParser.
     */
    public RecordParser getRecordParser() {
        return this.recordParser;
    }

    /**
     * Setter for property recordParser.
     * @param recordParser New value of property recordParser.
     */
    public void setRecordParser(RecordParser recordParser) {
        RecordParser oldRecordParser = this.recordParser;
        this.recordParser = recordParser;
        propertyChangeSupport.firePropertyChange("recordParser", oldRecordParser, recordParser);
    }

    /**
     * Indexed getter for property units.
     * @param index Index of the property.
     * @return Value of the property at <CODE>index</CODE>.
     */
    public Units getUnits(int index) {
        if ( this.units[index]==Units.dimensionless && this.fieldUnits[index]!=null && this.fieldUnits[index].length()>0 ) {
            return Units.lookupUnits( this.fieldUnits[index] );
        } else {
            return this.units[index];
        }
    }

    /**
     * Indexed setter for property units.  This now sets the field parser for 
     * the field to be a UNITS_PARSER if it is the default DOUBLE_PARSER.
     * @param index Index of the property.
     * @param units New value of the property at <CODE>index</CODE>.
     */
    public void setUnits(int index, Units units) {
        this.units[index] = units;
        if ( fieldParsers[index]==DOUBLE_PARSER ) setFieldParser(index,UNITS_PARSER);
        propertyChangeSupport.firePropertyChange("units", null, null);
    }
    
    /**
     * Set all the units at once.  This now sets the field parser for 
     * each field to be a UNITS_PARSER if it is the default DOUBLE_PARSER.
     * @param u array (or varargs) of units to be applied to the 0,1,2nd,... fields.
     */
    public void setUnits( Units ... u ) {
        System.arraycopy(u, 0, this.units, 0, u.length);
        for ( int i=0; i<u.length; i++ ) {
            if ( fieldParsers[i]==DOUBLE_PARSER ) setFieldParser( i,UNITS_PARSER );
        }
        propertyChangeSupport.firePropertyChange("units", null, null);
    }

    /**
     * returns the index of the field.  Supports the name, or field0, or 0, etc.
     * returns -1 when the column is not identified.
     * @param string the label for the field, such as "field2" or "time"
     * @return -1 or the index of the field.
     */
    public int getFieldIndex(String string) {
        for (int i = 0; i < fieldNames.length; i++) {
            if (fieldNames[i].equalsIgnoreCase(string)) {
                return i;
            }
        }
        int icol= -1;
        if (Pattern.matches("field[0-9]+", string )) {
            icol= Integer.parseInt(string.substring(5));
        } else if (Pattern.matches("[0-9]+", string )) {
            icol= Integer.parseInt(string);
        }
        if ( icol>=fieldCount ) {
            throw new IllegalArgumentException("bad column parameter: the record parser only expects "+fieldCount+" columns");
        }

        return icol;
    }
    
    /**
     * Holds value of property fillValue.
     */
    private double fillValue = -1e31;

    /**
     * return the fillValue.  numbers that parse to this value are considered 
     * to be fill. Note validMin and validMax may be used as well.
     * @return Value of property fillValue.
     */
    public double getFillValue() {
        return this.fillValue;
    }

    /**
     * numbers that parse to this value are considered to be fill. 
     * @param fillValue New value of property fillValue.
     */
    public void setFillValue(double fillValue) {
        double oldFillValue = this.fillValue;
        this.fillValue = fillValue;
        propertyChangeSupport.firePropertyChange("fillValue", oldFillValue, fillValue);
    }
    
    protected double validMin = Double.NEGATIVE_INFINITY;
    
    public static final String PROP_VALIDMIN = "validMin";

    /**
     * get the minimum valid value for any field.
     * @return validMin
     */
    public double getValidMin() {
        return validMin;
    }

    /**
     * set the minimum valid value for any field.  Values less than 
     * this are to be considered invalid.
     * @param validMin 
     */
    public void setValidMin(double validMin) {
        double oldValidMin = this.validMin;
        this.validMin = validMin;
        propertyChangeSupport.firePropertyChange(PROP_VALIDMIN, oldValidMin, validMin);
    }
    protected double validMax = Double.POSITIVE_INFINITY;
    public static final String PROP_VALIDMAX = "validMax";

    /**
     * get the maximum value for any field.
     * @return the validMax
     */
    public double getValidMax() {
        return validMax;
    }

    /**
     * set the maximum value for any field.  Values above this are to be
     * considered invalid.
     * @param validMax 
     */
    public void setValidMax(double validMax) {
        double oldValidMax = this.validMax;
        this.validMax = validMax;
        propertyChangeSupport.firePropertyChange(PROP_VALIDMAX, oldValidMax, validMax);
    }
}
