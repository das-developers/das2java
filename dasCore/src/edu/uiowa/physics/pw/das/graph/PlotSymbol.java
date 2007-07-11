/*
 * PlotSymbol.java
 *
 * Created on July 2, 2007, 3:48 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.uiowa.physics.pw.das.graph;

import edu.uiowa.physics.pw.das.DasProperties;
import edu.uiowa.physics.pw.das.components.propertyeditor.Displayable;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 *
 * @author jbf
 */
public interface PlotSymbol {
    public void draw( Graphics2D g, double x, double y, float size, FillStyle style );
}
