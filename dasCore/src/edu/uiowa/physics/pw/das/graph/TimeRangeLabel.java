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

import edu.uiowa.physics.pw.das.datum.DasFormatter;
import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.datum.TimeDatum;
import edu.uiowa.physics.pw.das.datum.Units;
import edu.uiowa.physics.pw.das.util.DasMath;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;

/**
 *
 * @author  jbf
 */
public class TimeRangeLabel extends DasCanvasComponent {
    
    DataRange dataRange;
    Formatter df;
    
    /** Creates a new instance of TimeRangeLabel */
    public TimeRangeLabel(DataRange dataRange, DasRow row, DasColumn column ) {
        //setRow(new AttachedRow(row,-0.05,-0.00));
        setRow(row);
        setColumn(column);
        this.dataRange= dataRange;
        
        edu.uiowa.physics.pw.das.event.MouseModule mm= new mouseModule();
        mouseAdapter.addMouseModule(mm);
        mouseAdapter.setPrimaryModule(mm); // THIS SHOULD BE AUTOMATIC!!!
        df= new Formatter();
    }
    
    private class mouseModule extends edu.uiowa.physics.pw.das.event.MouseModule {
    }
    
    private static class Formatter extends edu.uiowa.physics.pw.das.datum.DasFormatter {
        private boolean showSeconds=false;
        private boolean showMilli=false;
        
        private static int julday( int month, int day, int year ) {
            int jd = 367 * year - 7 * (year + (month + 9) / 12) / 4 -
            3 * ((year + (month - 9) / 7) / 100 + 1) / 4 +
            275 * month / 9 + day + 1721029;
            return jd;
        }
        public String format(Object tdo) {
            double seconds;
            int jd;  // julianDay
            
            edu.uiowa.physics.pw.das.datum.TimeDatum td= (edu.uiowa.physics.pw.das.datum.TimeDatum)tdo;
            
            if (td.getUnits()==edu.uiowa.physics.pw.das.datum.Units.mj1958) {
                double mj1958= td.doubleValue(Units.mj1958);
                seconds= mj1958 % 1 * 86400.;
                jd= (int)Math.floor(mj1958) + 2436205;
            } else if (td.getUnits()==edu.uiowa.physics.pw.das.datum.Units.us2000) {
                double us2000= td.doubleValue(Units.us2000);
                seconds= DasMath.modp( us2000, 86400000000. ) / 1000000;
                jd= (int)Math.floor( us2000 / 86400000000. ) + 2451545;
            } else {
                double us2000= td.doubleValue(Units.us2000);
                seconds= DasMath.modp( us2000, 86400000000. ) / 1000000;
                jd= (int)Math.floor( us2000 / 86400000000. ) + 2451545;
            }
            
            int iseconds= (int)(seconds+0.5);
            
            int year, month, day, hour, minute, second;
            
            hour = (int)(iseconds/3600);
            String shour=(hour<10.)?"0"+hour:""+hour;
            minute = (int)((iseconds - hour*3600)/60);
            String sminute=(minute<10.)?"0"+minute:""+minute;
            second = (int)(iseconds%60);
            String ssecond=(second<10.)?"0"+second:""+second;
            
            int jalpha, j1, j2, j3, j4, j5;
            
            float justSeconds;
            
            jalpha = (int)(((double)(jd - 1867216) - 0.25)/36524.25);
            j1 = jd + 1 + jalpha - jalpha/4;
            j2 = j1 + 1524;
            j3 = 6680 + (int)(((j2-2439870)-122.1)/365.25);
            j4 = 365*j3 + j3/4;
            j5 = (int)((j2-j4)/30.6001);
            
            day = j2 - j4 - (int)(30.6001*j5);
            month = j5-1;
            month = ((month - 1) % 12) + 1;
            year = j3 - 4715;
            year = year - (month > 2 ? 1 : 0);
            year = year - (year <= 0 ? 1 : 0);
            
            DecimalFormat nf= new DecimalFormat();
            nf.setMinimumIntegerDigits(2);
            String sdate= year + "-" + nf.format(month) + "-" + nf.format(day);
            
            int jd_jan1= julday(1,1,year);
            DecimalFormat df1= new DecimalFormat();
            df1.setMinimumIntegerDigits(3);
            sdate+= " ("+df1.format(jd-jd_jan1+1)+")";
            
            String result= sdate + " " + shour + ":" + sminute;

            showSeconds= ( iseconds % 60 ) != 0.;
            showMilli= (seconds % 1 ) != 0.;
            
            if (showSeconds) {
                if (showMilli) {
                    DecimalFormat df= new DecimalFormat();
                    df.setMinimumFractionDigits(3);
                    df.setMinimumIntegerDigits(2);
                    
                    result+= ":" + df.format(iseconds%60);
                    
                } else {
                    result+= ":" + ssecond;
                }
            }
            
            return result;
        }
    }
    
    public void paintComponent(Graphics graphics) {
        Graphics2D g= (Graphics2D) graphics;
        g.setRenderingHints(edu.uiowa.physics.pw.das.DasProperties.getRenderingHints());
        
        edu.uiowa.physics.pw.das.datum.Datum min= edu.uiowa.physics.pw.das.datum.Datum.create(dataRange.getMinimum(),dataRange.getUnits());
        edu.uiowa.physics.pw.das.datum.Datum max= edu.uiowa.physics.pw.das.datum.Datum.create(dataRange.getMaximum(),dataRange.getUnits());
        
        FontMetrics fm= g.getFontMetrics();
        
        int y = (int)Math.floor(getRow().getDMinimum() + 0.5);
        int x = (int)Math.floor(getColumn().getDMinimum() + 0.5);
        
        g.translate(-getX(),-getY());
        
        g.drawString(df.format(min), x, y-fm.getHeight()/2);
        
        String label= df.format(max);
        g.drawString(label,
           (int)( x + getColumn().getWidth() - fm.stringWidth(label)),
           y-fm.getHeight()/2);
        
    }
    
    public void resize() {
        Rectangle2D.Double bounds= DasDevicePosition.toRectangle(getRow(),getColumn());
        this.setBounds(new Rectangle((int)bounds.x-30,(int)bounds.y-30,(int)bounds.width+60,(int)bounds.height+30));
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
        DataRange dataRange= new DataRange(null,edu.uiowa.physics.pw.das.datum.TimeDatum.create("1998-01-01"),edu.uiowa.physics.pw.das.datum.TimeDatum.create("1999-01-01"),false);
        canvas.addCanvasComponent(new TimeRangeLabel(dataRange,row,column));
        
        panel.setLayout(new BorderLayout());
        panel.add(canvas,BorderLayout.CENTER);
        jframe.setContentPane(panel);
        jframe.pack();
        jframe.setVisible(true);
        
        canvas.repaint();
    }
}
