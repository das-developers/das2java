
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
import java.util.Map.Entry;

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
    String[] propNamesArray= new String[0];
    Map<String,StackTraceElement[]> sources= new HashMap<>();
    Map<String,Long> birthMilli= new HashMap<>();

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
            final String key= listener.toString();
            if ( key.startsWith("scaleListener") ) {
                System.err.println("+++ add scaleListener");
            }
            propNames.add( key );
            propNamesArray= propNames.toArray( new String[propNames.size()] );
            sources.put( key, new Exception().getStackTrace() );
            if ( System.currentTimeMillis() - t0 < 50000 ) {
                birthMilli.put( key, 0L );
            } else {
                birthMilli.put( key, System.currentTimeMillis() );
            }
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
            final String key= listener.toString()+ " " + propertyName;
            propNames.add( key );
            propNamesArray= propNames.toArray( new String[propNames.size()] );
            sources.put( key, new Exception().getStackTrace() );
            if ( System.currentTimeMillis() - t0 < 50000 ) {
                birthMilli.put( key, 0L );
            } else {
                birthMilli.put( key, System.currentTimeMillis() );
            }
        }
    }

    @Override
    public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
        super.removePropertyChangeListener(listener);
        if ( listener!=null ) {
            final String key = listener.toString();            
            if ( key.startsWith("scaleListener") ) {
                System.err.println("--- rm scaleListener");
            }
            propNames.remove(key);
            propNamesArray= propNames.toArray( new String[propNames.size()] );
            sources.remove(key);
            birthMilli.remove(key);
        }
        printOldListeners();
    }

    @Override
    public synchronized void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        super.removePropertyChangeListener(propertyName, listener);
        //TODO: possible bug: sometimes with TSBs listener is null.
        if ( listener!=null ) {
            final String key= listener.toString()+ " " + propertyName;
            propNames.remove( key );
            propNamesArray= propNames.toArray( new String[propNames.size()] );
            sources.remove( key );
            birthMilli.remove( key );
        }
        printOldListeners();
    }
    
    private void printOldListeners() {
        long tnow= System.currentTimeMillis();
        for ( Entry<String,Long> e: birthMilli.entrySet() ) {
            if ( e.getValue()>0 && ( tnow-e.getValue() ) > 20000 ) {
                StackTraceElement[] sts= sources.get(e.getKey());
                System.err.println("== "+e.getKey()+" ("+( tnow-e.getValue() ) +"ms) ==");
                int i=5;
                for ( StackTraceElement st: sts ) {
                    if ( i-- == 0 ) break;
                    System.err.println( st.toString() );
                }
            } 
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
