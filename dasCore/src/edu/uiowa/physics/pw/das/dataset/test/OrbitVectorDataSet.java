/* File: OrbitVectorDataSet.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on November 19, 2003, 11:49 AM by __FULLNAME__ <__EMAIL__>
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


package edu.uiowa.physics.pw.das.dataset.test;

import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.datum.*;

/**
 *
 * @author  jbf
 */
public class OrbitVectorDataSet implements VectorDataSet {
    
    static double[][] data;
    private double[][] idata; // pixel space    
    
    private static OrbitVectorDataSet xview= new OrbitVectorDataSet(0);
    private static OrbitVectorDataSet yview= new OrbitVectorDataSet(1);
    
    Units xunits;
    Units yunits;
    Units tunits;
    int view;
    
    private OrbitVectorDataSet(int view) {
        data= new double[3][40];        
        for ( int i=0; i<data[0].length; i++ ) {
            data[0][i]= 5*Math.sin(i*1*2*Math.PI/40);
            data[1][i]= 5*Math.cos(i*1.55*2*Math.PI/40);
            data[2][i]= i;
        }
        xunits= yunits= tunits= Units.dimensionless;
        this.view= view;  //0=x, 1=y, 2=t
    }
    
    public static OrbitVectorDataSet create() {
        return xview;
    }
    
    public Datum getDatum(int i) {
        return yunits.createDatum(getDouble(i, yunits));
    }
    
    public double getDouble(int i, Units units) {
        return data[view][i];
    }
    
    public int getInt(int i, Units units) {
        return (int)getDouble( i, units );
    }
    
    public DataSet getPlanarView(String planeID) {
        if ( "x".equals(planeID) ) {
            return xview;
        } else if ("y".equals(planeID) ){
            return yview;
        } else {
            throw new IllegalArgumentException("No such plane");
        }
    }
    
    public String[] getPlaneIds() {
        return new String[] { "x", "y" };
    }
    
    public Object getProperty(String name) {
        return null;
    }
    
    public java.util.Map getProperties() {
        return new java.util.HashMap();
    }
    
    public int getXLength() {
        return data[0].length;        
    }
    
    public Datum getXTagDatum(int i) {
        return tunits.createDatum(getXTagDouble(i,tunits));
    }
    
    public double getXTagDouble(int i, Units units) {        
        return tunits.convertDoubleTo(units,data[2][i]);
    }
    
    public int getXTagInt(int i, Units units) {
        return (int)getXTagDouble(i,tunits);
    }
    
    public Units getXUnits() {
        return tunits;
    }
    
    public Units getYUnits() {
        if ( view==0 ) return xunits; 
        else return yunits; 
        
    }
    
}
