
package org.das2.graph;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.das2.components.propertyeditor.Enumeration;

/**
 * enumeration of relative positions to a row, column box.
 * @author jbf
 */
public enum AnchorPosition implements Enumeration {
    NW, 
    NE, 
    SW,
    SE,
    Center,
    N,          // centered middle
    E,
    W,
    S,
    OutsideE,
    OutsideN,   // titles...
    OutsideNNW, // top-left label.
    OutsideNNE,  // right-side label.
    OutsideNE;  // right-side label.
    
    @Override
    public Icon getListIcon() {
        BufferedImage im= new BufferedImage(16,16,BufferedImage.TYPE_INT_RGB);
        Graphics2D g= im.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0,0,16,16);
        g.setColor(Color.GRAY);
        g.drawRect( 2,2,10,10 );
        if ( this==NW ) {
            g.fillRect(2,2,5,5);
        } else if ( this==NE ) {
            g.fillRect(7,2,5,5);
        } else if ( this==SW ) {
            g.fillRect(2,7,5,5);
        } else if ( this==SE ) {
            g.fillRect(7,7,5,5);            
        } else if ( this==Center ) {
            g.fillRect(5,5,5,5);
        } else if ( this==N ) {
            g.fillRect(5,2,5,5);
        } else if ( this==E ) {
            g.fillRect(7,5,5,5);
        } else if ( this==W ) {
            g.fillRect(2,5,5,5);
        } else if ( this==S ) {
            g.fillRect(5,7,5,5);
        } else if ( this==OutsideE ) {
            g.fillRect(12,5,5,5);
        } else if ( this==OutsideN ) {
            g.fillRect(5,-3,5,5);
        } else if ( this==OutsideNNW ) {
            g.fillRect(2,-3,5,5);
        } else if ( this==OutsideNNE ) {
            g.fillRect(7,-3,6,5);
        } else if ( this==OutsideNE ) {
            g.fillRect(12,2,5,5);            
        } else {
            g.drawString("?", 0, 14);
        }
        return new ImageIcon(im);
    }
    

}
