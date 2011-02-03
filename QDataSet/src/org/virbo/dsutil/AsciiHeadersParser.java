/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.dsutil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import org.virbo.dataset.AbstractDataSet;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SemanticOps;

/**
 * This additional support for parsing ascii data looks at the comment block in
 * an ASCII file for a structured set of Dataset tags further describing the
 * data.  This is loosely formatted JSON that describes each column of the
 * data file more abstractly than the AsciiParser.
 *
 * This is based on QDataSet metadata tags, as much as possible.
 * 
 * @author jbf
 */
public class AsciiHeadersParser {

    char commented= '?'; // tri-state: '?' 'T' 'F'

    /**
     * return the next comment line with content, dropping empty lines, or null.
     * The comment line is returned without the comment character
     * @param reader
     * @return
     */
    private String readNextLine( BufferedReader reader ) throws IOException {
         String line = reader.readLine();
         if ( commented=='?' && line.length()>0 ) {
             commented= line.charAt(0)=='#' ? 'Y' : 'N';
         }
         if ( line != null && line.startsWith("#") ) {
             line = line.substring(1);
         } else {
             if ( commented=='Y' ) return null;
         }
         while ( line!=null && line.trim().isEmpty() ) {
            line = reader.readLine();
            if ( line != null && line.startsWith("#") ) {
                line = line.substring(1);
            } else {
                return null;
            }
         }
         return line;
    }
    
    /**
     * Preprocess the string to make more valid JSON.
     * 1. pop off comment character (#) from line.
     * 2. add leading and trailing braces (}) if the first char is not an opening brace.
     * 3. add implicit comma at line breaks unless the next line starts with comma or closing bracket (]).
     * 4. closing brace closes JSON.
     * Note the Java JSON parser used is already pretty loose:
     * 1. strings needn't be quoted if they don't contain whitespace.
     *    a. I like this for structure tag names "UNITS:"
     *    b. I'd discourage this for string values, because they might be interpreted as numbers.  UNITS:"1e2 nT"
     * 2. equals (and other characters) can be used instead of COLONs.  UNITS="1e2 nT"
     * 
     * @param s
     * @return
     */
    protected String prep(String s) {
        boolean dontHaveOpeningBrace = true;
        boolean addClosingBrace = false;
        boolean expectClosingBrace= false;

        int braceLevel= 0;
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = new BufferedReader(new StringReader(s));

            String line = readNextLine( reader );

            int iline = 1;
            while (line != null) {

                String trimLine = line.trim();

                if (dontHaveOpeningBrace) {
                    if (!trimLine.startsWith("{")) {
                        line = "{" + line;
                        addClosingBrace = true;
                    } else {
                        expectClosingBrace= true;
                    }
                    dontHaveOpeningBrace = false;
                }

                // read ahead to get the next line containing text, so we can avoid adding comma to dangling text.  See test3_1.
                String nextLine = readNextLine( reader );

                // we can add a comma at the end of a line to make it valid.
                char lastChar = trimLine.charAt(trimLine.length() - 1);
                if (lastChar == '"' || Character.isDigit(lastChar) || lastChar == ']' || lastChar == '}') {
                    char nextChar;
                    if (nextLine != null && nextLine.trim().length() > 0) {
                        nextChar = nextLine.trim().charAt(0);
                        if (nextChar != ',' && nextChar != ']') {
                            line = line + ",";
                        }
                    }
                }

                // update the brace level
                boolean inQuote= false;
                boolean backSlash= false;
                for ( int i=0; i<trimLine.length(); i++ ) {
                    int ch= trimLine.charAt(i);
                    if ( backSlash ) {
                        backSlash= false;
                        if ( ch=='"' ) {
                            continue;
                        }
                    }
                    switch ( ch ) {
                        case '{': if ( !inQuote ) braceLevel++; break;
                        case '}': if ( !inQuote ) braceLevel--; break;
                        case '"': inQuote= !inQuote; break;
                        case '\\': backSlash= true; break;
                        default:
                    }
                }

                sb.append(line).append("\n");

                line = nextLine;
                iline++;

                // If we had an opening brace, then the closing brace can finish off the JSON so additional comments are ignored.
                if ( expectClosingBrace && braceLevel==0 ) {
                    line=null;
                }

            }

            reader.close();

            if (addClosingBrace) {
                sb.append("}");
            }
            return sb.toString();

        } catch (IOException ex) {
            throw new RuntimeException(ex);

        }

    }

    /**
     * attempt to parse the metadata stored in the header.  The header lines must
     * be prefixed with hash (#).
     *
     * @param header
     * @return
     */
    public static BundleDescriptor parseMetadata( String header, String[] columns ) throws ParseException {
        try {
            JSONObject jo;
            AsciiHeadersParser ahp= new AsciiHeadersParser();
            jo = new JSONObject( ahp.prep(header) );

            BundleDescriptor bd= new BundleDescriptor();
            for ( int i=0; i<columns.length; i++ ) {
                bd.addDataSet( columns[i], i, new int[0] );
            }

            return fillMetadata(bd,jo);

        } catch (JSONException ex) {
            throw new ParseException( ex.toString(), 0 );
        }
    }

    public static class BundleDescriptor extends AbstractDataSet {

        int len;
        Map<String,Integer> datasets;
        Map<Integer,String> datasets2;
        Map<String,int[]> qubes;

        BundleDescriptor(  ) {
            properties= new LinkedHashMap();
            datasets= new LinkedHashMap();
            datasets2= new LinkedHashMap();
            qubes= new LinkedHashMap();
        }

        public int indexOf( String name ) {
            Integer i= datasets.get(name);
            if ( i==null ) {
                return -1;
            } else {
                return i.intValue();
            }
        }

        public void putProperty( String name, int i, Object value ) {
            properties.put( name+"__"+i, value );
        }

        /**
         * add the named dataset with the dimensions.  Note qube
         * doesn't include the first dimension, and this may be null for
         * rank 1 datasets.
         *
         * @param name name of the dataset
         * @param i  position in the qube.
         * @param qube the dimensions or null for rank 1 data, e.g. vector= [3]
         */
        protected void addDataSet( String name, int i, int[] qube ) {
            datasets.put( name, i );
            datasets2.put( i, name );
            qubes.put( name, qube );
        }

        public int rank() {
            return 2;
        }

        @Override
        public int length() {
            return datasets.size();
        }

        @Override
        public int length(int i) {
            String name= datasets2.get(i);
            int[] qube= qubes.get(name);
            if ( qube==null || qube.length==0 ) {
                return 0;
            } else {
                return qube[0];
            }
        }

        @Override
        public Object property(String name, int i) {
            Object v= properties.get( name+"__"+i );
            return v;
        }

        @Override
        public double value(int i0, int i1) {
            // support bundling just rank N-1 datasets.  to support higher rank
            // datasets, this should return the qube dims.
            throw new IndexOutOfBoundsException("length=0");
        }

    }

    /**
     * JSON can be specified in two ways:
     * 1. listing each dimension property and the values for each column (transpose)
     * 2. listing for each column, the dimension properties.
     * @param jo
     * @return
     */
    private static boolean isTranspose( JSONObject jo ) {
        for ( String s: DataSetUtil.dimensionProperties() ) {
            if ( jo.has(s) ) {
                return true;
            }
        }
        for ( String s: new String[] { "DEPEND_0" } ) {
            if ( jo.has(s) ) {
                return true;
            }
        }
        return false;
    }

    private static Object coerceToType( String propName, Object propValue ) {
        if ( propName.equals( QDataSet.UNITS ) ) {
            return SemanticOps.lookupUnits((String)propValue);
        } else if ( propName.equals( QDataSet.VALID_MIN ) ) {
            return Double.parseDouble(String.valueOf(propValue) );
        } else if ( propName.equals( QDataSet.VALID_MAX ) ) {
            return Double.parseDouble(String.valueOf(propValue) );
        } else if ( propName.equals( QDataSet.TYPICAL_MIN ) ) {
            return Double.parseDouble(String.valueOf(propValue) );
        } else if ( propName.equals( QDataSet.TYPICAL_MAX ) ) {
            return Double.parseDouble(String.valueOf(propValue) );
        } else if ( propValue.equals( QDataSet.FILL_VALUE ) ) {
            return Double.parseDouble(String.valueOf(propValue) );
        } else if ( propValue.equals( QDataSet.MONOTONIC ) ) {
            return Boolean.valueOf(String.valueOf(propValue) );
        } else {
            return String.valueOf(propValue);
        }
    }

    /**
     * return a map of metadata for each column
     * @param jo
     * @return
     */
    private static BundleDescriptor fillMetadata( BundleDescriptor bd, JSONObject jo ) throws JSONException {

        String[] names= JSONObject.getNames(jo);
        if ( isTranspose(jo) ) {
            throw new IllegalArgumentException("not implemented");
        } else {
            Iterator it= jo.keys();
            for ( ; it.hasNext(); ) {
                 String key= (String) it.next();
                 Object o= jo.get(key);
                 if ( !( o instanceof JSONObject ) ) {
                     System.err.println("expected JSONObject for value: "+key );
                     continue;
                 } else {
                     System.err.println("STOP HERE");
                     System.err.println(bd);
                     int ids= bd.indexOf( key ); //DANGER:Rank2
                     bd.addDataSet( key, ids, null );
                     JSONObject propsj= ((JSONObject)o);
                     Iterator props= propsj.keys();
                     bd.putProperty( QDataSet.NAME, ids, key );
                     for ( ; props.hasNext(); ) {
                         String prop= (String) props.next();
                         Object v= coerceToType( prop, propsj.get(prop) );
                         bd.putProperty( prop, ids, v );
                     }
                 }
            }
        }
        return bd;
    }


}
