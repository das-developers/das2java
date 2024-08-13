/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.components;

import java.awt.Font;
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

    private String font = "";

    public static final String PROP_FONT = "font";

    public String getFont() {
        return font;
    }

    public void setFont(String font) {
        String oldFont = this.font;
        this.font = font;
        propertyChangeSupport.firePropertyChange(PROP_FONT, oldFont, font);
    }

    public static String encodeFont( Font f ) {
        String style="-";
        if ( f.isBold() ) style+="bold";
        if ( f.isItalic() ) style+="italic";
        String result= f.getFamily();
        if ( style.length()>1 ) result+= style;
        return result + "-" + f.getSize();

    }

    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

}
