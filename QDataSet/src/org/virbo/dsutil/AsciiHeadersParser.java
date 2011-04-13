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
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.datum.DatumUtil;
import org.das2.datum.EnumerationUnits;
import org.das2.datum.Units;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.virbo.dataset.AbstractDataSet;
import org.virbo.dataset.DDataSet;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.MutablePropertyDataSet;
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

    public static final String PROP_DIMENSION = "DIMENSION";
    public static final String PROP_ELEMENTS = "ELEMENTS";

    private static final Logger logger= Logger.getLogger("org.virbo.dsutil.AsciiHeadersParser");
    
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
         while ( line!=null && line.trim().length()==0 ) {
            line = reader.readLine();
            if ( line != null && line.startsWith("#") ) {
                line = line.substring(1);
            } else {
                if ( commented=='Y' ) {
                    return null;
                } else {
                    return line;
                }
                
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
     * calculate the bundle descriptor, possibly folding together columns to create
     * high rank datasets.
     * @param bd
     * @param columns
     */
    private static BundleDescriptor calcBundleDescriptor( JSONObject jo, String[] columns ) {

        String[] snames= new String[ columns.length ];

        BundleDescriptor bd= new BundleDescriptor();

        Map<Integer,String> dsNames= new LinkedHashMap();  // enumeration of all the names that are not in line.
        Map<String,Integer> dsToPosition= new LinkedHashMap(); // name to the index of first column

        int ids= 0; // index of the dataset in the bundleDescriptor.

        String[] names= JSONObject.getNames(jo);
        for ( int ivar=0; ivar<names.length; ivar++ ) {
            String name= names[ivar];
            logger.log( Level.FINE, "processing name[{0}]={1}", new Object[]{ivar, name});
            try {
                JSONObject jo1= jo.getJSONObject(name);
                int[] idims;
                if ( !jo1.has(PROP_DIMENSION) ) {
                    idims= new int[0];

                } else {
                    Object dims= jo1.get(PROP_DIMENSION);
                    
                    if ( dims instanceof JSONArray ) {
                        idims= new int[ ((JSONArray)dims).length() ];
                        for ( int j=0;j<idims.length;j++ ) {
                            idims[j]= ((JSONArray)dims).getInt(j);
                        }
                    } else if ( dims instanceof Integer ) {
                        idims= new int[ (Integer)dims ];
                    } else {
                        throw new IllegalArgumentException( "Expected array for DIMENSION in "+ name );
                    }
                }
                if ( idims.length>1 ) throw new IllegalArgumentException("only rank 2 datasets supported, DIMENSION len="+ idims.length );
                int total= idims.length==0 ? 1 : idims[0];
                for ( int j=1;j<idims.length; j++) {
                    total*= idims[j];
                }

                if ( jo1.has(PROP_ELEMENTS) ) {
                    Object oelements= jo1.get( PROP_ELEMENTS );
                    if ( oelements instanceof JSONArray ) {
                        JSONArray elements= (JSONArray)oelements;
                        String lookFor= elements.getString(0);
                        int icol= -1;
                        for ( int j=0; j<columns.length; j++ ) {
                            if ( columns[j].equals(lookFor) ) {
                                logger.log( Level.FINE, "found column named {0} at {1}", new Object[]{lookFor, j} );
                                icol= j;
                                bd.addDataSet( name, ids, idims );
                                break;
                            }
                        }
                        if ( icol==-1 ) {
                            if ( jo1.has("START_COLUMN") ) {
                                icol=  jo1.getInt("START_COLUMN");
                                logger.log( Level.FINE, "using START_COLUMN={1} property for {0}", new Object[]{lookFor, icol } );
                                bd.addDataSet( name, ids, idims );
                            } else {
                                if ( jo1.has("VALUES") ) {
                                    bd.addDataSet( lookFor, getDataSet( jo1, jo1.getJSONArray("VALUES"), idims ) );
                                } else {
                                    throw new IllegalArgumentException("Couldn't find column starting with: "+lookFor);
                                }
                            }
                        }
                        
                        if ( icol>-1 ) {
                            dsNames.put( ids, name );
                            dsToPosition.put( name, icol );
                            ids+= DataSetUtil.product(idims);
                        }

                        for ( int j=0; j<total;j++ ) {
                            if ( !columns[icol+j].equals(elements.getString(j) ) ) { //TODO: verify this code.
                                throw new IllegalArgumentException("Expected JSON array to contain "+columns[icol+j]+" in ELEMENTS at index= "+(icol+j) );
                            }
                            snames[icol+j]= name;
                        }
                        if ( total!=elements.length() ) throw new IllegalArgumentException("expected "+total+" items in ELEMENTS" );

                    } else {
                        throw new IllegalArgumentException("Expected array for ELEMENTS in "+name );
                    }
                } else {
                    String lookFor= name;
                    int icol= -1;
                    for ( int j=0; j<columns.length; j++ ) {
                        if ( columns[j].equals(lookFor) ) {
                            logger.log( Level.FINE, "found column named {0} at {1}", new Object[]{lookFor, j} );
                            icol= j;
                            bd.addDataSet( name, ids, idims );
                            break;
                        }
                    }
                    if ( icol==-1 ) {
                        if ( jo1.has("START_COLUMN") ) {
                            icol=  jo1.getInt("START_COLUMN");
                            logger.log( Level.FINE, "using START_COLUMN={1} property for {0}", new Object[]{lookFor, icol } );
                            bd.addDataSet( name, ids, idims );
                        } else {
                            if ( jo1.has("VALUES") ) {
                                bd.addDataSet( lookFor, getDataSet( jo1, jo1.getJSONArray("VALUES"), idims ) );
                                continue;
                            } else {
                                throw new IllegalArgumentException("Couldn't find column starting with: "+lookFor);
                            }
                        }
                    }

                    if ( icol>-1 ) {
                       dsNames.put( ids, name );
                       dsToPosition.put( name, icol );
                       ids+= DataSetUtil.product(idims);
                    }

                    for ( int j=0; j<total;j++ ) {
                        if ( snames[icol+j]!=null ) {
                            // it might be nice to allow two variables to use a column. (e.g. virtual variables)
                            throw new IllegalArgumentException("column "+(icol+j)+" is already used by "+snames[icol+j]+", cannot be used by " +name );
                        }
                        snames[icol+j]= name;
                    }
                }

            } catch ( JSONException ex ) {
                ex.printStackTrace();
            }
        }

        bd= bd.resortDataSets( dsToPosition );

        for ( Entry<String,Integer> ee: dsToPosition.entrySet() ) {
            int i= ee.getValue();
            if ( snames[i]==null ) {
                bd.addDataSet( columns[i], ids, new int[0] );
            } 
            ids++;
        }

        return bd;

    }


    /**
     * return the QDataSet 
     * @param arr
     * @param dims
     * @return
     */
    public static QDataSet getDataSet( JSONObject jo, JSONArray values, int[] dims ) throws JSONException {
        double[] dd= new double[ values.length() ];
        for ( int i=0; i<values.length(); i++ ) {
            try {
                dd[i] = values.getDouble(i);
            } catch (JSONException ex) {
                throw ex;
            }
        }
        DDataSet result= DDataSet.wrap( dd, dims );
        fillMetadata1( result, jo );

        return result;
    }

    /**
     * attempt to parse the metadata stored in the header.  The header lines must
     * be prefixed with hash (#).  Loosely formatted test is converted into
     * nicely-formatted JSON and then parsed with a JSON parser.  Note the Java
     * JSON parser itself is pretty loose, for example allowing 1-word strings to
     * go without quotes delimiters.
     *
     * @param header
     * @return
     */
    public static BundleDescriptor parseMetadata( String header, String[] columns ) throws ParseException {
        try {
            JSONObject jo;
            AsciiHeadersParser ahp= new AsciiHeadersParser();
            String sjson= ahp.prep(header);
            jo = new JSONObject( sjson );
            BundleDescriptor bd= calcBundleDescriptor( jo, columns );

            fillMetadata( bd,jo );
            return bd;

        } catch (JSONException ex) {
            throw new ParseException( ex.toString(), 0 );
        }
    }

    public static class BundleDescriptor extends AbstractDataSet {

        int len;
        Map<String,Integer> datasets;
        Map<Integer,String> datasets2;
        Map<String,QDataSet> inlineDataSets;  // in-line datasets, like DEPEND_1.
        Map<Integer,Map<String,Object>> props;
        Map<String,int[]> qubes;

        BundleDescriptor(  ) {
            properties= new LinkedHashMap();
            datasets= new LinkedHashMap();
            datasets2= new LinkedHashMap();
            inlineDataSets= new LinkedHashMap();
            props= new LinkedHashMap();
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


        /**
         * add the named dataset with the dimensions.  Note qube
         * doesn't include the first dimension, and this may be null for
         * rank 1 datasets.
         *
         * @param name name of the dataset
         * @param i  index of the dataset.  These must be contiguous.
         * @param qube the dimensions or null for rank 1 data, e.g. vector= [3]
         */
        protected void addDataSet( String name, int i, int[] qube ) {
            System.err.println( String.format( "addDataSet %s %d %d", name, i, DataSetUtil.product(qube) ) );
            int len= DataSetUtil.product(qube);
            datasets.put( name, i );
            for ( int j=0; j<len; j++ ) {
                datasets2.put( i+j, name );
            }
            putProperty( QDataSet.LABEL, i, name );
            putProperty( QDataSet.NAME, i, name );
            putProperty( QDataSet.START_INDEX, i, i );
            qubes.put( name, qube );
            
        }

        public int rank() {
            return 2;
        }

        @Override
        public int length() {
            return datasets2.size();
        }

        @Override
        public int length(int i) {
            String name= datasets2.get(i);
            int[] qube= qubes.get(name);
            if ( qube==null || qube.length==0 ) {
                return 0;
            } else {
                return qube.length;
            }
        }

        @Override
        public Object property(String name, int ic) {
            synchronized (this) {
                String dsname= datasets2.get(ic);
                int i= datasets.get(dsname);
                Map<String,Object> props1= props.get(i);
                if ( props1==null ) {
                    return null;
                } else {
                    return props1.get(name);
                }
            }
        }

        @Override
        public synchronized void putProperty( String name, int ic, Object v ) {
            String dsname= datasets2.get(ic);
            int i= datasets.get(dsname);
            Map<String,Object> props1= props.get( i );
            if ( props1==null ) {
                props1= new LinkedHashMap<String,Object>();
                props.put( i, props1 );
            }
            if ( name.startsWith( "DEPEND_" ) && !(name.equals("DEPEND_0") ) ) {
                if ( inlineDataSets.containsKey((String)v) ) {
                    props1.put( name, inlineDataSets.get((String)v) );
                } else {
                    System.err.println("unable to resolve property "+name+"="+v+" of "+datasets2.get(i)+".  No such dataset found." );
                    props1.put( name, v );
                }
            } else {
                props1.put( name, v );
            }
        }

        @Override
        public double value(int i0, int i1) {
            String name= datasets2.get(i0);
            int[] qube= qubes.get(name);
            if ( qube==null ) {
                throw new IndexOutOfBoundsException("length=0");
            }
            if ( i1>=qube.length ) {
                throw new ArrayIndexOutOfBoundsException("qube is "+qube.length+".");
            }
            return qube[i1];
            
        }

        private void addDataSet(String lookFor, QDataSet dataSet) {
            inlineDataSets.put( lookFor, dataSet );
        }

       /**
        * the parsed JSON comes in any old order, so we need to resort our data.
        * @param bd
        * @param dsToPosition
        * @return
        */
        BundleDescriptor resortDataSets( Map<String,Integer> dsToPosition ) {
            Map<Integer,String> positionToDs= new LinkedHashMap();

            for ( Entry<String,Integer> entry: dsToPosition.entrySet() ) {
                positionToDs.put( entry.getValue(), entry.getKey() );
            }

            BundleDescriptor newb= new BundleDescriptor();

            int column=0;
            int i=0;
            while ( i< this.length() ) {
                if ( positionToDs.containsKey(column) ) {
                    String name= positionToDs.get(column);
                    int oldIndex= datasets.get(name);
                    int[] qube= qubes.get(name);
                    int len= DataSetUtil.product( qube );
                    newb.addDataSet( name, i, qube );
                    Map<String,Object> pp= props.get( oldIndex );
                    pp.put( QDataSet.START_INDEX, i );
                    newb.props.put( i, pp ); // danger: shallow copy
                    i+= len;
                }
                column++;
            }
            for ( String s: inlineDataSets.keySet() ) {
                newb.addDataSet( s, inlineDataSets.get(s) );
            }
            return newb;
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
        try {
            if ( propName.equals( QDataSet.UNITS ) ) {
                return SemanticOps.lookupUnits( String.valueOf(propValue) );
            } else if ( propName.equals( QDataSet.FILL_VALUE ) ) {
                return Double.parseDouble(String.valueOf(propValue) );
            } else if ( propName.equals( QDataSet.VALID_MIN ) ) {
                return Double.parseDouble(String.valueOf(propValue) );
            } else if ( propName.equals( QDataSet.VALID_MAX ) ) {
                return Double.parseDouble(String.valueOf(propValue) );
            } else if ( propName.equals( QDataSet.TYPICAL_MIN ) ) {
                return Double.parseDouble(String.valueOf(propValue) );
            } else if ( propName.equals( QDataSet.TYPICAL_MAX ) ) {
                return Double.parseDouble(String.valueOf(propValue) );
            } else if ( propName.equals( QDataSet.SCALE_TYPE ) ) {
                return String.valueOf( propValue );
            } else if ( propName.equals( QDataSet.MONOTONIC ) ) {
                return Boolean.valueOf(String.valueOf(propValue) );
            } else if ( propName.equals( QDataSet.CADENCE ) ) {
                return DataSetUtil.asDataSet( DatumUtil.parse( String.valueOf( propValue) ) );
            } else if ( propName.equals( QDataSet.FORMAT ) ) {
                return String.valueOf( propValue );
            } else {
                return String.valueOf( propValue );
            }
        } catch ( ParseException ex ) {
            System.err.println("unable to parse value for "+propName+": "+propValue );
            return null;
            
        } catch ( NumberFormatException ex ) {
            System.err.println("unable to parse value for "+propName+": "+propValue );
            return null;
        }
    }

    /**
     * return a map of metadata for each column or bundled dataset.
     * @param jo
     * @return
     */
    private static void fillMetadata( BundleDescriptor bd, JSONObject jo ) throws JSONException {

        if ( isTranspose(jo) ) {
            throw new IllegalArgumentException("not implemented");
        } else {
            Iterator it= jo.keys();
            for ( ; it.hasNext(); ) {
                 String key= (String) it.next();
                 if ( key.equals("I") ) {
                     System.out.println("Here is I");
                 }
                 Object o= jo.get(key);
                 if ( !( o instanceof JSONObject ) ) {
                     System.err.println("expected JSONObject for value: "+key );
                     continue;
                 } else {
                     System.err.println("KEY: "+key);
                     int ids= bd.indexOf( key ); 
                     if ( ids==-1 ) {
                         logger.log(Level.WARNING, "metadata found for key {0}, but this is not found in the ascii file parser", key);
                         continue;
                     }
                     JSONObject propsj= ((JSONObject)o);
                     bd.putProperty( QDataSet.NAME, ids, key );
                     Iterator props= propsj.keys();
                     for ( ; props.hasNext(); ) {
                         String prop= (String) props.next();
                         Object sv= propsj.get(prop);
                         if ( prop.equals("DIMENSION") || prop.equals("START_COLUMN") || prop.equals("ELEMENT_NAMES") ) {
                            continue;
                         } else if ( prop.equals("UNITS") && ( sv.equals("UTC") || sv.equals("UT") ) ) {
                            bd.putProperty( prop, ids, Units.us2000 );
                         } else if ( prop.equals("ENUM") && sv instanceof JSONArray ) {
                            JSONArray joa= (JSONArray)sv;
                            EnumerationUnits uu= EnumerationUnits.create(key);
                            for ( int i=0; i<joa.length(); i++ ) {
                                uu.createDatum( joa.getString(i) );
                            }
                            bd.putProperty( QDataSet.UNITS, ids, uu );
                         } else {
                            if ( sv instanceof JSONArray || sv instanceof JSONObject ) {
                                System.err.println("invalid value for property "+prop+ ": "+sv );
                            } else {
                                Object v= coerceToType( prop, sv );
                                bd.putProperty( prop, ids, v );

                                if ( prop.equals("DEPEND_1") ) {
                                    bd.putProperty( prop, ids, v );
                                }
                            }
                         }
                     }
                 }
            }
        }        
    }

    private static void fillMetadata1( MutablePropertyDataSet bd, JSONObject jo ) throws JSONException {
         JSONObject propsj= ((JSONObject)jo);
         Iterator props= propsj.keys();
         for ( ; props.hasNext(); ) {
             String prop= (String) props.next();
             Object sv= propsj.get(prop);
             if ( prop.equals("UNITS") && ( sv.equals("UTC") || sv.equals("UT") ) ) {
                bd.putProperty( prop, Units.us2000 );
             } else {
                Object v= coerceToType( prop, sv );
                bd.putProperty( prop, v );
             }
         }
    }


}
