package edu.uiowa.physics.pw.das.graph;

import edu.uiowa.physics.pw.das.datum.Datum;
import java.awt.*;
import java.awt.geom.GeneralPath;

public class ColumnColumnConnector extends DasCanvasComponent implements java.beans.PropertyChangeListener {

    private DasCanvas parent;

    private DasRow topRow;
    private DasRow bottomRow;

    private DasPlot topPlot;
    private DasPlot bottomPlot;
    
    private boolean centerbottomColumn= false; // this causes a funny bug
    
    public ColumnColumnConnector( DasCanvas parent, DasPlot topPlot, DasRow topRow, DasPlot bottomPlot ) {
        super( );
        setForeground( Color.LIGHT_GRAY );
        setRow( topRow );
        setColumn( topPlot.getColumn() );
        this.topPlot= topPlot;
        this.topRow= topRow;
        if ( topRow==null ) topRow= topPlot.getRow();
        this.bottomPlot= bottomPlot;
        
        this.parent= parent;
        topPlot.addPropertyChangeListener(this);
        topPlot.getXAxis().addPropertyChangeListener(this);
        bottomPlot.addPropertyChangeListener(this);
        bottomPlot.getXAxis().addPropertyChangeListener(this);
    }

    private Rectangle getMyBounds() {
        int ytop= topRow.getDMaximum();
        int ybottom= bottomPlot.getRow().getDMinimum();
        int xhigh= Math.max( topPlot.getColumn().getDMaximum(), bottomPlot.getColumn().getDMaximum() );
        int xlow= Math.min( topPlot.getColumn().getDMinimum(), bottomPlot.getColumn().getDMinimum() );

        Rectangle result= new Rectangle( xlow, ytop, (xhigh-xlow)+1, (ybottom-ytop)  );
        return result;
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
    
    protected void paintComponent(Graphics g1) {
        
        if ( ! topPlot.getXAxis().getUnits().isConvertableTo( bottomPlot.getXAxis().getUnits() ) ) return;
        
        bottomPlot.addPropertyChangeListener(this);
        
        Graphics2D g= (Graphics2D)g1.create();
        g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

        g.translate(-getX(), -getY());

        int hlen=3;

        int y1= topRow.getDMaximum()+hlen;
        int y2= bottomPlot.getRow().getDMinimum()-1-hlen;
        Datum dlow= max( topPlot.getXAxis().getDataMinimum(), bottomPlot.getXAxis().getDataMinimum() );
        Datum dhigh= min( topPlot.getXAxis().getDataMaximum(), bottomPlot.getXAxis().getDataMaximum() );
        int xhigh1= (int)topPlot.getXAxis().transform(dhigh);
        int xhigh2= (int)bottomPlot.getXAxis().transform(dhigh);
        int xlow1= (int)topPlot.getXAxis().transform(dlow);
        int xlow2= (int)bottomPlot.getXAxis().transform(dlow);

        GeneralPath gp= new GeneralPath();
        GeneralPath fillPath= new GeneralPath();
        
        gp.moveTo( xlow1,y1-hlen );   fillPath.moveTo( xlow1,y1-hlen );  
        gp.lineTo( xlow1,y1 );        fillPath.lineTo( xlow1,y1 );
        gp.lineTo( xlow2,y2 );        fillPath.lineTo( xlow2,y2 );
        gp.lineTo( xlow2,y2+hlen );   fillPath.lineTo( xlow2,y2+hlen );
        
        gp.moveTo( xhigh2,y2+hlen );  fillPath.lineTo( xhigh2,y2+hlen );
        gp.lineTo( xhigh2,y2 );       fillPath.lineTo( xhigh2,y2 );
        gp.lineTo( xhigh1,y1 );       fillPath.lineTo( xhigh1,y1 ); 
        gp.lineTo( xhigh1,y1-hlen );  fillPath.lineTo( xhigh1,y1-hlen );
        
        if ( fill ) {
            g.setColor( fillColor );
            g.fill(fillPath);
        }

        g.setColor( getForeground() );
                
        g.draw( gp );
        
       /* g.draw(new java.awt.geom.Line2D.Double(xlow1,y1-hlen,xlow1,y1));
        g.draw(new java.awt.geom.Line2D.Double(xlow2,y2,xlow2,y2+hlen));
        g.draw(new java.awt.geom.Line2D.Double(xlow1,y1,xlow2,y2));
        g.draw(new java.awt.geom.Line2D.Double(xhigh1,y1-hlen,xhigh1,y1));
        g.draw(new java.awt.geom.Line2D.Double(xhigh2,y2,xhigh2,y2+hlen));
        g.draw(new java.awt.geom.Line2D.Double(xhigh1,y1,xhigh2,y2));*/

        g.dispose();

        getMouseAdapter().paint(g1);
    }

    public void propertyChange(java.beans.PropertyChangeEvent propertyChangeEvent) {
        markDirty();
        update();
    }

    /**
     * Holds value of property fillColor.
     */
    private Color fillColor= new Color( 240,240,240,0 );

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
        propertyChangeSupport.firePropertyChange ("fillColor", oldFillColor, fillColor);
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
        propertyChangeSupport.firePropertyChange ("fill", new Boolean (oldFill), new Boolean (fill));
    }

}

