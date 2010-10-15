/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.graph;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import org.das2.DasException;
import org.virbo.dataset.DataSetUtil;
import org.das2.dataset.TableDataSet;
import org.das2.dataset.VectorDataSet;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import org.das2.util.GrannyTextRenderer;
import org.das2.util.monitor.ProgressMonitor;
import org.virbo.dataset.DataSetOps;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SemanticOps;
import org.virbo.dsops.Ops;

/**
 *
 * @author jbf
 */
public class DigitalRenderer extends Renderer {

    protected Color color = Color.BLACK;
    public static final String PROP_COLOR = "color";

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        Color oldColor = this.color;
        this.color = color;
        propertyChangeSupport.firePropertyChange(PROP_COLOR, oldColor, color);
    }

    public enum Align {

        SW, NW, NE, SE, CENTER,
    }
    protected Align align = Align.CENTER;
    public static final String PROP_ALIGN = "align";

    public Align getAlign() {
        return align;
    }

    public void setAlign(Align align) {
        Align oldAlign = this.align;
        this.align = align;
        refresh();
        propertyChangeSupport.firePropertyChange(PROP_ALIGN, oldAlign, align);
    }
    Shape selectionArea;

    /**
     * like accept context, but provides a shape to indicate selection.  This
     * should be roughly the same as the locus of points where acceptContext is
     * true.
     * @return
     */
    public Shape selectionArea() {
        return selectionArea;
    }

    protected int dataSetSizeLimit = 10000;
    public static final String PROP_DATASETSIZELIMIT = "dataSetSizeLimit";

    public int getDataSetSizeLimit() {
        return dataSetSizeLimit;
    }

    public void setDataSetSizeLimit(int dataSetSizeLimit) {
        int oldDataSetSizeLimit = this.dataSetSizeLimit;
        this.dataSetSizeLimit = dataSetSizeLimit;
        propertyChangeSupport.firePropertyChange(PROP_DATASETSIZELIMIT, oldDataSetSizeLimit, dataSetSizeLimit);
    }

    private int firstIndex=-1, lastIndex=-1;
    private boolean dataSetClipped= false;

    /**
     * updates firstIndex and lastIndex that point to the part of
     * the data that is plottable.  The plottable part is the part that
     * might be visible while limiting the number of plotted points.
     * //TODO: bug 0000354: warning message bubble about all data before or after visible range
     */
    private synchronized void updateFirstLast(DasAxis xAxis, DasAxis yAxis, QDataSet dataSet) {

        Units xUnits = xAxis.getUnits();
    
        int ixmax;
        int ixmin;

        QDataSet wds= SemanticOps.weightsDataSet(dataSet);
        QDataSet xds= SemanticOps.xtagsDataSet(dataSet);

        Boolean xMono = SemanticOps.isMonotonic(xds);

        if (xMono != null && xMono.booleanValue()) {
            DatumRange visibleRange = xAxis.getDatumRange();
            if (parent.isOverSize()) {
                Rectangle plotBounds = parent.getUpdateImageBounds();
                if ( plotBounds!=null ) {
                    visibleRange = new DatumRange(xAxis.invTransform(plotBounds.x), xAxis.invTransform(plotBounds.x + plotBounds.width));
                }

            }
            ixmin = DataSetUtil.getPreviousIndex(dataSet, visibleRange.min());
            ixmax = DataSetUtil.getNextIndex(dataSet, visibleRange.max()) + 1; // +1 is for exclusive.

        } else {
            ixmin = 0;
            ixmax = dataSet.length();
        }

        double x = Double.NaN;

        int index;

        // find the first valid point, set x0, y0 //
        for (index = ixmin; index < ixmax; index++) {
            x = (double) xds.value(index);

            final boolean isValid = wds.value(index)>0 && xUnits.isValid(x);
            if (isValid) {
                firstIndex = index;  // TODO: what if no valid points?

                index++;
                break;
            }
        }

        if ( firstIndex==-1 ) { // no valid points
            lastIndex= ixmax;
            firstIndex= ixmax;
        }

        // find the last valid point, minding the dataSetSizeLimit
        int pointsPlotted = 0;
        for (index = firstIndex; index < ixmax && pointsPlotted < dataSetSizeLimit; index++) {

            final boolean isValid = wds.value(index)>0 && xUnits.isValid(x);

            if (isValid) {
                pointsPlotted++;
            }
        }

        if (index < ixmax && pointsPlotted == dataSetSizeLimit) {
            dataSetClipped = true;
        } else {
            dataSetClipped = false;
        }

        lastIndex = index;

    }

    @Override
    public void render(Graphics g, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {
        g.setColor(color);
        if ( ! isTableDataSet(ds) ) {
            renderRank1( ds, g, xAxis, yAxis, mon);
        } else {
            renderRank2( ds, g, xAxis, yAxis, mon);
        }
    }

    private void renderRank1( QDataSet ds, Graphics g, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {
        FontMetrics fm = g.getFontMetrics();
        int ha = 0;
        if (align == Align.NE || align == Align.NW) ha = fm.getAscent();
        if (align == Align.CENTER) ha = fm.getAscent() / 2;
        float wa = 0.f; // amount to adjust the position.
        if (align == Align.NE || align == Align.SE) wa = 1.0f;
        if (align == Align.CENTER) wa = 0.5f;

        GeneralPath shape = new GeneralPath();

        GrannyTextRenderer gtr = new GrannyTextRenderer();

        Units u = SemanticOps.getUnits(ds);
        QDataSet xds= SemanticOps.xtagsDataSet(ds);
        Units xunits= SemanticOps.getUnits(xds);

        int count = 0;
        
        if ( ! xunits.isConvertableTo(xAxis.getUnits() ) ) {
            parent.postMessage( this, "inconvertible xaxis units", DasPlot.INFO, null, null );
            return;
        }
        if ( ! u.isConvertableTo(yAxis.getUnits() ) ) {
            parent.postMessage( this, "inconvertible yaxis units", DasPlot.INFO, null, null );
            return;
        }

        QDataSet wds= SemanticOps.weightsDataSet(ds);

        for (int i = firstIndex; i < lastIndex; i++) {
            int ix = (int) xAxis.transform( xds.value(i), xunits );

            String s;
            int iy;
            if ( wds.value(i)>0 ) {
                Datum d = u.createDatum( ds.value(i) );
                s = d.getFormatter().format(d, u);
                iy = (int) yAxis.transform(d) + ha;
            } else {
                s = "fill";
                iy = (int) yAxis.getRow().getDMaximum();
            }

            if (wa > 0.0) ix = ix - (int) (fm.stringWidth(s) * wa);

            gtr.setString(g, s);
            gtr.draw(g, ix, iy);
            Rectangle r = gtr.getBounds();
            r.translate(ix, iy);
            shape.append(r, false);
            if ( dataSetClipped ) {
                if ( getParent()!=null ) getParent().postMessage(this, "" + dataSetSizeLimit + " data point limit reached", DasPlot.WARNING, null, null);
                return;
            }

        }
        selectionArea = shape;
    }

    private void renderRank2( QDataSet ds, Graphics g, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {
        FontMetrics fm = g.getFontMetrics();
        int ha = 0;
        if (align == Align.NE || align == Align.NW) ha = fm.getAscent();
        if (align == Align.CENTER) ha = fm.getAscent() / 2;
        float wa = 0.f; // amount to adjust the position.
        if (align == Align.NE || align == Align.SE) wa = 1.0f;
        if (align == Align.CENTER) wa = 0.5f;

        GeneralPath shape = new GeneralPath();

        GrannyTextRenderer gtr = new GrannyTextRenderer();

        QDataSet fds= DataSetOps.flattenRank2(ds);

        Units u = SemanticOps.getUnits(ds);

        QDataSet zds= DataSetOps.unbundle(fds, fds.length(0)-1 );
        QDataSet xds;
        QDataSet yds;

        if ( fds.length(0)==1 ) {
            xds= DataSetOps.unbundle(fds, 0);
            yds= DataSetOps.unbundle(fds, 1);
        } else {
            xds= Ops.mod( Ops.dindgen(fds.length()), DataSetUtil.asDataSet(ds.length() ) );
            yds= Ops.div( Ops.dindgen(fds.length()), DataSetUtil.asDataSet(ds.length() ) );
        }

        Units xunits= SemanticOps.getUnits(xds);
        Units yunits= SemanticOps.getUnits(yds);

        if ( SemanticOps.getUnits(xds).isConvertableTo(xAxis.getUnits() ) ) {
            parent.postMessage( this, "inconvertible xaxis units", DasPlot.INFO, null, null );
            return;
        }
        if ( SemanticOps.getUnits(yds).isConvertableTo(yAxis.getUnits() ) ) {
            parent.postMessage( this, "inconvertible yaxis units", DasPlot.INFO, null, null );
            return;
        }

        int n= fds.length();

        QDataSet wds= SemanticOps.weightsDataSet(zds);

        int count = 0;
        for (int i = 0; i < n; i++) {
            int ix = (int) xAxis.transform(xds.value(i),xunits);
            int iy = (int) yAxis.transform(yds.value(i),yunits) + ha;

            String s;
            if ( wds.value(i)>0 ) {
                Datum d = u.createDatum( zds.value(i) );
                s = d.getFormatter().format(d, u);
                if (wa > 0.0) ix = ix - (int) (fm.stringWidth(s) * wa);
            } else {
                s = "fill";
            }

            gtr.setString(g, s);
            gtr.draw(g, ix, iy);
            Rectangle r = gtr.getBounds();
            r.translate(ix, iy);
            shape.append(r, false);
            if (count++ > 10000) {
                if ( getParent()!=null ) getParent().postMessage(this, "10000 data point limit reached", DasPlot.WARNING, null, null);
                return;
            }
        }
        selectionArea = shape;
    }

    @Override
    public boolean acceptContext(int x, int y) {
        return true;
    }

    @Override
    public void updatePlotImage(DasAxis xAxis, DasAxis yAxis, ProgressMonitor monitor) throws DasException {
        updateFirstLast( xAxis, yAxis, getDataSet() );
    }


}
