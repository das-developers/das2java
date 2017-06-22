/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.qds.filters;

import java.util.logging.Logger;
import javax.swing.JPanel;
import org.das2.util.LoggerManager;
import org.das2.qds.QDataSet;

/**
 * Implements the typical filter, where we don't care about the input data and
 * the filter itself implements the GUI.  Note when the filter property would change, 
 * the implementation must fire off a property change.
 * @author jbf
 */
public abstract class AbstractFilterEditorPanel extends JPanel implements FilterEditorPanel {
    
    protected static final Logger logger= LoggerManager.getLogger("qdataset.filters");
    
    /**
     * configure the GUI based on this filter.  The filter string will
     * start with the pipe character.
     * @param filter the filter string
     */    
    @Override
    public abstract void setFilter( String filter );

    /**
     * return the filter specified by the GUI.  The filter string will
     * start with the pipe character.  When this would change, implementations should fire
     * off a property change event like so:
     **<blockquote><pre>
     *firePropertyChange( PROP_FILTER, null, ff );
     *</pre></blockquote>
     * where ff is the new value.
     * @return the filter string
     */
    @Override
    public abstract String getFilter( );
    
    @Override
    public void setInput( QDataSet ds ) {
        // do nothing, ignore input
    }
    
    @Override
    public JPanel getPanel() {
        return this;
    }

    @Override
    public boolean validateFilter(String filter, QDataSet in) {
        return true;
    }
    
}
