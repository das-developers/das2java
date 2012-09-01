/*
 * PDFGraphicsOutput.java
 *
 * Created on January 28, 2005, 5:27 PM
 */

package org.das2.util.awt;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author eew
 */
public class PdfGraphicsOutput implements GraphicsOutput {
    
    private float width;
    private float height;
    private OutputStream out;
    private Document doc;
    private PdfWriter writer;
    private PdfContentByte cb;
    private Graphics2D graphics;

    
    /** Creates a new instance of PDFGraphicsOutput */
    public PdfGraphicsOutput() {}

    public Graphics2D getGraphics2D() {
        if (graphics != null) {
            return graphics;
        }
        graphics = cb.createGraphicsShapes(width, height);

        return graphics;
    }

    public void finish() throws IOException {
        graphics.dispose();
        cb.restoreState();
        doc.close();
    }

    public Graphics getGraphics() {
        return getGraphics2D();
    }

    public void setOutputStream(OutputStream out) {
        this.out = out;
    }

    public void setSize(int width, int height) {
        this.width = (float)width;
        this.height = (float)height;
    }
    public void start() {
        try {
            Rectangle rect = new Rectangle(width, height);
            doc = new Document(rect, 0f, 0f, 0f, 0f);
            doc.addCreator("das2.org");
            doc.addCreationDate();
            writer = PdfWriter.getInstance(doc, out);
            doc.open();
            cb = writer.getDirectContent();
            cb.saveState();
        }
        catch (DocumentException de) {
            throw new RuntimeException(de); 
        }
    }
    
}
