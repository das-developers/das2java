
package org.das2.graph;

import java.awt.Color;
import java.util.Map;

/**
 * single place to contain Color-Name mapping.  See https://sourceforge.net/p/autoplot/feature-requests/263/
 * @author jbf
 * @deprecated use org.das2.util#ColorUtil
 */
public class ColorUtil {
    
    /**
     * return a map of the named colors.
     * @return a map of the named colors.
     */
    public static Map<String,Color> getNamedColors() {
        return org.das2.util.ColorUtil.getNamedColors();
    }
    
    /**
     * return the preferred name for the color
     * @param color
     * @return the preferred name (or #RGB) for the color
     */
    public static String nameForColor( Color color ) {
        return org.das2.util.ColorUtil.nameForColor(color);
    }
        
    /**
     * return either a named color or 
     * "#" + Integer.toHexString( color.getRGB() &amp; 0xFFFFFF)
     * @param color
     * @return named color or hex string like "#FF0000" for Red.
     */
    public static String encodeColor( Color color ) {
        return org.das2.util.ColorUtil.encodeColor(color);
    }
    
    /**
     * decode the color, throwing a RuntimeException when the color 
     * is not parsable. Valid entries include:<ul>
     * <li>"red" 
     * <li>"RED" 
     * <li>"0xFF0000" 
     * <li>"0xff0000" 
     * <li>"#ffeedd"
     * </ul>
     * This also allows a color name to follow the RGB like so:<ul>
     * <li>"0xFFFF00 (Purple)"
     * </ul>
     * to improve legibility of .vap files.
     * @param s
     * @return 
     */
    public static Color decodeColor( String s ) throws NullPointerException {
        return org.das2.util.ColorUtil.decodeColor(s);
    }
    
    /**
     * return standard color for slightly masking background.
     * @return 
     */
    public static Color getRicePaperColor() {
        return org.das2.util.ColorUtil.getRicePaperColor();
    }
}
