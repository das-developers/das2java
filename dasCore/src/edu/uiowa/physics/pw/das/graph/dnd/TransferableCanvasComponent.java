/* File: TransferableCanvasComponent.java
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

package edu.uiowa.physics.pw.das.graph.dnd;

import edu.uiowa.physics.pw.das.graph.*;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author  eew
 */
public class TransferableCanvasComponent implements Transferable {
    
    public static final DataFlavor CANVAS_COMPONENT_FLAVOR = localJVMFlavor("edu.uiowa.physics.pw.das.graph.DasCanvasComponent");
    public static final DataFlavor AXIS_FLAVOR = localJVMFlavor("edu.uiowa.physics.pw.das.graph.DasAxis");
    public static final DataFlavor PLOT_FLAVOR = localJVMFlavor("edu.uiowa.physics.pw.das.graph.DasPlot");
    public static final DataFlavor COLORBAR_FLAVOR = localJVMFlavor("edu.uiowa.physics.pw.das.graph.DasColorBar");
    
    private List flavorList;
    private DasCanvasComponent component;
    
    private static DataFlavor localJVMFlavor(String className) {
        try {
            String x = DataFlavor.javaJVMLocalObjectMimeType;
            return new DataFlavor(x + ";class=" + className);
        }
        catch (ClassNotFoundException cnfe) {
            throw new RuntimeException(cnfe);
        }
    }
    
    public TransferableCanvasComponent(DasAxis axis) {
        flavorList = Arrays.asList(new DataFlavor[]{AXIS_FLAVOR, CANVAS_COMPONENT_FLAVOR, DataFlavor.stringFlavor});
        component = axis;
    }
    
    public TransferableCanvasComponent(DasPlot plot) {
        flavorList = Arrays.asList(new DataFlavor[]{PLOT_FLAVOR, CANVAS_COMPONENT_FLAVOR, DataFlavor.stringFlavor});
        component = plot;
    }
    
    public TransferableCanvasComponent(DasColorBar cb) {
        flavorList = Arrays.asList(new DataFlavor[]{PLOT_FLAVOR, CANVAS_COMPONENT_FLAVOR, DataFlavor.stringFlavor});
        component = cb;
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
            if (flavor.equals(DataFlavor.stringFlavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            else {
                return component;
            }
        }
        else {
            throw new UnsupportedFlavorException(flavor);
        }
    }
    
    /** Returns an array of DataFlavor objects indicating the flavors the data
     * can be provided in.  The array should be ordered according to preference
     * for providing the data (from most richly descriptive to least descriptive).
     * @return an array of data flavors in which this data can be transferred
     */
    public DataFlavor[] getTransferDataFlavors() {
        return (DataFlavor[])flavorList.toArray(new DataFlavor[flavorList.size()]);
    }
    
    /** Returns whether or not the specified data flavor is supported for
     * this object.
     * @param flavor the requested flavor for the data
     * @return boolean indicating whether or not the data flavor is supported
     */
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavorList.contains(flavor);
    }
    
}
