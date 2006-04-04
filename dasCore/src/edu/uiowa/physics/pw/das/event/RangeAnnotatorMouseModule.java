/*
 * RangeAnnotatorMouseModule.java
 *
 * Created on November 1, 2005, 4:30 PM
 *
 *
 */

package edu.uiowa.physics.pw.das.event;

import edu.uiowa.physics.pw.das.graph.DasPlot;
import java.awt.event.MouseEvent;

/**
 *
 * @author Jeremy
 */
public class RangeAnnotatorMouseModule extends HorizontalRangeSelectorMouseModule {
      public RangeAnnotatorMouseModule( DasPlot parent ) {
          super( parent, parent.getXAxis() );
          setLabel("Range Annotator");          
      }

    public void mouseRangeSelected(MouseDragEvent e) {        
        super.mouseRangeSelected(e);
        System.out.println(e);
    }
    
    public void mouseReleased(MouseEvent e) {        
        super.mouseReleased(e);
        System.out.println(e);
    }
      
}
