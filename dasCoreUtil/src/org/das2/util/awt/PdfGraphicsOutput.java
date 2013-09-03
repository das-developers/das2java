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
import com.itextpdf.text.pdf.BaseFont;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
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

    public static final String READING_FONTS="PleaseWait";
    public static final Object STATE_IDLE="idle";
    public static final Object STATE_READING="reading";
    public static final Object STATE_READY="ready";
    
    private static Map<String,File> fontToTtfMap;
    private static Object state= STATE_IDLE;

    /* these were introduced to make findbugs happy, but it also clarifies code.  On Macs and Linux, we also look in the user's home */
    private static final String MAC_FONT_HOME= "/Library/Fonts/";
    private static final String WINDOWS_FONT_HOME_1= "C:/Windows/Fonts";
    private static final String WINDOWS_FONT_HOME_2= "D:/Windows/Fonts";
    private static final String LINUX_FONT_HOME= "/usr/share/fonts/";
    private static final String SOLARIS_FONT_HOME= "/usr/X/lib/X11/fonts/TrueType/";
    /**
     * Establish a name to .ttf file mapping.  On my development system with 233 fonts, this takes less than 400ms.
     */
    private synchronized static Map<String,File> getFontToTtfMap() {
        
        String osName= System.getProperty( "os.name" );
        String userhome= System.getProperty("user.home");

        // Identify the search path for ttf fonts, which must have the extension .ttf.  See http://www.fontation.com/feature/ which confirms this code.
        File[] dirs;
        if ( osName.startsWith("Mac") ) {
            dirs= new File[] { new File( userhome + "/Library/Fonts/" ), new File( MAC_FONT_HOME ) };
        } else if ( osName.startsWith("Linux") ) {
            dirs= new File[] { new File( userhome, ".fonts" ), new File( LINUX_FONT_HOME ) }; // note Ubuntu is /usr/share/fonts/truetype, but this will work.
        } else if ( osName.startsWith("Windows") ) {
            dirs= new File[] { new File( WINDOWS_FONT_HOME_1), new File( WINDOWS_FONT_HOME_2 ) }; // 
        } else if ( osName.startsWith("SunOS") ) {
            dirs= new File[] { new File( userhome, "fonts" ), new File( SOLARIS_FONT_HOME ) };
        } else {
            logger.warning("unknown os.name, no fonts will be embedded");
            dirs= new File[] { };
        }
 
        if ( fontToTtfMap==null ) {
            state= STATE_READING;
        
            logger.log( Level.FINE, "indexing fonts..." );
            long t0= System.currentTimeMillis();
            HashMap fontToTtfMap1= new HashMap();
            for ( File dir: dirs ) {
                if ( !dir.exists() ) {
                    continue;
                }
                File[] ttfFonts= FileUtil.listRecursively( dir, "*.ttf" );
                for ( File f: ttfFonts ) {
                    FileInputStream in = null;
                    try {
                        com.itextpdf.text.pdf.BaseFont.createFont( f.toString(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED ); // check to see if iText is going to fuss about licensing.
                        in = new FileInputStream(f);
                        Font font= Font.createFont(Font.TRUETYPE_FONT, in );
                        logger.log( Level.FINEST, "adding {0} -> {1}", new Object[]{font.getFamily(), f});
                        fontToTtfMap1.put( font.getFamily(), f );                    
                    } catch ( DocumentException ex ) {
                        logger.log( Level.SEVERE, null, ex );
                    } catch (FontFormatException ex) {
                        logger.log(Level.SEVERE, null, ex);
                    } catch (IOException ex) {
                        logger.log(Level.SEVERE, null, ex);
                    } finally {
                        try {
                            if ( in!=null ) in.close();
                        } catch (IOException ex) {
                            logger.log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
            fontToTtfMap= fontToTtfMap1;
            state= STATE_READY;
            logger.log( Level.FINE, "{0}fonts indexed in {1} millis", new Object[] { fontToTtfMap.size(), ( System.currentTimeMillis() - t0) } );
        }
        return fontToTtfMap;
    }
    
    /**
     * kludge to support call from AWT.  If the font map is not yet
     * loaded, return READING_FONTS and start the lookup on a new thread.
     * @param font
     * @return READING_FONTS or the name (or null).
     */
    public static String ttfFromNameInteractive( final java.awt.Font font ) {
        synchronized ( state ) {
            if ( state==STATE_READY ) {
                return ttfFromName(font);
            }
            if ( fontToTtfMap==null ) {
                if ( state==STATE_IDLE ) {
                    state= STATE_READING;
                    Runnable run= new Runnable() {
                        public void run() {
                            String x= ttfFromName( font );
                        }
                    };
                    new Thread( run ).start();
                }
                return READING_FONTS;
            } else {
                return ttfFromName(font);
            }
        }
    }
    
    /**
     * return the name of the .ttf file for the platform, or null.
     * @param font
     * @return the name of the .ttf file, or null.
     */
    public static String ttfFromName( java.awt.Font font ) {
        String osName= System.getProperty( "os.name" ); 
        if ( osName.startsWith("Mac") ) {
            Map<String,File> map= getFontToTtfMap();
            File f= map.get(font.getName());
            if ( f==null ) {
                return null;
            } else {
                return f.toString();
            }
        } else if ( osName.startsWith("Linux") ) {
            Map<String,File> map= getFontToTtfMap();
            File f= map.get(font.getFamily());
            if ( f==null ) {
                return null;
            } else {
                return f.toString();
            }
        } else if ( osName.startsWith("Windows") ) {
            Map<String,File> map= getFontToTtfMap();
            File f= map.get(font.getName());
            if ( f==null ) {
                return null;
            } else {
                return f.toString();
            }
        }
        return null;
    }
        
    FontMapper fontMapper = new FontMapper() {
        public BaseFont awtToPdf(java.awt.Font font) {
            try {
                String ffile= ttfFromName(font);
                if ( ffile==null ) {
                    logger.log(Level.WARNING, "couldn''t find ttf font file for {0}", font.getName());
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
    
    public static void main( String[] args ) {
        getFontToTtfMap();
    }
     
}