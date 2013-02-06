/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.server.reader;

import org.das2.datum.TimeUtil.TimeStruct;

/** A selector encapsulates a request parameter, or parameter set, for a reader.
 * The most commonly used selector is a time range.  A time range selector would
 * most likely have the m_type = RANGE and m_format = DATETIME.
 *
 * Implementation Note:  We may want to break this into a selector base class
 * with one derived class for each value of m_type.
 *
 * @author cwp
 */
public class Selector {
	/** Defines the basic category of selector */
	public static enum Type {BOOLEAN, VALUE, ENUM, RANGE };

	/** Defines the value format for the selector */
	public static enum Format {BOOLEAN, INTEGER, REAL, STRING, DATETIME};

	/** Construct a range selector */
	public Selector(String sKey, Format fmt, String sBeg, String sEnd){

	}

	/** Construct a value selector */
	public Selector(String sKey, Format fmt, String sValue){

	}

	/** Construct an enumeration selector */
	public Selector(String sKey, String sValue){

	}

	/** Construct a boolean selector */
	public Selector(String sKey, boolean bValue){

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
	public TimeStruct [] getTimeStruct(){
		if(m_format != Format.DATETIME)
			throw new UnsupportedOperationException("Selector format is not DATETIME");

		if(m_type != Type.RANGE)
			return new TimeStruct [] {m_tValAry[0]};
		else
			return new TimeStruct [] {m_tValAry[0], m_tValAry[1]};
	}

	private String m_sKey = null;
	private Format m_format = null;
	private Type m_type = null;
	
	private String [] m_sValAry = null;
	
	/* Store the converted strings so that don't have to be re-converted for each
	 * request this wastes space, but gains efficiency (I think).
	 */
	private int [] m_nValAry = null;
	private double [] m_dValAry = null;
	private boolean [] m_bValAry = null;
	private TimeStruct [] m_tValAry = null;
	
}
