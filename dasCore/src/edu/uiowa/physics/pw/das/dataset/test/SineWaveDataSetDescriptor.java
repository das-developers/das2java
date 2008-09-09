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

import org.das2.DasException;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.datum.*;
import org.das2.util.monitor.ProgressMonitor;
import java.util.*;

/**
 *
 * @author  jbf
 */
public class SineWaveDataSetDescriptor extends DataSetDescriptor {
    
    Datum amplitude;    
    Datum period;
    Datum phase;
    
    /** 
     * Creates a new instance of SineWaveDataSetDescriptor with arbitrary phase.
     * @param amplitude the amplitude of the signal.
     * @param period is in the offset units of phase.   
     * 
     */
    public SineWaveDataSetDescriptor( Datum amplitude, Datum period ) {
        this( amplitude, period, null );
    }
    
    /** 
     * Creates a new instance of SineWaveDataSetDescriptor
     * @param amplitude the amplitude of the signal.
     * @param period is in the offset units of phase.   
     * @param phase datum in the same units as period.  null indicates that the phase is arbitrary and will change based on the data request.
     * 
     */
    public SineWaveDataSetDescriptor( Datum amplitude, Datum period, Datum phase ) {
        super(null);
        if ( 0. == period.doubleValue(period.getUnits() ) ) {
            throw new IllegalArgumentException( "period is zero" );
        }
            
        this.amplitude= amplitude;        
        this.period= period;
        this.phase= phase;
    }
    
    public DataSet getDataSetImpl(Datum start, Datum end, Datum resolution, ProgressMonitor monitor) throws DasException {        
        if ( resolution==null ) resolution= end.subtract( start ).divide(1000);
        int nstep= 2 + (int)(end.subtract(start).doubleValue(resolution.getUnits()) / resolution.doubleValue(resolution.getUnits()));        
        int stepSize= 1; /* not sure what this is useful for jbf */
        nstep= nstep / stepSize; 
       
        if ( phase==null ) phase= start;
        
        double[] yvalues= new double[nstep];
        double[] xtags= new double[nstep];
        Units xunits= phase.getUnits();
        Units offsetUnits= period.getUnits();
        Units yunits= amplitude.getUnits();
               
        for ( int i=0; i<nstep; i++ ) {
            Datum x= start.add(resolution.multiply(i*stepSize));            
            double y= amplitude.doubleValue(yunits) * Math.sin( 2 * Math.PI * ( x.subtract(phase).doubleValue(offsetUnits) 
               / period.doubleValue(offsetUnits))) ;
            xtags[i]= x.doubleValue(xunits);
            yvalues[i]= y;
        }
        VectorDataSet result= new DefaultVectorDataSet( xtags, xunits, yvalues, yunits, new HashMap() );
        
        return result;
    }    
    
    public Units getXUnits() {
        return period.getUnits();
    }
    
    public static SineWaveDataSetDescriptor newDataSetDescriptor(Map map) throws DasException {
        String periodStr = (String)map.get("period");
        String xUnitsStr = (String)map.get("xUnits");
        String amplitudeStr = (String)map.get("amplitude");
        String yUnitsStr = (String)map.get("yUnits");
        if (periodStr == null || amplitudeStr == null) {
            throw new DasException("period and amplitude must be specified for SineWaveDataSetDescriptors");
        }
        Units xUnits = xUnitsStr == null ? Units.dimensionless : Units.getByName(xUnitsStr);
        Units yUnits = xUnitsStr == null ? Units.dimensionless : Units.getByName(yUnitsStr);
        try {
            Datum period = xUnits.getOffsetUnits().parse(periodStr);
            Datum amplitude = yUnits.parse(amplitudeStr);
            return new SineWaveDataSetDescriptor(amplitude, period);
        }
        catch (java.text.ParseException pe) {
            throw new DasException(pe.getMessage());
        }
    }
    
}
