package org.das2.components.propertyeditor;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import org.das2.util.StringSchemeEditor;

/**
 * Editor for CSS-like style strings.
 *
 * For example:
 * <pre>
 * background=#ffff00;color=#0000ff;fillTexture=crosshash;
 * lineThick=2;lineStyle=dashed
 * </pre>
 *
 * The supported properties are:
 * <ul>
 *   <li>background</li>
 *   <li>color</li>
 *   <li>fillTexture</li>
 *   <li>lineThick</li>
 *   <li>lineStyle</li>
 * </ul>
 *
 * Properties which are not understood by this editor are preserved.
 *
 * @author jbf
 */
public class StyleStringSchemeEditor extends JPanel
        implements StringSchemeEditor {

    private final ColorEditor backgroundEditor;
    private final ColorEditor colorEditor;

    private final JComboBox<String> fillTextureCombo;
    private final JSpinner lineThickSpinner;
    private final JComboBox<String> lineStyleCombo;

    /**
     * Properties not understood by this editor.  These are retained so that
     * editing a style string does not discard properties added elsewhere.
     */
    private final Map<String,String> other =
            new LinkedHashMap<String,String>();

    public StyleStringSchemeEditor() {

        super(new GridBagLayout());

        /*
         * Transparent background and black foreground are reasonable
         * defaults for drawing annotations and shapes.
         */
        backgroundEditor = new ColorEditor(new Color(0, true));
        colorEditor = new ColorEditor(Color.BLACK);

        fillTextureCombo = new JComboBox<String>(new String[] {
            "solid",
            "hash",
            "crosshash",
            "backhash",
            "none"
        });

        lineThickSpinner = new JSpinner(
                new SpinnerNumberModel(
                        1.0d,       // initial
                        0.0d,       // minimum
                        100.0d,     // maximum
                        0.5d));     // step

        lineStyleCombo = new JComboBox<String>(new String[] {
            "solid",
            "dashed",
            "dotted",
            "dashDot",
            "dashDotDot",
            "longDash",
            "shortDash",
            "longDashDot",
            "sparseDot"
        });

        int row = 0;

        addRow(row++, "Background:",
                backgroundEditor.getSmallEditor());

        addRow(row++, "Color:",
                colorEditor.getSmallEditor());

        addRow(row++, "Fill Texture:",
                fillTextureCombo);

        addRow(row++, "Line Thick:",
                lineThickSpinner);

        addRow(row++, "Line Style:",
                lineStyleCombo);

        /*
         * Absorb unused vertical space so that the controls remain at
         * the top of the panel.
         */
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 2;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.VERTICAL;

        add(new JPanel(), c);
    }

    /**
     * Add one property editor row.
     *
     * @param row row number
     * @param label property label
     * @param editor editor component
     */
    private void addRow(int row, String label, Component editor) {

        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = row;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(3, 6, 3, 8);

        add(new JLabel(label), c);

        c = new GridBagConstraints();

        c.gridx = 1;
        c.gridy = row;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 0, 3, 6);

        add(editor, c);
    }

    @Override
    public void setValue(String value) {

        other.clear();

        /*
         * Reset controls to defaults.
         */
        backgroundEditor.setValue(new Color(0, true));
        colorEditor.setValue(Color.BLACK);

        fillTextureCombo.setSelectedItem("solid");
        lineThickSpinner.setValue(Double.valueOf(1.0));
        lineStyleCombo.setSelectedItem("solid");

        if (value == null || value.trim().length() == 0) {
            return;
        }

        String[] ss = value.split(";");

        for (int j = 0; j < ss.length; j++) {

            String s = ss[j].trim();

            if (s.length() == 0) {
                continue;
            }

            int i = s.indexOf('=');

            /*
             * Preserve bare properties as well, should they occur.
             */
            if (i == -1) {
                other.put(s, null);
                continue;
            }

            String name = s.substring(0, i).trim();
            String val = s.substring(i + 1).trim();

            if ("background".equalsIgnoreCase(name)) {

                try {
                    backgroundEditor.setAsText(val);
                } catch (IllegalArgumentException ex) {
                    other.put(name, val);
                }

            } else if ("color".equalsIgnoreCase(name)) {

                try {
                    colorEditor.setAsText(val);
                } catch (IllegalArgumentException ex) {
                    other.put(name, val);
                }

            } else if ("fillTexture".equalsIgnoreCase(name)) {

                fillTextureCombo.setSelectedItem(val);

            } else if ("lineThick".equalsIgnoreCase(name)) {

                try {
                    lineThickSpinner.setValue(
                            Double.valueOf(Double.parseDouble(val)));
                } catch (NumberFormatException ex) {
                    other.put(name, val);
                }

            } else if ("lineStyle".equalsIgnoreCase(name)) {

                lineStyleCombo.setSelectedItem(val);

            } else {

                /*
                 * Preserve properties that this version of the editor
                 * doesn't know about.
                 */
                other.put(name, val);
            }
        }
    }

    @Override
    public String getValue() {

        StringBuilder b = new StringBuilder();

        append(b, "background",
                backgroundEditor.getAsText());

        append(b, "color",
                colorEditor.getAsText());

        append(b, "fillTexture",
                (String) fillTextureCombo.getSelectedItem());

        Number n = (Number) lineThickSpinner.getValue();

        append(b, "lineThick",
                formatNumber(n.doubleValue()));

        append(b, "lineStyle",
                (String) lineStyleCombo.getSelectedItem());

        /*
         * Append properties we didn't understand.
         */
        for (Map.Entry<String,String> e : other.entrySet()) {

            if (b.length() > 0) {
                b.append(";");
            }

            b.append(e.getKey());

            if (e.getValue() != null) {
                b.append("=");
                b.append(e.getValue());
            }
        }

        return b.toString();
    }

    /**
     * Append one name=value pair.
     */
    private static void append(
            StringBuilder b, String name, String value) {

        if (value == null || value.length() == 0) {
            return;
        }

        if (b.length() > 0) {
            b.append(";");
        }

        b.append(name);
        b.append("=");
        b.append(value);
    }

    /**
     * Format integral thicknesses without a trailing ".0".
     */
    private static String formatNumber(double d) {

        if (d == Math.rint(d)) {
            return Integer.toString((int)d);
        }

        return Double.toString(d);
    }

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public void setContext(Object context) {
        // No context required.
    }

    @Override
    public String getLabel() {
        return "Style String Editor";
    }
}