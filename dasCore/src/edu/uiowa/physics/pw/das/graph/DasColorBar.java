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

package edu.uiowa.physics.pw.das.graph;

import edu.uiowa.physics.pw.das.*;
import edu.uiowa.physics.pw.das.dasml.FormBase;
import edu.uiowa.physics.pw.das.datum.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

/**
 *
 * @author  jbf
 */
public class DasColorBar extends DasAxis {
    
    private BufferedImage image;
    private DasColorBar.Type type;
    
    public DasColorBar( Datum min, Datum max, DasRow row, DasColumn column, boolean isLog) {
        this(min, max, row, column, RIGHT, isLog);
    }
    
    public DasColorBar( Datum min, Datum max, DasRow row, DasColumn column, int orientation, boolean isLog) {
        super(min, max, row, column, orientation, isLog);
        setLayout(new ColorBarLayoutManager());
        setType(DasColorBar.Type.COLOR_WEDGE);
    }
    
    public int itransform(double x, Units units) {
        int ncolor = type.getColorCount();
        int icolor= (int)transform(x,units,0, ncolor);
        
        if ( units.getFill().doubleValue(units)==x ) {
            return Color.LIGHT_GRAY.getRGB();
        } else {
            icolor= (icolor<0)?0:icolor;
            icolor= (icolor>=ncolor)?(ncolor-1):icolor;
            return type.getRGB(icolor);
        }
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
        image = null;
        markDirty();
        update();
        firePropertyChange("type", oldValue, type);
    }
    
    protected void paintComponent(Graphics g) {
        int x = (int)Math.round(getColumn().getDMinimum());
        int y = (int)Math.round(getRow().getDMinimum());
        int width = (int)Math.round(getColumn().getDMaximum()) - x;
        int height = (int)Math.round(getRow().getDMaximum()) - y;
        if (image == null || image.getWidth() != width || image.getHeight() != height) {
            if (isHorizontal()) {
                image = type.getHorizontalScaledImage(width, height);
            }
            else {
                image = type.getVerticalScaledImage(width, height);
            }
        }
        g.translate(-getX(), -getY());
        if (!isHorizontal()) {
            y++;
        }
        g.drawImage(image, x, y, this);
        g.translate(getX(), getY());
        super.paintComponent(g);
    }
    
    protected Rectangle getAxisBounds() {
        int x = (int)Math.round(getColumn().getDMinimum());
        int y = (int)Math.round(getRow().getDMinimum());
        int width = (int)Math.round(getColumn().getDMaximum()) - x;
        int height = (int)Math.round(getRow().getDMaximum()) - y;
        Rectangle rc = new Rectangle(x, y, width, height);
        Rectangle bounds = super.getAxisBounds();
        bounds.add(rc);
        return bounds;
    }
    
    public static DasColumn getColorBarColumn(DasColumn column) {
        return new AttachedColumn(column,1.05,1.10);
        /*    double xmin= column.getMinimum();
        double nsize= column.getMaximum() - column.getMinimum();
        DasColumn result= new DasColumn(column.parent,xmin+nsize*1.01,xmin+nsize*1.05);
        return result; */
    }
    
    /** Process a <code>&lt;colorbar&gt;</code> element.
     *
     * @param element The DOM tree node that represents the element
     */
    static DasColorBar processColorbarElement(Element element, DasRow row, DasColumn column, FormBase form) throws edu.uiowa.physics.pw.das.DasPropertyException, edu.uiowa.physics.pw.das.DasNameException {
        String name = element.getAttribute("name");
        boolean log = element.getAttribute("log").equals("true");
        double dataMinimum = Double.parseDouble(element.getAttribute("dataMinimum"));
        double dataMaximum = Double.parseDouble(element.getAttribute("dataMaximum"));
        int orientation = parseOrientationString(element.getAttribute("orientation"));
        String rowString = element.getAttribute("row");
        if (!rowString.equals("") || row == null) {
            row = (DasRow)form.checkValue(rowString, DasRow.class, "<row>");
        }
        String columnString = element.getAttribute("column");
        if (!columnString.equals("") || row == null) {
            column = (DasColumn)form.checkValue(columnString, DasColumn.class, "<column>");
        }
        
        DasColorBar cb = new DasColorBar(Datum.create(dataMinimum), Datum.create(dataMaximum), row, column, orientation, log);
        
        cb.setLabel(element.getAttribute("label"));
        cb.setOppositeAxisVisible(!element.getAttribute("oppositeAxisVisible").equals("false"));
        cb.setTickLabelsVisible(!element.getAttribute("tickLabelsVisible").equals("false"));
        cb.setType(DasColorBar.Type.parse(element.getAttribute("type")));

        cb.setDasName(name);
        DasApplication app = form.getDasApplication();
        NameContext nc = app.getNameContext();
        nc.put(name, cb);
        
        return cb;
    }
    
    public Element getDOMElement(Document document) {
        Element element = document.createElement("colorbar");
        String minimumStr = getDataMinimum().toString();
        element.setAttribute("dataMinimum", minimumStr);
        String maximumStr = getDataMaximum().toString();
        element.setAttribute("dataMaximum", maximumStr);

        element.setAttribute("name", getDasName());
        element.setAttribute("row", getRow().getDasName());
        element.setAttribute("column", getColumn().getDasName());

        element.setAttribute("label", getLabel());
        element.setAttribute("log", Boolean.toString(isLog()));
        element.setAttribute("tickLabelsVisible", Boolean.toString(areTickLabelsVisible()));
        element.setAttribute("oppositeAxisVisible", Boolean.toString(isOppositeAxisVisible()));
        element.setAttribute("animated", Boolean.toString(isAnimated()));
        element.setAttribute("orientation", orientationToString(getOrientation()));
        element.setAttribute("type", getType().toString());

        return element;
    }
    
    public static DasColorBar createNamedColorBar(String name) {
        DasColorBar cb = new DasColorBar(Datum.create(1.0, Units.dimensionless), Datum.create(10.0, Units.dimensionless), null, null, false);
        if (name == null) {
            name = "colorbar_" + Integer.toHexString(System.identityHashCode(cb));
        }
        try {
            cb.setDasName(name);
        }
        catch (edu.uiowa.physics.pw.das.DasNameException dne) {
            edu.uiowa.physics.pw.das.util.DasExceptionHandler.handle(dne);
        }
        return cb;
    }
    
    public Shape getActiveRegion() {
        int x = (int)Math.round(getColumn().getDMinimum());
        int y = (int)Math.round(getRow().getDMinimum());
        int width = (int)Math.round(getColumn().getDMaximum()) - x;
        int height = (int)Math.round(getRow().getDMaximum()) - y;
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
            int x = (int)Math.round(getColumn().getDMinimum());
            int y = (int)Math.round(getRow().getDMinimum());
            int width = (int)Math.round(getColumn().getDMaximum()) - x;
            int height = (int)Math.round(getRow().getDMaximum()) - y;
            Rectangle rc = new Rectangle(x - getX(), y - getY(), width, height);
            Rectangle bounds = primaryInputPanel.getBounds();
            bounds.add(rc);
            primaryInputPanel.setBounds(bounds);
        }
        
    }
    
    public static final class Type implements edu.uiowa.physics.pw.das.components.PropertyEditor.Enumeration {
        
        public static final Type COLOR_WEDGE = new Type("color_wedge");
        public static final Type GRAYSCALE = new Type("grayscale");
        public static final Type INVERSE_GRAYSCALE = new Type("inverse_grayscale");
        
        private BufferedImage image;
        private int[] colorTable;
        private final String desc;
        private javax.swing.Icon icon;
        
        private Type(String desc) {
            this.desc = desc;
        }
        
        public javax.swing.Icon getListIcon() {
            maybeInitializeIcon();
            return icon;
        }
        
        public void maybeInitializeIcon() {
            if (icon == null) {
                icon = new javax.swing.ImageIcon(getVerticalScaledImage(24, 24));
            }
        }
        
        public String toString() {
            return desc;
        }
        
        public int getColorCount() {
            maybeInitializeColorTable();
            return colorTable.length;
        }
        
        public int getRGB(int index) {
            maybeInitializeColorTable();
            return colorTable[index];
        }
        
        public BufferedImage getHorizontalScaledImage(int width, int height) {
            maybeInitializeImage();
            BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            AffineTransform at = new AffineTransform();
            at.scale((double)width / (double)colorTable.length, (double)height);
            at.rotate(-Math.PI/2.0);
            at.translate(-1.0, 0.0);
            AffineTransformOp op = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
            op.filter(image, scaled);
            return scaled;
        }
        
        public BufferedImage getVerticalScaledImage(int width, int height) {
            maybeInitializeImage();
            BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            AffineTransform at = new AffineTransform();
            at.scale((double)width, -(double)height / (double)colorTable.length);
            at.translate(0.0, -(double)colorTable.length);
            AffineTransformOp op = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
            op.filter(image, scaled);
            return scaled;
        }
        
        private void maybeInitializeImage() {
            if (image == null) {
                maybeInitializeColorTable();
                image = new BufferedImage(1, colorTable.length, BufferedImage.TYPE_INT_RGB);
                image.setRGB(0, 0, 1, colorTable.length, colorTable, 0, 1);
            }
        }
        
        private void maybeInitializeColorTable() {
            if (colorTable == null) {
                if (this == COLOR_WEDGE) {
                    initializeColorWedge();
                }
                else if (this == GRAYSCALE) {
                    initializeGrayScale();
                }
                else if (this == INVERSE_GRAYSCALE) {
                    initializeInverseGrayScale();
                }
            }
        }
        
        private void initializeColorWedge() {
            int[] index = {   0,   30,   63, 126, 162, 192, 221, 255 };
            int[] red =   {   0,    0,    0,   0, 255, 255, 255, 255 };
            int[] green = {   0,    0,  255, 255, 255, 185,  84, 0 };
            int[] blue =  { 137,  255,  255,   0,   0,   0,   0, 0 };
            colorTable = new int[256];

            int ii= 0;
            for (int i = 0; i < colorTable.length; i++) {
                if (i > index[ii + 1]) {
                    ii++;
                }
                double a= (i-index[ii]) / (double)(index[ii+1]-index[ii]);
                double rr= (red[ii]*(1-a) + red[ii+1]*a)/(double)255.;
                double gg= (green[ii]*(1-a) + green[ii+1]*a)/(double)255.;
                double bb= (blue[ii]*(1-a) + blue[ii+1]*a)/(double)255.;
                colorTable[i]= new Color((float)rr,(float)gg,(float)bb).getRGB();
            }
        }
        
        
        private void initializeInverseGrayScale() {
            colorTable = new int[256];
            for (int i = 0; i < 256; i++) {
                colorTable[i] = 0xff000000 | (i << 16) | (i << 8) | i;
            }
        }
        
        private void initializeGrayScale() {
            colorTable = new int[256];
            for (int i = 255; i >= 0; i--) {
                colorTable[255 - i] = 0xff000000 | (i << 16) | (i << 8) | i;
            }
        }
        
        public static Type parse(String s) {
            if (s.equals("color_wedge")) {
                return COLOR_WEDGE;
            }
            else if (s.equals("grayscale")) {
                return GRAYSCALE;
            }
            else if (s.equals("inverse_grayscale")) {
                return INVERSE_GRAYSCALE;
            }
            else {
                throw new IllegalArgumentException("invalid DasColorBar.Type string: " + s);
            }
        }
    }
    
}
