/* Copyright (C) 2019 Chris Piker 
 *
 * This file is part of the das2 Core library.
 *
 * das2 is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public Library License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 
 * USA
 */

package org.das2.util.catalog;

/** Exception thrown if a path can not be resolved.
 * 
 * These are handled internally by the catalog package if possible, but some paths just
 * aren't resolvable no matter how many catalog branches are inspected.
 * 
 * @author C. Piker, 2019-11-02
 */
public class DasResolveException extends Exception {
	String path;
	
	// The following constructors are provided by the base class and not repeated here:
	//
	// Exception()
	// Exception(String msg)
	// Exception(Throwable ex)
	// Exception(String msg, Throwable ex)
	/** Construct a das2 catalog resolution exception
	 * @param msg  A general error message
	 * @param sPath The catalog path or sub-path that could not be resolved
	 */
	public DasResolveException(String msg, String sPath){
		super(msg);
		path = sPath;
	}
	
	/** Construct a das2 catalog resolution exception, and attache a cause.
	 * @param msg  A general error message
	 * @param ex   A throwable object that cause the resolution failure
	 * @param sPath The catalog path or sub-path that could not be resolved
	 */
	public DasResolveException(String msg, Throwable ex, String sPath){
		super(msg, ex);
		path = sPath;
	}
}
