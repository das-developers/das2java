/*
 * RendererMouseModule.java
 *
 * Created on February 17, 2004, 6:34 PM
 */

package edu.uiowa.physics.pw.das.event;

/**
 *
 * @author  Jeremy
 */
public class RendererMouseModule extends MouseModule {
    
    /** Creates a new instance of RendererMouseModule */
    public RendererMouseModule( DragRenderer d, String label ) {        
        super.setLabel(label);
        super.dragRenderer= d;
    }
    
}
