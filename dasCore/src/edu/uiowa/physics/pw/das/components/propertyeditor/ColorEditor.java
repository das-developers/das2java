/*
 * ColorEditor.java
 *
 * Created on April 19, 2005, 2:52 PM
 */

package edu.uiowa.physics.pw.das.components.propertyeditor;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditorSupport;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractCellEditor;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

/**
 *
 * @author eew
 */
public class ColorEditor extends AbstractCellEditor implements java.beans.PropertyEditor, TableCellEditor {

    private static List colors = new ArrayList();
    static {
        colors.add(Color.BLACK);
        colors.add(Color.WHITE);
        colors.add(Color.BLUE);
        colors.add(Color.CYAN);
        colors.add(Color.DARK_GRAY);
        colors.add(Color.GRAY);
        colors.add(Color.GREEN);
        colors.add(Color.LIGHT_GRAY);
        colors.add(Color.MAGENTA);
        colors.add(Color.ORANGE);
        colors.add(Color.PINK);
        colors.add(Color.RED);
        colors.add(Color.YELLOW);
    }

    private JColorChooser custom;
    private PropertyEditorSupport editorSupport;
    private JComboBox choice;
    
    /** Creates a new instance of ColorEditor */
    public ColorEditor() {
        editorSupport = new PropertyEditorSupport(this){};
        custom = new JColorChooser();
        choice = new JComboBox(new ColorChoiceModel()) {
            public void setBounds(int x, int y, int width, int height) {
                Dimension preferred = getPreferredSize();
                super.setBounds(x, y, width, preferred.height);
            }
        };
        choice.setRenderer(new ColorCellRenderer());
        choice.setBorder(null);
        choice.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED
                        && choice.isDisplayable()) {
                    stopCellEditing();
                }
            }
        });
        custom.addPropertyChangeListener("color", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                setValue(e.getNewValue());
            }
        });
    }

    public boolean supportsCustomEditor() { return true; }
    
    public String getAsText() { return editorSupport.getAsText(); }
    
    public Component getCustomEditor() {
        Color c = (Color)getValue();
        custom.setColor(c);
        return custom;
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        editorSupport.addPropertyChangeListener(l);
    }

    public Object getCellEditorValue() {
        return editorSupport.getValue();
    }

    public String getJavaInitializationString() {
        return "???";
    }

    public String[] getTags() {
        return null;
    }

    public Object getValue() {
        return editorSupport.getValue();
    }

    public boolean isPaintable() {
        return false;
    }

    public void paintValue(Graphics graphics, Rectangle rectangle) {
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        editorSupport.removePropertyChangeListener(l);
    }

    public void setAsText(String str) throws IllegalArgumentException {
        editorSupport.setAsText(str);
    }

    public void setValue(Object obj) {
        editorSupport.setValue(obj);
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean selected, int row, int column) {
        setValue(value);
        choice.setSelectedItem(value);
        choice.setForeground(table.getForeground());
        choice.setBackground(table.getBackground());
        return choice;
    }
    
    private class ColorChoiceModel extends AbstractListModel implements ComboBoxModel {
        
        private final String CUSTOM_LABEL = "custom...";
        
        public Object getElementAt(int index) {
            if (index < colors.size()) {
                return colors.get(index);
            }
            else if (index == colors.size()) {
                return CUSTOM_LABEL;
            }
            else {
                throw new IndexOutOfBoundsException(Integer.toString(index));
            }
        }

        public Object getSelectedItem() {
            return getValue();
        }

        public int getSize() {
            return colors.size() + 1;
        }

        public void setSelectedItem(Object obj) {
            if (obj instanceof Color) {
                setValue(obj);
            }
            else if (CUSTOM_LABEL.equals(obj)) {
                Color c = custom.showDialog(choice, "Color Editor", (Color)getValue());
                if (c != null) {
                    setValue(c);
                }
            }
            else {
                throw new IllegalArgumentException(String.valueOf(obj));
            }
        }
    }
}
