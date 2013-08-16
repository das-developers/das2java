/*
 * PDFGraphicsOutput.java
 *
 * Created on January 28, 2005, 5:27 PM
 */

package org.das2.util.awt;

import com.itextpdf.awt.FontMapper;
import com.itextpdf.awt.PdfGraphics2D;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfIndirectReference;
import com.itextpdf.text.pdf.PdfStream;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.util.FileUtil;
import org.das2.util.LoggerManager;

/**
 * support writing to PDF.
 * @author eew
 */
public class PdfGraphicsOutput implements GraphicsOutput {
    
    private static final Logger logger= LoggerManager.getLogger("das2.graphics.pdf");
    
    private float width;
    private float height;
    private OutputStream out;
    private Document doc;
    private PdfWriter writer;
    private PdfContentByte cb;
    private Graphics2D graphics;

    /**
     * return the name of the .ttf file for the platform, or null.
     * @param font
     * @return 
     */
    String ttfFromName( java.awt.Font font ) {
        String osName= System.getProperty( "os.name" ); 
        String username= System.getProperty( "user.name" );
        if ( osName.startsWith("Mac") ) {
            String s=  "/Users/" + username + "/Library/Fonts/" + font.getName() + ".ttf" ;
            if ( new File(s).exists() ) {
                return s;
            }
            s= "/Library/Fonts/" + font.getName() + ".ttf";
            if ( new File(s).exists() ) {
                return s;
            }
            logger.log(Level.WARNING, "unable to find font file {0}", s);
        } else if ( osName.startsWith("Linux") ) {
            File f= new File("/usr/share/fonts/truetype");
            File ff= FileUtil.find( f, font.getName().replaceAll(" ","") + ".ttf" );
            return ff==null ? null : ff.getPath();
        } else if ( osName.startsWith("Windows") ) {
            File f= new File("C:/Windows/Fonts");
            File ff= FileUtil.find( f, font.getName() + ".ttf" );
            return ff==null ? null : ff.getPath();
        }
        return null;
    }
        
    FontMapper fontMapper = new FontMapper() {
        public BaseFont awtToPdf(java.awt.Font font) {
            try {
                FontFactory.registerDirectories();
                System.err.println( font.getName() );
                String ffile= ttfFromName(font);
                if ( ffile==null ) {
                    logger.warning("couldn't find font file");
                    return BaseFont.createFont();
                } else {
                    return BaseFont.createFont(
                        ffile,
                        BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                }
            } catch (DocumentException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        public java.awt.Font pdfToAwt(BaseFont font, int size) {
            return null;
        }
    };

    /** Creates a new instance of PDFGraphicsOutput */
    public PdfGraphicsOutput() {}

    public Graphics2D getGraphics2D() {
        if (graphics != null) {
            return graphics;
        }
        if ( graphicsShapes ) {
            graphics = new PdfGraphics2D(cb, width, height, true);
        } else {
            graphics = new PdfGraphics2D(cb, width, height, fontMapper);
        }

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

    private boolean graphicsShapes= true;
    
    /**
     * If true, then fonts are written out as lines and will match the screen.
     * If false, then labels are editable.
     * @param graphicsShapes 
     */
    public void setGraphicsShapes( boolean graphicsShapes ) {
        this.graphicsShapes= graphicsShapes;
    }
    
    /**
     * set the size in points.
     * @param width
     * @param height 
     */
    public void setSize( int width, int height ) {
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
        } catch (DocumentException de) {
            throw new RuntimeException(de); 
        }
    }
    
}