package org.das2.util;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EtchedBorder;
import javax.swing.colorchooser.AbstractColorChooserPanel;

/**
 * Panel for picking colors off the desktop and maintaining a list of colors.
 * This is one of the most confusing dialogs, and needs to be either well-
 * documented or re-done.  TODO: that.
 * 1. click "Pick from Desktop"
 * 2. press S which selects the color
 * 3. clicking on another GUI will cause focus to be lost.
 * 4. or clicking on the stop button will stop.
 * @author jbf
 */
public class DesktopColorChooserPanel extends AbstractColorChooserPanel {

    JToggleButton b = new JToggleButton("Pick From Desktop");
    JLabel p = new JLabel("");
    JLabel l = new JLabel("");
    JLabel icon = new JLabel(""); // 16x16 icon of the current color
    Color proposedColor = null;
    Palette palette = new Palette();

    boolean updating; // when we are scanning for a new color

    Timer t = new Timer(100, new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (updating) {
                update();
            }
        }
    });

    private void update() {
        try {
            PointerInfo info = MouseInfo.getPointerInfo();
            Point point = info.getLocation();
            l.setEnabled(true);
            Robot r = new Robot();
            BufferedImage im = r.createScreenCapture(new Rectangle(point.x - 20, point.y - 20, 40, 40));
            if (point.x < 20) {
                im.getGraphics().clipRect(0, 0, 20 - point.x, 40);
            }
            if (point.y < 20) {
                im.getGraphics().clipRect(0, 0, 40, 20 - point.y);
            }
            p.setIcon(new ImageIcon(im.getScaledInstance(80, 80, Image.SCALE_FAST)));
            setProposedColor(new Color(im.getRGB(20, 20)));
            repaint();
        } catch (AWTException ex) {
            l.setText("Exception: " + ex.getMessage());
        }
    }

    private void setProposedColor(Color c) {
        proposedColor = c;
        icon.setIcon(colorIcon(proposedColor, 32, 32));
    }

    private void stopPicking() {
        b.setSelected(false);
        b.setText("Pick From Desktop");
        updating = false;
    }

    @Override
    public void updateChooser() {
        t.setRepeats(true);
        t.start();
    }

    private final WindowFocusListener windowFocusListener = new WindowFocusListener() {
        @Override
        public void windowGainedFocus(WindowEvent e) {
            l.setText(MSG_GRABBING);
            l.setEnabled(true);
        }

        @Override
        public void windowLostFocus(WindowEvent e) {
            l.setText("Focus lost, click here to resume selection with keyboard.");
            l.setEnabled(false);
            stopPicking();
        }
    };

    @Override
    protected void buildChooser() {
        this.setLayout(new BorderLayout());
        l.setText(MSG_GRABBING);
        l.setEnabled(false);
        add(l, BorderLayout.NORTH);
        p.setIcon(colorIcon(Color.white, 80, 80));
        add(p, BorderLayout.CENTER);
        icon.setIcon(colorIcon(Color.white, 32, 32));
        add(icon, BorderLayout.WEST);
        icon.setBorder(new EtchedBorder());
        b.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (b.isSelected()) {
                    b.setText("S to Select color, click here to Stop picking");
                    updating = true;
                } else {
                    stopPicking();
                }
            }
        });
        add(b, BorderLayout.SOUTH);
        b.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if ((e.getKeyChar() == 's' || e.getKeyChar() == 'S') && proposedColor != null) {
                    getColorSelectionModel().setSelectedColor(proposedColor);
                    palette.addToPalette(proposedColor);
                } else if (e.getKeyChar() == 'x' || e.getKeyChar() == 'X') {
                    stopPicking();
                }
                super.keyPressed(e); //To change body of generated methods, choose Tools | Templates.
            }
        });
        add(palette, BorderLayout.EAST);
        SwingUtilities.invokeLater( new Runnable() {
            @Override
            public void run() {
                Window w= SwingUtilities.getWindowAncestor(b);
                if ( w!=null ) w.addWindowFocusListener(windowFocusListener);
            }
        });
    }
    protected static final String MSG_GRABBING = "<html>Grab colors from the desktop with the mouse.<br>S to select color, X to exit collection.";

    @Override
    public String getDisplayName() {
        return "Desktop Sampler";
    }

    @Override
    public Icon getSmallDisplayIcon() {
        return null;
    }

    @Override
    public Icon getLargeDisplayIcon() {
        return null;
    }

    public class Palette extends JComponent {

        List<Color> palette = new ArrayList();

        private Palette() {
            StringBuilder dp = new StringBuilder();
            dp.append(toHexString(Color.WHITE)).append(",");
            dp.append(toHexString(Color.RED.brighter())).append(",");
            dp.append(toHexString(Color.GREEN.brighter())).append(",");
            dp.append(toHexString(Color.BLUE.brighter())).append(",");
            dp.append(toHexString(Color.GRAY)).append(",");
            dp.append(toHexString(Color.RED)).append(",");
            dp.append(toHexString(Color.GREEN)).append(",");
            dp.append(toHexString(Color.BLUE)).append(",");
            dp.append(toHexString(Color.BLACK)).append(",");
            dp.append(toHexString(Color.RED.darker())).append(",");
            dp.append(toHexString(Color.GREEN.darker())).append(",");
            dp.append(toHexString(Color.BLUE.darker())).append(",");

            String ps = Preferences.userNodeForPackage(this.getClass()).get("palette", dp.toString());
            setPalette(ps);

            setBounds(0, 0, 2 * POFFX + 4 * PSIZE, 2 * POFFY + 3 * PSIZE);
            setMinimumSize(new Dimension(2 * POFFX + 4 * PSIZE, 2 * POFFY + 3 * PSIZE));
            setMaximumSize(getMinimumSize());
            setPreferredSize(getMinimumSize());
            setSize(getMinimumSize());
            setBorder(new EtchedBorder());

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int index = ((e.getY() - POFFY) / PSIZE) * 4 + (e.getX() - POFFX) / PSIZE;
                    if (index < palette.size()) {
                        Color c = palette.get(index);
                        getColorSelectionModel().setSelectedColor(c);
                        setProposedColor(c);
                    }
                }
            });
        }

        private void setPalette(String s) {
            String[] ss = s.split(",");
            for (int i = 0; i < 12; i++) {
                palette.add(Color.decode(ss[i]));
            }
        }

        private int PSIZE = 20;
        private int POFFX = 3;
        private int POFFY = 3;

        @Override
        protected void paintComponent(Graphics g) {

            for (int j = 0; j < 3; j++) {
                for (int i = 0; i < 4; i++) {
                    if (palette.size() > j * 4 + i) {
                        g.setColor(palette.get(j * 4 + i));
                        Rectangle r = new Rectangle(POFFX + i * PSIZE, POFFY + j * PSIZE, PSIZE - 1, PSIZE - 1);
                        g.fillRect(r.x, r.y, r.width, r.height);
                    }
                }
            }
            super.paintComponent(g); //To change body of generated methods, choose Tools | Templates.
        }

        public void addToPalette(Color c) {
            palette.add(0, c);
            while (palette.size() > 12) {
                palette.remove(12);
            }
            StringBuilder dp = new StringBuilder();
            for (Color color : palette) {
                dp.append(toHexString(color)).append(",");
            }
            Preferences.userNodeForPackage(this.getClass()).put("palette", dp.toString());
        }

    }

    /**
     * return an icon block with the color and size.
     *
     * @param iconColor the color
     * @param w the width in pixels
     * @param h the height in pixels
     * @return an icon.
     */
    public static Icon colorIcon(Color iconColor, int w, int h) {
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics g = image.getGraphics();
        if (iconColor.getAlpha() != 255) { // draw checkerboard to indicate transparency
            for (int j = 0; j < 16 / 4; j++) {
                for (int i = 0; i < 16 / 4; i++) {
                    g.setColor((i - j) % 2 == 0 ? Color.GRAY : Color.WHITE);
                    g.fillRect(0 + i * 4, 0 + j * 4, 4, 4);
                }
            }
        }
        g.setColor(iconColor);
        g.fillRect(0, 0, w, h);
        return new ImageIcon(image);
    }

    public static String toHexString(Color c) {
        return "0x" + Integer.toHexString(c.getRGB()).substring(2).toUpperCase();
    }

    public static void main(String[] args) {
        JColorChooser custom = new JColorChooser();
        custom.addChooserPanel(new DesktopColorChooserPanel());
        if (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(null, custom)) {
            System.err.println("c: " + custom.getColor());
        }
    }
}
