/* File: DasPNGEncoder.java
 * Copyright (C) 2002-2003 The University of Iowa
 * Created by: Jeremy Faden <jbf@space.physics.uiowa.edu>
 *             Jessica Swanner <jessica@space.physics.uiowa.edu>
 *             Edward E. West <eew@space.physics.uiowa.edu>
 *
 * This file is part of the das2 library.
 *
 * das2 is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.das2.util;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

/**
 *
 * @author  eew
 */
public class DasPNGEncoder extends DasPNGConstants {
    
    
    /** Creates a new instance of DasPNGEncoder */
    public DasPNGEncoder() {
    }
    
    /** Adds a tEXT chunk with the specified keyword and content.
     * @param keyword the specified keyword
     * @param content the content for the tEXT chunk
     */
    public void addText(String keyword, String content) {
        List list = (List)textMap.get(keyword);
        if (list == null) {
            list = new ArrayList();
            textMap.put(keyword, list);
        }
        list.add(content);
    }
    
    /** Removes the tEXT chunk with the specifed keyword and content.
     * @param keyword the specified keyword
     * @param content the specified content to be removed
     */
    public void removeText(String keyword, String content) {
        List list = (List)textMap.get(keyword);
        if (list != null) {
            list.remove(content);
        }
    }
    
    /** Removes all tEXT chunk with the specified keyword
     * @param keyword the specified keyword.
     */
    public void removeAllText(String keyword) {
        textMap.remove(keyword);
    }
    
    public void setGamma(int gamma) {
        this.gamma = gamma;
    }
    
    public void write(BufferedImage image, OutputStream out) throws IOException {
        LinkedList chunkList = new LinkedList();
        int totalSize = 0;
        chunkList.add(getHeaderBytes());
        chunkList.add(getIHDRBytes(image));
        gettEXtBytes(chunkList);
        chunkList.add(getgAMABytes());
        chunkList.add(getPLTEBytes(image));
        chunkList.add(getIDATBytes(image));
        chunkList.add(getIENDBytes());
        Iterator iterator = chunkList.iterator();
        while (iterator.hasNext()) {
            totalSize += ((byte[])iterator.next()).length;
        }
        ByteBuffer buffer = ByteBuffer.allocate(totalSize);
        iterator = chunkList.iterator();
        while (iterator.hasNext()) {
            buffer.put((byte[])iterator.next());
        }
        out.write(buffer.array());
    }
    
    private byte[] getHeaderBytes() {
        return new byte[] {
            (byte)137, (byte)80, (byte)78, (byte)71, 
            (byte)13, (byte)10, (byte)26, (byte)10
        };
    }
    
    /**
     * Width:              4 bytes
     * Height:             4 bytes
     * Bit depth:          1 byte (allowed values: 1, 2, 4, 8, 16)
     * Color type:         1 byte
           Color    Allowed    Interpretation
           Type    Bit Depths
           0       1,2,4,8,16  Each pixel is a grayscale sample.
           2       8,16        Each pixel is an R,G,B triple.
           3       1,2,4,8     Each pixel is a palette index;
                               a PLTE chunk must appear.
           4       8,16        Each pixel is a grayscale sample,
                               followed by an alpha sample.
           6       8,16        Each pixel is an R,G,B triple,
                               followed by an alpha sample.
     * Compression method: 1 byte (must be 0)
     * Filter method:      1 byte (must be 0)
     * Interlace method:   1 byte (interlacing not supported, will be 0)
     */
    private byte[] getIHDRBytes(BufferedImage image) {
        byte bitDepth;
        byte colorType;
        int imageType = image.getType();
        switch (imageType) {
            //24 bit image types
            case BufferedImage.TYPE_3BYTE_BGR:
            case BufferedImage.TYPE_INT_BGR:
            case BufferedImage.TYPE_INT_RGB:
            case BufferedImage.TYPE_USHORT_555_RGB:
            case BufferedImage.TYPE_USHORT_565_RGB:
                bitDepth = 8;
                colorType = 2;
                break;
                
            //32 bit alpha
            case BufferedImage.TYPE_4BYTE_ABGR:
            case BufferedImage.TYPE_INT_ARGB:
                bitDepth = 8;
                colorType = 6;
                break;
                
            case BufferedImage.TYPE_BYTE_INDEXED:
                bitDepth = 8;
                colorType = 3;
                break;
                
            case BufferedImage.TYPE_BYTE_GRAY:
                bitDepth = 8;
                colorType = 0;
                break;
                
            case BufferedImage.TYPE_USHORT_GRAY:
                bitDepth = 16;
                colorType = 0;
                break;
                
            //Currently unsupported types
            case BufferedImage.TYPE_BYTE_BINARY:
            case BufferedImage.TYPE_4BYTE_ABGR_PRE:
            case BufferedImage.TYPE_INT_ARGB_PRE:
            case BufferedImage.TYPE_CUSTOM:
            default:
                throw new RuntimeException("Unsupported image type");
        }
        byte compressionMethod = 0;
        byte filterMethod = 0;
        byte interlaceMethod = 0;
        
        byte[] array = new byte[25];
        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.putInt(13);
        buffer.put(getISO8859_1Bytes(CHUNK_TYPE_IHDR));
        buffer.putInt(image.getWidth());
        buffer.putInt(image.getHeight());
        buffer.put(bitDepth);
        buffer.put(colorType);
        buffer.put(compressionMethod);
        buffer.put(filterMethod);
        buffer.put(interlaceMethod);
        CRC32 crc = new CRC32();
        crc.update(array, 4, 17);
        buffer.putInt((int)crc.getValue());
        return array;
    }
    
    private byte[] getgAMABytes() {
        byte[] array = new byte[16];
        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.putInt(4);
        buffer.put(getISO8859_1Bytes(CHUNK_TYPE_gAMA));
        buffer.putInt(gamma);
        CRC32 crc = new CRC32();
        crc.update(array, 4, 8);
        buffer.putInt((int)crc.getValue());
        return array;
    }
    
    private byte[] getPLTEBytes(BufferedImage image) {
        if (image.getType() != BufferedImage.TYPE_BYTE_INDEXED) {
            return new byte[0];
        }
        IndexColorModel cm = (IndexColorModel)image.getColorModel();
        int colorCount = cm.getMapSize();
        throw new UnsupportedOperationException();
    }
    
    private byte[] getIDATBytes(BufferedImage image) {
        byte[] imageData;
        int imageType = image.getType();
        switch (imageType) {
            case BufferedImage.TYPE_3BYTE_BGR:
            case BufferedImage.TYPE_INT_BGR:
            case BufferedImage.TYPE_INT_RGB:
            case BufferedImage.TYPE_USHORT_555_RGB:
            case BufferedImage.TYPE_USHORT_565_RGB:
                imageData = getRGBBytes(image);
                break;
                
            //32 bit alpha
            case BufferedImage.TYPE_4BYTE_ABGR:
                imageData = getABGRBytes(image);
                break;
                
            case BufferedImage.TYPE_INT_ARGB:
                imageData = getARGBBytes(image);
                break;
                
            case BufferedImage.TYPE_BYTE_INDEXED:
            case BufferedImage.TYPE_BYTE_GRAY:
                imageData = get8BitSampleBytes(image);
                break;
                
            case BufferedImage.TYPE_USHORT_GRAY:
                imageData = get16BitSampleBytes(image);
                break;
                
            //Currently unsupported types
            case BufferedImage.TYPE_BYTE_BINARY:
            case BufferedImage.TYPE_4BYTE_ABGR_PRE:
            case BufferedImage.TYPE_INT_ARGB_PRE:
            case BufferedImage.TYPE_CUSTOM:
            default:
                throw new RuntimeException("Unsupported image type");
        }
        
        byte[] compressedImageData = new byte[imageData.length];
        Deflater deflater = new Deflater();
        deflater.setInput(imageData);
        deflater.finish();
        int compressedSize = deflater.deflate(compressedImageData);
        
        byte[] array = new byte[compressedSize + 12];
        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.putInt(compressedSize);
        buffer.put(getISO8859_1Bytes(CHUNK_TYPE_IDAT));
        buffer.put(compressedImageData, 0, compressedSize);
        CRC32 crc = new CRC32();
        crc.update(array, 4, compressedSize + 4);
        buffer.putInt((int)crc.getValue());
        return array;
    }
    
    private byte[] getRGBBytes(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] intPixels = new int[width * height];
        image.getRGB(0, 0, width, height, intPixels, 0, width);
        byte[] bytePixels = new byte[intPixels.length * 3 + height];
        
        for (int line = 0; line < height; line++) {
            int offset = (width * 3 + 1) * line;
            bytePixels[offset] = (byte)0;
            for (int pixel = 0; pixel < width; pixel++) {
                int intIndex = line * width + pixel;
                int byteIndex = offset + (pixel * 3 + 1);
                bytePixels[byteIndex] = (byte)((0xFF0000 & intPixels[intIndex]) >> 16);
                bytePixels[byteIndex + 1] = (byte)((0x00FF00 & intPixels[intIndex]) >> 8);
                bytePixels[byteIndex + 2] = (byte)(0x0000FF & intPixels[intIndex]);
            }
        }
        return bytePixels;
    }
    
    private byte[] getARGBBytes(BufferedImage image) {
        throw new UnsupportedOperationException();
    }
    
    private byte[] getABGRBytes(BufferedImage image) {
        throw new UnsupportedOperationException();
    }
    
    private byte[] get8BitSampleBytes(BufferedImage image) {
        throw new UnsupportedOperationException();
    }
    
    private byte[] get16BitSampleBytes(BufferedImage image) {
        throw new UnsupportedOperationException();
    }
    
    private byte[] getIENDBytes() {
        byte[] array = new byte[12];
        byte[] typeBytes = getISO8859_1Bytes(CHUNK_TYPE_IEND);
        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.putInt(0);
        buffer.put(typeBytes);
        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        buffer.putInt((int)crc.getValue());
        return array;
    }
    
    private void gettEXtBytes(List list) {
        Iterator entries = textMap.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry)entries.next();
            List contentList = (List)entry.getValue();
            Iterator content = contentList.iterator();
            while (content.hasNext()) {
                list.add(gettEXtBytes((String)entry.getKey(), (String)content.next()));
            }
        }
    }
    
    private byte[] gettEXtBytes(String keyword, String content) {
        byte[] keywordBytes = getISO8859_1Bytes(keyword);
        byte[] contentBytes = getISO8859_1Bytes(content);
        byte[] array = new byte[keywordBytes.length + contentBytes.length + 13];
        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.putInt(keywordBytes.length + contentBytes.length + 1);
        buffer.put(getISO8859_1Bytes(CHUNK_TYPE_tEXT));
        buffer.put(keywordBytes);
        buffer.put((byte)0);
        buffer.put(contentBytes);
        CRC32 crc = new CRC32();
        crc.update(array, 4, keywordBytes.length + contentBytes.length + 5);
        buffer.putInt((int)crc.getValue());
        return array;
    }
    
}
