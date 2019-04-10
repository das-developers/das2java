
package test.graph;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import org.das2.datum.Units;
import org.das2.graph.DasAxis;
import org.das2.graph.Renderer;
import org.das2.qds.QDataSet;

/**
 * Demonstration of defining a new Renderer.  This can be compiled into the
 * application and then added to the DasPlot.
 * 
 * This example Renderer takes a Rank 2 ds[n,4].  Each record is the x and y
 * positions, then the radius, then the color.
 * 
 * @author jbf
 */
public class MyRenderer extends Renderer {

    @Override
    public void render(Graphics2D g, DasAxis xAxis, DasAxis yAxis ) {
        if ( ds==null || ds.rank()!=2 || ds.length(0)!=4 ) {
            getParent().postMessage( this, "dataset not ready or appropriate", Level.INFO, null, null );
            return;
        }
        QDataSet bds= (QDataSet) ds.property(QDataSet.BUNDLE_1);
        Units xunits= (Units) bds.property(QDataSet.UNITS,0);
        Units yunits= (Units) bds.property(QDataSet.UNITS,1);
        for ( int i=0; i<ds.length(); i++ ) {
            QDataSet ds1= ds.slice(i);
            int ix= (int)xAxis.transform( ds1.value(0), xunits );
            int iy= (int)yAxis.transform( ds1.value(1), yunits );
            int radius= (int)( scale * ( xAxis.transform( ds1.value(0) + ds1.value(2), yunits ) - ix ) );
            int irgb= (int)ds1.value(3);
            g.setColor( new Color(irgb) );
            g.fillOval( ix-radius, iy-radius, radius*2, radius*2 );
        }
    }
    
    /**
     * scale for radius
     */
    private double scale= 1.0;
    
    /**
     * return the scale for the radius
     * @return the scale for the radius
     */
    public double getScale() {
        return scale;
    }
    
    /**
     * the scale for the radius
     * @param s the scale for the radius
     */
    public void setScale( double s ) {
        this.scale = s;
        update();
    }

    /**
     * set the control string.
     * @param s the control string
     */
    @Override
    public void setControl(String s) {
        super.setControl(s); 
        this.scale= getDoubleControl(s,this.scale);
        update();
    }
    
    /**
     * get the control string
     * @return the control string
     */
    @Override
    public String getControl() {
        Map<String,String> controls= new LinkedHashMap();
        if ( scale!=1.0 ) {
            controls.put( "scale", String.valueOf(scale) );
        }
        return Renderer.formatControl(controls);
    }
    
}
