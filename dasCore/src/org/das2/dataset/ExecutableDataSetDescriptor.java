/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.dataset;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.DasException;
import org.das2.DasIOException;
import org.das2.client.DataSetStreamHandler;
import org.das2.datum.Datum;
import org.das2.datum.Units;
import org.das2.system.DasLogger;
import org.das2.util.DasProgressMonitorInputStream;
import org.das2.util.StreamTool;
import org.das2.util.monitor.ProgressMonitor;

/** DataSetDescriptor implementation
 *
 * @author eew
 */
public class ExecutableDataSetDescriptor extends DataSetDescriptor {

	private String exePath;
	private String[] commandFmts;

    private static final Logger logger= DasLogger.getLogger(DasLogger.DATA_TRANSFER_LOG);
	
	public ExecutableDataSetDescriptor(String exePath, String[] commandFmts) {
		this.exePath = exePath;
		this.commandFmts = Arrays.copyOf(commandFmts, commandFmts.length);

		File exeFile = new File(exePath);
		if (!exeFile.exists()) {
			throw new IllegalArgumentException(exePath + " does not exist");
		}
		if (!exeFile.canExecute()) {
			throw new IllegalArgumentException(exePath + " is not executable");
		}
	}

	@Override
	protected DataSet getDataSetImpl(Datum start, Datum end, Datum resolution, ProgressMonitor monitor) throws DasException {
		InputStream in;
		DataSet result;

		String[] command = new String[commandFmts.length+1];
		command[0] = exePath;
		for (int iParam = 0; iParam < commandFmts.length; iParam++) {
			String sParam = commandFmts[iParam];
			if (sParam.contains("%{start}")) {
				sParam = sParam.replace("%{start}", start.toString());
			}
			else if (sParam.contains("%{end}")) {
				sParam = sParam.replace("%{end}", end.toString());
			}
			else if (sParam.contains("%{resolution}")) {
				sParam = sParam.replace("%{resolution}",
						Double.toString(resolution.doubleValue(Units.seconds)));
			}
			command[iParam+1] = sParam;
		}
		ProcessBuilder builder = new ProcessBuilder(command);
		builder.redirectError(ProcessBuilder.Redirect.INHERIT);
		try {
			Process p = builder.start();
			in = p.getInputStream();
			final DasProgressMonitorInputStream mpin = new DasProgressMonitorInputStream(in, monitor);
			ReadableByteChannel channel = Channels.newChannel(mpin);

			DataSetStreamHandler handler = new DataSetStreamHandler(properties, monitor);

			StreamTool.readStream(channel, handler);
			return handler.getDataSet();

		} catch (IOException ex) {
			throw new DasIOException(ex);
		}

	}

	@Override
	public Units getXUnits() {
		throw new UnsupportedOperationException("Not supported yet.");
	}
	
}
