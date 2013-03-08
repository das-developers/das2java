/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.dsutil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
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
import org.das2.datum.Datum;
import org.das2.datum.DatumUtil;
import org.das2.datum.EnumerationUnits;
import org.das2.datum.Units;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.virbo.dataset.AbstractDataSet;
import org.virbo.dataset.DDataSet;
import org.virbo.dataset.DataSetOps;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.MutablePropertyDataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SemanticOps;
import org.virbo.dsops.Ops;

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

    private static String[] toStringArray( JSONArray ja ) throws JSONException {
        String[] result= new String[ ja.length() ];
        for ( int i=0; i<result.length; i++ ) {
            result[i]= ja.getString(i);
        }
        return result;
    }

    private static void calcUserProperties( JSONObject jo, Map<String,Object> result ) throws JSONException {
        String[] names= JSONObject.getNames(jo);
        for ( int i=0; i<names.length; i++ ) {
            Object val= jo.get(names[i]);
            if ( val instanceof JSONObject ) {
                Map child= new HashMap();
                calcUserProperties( (JSONObject)jo, child );
            } else if ( val instanceof JSONArray ) {
                result.put( names[i], ((JSONArray)val) ); //TODO: convert this
            } else {
                result.put( names[i], val );
            }
        }
    }

    /**
     * calculate the bundle descriptor, possibly folding together columns to create
     * high rank datasets.
     * @param bd
     * @param columns
     * @param labels human-consumable labels for each column
     */
    private static BundleDescriptor calcBundleDescriptor( JSONObject jo, String[] columns, String[] columnLabels ) {

        String[] snames= new String[ columns.length ];

        BundleDescriptor bd= new BundleDescriptor();

        Map<Integer,String> dsNames= new LinkedHashMap();  // enumeration of all the names that are not in line.
        Map<String,Integer> dsToPosition= new LinkedHashMap(); // name to the index of first column

        int ids= 0; // index of the dataset in the bundleDescriptor.

        Map<JSONObject, String> messages= new LinkedHashMap();

        String[] names= JSONObject.getNames(jo);
        for ( int ivar=0; ivar<names.length; ivar++ ) {
            String jsonName= names[ivar];
            String name= Ops.safeName(jsonName);
            logger.log( Level.FINE, "processing name[{0}]={1}", new Object[]{ivar, jsonName});
            try {
                JSONObject jo1= jo.getJSONObject(jsonName);
                if ( jsonName.equals( QDataSet.USER_PROPERTIES ) ) {
                    Map<String,Object> val= new HashMap();
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
                    } else if ( dims instanceof Integer ) {
                        idims= new int[ 1 ];
                        idims[0]= (Integer)dims;
                    } else {
                        throw new IllegalArgumentException( "Expected array for DIMENSION in "+ jsonName );
                    }
                }
                if ( idims.length>1 ) {
                    throw new IllegalArgumentException("only rank 2 datasets supported, DIMENSION len="+ idims.length );
                }
                int total= idims.length==0 ? 1 : idims[0];
                for ( int j=1;j<idims.length; j++) {
                    total*= idims[j];
                }

                String[] labels=null;
                if ( jo1.has( PROP_ELEMENT_LABELS ) ) {
                    Object olabels= jo1.get( PROP_ELEMENT_LABELS );
                    if ( olabels instanceof JSONArray ) {
                        labels= toStringArray((JSONArray)olabels);
                    } else {
                        logger.log(Level.FINE, "unable to use ELEMENT_LABELS in {0}, should be array", jsonName);
                    }
                }
                String[] elementNames= null;
                if ( jo1.has( PROP_ELEMENT_NAMES ) ) {
                    Object oelements= jo1.get( PROP_ELEMENT_NAMES );
                    if ( oelements instanceof JSONArray ) {
                        elementNames= toStringArray((JSONArray)oelements);
                    } else {
                        logger.log(Level.FINE, "unable to use ELEMENT_NAMES in {0}, should be array", jsonName);
                    }
                }

                if ( elementNames!=null ) { //TODO: eliminate repeated code.
                        String lookFor= elementNames[0]; //Note ELEMENT_NAMES must correspond to adjacent columns.
                        int icol= -1;
                        int count= 0;
                        List<Integer> icols= new ArrayList();
                        for ( int j=0; j<columns.length; j++ ) {
                            if ( columns[j].equals(lookFor) ) {
                                logger.log( Level.FINE, "found column named {0} at {1}", new Object[]{lookFor, j} );
                                if ( count==0 ) icol= j;
                                count++;
                                icols.add(j);
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
                                if ( jo1.has("START_COLUMN") ) {
                                    logger.log( Level.FINE, "ignoring START_COLUMN property for {0}", new Object[]{lookFor } );
                                }
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
                            dsNames.put( ids, name );
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
                        if ( total!=elementNames.length ) throw new IllegalArgumentException("expected "+total+" items in ELEMENTS" );

                } else {
                    String lookFor= name;
                    int icol= -1;
                    for ( int j=0; j<columns.length; j++ ) {
                        if ( columns[j].equals(lookFor) ) {
                            logger.log( Level.FINE, "found column named {0} at {1}", new Object[]{lookFor, j} );
                            icol= j;
                            bd.addDataSet( name, ids, idims, null, labels ); // findbugs NP_LOAD_OF_KNOWN_NULL_VALUE
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
                       dsNames.put( ids, name );
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
                }

            } catch ( JSONException ex ) {
                logger.log( Level.WARNING, "When handling {0}: {1}", new Object[]{jsonName, ex.toString()});
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
        Units u= Units.dimensionless;
        for ( int i=0; i<values.length(); i++ ) {
            try {
                dd[i] = values.getDouble(i);
            } catch (JSONException ex) {
                String s= values.getString(i);
                try {
                    dd[i]= Units.us2000.parse(s).doubleValue(Units.us2000);
                    u= Units.us2000;
                } catch ( ParseException ex2 ) {
                    // "-7.846400, -24.376616, 30485.856600" "UNITS":"Deg., Deg., km","DIMENSION":[2],"NAME":"Apogee Pos Geodetic",
                    throw ex;
                }
            }
        }
        DDataSet result= DDataSet.wrap( dd, dims );
        if ( u!=Units.dimensionless ) result.putProperty( QDataSet.UNITS, u );
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
    public static BundleDescriptor parseMetadata( String header, String[] columns, String[] columnLabels ) throws ParseException {
        try {
            JSONObject jo;
            AsciiHeadersParser ahp= new AsciiHeadersParser();
            String sjson= ahp.prep(header);
            jo = new JSONObject( sjson );
            BundleDescriptor bd= calcBundleDescriptor( jo, columns, columnLabels );

            fillMetadata( bd,jo );
            return bd;

        } catch (JSONException ex) {
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
                            return labels[ic-ids];
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
                props1= new LinkedHashMap<String,Object>();
                props.put( i, props1 );
            }
            if ( name.startsWith( "DEPEND_" ) && !(name.equals("DEPEND_0") ) && v instanceof String ) {
                if ( inlineDataSets.containsKey((String)v) ) {
                    props1.put( name, inlineDataSets.get((String)v) );
                } else {
                    logger.log(Level.WARNING, "unable to resolve property {0}={1} of {2}.  No such dataset found.", new Object[]{name, v, datasets2.get(i)});
                    throw new IllegalArgumentException("unable to resolve property "+name+"="+v+" of "+datasets2.get(i)+".  No such dataset found." );
                    //props1.put( name, v );
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
            for ( String s: inlineDataSets.keySet() ) {
                newb.addDataSet( s, inlineDataSets.get(s) );
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
            logger.log(Level.WARNING, "unable to parse value for {0}: {1}", new Object[]{propName, propValue});
            return null;
            
        } catch ( NumberFormatException ex ) {
            logger.log(Level.WARNING, "unable to parse value for {0}: {1}", new Object[]{propName, propValue});
            return null;
        }
    }

    /**
     * return a map of metadata for each column or bundled dataset.
     * @param jo
     * @return
     */
    private static void fillMetadata( BundleDescriptor bd, JSONObject jo ) throws JSONException {

        if ( false ) {  //isTranspose(jo) ) {  //transpose will never be implemented.
            throw new IllegalArgumentException("not implemented");
        } else {
            Iterator it= jo.keys();
            for ( ; it.hasNext(); ) {
                 String key= (String) it.next();
                 Object o= jo.get(key);
                 if ( !( o instanceof JSONObject ) ) {
                     logger.log(Level.WARNING, "expected JSONObject for value: {0}", key);
                     continue;
                 } else {
                     String name= Ops.safeName(key);
                     int ids= bd.indexOf( name );
                     if ( ids==-1 ) {
                         JSONObject inlineObject= (JSONObject)o;
                         if ( !inlineObject.has("VALUES") ) {
                             logger.log(Level.WARNING, "metadata found for key {0}, but this is not found in the ascii file parser", key);
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
                            continue;
                         } else if ( prop.equals("UNITS") && ( sv.equals("UTC") || sv.equals("UT") ) ) {
                            bd.putProperty( prop, ids, Units.us2000 );
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
                         } else {
                            if ( sv instanceof JSONArray ) {
                                JSONArray asv= (JSONArray)sv;
                                Object item= asv.get(0);
                                boolean homogenious= true;
                                for ( int i=1; i<asv.length(); i++ ) {
                                    if ( !item.equals(asv.get(i)) ) homogenious= false;
                                }
                                if ( homogenious ) {
                                    Object v= coerceToType( prop, item );
                                    bd.putProperty( prop, ids, v );
                                } else {
                                    logger.log(Level.WARNING, "invalid value for property {0}: {1}", new Object[]{prop, sv});
                                }
                             } else if ( sv instanceof JSONObject ) {
                                logger.log(Level.WARNING, "invalid value for property {0}: {1}", new Object[]{prop, sv});
                            } else {
                                Object v= coerceToType( prop, sv );
                                bd.putProperty( prop, ids, v );
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
     * @param column the name of the inline dataset
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

    public static String[] getInlineDataSetNames( QDataSet bds ) {
        if ( bds instanceof BundleDescriptor ) {
            Map<String,QDataSet> inlineDataSets= ((BundleDescriptor)bds).inlineDataSets;
            return inlineDataSets.keySet().toArray( new String[inlineDataSets.size()] );
        } else {
            throw new IllegalArgumentException("bds is not a BundleDescriptor created by this class");
        }
    }

}
