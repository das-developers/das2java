package test.graph;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.function.Function;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import org.das2.graph.DasAxis;
import org.das2.graph.DasCanvas;
import org.das2.graph.DasColumn;
import org.das2.graph.DasPlot;
import org.das2.graph.DasRow;
import org.das2.graph.LegendPosition;
import org.das2.graph.Renderer;
import org.das2.graph.SeriesRenderer;
import org.das2.qds.DDataSet;

public class DasRightYAxisDemo {

  public DasRightYAxisDemo() {
    super();
  }

  public void run() {
    double xMax = 100.0;
    double yMax = 100.0;
    DasPlot plot = createTestPlot("Show right axis bug", x -> {
      return yMax * (x / xMax) * (x / xMax);
    }, xMax, yMax);

    DasCanvas canvas = new DasCanvas(600, 400);

    DasRow r = new DasRow(canvas, null, 0.0, 1.0, 2, -4, 0, 0);
    DasColumn c = new DasColumn(canvas, null, 0.0, 1.0, 5, -12, 0, 0);

    canvas.add(plot, r, c);

    JFrame mainFrame = new JFrame();
    mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    mainFrame.setPreferredSize(new Dimension(1200, 1024));
    Container container = mainFrame;

    JPanel mainPanel = new JPanel(new GridBagLayout());

    JScrollPane scrollPane = new JScrollPane(mainPanel);
    container.add(scrollPane, BorderLayout.CENTER);

    Dimension mainFrameSize = container.getPreferredSize();
    int minimumMainFrameHeight = container.getMinimumSize().height;

    int scrollBarWidth = scrollPane.getVerticalScrollBar().getPreferredSize().width;
    int scrollBarHeight = scrollPane.getHorizontalScrollBar().getPreferredSize().height;

    Dimension panelSize = new Dimension(mainFrameSize.width - scrollBarWidth,
        mainFrameSize.height - scrollBarHeight - minimumMainFrameHeight);

    mainPanel.setPreferredSize(panelSize);
    GridBagConstraints gbc = new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
        GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
    mainPanel.add(canvas, gbc);

    mainFrame.pack();
    mainFrame.setVisible(true);
  }

  protected DasPlot createTestPlot(String title, Function<Double, Double> function, double xMax,
      double yMax) {
    DasAxis xDasAxis = new DasAxis(new DatumRange(0.0, xMax, Units.dimensionless), DasAxis.BOTTOM);

    // Jeremy, if I use the line with orientation == DasAxis.LEFT, this shows up just fine but with
    // orientation == DasAxis.RIGHT, the plot is not shown:
    // DasAxis yDasAxis = new DasAxis(new DatumRange(0.0, yMax, Units.dimensionless), DasAxis.LEFT);
    DasAxis yDasAxis = new DasAxis(new DatumRange(0.0, yMax, Units.dimensionless), DasAxis.RIGHT);
    DasPlot dasPlot = new DasPlot(xDasAxis, yDasAxis);

    xDasAxis.setLabel("X");
    yDasAxis.setLabel("Y");
    xDasAxis.setVisible(true);
    yDasAxis.setVisible(true);

    dasPlot.setTitle(title);
    dasPlot.setLongTitles(true);

    int xDim = (int) xMax;
    DDataSet dataSet = DDataSet.createRank1(xDim);

    for (int i = 0; i < xDim; ++i) {
      double x = i;
      double y = function.apply(x);
      dataSet.putValue(i, y);
    }
    Renderer renderer = new SeriesRenderer();

    renderer.setDataSet(dataSet);
    renderer.setLegendLabel("I Am Legend (Neville)");
    renderer.setDrawLegendLabel(true);
    dasPlot.addRenderer(renderer);
    dasPlot.setDisplayLegend(true);
    dasPlot.setLegendPosition(LegendPosition.OutsideNE);

    return dasPlot;
  }

  public static void main(String args[]) {

    DasRightYAxisDemo show = new DasRightYAxisDemo();

    show.run();
  }

}
