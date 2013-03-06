/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.reader;

import java.util.List;

/**  Generates a selector set by parsing a list of parameters which could be from a
 * command line.  This is intended mostly to help with command line readers.  But could
 * be used else where.
 *
 * If the generator is put into Das2 Server compatible mode, the first not keyword=value
 * pair is parsed as a start time, and the second is parsed as an end time.
 *
 * @author cwp
 */
public class SelectorGenerator {

	/** Create a selector generator that expects all elements of the list to conform
	 * to the das3 style command line interface.
	 */
	public SelectorGenerator(){

	}

	public void setDas2Compatible(String sTimeKey){
		
	}

	public void addKey(String sKey, Selector.Format fmt){

	}

	public void addRangeKey(String sKey, Selector.Format fmt){

	}

	List<Selector> fromMainArgs(String[] lArgs){

		return null;
	}

	List<Selector> fromList(List<String> lArgs){

		return null;
	}
}
