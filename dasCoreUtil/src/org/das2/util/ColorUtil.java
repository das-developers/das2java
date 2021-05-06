
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
 * single place to contain Color-Name mapping.  These include
 * an old set of 10 or so color names, plus the 130 or so web color
 * names like "SaddleBrown" and "DarkOrchid".
 * @author jbf
 * @see https://sourceforge.net/p/autoplot/feature-requests/263/
 */
public class ColorUtil {
    
    private static final Map<Color,String> namedColors;
    private static final Map<String,Color> revNamedColors;
    
    private static final Logger logger= Logger.getLogger("das2.util");
    
    public static final Color LIGHT_PINK=Color.decode("#FFB6C1");
    public static final Color PINK=Color.decode("#FFC0CB");
    public static final Color CRIMSON=Color.decode("#DC143C");
    public static final Color LAVENDER_BLUSH=Color.decode("#FFF0F5");
    public static final Color PALE_VIOLET_RED=Color.decode("#DB7093");
    public static final Color HOT_PINK=Color.decode("#FF69B4");
    public static final Color DEEP_PINK=Color.decode("#FF1493");
    public static final Color MEDIUM_VIOLET_RED=Color.decode("#C71585");
    public static final Color ORCHID=Color.decode("#DA70D6");
    public static final Color THISTLE=Color.decode("#D8BFD8");
    public static final Color PLUM=Color.decode("#DDA0DD");
    public static final Color VIOLET=Color.decode("#EE82EE");
    public static final Color MAGENTA=Color.decode("#FF00FF");
    public static final Color FUCHSIA=Color.decode("#FF00FF");
    public static final Color DARK_MAGENTA=Color.decode("#8B008B");
    public static final Color PURPLE=Color.decode("#800080");
    public static final Color MEDIUM_ORCHID=Color.decode("#BA55D3");
    public static final Color DARK_VIOLET=Color.decode("#9400D3");
    public static final Color DARK_ORCHID=Color.decode("#9932CC");
    public static final Color INDIGO=Color.decode("#4B0082");
    public static final Color BLUE_VIOLET=Color.decode("#8A2BE2");
    public static final Color MEDIUM_PURPLE=Color.decode("#9370DB");
    public static final Color MEDIUM_SLATE_BLUE=Color.decode("#7B68EE");
    public static final Color SLATE_BLUE=Color.decode("#6A5ACD");
    public static final Color DARK_SLATE_BLUE=Color.decode("#483D8B");
    public static final Color LAVENDER=Color.decode("#E6E6FA");
    public static final Color GHOST_WHITE=Color.decode("#F8F8FF");
    public static final Color BLUE=Color.decode("#0000FF");
    public static final Color MEDIUM_BLUE=Color.decode("#0000CD");
    public static final Color MIDNIGHT_BLUE=Color.decode("#191970");
    public static final Color DARK_BLUE=Color.decode("#00008B");
    public static final Color NAVY=Color.decode("#000080");
    public static final Color ROYAL_BLUE=Color.decode("#4169E1");
    public static final Color CORNFLOWER_BLUE=Color.decode("#6495ED");
    public static final Color LIGHT_STEEL_BLUE=Color.decode("#B0C4DE");
    public static final Color LIGHT_SLATE_GRAY=Color.decode("#778899");
    public static final Color SLATE_GRAY=Color.decode("#708090");
    public static final Color DODGER_BLUE=Color.decode("#1E90FF");
    public static final Color ALICE_BLUE=Color.decode("#F0F8FF");
    public static final Color STEEL_BLUE=Color.decode("#4682B4");
    public static final Color LIGHT_SKY_BLUE=Color.decode("#87CEFA");
    public static final Color SKY_BLUE=Color.decode("#87CEEB");
    public static final Color DEEP_SKY_BLUE=Color.decode("#00BFFF");
    public static final Color LIGHT_BLUE=Color.decode("#ADD8E6");
    public static final Color POWDER_BLUE=Color.decode("#B0E0E6");
    public static final Color CADET_BLUE=Color.decode("#5F9EA0");
    public static final Color AZURE=Color.decode("#F0FFFF");
    public static final Color LIGHT_CYAN=Color.decode("#E0FFFF");
    public static final Color PALE_TURQUOISE=Color.decode("#AFEEEE");
    public static final Color CYAN=Color.decode("#00FFFF");
    public static final Color AQUA=Color.decode("#00FFFF");
    public static final Color DARK_TURQUOISE=Color.decode("#00CED1");
    public static final Color DARK_SLATE_GRAY=Color.decode("#2F4F4F");
    public static final Color DARK_CYAN=Color.decode("#008B8B");
    public static final Color TEAL=Color.decode("#008080");
    public static final Color MEDIUM_TURQUOISE=Color.decode("#48D1CC");
    public static final Color LIGHT_SEA_GREEN=Color.decode("#20B2AA");
    public static final Color TURQUOISE=Color.decode("#40E0D0");
    public static final Color AQUAMARINE=Color.decode("#7FFFD4");
    public static final Color MEDIUM_AQUAMARINE=Color.decode("#66CDAA");
    public static final Color MEDIUM_SPRING_GREEN=Color.decode("#00FA9A");
    public static final Color MINT_CREAM=Color.decode("#F5FFFA");
    public static final Color SPRING_GREEN=Color.decode("#00FF7F");
    public static final Color MEDIUM_SEA_GREEN=Color.decode("#3CB371");
    public static final Color SEA_GREEN=Color.decode("#2E8B57");
    public static final Color HONEYDEW=Color.decode("#F0FFF0");
    public static final Color LIGHT_GREEN=Color.decode("#90EE90");
    public static final Color PALE_GREEN=Color.decode("#98FB98");
    public static final Color DARK_SEA_GREEN=Color.decode("#8FBC8F");
    public static final Color LIME_GREEN=Color.decode("#32CD32");
    public static final Color LIME=Color.decode("#00FF00");
    public static final Color FOREST_GREEN=Color.decode("#228B22");
    public static final Color GREEN=Color.decode("#008000");
    public static final Color DARK_GREEN=Color.decode("#006400");
    public static final Color CHARTREUSE=Color.decode("#7FFF00");
    public static final Color LAWN_GREEN=Color.decode("#7CFC00");
    public static final Color GREEN_YELLOW=Color.decode("#ADFF2F");
    public static final Color DARK_OLIVE_GREEN=Color.decode("#556B2F");
    public static final Color YELLOW_GREEN=Color.decode("#9ACD32");
    public static final Color OLIVE_DRAB=Color.decode("#6B8E23");
    public static final Color BEIGE=Color.decode("#F5F5DC");
    public static final Color LIGHT_GOLDENROD_YELLOW=Color.decode("#FAFAD2");
    public static final Color IVORY=Color.decode("#FFFFF0");
    public static final Color LIGHT_YELLOW=Color.decode("#FFFFE0");
    public static final Color YELLOW=Color.decode("#FFFF00");
    public static final Color OLIVE=Color.decode("#808000");
    public static final Color DARK_KHAKI=Color.decode("#BDB76B");
    public static final Color LEMON_CHIFFON=Color.decode("#FFFACD");
    public static final Color PALE_GOLDENROD=Color.decode("#EEE8AA");
    public static final Color KHAKI=Color.decode("#F0E68C");
    public static final Color GOLD=Color.decode("#FFD700");
    public static final Color CORNSILK=Color.decode("#FFF8DC");
    public static final Color GOLDENROD=Color.decode("#DAA520");
    public static final Color DARK_GOLDENROD=Color.decode("#B8860B");
    public static final Color FLORAL_WHITE=Color.decode("#FFFAF0");
    public static final Color OLD_LACE=Color.decode("#FDF5E6");
    public static final Color WHEAT=Color.decode("#F5DEB3");
    public static final Color MOCCASIN=Color.decode("#FFE4B5");
    public static final Color ORANGE=Color.decode("#FFA500");
    public static final Color PAPAYA_WHIP=Color.decode("#FFEFD5");
    public static final Color BLANCHED_ALMOND=Color.decode("#FFEBCD");
    public static final Color NAVAJO_WHITE=Color.decode("#FFDEAD");
    public static final Color ANTIQUE_WHITE=Color.decode("#FAEBD7");
    public static final Color TAN=Color.decode("#D2B48C");
    public static final Color BURLY_WOOD=Color.decode("#DEB887");
    public static final Color BISQUE=Color.decode("#FFE4C4");
    public static final Color DARK_ORANGE=Color.decode("#FF8C00");
    public static final Color LINEN=Color.decode("#FAF0E6");
    public static final Color PERU=Color.decode("#CD853F");
    public static final Color PEACH_PUFF=Color.decode("#FFDAB9");
    public static final Color SANDY_BROWN=Color.decode("#F4A460");
    public static final Color CHOCOLATE=Color.decode("#D2691E");
    public static final Color SADDLE_BROWN=Color.decode("#8B4513");
    public static final Color SEASHELL=Color.decode("#FFF5EE");
    public static final Color SIENNA=Color.decode("#A0522D");
    public static final Color LIGHT_SALMON=Color.decode("#FFA07A");
    public static final Color CORAL=Color.decode("#FF7F50");
    public static final Color ORANGE_RED=Color.decode("#FF4500");
    public static final Color DARK_SALMON=Color.decode("#E9967A");
    public static final Color TOMATO=Color.decode("#FF6347");
    public static final Color MISTY_ROSE=Color.decode("#FFE4E1");
    public static final Color SALMON=Color.decode("#FA8072");
    public static final Color SNOW=Color.decode("#FFFAFA");
    public static final Color LIGHT_CORAL=Color.decode("#F08080");
    public static final Color ROSY_BROWN=Color.decode("#BC8F8F");
    public static final Color INDIAN_RED=Color.decode("#CD5C5C");
    public static final Color RED=Color.decode("#FF0000");
    public static final Color BROWN=Color.decode("#A52A2A");
    public static final Color FIRE_BRICK=Color.decode("#B22222");
    public static final Color DARK_RED=Color.decode("#8B0000");
    public static final Color MAROON=Color.decode("#800000");
    public static final Color WHITE=Color.decode("#FFFFFF");
    public static final Color WHITE_SMOKE=Color.decode("#F5F5F5");
    public static final Color GAINSBORO=Color.decode("#DCDCDC");
    public static final Color LIGHT_GREY=Color.decode("#D3D3D3");
    public static final Color SILVER=Color.decode("#C0C0C0");
    public static final Color DARK_GRAY=Color.decode("#A9A9A9");
    public static final Color GRAY=Color.decode("#808080");
    public static final Color DIM_GRAY=Color.decode("#696969");
    public static final Color BLACK=Color.decode("#000000");

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
        namedColors.put(LIGHT_PINK,"LightPink");
        namedColors.put(PINK,"Pink");
        namedColors.put(CRIMSON,"Crimson");
        namedColors.put(LAVENDER_BLUSH,"LavenderBlush");
        namedColors.put(PALE_VIOLET_RED,"PaleVioletRed");
        namedColors.put(HOT_PINK,"HotPink");
        namedColors.put(DEEP_PINK,"DeepPink");
        namedColors.put(MEDIUM_VIOLET_RED,"MediumVioletRed");
        namedColors.put(ORCHID,"Orchid");
        namedColors.put(THISTLE,"Thistle");
        namedColors.put(PLUM,"Plum");
        namedColors.put(VIOLET,"Violet");
        namedColors.put(MAGENTA,"Magenta");
        namedColors.put(FUCHSIA,"Fuchsia");
        namedColors.put(DARK_MAGENTA,"DarkMagenta");
        namedColors.put(PURPLE,"Purple");
        namedColors.put(MEDIUM_ORCHID,"MediumOrchid");
        namedColors.put(DARK_VIOLET,"DarkViolet");
        namedColors.put(DARK_ORCHID,"DarkOrchid");
        namedColors.put(INDIGO,"Indigo");
        namedColors.put(BLUE_VIOLET,"BlueViolet");
        namedColors.put(MEDIUM_PURPLE,"MediumPurple");
        namedColors.put(MEDIUM_SLATE_BLUE,"MediumSlateBlue");
        namedColors.put(SLATE_BLUE,"SlateBlue");
        namedColors.put(DARK_SLATE_BLUE,"DarkSlateBlue");
        namedColors.put(LAVENDER,"Lavender");
        namedColors.put(GHOST_WHITE,"GhostWhite");
        namedColors.put(BLUE,"Blue");
        namedColors.put(MEDIUM_BLUE,"MediumBlue");
        namedColors.put(MIDNIGHT_BLUE,"MidnightBlue");
        namedColors.put(DARK_BLUE,"DarkBlue");
        namedColors.put(NAVY,"Navy");
        namedColors.put(ROYAL_BLUE,"RoyalBlue");
        namedColors.put(CORNFLOWER_BLUE,"CornflowerBlue");
        namedColors.put(LIGHT_STEEL_BLUE,"LightSteelBlue");
        namedColors.put(LIGHT_SLATE_GRAY,"LightSlateGray");
        namedColors.put(SLATE_GRAY,"SlateGray");
        namedColors.put(DODGER_BLUE,"DodgerBlue");
        namedColors.put(ALICE_BLUE,"AliceBlue");
        namedColors.put(STEEL_BLUE,"SteelBlue");
        namedColors.put(LIGHT_SKY_BLUE,"LightSkyBlue");
        namedColors.put(SKY_BLUE,"SkyBlue");
        namedColors.put(DEEP_SKY_BLUE,"DeepSkyBlue");
        namedColors.put(LIGHT_BLUE,"LightBlue");
        namedColors.put(POWDER_BLUE,"PowderBlue");
        namedColors.put(CADET_BLUE,"CadetBlue");
        namedColors.put(AZURE,"Azure");
        namedColors.put(LIGHT_CYAN,"LightCyan");
        namedColors.put(PALE_TURQUOISE,"PaleTurquoise");
        namedColors.put(CYAN,"Cyan");
        namedColors.put(AQUA,"Aqua");
        namedColors.put(DARK_TURQUOISE,"DarkTurquoise");
        namedColors.put(DARK_SLATE_GRAY,"DarkSlateGray");
        namedColors.put(DARK_CYAN,"DarkCyan");
        namedColors.put(TEAL,"Teal");
        namedColors.put(MEDIUM_TURQUOISE,"MediumTurquoise");
        namedColors.put(LIGHT_SEA_GREEN,"LightSeaGreen");
        namedColors.put(TURQUOISE,"Turquoise");
        namedColors.put(AQUAMARINE,"Aquamarine");
        namedColors.put(MEDIUM_AQUAMARINE,"MediumAquamarine");
        namedColors.put(MEDIUM_SPRING_GREEN,"MediumSpringGreen");
        namedColors.put(MINT_CREAM,"MintCream");
        namedColors.put(SPRING_GREEN,"SpringGreen");
        namedColors.put(MEDIUM_SEA_GREEN,"MediumSeaGreen");
        namedColors.put(SEA_GREEN,"SeaGreen");
        namedColors.put(HONEYDEW,"Honeydew");
        namedColors.put(LIGHT_GREEN,"LightGreen");
        namedColors.put(PALE_GREEN,"PaleGreen");
        namedColors.put(DARK_SEA_GREEN,"DarkSeaGreen");
        namedColors.put(LIME_GREEN,"LimeGreen");
        namedColors.put(LIME,"Lime");
        namedColors.put(FOREST_GREEN,"ForestGreen");
        namedColors.put(GREEN,"Green");
        namedColors.put(DARK_GREEN,"DarkGreen");
        namedColors.put(CHARTREUSE,"Chartreuse");
        namedColors.put(LAWN_GREEN,"LawnGreen");
        namedColors.put(GREEN_YELLOW,"GreenYellow");
        namedColors.put(DARK_OLIVE_GREEN,"DarkOliveGreen");
        namedColors.put(YELLOW_GREEN,"YellowGreen");
        namedColors.put(OLIVE_DRAB,"OliveDrab");
        namedColors.put(BEIGE,"Beige");
        namedColors.put(LIGHT_GOLDENROD_YELLOW,"LightGoldenrodYellow");
        namedColors.put(IVORY,"Ivory");
        namedColors.put(LIGHT_YELLOW,"LightYellow");
        namedColors.put(YELLOW,"Yellow");
        namedColors.put(OLIVE,"Olive");
        namedColors.put(DARK_KHAKI,"DarkKhaki");
        namedColors.put(LEMON_CHIFFON,"LemonChiffon");
        namedColors.put(PALE_GOLDENROD,"PaleGoldenrod");
        namedColors.put(KHAKI,"Khaki");
        namedColors.put(GOLD,"Gold");
        namedColors.put(CORNSILK,"Cornsilk");
        namedColors.put(GOLDENROD,"Goldenrod");
        namedColors.put(DARK_GOLDENROD,"DarkGoldenrod");
        namedColors.put(FLORAL_WHITE,"FloralWhite");
        namedColors.put(OLD_LACE,"OldLace");
        namedColors.put(WHEAT,"Wheat");
        namedColors.put(MOCCASIN,"Moccasin");
        namedColors.put(ORANGE,"Orange");
        namedColors.put(PAPAYA_WHIP,"PapayaWhip");
        namedColors.put(BLANCHED_ALMOND,"BlanchedAlmond");
        namedColors.put(NAVAJO_WHITE,"NavajoWhite");
        namedColors.put(ANTIQUE_WHITE,"AntiqueWhite");
        namedColors.put(TAN,"Tan");
        namedColors.put(BURLY_WOOD,"BurlyWood");
        namedColors.put(BISQUE,"Bisque");
        namedColors.put(DARK_ORANGE,"DarkOrange");
        namedColors.put(LINEN,"Linen");
        namedColors.put(PERU,"Peru");
        namedColors.put(PEACH_PUFF,"PeachPuff");
        namedColors.put(SANDY_BROWN,"SandyBrown");
        namedColors.put(CHOCOLATE,"Chocolate");
        namedColors.put(SADDLE_BROWN,"SaddleBrown");
        namedColors.put(SEASHELL,"Seashell");
        namedColors.put(SIENNA,"Sienna");
        namedColors.put(LIGHT_SALMON,"LightSalmon");
        namedColors.put(CORAL,"Coral");
        namedColors.put(ORANGE_RED,"OrangeRed");
        namedColors.put(DARK_SALMON,"DarkSalmon");
        namedColors.put(TOMATO,"Tomato");
        namedColors.put(MISTY_ROSE,"MistyRose");
        namedColors.put(SALMON,"Salmon");
        namedColors.put(SNOW,"Snow");
        namedColors.put(LIGHT_CORAL,"LightCoral");
        namedColors.put(ROSY_BROWN,"RosyBrown");
        namedColors.put(INDIAN_RED,"IndianRed");
        namedColors.put(RED,"Red");
        namedColors.put(BROWN,"Brown");
        namedColors.put(FIRE_BRICK,"FireBrick");
        namedColors.put(DARK_RED,"DarkRed");
        namedColors.put(MAROON,"Maroon");
        namedColors.put(WHITE,"White");
        namedColors.put(WHITE_SMOKE,"WhiteSmoke");
        namedColors.put(GAINSBORO,"Gainsboro");
        namedColors.put(LIGHT_GREY,"LightGrey");
        namedColors.put(SILVER,"Silver");
        namedColors.put(DARK_GRAY,"DarkGray");
        namedColors.put(GRAY,"Gray");
        namedColors.put(DIM_GRAY,"DimGray");
        namedColors.put(BLACK,"Black");
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
        revNamedColors.put("lightpink",LIGHT_PINK);
        revNamedColors.put("pink",PINK);
        revNamedColors.put("crimson",CRIMSON);
        revNamedColors.put("lavenderblush",LAVENDER_BLUSH);
        revNamedColors.put("palevioletred",PALE_VIOLET_RED);
        revNamedColors.put("hotpink",HOT_PINK);
        revNamedColors.put("deeppink",DEEP_PINK);
        revNamedColors.put("mediumvioletred",MEDIUM_VIOLET_RED);
        revNamedColors.put("orchid",ORCHID);
        revNamedColors.put("thistle",THISTLE);
        revNamedColors.put("plum",PLUM);
        revNamedColors.put("violet",VIOLET);
        revNamedColors.put("magenta",MAGENTA);
        revNamedColors.put("fuchsia",FUCHSIA);
        revNamedColors.put("darkmagenta",DARK_MAGENTA);
        revNamedColors.put("purple",PURPLE);
        revNamedColors.put("mediumorchid",MEDIUM_ORCHID);
        revNamedColors.put("darkviolet",DARK_VIOLET);
        revNamedColors.put("darkorchid",DARK_ORCHID);
        revNamedColors.put("indigo",INDIGO);
        revNamedColors.put("blueviolet",BLUE_VIOLET);
        revNamedColors.put("mediumpurple",MEDIUM_PURPLE);
        revNamedColors.put("mediumslateblue",MEDIUM_SLATE_BLUE);
        revNamedColors.put("slateblue",SLATE_BLUE);
        revNamedColors.put("darkslateblue",DARK_SLATE_BLUE);
        revNamedColors.put("lavender",LAVENDER);
        revNamedColors.put("ghostwhite",GHOST_WHITE);
        revNamedColors.put("blue",BLUE);
        revNamedColors.put("mediumblue",MEDIUM_BLUE);
        revNamedColors.put("midnightblue",MIDNIGHT_BLUE);
        revNamedColors.put("darkblue",DARK_BLUE);
        revNamedColors.put("navy",NAVY);
        revNamedColors.put("royalblue",ROYAL_BLUE);
        revNamedColors.put("cornflowerblue",CORNFLOWER_BLUE);
        revNamedColors.put("lightsteelblue",LIGHT_STEEL_BLUE);
        revNamedColors.put("lightslategray",LIGHT_SLATE_GRAY);
        revNamedColors.put("slategray",SLATE_GRAY);
        revNamedColors.put("dodgerblue",DODGER_BLUE);
        revNamedColors.put("aliceblue",ALICE_BLUE);
        revNamedColors.put("steelblue",STEEL_BLUE);
        revNamedColors.put("lightskyblue",LIGHT_SKY_BLUE);
        revNamedColors.put("skyblue",SKY_BLUE);
        revNamedColors.put("deepskyblue",DEEP_SKY_BLUE);
        revNamedColors.put("lightblue",LIGHT_BLUE);
        revNamedColors.put("powderblue",POWDER_BLUE);
        revNamedColors.put("cadetblue",CADET_BLUE);
        revNamedColors.put("azure",AZURE);
        revNamedColors.put("lightcyan",LIGHT_CYAN);
        revNamedColors.put("paleturquoise",PALE_TURQUOISE);
        revNamedColors.put("cyan",CYAN);
        revNamedColors.put("aqua",AQUA);
        revNamedColors.put("darkturquoise",DARK_TURQUOISE);
        revNamedColors.put("darkslategray",DARK_SLATE_GRAY);
        revNamedColors.put("darkcyan",DARK_CYAN);
        revNamedColors.put("teal",TEAL);
        revNamedColors.put("mediumturquoise",MEDIUM_TURQUOISE);
        revNamedColors.put("lightseagreen",LIGHT_SEA_GREEN);
        revNamedColors.put("turquoise",TURQUOISE);
        revNamedColors.put("aquamarine",AQUAMARINE);
        revNamedColors.put("mediumaquamarine",MEDIUM_AQUAMARINE);
        revNamedColors.put("mediumspringgreen",MEDIUM_SPRING_GREEN);
        revNamedColors.put("mintcream",MINT_CREAM);
        revNamedColors.put("springgreen",SPRING_GREEN);
        revNamedColors.put("mediumseagreen",MEDIUM_SEA_GREEN);
        revNamedColors.put("seagreen",SEA_GREEN);
        revNamedColors.put("honeydew",HONEYDEW);
        revNamedColors.put("lightgreen",LIGHT_GREEN);
        revNamedColors.put("palegreen",PALE_GREEN);
        revNamedColors.put("darkseagreen",DARK_SEA_GREEN);
        revNamedColors.put("limegreen",LIME_GREEN);
        revNamedColors.put("lime",LIME);
        revNamedColors.put("forestgreen",FOREST_GREEN);
        revNamedColors.put("green",GREEN);
        revNamedColors.put("darkgreen",DARK_GREEN);
        revNamedColors.put("chartreuse",CHARTREUSE);
        revNamedColors.put("lawngreen",LAWN_GREEN);
        revNamedColors.put("greenyellow",GREEN_YELLOW);
        revNamedColors.put("darkolivegreen",DARK_OLIVE_GREEN);
        revNamedColors.put("yellowgreen",YELLOW_GREEN);
        revNamedColors.put("olivedrab",OLIVE_DRAB);
        revNamedColors.put("beige",BEIGE);
        revNamedColors.put("lightgoldenrodyellow",LIGHT_GOLDENROD_YELLOW);
        revNamedColors.put("ivory",IVORY);
        revNamedColors.put("lightyellow",LIGHT_YELLOW);
        revNamedColors.put("yellow",YELLOW);
        revNamedColors.put("olive",OLIVE);
        revNamedColors.put("darkkhaki",DARK_KHAKI);
        revNamedColors.put("lemonchiffon",LEMON_CHIFFON);
        revNamedColors.put("palegoldenrod",PALE_GOLDENROD);
        revNamedColors.put("khaki",KHAKI);
        revNamedColors.put("gold",GOLD);
        revNamedColors.put("cornsilk",CORNSILK);
        revNamedColors.put("goldenrod",GOLDENROD);
        revNamedColors.put("darkgoldenrod",DARK_GOLDENROD);
        revNamedColors.put("floralwhite",FLORAL_WHITE);
        revNamedColors.put("oldlace",OLD_LACE);
        revNamedColors.put("wheat",WHEAT);
        revNamedColors.put("moccasin",MOCCASIN);
        revNamedColors.put("orange",ORANGE);
        revNamedColors.put("papayawhip",PAPAYA_WHIP);
        revNamedColors.put("blanchedalmond",BLANCHED_ALMOND);
        revNamedColors.put("navajowhite",NAVAJO_WHITE);
        revNamedColors.put("antiquewhite",ANTIQUE_WHITE);
        revNamedColors.put("tan",TAN);
        revNamedColors.put("burlywood",BURLY_WOOD);
        revNamedColors.put("bisque",BISQUE);
        revNamedColors.put("darkorange",DARK_ORANGE);
        revNamedColors.put("linen",LINEN);
        revNamedColors.put("peru",PERU);
        revNamedColors.put("peachpuff",PEACH_PUFF);
        revNamedColors.put("sandybrown",SANDY_BROWN);
        revNamedColors.put("chocolate",CHOCOLATE);
        revNamedColors.put("saddlebrown",SADDLE_BROWN);
        revNamedColors.put("seashell",SEASHELL);
        revNamedColors.put("sienna",SIENNA);
        revNamedColors.put("lightsalmon",LIGHT_SALMON);
        revNamedColors.put("coral",CORAL);
        revNamedColors.put("orangered",ORANGE_RED);
        revNamedColors.put("darksalmon",DARK_SALMON);
        revNamedColors.put("tomato",TOMATO);
        revNamedColors.put("mistyrose",MISTY_ROSE);
        revNamedColors.put("salmon",SALMON);
        revNamedColors.put("snow",SNOW);
        revNamedColors.put("lightcoral",LIGHT_CORAL);
        revNamedColors.put("rosybrown",ROSY_BROWN);
        revNamedColors.put("indianred",INDIAN_RED);
        revNamedColors.put("red",RED);
        revNamedColors.put("brown",BROWN);
        revNamedColors.put("firebrick",FIRE_BRICK);
        revNamedColors.put("darkred",DARK_RED);
        revNamedColors.put("maroon",MAROON);
        revNamedColors.put("white",WHITE);
        revNamedColors.put("whitesmoke",WHITE_SMOKE);
        revNamedColors.put("gainsboro",GAINSBORO);
        revNamedColors.put("lightgrey",LIGHT_GREY);
        revNamedColors.put("silver",SILVER);
        revNamedColors.put("darkgray",DARK_GRAY);
        revNamedColors.put("gray",GRAY);
        revNamedColors.put("dimgray",DIM_GRAY);
        revNamedColors.put("black",BLACK);
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
                return "#" + String.format( "%06x", color.getRGB() & 0xFFFFFF );   
            } else {
                return "#" + String.format( "%02x%06x", color.getAlpha(), color.getRGB() & 0xFFFFFF );       
            }
        }
    }
        
    /**
     * return either a named color or 
     * #00aaff for opaque colors and #80aaff00 for transparent colors.
     * @param color
     * @return named color or hex string like "#FF0000" for Red.
     */
    public static String encodeColor( Color color ) {
        String s= namedColors.get(color);
        if ( s!=null ) {
            return s;
        } else {
            if ( color.getAlpha()==255 ) {
                return "#" + String.format( "%06x", color.getRGB() & 0xFFFFFF );   
            } else {
                return "#" + String.format( "%02x%06x", color.getAlpha(), color.getRGB() & 0xFFFFFF );       
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
     * to improve legibility of .vap files.  
     * <a href="https://wikipedia.org/wiki/X11_color_names#Color_name_chart">X11 color names</a>
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
        
        if ( r!=null ) {
            return r;
        } else {
            try {
                Pattern p= Pattern.compile("(0x|#)([0-9a-fA-F][0-9a-fA-F])([0-9a-fA-F][0-9a-fA-F])([0-9a-fA-F][0-9a-fA-F])([0-9a-fA-F][0-9a-fA-F])");
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
