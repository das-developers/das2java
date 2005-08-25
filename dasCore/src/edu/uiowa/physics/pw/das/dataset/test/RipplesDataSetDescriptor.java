/* File: RipplesDataSetDescriptor.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on November 14, 2003, 12:16 PM by __FULLNAME__ <__EMAIL__>
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

import edu.uiowa.physics.pw.das.*;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.util.*;

/**
 *
 * @author  jbf
 */
public class RipplesDataSetDescriptor extends DataSetDescriptor {
    
    double x1,y1,p1;
    double x2,y2,p2;
    int nx,ny;
    
    public RipplesDataSetDescriptor( ) {
        this( 14, 17, 10, 20, 60, 15 , 100, 100 );
    }
    
    /** Creates a new instance of RipplesDataSetDescriptor */
    public RipplesDataSetDescriptor( double x1, double y1, double p1, double x2, double y2, double p2 , int nx, int ny ) {
        this.x1= x1;
        this.y1= y1;
        this.p1= p1;
        this.x2= x2;
        this.y2= y2;
        this.p2= p2;
        this.nx= nx;
        this.ny= ny;
    }
    
    public Units getXUnits() {
        return Units.dimensionless;
    }
    
    public edu.uiowa.physics.pw.das.datum.Units getYUnits() {
        return Units.dimensionless;
    }
    
    public edu.uiowa.physics.pw.das.datum.Units getZUnits() {
        return Units.dimensionless;
    }
    
    public DataSet getDataSetImpl(Datum start, Datum end, Datum resolution, DasProgressMonitor monitor) throws DasException {
        
        double[] x= new double[nx];
        double[] y= new double[ny];
        double[][] z= new double[nx][ny];
        
        monitor.setTaskSize(x.length);
        monitor.started();
        
        for (int i=0; i<x.length; i++) {
            x[i]= (float)i;
            for (int j=0; j<y.length; j++) {
                double rad1= Math.sqrt((i-x1)*(i-x1)+(j-y1)*(j-y1));
                double exp1= Math.exp(-rad1/p1)*Math.cos(Math.PI*rad1/p1);
                double rad2= Math.sqrt((i-x2)*(i-x2)+(j-y2)*(j-y2));
                double exp2= Math.exp(-rad2/p2)*Math.cos(Math.PI*rad2/p2);                
                
                z[i][j]= (exp1+exp2);
                if (22<i && i<24) z[i][j]=-1e31f;
            }
            if ( monitor.isCancelled() ) break;
            monitor.setTaskProgress(i);
        }
        for (int j=0; j<y.length; j++) {
            y[j]= (double)j;
        }
        
        /*z[50][50]=-1.0f;
        z[0][0]= 1.0f;
        z[25][25]= -.5f;*/
        
        return DefaultTableDataSet.createSimple( x, y, z );
    }
    
}
