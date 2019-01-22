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
import org.das2.qds.DataSetUtil;
import org.das2.datum.format.DefaultDatumFormatterFactory;
import org.das2.datum.format.DatumFormatter;
import org.das2.graph.DasAxis;
import org.das2.graph.DasPlot;

import org.das2.datum.Datum;

import org.das2.graph.Renderer;
import java.awt.*;
import java.awt.geom.Point2D;
import java.text.*;
import org.das2.datum.InconvertibleUnitsException;
import org.das2.datum.Units;
import org.das2.datum.UnitsUtil;
import org.das2.datum.format.EnumerationDatumFormatter;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;
import org.das2.qds.examples.Schemes;
import org.das2.qds.ops.Ops;

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
    private DatumFormatter nfx;
    private DatumFormatter nfy;
    private DatumFormatter nfz;
    private FontMetrics fm;
    private Rectangle hDirtyBounds;
    private Rectangle vDirtyBounds;
    private Point crossHairLocation = null;
    private DataSetConsumer dataSetConsumer;
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
        try {
            return DefaultDatumFormatterFactory.getInstance().newFormatter(result);
        } catch ( IllegalArgumentException ex ) {
            return nfz; //TODO: track this down.  It has something to do with logLinDomainDivider for colorbar.
        }
    }

    private String getZComponentsString( QDataSet tds, Datum x, Datum y ) {
        assert tds.rank()==3;
        try {
            QDataSet xds= SemanticOps.xtagsDataSet(tds);
            int i = DataSetUtil.closestIndex(xds, x);
            QDataSet tds1= Ops.copy( tds.slice(i) );
            QDataSet yds= (QDataSet) tds1.property(QDataSet.DEPEND_0);  // Can't use SemanticOps.xtagsDataSet, because it picks up "red" not the index.
            if ( yds==null ) yds= Ops.indgen(tds1.length());
            int j;
            try {
                yds= Ops.copy(yds);
                j= DataSetUtil.closestIndex(yds, y);
            } catch ( IllegalArgumentException ex ) {
                return ex.getMessage();
            }
            QDataSet rgb= tds1.slice(j);
            return DataSetUtil.toString(rgb);
        } catch ( InconvertibleUnitsException ex ) {
            return "N/A";
        }
    }
    
    private String getZString( QDataSet tds, Datum x, Datum y, int[] ij) {
        QDataSet xds= SemanticOps.xtagsDataSet(tds);
        int i;
        try {
            i= DataSetUtil.closestIndex(xds, x);
        } catch ( InconvertibleUnitsException ex ) {
            Units u= SemanticOps.getUnits(xds);
            i= DataSetUtil.closestIndex( xds, x.value(), u );
        }
        QDataSet tds1= tds.slice(i);
        QDataSet yds= SemanticOps.xtagsDataSet(tds1);
        int j;
        try {
            j= DataSetUtil.closestIndex(yds, y);
        } catch ( InconvertibleUnitsException ex ) {
            Units u= SemanticOps.getUnits(yds);
            if ( UnitsUtil.isIntervalOrRatioMeasurement(u) ) {
                j= DataSetUtil.closestIndex( yds, y.value(), u );
            } else {
                return "";
            }
        } catch ( IllegalArgumentException ex ) {
            return ex.getMessage();
        }
        double d= tds1.value(j);
        Datum zValue = SemanticOps.getDatum( tds1, d );

        if (ij != null) {
            ij[0] = i;
            ij[1] = j;
        }

        try {
            if (dataSetConsumer instanceof TableDataSetConsumer) {
                if ( !UnitsUtil.isIntervalOrRatioMeasurement( zValue.getUnits() ) ) {
                    nfz= new EnumerationDatumFormatter();
                } else {
                    nfz = ((TableDataSetConsumer) dataSetConsumer).getZAxis().getDatumFormatter();
                    nfz = addResolutionToFormat(nfz);
                }
            } else {
                nfz = DefaultDatumFormatterFactory.getInstance().newFormatter("0.000");
            }
        } catch (java.text.ParseException pe) {
            org.das2.DasProperties.getLogger().severe("failure to create formatter");
            DasAxis axis = ((TableDataSetConsumer) dataSetConsumer).getZAxis();
            axis.getUnits().getDatumFormatterFactory().defaultFormatter();
        }

        StringBuilder result;
        if (zValue.isFill()) {
            result = new StringBuilder( "fill" );
        } else {
            result = new StringBuilder( nfz.grannyFormat(zValue) );
        }
        if (allPlanesReport) {
            if (debugging) {
                result.append( "!c" ).append( tds.toString() );
            }
            for (int iplane = 0; iplane < QDataSet.MAX_PLANE_COUNT; iplane++) {
                QDataSet plane= (QDataSet) tds.property( "PLANE_"+iplane );
                if ( plane==null ) break;
                result.append("!c");
                result.append( plane.property(QDataSet.NAME) ).append( ":" ).append(  nfz.grannyFormat( SemanticOps.getDatum( plane, plane.value(i,j) ) ) );
                if (debugging) {
                    result.append(" ").append( plane.toString() );
                }
            }
            if (debugging) {
                result.append( "!ci:" ).append( i ).append( " j:" ).append( j );
            }
        }
        return result.toString();
    }

    private int closestPointVector(QDataSet ds, Datum x, Datum y) {

        QDataSet xds= SemanticOps.xtagsDataSet(ds);
        Units xunits= SemanticOps.getUnits(xds);

        boolean xmono = SemanticOps.isMonotonic(xds);

        DasAxis xa, ya;
        xa = (this.XAxis == null) ? parent.getXAxis() : XAxis;
        ya = (this.YAxis == null) ? parent.getYAxis() : YAxis;

        int start, end;
        Point2D.Double me = new Point2D.Double(xa.transform(x), ya.transform(y));
        if ( xmono ) {
            start = DataSetUtil.getPreviousIndex(xds, xa.getDataMinimum());
            end = DataSetUtil.getNextIndex(xds, xa.getDataMaximum());
        } else {
            start = 0;
            end = xds.length();
        }

        int bestIndex = -1;
        double bestXDist = Double.POSITIVE_INFINITY;
        double bestDist = Double.POSITIVE_INFINITY;
        //int comparisons = 0;

        // prime the best dist comparison by scanning decimated dataset
        for (int i = start; i < end; i += 100) {
            double x1 = xa.transform( xds.value(i), xunits );
            double dist = Math.abs(x1 - me.getX());
            if (dist < bestXDist) {
                bestXDist = dist;
            }
        }

        Units units= SemanticOps.getUnits(ds);

        for (int i = start; i < end; i++) {
            double x1 = xa.transform( xds.value(i), xunits );
            if (Math.abs(x1 - me.getX()) <= bestXDist) {
                Point2D them = new Point2D.Double(x1, ya.transform( ds.value(i), units ) );
                double dist = me.distance(them);
                //comparisons++;
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
        
        QDataSet ds;

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

        if ( dataSetConsumer instanceof Renderer ) {
            Renderer r= (Renderer)dataSetConsumer;
            if ( r.isActive()==false || r.getParent()==null ) {
                ds= null;
            }
        }

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
                QDataSet xds= SemanticOps.xtagsDataSet(ds);

                if ( SemanticOps.isTableDataSet(ds) ) {
                    QDataSet tds;
                    QDataSet yds;
                    if ( SemanticOps.isSimpleTableDataSet(ds) ) {
                        tds= (QDataSet) ds;
                    } else if ( Schemes.isCompositeImage(ds) ) {
                        tds= (QDataSet) ds;
                    } else {
                        tds= SemanticOps.getSimpleTableContaining( ds, x, y );
                        if ( tds==null ) tds= ds.slice(0);
                    }
                    yds= SemanticOps.ytagsDataSet(tds);

                    String zAsString;
                    if ( snapping) {
                        int[] ij = new int[2];
                        zAsString = getZString(tds, x, y, ij);
                        x = SemanticOps.getDatum( xds, xds.value(ij[0]) );
                        xAsString = nfx.format(x);
                        y = SemanticOps.getDatum( yds, yds.value(ij[1]) );
                        yAsString = nfy.format(y);
                    } else {
                        if ( Schemes.isCompositeImage(ds) ) {
                            zAsString = "!c" + getZComponentsString( tds, x, y );
                        } else {
                            zAsString = getZString(tds, x, y, null);
                        }
                    }
                    report = "x:" + xAsString + nl + "y:" + yAsString + nl + "z:" + zAsString;
                } else {
                    if ( snapping) {
                        QDataSet vds = (QDataSet) ds;
                        if (vds.length() == 0) {
                            yAsString = "(empty dataset)";
                        } else {
                            int i = closestPointVector(vds, x, y);
                            x = SemanticOps.getDatum( xds, xds.value(i) );
                            y = SemanticOps.getDatum( vds, vds.value(i) );
                            xAsString = nfx.format(x);
                            yAsString = nfy.format(y);
                            if (allPlanesReport) {
                                StringBuilder result = new StringBuilder( yAsString );

                                for (int iplane = 0; iplane < QDataSet.MAX_PLANE_COUNT; iplane++) {
                                    QDataSet plane= (QDataSet) vds.property( "PLANE_"+iplane );
                                    if ( plane==null ) break;
                                    result.append( "!c" );
                                    if ( plane.rank()==1 ) {
                                        String n= Ops.guessName(plane);
                                        String s= plane.slice(i).svalue();
                                        result.append( n ).append( ":" ) .append( s );
                                        if (debugging) {
                                            result.append( " " ).append( plane.toString() );
                                        }
                                    } else if ( plane.rank()==2 ) {
                                        for ( int j=0; j<plane.length(0); j++ ) {
                                            String n= Ops.guessName(Ops.slice1(plane,j));
                                            String s= plane.slice(i).slice(j).svalue();
                                            result.append( n ).append( ":" ) .append( s );
                                            if (debugging) {
                                                result.append( " " ).append( plane.toString() );
                                            }
                                        }
                                    }
                                }

                                yAsString = result.toString();
                            }
                        }
                    }
                    report = "x:" + xAsString + nl + "y:" + yAsString;
                }
            }

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

    @Override
    public void clear(Graphics g) {
        super.clear(g);
        parent.paintImmediately(hDirtyBounds);
        parent.paintImmediately(vDirtyBounds);
    }

    @Override
    public boolean isPointSelection() {
        return true;
    }

    @Override
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

    @Override
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
