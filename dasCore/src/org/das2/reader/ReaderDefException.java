/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.reader;

/** Class to represent errors with a data source definition.
 * Typically this means the XML file has a problem.
 *
 * @author cwp
 */
public class ReaderDefException extends Exception{

	public ReaderDefException(String str){
		super(str);
	}

	public ReaderDefException(String str, Throwable ex){
		super(str, ex);
	}
}
