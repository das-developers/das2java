/* File: DasColumn.java
 * Copyright (C) 2002-2003 University of Iowa
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

import edu.uiowa.physics.pw.das.dasml.FormBase;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author  jbf
 */
public class DasColumn extends DasDevicePosition {

    public DasColumn(DasCanvas parent, double left, double right) {
        super(parent,left,right);
    }
    
    public DasColumn createSubColumn( double pleft, double pright ) {
        double left= getMinimum();
        double right= getMaximum();
        double delta= right-left;
        return new DasColumn(this.parent,left+pleft*delta,left+pright*delta);
    }
    
    protected double getDeviceSize()
    {
        return (double)parent.getWidth();
    }
    
    public double getWidth() {
        return getDMaximum()-getDMinimum();
    }
    
    public static DasColumn create(DasCanvas parent) {
        return new DasColumn(parent,0.1,0.8);
    }
    
    public DasColumn createAttachedColumn(double pleft, double pright) {
        return new AttachedColumn(this,pleft,pright);
    }
    
    /** Process a <code>&lt;column7gt;</code> element.
     *
     * @param element The DOM tree node that represents the element
     */
    static DasColumn processColumnElement(Element element, DasCanvas canvas, FormBase form) {
        String name = element.getAttribute("name");
        double minimum
        = Double.parseDouble(element.getAttribute("minimum"));
        double maximum
        = Double.parseDouble(element.getAttribute("maximum"));
        DasColumn column = new DasColumn(canvas, minimum, maximum);
        try {
            column.setDasName(name);
        }
        catch (edu.uiowa.physics.pw.das.DasNameException dne) {
            edu.uiowa.physics.pw.das.util.DasExceptionHandler.handle(dne);
        }
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
