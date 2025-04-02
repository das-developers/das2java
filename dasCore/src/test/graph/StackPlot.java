
package test.graph;

import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.das2.datum.DatumRange;
import org.das2.datum.DatumRangeUtil;
import org.das2.datum.Units;
import org.das2.graph.DasCanvas;
import org.das2.graph.DasPlot;
import org.das2.graph.GraphUtil;

/**
 * This demonstrates how to make a stack of plots.  Note that during 
 * construction, the canvas has not yet been sized.  Calling "prepareForOutput"
 * will finally initialize the dimensions of all components.
 * 
 * TODO: Why does the constructor take width and height if you have to indicate
 * the width and height in prepareForOutput?
 * 
 * @author jbf
 */
public class StackPlot {

    DasCanvas canvas;
    JPanel contentPane;
    protected DasPlot plot;
    protected DasPlot plot2;

    private synchronized JPanel getContentPane() {
        if (contentPane == null) {
            contentPane = new JPanel();
        }
        return contentPane;
    }

    public JFrame showFrame() {
        JFrame frame= new JFrame( "Time Axis Demo");
        frame.getContentPane().add(getContentPane());
        frame.pack();
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        frame.setVisible(true);
        return frame;
    }

    public StackPlot() {
        try {
            int width = 500;
            int height = 500;
            
            getContentPane().setLayout(new BorderLayout());
            
            canvas = new DasCanvas(width, height);
            
            getContentPane().add(canvas, BorderLayout.CENTER );
            
            DatumRange xrange= DatumRangeUtil.parseTimeRange("2023-08-01/P1M");
            DatumRange yrange= DatumRange.newRange(0.1,100, Units.dimensionless);
            
            plot= GraphUtil.newDasPlot(canvas, xrange, yrange);
            plot.getRow().setMinLayout( "+2em");
            plot.getRow().setMaxLayout( "50%-3em" );
        
            plot2= GraphUtil.newDasPlot(canvas, xrange, yrange);
            plot2.getRow().setMinLayout( "50%+1em");
            plot2.getRow().setMaxLayout( "100%-4em" );
            
            canvas.prepareForOutput( width, height );
            
        } catch (ParseException ex) {
            Logger.getLogger(TimeAxisDemo.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void main( String[] args ) throws IOException {
        StackPlot p= new StackPlot();
        
        BufferedImage im= p.canvas.getImage( p.canvas.getWidth(), p.canvas.getHeight() );
        
        ImageIO.write( im, "png", new File( "/tmp/StackPlot.png" ) );
        
        System.exit(0);
    }
}

