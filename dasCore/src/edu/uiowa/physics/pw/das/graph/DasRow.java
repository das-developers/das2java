/* File: DasRow.java
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
public class DasRow extends DasDevicePosition {
    
    public DasRow(DasCanvas parent, double top, double bottom) {
        super(parent,top,bottom);
    }
    
    static class NullDasRow extends DasRow {
        public NullDasRow() {
            super(null, 0,0);
        }
        public int getDMinimum() {
            throw new RuntimeException("null row, row was not set before layout");
        }
        public int getDMaximum() {
            throw new RuntimeException("null row, row was not set before layout");
        }
        
        public void addPropertyChangeListener(String propertyName, java.beans.PropertyChangeListener listener) {
            throw new RuntimeException( "NULL.addPropertyChangeListener" );
        }
        
        public void removePropertyChangeListener(String propertyName, java.beans.PropertyChangeListener listener) {
            throw new RuntimeException( "NULL.removePropertyChangeListener" );
        }
        
        public void addpwUpdateListener(edu.uiowa.physics.pw.das.graph.event.DasUpdateListener l) {
            throw new RuntimeException( "NULL.addpwUpdateListener" );
        }
        
        public void removepwUpdateListener(edu.uiowa.physics.pw.das.graph.event.DasUpdateListener l) {
            throw new RuntimeException( "NULL.removepwUpdateListener" );
        }
        
        protected void fireUpdate() {
            throw new RuntimeException( "NULL.fireUpdate" );
        }
        
    }
    
    public static final DasRow NULL= new NullDasRow();
    
    protected int getDeviceSize() {
        return parent.getHeight();
    }
    
    public DasRow createSubRow(double ptop, double pbottom) {
        double top= getMinimum();
        double bottom= getMaximum();
        double delta= top-bottom;
        return new DasRow(this.parent,bottom+ptop*delta,bottom+pbottom*delta);
    }
    
    public int getHeight() {
        return getDMaximum()-getDMinimum();
    }
    
    public static DasRow create(DasCanvas parent) {
        return new DasRow(parent,0.1,0.9);
    }
    
    /** Process a <code>&lt;row&gt;</code> element.
     *
     * @param element The DOM tree node that represents the element
     */
    static DasRow processRowElement(Element element, DasCanvas canvas, FormBase form) throws DasException {
        String name = element.getAttribute("name");
        double minimum = Double.parseDouble(element.getAttribute("minimum"));
        double maximum = Double.parseDouble(element.getAttribute("maximum"));
        DasRow row =  new DasRow(canvas, minimum, maximum);
        row.setDasName(name);
        DasApplication app = form.getDasApplication();
        NameContext nc = app.getNameContext();
        nc.put(name, row);
        return row;
    }
    
    public Element getDOMElement(Document document) {
        Element element = document.createElement("row");
        element.setAttribute("name", getDasName());
        element.setAttribute("minimum", Double.toString(getMinimum()));
        element.setAttribute("maximum", Double.toString(getMaximum()));
        return element;
    }
    
}
