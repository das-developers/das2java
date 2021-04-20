/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.qstream;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for XML elements that appear on QStreams.  The tag name is used
 * to register the element.
 * @author jbf
 */
public class DescriptorRegistry {   
    static Map<String,DescriptorFactory> entries= new HashMap<String,DescriptorFactory>();
        
    static {
        register( "packet", new PacketDescriptorFactory() );
        register( "exception", new ExceptionDescriptorFactory() );
        register( "comment", new StreamCommentDescriptorFactory() );
        register( "enumerationUnit", new EnumerationUnitDescriptorFactory() );
    }
    
    /**
     * register the factory used for the tag name.
     * @param s the tag name
     * @param factory the factory
     */
    public synchronized static void register( String s, DescriptorFactory factory ) {
        entries.put(s, factory );
    }
    
    /**
     * look up the factory used for the tag name
     * @param s the tag name
     * @return the factory
     */
    public static DescriptorFactory get( String s ) {
        return entries.get(s);
    }
}
