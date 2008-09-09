/*
 * ComponentsUtil.java
 *
 * Created on February 28, 2006, 12:10 PM
 *
 *
 */

package org.das2.components;

import edu.uiowa.physics.pw.das.graph.DasCanvas;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 *
 * @author Jeremy
 */
public class ComponentsUtil {
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
        if (parentWindow instanceof Frame) {
            popupWindow = new JDialog((Frame)parentWindow);
        } else if (parentWindow instanceof Dialog) {
            popupWindow = new JDialog((Dialog)parentWindow);
        } else {
            popupWindow = new JDialog();
        }
        
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
}
