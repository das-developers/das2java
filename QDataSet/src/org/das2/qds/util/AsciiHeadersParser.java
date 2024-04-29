/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.qds.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
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
import org.das2.qds.AbstractDataSet;
import org.das2.qds.DDataSet;
import org.das2.qds.DataSetOps;
import org.das2.qds.DataSetUtil;
import org.das2.qds.MutablePropertyDataSet;
import org.das2.qds.QDataSet;
import org.das2.qds.ops.Ops;

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

    /**
     * property for dimension of the data defining rank and qube dims. For example,
     *   "[]" (default}
     *   "[1]" scalar--alternate form, and [] is preferred.
     *   "[3]" (three element vector)
     *   "[20,30]" (qube of 60 elements).
     */
    public static final String PROP_DIMENSION = "DIMENSION";

    /**
     * NAME identifier to assign to each column of the parameter.  These should follow QDataSet NAME rules.
     * This is always a 1-D array.
     */
    public static final String PROP_ELEMENT_NAMES = "ELEMENT_NAMES";

    /**
     * Human-readable label for each column of the parameter.
     */
    public static final String PROP_ELEMENT_LABELS = "ELEMENT_LABELS";

    private static final Logger logger= Logger.getLogger("qdataset.ascii");

    char commented= '?'; // tri-state: '?' 'T' 'F'

    /**
     * return the next comment line with content, dropping empty lines, or null.
     * The comment line is returned without the comment character
     * @param reader
     * @return
     */
    private String readNextLine( BufferedReader reader ) throws IOException {
         String line = reader.readLine();
         if ( line==null ) return null;
         if ( commented=='?' && line.length()>0 ) {
             commented= line.charAt(0)=='#' ? 'Y' : 'N';
         }
         if ( line.startsWith("#") ) {
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
     * TODO: We see that it's misguided to have all this preprocessing done, since it limits
     * who can process these headers.  Many if all of the features here are going to be removed.
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
            try (BufferedReader reader = new BufferedReader(new StringReader(s))) {
                String line = readNextLine( reader );
                
                //int iline = 1;
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
                    char lastChar;
                    if ( trimLine.length()==0 ) {
                        lastChar= ' ';
                    } else {
                        lastChar= trimLine.charAt(trimLine.length() - 1);
                    }
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
                    //iline++;

                    // If we had an opening brace, then the closing brace can finish off the JSON so additional comments are ignored.
                    if ( expectClosingBrace && braceLevel==0 ) {
                        line=null;
                    }
                    
                }
            }

            if (addClosingBrace) {
                sb.append("}");
            }
            return sb.toString();

        } catch (IOException ex) {
            throw new RuntimeException(ex);

        }

    }

    private static String[] toStringArray( JSONArray ja ) throws JSONException {
        String[] result= new String[ ja.length() ];
        for ( int i=0; i<result.length; i++ ) {
            result[i]= ja.getString(i);
        }
        return result;
    }

    private static void calcUserProperties( JSONObject jo, Map<String,Object> result ) throws JSONException {
        String[] names= JSONObject.getNames(jo);
        for (String name : names) {
            Object val = jo.get(name);
            if (val instanceof JSONObject) {
                Map<String,Object> child= new HashMap<>();
                calcUserProperties( (JSONObject)jo, child );
            } else if (val instanceof JSONArray) {
                result.put(name, (JSONArray)val); //TODO: convert this
            } else {
                result.put(name, val);
            }
        }
    }

    /**
     * calculate the bundle descriptor, possibly folding together columns to create
     * high rank datasets, reading JSONHeadedASCII (rich ascii)
     * @param jp the JSON object describing
     * @param columns column names.
     * @param columnLabels human-consumable labels for each column
     */
    private static BundleDescriptor calcBundleDescriptor( JSONObject jo, String[] columns, String[] columnLabels ) {

        String[] snames= new String[ columns.length ];

        BundleDescriptor bd= new BundleDescriptor();

        String dep0Name= null;
        
        //Map<Integer,String> dsNames= new LinkedHashMap();  // enumeration of all the names that are not in line.
        Map<String,Integer> dsToPosition= new LinkedHashMap<>(); // name to the index of first column
        
        int ids= 0; // index of the dataset in the bundleDescriptor.

        Map<JSONObject, String> messages= new LinkedHashMap<>();

        String[] names= JSONObject.getNames(jo);
        for ( int ivar=0; ivar<names.length; ivar++ ) {
            String jsonName= names[ivar];
            String name= Ops.safeName(jsonName);
            logger.log( Level.FINE, "processing name[{0}]={1}", new Object[]{ivar, jsonName});
            try {
                JSONObject jo1;
                Object o= jo.get(jsonName);
                
                if ( o instanceof JSONObject ) {
                    jo1= (JSONObject)o;
                } else {
                    Object oval= bd.property(QDataSet.USER_PROPERTIES);
                    if ( oval!=null ) {
                        if ( !( oval instanceof Map ) ) {
                            throw new IllegalArgumentException("USER_PROPERTIES is not a map");
                        }
                    }
                    @SuppressWarnings("unchecked")
                    Map<String,Object> val= (Map<String,Object>)oval;
                    if ( val==null ) {
                        val= new HashMap<>();
                        bd.putProperty(QDataSet.USER_PROPERTIES, val);
                    }
                    val.put( jsonName, o );
                    continue;
                }
                
                if ( jsonName.equals( QDataSet.USER_PROPERTIES ) ) {
                    Map<String,Object> val= new HashMap<>();
                    calcUserProperties( jo1,val );
                    bd.putProperty( jsonName,val );
                    continue; 
                }
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
                        if ( idims.length==1 && idims[0]==1 && !jo1.has(PROP_ELEMENT_NAMES) ) { // Firebird file has 1-element arrays instead of scalars.
                            idims= new int[0];
                        }
                    } else if ( dims instanceof Integer ) {
                        idims= new int[ 1 ];
                        idims[0]= (Integer)dims;
                    } else {
                        throw new IllegalArgumentException( "Expected array for DIMENSION in "+ jsonName );
                    }
                }
                //if ( idims.length>1 ) {
                //    throw new IllegalArgumentException("only rank 2 datasets supported, DIMENSION len="+ idims.length );
                //}
                int total= idims.length==0 ? 1 : idims[0];
                for ( int j=1;j<idims.length; j++) {
                    total*= idims[j];
                }

                String[] labels=null;
                if ( jo1.has( PROP_ELEMENT_LABELS ) ) {
                    Object olabels= jo1.get( PROP_ELEMENT_LABELS );
                    if ( olabels instanceof JSONArray ) {
                        labels= toStringArray((JSONArray)olabels);
                    } else if ( olabels instanceof String && total==1 ) {
                        logger.log(Level.FINE, "scalar for 1-element array for ELEMENT_LABELS in {0} is acceptable", jsonName);
                        labels= new String[] { (String)olabels }; 
                    } else {
                        logger.log(Level.FINE, "unable to use ELEMENT_LABELS in {0}, should be array", jsonName);
                    }
                }
                String[] elementNames= null;
                if ( jo1.has( PROP_ELEMENT_NAMES ) ) {
                    Object oelements= jo1.get( PROP_ELEMENT_NAMES );
                    if ( oelements instanceof JSONArray ) {
                        elementNames= toStringArray((JSONArray)oelements);
                    } else if ( oelements instanceof String && total==1 ) {
                        logger.log(Level.FINE, "scalar for 1-element array for ELEMENT_NAMES in {0} is acceptable", jsonName);
                        elementNames= new String[] { (String)oelements  }; 
                    } else {
                        logger.log(Level.FINE, "unable to use ELEMENT_NAMES in {0}, should be array", jsonName);
                    }
                }

                if ( elementNames!=null ) { //TODO: eliminate repeated code.
                        for (String elementName : elementNames) {
                            if (elementName == null) {
                                throw new IllegalArgumentException("rich ascii JSON header contains error");
                            }
                        }
                
                        String lookFor= elementNames[0]; //Note ELEMENT_NAMES must correspond to adjacent columns.
                        int icol= -1;
                        int count= 0;
                        List<Integer> icols= new ArrayList<>();
                        if ( !jo1.has("VALUES") ) {
                            //early version of JSONHeadedASCII (rich ascii) allowed lookups.
                            for ( int j=0; j<columns.length; j++ ) {
                                if ( columns[j].equals(lookFor) ) {
                                    logger.log( Level.FINE, "found column named {0} at {1}", new Object[]{lookFor, j} );
                                    if ( count==0 ) icol= j;
                                    count++;
                                    icols.add(j);
                                }
                            }
                        }
                        if ( icol!=-1 ) {
                            if ( count>1 ) {
                                logger.log(Level.WARNING, "Multiple columns have label \"{0}\": {1}", new Object[] { lookFor, icols } );
                                if ( jo1.has("START_COLUMN") ) {
                                    icol=  jo1.getInt("START_COLUMN");
                                    logger.log( Level.FINE, "using START_COLUMN={1} property for {0}", new Object[]{lookFor, icol } );
                                } else {
                                    logger.log( Level.FINE, "using first column ({1}) for {0}", new Object[]{lookFor, icol } );
                                }
                                if ( labels==null ) {
                                    labels= new String[elementNames.length];
                                    for ( int i=0; i<elementNames.length; i++ ) labels[i]= columnLabels[i+icol];
                                }
                                bd.addDataSet( name, ids, idims, elementNames, labels );
                            } else {
                                if ( labels==null ) {
                                    labels= new String[elementNames.length];
                                    for ( int i=0; i<elementNames.length; i++ ) labels[i]= columnLabels[i+icol];
                                }
                                bd.addDataSet( name, ids, idims, elementNames, labels );
                            }
                        } else {
                            if ( jo1.has("START_COLUMN") ) {
                                icol=  jo1.getInt("START_COLUMN");
                                logger.log( Level.FINE, "using START_COLUMN={1} property for {0}", new Object[]{lookFor, icol } );
                                if ( labels==null ) {
                                    labels= new String[elementNames.length];
                                    for ( int i=0; i<elementNames.length; i++ ) {
                                        labels[i]= columnLabels[i+icol];
                                        if ( labels[i]==null ) labels[i]= elementNames[i];
                                    }
                                }
                                bd.addDataSet( name, ids, idims, elementNames, labels );
                            } else {
                                if ( jo1.has("VALUES") ) {
                                    logger.log( Level.FINE, "missing START_COLUMN element, {0} must be a DEPEND_1 dataset", name );
                                    int n= DataSetUtil.product(idims);
                                    JSONArray arr= jo1.getJSONArray("VALUES");
                                    if ( n!=arr.length() ) {
                                        throw new IllegalArgumentException("VALUES element doesn't match DIMENSION under "+ jsonName );
                                    }
                                    DDataSet vv= getDataSet( jo1, arr, idims );
                                    vv.putProperty( QDataSet.NAME, name );
                                    //TODO: we have to ignore ELEMENT_NAMES and ELEMENT_LABELS for now, there's no place in QDataSet for them.
                                    bd.addDataSet( name, vv );
                                } else {
                                    throw new IllegalArgumentException("Couldn't find column starting with: "+lookFor);
                                }
                            }
                        }
                        
                        if ( icol>-1 ) {
                            //dsNames.put( ids, name );
                            dsToPosition.put( name, icol );
                            ids+= DataSetUtil.product(idims);
                        }
                        
                        if ( icol>-1 ) {
                            for ( int j=0; j<total;j++ ) {
    //                            if ( !columns[icol+j].equals(elementNames[j] ) ) { //TODO: verify this code.
    //                                throw new IllegalArgumentException("Expected JSON array to contain "+columns[icol+j]+" in ELEMENTS at index= "+(icol+j) );
    //                            }
                                snames[icol+j]= name;
                            }
                        }
                        if ( total!=elementNames.length ) {
                            throw new IllegalArgumentException("expected "+total+" items in ELEMENTS" );
                        }

                } else {
                    String lookFor= name;
                    int icol= -1;
                    if ( total>1 ) {
                        elementNames= new String[total];
                        for ( int i=0; i<total; i++ ) {
                            elementNames[i]= name + "_" + i;
                        }
                    }
                    for ( int j=0; j<columns.length; j++ ) {
                        if ( columns[j].equals(lookFor) ) {
                            logger.log( Level.FINE, "found column named {0} at {1}", new Object[]{lookFor, j} );
                            icol= j;
                            bd.addDataSet( name, ids, idims, elementNames, labels ); // findbugs NP_LOAD_OF_KNOWN_NULL_VALUE
                            break;
                        }
                    }
                    if ( icol==-1 ) {
                        if ( jo1.has("START_COLUMN") ) {
                            icol=  jo1.getInt("START_COLUMN");
                            logger.log( Level.FINE, "using START_COLUMN={1} property for {0}", new Object[]{lookFor, icol } );
                            bd.addDataSet( name, ids, idims, elementNames, labels );
                        } else {
                            if ( jo1.has("VALUES") ) {
                                int n= DataSetUtil.product(idims);
                                JSONArray arr= jo1.getJSONArray("VALUES");
                                if ( n!=arr.length() ) {
                                    throw new IllegalArgumentException("VALUES element doesn't match DIMENSION under "+ jsonName );
                                }
                                bd.addDataSet( name, getDataSet( jo1, arr, idims ) );
                                continue;
                            } else {
                                messages.put(jo1,"Couldn't find column starting with: "+lookFor);
                                if ( columns[0].equals("field0") ) {
                                    throw new IllegalArgumentException("Couldn't find column starting with \""+lookFor+"\".  Are the columns named?");
                                }
                            }
                        }
                    }

                    if ( icol>-1 ) {
                       //dsNames.put( ids, name );
                       dsToPosition.put( name, icol );
                       ids+= DataSetUtil.product(idims);
                    }

                    if ( icol>-1 ) {
                        for ( int j=0; j<total;j++ ) {
                            if ( snames[icol+j]!=null ) {
                                // it might be nice to allow two variables to use a column. (e.g. virtual variables)
                                messages.put(jo1,"column "+(icol+j)+" is already used by "+snames[icol+j]+", cannot be used by " +name );
                            }
                            snames[icol+j]= name;
                        }
                    }

                    if ( icol==0 ) {
                        if ( jo1.optString("dtype","").equals("UTC") || jo1.optString("UNITS","").equals("UTC") ) {
                           dep0Name=name;
                        }
                    }

                }

            } catch ( JSONException ex ) {
                logger.log( Level.WARNING, "Exception encountered when handling {0}:", jsonName);
                logger.log( Level.WARNING, ex.toString(), ex );
            }
        }
        
        if ( dep0Name!=null ) {
            for ( int i=0; i<bd.length(); i++ ) {
                if ( !dep0Name.equals( bd.property( QDataSet.NAME, i ) ) ) {
                    if ( bd.property( QDataSet.DEPENDNAME_0, i)==null ) {
                        bd.putProperty( QDataSet.DEPENDNAME_0, i, dep0Name );
                    }
                }
            }
        }

        if ( messages.size()>0 ) {
            for ( Entry<JSONObject,String> jos1: messages.entrySet() ) {
                logger.log( Level.INFO, "{0}", jos1.getValue() );
            }
        }

        Map<String,Object> props= DataSetUtil.getProperties( bd, DataSetUtil.globalProperties(), null );
        bd= bd.resortDataSets( dsToPosition );
        DataSetUtil.putProperties( props, bd );

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
    private static DDataSet getDataSet( JSONObject jo, JSONArray values, int[] dims ) throws JSONException {
        double[] dd= new double[ values.length() ];
        Object[] oo= new Object[ values.length() ];
        Units u= Units.dimensionless;
        for ( int i=0; i<values.length(); i++ ) {
            try {
                dd[i] = values.getDouble(i);
            } catch (JSONException ex) {
                String s= values.getString(i);
                try {
                    if ( s.contains(",") ) {
                        String[] ss= s.split(",");
                        double[] dd1= new double[ss.length];
                        for ( int j=0; j<ss.length; j++ ) {
                            dd1[j]= Double.parseDouble(ss[j]);
                        }
                        oo[i]= dd1;
                    } else {
                        dd[i]= Units.us2000.parse(s).doubleValue(Units.us2000);
                        u= Units.us2000;
                    }
                } catch ( ParseException ex2 ) {
                    // "-7.846400, -24.376616, 30485.856600" "UNITS":"Deg., Deg., km","DIMENSION":[2],"NAME":"Apogee Pos Geodetic",
                    throw ex;
                }
            }
        }
        DDataSet result;
        if ( oo[0]!=null ) {
            int n= Array.getLength(oo[0]);
            result= DDataSet.createRank2(values.length(),n);
            for ( int i=0; i<oo.length; i++ ) {
                for ( int j=0; j<n; j++ ) {
                    result.putValue( i, j, Array.getDouble(oo[i],j) );
                }
            }
        } else {
            result= DDataSet.wrap( dd, dims );
        }
        if ( u!=Units.dimensionless ) result.putProperty( QDataSet.UNITS, u );
        fillMetadata1( result, jo );

        return result;
    }

    private static class ParamDescription {
        boolean hasFill= false;
        double fillValue= -1e38;
        Units units= Units.dimensionless;
        String name= "";
        String description= "";
        private ParamDescription( String name ) {
            this.name= name;
        }
        public boolean getHasFill() {
            return hasFill;
        }
        public double getFillValue() {
            return fillValue;
        }
    }
        
    public static BundleDescriptor parseMetadataHapi( JSONObject doc ) throws JSONException, ParseException {
        JSONArray parameters= doc.getJSONArray("parameters");
        int nparameters= parameters.length();

        ParamDescription[] pds= new ParamDescription[nparameters];

        for ( int i=0; i<nparameters; i++ ) {
            String name= parameters.getJSONObject(i).getString("name");
            pds[i]= new ParamDescription( name );

            String type;
            if ( parameters.getJSONObject(i).has("type") ) {
                type= parameters.getJSONObject(i).getString("type");
                if ( type==null ) type="";
            } else {
                type= "";
            }
            if ( type.equals("") ) {
                logger.log(Level.FINE, "type is not defined: {0}", name);
            }
            if ( type.equalsIgnoreCase("isotime") ) {
                if ( !type.equals("isotime") ) {
                    logger.log(Level.WARNING, "isotime should not be capitalized: {0}", type);
                }
                pds[i].units= Units.us2000;
            } else {
                if ( parameters.getJSONObject(i).has("units") ) {
                    String sunits= parameters.getJSONObject(i).getString("units");
                    if ( sunits!=null ) {
                        pds[i].units= Units.lookupUnits(sunits);
                    }
                } else {
                    pds[i].units= Units.dimensionless;
                }
                if ( parameters.getJSONObject(i).has("fill") ) {
                    String sfill= parameters.getJSONObject(i).getString("fill");
                    if ( sfill!=null ) {
                        pds[i].fillValue= pds[i].units.parse( sfill ).doubleValue( pds[i].units );
                        pds[i].hasFill= true;
                    }
                } else {
                    pds[i].fillValue= -1e31; // when a value cannot be parsed, but it is not identified.
                }
                if ( parameters.getJSONObject(i).has("description") ) {                   
                    pds[i].description= parameters.getJSONObject(i).getString("description");
                    if ( pds[i].description==null ) pds[i].description= "";
                } else {
                    pds[i].description= ""; // when a value cannot be parsed, but it is not identified.
                }
            }
        }
        BundleDescriptor bds= new BundleDescriptor();
        for ( int i=0; i<pds.length; i++ ) {
            bds.addDataSet( Ops.safeName(pds[i].name), i, new int[0] );
        }
                
        return bds; 
    }
    
    /**
     * attempt to parse the JSON metadata stored in the header.  The header lines must
     * be prefixed with hash (#).  Loosely formatted text is converted into
     * nicely-formatted JSON and then parsed with a JSON parser.  Note the Java
     * JSON parser itself is pretty loose, for example allowing 1-word strings to
     * go without quote delimiters.  The scheme for this header is either:<ul>
     * <li>"rich ascii" or http://autoplot.org/richAscii
     * <li>"hapi" https://github.com/hapi-server/data-specification/blob/master/hapi-dev/HAPI-data-access-spec-dev.md#info
     * </ul>
     * @see http://autoplot.org/richAscii
     * @see https://github.com/hapi-server/data-specification/blob/master/hapi-dev/HAPI-data-access-spec-dev.md#info
     * @param header the JSON header
     * @param columns identifiers for each column
     * @param columnLabels labels for each column
     * @return the BundleDescriptor
     * @throws java.text.ParseException
     */
    public static BundleDescriptor parseMetadata( String header, String[] columns, String[] columnLabels ) throws ParseException {
        try {
            JSONObject jo;
            AsciiHeadersParser ahp= new AsciiHeadersParser();
            String sjson= ahp.prep(header);
            jo = new JSONObject( sjson );
            if ( jo.has("HAPI") ) {
                BundleDescriptor bd= parseMetadataHapi( jo );
                return bd;
            } else {
                BundleDescriptor bd= calcBundleDescriptor( jo, columns, columnLabels );

                fillMetadata( bd,jo );
                return bd;
            }

        } catch (JSONException | IllegalArgumentException ex) {
            ex.printStackTrace();
            throw new ParseException( ex.toString(), 0 );
        }
    }

    public static class BundleDescriptor extends AbstractDataSet {

        Map<String,Integer> datasets;
        Map<Integer,String> datasets2;
        Map<String,QDataSet> inlineDataSets;  // in-line datasets, like DEPEND_1.
        Map<Integer,Map<String,Object>> props;
        Map<String,int[]> qubes;

        BundleDescriptor(  ) {
            properties= new LinkedHashMap<>();
            datasets= new LinkedHashMap<>();
            datasets2= new LinkedHashMap<>();
            inlineDataSets= new LinkedHashMap<>();
            props= new LinkedHashMap<>();
            qubes= new LinkedHashMap<>();
        }

        public int indexOf( String name ) {
            Integer i= datasets.get(name);
            if ( i==null ) {
                return -1;
            } else {
                return i;
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
            addDataSet( name, i, qube, null, null );
        }

        /**
         * add the named dataset with the dimensions.  Note qube
         * doesn't include the first dimension, and this may be null for
         * rank 1 datasets.
         *
         * @param name name of the dataset
         * @param i  index of the dataset.  These must be contiguous.
         * @param qube the dimensions or null for rank 1 data, e.g. vector= [3]
         * @param names the names for each column.  See QDataSet NAME property.  This implies Vector.
         * @param labels the labels for each column.  See QDataSet LABEL property.
         */
        protected void addDataSet( String name, int i, int[] qube, String[] names, String[] labels ) {
            int len= DataSetUtil.product(qube);
            name= Ops.safeName(name);
            datasets.put( name, i );
            for ( int j=0; j<len; j++ ) {
                datasets2.put( i+j, name );
            }
            putProperty( QDataSet.LABEL, i, name );
            putProperty( QDataSet.NAME, i, name );
            if ( qube.length>0 ) {
                putProperty( QDataSet.QUBE, i, Boolean.TRUE );
                putProperty( QDataSet.ELEMENT_NAME, i, name );
                putProperty( QDataSet.ELEMENT_LABEL, i, name );
                putProperty( QDataSet.START_INDEX, i, i ); // datasets2 does the mapping.
            }
            if ( qube.length>0 && names!=null ) {
                for ( int k=0; k<names.length; k++ ) {  
                    names[k]= Ops.safeName(names[k]);
                }
            }
            if ( names!=null ) putProperty( PROP_ELEMENT_NAMES, i, names );
            if ( labels!=null ) putProperty( PROP_ELEMENT_LABELS, i, labels );
            
            qubes.put( name, qube );
            
        }

        private void addDataSet(String lookFor, QDataSet dataSet) {
            inlineDataSets.put( lookFor, dataSet );
        }

        @Override
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
                if ( datasets==null || datasets.get(dsname)==null ) {
                    throw new IllegalArgumentException("No slice at "+ic );
                }
                int ids= datasets.get(dsname); // index of the beginning of the rank 2 dataset
                if ( name.equals( QDataSet.NAME ) ) {
                    Map<String,Object> props1= props.get(ids);
                    if ( props1!=null ) {
                        String[] names= (String[]) props1.get( PROP_ELEMENT_NAMES );
                        if ( names!=null ) {
                            return names[ic-ids];
                        }
                    }
                }
                if ( name.equals( QDataSet.LABEL ) ) {
                    Map<String,Object> props1= props.get(ids);
                    if ( props1!=null ) {
                        String[] labels= (String[]) props1.get( PROP_ELEMENT_LABELS );
                        if ( labels==null ) {
                            labels= (String[]) props1.get( PROP_ELEMENT_NAMES );
                        }
                        if ( labels!=null ) {
                            if ( ic-ids>=labels.length ) {
                                return ""; // http://solar.physics.montana.edu/FIREBIRD_II/Data/FU_3/hires/FU3_Hires_2018-10-01_L2.txt
                            } else {
                                return labels[ic-ids];
                            }
                        }
                    }
                }
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
                props1= new LinkedHashMap<>();
                props.put( i, props1 );
            }
            if ( name.equals( QDataSet.DEPENDNAME_1 ) ) {
                QDataSet dep1= inlineDataSets.get((String)v);
                if ( dep1!=null ) {
                    props1.put( QDataSet.DEPEND_1, inlineDataSets.get((String)v) );
                } else {
                    logger.log(Level.WARNING, "unable to resolve property \"{0}\"=\"{1}\" of \"{2}\".  No such dataset found.", new Object[]{name, v, datasets2.get(i)});
                    props1.put( name, v );
                }
            } else {
                if ( name.startsWith( "DEPEND_" ) && !(name.equals("DEPEND_0") ) && v instanceof String ) {
                    if ( inlineDataSets.containsKey((String)v) ) {
                        props1.put( name, inlineDataSets.get((String)v) );
                    } else {
                        logger.log(Level.WARNING, "unable to resolve property \"{0}\"=\"{1}\" of \"{2}\".  No such dataset found.", new Object[]{name, v, datasets2.get(i)});
                        throw new IllegalArgumentException("unable to resolve property \""+name+"\"=\""+v+"\" of \""+datasets2.get(i)+"\".  No such dataset found." );
                        //props1.put( name, v );
                    }
                } else {
                    props1.put( name, v );
                }
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

        /**
         * special code because of START_INDEX property.  Must trim at DataSet boundaries.
         * @param start
         * @param end
         * @return
         */
        @Override
        public QDataSet trim(int start, int end) {
            return DataSetOps.trim( this, start, end-start );
            //throw new IllegalArgumentException("Not supported");
        }


       /**
        * the parsed JSON comes in any old order, so we need to resort our data.
        * @param bd
        * @param dsToPosition
        * @return
        */
        BundleDescriptor resortDataSets( Map<String,Integer> dsToPosition ) {
            Map<Integer,String> positionToDs= new LinkedHashMap<>();

            int maxColumn=-1;
            for ( Entry<String,Integer> entry: dsToPosition.entrySet() ) {
                if ( positionToDs.get(entry.getValue())!=null ) {
                    throw new IllegalArgumentException("two datasets occupy the same position: "+entry.getKey()+","+positionToDs.get(entry.getValue()) );
                }
                positionToDs.put( entry.getValue(), entry.getKey() );
                if ( maxColumn<entry.getValue() ) maxColumn= entry.getValue();
            }

            BundleDescriptor newb= new BundleDescriptor();

            
            int column=0;
            int i=0;
            while ( i< this.length() && column<=maxColumn ) {
                if ( positionToDs.containsKey(column) ) {
                    String name= positionToDs.get(column);
                    Integer ioldIndex= datasets.get(name);
                    if ( ioldIndex==null ) {
                        logger.log(Level.WARNING, "unable to find dataset for \"{0}\"", name);
                    }
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
            for ( Entry<String,QDataSet> e: inlineDataSets.entrySet() ) {
                newb.addDataSet( e.getKey(), e.getValue() );
            }
            return newb;
        }

    }

    /**
     * provide mapping from JSON object type to QDataSet property type.
     * @param propName
     * @param propValue
     * @return
     */
    private static Object coerceToType( String propName, Object propValue ) {
        try {
            switch (propName) {
                case QDataSet.UNITS:
                    return Units.lookupUnits( String.valueOf(propValue) );
                case QDataSet.FILL_VALUE:
                    return Double.parseDouble(String.valueOf(propValue) );
                case QDataSet.VALID_MIN:
                    return Double.parseDouble(String.valueOf(propValue) );
                case QDataSet.VALID_MAX:
                    return Double.parseDouble(String.valueOf(propValue) );
                case QDataSet.TYPICAL_MIN:
                    return Double.parseDouble(String.valueOf(propValue) );
                case QDataSet.TYPICAL_MAX:
                    return Double.parseDouble(String.valueOf(propValue) );
                case QDataSet.SCALE_TYPE:
                    return String.valueOf( propValue );
                case QDataSet.MONOTONIC:
                    return Boolean.valueOf(String.valueOf(propValue) );
                case QDataSet.CADENCE:
                    return DataSetUtil.asDataSet( DatumUtil.parse( String.valueOf( propValue) ) );
                case QDataSet.FORMAT:
                    return String.valueOf( propValue );
                default:
                    return String.valueOf( propValue );
            }
        } catch ( ParseException | NumberFormatException ex ) {
            logger.log(Level.WARNING, "unable to parse value for {0}: {1}", new Object[]{propName, propValue});
            return null;
            
        }
    }

    /**
     * convert JSONArray of strings or numbers to Java array
     * @param array the JSONArray
     * @param c the Java class of the objects.
     * @return the Java array
     * @throws JSONException 
     */
    private static Object convertJsonArray( JSONArray array, Class c ) throws JSONException {
        Object result= Array.newInstance( c, array.length() );
        for ( int i=0; i<array.length(); i++ ) {
            Array.set( result, i, array.get(i) );
        }
        return result;
    }
    
    /**
     * return a map of metadata for each column or bundled dataset.
     * @param jo
     * @return
     */
    private static void fillMetadata( BundleDescriptor bd, JSONObject jo ) throws JSONException {

        Iterator it= jo.keys();
        for ( ; it.hasNext(); ) {
             String key= (String) it.next();
             Object o= jo.get(key);
             if ( !( o instanceof JSONObject ) ) {
                 Object oUserProperties=bd.property( QDataSet.USER_PROPERTIES );
                 if ( oUserProperties!=null ) {
                     if ( !( oUserProperties instanceof Map ) ) {
                         throw new IllegalArgumentException("USER_PROPERTIES is not a map");
                     }
                 }
                 Map<String,Object> userProperties= (Map<String,Object>)oUserProperties ;

                 if ( userProperties==null ) {
                      userProperties= new LinkedHashMap<>();
                      bd.putProperty( QDataSet.USER_PROPERTIES, userProperties );
                 }
                 
                 if ( o instanceof JSONArray ) {
                     JSONArray ja= ((JSONArray)o);
                     String[] arr= new String[ ja.length() ];
                     for ( int i=0; i<arr.length; i++ ) {
                         arr[i]= ja.get(i).toString();
                     }
                     userProperties.put( key, arr );
                 } else {
                     userProperties.put( key, o.toString() ); 
                 }

             } else {
                 String name= Ops.safeName(key);
                 int ids= bd.indexOf( name );
                 if ( ids==-1 ) {
                     JSONObject inlineObject= (JSONObject)o;
                     if ( !inlineObject.has("VALUES") ) {
                         // this is when ancillary metadata is in header, and is fine to ignore.
                         logger.log(Level.FINE, "metadata found for key {0}, but values are not found in the ascii file columns", key);
                         continue;
                     } else {
                         // inline dataset already has metadata.
                         continue;
                     }
                 }
                 JSONObject propsj= ((JSONObject)o);
                 bd.putProperty( QDataSet.NAME, ids, name );
                 Iterator props= propsj.keys();
                 for ( ; props.hasNext(); ) {
                     String prop= (String) props.next();
                     Object sv= propsj.get(prop);
                     if ( prop.equals( PROP_DIMENSION ) || prop.equals( "START_COLUMN") || prop.equals("ELEMENT_NAMES") || prop.equals("ELEMENT_LABELS") ) {
                         if ( prop.equals("ELEMENT_NAMES") && sv instanceof JSONArray ) {
//                                 String[] ss= toStringArray( (JSONArray)sv );
//                                 String[] labels= toStringArray( (JSONArray)sv );
//                                 for ( int i=0; i<ss.length; i++ ) {
//                                     if ( ss[i].startsWith("|") )  {
//                                         ss[i]= Ops.safeName(ss[i]);
//                                     }
//                                 }
//                                 if ( propsj.has("ELEMENT_LABELS") ) {
//                                     Object el= propsj.get("ELEMENT_LABELS");
//                                     if ( el instanceof JSONArray ) {
//                                         labels= toStringArray( (JSONArray)el );
//                                     }
//                                 }
//                                 if ( propsj.has("DEPEND_1") ) { // in the CRRES file, we have both the channel labels enumerated, and a pitch angle dependence.
//                                     BundleDescriptor bds= new BundleDescriptor();
//                                     for ( int i=0; i<ss.length; i++ ) {
//                                         bds.addDataSet( Ops.safeName(ss[i]), i, new int[0] );
//                                         bds.putProperty( QDataSet.LABEL, i, labels[i] );
//                                     }
//                                     bd.putProperty( QDataSet.BUNDLE_1, ids, bds );
//                                    //bd.putProperty( "DEPEND_1", ids, Ops.labels( ss ) );
//                                 } else {
//                                    bd.putProperty( "DEPEND_1", ids, Ops.labels( ss ) );
//                                 }
                         }
                         if ( bd.property( "RENDER_TYPE", ids )==null ) {
                            bd.putProperty( "RENDER_TYPE", ids, "series" );
                         }

                     } else if ( prop.equals("UNITS") && ( sv.equals("UTC") || sv.equals("UT") ) ) {
                        bd.putProperty( prop, ids, Units.us2000 );
                     } else if ( prop.equals("dtype") && ( sv.equals("UTC") || sv.equals("UT") ) ) {
                        bd.putProperty( "UNITS", ids, Units.us2000 );
                     } else if ( prop.equals("ENUM") && sv instanceof JSONArray ) {
                        JSONArray joa= (JSONArray)sv;
                        EnumerationUnits uu= EnumerationUnits.create(name);
                        for ( int i=0; i<joa.length(); i++ ) {
                            uu.createDatum( joa.getString(i) );
                        }
                        bd.putProperty( QDataSet.UNITS, ids, uu );
                     } else if ( prop.equals( "LABEL" ) ) {
                        if ( bd.length(ids)>0 ) {
                            bd.putProperty( QDataSet.ELEMENT_LABEL, ids, sv );
                        } else {
                            bd.putProperty( QDataSet.LABEL, ids, sv );
                        }
                     } else if ( prop.equals("DEPEND_0") ) {
                         //bd.putProperty( QDataSet.DEPENDNAME_0, ids, sv );
                     } else if ( prop.equals("DEPEND_1") ) {
                         bd.putProperty( QDataSet.DEPENDNAME_1, ids, sv );
                     } else {
                        if ( sv instanceof JSONArray ) {
                            JSONArray asv= (JSONArray)sv;
                            Object item= asv.get(0);
                            Class clas= item.getClass();
                            boolean allSameValue= true;
                            boolean allSameClass= true;
                            for ( int i=1; i<asv.length(); i++ ) {
                                if ( !item.equals(asv.get(i)) ) allSameValue= false;
                                if ( !item.getClass().equals(clas) ) allSameClass= false;
                            }
                            if ( allSameValue ) {
                                Object v= coerceToType( prop, item );
                                bd.putProperty( prop, ids, v );
                            } else {
                                if ( DataSetUtil.isDimensionProperty(prop) ) {
                                    logger.log(Level.WARNING, "invalid value for property {0}: {1}", new Object[]{prop, sv});
                                } else {
                                    Object oUserProperties=bd.property( QDataSet.USER_PROPERTIES, ids );
                                    if ( oUserProperties==null ) { //TODO: still needs attention
                                        if ( oUserProperties==null ) {
                                            oUserProperties= new LinkedHashMap<>();
                                            bd.putProperty( QDataSet.USER_PROPERTIES, ids, oUserProperties );
                                        }
                                    }
                                    if ( oUserProperties!=null ) {
                                        if ( !( oUserProperties instanceof Map ) ) throw new IllegalArgumentException("USER_PROPERTIES is not a map");
                                        Map<String,Object> userProperties= (Map<String,Object>)oUserProperties ;
                                        if ( allSameClass ) {
                                            userProperties.put( prop, convertJsonArray( asv, clas ) );
                                        } else {
                                            userProperties.put( prop, convertJsonArray( asv, Object.class ) );
                                        }
                                    }
                                }
                            }
                        } else if ( sv instanceof JSONObject ) {
                            logger.log(Level.WARNING, "invalid value for property {0}: {1}", new Object[]{prop, sv});
                        } else {
                            Object v= coerceToType( prop, sv );
                            try {
                                bd.putProperty( prop, ids, v );
                            } catch ( IllegalArgumentException ex ) {
                                bd.putProperty( prop, ids, null );
                                ex.printStackTrace();
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

    /**
     * allow inline dataset to be retrieved.
     * @param bds a BundleDescriptor, from BUNDLE_1.  This must have been created by this code.
     * @param name the name of the inline dataset
     * @return the dataset, or null if the dataset is not found.
     * @throws IllegalArgumentException if the dataset not a BundleDescriptor.
     */
    public static QDataSet getInlineDataSet( QDataSet bds, String name ) {
        if ( bds instanceof BundleDescriptor ) {
            Map<String,QDataSet> inlineDataSets= ((BundleDescriptor)bds).inlineDataSets;
            QDataSet result= inlineDataSets.get(name);
            return result;
        } else {
            throw new IllegalArgumentException("bds is not a BundleDescriptor created by this class");
        }
    }

    /**
     * return the list of inline dataset names.  This was probably used during
     * development.
     * @param bds bundle dataset descriptor, though only BundleDescriptor is supported.
     * @return the inline dataset names.
     */
    public static String[] getInlineDataSetNames( QDataSet bds ) {
        if ( bds instanceof BundleDescriptor ) {
            Map<String,QDataSet> inlineDataSets= ((BundleDescriptor)bds).inlineDataSets;
            return inlineDataSets.keySet().toArray( new String[inlineDataSets.size()] );
        } else {
            throw new IllegalArgumentException("bds is not a BundleDescriptor created by this class");
        }
    }

}
