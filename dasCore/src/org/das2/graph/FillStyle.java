
package org.das2.graph;

import org.das2.DasProperties;
import org.das2.components.propertyeditor.Displayable;
import org.das2.components.propertyeditor.Enumeration;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * Controls if the plot symbols are filled or not.
 * @author jbf
 */
public class FillStyle implements Displayable, Enumeration {
    
    String label;
    ImageIcon icon;
    
    FillStyle( String label ) {
        this.label= label;
        this.icon= null;
    }
    
    public String getListLabel() {
        return label;
    }
            
    public String toString() {
        return label;
    }
    
    
    public Icon getListIcon() {
        if ( icon==null ) {
            Image i= new BufferedImage(10,10,BufferedImage.TYPE_INT_RGB);
            Graphics2D g= ( Graphics2D) i.getGraphics();
            g.setBackground( Color.white );
            g.setRenderingHints(DasProperties.getRenderingHints());
            g.setColor( Color.white );
            g.fillRect(0,0, 10,10);
            g.setColor( Color.black );
            DefaultPlotSymbol.CIRCLES.draw(g,3,3,6.f,this);
            DefaultPlotSymbol.CIRCLES.draw(g,6,6,6.f,this);
            this.icon= new ImageIcon(i);
        }
        return icon;
    }

    public void drawListIcon( Graphics2D g, int x, int y ) {
        ImageIcon i= (ImageIcon) getListIcon();
        g.drawImage(i.getImage(), x, y, null);
    }

    /**
     * @deprecated use solid
     */
    public static final FillStyle STYLE_FILL= new FillStyle( "fill" );
    public static final FillStyle STYLE_SOLID= new FillStyle( "solid" );
    public static final FillStyle STYLE_NONE= new FillStyle( "none" );
    /**
     * @deprecated use none
     */
    public static final FillStyle STYLE_DRAW= new FillStyle( "draw" );
    public static final FillStyle STYLE_OUTLINE= new FillStyle( "outline" );
}
