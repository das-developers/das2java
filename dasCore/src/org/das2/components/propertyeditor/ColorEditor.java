/*
 * ColorEditor.java
 *
 * Created on April 19, 2005, 2:52 PM
 */

package org.das2.components.propertyeditor;

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
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import org.das2.util.DesktopColorChooserPanel;

/**
 *
 * @author eew
 */
public class ColorEditor extends AbstractCellEditor implements java.beans.PropertyEditor, TableCellEditor {

    private static final List colors = new ArrayList();
    static {
        colors.add(Color.BLACK);
        colors.add(Color.BLUE);                      // dark BRG
        colors.add(Color.RED);
        colors.add(Color.GREEN.darker());
        colors.add(Color.DARK_GRAY);                 // grey scale
        colors.add(Color.GRAY);
        colors.add(Color.LIGHT_GRAY);
        colors.add(Color.WHITE);
        colors.add( new Color(128, 128, 255) );      // light BRG
        colors.add(Color.PINK);
        colors.add(Color.GREEN);
        colors.add(Color.CYAN);
        colors.add(Color.YELLOW);
        colors.add(Color.MAGENTA);                   // others
        colors.add(Color.ORANGE);
        colors.add( new Color(0,true) );
    }
    
    private JColorChooser custom;
    private transient final PropertyEditorSupport editorSupport;
    private JComboBox choice;
    
    
    /**
     * allow clients to add additional colors.
     * @param c
     * @param name 
     */
    public static void addColor( Color c, String name ) {
        if ( colors.contains(c) ) return;
        colors.add(colors.size()-1,c); // before "none"
        ColorCellRenderer.addName( c, name );
    }
    
    /** Creates a new instance of ColorEditor */
    public ColorEditor() {
        //long t0= System.currentTimeMillis();

        editorSupport = new PropertyEditorSupport(this){};
        custom = null;
        choice = new JComboBox(new ColorChoiceModel()) {
            @Override
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
        //System.err.println( String.format( "total time in init ColorEditor: %d", ( System.currentTimeMillis()-t0 ) ));

    }
    
    /**
     * create the editor configured with the default.
     * @param init 
     */
    public ColorEditor( Color init ) {
        this();
        setValue(init);
    }

    public boolean supportsCustomEditor() { return true; }
    
    public String getAsText() { 
        int rgb= ((Color)editorSupport.getValue()).getRGB();
        String hex;
        if ( rgb==0 ) {
            hex= "#000000";
        } else {
            hex= "#"+Integer.toHexString( rgb ).substring(2); 
        }
        return hex;
    }

    private void initCustom() {
            custom= new JColorChooser();
            custom.addPropertyChangeListener("color", new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent e) {
                    setValue(e.getNewValue());
                }
            } );
            custom.setColor( (Color)getValue() );
            custom.addChooserPanel(new NamedColorChooserPanel()); 
            custom.addChooserPanel(new DesktopColorChooserPanel()); 
    }

    public Component getCustomEditor() {
        Color c = (Color)getValue();
        if ( custom==null ) {
            initCustom();
        }
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
        Color c= Color.decode(str);
        setValue(c);
    }

    public void setValue(Object obj) {
        Object oldValue= this.editorSupport.getValue();
        editorSupport.setValue(obj);
        if ( oldValue!=obj ) {
            choice.setSelectedItem(obj);
            ((ColorChoiceModel)choice.getModel()).setSelectedItem(obj);
            choice.repaint();
        }
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean selected, int row, int column) {
        setValue(value);
        ((ColorChoiceModel)choice.getModel()).setSelectedItem(value);
        choice.setForeground(table.getForeground());
        choice.setBackground(table.getBackground());
        return choice;
    }
    
    public Component getSmallEditor() {
        ((ColorChoiceModel)choice.getModel()).setSelectedItem(getValue());
        return choice;
    }
    
    private class ColorChoiceModel extends AbstractListModel implements ComboBoxModel {
        
        private final static String CUSTOM_LABEL = "custom...";
        
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
                if ( custom==null ) {
                    initCustom();
                }
                if ( JOptionPane.OK_OPTION==JOptionPane.showConfirmDialog( choice, custom, "Color Editor", JOptionPane.OK_CANCEL_OPTION  ) ) {
                    setValue(custom.getColor());
                }
            }
            else {
                throw new IllegalArgumentException(String.valueOf(obj));
            }
        }
    }
}
