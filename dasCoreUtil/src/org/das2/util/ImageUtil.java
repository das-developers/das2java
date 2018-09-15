
package org.das2.util;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Utilities for image files and images.
 * @author jbf
 */
public class ImageUtil {
    
    private static final Logger logger= LoggerManager.getLogger("das2.util");
    
    /**
     * return the node containing JSON metadata showing where the plots are in images
     * produced by the Autoplot Das2 library.
     * @param file the png file.
     * @return null or the JSON describing the image.  See http://autoplot.org/developer.richPng
     * @throws IOException 
     */
    public static String getJSONMetadata( File file ) throws IOException {
                
        try (ImageInputStream iis = ImageIO.createImageInputStream(file)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);

            if (readers.hasNext()) {

                // pick the first available ImageReader
                ImageReader reader = readers.next();

                // attach source to the reader
                reader.setInput(iis, true);

                // read metadata of first image
                IIOMetadata metadata = reader.getImageMetadata(0);
                try {
                    IIOMetadataNode n= (IIOMetadataNode)metadata.getAsTree("javax_imageio_png_1.0");
                    NodeList nl= n.getElementsByTagName("tEXtEntry");
                    for ( int i=0; i<nl.getLength(); i++ ) {
                        Element e= (Element)nl.item(i);
                        String n3= e.getAttribute("keyword");
                        if ( n3.equals("plotInfo") ) {
                            return e.getAttribute("value");
                        }
                    }
                } catch ( IllegalArgumentException ex ) {
                    logger.log( Level.FINE, ex.getMessage() );
                    return null;
                }
            }
        } catch ( IllegalArgumentException ex ) {
            // return null below
            
        } 
        
        return null;
    }    
    
    
    /**
     * convenient typical use.
     * @param img image to resize.
     * @param thumbSize corner-to-corner size, preserving aspect ratio.
     * @return buffered image that is thumbSize across.
     */
    public static BufferedImage getScaledInstance( BufferedImage img, int thumbSize ) {
        int w0= img.getWidth();
        int h0= img.getHeight();
        int thumbH, thumbW;

        double aspect = 1. * w0 / h0;
        thumbH = (int) (Math.sqrt(Math.pow(thumbSize, 2) / (aspect * aspect + 1.)));
        thumbW = (int) (thumbH * aspect);

        return getScaledInstance( img, thumbW, thumbH, RenderingHints.VALUE_INTERPOLATION_BILINEAR, true );
    }

    /**
     * Convenience method that returns a scaled instance of the
     * provided {@code BufferedImage}.
     *
     * @param img the original image to be scaled
     * @param targetWidth the desired width of the scaled instance,
     *    in pixels
     * @param targetHeight the desired height of the scaled instance,
     *    in pixels
     * @param hint one of the rendering hints that corresponds to
     *    {@code RenderingHints.KEY_INTERPOLATION} (e.g.
     *    {@code RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR},
     *    {@code RenderingHints.VALUE_INTERPOLATION_BILINEAR},
     *    {@code RenderingHints.VALUE_INTERPOLATION_BICUBIC})
     * @param higherQuality if true, this method will use a multi-step
     *    scaling technique that provides higher quality than the usual
     *    one-step technique (only useful in downscaling cases, where
     *    {@code targetWidth} or {@code targetHeight} is
     *    smaller than the original dimensions, and generally only when
     *    the {@code BILINEAR} hint is specified)
     * @return a scaled version of the original {@code BufferedImage}
     */
    public static BufferedImage getScaledInstance(BufferedImage img,
                                           int targetWidth,
                                           int targetHeight,
                                           Object hint,
                                           boolean higherQuality)
    {
        int type = (img.getTransparency() == Transparency.OPAQUE) ?
            BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage ret = (BufferedImage)img;
        int w, h;
        if (higherQuality) {
            // Use multi-step technique: start with original size, then
            // scale down in multiple passes with drawImage()
            // until the target size is reached
            w = img.getWidth();
            h = img.getHeight();
        } else {
            // Use one-step technique: scale directly from original
            // size to target size with a single drawImage() call
            w = targetWidth;
            h = targetHeight;
        }

        int count= 0;
        do {
            if (higherQuality && w > targetWidth) {
                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }
            }

            if (higherQuality && h > targetHeight) {
                h /= 2;
                if (h < targetHeight) {
                    h = targetHeight;
                }
            }

            BufferedImage tmp = new BufferedImage(w, h, type);
            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint );
            g2.drawImage(ret, 0, 0, w, h, null);
            g2.dispose();

            ret = tmp;
            count++; // I noticed a case where it hung in this loop.
            
        } while ( count<50 && ( w != targetWidth || h != targetHeight) );

        if ( count==50 ) {
            logger.log( Level.WARNING, "ran out of iterations in imageResize" );
        }

        return ret;
    }

    
}
