/*
 * AsciiParser.java
 *
 * Created on May 25, 2007, 7:01 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.virbo.dsutil;

import org.das2.datum.Units;
import org.das2.datum.UnitsUtil;
import org.das2.util.monitor.ProgressMonitor;
import org.das2.util.monitor.NullProgressMonitor;
import java.io.*;
import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.*;
import org.das2.util.TimeParser;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.WritableDataSet;

/**
 * Class for reading ascii tables into a QDataSet.  This parses a file by breaking
 * it up into records, and passing the record off to a delegate record parser.
 * The record parser then breaks up the record into fields, and each field is 
 * parser by a delegate field parser.  Each column of the table as a Unit and a 
 * field name associated with it.
 * 
 * Examples of record parsers include DelimParser,
 * which splits the record by a delimiter such as a tab or comma, and FixedColumnsParser,
 * which splits the record by character positions.  Example of field parsers include
 * DOUBLE_PARSER which parses the value as a double, and UNITS_PARSER, which uses
 * the Unit attached to the column to interpret the value.
 * 
 * The skipLines property tells the parser to skip a given number of header lines 
 * before attempting to parse the record.  Also, commentPrefix identifies lines to be 
 * ignored.  In either the header or in comments, we look for propertyPattern, and
 * if a property is matched, then the builder property
 * is set.  Two Patterns are provided NAME_COLON_VALUE_PATTERN and
 * NAME_EQUAL_VALUE_PATTERN for convenience.   
 *
 * Adapted to v3.0 QDataSet model, Jeremy, May 2007.
 *
 * @author Jeremy
 */
public class AsciiParser {

    Pattern propertyPattern = null;
    String commentPrefix = "#";
    String[] fieldNames;
    Units[] units;

    /** either the unit or depend 1 value associated with the column 
     * e.g. Density(cc**-3)  or  flux_C4(6.4)
     */
    String[] fieldUnits;

    FieldParser[] fieldParsers;
    final static String numberPart = "[\\d\\.eE\\+\\-]+";
    final static String decimalRegex = numberPart;
    int skipLines;
    boolean skipColumnHeader = false;
    /**
     * regular expression identifying the end of the header, or null.
     */
    String headerDelimiter1 = null;
    int recordCountLimit = Integer.MAX_VALUE;
    int fieldCount;
    public final static Pattern NAME_COLON_VALUE_PATTERN = Pattern.compile("\\s*([a-zA-Z_].*?)\\s*\\:\\s*(.+)\\s*");
    public final static Pattern NAME_EQUAL_VALUE_PATTERN = Pattern.compile("\\s*([a-zA-Z_].*?)\\s*\\=\\s*(.+)\\s*");
    public final static Pattern COLUMN_HEADER_PATTERN = Pattern.compile("\\s*\"?([a-zA-Z][a-zA-Z _0-9]*)(\\(([a-zA-Z_\\.0-9]*)\\))?\"?\\s*");
    public final static String PROPERTY_FIELD_NAMES = "fieldNames";
    public static final String PROPERTY_FILE_HEADER = "fileHeader";
    public static final String PROPERTY_FIRST_RECORD = "firstRecord";
    public static final String PROPERTY_FIELD_PARSER = "fieldParser";
    public static final String DELIM_COMMA = ",";
    public static final String DELIM_TAB = "\t";
    public static final String DELIM_WHITESPACE = "\\s+";
    private static Logger logger = Logger.getLogger("virbo.dataset.asciiparser");

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
     * return the first record that the parser would parse.  If skipLines is 
     * more than the total number of lines, or all lines are comments, then null
     * is returned.
     * 
     * @param filename
     * @return the first line after skip lines and comment lines.
     */
    public String readFirstRecord(String filename) throws IOException {
        return readFirstRecord(new BufferedReader(new FileReader(filename)));
    }

    /**
     * return the first line of the freshly opened file.
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
     * @param filename
     * @return the first parseable line, or null if no such line exists.
     * @throws java.io.IOException
     */
    public String readFirstParseableRecord(String filename) throws IOException {
        BufferedReader reader = new LineNumberReader(new FileReader(filename));

        String line;
        String lastLine = null;

        line = reader.readLine();
        int iline = 0;
        while (line != null && isHeader(iline, lastLine, line, 0)) {
            lastLine = line;
            line = reader.readLine();
            iline++;
        }

        DataSetBuilder builder = new DataSetBuilder(2, 100, recordParser.fieldCount(), 1);

        while (line != null && this.recordParser.tryParseRecord(line, 0, builder) == false) {
            line = reader.readLine();
        }

        reader.close();

        return line;
    }

    /**
     * try to figure out how many lines to skip by looking for the line where
     * the number of fields becomes stable.
     * @param filename
     * @param recParser
     * @return
     */
    public int guessSkipLines( String filename, RecordParser recParser ) throws IOException {
        BufferedReader reader = new LineNumberReader(new FileReader(filename));

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
        int currentFirstRecord=iline;
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

        reader.close();

        return currentFirstRecord;
    }

    /**
     * read in records, allowing for a header of non-records before
     * guessing the delim parser.  This will return a reference to the
     * DelimParser and set skipLines.
     * @param filename
     * @return the record parser to use, or null if no records are found.
     */
    public DelimParser guessSkipAndDelimParser( String filename ) throws IOException {
        BufferedReader reader = new BufferedReader( new FileReader(filename) );

        String line;
        String lastLine = null;

        line = reader.readLine();
        int iline = 0;

        while (line != null && isHeader(iline, lastLine, line, 0)) {
            lastLine = line;
            line = reader.readLine();
            iline++;
        }

        if ( line==null ) return null;

        DelimParser p= guessDelimParser(line);

        List<String> lines= new LinkedList<String>();

        while ( iline<10000 && line != null && p.tryParseRecord(line, 0, null) == false ) {
            lines.add(line);
            line = reader.readLine();
            iline++;
            while ( lines.size()>5 ) {
                lines.remove(0);
            }
            if ( line!=null ) p= guessDelimParser(line);
        }
        
        DelimParser result= p;
        for ( int i=lines.size()-1; i>=0; i-- ) {
            if ( p.fieldCount(lines.get(i))==p.fieldCount() ) {
                result= createDelimParser( lines.get(i), p.getDelim() ); // set column names
                break;
            }
        }
        reader.close();
        
        return result;
    }


    /**
     * read in the first record, then guess the delimiter and possibly the column headers.
     * @param Reader pointed to the beginning of the file.
     * @return RecordParser object that can be queried.  (Strange interface.)
     * @throws java.io.IOException
     */
    public DelimParser guessDelimParser(String line) throws IOException {

        String fieldSep = null;

        int tabDelimFieldCount= line.split("\t",-2 ).length;
        int commaDelimFieldCount= line.split( ",",-2 ).length;
        int whitespaceDelimFieldCount= line.split("\\s+",-2 ).length;

        if ( tabDelimFieldCount > 1) {  // always use tabs over others
            fieldSep = "\t";
        } else if ( commaDelimFieldCount > 1 && commaDelimFieldCount>= whitespaceDelimFieldCount/2 ) { //TODO: improve this
            fieldSep = ",";
        } else {
            fieldSep = "\\s+";
        }

        DelimParser result = createDelimParser(line, fieldSep);
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
        return setDelimParser(new FileReader(filename), delim);
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

        BufferedReader reader = new LineNumberReader(in);

        String line;

        line = readFirstRecord(reader);
        reader.close();

        DelimParser result = createDelimParser(line, delimRegex);
        this.recordParser = result;

        return result;

    }

    /**
     * The regex parser is a slow parser, but gives precise control.
     * 
     */
    public RecordParser setRegexParser(String[] fieldNames) {
        this.fieldCount = fieldNames.length;
        this.fieldNames = fieldNames;
        this.units = new Units[fieldCount];
        for (int i = 0; i < fieldCount; i++) {
            units[i] = Units.dimensionless;
        }
        StringBuffer regexBuf = new StringBuffer();
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
        return setFixedColumnsParser(new FileReader(filename), delim);
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
        BufferedReader reader = new LineNumberReader(in);

        String line;

        line = readFirstRecord(reader);
        reader.close();

        int[] columnOffsets;
        int[] columnWidths;

        int col = 0;

        String[] ss = line.split(delim);
        columnOffsets = new int[ss.length];
        columnWidths = new int[ss.length - 1];
        fieldCount= ss.length;
        fieldParsers = new FieldParser[ss.length - 1];

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


        fieldNames = new String[fieldCount];
        for (int i = 1; i < ss.length; i++) {
            fieldParsers[i - 1] = DOUBLE_PARSER;
            fieldNames[i - 1] = "field" + (i - 1);
        }

        int[] co = new int[columnWidths.length];
        for (int i = 0; i < columnWidths.length; i++) {
            co[i] = columnOffsets[i];
        }

        this.units = new Units[fieldCount];
        for (int i = 0; i < fieldCount; i++) {
            units[i] = Units.dimensionless;
        }
        FixedColumnsParser p = new FixedColumnsParser(co, columnWidths);
        this.recordParser = p;

        this.propertyPattern = null;

        return p;
    }

    /**
     * return the field count that would result in the largest number of records parsed.  The
     * entire file is scanned, and for each line the number of decimal fields is counted.  At the end
     * of the scan, the fieldCount with the highest record count is returned.
     */
    public static int guessFieldCount(String filename) throws FileNotFoundException, IOException {

        final int maxFieldCount = 10;  // can only identify maxFieldCount - 1.

        int[] recCount = new int[maxFieldCount];

        StringBuffer regexBuf = new StringBuffer();
        regexBuf.append("\\s*(" + decimalRegex + ")");
        for (int i = 1; i < maxFieldCount; i++) {
            regexBuf.append("([\\s+,+]\\s*(" + decimalRegex + "))?");
        }
        regexBuf.append("\\s*");

        Pattern pat = Pattern.compile(regexBuf.toString());

        BufferedReader reader = new LineNumberReader(new FileReader(filename));

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
     */
    public static AsciiParser newParser(String[] fieldNames) {
        return new AsciiParser(fieldNames);
    }

    /**
     * skip a number of lines before trying to parse anything.  This can be
     * set to point at the first valid line, and the RecordParser will be 
     * configured using that line.
     */
    public void setSkipLines(int skipLines) {
        this.skipLines = skipLines;
    }

    /**
     * limit the number of records read.  parsing will stop once this number of
     * records is read.  This is Integer.MAX_VALUE by default.
     */
    public void setRecordCountLimit(int recordCountLimit) {
        this.recordCountLimit = recordCountLimit;
    }

    /**
     * specify the Pattern used to recognize properties.  Note property
     * values are not parsed, they are provided as Strings.
     */
    public void setPropertyPattern(Pattern propertyPattern) {
        this.propertyPattern = propertyPattern;
    }

    /**
     * Records starting with this are not processed as data, for example "#".
     * This is initially "#".  Setting this to null disables this check.
     * @param comment
     */
    public void setCommentPrefix(String comment) {
        this.commentPrefix = comment;
    }
    protected String headerDelimiter = null;
    public static final String PROP_HEADERDELIMITER = "headerDelimiter";

    public String getHeaderDelimiter() {
        return headerDelimiter;
    }

    public void setHeaderDelimiter(String headerDelimiter) {
        String oldHeaderDelimiter = this.headerDelimiter;
        this.headerDelimiter = headerDelimiter;
        propertyChangeSupport.firePropertyChange(PROP_HEADERDELIMITER, oldHeaderDelimiter, headerDelimiter);
    }

    /**
     * Parse the stream using the current settings.
     */
    public WritableDataSet readStream(Reader in, ProgressMonitor mon) throws IOException {
        return readStream(in, null, mon);
    }

    /**
     * read in the stream, including the first record if non-null.
     * @param in
     * @param firstRecord, if non-null, parse this record first.  This allows information to be extracted about the
     * records, then fed into this loop.
     * @param mon
     * @return
     * @throws java.io.IOException
     */
    public WritableDataSet readStream(Reader in, String firstRecord, ProgressMonitor mon) throws IOException {
        BufferedReader reader = new BufferedReader(in);
        String line = null;
        String lastLine = null;

        int iline = 0;
        int irec = 0;

        mon.started();

        DataSetBuilder builder = new DataSetBuilder(2, 100, recordParser.fieldCount());
        builder.setFillValue(fillValue);
        builder.setValidMax(validMax);
        builder.setValidMin(validMin);

        long bytesRead = 0;

        StringBuffer headerBuffer = new StringBuffer();

        int skipInt = skipLines + (skipColumnHeader ? 1 : 0);

        lastLine = line;
        if (firstRecord != null) {
            line = firstRecord;
            firstRecord = null;
        } else {
            line = reader.readLine();
        }

        Matcher m;
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
                if (propertyPattern != null && (m = propertyPattern.matcher(line)).matches()) {
                    builder.putProperty(m.group(1).trim(), m.group(2).trim());
                } else {
                    if (keepFileHeader) {
                        headerBuffer.append(line).append("\n");
                    }
                }
                
            } else {
                try {

                    if (firstRecord == null) {
                        firstRecord = line;
                    }

                    // *** here's where we parse each record ***
                    if (recordParser.tryParseRecord(line, irec, builder)) {
                        irec++;
                        builder.nextRecord();

                    } else {
                        //System.out.println(line);
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }

            }

            lastLine = line;
            line = reader.readLine();
        }

        mon.finished();

        String header = headerBuffer.toString();
        builder.putProperty(PROPERTY_FILE_HEADER, header);
        builder.putProperty(PROPERTY_FIRST_RECORD, firstRecord);
        builder.putProperty(QDataSet.USER_PROPERTIES, new HashMap(builder.properties));

        return builder.getDataSet();
    }

    public static interface RecordParser {

        /**
         * returns true if the line appears to be a record.  If it is a record,
         * then the record is inserted into the builder.
         */
        boolean tryParseRecord(String line, int irec, DataSetBuilder builder);

        /**
         * indicate the number of fields this RecordParser is expecting on each
         * line.
         */
        int fieldCount();

        /**
         * return the number of fields in this line.  All records will have this
         * number of fields.  This is used for discovery and to configure the parser.
         */
        int fieldCount(String line);

        /**
         * return the string for each field.  This is useful
         * for discovery, and is not used in the bulk parsing.
         * @param line
         * @deprecated, use splitRecord instead
         * @return
         */
        String[] fields(String line);

        /**
         * attempts to extract fields from the record, returning true if
         * the record could be split.
         * @param line
         * @param fields array to store the fields.  fieldCount() should be used
         * to determine the length of the array.
         * @return true if the line is a record that can be split into fields.
         */
        boolean splitRecord( String line, String[] fields );
    }

    public static interface FieldParser {

        double parseField(String field, int columnIndex) throws ParseException;
    }
    /**
     * parses the field using Double.parseDouble, java's double parser.
     */
    public static final FieldParser DOUBLE_PARSER = new FieldParser() {

        public final double parseField(String field, int columnIndex) {
            return Double.parseDouble(field);
        }
    };
    /**
     * delegates to the unit object set for this field to parse the data.
     */
    public final FieldParser UNITS_PARSER = new FieldParser() {

        public final double parseField(String field, int columnIndex) throws ParseException {
            Units u = AsciiParser.this.units[columnIndex];
            return u.parse(field).doubleValue(u);
        }
    };

    /**
     * hide the nuances of java's split function.  When the string endswith the
     * regex, add an empty field.  Also, trim the string so leading and trailing
     * whitespace is not treated as a delimiter.
     * @param string
     * @param regex regular expression like \\s+
     * @return string array containing the fields.
     */
    private static final String[] split(String string, String regex) {
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
     * hide the nuances of java's split function.  When the string endswith the
     * regex, add an empty field.  Also, trim the string so leading and trailing
     * whitespace is not treated as a delimiter.
     * 
     * no longer used because String.split( delim, -2 ) has the same effect.
     * 
     * @param string
     * @param regex regular expression like \\s+
     * @return string array containing the fields.
     */
    private static final String[] split(String string, Pattern delimPattern, String delimRegex) {
        String[] ss = delimPattern.split(string.trim());
        if (string.endsWith(delimRegex)) {
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
     * @return
     */
    private DelimParser createDelimParser(String line, String fieldSep) {

        String[] ss = split(line.trim(), fieldSep);

        fieldCount = ss.length;
        fieldParsers = new FieldParser[fieldCount];

        this.units = new Units[fieldCount];
        fieldNames = new String[fieldCount];
        fieldUnits= new String[fieldCount];

        boolean isColumnHeaders = true;
        for (int i = 0; i < ss.length; i++) {
            units[i] = Units.dimensionless;
            fieldParsers[i] = DOUBLE_PARSER;
            Matcher m;
            if ((m = COLUMN_HEADER_PATTERN.matcher(ss[i])).matches()) {
                fieldNames[i] = m.group(1).trim().replaceAll(" ", "_");
                fieldUnits[i]= m.group(3);
                if (fieldUnits[i]!=null) fieldUnits[i]= fieldUnits[i].trim();
            // TODO: check for units too.
            // if ( m.groupCount() is 2) String u= m.group(2).trim()
            } else {
                if (isColumnHeaders) {
                    logger.finest("first parsed line does not appear to be column header because of field #" + i + ": " + ss[i]);
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
        } else {
            skipColumnHeader = true;
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

        public DelimParser(int fieldCount, String delim) {
            this.fieldCount = fieldCount;
            this.doParseField = new boolean[fieldCount];
            for (int i = 0; i < fieldCount; i++) {
                this.doParseField[i] = true;
            }
            this.delimRegex = delim;
            this.delimPattern = Pattern.compile(delim);
        }

        /**
         * returns the delimiter, which is a regex.  Examples include "," "\t", and "\s+"
         * @return
         */
        public String getDelim() {
            return delimRegex;
        }

        public boolean tryParseRecord(String line, int irec, DataSetBuilder builder) {
            int j = 0;
            int okayCount = 0;
            int failCount = 0;
            int tryCount= 0;
            int ipos = 0;

            String[] ss = new String[fieldCount];
            if ( !splitRecord(line, ss ) ) {
                return false;
            }

            for (j = 0; j < fieldCount; j++) {
                tryCount++;
                if (doParseField[j]) {
                    String parseable = ss[j];
                    try {
                        double d= fieldParsers[j].parseField(parseable, j);
                        if ( builder!=null ) builder.putValue(irec, j, d );
                        okayCount++;
                    } catch (ParseException e) {
                        failCount++;
                    } catch (NumberFormatException e) {
                        failCount++;
                    }
                }
            }
            // the record is parsable if there are two or more parsable fields.
            // it is not parsable if no fields can be parsed.
            return ( failCount < tryCount ) && ( okayCount > 1 || failCount < 3 );
        }

        public int fieldCount() {
            return fieldCount;
        }

        public int fieldCount(String line) {
            return fields(line).length;
        }

        public void setSkipField(int ifield, boolean skip) {
            this.doParseField[ifield] = !skip;
        }

        public String[] fields(String line) {
            String[] ss = line.trim().split(delimRegex, -2);
            return ss;
        }

        public boolean splitRecord(String line, String[] fields) {
            String[] ss = line.trim().split(delimRegex, -2);
            if ( ss.length==fieldCount ) {
                System.arraycopy( ss, 0, fields, 0, fieldCount );
                return true;
            } else {
                return false;
            }
        }
    }

    public final class RegexParser implements RecordParser {

        Pattern recordPattern;
        int fieldCount;

        public RegexParser(String regex) {
            recordPattern = Pattern.compile(regex);
            this.fieldCount = recordPattern.matcher("").groupCount();
        }

        public int fieldCount() {
            return fieldCount;
        }

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
                    if (!allInvalid) {
                        return true;
                    } else {
                        return false;
                    }
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
        public int fieldCount(String line) {
            return line.split("\\s*").length;
        }

        public String[] fields(String line) {
            Matcher m;
            m = recordPattern.matcher(line);
            String[] fields = new String[m.groupCount() - 1];
            for (int i = 0; i < fieldCount; i++) {
                fields[i] = m.group(i + 1);
            }
            return fields;
        }

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
    }

    public FixedColumnsParser setFixedColumnsParser(int[] columnOffsets, int[] columnWidths, FieldParser[] parsers) {
        FixedColumnsParser result = new FixedColumnsParser(columnOffsets, columnWidths);
        this.recordParser = result;
        this.fieldParsers = parsers;
        this.fieldNames= new String[ result.fieldCount ];
        this.units= new Units[ result.fieldCount ];
        for ( int i=0; i<result.fieldCount; i++ ) {
            this.fieldNames[i]= "field"+i;
            this.units[i]= Units.dimensionless;
        }
        return result;
    }

    public final class FixedColumnsParser implements RecordParser {

        int[] columnOffsets;
        int[] columnWidths;
        private int fieldCount;

        public FixedColumnsParser(int[] columnOffsets, int[] columnWidths) {
            this.columnOffsets = columnOffsets;
            this.columnWidths = columnWidths;
            this.fieldCount = columnOffsets.length;
        }

        public int fieldCount() {
            return fieldCount;
        }

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
                } catch (NumberFormatException ex) {
                    failCount++;
                    fails[i] = true;
                } catch (ParseException ex) {
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
     * return the name of each field.  field0, field1, ... are the default names when
     * names are not discovered in the table.
     * @return
     */
    public String[] getFieldNames() {
        return this.fieldNames;
    }

    /**
     * return the units that were associated with the field.  This might also be
     * the channel label for spectrograms.  In "field0(str)" this is str.
     * elements may be null if not found.
     * @return
     */
    public String[] getFieldUnits() {
        return this.fieldUnits;
    }
    /**
     * Parse the file using the current settings.
     * @return a rank 2 dataset.
     */
    public WritableDataSet readFile(String filename, ProgressMonitor mon) throws IOException {
        long size = new File(filename).length();
        mon.setTaskSize(size);
        return readStream(new FileReader(filename), mon);
    }

    public static void main(String[] args) throws Exception {

        TimeParser tp= TimeParser.create( "%{ignore} %y %m %d %{ignore} %H" );
        System.err.println( tp.parse("JF 09 12 02 xxx 04").getTimeDatum() );

        String file = "/media/mini/data.backup/examples/dat/2490lintest90005.raw";

        {
            AsciiParser parser = AsciiParser.newParser(5);
            parser.setPropertyPattern(Pattern.compile("\\s*(.+)\\s*\\:\\s*(.+)\\s*"));
            long t0 = System.currentTimeMillis();
            WritableDataSet ds = parser.readFile(file, new NullProgressMonitor());
            System.out.println("" + (System.currentTimeMillis() - t0));
            System.out.println(ds.property("Frequency"));
            System.out.println(ds.value(0));
            System.out.flush();
        }
        {
            AsciiParser parser = AsciiParser.newParser(5);
            parser.setSkipLines(30);
            RecordParser rec = parser.guessDelimParser(parser.readFirstRecord(file));

            parser.setPropertyPattern(Pattern.compile("\\s*(.+)\\s*\\:\\s*(.+)\\s*"));
            long t0 = System.currentTimeMillis();
            WritableDataSet ds = parser.readFile(file, new NullProgressMonitor());
            System.out.println("" + (System.currentTimeMillis() - t0));
            System.out.println(ds.property("Frequency"));
            for (int j = 0; j < parser.fieldCount; j++) {
                System.out.print(ds.value(0, j) + " ");
            }
            System.out.println();
            System.out.flush();
        }



    }

    /** Creates a new instance of AsciiParser */
    public AsciiParser() {
    }
    /**
     * Holds value of property keepFileHeader.
     */
    private boolean keepFileHeader;
    /**
     * Utility field used by bound properties.
     */
    private java.beans.PropertyChangeSupport propertyChangeSupport = new java.beans.PropertyChangeSupport(this);

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
        propertyChangeSupport.firePropertyChange("keepHeader", new Boolean(oldKeepHeader), new Boolean(keepHeader));
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
        return this.units[index];
    }

    /**
     * Indexed setter for property units.
     * @param index Index of the property.
     * @param units New value of the property at <CODE>index</CODE>.
     */
    public void setUnits(int index, Units units) {
        this.units[index] = units;
        propertyChangeSupport.firePropertyChange("units", null, null);
    }

    /**
     * returns the index of the field
     */
    public int getFieldIndex(String string) {
        for (int i = 0; i < fieldNames.length; i++) {
            if (fieldNames[i].equalsIgnoreCase(string)) {
                return i;
            }
        }
        return -1;
    }
    /**
     * Holds value of property fillValue.
     */
    private double fillValue = -1e31;

    /**
     * Getter for property fillValue.
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
        propertyChangeSupport.firePropertyChange("fillValue", new Double(oldFillValue), new Double(fillValue));
    }
    protected double validMin = Double.NEGATIVE_INFINITY;
    public static final String PROP_VALIDMIN = "validMin";

    public double getValidMin() {
        return validMin;
    }

    public void setValidMin(double validMin) {
        double oldValidMin = this.validMin;
        this.validMin = validMin;
        propertyChangeSupport.firePropertyChange(PROP_VALIDMIN, oldValidMin, validMin);
    }
    protected double validMax = Double.POSITIVE_INFINITY;
    public static final String PROP_VALIDMAX = "validMax";

    public double getValidMax() {
        return validMax;
    }

    public void setValidMax(double validMax) {
        double oldValidMax = this.validMax;
        this.validMax = validMax;
        propertyChangeSupport.firePropertyChange(PROP_VALIDMAX, oldValidMax, validMax);
    }
}
