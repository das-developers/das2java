/* File: CrossHairRenderer.java
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
package org.das2.event;

import org.das2.components.propertyeditor.Editable;
import org.das2.dataset.DataSetConsumer;
import org.das2.dataset.TableDataSetConsumer;
import org.das2.dataset.TableDataSet;
import org.das2.dataset.TableUtil;
import org.das2.dataset.DataSet;
import org.das2.dataset.DataSetUtil;
import org.das2.dataset.VectorDataSet;
import org.das2.datum.format.DefaultDatumFormatterFactory;
import org.das2.datum.format.DatumFormatter;
import org.das2.graph.DasAxis;
import org.das2.graph.DasPlot;

import org.das2.datum.Datum;

import org.das2.graph.Renderer;
import java.awt.*;
import java.awt.geom.Point2D;
import java.text.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author  eew
 */
public class CrossHairRenderer extends LabelDragRenderer implements DragRenderer, Editable {

    protected int xInitial;
    protected int yInitial;
    protected DasAxis XAxis;
    protected DasAxis YAxis;
    protected DasPlot parent;
    private int ix = 0; // store the current position within the dataset object
    private int iy = 0;
    private int context;
    private DatumFormatter nfx;
    private DatumFormatter nfy;
    private DatumFormatter nfz;
    private FontMetrics fm;
    private int dxMax = -999999;
    private Rectangle hDirtyBounds;
    private Rectangle vDirtyBounds;
    private Point crossHairLocation = null;
    private DataSetConsumer dataSetConsumer;

	private List<CrossHairMouseModule.InfoItem> infoItems = new ArrayList();

    /**
     * Holds value of property allPlanesReport.
     */
    private boolean allPlanesReport;
    /**
     * Holds value of property debugging.
     */
    private boolean debugging;
    /**
     * <code>snapping = true</code> means that the cross-hair digitizer will only
     * display x and y values that are valid tags in the data set.
     */
    private boolean snapping;

    public CrossHairRenderer(DasPlot parent, DataSetConsumer dataSetConsumer, DasAxis xAxis, DasAxis yAxis) {
        super(parent);
        this.XAxis = xAxis;
        this.YAxis = yAxis;
        this.parent = parent;
        this.dataSetConsumer = dataSetConsumer;
        hDirtyBounds = new Rectangle();
        vDirtyBounds = new Rectangle();
    }

	void addInfoItem(CrossHairMouseModule.InfoItem item) {
		infoItems.add(item);
	}

	void removeInfoItem(CrossHairMouseModule.InfoItem item) {
		infoItems.remove(item);
	}

    private DatumFormatter addResolutionToFormat(DatumFormatter nfz) throws ParseException {
        String formatString = nfz.toString();
        String result;
        if (formatString.indexOf('E') == -1) {
            result = formatString + "00";
        } else {
            String[] ss = formatString.split("E");
            if (ss[0].indexOf('.') == -1) {
                result = ss[0] + ".00" + "E0";
            } else {
                result = ss[0] + "00" + "E0";
            }
        }
        return DefaultDatumFormatterFactory.getInstance().newFormatter(result);
    }

    private String getZString(TableDataSet tds, Datum x, Datum y, int[] ij) {
        int i = DataSetUtil.closestColumn(tds, x);
        int j = TableUtil.closestRow(tds, tds.tableOfIndex(i), y);
        Datum zValue = tds.getDatum(i, j);

        if (ij != null) {
            ij[0] = i;
            ij[1] = j;
        }

        try {
            if (dataSetConsumer instanceof TableDataSetConsumer) {
                nfz = ((TableDataSetConsumer) dataSetConsumer).getZAxis().getDatumFormatter();
                nfz = addResolutionToFormat(nfz);
            } else {
                nfz = DefaultDatumFormatterFactory.getInstance().newFormatter("0.000");
            }
        } catch (java.text.ParseException pe) {
            org.das2.DasProperties.getLogger().severe("failure to create formatter");
            DasAxis axis = ((TableDataSetConsumer) dataSetConsumer).getZAxis();
            axis.getUnits().getDatumFormatterFactory().defaultFormatter();
        }

        String result;
        if (zValue.isFill()) {
            result = "fill";
        } else {
            result = nfz.grannyFormat(zValue);
        }
        if (allPlanesReport) {
            if (debugging) {
                result += "!c" + tds.toString();
            }
            String[] planeIds = tds.getPlaneIds();
            for (int iplane = 0; iplane < planeIds.length; iplane++) {
                if (!planeIds[iplane].equals("")) {
                    result = result + "!c";
                    result += planeIds[iplane] + ":" + nfz.grannyFormat(((TableDataSet) tds.getPlanarView(planeIds[iplane])).getDatum(i, j));
                    if (debugging) {
                        result += " " + ((TableDataSet) tds.getPlanarView(planeIds[iplane])).toString();
                    }
                }
            }
            if (debugging) {
                result += "!ci:" + i + " j:" + j;
            }
        }
        return result;
    }

    private int closestPointVector(VectorDataSet ds, Datum x, Datum y) {

        Boolean xmono = (Boolean) ds.getProperty(DataSet.PROPERTY_X_MONOTONIC);

        DasAxis xa, ya;
        xa = (this.XAxis == null) ? parent.getXAxis() : XAxis;
        ya = (this.YAxis == null) ? parent.getYAxis() : YAxis;

        int start, end;
        Point2D.Double me = new Point2D.Double(xa.transform(x), ya.transform(y));
        if (xmono != null && xmono.equals(Boolean.TRUE)) {
            start = DataSetUtil.getPreviousColumn(ds, xa.getDataMinimum());
            end = DataSetUtil.getNextColumn(ds, xa.getDataMaximum());
        } else {
            start = 0;
            end = ds.getXLength();
        }

        int bestIndex = -1;
        double bestXDist = Double.POSITIVE_INFINITY;
        double bestDist = Double.POSITIVE_INFINITY;
        int comparisons = 0;

        // prime the best dist comparison by scanning decimated dataset
        for (int i = start; i < end; i += 100) {
            double x1 = xa.transform(ds.getXTagDatum(i));
            double dist = Math.abs(x1 - me.getX());
            if (dist < bestXDist) {
                bestXDist = dist;
            }
        }

        for (int i = start; i < end; i++) {
            double x1 = xa.transform(ds.getXTagDatum(i));
            if (Math.abs(x1 - me.getX()) <= bestXDist) {
                Point2D them = new Point2D.Double(x1, ya.transform(ds.getDatum(i)));
                double dist = me.distance(them);
                comparisons++;
                if (dist < bestDist) {
                    bestIndex = i;
                    bestDist = dist;
                    bestXDist = Math.abs(x1 - me.getX());
                }
            }
        }

        return bestIndex;


    }

    @Override
    public Rectangle[] renderDrag(Graphics g1, Point p1, Point p2) {
        Graphics2D g = (Graphics2D) g1;
        g.setRenderingHints((RenderingHints) org.das2.DasProperties.getRenderingHints());
        
        DataSet ds;
        
        if ( dataSetConsumer!=null ) {
            ds = dataSetConsumer.getConsumedDataSet();
        } else {
            Renderer[] rends= ((DasPlot)this.parent).getRenderers();
            if ( rends.length>0 ) {
                ds= rends[0].getConsumedDataSet();
            } else {
                ds= null;
            }
        }

        Rectangle[] superDirty = null;

        Datum x = null;
        Datum y = null;

        DasAxis xa, ya;
        xa = (this.XAxis == null) ? parent.getXAxis() : XAxis;
        ya = (this.YAxis == null) ? parent.getYAxis() : YAxis;

        if (crossHairLocation == null) {

            x = xa.invTransform(p2.x);
            y = ya.invTransform(p2.y);

            nfy = y.getFormatter();

            String xAsString;
            nfx = x.getFormatter();
            xAsString = nfx.format(x);


            String yAsString;
            yAsString = nfy.format(y);

            String report;

            String nl = multiLine ? "!c" : " ";

            report = "x:" + xAsString + nl + "y:" + yAsString;
            
            if (ds != null) {
                if (ds instanceof TableDataSet) {
                    TableDataSet tds = (TableDataSet) ds;
                    String zAsString;
                    if (tds != null && snapping) {
                        int[] ij = new int[2];
                        zAsString = getZString(tds, x, y, ij);
                        x = tds.getXTagDatum(ij[0]);
                        xAsString = nfx.format(x);
                        y = tds.getYTagDatum(tds.tableOfIndex(ij[0]), ij[1]);
                        yAsString = nfy.format(y);
                    } else {
                        zAsString = getZString(tds, x, y, null);
                    }
                    report = "x:" + xAsString + nl + "y:" + yAsString + nl + "z:" + zAsString;
                } else {
                    if (ds == null && dataSetConsumer instanceof DasPlot) {
                        if (((DasPlot) dataSetConsumer).getRenderers().length > 0) {
                            ds = ((DasPlot) dataSetConsumer).getRenderer(0).getDataSet();
                        }
                    }
                    if (ds != null && snapping) {
                        VectorDataSet vds = (VectorDataSet) ds;
                        if (vds.getXLength() == 0) {
                            yAsString = "(empty dataset)";
                        } else {
                            int i = closestPointVector(vds, x, y);
                            x = vds.getXTagDatum(i);
                            y = vds.getDatum(i);
                            xAsString = nfx.format(x);
                            yAsString = nfy.format(y);
                            if (allPlanesReport) {
                                String result = yAsString;
                                String[] planeIds = vds.getPlaneIds();
                                for (int iplane = 0; iplane < planeIds.length; iplane++) {
                                    if (!planeIds[iplane].equals("")) {
                                        result = result + "!c";
                                        result += planeIds[iplane] + ":" + nfy.grannyFormat(((VectorDataSet) vds.getPlanarView(planeIds[iplane])).getDatum(i));
                                        if (debugging) {
                                            result += " " + ((VectorDataSet) vds.getPlanarView(planeIds[iplane])).toString();
                                        }
                                    }
                                }
                                yAsString = result;
                            }
                        }
                    }
                    report = "x:" + xAsString + nl + "y:" + yAsString;
                }
            }

			StringBuilder builder = new StringBuilder(report);
			for (CrossHairMouseModule.InfoItem item : infoItems) {
				builder.append("!C");
				item.label(x, y, builder);
			}
			report = builder.toString();

            setLabel(report);
            super.renderDrag(g, p1, p2);
        }

        if (snapping && x != null && y != null) {
            Point p3 = new Point((int) xa.transform(x), (int) ya.transform(y));
            drawCrossHair(g, p3);
        /*
        //p2= GraphUtil.moveTowards( p2, p3, 4 );
        g.drawLine( p2.x, p2.y, p3.x, p3.y );
        dirtyBounds.add( p3 );
        dirtyBounds.add( p2 );*/
        } else {
            drawCrossHair(g, p2);
        }


        return new Rectangle[]{this.hDirtyBounds,
                    this.vDirtyBounds,
                    dirtyBounds
                };
    }

    private void drawCrossHair(Graphics g0, Point p) {

        Graphics2D g = (Graphics2D) g0.create();
        g.setClip(null);

        Color color0 = Color.black;

        g.setColor(color0);

        Dimension d = parent.getCanvas().getSize();
        hDirtyBounds.setBounds(0, p.y - 1, d.width, 3);

        Stroke stroke0 = g.getStroke();

        g.setColor(ghostColor);
        g.setStroke(new BasicStroke(3.0f));
        g.drawLine(0, p.y, d.width, p.y);
        g.drawLine(p.x, 0, p.x, d.height);

        g.setColor(color0);
        g.setStroke(stroke0);

        g.drawLine(0, p.y, d.width, p.y);
        vDirtyBounds.setBounds(p.x - 1, 0, 3, d.height);
        g.drawLine(p.x, 0, p.x, d.height);

        g.dispose();

    }

    public void clear(Graphics g) {
        super.clear(g);
        parent.paintImmediately(hDirtyBounds);
        parent.paintImmediately(vDirtyBounds);
    }

    public boolean isPointSelection() {
        return true;
    }

    public boolean isUpdatingDragSelection() {
        return false;
    }

    /**
     * Getter for property allPlanesReport.
     * @return Value of property allPlanesReport.
     */
    public boolean isAllPlanesReport() {
        return this.allPlanesReport;
    }

    /**
     * Setter for property allPlanesReport.
     * @param allPlanesReport New value of property allPlanesReport.
     */
    public void setAllPlanesReport(boolean allPlanesReport) {
        this.allPlanesReport = allPlanesReport;
    }

    /**
     * Getter for property debugging.
     * @return Value of property debugging.
     */
    public boolean isDebugging() {
        return this.debugging;
    }

    /**
     * Setter for property debugging.
     * @param debugging New value of property debugging.
     */
    public void setDebugging(boolean debugging) {
        this.debugging = debugging;
    }

    public Rectangle[] getDirtyBounds() {
        return new Rectangle[]{super.dirtyBounds, this.hDirtyBounds, this.vDirtyBounds};
    }

    public boolean isSnapping() {
        return snapping;
    }

    public void setSnapping(boolean b) {
        snapping = b;
    }
    /**
     * Holds value of property multiLine.
     */
    private boolean multiLine = false;

    /**
     * Getter for property multiLine.
     * @return Value of property multiLine.
     */
    public boolean isMultiLine() {

        return this.multiLine;
    }

    /**
     * Setter for property multiLine.
     * @param multiLine New value of property multiLine.
     */
    public void setMultiLine(boolean multiLine) {

        this.multiLine = multiLine;
    }
}
