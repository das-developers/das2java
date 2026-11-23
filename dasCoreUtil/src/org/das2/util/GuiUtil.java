
package org.das2.util;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Swing Utilities written for DecoratedFileChooser, but not used.  These
 * could be useful for scripts or in the future.
 * @author jbf
 */
public class GuiUtil {
    
    /**
     * make the component flash
     * @param component
     * @param color 
     */
    public static void flashComponent(
            JComponent component, Color color) {

        Color c0= component.getBackground();
        Color c20= component.getForeground();
        component.setBackground(color);
        component.setForeground(color);
        component.repaint();
        
        Timer timer = new Timer(500, e -> {
            component.setBackground(c0);
            component.setForeground(c20);
        });
        timer.setRepeats(false);
        timer.start();
    }

    /**
     * find a component of the type, descending from the parent.
     * @param <T>
     * @param parent
     * @param type
     * @return null or the component.
     */
    public static <T extends Component> T findComponent(
        Container parent, Class<T> type) {

        for (Component c : parent.getComponents()) {
            if (type.isInstance(c)) {
                return type.cast(c);
            }
            if (c instanceof Container) {
                T result = findComponent((Container) c, type);
                if (result != null) {
                    return result;
                }
            }
        }   
        return null;
    }
    
    /**
     * find the JPanel with CardLayout descending from the parent.
     * @param parent
     * @return null or the JPanel with CardLayout
     */
    public static JPanel findCardPanel(Container parent) {
        for (Component c : parent.getComponents()) {
            if (c instanceof JPanel) {
                JPanel panel = (JPanel) c;
                if (panel.getLayout() instanceof CardLayout) {
                    return panel;
                }
            }

            if (c instanceof Container) {
                JPanel result = findCardPanel((Container) c);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }
    
    
}
