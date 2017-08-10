
package org.das2.graph;

import org.das2.dataset.DataSetDescriptor;
import org.das2.DasException;
import org.das2.util.monitor.ProgressMonitor;
import java.awt.*;
import java.awt.geom.*;
import org.das2.qds.DataSetOps;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;

/**
 * Old renderer that really doesn't do anything that the SeriesRenderer can't do,
 * but is much easier to understand.
 * @author  jbf
 */
public class CurveRenderer extends Renderer {
       
    private final String xplane;
    private final String yplane;
        
    private Color color= Color.black;
    private PsymConnector psymConnector = PsymConnector.SOLID;
    private Psym psym = Psym.NONE;
    private double symSize = 1.0; // radius in pixels
    private float lineWidth = 1.5f; // width in pixels
    
    private GeneralPath path;
        
    /** The dataset descriptor should return a rank 2 QDataSet with time for
     * and a bundle descriptor for BUNDLE_1.  DataSetOps.unbundle is used
     * to extract the xplane and yplane components.
     *
     * @param dsd null or the DataSetDescriptor which can load more data.
     * @param xplane the name of the bundled dataset, or null, or ""
     * @param yplane the name of the bundled dataset, or null, or ""
     * @see org.das2.qds.DataSetOps#unbundle(org.das2.qds.QDataSet, java.lang.String) 
     */
    public CurveRenderer( DataSetDescriptor dsd, String xplane, String yplane ) {
        super(dsd);
        
        setLineWidth( 1.0f );
        
        this.xplane= xplane;
        this.yplane= yplane;
    }
    
    
    @Override
    public void render(java.awt.Graphics g1, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {

        QDataSet dataSet= getDataSet();
        
        if (dataSet == null || dataSet.length() == 0) {
            return;
        }

        QDataSet xds;
        if ( xplane!=null && xplane.length()>1 ) {
            xds= DataSetOps.unbundle( dataSet, xplane );
        } else if ( dataSet.rank()==1 ) {
            xds= SemanticOps.xtagsDataSet(dataSet);
        } else {
            throw new IllegalArgumentException("rank must be 1 or xplane identified");
        }
        
        QDataSet yds;
        if ( yplane!=null && yplane.length()>1 ) {
            yds= DataSetOps.unbundle( dataSet, yplane );
        } else if ( dataSet.rank()==1 ) {
            yds= dataSet;
        } else {
            throw new IllegalArgumentException("rank must be 1 or xplane identified");
        }
        QDataSet wds= SemanticOps.weightsDataSet(xds);

        Graphics2D graphics= (Graphics2D) g1.create();
        
        RenderingHints hints0= graphics.getRenderingHints();

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
        graphics.setColor(color);
        
        if (path != null) {
            psymConnector.draw(graphics, path, (float)lineWidth);
        }
                
        org.das2.datum.Units xUnits= xAxis.getUnits();
        org.das2.datum.Units yUnits= yAxis.getUnits();
        
        for (int index = 0; index < xds.length(); index++) {
            if ( wds.value(index)>0  ) {
                double i = xAxis.transform(xds.value(index),xUnits);
                double j = yAxis.transform(yds.value(index),yUnits);
                psym.draw( g1, i, j, (float)symSize );
            }
        }
                
        graphics.setRenderingHints(hints0);
    }
    
    @Override
    public void updatePlotImage(DasAxis xAxis, DasAxis yAxis, ProgressMonitor monitor) throws DasException {
        super.updatePlotImage( xAxis, yAxis, monitor );
        
        QDataSet dataSet= getDataSet();
        
        if (dataSet == null || dataSet.length() == 0) {
            return;
        }
        
        QDataSet xds;
        if ( xplane!=null && xplane.length()>1 ) {
            xds= DataSetOps.unbundle( dataSet, xplane );
        } else if ( dataSet.rank()==1 ) {
            xds= SemanticOps.xtagsDataSet(dataSet);
        } else {
            throw new IllegalArgumentException("rank must be 1 or xplane identified");
        }
        
        QDataSet yds;
        if ( yplane!=null && yplane.length()>1 ) {
            yds= DataSetOps.unbundle( dataSet, yplane );
        } else if ( dataSet.rank()==1 ) {
            yds= dataSet;
        } else {
            throw new IllegalArgumentException("rank must be 1 or xplane identified");
        }
        
        path= GraphUtil.getPath( xAxis, yAxis, xds, yds, false, false );
    }
    
    /** Getter for property lineWidth.
     * @return Value of property lineWidth.
     *
     */
    public final double getLineWidth() {
        return this.lineWidth;
    }
    
    public static final String PROP_LINE_WIDTH = "lineWidth";
    public static final String PROP_COLOR = "color";
    public static final String PROP_SYM_SIZE = "symSize";
    public static final String PROP_SYM_CONNECTOR = "symConnector";
    public static final String PROP_PSYM = "psym";
    
    /** Setter for property lineWidth.
     * @param lineWidth New value of property lineWidth.
     *
     */
    public final void setLineWidth(double lineWidth) {
        double oldValue= this.lineWidth;
        this.lineWidth = (float)lineWidth;
        updateCacheImage();
        propertyChangeSupport.firePropertyChange( PROP_LINE_WIDTH, oldValue, lineWidth );
    }

    public final double getSymSize() {
        return symSize;
    }

    /**
     * set the symbol size in pixels (ems)
     * @param symSize 
     */
    public final void setSymSize(double symSize) {
        double old= this.symSize;
        this.symSize = symSize;
        updateCacheImage();
        propertyChangeSupport.firePropertyChange( PROP_SYM_SIZE, old, symSize );
    }
    
    public PsymConnector getPsymConnector() {
        return psymConnector;
    }
    
    /**
     * set the type of line connecting plot symbols (e.g. solid, none, dotted).
     * @param p 
     */
    public void setPsymConnector(PsymConnector p) {
        PsymConnector old= this.psymConnector;
        if (p == null) throw new NullPointerException("psymConnector cannot be null");
        psymConnector = p;
        updateCacheImage();
        propertyChangeSupport.firePropertyChange( PROP_SYM_CONNECTOR, old, p );
    }

    /** Getter for property psym.
     * @return Value of property psym.
     */
    public final Psym getPsym() {
        return this.psym;
    }
        
    
    /** Setter for property psym.
     * @param psym New value of property psym.
     */
    public final void setPsym(Psym psym) {
        if (psym == null) throw new NullPointerException("psym cannot be null");
        Psym old= this.psym;
        this.psym = psym;
        updateCacheImage();
        propertyChangeSupport.firePropertyChange( PROP_PSYM, old, psym );
    }

    public Color getColor() {
        return color;
    }

    /**
     * setter for the property color.
     * @param color 
     */
    public void setColor(Color color) {
        this.color = color;
    }
    
}
