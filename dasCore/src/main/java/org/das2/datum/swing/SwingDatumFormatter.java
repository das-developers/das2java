/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.datum.swing;

import org.das2.datum.Datum;
import org.das2.datum.Units;
import org.das2.datum.format.DatumFormatter;
import org.das2.datum.format.DefaultDatumFormatterFactory;
import java.text.ParseException;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.JFormattedTextField.AbstractFormatterFactory;
import javax.swing.text.DefaultFormatterFactory;

/**
 *
 * @author eew
 */
public class SwingDatumFormatter extends AbstractFormatter {

	private Units units;

	private DatumFormatter formatter;

	public SwingDatumFormatter() {
		formatter = DefaultDatumFormatterFactory.getInstance().defaultFormatter();
	}

	@Override
	public Object stringToValue(String text) throws ParseException {
		if (units == null) {
			units = Units.dimensionless;
		}
		return units.parse(text);
	}

	@Override
	public String valueToString(Object value) throws ParseException {
		Datum d = (Datum)value;
		if (d == null) {
			return "";
		}
		units = d.getUnits();
		return formatter.format(d, units);
	}

	public Units getUnits() {
		return units;
	}

	public void setUnits(Units units) {
		this.units = units;
	}

	public static final AbstractFormatterFactory newFactory() {
		return new DefaultFormatterFactory(new SwingDatumFormatter());
	}

}
