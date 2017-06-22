package test.graph;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import org.das2.graph.DasAxis;
import org.das2.graph.DasCanvas;
import org.das2.graph.DasColorBar;
import org.das2.graph.DasColumn;
import org.das2.graph.DasPlot;
import org.das2.graph.DasRow;
import org.das2.graph.GraphUtil;
import org.das2.graph.SpectrogramRenderer;
import org.das2.graph.SpectrogramRenderer.RebinnerEnum;
import org.das2.qds.AbstractDataSet;
import org.das2.qds.DDataSet;
import org.das2.qds.QDataSet;


public class SpectrumTest {

	private static final int T0 = 381888000;
	private static final int DELTAT = 300;
	private static final Units TIME_UNITS = Units.t2000;
	private static final int SPECHEIGHT = 3;
	private static final Double[] a = new Double[] {1.0,2.0,3.0,4.0,5.0,6.0,7.0};
	private static final int E0 = 100;
	private static final int DELTAE = 10;
	private static final Units E_UNITS = Units.eV;
	private static final Units Z_UNITS = Units.hertz;
	
	
	private static List<Double[]> m = new ArrayList<Double[]>();
	static
	{
		for (int irow=0;irow<SPECHEIGHT;irow++) {
			Double[] temp = new Double[a.length];
			for (int icol=0;icol<a.length;icol++){
				temp[icol] = a[icol]*irow;
			}
			m.add(temp);
		}
	}

	
	 
	public static class MySpectrogramData extends AbstractDataSet{

		
		
		private static class MyTimestamps extends AbstractDataSet {
			


			{
				putProperty(QDataSet.UNITS, TIME_UNITS);
				putProperty(QDataSet.LABEL,"Time");
				putProperty(QDataSet.MONOTONIC,true);
			}
			
			@Override
			public int rank() {
				return 1;
			}
			
			@Override
			public int length() {
				return a.length;
			}
			
			
			@Override
			public double value(int i) {
				return T0+i*DELTAT;
			}
		}




		
				
		public MySpectrogramData() {
			QDataSet timestamps = new MyTimestamps();
			putProperty(QDataSet.DEPEND_0,timestamps);
			putProperty(QDataSet.UNITS, Z_UNITS);
			putProperty(QDataSet.LABEL, "Spectrum");
			
			double[] ecenters = new double[SPECHEIGHT];
			for (int i=0;i<ecenters.length;i++) {
				ecenters[i] = E0+i*DELTAE;
			}
			DDataSet dcenters = DDataSet.wrap(ecenters ,E_UNITS);
			putProperty(DEPEND_1, dcenters);

		}


		@Override
		public int rank() {
			return 2;
		}

		@Override
		public int length() {
			return a.length;
		
		}
		
		@Override
		public int length(int spectrogramComponentIndex) {
			return SPECHEIGHT;
		}

		
		@Override
		public double value(int i,int spectrogramComponentIndex) {
			return m.get(spectrogramComponentIndex)[i];
			}

		
	}

	public static void main(String[] args){
				
                SwingUtilities.invokeLater( new Runnable() {
                 public void run() {
                        MySpectrogramData qd = new MySpectrogramData();

        		JFrame top = new JFrame("Spectrogram Test");
                	DasCanvas canvas = new DasCanvas(400,400);
		

                        DasRow row = new DasRow(canvas,0.1,0.9);
                        DasColumn column = DasColumn.create(canvas,null,"7em","100%-10em");

                        DasPlot plot = makePlot(qd);
                        canvas.add(plot,row,column);

                        top.add(canvas);
                        top.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                        top.validate();
                        top.pack();
                        top.setVisible(true);
                    }
                } );

	}

	private static DasPlot makePlot(QDataSet qds) {
		DasAxis xaxis = new DasAxis(new DatumRange(T0,T0+a.length*DELTAT, Units.t2000), DasAxis.BOTTOM);
        xaxis.setLabel("Time");
        
        DasAxis yaxis = new DasAxis(new DatumRange(E0,E0+SPECHEIGHT*DELTAE, Units.eV), DasAxis.LEFT);
		yaxis.setLabel("E");
        
                
        final DasPlot plot = new DasPlot(xaxis, yaxis);
        
        DasAxis zaxis;
        zaxis = GraphUtil.guessZAxis(qds);

        
        DasColorBar colorBar = new DasColorBar(zaxis.getDataMinimum(), zaxis.getDataMaximum(),false);
        colorBar.setLabel("Color");

        SpectrogramRenderer r= new SpectrogramRenderer(null, colorBar);
        r.setRebinner(RebinnerEnum.binAverage);

        
    	r.setDataSet(qds);
    	plot.addRenderer( r );

    	
        return plot;
	}
	
		


}

