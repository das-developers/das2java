/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.qstream;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 *
 * @author jbf
 */
public class StringSerializeDelegate implements SerializeDelegate {

    public String format(Object o) {
        return (String) o;
    }

    public Object parse(String typeId,String s) {
        return s;
    }

    public String typeId(Class clas) {
        return "String";
    }

}
