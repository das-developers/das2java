/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.reader;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Logger;

/** Generic interface for readers loadable by the tomcat server, or by the standalone
 * applications.
 *
 * @author cwp
 */
public interface Reader {

	/** Does this reader support connected operations over a BEEP channel */
	public boolean canConnect();

	/** Does this reader support a particular stream format */
	public boolean supportsFormat(OutputFormat fmt);

	/** Outputs all data in the range denoted by the selectors into the given
	 * output stream.
	 *
	 * Multiple calls to the retrieve interface for the same class instance must be
	 * thread safe
	 *
	 * @param selectors An array of selectors, must have at least one specified
	 * @param format The data and header output format requested
	 * @param stream The stream where the bytes should be emitted.
	 */
	public void retrieve(List<Selector> lSel, OutputFormat fmt, OutputStream fOut,
	                     Logger log) 
								throws IOException, NoDataException, BadQueryException;

	/** Setup a bidirectional communications channel with a client.
	 * Once the channel is connected, the server may no longer be involved.
	 *
	 * @param selectors An initial array of selectors there may be not specified at
	 *        startup, in which case the reader just reports ready.
	 * @param format The initial data and header output format requested
	 * @param beepChannel The channel onto which bytes should be written
	 * @returns 0 if the connection was properly closed, or a non zero value from the
	 *          underlying beep library if an improper shutdown occurred.
	 */
	public int connect(List<Selector> lSel, OutputFormat fmt, Object beepChannel,
		                Logger log) throws IOException, BadQueryException;
}
