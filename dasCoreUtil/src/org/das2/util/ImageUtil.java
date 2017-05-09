/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.util;

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
    
    /**
     * return the node containing JSON metadata showing where the plots are in images
     * produced by the Autoplot Das2 library.
     * @param file the png file.
     * @return null or the JSON describing the image.  See http://autoplot.org/developer.richPng
     * @throws IOException 
     */
    public static String getJSONMetadata( File file ) throws IOException {
        
        Logger logger= LoggerManager.getLogger("das2.util");
        
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
}
