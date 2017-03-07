/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.graph;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.util.IllegalFormatConversionException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.das2.DasException;
import org.virbo.dataset.DataSetUtil;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.DatumRangeUtil;
import org.das2.datum.InconvertibleUnitsException;
import org.das2.datum.Units;
import org.das2.datum.UnitsConverter;
import org.das2.datum.UnitsUtil;
import org.das2.datum.format.DatumFormatter;
import org.das2.datum.format.DefaultDatumFormatter;
import static org.das2.graph.Renderer.CONTROL_KEY_COLOR;
import static org.das2.graph.Renderer.encodeColorControl;
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
     * @param ds the dataset
     * @return a bounding box 
     * @see org.virbo.dataset.examples.Schemes#boundingBox() 
     */
    public static QDataSet doAutorange( QDataSet ds ) {

        QDataSet xds;
        QDataSet yds;

        if ( ds.rank()==0 ) {
            JoinDataSet bds= new JoinDataSet(2);
            bds.join( DDataSet.wrap( new double[] { 0,1 } ) );
            bds.join( DDataSet.wrap( new double[] { 0,1 } ) );
            return bds;
            
        } else {
            xds= SemanticOps.xtagsDataSet(ds);
            yds= SemanticOps.ytagsDataSet(ds);

            QDataSet xrange= doRange( xds );
            QDataSet yrange= doRange( yds );

            JoinDataSet bds= new JoinDataSet(2);
            bds.join(xrange);
            bds.join(yrange);

            return bds;
        }

    }

    private static QDataSet doRange( QDataSet xds ) {
        if ( UnitsUtil.isNominalMeasurement( SemanticOps.getUnits(xds) ) ) {
            return DataSetUtil.asDataSet( DatumRangeUtil.newDimensionless(0,10) );
        }
        QDataSet xrange= Ops.extent(xds);
        if ( xrange.value(1)==xrange.value(0) ) {
            if ( !"log".equals( xrange.property(QDataSet.SCALE_TYPE)) ) {
                xrange= DDataSet.wrap( new double[] { xrange.value(0)-1, xrange.value(1)+1 } ).setUnits( SemanticOps.getUnits(xrange) );
            } else {
                xrange= DDataSet.wrap( new double[] { xrange.value(0)/10, xrange.value(1)*10 } ).setUnits( SemanticOps.getUnits(xrange) );
            }
        }
        xrange= Ops.rescaleRangeLogLin(xrange, -0.1, 1.1 );
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
        updateCacheImage();
        propertyChangeSupport.firePropertyChange(PROP_COLOR, oldColor, color);
        propertyChangeSupport.firePropertyChange(PROP_CONTROL, null, getControl() );
    }

    public enum Align {
        SW, NW, NE, SE, CENTER,
    }
    protected Align align = Align.CENTER;
    public static final String PROP_ALIGN = "align";

    public Align getAlign() {
        return align;
    }

    /**
     * For the box containing the digital number, which corner is anchored 
     * to the data.  Note this is consistent with the anchorPosition property of
     * annotations.
     * @param align 
     */
    public void setAlign(Align align) {
        Align oldAlign = this.align;
        this.align = align;
        updateCacheImage();
        propertyChangeSupport.firePropertyChange(PROP_ALIGN, oldAlign, align);
        propertyChangeSupport.firePropertyChange(PROP_CONTROL, null, getControl() );
    }

    private PlotSymbol plotSymbol = DefaultPlotSymbol.NONE;

    public static final String PROP_PLOTSYMBOL = "plotSymbol";

    public PlotSymbol getPlotSymbol() {
        return plotSymbol;
    }

    public void setPlotSymbol(PlotSymbol plotSymbol) {
        PlotSymbol oldPlotSymbol = this.plotSymbol;
        this.plotSymbol = plotSymbol;
        propertyChangeSupport.firePropertyChange(PROP_PLOTSYMBOL, oldPlotSymbol, plotSymbol);
        updateCacheImage();
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
        updateCacheImage();
        propertyChangeSupport.firePropertyChange(PROP_FORMAT, oldValue, value );
        propertyChangeSupport.firePropertyChange(PROP_CONTROL, null, getControl() );
    }


    /**
     * font size, 0 indicates the plot font should be used.
     * TODO: this should be relativeSize.  Add a property for this as well.
     */
    public static final String PROP_SIZE= "size";

    double size= 0.0;

    public double getSize() {
        return size;
    }

    public void setSize(double size) {
        double oldValue= this.size;
        this.size = size;
        updateCacheImage();
        propertyChangeSupport.firePropertyChange(PROP_FORMAT, oldValue, size );
        propertyChangeSupport.firePropertyChange(PROP_CONTROL, null, getControl() );
    }

    private String fontSize = "";

    /**
     * font size, expressed as relative string like "1em" or "6pt"
     */
    public static final String PROP_FONTSIZE = "fontSize";

    public String getFontSize() {
        return fontSize;
    }

    public void setFontSize(String fontSize) {
        String oldFontSize = this.fontSize;
        this.fontSize = fontSize;
        updateCacheImage();
        propertyChangeSupport.firePropertyChange(PROP_FONTSIZE, oldFontSize, fontSize);
    }

    private String fillLabel = "fill";

    public static final String PROP_FILLLABEL = "fillLabel";

    public String getFillLabel() {
        return fillLabel;
    }

    /**
     * the label printed where fill data (invalid data placeholder) is found.
     * @param fillLabel the label such as "fill" or "" for nothing.
     */
    public void setFillLabel(String fillLabel) {
        String oldFillLabel = this.fillLabel;
        this.fillLabel = fillLabel;
        propertyChangeSupport.firePropertyChange(PROP_FILLLABEL, oldFillLabel, fillLabel);
    }

    @Override
    public void setControl(String s) {
        super.setControl(s);
        this.size= getDoubleControl( "size", this.size );
        this.color= getColorControl( "color",  color );
        this.fontSize= getControl( "fontSize", fontSize );
        this.format= getControl( "format", format );
        try {
            this.align= Align.valueOf( getControl("align","CENTER") );
        } catch (IllegalArgumentException ex ){
            this.align= Align.CENTER;
        }
        this.fillLabel= getControl( "fillLabel", fillLabel );
    }
    
    @Override
    public String getControl() {
        Map<String,String> controls= new LinkedHashMap();
        controls.put( "size", String.valueOf(this.size) );
        controls.put( "fontSize", this.fontSize );
        controls.put( CONTROL_KEY_COLOR, encodeColorControl( color ) );
        controls.put( "format", format );
        controls.put( "align", align.toString() );
        controls.put( "fillLabel", fillLabel );
        return Renderer.formatControl(controls);
    }
    
    Shape selectionArea;

    /**
     * like accept context, but provides a shape to indicate selection.  This
     * should be roughly the same as the locus of points where acceptContext is
     * true.
     * @return
     */
    public Shape selectionArea() {
        return selectionArea==null ? SelectionUtil.NULL : selectionArea;
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
    private void updateFirstLast(DasAxis xAxis, DasAxis yAxis, QDataSet dataSet) {

        Units xUnits = xAxis.getUnits();
    
        int ixmax;
        int ixmin;

        if ( dataSet==null ) return;
        
        if ( !UnitsUtil.isIntervalOrRatioMeasurement( SemanticOps.getUnits(dataSet) ) ) {
            firstIndex=0;
            lastIndex= Math.min( dataSet.length(), this.dataSetSizeLimit );
            return;
        }
        
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

        boolean xMono = SemanticOps.isMonotonic(xds);

        if ( SemanticOps.isBins(dataSet) ) {
            ixmin = 0;
            ixmax = dataSet.length();
            
        } else if ( xMono ) {
            DatumRange visibleRange = xAxis.getDatumRange();
            DasPlot parent= getParent();
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

        dataSetClipped = index < ixmax && pointsPlotted == dataSetSizeLimit;

        lastIndex = index;

    }

    @Override
    public void render(Graphics g, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {
        
        g.setColor(color);
        DasPlot parent= getParent();
                
        if ( ds==null ) {
            if ( getLastException()!=null ) {
                renderException(g, xAxis, yAxis, lastException);
            } else {
                parent.postMessage(this, "no data set", DasPlot.INFO, null, null);
            }
            return;
        }
        try {
            if ( ds.rank()==0 || ( ds.rank()==1 && SemanticOps.isRank1Bundle(ds) ) ) {
                renderRank0( ds, g, xAxis, yAxis, mon);
            } else if ( SemanticOps.isBins(ds) ) {
                renderRank0( ds, g, xAxis, yAxis, mon);
            } else if ( UnitsUtil.isOrdinalMeasurement( SemanticOps.getUnits(ds) ) ) {
                renderRank0( ds, g, xAxis, yAxis, mon);
            } else if ( ! SemanticOps.isTableDataSet(ds) ) {
                renderRank1( ds, g, xAxis, yAxis, mon, firstIndex, lastIndex );
            } else if ( ds.rank()!=2 ) {
                parent.postMessage(this, "unable to render rank "+ds.rank()+" data", DasPlot.WARNING, null, null);
            } else {
                renderRank2( ds, g, xAxis, yAxis, mon);
            }
        } catch ( InconvertibleUnitsException ex ) {
            parent.postMessage(this, "inconvertible units", DasPlot.INFO, null, null);
        }
    }

    private void renderRank0( QDataSet ds, Graphics g, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {
        DasPlot parent= getParent();
        StringBuilder sb= new StringBuilder();
        if ( ds.rank()==0 ) {
            String label= (String)ds.property( QDataSet.LABEL );
            if ( label==null ) {
                label= (String) ds.property( QDataSet.NAME );
            }
            if ( label!=null ) {
                sb.append(label).append( "=");
            }
            Datum d=  DataSetUtil.asDatum(ds);
            if ( d.isFill() ) {
                sb.append(fillLabel).append(" (").append(ds.value()).append( ")");
            } else {
                sb.append(d.toString());
            }
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

    /**
     * 
     * @param ds
     * @param g1
     * @param xAxis
     * @param yAxis
     * @param mon
     * @param firstIndexx
     * @param lastIndexx
     */
    private void renderRank1( QDataSet ds, Graphics g1, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon, int firstIndexx, int lastIndexx ) {
        
        Graphics2D g= (Graphics2D)g1;
        
        Font f0= g.getFont();
        if ( size>0 ) { // legacy support
            Font f= f0.deriveFont((float)size);
            g.setFont(f);
        } else {
            setUpFont( g, fontSize );
        }
        
        FontMetrics fm = g.getFontMetrics();

        int ha = 0;
        if (align == Align.NE || align == Align.NW) {
            ha = fm.getAscent();
            if ( plotSymbol!=DefaultPlotSymbol.NONE ) ha+=3;
        } else if (align == Align.CENTER) {
            ha = fm.getAscent() / 2;
        } else {
            if ( plotSymbol!=DefaultPlotSymbol.NONE ) ha-=3;
        }
        float wa = 0.f; // amount to adjust the position.
        int widthSymbolOffset;
        if (align == Align.NE || align == Align.SE) {
            wa = 1.0f;
            widthSymbolOffset= -3;
        } else if (align == Align.CENTER) {
            wa = 0.5f;
            widthSymbolOffset= 0;
        } else {
            widthSymbolOffset= 3;
        }

        GeneralPath shape = new GeneralPath();

        GrannyTextRenderer gtr = new GrannyTextRenderer();

        QDataSet xds= SemanticOps.xtagsDataSet(ds);
        QDataSet yds= SemanticOps.ytagsDataSet(ds);

        QDataSet zds= (QDataSet) yds.property(QDataSet.PLANE_0);
        if ( zds==null && ds.rank()==2 && ds.length(0)==3 ) {
            zds= DataSetOps.unbundle(ds,2);
        }
        if ( zds==null ) zds= yds;
        Units u = SemanticOps.getUnits(zds);

        Units xunits= SemanticOps.getUnits(xds);
        Units yunits= SemanticOps.getUnits(yds);

        String form=this.format;
        String dsformat= (String) zds.property(QDataSet.FORMAT);
        boolean isInts= false;
        boolean isLongs= false;
        
        if ( form.length()==0 && dsformat!=null ) {
            form= dsformat.trim();
        }
        if ( form.length()==0 ) {
            form= "%.2f";
        }
        if ( form.endsWith("x") || form.endsWith("X")
            || form.endsWith("d") || form.endsWith("o") ) {
            isLongs= true; 
        } else if ( form.endsWith("c") || form.endsWith("C") ) {
            isInts= true; 
        }        

        DasPlot parent= getParent();

        if ( ! xunits.isConvertibleTo(xAxis.getUnits() ) ) {
            parent.postMessage( this, "inconvertible xaxis units", DasPlot.INFO, null, null );
            if ( UnitsUtil.isRatioMeasurement(xunits) ) {
                xunits= xAxis.getUnits();    
            } else {
                return;
            }
        }
        if ( ! yunits.isConvertibleTo(yAxis.getUnits() ) ) {
            parent.postMessage( this, "inconvertible yaxis units", DasPlot.INFO, null, null );
            if ( UnitsUtil.isRatioMeasurement(yunits) ) {
                yunits= yAxis.getUnits();
            } else {
                return;
            }
        }

        QDataSet wds= SemanticOps.weightsDataSet(zds);

        for (int i = firstIndexx; i < lastIndexx; i++) {
            int ix = (int) xAxis.transform( xds.value(i), xunits );

            String s;
            int iy;
            if ( wds.value(i)>0 ) {
                Datum d = u.createDatum( zds.value(i) );
                Datum y = yunits.createDatum( yds.value(i) );
                DatumFormatter df= d.getFormatter();
                if ( df instanceof DefaultDatumFormatter ) {
                    if ( isInts ) {
                        s = String.format( form, (int)zds.value(i) );
                    } else if ( isLongs ) {
                        s = String.format( form, (long)zds.value(i) );
                    } else {
                        s = String.format( form, zds.value(i) );
                    }
                } else {
                    s = d.getFormatter().format(d, u);
                }                
                iy = (int) yAxis.transform(y);
                if ( plotSymbol!=DefaultPlotSymbol.NONE ) {
                    plotSymbol.draw( g, ix,  yAxis.transform(y), 3, FillStyle.STYLE_FILL );
                }
                iy= iy + ha;
                
            } else {
                Datum y = yunits.createDatum( yds.value(i) );
                s = fillLabel;
                if ( y.isFill() ) {
                    iy= (int) yAxis.getRow().getDMaximum();
                } else {
                    iy = (int) yAxis.transform(y);
                    if ( plotSymbol!=DefaultPlotSymbol.NONE ) {
                        plotSymbol.draw( g, ix,  yAxis.transform(y), 3, FillStyle.STYLE_FILL );
                    }
                    iy= iy + ha;
                }
            }
            
            if (wa > 0.0) ix = ix - (int) (fm.stringWidth(s) * wa);
            ix+= widthSymbolOffset;
                
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

    private void renderRank2( QDataSet ds, Graphics g1, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {
        
        QDataSet ds1;
        if ( firstIndex<lastIndex ) {
            ds1= ds.trim( firstIndex, lastIndex );
            if ( ds1.property(QDataSet.DEPEND_0)==null ) {
                ds1= Ops.putProperty(ds1,QDataSet.DEPEND_0,Ops.linspace(firstIndex,lastIndex-1,lastIndex-firstIndex) );
            }
            if ( ds1.property(QDataSet.DEPEND_1)==null ) {
                ds1= Ops.putProperty(ds1,QDataSet.DEPEND_1,Ops.dindgen(ds1.length(0)));
            }
        } else {
            ds1= ds;
            if ( ds1.property(QDataSet.DEPEND_0)==null ) {
                ds1= Ops.putProperty(ds1,QDataSet.DEPEND_0,Ops.linspace(firstIndex,lastIndex-1,firstIndex-lastIndex) );
            }
            if ( ds1.property(QDataSet.DEPEND_1)==null ) {
                ds1= Ops.putProperty(ds1,QDataSet.DEPEND_1,Ops.dindgen(ds1.length(0)));
            }
        }

        QDataSet fds= DataSetOps.flattenRank2(ds1);

        renderRank1( fds, g1, xAxis, yAxis, mon, 0, fds.length() );
    }

    @Override
    public boolean acceptContext(int x, int y) {
        if ( selectionArea!=null ) {
            return selectionArea.contains( x, y );
        } else {
            return false;
        }
    }

    @Override
    public void updatePlotImage(DasAxis xAxis, DasAxis yAxis, ProgressMonitor monitor) throws DasException {
        
        super.incrementUpdateCount();
        
        QDataSet lds= getDataSet();
        if ( lds==null ) return;
        if ( lds.rank()==0 || ( getDataSet().rank()==1 && SemanticOps.isRank1Bundle(lds) ) ) {
            // nothin
        } else {
            try {
                updateFirstLast( xAxis, yAxis, lds );
            } catch ( InconvertibleUnitsException ex ) {
                // nothin
            }
        }
    }


}
