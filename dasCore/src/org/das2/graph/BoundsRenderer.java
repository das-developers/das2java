
package org.das2.graph;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.util.LinkedHashMap;
import java.util.Map;
import org.das2.datum.UnitsUtil;
import org.das2.qds.QDataSet;
import org.das2.qds.examples.Schemes;
import org.das2.qds.ops.Ops;

/**
 * Draw the region bounded by the dataset.  If the dataset is a bounding box, the box is drawn.  If the
 * data is a rank 2 time series with bins (N by 2), then it is drawn.
 * @author jbf
 * @see org.das2.qds.examples.Schemes#isBoundingBox(org.das2.qds.QDataSet) 
 */
public class BoundsRenderer extends Renderer {

    private void expectDs() {
        throw new IllegalArgumentException("Expect rank 2 bins dataset");
    }

    @Override
    public boolean acceptsDataSet(QDataSet ds) {
        return ds.rank()==2 && ds.length(0)==2;
    }

    @Override
    public boolean acceptContext(int x, int y) {
        return context==null ? false : context.contains(x, y);
    }
    
    public Area selectionArea() {
        return new Area(context);
    }
    
    private GeneralPath context=null;
    
    public static QDataSet doAutorange( QDataSet ds ) {
        if ( Schemes.isBoundingBox(ds) ) {
            QDataSet xxx= Ops.slice0( ds,0 );
            QDataSet yyy= Ops.slice0( ds,1 );
            xxx= Ops.rescaleRangeLogLin( xxx, -0.1, 1.1 );
            yyy= Ops.rescaleRangeLogLin( yyy, -0.1, 1.1 );
            return Ops.join( xxx, yyy );            
        } else {
            QDataSet mins= Ops.slice1( ds,0 );
            QDataSet maxs= Ops.slice1( ds,1 );
            QDataSet yext= Ops.extent( mins, Ops.extent( maxs ) );
            QDataSet xext= Ops.extent( Ops.xtags(mins), Ops.extent( Ops.xtags(maxs) ) );
            yext= Ops.rescaleRangeLogLin( yext, -0.1, 1.1 );
            xext= Ops.rescaleRangeLogLin( xext, -0.1, 1.1 );
            return Ops.join( xext, yext );
        }
    }
    
    
    private Color color = new Color( 0, 0, 0 );

    private Color fillColor= new Color( 0, 0, 0, 128 );
    
    public static final String PROP_COLOR = "color";

    public static final String PROP_FILL_COLOR = "fillColor";
        @Override
    public String getControl() {
        Map<String,String> controls= new LinkedHashMap();
        controls.put( "fillColor", encodeColorControl(fillColor) );
        controls.put( "color", encodeColorControl(color) );
        return Renderer.formatControl(controls);
    }
    

    @Override
    public void setControl(String s) {
        super.setControl(s);
        this.color= getColorControl( "color", color );
        this.fillColor= getColorControl( "fillColor", fillColor );
    }    
    
    public Color getColor() {
        return color;
    }

    public void setColor(Color string) {
        Color oldColor = this.color;
        this.color = string;
        propertyChangeSupport.firePropertyChange(PROP_COLOR, oldColor, string);
    }

    public Color getFillColor() {
        return fillColor;
    }
    
    public void setFillColor( Color color ) {
        Color oldColor = this.fillColor;
        this.fillColor = color;
        propertyChangeSupport.firePropertyChange(PROP_COLOR, oldColor, color );
    }
    
    @Override
    public void render(Graphics2D g1, DasAxis xAxis, DasAxis yAxis ) {
        Graphics2D g= (Graphics2D)g1;
        QDataSet d= this.getDataSet();
        if ( ds==null ) {
            if ( getLastException()!=null ) {
                renderException(g, xAxis, yAxis, lastException);
            } else {
                getParent().postMessage(this, "no data set", DasPlot.INFO, null, null);
            }
            return;
        }
        if ( d.rank()!=2 ) expectDs();
        QDataSet mins;
        QDataSet maxs;
        if ( Schemes.isBoundingBox(ds) ) {
            //mins= Ops.dataset( ds.slice(0).slice(0), ds.slice(1).slice(0) )
            mins= Ops.link( 
                Ops.slice0( d,0 ), 
                Ops.join( d.slice(1).slice(0), d.slice(1).slice(0) ) );
            maxs= Ops.link( 
                Ops.slice0( d,0 ), 
                Ops.join( d.slice(1).slice(1), d.slice(1).slice(1) ) );
            if ( mins.property(QDataSet.UNITS)==null && UnitsUtil.isRatioMeasurement(yAxis.getUnits()) ) {
                mins= Ops.putProperty( mins, QDataSet.UNITS, yAxis.getUnits() );
            }
            if ( maxs.property(QDataSet.UNITS)==null && UnitsUtil.isRatioMeasurement(yAxis.getUnits()) ) {
                maxs= Ops.putProperty( maxs, QDataSet.UNITS, yAxis.getUnits() );
            }
            GeneralPath pbox= GraphUtil.getPath(xAxis,yAxis,
                Ops.append(mins,Ops.append(Ops.reverse(maxs),mins.slice(0))),false,false);
            g.setColor( this.fillColor );
            g.fill(pbox);
            g.setColor( this.getColor() );
            g.draw(pbox);
            context= pbox;
        } else {
            mins= Ops.slice1( d,0 );
            maxs= Ops.slice1( d,1 );
            if ( mins.property(QDataSet.UNITS)==null && UnitsUtil.isRatioMeasurement(yAxis.getUnits()) ) {
                mins= Ops.putProperty( mins, QDataSet.UNITS, yAxis.getUnits() );
            }
            if ( maxs.property(QDataSet.UNITS)==null && UnitsUtil.isRatioMeasurement(yAxis.getUnits()) ) {
                maxs= Ops.putProperty( maxs, QDataSet.UNITS, yAxis.getUnits() );
            }            
            GeneralPath path= GraphUtil.getPath(xAxis,yAxis,
                Ops.append(mins,Ops.append(Ops.reverse(maxs),mins.slice(0))),false,false);
            g.setColor( this.fillColor );
            g.fill(path);
            g.setColor( this.getColor() );
            g.draw(path);
            context= path;
        }
        
    }
    
    
}
