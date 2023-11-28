
package test.graph;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import org.das2.graph.AnchorPosition;
import org.das2.graph.AnchorType;
import org.das2.graph.DasAnnotation;
import org.das2.graph.DasAxis;
import org.das2.graph.DasCanvas;
import org.das2.graph.DasColumn;
import org.das2.graph.DasPlot;
import org.das2.graph.DasRow;
import org.das2.graph.GraphUtil;

/**
 *
 * @author jbf
 */
public class AnnotationDemo {
        
    JPanel contentPane;
    protected DasPlot plot1,plot2;
    
    private synchronized JPanel getContentPane() {
        if (contentPane == null) {
            contentPane = new JPanel();
        }
        return contentPane;
    }

    public JFrame showFrame() {
        JFrame frame= new JFrame( "Annotation Demo");
        frame.getContentPane().add(getContentPane());
        frame.pack();
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        frame.setVisible(true);
        return frame;
    }
    
    public AnnotationDemo() {
        int width = 500;
        int height = 400;

        getContentPane().setLayout(new BorderLayout());

        DasCanvas canvas = new DasCanvas(width, height);

        getContentPane().add(canvas, BorderLayout.CENTER );

        DatumRange xrange= DatumRange.newRange(0,10,Units.seconds);
        //DatumRange xrange= DatumRangeUtil.parseTimeRangeValid("2009");
        DatumRange yrange= DatumRange.newRange(0.1,100, Units.dimensionless);

        plot1= GraphUtil.newDasPlot(canvas, xrange, yrange);

        plot2= GraphUtil.newDasPlot(canvas, xrange, yrange);
        
        plot1.getXAxis().setUseDomainDivider(true);
        plot1.getYAxis().setUseDomainDivider(true);
    
        plot2.getXAxis().setUseDomainDivider(true);
        plot2.getYAxis().setUseDomainDivider(true);

        plot2.setColumn( plot1.getColumn() );
        plot2.getXAxis().setColumn( plot1.getColumn() );
        plot2.getYAxis().setColumn( plot1.getColumn() );

        plot1.getColumn().setMaxLayout("100%-20em");
        
        plot2.getYAxis().setOrientation( DasAxis.RIGHT );
        
        DasAnnotation anno= new DasAnnotation("");
        anno.setAnchorType( AnchorType.PLOT );
        anno.setAnchorPosition( AnchorPosition.OutsideNE );
        anno.setAnchorOffset( "+5.0em,-0.0em");
        anno.setText("!(painter;psym;boxes;size=0.8em;connect=solid;color=red)  Annotation 1<br>!(painter;psym;boxes;size=0.8em;connect=solid;color=blue)  Second Anno");
        canvas.add( anno, plot1.getRow(), plot1.getColumn() );
    }
            
    public static void main( String[] args ) {
        new AnnotationDemo().showFrame();
    }
}
