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
 *
 * @author  Owner
 */
public class ColorBarComponent extends JPanel {
    DasColorBar colorBar;
    DasCanvas canvas;
    
    /** Creates a new instance of ColorBarComponent */
    public ColorBarComponent(Datum min, Datum max, boolean isLog) {        
        canvas= new DasCanvas(100, 500);
        DasRow row= DasRow.create(canvas);
        DasColumn column= DasColumn.create(canvas);        
        colorBar= new DasColorBar( min, max, isLog );
        canvas.add(colorBar,row, column);
        this.add(canvas);        
    }
    
    public DasColorBar getColorBar() {
        return colorBar;
    }
    
}
