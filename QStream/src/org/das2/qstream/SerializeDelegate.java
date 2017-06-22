/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.qstream;

import java.text.ParseException;

/**
 *
 * @author jbf
 */
public interface SerializeDelegate {
    String format( Object o );
    
    public Object parse( String typeId,String s  ) throws ParseException;
    
    /**
     * identifier for the delegate, that is used to qualify the name of the thing formatted.
     * @return
     */
    String typeId( Class clas );
}
