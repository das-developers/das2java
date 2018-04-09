/*
 * LabelDragRenderer.java
 *
 * Created on October 5, 2004, 1:25 PM
 */

package org.das2.event;

import java.util.logging.Level;
import org.das2.graph.DasCanvasComponent;
import org.das2.util.GrannyTextRenderer;
import org.das2.system.DasLogger;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.das2.graph.DasCanvas;

/**
 *
 * @author  Jeremy
 */
public class LabelDragRenderer implements DragRenderer {
    
    String label="Label not set";
    GrannyTextRenderer gtr;
    DasCanvasComponent parent;
    InfoLabel infoLabel;
    
    int labelPositionX=1; // 1=right, -1=left
    int labelPositionY=-1; // 1=below, -1=above
    
    /* the implementing class is responsible for setting this */
    Rectangle dirtyBounds;
    
    static final Logger logger= DasLogger.getLogger(DasLogger.GUI_LOG);
    
    int maxLabelWidth;
    
    @Override
    public void clear(Graphics g) {
        if ( dirtyBounds!=null ) parent.paintImmediately(dirtyBounds);
        dirtyBounds= null;
    }
    
    public LabelDragRenderer( DasCanvasComponent parent ) {
        this.parent= parent;
        this.dirtyBounds= new Rectangle();
        gtr= new GrannyTextRenderer();
    }
    
    /**
     * This method is called by the DMIA on mouse release.  We use this to infer the mouse release
     * and hide the Window.  Note this assumes isUpdatingDragSelection is false!
     * TODO: DMIA should call clear so this is more explicit.
     */
    public MouseDragEvent getMouseDragEvent(Object source, java.awt.Point p1, java.awt.Point p2, boolean isModified) {
        maxLabelWidth= 0;
        if ( tooltip ) {
            if ( infoLabel!=null ) infoLabel.hide(parent);
        }
        return null;
    }
    
    @Override
    public boolean isPointSelection() {
        return true;
    }
    
    @Override
    public boolean isUpdatingDragSelection() {
        return false;
    }
    
    public void setLabel( String s ) {
        this.label= s;
    }
    
    private Rectangle paintLabel( Graphics g1, java.awt.Point p2 ) {
        
        if ( label==null ) return null;
        
        Graphics2D g= (Graphics2D)g1;
        g.setClip(null);
        g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        
        if ( parent==null ) return null;
        if ( parent.getCanvas()==null ) {
            return null;
        }
        
        Dimension d= parent.getCanvas().getSize();
        
        gtr.setString( g1, label);
        
        int dx= (int)gtr.getWidth()+6;
        
        int dy= (int)gtr.getHeight();
        
        if (maxLabelWidth<dx) {
            maxLabelWidth=dx;
        }
        
        if ( ( p2.x + maxLabelWidth > d.width ) && (p2.x-3-dx>0) ) {
            labelPositionX= -1;
        } else {
            labelPositionX= 1;
        }
        
        int xp;
        if ( labelPositionX==1 ) {
            xp= p2.x+3;
        } else {
            xp= p2.x-3-dx;
        }
        
        int yp;
        if ( p2.y-3-dy < 13) {
            labelPositionY= -1;
        } else {
            labelPositionY= 1;
        }
        
        if ( labelPositionY==1 ) {
            yp= p2.y-3-dy;
        } else {
            yp= p2.y+3;
        }
        
        dirtyBounds= new Rectangle();
        
        Color color0= g.getColor();
        
        // draw the translucent background
        g.setColor(new Color(255,255,255,200));
        dirtyBounds.setRect(xp,yp,dx,dy);
        g.fill(dirtyBounds);
        
        // draw the label
        g.setColor(new Color(20,20,20));
        gtr.draw( g, xp+3, (float)(yp+gtr.getAscent()) );
        
        g.setColor(color0);
        
        return dirtyBounds;
    }
    
    @Override
    public Rectangle[] renderDrag(Graphics g, Point p1, Point p2) {
        logger.log(Level.FINEST, "renderDrag {0}", p2);
        Rectangle[] result;
        if ( tooltip ) {
            if ( infoLabel==null ) infoLabel= new InfoLabel();
            Point p= (Point)p2.clone();
            DasCanvas c= parent.getCanvas();
            if ( c!=null ) {
                SwingUtilities.convertPointToScreen( p, parent.getCanvas() );
                infoLabel.setText( label, p, parent, labelPositionX, labelPositionY );
                result= new Rectangle[0];
            } else {
                infoLabel.setText( label, p, parent, labelPositionX, labelPositionY );
                result= new Rectangle[0];
            }
        } else {
            if ( label==null ) {
                result= new Rectangle[0];
            } else {
                Rectangle r= paintLabel( g, p2 );
                result= new Rectangle[] { r };
            }
        }        
        return result;
    }
    
    /**
     * added to more conveniently keep track of dirty bounds when subclassing.
     */
    ArrayList newDirtyBounds;
    
    protected void resetDirtyBounds( ) {
        newDirtyBounds= new ArrayList();
    }
    
    protected void addDirtyBounds( Rectangle[] dirty ) {        
        if ( dirty!=null && dirty.length>0 ) newDirtyBounds.addAll( Arrays.asList( dirty ) );
    }
    
    protected void addDirtyBounds( Rectangle dirty ) {
        if ( dirty!=null ) newDirtyBounds.add( dirty );
    }
    
    protected Rectangle[] getDirtyBounds() {
        try {
        return (Rectangle[]) newDirtyBounds.toArray( new Rectangle[newDirtyBounds.size()] );
        } catch ( RuntimeException e ) {
            throw e;
        }
    }
    
 /*   public void keyPressed(KeyEvent e) {
        int keyCode= e.getKeyCode();
  
        if ( keyCode==KeyEvent.VK_LEFT || keyCode==KeyEvent.VK_RIGHT || keyCode==KeyEvent.VK_UP || keyCode==KeyEvent.VK_DOWN ) {
                int x=0;
                int y=0;
                try {
                    int xOff= parent.getLocationOnScreen().x-parent.getX();
                    int yOff= parent.getLocationOnScreen().y-parent.getY();
                    final java.awt.Robot robot= new java.awt.Robot();
                    switch ( keyCode ) {
                        case KeyEvent.VK_LEFT:
                            robot.mouseMove(lastMousePoint.getX()+xOff-1, lastMousePoint.getY()+yOff);
                            break;
                        case KeyEvent.VK_RIGHT:
                            robot.mouseMove(lastMousePoint.getX()+xOff+1, lastMousePoint.getY()+yOff);
                            break;
                        case KeyEvent.VK_UP:
                            robot.mouseMove(lastMousePoint.getX()+xOff, lastMousePoint.getY()+yOff-1);
                            break;
                        case KeyEvent.VK_DOWN:
                            robot.mouseMove(lastMousePoint.getX()+xOff, lastMousePoint.getY()+yOff+1);
                            break;
                    }
                } catch ( java.awt.AWTException e1 ) {
                    logger.log(Level.SEVERE,null,e1);
                }
  
            } else {
  
                DataPointSelectionEvent dpse= getDataPointSelectionEvent(lastMousePoint);
                HashMap planes= new HashMap();
                planes.put( "keyChar", String.valueOf( e.getKeyChar() ) );
                dpse= new DataPointSelectionEvent( this, dpse.getX(), dpse.getY(), planes );
                fireDataPointSelectionListenerDataPointSelected( dpse );
            }
        }
    }*/
    
    boolean tooltip= false;
    public boolean isTooltip() {
        return tooltip;
    }
    
    public void setTooltip( boolean tooltip ) {
        this.tooltip= tooltip;
        if ( tooltip ) {
            labelPositionX= 1;
            labelPositionY= -1;
        }
    }
}
