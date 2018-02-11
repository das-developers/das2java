package org.das2.graph;

/**
 * Interface to allow customizations to plots to be injected at the end of the DasPlot constructor.
 * The caller needs to be very cautious using this, especially when calling any non-final methods.
 */
public interface Customizer {
	/**
	 * Perform any action(s) to change the default behavior of a newly constructed plot.
	 * @param plot the plot being customized
	 */
	void customize(DasPlot plot);
}