/*
 * PathIterable.java
 *
 * Created on July 3, 2007, 11:02 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.das2.graph;

import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;

/**
 * A PathIterable is an object that can return a PathIterator for the 
 * @author jbf
 */
public interface PathIterable {
    PathIterator pathIterator( AffineTransform at );
}
