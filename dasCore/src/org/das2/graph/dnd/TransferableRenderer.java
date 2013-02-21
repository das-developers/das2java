/* File: TransferableRenderer.java
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

package org.das2.graph.dnd;

import org.das2.graph.Renderer;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

/**
 *
 * @author  eew
 */
public class TransferableRenderer implements Transferable {
    
    public static final DataFlavor RENDERER_FLAVOR = localJVMFlavor(org.das2.graph.Renderer.class);

    private static DataFlavor localJVMFlavor(Class cl) {
        try {
			String className = cl.getName();
            String x = DataFlavor.javaJVMLocalObjectMimeType;
            return new DataFlavor(x + ";class=" + className,null,cl.getClassLoader());
        }
        catch (ClassNotFoundException cnfe) {
            throw new RuntimeException(cnfe);
        }
    }
    
    /** The Renderer that this tranferable encapsulates. */
    private Renderer renderer;
    
    /** Creates a new instance of TransferableRenderer */
    public TransferableRenderer(Renderer renderer) {
        this.renderer = renderer;
    }
    
    /** Returns an object which represents the data to be transferred.  The class
     * of the object returned is defined by the representation class of the flavor.
     *
     * @param flavor the requested flavor for the data
     * @see DataFlavor#getRepresentationClass
     * @exception IOException                if the data is no longer available
     *              in the requested flavor.
     * @exception UnsupportedFlavorException if the requested data flavor is
     *              not supported.
     */
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (isDataFlavorSupported(flavor)) {
            if (flavor.equals(RENDERER_FLAVOR)) {
                return renderer;
            }
        }
        throw new UnsupportedFlavorException(flavor);
    }
    
    /** Returns an array of DataFlavor objects indicating the flavors the data
     * can be provided in.  The array should be ordered according to preference
     * for providing the data (from most richly descriptive to least descriptive).
     * @return an array of data flavors in which this data can be transferred
     */
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[] { RENDERER_FLAVOR };
    }
    
    /** Returns whether or not the specified data flavor is supported for
     * this object.
     * @param flavor the requested flavor for the data
     * @return boolean indicating whether or not the data flavor is supported
     */
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.equals(RENDERER_FLAVOR);
    }
    
}
