/* File: DasColumn.java
 * Copyright (C) 2002-2014 The University of Iowa
 * Created by: Jeremy Faden <jeremy-faden@space.physics.uiowa.edu>
 *             Edward E. West <edward-west@space.physics.uiowa.edu>
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

import org.das2.NameContext;
import org.das2.DasApplication;
import org.das2.DasException;
import org.das2.dasml.FormBase;
import java.text.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Define a vertical region on a DasCanvas.
 * Extends from the top to the bottom of the canvas.  Canvas components such as
 * plots, axes, colorbars and labels use DasColumns and DasRows for positioning.
 * 
 * @author jbf
 * @author eew
 * @since 2.0
 */
public class DasColumn extends DasDevicePosition {

	/** Create a new vertical region on a DasCanvas.
	 * 
	 * @param parent The canvas on which the region will be defined
	 * @param rLeft a value between 0.0 and 1.0 defining the left boundary of the column
	 * @param rRight a value between 0.0 and 1.0 defining the right boundary of the column
	 */
    public DasColumn(DasCanvas parent, double rLeft, double rRight)
	 {
        super(parent,rLeft,rRight,true);
    }
    
	 /** Create a new vertical region on a DasCanvas
	  * 
	  * @param canvas The canvas on which the region will be defined
	  * @param parent If not null, the column is defined relative to another column.  So
	  *        rMin = 0.0 is the left position of the parent column and rMax = 1.0 is the
	  *        right position of the parent column.
	  * @param rLeft a value between 0.0 and 1.0 defining the left boundary of the column
	  * @param rRight a value between 0.0 and 1.0 defining the right boundary of the column
	  * @param emMin Offset from rLeft in "M's", adds with ptMin
	  * @param emMax Offset from rRight in "M's", adds with ptMax
	  * @param ptMin Offset from rLeft in pixels, adds with emMin
	  * @param ptMax Offset from rRight in pixels, adds with emMax
	  */
    public DasColumn(DasCanvas canvas, DasColumn parent, double rLeft, double rRight,
                     double emMin, double emMax, int ptMin, int ptMax )
	 {
        super( canvas, true, parent, rLeft, rRight, emMin, emMax, ptMin, ptMax );
    }
    
    
    /** Makes a new DasColumn by parsing a formatted string
     * makes a new DasColumn by parsing a string like "100%-5em+3pt" to get the offsets.
     * The three qualifiers are "%", "em", and "pt", but "px" is allowed as well 
     * as surely people will use that by mistake.  If an offset or the normal position
     * is not specified, then 0 is used.
     *
     * @param canvas the canvas for the layout, ignored when a parent DasColumn is used.
     * @param parent if non-null, this DasColumn is specified with respect to parent.
     * @param minStr a string like "0%+5em"
     * @param maxStr a string like "100%-7em"
     * @throws IllegalArgumentException if the strings cannot be parsed
     */
    public static DasColumn create(DasCanvas canvas, DasColumn parent, String minStr, 
	                                String maxStr ) 
	 {
        double[] min, max;
        try {
            min= parseFormatStr( minStr );
        } catch ( ParseException e ) {
            throw new IllegalArgumentException("unable to parse min: \""+minStr+"\"");
        }
        try {
            max= parseFormatStr( maxStr );
        } catch ( ParseException e ) {
            throw new IllegalArgumentException("unable to parse max: \""+maxStr+"\"");
        }
        return new DasColumn( canvas, parent, min[0], max[0], min[1], max[1], (int)min[2], (int)max[2] );
    }
    
    public static final DasColumn NULL= new DasColumn(null,null,0,0,0,0,0,0);
    
    /**
     * @deprecated This created a column that was not attached to anything, so
     * it was simply a convenience method that didn't save much effort.
     */
    public DasColumn createSubColumn( double pleft, double pright ) {
        double left= getMinimum();
        double right= getMaximum();
        double delta= right-left;
        return new DasColumn(getCanvas(),left+pleft*delta,left+pright*delta);
    }
    
    public int getWidth() {
        return getDMaximum()-getDMinimum();
    }
    
    public static DasColumn create(DasCanvas parent) {
        return new DasColumn(parent,null,0.0,1.0,5,-5,0,0);
    }
    
    public static DasColumn create( DasCanvas parent, int iplot, int nplot ) {
        double min= 0.1 + iplot * ( 0.7 ) / nplot;
        double max= 0.099 + ( iplot + 1 ) * ( 0.7 ) / nplot;
        return new DasColumn( parent, min, max );
    }
    
    public DasColumn createAttachedColumn(double pleft, double pright) {
        return new DasColumn(null,this,pleft,pright,0,0,0,0);
    }

    /**
     * create a child by parsing spec strings like "50%+3em"
     * @throws IllegalArgumentException when the string is malformed.
     * @param smin
     * @param smax
     * @return
     */
    public DasColumn createChildColumn( String smin, String smax ) {
        try {
            double[] min= DasDevicePosition.parseFormatStr(smin);
            double[] max= DasDevicePosition.parseFormatStr(smax);
            return new DasColumn( null, this, min[0], max[0], min[1], max[1], (int)min[2], (int)max[2] );
        } catch ( ParseException ex ) {
            throw new IllegalArgumentException(ex);
        }
    }

    /** Process a <code>&lt;column7gt;</code> element.
     *
     * @param element The DOM tree node that represents the element
     */
    static DasColumn processColumnElement(Element element, DasCanvas canvas, FormBase form) throws DasException {
        String name = element.getAttribute("name");
        double minimum
                = Double.parseDouble(element.getAttribute("minimum"));
        double maximum
                = Double.parseDouble(element.getAttribute("maximum"));
        DasColumn column = new DasColumn(canvas, minimum, maximum);
        column.setDasName(name);
        DasApplication app = form.getDasApplication();
        NameContext nc = app.getNameContext();
        nc.put(name, column);
        return column;
    }
    
    public Element getDOMElement(Document document) {
        Element element = document.createElement("column");
        element.setAttribute("name", getDasName());
        element.setAttribute("minimum", Double.toString(getMinimum()));
        element.setAttribute("maximum", Double.toString(getMaximum()));
        return element;
    }
    
    /**
     * return the left of the column.
     * @return
     */
    public int left() {
        return getDMinimum();
    }
    
    /**
     * return the right (non-inclusive) of the column.
     * @return
     */
    public int right() {
        return getDMaximum();
    }
}

