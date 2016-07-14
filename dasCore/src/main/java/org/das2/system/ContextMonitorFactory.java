/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.system;

import org.das2.graph.DasCanvasComponent;
import org.das2.util.monitor.ProgressMonitor;

/**
 *
 * @author eew
 */
public class ContextMonitorFactory extends DefaultMonitorFactory {

	private DasCanvasComponent context;

	public ContextMonitorFactory(DasCanvasComponent context) {
		if (context == null) throw new NullPointerException();
		this.context = context;
	}

	@Override
	public ProgressMonitor getMonitor(String label, String description) {
		return super.getMonitor(context, label, description);
	}

}
