/*
 * PngGraphicsOutput.java
 *
 * Created on January 31, 2005, 5:10 PM
 */

package org.das2.util.awt;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import javax.imageio.ImageIO;

/**
 *
 * @author eew
 */
public class PngGraphicsOutput implements GraphicsOutput {
    
    private OutputStream out;
    private int width;
    private int height;
    private Graphics2D graphics;
    private BufferedImage image;
    
    /** Creates a new instance of PngGraphicsOutput */
    public PngGraphicsOutput() {}

    public void finish() throws IOException {
        graphics.dispose();
        ImageIO.write(image, "PNG", out);
    }

    public Graphics getGraphics() {
        return getGraphics2D();
    }

    public Graphics2D getGraphics2D() {
        if (graphics == null) {
            graphics = image.createGraphics();
        }
        return graphics;
    }

    public void setOutputStream(OutputStream out) {
        this.out = out;
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void start() {
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    }
    
}
