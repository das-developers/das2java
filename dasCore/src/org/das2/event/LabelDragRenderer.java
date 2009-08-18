/*
 * LabelDragRenderer.java
 *
 * Created on October 5, 2004, 1:25 PM
 */

package org.das2.event;

import org.das2.graph.DasCanvasComponent;
import org.das2.util.GrannyTextRenderer;
import org.das2.system.DasLogger;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

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
    
    Logger logger= DasLogger.getLogger(DasLogger.GUI_LOG);
    
    int maxLabelWidth;
    
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
            if ( infoLabel!=null ) infoLabel.hide();
        }
        return null;
    }
    
    public boolean isPointSelection() {
        return true;
    }
    
    public boolean isUpdatingDragSelection() {
        return false;
    }
    
    public void setLabel( String s ) {
        this.label= s;
    }
    
    private class InfoLabel {
        JWindow window;
        JPanel label;
        GrannyTextRenderer gtr;
        
        JPanel containedPanel;
        JComponent glassPane;
        
        boolean contained= true;
        
        void init() {
            Window root= (Window)SwingUtilities.getRoot( parent );
            window= new JWindow( root );
            label= new JPanel() {
                public void paintComponent( Graphics g ) {
                    g.clearRect(0,0, getWidth(), getHeight() );
                    gtr.draw( g, 0, (int)gtr.getAscent() );
                }
            };
            label.setOpaque( true );
            label.setPreferredSize( new Dimension( 300,20 ) );
            window.getContentPane().add( label );
            window.pack();
            gtr= new GrannyTextRenderer();
            
            glassPane= (JComponent)parent.getCanvas().getGlassPane();
            containedPanel= new JPanel() {
                public void paintComponent( Graphics g ) {
                    g.clearRect(0,0, getWidth(), getHeight() );
                    gtr.draw( g, 0, (int)gtr.getAscent() );
                }
            };
            containedPanel.setVisible(false);
            glassPane.add(containedPanel);
            contained= true;
            
        }
        
        void setText( String text, Point p ) {
            if ( window==null ) init();
            if ( text!=null ) {
                gtr.setString( containedPanel.getFont(), text );
                Rectangle rect= gtr.getBounds();

                int posx= p.x + labelPositionX * 3 + Math.min( labelPositionX, 0 ) * rect.width;
                int posy= p.y + labelPositionY * 3 + Math.min( labelPositionY, 0 ) * rect.height;
                
                Rectangle bounds= gtr.getBounds();
                
                Point p2= new Point( posx, posy );
                SwingUtilities.convertPointFromScreen( p2, glassPane );
                    
                bounds.translate( p2.x, p2.y );
                
                contained= glassPane.getBounds().contains( bounds );
                
                if ( contained ) {
                    
                    containedPanel.setSize( new Dimension( rect.width, rect.height ) );
                    containedPanel.setLocation( p2.x, p2.y );
                    window.setVisible(false);
                    containedPanel.setVisible(true);
                    containedPanel.repaint();
                    
                } else {
                    
                    gtr.setString(label.getFont(),text);
                    rect= gtr.getBounds();
                    window.setSize( new Dimension( rect.width, rect.height ) );
                    
                    posx= p.x + labelPositionX * 3 + Math.min( labelPositionX, 0 ) * rect.width;
                    posy= p.y + labelPositionY * 3 + Math.min( labelPositionY, 0 ) * rect.height;
                    
                    containedPanel.setVisible(false);
                    window.setLocation( posx, posy );
                    window.setVisible(true);
                    window.repaint();
                }
            } else {
                hide();
            }
        }
        
        void hide() {
            if ( window==null ) init();
            if ( contained ) {
                containedPanel.setVisible(false);
            } else {
                window.setVisible(false);
            }
        }
        
    }
    
    private Rectangle paintLabel( Graphics g1, java.awt.Point p2 ) {
        
        if ( label==null ) return null;
        
        Graphics2D g= (Graphics2D)g1;
        g.setClip(null);
        g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        
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
        
        dirtyBounds= new Rectangle();;
        
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
    
    public Rectangle[] renderDrag(Graphics g, Point p1, Point p2) {
        logger.finest("renderDrag "+p2);
        Rectangle[] result;
        if ( tooltip ) {
            if ( infoLabel==null ) infoLabel= new InfoLabel();
            Point p= (Point)p2.clone();
            SwingUtilities.convertPointToScreen( p, parent.getCanvas() );
            infoLabel.setText( label, p );
            result= new Rectangle[0];
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
                    org.das2.util.DasDie.println(e1.getMessage());
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
