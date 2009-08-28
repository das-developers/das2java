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
import org.das2.dataset.TableDataSet;
import org.das2.dataset.VectorDataSet;
import org.das2.datum.Datum;
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

        for (int i = 0; i < ds.getXLength(); i++) {
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
            if (count++ > 10000) {
                getParent().postMessage(this, "10000 data point limit reached", DasPlot.WARNING, null, null);
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
                        getParent().postMessage(this, "10000 data point limit reached", DasPlot.WARNING, null, null);
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
}
