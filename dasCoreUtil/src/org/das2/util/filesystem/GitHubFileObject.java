/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.util.filesystem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.das2.util.filesystem.WebFileObject.logger;

/**
 *
 * @author jbf
 */
public class GitHubFileObject extends WebFileObject {
    
    
    public GitHubFileObject(WebFileSystem wfs, String pathname, Date modifiedDate) {
        super(wfs, pathname, modifiedDate);
    }

    @Override
    public <T> T getCapability(Class<T> clazz) {
        boolean enable=true;
        if ( enable && clazz==WriteCapability.class ) {
            File localClone= wfs.getReadOnlyCache();
            if ( localClone!=null ) {
                return (T) new WriteCapability() {
                    @Override
                    public OutputStream getOutputStream() throws IOException {
                        File f= Paths.get( localClone.getAbsolutePath(), getNameExt() ).toFile();
                        return new FileOutputStream(f);
                    }

                    @Override
                    public boolean canWrite() {
                        File f= Paths.get( localClone.getAbsolutePath(), getNameExt() ).toFile();
                        if ( f.exists() ) {
                            return f.canWrite();
                        } else {
                            return f.getParentFile().canWrite();
                        }
                        
                    }

                    @Override
                    public boolean delete() throws IOException {
                        File f= Paths.get( localClone.getAbsolutePath(), getNameExt() ).toFile();
                        if ( f.exists() ) {
                            return f.delete();
                        } else {
                            return false;
                        }
                    }

                    @Override
                    public boolean commit(String message) throws IOException {
                        return true;
                    }               
                    
                };
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
    
    
    
}
