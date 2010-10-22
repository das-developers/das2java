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
    public OutputStream getOutputStream( ) throws IOException;
    public boolean canWrite() throws IOException;
    public boolean delete() throws IOException;
}
