/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.graph;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.ParseException;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import org.das2.graph.DasCanvas;
import org.das2.graph.DasColumn;
import org.das2.graph.DasRow;
import org.das2.graph.DasSliceController;
import org.das2.qds.QDataSet;
import org.das2.qds.ops.Ops;


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
    
    DasSliceController sliceCont;

    public JFrame showFrame() {
        JFrame frame= new JFrame( "Slice Demo");
        frame.add(getContentPane());
        frame.pack();
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        frame.setVisible(true);
//        frame.setSize(800, 800);
        return frame;
    }
    
    public DasSliceControllerDemo() throws ParseException{
        int width = 1000;
        int height = 100;
        
        getContentPane().setLayout(new BorderLayout());
 
        DasCanvas canvas = new DasCanvas(width, height);
        
        getContentPane().add(canvas, BorderLayout.CENTER );
        
        QDataSet qds = Ops.timegen("2009-08-08T11:22:12.123", "0.2 sec", 1000);

       sliceCont = new DasSliceController(Datum.create(1.0, Units.MeV), Datum.create(1.0, Units.MeV));

       DasRow row = new DasRow(canvas, 0, 1);
       DasColumn col = new DasColumn(canvas, 0, 1);
       
       sliceCont.setRow(row);
       sliceCont.setColumn(col);
       
        PropertyChangeListener sliceRangeListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                
                System.err.println("got property change event " + evt.getPropertyName() );
                System.err.println("Value = " + evt.getNewValue());
            }

        };
        
        sliceCont.addPropertyChangeListener(DasSliceController.PROP_LDATUM, sliceRangeListener);
        sliceCont.addPropertyChangeListener(DasSliceController.PROP_RDATUM, sliceRangeListener);
        canvas.add(sliceCont);
        
    }
    
    public DasSliceController getDasSliceController() {
         return sliceCont;
     }

    
     
    public static void main( String[] args ) throws ParseException {
        new DasSliceControllerDemo().showFrame();
    }
}
