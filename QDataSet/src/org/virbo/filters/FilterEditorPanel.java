/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.filters;

import javax.swing.JPanel;
import org.virbo.dataset.QDataSet;

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
     * start with the pipe character.
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
     * @return 
     */
    JPanel getPanel();
    
    public static String PROP_FILTER= "filter";
    
}
