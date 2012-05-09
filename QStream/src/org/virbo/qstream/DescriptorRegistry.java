/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.qstream;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author jbf
 */
public class DescriptorRegistry {   
    static Map<String,DescriptorFactory> entries= new HashMap<String,DescriptorFactory>();
        
    static {
        register( "packet", new PacketDescriptorFactory() );
        register( "exception", new ExceptionDescriptorFactory() );
        register( "comment", new StreamCommentDescriptorFactory() );
    }
    
    public synchronized static void register( String s, DescriptorFactory factory ) {
        entries.put(s, factory );
    }
    
    public static DescriptorFactory get( String s ) {
        return entries.get(s);
    }
}
