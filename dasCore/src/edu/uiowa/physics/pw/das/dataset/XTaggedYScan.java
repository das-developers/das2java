/* File: XTaggedYScan.java
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

package edu.uiowa.physics.pw.das.dataset;

/**
 *
 * @author  eew
 */
public class XTaggedYScan implements java.io.Serializable, Cloneable {
    
    static final long serialVersionUID = -5171867293621291751L;
    
    public double x;
    public float[] z;
    
    /** Creates a new instance of XTaggedYScan */
    public XTaggedYScan() {
    }
    
    public XTaggedYScan(double x, float[] z) {
        this.x= x;
        this.z= (float[])z.clone();
    }
    
    public Object clone() {
        XTaggedYScan result=null;
        try {
            result= (XTaggedYScan)super.clone();
        } catch ( java.lang.CloneNotSupportedException e ) {
            edu.uiowa.physics.pw.das.util.DasDie.println(""+e);
        }
        result.z= new float[this.z.length];
        System.arraycopy(this.z,0,result.z,0,this.z.length);
        result.x= this.x;
        return (Object)result;
    }
    
    public String toString() {
        int i;
        String zString = "[";
        
        for (i = 0; i < z.length-1; i++)
            zString = zString + z[i] + ",";
        zString = zString + z[i] + "]";
        
        return "x tag: " + x + "\n z values: " + zString + "\n";
    }
    
}
