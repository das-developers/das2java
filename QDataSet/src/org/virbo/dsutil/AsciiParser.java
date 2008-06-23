/*
 * AsciiParser.java
 *
 * Created on May 25, 2007, 7:01 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.virbo.dsutil;

import edu.uiowa.physics.pw.das.datum.Units;
import edu.uiowa.physics.pw.das.datum.UnitsUtil;
import org.das2.util.monitor.ProgressMonitor;
import org.das2.util.monitor.NullProgressMonitor;
import java.io.*;
import java.text.ParseException;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.regex.*;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.WritableDataSet;

/**
 * Class for reading ascii tables into a QDataSet.  This parses a
 * file by looking at each line to see if it matches one of
 * two Patterns: one for properties and one for records.  If a record matched,
 * then the record is matched and fields pulled out, parsed and insered a
 * DataSetBuilder.  If a property is matched, then the builder property
 * is set.  Two Patterns are provided NAME_COLON_VALUE_PATTERN and
 * NAME_EQUAL_VALUE_PATTERN for convenience.  The record pattern is currently
 * the number of fields identified with whitespace in between.  Note the X
 * tags are just the record numbers.
 *
 * Adapted to v3.0 QDataSet model, Jeremy, May 2007.
 *
 * @author Jeremy
 */
public class AsciiParser {

    Pattern propertyPattern;
    String commentRegex;
    String[] fieldNames;
    Units[] units;
    FieldParser[] fieldParsers;
    final static String numberPart = "[\\d\\.eE\\+\\-]+";
    final static String decimalRegex = numberPart;
    int skipLines;
    boolean skipColumnHeader= false;
    int recordCountLimit;
    int fieldCount;
    public final static Pattern NAME_COLON_VALUE_PATTERN = Pattern.compile("\\s*(.+?)\\s*\\:\\s*(.+)\\s*");
    public final static Pattern NAME_EQUAL_VALUE_PATTERN = Pattern.compile("\\s*(.+?)\\s*\\=\\s*(.+)\\s*");
    public final static Pattern COLUMN_HEADER_PATTERN = Pattern.compile("\\s*([a-zA-Z][a-zA-Z _0-9]*)(\\([a-zA-Z _0-9]*\\))?\\s*");
    public final static String PROPERTY_FIELD_NAMES = "fieldNames";
    private static final String SPACES;
    private static final String DASHES;
    public static final String PROPERTY_FILE_HEADER = "fileHeader";
    private static final String PROPERTY_FIRST_RECORD = "firstRecord";
    private static final String PROPERTY_FIELD_PARSER = "fieldParser";

    static {
        StringBuffer buf = new StringBuffer(1024);
        for (int i = 0; i < 1024; i++) {
            buf.append(" ");
        }
        SPACES = buf.toString();

        buf = new StringBuffer(1024);
        for (int i = 0; i < 1024; i++) {
            buf.append("-");
        }
        DASHES = buf.toString();

    }
    private static Logger logger = Logger.getLogger("virbo.dataset.asciiparser");

    private AsciiParser(String[] fieldNames) {
        setRegexParser(fieldNames);
    }

    /**
     * configure the parser to split on a delimRegex.  For example,
     *   " +"    one or more spaces
     *   "\t"    tab
     *   ","    comma
     */
    public RecordParser setDelimParser(String filename, String delimRegex) throws IOException {
        return setDelimParser(new FileReader(filename), delimRegex);
    }

    /**
     * return the first record that the parser would parse.
     * 
     * @param filename
     * @return the first line after skip lines and comment lines.
     */
    public String readFirstRecord( String filename ) throws IOException {
        BufferedReader reader = new LineNumberReader( new FileReader(filename ) );

        String line;

        line = reader.readLine();
        for (int i = 0; i < skipLines; i++) {
            line = reader.readLine();
        }
        reader.close();

        return line;
    }
    
    /**
     * read in the first record, then guess the delimiter and possibly the column headers.
     * @param Reader pointed to the beginning of the file.
     * @return RecordParser object that can be queried.  (Strange interface.)
     * @throws java.io.IOException
     */
    public RecordParser guessDelimParser( String line ) throws IOException {

        String fieldSep = null;

        if (line.indexOf("\t") != -1) {
            fieldSep = "\t";
        } else if (line.indexOf(",") != -1) {
            fieldSep = ",";
        } else {
            fieldSep = "\\s+";
        }

        this.recordParser = createDelimParser(line, fieldSep);

        return recordParser;

    }

    public RecordParser setDelimParser(Reader in, String delimRegex) throws IOException {

        BufferedReader reader = new LineNumberReader(in);

        String line;

        line = reader.readLine();
        for (int i = 0; i < skipLines; i++) {
            line = reader.readLine();
        }
        reader.close();

        this.recordParser = createDelimParser(line, delimRegex);

        return recordParser;

    }

    /**
     * what was the point of the regex parser again?
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
     * the columns are.  
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

        line = reader.readLine();
        for (int i = 0; i < skipLines; i++) {
            line = reader.readLine();
        }
        reader.close();

        int[] columnOffsets;
        int[] columnWidths;

        int col = 0;

        String[] ss = line.split(delim);
        columnOffsets = new int[ss.length];
        columnWidths = new int[ss.length - 1];
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
     * skip a number of lines before trying to parse anything.
     */
    public void setSkipLines(int skipLines) {
        this.skipLines = skipLines;
    }

    /**
     * limit the number of records read.  parsing will stop at this limit.
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
     * Parse the stream using the current settings.
     */
    public WritableDataSet readStream(Reader in, ProgressMonitor mon) throws IOException {
        BufferedReader reader = new BufferedReader(in);
        String line;
        int iline = 0;
        int irec = 0;

        mon.started();

        DataSetBuilder builder = new DataSetBuilder(2, 100, recordParser.fieldCount(), 1);
        builder.setFillValue(fillValue);

        long bytesRead = 0;

        StringBuffer headerBuffer = new StringBuffer();
        String firstRecord= null;
        
        int skipInt= skipLines + ( skipColumnHeader ? 1 : 0 );
        
        Matcher m;
        while ((line = reader.readLine()) != null && !mon.isCancelled()) {
            bytesRead += line.length() + 1; // +1 is for end-of-line
            iline++;
            mon.setTaskProgress(bytesRead);
            if (iline % 100 == 0) {
                mon.setProgressMessage("reading line " + iline);
            }
            try {
                if (iline <= skipInt) {
                    if (keepFileHeader) {
                        headerBuffer.append(line);
                    }
                    if (propertyPattern != null && (m = propertyPattern.matcher(line)).matches()) {
                        builder.putProperty(m.group(1).trim(), m.group(2).trim());
                    }
                    continue;
                } else if ( iline==skipInt+1) {
                    firstRecord= line;
                }

                if (propertyPattern != null && (m = propertyPattern.matcher(line)).matches()) {
                    builder.putProperty(m.group(1).trim(), m.group(2).trim());

                } else if (recordParser.tryParseRecord(line, irec, builder)) {
                    irec++;
                    builder.nextRecord();

                } else {
                    //System.out.println(line);
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        mon.finished();

        builder.putProperty( PROPERTY_FILE_HEADER, headerBuffer.toString() );
        builder.putProperty( PROPERTY_FIRST_RECORD, firstRecord );
        builder.putProperty( QDataSet.USER_PROPERTIES, new HashMap( builder.properties ) ); // QDataSet doesn't propogate unknown props.

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
    }

    public static interface FieldParser {

        double parseField(String field, int columnIndex) throws ParseException;
    }
    public static final FieldParser DOUBLE_PARSER = new FieldParser() {

        public final double parseField(String field, int columnIndex) {
            return Double.parseDouble(field);
        }
    };
    public final FieldParser UNITS_PARSER = new FieldParser() {

        public final double parseField(String field, int columnIndex) throws ParseException {
            Units u = AsciiParser.this.units[columnIndex];
            return u.parse(field).doubleValue(u);
        }
    };

    /**
     * create a delimeter-based record parser by splitting the line and counting
     * the number of fields.  If the line appears to contain column headings,
     * then column names will be set as well.
     * This has the side effect of turning off property record pattern.
     * @param line
     * @param fieldSep
     * @return
     */
    private RecordParser createDelimParser(String line, String fieldSep) {

        String[] ss = line.split(fieldSep);

        int offset = 0;

        fieldCount = ss.length;
        if (ss[0].equals("")) {
            fieldCount--;
            offset = 1;
        }
        if (ss[ss.length - 1].equals("")) {
            fieldCount--;
        }
        fieldParsers = new FieldParser[fieldCount];

        this.units = new Units[fieldCount];
        fieldNames = new String[fieldCount];

        boolean isColumnHeaders = true;
        for (int i = 0; i < fieldCount; i++) {
            units[i] = Units.dimensionless;
            fieldParsers[i] = DOUBLE_PARSER;
            Matcher m;
            if ((m = COLUMN_HEADER_PATTERN.matcher(ss[i + offset])).matches()) {
                fieldNames[i] = m.group(1).trim().replaceAll(" ", "_");
            // TODO: check for units too.
                // if ( m.groupCount() is 2) String u= m.group(2).trim()
            } else {
                logger.finest("first parsed line does not appear to be column header because of field #" + i + ": " + ss[i]);
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
            skipColumnHeader= true;
        }
        
        RecordParser recordParser1 = new DelimParser(fieldParsers.length, fieldSep);

        this.propertyPattern = null;

        return recordParser1;

    }

    public final class DelimParser implements RecordParser {

        int fieldCount;
        String delimRegex;
        Pattern delimPattern;

        public DelimParser(int fieldCount, String delim) {
            this.fieldCount = fieldCount;
            this.delimRegex = delim;
            delimPattern = Pattern.compile(delim);
        }

        public boolean tryParseRecord(String line, int irec, DataSetBuilder builder) {
            int j = 0;
            boolean okay = true;
            int ipos = 0;

            String[] ss = delimPattern.split(line);
            if ( ss.length==0 ) {
                return false;
            }
            int emptyOffset = ss[0].equals("") ? 1 : 0;  // check that the first field isn't empty.
            int endOffset = ss[ss.length - 1].equals("") ? 1 : 0;

            if ((ss.length - emptyOffset - endOffset) != fieldCount) {
                return false;
            }
            for (j = 0; okay && j < fieldCount; j++) {
                String parseable = ss[j + emptyOffset];
                try {
                    builder.putValue(irec, j, fieldParsers[j].parseField(parseable, j));
                } catch (ParseException e) {
                    okay = false;
                }
            }
            return okay;
        }

        public int fieldCount() {
            return fieldCount;
        }

        public int fieldCount(String line) {
            return line.split(delimRegex).length;
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
                            double d = Double.parseDouble(m.group(i + 1));
                            allInvalid = false;
                        } catch (NumberFormatException e) {
                        }
                    }
                    if (!allInvalid) {
                        for (int i = 0; i < fieldCount; i++) {
                            builder.putValue(irec, i, units[i].parse(m.group(i + 1)).doubleValue(units[i]));
                        }
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
    }

    public FixedColumnsParser setFixedColumnsParser(int[] columnOffsets, int[] columnWidths, FieldParser[] parsers) {
        FixedColumnsParser result = new FixedColumnsParser(columnOffsets, columnWidths);
        this.recordParser = result;
        this.fieldParsers = parsers;
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
            int failCount = 0;

            for (int i = 0; i < fieldCount; i++) {
                // TODO: support for TimeLocationUnits.
                try {
                    if (fieldParsers[i] == null) {
                        System.err.println("here: " + i + "  " + fieldCount);
                    }
                    double d = fieldParsers[i].parseField(line.substring(columnOffsets[i], columnOffsets[i] + columnWidths[i]), i);
                    builder.putValue(irec, i, d);
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

            return true;
        }

        public int fieldCount(String line) {
            return line.split("\\s*").length;
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
     * Parse the file using the current settings.
     * @return a rank 2 dataset.
     */
    public WritableDataSet readFile(String filename, ProgressMonitor mon) throws IOException {
        long size = new File(filename).length();
        mon.setTaskSize(size);
        return readStream(new FileReader(filename), mon);
    }

    public static void main(String[] args) throws Exception {

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
            RecordParser rec = parser.guessDelimParser( parser.readFirstRecord(file) );
            
            parser.setPropertyPattern(Pattern.compile("\\s*(.+)\\s*\\:\\s*(.+)\\s*"));
            long t0 = System.currentTimeMillis();
            WritableDataSet ds = parser.readFile(file, new NullProgressMonitor());
            System.out.println("" + (System.currentTimeMillis() - t0));
            System.out.println(ds.property("Frequency"));
            for ( int j=0; j<parser.fieldCount; j++ ) {
                System.out.print(ds.value(0,j)+" ");
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
}
