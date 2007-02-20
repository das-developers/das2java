/*
 * AnnotatorMouseModule.java
 *
 * Created on March 25, 2005, 1:07 PM
 */

package edu.uiowa.physics.pw.das.event;

import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.graph.*;
import java.awt.Point;
import java.awt.event.MouseEvent;
        
/**
 *
 * @author Jeremy
 */
public class AnnotatorMouseModule extends MouseModule {

    DasCanvas canvas;
    
    /** Creates a new instance of AnnotatorMouseModule */
    public AnnotatorMouseModule( DasCanvasComponent parent ) {
        super( parent, new BoxRenderer(parent), "Annotate" );
        this.canvas= (DasCanvas)parent.getParent();
    }

    public DasCanvas getCanvas() {
        return canvas;
    }

    
    private MouseModule createArrowToMouseModule( final DasAnnotation anno ) {
        return new MouseModule( parent, new ArrowDragRenderer(), "Point At" ) {
            Point head;
            Point tail;
            
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                tail= e.getPoint();
            }

            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                head= e.getPoint();
                head.translate( anno.getX(), anno.getY() );
                DasCanvasComponent c= parent.getCanvas().getCanvasComponentAt( head.x, head.y );
                if ( c instanceof DasPlot ) {
                    final DasPlot p= (DasPlot) c;
                    final Datum x= p.getXAxis().invTransform( head.x );
                    final Datum y= p.getYAxis().invTransform( head.y );
                    anno.setPointAt( new DasAnnotation.PointDescriptor() {
                        public Point getPoint() {
                            int ix= (int)(p.getXAxis().transform(x));
                            int iy= (int)(p.getYAxis().transform(y));
                            return new Point(ix,iy);
                        }
                    } );
                }
            }
        };
    }
    
    public void mouseRangeSelected(MouseDragEvent e) {    
        super.mouseRangeSelected(e);
        
        System.out.println(e);
        MouseBoxEvent me= (MouseBoxEvent) e;
                
        double n;
        n= canvas.getHeight();
        DasRow row= new DasRow( canvas, me.getYMinimum()/n, me.getYMaximum()/n );

        n= canvas.getWidth();        
        DasColumn col= new DasColumn( canvas, me.getXMinimum()/n, me.getXMaximum()/n );
        
        DasAnnotation anno= new DasAnnotation("right click");
        MouseModule mm= createArrowToMouseModule( anno );
        anno.addMouseModule( mm );
        anno.getMouseAdapter().setPrimaryModule( mm );
        
        canvas.add( anno, row, col );                
        canvas.revalidate();
    }
    
    
    
}
