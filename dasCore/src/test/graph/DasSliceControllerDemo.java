/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.graph;

import java.awt.BorderLayout;
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
     
        int[] intArr = new int[]{1,2,3,4,5};
        QDataSet qds = dataset(intArr, Units.seconds);
                
        
        DasSliceController sliceCont = new DasSliceController(qds);
        DataRangeSelectionListener rangeListener = new DataRangeSelectionListener() {
            @Override
            public void dataRangeSelected(DataRangeSelectionEvent e) {
               
                System.err.println("Got the Data Range event");
                System.err.println("Max = " + e.getMaximum());
                System.err.println("Min = " + e.getMinimum());
            }
        };
        sliceCont.addDataRangeSelectionListener(rangeListener);
        canvas.add(sliceCont,new DasRow(canvas, 0, 1),new DasColumn(canvas, 0, 1));
        
    }
    public static void main( String[] args ) {
        new DasSliceControllerDemo().showFrame();
    }
}
