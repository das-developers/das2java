/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.util.filesystem;

import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author jbf
 */
public interface WriteCapability {
    /**
     * Get the output stream.
     * @return
     * @throws IOException
     */
    public OutputStream getOutputStream( ) throws IOException;

    /**
     * Test to see if we can write to this file.
     * @return
     * @throws IOException
     */
    public boolean canWrite() throws IOException;

    /**
     * delete the file
     * @return
     * @throws IOException
     */
    public boolean delete() throws IOException;
}
