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

import java.util.Map;

/** Source nodes add extra functions to generic catalog nodes
 *
 * @author cwp
 */
public interface DasSrcNode extends DasNode {

	/** Determine if the given list of query parameters are valid
	 * 
	 * @param params A map of key, value query parameters
	 * @return True if this set of parameters is valid, false otherwise
	 */
	public boolean queryVerify(Map<String, String> dQuery);

	
}
