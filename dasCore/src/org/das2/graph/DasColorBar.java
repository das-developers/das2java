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

/**
 *
 * @author  jbf
 */
public class DasColorBar extends DasAxis {
    
    private static final long serialVersionUID = 1L;

    public static final String PROPERTY_TYPE = "type";
    public static final String PROPERTY_FILL_COLOR= "fillColor";
    
    private BufferedImage image;
    private transient DasColorBar.Type type;
    private static int fillColor= Color.LIGHT_GRAY.getRGB();
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
    
    
    public final void setType(DasColorBar.Type type) {
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
    
    @Override
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
    
    @Override
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
    
    
    @Override
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
        
        @Override
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
      public static final Type APL_RAINBOW_BLACK0 = new Type("apl_rainbow_black0");
      public static final Type APL_RAINBOW_WHITE0 = new Type("apl_rainbow_white0");
      public static final Type GSFC_RP_SPECIAL = new Type("gsfc_rp_special");
      
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

      @Override
        public void drawListIcon( Graphics2D g, int x, int y ) {
            ImageIcon licon= (ImageIcon) getListIcon();
            g.drawImage(licon.getImage(), x, y, null);
        }

      @Override
        public javax.swing.Icon getListIcon() {
            maybeInitializeIcon();
            return icon;
        }
        
        public void maybeInitializeIcon() {
            if (icon == null) {
                icon = new javax.swing.ImageIcon(getVerticalScaledImage(16, 16));
            }
        }
        
      @Override
        public String toString() {
            return desc;
        }
        
      @Override
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
                int comp= ( i - bottom ) * 255 / ( top - bottom );
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

            colorTable[ncolor-1]= fillColor;
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
            } else if (this == APL_RAINBOW_WHITE0) {
                initializeColorWedgeWhite(size, bottom, top);
            } else if (this == APL_RAINBOW_BLACK0) {
                initializeColorWedgeBlack(size, bottom, top);
            } else if (this == GSFC_RP_SPECIAL ) {
                initializeRPSpecial(size, bottom, top);
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

        
        private void initializeColorWedgeWhite( int size, int bottom, int top ) {
         ColorWedgeColorSource ct = new ColorWedgeColorSource(true);
            int[] index = ct.getIndex();
            int[] red =  ct.getRed();
            int[] green = ct.getGreen();
            int[] blue =  ct.getBlue();
            colorTable = makeColorTable( index, red, green, blue, size, bottom, top );
            //colorTable[0] = ( colorTable[0] & 0xFFFFFF00 ) | 1;
        }
        
        private void initializeColorWedgeBlack( int size, int bottom, int top ) {
         ColorWedgeColorSource ct = new ColorWedgeColorSource(false);
            int[] index = ct.getIndex();
            int[] red =  ct.getRed();
            int[] green = ct.getGreen();
            int[] blue =  ct.getBlue();
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
        
        private void initializeRPSpecial( int size, int bottom, int top ) {
            int [] index= { 0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89,90,91,92,93,94,95,96,97,98,99,100,101,102,103,104,105,106,107,108,109,110,111,112,113,114,115,116,117,118,119,120,121,122,123,124,125,126,127,128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143,144,145,146,147,148,149,150,151,152,153,154,155,156,157,158,159,160,161,162,163,164,165,166,167,168,169,170,171,172,173,174,175,176,177,178,179,180,181,182,183,184,185,186,187,188,189,190,191,192,193,194,195,196,197,198,199,200,201,202,203,204,205,206,207,208,209,210,211,212,213,214,215,216,217,218,219,220,221,222,223,224,225,226,227,228,229,230,231,232,233,234,235,236,237,238,239,240,241,242,243,244,245,246,247,248,249,250,251,252,253,254,255 };
            int [] red= { 64,64,64,63,62,62,62,62,62,62,61,61,61,61,61,61,61,61,61,61,62,62,62,62,62,63,63,63,63,63,64,64,64,64,65,65,65,65,65,66,66,66,66,66,67,67,67,67,68,68,68,68,69,69,69,69,69,69,70,70,70,70,70,71,71,71,71,72,72,73,73,74,74,75,76,76,77,78,78,79,79,80,80,81,81,82,82,83,83,84,84,85,85,86,86,88,89,91,93,95,96,98,99,100,101,103,104,106,107,109,110,111,113,114,114,114,114,114,115,115,115,115,116,116,116,116,116,116,116,116,117,118,120,121,122,124,125,127,128,129,131,133,137,141,145,149,153,156,159,161,164,166,171,177,183,189,195,201,207,211,215,218,222,225,229,233,237,241,245,249,250,250,251,251,252,252,253,253,254,254,254,255,255,255,255,255,254,254,254,253,253,253,253,252,252,252,251,250,249,249,248,247,246,245,245,244,243,242,242,241,240,239,238,238,237,236,235,235,234,233,232,232,231,230,229,228,227,226,225,224,223,222,221,219,218,217,215,214,212,210,208,206,204,202,200,200,200,200,200,200,200,200,200,200,200,200 };
            int [] green= { 85,85,85,83,82,82,82,81,81,81,81,81,81,81,81,81,81,81,81,82,83,83,84,85,86,87,89,90,91,92,93,94,95,96,97,98,99,100,101,102,103,104,105,106,108,109,110,111,112,113,114,115,116,117,118,119,120,120,120,121,121,122,122,123,124,124,125,126,127,128,130,131,133,134,136,137,139,140,142,143,145,146,147,149,151,153,155,158,160,162,164,166,169,171,173,177,180,183,187,190,194,195,196,197,198,199,201,203,205,207,210,212,213,214,215,216,217,218,220,221,223,225,227,229,229,230,231,232,233,234,235,236,238,239,241,242,243,243,244,244,245,246,247,248,249,250,250,251,251,252,252,252,253,253,253,254,254,254,255,255,255,255,255,255,253,252,251,250,249,248,247,246,244,243,242,240,238,236,234,231,229,227,226,225,224,223,221,218,215,212,209,206,203,200,198,196,192,189,185,182,178,175,171,168,164,159,156,152,149,145,141,138,134,130,126,122,118,114,110,107,103,99,95,92,89,85,82,79,76,72,69,66,63,59,55,51,47,42,40,38,36,35,33,31,30,30,30,30,30,30,30,30,30,30,30,30 };
            int [] blue= { 141,141,141,138,136,136,136,135,135,135,135,135,135,135,135,135,134,134,134,135,135,136,137,138,138,139,140,141,142,143,144,145,146,147,148,149,150,151,152,153,154,155,156,157,158,159,160,161,162,163,164,165,166,167,168,169,169,170,170,171,171,171,171,172,172,173,173,173,174,174,175,176,177,178,178,179,180,181,182,182,183,184,184,185,185,186,187,187,188,188,189,189,190,190,191,191,192,192,192,193,194,193,193,193,192,192,192,192,191,189,188,187,186,185,184,183,182,182,180,177,174,171,168,165,161,159,157,155,153,151,147,143,139,135,131,127,124,123,121,119,117,115,112,110,107,104,101,99,98,97,96,95,94,92,90,89,87,85,83,82,81,81,80,79,78,76,75,73,72,71,70,69,68,68,67,67,66,64,63,62,61,60,59,58,58,57,56,55,53,52,50,49,47,46,45,44,43,42,40,39,38,37,36,34,33,32,31,30,29,28,27,26,25,25,24,23,22,21,20,20,19,18,17,16,16,15,15,15,14,14,13,13,12,12,11,10,9,9,8,8,8,7,7,7,6,6,6,6,6,6,6,6,6,6,6,6 };
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
            } else if (s.equals("apl_rainbow_black0")) {
                return APL_RAINBOW_BLACK0;
            } else if (s.equals("apl_rainbow_white0")) {
                return APL_RAINBOW_WHITE0;
            } else if (s.equals("gsfc_rp_special")) {
                return GSFC_RP_SPECIAL;
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
                
        @Override
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
            
            // DatumRange dr;
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
        
        @Override
        public void mouseReleased( @SuppressWarnings("unused") MouseEvent e ) {
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
        
        @Override
        public void mousePointSelected(MousePointSelectionEvent e) {
            setColorBar( e.getY() );
        }
        
        @Override
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
