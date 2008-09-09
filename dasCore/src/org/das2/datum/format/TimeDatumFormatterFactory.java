/* File: TimeDatumFormatterFactory.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on September 25, 2003, 3:54 PM
 *      by Edward West <eew@space.physics.uiowa.edu>
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

package org.das2.datum.format;

/**
 *
 * @author  Edward West
 */
public class TimeDatumFormatterFactory extends DatumFormatterFactory {
    
    private static TimeDatumFormatterFactory factory;
    
    /** Creates a new instance of TimeDatumFormatterFactory */
    protected TimeDatumFormatterFactory() {}
    
    public DatumFormatter defaultFormatter() {
        return TimeDatumFormatter.DEFAULT;
    }
    
    public DatumFormatter newFormatter(String format) throws java.text.ParseException {
        return new TimeDatumFormatter(format);
    }
    
    /** Get an instance of this factory. */
    public static TimeDatumFormatterFactory getInstance() {
        //This isn't thread safe, but who cares.  Instances are small and
        //functionally identical.
        if (factory == null) {
            factory = new TimeDatumFormatterFactory();
        }
        return factory;
    }
    
}
