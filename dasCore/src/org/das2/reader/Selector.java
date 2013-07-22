/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.reader;

import java.text.ParseException;
import org.das2.datum.CalendarTime;
import org.das2.datum.TimeUtil;

/** A selector encapsulates a request parameter, or parameter set, for a reader.
 * The most commonly used selector is a time range.  A time range selector would
 * most likely have the m_type = RANGE and m_format = TIMEPOINT.
 *
 * Implementation Note:  We may want to break this into a selector base class
 * with one derived class for each value of m_type.
 *
 * @author cwp
 */
public class Selector {
	/** Defines the basic category of selector */
	public static enum Type {VALUE, ENUM, RANGE };

	/** Defines the value format for the selector */
	public static enum Format {BOOLEAN, INTEGER, REAL, STRING, TIMEPOINT};

	// Instance Data
	private String m_sKey = null;
	private Format m_format = null;
	private Type m_type = null;

	private String [] m_sValAry = null;

	// Store the converted strings so that don't have to be re-converted for each
	// request this wastes space, but gains efficiency (I think).
	private int [] m_nValAry = null;
	private double [] m_dValAry = null;
	private boolean [] m_bValAry = null;
	private CalendarTime[] m_tValAry = null;

	// Argument parsing helper
	private void storeArg(Format fmt, int iSlot, String sArg) throws ParseException{
		switch(fmt){
		case BOOLEAN:
			if(sArg.equalsIgnoreCase("false")||sArg.equalsIgnoreCase("0"))
				m_bValAry[iSlot] = false;
			else
				m_bValAry[iSlot] = true;
			break;
		case INTEGER:
			m_nValAry[iSlot] = Integer.parseInt(sArg);
			break;
		case REAL:
			m_dValAry[iSlot] = Double.parseDouble(sArg);
			break;
		case TIMEPOINT:
			m_tValAry[iSlot] = new CalendarTime(sArg);
			break;
		}
	}

	/** Construct a range selector */
	public Selector(String sKey, Format fmt, String sBeg, String sEnd)
		throws ParseException
	{
		m_type = Type.RANGE;
		m_format = fmt;

		if(sKey == null)
			throw new IllegalArgumentException("Null Selector key value");
		m_sKey = sKey;

		if((sBeg == null)||(sEnd == null))
			throw new IllegalArgumentException("Null range point string.");

		m_sValAry = new String [] {sBeg, sEnd};
		switch(fmt){
		case BOOLEAN:  m_bValAry = new boolean [] {false, false}; break;
		case INTEGER:  m_nValAry = new int [] {0,0}; break;
		case REAL:     m_dValAry = new double [] {0,0}; break;
		case TIMEPOINT: m_tValAry = new CalendarTime [] {null, null}; break;
		}

		storeArg(fmt, 0, sBeg);
		storeArg(fmt, 1, sEnd);
	}

	/** Construct a value selector.
	 * Note: to create a selector that can be used to answer true/false questions,
	 * set the fmt parameter to BOOLEAN.
	 */
	public Selector(String sKey, Format fmt, String sValue)
		throws ParseException
	{
		m_type = Type.VALUE;
		m_format = fmt;

		if(sKey == null)
			throw new IllegalArgumentException("Null Selector key value");
		m_sKey = sKey;

		if(sValue == null)
			throw new IllegalArgumentException("Null value string.");
		m_sValAry = new String [] {sValue};
		switch(fmt){
		case BOOLEAN:  m_bValAry = new boolean [] {false}; break;
		case INTEGER:  m_nValAry = new int [] {0}; break;
		case REAL:     m_dValAry = new double [] {0}; break;
		case TIMEPOINT: m_tValAry = new CalendarTime [] {null}; break;
		}
		storeArg(fmt, 0, sValue);
	}

	/** Construct an enumeration selector */
	public Selector(String sKey, String sItem){
		m_type = Type.ENUM;
		m_format = Format.STRING;

		if(sKey == null)
			throw new IllegalArgumentException("Null Selector key value");
		m_sKey = sKey;

		if(sItem == null)
			throw new IllegalArgumentException("Null enumeration item string.");

		m_sValAry = new String [] {sItem};

		// No need to fill out any of the parsed arrays because enumeration items are
		// always strings.
	}

	/** Get the key (which is just a string) for this selector.
	 * Note That no two selectors for the same data-source may have the same key.
	 */
	public String key(){ return m_sKey;	}

	/** Find out what kind of selector you're dealing with. */
	public Type type(){ return m_type; }

	/** Get the data type for the selector */
	public Format format(){ return m_format;}


	/** Get the selection as string(s).
	 * Since all constructor values are strings, no matter the value type, this always
	 * works.
	 * @return An array containing two Strings if type() == RANGE, otherwise an array
	 *         containing a single string is returned.
	 */
	public String [] getStr(){
		if(m_type != Type.RANGE)
			return new String [] {m_sValAry[0]};
		else
			return new String [] {m_sValAry[0], m_sValAry[1]};
	}

	/** Get the selection as a single string, no matter the type of selector */
	@Override
	public String toString(){
		if(m_type != Type.RANGE)
			return String.format("%s %s", m_sKey, m_sValAry[0]);
		else
			return String.format("%s %s to %s %s",m_sKey,m_sValAry[0],m_sKey,m_sValAry[1]);
	}
	
	/** Get the selection as true/false value(s). */
	public boolean [] getBool(){
		if(m_format != Format.BOOLEAN)
			throw new UnsupportedOperationException("Selector is not a boolean.");

		if(m_type != Type.RANGE)
			return new boolean [] {m_bValAry[0]};
		else
			return new boolean [] {m_bValAry[0], m_bValAry[1]};
	}

	/** Get the selection as integer(s).
	 *
	 * @return An array containing two integers if type() == RANGE, otherwise an array
	 *         containing a single integer is returned.
	 */
	public int [] getInt(){
		if(m_format != Format.INTEGER)
			throw new UnsupportedOperationException("Selector format is not INTEGER.");

		if(m_type != Type.RANGE)
			return new int [] {m_nValAry[0]};
		else
			return new int [] {m_nValAry[0], m_nValAry[1]};
	}

	/** Get the selection as double(s).
	 * @return An array containing two doubles if type() == RANGE, otherwise an array
	 *         containing a single double is returned.
	 */
	public double [] getDouble(){
		if(m_format != Format.REAL)
			throw new UnsupportedOperationException("Selector format is not REAL");

		if(m_type != Type.RANGE)
			return new double [] {m_dValAry[0]};
		else
			return new double [] {m_dValAry[0], m_dValAry[1]};
	}

	/** Get the selection as time structure(s)
	 */
	public CalendarTime [] getTimeStruct(){
		if(m_format != Format.TIMEPOINT)
			throw new UnsupportedOperationException("Selector format is not DATETIME");

		if(m_type != Type.RANGE)
			return new CalendarTime [] {m_tValAry[0]};
		else
			return new CalendarTime [] {m_tValAry[0], m_tValAry[1]};
	}
	
}
