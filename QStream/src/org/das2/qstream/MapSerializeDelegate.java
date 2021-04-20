/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.qstream;

import java.beans.XMLDecoder;
import java.io.ByteArrayInputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.datum.Units;
import org.das2.util.Base64;
import org.das2.qds.DataSetUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Store map property to QStream.
 * @author jbf
 */
public class MapSerializeDelegate implements SerializeDelegate, XMLSerializeDelegate {

    private static final Logger logger= Logger.getLogger("qstream");

    @Override
    public Element xmlFormat( Document doc, Object o ) {
        Map m= (Map)o;
        Element result= doc.createElement( typeId( o.getClass() ) );
        for ( Object o2: m.entrySet() ) {
            Entry e= (Entry)o2;
            Object oval= e.getValue();
            SerializeDelegate sd= SerializeRegistry.getDelegate(oval.getClass());
            if ( sd==null ) {
                logger.log(Level.FINE, "sorry, can''t serialize {0}", e);
                continue;
            }
            Element child= doc.createElement("entry");
            child.setAttribute( "key", String.valueOf(e.getKey()) );
            if ( sd instanceof XMLSerializeDelegate ) {
                child.appendChild( ((XMLSerializeDelegate)sd).xmlFormat(doc,oval) );
            } else {
                String sval= sd.format(oval);
                child.setAttribute( "type", sd.typeId(oval.getClass()) );
                child.setAttribute( "value", sval );
            }
            result.appendChild(child);
        }
        return result;
    }

    @Override
    public Object xmlParse( Element e ) throws ParseException {
        LinkedHashMap result= new LinkedHashMap();
        NodeList nl= e.getChildNodes();
        for ( int i=0; i<nl.getLength(); i++ ) {
            if ( nl.item(i).getNodeType()!=Element.ELEMENT_NODE ) continue;
            Element child= (Element) nl.item(i);
            String key= child.getAttribute("key");
            String stype;
            Element eval= null;
            if ( child.hasAttribute("type") ) {
                stype= child.getAttribute("type");
            } else {
                eval= Util.singletonChildElement(child);
                stype= eval.getTagName();
            }
            try {
                SerializeDelegate sd= SerializeRegistry.getByName(stype);
                if ( sd==null ) throw new ParseException("unrecognized type: "+stype,0);
                if ( sd instanceof XMLSerializeDelegate ) {
                    Object oval= ((XMLSerializeDelegate)sd).xmlParse(eval);
                    result.put( key, oval );
                } else {
                    String sval= child.getAttribute("value");
                    Object oval= sd.parse(stype, sval);
                    result.put( key, oval );
                }
            } catch ( ParseException ex ) {
                
            }
        }
        return result;
    }


    @Override
    public String format(Object o) {
        Map m= (Map)o;
        StringBuilder buf= new StringBuilder();
        buf.append("map[");
        for ( Object o2: m.entrySet() ) {
            Entry e= (Entry)o2;
            Object oval= e.getValue();
            SerializeDelegate sd= SerializeRegistry.getDelegate(oval.getClass());
            if ( sd==null ) {
                logger.log(Level.WARNING, "sorry, can''t serialize {0}", e);
                continue;
            }
            buf.append( (String)e.getKey() );
            buf.append( "=" );
            String sval= sd.format(oval);
            buf.append(sd.typeId(oval.getClass())).append(":").append( URLEncoder.encode(sval));
            buf.append(" ");
        }
        buf.append("]");
        return buf.toString();
    }

    @Override
    public Object parse(String typeId, String s) throws ParseException {
        if (s.equals("")) {
            return Collections.EMPTY_MAP;
        }
        if ( s.startsWith("map[") ) {
            String[] ss= s.substring(4,s.length()-1).split(" ");
            if ( ss.length==1 && ss[0].equals("") ) return Collections.EMPTY_MAP;
            LinkedHashMap result= new LinkedHashMap();
            for ( String s1 : ss ) {
                try {
                    String[] nv= s1.split("=");
                    int i= nv[1].indexOf(":");
                    String stype= nv[1].substring(0,i);
                    SerializeDelegate sd= SerializeRegistry.getByName(stype);
                    String sval= URLDecoder.decode(nv[1].substring(i+1));
                    Object oval= sd.parse(stype, sval);
                    result.put( nv[0], oval );
                } catch ( ArrayIndexOutOfBoundsException ex ) {
                    throw ex;
                } catch ( ParseException ex ) {
                    throw ex;
                }
            }
            return result;
        } else {
            byte[] buff = Base64.getDecoder().decode(s);
            XMLDecoder dec = new XMLDecoder(new ByteArrayInputStream(buff));
            Object result = dec.readObject();
            return result;
        }
    }

    @Override
    public String typeId(Class clas) {
        return "map";
    }

    public static void main( String[] args ) throws ParseException {
        HashMap m1= new HashMap();
        HashMap m2= new HashMap();
        m2.put("dog",56);
        m2.put("cat","tiger summary");
        m2.put("ds",DataSetUtil.asDataSet(23,Units.us2000));
        m1.put( "units", Units.centigrade );
        m1.put("user_properties",m2);
        m1.put("cat","tiger summary");
        MapSerializeDelegate sd= new MapSerializeDelegate();

        String sval= sd.format(m1);

        System.err.println(sval);
        Map m3= (Map) sd.parse("map",sval);
        //sval= "map[total=Long:8928 invalidCount=Long:0 outliers=map:map%5B%5D outlierCount=Integer:0 binWidth=Double:500.0 binStart=Double:0.0 ]";
        //Map m4= (Map) sd.parse("map",sval);
        System.err.println(m3);
    }
}
