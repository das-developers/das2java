package org.das2.graph;

import org.das2.datum.format.DatumFormatter;
import org.das2.datum.Datum;
import org.das2.util.GrannyTextRenderer;
import java.awt.*;
import java.awt.geom.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * TickLabeller based on the formatting and bounding-box capabilities of the
 * GrannyTextRenderer.  This class by default creates a DatumFormatter for
 * the tickDescriptor it receives, and then uses the grannyFormat method to
 * get the label.  This object is useful as-is, but provides an easy way to
 * get complex labels (e.g. TCAs) by overriding init and getLabel.
 * @see GrannyTextRenderer
 */
public class GrannyTickLabeller implements TickLabeller {
    
    private TickVDescriptor ticks;
    
    private DatumFormatter manualDatumFormatter;
    private DatumFormatter df;
    
    public GrannyTickLabeller() {
    }
    
    /**
     * sets the ticks and DatumFormatter before drawing.
     * @param ticks
     */
    @Override
    public void init(TickVDescriptor ticks) {
        this.ticks= ticks;
        if ( this.manualDatumFormatter==null ) {
            this.df= ticks.getFormatter();
        } else {
            this.df= this.manualDatumFormatter; 
        }
    }
    
    public static final String PROP_FORMATTER= "formatter";
    /**
     * override the formatter in the TickVDescriptor.
     * @param df the formatter to use
     */
    public void setFormatter( DatumFormatter df ) {
        DatumFormatter oldDf= this.manualDatumFormatter;
        this.manualDatumFormatter= df;
        this.df= df;
        propertyChangeSupport.firePropertyChange(PROP_FORMATTER, oldDf, df );
    }
    
    /**
     * the formatter.
     * @return the formatter
     */
    public DatumFormatter getFormatter() {
        return this.manualDatumFormatter;
    }
    
    private double length(Line2D line) {
        double dx= line.getX2()-line.getX1();
        double dy= line.getY2()-line.getY1();
        double dist= Math.sqrt( dx*dx + dy*dy );
        return dist;
    }
    
    private Line2D normalize(Line2D line, double len) {
        // make line segment length len, starting at line.getP1()
        Point2D p1= line.getP1();
        double dx= line.getX2()-line.getX1();
        double dy= line.getY2()-line.getY1();
        double dist= Math.sqrt( dx*dx + dy*dy );
        Line2D result= (Line2D) line.clone();
        result.setLine( p1.getX(), p1.getY(), p1.getX() + dx/dist * len, p1.getY() + dy/dist*len );
        return result;
    }
    
    @Override
    public Rectangle labelMajorTick(Graphics g, int tickNumber, java.awt.geom.Line2D tickLine) {
        GrannyTextRenderer gtr= new GrannyTextRenderer();
        String grannyString= getLabel( tickNumber, ticks.tickV.get(tickNumber) );
        //if ( grannyString.equals("00:50") ) {
        //    System.err.println( "Here 86" );
        //}
        gtr.setString(g, grannyString );
        Rectangle bounds= gtr.getBounds();
        
        // the goal is to position the label such that the tick would intersect the label's center of mass.
        double tickSlope= ( tickLine.getY2()-tickLine.getY1() ) / ( tickLine.getX2()-tickLine.getX1() );
        double labelWidth= bounds.getWidth();
        double labelHeight= bounds.getHeight();
        double labelSlope= labelHeight / labelWidth;
        
        double labelX;
        double labelY;                
        
        int FONT_ASCENT_FUDGE=3;
        int FONT_DESCENT_FUDGE=3; 
        int TICK_PAD=2;  // distance from tick end to the label
        
        tickLine= normalize( tickLine, length(tickLine)+TICK_PAD ); 
        
        if ( Double.isInfinite( tickLine.getP2().getX() ) || Double.isNaN( tickLine.getP2().getX() ) ) {
            throw new IllegalArgumentException("tickLine must have some length");
        }
        if ( labelSlope > Math.abs( tickSlope ) ) { // tick intersects the height of the label bounds.
            if ( tickLine.getX2()>tickLine.getX1() ) {  // e.g. 3 O'Clock
                double rise= tickSlope * labelWidth / 2;
                labelX= tickLine.getX2();
                labelY= tickLine.getY2() - (labelHeight)/2 + gtr.getAscent() + rise;
                //g.setColor( Color.green );
                //((Graphics2D)g).draw( tickLine );
            } else { // e.g. 9 O'Clock
                double rise= - tickSlope * labelWidth / 2;
                labelX= tickLine.getX2() - labelWidth;
                labelY= tickLine.getY2() - labelHeight/2 + gtr.getAscent() + rise;
                //g.setColor( Color.red );
                //((Graphics2D)g).draw( tickLine );
            }
        } else { // tick intersects the width of the label bounds
            if ( tickLine.getY2()<tickLine.getY1() ) {  // e.g. 12 O'Clock
                double run= - labelHeight / tickSlope / 2;
                labelX= tickLine.getX2() + run - labelWidth/2;
                labelY= tickLine.getY2() - gtr.getDescent() + FONT_DESCENT_FUDGE;
                //g.setColor( Color.blue );
                //((Graphics2D)g).draw( tickLine );
            } else { // e.g. 6 O'Clock
                double run= labelHeight / tickSlope / 2;
                labelX= tickLine.getX2() + run - labelWidth/2;
                labelY= tickLine.getY2() + gtr.getAscent() - FONT_ASCENT_FUDGE;
                //g.setColor( Color.ORANGE );
                //((Graphics2D)g).draw( tickLine );
            }
        }
        
        gtr.draw( g, (float)labelX, (float)labelY );              
        
        bounds.translate( (int)labelX, (int)labelY );
        
        return bounds;
        
    }
    
    protected String getLabel(int tickNumber, Datum value) {
        return df.grannyFormat(value); //+ "!cseconds";
    }

    private transient final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    @Override
    public void finished() {
    }
    
}

