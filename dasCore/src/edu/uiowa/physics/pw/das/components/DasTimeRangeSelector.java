/* File: DasTimeRangeSelector.java
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

package edu.uiowa.physics.pw.das.components;

import edu.uiowa.physics.pw.das.datum.*;
/**
 *
 * @author  jbf
 */
import edu.uiowa.physics.pw.das.datum.TimeUtil;
import edu.uiowa.physics.pw.das.event.TimeRangeSelectionEvent;
import edu.uiowa.physics.pw.das.event.TimeRangeSelectionListener;
import edu.uiowa.physics.pw.das.util.*;
import edu.uiowa.physics.pw.das.util.DasDie;
import java.awt.*;

import javax.swing.event.EventListenerList;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.*;
import java.util.prefs.*;
import javax.swing.*;

public class DasTimeRangeSelector extends JPanel implements TimeRangeSelectionListener {
    
    private DatumRange range= null;
    
    JTextField idStart= null;
    JTextField idStop= null;
    JPanel startStopPane=null;
    
    boolean updateRangeString= false;   // true indicates use formatted range string in start time cell.
    
    /** Utility field used by event firing mechanism. */
    private EventListenerList listenerList =  null;
    
    /** Action that is associated with the previous button.
     * Access is given to subclasses so that other widgets can be associated
     * with this action (Popup menu, etc).
     */
    protected final Action previousAction = new AbstractAction("<<") {
        public void actionPerformed(ActionEvent e) {
            fireTimeRangeSelectedPrevious();
        }
    };
    
    /** Action that is associated with the next button.
     * Access is given to subclasses so that other widgets can be associated
     * with this action (Popup menu, etc).
     */
    protected final Action nextAction = new AbstractAction(">>") {
        public void actionPerformed(ActionEvent e) {
            fireTimeRangeSelectedNext();
        }
    };
    
    protected final Action rangeAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
            fireTimeRangeSelected();
        }
    };
    
    /** Creates a new instance of DasTimeRangeSelector */
    public DasTimeRangeSelector() {
        super();
        updateRangeString= Preferences.userNodeForPackage(this.getClass()).getBoolean("updateRangeString", false);
        buildComponents();
    }
        
    private void buildComponents() {
        this.setLayout(new FlowLayout());
        
        JButton b= new JButton();
        b.setAction(previousAction);
        b.setActionCommand("previous");
        b.setToolTipText("Scan back in time");
        this.add(b);
                
        startStopPane= new JPanel(new FlowLayout());
        
        idStart= new JTextField(18);
        idStart.setAction(rangeAction);
        idStart.setActionCommand("startTime");
        startStopPane.add(idStart);
        
        idStop= new JTextField(18);
        idStop.addActionListener(rangeAction);
        idStop.setActionCommand("endTime");
        startStopPane.add(idStop);
        
        this.add(startStopPane);
        
        b= new JButton();
        b.setAction(nextAction);
        b.setActionCommand("next");
        b.setToolTipText("Scan forward in time");
        this.add(b);
        
    }
    
    public DasTimeRangeSelector(Datum startTime, Datum endTime) {
        this();
        range= new DatumRange( startTime, endTime );
        update();
    }
        
    private void parseRange() {
        boolean updateRangeString0= updateRangeString;
        if ( idStop.getText().equals("") ) {
            DatumRange dr;
            try {
                String rangeString= idStart.getText();
                dr= DatumRangeUtil.parseTimeRange(rangeString);
                range= dr; 
                updateRangeString= true;                    
            } catch ( ParseException e ) {
                DasExceptionHandler.handle(e);                
            }                                               
        } else {            
            updateRangeString= false;
            try {
                Datum s1= TimeUtil.create(idStart.getText());      
                Datum s2= TimeUtil.create(idStop.getText());
                range= new DatumRange(s1,s2);                
            } catch ( ParseException e ) {
                DasExceptionHandler.handle(e);               
            }                            
        }
        if ( updateRangeString!=updateRangeString0 ) 
            Preferences.userNodeForPackage(getClass()).putBoolean("updateRangeString", updateRangeString );
        
        return;
    }
    
    public Datum getStartTime() {
        parseRange();
        return range.min();
    }
    
    public Datum getEndTime() {
        parseRange();
        return range.max();
    }
    
    public DatumRange getRange() {
        return range;
    }
    
    public void setRange( DatumRange range ) {
        this.range= range;
        update();
    }
    
    private void update() {
        if ( range!=null ) {
            if ( updateRangeString ) {
                String rangeString= DatumRangeUtil.formatTimeRange(range);
                idStart.setText( rangeString );                
                idStart.setColumns(28);
                idStop.setText("");
                idStop.setColumns(8);
                startStopPane.revalidate();                
            } else {
                idStart.setText(range.min().toString());
                idStop.setText(range.max().toString());
                idStart.setColumns(18);
                idStop.setColumns(18);
                startStopPane.revalidate();
            }
        }
    }
    
    public void setStartTime(Datum s1) {
        if ( range==null ) {
            return;
        } else {
            Datum endTime= range.max();
            if ( endTime.le(s1) ) {
                endTime= s1.add(1,Units.seconds);
            }
            range= new DatumRange( s1, endTime );
        }
        update();
    }
    
    public void setEndTime(Datum s2) {
        if ( range==null ) {
            return;
        } else {
            Datum startTime= range.min();
            if ( startTime.ge(s2) ) {
                startTime= s2.subtract(1, Units.seconds);
            }
            range= new DatumRange( startTime, s2 );
        }
        update();
    }
    
    public boolean isWithin(Datum s1, Datum s2) {
        Datum startTime= getStartTime();
        Datum endTime= getEndTime();
        return s1.compareTo(startTime) <= 0 && endTime.compareTo(s2) <= 0;
    }
    
    TimeRangeSelectionEvent lastEventProcessed=null;
    public void TimeRangeSelected(TimeRangeSelectionEvent e) {
        if (false) {
            DasDie.println("received event");
            Graphics2D g= (Graphics2D)getGraphics();
            g.setColor(new Color(0,255,255,200));
            Rectangle dirty= new Rectangle(0,0,getWidth(),getHeight());
            g.fill(dirty);
            try { Thread.sleep(600); } catch ( InterruptedException ie ) {};
            paintImmediately(dirty);
        }
        if (!e.equals(lastEventProcessed)) {
            lastEventProcessed= e;
            setStartTime( e.getStartTime() );
            setEndTime( e.getEndTime() );
            fireTimeRangeSelected(e);
        }
    }
    
    /** Registers TimeRangeSelectionListener to receive events.
     * @param listener The listener to register.
     */
    public synchronized void addTimeRangeSelectionListener(TimeRangeSelectionListener listener) {
        if (listenerList == null ) {
            listenerList = new EventListenerList();
        }
        listenerList.add(TimeRangeSelectionListener.class, listener);
    }
    
    /** Removes TimeRangeSelectionListener from the list of listeners.
     * @param listener The listener to remove.
     */
    public synchronized void removeTimeRangeSelectionListener(TimeRangeSelectionListener listener) {
        listenerList.remove(TimeRangeSelectionListener.class, listener);
    }
    
    protected void fireTimeRangeSelectedPrevious() {
        range= range.rescale(-1,0);
        fireTimeRangeSelected(new TimeRangeSelectionEvent(this,range.min(),range.max()));
    }
    
    protected void fireTimeRangeSelectedNext() {
        range= range.rescale(1,2);
        update();
        fireTimeRangeSelected(new TimeRangeSelectionEvent(this,range.min(),range.max()));
    }
    
    protected void fireTimeRangeSelected() {
        parseRange();
        update();
        fireTimeRangeSelected(new TimeRangeSelectionEvent(this,range.min(),range.max()));
    }
    
    /** Notifies all registered listeners about the event.
     *
     * @param event The event to be fired
     */
    protected void fireTimeRangeSelected(TimeRangeSelectionEvent event) {
        if (false) {
            DasDie.println("firing event");
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
            if (listeners[i]==TimeRangeSelectionListener.class) {
                ((TimeRangeSelectionListener)listeners[i+1]).TimeRangeSelected(event);
            }
        }
    }
    
    public Dimension getMaximumSize() {
        return super.getPreferredSize();
    }
    
    public Dimension getMinimumSize() {
        return super.getPreferredSize();
    }
    
}
