/* File: Psym.java
 * Copyright (C) 2002-2003 The University of Iowa
 * Created by: Jeremy Faden <jbf@space.physics.uiowa.edu>
 *             Jessica Swanner <jessica@space.physics.uiowa.edu>
 *             Edward E. West <eew@space.physics.uiowa.edu>
 *
 * This file is part of the das2 library.
 *
 * das2 is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package edu.uiowa.physics.pw.das.graph;

import org.das2.DasProperties;
import edu.uiowa.physics.pw.das.components.*;
import edu.uiowa.physics.pw.das.components.propertyeditor.*;

/** Type-safe enumeration class for the psym property of
 * a <code>DasSymbolPlot</code>.
 */

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.geom.Line2D;
import java.awt.image.*;

public class Psym implements Enumeration, Displayable {
    
    private static final String[] NAMES = {
        "none",     //0
        "dots",      //1
        "circles",   //2
        "triangles", //3
        "cross",      //4        
                
    };
    
    public static final Psym NONE = new Psym(0);
    
    public static final Psym DOTS = new Psym(1);
    
    public static final Psym CIRCLES = new Psym(2);
    
    public static final Psym TRIANGLES = new Psym(3);
    
    public static final Psym CROSS = new Psym(4);
    
    private int nameIndex;
    
    private Line2D line = new Line2D.Double();
    private Ellipse2D ellipse = new Ellipse2D.Double();
    
    ImageIcon imageIcon;
    
    private Psym(int nameIndex) {
        this.nameIndex = nameIndex;
        this.line= new Line2D.Double();
        Image i= new BufferedImage(10,10,BufferedImage.TYPE_INT_RGB);
        Graphics2D g= ( Graphics2D) i.getGraphics();
        g.setBackground( Color.white );
        g.setRenderingHints(DasProperties.getRenderingHints());
        g.setColor( Color.white );
        g.fillRect(0,0, 10,10);
        g.setColor( Color.black );
        draw(g,5,5,2.f);
        
        this.imageIcon= new ImageIcon(i);
    }
    
    public String toString() {
        return NAMES[nameIndex];
    }
    
    public String getListLabel() {
        return NAMES[nameIndex];
    }
    
    public Icon getListIcon() {
        return imageIcon;
    }
    
    /** Draw the psym at the given coordinates.
     * if <code>drawsLines()</code> returns false, then the
     * ix and iy parameters are ignored.
     */
    public void draw(Graphics g, double x, double y, float size) {
        //We are not guaranteed to get a Graphics2D.
        Graphics2D g2 = (Graphics2D)(g instanceof Graphics2D ? g : null);
        
        switch (nameIndex) {
            case 0: //LINES
                break;
            case 1: //DOTS
                if ( size < 1f ) {
                    if (g instanceof Graphics2D) {
                        ellipse.setFrame(x, y, 1, 1);
                        g2.fill(ellipse);
                    }
                    else {
                        g.fillOval((int)x, (int)y, 1, 1);
                    }
                } else {
                    if (g instanceof Graphics2D) {
                        ellipse.setFrame(x-size, y-size, size*2, size*2);
                        g2.fill(ellipse);
                    }
                    else {
                        g.fillOval((int)(x - size), (int)(x - size), (int)(size*2), (int)(size*2));
                    }
                }
                break;
            case 2: //CIRCLES
                Color color0= g.getColor();
                ellipse.setFrame(x - size, y - size, size * 2, size * 2);
                
                Color backgroundColor= Color.white;
                g.setColor(backgroundColor);
                if (g instanceof Graphics2D) {
                    g2.fill(ellipse);
                }
                else {
                    g.fillOval((int)(x-size), (int)(y-size), (int)(size*2), (int)(size*2));
                }
                g.setColor(color0);
                if (g instanceof Graphics2D) {
                    g2.draw(ellipse);
                }
                else {
                    g.drawOval((int)(x-size), (int)(y-size), (int)(size*2), (int)(size*2));
                }
                break;
            case 3: //TRIANGLES
                drawTriangle(g, x, y, size);
                break;
            case 4: //CROSS
                if (g instanceof Graphics2D) {
                    line.setLine(x-size, y, x+size, y);
                    g2.draw(line);
                    line.setLine(x, y-size, x, y+size);
                    g2.draw(line);
                }
                else {
                    g.drawLine((int)(x-size), (int)y, (int)(x+size), (int)y);
                    g.drawLine((int)x, (int)(y-size), (int)x, (int)(y+size));
                }
                break;
            default: throw new IllegalArgumentException("Invalid nameIndex for psym");
        }
    }
    
    public void drawTriangle(Graphics g, double x, double y, float size ) {
        if (g instanceof Graphics2D) {
            Graphics2D g2 = (Graphics2D)g;
            line.setLine(x, y-size, x+size, y+size);
            g2.draw(line);
            line.setLine(x+size, y+size, x-size, y+size);
            g2.draw(line);
            line.setLine(x-size, y+size, x, y-size);
            g2.draw(line);
        }
        else {
            g.drawLine((int)x, (int)(y-size), (int)(x+size), (int)(y+size));
            g.drawLine((int)(x+size), (int)(y+size), (int)(x-size), (int)(y+size));
            g.drawLine((int)(x-size), (int)(y+size), (int)x, (int)(y-size));
        }
    }
    
    public static Psym parsePsym(String str) {
        if (str.equals("none")) {
            return NONE;
        }
        else if (str.equals("dots")) {
            return DOTS;
        }
        else if (str.equals("circles")) {
            return CIRCLES;
        }
        else if (str.equals("triangles")) {
            return TRIANGLES;
        }
        else if (str.equals("cross")) {
            return CROSS;
        }
        else {
            throw new IllegalArgumentException(str);
        }
    }
    
}

