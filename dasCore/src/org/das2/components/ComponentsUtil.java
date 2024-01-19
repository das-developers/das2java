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
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
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
            @Override
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
        
        logger.log(Level.FINE, "verifyVisible({0})", r);
        long t0= System.currentTimeMillis();
        
        Rectangle visibleRect= new Rectangle();

        GraphicsEnvironment env= GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] devices = env.getScreenDevices();
        if ( devices.length>1 ) {
            for ( GraphicsDevice d : devices ) {
                visibleRect.add( d.getDefaultConfiguration().getBounds() );
            }
        }
        
        logger.log(Level.FINER, "calculate screen dimensions in ms: {0}", System.currentTimeMillis()-t0);
        
        logger.log(Level.FINE, "visibleRect: {0}", visibleRect);

        if ( visibleRect.intersects(r) ) {
            return null;
        } else {
            return visibleRect;
        }
    }
    
    public static Action getPdfButtonAction( final DasCanvas canvas ) {
        return new AbstractAction("PDF...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                final JFileChooser fileChooser = new JFileChooser();
                String ext= "pdf";
                fileChooser.setDialogTitle("Print to "+ext.toUpperCase());
                fileChooser.setFileFilter( new FileNameExtensionFilter( ext + " files", ext ) );
                Preferences prefs = Preferences.userRoot().node("org.das2");
                String savedir = prefs.get("savedir", null);
                if (savedir != null) fileChooser.setCurrentDirectory(new File(savedir));
                int choice = fileChooser.showSaveDialog(canvas);
                if (choice == JFileChooser.APPROVE_OPTION) {
                    try {
                        canvas.writeToPDF(fileChooser.getSelectedFile().toString());
                    } catch (IOException ex) {
                        logger.log(Level.SEVERE, null, ex);
                    }
                }
            }
        };
    }
                
}
