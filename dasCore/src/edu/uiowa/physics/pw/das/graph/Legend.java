/*
 * Legend.java
 *
 * Created on September 30, 2004, 5:01 PM
 */

package edu.uiowa.physics.pw.das.graph;

import edu.uiowa.physics.pw.das.components.propertyeditor.PropertyEditor;
import edu.uiowa.physics.pw.das.system.DasLogger;
import edu.uiowa.physics.pw.das.util.ObjectLocator;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.*;
import java.util.*;
import java.util.logging.Logger;
import javax.swing.*;

/**
 *
 * @author  Jeremy
 */
public class Legend extends DasCanvasComponent {
    
    final static Logger logger= DasLogger.getLogger( DasLogger.GRAPHICS_LOG );
    
    class LegendElement {
        Icon icon;
        Psym psym;
        PsymConnector psymConnector;
        SymbolLineRenderer rend;
        Color color;
        String label;
        Legend parent;
        
        Icon getIcon() {
            if ( rend!=null ) return rend.getListIcon(); else return icon;
        }
        
        void update() {
            if ( rend!=null ) this.icon= rend.getListIcon();
        }
        
        /**
         * return the Renderer or null if no renderer is associated with the 
         * legendElement.
         */
        Renderer getRenderer() {
            return rend;
        }

        private String getLabel() {
            return label;
        }
        
        private boolean isVisible() {
            return ( rend==null || rend.isActive() );
        }
        
        LegendElement( SymbolLineRenderer rend, String label ) {
            this.icon= rend.getListIcon();
            this.label= label;
            this.rend= rend;
        }
        
        LegendElement( Icon icon, String label ) {
            this.icon= icon;
            this.label= label;
        }
        
        
    }
    
    ArrayList elements; // LegendElement
    ObjectLocator locator;
    
    public Legend(  ) {
        elements= new ArrayList();
        getMouseAdapter().addMenuItem( new JMenuItem( getEditAction() ) );        
    }
    
    private Action getEditAction() {
        return new AbstractAction("Renderer Properties") {
            public void actionPerformed(ActionEvent e) {
                Point p= getMouseAdapter().getMousePressPosition();
                LegendElement item= (LegendElement)locator.closestObject(p);
                if ( item==null ) return;
                Renderer rend= item.getRenderer();
                PropertyEditor editor= new PropertyEditor( rend );
                editor.showDialog(Legend.this);
            }
        };
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
    
    public void remove( SymbolLineRenderer rend ) {
        for ( int i=0; i<elements.size(); i++ ) {
            LegendElement ele= (LegendElement) elements.get(i);
            if ( ele.rend != null && rend==ele.rend ) {
                elements.remove( i );
                repaint();
                break;
            }
        }
    }
    
    public void add( Icon icon, String label ) {
        elements.add( new LegendElement( icon, label ) );
    }
    
    public void resize() {
        setBounds( DasRow.toRectangle(getRow(), getColumn()) );
    }
    
    public void paintComponent( Graphics g1 ) {
        if ( elements.size()==0 ) {
            logger.info("no elements in legend, returning.");
            return;
        }
        Graphics2D g= (Graphics2D) g1;
        g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        int border= 5;
        int x= 5; //pixels
        int y= 5;
        FontMetrics fm= g.getFontMetrics();
                
        Color color0= g.getColor();
        
        locator= new ObjectLocator();
        
        boolean allVisible= true;
        int maxWidth=0;
        for ( int i=0; i<elements.size(); i++ ) {
            LegendElement e= (LegendElement)elements.get(i);
            Icon icon= e.getIcon();
            String invisibleString="";
            if ( !e.isVisible() ) {
                invisibleString="\u00B9";
                allVisible= false;
            }
            int itemWidth= fm.stringWidth(e.getLabel()+invisibleString);
            int itemHeight= ( fm.getHeight() > icon.getIconHeight() ? fm.getHeight() : icon.getIconHeight() + border );
            int w1= itemWidth + x + 20;
            if ( w1 > maxWidth ) maxWidth= w1;
            y+= itemHeight;
        }
        
        if ( !allVisible ) {
            Font font0= g.getFont();            
            g.setFont( font0.deriveFont(font0.getSize()*0.66f) );
            y+= g.getFontMetrics().getHeight()/2;
            g.setFont(font0);
            y+= g.getFontMetrics().getHeight()/2;
        }
        
        g.setColor( new Color( 255, 255, 255, 240 ) );
        
        g.fill( new Rectangle( 0, 0, maxWidth+10, y-1 ) );
        g.setColor( Color.DARK_GRAY );
        
        g.draw( new Rectangle( 0,0, maxWidth+10, y-1 ) );
                
        x= 5;
        y= 5;
                
        for ( int i=0; i<elements.size(); i++ ) {
            LegendElement e= (LegendElement)elements.get(i);
            Icon icon= e.getIcon();
            icon.paintIcon( this, g, x, y );
            String invisibleString="";
            if ( !e.isVisible() ) {
                invisibleString="\u00B9";
                allVisible= false;
            }
            g.drawString( e.getLabel()+invisibleString, x+20, y+icon.getIconHeight() );
            int itemHeight= ( fm.getHeight() > icon.getIconHeight() ? fm.getHeight() : icon.getIconHeight() + border );
            locator.addObject( new Rectangle( x, y-border/2, maxWidth, itemHeight ), e );          
            y+= ( fm.getHeight() > icon.getIconHeight() ? fm.getHeight() : icon.getIconHeight() + border );
        }

        if ( !allVisible ) {
            Font font0= g.getFont();            
            g.setFont( font0.deriveFont(font0.getSize()*0.66f) );
            y+= g.getFontMetrics().getHeight()/2;
            g.setColor( Color.DARK_GRAY );
            g.drawString( "\u00B9 not drawn", x+10, y );
            g.setFont(font0);
            y+= g.getFontMetrics().getHeight()/2;
        }
                
        g.setColor( color0 );
        int width= maxWidth+10+1;
        int height= y;
        
        /* experiment with resetting the row and column bounds.
         if ( width != getColumn().getWidth() ||  height!=getRow().getHeight() ) {
            Rectangle oldBounds= getBounds();
            int ymin= getRow().getDMinimum();
            int xmin= getColumn().getDMinimum();
            System.out.println( ""+(maxWidth+10)+"  "+ y );
            getRow().setDPosition( ymin, ymin + height );
            getColumn().setDPosition( xmin, xmin+width );
            
            oldBounds.width= oldBounds.width+1;
            oldBounds.height= oldBounds.height+1;
            
            getCanvas().repaint( oldBounds );
        }
        */
    }
    
}
