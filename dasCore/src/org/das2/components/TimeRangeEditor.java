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

package org.das2.components;

import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import org.das2.datum.Datum;
import org.das2.datum.DatumRangeUtil;
import org.das2.util.DasExceptionHandler;
import org.das2.datum.TimeUtil;
import org.das2.event.TimeRangeSelectionEvent;
import org.das2.event.TimeRangeSelectionListener;
import org.das2.system.DasLogger;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.event.EventListenerList;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.text.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.*;
import javax.swing.*;
import org.das2.datum.UnitsUtil;

/**
 * code copied from DasTimeRangeSelector for use with Autoplot.  
 * DasTimeRangeSelector wasn't very beany...
 * @author jbf
 */
public class TimeRangeEditor implements TimeRangeSelectionListener {

    private DatumRange range= null;

    JPanel panel;
    JTextField idStart= null;
    JTextField idStop= null;
    JButton viewButton= null;
    JPanel startStopModePane=null;
    CardLayout cardLayout= null;

    boolean updateRangeString= false;   // true indicates use formatted range string in start time cell.

    /** Utility field used by event firing mechanism. */
    private EventListenerList listenerList =  null;

    /** Action that is associated with the previous button.
     * Access is given to subclasses so that other widgets can be associated
     * with this action (Popup menu, etc).
     */
    protected final Action previousAction = new AbstractAction("<<") {
        @Override
        public void actionPerformed(ActionEvent e) {
            fireTimeRangeSelectedPrevious();
        }
    };

    /** Action that is associated with the next button.
     * Access is given to subclasses so that other widgets can be associated
     * with this action (Popup menu, etc).
     */
    protected final Action nextAction = new AbstractAction(">>") {
        @Override
        public void actionPerformed(ActionEvent e) {
            fireTimeRangeSelectedNext();
        }
    };

    protected final Action rangeAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            fireTimeRangeSelected();
        }
    };

    private boolean favoritesEnabled= false;
    
    private List favoritesList= null;
    private JPopupMenu favoritesMenu= null;

    private final int FAVORITES_LIST_SIZE= 5;

    private String favoritesGroup;

    private JButton favoritesButton;

    private JPanel timesPane;

    private JComboBox rangeComboBox;

    /** 
     * creates an instance of the editor, with an arbitrary range (today) loaded.
     */
    public TimeRangeEditor() {
        updateRangeString= Preferences.userNodeForPackage(this.getClass()).getBoolean("updateRangeString", false);
        buildComponents();
        Datum tnow= TimeUtil.prevMidnight( TimeUtil.now().convertTo(Units.us2000) );
        this.range= new DatumRange( tnow, TimeUtil.next( TimeUtil.DAY, tnow ) );
        update();
    }

    private Action getModeAction() {
        return new AbstractAction("mode") {
            @Override
            public void actionPerformed( ActionEvent e ) {
                updateRangeString= !updateRangeString;
                Preferences.userNodeForPackage(this.getClass()).putBoolean("updateRangeString", updateRangeString );
                revalidateUpdateMode();
                update();
            }
        };
    }

    private void revalidateUpdateMode() {
        if ( updateRangeString ) {
            //idStop.setColumns(8);
            idStart.setColumns(28);
            idStop.setVisible(false);
            viewButton.setVisible(true);
            //cardLayout.show( timesPane, "range" );
        } else {
            idStart.setColumns(18);
            //                idStop.setColumns(18);
            idStop.setVisible(true);
            //cardLayout.show( timesPane, "startStop" );
        }
        startStopModePane.revalidate();
    }

    private void buildComponents() {
        panel= new JPanel();
        panel.setLayout(new FlowLayout());

        JButton b= new JButton();
        b.setAction(previousAction);
        b.setActionCommand("previous");
        b.setToolTipText("Scan back in time");
        panel.add(b);

        startStopModePane= new JPanel(new FlowLayout());

        cardLayout= new CardLayout();
        timesPane= new JPanel( cardLayout );
        
        JPanel startStopPane2= new JPanel(new FlowLayout());

        idStart= new JTextField(18);
        idStart.setAction(rangeAction);
        idStart.setActionCommand("startTime");
        startStopPane2.add(idStart);

        idStop= new JTextField(18);
        idStop.addActionListener(rangeAction);
        idStop.setActionCommand("endTime");
        startStopPane2.add(idStop);

        timesPane.add( startStopPane2, "startStop" );

        startStopModePane.add( timesPane );
        favoritesButton= new JButton("v");
        favoritesButton.setToolTipText("recent entry times");
        favoritesButton.setPreferredSize(new Dimension( 20,20 ) );
        favoritesButton.setVisible(false);
        startStopModePane.add(favoritesButton);

        viewButton= new JButton(getModeAction());
        viewButton.setToolTipText("input mode: start/end vs time range string");
        viewButton.setPreferredSize(new Dimension( 20,20 ) );
        startStopModePane.add(viewButton);

        panel.add(startStopModePane);

        b= new JButton();
        b.setAction(nextAction);
        b.setActionCommand("next");
        b.setToolTipText("Scan forward in time");
        panel.add(b);

        revalidateUpdateMode();
    }

    /**
     * create an editor with the initial range.
     * @param startTime the start time
     * @param endTime the end time, which must be greater than startTime.
     */
    public TimeRangeEditor(Datum startTime, Datum endTime) {
        this(new DatumRange( startTime, endTime ));
    }

    /**
     * create an editor with the initial range.
     * @param range the range
     */
    public TimeRangeEditor( DatumRange range ) {
        this();
        this.range= range;
        update();
    }

    private void parseRange() {
        boolean updateRangeString0= updateRangeString;
        if ( idStop.getText().equals("") ) {
            DatumRange dr;
            try {
                String rangeString= idStart.getText();
                if ( rangeString.equals("") ) {
                    rangeString= (String)rangeComboBox.getEditor().getItem();
                }
                dr= DatumRangeUtil.parseTimeRange(rangeString);
                DatumRange oldRange= range;
                range= dr;
                updateRangeString= true;
                pcs.firePropertyChange( "range", oldRange, range );
            } catch ( ParseException e ) {
                DasExceptionHandler.handle(e);
            }
        } else {
            updateRangeString= false;
            try {
                Datum s1= TimeUtil.create(idStart.getText());
                Datum s2= TimeUtil.create(idStop.getText());
                DatumRange oldRange= range;
                range= new DatumRange(s1,s2);
                pcs.firePropertyChange( "range", oldRange, range );
            } catch ( ParseException e ) {
                DasExceptionHandler.handle(e);
            }
        }
        if ( updateRangeString!=updateRangeString0 )
            Preferences.userNodeForPackage(getClass()).putBoolean("updateRangeString", updateRangeString );

    }

    private void refreshFavorites() {
        favoritesMenu.removeAll();
        
        for ( Iterator i= favoritesList.iterator(); i.hasNext(); ) {
            final String fav= (String) i.next();
            Action favAction= new AbstractAction( fav ) {
                @Override
                public void actionPerformed( ActionEvent e ) {
                    TimeRangeEditor.this.setRange( DatumRangeUtil.parseTimeRangeValid(fav) );
                    fireTimeRangeSelected(new TimeRangeSelectionEvent(this,range));
                }
            };
            favoritesMenu.add( favAction );
        }
    }
    
    private void buildFavorites( ) {
        String favorites= Preferences.userNodeForPackage(getClass()).get( "timeRangeSelector.favorites."+favoritesGroup, "" );
        String[] ss= favorites.split("\\|\\|");
        favoritesList= new ArrayList();
        for (String s : ss) {
            if (!"".equals(s)) {
                favoritesList.add(s);
            }
        }
        favoritesMenu= new JPopupMenu();
        refreshFavorites();
        favoritesButton.add( favoritesMenu );
        favoritesButton.addActionListener( getFavoritesListener());        
    }
    
    private ActionListener getFavoritesListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed( ActionEvent e ) {
                favoritesMenu.show(panel, favoritesButton.getX(), favoritesButton.getY() );
            }
        };
    }
    /**
     * adds a droplist of recently entered times.  This should be a spacecraft string, or null.
     * @param group an arbitrary identifier for the group.
     */
    public void enableFavorites( String group ) {
        if ( group==null ) group="default";
        this.favoritesGroup= group;
        favoritesEnabled= true;
        favoritesButton.setVisible(true);
        buildFavorites( );
        
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

    public void setRange( DatumRange value ) {
        DatumRange oldValue= this.range;
        this.range= value;
        update();
        if (oldValue != value && oldValue != null && !oldValue.equals(value)) {
            pcs.firePropertyChange("range", oldValue, value);
        }
    }

    private void update() {
        if ( range!=null ) {
            if ( UnitsUtil.isTimeLocation( range.getUnits() ) ) {
                if ( updateRangeString ) {
                    String rangeString= DatumRangeUtil.formatTimeRange(range);
                    idStart.setText( rangeString );
                    idStop.setText("");
                } else {
                    idStart.setText(range.min().toString());
                    idStop.setText(range.max().toString());
                }
            } else {
                if ( updateRangeString ) {
                    String rangeString= range.toString();
                    idStart.setText( rangeString );
                    idStop.setText("");
                } else {
                    idStart.setText(range.min().toString());
                    idStop.setText(range.max().toString());
                }
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


    @Override
    public void timeRangeSelected(TimeRangeSelectionEvent e) {
        DatumRange lrange= e.getRange();
        if ( !lrange.equals(this.range) ) {
            setRange( e.getRange() );
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
        setRange( range.previous() );
        fireTimeRangeSelected(new TimeRangeSelectionEvent( this, range ));
    }

    protected void fireTimeRangeSelectedNext() {
        setRange( range.next() );
        fireTimeRangeSelected(new TimeRangeSelectionEvent(this,range));
    }

    protected void fireTimeRangeSelected() {
        parseRange();
        update();
        if ( favoritesEnabled ) {
            String store= range.toString();
            if ( favoritesList.contains( store ) ) favoritesList.remove(store); // bring to front
            favoritesList.add( 0, store );
            for ( int i=FAVORITES_LIST_SIZE; i<favoritesList.size(); i++ ) { // trim to remove old entries
                favoritesList.remove(i);
            }
            refreshFavorites();
            saveFavorites();
        }
        fireTimeRangeSelected(new TimeRangeSelectionEvent(this,range));
    }

    /** Notifies all registered listeners about the event.
     *
     * @param event The event to be fired
     */
    protected void fireTimeRangeSelected(TimeRangeSelectionEvent event) {
        if (listenerList == null) return;
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==TimeRangeSelectionListener.class) {
                String logmsg= "fire event: "+this.getClass().getName()+"-->"+listeners[i+1].getClass().getName()+" "+event;
                DasLogger.getLogger( DasLogger.GUI_LOG ).fine(logmsg);
                ((org.das2.event.TimeRangeSelectionListener)listeners[i+1]).timeRangeSelected(event);
                ((TimeRangeSelectionListener)listeners[i+1]).timeRangeSelected(event);
            }
        }
    }

    public Dimension getMaximumSize() {
        return panel.getPreferredSize();
    }

    public Dimension getMinimumSize() {
        return panel.getPreferredSize();
    }

    /**
     * get the GUI panel.  All this was because it was firing off events on 
     * construction, and I was copying DatumEditor.
     * @return the GUI panel.
     */
    public JPanel getPanel() {
        return panel;
    }

    private void saveFavorites() {
        if ( favoritesList.isEmpty() ) return;        
        StringBuilder favorites= new StringBuilder( (String)favoritesList.get(0) );
        for ( int i=1; i<favoritesList.size(); i++ ) {
            favorites.append( "||" ).append( favoritesList.get(i) );
        }
        Preferences.userNodeForPackage(getClass()).put( "timeRangeSelector."+favoritesGroup, favorites.toString() );
    }

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public synchronized void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(propertyName, listener);
    }

    public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    public synchronized void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(propertyName, listener);
    }

    public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }
}
