
package org.das2.qds.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.datum.LoggerManager;
import org.das2.qds.QDataSet;
import org.das2.qds.ops.Ops;
import org.das2.util.monitor.ProgressMonitor;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Tool for parsing ODL found at the top of .sts files. 
 * @author jbf
 */
public class OdlParser {
    
    private static final Logger logger= LoggerManager.getLogger("qdataset.ascii.odl");
    
    /**
     * read the ODL off the top of the file, returning the ODL
     * in a string and leaving the InputStream pointed at the 
     * line following the ODL.
     * @param r reader for the file which starts with ODL.  The reader will be left pointing at the first non-ODL line.
     * @param record
     * @return the ODL header.
     * @throws java.io.IOException 
     * @throws JSONException never
     */
    public static String readOdl( BufferedReader r, JSONObject record ) throws IOException, JSONException {

        boolean notdone= true;
                
        Stack<String> objectStack= new Stack<>();
        Stack<JSONObject> jsonStack= new Stack<>();
        
        StringBuilder sb= new StringBuilder();
        
        String line;
        
        
        HashSet<String> keepers= new HashSet<>();
        keepers.add("NAME");
        keepers.add("FORMAT");
        keepers.add("ALIAS");
        keepers.add("UNITS");
        keepers.add("TYPE");
        
        try {
            while ( notdone && ( line= r.readLine() )!=null ) {

                StringTokenizer st= new StringTokenizer(line);
                while ( st.hasMoreTokens() ) {
                    String s= st.nextToken();
                    if ( s.equals("END_OBJECT") ) {
                        sb.append( "            ".substring(0,2*(objectStack.size()-1)) );
                    } else {
                        sb.append( "            ".substring(0,2*objectStack.size()) );
                    }
                    if ( s.equals("OBJECT") ) {
                        sb.append(s).append(" ");
                        s= st.nextToken(); // =
                        sb.append(s).append(" ");
                        s= st.nextToken(); // the name
                        objectStack.push(s);
                        sb.append(s).append(" ");
                        if ( s.equals("RECORD") ) {
                            if ( record==null ) {
                                record= new JSONObject();
                            }
                            record.put( "array", new JSONArray() );
                            jsonStack.add(record);
                        } else if ( !jsonStack.empty() ) {
                            JSONArray nextArray= new JSONArray();
                            JSONObject jo= new JSONObject();
                            jo.put( "array", nextArray );
                            jsonStack.add( jo );
                        }

                    } else if ( s.equals( "END_OBJECT" ) ) {
                        sb.append( s ).append(" ");
                        sb.append("/* ").append(objectStack.pop()).append(" */");
                        if ( !jsonStack.empty() ) {
                            JSONObject finishedObject= jsonStack.pop();
                            if ( !jsonStack.empty() ) {
                                JSONObject thisObject= jsonStack.peek();
                                thisObject.getJSONArray("array").put(finishedObject);
                            }
                        }

                    } else {
                        if ( keepers.contains(s) && !jsonStack.empty() ) {
                            String t= s;
                            sb.append(s).append(" ");
                            s= st.nextToken(); // =
                            sb.append(s).append(" ");
                            s= st.nextToken(); // the name
                            JSONObject currentObject= jsonStack.peek();
                            currentObject.put( t, s );
                        } else {
                            sb.append( s ).append( " " );
                        }
                    }
                }
                if ( objectStack.isEmpty() ) {
                    notdone= false;
                }
                sb.append("\n");
            }
        } catch ( JSONException ex ) {
            throw new RuntimeException(ex);
            
        }
        return sb.toString();
    }
    
    /**
     * read the stream based on the spec in record.
     * @param r the reader, pointed after the ODL header.
     * @param record
     * @param monitor
     * @return 
     * @throws java.io.IOException
     */
    public static QDataSet readStream( BufferedReader r, JSONObject record, ProgressMonitor monitor ) throws IOException {
        int fieldCount= getFieldCount( record );
        AsciiParser parser= AsciiParser.newParser( fieldCount );
        QDataSet result= parser.readStream( r, monitor );
        r.close();
        return result;
    }
    
    private static QDataSet formTimeTags( QDataSet ds, JSONObject record ) {
        try {
            JSONArray array= record.getJSONArray("array");
            if ( array.length()==0 ) {
                throw new IllegalArgumentException("array is empty");
            }
            JSONObject time= array.getJSONObject(0);
            if ( time.has("NAME") && time.getString("NAME").equals("TIME") ) {
                JSONArray components= time.getJSONArray("array");
                QDataSet[] cds= new QDataSet[7];
                for ( int icomp=0; icomp<components.length(); icomp++ ) {
                    JSONObject comp= components.getJSONObject(icomp);
                    String name= comp.optString("NAME");
                    switch (name) {
                        case "YEAR":
                            cds[0]= Ops.slice1( ds, icomp );
                            break;
                        case "MONTH":
                            cds[1]= Ops.slice1( ds, icomp );
                            break;
                        case "DOY":
                            cds[1]= Ops.dataset(1);
                            cds[2]= Ops.slice1( ds, icomp );
                            break;
                        case "DAY":
                            cds[2]= Ops.slice1( ds, icomp );
                            break;
                        case "HOUR":
                            cds[3]= Ops.slice1( ds, icomp );
                            break;
                        case "MIN":
                            cds[4]= Ops.slice1( ds, icomp );
                            break;
                        case "SEC":
                            cds[5]= Ops.slice1( ds, icomp );
                            break;
                        case "MSEC":
                            cds[6]= Ops.multiply( Ops.slice1( ds, icomp ), 1000000 );
                            break;
                        default:
                            throw new IllegalArgumentException("unexpected component: "+name);
                    }
                }
                QDataSet tds= Ops.toTimeDataSet( cds[0], cds[1], cds[2], cds[3], cds[4], cds[5], cds[6] );
                ds= Ops.trim1( ds, components.length(), ds.length(0) );
                ds= Ops.link( tds, ds );
                return ds;
            } else {
                throw new IllegalArgumentException("expected record to have time components, a vector with TIME,YEAR,etc");
            }
        } catch (JSONException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
    
    public static int getFieldCount( JSONObject record ) {
        try {
            if ( record.has("FORMAT") ) {
                return 1;
            } else {
                int count=0;
                JSONArray array= record.getJSONArray("array");
                for ( int i=0; i<array.length(); i++ ) {
                    JSONObject jo= array.getJSONObject(i);
                    count= count + getFieldCount( jo );
                }
                return count;
            }
        } catch ( JSONException ex ) {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * return the format, though it doesn't appear to work.
     * @param record
     * @return 
     */
    public static String getFormat( JSONObject record ) {
        try {
            if ( record.has("FORMAT") ) {
                return record.getString("FORMAT");
            } else {
                StringBuilder sb= new StringBuilder();
                JSONArray array= record.getJSONArray("array");
                for ( int i=0; i<array.length(); i++ ) {
                    if (i>0) sb.append(",");
                    JSONObject jo= array.getJSONObject(i);
                    sb.append( getFormat(jo) );
                }
                return sb.toString();
            }
        } catch ( JSONException ex ) {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * 
     * @param record
     * @param startColumn
     * @param name 
     * @return [start,end] for rank 2, [col,col] for rank 1, or [-1,-1] for not found.
     */
    public static int[] getColumns( JSONObject record, int startColumn, String name ) {
        String n1= record.optString( "NAME" );
        String na=  record.optString( "ALIAS" );
        JSONArray array= record.optJSONArray("array");
        if ( n1.equals(name) || na.equals(name) ) {
            if ( array==null || array.length()==0 ) {
                return new int[] { startColumn, startColumn };
            } else {
                return new int[] { startColumn, startColumn + array.length() };
            }
        }
        int icol=0;
        for ( int i=0; i<array.length(); i++ ) {
            String s= array.optJSONObject(i).optString("NAME");
            if ( !s.isEmpty() ) {
                if ( name.startsWith(s) ) {
                    if ( name.equals(s) ) {
                        int[] rr= getColumns( array.optJSONObject(i), startColumn+icol, name ); // +1 is for the period
                        if ( rr[0]>-1 ) {
                            return new int[] { rr[0],rr[1] };
                        }
                    } else {
                        int[] rr= getColumns( array.optJSONObject(i), startColumn+icol, name.substring(s.length()+1) ); // +1 is for the period
                        if ( rr[0]>-1 ) {
                            return new int[] { icol+rr[0],icol+rr[1] };
                        }
                    }
                }   
            }
            icol += getFieldCount(array.optJSONObject(i));
        }
        return new int[] { -1, -1 };
    }
    
    /**
     * return the data with the given name
     * @param record
     * @param ds
     * @param name
     * @return 
     */
    public static QDataSet getDataSet( JSONObject record, QDataSet ds, String name ) {
        int[] rr= getColumns( record, 0, name );
        
        QDataSet result= formTimeTags( ds, record );
        
        QDataSet yy;
        if ( rr[0]==-1 ) {
            throw new IllegalArgumentException("no such dataset: "+name);
        } else if (rr[0]==rr[1]) {
            yy= Ops.slice1( ds, rr[0] );
        } else {
            yy= Ops.trim1( ds, rr[0], rr[1] );
        }
        return Ops.link( result.property(QDataSet.DEPEND_0), yy );
    }
        
}
