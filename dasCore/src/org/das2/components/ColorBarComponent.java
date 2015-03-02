/*
 * ColorBarComponent.java
 *
 * Created on November 15, 2003, 11:16 AM
 */

package org.das2.components;

import org.das2.graph.DasColorBar;
import org.das2.graph.DasColumn;
import org.das2.graph.DasCanvas;
import org.das2.graph.DasRow;
import org.das2.datum.Datum;
import javax.swing.*;

/**
 * ColorBarComponent wraps a DasColorBar and DasCanvas to make a component.
 * @deprecated This is not terribly useful and probably should be removed.
 * @author  Owner
 */
public class ColorBarComponent extends JPanel {
    DasColorBar colorBar;
    DasCanvas canvas;
    
    /**
     * create a new ColorBarComponent
     * @param min the minimum
     * @param max the maximum
     * @param isLog true if the colorbar should be log initially.
     */
    public ColorBarComponent(Datum min, Datum max, boolean isLog) {        
        canvas= new DasCanvas(100, 500);
        DasRow row= new DasRow(canvas,0.1,0.9);
        DasColumn column= DasColumn.create(canvas);        
        colorBar= new DasColorBar( min, max, isLog );
        canvas.add(colorBar,row, column);
        this.add(canvas);        
    }
    
    /**
     * get the colorbar
     * @return the colorbar
     */
    public DasColorBar getColorBar() {
        return colorBar;
    }
    
}
