/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.reader;

import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** Selector template.  Just pour in values to make real selectors!
 *
 * @author cwp
 */
public class SelectorTplt {
	private String m_sKey;
	private String m_sName;
	private String m_sUnits;
	private String m_sSummary;
	private String m_sDesciption;
	private Selector.Type m_type;
	private Selector.Format m_format;
	private String m_sValueTpltStr;
	private boolean m_bIsConstant;
	private String m_sContValue;

	/** Helper to make digging out the value of sub-elements less wordy */
	private static String getSubElementValue(Element element, String sSubName){
		return element.getElementsByTagName(sSubName).item(0).getFirstChild().getNodeValue();
	}

	private static boolean hasSubElement(Element el, String sSubName){
		NodeList nl = el.getChildNodes();
		for(int i = 0; i < nl.getLength(); i++){
			Node node = nl.item(i);
			if(node.getNodeType() != Node.ELEMENT_NODE) continue;
			Element elSub = (Element)node;
			if(elSub.getTagName().equals(sSubName)) return true;
		}
		return false;
	}

	SelectorTplt(Element el) throws SAXException{

		m_sKey = el.getAttribute("key");
		m_sName = el.getAttribute("name");

		if(el.hasAttribute("units"))
			m_sUnits = el.getAttribute("units");
		else
			m_sUnits = "";

		if(el.hasAttribute("constant")){
			m_bIsConstant = true;
			m_sContValue = el.getAttribute("constant");
		}
		else{
			m_sContValue = null;
			m_bIsConstant = false;
		}

		//Get the sub-elements for summary and description and load those as well

		m_sSummary = getSubElementValue(el, "summary");

		if(hasSubElement(el, "description"))
			m_sDesciption = getSubElementValue(el, "description");
		else
			m_sDesciption = "";


		if(el.hasAttribute("format")){
			String sFmt = el.getAttribute("format");
			if(sFmt.equals("REAL")) m_format = Selector.Format.REAL;
			if(sFmt.equals("INTEGER")) m_format = Selector.Format.INTEGER;
			if(sFmt.equals("BOOLEAN")) m_format = Selector.Format.BOOLEAN;
			if(sFmt.equals("STRING")) m_format = Selector.Format.STRING;
			if(sFmt.equals("DATETIME")){
				m_format = Selector.Format.DATETIME;
				if(m_sUnits.equals(""))
					m_sUnits="UTC";
			}
		}


		if(el.getTagName().equals("boolean")){
			m_type = Selector.Type.VALUE;
			m_format = Selector.Format.BOOLEAN;
			m_sValueTpltStr = "true|false";
			return;
		}

		if(el.getTagName().equals("value")){
			m_type = Selector.Type.VALUE;
			switch(m_format){
			case BOOLEAN: m_sValueTpltStr = "true|false"; break;
			case INTEGER: m_sValueTpltStr = "INTEGER"; break;
			case REAL:    m_sValueTpltStr = "REAL"; break;
			case STRING:  m_sValueTpltStr = "STRING"; break;
			case DATETIME: m_sValueTpltStr = "TIME"; break;
			}
			return;
		}

		if(el.getTagName().equals("range")){
			m_type = Selector.Type.RANGE;
			switch(m_format){
			case BOOLEAN: m_sValueTpltStr = "true|false"; break;
			case INTEGER: m_sValueTpltStr = "INTEGER"; break;
			case REAL:    m_sValueTpltStr = "REAL"; break;
			case STRING:  m_sValueTpltStr = "STRING"; break;
			case DATETIME: m_sValueTpltStr = "TIME"; break;
			}
			return;
		}

		if(el.getTagName().equals("enum")){
			m_type = Selector.Type.ENUM;
			m_format = Selector.Format.STRING;

			StringBuilder sbValTplt = new StringBuilder();
			StringBuilder sbDesc = new StringBuilder();
			sbDesc.append(m_sDesciption);
			sbDesc.append("Flag definitions: ");


			// The format string for enumerations is all the possible values, or'ed together
			NodeList nl = el.getElementsByTagName("item");
			
			for(int i = 0; i < nl.getLength(); i++){
				Element elItem = (Element) nl.item(i);

				if(i > 0) sbValTplt.append("|");
				sbValTplt.append( elItem.getAttribute("value") );
				if(i > 0) sbDesc.append(", ");
				sbDesc.append(elItem.getAttribute("value"));
				sbDesc.append(" - ");
				sbDesc.append(elItem.getAttribute("summary"));
			}
			m_sValueTpltStr = sbValTplt.toString();
			m_sDesciption = sbDesc.toString();
			return;
		}

		throw new SAXException("Selector type " + el.getTagName() + " is unknown.");
	}

	public Selector.Type getType(){  return m_type; }
	public String getKey(){          return m_sKey; }
	public boolean hasDescription(){ return ! m_sDesciption.equals(""); }
	public String getSummary(){      return m_sSummary; }
	public String getDescription(){  return m_sDesciption; }
	public String getUnitStr(){      return m_sUnits; }
	public String getName(){         return m_sName; }
	boolean isConstant(){            return m_bIsConstant; }
	public String getValTpltStr(){     return m_sValueTpltStr; }

	/** Create a selector from a constant value */
	Selector mkSelector() throws IllegalStateException, ReaderDefException{
		if(!m_bIsConstant)
			throw new IllegalStateException("Selector '"+m_sName+"' is not constant, so "
				+ "value argument(s) must be supplied.");


		try{
			if(m_type == Selector.Type.VALUE)
				return new Selector(m_sKey, m_format, m_sContValue);
			
			return new Selector(m_sKey, m_sContValue);
		}
		catch(ParseException ex){
			throw new ReaderDefException("Constant value '"+m_sContValue+"' for selector '"+
				                          m_sKey+"' fails to parse.", ex);
		}
	}

	Selector mkSelector(String sValue) throws BadQueryException{
		if(m_type == Selector.Type.RANGE)
			throw new IllegalStateException("Use mkRangeSelector() to generate range"
					                             + " type data selectors");
		try{
			if(m_type == Selector.Type.VALUE)
				return new Selector(m_sKey, m_format, sValue);
			if(m_type == Selector.Type.ENUM){

				// Make sure the val is one of the enum choices
				String[] lLegalVals = m_sValueTpltStr.split("\\|");
				for(String sLegal: lLegalVals){
					if(sLegal.equals(sValue))
						return new Selector(m_sKey, sValue);
				}
				throw new BadQueryException("Value '"+sValue+"' is not a valid choice for the '"
					                         + m_sKey + "' enumeration.");
			}
		}
		catch(ParseException ex){
			throw new BadQueryException(ex.getMessage(), ex);
		}
		return null;
	}

	Selector mkRangeSelector(String sBeg, String sEnd) throws BadQueryException{
		if(m_type != Selector.Type.RANGE)
			throw new IllegalStateException("Use mkSelector() to generate single value data "
				                             + "selectors");
		try{
			return new Selector(m_sKey, m_format, sBeg, sEnd);
		}
		catch(ParseException ex){
			throw new BadQueryException(ex.getMessage());
		}
	}




	

	

	
		
}
