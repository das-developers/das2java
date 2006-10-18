/* File: DasMLValidator.java
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

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.util.*;
import java.util.regex.Pattern;

/**
 * A validator for the dasML language developed for the University of
 * Iowa Space Plasma Wave Group.  This class is used as a pre-processor
 * to (hopefully) provide clear and helpful error messages.
 *
 * Warning:  This class is not thread-safe.  Unexpected results can occur
 *    if multiple threads use an instance of this class concurrently.
 *
 * @author  Edward West
 */
public class DasMLValidator extends DefaultHandler {
    
    public static Pattern INTEGER_PATTERN = Pattern.compile("(0|[1-9][0-9]*)");
    
    public static Pattern WINDOW_POSITION_PATTERN = Pattern.compile("\\((0|[1-9][0-9]*),(0|[1-9][0-9]*)\\)");
    
    public static Pattern FLOAT_PATTERN = Pattern.compile("-?[0-9]*(\\.[0-9]*)?([eE]-?[0-9]+)?");

    /**
     * Instance of the SAXParserFactory that this class uses to create
     * instances of SAXParser.
     */
    private static SAXParserFactory factory;
    
    /**
     * Static initialization block to property initialize factory
     */
    static {
        factory = SAXParserFactory.newInstance();
        factory.setValidating(true);
    }
    
    /**
     * Instance of SAXParser used by this validator
     * to parse documents.
     */
    private SAXParser parser;
    
    /**
     * Instance of ErrorHandler that the error events
     * from the SAXParser are delegated to.  This member is
     * only valid during a call to validate()
     */
    private ErrorHandler errorHandler;
    
    /**
     * The last error encountered by this validator.
     */
    private SAXException lastError;
    
    /**
     * Locator used to locate the position in the
     * XML document that where certain events have taken place.
     */
    private Locator locator;
    
    /**
     * Mapping of 'name' attributes to 'type' of element (element name)
     */
    private Map typeMap;
    
    /**
     * A list of TypeCheck that are to be processed once the whole
     * document is loaded.
     */
    private List typeCheckList;
    
    /** Creates a new instance of DasMLValidator */
    public DasMLValidator() throws ParserConfigurationException, SAXException {
        parser = factory.newSAXParser();
        typeMap = new HashMap();
        typeCheckList = new LinkedList();
    }

    /**
     * Parses and validates a dasML document.  All errors are
     * passed to the ErrorHandler instance specified.  SAXExceptions
     * thrown by the underlying parser are caught and suppressed by
     * this method.  If an application needs access to the errors,
     * an ErrorHandler must be provided.
     *
     * @param source The source of the XML document
     * @param errorHandler The ErrorHandler instance that will receive
     *    error messages from the parser.  This can be null
     * @return true if the document is a valid dasML document.
     * @throws IOException if the there is an error while reading the document.
     */
    public boolean validate(InputSource source, ErrorHandler errorHandler) throws java.io.IOException {
        this.errorHandler = errorHandler;
        if (this == errorHandler) throw new IllegalArgumentException("cannot pass an instance of DasMLValidator to its own validate() method");
        lastError = null;
        try {
            typeMap.clear();
            typeCheckList.clear();
            parser.parse(source, this);
        }
        catch (SAXException se) {
            //Save a reference to the error and return false
            lastError = se;
        }
        
        return lastError == null;
    }

    /**
     * Returns the last error encountered by this validator
     * or null if no error has been found.  This method
     * will only return an error if the last call to
     * validate(InputSource, ErrorHandler) returned false.
     * If an application wishes to have access to warnings
     * and non-fatal errors then an ErrorHandler must be provided.
     */
    public SAXException getLastError() {
        return lastError;
    }
    
    /** Report a fatal XML parsing error.
     *
     * @param e The error information encoded as an exception.
     * @exception org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     * @see org.xml.sax.ErrorHandler#fatalError
     * @see org.xml.sax.SAXParseException
     */
    public void fatalError(SAXParseException e) throws SAXException {
        if (errorHandler != null) {
            errorHandler.fatalError(e);
        }
        throw e;
    }
    
    /** Receive a Locator object for document events.
     *
     * @param locator A locator for all SAX document events.
     * @see org.xml.sax.ContentHandler#setDocumentLocator
     * @see org.xml.sax.Locator
     */
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }
    
    /** Receive notification of a recoverable parser error.
     *
     * @param e The warning information encoded as an exception.
     * @exception org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     * @see org.xml.sax.ErrorHandler#warning
     * @see org.xml.sax.SAXParseException
     */
    public void error(SAXParseException e) throws SAXException {
        if (errorHandler != null) {
            errorHandler.error(e);
        }
        lastError = e;
    }
    
    /** Receive notification of a parser warning.
     *
     * @param e The warning information encoded as an exception.
     * @exception org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     * @see org.xml.sax.ErrorHandler#warning
     * @see org.xml.sax.SAXParseException
     */
    public void warning(SAXParseException e) throws SAXException {
        if (errorHandler != null) {
            errorHandler.warning(e);
        }
        lastError = e;
    }
    
    /** Receive notification of the beginning of the document.
     *
     * @exception org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     * @see org.xml.sax.ContentHandler#startDocument
     */
    public void startDocument() throws SAXException {
    }
    
    /** Receive notification of the start of an element.
     *
     * @param qName The element type name.
     * @param attributes The specified or defaulted attributes.
     * @exception org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     * @see org.xml.sax.ContentHandler#startElement
     */
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        String name = attributes.getValue("name");
        if (name != null) {
            if (typeMap.containsKey(name))
                errorInternal("An element with the name " + name + " already exists.  " +
                              "The values of name attributes must be unique.");
            typeMap.put(name, qName);
        }
        if (qName.equals("window"))
            checkWindow(attributes);
        else if (qName.equals("form"))
            checkForm(attributes);
        else if (qName.equals("textfield"))
            checkTextfield(attributes);
        else if (qName.equals("checkbox"))
            checkCheckbox(attributes);
        else if (qName.equals("if"))
            ;//CHECK EXPRESSION
        else if (qName.equals("elseif"))
            ;//CHECK EXPRESSION
        else if (qName.equals("update")) //KLUDGE - Will be removed once <invoke method="" args=""> is implemented
            typeCheckList.add(new TypeCheck("update",
                                            "target",
                                            "spectrogram",
                                            attributes.getValue("target"),
                                            locator));
        else if (qName.equals("radiobutton"))
            checkRadiobutton(attributes);
        else if (qName.equals("panel"))
            checkPanel(attributes);
        else if (qName.equals("glue"))
            checkGlue(attributes);
        else if (qName.equals("canvas"))
            checkCanvas(attributes);
        else if (qName.equals("row"))
            checkRowColumn("row", attributes);
        else if (qName.equals("column"))
            checkRowColumn("column", attributes);
        else if (qName.equals("spectrogram"))
            checkSpectrogram(attributes);
        else if (qName.equals("xAxis"))
            checkXAxis(attributes);
        else if (qName.equals("yAxis"))
            checkYAxis(attributes);
        else if (qName.equals("zAxis"))
            checkZAxis(attributes);
        else if (qName.equals("axis"))
            checkAxis(attributes);
        else if (qName.equals("timeaxis"))
            checkTimeaxis(attributes);
        else if (qName.equals("attachedaxis"))
            checkAttachedaxis(attributes);
        else if (qName.equals("colorbar"))
            checkColorbar(attributes);
    }
    
    /**
     * Checks the attribute values for a colorbar element
     */
    private void checkColorbar(Attributes attributes) throws SAXException {
        String minimum = attributes.getValue("minimum");
        if (!FLOAT_PATTERN.matcher(minimum).matches() || minimum.charAt(0)=='-')
            errorInternal("The minimum attribute of a colorbar element must be a positive number");
        String maximum = attributes.getValue("maximum");
        if (!FLOAT_PATTERN.matcher(maximum).matches() || maximum.charAt(0)=='-')
            errorInternal("The maximum attribute of a colorbar element must be a positive number");
        String row = attributes.getValue("row");
        if (row != null) {
            typeCheckList.add(new TypeCheck("colorbar", "row", "row", row, locator));
        }
        else if (!insideSpectrogram) {
            errorInternal("The \"row\" attribute of a \"colorbar\" element must be specified"
                          + " if the element is not nested in a \"spectrogram\" element");
        }
        String column = attributes.getValue("column");
        if (column != null) {
            typeCheckList.add(new TypeCheck("colorbar", "column", "column", column, locator));
        }
        else if (!insideSpectrogram) {
            errorInternal("The \"column\" attribute of a \"colorbar\" element must be specified"
                          + " if the element is not nested in a \"spectrogram\" element");
        }
        String log = attributes.getValue("log");
        if (!(log.equals("true") || log.equals("false")))
            errorInternal("The log attribute of a colorbar must be either 'true' or 'false'");
    }
    
    private boolean hasXAxis = false;
    private boolean hasYAxis = false;
    private boolean hasZAxis = false;
    private boolean insideSpectrogram = false;
    
    /**
     * Checks the attribute values for a spectrogram element
     */
    private void checkSpectrogram(Attributes attributes) throws SAXParseException {
        String row = attributes.getValue("row");
        typeCheckList.add(new TypeCheck("spectrogram", "row", "row", row, locator));
        String column = attributes.getValue("column");
        typeCheckList.add(new TypeCheck("spectrogram", "column", "column", column, locator));
        
        String xAxis = attributes.getValue("xAxis");
        if (xAxis != null) {
            typeCheckList.add(new TypeCheck("spectrogram", "xAxis", "axis|timeaxis|attachedaxis", xAxis, locator));
            hasXAxis = true;
        }
        
        String yAxis = attributes.getValue("yAxis");
        if (yAxis != null) {
            typeCheckList.add(new TypeCheck("spectrogram", "yAxis", "axis|timeaxis|attachedaxis", yAxis, locator));
            hasYAxis = true;
        }
        
        String colorbar = attributes.getValue("colorbar");
        if (colorbar != null) {
            typeCheckList.add(new TypeCheck("spectrogram", "colorbar", "colorbar", colorbar, locator));
            hasZAxis = true;
        }
        
        insideSpectrogram = true;
    }
    
    private void endCheckSpectrogram() throws SAXException {
        
        if (!hasXAxis) {
            errorInternal("No xAxis specified.  Spectrograms require an xAxis to be specified");
        }
        if (!hasYAxis) {
            errorInternal("No yAxis specified.  Spectrograms required a yAxis to be specified");
        }
        if (!hasZAxis) {
            errorInternal("No zAxis specified.  Spectrograms required a zAxis to be specified");
        }
        
        hasXAxis = false;
        hasYAxis = false;
        hasZAxis = false;
        insideSpectrogram = false;
    }
    
    private void checkXAxis(Attributes attributes) throws SAXException {
        hasXAxis = true;
    }
    
    private void checkYAxis(Attributes attributes) throws SAXException {
        hasYAxis = true;
    }
    
    private void checkZAxis(Attributes attributes) throws SAXException {
        hasZAxis = true;
    }
    
    /**
     * Checks the attribute values for a attachedaxis element
     */
    private void checkAttachedaxis(Attributes attributes) throws SAXException {
        String ref = attributes.getValue("ref");
        typeCheckList.add(new TypeCheck("attachedaxis", "ref", "axis|timeaxis", ref, locator));
        String row = attributes.getValue("row");
        if (row != null) {
            typeCheckList.add(new TypeCheck("attachedaxis", "row", "row", row, locator));
        }
        else if (!insideSpectrogram) {
            errorInternal("The \"row\" attribute of an \"attachedaxis\" element must be specified"
                          + " if the element is not nested in a \"spectrogram\" element");
        }
        String column = attributes.getValue("column");
        if (column != null) {
            typeCheckList.add(new TypeCheck("attachedaxis", "column", "column", column, locator));
        }
        else if (!insideSpectrogram) {
            errorInternal("The \"column\" attribute of an \"attachedaxis\" element must be specified"
                          + " if the element is not nested in a \"spectrogram\" element");
        }
        String orientation = attributes.getValue("orientation");
        if (!(orientation.equals("horizontal") || orientation.equals("vertical")))
            errorInternal("The orientation attibute of an attachedaxis element must be either 'horizontal' or 'vertical'");
    }
    
    /**
     * Checks the attribute values for a timeaxis element
     */
    private void checkTimeaxis(Attributes attributes) throws SAXException {
        String showTca = attributes.getValue("showTca");
        if (!(showTca.equals("true") || showTca.equals("false")))
            errorInternal("The showTca attribute of a timeaxis element must be either 'true' or 'false'");
        String row = attributes.getValue("row");
        if (row != null) {
            typeCheckList.add(new TypeCheck("timeaxis", "row", "row", row, locator));
        }
        else if (!insideSpectrogram) {
            errorInternal("The \"row\" attribute of a \"timeaxis\" element must be specified"
                          + " if the element is not nested in a \"spectrogram\" element");
        }
        String column = attributes.getValue("column");
        if (column != null) {
            typeCheckList.add(new TypeCheck("timeaxis", "column", "column", column, locator));
        }
        else if (!insideSpectrogram) {
            errorInternal("The \"column\" attribute of a \"timeaxis\" element must be specified"
                          + " if the element is not nested in a \"spectrogram\" element");
        }
        String orientation = attributes.getValue("orientation");
        if (!(orientation.equals("horizontal") || orientation.equals("vertical")))
            errorInternal("The orientation attibute of an axis element must be either 'horizontal' or 'vertical'");
        if (showTca.equals("true") && orientation.equals("vertical"))
            errorInternal("Vertical axes cannot diplay time correlated annotations");
    }

    /**
     * Checks the attribute values for an axis element
     */
    private void checkAxis(Attributes attributes) throws SAXException {
        String log = attributes.getValue("log");
        if (!(log.equals("true") || log.equals("false")))
            errorInternal("The log attribute of an axis element must be either 'true' or 'false'");
        String dataMinimum = attributes.getValue("dataMinimum");
        if (!FLOAT_PATTERN.matcher(dataMinimum).matches())
            errorInternal("The dataMinimum attribute of an axis element must be a valid number'");
        String dataMaximum = attributes.getValue("dataMaximum");
        if (!FLOAT_PATTERN.matcher(dataMinimum).matches())
            errorInternal("The dataMaximum attribute of an axis element must be a valid number'");
        String row = attributes.getValue("row");
        if (row != null) {
            typeCheckList.add(new TypeCheck("axis", "row", "row", row, locator));
        }
        else if (!insideSpectrogram) {
            errorInternal("The \"row\" attribute of an \"axis\" element must be specified"
                          + " if the element is not nested in a \"spectrogram\" element");
        }
        String column = attributes.getValue("column");
        if (column != null) {
            typeCheckList.add(new TypeCheck("axis", "column", "column", column, locator));
        }
        else if (!insideSpectrogram) {
            errorInternal("The \"column\" attribute of an \"axis\" element must be specified"
                          + " if the element is not nested in a \"spectrogram\" element");
        }
        String orientation = attributes.getValue("orientation");
        if (!(orientation.equals("horizontal") || orientation.equals("vertical")))
            errorInternal("The orientation attibute of an axis element must be either 'horizontal' or 'vertical'");
    }
    
    /**
     * Checks the attribute values for a row or column element
     */
    private void checkRowColumn(String tagName, Attributes attributes) throws SAXException {
        String minimum = attributes.getValue("minimum");
        if (!FLOAT_PATTERN.matcher(minimum).matches() || minimum.charAt(0)=='-')
            errorInternal("The minimum attribute of a " + tagName + " element must be a positive number");
        String maximum = attributes.getValue("maximum");
        if (!FLOAT_PATTERN.matcher(maximum).matches() || maximum.charAt(0)=='-')
            errorInternal("The maximum attribute of a " + tagName + " element must be a positive number");
    }
    
    /**
     * Checks the attribute values for a canvas element
     */
    private void checkCanvas(Attributes attributes) throws SAXException {
        String width = attributes.getValue("width");
        if (!INTEGER_PATTERN.matcher(width).matches())
            errorInternal("The width attribute of a canvas element must be a positive integer");
        String height = attributes.getValue("height");
        if (!INTEGER_PATTERN.matcher(height).matches())
            errorInternal("The height attribute of a canvas element must be a positive integer");
    }
    
    /**
     * Checks the attribute values for a glue element
     */
    private void checkGlue(Attributes attributes) throws SAXException {
        String direction = attributes.getValue("direction");
        if (!(direction.equals("horizontal") || direction.equals("vertical")))
            errorInternal("The direction attribute of a glue element must be either 'horizontal' or 'vertical'");
    }
        
    /**
     * Checks the attribute values for a panel element
     */
    private void checkPanel(Attributes attributes) throws SAXException {
        String direction = attributes.getValue("direction");
        if (!(direction.equals("horizontal") || direction.equals("vertical")))
            errorInternal("The direction attribute of a panel element must be either 'horizontal' or 'vertical'");
        String border = attributes.getValue("border");
        if (!(border.equals("true") || border.equals("false")))
            errorInternal("The border attribute of a panel element must be either 'true' or 'false'");
    }
    
    /**
     * Checks the attribute values for a checkbox element
     */
    private void checkRadiobutton(Attributes attributes) throws SAXException {
        String group = attributes.getValue("group");
        typeCheckList.add(new TypeCheck("radiobutton",
                                        "group",
                                        "buttongroup",
                                        group,
                                        locator));
        String selected = attributes.getValue("selected");
        if (!(selected.equals("true") || selected.equals("false")))
            errorInternal("The selected attribute of a radiobutton element must be either 'true' or 'false'");
    }
    
    /**
     * Checks the attribute values for a checkbox element
     */
    private void checkCheckbox(Attributes attributes) throws SAXException {
        String selected = attributes.getValue("selected");
        if (!(selected.equals("true") || selected.equals("false")))
            errorInternal("The selected attribute of a checkbox element must be either 'true' or 'false'");
    }
    
    /**
     * Checks the attribute values for a textfield element
     */
    private void checkTextfield(Attributes attributes) throws SAXException {
        String length = attributes.getValue("length");
        if (!INTEGER_PATTERN.matcher(length).matches())
            errorInternal("The length attribute of textfield elements must be a positive integer");
    }
    
    /**
     * Checks the attribute values for a form element
     */
    private void checkForm(Attributes attributes) throws SAXException {
        String alignment = attributes.getValue("alignment");
        if (!(alignment.equals("left") || alignment.equals("center") || alignment.equals("right")))
            errorInternal("The alignment attribute of a form element must be 'left', 'center', or 'right'");
    }
    
    /**
     * Checks the attribute values for a window element
     */
    private void checkWindow(Attributes attributes) throws SAXException {
        String width = attributes.getValue("width");
        if (!INTEGER_PATTERN.matcher(width).matches())
            errorInternal("The width attribute of a window element must be a positve integer.");
        String height = attributes.getValue("height");
        if (!INTEGER_PATTERN.matcher(height).matches())
            errorInternal("The height attribute of a window element must be a positive integer.");
        String location = attributes.getValue("location");
        if (!WINDOW_POSITION_PATTERN.matcher(location).matches())
            errorInternal("The location attribute of a window element must be a pair of the form (x,y)");
        String visible = attributes.getValue("visible");
        if (!visible.equals("true") && !visible.equals("false"))
            errorInternal("The visible attribute of a window element must be either 'true' or 'false'");
    }
    
    /** Receive notification of the end of the document.
     *
     * @exception org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     * @see org.xml.sax.ContentHandler#endDocument
     */
    public void endDocument() throws SAXException {
        Iterator iterator = typeCheckList.iterator();
        SAXParseException exception = null;
        while (iterator.hasNext()) {
            TypeCheck check = (TypeCheck)iterator.next();
            String type = (String)typeMap.get(check.value);
            if (type == null) {
                exception = new SAXParseException("No element of type \"" + check.type + "\" with attribute " +
                                                  "name=\"" + check.value + "\" exists.",
                                                  check);
                error(exception);
            }
            if (!Pattern.matches(check.type, type)) {
                exception = new SAXParseException("Element '" + check.elementName + "', " +
                                                  "attribute '" + check.attributeName + "' : " +
                                                  type + " expected, but found " + type + 
                                                  " (" + check.value + ")",
                                                  check);
                error(exception);
            }
        }
        if (exception != null) throw exception;
    }
    
    /**
     * This class encapsulates the information necessary
     * to check that the value of an attribute references
     * an element of the property type.
     */
    private static class TypeCheck implements Locator {
        public String elementName;
        public String attributeName;
        public String type;
        public String value;
        private int lineNumber;
        private int columnNumber;
        private String publicId;
        private String systemId;
        public TypeCheck(String elementName, String attributeName, String type, String value, Locator locator) {
            this.elementName = elementName;
            this.attributeName = attributeName;
            this.type = type;
            this.value = value;
            this.lineNumber = locator.getLineNumber();
            this.columnNumber = locator.getColumnNumber();
            this.publicId = locator.getPublicId();
            this.systemId = locator.getSystemId();
        }
        public int getColumnNumber() { return columnNumber; }
        public int getLineNumber() { return lineNumber; }
        public String getPublicId() { return publicId; }
        public String getSystemId() { return systemId; }
        
    }
    
    private void errorInternal(String message) throws SAXException {
        lastError = new SAXParseException(message, locator);
        error((SAXParseException)lastError);
    }
    
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("usage: java edu.uiowa.physics.pw.das.dasml.DasMLValidator <filename>");
            return;
        }
        
        ErrorHandler errorHandler = new ErrorHandler() {
            public void warning(SAXParseException spe) throws SAXException {
                edu.uiowa.physics.pw.das.util.DasDie.println("Line " + spe.getLineNumber() + ", " + spe.getMessage());
            }
            public void error(SAXParseException spe) throws SAXException {
                edu.uiowa.physics.pw.das.util.DasDie.println("Line " + spe.getLineNumber() + ", " + spe.getMessage());
            }
            public void fatalError(SAXParseException spe) throws SAXException {
                edu.uiowa.physics.pw.das.util.DasDie.println("Line " + spe.getLineNumber() + ", " + spe.getMessage());
            }
        };
        
        try {
            String path = new java.io.File(args[0]).getCanonicalPath();
            DasMLValidator validator = new DasMLValidator();
            if (validator.validate(new InputSource("file://" + path), errorHandler)) {
                edu.uiowa.physics.pw.das.util.DasDie.println("No errors");
            }
        }
        catch (ParserConfigurationException pce) {
            edu.uiowa.physics.pw.das.util.DasDie.println(pce.getMessage());
        }
        catch (SAXException se) {
            edu.uiowa.physics.pw.das.util.DasDie.println(se.getMessage());
        }
        catch (java.io.IOException ioe) {
            edu.uiowa.physics.pw.das.util.DasDie.println(ioe.getMessage());
        }
        
    }
    
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equals("spectrogram")) {
            endCheckSpectrogram();
        }
    }
    
}
