/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.util.filesystem;

import java.io.IOException;
import java.io.OutputStream;

/**
 * The FileObject can be written.
 * @author jbf
 */
public interface WriteCapability {
    
    /**
     * Get the output stream.  The client who has requested the stream must close the stream.
     * @return
     * @throws IOException
     */
    public OutputStream getOutputStream( ) throws IOException;

    /**
     * Test to see if we can write to this file.  This should not
     * create a file, only getOutputStream should do this.  
     * TODO: someday we'll want a locking mechanism.
     * @return true if the file can be created.
     */
    public boolean canWrite();

    /**
     * delete the file
     * @return true if the file was deleted.
     * @throws IOException
     */
    public boolean delete() throws IOException;
}
