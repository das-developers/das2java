/* File: XMultiY.java
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

package edu.uiowa.physics.pw.das.dataset;

/**
 *
 * @author  eew
 */
public class XMultiY implements java.io.Serializable {
    
    static final long serialVersionUID = -8494703975732274115L;
    
    public double x;
    
    public double[] y;
    
    /** Creates a new instance of XMultiY */
    public XMultiY() {
    }
    
    public XMultiY(double x, double[] y) {
        this.x= x;
        this.y= (double[]) y.clone();
    }
    
    public String toString()
    {
        int i;
        String yString = "[";
        
        for (i = 0; i < y.length-1; i++)
            yString = yString + y[i] + ",";
        yString = yString + y[i] + "]";
        
        return "x tag: " + x + "\n y values: " + yString + "\n";
    }
    
}
