
package org.das2.qds.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Stack;
import java.util.StringTokenizer;

/**
 * Tool for parsing ODL found at the top of .sts files. 
 * @author jbf
 */
public class OdlParser {
    /**
     * read the ODL off the top of the file, returning the ODL
     * in a string and leaving the InputStream pointed at the 
     * line following the ODL.
     * @param r reader for the file which starts with ODL.  The reader will be left pointing at the first non-ODL line.
     * @return the ODL header.
     * @throws java.io.IOException 
     */
    public static String readOdl( BufferedReader r ) throws IOException {

        boolean notdone= true;
                
        Stack<String> objectStack= new Stack<>();
        
        StringBuilder sb= new StringBuilder();
        
        String line;
        
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
                } else if ( s.equals( "END_OBJECT" ) ) {
                    sb.append( s ).append(" ");
                    sb.append("/* ").append(objectStack.pop()).append(" */");
                } else {
                    sb.append( s ).append( " " );
                }
            }
            if ( objectStack.isEmpty() ) {
                notdone= false;
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }
    
}
