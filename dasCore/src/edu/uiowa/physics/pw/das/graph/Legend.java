/*
 * Legend.java
 *
 * Created on September 30, 2004, 5:01 PM
 */

package edu.uiowa.physics.pw.das.graph;

import edu.uiowa.physics.pw.das.*;
import java.awt.*;
import java.awt.image.*;
import java.util.*;
import javax.swing.*;

/**
 *
 * @author  Jeremy
 */
public class Legend extends DasCanvasComponent {
    
    class LegendElement {
        Icon icon;
        Psym psym;
        PsymConnector psymConnector;
        Color color;
        String label;
        Icon getIcon() {
            return icon;
        }
        
        LegendElement( SymbolLineRenderer rend, String label ) {
            this.icon= rend.getListIcon();
            this.label= label;
        }
        
        LegendElement( Icon icon, String label ) {
            this.icon= icon;
            this.label= label;
        }
        
    }
    
    ArrayList elements; // LegendElement
    
    public Legend(  ) {
        elements= new ArrayList();
    }
    
    public static Icon getIcon( Color color ) {        
        Image image= new BufferedImage(6,10,BufferedImage.TYPE_INT_RGB);
        Graphics g2= image.getGraphics();
        g2.setColor(color);
        g2.fillRect(0,0,6,10);        
        return new ImageIcon( image );
    }
    
    public void add( SymbolLineRenderer rend, String label ) {
        LegendElement e= new LegendElement( rend, label );
        elements.add(e);
    }
    
    public void add( Icon icon, String label ) {
        elements.add( new LegendElement( icon, label ) );
    }
    
    public void resize() {
        setBounds( DasRow.toRectangle(getRow(), getColumn()) );
    }
    
    public void paintComponent( Graphics g1 ) {
        Graphics2D g= (Graphics2D) g1;
        g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        int border= 5;
        int x= 5; //pixels
        int y= 5;
        FontMetrics fm= g.getFontMetrics();
        
        Color color0= g.getColor();
        
        int maxWidth=0;
        for ( int i=0; i<elements.size(); i++ ) {
            LegendElement e= (LegendElement)elements.get(i);
            Icon icon= e.getIcon();
            icon.paintIcon( this, g, x, y );
            g.drawString( e.label, x+20, y+icon.getIconHeight() );
            int w1=  fm.stringWidth(e.label) + x + 20;
            if ( w1 > maxWidth ) maxWidth= w1;
            
            y+= ( fm.getHeight() > icon.getIconHeight() ? fm.getHeight() : icon.getIconHeight() + border );
            
        }
        
        g.setColor( Color.LIGHT_GRAY );
        g.draw( new Rectangle( 0,0, maxWidth+10, y-1 ) );
        g.setColor( color0 );
    }
    
}
