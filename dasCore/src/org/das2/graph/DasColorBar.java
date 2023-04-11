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

import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import org.das2.components.propertyeditor.Displayable;
import org.das2.datum.Units;
import org.das2.datum.Datum;
import org.das2.components.propertyeditor.Enumeration;
import java.awt.image.IndexColorModel;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import org.das2.datum.DatumRange;
import org.das2.qds.QDataSet;
import org.das2.qds.ops.Ops;

/**
 * Axis that converts to RGB color instead of horizontal or vertical
 * position.
 * @author  jbf
 */
public class DasColorBar extends DasAxis {
    
    private static final long serialVersionUID = 1L;

    /**
     * handle for the property "type".
     */
    public static final String PROPERTY_TYPE = "type";

    /**
     * handle for the property fillColor.
     */
    public static final String PROPERTY_FILL_COLOR= "fillColor";
    
    private BufferedImage image;
    private transient DasColorBar.Type type;
    private static int fillColor= Color.LIGHT_GRAY.getRGB();
    private int fillColorIndex;
    private int ncolor;
    
    /**
     * number of colors in each table, not including the grey for fill.
     */
    private static final int COLORTABLE_SIZE=240;
    
    /**
     * Create an color bar object, relating data and color.
     * @param min the minimum value
     * @param max the maximum value
     * @param log if true then the axis is a log axis.
     */
    public DasColorBar( Datum min, Datum max, boolean log) {
        this(min, max, RIGHT, log);
    }
    
    /**
     * Create an color bar object, relating data and color.
     * @param min the minimum value
     * @param max the maximum value
     * @param orientation the position relative to a plot, one of DasAxis.TOP, DasAxis.BOTTOM, DasAxis.LEFT, DasAxis.RIGHT.
     * @param log if true then the axis is a log axis.
     */
    public DasColorBar( Datum min, Datum max, int orientation, boolean log) {
        super(min, max, orientation, log);
        setLayout(new ColorBarLayoutManager());
        setType(DasColorBar.Type.COLOR_WEDGE);
    }
    
    Map<String,Color> theSpecialColors= Collections.emptyMap();
    
    private String specialColors = "";

    public static final String PROP_SPECIALCOLORS = "specialColors";

    public String getSpecialColors() {
        return specialColors;
    }

    /**
     * set this to a comma-delineated list of name:value pairs,
     * @param specialColors 
     */
    public void setSpecialColors(String specialColors) {
        String oldSpecialColors = this.specialColors;
        this.specialColors = specialColors;
        Map<String,Color> sc2= new LinkedHashMap<>();
        String[] ss= specialColors.split(",",-2);
        for ( String s: ss ) {
            String[] dc= s.split(":",-2);
            if ( dc.length>1 ) {
                sc2.put(dc[0],org.das2.util.ColorUtil.decodeColor(dc[1]));
            }
        }
        if ( specialColors.trim().length()==0 ) {
            theSpecialColors= null;
        } else {
            theSpecialColors= sc2;
        }
        firePropertyChange(PROP_SPECIALCOLORS, oldSpecialColors, specialColors);
    }        

    /**
     * convert the double to an RGB color.
     * @param data a data value
     * @param units the units of the given data value.
     * @return the combined RGB components
     * @see Color#Color(int) 
     */
    public int rgbTransform(double data, Units units) {

        int icolor= (int)transform(data,units,0, ncolor);
        
        if ( theSpecialColors!=null ) {
            Datum datum= units.createDatum(data);
            boolean haveRgbColor= false;
            for ( Map.Entry<String,Color> e: theSpecialColors.entrySet() ) {
                String k= e.getKey();
                double rgb= e.getValue().getRGB();
                try {
                    if ( k.startsWith("within") ) {
                        String s= k.substring(7,k.length()-1);
                        DatumRange dr= Ops.datumRange(s.replaceAll("\\+"," "));
                        if ( dr.contains( datum ) ) {
                            icolor= (int)rgb;
                            haveRgbColor= true;
                        }
                    } else if ( k.startsWith("without") ) {
                        String s= k.substring(7,k.length()-1);                        
                        DatumRange dr= Ops.datumRange(s.replaceAll("\\+"," "));
                        if ( !dr.contains( datum ) ) {
                            icolor= (int)rgb;
                            haveRgbColor= true;
                        }
                    } else if ( k.startsWith("lt") ) {
                        if ( datum.lt( units.parse(k.substring(3,k.length()-1)) ) ) {
                            icolor= (int)rgb;
                            haveRgbColor= true;
                        }
                    } else if ( k.startsWith("gt") ) {
                        if ( datum.gt( units.parse(k.substring(3,k.length()-1)) ) ) {
                            icolor= (int)rgb;
                            haveRgbColor= true;
                        }
                    } else if ( k.startsWith("eq") ) {
                        if ( datum.equals(units.parse(k.substring(3,k.length()-1)) ) ) {
                            icolor= (int)rgb;
                            haveRgbColor= true;
                        }
                    } else {
                        if ( datum.equals(units.parse(k)) ) {
                            icolor= (int)rgb;
                            haveRgbColor= true;
                        }
                    }
                } catch ( ParseException | NumberFormatException ex ) {
                    System.err.println("error in specialColors: "+k);
                    //ex.printStackTrace();
                }
            }
            if ( haveRgbColor ) return icolor;
        }
                    
        if ( units.isFill(data) ) {
            return fillColor;
        } else {
            icolor= (icolor<0)?0:icolor;
            icolor= (icolor>=ncolor)?(ncolor-1):icolor;
            return type.getRGB(icolor);
        }
    }
    
    /**
     * convert the double to an indexed color.
     * @param data a data value
     * @param units the units of the given data value.
     * @return the index into the color table.
     * @see #getIndexColorModel() 
     */
    public int indexColorTransform( double data, Units units ) {
        if ( units.isFill(data) ) {
            return fillColorIndex;
        } else {
            int icolor= (int)transform(data,units,0,ncolor);
            icolor= (icolor<0)?0:icolor;
            icolor= (icolor>=ncolor)?(ncolor-1):icolor;
            return icolor;
        }
    }
    
    /**
     * return the color model so that indexed color model can be used.
     * @return the color model 
     */
    public IndexColorModel getIndexColorModel() {
        return new IndexColorModel( 8, type.getColorCount()+1, type.colorTable, 0, true, -1, DataBuffer.TYPE_BYTE );
    }
    
    /**
     * return the index of the fill color in the indexed color model.
     * @return the index of the fill color
     */
    public int getFillColorIndex() {
        return fillColorIndex;
    }
    
    /**
     * return the type of colorbar (e.g. DasColorBar.Type.GRAYSCALE or DasColorBar.Type.APL_RAINBOW_BLACK0)
     * @return the type of colorbar
     */
    public DasColorBar.Type getType() {
        return type;
    }
    
    /**
     * set the type of colorbar
     * @param type type of colorbar (e.g. DasColorBar.Type.GRAYSCALE or DasColorBar.Type.APL_RAINBOW_BLACK0)
     */
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
        markDirty("type");
        update();
        firePropertyChange( PROPERTY_TYPE, oldValue,type);
    }
    
    @Override
    protected void paintComponent(Graphics g) {

        if (getCanvas().isValueAdjusting()) {
            return;
        }
        
        if ( showColorBar ) {
            int x = getColumn().getDMinimum();
            int y = getRow().getDMinimum();
            int width = getColumn().getDMaximum() - x;
            int height = getRow().getDMaximum() - y;
            //if (image == null || image.getWidth() != width || image.getHeight() != height) {
            if (isHorizontal()) {
                image = type.getHorizontalScaledImage(width, height);
            } else {
                image = type.getVerticalScaledImage(width, height+1);
            }
            //}
            g.translate(-getX(), -getY());
            try {
                g.drawImage(image, x, y, this);
            } catch ( ClassCastException ex ) {
                //bug rte_1917581137, where x2go/Mate don't get along.
                System.err.println("rte_1917581137: "+ex.getMessage());
            }
            g.translate(getX(), getY());
        }
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
    
    /**
     * return a column suitable for the colorbar, based on the spectrogram
     * column.
     * @param column the column for the spectrogram described.
     * @return the new column.
     */
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

    private boolean showColorBar = true;

    public static final String PROP_SHOWCOLORBAR = "showColorBar";

    public boolean isShowColorBar() {
        return showColorBar;
    }

    /**
     * when set to false, this is basically an ordinary axis.  Autoplot uses
     * this to support StackedHistogram mode.
     * @param showColorBar true if the colorbar should be drawn.
     */
    public void setShowColorBar(boolean showColorBar) {
        boolean oldShowColorBar = this.showColorBar;
        this.showColorBar = showColorBar;
        firePropertyChange(PROP_SHOWCOLORBAR, oldShowColorBar, showColorBar);
    }

    
    /**
     * TODO: Ed document me
     */
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
    
    /**
     * enumeration of the types of colorbars.
     */
    public static final class Type implements Enumeration, Displayable {
    	/**
    	 * List of all types in the order in which they are created. Must be
    	 * instantiated before any individual Type object.
    	 */
        private static List<Type> allTypesInOrder = new ArrayList<>();
     
        /**
         * Rainbow colorbar used by default.  TODO: this is a misnomer, where
         * color_wedge was the type of object in das1, not the instance of the type,
         * and this should be renamed to "rainbow"
         */
        public static final Type COLOR_WEDGE = new Type("color_wedge");

        /**
         * rainbow colorbar used at APL that has black at the bottom.
         */
        public static final Type APL_RAINBOW_BLACK0 = new Type("apl_rainbow_black0");

        /**
         * rainbow colorbar used at APL that has white at the bottom.
         */
        public static final Type APL_RAINBOW_WHITE0 = new Type("apl_rainbow_white0");

        /**
         * rainbow colorbar introduced for use at Goddard Space Flight Center.
         */
        public static final Type GSFC_RP_SPECIAL = new Type("gsfc_rp_special");

        /**
         * Mimic the default Matlab colorbar for comparison with Matlab-generated spectrograms.
         */
        public static final Type MATLAB_JET = new Type("matlab_jet");

        /**
         * Mimic the default Matlab colorbar for comparison with Matlab-generated spectrograms, but with black for the bottom most color.
         */
        public static final Type MATLAB_JET_BLACK0 = new Type("matlab_jet_black0");
        
        /**
         * Mimic the default Matlab colorbar HSV.
         */
        public static final Type MATLAB_HSV = new Type("matlab_hsv");

        public static final Type BLUE_TO_ORANGE = new Type("blue_to_orange");

        /**
         * gray scale with white at the minimum (bottom) and black at the maximum.
         */
        public static final Type GRAYSCALE = new Type("grayscale");

        /**
         * gray scale with black at the minimum (bottom) and white at the maximum.
         */
        public static final Type INVERSE_GRAYSCALE = new Type("inverse_grayscale");

        /**
         * rainbow that wraps around so that the top and bottom are the same color,
         * When used with care this is useful for spaces that wrap around (modulo),
         * such as longitude.
         */
        public static final Type WRAPPED_COLOR_WEDGE = new Type("wrapped_color_wedge");
        
        /**
         * colorbar with black in the middle, blue at the minimum and red at the maximum
         * for showing deviations from the center.
         */
        public static final Type BLUE_BLACK_RED_WEDGE = new Type("blue_black_red");

        /**
         * colorbar with white in the middle, blue at the minimum and red at the maximum
         * for showing deviations from the center.
         */
        public static final Type BLUE_WHITE_RED_WEDGE = new Type("blue_white_red");

        /**
         * black to red, introduced to show the red component of RGB images
         */
        public static final Type BLACK_RED = new Type("black_red");

        /**
         * black to green, introduced to show the green component of RGB images
         */
        public static final Type BLACK_GREEN = new Type("black_green");

        /**
         * black to blue, introduced to show the blue component of RGB images
         */
        public static final Type BLACK_BLUE = new Type("black_blue");

        /**
         * white to red, introduced to show grayscale-like image with color for comparisons
         */
        public static final Type WHITE_RED = new Type("white_red");

        /**
         * white to green, introduced to show grayscale-like image with color for comparisons
         */
        public static final Type WHITE_GREEN = new Type("white_green");

        /**
         * white to blue, introduced to show grayscale-like image with color for comparisons
         */
        public static final Type WHITE_BLUE = new Type("white_blue");

        /**
         * white to blue to black
         */
        public static final Type WHITE_BLUE_BLACK = new Type("white_blue_black");

        /**
         * black to blue to white
         */
        public static final Type INVERSE_WHITE_BLUE_BLACK = new Type("inverse_white_blue_black");
		  
        /**
         * Violet -Yellow
         */
        public static final Type VIOLET_YELLOW = new Type("violet_yellow");
        
        /**
         * SciPy Plasma
         */
        public static final Type SCIPY_PLASMA = new Type("scipy_plasma");
        
        /**
         * AJ4CO_RAINBOW for Radio JOVE community
         */
        public static final Type AJ4CO_RAINBOW = new Type("aj4co_rainbow");
        
        private BufferedImage image;
        private int[] colorTable;
        private final String desc;
        private javax.swing.Icon icon;
        
        private static Map<String,Type> extraTypes= new HashMap<>();
        
        private Type(String desc) {
            this.desc = desc;
            registerType(this);
        }
        
        /**
         * create a new color table Type.
         * @param desc string label for the colortable.
         * @param colorTable array of integers from makeColorTable
         * @see #makeColorTable(int[], int[], int[], int[], int, int, int) 
         */
        public Type(String desc,int[] colorTable ) {
            this.desc= desc;
            this.colorTable= Arrays.copyOf( colorTable, colorTable.length );
            registerType(this);
            extraTypes.put(desc,this);
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
        
        /**
         * initialize the icon representing this colorbar, if not done already.
         */
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
        
        /**
         * Return the number of colors in the color bar.  Fill (gray) is an additional
         * color, and it's understood that the colors indeces from 0 to getColorCount()-1 
         * are the color wedge, and getColorCount() is the fill color.
         * @return the number of colors.
         */        
        public int getColorCount() {
            maybeInitializeColorTable();
            return colorTable.length-1;
        }
        
        /**
         * return the RGB encoded color for the index.
         * @param index the index, from 0 to getColorCount().
         * @return the RGB color
         * @see Color#Color(int) 
         */
        public int getRGB(int index) {
            maybeInitializeColorTable();
            return colorTable[index];
        }
        
        /**
         * return an image showing the colors from left to right.
         * @param width the width of the image
         * @param height the height of the image
         * @return the image
         */
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
        
        /**
         * return an image showing the colors from bottom to top.
         * @param width the width of the image
         * @param height the height of the image
         * @return the image
         */
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
        
        /**
         * returns a color table with interpolated colors for the wedge from 0 to ncolor-1, and at ncolor, the fill color.
         * @param index set of indeces that are control points for interpolation, including 0 and ncolor-1.
         * @param red the red value from 0 to 255 at each index
         * @param green the green value from 0 to 255 at each index
         * @param blue the blue value from 0 to 255 at each index
         * @param ncolor number of colors, typically COLORTABLE_SIZE=240.
         * @param bottom the bottom, typically 0.
         * @param top the top, typically COLORTABLE_SIZE=240.
         * @return an array of RGB colors.
         * @see Color#Color(int) 
         */
        public static int[] makeColorTable( int [] index, int[] red, int[] green, int[] blue, int ncolor, int bottom, int top ) {
            // index should go from 0-255.
            // truncate when ncolor>COLORTABLE_SIZE
            
            if ( top>255 )  throw new IllegalArgumentException("top can be no more than 255");
            if ( bottom<0 ) throw new IllegalArgumentException("bottom can be no less than 0");
            if ( bottom>top ) throw new IllegalArgumentException("bottom must be less than top");
                
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
//                System.err.println( String.format( "%4d %4.2f %3f %3f %3f", i, a, rr, gg, bb ) );
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
            } else if (this == MATLAB_JET ) {
                initializeMatlabJet(size, bottom, top, false);
            } else if (this == MATLAB_JET_BLACK0 ) {
                initializeMatlabJet(size, bottom, top, true);
            } else if (this == MATLAB_HSV ) {
                initializeMatlabHSV(size, bottom, top);
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
            } else if (this == BLACK_RED) {
                initializeBlackRed(size, bottom, top);
            } else if (this == BLACK_GREEN ) {
                initializeBlackGreen(size, bottom, top);
            } else if (this == BLACK_BLUE) {
                initializeBlackBlue(size, bottom, top);
            } else if (this == WHITE_RED) {
                initializeWhiteRed(size, bottom, top);
            } else if (this == WHITE_GREEN ) {
                initializeWhiteGreen(size, bottom, top);
            } else if (this == WHITE_BLUE) {
                initializeWhiteBlue(size, bottom, top);
            } else if (this == WHITE_BLUE_BLACK) {
                initializeWhiteBlueBlack(size, bottom, top);
            } else if (this == INVERSE_WHITE_BLUE_BLACK) {
                initializeRevWhiteBlueBlack(size, bottom, top);
            } else if (this == VIOLET_YELLOW ) {
                initializeVioletYellow(size, bottom, top);
            } else if (this == BLUE_TO_ORANGE ) {
                initializeBlueToOrange(size, bottom, top);
            } else if (this == SCIPY_PLASMA ) {
                initializeSciPyPlasma(size, bottom, top);
            } else if (this == AJ4CO_RAINBOW ) {
                initializeAJ4CO(size, bottom, top);
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
        
        private void initializeSciPyPlasma( int size, int bottom, int top ) {
            // cat | awk '{ print $3 "," }'  | xargs
            int[] index =   {0 ,1 ,2 ,3 ,4 ,5 ,6 ,7 ,8 ,9 ,10 ,11 ,12 ,13 ,14 ,15 ,16 ,17 ,18 ,19 ,20 ,21 ,22 ,23 ,24 ,25 ,26 ,27 ,28 ,29 ,30 ,31 ,32 ,33 ,34 ,35 ,36 ,37 ,38 ,39 ,40 ,41 ,42 ,43 ,44 ,45 ,46 ,47 ,48 ,49 ,50 ,51 ,52 ,53 ,54 ,55 ,56 ,57 ,58 ,59 ,60 ,61 ,62 ,63 ,64 ,65 ,66 ,67 ,68 ,69 ,70 ,71 ,72 ,73 ,74 ,75 ,76 ,77 ,78 ,79 ,80 ,81 ,82 ,83 ,84 ,85 ,86 ,87 ,88 ,89 ,90 ,91 ,92 ,93 ,94 ,95 ,96 ,97 ,98 ,99 ,100 ,101 ,102 ,103 ,104 ,105 ,106 ,107 ,108 ,109 ,110 ,111 ,112 ,113 ,114 ,115 ,116 ,117 ,118 ,119 ,120 ,121 ,122 ,123 ,124 ,125 ,126 ,127 ,128 ,129 ,130 ,131 ,132 ,133 ,134 ,135 ,136 ,137 ,138 ,139 ,140 ,141 ,142 ,143 ,144 ,145 ,146 ,147 ,148 ,149 ,150 ,151 ,152 ,153 ,154 ,155 ,156 ,157 ,158 ,159 ,160 ,161 ,162 ,163 ,164 ,165 ,166 ,167 ,168 ,169 ,170 ,171 ,172 ,173 ,174 ,175 ,176 ,177 ,178 ,179 ,180 ,181 ,182 ,183 ,184 ,185 ,186 ,187 ,188 ,189 ,190 ,191 ,192 ,193 ,194 ,195 ,196 ,197 ,198 ,199 ,200 ,201 ,202 ,203 ,204 ,205 ,206 ,207 ,208 ,209 ,210 ,211 ,212 ,213 ,214 ,215 ,216 ,217 ,218 ,219 ,220 ,221 ,222 ,223 ,224 ,225 ,226 ,227 ,228 ,229 ,230 ,231 ,232 ,233 ,234 ,235 ,236 ,237 ,238 ,239 ,240 ,241 ,242 ,243 ,244 ,245 ,246 ,247 ,248 ,249 ,250 ,251 ,252 ,253 ,254 ,255 };
            int[] red =   {12 ,16 ,19 ,22 ,24 ,27 ,29 ,31 ,33 ,35 ,38 ,40 ,42 ,43 ,45 ,47 ,49 ,51 ,53 ,54 ,56 ,58 ,60 ,61 ,63 ,65 ,66 ,68 ,70 ,71 ,73 ,75 ,76 ,78 ,80 ,81 ,83 ,84 ,86 ,88 ,89 ,91 ,92 ,94 ,95 ,97 ,99 ,100 ,102 ,103 ,105 ,106 ,108 ,110 ,111 ,113 ,114 ,116 ,117 ,119 ,120 ,122 ,123 ,125 ,126 ,128 ,129 ,131 ,132 ,134 ,135 ,136 ,138 ,139 ,141 ,142 ,144 ,145 ,146 ,148 ,149 ,150 ,152 ,153 ,155 ,156 ,157 ,158 ,160 ,161 ,162 ,164 ,165 ,166 ,167 ,169 ,170 ,171 ,172 ,173 ,175 ,176 ,177 ,178 ,179 ,180 ,181 ,183 ,184 ,185 ,186 ,187 ,188 ,189 ,190 ,191 ,192 ,193 ,194 ,195 ,196 ,197 ,198 ,199 ,200 ,201 ,202 ,203 ,204 ,205 ,206 ,207 ,208 ,208 ,209 ,210 ,211 ,212 ,213 ,214 ,215 ,215 ,216 ,217 ,218 ,219 ,220 ,220 ,221 ,222 ,223 ,224 ,224 ,225 ,226 ,227 ,227 ,228 ,229 ,230 ,230 ,231 ,232 ,232 ,233 ,234 ,235 ,235 ,236 ,237 ,237 ,238 ,238 ,239 ,240 ,240 ,241 ,241 ,242 ,242 ,243 ,244 ,244 ,245 ,245 ,246 ,246 ,247 ,247 ,247 ,248 ,248 ,249 ,249 ,249 ,250 ,250 ,251 ,251 ,251 ,251 ,252 ,252 ,252 ,252 ,253 ,253 ,253 ,253 ,253 ,254 ,254 ,254 ,254 ,254 ,254 ,254 ,254 ,254 ,254 ,254 ,254 ,254 ,254 ,254 ,254 ,253 ,253 ,253 ,253 ,253 ,252 ,252 ,252 ,251 ,251 ,251 ,250 ,250 ,249 ,249 ,248 ,248 ,247 ,247 ,246 ,246 ,245 ,244 ,244 ,243 ,242 ,242 ,241 ,241 ,240 };
            int[] green =   {7 ,7 ,6 ,6 ,6 ,6 ,6 ,5 ,5 ,5 ,5 ,5 ,5 ,5 ,4 ,4 ,4 ,4 ,4 ,4 ,4 ,4 ,3 ,3 ,3 ,3 ,3 ,3 ,3 ,2 ,2 ,2 ,2 ,2 ,2 ,1 ,1 ,1 ,1 ,1 ,1 ,0 ,0 ,0 ,0 ,0 ,0 ,0 ,0 ,0 ,0 ,0 ,0 ,0 ,0 ,0 ,0 ,0 ,0 ,1 ,1 ,1 ,2 ,2 ,3 ,3 ,4 ,4 ,5 ,6 ,7 ,7 ,8 ,9 ,11 ,12 ,13 ,14 ,15 ,16 ,17 ,18 ,19 ,20 ,21 ,23 ,24 ,25 ,26 ,27 ,28 ,29 ,30 ,32 ,33 ,34 ,35 ,36 ,37 ,38 ,40 ,41 ,42 ,43 ,44 ,45 ,46 ,47 ,49 ,50 ,51 ,52 ,53 ,54 ,55 ,57 ,58 ,59 ,60 ,61 ,62 ,63 ,64 ,66 ,67 ,68 ,69 ,70 ,71 ,72 ,73 ,75 ,76 ,77 ,78 ,79 ,80 ,81 ,83 ,84 ,85 ,86 ,87 ,88 ,89 ,91 ,92 ,93 ,94 ,95 ,96 ,98 ,99 ,100 ,101 ,102 ,104 ,105 ,106 ,107 ,108 ,110 ,111 ,112 ,113 ,114 ,116 ,117 ,118 ,119 ,121 ,122 ,123 ,124 ,126 ,127 ,128 ,129 ,131 ,132 ,133 ,135 ,136 ,137 ,139 ,140 ,141 ,143 ,144 ,145 ,147 ,148 ,149 ,151 ,152 ,154 ,155 ,156 ,158 ,159 ,161 ,162 ,164 ,165 ,166 ,168 ,169 ,171 ,172 ,174 ,175 ,177 ,178 ,180 ,181 ,183 ,185 ,186 ,188 ,189 ,191 ,192 ,194 ,195 ,197 ,199 ,200 ,202 ,203 ,205 ,207 ,208 ,210 ,212 ,213 ,215 ,217 ,218 ,220 ,222 ,223 ,225 ,227 ,229 ,230 ,232 ,234 ,235 ,237 ,239 ,241 ,242 ,244 ,246 ,247 ,249 };
            int[] blue =   {135 ,136 ,137 ,138 ,140 ,141 ,142 ,143 ,144 ,145 ,146 ,146 ,147 ,148 ,149 ,150 ,151 ,151 ,152 ,153 ,154 ,154 ,155 ,156 ,156 ,157 ,158 ,158 ,159 ,160 ,160 ,161 ,161 ,162 ,162 ,163 ,163 ,164 ,164 ,165 ,165 ,165 ,166 ,166 ,166 ,167 ,167 ,167 ,167 ,168 ,168 ,168 ,168 ,168 ,168 ,168 ,169 ,169 ,169 ,168 ,168 ,168 ,168 ,168 ,168 ,168 ,167 ,167 ,167 ,167 ,166 ,166 ,166 ,165 ,165 ,164 ,164 ,163 ,163 ,162 ,161 ,161 ,160 ,160 ,159 ,158 ,157 ,157 ,156 ,155 ,154 ,154 ,153 ,152 ,151 ,150 ,149 ,149 ,148 ,147 ,146 ,145 ,144 ,143 ,142 ,141 ,140 ,139 ,138 ,137 ,137 ,136 ,135 ,134 ,133 ,132 ,131 ,130 ,129 ,128 ,127 ,126 ,125 ,124 ,123 ,122 ,122 ,121 ,120 ,119 ,118 ,117 ,116 ,115 ,114 ,113 ,112 ,112 ,111 ,110 ,109 ,108 ,107 ,106 ,105 ,105 ,104 ,103 ,102 ,101 ,100 ,99 ,98 ,98 ,97 ,96 ,95 ,94 ,93 ,92 ,92 ,91 ,90 ,89 ,88 ,87 ,86 ,86 ,85 ,84 ,83 ,82 ,81 ,80 ,80 ,79 ,78 ,77 ,76 ,75 ,74 ,73 ,73 ,72 ,71 ,70 ,69 ,68 ,67 ,67 ,66 ,65 ,64 ,63 ,62 ,61 ,60 ,60 ,59 ,58 ,57 ,56 ,55 ,54 ,54 ,53 ,52 ,51 ,50 ,49 ,49 ,48 ,47 ,46 ,46 ,45 ,44 ,43 ,43 ,42 ,41 ,41 ,40 ,40 ,39 ,39 ,38 ,38 ,37 ,37 ,37 ,36 ,36 ,36 ,36 ,36 ,36 ,36 ,36 ,36 ,36 ,37 ,37 ,37 ,37 ,38 ,38 ,38 ,39 ,39 ,39 ,38 ,38 ,37 ,36 ,33 };
            colorTable = makeColorTable( index, red, green, blue, size, bottom, top );
            colorTable[0] = ( colorTable[0] & 0xFFFFFF00 ) | 1;
        }        
        
        /**
         * Colorbar requested by the RadioJOVE community.  Their version of this has 4096 colors (and levels), 
         * but it is not trivial to make our colorbar support more than 256, and this 
         * will happen later this year (after TESS 2018).
         * See sftp://jfaden.net/home/jbf/ct/autoplot/rfe/318/codeAJ4CO.jy
         * @param size
         * @param bottom
         * @param top 
         */
        private void initializeAJ4CO( int size, int bottom, int top ) {
            // cat | awk '{ print $3 "," }'  | xargs
            int[] index= { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143, 144, 145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 155, 156, 157, 158, 159, 160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175, 176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195, 196, 197, 198, 199, 200, 201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223, 224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239, 240 };
            int[] red= { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5, 9, 13, 18, 22, 26, 31, 35, 39, 43, 47, 52, 56, 60, 64, 67, 71, 75, 79, 83, 87, 90, 94, 98, 101, 105, 108, 112, 115, 118, 122, 125, 128, 132, 135, 138, 141, 144, 147, 150, 153, 156, 159, 161, 164, 167, 170, 172, 175, 177, 180, 182, 185, 187, 189, 192, 194, 196, 198, 200, 202, 204, 206, 208, 210, 212, 214, 216, 217, 219, 221, 222, 224, 226, 227, 228, 230, 231, 233, 234, 235, 236, 237, 239, 240, 241, 242, 243, 244, 244, 245, 246, 247, 248, 248, 249, 250, 250, 251, 251, 252, 252, 253, 253, 253, 254, 254, 254, 254, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 254, 254, 254, 254, 253, 253, 253, 252, 252, 252, 251, 251, 250, 250, 249, 249, 248, 248, 247, 247, 246, 245, 245, 244, 243, 243, 242, 241, 240, 240, 239, 238, 237, 237, 236, 235, 234, 233, 232, 232, 231, 230, 229, 228, 227, 227, 226, 225, 224, 224, 223, 222, 221, 221, 222, 224, 226, 228, 230, 232, 234, 236, 238, 240, 242, 244, 246, 247, 249, 251, 253 };
            int[] green= { 0, 5, 11, 16, 22, 27, 33, 38, 43, 49, 54, 60, 65, 70, 76, 81, 87, 92, 98, 103, 108, 114, 119, 125, 132, 139, 144, 150, 155, 159, 164, 168, 172, 176, 180, 184, 187, 191, 194, 197, 200, 203, 206, 209, 211, 214, 216, 219, 221, 223, 225, 227, 229, 231, 233, 235, 236, 238, 239, 241, 242, 243, 245, 246, 247, 248, 249, 249, 250, 251, 252, 252, 253, 253, 254, 254, 254, 255, 255, 255, 255, 255, 255, 255, 255, 255, 254, 254, 254, 253, 253, 252, 252, 251, 250, 250, 249, 248, 247, 246, 246, 245, 244, 243, 241, 240, 239, 238, 237, 235, 234, 233, 231, 230, 229, 227, 226, 224, 222, 221, 219, 218, 216, 214, 212, 211, 209, 207, 205, 203, 201, 199, 197, 195, 193, 191, 189, 187, 185, 183, 181, 179, 177, 175, 172, 170, 168, 166, 164, 161, 159, 157, 154, 152, 150, 148, 145, 143, 141, 138, 136, 133, 131, 129, 126, 124, 122, 119, 117, 114, 112, 110, 107, 105, 102, 100, 98, 95, 93, 90, 88, 86, 83, 81, 79, 76, 74, 72, 69, 67, 65, 62, 60, 58, 55, 53, 51, 49, 47, 44, 42, 40, 38, 36, 34, 32, 29, 27, 25, 23, 21, 20, 18, 16, 14, 12, 10, 9, 7, 5, 4, 3, 1, 0, 11, 26, 40, 54, 69, 83, 98, 112, 127, 141, 156, 170, 184, 199, 213, 228, 242 };
            int[] blue= { 0, 74, 103, 125, 142, 157, 170, 181, 192, 201, 209, 216, 222, 228, 233, 238, 242, 245, 248, 250, 252, 254, 254, 255, 255, 255, 254, 254, 253, 252, 251, 250, 249, 248, 246, 245, 244, 242, 240, 239, 237, 235, 233, 231, 229, 227, 225, 223, 221, 218, 216, 214, 211, 209, 206, 204, 201, 199, 196, 193, 191, 188, 185, 182, 179, 176, 173, 170, 167, 164, 161, 158, 155, 152, 149, 146, 143, 140, 136, 133, 130, 127, 123, 120, 117, 114, 110, 107, 104, 100, 97, 94, 90, 87, 83, 80, 77, 73, 70, 67, 63, 60, 57, 53, 50, 46, 43, 40, 36, 33, 30, 26, 23, 20, 16, 13, 10, 7, 3, 0, 3, 6, 10, 13, 16, 19, 22, 26, 29, 32, 35, 38, 41, 44, 47, 50, 53, 56, 59, 62, 65, 68, 71, 74, 77, 79, 82, 85, 88, 90, 93, 96, 99, 101, 104, 106, 109, 111, 114, 117, 119, 121, 124, 126, 129, 131, 133, 136, 138, 140, 142, 145, 147, 149, 151, 153, 155, 157, 159, 161, 163, 165, 167, 169, 171, 173, 174, 176, 178, 180, 181, 183, 185, 186, 188, 189, 191, 192, 194, 195, 197, 198, 199, 201, 202, 203, 205, 206, 207, 208, 209, 210, 211, 213, 214, 215, 215, 216, 217, 218, 219, 220, 220, 221, 222, 224, 226, 228, 230, 232, 234, 236, 238, 240, 242, 244, 246, 247, 249, 251, 253 };
            colorTable = makeColorTable( index, red, green, blue, size, bottom, top );
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
        
        private void initializeMatlabJet( int size, int bottom, int top, boolean black0) {
            int [] index= { 0, 4, 8, 12, 16, 20, 24, 28, 32, 36, 40, 44, 48, 52, 56, 60, 64, 68, 72, 76, 81, 85, 89, 93, 97, 101, 105, 109, 113, 117, 121, 125, 129, 133, 137, 141, 145, 149, 153, 157, 162, 166, 170, 174, 178, 182, 186, 190, 194, 198, 202, 206, 210, 214, 218, 222, 226, 230, 234, 238, 243, 247, 251, 255 };
            int [] red= { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 15, 31, 47, 63, 79, 95, 111, 127, 143, 159, 175, 191, 207, 223, 239, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 239, 223, 207, 191, 175, 159, 143, 127 };
            int [] green= { 0, 0, 0, 0, 0, 0, 0, 0, 15, 31, 47, 63, 79, 95, 111, 127, 143, 159, 175, 191, 207, 223, 239, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 239, 223, 207, 191, 175, 159, 143, 127, 111, 95, 79, 63, 47, 31, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
            int [] blue= { 143, 159, 175, 191, 207, 223, 239, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 239, 223, 207, 191, 175, 159, 143, 127, 111, 95, 79, 63, 47, 31, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
            if ( black0 ) {
                blue[0]= 0;
            }
            colorTable = makeColorTable( index, red, green, blue, size, bottom, top );
        }   
   
        private void initializeMatlabHSV( int size, int bottom, int top ) {
            int [] index= { 0,4,8,12,16,20,24,28,32,36,40,45,49,53,57,61,65,69,73,77,81,85,89,93,97,101,105,109,113,117,121,125,130,134,138,142,146,150,154,158,162,166,170,174,178,182,186,190,194,198,202,206,210,215,219,223,227,231,235,239,243,247,251,255};
            int [] red= { 255,255,255,255,255,255,255,255,255,255,255,247,223,199,175,151,127,103,79,55,31,7,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7,31,55,79,103,127,151,175,199,223,247,255,255,255,255,255,255,255,255,255,255};
            int [] green= { 0,23,47,71,95,119,143,167,191,215,239,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,231,207,183,159,135,111,87,63,39,15,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
            int [] blue= { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,15,39,63,87,111,135,159,183,207,231,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,239,215,191,167,143,119,95,71,47,23};
            colorTable = makeColorTable( index, red, green, blue, size, bottom, top );
        }
        
        private void initializeWhiteRed( int size, int bottom, int top ) {
            int [] index= { 0, 255 };
            int [] red= { 255, 255 };
            int [] green= { 255, 0 };
            int [] blue= { 255, 0 };
            colorTable = makeColorTable( index, red, green, blue, size, bottom, top );
        }   
        private void initializeWhiteGreen( int size, int bottom, int top ) {
            int [] index= { 0, 255 };
            int [] red= { 255, 0 };
            int [] green= { 255, 255 };
            int [] blue= { 255, 0 };
            colorTable = makeColorTable( index, red, green, blue, size, bottom, top );
        }   
        private void initializeWhiteBlue( int size, int bottom, int top ) {
            int [] index= { 0, 255 };
            int [] red= { 255, 0 };
            int [] green= { 255, 0 };
            int [] blue= { 255, 255 };
            colorTable = makeColorTable( index, red, green, blue, size, bottom, top );
        }   

        private void initializeBlackRed( int size, int bottom, int top ) {
            int [] index= { 0, 255 };
            int [] red= { 0, 255 };
            int [] green= { 0, 0 };
            int [] blue= { 0, 0 };
            colorTable = makeColorTable( index, red, green, blue, size, bottom, top );
        }   
        private void initializeBlackGreen( int size, int bottom, int top ) {
            int [] index= { 0, 255 };
            int [] red= { 0, 0 };
            int [] green= { 0, 255 };
            int [] blue= { 0, 0 };
            colorTable = makeColorTable( index, red, green, blue, size, bottom, top );
        }   
        private void initializeBlackBlue( int size, int bottom, int top ) {
            int [] index= { 0, 255 };
            int [] red= { 0, 0 };
            int [] green= { 0, 0 };
            int [] blue= { 0, 255 };
            colorTable = makeColorTable( index, red, green, blue, size, bottom, top );
        }   
        
        /**
         * Masafumi requested reversed IDL colorbar 2
         * @param size
         * @param bottom
         * @param top 
         */
        private void initializeWhiteBlueBlack( int size, int bottom, int top ) {
            //int [] index= { 0,   94,   188,  255  };
            //int [] red=   { 0,    0,     0,  255 };
            //int [] green= { 0,    0,   150,  255 };
            //int [] blue=  { 0,    133, 255,  255 };
            int [] index= { 0,     67,   161,  255  };
            int [] red=   { 255,    0,     0,  0 };
            int [] green= { 255,    150,   0,  0 };
            int [] blue=  { 255,    255, 133,  0 };
            colorTable = makeColorTable( index, red, green, blue, size, bottom, top );
        }   
		        
		/**
         * Masafumi requested reverse of reversed IDL colorbar 2
         * @param size
         * @param bottom
         * @param top 
         */
        private void initializeRevWhiteBlueBlack( int size, int bottom, int top ) {
            int [] index= { 0,   94,   188,  255  };
            int [] red=   { 0,    0,     0,  255 };
            int [] green= { 0,    0,   150,  255 };
            int [] blue=  { 0,    133, 255,  255 };
            //int [] index= { 0,     67,   161,  255  };
            //int [] red=   { 255,    0,     0,  0 };
            //int [] green= { 255,    150,   0,  0 };
            //int [] blue=  { 255,    255, 133,  0 };
            colorTable = makeColorTable( index, red, green, blue, size, bottom, top );
        }   
        
        //Rob W. JADE colorbar
        private void initializeVioletYellow( int size, int bottom, int top ) {
            int [] index= {  0,  27,  64,  95, 127, 159, 183, 202, 225, 246, 255 };
            int [] red=   { 68,  71,  59,  45,  34,  36,  71, 110, 174, 233, 253 };
            int [] green= {  2,  36,  83, 111, 139, 171, 192, 206, 221, 229, 231 };
            int [] blue=  { 86, 114, 139, 142, 141, 129, 110,  87,  46,  23,  37,};
            colorTable = makeColorTable( index, red, green, blue, size, bottom, top );
        }   

        /**
         * from the string, identify the type.
         * @param s string like "apl_rainbow_black0"
         * @return type like Type.APL_RAINBOW_BLACK0.
         */
        public static Type parse(String s) {
        	for (Type t: allTypesInOrder) {
        		if (t.getListLabel().equalsIgnoreCase(s)) {
        			return t;
        		}
        	}
            switch (s) {
                case "black_white":
                    return INVERSE_GRAYSCALE;
                default:
                    throw new IllegalArgumentException("undefined DasColorBar.Type identifier: " + s);
            }
        }

        /**
         * return a list of all defined Types, in the order in which they were constructed.
         * @return list of Type objects.
         */
        public static List<Type> getAllTypes() {
        	return new ArrayList<>(allTypesInOrder);
        }

        private static void registerType(Type t) {
        	String s = t.getListLabel();
        	for (Type type: allTypesInOrder) {
        		if (s.equalsIgnoreCase(type.getListLabel())) {
        			throw new IllegalArgumentException("duplicated DasColorBar.Type identifier: " + s);
        		}
        	}
        	allTypesInOrder.add(t);
        }
    }
    
    /*
     * ColorBarRepalletteMouseModule removed because it is no longer useful.
     */

    /**
     * get the color used to indicate fill, often gray or a transparent 
     * white.  Note all instances use the same fillColor.
     * @return the fill color.
     */
    public Color getFillColor() {
        return new Color( this.fillColor, true );
    }

    /**
     * set the color used to indicate fill, often gray or a transparent 
     * white.  Note all instances use the same fillColor.
     * @param fillColor the new fill color.
     */
    public void setFillColor(Color fillColor) {
        Color oldColor= new Color( this.fillColor );
        this.fillColor = fillColor.getRGB();
        this.type.initializeColorTable( COLORTABLE_SIZE, 0, this.type.getColorCount() );
        markDirty("fillColor");
        update();
        firePropertyChange( PROPERTY_FILL_COLOR, oldColor,fillColor );
    }

}
