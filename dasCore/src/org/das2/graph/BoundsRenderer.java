
package org.das2.graph;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.util.LinkedHashMap;
import java.util.Map;
import org.das2.qds.QDataSet;
import org.das2.qds.ops.Ops;
import org.das2.util.monitor.ProgressMonitor;

/**
 *
 * @author jbf
 */
public class BoundsRenderer extends Renderer {

    private void expectDs() {
        throw new IllegalArgumentException("Expect rank 2 bins dataset");
    }
    
    private Color color = Color.BLACK;

    public static final String PROP_COLOR = "color";

        @Override
    public String getControl() {
        Map<String,String> controls= new LinkedHashMap();
        controls.put( "color", encodeColorControl(color) );
        return Renderer.formatControl(controls);
    }
    

    @Override
    public void setControl(String s) {
        super.setControl(s);
        this.color= getColorControl( "color", color );
    }    
    
    public Color getColor() {
        return color;
    }

    public void setColor(Color string) {
        Color oldColor = this.color;
        this.color = string;
        propertyChangeSupport.firePropertyChange(PROP_COLOR, oldColor, string);
    }
    
    @Override
    public void render(Graphics g1, DasAxis xAxis, DasAxis yAxis ) {
        Graphics2D g= (Graphics2D)g1;
        QDataSet d= this.getDataSet();
        if ( d.rank()!=2 ) expectDs();
        QDataSet mins= Ops.slice1( d,0 );
        QDataSet maxs= Ops.slice1( d,1 );
        GeneralPath pmin= GraphUtil.getPath(xAxis,yAxis,Ops.append(mins,Ops.reverse(maxs)),false,false);
        g.setColor( this.getColor() );
        g.fill(pmin);
    }
    
    
}
