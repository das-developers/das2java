/* File: Psym.java
 * Copyright (C) 2002-2003 University of Iowa
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

/** Type-safe enumeration class for the psym property of
 * a <code>DasSymbolPlot</code>.
 */

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;

public class Psym implements edu.uiowa.physics.pw.das.components.PropertyEditor.Enumeration {
    
    private static final String[] NAMES = {
        "lines",     //0
        "dots",      //1
        "circles",   //2
        "triangles", //3
        "cross",      //4
        "psym10",
    };
    
    private static final boolean[] DRAWS_LINES = {
        true,  //0 LINES
        false, //1 DOTS
        false, //2 CIRCLES
        false, //3 TRIANGLES
        false, //4 CROSS
        true,
    };
    
    public static final Psym LINES = new Psym(0);
    
    public static final Psym DOTS = new Psym(1);
    
    public static final Psym CIRCLES = new Psym(2);
    
    public static final Psym TRIANGLES = new Psym(3);
    
    public static final Psym CROSS = new Psym(4);
    
    public static final Psym PSYM10= new Psym(5);
    
    private int nameIndex;
    Line2D.Double line;
    
    private Psym(int nameIndex) {
        this.nameIndex = nameIndex;
        this.line= new Line2D.Double();
    }
    
    public String toString() {
        return NAMES[nameIndex];
    }
    
    public Icon getListIcon() {
        return null;
    }
    
    public boolean drawsLines() {
        return DRAWS_LINES[nameIndex];
    }
    
    /** Draw the psym at the given coordinates.
     * if <code>drawsLines()</code> returns false, then the
     * ix and iy parameters are ignored.
     */
    public void draw(Graphics g, int x, int y) {
        switch (nameIndex) {
            case 0: //LINES
                break;
            case 1: //DOTS
                g.drawLine(x, y, x, y);
                break;
            case 2: //CIRCLES
                g.drawOval(x-4, y-4, 8, 8);
                break;
            case 3: //TRIANGLES
                drawTriangle(g, x, y);
                break;
            case 4: //CROSS
                g.drawLine(x-1, y, x+1, y);
                g.drawLine(x, y-1, x, y+1);
                break;
            case 5:
                break;
            default: throw new IllegalArgumentException("Invalid nameIndex for psym");
        }
    }
    
    public void drawLine(Graphics2D graphics, int x0, int y0, int x, int y ) {
        if ( nameIndex==0 ) {
            line.setLine(x0,y0,x,y);
            graphics.draw(line);
        } else if ( nameIndex==5 ) {
            int xMid= (x0 + x) / 2;
            line.setLine(x0,y0,xMid,y0);  graphics.draw(line);
            line.setLine(xMid,y0,xMid,y);  graphics.draw(line);
            line.setLine(xMid,y,x,y);  graphics.draw(line);
        }
    }
    
    public void drawTriangle(Graphics g, int x, int y) {
        g.drawLine(x, y-2, x+2, y+2);
        g.drawLine(x+2, y+2, x-2, y+2);
        g.drawLine(x-2, y+2, x, y-2);
    }
    
    public static Psym parsePsym(String str) {
        if (str.equals("lines")) {
            return LINES;
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

