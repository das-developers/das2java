/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.reader;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/** This log formatter removes the date and class lines from the output, because end
 * users don't care about that anyway and the message strings themselves can be looked
 * up in the source code to determine what function emitted the message.
 *
 * @author cwp
 */
public class RdrLogFormatter extends Formatter{

	public RdrLogFormatter(){
		super();
	}

	@Override
	public String format(LogRecord record){
		StringBuffer sb = new StringBuffer();

		if(record.getLevel() == Level.OFF)
			return "";
		
		if(record.getLevel() == Level.SEVERE)
			return sb.append("ERROR: ").append(formatMessage(record)).append('\n').toString();
				
		if(record.getLevel() == Level.WARNING)
			return sb.append("WARNING: ").append(formatMessage(record)).append('\n').toString();

		if((record.getLevel() == Level.INFO)||(record.getLevel() == Level.CONFIG))
			return sb.append("INFO: ").append(formatMessage(record)).append('\n').toString();

		if(record.getLevel() == Level.FINE)
			return sb.append("DEBUG: ").append(formatMessage(record)).append('\n').toString();

		return sb.append("TRACE: ").append(formatMessage(record)).append('\n').toString();
	}

	@Override
	public String getHead(Handler h){
		return super.getHead(h);
	}

	@Override
	public String getTail(Handler h){
		return super.getTail(h);
	}
}
