/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.qstream;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 *
 * @author jbf
 */
public class StringSerializeDelegate implements SerializeDelegate {

    public String format(Object o) {
        try {
            return URLEncoder.encode((String) o, "US-ASCII");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public Object parse(String typeId,String s) {
        try {
            return URLDecoder.decode(s, "US-ASCII");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public String typeId(Class clas) {
        return "String";
    }

}
