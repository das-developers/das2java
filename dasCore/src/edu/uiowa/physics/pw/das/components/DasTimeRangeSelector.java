/* File: DasTimeRangeSelector.java
 * Copyright (C) 2002-2003 University of Iowa
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

package edu.uiowa.physics.pw.das.components;

/**
 *
 * @author  jbf
 */
import edu.uiowa.physics.pw.das.datum.TimeDatum;
import edu.uiowa.physics.pw.das.event.TimeRangeSelectionEvent;
import edu.uiowa.physics.pw.das.util.DasDate;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class DasTimeRangeSelector extends javax.swing.JPanel implements ActionListener, edu.uiowa.physics.pw.das.event.TimeRangeSelectionListener {
    
    private DasDate startTime = null;
    private DasDate endTime = null;
    
    JTextField idStart= null;
    JTextField idStop= null;
    
    /** Utility field used by event firing mechanism. */
    private javax.swing.event.EventListenerList listenerList =  null;
    
    /** Creates a new instance of DasTimeRangeSelector */
    public DasTimeRangeSelector() {
        super();
        buildComponents();
    }
    
    private void buildComponents() {
        this.setLayout(new FlowLayout(FlowLayout.RIGHT));
        
        JButton b= new JButton("<<");
        b.addActionListener(this);
        b.setActionCommand("previous");
        b.setToolTipText("Scan back in time");
        this.add(b);
        
        idStart= new JTextField("");
        idStart.setSize(18,1);
        idStart.addActionListener(this);
        idStart.setActionCommand("startTime");
        this.add(idStart);
        
        idStop= new JTextField("");
        idStop.setSize(18,1);
        idStop.addActionListener(this);
        idStop.setActionCommand("endTime");
        this.add(idStop);
        
        b= new JButton(">>");
        b.addActionListener(this);
        b.setActionCommand("next");
        b.setToolTipText("Scan forward in time");
        this.add(b);
        
    }
    
    public DasTimeRangeSelector(DasDate startTime, DasDate endTime) {
        this();
        setStartTime(startTime);
        update();
        setEndTime(endTime);
        update();
    }
    
    public DasDate getStartTime() {
        DasDate s1;
        try {
            s1= new DasDate(idStart.getText());
        } catch (IllegalArgumentException e) {
            s1= null;
        }
        return s1;
    }
    
    public DasDate getEndTime() {
        DasDate s2;
        try {
            s2= new DasDate(idStop.getText());
        } catch (IllegalArgumentException e) {
            s2= null;
        }
        return s2;
    }
    
    private void update() {
        if (startTime!=null) idStart.setText(startTime.toString());
        if (endTime!=null) idStop.setText(endTime.toString());
    }
    
    public void setStartTime(DasDate s1) {
        startTime= s1;
        endTime= getEndTime();
        if (endTime!=null) {
            if (startTime.compareTo(endTime)==1)
                endTime= new DasDate( startTime.toString() );
        }
        update();
    }
    
    public void setEndTime(DasDate s2) {
        endTime= s2;
        startTime= getStartTime();
        if (startTime!=null) {
            if (startTime.compareTo(endTime)==1)
                startTime= new DasDate( endTime.toString() );
        }
        update();
    }
    
    public boolean isWithin(DasDate s1, DasDate s2) {
        DasDate startTime= getStartTime();
        DasDate endTime= getEndTime();
        return s1.compareTo(startTime) <= 0 && endTime.compareTo(s2) <= 0;
    }
    
    public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
        String command= actionEvent.getActionCommand();
        if (command.equals("previous")) {
            double twidth= getEndTime().subtract(getStartTime());
            setStartTime(getStartTime().subtract(twidth));
            setEndTime(getEndTime().subtract(twidth));
            fireTimeRangeSelectionListenerTimeRangeSelected(
            new TimeRangeSelectionEvent(this,edu.uiowa.physics.pw.das.datum.TimeDatum.create(startTime),edu.uiowa.physics.pw.das.datum.TimeDatum.create(endTime)));
        } else if (command.equals("next")) {
            double twidth= getEndTime().subtract(getStartTime());
            setStartTime(getStartTime().add(twidth));
            setEndTime(getEndTime().add(twidth));
            fireTimeRangeSelectionListenerTimeRangeSelected(
            new TimeRangeSelectionEvent(this,edu.uiowa.physics.pw.das.datum.TimeDatum.create(startTime),edu.uiowa.physics.pw.das.datum.TimeDatum.create(endTime)));
        } else if (command.equals("startTime")) {
            setStartTime(getStartTime());
            fireTimeRangeSelectionListenerTimeRangeSelected(
            new TimeRangeSelectionEvent(this,edu.uiowa.physics.pw.das.datum.TimeDatum.create(startTime),edu.uiowa.physics.pw.das.datum.TimeDatum.create(endTime)));
        } else if (command.equals("endTime")) {
            setEndTime(getEndTime());
            fireTimeRangeSelectionListenerTimeRangeSelected(
            new TimeRangeSelectionEvent(this,edu.uiowa.physics.pw.das.datum.TimeDatum.create(startTime),edu.uiowa.physics.pw.das.datum.TimeDatum.create(endTime)));
        }
    }
    
    TimeRangeSelectionEvent lastEventProcessed=null;
    public void TimeRangeSelected(TimeRangeSelectionEvent e) {
        if (false) {
            edu.uiowa.physics.pw.das.util.DasDie.println("received event");
            Graphics2D g= (Graphics2D)getGraphics();
            g.setColor(new Color(0,255,255,200));
            Rectangle dirty= new Rectangle(0,0,getWidth(),getHeight());
            g.fill(dirty);
            try { Thread.sleep(600); } catch ( InterruptedException ie ) {};
            paintImmediately(dirty);
        }
        if (!e.equals(lastEventProcessed)) {
            lastEventProcessed= e;
            setStartTime(DasDate.create(e.getStartTime()));
            setEndTime(DasDate.create(e.getEndTime()));
            fireTimeRangeSelectionListenerTimeRangeSelected(e);
        }
    }
    
    /** Registers TimeRangeSelectionListener to receive events.
     * @param listener The listener to register.
     */
    public synchronized void addTimeRangeSelectionListener(edu.uiowa.physics.pw.das.event.TimeRangeSelectionListener listener) {
        if (listenerList == null ) {
            listenerList = new javax.swing.event.EventListenerList();
        }
        listenerList.add(edu.uiowa.physics.pw.das.event.TimeRangeSelectionListener.class, listener);
    }
    
    /** Removes TimeRangeSelectionListener from the list of listeners.
     * @param listener The listener to remove.
     */
    public synchronized void removeTimeRangeSelectionListener(edu.uiowa.physics.pw.das.event.TimeRangeSelectionListener listener) {
        listenerList.remove(edu.uiowa.physics.pw.das.event.TimeRangeSelectionListener.class, listener);
    }
    
    /** Notifies all registered listeners about the event.
     *
     * @param event The event to be fired
     */
    private void fireTimeRangeSelectionListenerTimeRangeSelected(TimeRangeSelectionEvent event) {
        if (false) {
            edu.uiowa.physics.pw.das.util.DasDie.println("firing event");
            Graphics2D g= (Graphics2D)getGraphics();
            g.setColor(new Color(255,255,0,200));
            Rectangle dirty= new Rectangle(0,0,getWidth(),getHeight());
            g.fill(dirty);
            try { Thread.sleep(600); } catch ( InterruptedException e ) {};
            paintImmediately(dirty);
        }
        if (listenerList == null) return;
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==edu.uiowa.physics.pw.das.event.TimeRangeSelectionListener.class) {
                ((edu.uiowa.physics.pw.das.event.TimeRangeSelectionListener)listeners[i+1]).TimeRangeSelected(event);
            }
        }
    }
    
}
