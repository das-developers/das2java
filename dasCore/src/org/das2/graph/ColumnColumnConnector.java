package org.das2.graph;

import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.DatumRangeUtil;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.beans.PropertyChangeListener;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.MenuElement;
import javax.swing.event.MouseInputAdapter;
import org.das2.DasApplication;
import org.das2.datum.Units;

/**
 * draws lines connecting two DasPlots, one on top of the other, typically used
 * to show a zoom in above of a context below.
 */
public final class ColumnColumnConnector extends DasCanvasComponent {
    
    public static final String PROP_FILL_COLOR = "fillColor";
    public static final String PROP_FILL = "fill";
    public static final String PROP_BOTTOM_CURTAIN = "bottomCurtain";
    public static final String PROP_CURTAIN_OPACITY_PERCENT = "curtainOpacityPercent";
        
    private final DasRow topRow;
    
    private final DasPlot topPlot;
    private final DasPlot bottomPlot;

    /**
     * true if the bottom curtain should be drawn
     */
    private boolean bottomCurtainDrawn= true;

    public ColumnColumnConnector( final DasCanvas parent, DasPlot topPlot, DasRow topRow, final DasPlot bottomPlot ) {
        super( );
        putClientProperty( JLayeredPane.LAYER_PROPERTY, DasCanvas.AXIS_LAYER );
        this.topPlot= topPlot;
        this.topRow= topRow;
        this.bottomPlot= bottomPlot;
        
        setForeground( Color.LIGHT_GRAY );
        if ( topRow==null ) topRow= topPlot.getRow();
        setRow( topRow );
        setColumn( topPlot.getColumn() );

        PropertyChangeListener pcl= createPropertyChangeListener();

        topPlot.addPropertyChangeListener(pcl);
        topPlot.getXAxis().addPropertyChangeListener(pcl);
        topPlot.getYAxis().addPropertyChangeListener(pcl);
        bottomPlot.addPropertyChangeListener(pcl);
        bottomPlot.getXAxis().addPropertyChangeListener(pcl);

        if ( !DasApplication.getDefaultApplication().isHeadless() ) {
            JPopupMenu mi= this.bottomPlot.getDasMouseInputAdapter().getPrimaryPopupMenu();
            mi.setLabel("Plot Menu");
        
            JPopupMenu delegateMenu= makeDelegateMenu( mi );
            
            getDasMouseInputAdapter().addMenuItem( delegateMenu );
            MenuElement me= getDasMouseInputAdapter().getPrimaryPopupMenu().getSubElements()[0];
            ((JMenuItem)me.getComponent()).setText("Connector Properties");

            addMouseListener(new MouseInputAdapter() {

                @Override
                public void mousePressed(MouseEvent e) {
                    Point p= new Point( getX() + e.getX(), getY() + e.getY() );
                    bottomPlot.getDasMouseInputAdapter().setMousePressPositionOnCanvas(p);
                    DasPlot bot= ColumnColumnConnector.this.bottomPlot;
                    int ir = bot.findRendererAt(getX() + e.getX(), getY() + e.getY());
                    Renderer r = null;
                    if ( ir>-1 ) {
                        r= (Renderer) bot.getRenderer(ir);
                    }
                    bot.setFocusRenderer(r);
                }
            });

        }
    }
    
    
    private JMenu makeDelegateMenu( JMenu mi ) {
        JMenu result= new JMenu(mi.getText());
        for ( Component c: mi.getMenuComponents() ) {
            if ( c instanceof JMenuItem ) {
                JMenuItem tmi= (JMenuItem)c;
                JMenuItem cmi= new JMenuItem(tmi.getAction());
                cmi.setText(tmi.getText());
                result.add(cmi);
            } else if ( c instanceof JSeparator ) {
                result.add( new JSeparator() );
            } else if ( c instanceof JMenu ) {
                result.add( makeDelegateMenu( ((JMenu)c) ) );
            }
        }
        return result;
    }
    
    /**
     * create a menu that delegates to the menu underneath
     * @param mi
     * @return 
     */
    private JPopupMenu makeDelegateMenu( JPopupMenu mi ) {
        
        JPopupMenu result= new JPopupMenu();
        int i=0;
        for ( Component c: mi.getComponents() ) {
            i=i+1;
            if ( c instanceof javax.swing.JMenu ) {
                result.add( makeDelegateMenu( ((JMenu)c) ) );
            } else if ( c instanceof JCheckBoxMenuItem ) {
                //drop it--it's a mouse module...
            } else if ( c instanceof JMenuItem ) {
                JMenuItem tmi= (JMenuItem)c;
                JMenuItem cmi= new JMenuItem(tmi.getAction());
                cmi.setText(tmi.getText());
                result.add(cmi);
            } else if ( c instanceof JSeparator ) {
                result.add( new JSeparator() );
            } 
        }
        return result;
    }
    
    
    private Rectangle getMyBounds() {
        int ytop= topRow.getDMaximum();
        int ybottom= this.bottomCurtainDrawn ? bottomPlot.getRow().getDMaximum() : bottomPlot.getRow().getDMinimum() ;
        int xhigh= Math.max( topPlot.getColumn().getDMaximum(), bottomPlot.getColumn().getDMaximum() );
        int xlow= Math.min( topPlot.getColumn().getDMinimum(), bottomPlot.getColumn().getDMinimum() );
        
        Rectangle result= new Rectangle( xlow, ytop, (xhigh-xlow)+1, (ybottom-ytop)  );
        return result;
    }
    
    @Override
    public Shape getActiveRegion() {
        return getMyBounds();
    }
    
    @Override
    public void resize() {
        setBounds(getMyBounds());
    }
    
    private Datum min( Datum d1, Datum d2 ) {
        return d1.lt(d2) ? d1 : d2;
    }
    
    private Datum max( Datum d1, Datum d2 ) {
        return d1.gt(d2) ? d1 : d2;
    }

    private void paintBottomContext( Graphics2D g, DatumRange context ) {
                g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

        g.translate(-getX(), -getY());

        int xhigh1= (int)topPlot.getXAxis().transform(context.min());
        int xhigh2= (int)bottomPlot.getXAxis().getColumn().getDMiddle();
        int xlow1= (int)topPlot.getXAxis().transform(context.min());
        int xlow2= (int)bottomPlot.getXAxis().getColumn().getDMiddle();

        //if ( xhigh1 > xhigh2 ) return;
        paintIt( g, xhigh1, xhigh2, xlow1, xlow2 );
    }

    private void paintTopContext( Graphics2D g, DatumRange context ) {
                g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

        g.translate(-getX(), -getY());

        int xhigh1= (int)topPlot.getXAxis().getColumn().getDMiddle();
        int xhigh2= (int)bottomPlot.getXAxis().transform(context.min());
        int xlow1= (int)topPlot.getXAxis().getColumn().getDMiddle();
        int xlow2= (int)bottomPlot.getXAxis().transform(context.min());

        //if ( xhigh1 > xhigh2 ) return;
        paintIt( g, xhigh1, xhigh2, xlow1, xlow2 );
    }

    private void paintIt( Graphics2D g, int xhigh1, int xhigh2, int xlow1, int xlow2 ) {
        int hlen=3;

        int y1= topRow.getDMaximum()+hlen;
        int y2= bottomPlot.getRow().getDMinimum()-1-hlen;
        int y3= bottomPlot.getRow().getDMinimum()-1;

        GeneralPath gp= new GeneralPath();
        GeneralPath fillPath= new GeneralPath();

        gp.moveTo( xlow1,y1-hlen );                   fillPath.moveTo( xlow1,y1-hlen );
        gp.lineTo( xlow1,y1 );                        fillPath.lineTo( xlow1,y1 );
        gp.lineTo( xlow2,y2 );                        fillPath.lineTo( xlow2,y2 );
        gp.lineTo( xlow2,y3 );                        fillPath.lineTo( xlow2,y3 );
        gp.moveTo( xhigh2, y3 );
        fillPath.lineTo( xhigh2,y3 );
        gp.lineTo( xhigh2,y2 );       fillPath.lineTo( xhigh2,y2 );
        gp.lineTo( xhigh1,y1 );       fillPath.lineTo( xhigh1,y1 );
        gp.lineTo( xhigh1,y1-hlen );  fillPath.lineTo( xhigh1,y1-hlen );

        if ( fill ) {
            g.setColor( fillColor );
            g.fill(fillPath);
        }

        g.setColor( getForeground() );

        g.draw( gp );

    }

    @Override
    protected void paintComponent(Graphics g1) {

        if ( ! topPlot.getXAxis().getUnits().isConvertibleTo( bottomPlot.getXAxis().getUnits() ) ) {
            //context plots
            // check to see if bottom panel is slice of top
            DatumRange bottomContext= bottomPlot.getDisplayContext(); //TODO: this is not a closed-loop system.  We should indicate timerange found in dataset.
            DatumRange topContext= topPlot.getDisplayContext();

            boolean topIsInterestingContext= topContext!=null && topContext.getUnits()!=Units.dimensionless;

            boolean useTop= bottomContext==null || ( bottomContext.getUnits()==Units.dimensionless && topIsInterestingContext );

            //problem: Autoplot uses "0 to 100" as the default context.
            //problem: the context property is the setting for the axis, not the feedback.

            if ( !useTop ) {
                boolean isContext= bottomContext!=null && topPlot.getXAxis().getUnits().isConvertibleTo( bottomContext.getUnits() );
                if ( isContext ) {
                     Graphics2D g2=(Graphics2D)g1.create();
                     paintBottomContext( g2, bottomContext );
                     g2.dispose();
                     return;
                }
            } else {
                boolean isContext= topContext!=null && bottomPlot.getXAxis().getUnits().isConvertibleTo( topContext.getUnits() );
                bottomContext= topPlot.getDisplayContext();
                if ( isContext ) {
                    Graphics2D g2=(Graphics2D)g1.create();
                    paintTopContext( g2, bottomContext );
                    g2.dispose();
                    return;
                } else {
                    return;
                }
            }
        }

        Graphics2D g= (Graphics2D)g1.create();
        g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        
        g.translate(-getX(), -getY());
        
        int hlen=3;
        
        int y1= topRow.getDMaximum()+hlen;
        int y2= bottomPlot.getRow().getDMinimum()-1-hlen;
        int y3= bottomPlot.getRow().getDMinimum()-1;
        int y4= bottomPlot.getRow().getDMaximum();

        if ( ! topPlot.getXAxis().getUnits().isConvertibleTo( bottomPlot.getXAxis().getUnits() ) ) {
            // transition state (?) that caused failure of autoplot-test500 005.
            return;
        }
        Datum dlow= max( topPlot.getXAxis().getDataMinimum(), bottomPlot.getXAxis().getDataMinimum() );
        Datum dhigh= min( topPlot.getXAxis().getDataMaximum(), bottomPlot.getXAxis().getDataMaximum() );
        int xhigh1= (int)topPlot.getXAxis().transform(dhigh);
        int xhigh2= (int)bottomPlot.getXAxis().transform(dhigh);
        int xlow1= (int)topPlot.getXAxis().transform(dlow);
        int xlow2= (int)bottomPlot.getXAxis().transform(dlow);
        
        //if ( xhigh1 > xhigh2 ) return;
        
        GeneralPath gp= new GeneralPath();
        GeneralPath fillPath= new GeneralPath();
        
        gp.moveTo( xlow1,y1-hlen );                   fillPath.moveTo( xlow1,y1-hlen );
        gp.lineTo( xlow1,y1 );                        fillPath.lineTo( xlow1,y1 );
        gp.lineTo( xlow2,y2 );                        fillPath.lineTo( xlow2,y2 );
        gp.lineTo( xlow2,y3 );                        fillPath.lineTo( xlow2,y3 );
        gp.moveTo( xhigh2, y3 );
        fillPath.lineTo( xhigh2,y3 );
        gp.lineTo( xhigh2,y2 );       fillPath.lineTo( xhigh2,y2 );
        gp.lineTo( xhigh1,y1 );       fillPath.lineTo( xhigh1,y1 );
        gp.lineTo( xhigh1,y1-hlen );  fillPath.lineTo( xhigh1,y1-hlen );
        
        if ( fill ) {
            g.setColor( fillColor );
            g.fill(fillPath);
        }
        
        g.setColor( getForeground() );
        
        g.draw( gp );
        
        if ( bottomCurtain && topPlot.getYAxis().getUnits().isConvertibleTo( bottomPlot.getYAxis().getUnits() ) ) {
        
            DatumRange drtop= topPlot.getYAxis().getDatumRange();
            DatumRange yaxisRange= bottomPlot.getYAxis().getDatumRange();

            drtop= DatumRangeUtil.sloppyIntersection( yaxisRange, drtop );
            
            int y5,y6;
            
            if ( showYPosition ) {
                y5= (int)( bottomPlot.getYAxis().transform( drtop.max() )+0.00001 );
                y6= (int)( bottomPlot.getYAxis().transform( drtop.min() )+0.00001 );
            } else {
                y5= bottomPlot.getYAxis().getRow().getDMinimum();
                y6= bottomPlot.getYAxis().getRow().getDMaximum();
            }
            
            if ( curtainOpacityPercent > 0 ) {
                Color canvasColor= getCanvas().getBackground();
                Color curtainColor= new Color( canvasColor.getRed(), canvasColor.getGreen(), canvasColor.getBlue(),
                        curtainOpacityPercent * 255 / 100 );
                
                GeneralPath gpfill= new GeneralPath( DasRow.toRectangle(bottomPlot.getRow(),bottomPlot.getColumn() ) );
                gpfill.append( new Rectangle( xlow2, y5, xhigh2-xlow2, y6-y5 ), false );
                gpfill.setWindingRule( GeneralPath.WIND_EVEN_ODD );
                
                g.setColor( curtainColor );
                g.fill( gpfill );
                //g.fillRect( xLeft, y3+1, xlow2-xLeft, y4-y3-1 );
                //g.fillRect( xhigh2+1, y3+1, xRight-xhigh2-1, y4-y3-1 );
                g.setColor( getForeground() );
            }
            
            if ( yaxisRange.contains(drtop.max()) )g.drawLine( xlow2, y5, xhigh2, y5 );
            if ( yaxisRange.contains(drtop.min()) && drtop.min().gt( yaxisRange.min() ) ) g.drawLine( xlow2, y6, xhigh2, y6 );
            g.drawLine( xlow2, y3, xlow2, y4 );
            g.drawLine( xhigh2, y3, xhigh2, y4 );
            
            
        }
        
        g.dispose();
        
        getDasMouseInputAdapter().paint(g1);
    }

    private PropertyChangeListener createPropertyChangeListener() {
        return new PropertyChangeListener() {
            @Override
            public void propertyChange(java.beans.PropertyChangeEvent propertyChangeEvent) {
                bottomCurtainDrawn= topPlot.getXAxis().getUnits().isConvertibleTo( bottomPlot.getXAxis().getUnits() );
                markDirty();
                update();
            }
        };
    }
    
    /**
     * Holds value of property fillColor.
     */
    private Color fillColor= new Color( 240,240,240,255 );
    
    /**
     * Utility field used by bound properties.
     */
    private final java.beans.PropertyChangeSupport propertyChangeSupport =  new java.beans.PropertyChangeSupport(this);
    
    /**
     * Adds a PropertyChangeListener to the listener list.
     * @param l The listener to add.
     */
    @Override
    public void addPropertyChangeListener(java.beans.PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(l);
    }
    
    /**
     * Removes a PropertyChangeListener from the listener list.
     * @param l The listener to remove.
     */
    @Override
    public void removePropertyChangeListener(java.beans.PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener(l);
    }
    
    /**
     * Getter for property fillColor.
     * @return Value of property fillColor.
     */
    public Color getFillColor() {
        return this.fillColor;
    }
    
    /**
     * Setter for property fillColor.
     * @param fillColor New value of property fillColor.
     */
    public void setFillColor(Color fillColor) {
        Color oldFillColor = this.fillColor;
        this.fillColor = fillColor;
        repaint();
        propertyChangeSupport.firePropertyChange(PROP_FILL_COLOR, oldFillColor, fillColor);
    }
    
    /**
     * Holds value of property fill.
     */
    private boolean fill=false;
    
    /**
     * Getter for property fill.
     * @return Value of property fill.
     */
    public boolean isFill() {
        return this.fill;
    }
    
    /**
     * Setter for property fill.
     * @param fill New value of property fill.
     */
    public void setFill(boolean fill) {
        boolean oldFill = this.fill;
        this.fill = fill;
        repaint();
        propertyChangeSupport.firePropertyChange(PROP_FILL, Boolean.valueOf(oldFill), Boolean.valueOf(fill));
    }
    
    /**
     * Holds value of property bottomCurtain.
     */
    private boolean bottomCurtain;
    
    /**
     * Getter for property bottomCurtain.
     * @return Value of property bottomCurtain.
     */
    public boolean isBottomCurtain() {
        return this.bottomCurtain;
    }
    
    /**
     * Setter for property bottomCurtain.
     * @param bottomCurtain New value of property bottomCurtain.
     */
    public void setBottomCurtain(boolean bottomCurtain) {
        boolean oldBottomCurtain = this.bottomCurtain;
        this.bottomCurtain = bottomCurtain;
        repaint();
        propertyChangeSupport.firePropertyChange(PROP_BOTTOM_CURTAIN, Boolean.valueOf(oldBottomCurtain), Boolean.valueOf(bottomCurtain));
    }
    
    /**
     * Holds value of property curtainOpacityPercent.
     */
    private int curtainOpacityPercent= 40;
    
    /**
     * Getter for property curtainOpacityPercent.
     * @return Value of property curtainOpacityPercent.
     */
    public int getCurtainOpacityPercent() {
        return this.curtainOpacityPercent;
    }
    
    /**
     * Setter for property curtainOpacityPercent.
     * @param curtainOpacityPercent New value of property curtainOpacityPercent.
     */
    public void setCurtainOpacityPercent(int curtainOpacityPercent) {
        int oldCurtainOpacityPercent = this.curtainOpacityPercent;
        this.curtainOpacityPercent = Math.max( 0, Math.min( 100, curtainOpacityPercent ) );
        repaint();
        propertyChangeSupport.firePropertyChange(PROP_CURTAIN_OPACITY_PERCENT, oldCurtainOpacityPercent, curtainOpacityPercent );
    }
    
    private boolean showYPosition = true;
    public static final String PROP_SHOWYPOSITION = "showYPosition";

    public boolean isShowYPosition() {
        return showYPosition;
    }

    /**
     * don't indicate the y axis position, for example if relating data with the time axes but different Y units.
     * @param showYPosition 
     */
    public void setShowYPosition(boolean showYPosition) {
        boolean oldShowYPosition = this.showYPosition;
        this.showYPosition = showYPosition;
        repaint();
        propertyChangeSupport.firePropertyChange(PROP_SHOWYPOSITION, oldShowYPosition, showYPosition);
    }

}

