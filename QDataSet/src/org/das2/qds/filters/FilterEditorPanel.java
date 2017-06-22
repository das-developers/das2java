
package org.das2.qds.filters;

import javax.swing.JPanel;
import org.das2.qds.QDataSet;

/**
 * Interface for adding small GUIs to control each of the filters.  For example
 * "|divide(5)" is controlled with a GUI that accepts the float parameter that 
 * might check that the operand is not zero.  These should each implement get
 * and setFilter, and fire off a property change event when the value is changed,
 * so the GUI can be interactively.
 * @author mmclouth
 */
public interface FilterEditorPanel {
    /**
     * return the filter specified by the GUI.  The filter string will
     * start with the pipe character.  This may be called from off of the event
     * thread!
     * @return the filter string
     */
    String getFilter();
    
    /**
     * configure the GUI based on this filter.  The filter string will
     * start with the pipe character.
     * @param filter the filter string
     */
    void setFilter( String filter );
    
    /**
     * configure the GUI based on this input
     * @param ds the data that will be input to the filter
     */
    void setInput( QDataSet ds );
    
    /**
     * the panel for this editor.
     * @return the panel for this editor.
     */
    JPanel getPanel();
    
    /**
     * return true if the filter is valid
     * @param filter "slice1(-1)"
     * @param in the input, or null.
     * @return false if the input is clearly not valid.
     */
    boolean validateFilter( String filter, QDataSet in );
    
    public static String PROP_FILTER= "filter";
    
}
