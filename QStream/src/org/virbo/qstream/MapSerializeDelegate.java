/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.qstream;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.util.Base64;

/**
 *
 * @author jbf
 */
public class MapSerializeDelegate implements SerializeDelegate {

    public String format(Object o) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(1000);
            XMLEncoder enc = new XMLEncoder(out);
            enc.writeObject(o);
            enc.close();
            out.close();
            return Base64.encodeBytes(out.toByteArray());
        } catch (IOException ex) {
            Logger.getLogger(MapSerializeDelegate.class.getName()).log(Level.SEVERE, null, ex);
            return "";
        }
    }

    public Object parse(String typeId, String s) throws ParseException {
        if ( s.equals("") ) return Collections.EMPTY_MAP;
        byte[] buff= Base64.decode(s);
        XMLDecoder dec= new XMLDecoder( new ByteArrayInputStream(buff) );
        Object result= dec.readObject();
        return result;
    }

    public String typeId(Class clas) {
        return "map";
    }

}
