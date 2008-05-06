/* File: TransferableFormComponent.java
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

package edu.uiowa.physics.pw.das.dasml;

//import org.apache.xml.serialize.*;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.StringWriter;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

/**
 *
 * @author  eew
 */
public class TransferableFormComponent implements Transferable {
    
    public static final DataFlavor COMPONENT_FLAVOR = createJVMLocalDataFlavor("edu.uiowa.physics.pw.das.dasml.FormComponent");
    public static final DataFlavor PANEL_FLAVOR = createJVMLocalDataFlavor("edu.uiowa.physics.pw.das.dasml.FormPanel");
    public static final DataFlavor TEXT_FLAVOR = createJVMLocalDataFlavor("edu.uiowa.physics.pw.das.dasml.FormText");
    public static final DataFlavor TEXTFIELD_FLAVOR = createJVMLocalDataFlavor("edu.uiowa.physics.pw.das.dasml.FormTextField");
    public static final DataFlavor BUTTON_FLAVOR = createJVMLocalDataFlavor("edu.uiowa.physics.pw.das.dasml.FormButton");
    public static final DataFlavor CHECKBOX_FLAVOR = createJVMLocalDataFlavor("edu.uiowa.physics.pw.das.dasml.FormCheckBox");
    public static final DataFlavor BUTTONGROUP_FLAVOR = createJVMLocalDataFlavor("edu.uiowa.physics.pw.das.dasml.FormRadioButtonGroup");
    public static final DataFlavor RADIOBUTTON_FLAVOR = createJVMLocalDataFlavor("edu.uiowa.physics.pw.das.dasml.FormRadioButton");
    public static final DataFlavor TAB_FLAVOR = createJVMLocalDataFlavor("edu.uiowa.physics.pw.das.dasml.FormTab");
    public static final DataFlavor CHOICE_FLAVOR = createJVMLocalDataFlavor("edu.uiowa.physics.pw.das.dasml.FormChoice");
    public static final DataFlavor LIST_FLAVOR = createJVMLocalDataFlavor("edu.uiowa.physics.pw.das.dasml.FormList");
    public static final DataFlavor WINDOW_FLAVOR = createJVMLocalDataFlavor("edu.uiowa.physics.pw.das.dasml.FormWindow");
    public static final DataFlavor DASML_FRAGMENT_FLAVOR;
    static {
        try {
            DASML_FRAGMENT_FLAVOR = new DataFlavor("x-text/dasml-fragment;class=java.lang.String");
        }
        catch (ClassNotFoundException cnfe) {
            throw new RuntimeException(cnfe);
        }
    }
    
    private final FormComponent formComponent;
    private final DataFlavor moreSpecificDataFlavor;
    private DataFlavor[] flavorList;
    
    public TransferableFormComponent(FormPanel panel) {
        formComponent = panel;
        moreSpecificDataFlavor = PANEL_FLAVOR;
    }
    
    public TransferableFormComponent(FormText text) {
        formComponent = text;
        moreSpecificDataFlavor = TEXT_FLAVOR;
    }
    
    public TransferableFormComponent(FormTextField textField) {
        formComponent = textField;
        moreSpecificDataFlavor = TEXTFIELD_FLAVOR;
    }
    
    public TransferableFormComponent(FormButton button) {
        formComponent = button;
        moreSpecificDataFlavor = BUTTON_FLAVOR;
    }
    
    public TransferableFormComponent(FormCheckBox checkBox) {
        formComponent = checkBox;
        moreSpecificDataFlavor = CHECKBOX_FLAVOR;
    }
    
    public TransferableFormComponent(FormRadioButtonGroup buttonGroup) {
        formComponent = buttonGroup;
        moreSpecificDataFlavor = BUTTONGROUP_FLAVOR;
    }
    
    public TransferableFormComponent(FormRadioButton radioButton) {
        formComponent = radioButton;
        moreSpecificDataFlavor = RADIOBUTTON_FLAVOR;
    }
    
    public TransferableFormComponent(FormTab form) {
        formComponent = form;
        moreSpecificDataFlavor = TAB_FLAVOR;
    }
    
    public TransferableFormComponent(FormChoice choice) {
        formComponent = choice;
        moreSpecificDataFlavor = CHOICE_FLAVOR;
    }
    
    public TransferableFormComponent(FormList list) {
        formComponent = list;
        moreSpecificDataFlavor = LIST_FLAVOR;
    }
    
    public TransferableFormComponent(FormWindow window) {
        formComponent = window;
        moreSpecificDataFlavor = WINDOW_FLAVOR;
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
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, java.io.IOException {
        if (flavor.equals(moreSpecificDataFlavor) || flavor.equals(COMPONENT_FLAVOR)) {
            return formComponent;
        }
        else if (flavor.equals(DataFlavor.stringFlavor)
            || flavor.equals(DASML_FRAGMENT_FLAVOR)) {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.newDocument();
                document.appendChild(formComponent.getDOMElement(document));
                StringWriter writer = new StringWriter();

				DOMImplementationLS ls = (DOMImplementationLS)
						document.getFeature("LS", "3.0");
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
        throw new UnsupportedFlavorException(flavor);
    }
        
    /** Returns an array of DataFlavor objects indicating the flavors the data
     * can be provided in.  The array should be ordered according to preference
     * for providing the data (from most richly descriptive to least descriptive).
     * @return an array of data flavors in which this data can be transferred
     */
    public DataFlavor[] getTransferDataFlavors() {
        if (flavorList == null) {
            flavorList = new DataFlavor[] {
                moreSpecificDataFlavor,
                COMPONENT_FLAVOR,
                DASML_FRAGMENT_FLAVOR,
                DataFlavor.stringFlavor,
            };
        }
        return flavorList;
    }
    
    /** Returns whether or not the specified data flavor is supported for
     * this object.
     * @param flavor the requested flavor for the data
     * @return boolean indicating whether or not the data flavor is supported
     */
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.equals(COMPONENT_FLAVOR)
            || flavor.equals(moreSpecificDataFlavor)
            || flavor.equals(DataFlavor.stringFlavor);
    }
    
    private static DataFlavor createJVMLocalDataFlavor(String classname) {
        try {
            String mimeType = DataFlavor.javaJVMLocalObjectMimeType
                + ";class="+classname;
            return new DataFlavor(mimeType);
        }
        catch (ClassNotFoundException cnfe) {
            throw new RuntimeException(cnfe);
        }
    }
    
}
