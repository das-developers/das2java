package edu.uiowa.physics.pw.das.components.propertyeditor;

import java.text.ParseException;
import javax.swing.JFormattedTextField;
import javax.swing.text.DocumentFilter;

class FloatingPointFormatter extends JFormattedTextField.AbstractFormatter {
    
    /** Parses <code>text</code> returning an arbitrary Object. Some
     * formatters may return null.
     *
     * @throws ParseException if there is an error in the conversion
     * @param text String to convert
     * @return Object representation of text
     *
     */
    public Object stringToValue(String text) throws ParseException {
        try {
            Double d = new Double(text);
            if (d.doubleValue() == Double.NEGATIVE_INFINITY ||
            d.doubleValue() == Double.POSITIVE_INFINITY ||
            d.doubleValue() == Double.NaN) {
                throw new ParseException("+/-infinity and NaN are not allowed", 0);
            }
            return d;
        }
        catch (NumberFormatException nfe) {
            throw new ParseException(nfe.getMessage(), 0);
        }
    }
    
    /** Returns the string value to display for <code>value</code>.
     *
     * @throws ParseException if there is an error in the conversion
     * @param value Value to convert
     * @return String representation of value
     *
     */
    public String valueToString(Object value) throws ParseException {
        if (value instanceof Number) {
            double doubleValue = ((Number)value).doubleValue();
            return value.toString();
        }
        else throw new ParseException("value must be of type Number", 0);
    }
    
    protected DocumentFilter getDocumentFilter() {
        return new FloatingPointDocumentFilter();
    }
    
}

