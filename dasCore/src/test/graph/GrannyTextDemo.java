
package test.graph;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.das2.graph.DasAnnotation;
import org.das2.graph.DasCanvas;
import org.das2.graph.DasColumn;
import org.das2.graph.DasPlot;
import org.das2.graph.DasRow;
import org.das2.util.awt.PdfGraphicsOutput;

/**
 *
 * @author jbf
 */
public class GrannyTextDemo {
        JPanel contentPane;
    protected DasPlot plot;

    private synchronized JPanel getContentPane() {
        if (contentPane == null) {
            contentPane = new JPanel();
        }
        return contentPane;
    }

    public JFrame showFrame() {
        JFrame frame= new JFrame( "Granny Text Demo");
        frame.getContentPane().add(getContentPane());
        frame.pack();
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        frame.setVisible(true);
        return frame;
    }

    private void printCanvas(DasCanvas canvas) throws FileNotFoundException, IOException {
        PdfGraphicsOutput go = new PdfGraphicsOutput();
        FileOutputStream out= new FileOutputStream(new File("/tmp/ap/jeremy.pdf") );
        go.setGraphicsShapes( false );
        go.setOutputStream(out);
        go.setSize( canvas.getWidth(), canvas.getHeight() );
        go.start();
        canvas.print(go.getGraphics());
        go.finish();
    }
    
    private Action getPrintAction( final DasCanvas c ) {
        return new AbstractAction("print") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    printCanvas(c);
                } catch (IOException ex) {
                    Logger.getLogger(GrannyTextDemo.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
    }
    
    public GrannyTextDemo(String text) {
        int width = 500;
        int height = 100;

        getContentPane().setLayout(new BorderLayout());

        DasCanvas canvas = new DasCanvas(width, height);
        //canvas.setFont( Font.decode("Kalam-24") );
        //canvas.setFont( Font.decode("Loma-24") );
        //canvas.setFont( Font.decode("Laksaman-24") );
        canvas.setFont( Font.decode("Roboto-24") );
        getContentPane().add(canvas, BorderLayout.CENTER );
        
        JButton button= new JButton(getPrintAction(canvas));
        getContentPane().add(button, BorderLayout.SOUTH );
        DasRow row= DasRow.create( canvas, null, "20%", "80%" );
        DasColumn col= DasColumn.create( canvas, null, "20%", "80%" );
        DasAnnotation anno= new DasAnnotation(text);
        anno.setFontSize(20.f);
        canvas.add( anno, row, col );
        canvas.revalidate();
        

    }

    public static void main( String[] args ) {
        //new GrannyTextDemo("!c!c!(underline)HELLO!(ununderline) There!").showFrame();
        new GrannyTextDemo("Serif <b>in</b> 1917!c!c!<i>HELLO</i> There!").showFrame();
    }

}
