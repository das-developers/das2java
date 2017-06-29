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

package org.das2.util;

import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for rendering "Granny" strings, which use the codes
 * identified by Grandle and Nystrom in their 1980 paper to provide 
 * rich formatting such as new lines and superscripts.
 * These are strings like "E=mc!e2" where the !e indicates the pen should be 
 * moved to the exponent position before drawing.  This supports sequences
 * including:<pre>
 * !A  shift up one half line
 * !B  shift down one half line  (e.g.  !A3!n-!B4!n is 3/4).
 * !C  newline 
 * !D  subscript 0.62 of old font size.
 * !U  superscript of 0.62 of old font size.
 * !E  superscript 0.44 of old font size.
 * !I  subscript 0.44 of old font size.
 * !N  return to the original font size.
 * !R  restore position to last saved position
 * !S  save the current position.
 * !K  reduce the font size. (Not in IDL's set.)
 * !!  the exclamation point (!)
 *   </pre>
 * For Greek and math symbols, Unicode characters should be
 * used like so: &amp;#9742; (&#9742 phone symbol), or symbols like <tt>&amp;Omega;</tt> and <tt>&amp;omega;</tt>
 * 
 * 
 * @author  Edward West
 */
public class GrannyTextRenderer {
    
    public static final float LEFT_ALIGNMENT = 0.0f;
    public static final float CENTER_ALIGNMENT = 0.5f;
    public static final float RIGHT_ALIGNMENT = 1.0f;
    
    private Rectangle bounds=null;
    private ArrayList<Rectangle> lineBounds;
    private String str;
    private String[] tokens;
    private float alignment = LEFT_ALIGNMENT;
    
    private static final Logger logger= LoggerManager.getLogger("das2.graph");
    
    public GrannyTextRenderer( ) {
        //setAlignment(CENTER_ALIGNMENT);
    }
    
    /**
     * returns the bounds of the current string.  The lower-left corner of
     * the first character will be roughly (0,0), to be compatible with
     * FontMetrics.getStringBounds().
     *
     * @return a Rectangle indicating the text boundaries.
     * @throws IllegalArgumentException if the string has not been set.
     */
    public Rectangle getBounds() {
        if ( lineBounds==null ) throw new IllegalArgumentException("string is not set");
        maybeInitBounds();
        return new Rectangle(bounds); // defensive copy
    }

    private void maybeInitBounds() {
        if (bounds == null) {
            ArrayList<Rectangle> llineBounds= new ArrayList<>(this.lineBounds);
            if ( llineBounds.size()>0 ) {
                bounds = new Rectangle((Rectangle)llineBounds.get(0));
                for (int i = 1; i < llineBounds.size(); i++) {
                    bounds.add(llineBounds.get(i));
                }
            } else {
                bounds = new Rectangle( 0,-12,12,12 );
            }
        }
    }
    
    /**
     * returns the width of the bounding box, in pixels.
     * @return the width of the bounding box, in pixels.
     * @throws IllegalArgumentException if the string has not been set.
     */
    public double getWidth() {
        if ( lineBounds==null ) throw new IllegalArgumentException("string is not set");
        maybeInitBounds();
        return bounds.getWidth();
    }
    
    /**
     * returns the width in pixels of the first line.
     * @return the width in pixels of the first line.
     * @throws IllegalArgumentException if the string has not been set.
     *
     */
    public double getLineOneWidth() {
        if ( lineBounds==null ) throw new IllegalArgumentException("string is not set");
        return getLineWidth(1);
    }
    
    /**
     * returns the calculated width each line.
     * @param lineNumber the index of the line, starting with 1.
     * @return the width of the bounding box, in pixels.
     * @throws IndexOutOfBoundsException if no such line exists.
     */
    private double getLineWidth(int lineNumber) {
        if ( lineBounds==null ) throw new IllegalArgumentException("string is not set");
        return ((Rectangle)lineBounds.get(lineNumber - 1)).getWidth();
    }
    
    /**
     * returns the hieght of the calculated bounding box.
     * @return the height of the bounding box, in pixels.
     * @throws IllegalArgumentException if the string has not been set.
     */
    public double getHeight() {
        if ( lineBounds==null ) throw new IllegalArgumentException("string is not set");
        maybeInitBounds();
        return bounds.getHeight();
    }
    
    /**
     * return the amount that the bounding box will go above the baseline.
     * This is also the height of the first line.
     * @return the amount that the bounding box will go above the baseline.
     * @throws IllegalArgumentException if the string has not been set.
     */
    public double getAscent() {
        if ( lineBounds==null ) throw new IllegalArgumentException("string is not set");
        return -1*((Rectangle)lineBounds.get(0)).getY();
    }
    
    /**
     * return the amount that the bounding box will go below the baseline.
     * @return the amount that the bounding box will go below the baseline.
     * @throws IllegalArgumentException if the string has not been set.
     */
    public double getDescent() {
        if ( lineBounds==null ) throw new IllegalArgumentException("string is not set");
        maybeInitBounds();
        return bounds.getHeight() + bounds.getY();
    }
    
    /**
     * reset the current string for the GTR to draw, calculating the boundaries
     * of the string.  For greek and math symbols, unicode characters should be
     * used.  (See www.unicode.org).  See the documentation for this class
     * for a description of symbols.
     * @deprecated use setString( Graphics g, String str ) instead.
     * @param c the component which will provide the graphics.
     * @param str the granny string, such as "E=mc!e2"
     */
    public void setString( Component c, String str ) {
        bounds = null;
        lineBounds = new ArrayList();
        this.str = Entities.decodeEntities(str);
        this.tokens = buildTokenArray(this.str);
        this.draw( c.getGraphics(), c.getFont(), 0f, 0f, false );
    }
    
    /**
     * reset the current string for the GTR to draw, calculating the boundaries
     * of the string.  For greek and math symbols, unicode characters should be
     * used.  (See www.unicode.org).
     *
     * @param g the graphics context which will supply the FontMetrics.
     * @param str the granny string, such as "E=mc!e2"
     */
    public void setString( Graphics g, String str) {
        bounds = null;
        lineBounds = new ArrayList();
        this.str = Entities.decodeEntities(str);
        this.tokens = buildTokenArray(this.str);
        this.draw( g, g.getFont(), 0f, 0f, false );
    }
    
    /**
     * reset the current string for the GTR to draw, calculating the boundaries
     * of the string.  For greek and math symbols, unicode characters should be
     * used.  (See www.unicode.org).
     *
     * @param font the font.  This should be consistent
     *    with the Font used when drawing.
     * @param label the granny string, such as "E=mc!e2"
     */
    public void setString( Font font, String label) {
        bounds = null;
        lineBounds = new ArrayList();
        this.str = Entities.decodeEntities(label);
        this.tokens = buildTokenArray(this.str);
        this.draw( null, font, 0f, 0f, false );
    }
        
    /**
     * returns the current alignment, by default LEFT_ALIGNMENT.
     * @return the current alignment.
     */
    public float getAlignment() {
        return alignment;
    }
    
    /**
     * set the alignment for rendering, one of LEFT_ALIGNMENT  CENTER_ALIGNMENT or RIGHT_ALIGNMENT.
     * @param a the alignment, one of LEFT_ALIGNMENT  CENTER_ALIGNMENT or RIGHT_ALIGNMENT.
     */
    public void setAlignment( float a) {
        if (a != LEFT_ALIGNMENT && a != CENTER_ALIGNMENT && a != RIGHT_ALIGNMENT) {
            throw new IllegalArgumentException("alignment should 0., 0.5, or 1.0");
        }
        alignment = a;
    }
    
    /**
     * draw the current string.  Note the first line will be above iy, and following lines will
     * be below iy.  This is to be consistent with Graphics2D.drawString.
     *
     * @param ig Graphic object to use to render the text.
     * @param ix The x position of the first character of text.
     * @param iy The y position of the baseline of the first line of text.
     */
    public void draw( Graphics ig, float ix, float iy ) {
        this.draw( ig, ig.getFont(), ix, iy, true);
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
     * @param ix The x position of the first character of text.
     * @param iy The y position of the baseline of the first line of text.
     * @param c The <code>Component</code> that the <code>String</code> will be rendered in.
     *    This can be <code>null</code> if draw is <code>true</code>
     * @param draw A <code>boolean</code> flag indicating whether or not the drawing code should be executed.
     * @throws NullPointerException if ig is <code>null</code> AND draw is <code>true</code>.
     * @throws NullPointerException if c is <code>null</code> AND draw is <code>false</code>.
     */
    private void draw(Graphics ig, Font baseFont, float ix, float iy, boolean draw ) {
        Graphics2D g = null;
        Rectangle boundsl = null;
        
        if (draw) {
            g = (Graphics2D)ig.create();
//            RenderingHints hints = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
//                    RenderingHints.VALUE_ANTIALIAS_ON);
//            hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
//            g.setRenderingHints(hints);
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
        
        final class TextPosition {
            public TextPosition(int sub, int ei, float x, float y) {
                this.sub = sub; this.ei = ei; this.x = x; this.y = y; }
            public TextPosition(TextPosition p) {
                copy(p); }
            public void copy(TextPosition p) {
                sub = p.sub; ei = p.ei; x = p.x; y = p.y; }
            public int sub;
            public int ei;
            public float x;
            public float y;
        }
        
        if ( ig==null ) {
            ig= getHeadlessGraphicsContext();
        }
        
        if ( baseFont==null ) {
            baseFont= Font.decode("sans-10");
        }
        
        int lineNum=1;
        
        TextPosition current = new TextPosition(NONE, NONE, ix, iy);
        if (draw) {
            if (alignment == CENTER_ALIGNMENT) {
                current.x += (getWidth() - getLineOneWidth()) / 2.0;
            } else if (alignment == RIGHT_ALIGNMENT) {
                current.x += (getWidth() - getLineOneWidth());
            }
        }
        
        if (!draw) {
            boundsl= new Rectangle((int)ix,(int)iy,0,0);
        }
        
        Stack saveStack = new Stack();
        
        for (String strl : tokens) {
            if ( !strl.equals("!!") && strl.charAt(0) == '!') {
                if ( strl.length()==1 ) break;
                switch (strl.charAt(1)) {
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
                            } else if (alignment == RIGHT_ALIGNMENT) {
                                current.x += (getWidth() - getLineWidth(lineNum));
                            }
                        }
                        saveStack.clear();
                        if (!draw) {
                            lineBounds.add(boundsl);
                            boundsl = new Rectangle((int)current.x, (int)current.y, 0, 0);
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
                        if ( !saveStack.empty() ) {
                            if (saveStack.peek() == null) return;
                            current.copy((TextPosition)saveStack.pop());
                        } else {
                            logger.log(Level.WARNING, "saveStack was empty: missing !s from: {0}", this.str);
                        }
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
            } else {
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
                if ( strl.equals("!!") ) strl= "!";
                if (draw) {
                    g.setFont(font);
                    g.drawString(strl, current.x, y);
                    current.x += g.getFontMetrics(font).stringWidth(strl);
                    //bounds.translate((int)ix,(int)iy);
                    //g.draw(bounds);  //useful for debugging
                    //g.drawLine((int)ix,(int)iy,(int)ix+4,(int)iy);
                } else {
                    FontMetrics fm= ig.getFontMetrics(font);
                    boundsl.add(current.x, y+fm.getDescent());
                    boundsl.add(current.x+fm.stringWidth(strl),y-fm.getAscent() ); // removed -5.0 pixels
                    current.x += ig.getFontMetrics(font).stringWidth(strl);
                }
            }
        } // for (String strl : tokens) {
        if (!draw) {
            lineBounds.add(boundsl);
        }
        if (draw) {
            g.dispose();
        }
    }
    
    private static String[] buildTokenArray(String str) {
        java.util.List<String> vector = new ArrayList();
        int begin;
        int end = 0;
        while(end < str.length()) {
            begin = end;
            if (str.charAt(begin) == '!') {
                end = begin + 2;
                if ( end>=str.length() ) end= str.length();
            } else {
                end = str.indexOf('!', begin);
                if (end == -1) end = str.length();
            }
            vector.add(str.substring(begin, end));
        }

        String[] list= vector.toArray( new String[vector.size()] );
        
        return list;
    }
    
    @Override
    public String toString() {
        maybeInitBounds();
        StringBuilder buffer = new StringBuilder(getClass().getName());
        buffer.append(": ").append(str).append(", ");
        buffer.append("bounds: ").append(bounds).append(", ").append("lineBounds:").append(lineBounds).append(", ");
        return buffer.toString();
    }
    
//    
//    /**
//     * useful for debugging.
//     */
//    private void drawBounds(Graphics g, int ix, int iy) {
//        g.setColor(Color.BLUE);
//        g.drawRect(bounds.x + ix, bounds.y + iy, bounds.width, bounds.height);
//        g.setColor(Color.GREEN);
//        for (java.util.Iterator i = lineBounds.iterator(); i.hasNext();) {
//            Rectangle rc = (Rectangle)i.next();
//            g.drawRect(rc.x + ix, rc.y + iy, rc.width, rc.height);
//        }
//    }

    private static Graphics headlessGraphics=null;
    private static synchronized Graphics getHeadlessGraphicsContext() {
        if ( headlessGraphics==null ) {
            headlessGraphics= new BufferedImage(10,10,BufferedImage.TYPE_INT_ARGB).getGraphics();
        }
        return headlessGraphics;
    }
    
}
