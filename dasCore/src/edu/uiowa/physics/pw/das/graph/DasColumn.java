/* File: DasColumn.java
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author  jbf
 */
public class DasColumn extends DasDevicePosition {

    public DasColumn(DasCanvas parent, double left, double right) {
        super(parent,left,right,true);
    }
    
    public DasColumn( DasCanvas canvas, DasColumn parent, double nMin, double nMax, 
            double emMin, double emMax, int ptMin, int ptMax ) {
        super( canvas, true, parent, nMin, nMax, emMin, emMax, ptMin, ptMax );
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
           return new DasColumn(parent,null,0.0,1.0,5,-3,0,0);
    }
    
    public static DasColumn create( DasCanvas parent, int iplot, int nplot ) {
        double min= 0.1 + iplot * ( 0.7 ) / nplot;
        double max= 0.099 + ( iplot + 1 ) * ( 0.7 ) / nplot;
        return new DasColumn( parent, min, max );
    }
    
    public DasColumn createAttachedColumn(double pleft, double pright) {
        return new DasColumn(null,this,pleft,pright,0,0,0,0);
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
    
        
}
