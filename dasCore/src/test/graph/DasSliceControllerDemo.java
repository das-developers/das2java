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
import org.das2.graph.DasAxis;
import org.das2.graph.DasCanvas;
import org.das2.graph.DasColorBar;
import org.das2.graph.DasColumn;
import org.das2.graph.DasPlot;
import org.das2.graph.DasRow;
import org.das2.graph.DasSliceController;
import org.das2.graph.SpectrogramRenderer;
import org.das2.qds.QDataSet;
import org.das2.qds.ops.Ops;
import static org.das2.qds.ops.Ops.collapse0;
import static org.das2.qds.ops.Ops.findex;
import static org.das2.qds.ops.Ops.findgen;


/**
 *
 * @author leiffert
 */
public class DasSliceControllerDemo {
    
    JPanel contentPane;
    protected DasPlot plot;

    private synchronized JPanel getContentPane() {
        if (contentPane == null) {
            contentPane = new JPanel();
        }
        return contentPane;
    }
    
    DasSliceController sliceCont;
    SpectrogramRenderer rend;
    QDataSet qds;
    
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
        int height = 1000;
        
        getContentPane().setLayout(new BorderLayout());
 
        DasCanvas canvas = new DasCanvas(width, height);
        
        getContentPane().add(canvas, BorderLayout.CENTER );
        
//        QDataSet qds = Ops.timegen("2009-08-08T11:22:12.123", "0.2 sec", 1000);

       sliceCont = new DasSliceController(Datum.create(1.0, Units.dimensionless), Datum.create(1.0, Units.dimensionless));

       DasRow row = new DasRow(canvas, 0, .1);
       DasColumn col = new DasColumn(canvas, 0, 1);
       
       sliceCont.setRow(row);
       sliceCont.setColumn(col);
       
        PropertyChangeListener sliceRangeListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                
                System.err.println("got property change event " + evt.getPropertyName() );
                System.err.println("Old value = " + evt.getOldValue() + " New Value = " + evt.getNewValue());
                if(evt.getPropertyName().equals("datumRange")){
                    datumRangeChange(evt);
                }
            }

        };
        
        sliceCont.addPropertyChangeListener(DasSliceController.PROP_DATUMRANGE, sliceRangeListener);
//        sliceCont.addPropertyChangeListener(DasSliceController.PROP_LDATUM, sliceRangeListener);
//        sliceCont.addPropertyChangeListener(DasSliceController.PROP_RDATUM, sliceRangeListener);
        canvas.add(sliceCont);
        
      
        DatumRange xrange = DatumRange.newDatumRange(0,10,Units.dimensionless);
        DatumRange yrange= DatumRange.newDatumRange(0,30, Units.dimensionless);
        DatumRange cbrange = DatumRange.newDatumRange(0, 1, Units.dimensionless);
        
        DasColorBar cb = new DasColorBar(cbrange.min(), cbrange.max(), false);
        rend = new SpectrogramRenderer(null, cb);
        rend.setRebinner( SpectrogramRenderer.RebinnerEnum.binAverage );
        
        qds = Ops.ripples(1000,20,30);
        
        rend.setDataSet( qds.slice(1) );
        
        
        DasAxis xaxis = new DasAxis(xrange.min(), xrange.max(), DasAxis.HORIZONTAL);
        DasAxis yaxis = new DasAxis(yrange.min(), yrange.max(), DasAxis.VERTICAL);
        DasRow plotRow = new DasRow(canvas, null, 0.1, 1, 2, -3, 0, 0);
        DasColumn plotCol = new DasColumn(canvas,null, 0, 1, 5, -3, 0, 0);
        DasPlot result = new DasPlot(xaxis, yaxis);
        result.setRow(plotRow);
        result.setColumn(plotCol);
        result.addRenderer(rend);
        result.getColumn().setEmMaximum(-10);
        result.getXAxis().setDatumRange( DatumRange.newDatumRange(0,20,Units.dimensionless) );
        canvas.add(result);
//        plot = GraphUtil.newDasPlot(canvas, xrange, yrange);
        result.getXAxis().setUseDomainDivider(true);
        result.getYAxis().setUseDomainDivider(true);
    }
    
    public DasSliceController getDasSliceController() {
         return sliceCont;
     }

    
    private void datumRangeChange(PropertyChangeEvent e){
        DatumRange oldRange = (DatumRange) e.getOldValue();
        DatumRange newRange = (DatumRange) e.getNewValue();
        
        QDataSet lIndex = findex(findgen(qds.length()), newRange.min());
        QDataSet rIndex = findex(findgen(qds.length()), newRange.max());
//        System.err.println("lIndex length = " + lIndex.length());
//        System.err.println("rIndex length = " + rIndex.length());
        int lowIndex = (int) lIndex.value();
        int upIndex = (int) rIndex.value();
        QDataSet trimmedQds = qds.trim(lowIndex, upIndex + 1);
        trimmedQds = collapse0(trimmedQds);
        System.err.println("trimmed ds rank = " + trimmedQds.rank());
        rend.setDataSet(trimmedQds);
        
    } 
    
    public static void main( String[] args ) throws ParseException {
        new DasSliceControllerDemo().showFrame();
    }
}
