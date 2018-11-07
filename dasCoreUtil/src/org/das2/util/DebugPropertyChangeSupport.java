
package org.das2.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeListenerProxy;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PropertyChangeSupport implementation that provides debugging information, 
 * such as toString.
 * @author jbf
 */
public class DebugPropertyChangeSupport extends PropertyChangeSupport {

    static long t0= System.currentTimeMillis();
    
    String myBean;
    public long t= System.currentTimeMillis() - t0;
    
    List<String> propNames= new ArrayList();
    Map<String,StackTraceElement[]> sources= new HashMap<>();

    public DebugPropertyChangeSupport( Object bean ) {
        super(bean);
        myBean= bean.toString();
    }

    @Override
    public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
        //TODO: danger--remove this from production code.
        if ( Arrays.asList(getPropertyChangeListeners()).contains( listener ) ) {
            return;
        }
        super.addPropertyChangeListener(listener);
        if ( listener!=null ) {
            propNames.add( listener.toString() );
            sources.put( listener.toString(), new Exception().getStackTrace() );
        }
    }

    @Override
    public synchronized void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        //TODO: danger--remove this from production code.
        if ( Arrays.asList(getPropertyChangeListeners()).contains( listener ) ) {
            return;
        }
        super.addPropertyChangeListener(propertyName, listener); 
        if ( listener!=null ) {
            propNames.add( listener.toString()+ " " + propertyName );
            sources.put( listener.toString()+ " " + propertyName, new Exception().getStackTrace() );
        }
    }

    @Override
    public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
        super.removePropertyChangeListener(listener);
        if ( listener!=null ) {
            propNames.remove( listener.toString() );
            sources.remove(listener.toString() );
        }
    }

    @Override
    public synchronized void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        super.removePropertyChangeListener(propertyName, listener);
        //TODO: possible bug: sometimes with TSBs listener is null.
        if ( listener!=null ) {
            propNames.remove( listener.toString()+ " " + propertyName );
            sources.remove(listener.toString()+ " " + propertyName );
        }
    }
    
    @Override
    public void firePropertyChange(PropertyChangeEvent event) {
        try {
            super.firePropertyChange(event); 
        } catch ( ConcurrentModificationException ex ) {
            ex.printStackTrace(); // bug1962
        }
    }

    @Override
    public String toString() {
        PropertyChangeListener[] listeners= getPropertyChangeListeners();
        StringBuilder result= new StringBuilder(super.toString());
        for (PropertyChangeListener listener : listeners) {
            if (listener instanceof PropertyChangeListenerProxy) {
                PropertyChangeListenerProxy proxy = (PropertyChangeListenerProxy) listener;
                result.append("\n").append(proxy.getListener()).append(" (property ").append(proxy.getPropertyName()).append(")");
            } else {
                result.append("\n").append(listener);
            }
        }
        return result.toString();
    }
    
    /**
     * return a list of the explicit properties we are listening to.
     * @return 
     */
    public synchronized String[] getPropNames() {
        return propNames.toArray( new String[propNames.size()] );
    }
    
}
