package edu.uiowa.physics.pw.das.graph;

import java.awt.*;

public class RowRowConnector extends DasCanvasComponent implements java.beans.PropertyChangeListener {
    
    private DasCanvas parent;
    
    private DasRow leftRow;   
    private DasRow rightRow;

    private DasColumn leftColumn;   
    private DasColumn rightColumn;

    public RowRowConnector( DasCanvas parent, DasRow leftRow, DasRow rightRow, DasColumn leftColumn, DasColumn rightColumn ) {        
        this.leftRow= leftRow;
        this.rightRow= rightRow;
        this.leftColumn= leftColumn;
        this.rightColumn= rightColumn;
        this.parent= parent;
        leftRow.addPropertyChangeListener(this);        
        rightRow.addPropertyChangeListener(this);        
        rightColumn.addPropertyChangeListener(this);        
        leftColumn.addPropertyChangeListener(this);        
    }
    
    public Rectangle getBounds() {
        int xleft= leftColumn.getDMaximum();
        int xright= rightColumn.getDMaximum();
        int ylow= Math.max( leftRow.getDMaximum(), rightRow.getDMaximum() );        
        int yhigh= Math.min( leftRow.getDMinimum(), rightRow.getDMinimum() );
                
        Rectangle result= new Rectangle( xleft, yhigh, (xright-xleft), (ylow-yhigh+2) );                
        
        return result;
    }
        
    public void setLeftRow( DasRow row ) {
        this.leftRow= row;
        repaint();
    }
        
    public void resize() {
        setBounds(getBounds());
    }
    
    protected void paintComponent(Graphics g1) {        
        Graphics2D g= (Graphics2D)g1.create();
        g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        g.translate(-getX(), -getY());
        
        int hlen=3;
        
        int x1= leftColumn.getDMaximum()+hlen;
        int x2= rightColumn.getDMaximum()-hlen;
        int ylow1= leftRow.getDMaximum();
        int ylow2= rightRow.getDMaximum();
        int yhigh1= leftRow.getDMinimum();
        int yhigh2= rightRow.getDMinimum();
        
        g.setColor(Color.lightGray);
        g.draw(new java.awt.geom.Line2D.Double(x1-hlen,ylow1,x1,ylow1));
        g.draw(new java.awt.geom.Line2D.Double(x2,ylow2,x2+hlen,ylow2));
        g.draw(new java.awt.geom.Line2D.Double(x1,ylow1,x2,ylow2));
        g.draw(new java.awt.geom.Line2D.Double(x1-hlen,yhigh1,x1,yhigh1));
        g.draw(new java.awt.geom.Line2D.Double(x2,yhigh2,x2+hlen,yhigh2));
        g.draw(new java.awt.geom.Line2D.Double(x1,yhigh1,x2,yhigh2));
        
        g.dispose();
        
        getMouseAdapter().paint(g1);
    }
    
    public void propertyChange(java.beans.PropertyChangeEvent propertyChangeEvent) {
        update();
    }        
    
}

