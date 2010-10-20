/*
 * PlotSymbol.java
 *
 * Created on July 2, 2007, 3:48 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.das2.graph;

import java.awt.Graphics2D;

/**
 *
 * @author jbf
 */
public interface PlotSymbol {
    public void draw( Graphics2D g, double x, double y, float size, FillStyle style );
}
