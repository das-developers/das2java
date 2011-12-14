package org.das2.event;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import org.das2.graph.DasCanvasComponent;
import org.das2.util.GrannyTextRenderer;

/**
 * see LabelDragRenderer.  Introduced to remove all double inner classes (LabelDragRenderer$InfoLabel$1.class, because I think they cause problems with pack200.
 * @author jbf
 */
class InfoLabel {

    JWindow window;
    JPanel label;
    GrannyTextRenderer gtr;
    JPanel containedPanel;
    JComponent glassPane;
    boolean contained = true;

    void init(DasCanvasComponent parent) {
        Window root = (Window) SwingUtilities.getRoot(parent);
        window = new JWindow(root);
        label = new JPanel() {

            @Override
            public void paintComponent(Graphics g) {
                ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.clearRect(0, 0, getWidth(), getHeight());
                gtr.draw(g, 0, (int) gtr.getAscent());
            }
        };
        label.setOpaque(true);
        label.setPreferredSize(new Dimension(300, 20));
        window.getContentPane().add(label);
        window.pack();
        gtr = new GrannyTextRenderer();
        glassPane = (JComponent) parent.getCanvas().getGlassPane();
        containedPanel = new JPanel() {

            @Override
            public void paintComponent(Graphics g) {
                ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.clearRect(0, 0, getWidth(), getHeight());
                gtr.draw(g, 0, (int) gtr.getAscent());
            }
        };
        containedPanel.setVisible(false);
        glassPane.add(containedPanel);
        contained = true;
    }

    void setText(String text, Point p, DasCanvasComponent parent, int labelPositionX, int labelPositionY) {
        if (window == null) {
            init(parent);
        }
        if (text != null) {
            gtr.setString(containedPanel.getFont(), text);
            Rectangle rect = gtr.getBounds();
            int posx = p.x + labelPositionX * 3 + Math.min(labelPositionX, 0) * rect.width;
            int posy = p.y + labelPositionY * 3 + Math.min(labelPositionY, 0) * rect.height;
            Rectangle bounds = gtr.getBounds();
            Point p2 = new Point(posx, posy);
            SwingUtilities.convertPointFromScreen(p2, glassPane);
            bounds.translate(p2.x, p2.y);
            contained = glassPane.getBounds().contains(bounds);
            if (contained) {
                containedPanel.setSize(new Dimension(rect.width, rect.height));
                containedPanel.setLocation(p2.x, p2.y);
                window.setVisible(false);
                containedPanel.setVisible(true);
                containedPanel.repaint();
            } else {
                gtr.setString(label.getFont(), text);
                rect = gtr.getBounds();
                window.setSize(new Dimension(rect.width, rect.height));
                posx = p.x + labelPositionX * 3 + Math.min(labelPositionX, 0) * rect.width;
                posy = p.y + labelPositionY * 3 + Math.min(labelPositionY, 0) * rect.height;
                containedPanel.setVisible(false);
                window.setLocation(posx, posy);
                window.setVisible(true);
                window.repaint();
            }
        } else {
            hide(parent);
        }
    }

    void hide(DasCanvasComponent parent) {
        if (window == null) {
            init(parent);
        }
        if (contained) {
            containedPanel.setVisible(false);
        } else {
            window.setVisible(false);
        }
    }
}
