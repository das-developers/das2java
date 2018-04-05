/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.graph;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.das2.datum.Datum;
import org.das2.datum.Units;
import org.das2.event.DataRangeSelectionEvent;
import org.das2.event.DataRangeSelectionListener;
import org.das2.graph.DasAnnotation;
import org.das2.graph.DasCanvas;
import org.das2.graph.DasColumn;
import org.das2.graph.DasPlot;
import org.das2.graph.DasRow;
import org.das2.graph.DasSliceController;
import org.das2.qds.DDataSet;
import org.das2.qds.QDataSet;
import org.das2.qds.ops.Ops;
import static org.das2.qds.ops.Ops.dataset;


/**
 *
 * @author leiffert
 */
public class DasSliceControllerDemo {
    
    JPanel contentPane;
    //protected DasPlot plot;

    private synchronized JPanel getContentPane() {
        if (contentPane == null) {
            contentPane = new JPanel();
        }
        return contentPane;
    }

    public JFrame showFrame() {
        JFrame frame= new JFrame( "Slice Demo");
        frame.getContentPane().add(getContentPane());
        frame.pack();
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        frame.setVisible(true);
        frame.setSize(800, 100);
        return frame;
    }
    
    DasSliceControllerDemo(){
        int width = 500;
        int height = 50;
        
        

        getContentPane().setLayout(new BorderLayout());

        DasCanvas canvas = new DasCanvas(width, height);
        
        getContentPane().add(canvas, BorderLayout.CENTER );
        
        QDataSet qds = Ops.findgen(100);
        Ops.putProperty(qds, QDataSet.UNITS, Units.eV);

        DasSliceController sliceCont = new DasSliceController(qds);
        
        PropertyChangeListener sliceRangeListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                System.err.println("got property change event");
                System.err.println("Range = " + evt.getNewValue());
            }
        };
        
        sliceCont.addPropertyChangeListener(DasSliceController.PROP_CURRENTDATUMRANGE, sliceRangeListener);
        canvas.add(sliceCont,new DasRow(canvas, 0, 1),new DasColumn(canvas, 0, 1));
        
    }
    public static void main( String[] args ) {
        new DasSliceControllerDemo().showFrame();
    }
}
