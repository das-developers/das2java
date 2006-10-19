/* File: GrannyTextRenderer.java
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

package edu.uiowa.physics.pw.das.util;

import java.awt.*;
import java.util.ArrayList;
import java.util.Stack;
import java.util.Vector;

/**
 * Renders Grandle and Nystrom strings, like "E=mc!e2"
 * @author  Edward West
 */
public class GrannyTextRenderer {
    
    public static final int LEFT_ALIGNMENT = 0;
    public static final int CENTER_ALIGNMENT = 1;
    public static final int RIGHT_ALIGNMENT = 2;
    
    private Rectangle bounds=null;
    private ArrayList lineBounds;
    private String str;
    private String[] tokens;
    private int alignment = LEFT_ALIGNMENT;        
    private Component parent;
            
    /**
     * Holds value of property fontSize.
     */
    private float fontSize;
    
    public GrannyTextRenderer( ) {
        this.fontSize= 12.0f;
    }
    
    public Rectangle getBounds() {
        maybeInitBounds();
        return new Rectangle(bounds); // defensive copy
    }
    
    private void maybeInitBounds() {
        if (bounds == null) {
            bounds = new Rectangle((Rectangle)lineBounds.get(0));
            for (int i = 1; i < lineBounds.size(); i++) {
                bounds.add((Rectangle)lineBounds.get(i));
            }
        }
    }
    
    public double getWidth() {
        maybeInitBounds();
        return bounds.getWidth();
    }
    
    public double getLineOneWidth() {
        return getLineWidth(1);
    }
    
    public double getLineWidth(int lineNumber) {
        return ((Rectangle)lineBounds.get(lineNumber - 1)).getWidth();
    }
    
    public double getHeight() {
        maybeInitBounds();
        return bounds.getHeight();
    }
    
    public double getAscent() {
        return -1*((Rectangle)lineBounds.get(0)).getY();
    }
    
    public double getDescent() {
        maybeInitBounds();
        return bounds.getHeight() + bounds.getY();
    }
    
    /**
     * reset the current string for the GTR to draw, calculating the boundaries 
     * of the string.  
     */
    public void setString(Component c, String str) {
        this.parent= c;
        bounds = null;
        lineBounds = new ArrayList();
        this.str = str;
        this.tokens = buildTokenArray(str);
        this.draw(null, 0f, 0f, c, false);
    }
    
    public int getAlignment() {
        return alignment;
    }
    
    public void setAlignment(int a) {
        if (a != LEFT_ALIGNMENT && a != CENTER_ALIGNMENT && a != RIGHT_ALIGNMENT) {
            throw new IllegalArgumentException();
        }
        alignment = a;
    }
    
    public void draw(Graphics ig, float ix, float iy) {
        this.draw(ig, ix, iy, null, true);
    }
    
    
    /**
     * Draws the given string and/or computes its bounds.
     *
     * This method is intended to be called by both {@link #drawString(Graphics,String,float,float)}
     * and {@link #getBounds(String,float,float,Component)}.
     *
     * All added string rendering capabilities should be handled here so that any changes
     * will be incorporated into both the rendering algorithm and the bounds finding
     * algorithm at the same time.
     *
     * @param ig Graphic object to use to render the text.  This can be <code>null</code> if
     *    draw is <code>false</code>.
     * @param str The <code>String</code> object to be rendered, and/or measured for bounds.
     * @param ix The x position of the first character of text.
     * @param iy The y position of the baseline of the first line of text.
     * @param c The <code>Component</code> that the <code>String</code> will be rendered in.
     *    This can be <code>null</code> if draw is <code>true</code>
     * @param draw A <code>boolean</code> flag indicating whether or not the drawing code should be executed.
     * @throws NullPointerException if ig is <code>null</code> AND draw is <code>true</code>.
     * @throws NullPointerException if c is <code>null</code> AND draw is <code>false</code>.
     */
    private void draw(Graphics ig, float ix, float iy, Component c,  boolean draw ) {
        Graphics2D g = null;
        Rectangle bounds = null;
        
        if (draw) {
            g = (Graphics2D)ig.create();
            RenderingHints hints = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
            hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHints(hints);
        }
        
        final int NONE  = 0;
        final int SUB_U = 1;
        final int SUB_D = 2;
        final int SUB_L = 3;
        final int EXP   = 4;
        final int IND   = 5;
        
        final int LOWCAPS= 10;  // not in IDL's set
        final int SUB_A = 11;
        final int SUB_B = 12;
        
        class TextPosition {
            public TextPosition(int sub, int ei, float x, float y)
            { this.sub = sub; this.ei = ei; this.x = x; this.y = y; }
            public TextPosition(TextPosition p)
            { copy(p); }
            public void copy(TextPosition p)
            { sub = p.sub; ei = p.ei; x = p.x; y = p.y; }
            public int sub;
            public int ei;
            public float x;
            public float y;
        }
        
        Font baseFont;
        if (draw) {
        //    baseFont = g.getFont().deriveFont( fontSize );
            baseFont= g.getFont();
        }
        else {
        //    baseFont = c.getFont().deriveFont( fontSize );
            baseFont= c.getFont();
        }
        if ( baseFont==null ) {
            baseFont= Font.decode("sans-10");
        }
        
        int lineNum=1;
        
        TextPosition current = new TextPosition(NONE, NONE, ix, iy);
        if (draw) {
            if (alignment == CENTER_ALIGNMENT) {
                current.x += (getWidth() - getLineOneWidth()) / 2.0;
            }
            else if (alignment == RIGHT_ALIGNMENT) {
                current.x += (getWidth() - getLineOneWidth());
            }
        }
        
        if (!draw) {
            bounds= new Rectangle((int)ix,(int)iy,0,0);
        }
        
        Stack saveStack = new Stack();
        
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].charAt(0) == '!') {
                switch (tokens[i].charAt(1)) {
                    case 'A':
                    case 'a':
                        current.sub= SUB_A;
                        current.ei = NONE;
                        break;
                    case 'B':
                    case 'b':
                        current.sub= SUB_B;
                        current.ei = NONE;
                        break;
                    case 'C':
                    case 'c':
                        lineNum++;
                        current.sub = NONE;
                        current.ei = NONE;
                        current.x = ix;
                        current.y += baseFont.getSize2D();
                        if (draw) {
                            g.setFont(baseFont);
                            if (alignment == CENTER_ALIGNMENT) {
                                current.x += (getWidth() - getLineWidth(lineNum)) / 2.0;
                            }
                            else if (alignment == RIGHT_ALIGNMENT) {
                                current.x += (getWidth() - getLineWidth(lineNum));
                            }
                        }
                        saveStack.clear();
                        if (!draw) {
                            lineBounds.add(bounds);
                            bounds = new Rectangle((int)current.x, (int)current.y, 0, 0);
                        }
                        break;
                    case 'U':
                    case 'u':
                        current.sub = SUB_U;
                        current.ei = NONE;
                        break;
                    case 'D':
                    case 'd':
                        current.sub = SUB_D;
                        current.ei = NONE;
                        break;
                    case 'L':
                    case 'l':
                        current.sub = SUB_L;
                        current.ei = NONE;
                        break;
                    case 'K':
                    case 'k':
                        current.ei = LOWCAPS;
                        break;
                    case 'E':
                    case 'e':
                        current.ei = EXP;
                        break;
                    case 'I':
                    case 'i':
                        current.ei = IND;
                        break;
                    case 'S':
                    case 's':
                        saveStack.push(new TextPosition(current));
                        break;
                    case 'R':
                    case 'r':
                        if (saveStack.peek() == null) return;
                        current.copy((TextPosition)saveStack.pop());
                        break;
                    case 'N':
                    case 'n':
                        current.sub = NONE;
                        current.ei = NONE;
                        break;
                    case  '!':
                        break;
                    default:break;
                }
            }
            else {
                Font font = baseFont;
                float size = baseFont.getSize2D();
                float y = current.y;
                switch (current.sub) {
                    case SUB_U:
                        font = baseFont.deriveFont(size * 0.62f);
                        y = y - 0.38f * size;
                        size = size * 0.62f;
                        break;
                    case SUB_D:
                        font = baseFont.deriveFont(size * 0.62f);
                        y = y + 0.31f * size;
                        size = size * 0.62f;
                        break;
                    case SUB_L:
                        font = baseFont.deriveFont(size * 0.62f);
                        y = y + 0.62f * size;
                        size = size * 0.62f;
                        break;
                    case SUB_A:
                        y= current.y - size/2;
                        break;
                    case SUB_B:
                        y= current.y + size/2;
                        break;
                        
                    default:break;
                }
                switch (current.ei) {
                    case EXP:
                        font = font.deriveFont(size * 0.44f);
                        y = y - 0.56f * size;
                        break;
                    case IND:
                        font = font.deriveFont(size * 0.44f);
                        y = y + 0.22f * size;
                        break;
                    case LOWCAPS:
                        font= font.deriveFont(size * 0.80f );
                        break;
                    default:break;
                }
                if (draw) {
                    g.setFont(font);
                    g.drawString(tokens[i], current.x, y);
                    current.x += g.getFontMetrics(font).stringWidth(tokens[i]);
                    //bounds.translate((int)ix,(int)iy);
                    //g.draw(bounds);  //useful for debugging
                    //g.drawLine((int)ix,(int)iy,(int)ix+4,(int)iy);
                }
                else {
                    FontMetrics fm= c.getFontMetrics(font);
                    bounds.add(current.x, y+fm.getDescent());
                    bounds.add(current.x+fm.stringWidth(tokens[i]),y-fm.getAscent() ); // removed -5.0 pixels 
                    current.x += c.getFontMetrics(font).stringWidth(tokens[i]);
                }
            }
        } // for (int i = 0; i < tokens.length; i++)
        if (!draw) {
            lineBounds.add(bounds);
        }
    }
    
    private static String[] buildTokenArray(String str) {
        Vector vector = new Vector();
        int begin;
        int end = 0;
        int index = 0;
        while(end < str.length()) {
            begin = end;
            if (str.charAt(begin) == '!') {
                end = begin + 2;
            }
            else {
                end = str.indexOf('!', begin);
                if (end == -1) end = str.length();
            }
            vector.add(str.substring(begin, end));
        }
        
        String[] list = new String[vector.size()];
        
        vector.copyInto((Object[])list);
        
        return list;
    }
    
    public String toString() {
        maybeInitBounds();
        StringBuffer buffer = new StringBuffer(getClass().getName());
        buffer.append(": ").append(str).append(", ");
        buffer.append(bounds).append(", ").append(lineBounds).append(", ");
        return buffer.toString();
    }
    
    public void drawBounds(Graphics g, int ix, int iy) {
        g.setColor(Color.BLUE);
        g.drawRect(bounds.x + ix, bounds.y + iy, bounds.width, bounds.height);
        g.setColor(Color.GREEN);
        for (java.util.Iterator i = lineBounds.iterator(); i.hasNext();) {
            Rectangle rc = (Rectangle)i.next();
            g.drawRect(rc.x + ix, rc.y + iy, rc.width, rc.height);
        }
    }

    /**
     * Getter for property fontSize.
     * @return Value of property fontSize.
     */
    public float getFontSize() {
        return this.fontSize;
    }

    /**
     * Setter for property fontSize.
     * @param fontSize New value of property fontSize.
     */
    public void setFontSize(float fontSize) {
        this.fontSize = fontSize;
        if ( this.parent!=null ) setString( parent, this.str );
    }
        
}
