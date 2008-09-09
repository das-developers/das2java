/* File: LocalFileStandardDataStreamSource.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on January 14, 2004, 3:26 PM
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

package org.das2.stream.test;

import org.das2.DasException;
import org.das2.DasIOException;
import edu.uiowa.physics.pw.das.client.*;
import edu.uiowa.physics.pw.das.stream.*;
import edu.uiowa.physics.pw.das.dataset.DataSetDescriptor;
import edu.uiowa.physics.pw.das.datum.Datum;

import java.io.*;
import java.util.Map;

/**
 *
 * @author  Edward West
 */
public class LocalFileStandardDataStreamSource implements StandardDataStreamSource {
    
    private File file;
    
    /** Creates a new instance of LocalFileStandardDataStreamSource */
    public LocalFileStandardDataStreamSource(File file) {
        this.file = file;
    }
    
    public InputStream getInputStream(StreamDataSetDescriptor dsd, Datum start, Datum end) throws DasException {
        try {
            return new FileInputStream(file);
        }
        catch (IOException ioe) {
            throw new DasIOException(ioe);
        }
    }
    
    public InputStream getReducedInputStream(StreamDataSetDescriptor dsd, Datum start, Datum end, Datum timeResolution) throws DasException {
        return getInputStream(dsd, start, end);
    }
    
    public void reset() {
    }
    
    public static DataSetDescriptor newDataSetDescriptor(Map map) throws DataSetDescriptorNotAvailableException {
        String filename = (String)map.get("file");
        File file = new File(filename);
        StreamDescriptor sd = new StreamDescriptor();
        return new StreamDataSetDescriptor(sd, new LocalFileStandardDataStreamSource(file));
    }
    
}
