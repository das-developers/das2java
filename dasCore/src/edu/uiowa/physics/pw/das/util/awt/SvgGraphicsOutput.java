/*
 * SVGGraphicsOutput.java
 *
 * Created on January 28, 2005, 5:13 PM
 */

package edu.uiowa.physics.pw.das.util.awt;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.batik.svggen.SVGGraphics2D;
import org.w3c.dom.Document;

/**
 *
 * @author eew
 */
public class SvgGraphicsOutput implements GraphicsOutput {
    
    private Writer writer;
    private Document document;
    private SVGGraphics2D graphics;
    private int width;
    private int height;
    
    /** Creates a new instance of SVGGraphicsOutput */
    public SvgGraphicsOutput() {}

    public Graphics getGraphics() {
        return getGraphics2D();
    }

    public Graphics2D getGraphics2D() {
        if (graphics != null) {
            return graphics;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.newDocument();
            graphics = new SVGGraphics2D(document);
            graphics.setSVGCanvasSize(new Dimension(width, height));
            return graphics;
        }
        catch (ParserConfigurationException pce) {
            throw new RuntimeException(pce);
        }
    }

    public void finish() throws IOException {
        graphics.stream(writer, false);
        writer.close();
    }

    public void setOutputStream(OutputStream out) {
        try {
            this.writer = new OutputStreamWriter(out, "UTF-8");
        }
        catch (UnsupportedEncodingException uee) {
            throw new RuntimeException(uee);
        }
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void start() {
    }
    
}
