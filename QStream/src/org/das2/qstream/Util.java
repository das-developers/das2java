/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.qstream;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Utility classes for formatting and parsing streams.
 * @author jbf
 */
public class Util {
    public static String encodeArray(int[] qube, int off, int len) {
        StringBuffer buf = new StringBuffer();
        if ( len==0 ) return "";
        buf.append(qube[off]);
        for (int i = 1; i < len; i++) {
            buf.append("," + qube[off + i]);
        }
        return buf.toString();
    }
    
    public static int[] decodeArray( String s ) {
        if (s.trim().length()==0 ) return new int[0];
        String[] ss= s.split(",",-2);
        int[] result= new int[ ss.length ];
        for ( int i=0; i<result.length; i++ ) {
            result[i]= Integer.parseInt(ss[i]);
        }
        return result;
    }

    public static int[] subArray(int[] qube, int off, int len ) {
        int[] result= new int[len];
        for ( int i=0; i<len; i++ ) {
            result[i]= qube[i+off];
        }
        return result;
    }

    /**
     * returns the single child element of a node, or throws IllegalArgumentException
     * @param n
     * @return
     */
    public static Element singletonChildElement( Element n ) {
        NodeList nl= n.getChildNodes();
        Element child= null;
        for ( int i=0; i<nl.getLength(); i++ ) {
            if ( nl.item(i).getNodeType()==Element.ELEMENT_NODE ) {
                if ( child!=null ) throw new IllegalArgumentException("multiple child nodes found when just one was expected");
                child= (Element) nl.item(i);
            }
        }
        if ( child==null ) throw new IllegalArgumentException("no child nodes found when just one was expected");
        return child;
    }
}
