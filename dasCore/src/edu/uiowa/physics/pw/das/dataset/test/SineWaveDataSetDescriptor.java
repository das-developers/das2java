/* File: SineWaveDataSetDescriptor.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on October 29, 2003, 11:55 AM by __FULLNAME__ <__EMAIL__>
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

import edu.uiowa.physics.pw.das.DasException;
import edu.uiowa.physics.pw.das.client.*;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.util.DasProgressMonitor;

/**
 *
 * @author  jbf
 */
public class SineWaveDataSetDescriptor extends XMultiYDataSetDescriptor {
    
    Datum amplitude;    
    Datum period;
    Datum phase;
    
    /** Creates a new instance of SineWaveDataSetDescriptor */
    public SineWaveDataSetDescriptor( Datum amplitude, Datum period ) {
        super(null);
        this.amplitude= amplitude;        
        this.period= period;
        this.phase= period.getUnits().createDatum(0); // arbitary by intent        
    }
    
    public DataSet getDataSetImpl(Datum start, Datum end, Datum resolution, DasProgressMonitor monitor) throws DasException {
        int nstep= (int)(end.subtract(start).doubleValue(resolution.getUnits()) / resolution.doubleValue(resolution.getUnits()));        
        int stepSize= 5;
        nstep= nstep / stepSize; 
        
        double[] yvalues= new double[nstep];
        double[] xtags= new double[nstep];
        Units xunits= period.getUnits();
        Units yunits= amplitude.getUnits();
       
        for ( int i=0; i<nstep; i++ ) {
            Datum x= start.add(resolution.multiply(i*stepSize));            
            double y= amplitude.doubleValue(yunits) * Math.sin( 2 * Math.PI * ( x.subtract(phase).doubleValue(xunits) 
               / period.doubleValue(xunits))) ;
            xtags[i]= x.doubleValue(xunits);
            yvalues[i]= y;
        }
        VectorDataSet result= new DefaultVectorDataSet( xtags, xunits, yvalues, yunits, null );
        
        return result;
    }    
    
}
