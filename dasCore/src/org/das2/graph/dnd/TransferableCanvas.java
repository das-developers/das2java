/* File: TransferableCanvas.java
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

import org.das2.graph.DasCanvas;
//import org.apache.xml.serialize.Method;
//import org.apache.xml.serialize.OutputFormat;
//import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.StringWriter;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

/**
 *
 * @author  eew
 */
public class TransferableCanvas implements Transferable {
    
    public final static DataFlavor CANVAS_FLAVOR;
    static {
        try {
            String typeStr = DataFlavor.javaJVMLocalObjectMimeType
                + ";class=org.das2.graph.DasCanvas";
            CANVAS_FLAVOR = new DataFlavor(typeStr);
        }
        catch (ClassNotFoundException cnfe) {
            throw new RuntimeException(cnfe);
        }
    }
    
    private DasCanvas canvas;
    
    /** Creates a new instance of TransferableCanvas */
    public TransferableCanvas(DasCanvas canvas) {
        this.canvas = canvas;
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
        if (flavor.equals(CANVAS_FLAVOR)) {
            return canvas;
        }
        else if (flavor.equals(DataFlavor.stringFlavor)) {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.newDocument();
                document.appendChild(canvas.getDOMElement(document));
                StringWriter writer = new StringWriter();

				DOMImplementationLS ls = (DOMImplementationLS)
						document.getImplementation().getFeature("LS", "3.0");
				LSOutput output = ls.createLSOutput();
				output.setEncoding("UTF-8");
				output.setCharacterStream(writer);
				LSSerializer serializer = ls.createLSSerializer();
				serializer.write(document, output);

				/*
                OutputFormat format = new OutputFormat(Method.XML, "UTF-8", true);
                format.setOmitXMLDeclaration(true);
                format.setOmitDocumentType(true);
                XMLSerializer serializer = new XMLSerializer(writer, format);
                serializer.serialize(document);
				 */

                return writer.toString();
            }
            catch (ParserConfigurationException pce) {
                throw new RuntimeException(pce);
            }
        }
        throw new UnsupportedFlavorException(flavor);    }
    
    /** Returns an array of DataFlavor objects indicating the flavors the data
     * can be provided in.  The array should be ordered according to preference
     * for providing the data (from most richly descriptive to least descriptive).
     * @return an array of data flavors in which this data can be transferred
     */
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[] {CANVAS_FLAVOR, DataFlavor.stringFlavor};
    }
    
    /** Returns whether or not the specified data flavor is supported for
     * this object.
     * @param flavor the requested flavor for the data
     * @return boolean indicating whether or not the data flavor is supported
     */
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.equals(CANVAS_FLAVOR) || flavor.equals(DataFlavor.stringFlavor);
    }
    
}
