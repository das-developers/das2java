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
        private Icon createIconFor( Psym psym, PsymConnector connector, Color color ) {
            Image i= new BufferedImage(15,10,BufferedImage.TYPE_INT_ARGB);
            Graphics2D g= (Graphics2D)i.getGraphics();
            g.setRenderingHints(DasProperties.getRenderingHints());
            g.setColor(new Color( 0,0,0, 0 ));
            g.fillRect(0,0,15,10);
            g.setColor(color);
            Stroke stroke0= g.getStroke();
            connector.drawLine( g, 2, 3, 13, 7, 1.5f );
            g.setStroke(stroke0);
            psym.draw( g, 7, 5, 3.f );        
            return  new ImageIcon(i);
        }
        
        LegendElement( Psym psym, PsymConnector psymConnector, Color color, String label ) {
            this.psym= psym;
            this.psymConnector= psymConnector;
            this.color= color;
            icon= createIconFor( psym, psymConnector, color );
            this.label= label;
        }
        
        LegendElement( SymbolLineRenderer rend, String label ) {
            this( rend.getPsym(), rend.getPsymConnector(), rend.getColor(), label );
        }
        
    }
    
    ArrayList elements; // LegendElement
    
    public Legend(  ) {
        elements= new ArrayList();
    }
    
    public void add( SymbolLineRenderer rend, String label ) {
        LegendElement e= new LegendElement( rend, label );
        elements.add(e);
    }
    
    public void add( Psym psym, PsymConnector psymConnector, String label, Color color ) {
        LegendElement e= new LegendElement( psym, psymConnector, color, label );
        elements.add(e);
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
