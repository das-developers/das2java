
package org.das2.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import org.das2.graph.SymbolLineRenderer;
import org.das2.graph.DasColumn;
import org.das2.graph.DasCanvas;
import org.das2.graph.DasRow;
import org.das2.graph.DasPlot;
import org.das2.graph.DasAxis;
import org.das2.dataset.TableDataSetConsumer;
import org.das2.dataset.AverageTableRebinner;
import org.das2.dataset.RebinDescriptor;
import org.das2.datum.DatumRange;
import org.das2.DasException;
import org.das2.datum.Datum;
import org.das2.event.DataRangeSelectionEvent;
import org.das2.event.DataRangeSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.das2.graph.Renderer;
import org.das2.graph.SeriesRenderer;
import org.das2.qds.DataSetOps;
import org.das2.qds.DataSetUtil;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;
import org.das2.qds.examples.Schemes;
import org.das2.qds.ops.Ops;
import org.das2.util.LoggerManager;

/**
 * show the average of the data over an interval
 * @author jbf
 */
public class HorizontalSpectrogramAverager implements DataRangeSelectionListener {
    
    private static final Logger logger= LoggerManager.getLogger("das2.gui.dmia");
    
    private JDialog popupWindow;
    private final DasPlot parentPlot;
    private DasPlot myPlot;
    private final DasAxis sourceZAxis;
    private final DasAxis sourceXAxis;
    protected Datum value;
    private SeriesRenderer renderer;
    private final Color markColor = new Color(230,230,230);
    
    JPanel buttonPanel;
    List<Action> additionalActions= new ArrayList<>();
    
    protected HorizontalSpectrogramAverager( DasPlot parent, DasAxis sourceXAxis, DasAxis sourceZAxis ) {
        this.sourceZAxis= sourceZAxis;
        this.sourceXAxis= sourceXAxis;
        this.parentPlot= parent;
    }

    private void initPlot() {
        DasAxis xAxis= sourceXAxis.createAttachedAxis( DasAxis.HORIZONTAL );
        DasAxis yAxis = sourceZAxis.createAttachedAxis(DasAxis.VERTICAL);
        myPlot= new DasPlot( xAxis, yAxis);
        renderer= new SymbolLineRenderer();
        renderer.setAntiAliased(true);
        myPlot.addRenderer(renderer);
        myPlot.addRenderer( new Renderer() {
            @Override
            public void render(Graphics2D g, DasAxis xAxis, DasAxis yAxis ) {
                if ( value!=null ) {
                    int ix= (int)myPlot.getXAxis().transform(value);
                    DasRow row= myPlot.getRow();
                    int iy0= (int)row.getDMinimum();
                    int iy1= (int)row.getDMaximum();
                    g.drawLine(ix+3,iy0,ix,iy0+3);
                    g.drawLine(ix-3,iy0,ix,iy0+3);
                    g.drawLine(ix+3,iy1,ix,iy1-3);
                    g.drawLine(ix-3,iy1,ix,iy1-3);

                    g.setColor(markColor);
                    g.drawLine( ix, iy0+4, ix, iy1-4 );
                }
            }
        } );
    }
    
    /**
     * add a button
     * @param a the action for the button.
     */
    public void addAction( Action a ) {
        additionalActions.add(a);
        if ( buttonPanel!=null ) {
            JButton b= new JButton(a);
            buttonPanel.add(b,0);
        }
    }    

    protected void setDataSet( QDataSet ds ) {
       renderer.setDataSet(ds);
    }
    
    public QDataSet getDataSet() {
        return renderer.getDataSet();
    }

    
    public static HorizontalSpectrogramAverager createAverager(DasPlot plot, TableDataSetConsumer dataSetConsumer) {
        DasAxis sourceXAxis = plot.getXAxis();
        DasAxis sourceZAxis = dataSetConsumer.getZAxis();
        return new HorizontalSpectrogramAverager(plot, sourceXAxis, sourceZAxis);
    }
    
    public void showPopup() {
        if (SwingUtilities.isEventDispatchThread()) {
            showPopupImpl();
        } else {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    showPopupImpl();
                }
            };
            SwingUtilities.invokeLater(r);
        }
    }
    
    /** This method should ONLY be called by the AWT event thread */
    private void showPopupImpl() {
        if (popupWindow == null) {
            createPopup();
        }
        popupWindow.setVisible(true);
    }
    
    /**
     * dispose of the popup slicer.
     */
    public void dispose() {
        if ( popupWindow!=null ) {
            popupWindow.setVisible(false);
            popupWindow.dispose();
        }
    }
    
    /**
     * clear the current dataset to avoid units errors.
     */
    public void clear( ) {
        if ( renderer!=null ) this.renderer.setDataSet(null);
    }
            
    
    /** This method should ONLY be called by the AWT event thread */
    private void createPopup() {
        if ( myPlot==null ) {
            initPlot();
        }
        int width = parentPlot.getCanvas().getWidth() / 2;
        int height = parentPlot.getCanvas().getHeight() / 2;
        final DasCanvas canvas = new DasCanvas(width, height);
        DasRow row = new DasRow(canvas, null, 0, 1.0, 3, -5, 0, 0 );
        DasColumn column = new DasColumn(canvas, null, 0, 1.0, 7, -3, 0, 0 );
        canvas.add(myPlot, row, column);
        
        JPanel content = new JPanel(new BorderLayout());
        
        buttonPanel = new JPanel();
        BoxLayout buttonLayout = new BoxLayout(buttonPanel, BoxLayout.X_AXIS);
        buttonPanel.setLayout(buttonLayout);
        
        if ( additionalActions!=null && additionalActions.size()>0 ) {
            for ( Action a: additionalActions ) {
                buttonPanel.add( new JButton(a) );
            }
        }
        
        buttonPanel.add(Box.createHorizontalGlue());

        JButton pdfButton= new JButton( ComponentsUtil.getPdfButtonAction(canvas) );
        buttonPanel.add( pdfButton );

        JButton printButton= new JButton( new AbstractAction("Print...") {
            @Override
            public void actionPerformed( ActionEvent e ) {
                canvas.makeCurrent();
                DasCanvas.PRINT_ACTION.actionPerformed(e);
            }
        });
        buttonPanel.add( printButton );


        JButton close = new JButton("Hide Window");
        close.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                popupWindow.setVisible(false);
            }
        });

        buttonPanel.add(close);
        
        content.add(canvas, BorderLayout.CENTER);
        content.add(buttonPanel, BorderLayout.SOUTH);
        
        Window parentWindow = SwingUtilities.getWindowAncestor(parentPlot);
        popupWindow = new JDialog(parentWindow);
        popupWindow.setTitle("Horizontal Spectrogram Averager");
        popupWindow.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        popupWindow.setContentPane(content);
        popupWindow.pack();
        
        Point parentLocation = new Point( 0, parentPlot.getY() );
        parentLocation.translate( parentPlot.getX()/20, -1 * myPlot.getRow().getDMinimum() );
        SwingUtilities.convertPointToScreen(parentLocation, parentPlot.getCanvas());
        
        //make sure some portion of the slice window is visible on the screen.
        int xx= parentLocation.x + parentPlot.getCanvas().getWidth();
        int yy= parentLocation.y;
        
        Rectangle r= ComponentsUtil.verifyVisible( new Rectangle( xx, yy, width, height ) );
        if ( r!=null ) {
            xx= r.x;
            yy= r.y;
        }
        
        popupWindow.setLocation(xx,yy);
    }
    
    protected boolean isPopupVisible() {
        return ( popupWindow != null && popupWindow.isVisible()) && myPlot.getCanvas() != null;
    }
    
    @Override
    public void dataRangeSelected(DataRangeSelectionEvent e) {
        QDataSet ds = e.getDataSet();

        if ( ds==null ) {
            return;
        } else if ( Schemes.isXYZScatter(ds) ) {
            logger.fine("gridding data to support mouse module");
            ds= Ops.grid(ds);
        } else if ( !SemanticOps.isTableDataSet(ds) ) {
            logger.log(Level.WARNING, "dataset scheme is not supported: {0}", ds);
        }

        QDataSet xtys = (QDataSet)ds;
        Datum yValue1 = e.getMinimum();
        Datum yValue2 = e.getMaximum();
        
        if ( yValue2.equals(yValue1) ) {
            return;
        }

        if (!isPopupVisible()) {
            showPopup();
        } else {
            Rectangle r= ComponentsUtil.verifyVisible( popupWindow.getBounds() );
            if ( r!=null ) {
                popupWindow.setLocation( r.x, r.y );
            }
        }
        
        RebinDescriptor ddY = new RebinDescriptor(yValue1, yValue2, 1, false);
        ddY.setOutOfBoundsAction(RebinDescriptor.MINUSONE);
        
        if ( xtys.rank()==3 ) {
            QDataSet tds1= null;
            for ( int i=0; i<xtys.length(); i++ ) {
                QDataSet bounds = DataSetOps.dependBounds(xtys.slice(i));
                DatumRange yrange= DataSetUtil.asDatumRange( bounds.slice(1), true );
                if ( yrange.contains(yValue1) ) {
                    tds1 = xtys.slice(i);
                    break;
                }
            }
            if ( tds1==null ) return;
            QDataSet rebinned = Ops.reduceMean( Ops.trim1( tds1, Ops.dataset(yValue1), Ops.dataset(yValue2) ), 1 );
            QDataSet ds1 = Ops.link( tds1.property(QDataSet.DEPEND_0), rebinned );
            renderer.setDataSet(ds1);                
        } else {
            QDataSet rebinned = Ops.reduceMean( Ops.trim1( ds, Ops.dataset(yValue1), Ops.dataset(yValue2) ), 1 );
            QDataSet ds1 = Ops.link( ds.property(QDataSet.DEPEND_0), rebinned );
            renderer.setDataSet(ds1);
        }

        value= e.getReference();

        String title= parentPlot.getTitle().trim();
        if ( title.length()>0 ) {
            title= "Averaged " + title+"!c";
        }
        
        myPlot.setTitle( title + new DatumRange( yValue1, yValue2 ).toString() );
        if ( !myPlot.getYAxis().getLabel().equals( sourceXAxis.getLabel() ) ) {
            myPlot.getYAxis().setLabel( sourceXAxis.getLabel() );
        }
        if ( !myPlot.getYAxis().getLabel().equals( sourceZAxis.getLabel() ) ) {
            myPlot.getYAxis().setLabel( sourceZAxis.getLabel() );
        }
        //eventBirthMilli= e.birthMilli;
    }
    

}
