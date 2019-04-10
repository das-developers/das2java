/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.graph;

import javax.swing.JFrame;
import org.das2.DasException;
import org.das2.dataset.DataSetRebinner;
import org.das2.dataset.RebinDescriptor;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import org.das2.graph.DasColorBar;
import org.das2.graph.SpectrogramRenderer;
import org.das2.graph.SpectrogramRenderer.RebinnerEnum;
import org.das2.qds.QDataSet;
import org.das2.qds.WritableDataSet;
import org.das2.qds.ops.Ops;

/**
 *
 * @author jbf
 */
public class SpectrogramRendererDemoRebinner extends PlotDemo {
    
    public class MyRebinner implements DataSetRebinner {

        @Override
        public QDataSet rebin(QDataSet ds, RebinDescriptor x, RebinDescriptor y, RebinDescriptor z) throws IllegalArgumentException, DasException {
            WritableDataSet result= Ops.zeros( x.numberOfBins(), y.numberOfBins() );
            for ( int i=0; i<x.numberOfBins(); i++ ) { // checker board
                for ( int j=0; j<y.numberOfBins(); j++ ) {
                    if ( (i+j) % 2 ==0 ) {
                        result.putValue( i,j, 1 );
                    }
                }
            }
            return result;
        }
        
    }
    
    public class MyRebinnerEnum extends RebinnerEnum {
    
        public MyRebinnerEnum( String label) {
            super( new MyRebinner(), label );
        }
        
    }
    
    public SpectrogramRendererDemoRebinner() {
        super();

        DasColorBar cb= new DasColorBar( Datum.create(0), Datum.create(1), false );
        SpectrogramRenderer rend= new SpectrogramRenderer(null,cb);
        rend.setRebinner( new MyRebinnerEnum("My Rebinner") );
        
        rend.setDataSet( Ops.ripples(20,20) );

        plot.addRenderer( rend );
        plot.getColumn().setEmMaximum(-10);
        plot.getXAxis().setDatumRange( DatumRange.newDatumRange(0,20,Units.dimensionless) );

    }

    public static void main( String[] args ) {
        JFrame frame= new SpectrogramRendererDemoRebinner().showFrame();
        frame.setSize( 800,800 );
    }
}
