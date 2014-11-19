/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.reader;

import java.util.LinkedList;
import java.util.List;
import org.das2.datum.CalendarTime;

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

	// Instance Data
	protected String m_sKey = null;
	protected Constraint.Format m_format = null;
	protected List<Constraint> m_lConstraints = null;
	
	/** Construct an empty selector */
	public Selector(String sKey, Constraint.Format fmt){
		m_sKey = sKey;
		m_format = fmt;
	}

	/** Add a constraint to a selector */
	public void addConstraint(Constraint cons){
		if(cons.getFormat() != m_format)
			throw new IllegalArgumentException(String.format("Constraint format is %s, "
				+ "expected %s", cons.getFormat(), m_format));
		if(m_lConstraints == null){
			m_lConstraints = new LinkedList<>();
		}
		m_lConstraints.add(cons);
	}

	/** Get the selection as a single string, no matter the type of selector */
	@Override
	public String toString(){

		StringBuilder sb = new StringBuilder();
		for(Constraint cons: m_lConstraints){
			if(sb.length() != 0) sb.append(' ');
			sb.append(String.format("%s%s%s", m_sKey, cons.getOpStr(), cons.getValue()));
		}
		return sb.toString();
	}

	/** Get the key (which is just a string) for this selector.
	 * Note That no two selectors for the same data-source may have the same key.
	 */
	public String key(){ return m_sKey;	}

	/** Get the data type for the selector */
	public Constraint.Format format(){ return m_format;}

	/** Get the number of constraints defined for this selector */
	public int getConstraintCount(){
		if(m_lConstraints == null) return 0;
		else return m_lConstraints.size();
	}

	/** Grab a constraint to work with */
	public Constraint getConstraint(int i){
		return m_lConstraints.get(i);
	}
	
	/** Get a constraint by type.
	 * 
	 * @param op The type of comparision to check.
	 * @return null if the given comparison isn't one of the one's specified, otherwise
	 *         a constraint object is returned.  It will have to be queiried to find it's
	 *         value type.
	 */
	public Constraint getConstraint(Constraint.Op op){
		for(Constraint cons: m_lConstraints){
			if(cons.getOp() == op){
				return cons;
			}
		}
		return null;
	}

	// Check that we can use the convenience rountines
	private void ckUniqueEq(){
		if(m_lConstraints.size() != 1)
			throw new UnsupportedOperationException("Selector does not contain a unique constraint");

		if(m_lConstraints.get(0).getOp() != Constraint.Op.EQ)
			throw new UnsupportedOperationException("Selector does not have an equality constraint.");
	}

	/** Convenience routine to get a string when there is only a single equality 
	 * constraint. */
	public String getValue(){
		ckUniqueEq();
		return m_lConstraints.get(0).getValue();
	}

	public int getIntValue(){
		ckUniqueEq();
		return m_lConstraints.get(0).getIntValue();
	}

	public double getRealValue(){
		ckUniqueEq();
		return m_lConstraints.get(0).getRealValue();
	}

	public boolean getBoolValue(){
		ckUniqueEq();
		return m_lConstraints.get(0).getBoolValue();
	}

	public CalendarTime getTimeValue(){
		ckUniqueEq();
		return m_lConstraints.get(0).getTimeValue();
	}
}
