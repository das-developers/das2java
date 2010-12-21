/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.util.filesystem;

/**
 * marker interface for URI syntax, motivated by file://home (two slashes)
 * @author jbf
 */
public class URIException extends IllegalArgumentException {
    public URIException( String msg ) {
        super(msg);
    }

}
