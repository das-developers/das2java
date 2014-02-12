/*
 * AnnotatorMouseModule.java
 *
 * Created on March 25, 2005, 1:07 PM
 */

package org.das2.event;

import org.das2.graph.DasCanvasComponent;
import org.das2.graph.DasColumn;
import org.das2.graph.DasAnnotation;
import org.das2.graph.DasCanvas;
import org.das2.graph.DasRow;
        
/**
 * Draw a box and add an annotation.
 * @author Jeremy
 */
public class AnnotatorMouseModule extends MouseModule {

    DasCanvas canvas;
    
    /** Creates a new instance of AnnotatorMouseModule
     * @param parent typically a plot.
     */
    public AnnotatorMouseModule( DasCanvasComponent parent ) {
        super( parent, new BoxRenderer(parent), "Annotate" );
        this.canvas= (DasCanvas)parent.getParent();
    }

    public DasCanvas getCanvas() {
        return canvas;
    }
    
    @Override
    public void mouseRangeSelected(MouseDragEvent e) {    
        super.mouseRangeSelected(e);
        
        System.out.println(e);
        if ( !( e instanceof MouseBoxEvent ) ) {
            logger.warning("Expected MouseBoxEvent");
            return;
        }
        MouseBoxEvent me= (MouseBoxEvent) e;
                
        double n;
        n= canvas.getHeight();
        DasRow row= new DasRow( canvas, me.getYMinimum()/n, me.getYMaximum()/n );

        n= canvas.getWidth();        
        DasColumn col= new DasColumn( canvas, me.getXMinimum()/n, me.getXMaximum()/n );
        
        DasAnnotation anno= new DasAnnotation("right click");
        
        canvas.add( anno, row, col );                
        canvas.revalidate();
    }
    
    
    
}
