/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.reader;

/** Readers throw this type of exception when the set of selectors does not sufficiently
 * nail down a dataset to return.  Usually a request validator will catch bad queries
 * but, this condition could conceievably still arise due to a miss configured server.
 *
 * @author cwp
 */
public class BadQueryException extends Exception {

	public BadQueryException(String str){
		super(str);
	}

	public BadQueryException(String str, Throwable ex){
		super(str, ex);
	}

}
