/*
 * ColorBarComponent.java
 *
 * Created on November 15, 2003, 11:16 AM
 */

package edu.uiowa.physics.pw.das.components;

import org.das2.datum.Datum;
import edu.uiowa.physics.pw.das.graph.*;
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
