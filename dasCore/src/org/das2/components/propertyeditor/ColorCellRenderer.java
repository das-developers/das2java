/*
 * ColorCellRenderer.java
 *
 * Created on April 28, 2005, 4:39 PM
 */

package org.das2.components.propertyeditor;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author eew
 */
class ColorCellRenderer implements ListCellRenderer, TableCellRenderer, Icon {
    private static Map names = new HashMap();
    static {
        names.put(Color.BLACK, "black");
        names.put(Color.WHITE, "white");
        names.put(Color.BLUE, "blue");
        names.put(Color.CYAN, "cyan");
        names.put(Color.DARK_GRAY, "dark gray");
        names.put(Color.GREEN.darker(),"dark green");
        names.put(new Color(128, 128, 255), "purple" );
        names.put(Color.GRAY, "gray");
        names.put(Color.GREEN, "green");
        names.put(Color.LIGHT_GRAY, "light gray");
        names.put(Color.MAGENTA, "magenta");
        names.put(Color.ORANGE, "orange");
        names.put(Color.PINK, "pink");
        names.put(Color.RED, "red");
        names.put(Color.YELLOW, "yellow");
    }
    
    protected static void addName( Color c, String name ) {
        names.put( c, name );
    }
    
    private JLabel label;
    private Border noFocusBorder = BorderFactory.createEmptyBorder(1, 1, 1, 1);
    private Color iconColor;
    
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowIndex, int columnIndex) {
        Color f = isSelected ? table.getSelectionForeground() : table.getForeground();
        Color b = isSelected ? table.getSelectionBackground() : table.getBackground();
        return getLabel(table, f, b, value, isSelected, hasFocus);
    }

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean hasFocus) {
        Color f = isSelected ? list.getSelectionForeground() : list.getForeground();
        Color b = isSelected ? list.getSelectionBackground() : list.getBackground();
        return getLabel(list, f, b, value, isSelected, hasFocus);
    }
    
    public Component getLabel(JComponent c, Color f, Color b, Object value, boolean isSelected, boolean hasFocus) {
        initLabel();
        label.setForeground(f);
        label.setBackground(b);
	label.setEnabled(c.isEnabled());
	label.setFont(c.getFont());
	label.setBorder(hasFocus ? UIManager.getBorder("List.focusCellHighlightBorder") : noFocusBorder);
        if (value instanceof Color) {
            String name = (String)names.get(value);
            if (name == null) { 
                if ( ((Color)value).getAlpha()==0 ) {
                    name="none";
                } else {
                    name = toString((Color)value); 
                }
            }
            label.setIcon(this);
            label.setText(name);
            iconColor = (Color)value;
        }
        else {
            label.setIcon(null);
            label.setText(String.valueOf(value));
        }
        return label;
    }
    
    private static String toString(Color c) {
        return "[" + c.getRed() + "," + c.getGreen() + "," + c.getBlue() + "]";
    }

    private void initLabel() {
        if (label == null) {
            label = new JLabel();
            label.setOpaque(true);
            label.setBorder(noFocusBorder);
        }
    }

    public int getIconHeight() { return 16; }

    public int getIconWidth() { return 16; }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        Color save = g.getColor();
        if ( iconColor.getAlpha()!=255 ) {
            for ( int j=0; j<16/4; j++ ) {
                for ( int i=0; i<16/4; i++ ) {
                    g.setColor( (i-j)%2 ==0 ? Color.GRAY : Color.WHITE );
                    g.fillRect( x+i*4,y+j*4,4,4);
                }
            }
        }
        g.setColor(iconColor);
        g.fillRect(x, y, getIconWidth(), getIconHeight());
        g.setColor(save);
    }

}
