/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.util.reader;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;

/** Generic interface for readers loadable by the tomcat server, or by the standalone
 * applications.
 *
 * @author cwp
 */
public interface StreamSource {

	/** Does this reader support connected operations over a BEEP channel */
	public boolean canConnect();

	/** Outputs all data in the range denoted by the selectors into the given
	 * output stream.
	 * @param selectors An array of selectors, must have at least one specified
	 * @param format The data and header output format requested
	 * @param stream The stream where the bytes should be emitted.
	 */
	public void retrieve(Selector[] selectors, OutputFormat format, OutputStream stream,
	                     Logger logger) throws IOException, NoDataException;

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
	public int connect(Selector[] selectors, OutputFormat format, Object beepChannel,
		                Logger logger) throws IOException;
}
