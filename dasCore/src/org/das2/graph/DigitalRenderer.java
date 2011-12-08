/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.graph;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.util.IllegalFormatConversionException;
import org.das2.DasException;
import org.virbo.dataset.DataSetUtil;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import org.das2.datum.UnitsUtil;
import org.das2.datum.format.DatumFormatter;
import org.das2.datum.format.DefaultDatumFormatter;
import org.das2.util.GrannyTextRenderer;
import org.das2.util.monitor.ProgressMonitor;
import org.virbo.dataset.DDataSet;
import org.virbo.dataset.DataSetOps;
import org.virbo.dataset.JoinDataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SemanticOps;
import org.virbo.dsops.Ops;

/**
 *
 * @author jbf
 */
public class DigitalRenderer extends Renderer {

    /**
     * autorange on the data, returning a rank 2 bounds for the dataset.
     *
     * @param fillDs
     * @return
     */
    public static QDataSet doAutorange( QDataSet ds ) {

        QDataSet xds;
        QDataSet yds;

        xds= SemanticOps.xtagsDataSet(ds);
        yds= SemanticOps.ytagsDataSet(ds);

        QDataSet xrange= doRange( xds );
        QDataSet yrange= doRange( yds );

        JoinDataSet bds= new JoinDataSet(2);
        bds.join(xrange);
        bds.join(yrange);

        return bds;

    }

    private static QDataSet doRange( QDataSet xds ) {
        QDataSet xrange= Ops.extent(xds);
        if ( xrange.value(1)==xrange.value(0) ) {
            if ( !"log".equals( xrange.property(QDataSet.SCALE_TYPE)) ) {
                xrange= DDataSet.wrap( new double[] { xrange.value(0)-1, xrange.value(1)+1 } ).setUnits( SemanticOps.getUnits(xrange) );
            } else {
                xrange= DDataSet.wrap( new double[] { xrange.value(0)/10, xrange.value(1)*10 } ).setUnits( SemanticOps.getUnits(xrange) );
            }
        }
        xrange= Ops.rescaleRange( xrange, -0.1, 1.1 );
        xrange= Ops.rescaleRange( xrange, -0.1, 1.1 );
        return xrange;
    }

    protected Color color = Color.BLACK;
    public static final String PROP_COLOR = "color";

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        Color oldColor = this.color;
        this.color = color;
        refresh();
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

    /**
     * format, empty string means use either dataset's format, or %.2f
     */
    public static final String PROP_FORMAT= "format";

    private String format="";

    public String getFormat() {
        return format;
    }

    /**
     * note the dataset's format will be used if this is "", and "%.2f" if no
     * format is found there.
     * @param value
     */
    public void setFormat(String value) {
        String oldValue= this.format;
        this.format = value;
        refresh();
        propertyChangeSupport.firePropertyChange(PROP_FORMAT, oldValue, value );
    }


    /**
     * font size, 0 indicates the plot font should be used.
     */
    public static final String PROP_SIZE= "size";

    double size= 0.0;

    public double getSize() {
        return size;
    }

    public void setSize(double size) {
        double oldValue= this.size;
        this.size = size;
        refresh();
        propertyChangeSupport.firePropertyChange(PROP_FORMAT, oldValue, size );
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

        if ( dataSet==null ) return;
        
        QDataSet wds;
        if ( dataSet.rank()==0 ) {
            wds= SemanticOps.weightsDataSet(dataSet);
        } else if ( dataSet.rank()==1 ) {
            wds= SemanticOps.weightsDataSet(dataSet);
        } else if ( SemanticOps.isSimpleTableDataSet(dataSet) ) {
            wds= SemanticOps.weightsDataSet(DataSetOps.slice1(dataSet,0));
        } else {
            firstIndex=0;
            lastIndex= Math.min( dataSet.length(), this.dataSetSizeLimit );
            return;
        }
        QDataSet xds= SemanticOps.xtagsDataSet(dataSet);

        Boolean xMono = SemanticOps.isMonotonic(xds);


        if ( SemanticOps.isBins(dataSet) ) {
            ixmin = 0;
            ixmax = dataSet.length();
            
        } else if ( xMono != null && xMono.booleanValue()) {
            DatumRange visibleRange = xAxis.getDatumRange();
            if (parent.isOverSize()) {
                Rectangle plotBounds = parent.getUpdateImageBounds();
                if ( plotBounds!=null ) {
                    visibleRange = new DatumRange(xAxis.invTransform(plotBounds.x), xAxis.invTransform(plotBounds.x + plotBounds.width));
                }

            }
            ixmin = DataSetUtil.getPreviousIndex(xds, visibleRange.min());
            ixmax = DataSetUtil.getNextIndex(xds, visibleRange.max()) + 1; // +1 is for exclusive.

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
        if ( ds==null ) {
            if ( getLastException()!=null ) {
                renderException(g, xAxis, yAxis, lastException);
            } else {
                parent.postMessage(this, "no data set", DasPlot.INFO, null, null);
            }
            return;
        }
        if ( ds.rank()==0 || ( ds.rank()==1 && SemanticOps.isRank1Bundle(ds) ) ) {
            renderRank0( ds, g, xAxis, yAxis, mon);
        } else if ( SemanticOps.isBins(ds) ) {
            renderRank0( ds, g, xAxis, yAxis, mon);
        } else if ( UnitsUtil.isOrdinalMeasurement( SemanticOps.getUnits(ds) ) ) {
            renderRank0( ds, g, xAxis, yAxis, mon);
        } else if ( ! SemanticOps.isTableDataSet(ds) ) {
            renderRank1( ds, g, xAxis, yAxis, mon);
        } else {
            renderRank2( ds, g, xAxis, yAxis, mon);
        }
    }

    private void renderRank0( QDataSet ds, Graphics g, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {
        StringBuilder sb= new StringBuilder();
        if ( ds.rank()==0 ) {
            String label= (String)ds.property( QDataSet.LABEL );
            if ( label==null ) {
                label= (String) ds.property( QDataSet.NAME );
            }
            if ( label!=null ) {
                sb.append(label).append( "=");
            }
            sb.append( DataSetUtil.asDatum(ds).toString() );
        } else if ( SemanticOps.isBins(ds) ) {
            String label= (String)ds.property( QDataSet.LABEL );
            if ( label==null ) {
                label= (String) ds.property( QDataSet.NAME );
            }
            if ( label!=null ) {
                sb.append(label).append( "=");
            }
            sb.append( DataSetUtil.toString(ds) );
        } else if ( SemanticOps.isBundle(ds) ) {
            for ( int i=0; i<Math.min(4,ds.length()); i++ ) {
                if ( i>0 ) sb.append(", ");
                QDataSet ds1= DataSetOps.unbundle(ds, i);
                String label= (String)ds1.property( QDataSet.LABEL );
                if ( label==null ) {
                    label= (String) ds1.property( QDataSet.NAME );
                }
                if ( label!=null ) {
                    sb.append(label).append( "=");
                } 
                sb.append(  DataSetUtil.asDatum(ds1).toString() );
            }
        } else {
            for ( int i=0; i<Math.min(4,ds.length()); i++ ) {
                if ( i>0 ) sb.append(", ");
                QDataSet ds1= ds.slice(i);

                sb.append(  DataSetUtil.asDatum(ds1).toString() );
            }
            if ( ds.length()>4 ) {
                sb.append(  ", ..." );
            }
        }
        parent.postMessage(this, sb.toString(), DasPlot.INFO, null, null);
    }

    private void renderRank1( QDataSet ds, Graphics g, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {
        Font f0= g.getFont();
        Font f= f0;
        if ( size>0 ) {
            f= f0.deriveFont((float)size);
        }
        g.setFont(f);
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
        QDataSet yds= SemanticOps.ytagsDataSet(ds);
        Units xunits= SemanticOps.getUnits(xds);

        String form=this.format;
        String dsformat= (String) ds.property(QDataSet.FORMAT);
        if ( form.length()==0 && dsformat!=null ) {
            form= dsformat;
        }
        if ( form.length()==0 ) {
            form= "%.2f";
        }

        int count = 0;
        
        if ( ! xunits.isConvertableTo(xAxis.getUnits() ) ) {
            parent.postMessage( this, "inconvertible xaxis units", DasPlot.INFO, null, null );
            return;
        }
        if ( ! u.isConvertableTo(yAxis.getUnits() ) ) {
            parent.postMessage( this, "inconvertible yaxis units", DasPlot.INFO, null, null );
            return;
        }

        QDataSet wds= SemanticOps.weightsDataSet(yds);

        for (int i = firstIndex; i < lastIndex; i++) {
            int ix = (int) xAxis.transform( xds.value(i), xunits );

            String s;
            int iy;
            if ( wds.value(i)>0 ) {
                Datum d = u.createDatum( yds.value(i) );
                DatumFormatter df= d.getFormatter();
                if ( df instanceof DefaultDatumFormatter ) {
                    try {
                        s = String.format( form, yds.value(i) );
                    } catch ( IllegalFormatConversionException ex ) { // '%2X'
                        char c= ex.getConversion();
                        if ( c=='X' || c=='x' || c=='d' || c=='o' || c=='c' || c=='C'  ) {
                            s = String.format( form, (long)yds.value(i) );
                        } else {
                            throw ex;
                        }
                    }
                } else {
                    s = d.getFormatter().format(d, u);
                }
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
        g.setFont(f0);
        
    }

    private void renderRank2( QDataSet ds, Graphics g, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {
        Font f0= g.getFont();
        Font f= f0;
        if ( size>0 ) {
            f= f0.deriveFont((float)size);
        }
        g.setFont(f);
        FontMetrics fm = g.getFontMetrics();

        int ha = 0;
        if (align == Align.NE || align == Align.NW) ha = fm.getAscent();
        if (align == Align.CENTER) ha = fm.getAscent() / 2;
        float wa = 0.f; // amount to adjust the position.
        if (align == Align.NE || align == Align.SE) wa = 1.0f;
        if (align == Align.CENTER) wa = 0.5f;

        GeneralPath shape = new GeneralPath();

        GrannyTextRenderer gtr = new GrannyTextRenderer();

        QDataSet xds= SemanticOps.xtagsDataSet(ds);
        QDataSet yds= SemanticOps.ytagsDataSet(ds);
        QDataSet fds= DataSetOps.flattenRank2(ds);
        QDataSet zds;
        Units u = SemanticOps.getUnits(ds);

        if ( fds.length(0)>1 ) {
            xds= DataSetOps.unbundle(fds, 0);
            yds= DataSetOps.unbundle(fds, 1);
            zds= DataSetOps.unbundle(fds, fds.length(0)-1 );
        } else {
            xds= Ops.div( Ops.dindgen(fds.length()), DataSetUtil.asDataSet(ds.length(0) ) );
            yds= Ops.mod( Ops.dindgen(fds.length()), DataSetUtil.asDataSet(ds.length(0) ) );
            zds= fds;
        }

        String form=this.format;
        String dsformat= (String) ds.property(QDataSet.FORMAT);
        if ( form.length()==0 && dsformat!=null ) {
            form= dsformat;
        }
        if ( form.length()==0 ) {
            form= "%.2f";
        }

        Units xunits= SemanticOps.getUnits(xds);
        Units yunits= SemanticOps.getUnits(yds);

        if ( !SemanticOps.getUnits(xds).isConvertableTo(xAxis.getUnits() ) ) {
            parent.postMessage( this, "inconvertible xaxis units", DasPlot.INFO, null, null );
            return;
        }
        if ( !SemanticOps.getUnits(yds).isConvertableTo(yAxis.getUnits() ) ) {
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
                DatumFormatter df= d.getFormatter();
                if ( df instanceof DefaultDatumFormatter ) {
                    s = String.format( form, zds.value(i) );
                } else {
                    s = d.getFormatter().format(d, u);
                }
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
        g.setFont(f0);
    }

    @Override
    public boolean acceptContext(int x, int y) {
        if ( selectionArea!=null ) {
            return selectionArea.contains( x, y );
        } else {
            return true;
        }
    }

    @Override
    public void updatePlotImage(DasAxis xAxis, DasAxis yAxis, ProgressMonitor monitor) throws DasException {
        QDataSet ds= getDataSet();
        if ( ds==null ) return;
        if ( ds.rank()==0 || ( getDataSet().rank()==1 && SemanticOps.isRank1Bundle(ds) ) ) {
            // nothin
        } else {
            updateFirstLast( xAxis, yAxis, ds );
        }
    }


}
