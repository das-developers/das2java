/* File: RipplesDataSet.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on November 18, 2003, 12:52 PM by __FULLNAME__ <__EMAIL__>
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
public class RipplesDataSet extends FunctionTableDataSet implements TableDataSet {
    
    double x1,y1,p1;
    double x2,y2,p2;    
    
    public RipplesDataSet( ) {
        this( 2, 3, 1, 13, 15, 2, 30, 30 );
    }
    
    /** Creates a new instance of RipplesDataSetDescriptor */
    public RipplesDataSet( double x1, double y1, double p1, double x2, double y2, double p2, int xlength, int ylength ) {
        this.x1= x1;
        this.y1= y1;
        this.p1= p1;
        this.x2= x2;
        this.y2= y2;
        this.p2= p2;
        super.xtags= xlength;
        super.ytags= ylength;
        /** Creates a new instance of RipplesDataSet */
    }    
    
    public double getDouble(int i, int j, Units units) {
        double x= getXTagDouble(i,xUnits);
        double y= getYTagDouble(0,j,yUnits);
        if (12.<x && x<14.) {
            return units.getFillDouble();
        } else {
            double rad1= Math.sqrt((x-x1)*(x-x1)+(y-y1)*(y-y1));
            double exp1= Math.exp(-rad1/p1)*Math.cos(Math.PI*rad1/p1);
            double rad2= Math.sqrt((x-x2)*(x-x2)+(y-y2)*(y-y2));
            double exp2= Math.exp(-rad2/p2)*Math.cos(Math.PI*rad2/p2);
            double z= (exp1+exp2);
            return zUnits.convertDoubleTo(units,z);
        }
    }
        
}
