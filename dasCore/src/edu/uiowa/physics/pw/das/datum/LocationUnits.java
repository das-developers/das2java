/* File: LocationUnits.java
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

package edu.uiowa.physics.pw.das.datum;

/**
 *
 * @author  jbf
 */
public class LocationUnits extends NumberUnits {
    
    Units offsetUnits; 
    
    /** Creates a new instance of LocationUnit */
    public LocationUnits( String id, String description, Units offsetUnits ) {
        super( id, description );
        this.offsetUnits= offsetUnits;
    }
    
    public Units getOffsetUnits() {
        return this.offsetUnits;
    }
    
    public Datum add(Number a, Number b, Units bUnits) {
        if ( bUnits instanceof LocationUnits ) {
            throw new IllegalArgumentException("You can't add "+this+" to "+bUnits+", they both identify a location in a space");
        } else {
            Units offsetUnits= getOffsetUnits();
            if ( bUnits!=offsetUnits) {
                UnitsConverter uc= Units.getConverter( bUnits, offsetUnits );
                b= uc.convert(b);
            }
            return createDatum( add( a, b ) );
        }
    }
    
    public Datum divide(Number a, Number b, Units bUnits) {
        throw new IllegalArgumentException("multiplication of locationUnits");
    }
    
    public Datum multiply(Number a, Number b, Units bUnits) {
        throw new IllegalArgumentException("division of locationUnits");
    }
    
    public Datum subtract( Number a, Number b, Units bUnits) {
        if ( bUnits instanceof LocationUnits ) {
            if ( this != bUnits ) {
                b= bUnits.getConverter(this).convert(b);
            }
            return getOffsetUnits().createDatum(subtract( a, b ));            
        } else {
            if ( bUnits != getOffsetUnits()) {
                b= bUnits.getConverter( getOffsetUnits() ).convert(b);
            }
            return createDatum( subtract( a, b ) );
        }
    }
    
}
