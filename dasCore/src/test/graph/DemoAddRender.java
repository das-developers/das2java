/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.graph;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import org.das2.graph.DasCanvas;
import org.das2.graph.DasPlot;
import org.das2.graph.GraphUtil;
import org.das2.graph.Renderer;
import org.das2.qds.QDataSet;
import org.das2.qds.util.DataSetBuilder;

/**
 *
 * @author jbf
 */
public class DemoAddRender {
    public static void main( String[] args ) {
        DasCanvas c= new DasCanvas(480,480);
        DasPlot p= GraphUtil.newDasPlot( c, DatumRange.newDatumRange(0,10,Units.dimensionless), 
                DatumRange.newDatumRange(0,10,Units.dimensionless) );
        Renderer myRenderer= new MyRenderer();
        DataSetBuilder build=new DataSetBuilder(2,100,4);
        build.setUnits(0,Units.dimensionless);
        build.setUnits(1,Units.dimensionless);
        build.setUnits(2,Units.dimensionless);
        build.putValue( -1, 0, 5 ); build.putValue( -1, 1, 5 ); build.putValue( -1, 2, 3 ); build.putValue( -1, 3, 0 );
        build.nextRecord();
        build.putValue( -1, 0, 6 ); build.putValue( -1, 1, 6 ); build.putValue( -1, 2, 2 ); build.putValue( -1, 3, 0xFF0000 );
        build.nextRecord();
        build.putValue( -1, 0, 3 ); build.putValue( -1, 1, 3 ); build.putValue( -1, 2, 1 ); build.putValue( -1, 3, 0xFFFF00 );
        build.nextRecord();
        QDataSet myds= build.getDataSet();
        myRenderer.setDataSet( myds );
        p.addRenderer( myRenderer );
        JDialog d= new JDialog();
        d.add(c);
        d.pack();
        d.setVisible(true);
    }
}
