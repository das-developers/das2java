
package org.das2.event;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;
import java.util.logging.Level;
import static org.das2.event.MouseModule.logger;
import org.das2.graph.DasCanvasComponent;

/**
 * adds 1,2,3,4,5 keystrokes to the renderer.
 * @author jbf
 */
public class LengthMouseModule extends MouseModule {
    
    public LengthMouseModule( DasCanvasComponent parent, LengthDragRenderer dragRenderer, String label ) {
        super(parent, dragRenderer, label);
    }
            
    @Override
    public String getDirections() {
        LengthDragRenderer r= (LengthDragRenderer)super.getDragRenderer();
        r.setNCycles(1);
        return "Press P to pin, C to copy data to clipboard, 1-9 set divisor.";
    }

    @Override
    public void keyTyped(KeyEvent keyEvent) {
        logger.log(Level.FINE, "keyTyped {0} {1}", new Object[]{keyEvent.getKeyChar(), keyEvent.isMetaDown()});
        if ( keyEvent.getKeyChar()>='1' && keyEvent.getKeyChar()<='9' ) { 
            LengthDragRenderer r= (LengthDragRenderer)super.getDragRenderer();
            r.setNCycles(keyEvent.getKeyChar()-'0');
            parent.repaint();
        } else if ( keyEvent.getKeyChar()=='c' ) { 
            LabelDragRenderer r= (LabelDragRenderer) super.dragRenderer;
            String text= r.label.replaceAll("!c"," ");
            StringSelection stringSelection = new StringSelection( text );
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, (Clipboard clipboard1, Transferable contents) -> {
            });
            logger.log(Level.FINE, "copied to mouse buffer: {0}", text);
        }
    }    
}
