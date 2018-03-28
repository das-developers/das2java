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


package org.das2.graph;

import org.das2.components.propertyeditor.Displayable;
import org.das2.components.propertyeditor.Enumeration;
import org.das2.DasProperties;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import javax.swing.*;

/**
 * Enumeration of symbol connecting lines, such as None, Solid, DotFine.
 * @author  jbf
 */
public class PsymConnector implements Enumeration, Displayable {
    
    String name;
    Icon imageIcon;
    BasicStroke stroke;
    BasicStroke cacheStroke;
    float cacheWidth;
    Line2D line;
    
    public static final PsymConnector NONE= new PsymConnector( "None", null );
    public static final PsymConnector SOLID= new PsymConnector( "Solid", new BasicStroke( 1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND ) );
    public static final PsymConnector DOTFINE= new PsymConnector( "DotFine",new BasicStroke( 1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[] {1.5f,2.0f}, 0.f ) );
    public static final PsymConnector DASHFINE= new PsymConnector( "DashFine",new BasicStroke( 1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[] {3.0f,2.0f}, 2.5f ) );
    public static final PsymConnector DASHES= new PsymConnector( "Dashes",new BasicStroke( 1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[] {6.0f,4.0f}, 5.0f ) );
    public static final PsymConnector DOTDASHES= new PsymConnector( "DotDashes",new BasicStroke( 1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[] {0.0f,3.0f,6.0f,3.0f}, 4.5f ) );
    public static final PsymConnector DOTS= new PsymConnector( "Dots",new BasicStroke( 1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[] {0.0f,3.0f}, 0.f ) );
    
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

    public void draw(Graphics2D g, GeneralPath path, float width) {
        if (stroke != null ) {
            Stroke s = g.getStroke();
            g.setStroke(getStroke(width));
            g.draw(path);
            g.setStroke(s);
        }
    }

    /**
     * See draw( Graphics2D, GeneralPath, float ) for drawing traces.  This method
     * is still supported for drawing icons, etc.
     * @param g
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @param width
     */
    public final void drawLine(Graphics2D g, double x1, double y1, double x2, double y2, float width ) {
        if ( stroke!=null ) {
            Line2D _line;
            if (!SwingUtilities.isEventDispatchThread()) {
                //For thread-safeness
                _line = new Line2D.Double();
            }
            else {
                //We know there is only one dispatch thread, so just reuse line member.
                _line = line;
            }
            _line.setLine(x1, y1, x2, y2);
            g.setStroke( getStroke(width) );
            g.draw(_line);
        }
    }
    
    @Override
    public javax.swing.Icon getListIcon() {
        return imageIcon;
    }

    @Override
    public void drawListIcon( Graphics2D g, int x, int y ) {
        ImageIcon i= (ImageIcon) getListIcon();
        g.drawImage(i.getImage(), x, y, null);
    }
    
    @Override
    public String getListLabel() {
        return name;
    }
              
    @Override
    public String toString() {
        return name;
    }
    
}
