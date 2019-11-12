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

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.das2.util.LoggerManager;

/** All nodes which present query parameters to higher level data readers should
 * inherit from this class.
 * 
 * In addition to the standard 3-phase construction interface, this abstract
 * class adds the ability to have query parameters and provide verification
 * of parameter values.  It can also provide example download URLs
 *
 * @author cwp
 */
abstract class AbstractSrcNode extends AbstractNode implements DasSrcNode
{
	private static final Logger LOGGER = LoggerManager.getLogger( "das2.catalog.abssrc" );
	
	public AbstractSrcNode(DasDirNode parent, String name, List<String> lUrls)
	{
		super(parent, name, lUrls);
		
	}

	@Override
	public boolean isSrc() { return true; }
	
	@Override
	public boolean isDir(){ return false; }
	
	@Override
	public boolean isInfo(){ return false; }
	
	@Override
	public boolean queryVerify(Map<String, String> dQuery) {
		throw new UnsupportedOperationException("Not supported yet.");
	}
	
}
