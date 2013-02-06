/*
 * GraphicsOutput.java
 *
 * Created on January 28, 2005, 5:06 PM
 */

package org.das2.util.awt;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author eew
 */
public interface GraphicsOutput {
    public Graphics getGraphics();
    public Graphics2D getGraphics2D();
    public void setOutputStream(OutputStream out);
    public void setSize(int width, int height);
    public void start();
    public void finish() throws IOException;
}
