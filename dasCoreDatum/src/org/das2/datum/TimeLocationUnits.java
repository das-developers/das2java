/* File: TimeLocationUnits.java
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

package org.das2.datum;

import org.das2.datum.format.DatumFormatterFactory;
import org.das2.datum.format.TimeDatumFormatterFactory;

/**
 *
 * @author  jbf
 */
public class TimeLocationUnits extends LocationUnits {
    
    /* TimeLocationUnits class is introduced because it is often necessary to
     * easily identify a time quantity, for instance when deciding whether to
     * use a timeAxis or not.  (TimeAxis is no longer a class, but we use a 
     * special tickV for time units.)
     */        
    
    public TimeLocationUnits( String id, String description, Units offsetUnits, Basis basis ) {
        super(id,description,offsetUnits,basis);
        if ( id.equals("us2000") ) {
            vmin= -3.15569088E16;
            vmax= 2.208987072E17;
        } else {
            vmin= Double.NaN;
            vmax= Double.NaN;
        }
    }

    double vmin;
    double vmax;
    
    private Datum fill= createDatum( Double.NaN );

    @Override
    public DatumFormatterFactory getDatumFormatterFactory() {
        return TimeDatumFormatterFactory.getInstance();
    }
        
    @Override
    public Datum parse(String s) throws java.text.ParseException {
        if ( s.length()==0 || s.equals("NaN") ) {
            return fill;
        }
        int [] rr= DatumRangeUtil.parseISO8601(s);
        if ( rr!=null ) {
            return TimeUtil.toDatum(rr,this);
        } else {
            return TimeUtil.toDatum(TimeUtil.parseTime(s),this);
        }
    }

    @Override
    public Datum getFillDatum() {
        return this.fill;
    }

    @Override
    public double getFillDouble() {
        return this.fill.doubleValue(this);
    }
    
    
    public String getTimeZone() {
        return "UT";
    }

    /**
     * we can't calculate these until the converters are registered, so we delay this,
     * accepting the cost of doing a check each time isFill is called.
     * Note we don't bother making this synchronized--it should be safe.
     */
    private void calculateRange() {
        vmin= Units.us2000.convertDoubleTo(this,Units.us2000.vmin); // DANGER: assumes Units.us2000 is initialized before this.
        vmax= Units.us2000.convertDoubleTo(this,Units.us2000.vmax);
    }

    @Override
    public boolean isFill( double value ) {
        if ( Double.isNaN(vmin) ) calculateRange();
        return value<vmin || value>vmax || Double.isNaN(value);
    }

    /**
     * test if the double represents a valid datum in the context of this unit.
     *
     * @return true if the data is not fill.
     */
    @Override
    public boolean isValid( double value ) {
        if ( Double.isNaN(vmin) ) calculateRange();
        return value>=vmin && value<=vmax && !Double.isNaN(value);
    }

    /**
     * return the minimum valid value.  Any value less than this value is invalid.
     * @return the minimum valid value
     */
    public double validMin( ) {
        return vmin;
    }

    /**
     * return the maximum valid value.  Any value greater than this value is invalid.
     * @return the maximum valid value.
     */
    public double validMax() {
        return vmax;
    }
          
}
