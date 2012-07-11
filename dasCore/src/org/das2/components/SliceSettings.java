/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.components;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 *
 * @author jbf
 */
public class SliceSettings {

    private boolean sliceRebinnedData = true;
    public static final String PROP_SLICEREBINNEDDATA = "sliceRebinnedData";

    public boolean isSliceRebinnedData() {
        return sliceRebinnedData;
    }

    public void setSliceRebinnedData(boolean sliceRebinnedData) {
        boolean oldSliceRebinnedData = this.sliceRebinnedData;
        this.sliceRebinnedData = sliceRebinnedData;
        propertyChangeSupport.firePropertyChange(PROP_SLICEREBINNEDDATA, oldSliceRebinnedData, sliceRebinnedData);
    }

    

    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

}
