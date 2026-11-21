
package org.das2.util;


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.plaf.LayerUI;

public class FlashLayerUI extends LayerUI<JComponent> {

    private Color color = Color.GREEN;
    private float alpha = 0f;
    private Timer timer;

    /**
     * Flash the layer with the given color.Must be called on the Swing EDT.
     * @param layer
     * @param color
     */
    public void flash(JLayer<? extends JComponent> layer,
                      Color color) {

        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> flash(layer, color));
            return;
        }

        if (timer != null) {
            timer.stop();
        }

        this.color = color;
        this.alpha = 0.30f;

        final long start = System.currentTimeMillis();
        final int duration = 600;

        timer = new Timer(30, e -> {
            float elapsed =
                    (System.currentTimeMillis() - start)
                    / (float) duration;

            alpha = 0.30f * Math.max(0f, 1f - elapsed);
            layer.repaint();

            if (elapsed >= 1f) {
                timer.stop();
                alpha = 0f;
                layer.repaint();
            }
        });

        layer.repaint();
        timer.start();
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        super.paint(g, c);

        if (alpha <= 0f) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, alpha));
            g2.setColor(color);
            g2.fillRect(0, 0, c.getWidth(), c.getHeight());
        } finally {
            g2.dispose();
        }
    }

    public FlashLayerUI() {
    }
    
    public static void dumpFileViews(Component c, String indent) {
        if (c instanceof JList ||
            c instanceof JTable ||
            c instanceof JScrollPane ||
            c instanceof JLayer) {

            System.out.println(indent
                    + c.getClass().getSimpleName()
                    + " visible=" + c.isVisible()
                    + " showing=" + c.isShowing()
                    + " parent=" + (c.getParent() == null
                        ? "null"
                        : c.getParent().getClass().getSimpleName()));
        }

        if (c instanceof Container) {
            for (Component child :
                    ((Container) c).getComponents()) {
                dumpFileViews(child, indent + "  ");
            }
        }
    }
}
