package org.das2.util;

/**
 * Parses "color=red;label=\"Hello; world\";width=3;style=dashed"
 * The delimiter can also be set to &amp; (ampersand character).
 * @author jbf
 */
import java.util.LinkedHashMap;
import java.util.Map;

public class NameValueParser {

    private char delim = ';';

    public char getDelim() {
        return delim;
    }

    /**
     * set the delimiter to ampersand or comma or semicolon.
     * @param delim 
     */
    public void setDelim(char delim) {
        this.delim = delim;
    }

    private char assign = '=';

    public char getAssign() {
        return assign;
    }

    /**
     * set the character used between the name and value, such as = or : (equal or colon).
     * @param assign 
     */
    public void setAssign(char assign) {
        this.assign = assign;
    }

    private String missing = "";

    public static final String PROP_MISSING = "missing";

    public String getMissing() {
        return missing;
    }

    /**
     * set the missing value string
     * @param missing 
     */
    public void setMissing(String missing) {
        this.missing = missing;
    }

    /**
     * parse the string into name, value pairs.  
     * @param s
     * @return 
     */
    public Map<String, String> parse(String s) {
        Map<String, String> result = new LinkedHashMap<>();

        boolean quoted = false;
        int start = 0;

        for (int i = 0; i <= s.length(); i++) {
            char c = i < s.length() ? s.charAt(i) : delim;

            if (c == '"') {
                quoted = !quoted;
            } else if (c == delim && !quoted) {
                String item = s.substring(start, i).trim();
                start = i + 1;

                if (item.length() == 0) {
                    continue;
                }

                String name,value;
                
                int eq = item.indexOf(assign);
                if (eq < 0) {
                    name = item;
                    value = missing;
                } else {
                    name = item.substring(0, eq).trim();
                    value = item.substring(eq + 1).trim();
                }

                if (value.length() >= 2
                        && value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }

                result.put(name, value);
            }
        }

        if (quoted) {
            throw new IllegalArgumentException("Unterminated quote");
        }

        return result;
    }
}
