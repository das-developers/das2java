/*
 * PolynomialDataSetDescriptor.java
 *
 * Created on March 9, 2005, 11:16 AM
 */

package edu.uiowa.physics.pw.das.dataset.test;

import edu.uiowa.physics.pw.das.DasException;
import edu.uiowa.physics.pw.das.dataset.DataSet;
import edu.uiowa.physics.pw.das.dataset.DataSetDescriptor;
import edu.uiowa.physics.pw.das.dataset.VectorDataSetBuilder;
import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.datum.DatumRange;
import edu.uiowa.physics.pw.das.datum.Units;
import edu.uiowa.physics.pw.das.graph.DasAxis;
import edu.uiowa.physics.pw.das.graph.DasCanvas;
import edu.uiowa.physics.pw.das.graph.DasColumn;
import edu.uiowa.physics.pw.das.graph.DasPlot;
import edu.uiowa.physics.pw.das.graph.DasRow;
import edu.uiowa.physics.pw.das.graph.SymbolLineRenderer;
import edu.uiowa.physics.pw.das.util.DasProgressMonitor;
import javax.swing.JFrame;

/**
 *
 * @author eew
 */
public class PolynomialDataSetDescriptor extends DataSetDescriptor {
    
    private Units xUnits;
    private Units yUnits;
    private double[] c;
    private double minDx;
    private double xOffset;
    private double ymin=Double.NEGATIVE_INFINITY; // hack to limit size of datasets
    
    /** Creates a new instance of PolynomialDataSetDescriptor */
    public PolynomialDataSetDescriptor(double[] c, Units xUnits, Units yUnits, Datum resolution) {
        this.xUnits = xUnits;
        this.yUnits = yUnits;
        this.c = (double[])c.clone();
        minDx = resolution.doubleValue(xUnits.getOffsetUnits());
        xOffset = 0;
    }
    
    public PolynomialDataSetDescriptor(double[] c, Units xUnits, Units yUnits, Datum resolution, Datum xOffset) {
        this(c, xUnits, yUnits, resolution);
        this.xOffset = xOffset.doubleValue(xUnits);
    }
    
    /**
     * limit the range of the calculated function to here.
     */
    public void setYMin( Datum ymin ) {
        this.ymin= ymin.doubleValue(yUnits);
    }

    protected DataSet getDataSetImpl(Datum start, Datum end, Datum resolution, DasProgressMonitor dasProgressMonitor) throws DasException {
        double x0 = start.doubleValue(xUnits) - xOffset;
        double x1 = end.doubleValue(xUnits) - xOffset;
        double dx = resolution == null ? minDx : Math.max(minDx, resolution.doubleValue(xUnits.getOffsetUnits()));
        
        VectorDataSetBuilder builder = new VectorDataSetBuilder(xUnits, yUnits);
        
        double x = x0;
        for (int i = 0; x < x1; i++, x=x0+i*dx) {
            double y= eval(x);
            if ( y>ymin ) builder.insertY(x, eval(x));
        }
        if ( eval(x1)>ymin ) builder.insertY(x1, eval(x1));
        return builder.toVectorDataSet();
    }
    
    private double eval(double x) {
        double y = 0;
        for (int ic = c.length - 1; ic >= 0; ic--) {
            y = y = c[ic] + x * y;
        }
        return y;
    }

    public Units getXUnits() {
        return xUnits;
    }
    
    public static void main(String[] args) {
        
        double[] c = {90.0, 3.0, -1.0};
        PolynomialDataSetDescriptor dsd = new PolynomialDataSetDescriptor(c, Units.dimensionless, Units.dimensionless, Datum.create(1.0));
        
        DasAxis xAxis = new DasAxis(Datum.create(-10.0), Datum.create(10.0), DasAxis.BOTTOM);
        DasAxis yAxis = new DasAxis(Datum.create(0.0), Datum.create(100.0), DasAxis.LEFT);
        DasPlot plot = new DasPlot(xAxis, yAxis);
        plot.addRenderer(new SymbolLineRenderer(dsd));
        DasCanvas canvas = new DasCanvas(400, 400);
        canvas.add(plot, new DasRow(canvas, 0.1, 0.9), new DasColumn(canvas, 0.1, 0.9));
        
        JFrame frame = new JFrame("Polynomial");
        frame.setContentPane(canvas);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
    
}
