/* File: StreamTool.java
 * Copyright (C) 2002-2003 The University of Iowa
 * Created by: Jeremy Faden <jbf@space.physics.uiowa.edu>
 *             Jessica Swanner <jessica@space.physics.uiowa.edu>
 *             Edward E. West <eew@space.physics.uiowa.edu>
 *
 * This file is part of the das2 library.
 *
 * das2 is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.uiowa.physics.pw.das.util;

/**
 *
 * @author  jbf
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.StringBufferInputStream;
import java.util.ArrayList;

public class StreamTool {
    
    /** Creates a new instance of StreamTool */
    public StreamTool() {
    }
    
    public static class DelimeterNotFoundException extends Exception {
    }
    
    public static byte[] advanceTo( InputStream in, byte[] delim ) throws IOException, DelimeterNotFoundException {
        
        // Read from stream to delimeter, leaving the InputStream immediately after
        // and returning bytes from stream.
        
        byte[] data = new byte[4096];
        
        ArrayList list= new ArrayList();
        
        int bytesMatched=0;
        int matchIndex=0;
        
        int streamIndex=0;  // offset in bytes from the beginning of the stream
        
        int index=0;
        boolean notDone=true;
        
        int unreadOffset=-99999;
        int unreadLength=-99999;               
        
        int totalBytesRead=0;
        int offset=0;  // offset within byte[4096]
        
        while ( notDone ) {
            
            int byteRead= in.read();
            totalBytesRead++;
            
            if ( byteRead==-1 ) {
                notDone=false;
                
            } else {
            
                data[offset]= (byte)byteRead;
            
                if ( delim[bytesMatched]==byteRead ) {
                    bytesMatched++;
                } else {
                    bytesMatched=0;
                }
                if ( bytesMatched==delim.length ) {
                    notDone= false;
                    index= totalBytesRead - delim.length;
                }
            }
            
            if ( notDone ) {
                offset++;
                if ( offset==4096 ) {
                    list.add(data);
                    offset=0;
                    data= new byte[4096];
                }
            }
        }
        
        
        if ( bytesMatched!=delim.length ) {
            throw new StreamTool.DelimeterNotFoundException();
        }
        
        byte[] result= new byte[index];
        for ( int i=0; i<list.size(); i++ ) {
            System.arraycopy( list.get(i), 0, result, i*4096, 4096);
        }
        System.arraycopy( data, 0, result, list.size()*4096, index-(list.size()*4096) );
        return result;
        
    }
    
    public static void main( String[] args ) {
        PushbackInputStream in= new PushbackInputStream( new StringBufferInputStream("hello there silly man") );
        
        try {
            byte[] firstPart= advanceTo(in,"er".getBytes());
            System.out.println( "before part: "+ new String(firstPart));
            
            int buf;
            byte[] arrayBuf= new byte[1];
            
            System.out.print("after part: ");
            while ( (buf=in.read())!=-1 ) {
                arrayBuf[0]= (byte)buf;
                System.out.print( new String(arrayBuf) );
            }
            
            System.out.println();
            
        } catch ( IOException ex ) {
            System.out.println(ex);
        } catch ( StreamTool.DelimeterNotFoundException ex ) {
            System.out.println(ex);
        }
        
        
        
    }
    
}
