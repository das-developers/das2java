/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.graph;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * single place to contain Color-Name mapping.
 * @author jbf
 */
public class ColorUtil {
    
    private static final Map<Color,String> namedColors;
    private static final Map<String,Color> revNamedColors;
    
    static { 
        namedColors= new HashMap<Color,String>();
        namedColors.put(Color.BLACK, "black" );
        namedColors.put(Color.BLUE, "blue" );                      // dark blue
        namedColors.put(Color.RED, "red" );
        namedColors.put(Color.GREEN.darker(), "dark green" );
        namedColors.put(Color.DARK_GRAY, "dark grey");                 // grey scale
        namedColors.put(Color.GRAY, "grey" );
        namedColors.put(Color.LIGHT_GRAY, "light grey" );
        namedColors.put(Color.WHITE, "white" );
        namedColors.put( new Color(128, 128, 255), "light blue" );      // light blue
        namedColors.put(Color.PINK, "pink" );
        namedColors.put(Color.GREEN, "green" );
        namedColors.put(Color.CYAN, "cyan" );
        namedColors.put(Color.YELLOW, "yellow");
        namedColors.put(Color.MAGENTA, "magenta" );                   // others
        namedColors.put(Color.ORANGE, "orange" );
        revNamedColors= new HashMap<String,Color>();
        revNamedColors.put("black",Color.BLACK  );
        revNamedColors.put("blue", Color.BLUE );                      // dark blue
        revNamedColors.put("red", Color.RED );
        revNamedColors.put("dark green",Color.GREEN.darker() );
        revNamedColors.put( "dark grey",Color.DARK_GRAY);                 // grey scale
        revNamedColors.put( "grey",Color.GRAY );
        revNamedColors.put( "light grey", Color.LIGHT_GRAY );
        revNamedColors.put("white",Color.WHITE );
        revNamedColors.put( "light blue", new Color(128, 128, 255) );      // light blue
        revNamedColors.put("pink",Color.PINK  );
        revNamedColors.put("green",Color.GREEN  );
        revNamedColors.put("cyan",Color.CYAN );
        revNamedColors.put("yellow",Color.YELLOW );
        revNamedColors.put("magenta",Color.MAGENTA );                   // others
        revNamedColors.put("orange",Color.ORANGE );
    }
    
    public static String encodeColor( Color color ) {
        String s= namedColors.get(color);
        if ( s!=null ) {
            return s;
        } else {
            return "#" + Integer.toHexString(color.getRGB() & 0xFFFFFF);
        }
    }
    
    public static Color decodeColor( String s ) {
        s= s.toLowerCase();
        Color r= revNamedColors.get(s);
        if ( r!=null ) {
            return r;
        } else {
            return Color.decode(s);
        }
    }
}
