package org.das2.reader;

import java.text.ParseException;
import org.das2.datum.CalendarTime;

/**
 * Inner class to hold a value plus a comparison operator
 */
public final class Constraint{

	/** Defines a comparison type, should probably be moved out to some other class */
	public static enum Op {EQ, NE, LT, GT, LE, GE;
		@Override
		public String toString(){
			return "."+name()+".";
		}
		
		static public Op fromString(String sStr){
			if(sStr.equalsIgnoreCase(".eq.")) return EQ;
			if(sStr.equalsIgnoreCase(".ne.")) return NE;
			if(sStr.equalsIgnoreCase(".lt.")) return LT;
			if(sStr.equalsIgnoreCase(".end.")) return LT;
			if(sStr.equalsIgnoreCase(".gt.")) return GT;
			if(sStr.equalsIgnoreCase(".ge.")) return GE;
			if(sStr.equalsIgnoreCase(".beg.")) return GE;
			if(sStr.equalsIgnoreCase(".le.")) return LE;
			throw new IllegalArgumentException("Unknown comparison string '"+sStr+"'");
		}
	};

	/** Defines the value format for the selector */
	public static enum Format {BOOLEAN, INTEGER, REAL, STRING, TIMEPOINT};

	// Basic properties
	private Op m_op;
	private String m_sVal;
	private Format m_fmt;
	// Place to park conversions so they don't have to be redone over and over
	private boolean m_bVal;
	private int m_nVal;
	private double m_dVal;
	private CalendarTime m_ctVal;

	public Constraint(Op op, Format fmt, String sVal)
		throws ParseException{

		if(sVal == null)
			throw new NullPointerException("Constraint value must not be null.");
		m_op = op;
		m_fmt = fmt;
		m_sVal = sVal;
		switch(m_fmt){
		case BOOLEAN:
			if(sVal.equalsIgnoreCase("false") || sVal.equals("0"))
				m_bVal = false;
			else
				m_bVal = true;
			break;
		case INTEGER:
			m_nVal = Integer.parseInt(sVal);
			break;
		case REAL:
			m_dVal = Double.parseDouble(sVal);
			break;
		case TIMEPOINT:
			m_ctVal = new CalendarTime(sVal);
			break;
		}
	}

	public Format getFormat(){
		return m_fmt;
	}

	public Op getOp(){
		return m_op;
	}

	public String getOpStr(){
		return m_op.toString();
	}

	public String getValue(){ return m_sVal; }

	public int getIntValue(){
		if(m_fmt != Format.TIMEPOINT)
			throw new UnsupportedOperationException("Constraint format is not INTEGER");
		return m_nVal;
	}

	public double getRealValue(){
		if(m_fmt != Format.REAL)
			throw new UnsupportedOperationException("Constraint format is not REAL");
		return m_dVal;
	}

	public boolean getBoolValue(){
		if(m_fmt != Format.BOOLEAN)
			throw new UnsupportedOperationException("Constraint format is not BOOLEAN");
		return m_bVal;
	}

	public CalendarTime getTimeValue(){
		if(m_fmt != Format.TIMEPOINT)
			throw new UnsupportedOperationException("Constraint format is not DATETIME");
		
		return m_ctVal;
	}
}
