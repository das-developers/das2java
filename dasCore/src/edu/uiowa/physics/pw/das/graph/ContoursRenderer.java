/*
 * ContoursRenderer.java
 *
 * Created on December 7, 2007, 2:47 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.uiowa.physics.pw.das.graph;

import edu.uiowa.physics.pw.das.DasException;
import edu.uiowa.physics.pw.das.components.propertyeditor.Displayable;
import edu.uiowa.physics.pw.das.dataset.ClippedTableDataSet;
import edu.uiowa.physics.pw.das.dataset.TableDataSet;
import edu.uiowa.physics.pw.das.dataset.VectorDataSet;
import edu.uiowa.physics.pw.das.datum.DatumVector;
import edu.uiowa.physics.pw.das.datum.Units;
import edu.uiowa.physics.pw.das.math.Contour;
import org.das2.util.monitor.DasProgressMonitor;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Renderer for making contour plots
 * @author jbf
 */
public class ContoursRenderer extends Renderer implements Displayable {
    
    /** Creates a new instance of ContoursRenderer */
    public ContoursRenderer() {
    }
    
    GeneralPath[] paths;
    String[] pathLabels;
    
    public synchronized void render(Graphics g1, DasAxis xAxis, DasAxis yAxis, DasProgressMonitor mon) {
        
        Graphics2D g= (Graphics2D)g1;
        //g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        
        if ( paths==null ) return;
        
        g.setColor( color );
        
        if ( drawLabels ) {
            Area labelClip= paintLabels(g);
            labelClip.transform( AffineTransform.getTranslateInstance( parent.getX(), parent.getY() ) );
            Shape rclip= g.getClip()==null ? new Rectangle(parent.getX(), parent.getY(),parent.getWidth(),parent.getHeight() ) : g.getClip();
            Area clip= new Area( rclip );
            clip.subtract( labelClip );
            g.setClip( clip );
            //g.draw( labelClip );
            
        }
        
        for ( int i=0; i<paths.length; i++ ) {
            if ( paths[i]!=null ) {
                g.draw( paths[i] );
            }
        }

    }
    
    /**
     * returns clip, in the canvas reference frame
     */
    private Area paintLabels(final Graphics2D g) {
        
        Area clip= new Area( );
        
        Font font0= g.getFont();
        
        // do labels
        AffineTransform at0= g.getTransform();
        Font font= font0.deriveFont(8f);
        
        g.setFont( font );
        
        String[] cons= this.contours.trim().split(",");
        
        for ( int i=0; i<paths.length; i++ ) {
            if ( paths[i]==null ) continue;
            
            String label= pathLabels[i];
            GeneralPath p= paths[i];
            
            if ( p!=null ) {
                
                PathIterator it1= p.getPathIterator(null);
                PathIterator it2= p.getPathIterator(null);
                
                while ( !it1.isDone() ) {
                    
                    double len= GraphUtil.pointsAlongCurve( it1, null, null, null, true );
                    
                    int nlabel= (int)Math.floor( 0.5 + len / this.labelCadence );
                    
                    double r= ( len - nlabel * ( labelCadence - 1 ) ) / 2;
                    
                    if ( nlabel==0 ) {
                        double len2= GraphUtil.pointsAlongCurve( it2, null, null, null, true );
                        
                    } else {
                        double[] lens= new double[nlabel];
                        for ( int ilabel=0; ilabel<nlabel; ilabel++ ) {
                            lens[ilabel]= r + labelCadence * ilabel;
                        }
                        Point2D.Double[] points= new Point2D.Double[nlabel];
                        double[] orient= new double[nlabel];
                        
                        double len3= GraphUtil.pointsAlongCurve( it2, lens, points, orient, true );
                        
                        for ( int ilabel=0; ilabel<nlabel; ilabel++ ) {
                            AffineTransform at= new AffineTransform(at0);
                            at.translate( points[ilabel].x, points[ilabel].y );
                            at.rotate( orient[ilabel] );
                            g.setTransform( at );
                            
                            Rectangle2D sbounds= g.getFontMetrics().getStringBounds(label,g);
                            double w= sbounds.getWidth();
                            
                            GeneralPath rect= new GeneralPath( sbounds );
                            rect.transform( AffineTransform.getTranslateInstance( -w/2, 0 ) );
                            rect.transform( at );
                            clip.add( new Area( rect ) );
                            
                            
                            g.setColor( parent.getForeground() );
                            g.drawString(label,(int)(-w/2),0);
                        }
                        
                    }
                }
            }
        }
        g.setTransform(at0);
        
        return clip;
    }
    
    
    protected void installRenderer() {
    }
    
    protected void uninstallRenderer() {
    }
    
    protected Element getDOMElement(Document document) {
        return null;
    }
    
    public Icon getListIcon() {
        return new ImageIcon( SpectrogramRenderer.class.getResource("/images/icons/contoursRenderer.png" ) );
    }
        
    public synchronized  void updatePlotImage(DasAxis xAxis, DasAxis yAxis, DasProgressMonitor monitor) throws DasException {
        super.updatePlotImage(xAxis, yAxis, monitor);
        
        TableDataSet tds= (TableDataSet) getDataSet();
        
        if ( tds==null ) return;
        
        tds= new ClippedTableDataSet( tds, xAxis.getDatumRange(), yAxis.getDatumRange() );
        
        Units units= tds.getZUnits();
        
        String[] cons= this.contours.trim().split(",");
        double[] dcons= new double[cons.length];
        for ( int i=0; i<cons.length; i++ ) {
            if ( cons[i].trim().equals("") ) continue;
            double c= Double.parseDouble( cons[i] );
            dcons[i]= c;
        }
        DatumVector dv= DatumVector.newDatumVector(dcons,tds.getZUnits());
        
        VectorDataSet vds= Contour.contour( tds, dv );
        
        paths= new GeneralPath[dv.getLength()];
        
        double d0= units.getFillDouble();
        int ii=-1;
        
        VectorDataSet xds= (VectorDataSet) vds.getPlanarView( Contour.PLANE_X );
        VectorDataSet yds= (VectorDataSet) vds.getPlanarView( Contour.PLANE_Y );
        
        Units xunits= xAxis.getUnits();
        Units yunits= yAxis.getUnits();
        
        ArrayList list= new ArrayList();
        ArrayList labels= new ArrayList();
        
        GeneralPath currentPath=null;
        
        double n0= 0; // node counter.  Breaks are indicated by increment, so keep track of the last node.
        double slen= 0.; // path length
        float fx0=0f, fy0=0f; // for calculating path length
        
        NumberFormat nf= new DecimalFormat("0.00" );
        
        for ( int i=0; i<vds.getXLength(); i++ ) {
            double d= vds.getDouble(i,units);
            int n= (int)vds.getXTagDouble( i,Units.dimensionless );
            
            float fx= (float)xAxis.transform( xds.getDatum(i) );
            float fy= (float)yAxis.transform( yds.getDatum(i) );
            
            if ( d!=d0 )  {
                ii++;
                currentPath= new GeneralPath();
                list.add( currentPath );
                labels.add( nf.format(d) );
                
                d0= d;
                currentPath.moveTo( fx , fy );
                slen= 0.;
                fx0= fx;
                fy0= fy;
            }
            if( n!=(n0+1) ) {
                currentPath.moveTo( fx, fy );
                fx0= fx;
                fy0= fy;
                slen= 0.;
            } else {
                currentPath.lineTo( fx, fy );
            }
            n0= n;
        }
        
        paths= (GeneralPath[]) list.toArray( new GeneralPath[list.size()] );
        pathLabels= ( String[]) labels.toArray( new String[labels.size()] );
    }
    
    /**
     * Holds value of property contours.
     */
    private String contours="-.7,-.6,-.5,-.4,-.3,-.2,-.1,0,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9";
    
    /**
     * Getter for property contours.
     * @return Value of property contours.
     */
    public String getContours() {
        return this.contours;
    }
    
    /**
     * Setter for property contours.
     * @param contours New value of property contours.
     */
    public void setContours(String contours) {
        String oldContours = this.contours;
        this.contours = contours;
        update();
        propertyChangeSupport.firePropertyChange("contours", oldContours, contours);
    }
    
    /**
     * property labelCadence, the inter-label distance, in ems.
     */
    private double labelCadence=100;
    
    /**
     * Getter for property labelCadence.
     * @return Value of property labelCadence.
     */
    public double getLabelCadence() {
        return this.labelCadence;
    }
    
    /**
     * Setter for property labelCadence.
     * @param labelCadence New value of property labelCadence.
     */
    public void setLabelCadence(double labelCadence) {
        double oldLabelCadence = this.labelCadence;
        this.labelCadence = labelCadence;
        update();
        propertyChangeSupport.firePropertyChange("labelCadence", new Double(oldLabelCadence), new Double(labelCadence));
    }
    
    public boolean acceptContext(int x, int y) {
        if ( paths==null ) return false;
        for ( int i=0; i<paths.length; i++ ) {
            if ( paths[i]!=null ) {
                if ( paths[i].intersects( x-2,y-2,5,5 ) ) return true;
            }
        }
        return false;
    }
    
    /**
     * Holds value of property drawLabels.
     */
    private boolean drawLabels;
    
    /**
     * Getter for property drawLabels.
     * @return Value of property drawLabels.
     */
    public boolean isDrawLabels() {
        return this.drawLabels;
    }
    
    /**
     * Setter for property drawLabels.
     * @param drawLabels New value of property drawLabels.
     */
    public void setDrawLabels(boolean drawLabels) {
        boolean oldDrawLabels = this.drawLabels;
        this.drawLabels = drawLabels;
        update();
        propertyChangeSupport.firePropertyChange("drawLabels", new Boolean(oldDrawLabels), new Boolean(drawLabels));
    }

    /**
     * Holds value of property color.
     */
    private Color color= Color.BLACK;

    /**
     * Getter for property color.
     * @return Value of property color.
     */
    public Color getColor() {
        return this.color;
    }

    /**
     * Setter for property color.
     * @param color New value of property color.
     */
    public void setColor(Color color) {
        Color oldColor = this.color;
        this.color = color;
        update();
        propertyChangeSupport.firePropertyChange ("color", oldColor, color);
    }

    public String getListLabel() {
        return "Contours Renderer";
    }
    
}
