/*
 * ComponentsUtil.java
 *
 * Created on February 28, 2006, 12:10 PM
 *
 *
 */

package org.das2.components;

import org.das2.graph.DasCanvas;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.das2.datum.LoggerManager;

/**
 * Utilities for managing components.
 * @author Jeremy
 */
public class ComponentsUtil {
    
    private static Logger logger= LoggerManager.getLogger("das2.gui");
            
    public static DasCanvas createPopupCanvas( Component parent, String title, int width, int height ) {
        DasCanvas canvas = new DasCanvas(width, height);
        
        JPanel content = new JPanel(new BorderLayout());
        
        JPanel buttonPanel = new JPanel();
        
        BoxLayout buttonLayout = new BoxLayout(buttonPanel, BoxLayout.X_AXIS);        
        JButton close = new JButton("Hide Window");
        
        buttonPanel.setLayout(buttonLayout);
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(close);
        
        content.add(canvas, BorderLayout.CENTER);
        content.add(buttonPanel, BorderLayout.SOUTH);
        
        final JDialog popupWindow;
        Window parentWindow = SwingUtilities.getWindowAncestor(parent);
        popupWindow = new JDialog(parentWindow);
        
        popupWindow.setTitle(title);
        popupWindow.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        popupWindow.setContentPane(content);
        popupWindow.pack();
        
        close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                popupWindow.setVisible(false);
            }
        });
        
        Point parentLocation = new Point();
        SwingUtilities.convertPointToScreen( parentLocation, parent );
        popupWindow.setLocation( parentLocation.x + parent.getWidth(),parentLocation.y);
        
        return canvas;
    }
    
    /**
     * return null if the rectangle is visible, or a new rectangle where it
     * should be moved if it is not.
     * @param r
     * @return null or a new suggested rectangle.
     */
    public static synchronized Rectangle verifyVisible( Rectangle r ) {
        
        long t0= System.currentTimeMillis();
        
        Dimension dimensions = Toolkit.getDefaultToolkit().getScreenSize();
        Rectangle dims= new Rectangle(0,0,dimensions.width,dimensions.height);
        logger.log(Level.FINE, "screen dimensions: {0}", dimensions);

        GraphicsEnvironment env= GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] devices = env.getScreenDevices();
        if ( devices.length>1 ) {
            for ( GraphicsDevice d : devices ) {
                dims.add( d.getDefaultConfiguration().getBounds() );
            }
        }
        
        logger.log(Level.FINE, "calculate screen dimensions in ms: {0}", System.currentTimeMillis()-t0);
        
        Rectangle visibleRect= new Rectangle(0,0,dims.width,dims.height);

        if ( visibleRect.intersects(r) ) {
            return null;
        } else {
            return visibleRect;
        }
    }
    
    
}
