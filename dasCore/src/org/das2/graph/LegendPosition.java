
package org.das2.graph;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.das2.components.propertyeditor.Enumeration;

/**
 * enumeration of legend positions
 * @author jbf
 */
public enum LegendPosition implements Enumeration {
    NW, 
    NE, // corner of plot
    SW,
    SE,
    OutsideNE,
    OutsideSE;

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
        } else if ( this==OutsideNE ) {
            g.fillRect(12,2,5,5);
        } else if ( this==OutsideSE ) {
            g.fillRect(12,8,5,5);
        }
        return new ImageIcon(im);
    }
    

}
