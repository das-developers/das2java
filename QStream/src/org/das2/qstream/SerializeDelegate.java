
package org.das2.qstream;

import java.text.ParseException;

/**
 * interface for serializing (formatting) and parsing objects.
 * @author jbf
 */
public interface SerializeDelegate {
    
    /**
     * format the object.
     * @param o the object
     * @return the string representation of the object.
     */
    public String format( Object o );
    
    /**
     * parse the object from the string
     * @param typeId the type ID
     * @param s the string, e.g. "royalBlue"
     * @return the object, e.g. Color.decode("#4169E1")
     * @throws ParseException 
     */
    public Object parse( String typeId, String s  ) throws ParseException;
    
    /**
     * identifier for the delegate, that is used to qualify the name of the thing formatted.
     * @param clas the class
     * @return
     */
    String typeId( Class clas );
}
