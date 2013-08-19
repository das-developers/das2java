/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.reader;

import java.io.IOException;
import java.net.URL;
import java.util.EnumMap;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.xml.sax.SAXException;

/** A validator for all Das2 client-server and server-reader XML messages
 *
 * @author cwp
 */
public class Das2MsgValidator {
	public static enum MsgType {ROOT, PEERS, LIST, DESCRIBE, LIST_LEVEL};
	
	private Map<MsgType, Validator> m_dValidators;
	
	public Das2MsgValidator(){
		m_dValidators = new EnumMap<MsgType, Validator>(MsgType.class);
		m_dValidators.put(MsgType.ROOT, null);
		m_dValidators.put(MsgType.PEERS, null);
		m_dValidators.put(MsgType.LIST, null);
		m_dValidators.put(MsgType.DESCRIBE, null);
		m_dValidators.put(MsgType.LIST_LEVEL, null);
	}
	
	public void validate(MsgType type, Source src) throws SAXException, IOException{
		synchronized(this){
			if(m_dValidators.get(type) == null){
				SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
				URL url = null;
				
				switch(type){
				case ROOT:
					throw new UnsupportedOperationException("Schema for message type "+
								  type.toString()+" is not yet implemented");
				case PEERS:
					throw new UnsupportedOperationException("Schema for message type "+
							  type.toString()+" is not yet implemented");
				case LIST:
					throw new UnsupportedOperationException("Schema for message type "+
							  type.toString()+" is not yet implemented");
				case DESCRIBE:
					url = Das2MsgValidator.class.getResource("/schema/das3_dsid-2.2.xsd");
					break;
				case LIST_LEVEL:
					throw new UnsupportedOperationException("Schema for message type "+
							  type.toString()+" is not yet implemented");
				default:
					throw new IllegalArgumentException("Operation "+type.toString()+
							  " is unknown.");
				}
				Schema schema = null;
				try{
					schema = sf.newSchema(url);
				}
				catch(SAXException ex){
					throw new RuntimeException(ex);
				}
				m_dValidators.put(type, schema.newValidator());
			}
		}
		
		m_dValidators.get(type).validate(src);
	}
}
