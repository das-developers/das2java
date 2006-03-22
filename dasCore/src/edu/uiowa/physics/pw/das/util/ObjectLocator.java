/*
 * ObjectLocator.java
 *
 * Created on February 2, 2006, 4:01 PM
 *
 *
 */

package edu.uiowa.physics.pw.das.util;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;

/**
 * Keeps track of shapes and Objects located at each point, and can quickly find the closest.
 * consider Use spheres of influence.  Brute force implementation presently
 */
public class ObjectLocator {
    List shapes= new ArrayList();
    List objects= new ArrayList();
    public void addObject( Shape bounds, Object object ) {
        Shape clone= (Shape)new GeneralPath(bounds);
        shapes.add( clone );
        objects.add( object );
    }
    public void removeObject( Object object ) {
        int i= objects.indexOf(object);
        objects.remove(i);
        shapes.remove(i);
    }
    
    public Object closestObject( Point p ) {        
        int i;
        for ( i=shapes.size()-1; i>=0; i-- ) {
            Shape s= (Shape)shapes.get(i);
            if ( s.contains( p ) ) {
                break;
            }
        }
        if ( i==-1 ) {
            return null;
        } else {
            return objects.get(i);
        }
    }
    
    public Shape closestShape( Point p ) {
        int i;
        for ( i=shapes.size()-1; i>=0; i-- ) {
            Shape s= (Shape)shapes.get(i);
            if ( s.contains( p ) ) {
                break;
            }
        }        
        if ( i==-1 ) {
            return null;
        } else {
            return (Shape)shapes.get(i);
        }
    }
    
    public Object getObject( Shape shape ) {
        int i= shapes.indexOf( shape );
        return objects.get(i);
    }
}