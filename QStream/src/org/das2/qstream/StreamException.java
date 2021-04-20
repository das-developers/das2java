/* File: StreamException.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on February 11, 2004, 11:03 AM
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

package org.das2.qstream;

/**
 * This odd class seems to communication information in two directions.
 * First, in the streamException method of StreamHandler it is used to communicate that
 * something went wrong in the source.  For example, a data file was not found.
 * Second, each of the StreamHandler methods can throe StreamExceptions, so they
 * could communicate that the client had a problem.
 * TODO: this should probably be split up into two exceptions...
 * @author  eew
 */
public class StreamException extends Exception {

    public static final String NO_DATA_IN_INTERVAL= "NoDataInInterval";
    public static final String EMPTY_RESPONSE_FROM_READER= "EmptyResponseFromReader";

    /** Creates a new instance of StreamException */
    public StreamException(String message) {
        super(message);
    }

    public StreamException(Exception cause) {
        super(cause.getMessage());
        initCause(cause);
    }

    public StreamException( String message, Throwable cause ) {
        super(message, cause);
    }
    
}
