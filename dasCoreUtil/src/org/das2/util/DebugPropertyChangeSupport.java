/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.util;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeListenerProxy;
import java.beans.PropertyChangeSupport;
import java.util.Arrays;

/**
 *
 * @author jbf
 */
public class DebugPropertyChangeSupport extends PropertyChangeSupport {

    public DebugPropertyChangeSupport( Object bean ) {
        super(bean);
    }

    @Override
    public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
        //TODO: danger--remove this from production code.
        if ( Arrays.asList(getPropertyChangeListeners()).contains( listener ) ) {
            return;
        }
        super.addPropertyChangeListener(listener);
    }

    
    @Override
    public String toString() {
        PropertyChangeListener[] listeners= getPropertyChangeListeners();
        StringBuffer result= new StringBuffer(super.toString());
        for ( int i=0; i<listeners.length; i++ ) {
            if ( listeners[i] instanceof PropertyChangeListenerProxy ) {
                PropertyChangeListenerProxy proxy= (PropertyChangeListenerProxy)listeners[i];
                result.append("\n"+proxy.getListener() + " (property " + proxy.getPropertyName() + ")" );
            } else {
                result.append("\n"+listeners[i] );
            }
        }
        return result.toString();
    }
    
}
