/*
 * DatumJFormatter.java
 *
 * Created on June 12, 2007, 9:11 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.das2.datum.swing;

import java.text.ParseException;
import javax.swing.JFormattedTextField;
import org.das2.datum.Datum;
import org.das2.datum.Units;
import org.das2.datum.format.DatumFormatter;
import org.das2.datum.format.DefaultDatumFormatterFactory;

/**
 *
 * @author eew
 */
public class DatumJFormatterFactory extends JFormattedTextField.AbstractFormatterFactory {
    
    DatumFormatter format;
    SwingFormatter wrapper;
    Units units;
    Units implicitUnits = Units.dimensionless;
    
    public DatumJFormatterFactory() {
        this(DefaultDatumFormatterFactory.getInstance().defaultFormatter());
    }
    
    /** Creates a new instance of DatumJFormatter */
    public DatumJFormatterFactory(DatumFormatter format) {
        this.format = format;
        this.wrapper = new SwingFormatter();
    }
    
    public void explicitUnits(Units u) {
        units = u;
    }

    public JFormattedTextField.AbstractFormatter getFormatter(JFormattedTextField tf) {
        return wrapper;
    }
    
    private class SwingFormatter extends JFormattedTextField.AbstractFormatter {
        public Object stringToValue(String text) throws ParseException {
            Units u = units != null ? units : implicitUnits;
            return u.parse(text);
        }

        public String valueToString(Object value) throws ParseException {
            Datum datum = (Datum)value;
            if (datum == null) {
                throw new ParseException("Null values not allowed.", -1);
            }
            implicitUnits = datum.getUnits();
            if (units != null) {
                return format.format(datum, units);
            }
            else {
                return format.format(datum);
            }
        }
    }
    
}
