
package org.das2.util;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * single place to contain Color-Name mapping.  See https://sourceforge.net/p/autoplot/feature-requests/263/
 * @author jbf
 */
public class ColorUtil {
    
    private static final Map<Color,String> namedColors;
    private static final Map<String,Color> revNamedColors;
    
    private static final Logger logger= Logger.getLogger("das2.util");
            
    static { 
        // see /home/jbf/ct/autoplot/rfe/263/convertColorNames.jy 
        namedColors= new LinkedHashMap<>();
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
        namedColors.put(Color.decode("#FFB6C1"),"LightPink");
        namedColors.put(Color.decode("#FFC0CB"),"Pink");
        namedColors.put(Color.decode("#DC143C"),"Crimson");
        namedColors.put(Color.decode("#FFF0F5"),"LavenderBlush");
        namedColors.put(Color.decode("#DB7093"),"PaleVioletRed");
        namedColors.put(Color.decode("#FF69B4"),"HotPink");
        namedColors.put(Color.decode("#FF1493"),"DeepPink");
        namedColors.put(Color.decode("#C71585"),"MediumVioletRed");
        namedColors.put(Color.decode("#DA70D6"),"Orchid");
        namedColors.put(Color.decode("#D8BFD8"),"Thistle");
        namedColors.put(Color.decode("#DDA0DD"),"Plum");
        namedColors.put(Color.decode("#EE82EE"),"Violet");
        namedColors.put(Color.decode("#FF00FF"),"Magenta");
        namedColors.put(Color.decode("#FF00FF"),"Fuchsia");
        namedColors.put(Color.decode("#8B008B"),"DarkMagenta");
        namedColors.put(Color.decode("#800080"),"Purple");
        namedColors.put(Color.decode("#BA55D3"),"MediumOrchid");
        namedColors.put(Color.decode("#9400D3"),"DarkViolet");
        namedColors.put(Color.decode("#9932CC"),"DarkOrchid");
        namedColors.put(Color.decode("#4B0082"),"Indigo");
        namedColors.put(Color.decode("#8A2BE2"),"BlueViolet");
        namedColors.put(Color.decode("#9370DB"),"MediumPurple");
        namedColors.put(Color.decode("#7B68EE"),"MediumSlateBlue");
        namedColors.put(Color.decode("#6A5ACD"),"SlateBlue");
        namedColors.put(Color.decode("#483D8B"),"DarkSlateBlue");
        namedColors.put(Color.decode("#E6E6FA"),"Lavender");
        namedColors.put(Color.decode("#F8F8FF"),"GhostWhite");
        namedColors.put(Color.decode("#0000FF"),"Blue");
        namedColors.put(Color.decode("#0000CD"),"MediumBlue");
        namedColors.put(Color.decode("#191970"),"MidnightBlue");
        namedColors.put(Color.decode("#00008B"),"DarkBlue");
        namedColors.put(Color.decode("#000080"),"Navy");
        namedColors.put(Color.decode("#4169E1"),"RoyalBlue");
        namedColors.put(Color.decode("#6495ED"),"CornflowerBlue");
        namedColors.put(Color.decode("#B0C4DE"),"LightSteelBlue");
        namedColors.put(Color.decode("#778899"),"LightSlateGray");
        namedColors.put(Color.decode("#708090"),"SlateGray");
        namedColors.put(Color.decode("#1E90FF"),"DodgerBlue");
        namedColors.put(Color.decode("#F0F8FF"),"AliceBlue");
        namedColors.put(Color.decode("#4682B4"),"SteelBlue");
        namedColors.put(Color.decode("#87CEFA"),"LightSkyBlue");
        namedColors.put(Color.decode("#87CEEB"),"SkyBlue");
        namedColors.put(Color.decode("#00BFFF"),"DeepSkyBlue");
        namedColors.put(Color.decode("#ADD8E6"),"LightBlue");
        namedColors.put(Color.decode("#B0E0E6"),"PowderBlue");
        namedColors.put(Color.decode("#5F9EA0"),"CadetBlue");
        namedColors.put(Color.decode("#F0FFFF"),"Azure");
        namedColors.put(Color.decode("#E0FFFF"),"LightCyan");
        namedColors.put(Color.decode("#AFEEEE"),"PaleTurquoise");
        namedColors.put(Color.decode("#00FFFF"),"Cyan");
        namedColors.put(Color.decode("#00FFFF"),"Aqua");
        namedColors.put(Color.decode("#00CED1"),"DarkTurquoise");
        namedColors.put(Color.decode("#2F4F4F"),"DarkSlateGray");
        namedColors.put(Color.decode("#008B8B"),"DarkCyan");
        namedColors.put(Color.decode("#008080"),"Teal");
        namedColors.put(Color.decode("#48D1CC"),"MediumTurquoise");
        namedColors.put(Color.decode("#20B2AA"),"LightSeaGreen");
        namedColors.put(Color.decode("#40E0D0"),"Turquoise");
        namedColors.put(Color.decode("#7FFFD4"),"Aquamarine");
        namedColors.put(Color.decode("#66CDAA"),"MediumAquamarine");
        namedColors.put(Color.decode("#00FA9A"),"MediumSpringGreen");
        namedColors.put(Color.decode("#F5FFFA"),"MintCream");
        namedColors.put(Color.decode("#00FF7F"),"SpringGreen");
        namedColors.put(Color.decode("#3CB371"),"MediumSeaGreen");
        namedColors.put(Color.decode("#2E8B57"),"SeaGreen");
        namedColors.put(Color.decode("#F0FFF0"),"Honeydew");
        namedColors.put(Color.decode("#90EE90"),"LightGreen");
        namedColors.put(Color.decode("#98FB98"),"PaleGreen");
        namedColors.put(Color.decode("#8FBC8F"),"DarkSeaGreen");
        namedColors.put(Color.decode("#32CD32"),"LimeGreen");
        namedColors.put(Color.decode("#00FF00"),"Lime");
        namedColors.put(Color.decode("#228B22"),"ForestGreen");
        namedColors.put(Color.decode("#008000"),"Green");
        namedColors.put(Color.decode("#006400"),"DarkGreen");
        namedColors.put(Color.decode("#7FFF00"),"Chartreuse");
        namedColors.put(Color.decode("#7CFC00"),"LawnGreen");
        namedColors.put(Color.decode("#ADFF2F"),"GreenYellow");
        namedColors.put(Color.decode("#556B2F"),"DarkOliveGreen");
        namedColors.put(Color.decode("#9ACD32"),"YellowGreen");
        namedColors.put(Color.decode("#6B8E23"),"OliveDrab");
        namedColors.put(Color.decode("#F5F5DC"),"Beige");
        namedColors.put(Color.decode("#FAFAD2"),"LightGoldenrodYellow");
        namedColors.put(Color.decode("#FFFFF0"),"Ivory");
        namedColors.put(Color.decode("#FFFFE0"),"LightYellow");
        namedColors.put(Color.decode("#FFFF00"),"Yellow");
        namedColors.put(Color.decode("#808000"),"Olive");
        namedColors.put(Color.decode("#BDB76B"),"DarkKhaki");
        namedColors.put(Color.decode("#FFFACD"),"LemonChiffon");
        namedColors.put(Color.decode("#EEE8AA"),"PaleGoldenrod");
        namedColors.put(Color.decode("#F0E68C"),"Khaki");
        namedColors.put(Color.decode("#FFD700"),"Gold");
        namedColors.put(Color.decode("#FFF8DC"),"Cornsilk");
        namedColors.put(Color.decode("#DAA520"),"Goldenrod");
        namedColors.put(Color.decode("#B8860B"),"DarkGoldenrod");
        namedColors.put(Color.decode("#FFFAF0"),"FloralWhite");
        namedColors.put(Color.decode("#FDF5E6"),"OldLace");
        namedColors.put(Color.decode("#F5DEB3"),"Wheat");
        namedColors.put(Color.decode("#FFE4B5"),"Moccasin");
        namedColors.put(Color.decode("#FFA500"),"Orange");
        namedColors.put(Color.decode("#FFEFD5"),"PapayaWhip");
        namedColors.put(Color.decode("#FFEBCD"),"BlanchedAlmond");
        namedColors.put(Color.decode("#FFDEAD"),"NavajoWhite");
        namedColors.put(Color.decode("#FAEBD7"),"AntiqueWhite");
        namedColors.put(Color.decode("#D2B48C"),"Tan");
        namedColors.put(Color.decode("#DEB887"),"BurlyWood");
        namedColors.put(Color.decode("#FFE4C4"),"Bisque");
        namedColors.put(Color.decode("#FF8C00"),"DarkOrange");
        namedColors.put(Color.decode("#FAF0E6"),"Linen");
        namedColors.put(Color.decode("#CD853F"),"Peru");
        namedColors.put(Color.decode("#FFDAB9"),"PeachPuff");
        namedColors.put(Color.decode("#F4A460"),"SandyBrown");
        namedColors.put(Color.decode("#D2691E"),"Chocolate");
        namedColors.put(Color.decode("#8B4513"),"SaddleBrown");
        namedColors.put(Color.decode("#FFF5EE"),"Seashell");
        namedColors.put(Color.decode("#A0522D"),"Sienna");
        namedColors.put(Color.decode("#FFA07A"),"LightSalmon");
        namedColors.put(Color.decode("#FF7F50"),"Coral");
        namedColors.put(Color.decode("#FF4500"),"OrangeRed");
        namedColors.put(Color.decode("#E9967A"),"DarkSalmon");
        namedColors.put(Color.decode("#FF6347"),"Tomato");
        namedColors.put(Color.decode("#FFE4E1"),"MistyRose");
        namedColors.put(Color.decode("#FA8072"),"Salmon");
        namedColors.put(Color.decode("#FFFAFA"),"Snow");
        namedColors.put(Color.decode("#F08080"),"LightCoral");
        namedColors.put(Color.decode("#BC8F8F"),"RosyBrown");
        namedColors.put(Color.decode("#CD5C5C"),"IndianRed");
        namedColors.put(Color.decode("#FF0000"),"Red");
        namedColors.put(Color.decode("#A52A2A"),"Brown");
        namedColors.put(Color.decode("#B22222"),"FireBrick");
        namedColors.put(Color.decode("#8B0000"),"DarkRed");
        namedColors.put(Color.decode("#800000"),"Maroon");
        namedColors.put(Color.decode("#FFFFFF"),"White");
        namedColors.put(Color.decode("#F5F5F5"),"WhiteSmoke");
        namedColors.put(Color.decode("#DCDCDC"),"Gainsboro");
        namedColors.put(Color.decode("#D3D3D3"),"LightGrey");
        namedColors.put(Color.decode("#C0C0C0"),"Silver");
        namedColors.put(Color.decode("#A9A9A9"),"DarkGray");
        namedColors.put(Color.decode("#808080"),"Gray");
        namedColors.put(Color.decode("#696969"),"DimGray");
        namedColors.put(Color.decode("#000000"),"Black");      
        namedColors.put(Color.decode("#D55E00"),"mms2Red");
        namedColors.put(Color.decode("#009E73"),"mms3Green");
        namedColors.put(Color.decode("#56B4E9"),"mms4Blue");
        revNamedColors= new LinkedHashMap<>();
        revNamedColors.put("black",Color.BLACK  );
        revNamedColors.put("blue", Color.BLUE );                      // dark blue
        revNamedColors.put("red", Color.RED );
        revNamedColors.put("dark green",Color.GREEN.darker() );
        revNamedColors.put("dark grey",Color.DARK_GRAY);                 // grey scale
        revNamedColors.put("grey",Color.GRAY );
        revNamedColors.put("light grey", Color.LIGHT_GRAY );
        revNamedColors.put("white",Color.WHITE );
        revNamedColors.put("light blue", new Color(128, 128, 255) );      // light blue
        revNamedColors.put("pink",Color.PINK  );
        revNamedColors.put("green",Color.GREEN  );
        revNamedColors.put("cyan",Color.CYAN );
        revNamedColors.put("yellow",Color.YELLOW );
        revNamedColors.put("magenta",Color.MAGENTA );                   // others
        revNamedColors.put("orange",Color.ORANGE );
        revNamedColors.put("lightpink",Color.decode("#FFB6C1"));
        revNamedColors.put("pink",Color.decode("#FFC0CB"));
        revNamedColors.put("crimson",Color.decode("#DC143C"));
        revNamedColors.put("lavenderblush",Color.decode("#FFF0F5"));
        revNamedColors.put("palevioletred",Color.decode("#DB7093"));
        revNamedColors.put("hotpink",Color.decode("#FF69B4"));
        revNamedColors.put("deeppink",Color.decode("#FF1493"));
        revNamedColors.put("mediumvioletred",Color.decode("#C71585"));
        revNamedColors.put("orchid",Color.decode("#DA70D6"));
        revNamedColors.put("thistle",Color.decode("#D8BFD8"));
        revNamedColors.put("plum",Color.decode("#DDA0DD"));
        revNamedColors.put("violet",Color.decode("#EE82EE"));
        revNamedColors.put("magenta",Color.decode("#FF00FF"));
        revNamedColors.put("fuchsia",Color.decode("#FF00FF"));
        revNamedColors.put("darkmagenta",Color.decode("#8B008B"));
        revNamedColors.put("purple",Color.decode("#800080"));
        revNamedColors.put("mediumorchid",Color.decode("#BA55D3"));
        revNamedColors.put("darkviolet",Color.decode("#9400D3"));
        revNamedColors.put("darkorchid",Color.decode("#9932CC"));
        revNamedColors.put("indigo",Color.decode("#4B0082"));
        revNamedColors.put("blueviolet",Color.decode("#8A2BE2"));
        revNamedColors.put("mediumpurple",Color.decode("#9370DB"));
        revNamedColors.put("mediumslateblue",Color.decode("#7B68EE"));
        revNamedColors.put("slateblue",Color.decode("#6A5ACD"));
        revNamedColors.put("darkslateblue",Color.decode("#483D8B"));
        revNamedColors.put("lavender",Color.decode("#E6E6FA"));
        revNamedColors.put("ghostwhite",Color.decode("#F8F8FF"));
        revNamedColors.put("blue",Color.decode("#0000FF"));
        revNamedColors.put("mediumblue",Color.decode("#0000CD"));
        revNamedColors.put("midnightblue",Color.decode("#191970"));
        revNamedColors.put("darkblue",Color.decode("#00008B"));
        revNamedColors.put("navy",Color.decode("#000080"));
        revNamedColors.put("royalblue",Color.decode("#4169E1"));
        revNamedColors.put("cornflowerblue",Color.decode("#6495ED"));
        revNamedColors.put("lightsteelblue",Color.decode("#B0C4DE"));
        revNamedColors.put("lightslategray",Color.decode("#778899"));
        revNamedColors.put("slategray",Color.decode("#708090"));
        revNamedColors.put("dodgerblue",Color.decode("#1E90FF"));
        revNamedColors.put("aliceblue",Color.decode("#F0F8FF"));
        revNamedColors.put("steelblue",Color.decode("#4682B4"));
        revNamedColors.put("lightskyblue",Color.decode("#87CEFA"));
        revNamedColors.put("skyblue",Color.decode("#87CEEB"));
        revNamedColors.put("deepskyblue",Color.decode("#00BFFF"));
        revNamedColors.put("lightblue",Color.decode("#ADD8E6"));
        revNamedColors.put("powderblue",Color.decode("#B0E0E6"));
        revNamedColors.put("cadetblue",Color.decode("#5F9EA0"));
        revNamedColors.put("azure",Color.decode("#F0FFFF"));
        revNamedColors.put("lightcyan",Color.decode("#E0FFFF"));
        revNamedColors.put("paleturquoise",Color.decode("#AFEEEE"));
        revNamedColors.put("cyan",Color.decode("#00FFFF"));
        revNamedColors.put("aqua",Color.decode("#00FFFF"));
        revNamedColors.put("darkturquoise",Color.decode("#00CED1"));
        revNamedColors.put("darkslategray",Color.decode("#2F4F4F"));
        revNamedColors.put("darkcyan",Color.decode("#008B8B"));
        revNamedColors.put("teal",Color.decode("#008080"));
        revNamedColors.put("mediumturquoise",Color.decode("#48D1CC"));
        revNamedColors.put("lightseagreen",Color.decode("#20B2AA"));
        revNamedColors.put("turquoise",Color.decode("#40E0D0"));
        revNamedColors.put("aquamarine",Color.decode("#7FFFD4"));
        revNamedColors.put("mediumaquamarine",Color.decode("#66CDAA"));
        revNamedColors.put("mediumspringgreen",Color.decode("#00FA9A"));
        revNamedColors.put("mintcream",Color.decode("#F5FFFA"));
        revNamedColors.put("springgreen",Color.decode("#00FF7F"));
        revNamedColors.put("mediumseagreen",Color.decode("#3CB371"));
        revNamedColors.put("seagreen",Color.decode("#2E8B57"));
        revNamedColors.put("honeydew",Color.decode("#F0FFF0"));
        revNamedColors.put("lightgreen",Color.decode("#90EE90"));
        revNamedColors.put("palegreen",Color.decode("#98FB98"));
        revNamedColors.put("darkseagreen",Color.decode("#8FBC8F"));
        revNamedColors.put("limegreen",Color.decode("#32CD32"));
        revNamedColors.put("lime",Color.decode("#00FF00"));
        revNamedColors.put("forestgreen",Color.decode("#228B22"));
        revNamedColors.put("green",Color.decode("#008000"));
        revNamedColors.put("darkgreen",Color.decode("#006400"));
        revNamedColors.put("chartreuse",Color.decode("#7FFF00"));
        revNamedColors.put("lawngreen",Color.decode("#7CFC00"));
        revNamedColors.put("greenyellow",Color.decode("#ADFF2F"));
        revNamedColors.put("darkolivegreen",Color.decode("#556B2F"));
        revNamedColors.put("yellowgreen",Color.decode("#9ACD32"));
        revNamedColors.put("olivedrab",Color.decode("#6B8E23"));
        revNamedColors.put("beige",Color.decode("#F5F5DC"));
        revNamedColors.put("lightgoldenrodyellow",Color.decode("#FAFAD2"));
        revNamedColors.put("ivory",Color.decode("#FFFFF0"));
        revNamedColors.put("lightyellow",Color.decode("#FFFFE0"));
        revNamedColors.put("yellow",Color.decode("#FFFF00"));
        revNamedColors.put("olive",Color.decode("#808000"));
        revNamedColors.put("darkkhaki",Color.decode("#BDB76B"));
        revNamedColors.put("lemonchiffon",Color.decode("#FFFACD"));
        revNamedColors.put("palegoldenrod",Color.decode("#EEE8AA"));
        revNamedColors.put("khaki",Color.decode("#F0E68C"));
        revNamedColors.put("gold",Color.decode("#FFD700"));
        revNamedColors.put("cornsilk",Color.decode("#FFF8DC"));
        revNamedColors.put("goldenrod",Color.decode("#DAA520"));
        revNamedColors.put("darkgoldenrod",Color.decode("#B8860B"));
        revNamedColors.put("floralwhite",Color.decode("#FFFAF0"));
        revNamedColors.put("oldlace",Color.decode("#FDF5E6"));
        revNamedColors.put("wheat",Color.decode("#F5DEB3"));
        revNamedColors.put("moccasin",Color.decode("#FFE4B5"));
        revNamedColors.put("orange",Color.decode("#FFA500"));
        revNamedColors.put("papayawhip",Color.decode("#FFEFD5"));
        revNamedColors.put("blanchedalmond",Color.decode("#FFEBCD"));
        revNamedColors.put("navajowhite",Color.decode("#FFDEAD"));
        revNamedColors.put("antiquewhite",Color.decode("#FAEBD7"));
        revNamedColors.put("tan",Color.decode("#D2B48C"));
        revNamedColors.put("burlywood",Color.decode("#DEB887"));
        revNamedColors.put("bisque",Color.decode("#FFE4C4"));
        revNamedColors.put("darkorange",Color.decode("#FF8C00"));
        revNamedColors.put("linen",Color.decode("#FAF0E6"));
        revNamedColors.put("peru",Color.decode("#CD853F"));
        revNamedColors.put("peachpuff",Color.decode("#FFDAB9"));
        revNamedColors.put("sandybrown",Color.decode("#F4A460"));
        revNamedColors.put("chocolate",Color.decode("#D2691E"));
        revNamedColors.put("saddlebrown",Color.decode("#8B4513"));
        revNamedColors.put("seashell",Color.decode("#FFF5EE"));
        revNamedColors.put("sienna",Color.decode("#A0522D"));
        revNamedColors.put("lightsalmon",Color.decode("#FFA07A"));
        revNamedColors.put("coral",Color.decode("#FF7F50"));
        revNamedColors.put("orangered",Color.decode("#FF4500"));
        revNamedColors.put("darksalmon",Color.decode("#E9967A"));
        revNamedColors.put("tomato",Color.decode("#FF6347"));
        revNamedColors.put("mistyrose",Color.decode("#FFE4E1"));
        revNamedColors.put("salmon",Color.decode("#FA8072"));
        revNamedColors.put("snow",Color.decode("#FFFAFA"));
        revNamedColors.put("lightcoral",Color.decode("#F08080"));
        revNamedColors.put("rosybrown",Color.decode("#BC8F8F"));
        revNamedColors.put("indianred",Color.decode("#CD5C5C"));
        revNamedColors.put("red",Color.decode("#FF0000"));
        revNamedColors.put("brown",Color.decode("#A52A2A"));
        revNamedColors.put("firebrick",Color.decode("#B22222"));
        revNamedColors.put("darkred",Color.decode("#8B0000"));
        revNamedColors.put("maroon",Color.decode("#800000"));
        revNamedColors.put("white",Color.decode("#FFFFFF"));
        revNamedColors.put("whitesmoke",Color.decode("#F5F5F5"));
        revNamedColors.put("gainsboro",Color.decode("#DCDCDC"));
        revNamedColors.put("lightgrey",Color.decode("#D3D3D3"));
        revNamedColors.put("silver",Color.decode("#C0C0C0"));
        revNamedColors.put("darkgray",Color.decode("#A9A9A9"));
        revNamedColors.put("gray",Color.decode("#808080"));
        revNamedColors.put("dimgray",Color.decode("#696969"));
        revNamedColors.put("black",Color.decode("#000000"));
        revNamedColors.put("mms2red",Color.decode("#D55E00"));
        revNamedColors.put("mms3green",Color.decode("#009E73"));
        revNamedColors.put("mms4blue",Color.decode("#56B4E9"));
    }
    
    /**
     * return a map of the named colors.
     * @return a map of the named colors.
     */
    public static Map<String,Color> getNamedColors() {
        LinkedHashMap<String,Color> result= new LinkedHashMap<>();
        for ( Entry<Color,String> e: namedColors.entrySet() ) {
            result.put( e.getValue(), e.getKey() );
        }
        return result;
    }
    
    /**
     * return the preferred name for the color
     * @param color
     * @return the preferred name (or #RGB) for the color
     */
    public static String nameForColor( Color color ) {
        String n= namedColors.get(color);
        if ( n!=null ) {
            return n;
        } else {
            if ( color.getAlpha()==255 ) {
                return "#" + String.format( "%06X", color.getRGB() & 0xFFFFFF );   
            } else {
                return "#" + String.format( "%02X%06X", color.getAlpha(), color.getRGB() & 0xFFFFFF );       
            }
        }
    }
        
    /**
     * return either a named color or 
     * "#" + Integer.toHexString( color.getRGB() &amp; 0xFFFFFF)
     * @param color
     * @return named color or hex string like "#FF0000" for Red.
     */
    public static String encodeColor( Color color ) {
        String s= namedColors.get(color);
        if ( s!=null ) {
            return s;
        } else {
            if ( color.getAlpha()==255 ) {
                return "#" + String.format( "%06X", color.getRGB() & 0xFFFFFF );   
            } else {
                return "#" + String.format( "%02X%06X", color.getAlpha(), color.getRGB() & 0xFFFFFF );       
            }
        }
    }
    
    /**
     * decode the color, throwing a RuntimeException when the color 
     * is not parsable. Valid entries include:<ul>
     * <li>"red" 
     * <li>"RED" 
     * <li>"0xFF0000" 
     * <li>"0xff0000" 
     * <li>"#00000000" (transparent)
     * <li>"0x00ffffff" (transparent)
     * <li>"#ffeedd"
     * <li>"LightPink" (X11 color names)
     * </ul>
     * This also allows a color name to follow the RGB like so:<ul>
     * <li>"0xFFFF00 (Purple)"
     * </ul>
     * to improve legibility of .vap files.  <a href="https://wikipedia.org/wiki/X11_color_names#Color_name_chart">X11 color names</a>
     * can be found at wikipedia.
     * @param s the string representation 
     * @return the color
     * @see https://en.wikipedia.org/wiki/X11_color_names
     * @see http://cng.seas.rochester.edu/CNG/docs/x11color.html
     */
    public static Color decodeColor( String s ) {
        s= s.toLowerCase().trim();
        if ( s.endsWith(")") ) {
            int i= s.indexOf("(");
            if ( i>-1 ) {
                s= s.substring(0,i).trim();
            }
        }
        //s= s.replaceAll("\\s+",""); // there's a problem where we have "dark green" and "darkgreen"
        
        Color r= revNamedColors.get(s);
        
        Pattern p= Pattern.compile("(0x|#)([0-9a-fA-F][0-9a-fA-F])([0-9a-fA-F][0-9a-fA-F])([0-9a-fA-F][0-9a-fA-F])([0-9a-fA-F][0-9a-fA-F])");
        if ( r!=null ) {
            return r;
        } else {
            try {
                Matcher m= p.matcher(s);
                if ( m.matches() ) {
                    return new Color( 
                            Integer.parseInt(m.group(3),16), // R
                            Integer.parseInt(m.group(4),16), // G 
                            Integer.parseInt(m.group(5),16), // B
                            Integer.parseInt(m.group(2),16) ); // A
                } else {
                    Integer i= Integer.decode(s);
                    if ( ( i & 0xFF000000 ) != 0 ) { 
                        r= new Color( i, true);
                    } else {
                        r= new Color( i );
                    }            
                    return r;
                }
            } catch ( NumberFormatException ex ) {        
                logger.log(Level.INFO, "unable to find color for \"{0}\"", s);
                return Color.GRAY;
            }
        }
    }
    
    /**
     * return standard color for slightly masking background.
     * @return 
     */
    public static Color getRicePaperColor() {
        return new Color(255, 255, 255, 128);
    }
}
