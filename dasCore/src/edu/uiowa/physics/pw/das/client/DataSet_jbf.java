/* File: DataSet.java
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

package edu.uiowa.physics.pw.das.client;

import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.datum.Units;

/**
 *
 * @author  eew
 */
public abstract class DataSet_jbf implements java.io.Serializable, edu.uiowa.physics.pw.das.components.PropertyEditor.Editable {
    
    DataSetDescriptor dataSetDescriptor;
    
    protected Datum startTime;
    protected Datum endTime;
    protected Datum resolution;
    
    private Units xUnits;
    
    /** This member will only temporarily be public */
    public String dsdfPath;
    
    private String name="";
    
    public double xSampleWidth;
    
    public DataSet_jbf(DataSetDescriptor dsd) {
        this.dataSetDescriptor= dsd;
        if ( dsd!=null ) {
            this.xUnits= dsd.getXUnits();
        } else {
            this.xUnits= Units.dimensionless;
        }
    }
    
    public DataSet_jbf(DataSetDescriptor dsd, Datum startTime, Datum endTime, Datum resolution) {
        this(dsd);
        this.startTime = startTime;
        this.endTime = endTime;
        this.resolution= resolution;
    }
    
    public DataSet_jbf(Units xUnits) {
        this.xUnits= xUnits;
    }
    
    public DataSet_jbf() {
        this(Units.dimensionless);
    }
    
    public Units getXUnits() {
        return xUnits;
    }
    
    public void setXUnits(Units units) {
        xUnits = units;
    }
    
    public Datum getStartTime() {
        return startTime;
    }
    
    public Datum getEndTime() {
        return endTime;
    }
    
    public void setStartTime(Datum startTime) {
        this.startTime= startTime;
    }
    
    public void setEndTime(Datum endTime) {
        this.endTime= endTime;
    }
    
    public DataSetDescriptor getDataSetDescriptor() {
        return dataSetDescriptor;
    }
    
    public void setDataSetDescriptor(DataSetDescriptor dataSetDescriptor) {
        this.dataSetDescriptor= dataSetDescriptor;
    }
    
    public String getName() {
        return this.name;
    }
    
    public void setName(java.lang.String name) {
        this.name= name;
    }
    
    public int sizeBytes() {
        return -99999;
    }
    
    public Datum getXSampleWidth() {
        Units xUnits= getXUnits();
        if ( xUnits instanceof LocationUnits ) {
            xUnits= ((LocationUnits)xUnits).getOffsetUnits();
        }
        return Datum.create(xSampleWidth,xUnits);
    }
    
    public void setXSampleWidth( Datum datum ) {
        if ( getXUnits() instanceof LocationUnits ) {
            xSampleWidth= datum.doubleValue( ((LocationUnits)getXUnits()).getOffsetUnits() );
        } else {
            xSampleWidth= datum.doubleValue(getXUnits());
        }
    }
}
