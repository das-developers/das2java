
package org.das2.util.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.das2.util.LoggerManager;

/**
 * class taken from http://www.journaldev.com/957/java-zip-file-folder-example,
 * thanks Pankaj.
 * @author jbf
 */
public class ZipFiles {
    
    private static final Logger logger= LoggerManager.getLogger("das2.filesystem");
            
    private final List<String> filesListInDir = new ArrayList<>();

    public static void main(String[] args) {
        File file = new File("/Users/pankaj/sitemap.xml");
        String zipFileName = "/Users/pankaj/sitemap.zip";
        
        File dir = new File("/Users/pankaj/tmp");
        String zipDirName = "/Users/pankaj/tmp.zip";
        
        zipSingleFile(file, zipFileName);
        
        ZipFiles zipFiles = new ZipFiles();
        zipFiles.zipDirectory(dir, zipDirName);
    }

    /**
     * This method zips the directory
     * @param dir
     * @param zipDirName
     */
    public void zipDirectory(File dir, String zipDirName) {
        try {
            populateFilesList(dir); 
            try ( FileOutputStream fos = new FileOutputStream(zipDirName) ; ZipOutputStream zos = new ZipOutputStream(fos)) {
                for(String filePath : filesListInDir){
                    logger.log(Level.FINE, "Zipping {0}", filePath);
                    //for ZipEntry we need to keep only relative file path, so we used substring on absolute path
                    ZipEntry ze = new ZipEntry(filePath.substring(dir.getAbsolutePath().length()+1, filePath.length()));
                    zos.putNextEntry(ze);
                    try ( FileInputStream fis = new FileInputStream(filePath)) {//read the file and write to ZipOutputStream
                        byte[] buffer = new byte[8*1024];
                        int len;
                        while ((len = fis.read(buffer)) > 0) {
                            zos.write(buffer, 0, len);
                        }
                        zos.closeEntry();
                    }
                }
            }
        } catch (IOException e) {
            logger.log( Level.WARNING, e.getMessage(), e );
        }
    }
    
    /**
     * This method populates all the files in a directory to a List
     * @param dir
     * @throws IOException
     */
    private void populateFilesList(File dir) throws IOException {
        File[] files = dir.listFiles();
        if ( files==null ) return;
        for(File file : files){
            if(file.isFile()) filesListInDir.add(file.getAbsolutePath());
            else populateFilesList(file);
        }
    }

    /**
     * This method compresses the single file to zip format
     * @param file
     * @param zipFileName
     */
    private static void zipSingleFile(File file, String zipFileName) {
        try {//create ZipOutputStream to write to the zip file
            try ( FileOutputStream fos = new FileOutputStream(zipFileName); ZipOutputStream zos = new ZipOutputStream(fos) ) {
                //add a new Zip Entry to the ZipOutputStream
                ZipEntry ze = new ZipEntry(file.getName());
                zos.putNextEntry(ze);
                try ( FileInputStream fis = new FileInputStream(file) ) {//read the file and write to ZipOutputStream
                    byte[] buffer = new byte[8*1024];
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }   //Close the zip entry to write to zip file
                    zos.closeEntry();
                    //Close resources
                }
            }
            logger.log(Level.FINE, "{0} is zipped to {1}", new Object[]{file.getCanonicalPath(), zipFileName});
            
        } catch (IOException e) {
            logger.log( Level.WARNING, e.getMessage(), e );
        }

    }

}