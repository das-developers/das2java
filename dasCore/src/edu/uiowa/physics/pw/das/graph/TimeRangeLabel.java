/* File: TimeRangeLabel.java
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

import edu.uiowa.physics.pw.das.DasProperties;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.datum.format.*;
import edu.uiowa.physics.pw.das.event.MouseModule;
import edu.uiowa.physics.pw.das.util.DasMath;
import edu.uiowa.physics.pw.das.util.DasExceptionHandler;

import java.awt.*;
import java.awt.geom.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 *
 * @author  jbf
 */
public class TimeRangeLabel extends DasCanvasComponent {
    
    DataRange dataRange;
    DatumFormatter df;
    
    /** Creates a new instance of TimeRangeLabel */
    public TimeRangeLabel(DataRange dataRange, DasRow row, DasColumn column ) {
        //setRow(new AttachedRow(row,-0.05,-0.00));
        setRow(row);
        setColumn(column);
        this.dataRange= dataRange;
        
        MouseModule mm= new mouseModule();
        mouseAdapter.addMouseModule(mm);
        mouseAdapter.setPrimaryModule(mm); // THIS SHOULD BE AUTOMATIC!!!
        try {
            df = TimeDatumFormatterFactory.getInstance().newFormatter(
                "yyyy'-'MM'-'dd' ('DDD') 'HH':'mm':'ss.sss");
        }
        catch (java.text.ParseException pe) {
            df = TimeDatumFormatterFactory.getInstance().defaultFormatter();
        }
    }
    
    private class mouseModule extends MouseModule {
    }
    
    public void paintComponent(Graphics graphics) {
        Graphics2D g= (Graphics2D) graphics;
        g.setRenderingHints(DasProperties.getRenderingHints());
        
        Datum min= Datum.create(dataRange.getMinimum(),dataRange.getUnits());
        Datum max= Datum.create(dataRange.getMaximum(),dataRange.getUnits());
        
        FontMetrics fm= g.getFontMetrics();
        
        int y = getRow().getDMinimum();
        int x = getColumn().getDMinimum();
        
        g.translate(-getX(),-getY());
        
        g.drawString(df.format(min), x, y-fm.getHeight()/2);
        
        String label= df.format(max);
        g.drawString(label,
           ( x + getColumn().getWidth() - fm.stringWidth(label)),
           y-fm.getHeight()/2);
        
    }
    
    public void resize() {
        Rectangle bounds= DasDevicePosition.toRectangle(getRow(),getColumn());
        this.setBounds(new Rectangle(bounds.x-30,bounds.y-30,bounds.width+60,bounds.height+30));
    }
    
    public PropertyChangeListener createDataRangePropertyListener() {
        return new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                String propertyName = e.getPropertyName();
                Object oldValue = e.getOldValue();
                Object newValue = e.getNewValue();
                if (propertyName.equals("log")) {
                    update();
                    firePropertyChange("log", oldValue, newValue);
                }
                else if (propertyName.equals("minimum")) {
                    update();
                    firePropertyChange("dataMinimum", oldValue, newValue);
                }
                else if (propertyName.equals("maximum")) {
                    update();
                    firePropertyChange("dataMaximum", oldValue, newValue);
                }
                markDirty();
            }
        };
    }
    
    public static void main( String[] args ) {
        JFrame jframe= new JFrame();
        jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel= new JPanel();
        DasCanvas canvas= new DasCanvas(300,300);
        
        DasRow row= new DasRow(canvas,0.1,0.9);
        DasColumn column= new DasColumn(canvas,0.1,0.9);
        
        canvas.addCanvasComponent(DasPlot.createDummyPlot(row,column));
        DataRange dataRange= new DataRange(null,TimeUtil.createValid("1998-01-01"),TimeUtil.createValid("1999-01-01"),false);
        canvas.addCanvasComponent(new TimeRangeLabel(dataRange,row,column));
        
        panel.setLayout(new BorderLayout());
        panel.add(canvas,BorderLayout.CENTER);
        jframe.setContentPane(panel);
        jframe.pack();
        jframe.setVisible(true);
        
        canvas.repaint();
    }
}
