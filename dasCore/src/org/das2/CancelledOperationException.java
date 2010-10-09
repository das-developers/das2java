/* File: CancelledOperationException.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on January 20, 2004, 11:33 AM
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

package org.das2;

import java.io.InterruptedIOException;

/**
 * Exception used to indicate the operation was cancelled.
 * Note there is a CancelledOperationException within the util.filesystem
 * package that is used to decouple the util from the rest of das2.
 * 
 * @author  eew
 */
public class CancelledOperationException extends DasException {
    
    /** Creates a new instance of CancelledOperationException */
    public CancelledOperationException() {
        super();
    }
    
    public CancelledOperationException(String message) {
    }
    
    public CancelledOperationException(InterruptedIOException iioe) {
        super(iioe.getMessage());
        initCause(iioe);
    }
    
}
