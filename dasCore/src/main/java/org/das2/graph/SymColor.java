/* File: SymColor.java
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

package org.das2.graph;

import org.das2.components.propertyeditor.Displayable;
import org.das2.components.propertyeditor.Enumeration;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 *
 * @author  jbf
 */

public final class SymColor extends Color implements Enumeration, Displayable {
    
    private String name;
    private ImageIcon imageIcon;
    
    public static final SymColor black= new SymColor( "black",Color.black );
    public static final SymColor blue= new SymColor( "blue",Color.blue );
    public static final SymColor lightRed= new SymColor( "lightRed", new Color( 255, 128, 128 ) );
    public static final SymColor red= new SymColor( "red",Color.red );
    public static final SymColor darkRed= new SymColor( "darkRed",Color.red.darker() );
    public static final SymColor green= new SymColor( "green",Color.green );
    public static final SymColor darkGreen= new SymColor( "darkGreen",Color.green.darker() );
    public static final SymColor white= new SymColor( "white",Color.white );
    public static final SymColor gray= new SymColor( "gray",Color.gray );
    public static final SymColor magenta = new SymColor( "magenta",Color.magenta);
    
    /** Creates a new instance of SymColor */
    private SymColor(String name, Color color) {
        this(name, color.getRGB());
    }
    
    /** Creates a new instance of SymColor */
    private SymColor(String name, int rgb) {
        super(rgb);
        this.name= name;
        Image i= new BufferedImage(10,10,BufferedImage.TYPE_INT_RGB);
        Graphics g= i.getGraphics();
        g.setColor(this);
        g.fillRect(0,0,10,10);        
        this.imageIcon= new ImageIcon(i);
    }
    
    /** An icon can be provided that will be shown in a list
     * along with the textual description of the element.
     * This method should return <code>null</code> if there
     * is no icon available.
     */
    public Icon getListIcon() {
        return imageIcon;
    }
    
    public String getListLabel() {
        return name;
    }
    
    public String toString() {
        return name;
    }
    
    public Color toColor() {
        return this;
    }
    
    public static SymColor parseSymColor(String str) {
        if (str.equals("black")) {
            return black;
        }
        else if (str.equals("blue")) {
            return blue;
        }
        else if (str.equals("red")) {
            return red;
        }
        else if (str.equals("green")) {
            return green;
        }
        else if (str.equals("white")) {
            return white;
        }
        else if (str.equals("gray")) {
            return gray;
        }
        else {
            throw new IllegalArgumentException(str);
        }
    }
}
