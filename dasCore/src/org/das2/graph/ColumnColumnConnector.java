package org.das2.graph;

import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.DatumRangeUtil;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.beans.PropertyChangeListener;
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JLayeredPane;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.MenuElement;

/**
 * draws lines connecting two DasPlots, one on top of the other, typically used
 * to show a zoom in above of a context below.
 */
public class ColumnColumnConnector extends DasCanvasComponent {
    
    private DasCanvas parent;
    
    private DasRow topRow;
    private DasRow bottomRow;
    
    private DasPlot topPlot;
    private final DasPlot bottomPlot;

    /**
     * true if the bottom curtain should be drawn
     */
    private boolean bottomCurtainDrawn= true;

    public ColumnColumnConnector( DasCanvas parent, DasPlot topPlot, DasRow topRow, DasPlot bottomPlot ) {
        super( );
        putClientProperty( JLayeredPane.LAYER_PROPERTY, DasCanvas.AXIS_LAYER );
        this.topPlot= topPlot;
        this.topRow= topRow;
        this.bottomPlot= bottomPlot;
        this.parent= parent;
        
        setForeground( Color.LIGHT_GRAY );
        setRow( topRow );
        setColumn( topPlot.getColumn() );
        if ( topRow==null ) topRow= topPlot.getRow();

        PropertyChangeListener pcl= createPropertyChangeListener();

        topPlot.addPropertyChangeListener(pcl);
        topPlot.getXAxis().addPropertyChangeListener(pcl);
        topPlot.getYAxis().addPropertyChangeListener(pcl);
        bottomPlot.addPropertyChangeListener(pcl);
        bottomPlot.getXAxis().addPropertyChangeListener(pcl);

        JPopupMenu mi= this.bottomPlot.getDasMouseInputAdapter().getPrimaryPopupMenu();
        mi.setLabel("Plot Menu");
        
        getDasMouseInputAdapter().addMenuItem( mi );
        MenuElement me= getDasMouseInputAdapter().getPrimaryPopupMenu().getSubElements()[0];
        ((JMenuItem)me.getComponent()).setText("Connector Properties");
    }
    
    private Rectangle getMyBounds() {
        int ytop= topRow.getDMaximum();
        int ybottom= this.bottomCurtainDrawn ? bottomPlot.getRow().getDMaximum() : bottomPlot.getRow().getDMinimum() ;
        int xhigh= Math.max( topPlot.getColumn().getDMaximum(), bottomPlot.getColumn().getDMaximum() );
        int xlow= Math.min( topPlot.getColumn().getDMinimum(), bottomPlot.getColumn().getDMinimum() );
        
        Rectangle result= new Rectangle( xlow, ytop, (xhigh-xlow)+1, (ybottom-ytop)  );
        return result;
    }
    
    public Shape getActiveRegion() {
        return getMyBounds();
    }
    
    public void resize() {
        setBounds(getMyBounds());
    }
    
    private Datum min( Datum d1, Datum d2 ) {
        return d1.lt(d2) ? d1 : d2;
    }
    
    private Datum max( Datum d1, Datum d2 ) {
        return d1.gt(d2) ? d1 : d2;
    }

    private void paintBottomContext( Graphics2D g, Datum context ) {
                g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

        g.translate(-getX(), -getY());

        int xhigh1= (int)topPlot.getXAxis().transform(context);
        int xhigh2= (int)bottomPlot.getXAxis().getColumn().getDMiddle();
        int xlow1= (int)topPlot.getXAxis().transform(context);
        int xlow2= (int)bottomPlot.getXAxis().getColumn().getDMiddle();

        //if ( xhigh1 > xhigh2 ) return;
        paintIt( g, xhigh1, xhigh2, xlow1, xlow2 );
    }

    private void paintTopContext( Graphics2D g, Datum context ) {
                g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

        g.translate(-getX(), -getY());

        int xhigh1= (int)topPlot.getXAxis().getColumn().getDMiddle();
        int xhigh2= (int)bottomPlot.getXAxis().transform(context);
        int xlow1= (int)topPlot.getXAxis().getColumn().getDMiddle();
        int xlow2= (int)bottomPlot.getXAxis().transform(context);

        //if ( xhigh1 > xhigh2 ) return;
        paintIt( g, xhigh1, xhigh2, xlow1, xlow2 );
    }

    private void paintIt( Graphics2D g, int xhigh1, int xhigh2, int xlow1, int xlow2 ) {
        int hlen=3;

        int y1= topRow.getDMaximum()+hlen;
        int y2= bottomPlot.getRow().getDMinimum()-1-hlen;
        int y3= bottomPlot.getRow().getDMinimum()-1;
        int y4= bottomPlot.getRow().getDMaximum();

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
    
    protected void paintComponent(Graphics g1) {

        if ( ! topPlot.getXAxis().getUnits().isConvertableTo( bottomPlot.getXAxis().getUnits() ) ) {
            //kludge for context plots

            // check to see if bottom panel is slice of top
            Matcher m= Pattern.compile(".*!c(.*=)?(.+)").matcher(bottomPlot.getTitle());
            boolean isContext= m.matches();
            if ( isContext ) {
                try {
                    if ( m.groupCount()>1 && m.group(2).length()>15 ) { //15-> yyyy-mm-ddThh:mm
                        Datum context= topPlot.getXAxis().getUnits().parse(m.group(2));
                        Graphics2D g2=(Graphics2D)g1.create();
                        paintBottomContext( g2, context );
                        g2.dispose();
                        return;
                    } else {
                        return;
                    }
                } catch ( ParseException ex ) {
                    ex.printStackTrace();
                    return;
                }

            } else {
                m= Pattern.compile(".*!c(.*=)?(.+)").matcher(topPlot.getTitle());
                isContext= m.matches();
                if ( isContext ) {
                    try {
                        if ( m.groupCount()>1 && m.group(2).length()>15 ) {
                            Datum context= bottomPlot.getXAxis().getUnits().parse(m.group(2));
                            Graphics2D g2=(Graphics2D)g1.create();
                            paintTopContext( g2, context );
                            g2.dispose();
                            return;
                        } else {
                            return;
                        }
                    } catch ( ParseException ex ) {
                        ex.printStackTrace();
                        return;
                    }
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
        
        if ( bottomCurtain && topPlot.getYAxis().getUnits().isConvertableTo( bottomPlot.getYAxis().getUnits() ) ) {
            
            DatumRange drtop= topPlot.getYAxis().getDatumRange();
            DatumRange yaxisRange= bottomPlot.getYAxis().getDatumRange();

            drtop= DatumRangeUtil.sloppyIntersection( yaxisRange, drtop );
            
            int y5,y6;
            
            y5= (int)bottomPlot.getYAxis().transform( drtop.max() );
            y6= (int)bottomPlot.getYAxis().transform( drtop.min() );
            
            if ( curtainOpacityPercent > 0 ) {
                int xLeft= (int)topPlot.getXAxis().getColumn().getDMinimum();
                int xRight= (int)bottomPlot.getXAxis().getColumn().getDMaximum();
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
            public void propertyChange(java.beans.PropertyChangeEvent propertyChangeEvent) {
                bottomCurtainDrawn= topPlot.getXAxis().getUnits().isConvertableTo( bottomPlot.getXAxis().getUnits() );
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
    private java.beans.PropertyChangeSupport propertyChangeSupport =  new java.beans.PropertyChangeSupport(this);
    
    /**
     * Adds a PropertyChangeListener to the listener list.
     * @param l The listener to add.
     */
    public void addPropertyChangeListener(java.beans.PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(l);
    }
    
    /**
     * Removes a PropertyChangeListener from the listener list.
     * @param l The listener to remove.
     */
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
        propertyChangeSupport.firePropertyChange("fillColor", oldFillColor, fillColor);
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
        propertyChangeSupport.firePropertyChange("fill", new Boolean(oldFill), new Boolean(fill));
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
        propertyChangeSupport.firePropertyChange("bottomCurtain", new Boolean(oldBottomCurtain), new Boolean(bottomCurtain));
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
        propertyChangeSupport.firePropertyChange("curtainOpacityPercent", new Integer(oldCurtainOpacityPercent), new Integer(curtainOpacityPercent));
    }
    
}

