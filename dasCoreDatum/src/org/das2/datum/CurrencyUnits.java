/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.datum;

import java.text.ParseException;

/**
 * parse a few currencies, for demonstration purposes.
 * @author jbf
 */
public class CurrencyUnits extends NumberUnits {

    String ch;
    
    public CurrencyUnits( String id, String ch, String desc ) {
        super( id, desc );
        this.ch= ch;
    }
    
    /**
     * remove the symbol and parse as a double.
     * @param s
     * @return
     * @throws ParseException 
     */
    @Override
    public Datum parse(String s) throws ParseException {
        return super.parse(s.replace( ch,"" ).replaceAll(",","")); // sorry, rest of world...
    }
    
}
