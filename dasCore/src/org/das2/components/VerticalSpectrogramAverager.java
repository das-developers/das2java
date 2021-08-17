/* File: VerticalSpectrogramAverager.java
 * Copyright (C) 2002-2003 The University of Iowa
 * Created by: Jeremy Faden <jbf@space.physics.uiowa.edu>
 *             Jessica Swanner <jessica@space.physics.uiowa.edu>
 *             Edward E. West <eew@space.physics.uiowa.edu>
 *
 * This file is part of the das2 library.
 *
 * das2 is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

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
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.das2.graph.Renderer;
import org.das2.graph.SeriesRenderer;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;
import org.das2.qds.examples.Schemes;
import org.das2.qds.ops.Ops;
import org.das2.util.LoggerManager;

/**
 * show the average of the data over an interval
 * @author jbf
 */
public class VerticalSpectrogramAverager implements DataRangeSelectionListener {
    
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
    
    protected VerticalSpectrogramAverager( DasPlot parent, DasAxis sourceXAxis, DasAxis sourceZAxis ) {
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

    private String mode = "average";

    public static final String PROP_MODE = "mode";

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        String oldMode = this.mode;
        this.mode = mode;
        propertyChangeSupport.firePropertyChange(PROP_MODE, oldMode, mode);
    }

    private transient final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    /**
     * Add PropertyChangeListener.
     *
     * @param listener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Remove PropertyChangeListener.
     *
     * @param listener
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }


    protected void setDataSet( QDataSet ds ) {
       renderer.setDataSet(ds);
    }
    
    public QDataSet getDataSet() {
        return renderer.getDataSet();
    }

    
    public static VerticalSpectrogramAverager createAverager(DasPlot plot, TableDataSetConsumer dataSetConsumer) {
        DasAxis sourceYAxis = plot.getYAxis();
        DasAxis sourceZAxis = dataSetConsumer.getZAxis();
        return new VerticalSpectrogramAverager(plot, sourceYAxis, sourceZAxis);
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
        popupWindow.setTitle("Vertical Interval Averager");
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
        
        JComboBox modeCB= new JComboBox( new String[] {"average","sum" } ); //,"integrate" } );
        modeCB.addActionListener((ActionEvent e) -> {
            VerticalSpectrogramAverager.this.setMode((String) modeCB.getSelectedItem());
            JOptionPane.showMessageDialog( buttonPanel, "Re-slice to update.", "mode updated", JOptionPane.INFORMATION_MESSAGE );
        });
        buttonPanel.add( modeCB );
        
                
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
        Datum xValue1 = e.getMinimum();
        Datum xValue2 = e.getMaximum();
        
        if ( xValue2.equals(xValue1) ) {
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
        
        RebinDescriptor ddX = new RebinDescriptor(xValue1, xValue2, 1, false);
        ddX.setOutOfBoundsAction(RebinDescriptor.MINUSONE);
        AverageTableRebinner rebinner = new AverageTableRebinner();
        
        String title= parentPlot.getTitle().trim();
        
        try {
            if ( xtys.rank()==3 ) {
                QDataSet jds= null;
                    for ( int i=0; i<xtys.length(); i++ ) {
                    QDataSet rebinned = (QDataSet)rebinner.rebin(xtys.slice(i), ddX, null, null);
                    QDataSet ds1 = rebinned.slice(0);
                    jds= org.das2.qds.ops.Ops.append( jds, ds1 );
                }
                renderer.setDataSet(jds);         
            } else {
                QDataSet rebinned;
                switch (mode) {
                    case "average":
                        rebinned = Ops.reduceMean( Ops.trim( ds, Ops.dataset(xValue1), Ops.dataset(xValue2) ), 0 );
                        title= "Averaged " + title;
                        break;
                    case "sum":
                        rebinned = Ops.reduceSum( Ops.trim( ds, Ops.dataset(xValue1), Ops.dataset(xValue2) ), 0 );
                        title= "Summed " + title;
                        break;
                    case "integrate":
                        QDataSet trimmed= Ops.trim( ds, Ops.dataset(xValue1), Ops.dataset(xValue2) );
                        QDataSet dep1= (QDataSet) ds.property(QDataSet.DEPEND_1);
                        if ( dep1==null ) dep1= Ops.indgen(ds.length(0));
                        QDataSet normalize= Ops.reduceSum( Ops.diff( dep1 ), 1 );
                        renderer.setException( new Exception("integrate is not implemented.") );
                        return;
                        //rebinned = Ops.reduceSum( trimmed, 1 );
                        //rebinned= Ops.divide( rebinned, normalize );
                        //title= "Integrated " + title;
                        //break;
                    default:
                        renderer.setException( new Exception("unknown mode in averager: "+mode) );
                        return;
                }
                //QDataSet ds1 = Ops.link( ds.property(QDataSet.DEPEND_0), rebinned );
                QDataSet ds1 = rebinned;
                renderer.setDataSet(ds1);
            }
        } catch (DasException de) {
            //Do nothing.
        }
        
        value= e.getReference();
        
        myPlot.setTitle( title + "!c" + new DatumRange( xValue1, xValue2 ).toString() );
        if ( !myPlot.getXAxis().getLabel().equals( sourceXAxis.getLabel() ) ) {
            myPlot.getXAxis().setLabel( sourceXAxis.getLabel() );
        }
        if ( !myPlot.getYAxis().getLabel().equals( sourceZAxis.getLabel() ) ) {
            myPlot.getYAxis().setLabel( sourceZAxis.getLabel() );
        }
        //eventBirthMilli= e.birthMilli;
    }
    

}
