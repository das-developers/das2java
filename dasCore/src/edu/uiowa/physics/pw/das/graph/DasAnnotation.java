/*
 * DasAnnotation.java
 *
 * Created on December 20, 2004, 2:32 PM
 */

package edu.uiowa.physics.pw.das.graph;

import edu.uiowa.physics.pw.das.util.*;
import java.awt.*;

/**
 *
 * @author Jeremy
 */
public class DasAnnotation extends DasCanvasComponent {
        
    String string;
    GrannyTextRenderer gtr;
    Arrow arrow;

    /** Creates a new instance of DasAnnotation */
    public DasAnnotation( String string ) {
        this.gtr= new GrannyTextRenderer();        
        this.string= string;
    }

    public void setText( String string ) {
        gtr.setString(this, string);
        this.string= string;
        repaint();
    } 
    
    public String getText( ) {
        return this.string;
    }
        
    public void resize() {
        super.resize();        
        setBounds( DasDevicePosition.toRectangle( getRow(), getColumn() ) ) ;  
    }
    
    public void paintComponent( Graphics g1 ) {
        Graphics2D g= (Graphics2D)g1;
        g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        g.setColor( new Color(230,230,230,100) );
        g.fillRect(0,0,getColumn().getWidth(),getRow().getHeight());
        g.setColor( Color.black );
        gtr.draw(g,0,(float)gtr.getAscent());
    }

    protected void installComponent() {
        super.installComponent();
        this.gtr.setString( this, this.string );        
    }

    /**
     * Getter for property fontSize.
     * @return Value of property fontSize.
     */
    public float getFontSize() {    
        return gtr.getFontSize();
    }

    /**
     * Setter for property fontSize.
     * @param fontSize New value of property fontSize.
     */
    public void setFontSize(float fontSize) {
        gtr.setFontSize( fontSize );
        repaint();
    }
}
