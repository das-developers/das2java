
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
    OutsideN,   // titles
    OutsideE,   // right-side
    OutsideW,   // left-side
    OutsideS,   // bottom
    OutsideNNW, // top-left label.
    OutsideNNE, // top-right label.
    OutsideSSW, // bottom-left label.
    OutsideSSE, // bottom-right label.
    OutsideNE,  // right-side, upper label.
    OutsideSE,  // right-side, lower label.
    OutsideNW,  // left-side, upper label.
    OutsideSW;  // left-side, lower label.
    
    @Override
    public Icon getListIcon() {
        BufferedImage im= new BufferedImage(16,16,BufferedImage.TYPE_INT_RGB);
        Graphics2D g= im.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0,0,16,16);
        g.setColor(Color.GRAY);
        g.drawRect( 2,2,10,10 );
        switch (this) {
            case NW:
                g.fillRect(2,2,5,5);
                break;
            case NE:
                g.fillRect(7,2,5,5);
                break;
            case SW:
                g.fillRect(2,7,5,5);
                break;
            case SE:
                g.fillRect(7,7,5,5);
                break;
            case Center:
                g.fillRect(5,5,5,5);
                break;
            case N:
                g.fillRect(5,2,5,5);
                break;
            case E:
                g.fillRect(7,5,5,5);
                break;
            case W:
                g.fillRect(2,5,5,5);
                break;
            case S:
                g.fillRect(5,7,5,5);
                break;
            case OutsideE:
                g.fillRect(12,5,5,5);
                break;
            case OutsideW:
                g.fillRect(0,5,3,5);
                break;
            case OutsideN:
                g.fillRect(5,-3,5,5);
                break;
            case OutsideNNW:
                g.fillRect(2,-3,5,5);
                break;
            case OutsideNNE:
                g.fillRect(7,-3,6,5);
                break;
            case OutsideSSW:
                g.fillRect(2,12,5,5);
                break;
            case OutsideSSE:
                g.fillRect(7,8,6,5);
                break;
            case OutsideNE:
                g.fillRect(12,2,5,5);
                break;
            case OutsideSE:
                g.fillRect(12,8,5,5);
                break;
            case OutsideNW:
                g.fillRect(0,2,3,5);
                break;
            case OutsideSW:
                g.fillRect(0,8,3,5);
                break;
            case OutsideS:
                g.fillRect(12,7,5,5);
                break;
            default:
                g.drawString("?", 0, 14);
                break;
        }
        return new ImageIcon(im);
    }
    

}
