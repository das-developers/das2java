/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.util;

import java.util.Enumeration;
import java.util.Vector;

/** A grab bag of text processing utilities
 *
 * @author cwp
 */
public class TextUtil {
	/** Line wrap function, taken from
	 *   http://progcookbook.blogspot.com/2006/02/text-wrapping-function-for-java.html
	 * and then customized a little
	 *
	 * @param sText - The text to wrap
	 * @param nLineLen - The length of each lines text area
	 * @param sPrefix - A prefix string, to be added to each line, if not null
	 * @return
	 */
	public static String[] wrapText(String sText, int nLineLen, String sPrefix){
		// return empty array for null text
		if(sText == null){
			return new String[]{};
		}

		sText = sText.trim();

		// Strip out all the newlines and tabs that might happen to be in the text
		sText = sText.replaceAll("\t\r\n", "");

		// Collapse 2+ spaces to a single space
		sText = sText.replaceAll("\\s+", " ");

		// return text if len is zero or less
		if(nLineLen <= 0){
			return new String[]{sText};
		}

		// return text if less than length
		if(sText.length() <= nLineLen){
			if(sPrefix == null)
				return new String[]{sText};
			else
				return new String[]{sPrefix + sText};
		}

		char[] chars = sText.toCharArray();
		@SuppressWarnings("UseOfObsoleteCollectionType")
		Vector lines = new Vector();
		StringBuffer line = new StringBuffer();
		StringBuffer word = new StringBuffer();

		for(int i = 0; i < chars.length; i++){
			word.append(chars[i]);

			if(chars[i] == ' '){
				if((line.length() + word.length()) > nLineLen){
					lines.add(line.toString());
					line.delete(0, line.length());
				}

				line.append(word);
				word.delete(0, word.length());
			}
		}

		// handle any extra chars in current word
		if(word.length() > 0){
			if((line.length() + word.length()) > nLineLen){
				lines.add(line.toString());
				line.delete(0, line.length());
			}
			line.append(word);
		}

		// handle extra line
		if(line.length() > 0){
			lines.add(line.toString());
		}

		String[] lRet = new String[lines.size()];
		int c = 0; // counter
		if(sPrefix == null){
			for(Enumeration e = lines.elements(); e.hasMoreElements(); c++){
				lRet[c] = (String) e.nextElement();
			}
		}
		else{
			for(Enumeration e = lines.elements(); e.hasMoreElements(); c++){
				lRet[c] = sPrefix + (String) e.nextElement();
			}
		}

		return lRet;
	}
}
