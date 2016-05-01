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

package org.das2.datum;

/**
 * Units for describing locations, Stevens' Interval Scale.
 * @author  jbf
 */
public class LocationUnits extends NumberUnits {
    
    Units offsetUnits; 
    Basis basis;
    
    /** 
     * Creates a new instance of LocationUnit
     * @param id the identifier.
     * @param description human-consumable label.
     * @param offsetUnits units of offset from the basis.
     * @param basis the reference, such as "2000-01-01T00:00" or the freezing point of water.
     */
    public LocationUnits( String id, String description, Units offsetUnits, Basis basis ) {
        super( id, description );
        this.offsetUnits= offsetUnits;
        this.basis= basis;
    }
    
    /**
     * return the physical units of the basis vector, such as "microseconds" or "degrees"
     * @return
     */
    @Override
    public Units getOffsetUnits() {
        return this.offsetUnits;
    }
    
    /**
     * return the basis for the unit, such as "since 2000-01-01T00:00Z" or "north of Earth's equator"
     * @return
     */
    @Override
    public Basis getBasis() {
        return this.basis;
    }
    
    @Override
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
    
    @Override
    public Datum divide(Number a, Number b, Units bUnits) {
        throw new IllegalArgumentException("division of locationUnits: "+ this.createDatum(a)+" / "+bUnits.createDatum(b) );
    }
    
    @Override
    public Datum multiply(Number a, Number b, Units bUnits) {
        throw new IllegalArgumentException("multiplication of locationUnits"+ this.createDatum(a)+" * "+bUnits.createDatum(b));
    }
    
    @Override
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
