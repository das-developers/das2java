/* File: DasLabelAxis.java
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
package org.das2.graph;

import java.awt.Font;
import java.awt.Graphics2D;
import java.text.DecimalFormat;
import java.util.Arrays;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.DatumUtil;
import org.das2.datum.DatumVector;
import org.das2.datum.Units;
import org.das2.datum.format.DatumFormatter;
import org.das2.graph.event.DasUpdateEvent;
import org.das2.graph.event.DasUpdateListener;
import org.das2.util.GrannyTextRenderer;
import org.das2.qds.DataSetUtil;
import org.das2.qds.QDataSet;

public class DasLabelAxis extends DasAxis implements DasUpdateListener {

    DecimalFormat nfy = null;
    DatumVector labels = null;
    double[] labelValues = null;
    Units labelUnits = null;
    int[] labelPositions = null;
    DatumFormatter df = null;
    int indexMinimum;  // first label to be displayed
    int indexMaximum;
    /** Holds value of property outsidePadding. */
    private int outsidePadding = 5;
    /** Holds value of property floppyltemSpacing. */
    private boolean floppyItemSpacing = false;
    // last label to be displayed
    private void setLabels(DatumVector labels) {
        if (labels.getLength() == 0) {
            throw new IllegalArgumentException("labels can not be a zero-length array!");
        }
        this.labels = labels;
        this.labelPositions = new int[labels.getLength()];
        indexMinimum = 0;
        indexMaximum = labels.getLength() - 1;
        labelUnits = labels.getUnits();
        labelValues = labels.toDoubleArray(labelUnits);
        this.df = DatumUtil.bestFormatter(labels);
    }

    /**
     * vg1pws needed a way to explicitly set this.
     */
    public void setLabelFormatter(DatumFormatter df) {
        this.df = df;
    }

    protected DasLabelAxis(DatumVector labels, DataRange dataRange, int orientation) {
        super(dataRange, orientation);
        setLabels(labels);
        getDataRange().addUpdateListener(this);
    }

    public DasLabelAxis(DatumVector labels, int orientation) {
        super(labels.get(0), labels.get(labels.getLength() - 1), orientation, false);
        setLabels(labels);
        getDataRange().addUpdateListener(this);
    }

    public DasLabelAxis( QDataSet labels, int orientation ) {
        this( DataSetUtil.asDatumVector(labels), orientation );
    }
    
    public int[] getLabelPositions() {
        return Arrays.copyOf( this.labelPositions, this.labelPositions.length );
    }

    private void updateTickPositions() {
        if (isDisplayable()) {
            int nlabel = indexMaximum - indexMinimum + 1;

            int size;
            int min;

            double interItemSpacing;

            if (this.getOrientation() == DasAxis.HORIZONTAL) {
                size = getColumn().getWidth() - outsidePadding * 2;
                interItemSpacing = ((float) size) / nlabel;
                if (!floppyItemSpacing) {
                    interItemSpacing = (int) interItemSpacing;
                }
                min = (getColumn().getDMinimum() + outsidePadding + (int) (interItemSpacing / 2));
            } else {
                size = getRow().getHeight() - outsidePadding * 2;
                interItemSpacing = -1 * ((float) size) / nlabel;
                if (!floppyItemSpacing) {
                    interItemSpacing = (int) interItemSpacing;
                }
                min = getRow().getDMaximum() - outsidePadding + (int) (interItemSpacing / 2);
            }

            for (int i = 0; i < labelPositions.length; i++) {
                labelPositions[i] = min + (int) (interItemSpacing * ((i - indexMinimum) + 0));
            }

            firePropertyChange("labelPositions", null, labelPositions);
        }
    }


    @Override
    public Datum findTick(Datum xDatum, double direction, boolean minor) {
        // somehow tickv.minor is set to non-zero, and Axis.findTick gets messed up.
        // This is a work-around...
        return xDatum;
    }

    @Override
    public void updateTickV() {
        //super.updateTickV();
        updateTickPositions();
    }

    @Override
    public TickVDescriptor getTickV() {
        TickVDescriptor result = new TickVDescriptor();
        result.units = getUnits();
        result.tickV = labels.getSubVector(indexMinimum, indexMaximum + 1);
        result.minorTickV = DatumVector.newDatumVector(new double[0], result.units);
        return result;
    }

    @Override
    public double transform(double value, Units units) {
        if (units != this.labelUnits) {
            throw new IllegalArgumentException("units don't match");
        }
        int iclose = findClosestIndex(labelValues, value);
        return labelPositions[iclose];
    }

    private int findClosestIndex(int[] data, int searchFor) {
        int iclose = 0;
        double closest = Math.abs(data[iclose] - searchFor);
        for (int i = 0; i < labelPositions.length; i++) {
            double c1 = Math.abs(data[i] - searchFor);
            if (c1 < closest) {
                iclose = i;
                closest = c1;
            }
        }
        return iclose;
    }

    private int findClosestIndex(double[] data, double searchFor) {
        int iclose = 0;
        double closest = Math.abs(data[iclose] - searchFor);
        for (int i = 0; i < labelPositions.length; i++) {
            double c1 = Math.abs(data[i] - searchFor);
            if (c1 < closest) {
                iclose = i;
                closest = c1;
            }
        }
        return iclose;
    }

    @Override
    public Datum invTransform(double d) {
        int iclose = findClosestIndex(labelPositions, (int) d);
        return labels.get(iclose);
    }

    /**
     * override this to allow a single Datum.
     * @param dr
     * @return 
     */
    @Override
    protected boolean rangeIsAcceptable(DatumRange dr) {
        return true;
    }

    @Override
    protected String tickFormatter( Datum t ) {
        return df.format( t );
    }

    @Override
    protected String[] tickFormatter(DatumVector tickV, DatumRange datumRange) {
	return df.axisFormat( tickV, datumRange );
    }
    
    public int getInterItemSpace() {
        return (int) Math.abs(transform(labels.get(1)) - transform(labels.get(0)));
    }

    /**
     * get the minimum pixel location of the bin allocated to the Datum.
     * @param d
     * @return pixel location of the min.
     */
    public int getItemMin(Datum d) {
        Units units = d.getUnits();
        double value = d.doubleValue(units);

        int iclose = findClosestIndex(labelValues, units.convertDoubleTo(this.getUnits(), value));
        int tickPosition = labelPositions[iclose];
        int w = getInterItemSpace();
        return tickPosition - w / 2;
    }

    /**
     * get the maximum pixel location  of the bin allocated to the Datum.
     * @param d
     * @return  pixel location of the max.
     */
    public int getItemMax(Datum d) {
        int w = getInterItemSpace();
        return getItemMin(d) + w;
    }

    public DasAxis createAttachedAxis(DasRow row, DasColumn column) {
        DasLabelAxis result = new DasLabelAxis(labels, getDataRange(), this.getOrientation());
        return result;
    }

    @Override
    public DasAxis createAttachedAxis(int orientation) {
        return new DasLabelAxis(labels, getDataRange(), orientation);
    }

    public void update(DasUpdateEvent e) {
        double minimum = getDataRange().getMinimum();
        double maximum = getDataRange().getMaximum();
        if (getDataRange().getUnits() != this.labelUnits) {
            throw new IllegalArgumentException("units don't match");
        }

        this.indexMinimum = findClosestIndex(labelValues, minimum);
        this.indexMaximum = findClosestIndex(labelValues, maximum);

        if (this.indexMinimum > this.indexMaximum) {
            int t = this.indexMaximum;
            this.indexMaximum = this.indexMinimum;
            this.indexMinimum = t;
        }


    }

    @Override
    protected void paintHorizontalAxis(java.awt.Graphics2D g) {
        boolean bottomTicks = (getOrientation() == BOTTOM || isOppositeAxisVisible());
        boolean bottomTickLabels = (getOrientation() == BOTTOM && isTickLabelsVisible());
        boolean bottomLabel = (getOrientation() == BOTTOM && !axisLabel.equals(""));
        boolean topTicks = (getOrientation() == TOP || isOppositeAxisVisible());
        boolean topTickLabels = (getOrientation() == TOP && isTickLabelsVisible());
        boolean topLabel = (getOrientation() == TOP && !axisLabel.equals(""));

        int topPosition = getRow().getDMinimum() - 1;
        int bottomPosition = getRow().getDMaximum();
        int DMax = getColumn().getDMaximum();
        int DMin = getColumn().getDMinimum();

        Font labelFont = getTickLabelFont();

        TickVDescriptor ticks = getTickV();

        if (bottomTicks) {
            g.drawLine(DMin, bottomPosition, DMax, bottomPosition);
        }
        if (topTicks) {
            g.drawLine(DMin, topPosition, DMax, topPosition);
        }

        int tickLengthMajor = labelFont.getSize() * 2 / 3;
        int tickLength;

        String[] llabels= tickFormatter( ticks.tickV, getDatumRange() );
        
        for (int i = 0; i < ticks.tickV.getLength(); i++) {
            Datum d = ticks.tickV.get(i);
            int w = getInterItemSpace();
            int tickPosition = (int) Math.floor(transform(d) + 0.5) - w / 2;
            tickLength = tickLengthMajor;
            if (bottomTicks) {
                g.drawLine(getItemMin(d), bottomPosition, getItemMin(d), bottomPosition + tickLength);
                if (i == ticks.tickV.getLength() - 1) {
                    g.drawLine(getItemMax(d), bottomPosition, getItemMax(d), bottomPosition + tickLength);
                }
                if (bottomTickLabels) {
                    drawLabel(g, d, llabels[i], i, tickPosition + w / 2, bottomPosition + tickLength);
                }
            }
            if (topTicks) {
                g.drawLine(getItemMin(d), topPosition, getItemMin(d), topPosition - tickLength);
                if (i == ticks.tickV.getLength() - 1) {
                    g.drawLine(getItemMax(d), topPosition, getItemMax(d), topPosition - tickLength);
                }
                if (topTickLabels) {
                    drawLabel(g, d, llabels[i], i, tickPosition + w / 2, topPosition - tickLength);
                }
            }
        }


        if (!axisLabel.equals("")) {
            Graphics2D g2 = (Graphics2D) g.create();
            int titlePositionOffset = getTitlePositionOffset();
            GrannyTextRenderer gtr = new GrannyTextRenderer();
            gtr.setString(g2, axisLabel);
            int titleWidth = (int) gtr.getWidth();
            int baseline;
            int leftEdge;
            g2.setFont(getLabelFont());
            if (bottomLabel) {
                leftEdge = DMin + (DMax - DMin - titleWidth) / 2;
                baseline = bottomPosition + titlePositionOffset;
                gtr.draw(g2, (float) leftEdge, (float) baseline);
            }
            if (topLabel) {
                leftEdge = DMin + (DMax - DMin - titleWidth) / 2;
                baseline = topPosition - titlePositionOffset;
                gtr.draw(g2, (float) leftEdge, (float) baseline);
            }
            g2.dispose();
        }
    }

    @Override
    protected void paintVerticalAxis(java.awt.Graphics2D g) {
        boolean leftTicks = (getOrientation() == LEFT || isOppositeAxisVisible());
        boolean leftTickLabels = (getOrientation() == LEFT && isTickLabelsVisible());
        boolean leftLabel = (getOrientation() == LEFT && !axisLabel.equals(""));
        boolean rightTicks = (getOrientation() == RIGHT || isOppositeAxisVisible());
        boolean rightTickLabels = (getOrientation() == RIGHT && isTickLabelsVisible());
        boolean rightLabel = (getOrientation() == RIGHT && !axisLabel.equals(""));

        int leftPosition = getColumn().getDMinimum() - 1;
        int rightPosition = getColumn().getDMaximum();
        int DMax = getRow().getDMaximum();
        int DMin = getRow().getDMinimum();

        Font labelFont = getTickLabelFont();

        TickVDescriptor ticks = getTickV();

        if (leftTicks) {
            g.drawLine(leftPosition, DMin, leftPosition, DMax);
        }
        if (rightTicks) {
            g.drawLine(rightPosition, DMin, rightPosition, DMax);
        }

        int tickLengthMajor = labelFont.getSize() * 2 / 3;
        int tickLength;

        String[] llabels= tickFormatter( labels, getDatumRange() );
        for (int i = 0; i < ticks.tickV.getLength(); i++) {
            Datum datum = ticks.tickV.get(i);

            int tickPosition = (getItemMax(datum) + getItemMin(datum)) / 2 - g.getFontMetrics().getAscent() / 5;
            if (getRow().contains(tickPosition)) {
                tickLength = tickLengthMajor;
                if (leftTicks) {
                    if (i == ticks.tickV.getLength() - 1) {
                        g.drawLine(leftPosition, getItemMin(datum), leftPosition - tickLength, getItemMin(datum));
                    }
                    g.drawLine(leftPosition, getItemMax(datum), leftPosition - tickLength, getItemMax(datum));
                    if (leftTickLabels) {
                        drawLabel(g, datum, llabels[i], i, leftPosition - tickLength, tickPosition);
                    }
                }
                if (rightTicks) {
                    if (i == ticks.tickV.getLength() - 1) {
                        g.drawLine(rightPosition, getItemMin(datum), rightPosition + tickLength, getItemMin(datum));
                    }
                    g.drawLine(rightPosition, getItemMax(datum), rightPosition + tickLength, getItemMax(datum));
                    if (rightTickLabels) {
                        drawLabel(g, datum, llabels[i], i, rightPosition + tickLength, tickPosition);
                    }
                }
            }

        }

        if (!axisLabel.equals("")) {
            Graphics2D g2 = (Graphics2D) g.create();
            int titlePositionOffset = getTitlePositionOffset();
            GrannyTextRenderer gtr = new GrannyTextRenderer();
            gtr.setString(g2, axisLabel);
            int titleWidth = (int) gtr.getWidth();
            int baseline;
            int leftEdge;
            g2.setFont(getLabelFont());
            if (leftLabel) {
                g2.rotate(-Math.PI / 2.0);
                leftEdge = -DMax + (DMax - DMin - titleWidth) / 2;
                baseline = leftPosition - titlePositionOffset;
                gtr.draw(g2, (float) leftEdge, (float) baseline);
            }
            if (rightLabel) {
                g2.rotate(Math.PI / 2.0);
                leftEdge = DMin + (DMax - DMin - titleWidth) / 2;
                baseline = -rightPosition - titlePositionOffset;
                gtr.draw(g2, (float) leftEdge, (float) baseline);
            }
            g2.dispose();
        }

    }

    /** Getter for property outsidePadding.
     * @return Value of property outsidePadding.
     *
     */
    public int getOutsidePadding() {
        return this.outsidePadding;
    }

    /** Setter for property outsidePadding.
     * @param outsidePadding New value of property outsidePadding.
     *
     */
    public void setOutsidePadding(int outsidePadding) {
        int oldValue = outsidePadding;
        this.outsidePadding = outsidePadding;
        firePropertyChange("setOutsidePadding", oldValue, outsidePadding);
        updateTickPositions();
        update();
    }

    /** Getter for property floppyltemSpacing.
     * @return Value of property floppyltemSpacing.
     *
     */
    public boolean isFloppyItemSpacing() {
        return this.floppyItemSpacing;
    }

    /** Setter for property floppyltemSpacing.
     * @param floppyItemSpacing New value of property floppyltemSpacing.
     *
     */
    public void setFloppyItemSpacing(boolean floppyItemSpacing) {
        boolean oldValue = this.floppyItemSpacing;
        this.floppyItemSpacing = floppyItemSpacing;
        firePropertyChange("floppyItemSpacing", oldValue, floppyItemSpacing);
        updateTickPositions();
        update();
    }

    @Override
    public java.awt.geom.AffineTransform getAffineTransform(Memento memento, java.awt.geom.AffineTransform at) {
        return at;
    //equals doesn't seem to work
        /*if ( this.getMemento().equals( memento ) ) {
    return at;
    } else {
    return null;
    }*/
    }
}
