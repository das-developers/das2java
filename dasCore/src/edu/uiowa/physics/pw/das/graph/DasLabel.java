/* File: DasLabel.java
 * Copyright (C) 2002-2003 The University of Iowa
 * Created by: Jeremy Faden <jbf@space.physics.uiowa.edu>
 *             Jessica Swanner <jessica@space.physics.uiowa.edu>
 *             Edward E. West <eew@space.physics.uiowa.edu>
 *
 * This file is part of the das2 library.
 *
 * das2 is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.uiowa.physics.pw.das.graph;

import java.awt.*;
import java.awt.geom.AffineTransform;
/**
 *
 * @author  jbf
 */
public class DasLabel extends DasCanvasComponent {

    String text="";
    int x=0;
    int y=0;
    double angle=0;
    
    Font font= null;
    AffineTransform at= null;
    
    Rectangle bounds= null;
    
    final int RIGHT=99;
    final int LEFT=98;
    final int CENTER=97;
    
    int justification= LEFT;
    
    Component parent=null;
    
    /** Creates a new instance of DasLabel */
    public DasLabel() {
        at= new AffineTransform();
    }
    
    public DasLabel(String text, int x, int y) {
        this();
        this.text= text;
        this.x= x;
        this.y= y;
        setTransform();
    }
    
    public void draw(Graphics2D g) {
        Font oldfont=null;
        AffineTransform oldat=null;
        if (font!=null) {
            oldfont= g.getFont();
            g.setFont(font);
        }
        if (at!=null) {
            oldat= g.getTransform();
        }
                        
        g.setTransform(at);
        
        g.drawString(text,(float)x,(float)y);
        
        FontMetrics fm= g.getFontMetrics();
        
        bounds= fm.getStringBounds(text,g).getBounds();
        bounds.translate(x,y);
        edu.uiowa.physics.pw.das.util.DasDie.println(bounds);
        
        if (font!=null) g.setFont(oldfont);
        if (at!=null) g.setTransform(oldat);
    }
    
    private void setTransform() {
        at.setToIdentity();
        at.translate(x,y);
        at.rotate(angle*Math.PI/180);        
    }
    
    public void setRotation(double angle) {
        this.angle= angle;
        setTransform();
    }
    
    public void setPosition( int x, int y) {
        this.x= x;
        this.y= y;
        setTransform();
    }
    
    public Rectangle getBounds() {
        return bounds;
    }
    
    
    public void setParent(DasCanvas parent) {
        this.parent= parent;
    }
    

}
