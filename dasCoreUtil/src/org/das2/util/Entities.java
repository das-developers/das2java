/**
 * From Lucene Search Engine.
 * code found at http://www.koders.com, decodeEntities() added.
 */
package org.das2.util;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

/**
 * Encoder and decoder for HTML entities, like &amp;rho; for &rho;.
 * See https://shapecatcher.com/ to identify shapes and
 * also https://detexify.kirelabs.org/classify.html for identifying
 * LaTeX characters (which are not supported here).
 * 
 * @author jbf
 */
public class Entities {

    /**
     * return true of the font supports each character of the string.
     * @param f
     * @param s
     * @return 
     */
    public static boolean fontSupports( Font f, String s ) {
        for ( char c: s.toCharArray() ) {
            if ( !f.canDisplay(c) ) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * decoder goes from &amp;rho; to "&rho;"
     */
    static final HashMap<String,String> decoder = new HashMap(300);
    static final String[] encoder = new String[0x8000];

    /**
     * utility method for decoding entities like &amp;rho; into UNICODE.
     * Malformed entities (like &#03B1; instead of &#x03B1;) are formatted as "???"
     * @param str string e.g. "&amp;rho; degrees"
     * @return string with Unicode characters for entities.
     */
    public static String decodeEntities(String str) {
        int i0=0, i;
        
        int MAX_ENTITY_LEN=10;
        StringBuilder result= new StringBuilder();
        while ( true ) {
            i= str.indexOf("&",i0);
            
            if ( i==-1 ) {
                result.append( str.substring(i0) );
                return result.toString();
            } else {
                int i1= str.indexOf(";",i);
                if ( i1!=-1 && i1-i<MAX_ENTITY_LEN) {
                    result.append( str.substring(i0,i));
                    try {
                        String n= decode( str.substring(i,i1+1) );
                        if ( n.length()==0 ) {
                            result.append(str.substring(i,i1+1));
                        } else {
                            result.append( n );
                        }
                    } catch ( NumberFormatException ex ) {
                        result.append( "???" );  // indicate there's some sort of problem.
                    }
                    i0= i1+1;
                    if ( i0==str.length() ) return result.toString();
                } else {
                    result.append( str.substring(i0,i+1) );
                    i0= i+1;
                }
            }
        }
    }

    /**
     * decode one entity, and "" empty string is returned when the string cannot be decoded.
     * @param entity like &amp;rho;
     * @return unicode character like "&rho;"
     */
    public static String decode(String entity) {
        if (entity.charAt(entity.length() - 1) == ';') // remove trailing semicolon
        {
            entity = entity.substring(0, entity.length() - 1);
        }
        if (entity.charAt(1) == '#') {
            int start = 2;
            int radix = 10;
            if (entity.charAt(2) == 'X' || entity.charAt(2) == 'x') {
                start++;
                radix = 16;
            }
            Character c = (char) Integer.parseInt(entity.substring(start), radix);
            return c.toString();
        } else {
            String s = (String) decoder.get(entity);
            if (s != null) {
                return s;
            } else {
                return "";
            }
        }
    }

    /**
     * encode one or more entities.
     * @param s string with unicode like "&rho;"
     * @return like &amp;rho;
     */
    public static String encode(String s) {
        int length = s.length();
        StringBuilder buffer = new StringBuilder(length * 2);
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            int j = (int) c;
            if (j < 0x8000 && encoder[j] != null) {
                buffer.append(encoder[j]);		  // have a named encoding
                buffer.append(';');
            } else if (j < 0x80) {
                buffer.append(c);			      // use ASCII value
            } else {
                buffer.append("&#");			  // use numeric encoding
                buffer.append((int) c);
                buffer.append(';');
            }
        }
        return buffer.toString();
    }

    static void add(String entity, int value) {
        decoder.put(entity, String.valueOf((char) value) );
        if (value < 0x8000) {
            encoder[value] = entity;
        }
    }
    

    static {
        add("&nbsp", 160);
        add("&iexcl", 161);
        add("&cent", 162);
        add("&pound", 163);
        add("&curren", 164);
        add("&yen", 165);
        add("&brvbar", 166);
        add("&sect", 167);
        add("&uml", 168);
        add("&copy", 169);
        add("&ordf", 170);
        add("&laquo", 171);
        add("&not", 172);
        add("&shy", 173);
        add("&reg", 174);
        add("&macr", 175);
        add("&deg", 176);
        add("&plusmn", 177);
        add("&sup2", 178);
        add("&sup3", 179);
        add("&acute", 180);
        add("&micro", 181);
        add("&para", 182);
        add("&middot", 183);
        add("&cedil", 184);
        add("&sup1", 185);
        add("&ordm", 186);
        add("&raquo", 187);
        add("&frac14", 188);
        add("&frac12", 189);
        add("&frac34", 190);
        add("&iquest", 191);
        add("&Agrave", 192);
        add("&Aacute", 193);
        add("&Acirc", 194);
        add("&Atilde", 195);
        add("&Auml", 196);
        add("&Aring", 197);
        add("&AElig", 198);
        add("&Ccedil", 199);
        add("&Egrave", 200);
        add("&Eacute", 201);
        add("&Ecirc", 202);
        add("&Euml", 203);
        add("&Igrave", 204);
        add("&Iacute", 205);
        add("&Icirc", 206);
        add("&Iuml", 207);
        add("&ETH", 208);
        add("&Ntilde", 209);
        add("&Ograve", 210);
        add("&Oacute", 211);
        add("&Ocirc", 212);
        add("&Otilde", 213);
        add("&Ouml", 214);
        add("&times", 215);
        add("&Oslash", 216);
        add("&Ugrave", 217);
        add("&Uacute", 218);
        add("&Ucirc", 219);
        add("&Uuml", 220);
        add("&Yacute", 221);
        add("&THORN", 222);
        add("&szlig", 223);
        add("&agrave", 224);
        add("&aacute", 225);
        add("&acirc", 226);
        add("&atilde", 227);
        add("&auml", 228);
        add("&aring", 229);
        add("&aelig", 230);
        add("&ccedil", 231);
        add("&egrave", 232);
        add("&eacute", 233);
        add("&ecirc", 234);
        add("&euml", 235);
        add("&igrave", 236);
        add("&iacute", 237);
        add("&icirc", 238);
        add("&iuml", 239);
        add("&eth", 240);
        add("&ntilde", 241);
        add("&ograve", 242);
        add("&oacute", 243);
        add("&ocirc", 244);
        add("&otilde", 245);
        add("&ouml", 246);
        add("&divide", 247);
        add("&oslash", 248);
        add("&ugrave", 249);
        add("&uacute", 250);
        add("&ucirc", 251);
        add("&uuml", 252);
        add("&yacute", 253);
        add("&thorn", 254);
        add("&yuml", 255);
        add("&fnof", 402);
        add("&Alpha", 913);
        add("&Beta", 914);
        add("&Gamma", 915);
        add("&Delta", 916);
        add("&Epsilon", 917);
        add("&Zeta", 918);
        add("&Eta", 919);
        add("&Theta", 920);
        add("&Iota", 921);
        add("&Kappa", 922);
        add("&Lambda", 923);
        add("&Mu", 924);
        add("&Nu", 925);
        add("&Xi", 926);
        add("&Omicron", 927);
        add("&Pi", 928);
        add("&Rho", 929);
        add("&Sigma", 931);
        add("&Tau", 932);
        add("&Upsilon", 933);
        add("&Phi", 934);
        add("&Chi", 935);
        add("&Psi", 936);
        add("&Omega", 937);
        add("&alpha", 945);
        add("&beta", 946);
        add("&gamma", 947);
        add("&delta", 948);
        add("&epsilon", 949);
        add("&zeta", 950);
        add("&eta", 951);
        add("&theta", 952);
        add("&iota", 953);
        add("&kappa", 954);
        add("&lambda", 955);
        add("&mu", 956);
        add("&nu", 957);
        add("&xi", 958);
        add("&omicron", 959);
        add("&pi", 960);
        add("&rho", 961);
        add("&sigmaf", 962);
        add("&sigma", 963);
        add("&tau", 964);
        add("&upsilon", 965);
        add("&phi", 966);
        add("&chi", 967);
        add("&psi", 968);
        add("&omega", 969);
        add("&thetasym", 977);
        add("&upsih", 978);
        add("&piv", 982);
        add("&bull", 8226);
        add("&hellip", 8230);
        add("&prime", 8242);
        add("&Prime", 8243);
        add("&oline", 8254);
        add("&frasl", 8260);
        add("&weierp", 8472);
        add("&image", 8465);
        add("&real", 8476);
        add("&trade", 8482);
        add("&alefsym", 8501);
        add("&larr", 8592);
        add("&uarr", 8593);
        add("&rarr", 8594);
        add("&darr", 8595);
        add("&harr", 8596);
        add("&crarr", 8629);
        add("&lArr", 8656);
        add("&uArr", 8657);
        add("&rArr", 8658);
        add("&dArr", 8659);
        add("&hArr", 8660);
        add("&forall", 8704);
        add("&part", 8706);
        add("&exist", 8707);
        add("&empty", 8709);
        add("&nabla", 8711);
        add("&isin", 8712);
        add("&notin", 8713);
        add("&ni", 8715);
        add("&prod", 8719);
        add("&sum", 8721);
        add("&minus", 8722);
        add("&lowast", 8727);
        add("&radic", 8730);
        add("&prop", 8733); // a.k.a Proportional
        add("&infin", 8734);
        add("&ang", 8736);
        add("&and", 8743);
        add("&or", 8744);
        add("&cap", 8745);
        add("&cup", 8746);
        add("&int", 8747);
        add("&there4", 8756);
        add("&sim", 8764);
        add("&cong", 8773);
        add("&asymp", 8776);
        add("&ne", 8800);
        add("&equiv", 8801);
        add("&le", 8804);
        add("&ge", 8805);
        add("&sub", 8834);
        add("&sup", 8835);
        add("&nsub", 8836);
        add("&sube", 8838);
        add("&supe", 8839);
        add("&oplus", 8853);
        add("&otimes", 8855);
        add("&perp", 8869);
        add("&shortparallel",8741);   // parallel to
        add("&parallel", 8741 );
        add("&NotDoubleVerticalBar",8742); // not parallel to 
        add("&npar",8742);
        add("&sdot", 8901);
        add("&lceil", 8968);
        add("&rceil", 8969);
        add("&lfloor", 8970);
        add("&rfloor", 8971);
        add("&lang", 9001);
        add("&rang", 9002);
        add("&loz", 9674);
        add("&spades", 9824);
        add("&clubs", 9827);
        add("&hearts", 9829);
        add("&diams", 9830);
        add("&quot", 34);
        add("&amp", 38);
        add("&lt", 60);
        add("&gt", 62);
        add("&OElig", 338);
        add("&oelig", 339);
        add("&Scaron", 352);
        add("&scaron", 353);
        add("&Yuml", 376);
        add("&circ", 710);
        add("&tilde", 732);
        add("&ensp", 8194);
        add("&emsp", 8195);
        add("&thinsp", 8201);
        add("&zwnj", 8204);
        add("&zwj", 8205);
        add("&lrm", 8206);
        add("&rlm", 8207);
        add("&ndash", 8211);
        add("&mdash", 8212);
        add("&lsquo", 8216);
        add("&rsquo", 8217);
        add("&sbquo", 8218);
        add("&ldquo", 8220);
        add("&rdquo", 8221);
        add("&bdquo", 8222);
        add("&dagger", 8224);
        add("&Dagger", 8225);
        add("&permil", 8240);
        add("&lsaquo", 8249);
        add("&rsaquo", 8250);
        add("&euro", 8364);

    }
    
    /**
     * provide a picker GUI
     * @return empty string or the selected character HTML.
     */
    public static String pickEntityGUI() {
        Object[][] rowData;
        
        Object[] columns= new String[] { "Character", "Number" };
        List<String[]> items= new ArrayList<>();
        
        for ( Entry<String,String> e: decoder.entrySet() ) {
            items.add( new String[] { e.getValue(), e.getKey()+";" } );
        }
        
        rowData= items.toArray( new String[items.size()][] );
                
        JTable t= new JTable(rowData,columns);
        t.setFont( Font.decode("sans-14") );
        t.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        
        JScrollPane p= new JScrollPane(t);
        
        // also https://www.reilldesign.com/tutorials/character-entity-reference-chart.html
        JButton l= new JButton( new AbstractAction("See also https://www.freeformatter.com/html-entities.html") {
            @Override
            public void actionPerformed(ActionEvent e) {
                String target= "https://www.freeformatter.com/html-entities.html";
                try {
                    Desktop.getDesktop().browse( new URI(target) );
                } catch (URISyntaxException | IOException ex) {
                } 
            }
        });
        
        // also https://www.reilldesign.com/tutorials/character-entity-reference-chart.html
        JButton l2= new JButton( new AbstractAction("Find by sketch: https://shapecatcher.com/") {
            @Override
            public void actionPerformed(ActionEvent e) {
                String target= "https://shapecatcher.com/";
                try {
                    Desktop.getDesktop().browse( new URI(target) );
                } catch (URISyntaxException | IOException ex) {
                } 
            }
        });
        
        JPanel refsPanel= new JPanel();
        refsPanel.setLayout( new BoxLayout(refsPanel,BoxLayout.Y_AXIS ) );
        refsPanel.add( l );
        refsPanel.add( l2 );
        
        JPanel panel= new JPanel();
        panel.setLayout(new BorderLayout());
        
        panel.add(refsPanel,BorderLayout.NORTH);
        panel.add(p);
        
        if ( JOptionPane.showConfirmDialog( null, panel, "HTML Entities", JOptionPane.OK_CANCEL_OPTION )==JOptionPane.OK_OPTION ) {
            if ( t.getSelectedRow()==-1 ) {
                return "";
            } else {
                return (String)rowData[t.getSelectedRow()][1];
            }
        } else {
            return "";
        }
    }
    
    public static void main( String[] args ) {
        System.err.println("pick: " + pickEntityGUI() );
    }
}