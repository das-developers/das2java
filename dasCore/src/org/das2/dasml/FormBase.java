/* File: FormBase.java
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

package org.das2.dasml;

import org.das2.DasApplication;
import edu.uiowa.physics.pw.das.*;
import org.das2.beans.BeansUtil;
import edu.uiowa.physics.pw.das.datum.Datum;
import org.das2.util.DasExceptionHandler;
//import org.apache.xml.serialize.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.MethodDescriptor;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.List;
import org.das2.DasException;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;

/** This class displays a Java form that is generated from an XML Document that is provided as input.
 *
 * @author Edward West
 */
public class FormBase extends JTabbedPane implements FormComponent {
    
    /** The factory object used for creating DOM parsers. */
    private static DocumentBuilderFactory domFactory;
    
    private DasApplication application = DasApplication.getDefaultApplication();
    
    /** static initialization block for this class */
    static {
        domFactory = DocumentBuilderFactory.newInstance();
        URL schemaLocation = FormBase.class.getResource("schema/dasML.xsd");
        if (schemaLocation != null) {
            domFactory.setAttribute(
            "http://apache.org/xml/features/validation/schema-full-checking",
            Boolean.TRUE);
            domFactory.setAttribute(
            "http://apache.org/xml/properties/schema/external-noNamespaceSchemaLocation",
            schemaLocation.toExternalForm());
            domFactory.setValidating(true);
            domFactory.setNamespaceAware(true);
        }
        else {
            //TODO: Report this error, also maybe try to recover.
        }
        domFactory.setCoalescing(true);
        domFactory.setIgnoringElementContentWhitespace(true);
    }
    
    /**Initialization commands*/
    private CommandBlock initBlock;
    
    List windowList = new ArrayList();
    
    private JDesktopPane desktop;
    
    boolean editable;
    
    private boolean onHover = false;

    /** Setup for DnD support */
    {
        DnDSupport dndSupport = new DnDSupport();
        DropTarget dropTarget = new DropTarget(this, dndSupport);
        setDropTarget(dropTarget);
    }
    
    /** Creates a FormBase object
     *
     * @param url A uniform resouce locator pointing to the XML document to parse.
     */
    public FormBase(URL url, ErrorHandler eh, boolean editable) throws IOException, SAXException {
        this(url.openStream(), eh, editable);
    }
    
    public FormBase(InputStream in, ErrorHandler eh, boolean editable) throws IOException, SAXException {
        this(new InputStreamReader(in), eh, editable);
    }
    
    public FormBase(Reader reader, ErrorHandler eh, boolean editable) throws IOException, SAXException {
        this.editable = editable;
        
        try {
            Document document = parseDasML(reader, eh);
            createFormFromTree(document);
            registerComponent();
        }
        catch (ParserConfigurationException pce) {
            throw new IllegalStateException("DOM parser not configured properly: " + pce.getMessage());
        }
        catch (org.das2.DasException de) {
            throw new SAXException(de);
        }
    }
    
    public FormBase(boolean editable) {
        this.editable = editable;
    }
    
    static Document parseDasML(Reader reader, ErrorHandler eh) throws ParserConfigurationException, SAXException, IOException {
        InputSource source = new InputSource();
        source.setCharacterStream(reader);
        DocumentBuilder builder;
        synchronized (domFactory) {
            builder = domFactory.newDocumentBuilder();
        }
        builder.setErrorHandler(eh);
        return builder.parse(source);
    }
    
    /** Sets up the Java form using a DOM document tree.
     *
     * @param doc The DOM document tree.
     */
    private void createFormFromTree(Document doc) throws SAXException {
        try {
            Element das2 = doc.getDocumentElement();
            if (!das2.getTagName().equals("das2"))
                ;//TODO: do some sort of error handling here.
            NodeList children = das2.getChildNodes();
            int childCount = children.getLength();
            for (int index = 0; index < childCount; index++) {
                Node node = children.item(index);
                if (node instanceof Element) {
                    if (node.getNodeName().equals("form")) {
                        FormTab form = new FormTab((Element)node, this);
                        addForm(form);
                    }
                    else if (node.getNodeName().equals("window")) {
                        FormWindow window = new FormWindow((Element)node, this);
                        addWindow(window);
                    }
                    else if (node.getNodeName().equals("init")) {
                        initBlock = processInitElement((Element)node);
                    }
                    else {
                        //TODO: error message.
                    }
                }
                else {
                    //TODO: nothing, only interested in elements.
                }
            }
        }
        catch (DasException dne) {
            throw new SAXException(dne);
        }
        catch (ParsedExpressionException pee) {
            throw new SAXException(pee);
        }
    }
    
    private int getTabInsertionIndex() {
        int index = 0;
        int count = getComponentCount();
        while (index < count && getComponent(index) instanceof FormTab) {
            index++;
        }
        return index;
    }
    
    public void addForm(FormTab form) {
        synchronized (getTreeLock()) {
            if (getEditingMode()) {
                int index = getTabInsertionIndex();
                insertTab(form.getLabel(), null, form, null, index);
            }
            else {
                addTab(form.getLabel(), form);
            }
        }
        form.setEditingMode(getEditingMode());
    }
    
    public void addWindow(FormWindow window) {
        if (window.form != null) {
            window.form.removeWindow(window);
        }
        window.form = this;
        windowList.add(window);
        boolean editingMode = getEditingMode();
        window.setEditingMode(editingMode);
        if (editingMode) {
            if (desktop == null) {
                desktop = new JDesktopPane();
                add(desktop, "Windows");
            }
            window.pack();
            desktop.add(window.getInternalFrame());
        }
        this.firePropertyChange("window", null, window);
    }
    
    public void removeWindow(FormWindow window) {
        if (windowList.contains(window)) {
            windowList.remove(window);
            if (getEditingMode()) {
                desktop.remove(SwingUtilities.getAncestorOfClass(JInternalFrame.class, window));
            }
            firePropertyChange("window", window, null);
        }
    }
    
    /** Process a <code>&lt;action&gt;</code> element.
     *
     * @param element The DOM tree node that represents the element
     */
    private CommandBlock processActionElement(Element element) {
        return new CommandBlock(element, this);
    }
    
    /** Process a <code>&lt;glue&gt;</code> element.
     *
     * @param element The DOM tree node that represents the element
     */
    Component processGlueElement(Element element) {
        String direction = element.getAttribute("direction");
        if (direction.equals("horizontal"))
            return Box.createHorizontalGlue();
        else return Box.createVerticalGlue();
    }
 
    private CommandBlock processInitElement(Element element) throws SAXException {
        return new CommandBlock(element, this);
    }
    
    
    
    /**
     * Writes the XML representation of this form to the specified
     * byte stream
     *
     * @param out the specified byte stream
     */
    public void serialize(OutputStream out) throws IOException {
        try {
            DocumentBuilder builder = domFactory.newDocumentBuilder();
            Document document = builder.newDocument();
            document.appendChild(getDOMElement(document));

			DOMImplementationLS ls = (DOMImplementationLS)
					document.getImplementation().getFeature("LS", "3.0");
			LSOutput output = ls.createLSOutput();
			output.setEncoding("UTF-8");
			output.setByteStream(out);
			ls.createLSSerializer().write(document, output);
			out.close();

			/*
            String method = org.apache.xml.serialize.Method.XML;
            OutputFormat format = new OutputFormat(method, "UTF-8", true);
            format.setLineWidth(0);
            XMLSerializer serializer = new XMLSerializer(out, format);
            serializer.serialize(document);
			 */
        }
        catch (ParserConfigurationException pce) {
            IOException ioe = new IOException(pce.getMessage());
            ioe.initCause(pce);
            throw ioe;
        }
    }
        
    private boolean isValidType(Class type) {
        return type.isPrimitive()
        || type == String.class
        || type == Datum.class
        || edu.uiowa.physics.pw.das.datum.Datum.class.isAssignableFrom(type)
        || Number.class.isAssignableFrom(type);
    }
    
    
    public Object checkValue(String name, Class type, String tag) throws  org.das2.DasPropertyException, org.das2.DasNameException {
        try {
            Object obj = application.getNameContext().get(name);
            if (obj == null) {
                throw new org.das2.DasNameException(name + " must be defined before it is used");
            }
            if (!type.isInstance(obj)) {
                throw new org.das2.DasPropertyException(org.das2.DasPropertyException.TYPE_MISMATCH, name, null);
            }
            return obj;
        }
        catch (InvocationTargetException ite) {
            throw new RuntimeException(ite);
        }
    }
    
    public Object invoke(String name, String[] args) throws org.das2.DasPropertyException, DataFormatException, ParsedExpressionException, InvocationTargetException {
        int lastDot = name.lastIndexOf('.');
        if (lastDot == -1) throw new DataFormatException("No object associated with method name" + name);
        String objectName = name.substring(0, lastDot);
        String methodName = name.substring(lastDot+1);
        org.das2.util.DasDie.println("object name: " + objectName);
        org.das2.util.DasDie.println("method name: " + methodName);
        Object o = application.getNameContext().get(objectName);
        Method method = null;
        try {
            BeanInfo info = BeansUtil.getBeanInfo(o.getClass());
            MethodDescriptor[] methodDescriptors = info.getMethodDescriptors();
            for (int i = 0; i <= methodDescriptors.length; i++) {
                if (i == methodDescriptors.length)
                    throw new org.das2.DasPropertyException(org.das2.DasPropertyException.NOT_DEFINED, methodName, objectName);
                if (!methodDescriptors[i].getName().equals(methodName)) continue;
                //if (methodDescriptors[i].getMethod().getParameterTypes().length != args.length) continue;
                method = methodDescriptors[i].getMethod();
                break;
            }
            Class[] parameterTypes = method.getParameterTypes();
            Object[] argValues = new Object[args.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                argValues[i] = application.getNameContext().parseValue(args[i], parameterTypes[i]);
            }
            return method.invoke(o, argValues);
        }
        catch (IntrospectionException ie) {
            throw new DataFormatException(ie.getMessage());
        }
        catch (InvocationTargetException ite) {
            throw new DataFormatException(ite.getTargetException().getMessage());
        }
        catch (IllegalAccessException iae) {
            throw new DataFormatException(iae.getMessage());
        }
    }
    
    
    public boolean getEditingMode() {
        return editable;
    }
    
    public void setEditingMode(boolean b) {
        if (editable != b) {
            editable = b;
            int componentCount = getComponentCount();
            for (int i = 0; i < componentCount; i++) {
                Component c = getComponent(i);
                if (c instanceof FormTab) {
                    ((FormTab)c).setEditingMode(b);
                }
            }
            if (windowList.size() > 0) {
                if (b) {
                    if (desktop == null) {
                        desktop = new JDesktopPane();
                    }
                    add(desktop, "Windows");
                }
                else {
                    remove(desktop);
                }
                for (Iterator i = windowList.iterator(); i.hasNext();) {
                    FormWindow window = (FormWindow)i.next();
                    window.setEditingMode(b);
                    if (b) {
                        window.pack();
                        JInternalFrame it = window.getInternalFrame();
                        if (it.getParent() != desktop) {
                            desktop.add(it);
                        }
                    }
                }
            }
            revalidate();
            repaint();
            if (b) {
                firePropertyChange("editable", Boolean.FALSE, Boolean.TRUE);
            }
            else {
                firePropertyChange("editable", Boolean.TRUE, Boolean.FALSE);
            }
        }
    }
    
    public FormBase getForm() {
        return this;
    }
    
    public Element getDOMElement(Document document) {
        Element element = document.createElement("das2");
        for (int index = 0; index < getComponentCount(); index++) {
            FormComponent child = (FormComponent)getComponent(index);
            element.appendChild(child.getDOMElement(document));
        }
        if (!editable) {
            for (Iterator i = windowList.iterator(); i.hasNext();) {
                FormComponent child = (FormComponent)i.next();
                element.appendChild(child.getDOMElement(document));
            }
        }
        return element;
    }
    
    /** Paints the component's border.
     * <p>
     * If you override this in a subclass you should not make permanent
     * changes to the passed in <code>Graphics</code>. For example, you
     * should not alter the clip <code>Rectangle</code> or modify the
     * transform. If you need to do these operations you may find it
     * easier to create a new <code>Graphics</code> from the passed in
     * <code>Graphics</code> and manipulate it.
     *
     * @param g  the <code>Graphics</code> context in which to paint
     *
     * @see #paint
     * @see #setBorder
     */
    protected void paintBorder(Graphics g) {
        super.paintBorder(g);
        if (onHover) {
            Graphics2D g2 = (Graphics2D)g.create();
            Stroke thick = new BasicStroke(3.0f);
            g2.setStroke(thick);
            g2.setColor(Color.GRAY);
            g2.drawRect(1, 1, getWidth() - 2, getHeight() - 2);
            g2.dispose();
        }
    }
    
    public org.das2.util.DnDSupport getDnDSupport() {
        return null;
    }
    
    public boolean startDrag(int x, int y, int action, java.awt.event.MouseEvent evt) {
        return false;
    }
    
    public String getDasName() {
        return null;
    }
    
    public List getWindowList() {
        return Collections.unmodifiableList(windowList);
    }
    
    public void setDasName(String name) throws org.das2.DasNameException {
        throw new org.das2.DasNameException();
    }
    
    public void deregisterComponent() {
        for (int index = 0; index < getComponentCount(); index++) {
            Component c = getComponent(index);
            if (c instanceof FormComponent) {
                ((FormComponent)c).deregisterComponent();
            }
        }
        for (Iterator i = windowList.iterator(); i.hasNext();) {
            FormWindow w = (FormWindow)i.next();
            w.deregisterComponent();
        }
    }
    
    public org.das2.DasApplication getDasApplication() {
        return application;
    }
    
    public void registerComponent() throws org.das2.DasException {
        for (int index = 0; index < getComponentCount(); index++) {
            Component c = getComponent(index);
            if (c instanceof FormComponent) {
                ((FormComponent)c).registerComponent();
            }
        }
        for (Iterator i = windowList.iterator(); i.hasNext();) {
            FormWindow w = (FormWindow)i.next();
            w.registerComponent();
        }
    }
    
    private class DnDSupport implements DropTargetListener {
        
        private final Set acceptableFlavors = new HashSet(Arrays.asList(new DataFlavor[] {
            TransferableFormComponent.TAB_FLAVOR,
            TransferableFormComponent.WINDOW_FLAVOR
        }));
        
        /** Called while a drag operation is ongoing, when the mouse pointer enters
         * the operable part of the drop site for the <code>DropTarget</code>
         * registered with this listener.
         *
         * @param dtde the <code>DropTargetDragEvent</code>
         */
        public void dragEnter(DropTargetDragEvent dtde) {
            if (canAccept(dtde.getCurrentDataFlavors())) {
                dtde.acceptDrag(dtde.getSourceActions());
                onHover = true;
                repaint();
            }
        }
        
        /** Called while a drag operation is ongoing, when the mouse pointer has
         * exited the operable part of the drop site for the
         * <code>DropTarget</code> registered with this listener.
         *
         * @param dte the <code>DropTargetEvent</code>
         */
        public void dragExit(DropTargetEvent dte) {
            onHover = false;
            repaint();
        }
        
        /** Called when a drag operation is ongoing, while the mouse pointer is still
         * over the operable part of the drop site for the <code>DropTarget</code>
         * registered with this listener.
         *
         * @param dtde the <code>DropTargetDragEvent</code>
         */
        public void dragOver(DropTargetDragEvent dtde) {
        }
        
        /** Called when the drag operation has terminated with a drop on
         * the operable part of the drop site for the <code>DropTarget</code>
         * registered with this listener.
         * <p>
         * This method is responsible for undertaking
         * the transfer of the data associated with the
         * gesture. The <code>DropTargetDropEvent</code>
         * provides a means to obtain a <code>Transferable</code>
         * object that represents the data object(s) to
         * be transfered.<P>
         * From this method, the <code>DropTargetListener</code>
         * shall accept or reject the drop via the
         * acceptDrop(int dropAction) or rejectDrop() methods of the
         * <code>DropTargetDropEvent</code> parameter.
         * <P>
         * Subsequent to acceptDrop(), but not before,
         * <code>DropTargetDropEvent</code>'s getTransferable()
         * method may be invoked, and data transfer may be
         * performed via the returned <code>Transferable</code>'s
         * getTransferData() method.
         * <P>
         * At the completion of a drop, an implementation
         * of this method is required to signal the success/failure
         * of the drop by passing an appropriate
         * <code>boolean</code> to the <code>DropTargetDropEvent</code>'s
         * dropComplete(boolean success) method.
         * <P>
         * Note: The data transfer should be completed before the call  to the
         * <code>DropTargetDropEvent</code>'s dropComplete(boolean success) method.
         * After that, a call to the getTransferData() method of the
         * <code>Transferable</code> returned by
         * <code>DropTargetDropEvent.getTransferable()</code> is guaranteed to
         * succeed only if the data transfer is local; that is, only if
         * <code>DropTargetDropEvent.isLocalTransfer()</code> returns
         * <code>true</code>. Otherwise, the behavior of the call is
         * implementation-dependent.
         * <P>
         * @param dtde the <code>DropTargetDropEvent</code>
         */
        public void drop(DropTargetDropEvent dtde) {
            boolean success = false;
            if (canAccept(dtde.getCurrentDataFlavors())) {
                Transferable t = dtde.getTransferable();
                if (t.isDataFlavorSupported(TransferableFormComponent.COMPONENT_FLAVOR)) {
                    dtde.acceptDrop(dtde.getDropAction());
                    success = acceptComponent(t);
                }
                else if (t.isDataFlavorSupported(TransferableFormComponent.DASML_FRAGMENT_FLAVOR)) {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    success = acceptFragment(t);
                }
                dtde.dropComplete(success);
            }
            else {
                dtde.rejectDrop();
            }
            onHover = false;
            repaint();
        }
        
        private boolean acceptFragment(Transferable t) {
            boolean success = false;
            try {
                String data = (String)t.getTransferData(TransferableFormComponent.DASML_FRAGMENT_FLAVOR);
                Document document = FormBase.parseDasML(new StringReader(data), null);
                Element root = document.getDocumentElement();
                if (root.getTagName().equals("form")) {
                    FormTab tab = new FormTab(root, FormBase.this);
                    addForm(tab);
                    success = true;
                    revalidate();
                }
                else if (root.getTagName().equals("window")) {
                    FormWindow window = new FormWindow(root, FormBase.this);
                    addWindow(window);
                    success = true;
                    revalidate();
                }
            }
            catch (org.das2.dasml.ParsedExpressionException pee) {
                pee.printStackTrace();
            }

            catch (org.das2.DasException de) {
                de.printStackTrace();
            }            catch (UnsupportedFlavorException ufe) {
                //Allow to fall through.
                //exception is handled by allowing success to remain false
            }
            catch (IOException ioe) {
                DasExceptionHandler.handle(ioe);
                //Allow to fall through.
                //exception is handled by allowing success to remain false
            }
            catch (ParserConfigurationException pce) {
                DasExceptionHandler.handle(pce);
                //Allow to fall through.
                //exception is handled by allowing success to remain false
            }
            catch (SAXException se) {
                DasExceptionHandler.handle(se);
                //Allow to fall through.
                //exception is handled by allowing success to remain false
            }
            return success;
        }
        
        private boolean acceptComponent(Transferable t) {
            boolean success = false;
            try {
                Component c = (Component)t.getTransferData(TransferableFormComponent.COMPONENT_FLAVOR);
                if (c instanceof FormTab) {
                    addForm((FormTab)c);
                    success = true;
                    revalidate();
                }
                else if (c instanceof FormWindow) {
                    addWindow((FormWindow)c);
                    success = true;
                    revalidate();
                }
                else {
                    System.out.println(c);
                }
            }
            catch (UnsupportedFlavorException ufe) {
                //Allow to fall through.
                //exception is handled by allowing success to remain false
            }
            catch (IOException ioe) {
                //Allow to fall through.
                //exception is handled by allowing success to remain false
            }
            return success;
        }
        
        /** Called if the user has modified
         * the current drop gesture.
         * <P>
         * @param dtde the <code>DropTargetDragEvent</code>
         */
        public void dropActionChanged(DropTargetDragEvent dtde) {
            if (onHover) {
                dtde.acceptDrag(dtde.getDropAction());
            }
        }
        
        private boolean canAccept(DataFlavor[] flavors) {
            for (int i = 0; i < flavors.length; i++) {
                if (acceptableFlavors.contains(flavors[i])) {
                    return true;
                }
            }
            return false;
        }
        
    }
    
}
