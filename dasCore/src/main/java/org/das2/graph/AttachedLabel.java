/* File: DasAxis.java
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

import org.das2.util.GrannyTextRenderer;
import java.awt.*;

/**
 * A canvas component for labeling things that is positioned just outside of a row,column box.
 *
 * @author eew
 */
public class AttachedLabel extends DasCanvasComponent implements Cloneable {
    
    
    /*
     * PUBLIC CONSTANT DECLARATIONS
     */
    
    /** This value indicates that the axis should be located at the top of its cell */
    public static final int TOP = 1;
    
    /** This value indicates that the axis should be located at the bottom of its cell */
    public static final int BOTTOM = 2;
    
    /** This value indicates that the axis should be located to the left of its cell */
    public static final int LEFT = 3;
    
    /** This value indicateds that the axis should be located to the right of its cell */
    public static final int RIGHT = 4;
    
    /** This value indicates that the axis should be oriented horizontally */
    public static final int HORIZONTAL = BOTTOM;
    
    /** This value indicates that the axis should be oriented vertically */
    public static final int VERTICAL = LEFT;
    
    /* GENERAL LABEL INSTANCE MEMBERS */
    private int orientation;
    protected String axisLabel = "";
    
    /* Rectangles representing different areas of the axis */
    private Rectangle blTitleRect;
    private Rectangle trTitleRect;
    
    private double emOffset;

	private boolean flipLabel = false;
	
	private Font font = null;   // Stays null unless setLabelFont is called
    
    /* DEBUGGING INSTANCE MEMBERS */
    private static final boolean DEBUG_GRAPHICS = false;
    private static final Color[] DEBUG_COLORS;
    static {
        if (DEBUG_GRAPHICS) {
            DEBUG_COLORS = new Color[] {
                Color.BLACK, Color.RED, Color.GREEN, Color.BLUE,
                        Color.GRAY, Color.CYAN, Color.MAGENTA, Color.YELLOW,
            };
        } else {
            DEBUG_COLORS = null;
        }
    }
    private int debugColorIndex = 0;
    
    /** constructs an AttachedLabel.
     * @param label  The granny string to be displayed.
     * @param orientation  identifies the side of the box.  See TOP, BOTTOM, LEFT, RIGHT.
     * @param emOffset The offset from the edge of the box to the label, in "ems"-- the roughly the width of a letter "M," and
     *   more precisely the size of the current font.
     */
    public AttachedLabel( String label, int orientation, double emOffset) {
        super();
        this.orientation = orientation;
        this.emOffset = emOffset;
        this.axisLabel = label;
        setOpaque(false);
    }
    
    /** Sets the side of the row,column box to locate the label.
     * @param orientation should be one of AttachedLabel.TOP, AttachedLabel.BOTTOM, AttachedLabel.LEFT, AttachedLabel.RIGHT
     */
    public void setOrientation(int orientation) {
        boolean oldIsHorizontal = isHorizontal();
        setOrientationInternal(orientation);
    }
    
    /* This is a private internal implementation for
     * {@link #setOrientation(int)}.  This method is provided
     * to avoid calling a non-final non-private instance method
     * from a constructor.  Doing so can create problems if the
     * method is overridden in a subclass.
     */
    private void setOrientationInternal(int orientation) {
        this.orientation = orientation;
    }
    
    /** Mutator method for the title property of this axis.
     *
     * The title for this axis is displayed below the ticks for horizontal axes
     * or to left of the ticks for vertical axes.
     * @param t The new title for this axis
     */
    public void setLabel(String t) {
        if (t == null) throw new NullPointerException("axis label cannot be null");
        Object oldValue = axisLabel;
        axisLabel = t;
        update();
        firePropertyChange("label", oldValue, t);
    }
    
    /**
     * Accessor method for the title property of this axis.
     *
     * @return A String instance that contains the title displayed
     *    for this axis, or <code>null</code> if the axis has no title.
     */
    public String getLabel() {
        return axisLabel;
    }
    
    /**
     * @return the pixel position of the label.
     * @deprecated It's not clear how this should be used, and it does not appear to be used within dasCore and dasApps.
     */
    public final int getDevicePosition() {
        if (orientation == BOTTOM) {
            return getRow().getDMaximum();
        } else if (orientation == TOP) {
            return getRow().getDMinimum();
        } else if (orientation == LEFT) {
            return getColumn().getDMinimum();
        } else {
            return getColumn().getDMaximum();
        }
    }
    
    /**
     * @return returns the length in pixels of the axis.
     */
    public int getDLength() {
        if (isHorizontal())
            return getColumn().getWidth();
        else
            return getRow().getHeight();
    }
    
    /**
     * paints the axis component.  The tickV's and bounds should be calculated at this point
     * @param graphics 
     */
    protected void paintComponent(Graphics graphics) {
        
        /* This was code was keeping axes from being printed on PC's
        Shape saveClip = null;
        if (getCanvas().isPrintingThread()) {
            saveClip = graphics.getClip();
            graphics.setClip(null);
        }
         */
        
        Graphics2D g = (Graphics2D)graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.translate(-getX(), -getY());
        g.setColor(getForeground());
                
        /* Debugging code */
        /* The compiler will optimize it out if DEBUG_GRAPHICS == false */
        if (DEBUG_GRAPHICS) {
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.CAP_BUTT, 1f, new float[]{3f, 3f}, 0f));
            g.setColor(Color.BLUE);
            g.setColor(Color.LIGHT_GRAY);
            if (trTitleRect != null) g.draw(trTitleRect);
            g.setStroke(new BasicStroke(1f));
            g.setColor(DEBUG_COLORS[debugColorIndex]);
            debugColorIndex++;
            if (debugColorIndex >= DEBUG_COLORS.length) { debugColorIndex = 0; };
        }
        /* End debugging code */
        
        if (isHorizontal()) {
            paintHorizontalLabel(g);
        } else {
            paintVerticalLabel(g);
        }
        
        g.dispose();
        getDasMouseInputAdapter().paint(graphics);
    }
    
    /** Paint the axis if it is horizontal  */
    protected void paintHorizontalLabel(Graphics2D g) {
        Rectangle clip = g.getClipBounds();
        if (clip == null) {
            clip = new Rectangle(getX(), getY(), getWidth(), getHeight());
        }
        
        boolean bottomLabel = ((orientation == BOTTOM && !axisLabel.equals("")) && blTitleRect != null && blTitleRect.intersects(clip));
        boolean topLabel = ((orientation == TOP && !axisLabel.equals("")) && trTitleRect != null && trTitleRect.intersects(clip));
        
        int topPosition = getRow().getDMinimum() - 1;
        int bottomPosition = getRow().getDMaximum();
        int DMax= getColumn().getDMaximum();
        int DMin= getColumn().getDMinimum();
        
        Font labelFont = getLabelFont();
        
        int tickLengthMajor = labelFont.getSize() * 2 / 3;
        int tickLengthMinor = tickLengthMajor / 2;
        
        if (!axisLabel.equals("")) {
            Graphics2D g2 = (Graphics2D)g.create();
            int titlePositionOffset = getTitlePositionOffset();
            GrannyTextRenderer gtr = new GrannyTextRenderer();
            gtr.setString(g, axisLabel);
            int titleWidth = (int)gtr.getWidth();
            int baseline;
            int leftEdge;
            g2.setFont(getLabelFont());
            if (bottomLabel) {
                leftEdge = DMin + (DMax-DMin - titleWidth)/2;
                baseline = bottomPosition + titlePositionOffset;
                gtr.draw(g2, (float)leftEdge, (float)baseline);
            }
            if (topLabel) {
                leftEdge = DMin + (DMax-DMin - titleWidth)/2;
                baseline = topPosition - titlePositionOffset;
                gtr.draw(g2, (float)leftEdge, (float)baseline);
            }
            g2.dispose();
        }
    }
    
    /** Paint the axis if it is vertical  */
    protected void paintVerticalLabel(Graphics2D g) {
        Rectangle clip = g.getClipBounds();
        if (clip == null) {
            clip = new Rectangle(getX(), getY(), getWidth(), getHeight());
        }
        
        boolean leftLabel = ((orientation == LEFT && !axisLabel.equals("")) && blTitleRect != null && blTitleRect.intersects(clip));
        boolean rightLabel = ((orientation == RIGHT && !axisLabel.equals("")) && trTitleRect != null && trTitleRect.intersects(clip));
        
        int leftPosition = getColumn().getDMinimum() - 1;
        int rightPosition = getColumn().getDMaximum();
        int DMax= getRow().getDMaximum();
        int DMin= getRow().getDMinimum();
        
        Font labelFont = getLabelFont();
        
        int tickLengthMajor= labelFont.getSize()*2/3;
        int tickLengthMinor = tickLengthMajor / 2;
        
        if (!axisLabel.equals("")) {
            Graphics2D g2 = (Graphics2D)g.create();
            int titlePositionOffset = getTitlePositionOffset();
            GrannyTextRenderer gtr = new GrannyTextRenderer();
            gtr.setString(g, axisLabel);
            int titleWidth = (int)gtr.getWidth();
            int baseline;
            int leftEdge;
            g2.setFont(getLabelFont());
            if (leftLabel) {
                g2.rotate(-Math.PI/2.0);
                leftEdge = -DMax + (DMax-DMin - titleWidth)/2;
                baseline = leftPosition - titlePositionOffset;
                gtr.draw(g2, (float)leftEdge, (float)baseline);
                //leftEdge = DMin + (DMax-DMin - titleWidth)/2;
                //baseline = topPosition - titlePositionOffset;
                //gtr.draw(g2, (float)leftEdge, (float)baseline);
            }
            if (rightLabel) {
				if (flipLabel) {
					g2.rotate(-Math.PI/2.0);
					leftEdge = -DMax + (DMax-DMin - titleWidth)/2;
					baseline = rightPosition + titlePositionOffset
							+ (int)Math.ceil(gtr.getHeight())
							- 2*(int)Math.ceil(gtr.getDescent());
					gtr.draw(g2, (float)leftEdge, (float)baseline);
					//leftEdge = DMin + (DMax-DMin - titleWidth)/2;
					//baseline = bottomPosition + titlePositionOffset;
					//gtr.draw(g2, (float)leftEdge, (float)baseline);
				}
				else {
					g2.rotate(Math.PI/2.0);
					leftEdge = DMin + (DMax-DMin - titleWidth)/2;
					baseline = - rightPosition - titlePositionOffset;
					gtr.draw(g2, (float)leftEdge, (float)baseline);
				}
            }
            g2.dispose();
        }
    }
    
    /** calculates the distance from the box to the label.
     * @return integer distance in pixels.
     */
    protected int getTitlePositionOffset() {
        Font labelFont = getLabelFont();
        
        GrannyTextRenderer gtr = new GrannyTextRenderer();
        gtr.setString(labelFont, axisLabel);
        
        int offset = (int)Math.ceil(emOffset * labelFont.getSize());
        
        if (orientation == BOTTOM) {
            offset += gtr.getAscent();
        }
        return offset;
    }

	public boolean isLabelFlipped() {
		return flipLabel;
	}

	public void setLabelFlipped(boolean flipLabel) {
		this.flipLabel = flipLabel;
		update();
	}
    
    /** get the current font of the component.
     * @return Font of the component.
     */
    public Font getLabelFont() {
		  if(font != null) return font;
        return this.getFont();
    }
    
    /** set the font of the label.
     * @param labelFont Font for the component.  Currently this is ignored.
     */
    public void setLabelFont(Font labelFont) {
		font = labelFont;
		repaint();
    }
    
    /** clones the component
     * @return clone of this.
     */
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new Error("Assertion failure");
        }
    }
    
    /** 
     * revalidate component after resize.
     */
    public void resize() {
        setBounds(getLabelBounds());
        invalidate();
        validate();
    }
    
    /** get the Rectangle precisely enclosing the label.
     * @return Rectangle in canvas space.
     */
    protected Rectangle getLabelBounds() {
        Rectangle bounds;
        if (isHorizontal()) {
            bounds = getHorizontalLabelBounds();
        } else {
            bounds = getVerticalLabelBounds();
        }
        return bounds;
    }
    
    private Rectangle getHorizontalLabelBounds() {
        int topPosition = getRow().getDMinimum() - 1;
        int bottomPosition = getRow().getDMaximum();
        DasDevicePosition range = getColumn();
        int DMax = range.getDMaximum();
        int DMin = range.getDMinimum();
        
        boolean bottomLabel = (orientation == BOTTOM && !axisLabel.equals(""));
        boolean topLabel = (orientation == TOP && !axisLabel.equals(""));
        
        Rectangle bounds;
        
        //Add room for the axis label
        Font labelFont = getLabelFont();
        GrannyTextRenderer gtr = new GrannyTextRenderer();
        gtr.setString(labelFont, getLabel());
        int offset = getTitlePositionOffset();
        if (bottomLabel) {
            int x = 0;
            int y = getRow().getDMaximum() + offset - (int)Math.ceil(gtr.getAscent());
            int width = getCanvas().getWidth();
            int height = (int)Math.ceil(gtr.getHeight());
            blTitleRect = setRectangleBounds(blTitleRect, x, y, width, height);
        }
        if (topLabel) {
            int x = 0;
            int y = getRow().getDMinimum() - offset - (int)Math.ceil(gtr.getAscent());
            int width = getCanvas().getWidth();
            int height = (int)Math.ceil(gtr.getHeight());
            trTitleRect = setRectangleBounds(trTitleRect, x, y, width, height);
        }
        
        bounds = new Rectangle((orientation == BOTTOM) ? blTitleRect : trTitleRect);
        
        return bounds;
    }
    
    private Rectangle getVerticalLabelBounds() {
        boolean leftLabel = (orientation == LEFT && !axisLabel.equals(""));
        boolean rightLabel = (orientation == RIGHT && !axisLabel.equals(""));
        
        int leftPosition = getColumn().getDMinimum() - 1;
        int rightPosition = getColumn().getDMaximum();
        int DMax= getRow().getDMaximum();
        int DMin= getRow().getDMinimum();
        
        Rectangle bounds;
        
        int offset = getTitlePositionOffset();
        
        //Add room for the axis label
        Font labelFont = getLabelFont();
        GrannyTextRenderer gtr = new GrannyTextRenderer();
        gtr.setString(labelFont, getLabel());
        if (leftLabel) {
            int x = getColumn().getDMinimum() - offset - (int)Math.ceil(gtr.getAscent());
            int y = 0;
            int width = (int)Math.ceil(gtr.getHeight());
            int height = getCanvas().getHeight();
            blTitleRect = setRectangleBounds(blTitleRect, x, y, width, height);
        }
        if (rightLabel) {
            int x = getColumn().getDMaximum() + offset - (int)Math.ceil(gtr.getDescent());
            int y = 0;
            int width = (int)Math.ceil(gtr.getHeight());
            int height = getCanvas().getHeight();
            trTitleRect = setRectangleBounds(trTitleRect, x, y, width, height);
        }
        
        bounds = new Rectangle((orientation == LEFT) ? blTitleRect : trTitleRect);
        return bounds;
    }
    
    private static Rectangle setRectangleBounds(Rectangle rc, int x, int y, int width, int height) {
        if (rc == null) {
            return new Rectangle(x, y, width, height);
        } else {
            rc.setBounds(x, y, width, height);
            return rc;
        }
    }
    
    /** return orientation int
     * @return AttachedLabel.TOP, etc.
     */
    public int getOrientation() {
        return orientation;
    }
    
    
    /** true if the label is horizontal
     * @return true if the  label is horizontal
     */
    public boolean isHorizontal() {
        return orientation == BOTTOM || orientation == TOP;
    }
    
    /** return a string for the int orientation encoding.
     * @param i
     * @return "top", "right", etc.
     */
    protected static String orientationToString(int i) {
        switch (i) {
            case TOP:
                return "top";
            case BOTTOM:
                return "bottom";
            case LEFT:
                return "left";
            case RIGHT:
                return "right";
            default: throw new IllegalStateException("invalid orienation: " + i);
        }
    }
    
    /** 
     * @param orientationString left, right, horizontal, etc.
     * @return the int encoding for the orientation parameter,
     */
    protected static int parseOrientationString(String orientationString) {
        if (orientationString.equals("horizontal")) {
            return HORIZONTAL;
        } else if (orientationString.equals("vertical")) {
            return VERTICAL;
        } else if (orientationString.equals("left")) {
            return LEFT;
        } else if (orientationString.equals("right")) {
            return RIGHT;
        } else if (orientationString.equals("top")) {
            return TOP;
        } else if (orientationString.equals("bottom")) {
            return BOTTOM;
        } else {
            throw new IllegalArgumentException("Invalid orientation: " + orientationString);
        }
    }
    
    /** 
     * @return the bounds of the label
     */
    public Shape getActiveRegion() {
        return getLabelBounds();
    }

    /**
     * Getter for property emOffset.
     * @return Value of property emOffset.
     */
    public double getEmOffset() {

        return this.emOffset;
    }

    /**
     * Setter for property emOffset.
     * @param emOffset New value of property emOffset.
     */
    public void setEmOffset(double emOffset) {

        this.emOffset = emOffset;
    }
    
}
