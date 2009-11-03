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

package org.das2.graph;

import org.das2.datum.format.DatumFormatter;
import org.das2.datum.Units;
import org.das2.datum.format.TimeDatumFormatter;
import org.das2.datum.Datum;
import org.das2.datum.UnitsConverter;
import org.das2.datum.TimeUtil;
import org.das2.DasProperties;
import org.das2.event.MouseModule;
import org.das2.util.DasMath;
import org.das2.util.DasExceptionHandler;

import java.awt.*;
import java.awt.geom.*;
import java.beans.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.das2.datum.DatumRange;

/**
 *
 * @author  jbf
 */
public class TimeRangeLabel extends DasCanvasComponent {
    
    private static final DatumFormatter MINUTES;
    private static final DatumFormatter SECONDS;
    private static final DatumFormatter MILLISECONDS;

    private boolean rangeLabel;
    
    static {
        try {
            MINUTES = new TimeDatumFormatter("yyyy-MM-dd '('DDD')' HH:mm");
            SECONDS = new TimeDatumFormatter("yyyy-MM-dd '('DDD')' HH:mm:ss");
            MILLISECONDS = new TimeDatumFormatter("yyyy-MM-dd '('DDD')' HH:mm:ss.SSS");
        }
        catch (java.text.ParseException pe) {
            //If this is happening, then there is a major problem.
            throw new RuntimeException(pe);
        }
    }
    
    private DataRange dataRange;

	private Datum min = TimeUtil.createTimeDatum(2000, 1, 1, 0, 0, 0, 0);

	private Datum max = TimeUtil.createTimeDatum(2000, 1, 2, 0, 0, 0, 0);
    
    private DatumFormatter df;
    
    double emOffset = 2.0;
    
    private class DataRangePropertyChangeListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
			if (evt.getPropertyName().equals("minimum")) {
				setMin(Datum.create(dataRange.getMinimum(), dataRange.getUnits()));
			}
			else if (evt.getPropertyName().equals("maximum")) {
				setMax(Datum.create(dataRange.getMaximum(), dataRange.getUnits()));
			}
        }
    }
    
    private boolean startOnly = false;
    
    /** Creates a new instance of TimeRangeLabel */
    public TimeRangeLabel(DataRange dataRange) {
        this.dataRange= dataRange;
		this.min = Datum.create(dataRange.getMinimum(), dataRange.getUnits());
		this.max = Datum.create(dataRange.getMaximum(), dataRange.getUnits());
        DataRangePropertyChangeListener listener = new DataRangePropertyChangeListener();
        dataRange.addPropertyChangeListener("minimum", listener);
        dataRange.addPropertyChangeListener("maximum", listener);
        updateFormatter();
    }

	public TimeRangeLabel() {
		updateFormatter();
	}

	public DatumRange getRange() {
		return new DatumRange(min, max);
	}

	public void setRange(DatumRange range) {
		if (range == null) {
			min = max = null;
		}
		else {
			min = range.min();
			max = range.max();
		}
		repaint();
	}

	public Datum getMax() {
		return max;
	}

	public void setMax(Datum max) {
		this.max = max;
		repaint();
	}

	public Datum getMin() {
		return min;
	}

	public void setMin(Datum min) {
		this.min = min;
		repaint();
	}
    
    protected void paintComponent(Graphics graphics) {
        Graphics2D g= (Graphics2D) graphics;
        g.setRenderingHints(DasProperties.getRenderingHints());
        
        FontMetrics fm= g.getFontMetrics();
        
        int y = getRow().getDMinimum();
        int x = getColumn().getDMinimum();
        
        g.translate(-getX(),-getY());
        
        int yLevel= y - (int)(getFont().getSize()*emOffset + 0.5);
        
        if ( this.rangeLabel ) {
            String label= getRange().toString();
            g.drawString( label, x, yLevel );
            return;
        } else {
            g.drawString(df.format(min), x, yLevel );
        }
        
        if (!startOnly) {
            String label= df.format(max);
            x += getColumn().getWidth() - fm.stringWidth(label);
            g.drawString(label, x, yLevel );
        }
    }
    public void resize() {
        Rectangle bounds= new Rectangle(
                getColumn().getDMinimum()-30,
                getRow().getDMinimum()-(int)(getFont().getSize()*(emOffset+1)+0.5), 
                getColumn().getWidth()+60,
                getFont().getSize()*3 );
        this.setBounds( bounds );
    }
    
    private void updateFormatter() {
        //UnitsConverter converter = Units.getConverter(dataRange.getUnits(), Units.t2000);
        double min = this.min.doubleValue(Units.t2000);
        double max = this.max.doubleValue(Units.t2000);
        min = secondsSinceMidnight(min);
        max = secondsSinceMidnight(max);
        int minMS = (int)(min * 1000.);
        int maxMS = (int)(max * 1000.);
        if ((minMS % 1000) != 0 || (maxMS % 1000) != 0) {
            df = MILLISECONDS;
        }
        else if ((minMS % 60000) != 0 || (maxMS % 60000) != 0) {
            df = SECONDS;
        }
        else {
            df = MINUTES;
        }
    }
    
    private double secondsSinceMidnight(double t2000) {
        if (t2000 < 0) {
            t2000 = t2000 % 86400;
            if (t2000 == 0) {
                return 0;
            } else {
                return 86400 + t2000;
            }
        } else {
            return t2000 % 86400;
        }
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
                    updateFormatter();
                    firePropertyChange("dataMinimum", oldValue, newValue);
                }
                else if (propertyName.equals("maximum")) {
                    update();
                    updateFormatter();
                    firePropertyChange("dataMaximum", oldValue, newValue);
                }
                markDirty();
            }
        };
    }
    
    public boolean isStartOnly() {
        return startOnly;
    }
    
    public void setStartOnly(boolean b) {
        this.startOnly = b;
        if (isDisplayable()) {
            repaint();
        }
    }
    
    /**
     * Use strings like "2004-01-01 00:00 to 00:20" to identify times.
     */
    public void setRangeLabel( boolean b ) {
        this.rangeLabel= b;
        repaint();
    }
    
    public boolean isRangeLabel(  ) {
        return this.rangeLabel;
    }
    
    public double getEmOffset() {
        return emOffset;
    }
    
    public void setEmOffset(double emOffset) {
        this.emOffset = emOffset;
        if (getCanvas() != null) {
            resize();
            repaint();
        }
    }
    
    public static void main( String[] args ) {
        JFrame jframe= new JFrame();
        jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel= new JPanel();
        DasCanvas canvas= new DasCanvas(300,300);
        
        DasRow row1= new DasRow(canvas,0.1,0.2);
        DasRow row2 = new DasRow(canvas, 0.3, 0.4);
        DasRow row3 = new DasRow(canvas, 0.5, 0.6);
        DasRow row4 = new DasRow(canvas, 0.7, 0.8);
        DasColumn column= new DasColumn(canvas,0.1,0.9);
        
        DataRange dataRange1= new DataRange(null,TimeUtil.createValid("1998-01-01 12:20"),TimeUtil.createValid("1999-01-01"),false);
        DataRange dataRange2 = new DataRange(null, TimeUtil.createValid("1998-01-02 12:30:02"), TimeUtil.createValid("1999-01-01"),false);
        DataRange dataRange3 = new DataRange(null, TimeUtil.createValid("1998-01-03 12:40:02.244"), TimeUtil.createValid("1999-01-01"),false);

        canvas.add(new TimeRangeLabel(dataRange1),row1,column);
        canvas.add(new TimeRangeLabel(dataRange2),row2,column);
        canvas.add(new TimeRangeLabel(dataRange3),row3,column);
        
        panel.setLayout(new BorderLayout());
        panel.add(canvas,BorderLayout.CENTER);
        jframe.setContentPane(panel);
        jframe.pack();
        jframe.setVisible(true);
        
        canvas.repaint();
    }
}
