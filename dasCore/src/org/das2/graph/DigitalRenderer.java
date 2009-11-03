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
import org.das2.dataset.DataSet;
import org.das2.dataset.DataSetUtil;
import org.das2.dataset.TableDataSet;
import org.das2.dataset.VectorDataSet;
import org.das2.dataset.WeightsVectorDataSet;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import org.das2.util.GrannyTextRenderer;
import org.das2.util.monitor.ProgressMonitor;

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
    private synchronized void updateFirstLast(DasAxis xAxis, DasAxis yAxis, VectorDataSet dataSet) {

        Units xUnits = xAxis.getUnits();
        Units yUnits = yAxis.getUnits();

        int ixmax;
        int ixmin;

        VectorDataSet wds= WeightsVectorDataSet.create( dataSet );

        Boolean xMono = (Boolean) dataSet.getProperty(DataSet.PROPERTY_X_MONOTONIC);
        if (xMono != null && xMono.booleanValue()) {
            DatumRange visibleRange = xAxis.getDatumRange();
            if (parent.isOverSize()) {
                Rectangle plotBounds = parent.getUpdateImageBounds();
                if ( plotBounds!=null ) {
                    visibleRange = new DatumRange(xAxis.invTransform(plotBounds.x), xAxis.invTransform(plotBounds.x + plotBounds.width));
                }

            }
            ixmin = DataSetUtil.getPreviousColumn(dataSet, visibleRange.min());
            ixmax = DataSetUtil.getNextColumn(dataSet, visibleRange.max()) + 1; // +1 is for exclusive.

        } else {
            ixmin = 0;
            ixmax = dataSet.getXLength();
        }

        double x = Double.NaN;
        double y = Double.NaN;

        int index;

        // find the first valid point, set x0, y0 //
        for (index = ixmin; index < ixmax; index++) {
            x = (double) dataSet.getXTagDouble(index, xUnits);
            y = (double) dataSet.getDouble(index, yUnits);

            final boolean isValid = wds.getDouble(index,Units.dimensionless)>0 && xUnits.isValid(x);
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
            y = dataSet.getDouble(index, yUnits);

            final boolean isValid = wds.getDouble(index,Units.dimensionless)>0 && xUnits.isValid(x);

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
        if (ds instanceof VectorDataSet) {
            renderRank1((VectorDataSet) ds, g, xAxis, yAxis, mon);
        } else if (ds instanceof TableDataSet) {
            renderRank2((TableDataSet) ds, g, xAxis, yAxis, mon);
        }
    }

    private void renderRank1(VectorDataSet ds, Graphics g, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {
        FontMetrics fm = g.getFontMetrics();
        int ha = 0;
        if (align == Align.NE || align == Align.NW) ha = fm.getAscent();
        if (align == Align.CENTER) ha = fm.getAscent() / 2;
        float wa = 0.f; // amount to adjust the position.
        if (align == Align.NE || align == Align.SE) wa = 1.0f;
        if (align == Align.CENTER) wa = 0.5f;

        GeneralPath shape = new GeneralPath();

        GrannyTextRenderer gtr = new GrannyTextRenderer();

        Units u = ds.getYUnits();
        int count = 0;
        
        if ( ! ds.getXUnits().isConvertableTo(xAxis.getUnits() ) ) {
            parent.postMessage( this, "inconvertible xaxis units", DasPlot.INFO, null, null );
            return;
        }
        if ( ! ds.getYUnits().isConvertableTo(yAxis.getUnits() ) ) {
            parent.postMessage( this, "inconvertible yaxis units", DasPlot.INFO, null, null );
            return;
        }

        for (int i = firstIndex; i < lastIndex; i++) {
            int ix = (int) xAxis.transform(ds.getXTagDatum(i));

            Datum d = ds.getDatum(i);
            String s = d.getFormatter().format(d, u);

            if (wa > 0.0) ix = ix - (int) (fm.stringWidth(s) * wa);

            int iy = (int) yAxis.transform(d) + ha;

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

    private void renderRank2(TableDataSet ds, Graphics g, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {
        FontMetrics fm = g.getFontMetrics();
        int ha = 0;
        if (align == Align.NE || align == Align.NW) ha = fm.getAscent();
        if (align == Align.CENTER) ha = fm.getAscent() / 2;
        float wa = 0.f; // amount to adjust the position.
        if (align == Align.NE || align == Align.SE) wa = 1.0f;
        if (align == Align.CENTER) wa = 0.5f;

        GeneralPath shape = new GeneralPath();

        GrannyTextRenderer gtr = new GrannyTextRenderer();
        Units u = ds.getZUnits();

        if ( ds.getXUnits().isConvertableTo(xAxis.getUnits() ) ) {
            parent.postMessage( this, "inconvertible xaxis units", DasPlot.INFO, null, null );
            return;
        }
        if ( ds.getYUnits().isConvertableTo(yAxis.getUnits() ) ) {
            parent.postMessage( this, "inconvertible yaxis units", DasPlot.INFO, null, null );
            return;
        }

        int count = 0;
        for (int i = 0; i < ds.tableCount(); i++) {
            int nk = ds.getYLength(i);
            for (int j = ds.tableStart(i); j < ds.tableEnd(i); j++) {
                for (int k = 0; k < nk; k++) {
                    int ix = (int) xAxis.transform(ds.getXTagDatum(j));
                    Datum d = ds.getDatum(j, k);
                    String s = d.getFormatter().format(d, u);
                    if (wa > 0.0) ix = ix - (int) (fm.stringWidth(s) * wa);
                    int iy = (int) yAxis.transform(ds.getYTagDatum(i, k)) + ha;
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
        if ( getDataSet() instanceof VectorDataSet ) {
            updateFirstLast( xAxis, yAxis, (VectorDataSet)getDataSet() );
        }
    }


}
