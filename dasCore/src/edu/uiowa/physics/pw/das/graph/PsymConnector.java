/* File: PsymConnector.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on October 29, 2003, 10:42 AM by __FULLNAME__ <__EMAIL__>
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

import edu.uiowa.physics.pw.das.*;
import edu.uiowa.physics.pw.das.components.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import javax.swing.*;

/**
 *
 * @author  jbf
 */
public class PsymConnector implements PropertyEditor.Enumeration {
    
    String name;
    Icon imageIcon;
    BasicStroke stroke;
    BasicStroke cacheStroke;
    float cacheWidth;
    Line2D line;
    
    public static final PsymConnector NONE= new PsymConnector( "None", null );
    public static final PsymConnector SOLID= new PsymConnector( "Solid", new BasicStroke( 1.0f ) );
    public static final PsymConnector DOTFINE= new PsymConnector( "DotFine",
    new BasicStroke( 1.0f, BasicStroke.CAP_ROUND,
    BasicStroke.JOIN_ROUND, 1.0f, new float[] {1.5f,2.0f}, 0.f ) );
    public static final PsymConnector DASHFINE= new PsymConnector( "DashFine",
    new BasicStroke( 1.0f, BasicStroke.CAP_ROUND,
    BasicStroke.JOIN_ROUND, 1.0f, new float[] {3.0f,2.0f}, 0.f ) );
    
    public static final PsymConnector PSYM10= new PsymConnector( "Psym10", new BasicStroke( 1.0f ) ) {
        public void drawLine( Graphics2D g, int x1, int y1, int x2, int y2, float width ) {
            g.setStroke( getStroke(width) );
            int xMid= (x1 + x2) / 2;
            line.setLine(x1,y1,xMid,y1);  g.draw(line);
            line.setLine(xMid,y1,xMid,y2);  g.draw(line);
            line.setLine(xMid,y2,x2,y2);  g.draw(line);
        }
    };
    
    
    private PsymConnector( String name, BasicStroke stroke ) {
        line= new Line2D.Double();
        
        this.name= name;
        this.stroke= stroke;
        this.cacheStroke= stroke;
        if ( stroke!=null ) this.cacheWidth= cacheStroke.getLineWidth();
        
        Image i= new BufferedImage(15,10,BufferedImage.TYPE_INT_RGB);
        Graphics2D g= (Graphics2D)i.getGraphics();
        g.setRenderingHints(DasProperties.getRenderingHints());
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(0,0,15,10);
        g.setColor(Color.black);
        drawLine(g,2,3,13,7,2.f);
        this.imageIcon= new ImageIcon(i);
    }
    
    protected Stroke getStroke( float width ) {
        if ( width!=cacheWidth ) {
            float[] dashArray= stroke.getDashArray();
            float[] dashArrayWidth=null;
            if ( dashArray!=null ) {
                dashArrayWidth= new float[dashArray.length];
                for ( int i=0; i<dashArray.length; i++ ) {
                    dashArrayWidth[i]= dashArray[i] * width;
                }
            }
            cacheStroke= new BasicStroke( width, stroke.getEndCap(), stroke.getLineJoin(),
            stroke.getMiterLimit(), dashArrayWidth, stroke.getDashPhase()*width );
            cacheWidth= width;
        }
        return cacheStroke;
    }
    
    public void drawLine(Graphics2D g, int x1, int y1, int x2, int y2, float width ) {
        // jbf: this thing really wants a state, especially if it is going to
        // keep track of the dash phase between calls.  Perhaps this needs a
        // state object to go with it.
        if ( stroke==null ) {
            return;
        } else {
            g.setStroke( getStroke(width) );
            g.drawLine(x1,y1,x2,y2);
        }
    }
    
    public javax.swing.Icon getListIcon() {
        return imageIcon;
    }
    
    public String toString() {
        return name;
    }
    
    
    
}
