/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.dasml;

import java.awt.Component;
import java.awt.Dimension;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.das2.DasApplication;
import org.das2.DasException;
import org.das2.DasNameException;
import org.das2.DasPropertyException;
import org.das2.NameContext;
import org.das2.dataset.DataSetDescriptor;
import org.das2.datum.Datum;
import org.das2.datum.TimeUtil;
import org.das2.datum.Units;
import org.das2.graph.DasAxis;
import org.das2.graph.DasCanvas;
import org.das2.graph.DasCanvasComponent;
import org.das2.graph.DasColorBar;
import org.das2.graph.DasColumn;
import org.das2.graph.DasDevicePosition;
import org.das2.graph.DasLabelAxis;
import org.das2.graph.DasPlot;
import org.das2.graph.DasRow;
import org.das2.graph.DefaultPlotSymbol;
import org.das2.graph.PlotSymbol;
import org.das2.graph.Psym;
import org.das2.graph.Renderer;
import org.das2.graph.SeriesRenderer;
import org.das2.graph.SpectrogramRenderer;
import org.das2.graph.StackedHistogramRenderer;
import org.das2.graph.SymColor;
import org.das2.graph.SymbolLineRenderer;
import org.das2.system.DasLogger;
import org.das2.util.DasExceptionHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Introduced to factor out all the dasml stuff from the graph package.
 * @author jbf
 */
public class Processor {
    /** Process an <code>&lt;axis&gt;</code> element.
     *
     * @param element The DOM tree node that represents the element
     */
    static DasAxis processAxisElement(Element element, FormBase form) throws DasPropertyException, DasNameException, ParseException {
        String name = element.getAttribute("name");
        boolean log = element.getAttribute(DasAxis.PROP_LOG).equals("true");
        Datum dataMinimum;
        Datum dataMaximum;
        if ("TIME".equals(element.getAttribute("units"))) {
            String min = element.getAttribute("dataMinimum");
            String max = element.getAttribute("dataMaximum");
            dataMinimum = (min == null || min.equals("") ? TimeUtil.create("1979-02-26") : TimeUtil.create(min));
            dataMaximum = (max == null || max.equals("") ? TimeUtil.create("1979-02-27") : TimeUtil.create(max));
        } else {
            String min = element.getAttribute("dataMinimum");
            String max = element.getAttribute("dataMaximum");
            dataMinimum = (min == null || min.equals("") ? Datum.create(1.0) : Datum.create(Double.parseDouble(min)));
            dataMaximum = (max == null || max.equals("") ? Datum.create(10.0) : Datum.create(Double.parseDouble(max)));
        }
        int orientation = parseOrientationString(element.getAttribute("orientation"));
        DasAxis axis = new DasAxis(dataMinimum, dataMaximum, orientation, log);
        String rowString = element.getAttribute("row");
        if (!rowString.equals("")) {
            DasRow row = (DasRow) form.checkValue(rowString, DasRow.class, "<row>");
            axis.setRow(row);
        }
        String columnString = element.getAttribute("column");
        if (!columnString.equals("")) {
            DasColumn column = (DasColumn) form.checkValue(columnString, DasColumn.class, "<column>");
            axis.setColumn(column);
        }

        axis.setLabel(element.getAttribute(DasAxis.PROP_LABEL));
        axis.setOppositeAxisVisible(!element.getAttribute(DasAxis.PROP_OPPOSITE_AXIS_VISIBLE).equals("false"));
        axis.setTickLabelsVisible(!element.getAttribute("tickLabelsVisible").equals("false"));

        axis.setDasName(name);
        DasApplication app = form.getDasApplication();
        NameContext nc = app.getNameContext();
        nc.put(name, axis);

        return axis;
    }

    /** TODO
     * @param i
     * @return
     */
    protected static String orientationToString(int i) {
        switch (i) {
            case DasAxis.TOP:
                return "top";
            case DasAxis.BOTTOM:
                return "bottom";
            case DasAxis.LEFT:
                return "left";
            case DasAxis.RIGHT:
                return "right";
            default:
                throw new IllegalStateException("invalid orienation: " + i);
        }
    }

    /** TODO
     * @param orientationString
     * @return
     */
    protected static int parseOrientationString(String orientationString) {
        if (orientationString.equals("horizontal")) {
            return DasAxis.HORIZONTAL;
        } else if (orientationString.equals("vertical")) {
            return DasAxis.VERTICAL;
        } else if (orientationString.equals("left")) {
            return DasAxis.LEFT;
        } else if (orientationString.equals("right")) {
            return DasAxis.RIGHT;
        } else if (orientationString.equals("top")) {
            return DasAxis.TOP;
        } else if (orientationString.equals("bottom")) {
            return DasAxis.BOTTOM;
        } else {
            throw new IllegalArgumentException("Invalid orientation: " + orientationString);
        }
    }

    /** TODO
     * @param document
     * @return
     */
    public static Element getDOMElement( DasAxis axis, Document document) {
        Element element;
        if (axis.isAttached()) {
            element = document.createElement("attachedaxis");
        } else {
            element = document.createElement("axis");
        }
        if (axis.isAttached()) {
            element.setAttribute("ref", axis.getMasterAxis().getDasName());
        } else {
            String minimumStr = axis.getDataMinimum().toString();
            element.setAttribute("dataMinimum", minimumStr);
            String maximumStr = axis.getDataMaximum().toString();
            element.setAttribute("dataMaximum", maximumStr);
        }

        element.setAttribute("name", axis.getDasName());
        element.setAttribute("row", axis.getRow().getDasName());
        element.setAttribute("column", axis.getColumn().getDasName());

        element.setAttribute(DasAxis.PROP_LABEL, axis.getLabel());
        element.setAttribute(DasAxis.PROP_LOG, Boolean.toString(axis.isLog()));
        element.setAttribute("tickLabelsVisible", Boolean.toString(axis.isTickLabelsVisible()));
        element.setAttribute(DasAxis.PROP_OPPOSITE_AXIS_VISIBLE, Boolean.toString(axis.isOppositeAxisVisible()));
        element.setAttribute("animated", Boolean.toString(axis.isAnimated()));
        element.setAttribute("orientation", orientationToString(axis.getOrientation()));

        return element;
    }

    static DasAxis processTimeaxisElement(Element element, FormBase form) throws org.das2.DasPropertyException, org.das2.DasNameException, DasException, java.text.ParseException {
        String name = element.getAttribute("name");
        Datum timeMinimum = TimeUtil.create(element.getAttribute("timeMinimum"));
        Datum timeMaximum = TimeUtil.create(element.getAttribute("timeMaximum"));
        int orientation = parseOrientationString(element.getAttribute("orientation"));

        DasAxis timeaxis = new DasAxis(timeMinimum, timeMaximum, orientation);

        String rowString = element.getAttribute("row");
        if (!rowString.equals("")) {
            DasRow row = (DasRow) form.checkValue(rowString, DasRow.class, "<row>");
            timeaxis.setRow(row);
        }
        String columnString = element.getAttribute("column");
        if (!columnString.equals("")) {
            DasColumn column = (DasColumn) form.checkValue(columnString, DasColumn.class, "<column>");
            timeaxis.setColumn(column);
        }

        timeaxis.setDataPath(element.getAttribute("dataPath"));
        timeaxis.setDrawTca(element.getAttribute("showTca").equals("true"));
        timeaxis.setLabel(element.getAttribute(DasAxis.PROP_LABEL));
        timeaxis.setOppositeAxisVisible(!element.getAttribute(DasAxis.PROP_OPPOSITE_AXIS_VISIBLE).equals("false"));
        timeaxis.setTickLabelsVisible(!element.getAttribute("tickLabelsVisible").equals("false"));

        timeaxis.setDasName(name);
        DasApplication app = form.getDasApplication();
        NameContext nc = app.getNameContext();
        nc.put(name, timeaxis);

        return timeaxis;
    }

    /** Process a <code>&lt;attachedaxis&gt;</code> element.
     *
     * @param element The DOM tree node that represents the element
     */
    static DasAxis processAttachedaxisElement(Element element, FormBase form) throws DasPropertyException, DasNameException {
        String name = element.getAttribute("name");
        DasAxis ref = (DasAxis) form.checkValue(element.getAttribute("ref"), DasAxis.class, "<attachedaxis>");
        int orientation = (element.getAttribute("orientation").equals("horizontal") ?  DasAxis.HORIZONTAL : DasAxis.VERTICAL);

        DasAxis axis = ref.createAttachedAxis(orientation);

        String rowString = element.getAttribute("row");
        if (!rowString.equals("")) {
            DasRow row = (DasRow) form.checkValue(rowString, DasRow.class, "<row>");
            axis.setRow(row);
        }
        String columnString = element.getAttribute("column");
        if (!columnString.equals("")) {
            DasColumn column = (DasColumn) form.checkValue(columnString, DasColumn.class, "<column>");
            axis.setColumn(column);
        }

        axis.setDataPath(element.getAttribute("dataPath"));
        axis.setDrawTca(element.getAttribute("showTca").equals("true"));
        axis.setLabel(element.getAttribute(DasAxis.PROP_LABEL));
        axis.setOppositeAxisVisible(!element.getAttribute(DasAxis.PROP_OPPOSITE_AXIS_VISIBLE).equals("false"));
        axis.setTickLabelsVisible(!element.getAttribute("tickLabelsVisible").equals("false"));

        axis.setDasName(name);
        DasApplication app = form.getDasApplication();
        NameContext nc = app.getNameContext();
        nc.put(name, axis);

        return axis;
    }

    /** TODO
     * @param name
     * @return
     */
    public static DasAxis createNamedAxis(String name) {
        DasAxis axis = new DasAxis(Datum.create(1.0, Units.dimensionless), Datum.create(10.0, Units.dimensionless), DasAxis.HORIZONTAL);
        if (name == null) {
            name = "axis_" + Integer.toHexString(System.identityHashCode(axis));
        }
        try {
            axis.setDasName(name);
        } catch (DasNameException dne) {
            DasExceptionHandler.handle(dne);
        }
        return axis;
    }

    /** TODO
     * @return
     * @param document
     */
    public static Element getDOMElement( DasCanvas canvas, Document document) {
        Element element = document.createElement("canvas");
        Dimension size = canvas.getPreferredSize();
        element.setAttribute("name", canvas.getDasName());
        element.setAttribute("width", Integer.toString(size.width));
        element.setAttribute("height", Integer.toString(size.height));

        for (int index = 0; index < canvas.devicePositionList().size(); index++) {
            Object obj = canvas.devicePositionList().get(index);
            if (obj instanceof DasRow) {
                DasRow row = (DasRow) obj;
                element.appendChild(getDOMElement(row,document));
            } else if (obj instanceof DasColumn) {
                DasColumn column = (DasColumn) obj;
                element.appendChild(getDOMElement(column,document));
            }
        }

        Component[] components = canvas.getCanvasComponents();
        Map elementMap = new LinkedHashMap();

        //THREE PASS ALGORITHM.
        //1.  Process all DasAxis components.
        //    Add all <axis>, <timeaxis>, <attachedaxis> elements to elementList.
        //2.  Process all DasColorBar components.
        //    Remove all <axis> elements that correspond to axis property of colorbars.
        //    Add all <colorbar> elements to elementList.
        //3.  Process all DasSpectrogramPlot and DasPlot components.
        //    Remove all <axis>, <attachedaxis>, <timeaxis>, and <colorbar> elements
        //        that correspond to xAxis, yAxis, and colorbar properties of
        //        plots spectrograms and spectrogram renderers.
        //    Add all <plot> <spectrogram> elements to elementList.

        for (int index = 0; index < components.length; index++) {
            if (components[index] instanceof DasAxis) {
                DasAxis axis = (DasAxis) components[index];
                elementMap.put(axis.getDasName(), getDOMElement( axis,document));
            }
        }
        for (int index = 0; index < components.length; index++) {
            if (components[index] instanceof DasColorBar) {
                DasColorBar colorbar = (DasColorBar) components[index];
                elementMap.put(colorbar.getDasName(), getDOMElement(colorbar,document));
            }
        }
        for (int index = 0; index < components.length; index++) {
            if (components[index] instanceof DasPlot) {
                DasPlot plot = (DasPlot) components[index];
                elementMap.remove(plot.getXAxis().getDasName());
                elementMap.remove(plot.getYAxis().getDasName());
                Renderer[] renderers = plot.getRenderers();
                for (int i = 0; i < renderers.length; i++) {
                    if (renderers[i] instanceof SpectrogramRenderer) {
                        SpectrogramRenderer spectrogram = (SpectrogramRenderer) renderers[i];
                        elementMap.remove(spectrogram.getColorBar().getDasName());
                    }
                }
                elementMap.put(plot.getDasName(), getDOMElement(plot,document));
            }
        }

        for (Iterator iterator = elementMap.values().iterator(); iterator.hasNext();) {
            Element e = (Element) iterator.next();
            if (e != null) {
                element.appendChild(e);
            }
        }
        return element;
    }

    /** Process a <code>&lt;canvas&gt;</code> element.
     *
     * @param form
     * @param element The DOM tree node that represents the element
     * @throws DasPropertyException
     * @throws DasNameException
     * @throws ParsedExpressionException
     * @return
     */
    public static DasCanvas processCanvasElement(Element element, FormBase form)
            throws DasPropertyException, DasNameException, DasException, ParsedExpressionException, java.text.ParseException {
        try {
            Logger log = DasLogger.getLogger(DasLogger.DASML_LOG);

            String name = element.getAttribute("name");
            int width = Integer.parseInt(element.getAttribute("width"));
            int height = Integer.parseInt(element.getAttribute("height"));

            DasApplication app = form.getDasApplication();
            NameContext nc = app.getNameContext();

            DasCanvas canvas = new DasCanvas(width, height);

            NodeList children = element.getChildNodes();
            int childCount = children.getLength();
            for (int index = 0; index < childCount; index++) {
                Node node = children.item(index);
                log.fine("node=" + node.getNodeName());
                if (node instanceof Element) {
                    String tagName = node.getNodeName();
                    if (tagName.equals("row")) {
                        DasRow row = processRowElement((Element) node, canvas, form);
                    } else if (tagName.equals("column")) {
                        DasColumn column = processColumnElement((Element) node, canvas, form);
                    } else if (tagName.equals("axis")) {
                        DasAxis axis = processAxisElement((Element) node, form);
                        canvas.add(axis);
                    } else if (tagName.equals("timeaxis")) {
                        DasAxis timeaxis = processTimeaxisElement((Element) node, form);
                        canvas.add(timeaxis);
                    } else if (tagName.equals("attachedaxis")) {
                        DasAxis attachedaxis = processAttachedaxisElement((Element) node, form);
                        canvas.add(attachedaxis);
                    } else if (tagName.equals("colorbar")) {
                        DasColorBar colorbar = processColorbarElement((Element) node, form);
                        canvas.add(colorbar);
                    } else if (tagName.equals("plot")) {
                        DasPlot plot = processPlotElement((Element) node, form);
                        canvas.add(plot);
                    } else if (tagName.equals("spectrogram")) {
                        DasPlot plot = processPlotElement((Element) node, form);
                        canvas.add(plot);
                    }

                }
            }
            canvas.setDasName(name);
            nc.put(name, canvas);

            return canvas;
        } catch (org.das2.DasPropertyException dpe) {
            if (!element.getAttribute("name").equals("")) {
                dpe.setObjectName(element.getAttribute("name"));
            }
            throw dpe;
        }
    }

    /** Process a <code>&lt;row&gt;</code> element.
     *
     * @param element The DOM tree node that represents the element
     */
    static DasRow processRowElement(Element element, DasCanvas canvas, FormBase form) throws DasException {
        String name = element.getAttribute("name");
        double minimum = Double.parseDouble(element.getAttribute("minimum"));
        double maximum = Double.parseDouble(element.getAttribute("maximum"));
        DasRow row =  new DasRow(canvas, minimum, maximum);
        row.setDasName(name);
        DasApplication app = form.getDasApplication();
        NameContext nc = app.getNameContext();
        nc.put(name, row);
        return row;
    }

    public static Element getDOMElement( DasRow row, Document document) {
        Element element = document.createElement("row");
        element.setAttribute("name", row.getDasName());
        element.setAttribute("minimum", Double.toString(row.getMinimum()));
        element.setAttribute("maximum", Double.toString(row.getMaximum()));
        return element;
    }

    /** Process a <code>&lt;column7gt;</code> element.
     *
     * @param element The DOM tree node that represents the element
     */
    static DasColumn processColumnElement(Element element, DasCanvas canvas, FormBase form) throws DasException {
        String name = element.getAttribute("name");
        double minimum
                = Double.parseDouble(element.getAttribute("minimum"));
        double maximum
                = Double.parseDouble(element.getAttribute("maximum"));
        DasColumn column = new DasColumn(canvas, minimum, maximum);
        column.setDasName(name);
        DasApplication app = form.getDasApplication();
        NameContext nc = app.getNameContext();
        nc.put(name, column);
        return column;
    }

    public static Element getDOMElement(DasColumn column, Document document) {
        Element element = document.createElement("column");
        element.setAttribute("name", column.getDasName());
        element.setAttribute("minimum", Double.toString(column.getMinimum()));
        element.setAttribute("maximum", Double.toString(column.getMaximum()));
        return element;
    }

    public static SpectrogramRenderer processSpectrogramElement(
            Element element, DasPlot parent, FormBase form) throws DasPropertyException, DasNameException, ParseException {
        String dataSetID = element.getAttribute("dataSetID");
        DasColorBar colorbar = null;

        NodeList children = element.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node node = children.item(index);
            if (node instanceof Element && node.getNodeName().equals("zAxis")) {
                colorbar = processZAxisElement((Element) node, form);
            }
        }

        if (colorbar == null) {
            try {
                colorbar = (DasColorBar) form.checkValue(element.getAttribute("colorbar"), DasColorBar.class, "<colorbar>");
            } catch (DasPropertyException dpe) {
                dpe.setPropertyName("colorbar");
                throw dpe;
            }
        }

        SpectrogramRenderer renderer = new SpectrogramRenderer( null, colorbar);
        parent.addRenderer(renderer);
        try {
            renderer.setDataSetID(dataSetID);
        } catch (DasException de) {
            DasExceptionHandler.handle(de);
        }
        return renderer;
    }

    private static DasColorBar processZAxisElement(Element element, FormBase form) throws DasPropertyException, DasNameException, ParseException {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element) {
                if (node.getNodeName().equals("colorbar")) {
                    return processColorbarElement((Element) node, form);
                }
            }
        }
        return null;
    }


    /** Process a <code>&lt;colorbar&gt;</code> element.
     *
     * @param element The DOM tree node that represents the element
     */
    static DasColorBar processColorbarElement(Element element, FormBase form) throws  org.das2.DasPropertyException,org.das2.DasNameException, java.text.ParseException {
        String name = element.getAttribute("name");
        boolean log = element.getAttribute("log").equals("true");
        String unitStr = element.getAttribute("units");
        if (unitStr == null) {
            unitStr = "";
        }
        Datum dataMinimum;
        Datum dataMaximum;
        if (unitStr.equals("TIME")) {
            String min = element.getAttribute("dataMinimum");
            String max = element.getAttribute("dataMaximum");
            dataMinimum = (min == null || min.equals("") ? TimeUtil.create("1979-02-26") : TimeUtil.create(min));
            dataMaximum = (max == null || max.equals("") ? TimeUtil.create("1979-02-27") : TimeUtil.create(max));
        } else {
            Units units = Units.getByName(unitStr);
            String min = element.getAttribute("dataMinimum");
            String max = element.getAttribute("dataMaximum");
            dataMinimum = (min == null || min.equals("") ? Datum.create(1.0, units) : Datum.create(Double.parseDouble(min), units));
            dataMaximum = (max == null || max.equals("") ? Datum.create(10.0, units) : Datum.create(Double.parseDouble(max), units));
        }
        int orientation = parseOrientationString(element.getAttribute("orientation"));

        DasColorBar cb = new DasColorBar(dataMinimum, dataMaximum, orientation, log);

        String rowString = element.getAttribute("row");
        if (!rowString.equals("")) {
            DasRow row = (DasRow)form.checkValue(rowString, DasRow.class, "<row>");
            cb.setRow(row);
        }
        String columnString = element.getAttribute("column");
        if (!columnString.equals("")) {
            DasColumn column = (DasColumn)form.checkValue(columnString, DasColumn.class, "<column>");
            cb.setColumn(column);
        }

        cb.setLabel(element.getAttribute("label"));
        cb.setOppositeAxisVisible(!element.getAttribute("oppositeAxisVisible").equals("false"));
        cb.setTickLabelsVisible(!element.getAttribute("tickLabelsVisible").equals("false"));
        cb.setType(DasColorBar.Type.parse(element.getAttribute(DasColorBar.PROPERTY_TYPE)));

        cb.setDasName(name);
        DasApplication app = form.getDasApplication();
        NameContext nc = app.getNameContext();
        nc.put(name, cb);

        return cb;
    }

    public static Element getDOMElement( DasColorBar colorbar, Document document) {
        Element element = document.createElement("colorbar");
        String minimumStr = colorbar.getDataMinimum().toString();
        element.setAttribute("dataMinimum", minimumStr);
        String maximumStr = colorbar.getDataMaximum().toString();
        element.setAttribute("dataMaximum", maximumStr);

        element.setAttribute("name", colorbar.getDasName());
        element.setAttribute("row", colorbar.getRow().getDasName());
        element.setAttribute("column", colorbar.getColumn().getDasName());

        element.setAttribute("label", colorbar.getLabel());
        element.setAttribute("log", Boolean.toString(colorbar.isLog()));
        element.setAttribute("tickLabelsVisible", Boolean.toString(colorbar.isTickLabelsVisible()));
        element.setAttribute("oppositeAxisVisible", Boolean.toString(colorbar.isOppositeAxisVisible()));
        element.setAttribute("animated", Boolean.toString(colorbar.isAnimated()));
        element.setAttribute("orientation", orientationToString(colorbar.getOrientation()));
        element.setAttribute(DasColorBar.PROPERTY_TYPE, colorbar.getType().toString());

        return element;
    }

    public static DasColorBar createNamedColorBar(String name) {
        DasColorBar cb = new DasColorBar(Datum.create(1.0, Units.dimensionless), Datum.create(10.0, Units.dimensionless), false);
        if (name == null) {
            name = "colorbar_" + Integer.toHexString(System.identityHashCode(cb));
        }
        try {
            cb.setDasName(name);
        } catch (org.das2.DasNameException dne) {
            org.das2.util.DasExceptionHandler.handle(dne);
        }
        return cb;
    }
    
    public static DasPlot processPlotElement(Element element, FormBase form) throws org.das2.DasPropertyException, org.das2.DasNameException, DasException, java.text.ParseException {
        String name = element.getAttribute("name");

        DasRow row = (DasRow) form.checkValue(element.getAttribute("row"), DasRow.class, "<row>");
        DasColumn column = (DasColumn) form.checkValue(element.getAttribute("column"), DasColumn.class, "<column>");

        DasAxis xAxis = null;
        DasAxis yAxis = null;
        DasColorBar colorbar = null;

        //Get the axes
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element) {
                if (node.getNodeName().equals("xAxis")) {
                    xAxis = processXAxisElement((Element) node, row, column, form);
                } else if (node.getNodeName().equals("yAxis")) {
                    yAxis = processYAxisElement((Element) node, row, column, form);
                } else if (node.getNodeName().equals("zAxis")) {
                    colorbar = processZAxisElement((Element) node, row, column, form);
                }

            }
        }

        if (xAxis == null) {
            xAxis = (DasAxis) form.checkValue(element.getAttribute("xAxis"), DasAxis.class, "<axis> or <timeaxis>");
        }
        if (yAxis == null) {
            yAxis = (DasAxis) form.checkValue(element.getAttribute("yAxis"), DasAxis.class, "<axis> or <timeaxis>");
        }

        DasPlot plot = new DasPlot(xAxis, yAxis);

        if (element.getNodeName().equals("spectrogram")) {
            SpectrogramRenderer rend = new SpectrogramRenderer(null, colorbar);
            plot.addRenderer(rend);
        }

        plot.setTitle(element.getAttribute("title"));
        plot.setDasName(name);
        plot.setRow(row);
        plot.setColumn(column);
        DasApplication app = form.getDasApplication();
        NameContext nc = app.getNameContext();
        nc.put(name, plot);

        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element) {
                if (node.getNodeName().equals("renderers")) {
                    processRenderersElement((Element) node, plot, form);
                }
            }
        }

        return plot;
    }

    private static DasAxis processXAxisElement(Element element, DasRow row, DasColumn column, FormBase form) throws org.das2.DasPropertyException, org.das2.DasNameException, DasException, java.text.ParseException {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element) {
                Element e = (Element) node;
                if (node.getNodeName().equals("axis")) {
                    DasAxis axis = processAxisElement(e, form);
                    if (!axis.isHorizontal()) {
                        axis.setOrientation(DasAxis.HORIZONTAL);
                    }
                    return axis;
                } else if (node.getNodeName().equals("timeaxis")) {
                    DasAxis axis = processTimeaxisElement(e, form);
                    if (!axis.isHorizontal()) {
                        axis.setOrientation(DasAxis.HORIZONTAL);
                    }
                    return axis;
                } else if (node.getNodeName().equals("attachedaxis")) {
                    DasAxis axis = processAttachedaxisElement(e, form);
                    if (!axis.isHorizontal()) {
                        axis.setOrientation(DasAxis.HORIZONTAL);
                    }
                    return axis;
                }
            }
        }
        return null;
    }

    private static DasAxis processYAxisElement(Element element, DasRow row, DasColumn column, FormBase form) throws org.das2.DasPropertyException, org.das2.DasNameException, org.das2.DasException, java.text.ParseException {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element) {
                Element e = (Element) node;
                if (node.getNodeName().equals("axis")) {
                    DasAxis axis = processAxisElement(e, form);
                    if (axis.isHorizontal()) {
                        axis.setOrientation(DasAxis.VERTICAL);
                    }
                    return axis;
                } else if (node.getNodeName().equals("timeaxis")) {
                    DasAxis axis = processTimeaxisElement(e, form);
                    if (axis.isHorizontal()) {
                        axis.setOrientation(DasAxis.VERTICAL);
                    }
                    return axis;
                } else if (node.getNodeName().equals("attachedaxis")) {
                    DasAxis axis = processAttachedaxisElement(e, form);
                    if (axis.isHorizontal()) {
                        axis.setOrientation(DasAxis.VERTICAL);
                    }
                    return axis;
                }
            }
        }
        return null;
    }

    private static DasColorBar processZAxisElement(Element element, DasRow row, DasColumn column, FormBase form) throws DasPropertyException, DasNameException, DasException, java.text.ParseException {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element) {
                if (node.getNodeName().equals("colorbar")) {
                    return processColorbarElement((Element) node, form);
                }
            }
        }
        return null;
    }

    private static void processRenderersElement(Element element, DasPlot parent, FormBase form) throws org.das2.DasPropertyException, org.das2.DasNameException, org.das2.DasException, java.text.ParseException {
        NodeList children = element.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node node = children.item(index);
            if (node instanceof Element) {
                if (node.getNodeName().equals("spectrogram")) {
                    parent.addRenderer( processSpectrogramElement((Element) node, parent, form));
                } else if (node.getNodeName().equals("lineplot")) {
                    parent.addRenderer( processLinePlotElement((Element) node, parent, form));
                }
            }
        }
    }

    public static SeriesRenderer processLinePlotElement(Element element, DasPlot parent, FormBase form) {
        String dataSetID = element.getAttribute("dataSetID");
        PlotSymbol psym = DefaultPlotSymbol.BOX; //TODO: parsePsym(element.getAttribute("psym"));
        SymColor color = SymColor.parseSymColor(element.getAttribute("color"));
        SeriesRenderer renderer = new SeriesRenderer();
        parent.addRenderer(renderer);
        float lineWidth = Float.parseFloat(element.getAttribute("lineWidth"));
        try {
            renderer.setDataSetID(dataSetID);
        } catch (org.das2.DasException de) {
            org.das2.util.DasExceptionHandler.handle(de);
        }
        renderer.setPsym(psym);
        renderer.setColor(color);
        renderer.setLineWidth(lineWidth);
        return renderer;
    }


    public static Element getDOMElement(DasPlot plot,Document document) {

        Element element = document.createElement("plot");
        element.setAttribute("name", plot.getDasName());
        element.setAttribute("row", plot.getRow().getDasName());
        element.setAttribute("column", plot.getColumn().getDasName());
        element.setAttribute("title", plot.getTitle());

        Element xAxisChild = document.createElement("xAxis");
        Element xAxisElement = getDOMElement(plot.getXAxis(),document);
        xAxisElement.removeAttribute("orientation");
        if (xAxisElement.getAttribute("row").equals(plot.getRow().getDasName())) {
            xAxisElement.removeAttribute("row");
        }
        if (xAxisElement.getAttribute("column").equals(plot.getColumn().getDasName())) {
            xAxisElement.removeAttribute("column");
        }
        xAxisChild.appendChild(xAxisElement);
        element.appendChild(xAxisChild);

        Element yAxisChild = document.createElement("yAxis");
        Element yAxisElement = getDOMElement(plot.getYAxis(),document);
        yAxisElement.removeAttribute("orientation");
        if (yAxisElement.getAttribute("row").equals(plot.getRow().getDasName())) {
            yAxisElement.removeAttribute("row");
        }
        if (yAxisElement.getAttribute("column").equals(plot.getColumn().getDasName())) {
            yAxisElement.removeAttribute("column");
        }
        yAxisChild.appendChild(yAxisElement);
        element.appendChild(yAxisChild);

        Renderer[] renderers = plot.getRenderers();
        if (renderers.length > 0) {
            Element renderersChild = document.createElement("renderers");
            for (int index = 0; index < renderers.length; index++) {
                renderersChild.appendChild( getDOMElement(renderers[index],document) );
            }
            element.appendChild(renderersChild);
        }
        return element;
    }

    public static DasPlot createNamedPlot(String name) {
        DasAxis xAxis = createNamedAxis(null);
        xAxis.setOrientation(DasAxis.BOTTOM);
        DasAxis yAxis = createNamedAxis(null);
        yAxis.setOrientation(DasAxis.LEFT);
        DasPlot plot = new DasPlot(xAxis, yAxis);
        if (name == null) {
            name = "plot_" + Integer.toHexString(System.identityHashCode(plot));
        }
        try {
            plot.setDasName(name);
        } catch (org.das2.DasNameException dne) {
            org.das2.util.DasExceptionHandler.handle(dne);
        }
        return plot;
    }
    
    public static Element getDOMElement( Renderer r, Document doc ) {
        Element element=null;
        if ( r instanceof SymbolLineRenderer ) {
            SymbolLineRenderer slr= (SymbolLineRenderer)r;
            element = doc.createElement("lineplot");
            element.setAttribute("dataSetID", slr.getDataSetID());
            element.setAttribute("psym", slr.getPsym().toString());
            element.setAttribute("color", slr.getColor().toString());
        } else if ( r instanceof SpectrogramRenderer ) {
            SpectrogramRenderer spec= (SpectrogramRenderer)r;
            element = doc.createElement("spectrogram");
            element.setAttribute("dataSetID", spec.getDataSetID());

            Element zAxisChild = doc.createElement("zAxis");
            Element zAxisElement = getDOMElement( spec.getColorBar(),doc );
            if (zAxisElement.getAttribute("row").equals(spec.getParent().getRow().getDasName())) {
               zAxisElement.removeAttribute("row");
          }
         if (zAxisElement.getAttribute("column").equals(spec.getParent().getColumn().getDasName())) {
             zAxisElement.removeAttribute("column");
           }
          zAxisChild.appendChild(zAxisElement);
          element.appendChild(zAxisChild);

        } else if ( r instanceof StackedHistogramRenderer ) {
            StackedHistogramRenderer shr= (StackedHistogramRenderer)r;
            element = doc.createElement("stackedHistogram");
            element.setAttribute("zAxis", shr.getZAxis().getDasName() );
            element.setAttribute("dataSetID", shr.getDataSetID() );

        }

        return element;
    }


    public static Renderer processStackedHistogramElement(Element element, DasPlot parent, FormBase form) throws DasPropertyException, DasNameException, ParseException {
        String dataSetID = element.getAttribute("dataSetID");

        Renderer renderer = new StackedHistogramRenderer( parent, (DataSetDescriptor)null, (DasAxis)null, (DasLabelAxis)parent.getYAxis() );
        try {
            renderer.setDataSetID(dataSetID);
        } catch (DasException de) {
            DasExceptionHandler.handle(de);
        }
        return renderer;
    }

    /**
     * @param name
     * @param width
     * @param height
     * @return DasCanvas with a name.
     */
    public static DasCanvas createFormCanvas(String name, int width, int height) {
        DasCanvas canvas = new DasCanvas(width, height);
        if (name == null) {
            name = "canvas_" + Integer.toHexString(System.identityHashCode(canvas));
        }
        try {
            canvas.setDasName(name);
        } catch (org.das2.DasNameException dne) {
            org.das2.util.DasExceptionHandler.handle(dne);
        }
        return canvas;
    }

    /** TODO
     * @return
     */
    public FormBase getForm( DasCanvas canvas ) {
        Component parent = canvas.getParent();
        if (parent instanceof FormComponent) {
            return ((FormComponent) parent).getForm();
        }
        return null;
    }

    public void deregisterComponent(DasCanvas canvas) {
        DasApplication app = canvas.getDasApplication();
        if (app != null) {
            NameContext nc = app.getNameContext();
            for (Iterator i = canvas.devicePositionList().iterator(); i.hasNext();) {
                DasDevicePosition dp = (DasDevicePosition) i.next();
                try {
                    if (nc.get(dp.getDasName()) == dp) {
                        nc.remove(dp.getDasName());
                    }
                } catch (DasPropertyException dpe) {
                    //This exception would only occur due to some invalid state.
                    //So, wrap it and toss it.
                    IllegalStateException se = new IllegalStateException(dpe.toString());
                    se.initCause(dpe);
                    throw se;
                } catch (java.lang.reflect.InvocationTargetException ite) {
                    //This exception would only occur due to some invalid state.
                    //So, wrap it and toss it.
                    IllegalStateException se = new IllegalStateException(ite.toString());
                    se.initCause(ite);
                    throw se;
                }
            }
            for (int index = 0; index < canvas.getComponentCount(); index++) {
                Component c = canvas.getComponent(index);
                if (c instanceof DasCanvasComponent) {
                    DasCanvasComponent cc = (DasCanvasComponent) c;
                    try {
                        if (nc.get(cc.getDasName()) == cc) {
                            nc.remove(cc.getDasName());
                        }
                    } catch (DasPropertyException dpe) {
                        //This exception would only occur due to some invalid state.
                        //So, wrap it and toss it.
                        IllegalStateException se = new IllegalStateException(dpe.toString());
                        se.initCause(dpe);
                        throw se;
                    } catch (java.lang.reflect.InvocationTargetException ite) {
                        //This exception would only occur due to some invalid state.
                        //So, wrap it and toss it.
                        IllegalStateException se = new IllegalStateException(ite.toString());
                        se.initCause(ite);
                        throw se;
                    }
                }
            }
            try {
                if (nc.get(canvas.getDasName()) == this) {
                    nc.remove(canvas.getDasName());
                }
            } catch (DasPropertyException dpe) {
                //This exception would only occur due to some invalid state.
                //So, wrap it and toss it.
                IllegalStateException se = new IllegalStateException(dpe.toString());
                se.initCause(dpe);
                throw se;
            } catch (java.lang.reflect.InvocationTargetException ite) {
                //This exception would only occur due to some invalid state.
                //So, wrap it and toss it.
                IllegalStateException se = new IllegalStateException(ite.toString());
                se.initCause(ite);
                throw se;
            }
        }
    }

    public void registerComponent( DasCanvas canvas ) throws org.das2.DasException {
        try {
            DasApplication app = canvas.getDasApplication();
            if (app != null) {
                NameContext nc = app.getNameContext();
                for (Iterator i = canvas.devicePositionList().iterator(); i.hasNext();) {
                    DasDevicePosition dp = (DasDevicePosition) i.next();
                    nc.put(dp.getDasName(), dp);
                }
                for (int index = 0; index < canvas.getComponentCount(); index++) {
                    Component c = canvas.getComponent(index);
                    if (c instanceof DasCanvasComponent) {
                        DasCanvasComponent cc = (DasCanvasComponent) c;
                        nc.put(cc.getDasName(), cc);
                    }
                }
                nc.put(canvas.getDasName(), this);
            }
        } catch (DasNameException dne) {
            deregisterComponent(canvas);
            throw dne;
        }
    }

    public static final Pattern refPattern = Pattern.compile("\\$\\{([^\\}]+)\\}");
    public static final Pattern intPattern = Pattern.compile("-?(0|[1-9][0-9]*)");
    public static final Pattern floatPattern = Pattern.compile("-?[0-9]*(\\.[0-9]*)?([eE]-?[0-9]+)?");

    protected static String replaceReferences( NameContext n, String str) throws DasPropertyException, InvocationTargetException {
        Matcher matcher = refPattern.matcher(str);
        while (matcher.find()) {
            String name = matcher.group(1).trim();
            Object value = n.get(name);
            str = matcher.replaceFirst(value.toString());
            matcher.reset(str);
        }
        return str;
    }

    /**
     * Parses the given <code>String</code> object in an attempt to
     * produce the an object of the given type.
     *
     * @param valueString the given <code>String</code>
     * @param type the given type
     */
    public static Object parseValue( NameContext nameContext, String valueString, Class type) throws org.das2.dasml.ParsedExpressionException, InvocationTargetException, DasPropertyException {
        Object parsedValue;
        valueString = replaceReferences(nameContext,valueString);
        if (type == String.class) {
            return valueString;
        }
        else if (type == boolean.class || type == Boolean.class) {
            if (valueString.equals("true")) return Boolean.TRUE;
            if (valueString.equals("false")) return Boolean.FALSE;
            ParsedExpression exp = new ParsedExpression(valueString);
            Object o = exp.evaluate(nameContext);
            if (!(o instanceof Boolean)) throw new ParsedExpressionException("'" + valueString + "' does not evaluate to a boolean value");
            return o;
        }
        else if (type == int.class || type == Integer.class) {
            if (intPattern.matcher(valueString).matches()) {
                return new Integer(valueString);
            }
            ParsedExpression exp = new ParsedExpression(valueString);
            Object o = exp.evaluate(nameContext);
            if (!(o instanceof Number)) throw new ParsedExpressionException("'" + valueString + "' does not evaluate to a numeric value");
            return (o instanceof Integer ? (Integer)o : new Integer(((Number)o).intValue()));
        }
        else if (type == long.class || type == Long.class) {
            if (intPattern.matcher(valueString).matches()) {
                parsedValue = new Long(valueString);
            }
            ParsedExpression exp = new ParsedExpression(valueString);
            Object o = exp.evaluate(nameContext);
            if (!(o instanceof Number)) throw new ParsedExpressionException("'" + valueString + "' does not evaluate to a numeric value");
            return new Long(((Number)o).longValue());
        }
        else if (type == float.class || type == Float.class) {
            if (floatPattern.matcher(valueString).matches()) {
                parsedValue = new Float(valueString);
            }
            ParsedExpression exp = new ParsedExpression(valueString);
            Object o = exp.evaluate(nameContext);
            if (!(o instanceof Number)) throw new ParsedExpressionException("'" + valueString + "' does not evaluate to a numeric value");
            return new Float(((Number)o).floatValue());
        }
        else if (type == double.class || type == Double.class) {
            if (floatPattern.matcher(valueString).matches()) {
                parsedValue = new Double(valueString);
            }
            ParsedExpression exp = new ParsedExpression(valueString);
            Object o = exp.evaluate(nameContext);
            if (!(o instanceof Number)) throw new ParsedExpressionException("'" + valueString  + "' does not evaluate to a numeric value");
            return (o instanceof Double ? (Double)o : new Double(((Number)o).doubleValue()));
        }
        else if (type == Datum.class) {
            try {
                return TimeUtil.create(valueString);
            }
            catch ( java.text.ParseException ex ) {
                try {
                    return Datum.create(Double.parseDouble(valueString));
                }
                catch (NumberFormatException iae) {
                    throw new ParsedExpressionException(valueString + " cannot be parsed as a Datum");
                }
            }

        }
        else {
            throw new IllegalStateException(type.getName() + " is not a recognized type");
        }
    }

}
