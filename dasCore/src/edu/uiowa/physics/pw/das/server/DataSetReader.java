/* File: DataSetReader.java
 * Copyright (C) 2002-2003 University of Iowa
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

package edu.uiowa.physics.pw.das.server;

import edu.uiowa.physics.pw.das.dataset.DataSet;
import edu.uiowa.physics.pw.das.util.DasDate;

import java.io.IOException;

/**
 *
 * @author  eew
 */
public interface DataSetReader
{
    public edu.uiowa.physics.pw.das.dataset.DataSet getDataSet(String dsdf, Object params, DasDate start, DasDate end) throws IOException;
    
    public void addDasReaderListener(edu.uiowa.physics.pw.das.event.DasReaderListener listener);
    
    public void removeDasReaderListener(edu.uiowa.physics.pw.das.event.DasReaderListener listener);
}
