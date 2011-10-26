/* File: DasColorBar.java
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
import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import org.das2.datum.Datum;
import org.das2.datum.DatumRangeUtil;
import org.das2.components.propertyeditor.Enumeration;
import org.das2.event.DataRangeSelectionEvent;
import org.das2.event.HorizontalSliceSelectionRenderer;
import org.das2.event.MouseModule;
import org.das2.event.MousePointSelectionEvent;
import java.awt.image.IndexColorModel;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import javax.swing.ImageIcon;
import javax.swing.event.EventListenerList;

/**
 *
 * @author  jbf
 */
public class DasColorBar extends DasAxis {
    
    public static final String PROPERTY_TYPE = "type";
    public static final String PROPERTY_FILL_COLOR= "fillColor";
    
    private BufferedImage image;
    private DasColorBar.Type type;
    private static int sFillColor= Color.LIGHT_GRAY.getRGB();
    private int fillColor= Color.LIGHT_GRAY.getRGB();
    private int fillColorIndex;
    private int ncolor;
    
    private static final int COLORTABLE_SIZE=240;
    
    public DasColorBar( Datum min, Datum max, boolean isLog) {
        this(min, max, RIGHT, isLog);
    }
    
    public DasColorBar( Datum min, Datum max, int orientation, boolean isLog) {
        super(min, max, orientation, isLog);
        setLayout(new ColorBarLayoutManager());
        setType(DasColorBar.Type.COLOR_WEDGE);
    }
    
    
    public int rgbTransform(double x, Units units) {
        int icolor= (int)transform(x,units,0, ncolor);
        
        if ( units.isFill(x) ) {
            return fillColor;
        } else {
            icolor= (icolor<0)?0:icolor;
            icolor= (icolor>=ncolor)?(ncolor-1):icolor;
            return type.getRGB(icolor);
        }
    }
    
    public int indexColorTransform( double x, Units units ) {
        if ( units.isFill(x) ) {
            return fillColorIndex;
        } else {
            int icolor= (int)transform(x,units,0,ncolor);
            icolor= (icolor<0)?0:icolor;
            icolor= (icolor>=ncolor)?(ncolor-1):icolor;
            return icolor;
        }
    }
    
    public IndexColorModel getIndexColorModel() {
        return new IndexColorModel( 8, type.getColorCount()+1, type.colorTable, 0, true, -1, DataBuffer.TYPE_BYTE );
    }
    
    public int getFillColorIndex() {
        return fillColorIndex;
    }
    
    public DasColorBar.Type getType() {
        return type;
    }
    
    
    public void setType(DasColorBar.Type type) {
        if (this.type == type) {
            return;
        }
        DasColorBar.Type oldValue = this.type;
        this.type = type;
        this.ncolor= type.getColorCount();
        image = null;
        fillColorIndex= getType().getColorCount();
        fillColor= getType().getRGB(fillColorIndex);
        markDirty();
        update();
        firePropertyChange( PROPERTY_TYPE, oldValue,type);
    }
    
    protected void paintComponent(Graphics g) {

        if (getCanvas().isValueAdjusting()) {
            return;
        }
        
        int x = getColumn().getDMinimum();
        int y = getRow().getDMinimum();
        int width = getColumn().getDMaximum() - x;
        int height = getRow().getDMaximum() - y;
        //if (image == null || image.getWidth() != width || image.getHeight() != height) {
        if (isHorizontal()) {
            image = type.getHorizontalScaledImage(width, height);
        } else {
            image = type.getVerticalScaledImage(width, height);
        }
        //}
        g.translate(-getX(), -getY());
        if (!isHorizontal()) {
            y++;
        }
        g.drawImage(image, x, y, this);
        g.translate(getX(), getY());
        super.paintComponent(g);
    }
    
    protected Rectangle getAxisBounds() {
        int x = getColumn().getDMinimum();
        int y = getRow().getDMinimum();
        int width = getColumn().getDMaximum() - x;
        int height = getRow().getDMaximum() - y;
        Rectangle rc = new Rectangle(x, y, width, height);
        Rectangle bounds = super.getAxisBounds();
        bounds.add(rc);
        return bounds;
    }
    
    public static DasColumn getColorBarColumn(DasColumn column) {
        return new DasColumn( null, column, 1.0, 1.0, 1, 2, 0, 0  );
    }
    
    
    public Shape getActiveRegion() {
        int x = getColumn().getDMinimum();
        int y = getRow().getDMinimum();
        int width = getColumn().getWidth();
        int height = getRow().getHeight();
        Rectangle bounds = primaryInputPanel.getBounds();
        bounds.translate(getX(), getY());
        Rectangle middleBounds = new Rectangle(x, y, width, height);
        bounds.add(middleBounds);
        if (isOppositeAxisVisible()) {
            Rectangle secondaryBounds = secondaryInputPanel.getBounds();
            secondaryBounds.translate(getX(), getY());
            bounds.add(secondaryBounds);
        }
        return bounds;
    }
    
    protected class ColorBarLayoutManager extends AxisLayoutManager {
        
        public void layoutContainer(Container parent) {
            super.layoutContainer(parent);
            int x = getColumn().getDMinimum();
            int y = getRow().getDMinimum();
            int width = getColumn().getWidth();
            int height = getRow().getHeight();
            Rectangle rc = new Rectangle(x - getX(), y - getY(), width, height);
            Rectangle bounds = primaryInputPanel.getBounds();
            bounds.add(rc);
            primaryInputPanel.setBounds(bounds);
        }
        
    }
    
    public static final class Type implements Enumeration, Displayable {
        
        public static final Type COLOR_WEDGE = new Type("color_wedge");
        //public static final Type BLUE_TO_ORANGE = new Type("blue_to_orange");
        public static final Type GRAYSCALE = new Type("grayscale");
        public static final Type INVERSE_GRAYSCALE = new Type("inverse_grayscale");
        public static final Type WRAPPED_COLOR_WEDGE = new Type("wrapped_color_wedge");
        
        public static final Type BLUE_BLACK_RED_WEDGE = new Type("blue_black_red");
        public static final Type BLUE_WHITE_RED_WEDGE = new Type("blue_white_red");

        private BufferedImage image;
        private int[] colorTable;
        private final String desc;
        private javax.swing.Icon icon;
        
        private Type(String desc) {
            this.desc = desc;
        }

        public void drawListIcon( Graphics2D g, int x, int y ) {
            ImageIcon icon= (ImageIcon) getListIcon();
            g.drawImage(icon.getImage(), x, y, null);
        }

        public javax.swing.Icon getListIcon() {
            maybeInitializeIcon();
            return icon;
        }
        
        public void maybeInitializeIcon() {
            if (icon == null) {
                icon = new javax.swing.ImageIcon(getVerticalScaledImage(16, 16));
            }
        }
        
        public String toString() {
            return desc;
        }
        
        public String getListLabel() {
            return desc;
        }
        
        /* It's understood that the colors indeces from 0 to getColorCount()-1 are the color wedge, and getColorCount() is the fill color. */
        public int getColorCount() {
            maybeInitializeColorTable();
            return colorTable.length-1;
        }
        
        public int getRGB(int index) {
            maybeInitializeColorTable();
            return colorTable[index];
        }
        
        public BufferedImage getHorizontalScaledImage(int width, int height) {
            maybeInitializeImage();
            BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            AffineTransform at = new AffineTransform();
            at.scale((double)width / (double)getColorCount(), (double)height);
            at.rotate(-Math.PI/2.0);
            at.translate(-1.0, 0.0);
            AffineTransformOp op = new AffineTransformOp(at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
            op.filter(image, scaled);
            return scaled;
        }
        
        public BufferedImage getVerticalScaledImage(int width, int height) {
            maybeInitializeImage();
            BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            AffineTransform at = new AffineTransform();
            at.scale((double)width, -(double)height / (double)getColorCount());
            at.translate(0.0, -(double)getColorCount());
            AffineTransformOp op = new AffineTransformOp(at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
            op.filter(image, scaled);
            return scaled;
        }
        
        private void maybeInitializeImage() {
            if (image == null) {
                maybeInitializeColorTable();
                image = new BufferedImage(1, getColorCount(), BufferedImage.TYPE_INT_RGB);
                image.setRGB(0, 0, 1, getColorCount(), colorTable, 0, 1);
            }
        }
        
        // returns a color table with interpolated colors for the wedge from 0 to ncolor-1, and at ncolor, the fill color.
        private static int[] makeColorTable( int [] index, int[] red, int[] green, int[] blue, int ncolor, int bottom, int top ) {
            // index should go from 0-255.
            // truncate when ncolor>COLORTABLE_SIZE
            int[] colorTable = new int[ncolor];
            
            int ii= 0;
            for (int i = 0; i < ncolor-1; i++) {
                float comp= ( i - bottom ) * 255.f / ( top - bottom );
                if ( comp > index[ii + 1]) {
                    ii++;
                }
                
                double a;
                if ( ii>=(index.length-1) ) {
                    a= 1;
                    ii= index.length-2;
                } else {
                    a= (comp-index[ii]) / (double)(index[ii+1]-index[ii]);
                }
                if ( a>1. ) a=1.;
                if ( a<0. ) a=0.;
                double rr= (red[ii]*(1-a) + red[ii+1]*a)/(double)255.;
                double gg= (green[ii]*(1-a) + green[ii+1]*a)/(double)255.;
                double bb= (blue[ii]*(1-a) + blue[ii+1]*a)/(double)255.;
                colorTable[i]= new Color((float)rr,(float)gg,(float)bb).getRGB();
            }

            colorTable[ncolor-1]= sFillColor; // probably not used
            return colorTable;
        }
        
        private void maybeInitializeColorTable() {
            if (colorTable == null) {
                initializeColorTable(COLORTABLE_SIZE,0,COLORTABLE_SIZE);
            }
        }
        
        private void initializeColorTable( int size, int bottom, int top ) {
            if (this == COLOR_WEDGE) {
                initializeColorWedge(size, bottom, top);
            } else if (this == GRAYSCALE) {
                initializeGrayScale(size, bottom, top);
            } else if (this == INVERSE_GRAYSCALE) {
                initializeInverseGrayScale(size, bottom, top);
            } else if (this == WRAPPED_COLOR_WEDGE) {
                initializeWrappedColorWedge(size, bottom, top);
            } else if (this == BLUE_BLACK_RED_WEDGE) {
                initializeBlueBlackRedWedge(size, bottom, top);
            } else if (this == BLUE_WHITE_RED_WEDGE) {
                initializeBlueWhiteRedWedge(size, bottom, top);
            //} else if (this == BLUE_TO_ORANGE ) {
            //    initializeBlueToOrange(size, bottom, top);
            }
        }
        
        private void initializeColorWedge( int size, int bottom, int top ) {
            int[] index = {   0,   30,   63, 126, 162, 192, 221, 255 };
            int[] red =   {   0,    0,    0,   0, 255, 255, 255, 255 };
            int[] green = {   0,    0,  255, 255, 255, 185,  84, 0 };
            int[] blue =  { 137,  255,  255,   0,   0,   0,   0, 0 };
            colorTable = makeColorTable( index, red, green, blue, size, bottom, top );
            colorTable[0] = ( colorTable[0] & 0xFFFFFF00 ) | 1;
        }
        
        private void initializeBlueBlackRedWedge( int size, int bottom, int top ) {
            int[] index = {   0,   64,   128, 192, 255 };
            int[] red =   {   0,    0,     0, 128, 255 };
            int[] green = {   0,    0,     0,   0,   0 };
            int[] blue =  { 255,  128,     0,   0,   0 };
            colorTable = makeColorTable( index, red, green, blue, size, bottom, top );
        }

        private void initializeBlueWhiteRedWedge( int size, int bottom, int top ) {
            int[] index = {     0,   64,   128, 192, 255 };
            int[] red =   {     0,  128,   255, 255, 255 };
            int[] green = {     0,  128,   255, 128,   0 };
            int[] blue =  {   255,  255,   255, 128,   0 };
            colorTable = makeColorTable( index, red, green, blue, size, bottom, top );
        }

        private void initializeBlueToOrange( int size, int bottom, int top ) {
            // cat | awk '{ print $3 "," }'  | xargs
            int[] index = {   0, 23, 46, 69, 92, 115, 139, 162, 185, 208, 231, 255 };
            int[] red =   {   0, 25, 50, 101, 153, 204, 255, 255, 255, 255, 255, 255 };
            int[] green = {   42, 101, 153, 204, 237, 255, 255, 238, 204, 153, 102, 42 };
            int[] blue =  { 255, 255, 255, 255, 255, 255, 204, 153, 101, 50, 25, 0 };
            colorTable = makeColorTable( index, red, green, blue, size, bottom, top );
            colorTable[0] = ( colorTable[0] & 0xFFFFFF00 ) | 1;
        }
        
        private void initializeWrappedColorWedge( int size, int bottom, int top ) {
            int[] index = {   0,   32,   64,  96, 128, 160, 192, 224, 255, };
            int[] red =   { 225,    0,    0,   0, 255, 255, 255, 255, 255, };
            int[] green = {   0,    0,  255, 255, 255, 185,  84,   0,   0, };
            int[] blue =  { 225,  255,  255,   0,   0,   0,   0,   0, 255, };
            colorTable = makeColorTable( index, red, green, blue, size, bottom, top );
        }
        
        private void initializeInverseGrayScale( int size, int bottom, int top ) {
            int [] index= { 0, 255 };
            int [] red= { 0, 255 };
            int [] green= { 0, 255 };
            int [] blue= { 0, 255 };
            colorTable = makeColorTable( index, red, green, blue, size, bottom, top );
        }
        
        private void initializeGrayScale( int size, int bottom, int top ) {
            int [] index= { 0, 255 };
            int [] red= { 255, 0 };
            int [] green= { 255, 0 };
            int [] blue= { 255, 0 };
            colorTable = makeColorTable( index, red, green, blue, size, bottom, top );
        }
        
        public static Type parse(String s) {
            if (s.equals("color_wedge")) {
                return COLOR_WEDGE;
            } else if (s.equals("grayscale")) {
                return GRAYSCALE;
            } else if (s.equals("inverse_grayscale")) {
                return INVERSE_GRAYSCALE;
            } else if (s.equals("blue_black_red")) {
                return BLUE_BLACK_RED_WEDGE;
            } else if (s.equals("blue_white_red")) {
                return BLUE_WHITE_RED_WEDGE;
            //} else if (s.equals("blue_to_orange")) {
            //    return BLUE_TO_ORANGE;                
            } else {
                throw new IllegalArgumentException("invalid DasColorBar.Type string: " + s);
            }
        }
        
    }
    
    public MouseModule getRepaletteMouseModule( Renderer r ) {
        return new ColorBarRepaletteMouseModule( r, this );
    }
    
    public class ColorBarRepaletteMouseModule extends MouseModule {
        
        DasColorBar colorBar;
        Renderer parent;
        DatumRange range0;
        int lastTopColor;
        int lastBottomColor;
        
        boolean animated0;
        int state;
        int STATE_IGNORE=300;
        int STATE_TOP=200;
        int STATE_BOTTOM=100;
        
        /** Utility field used by event firing mechanism. */
        private EventListenerList listenerList =  null;
        
        public String getLabel() { return "Repalette"; };
        
        public ColorBarRepaletteMouseModule( Renderer parent, DasColorBar colorBar ) {
            if (colorBar.isHorizontal()) {
                throw new IllegalArgumentException("Axis orientation is not vertical");
            }
            if ( parent==null ) {
                throw new IllegalArgumentException("Parent is null");
            }
            this.parent= parent;
            //  this.dragRenderer= (DragRenderer)HorizontalRangeRenderer.renderer;
            this.dragRenderer= new HorizontalSliceSelectionRenderer(parent.getParent());
            this.colorBar= colorBar;
        }
        
        private void setColorBar( int y ) {
            
            int bottomColor, topColor;
            
            DatumRange dr;
            DasRow row= colorBar.getRow();
            
            double alpha=  ( row.getDMaximum() - y ) / (1.*row.getHeight());
            
            if ( state==STATE_TOP ) {
                topColor= (int)( COLORTABLE_SIZE * alpha );
                topColor= Math.max( COLORTABLE_SIZE / 20 + 1, topColor );
                bottomColor= 0;
            } else if ( state==STATE_BOTTOM ) {
                topColor= COLORTABLE_SIZE;
                bottomColor= (int)( COLORTABLE_SIZE * alpha );
                bottomColor= Math.min( COLORTABLE_SIZE * 19 / 20, bottomColor );
            } else {
                return;
            }
            
            System.err.println( ""+bottomColor + " "+topColor );
            lastTopColor= topColor;
            lastBottomColor= bottomColor;
            
            colorBar.type.initializeColorTable( COLORTABLE_SIZE, bottomColor, lastTopColor );
            
            colorBar.image= null;
            colorBar.type.image= null;
            colorBar.repaint();
            parent.refreshImage();
        }
        
        public void mouseReleased( MouseEvent e ) {
            if ( state!=STATE_IGNORE ) {
                colorBar.setAnimated(animated0);
                int lastTopColor= this.lastTopColor;
                
                DatumRange dr;
                double a0= lastBottomColor / ( 1.*COLORTABLE_SIZE );
                double a1= lastTopColor / ( 1.*COLORTABLE_SIZE);
                
                if ( isLog() ) {
                    dr= DatumRangeUtil.rescaleLog( range0, a0, a1 );
                } else {
                    dr= DatumRangeUtil.rescale( range0, a0, a1);
                }
                
                colorBar.setDatumRange( dr );
                
                colorBar.type.initializeColorTable( COLORTABLE_SIZE, 0, COLORTABLE_SIZE );
                
                colorBar.image= null;
                colorBar.type.image= null;
                colorBar.repaint();
                parent.refreshImage();
                
            }
        }
        
        public void mousePointSelected(MousePointSelectionEvent e) {
            setColorBar( e.getY() );
        }
        
        public void mousePressed(java.awt.event.MouseEvent e) {
            super.mousePressed(e);
            if ( DasColorBar.this.getColumn().contains(e.getX()+DasColorBar.this.getX()) ) {
                if ( e.getY() + DasColorBar.this.getY() > DasColorBar.this.getRow().getDMiddle() ) {
                    state= STATE_BOTTOM;
                } else {
                    state= STATE_TOP;
                }
                animated0= colorBar.isAnimated();
                colorBar.setAnimated(false);
                range0= colorBar.getDatumRange();
            } else {
                state= STATE_IGNORE;
            }
        }
        
    }

    /**
     * Getter for property fillColor.
     * @return Value of property fillColor.
     */
    public Color getFillColor() {
        return new Color( this.fillColor, true );
    }

    /**
     * Setter for property fillColor.
     * @param fillColor New value of property fillColor.
     */
    public void setFillColor(Color fillColor) {
        Color oldColor= new Color( this.fillColor );
        this.fillColor = fillColor.getRGB();
        this.type.initializeColorTable( COLORTABLE_SIZE, 0, this.type.getColorCount() );
        markDirty();
        update();
        firePropertyChange( PROPERTY_FILL_COLOR, oldColor,fillColor );
    }

    @Override
    public boolean isVisible() {
        return super.isVisible();
    }
    
    @Override
    public void setVisible( boolean vi ) {
        super.setVisible(vi);
    }

}
