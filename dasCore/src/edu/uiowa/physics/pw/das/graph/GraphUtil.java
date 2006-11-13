/*
 * Util.java
 *
 * Created on September 22, 2004, 1:47 PM
 */

package edu.uiowa.physics.pw.das.graph;

import edu.uiowa.physics.pw.das.dasml.*;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.graph.*;
import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import javax.swing.*;
import javax.xml.parsers.*;
import org.apache.xml.serialize.*;
import org.w3c.dom.*;

/**
 *
 * @author  Jeremy
 */
public class GraphUtil {
    
    public static DasPlot newDasPlot( DasCanvas canvas, DatumRange x, DatumRange y ) {
        DasAxis xaxis= new DasAxis( x.min(), x.max(), DasAxis.HORIZONTAL );
        DasAxis yaxis= new DasAxis( y.min(), y.max(), DasAxis.VERTICAL );
        DasRow row= new DasRow( canvas, null, 0, 1, 2, -3, 0, 0 );
        DasColumn col= new DasColumn( canvas, null, 0, 1, 5, -3, 0, 0 );
        DasPlot result= new DasPlot( xaxis, yaxis );
        canvas.add( result, row, col );
        return result;
    }
    
    public static void serializeCanvas( DasCanvas canvas, OutputStream out ) {
        try {
            Document document= DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            document.appendChild(canvas.getDOMElement(document));
            StringWriter writer = new StringWriter(); 
            OutputFormat format = new OutputFormat(Method.XML, "UTF-8", true);
            XMLSerializer serializer = new XMLSerializer( new OutputStreamWriter(out), format);
            serializer.serialize(document);
            out.close();
        } catch ( Exception e ) {
            throw new RuntimeException(e);
        }
    }
    
    public static JTabbedPane loadCanvas( InputStream in ) throws Exception {
        FormBase form = new FormBase( in, null, true );
        return form;
    }
    
    public static GeneralPath getPath( DasAxis xAxis, DasAxis yAxis, VectorDataSet xds, boolean histogram ) {
        return getPath( xAxis, yAxis, new XTagsVectorDataSet( xds ), xds, histogram );
    }
    
    public static GeneralPath getPath(DasAxis xAxis, DasAxis yAxis, VectorDataSet xds, VectorDataSet yds, boolean histogram) {
        
        GeneralPath newPath = new GeneralPath();
        
        Dimension d;
        
        double xmin, xmax, ymin, ymax;
        int ixmax, ixmin;
        
        Units xUnits= xAxis.getUnits();
        Units yUnits= yAxis.getUnits();
        
        xmax= xAxis.getDataMaximum().doubleValue(xUnits);
        xmin= xAxis.getDataMinimum().doubleValue(xUnits);
        ymax= yAxis.getDataMaximum().doubleValue(yUnits);
        ymin= yAxis.getDataMinimum().doubleValue(yUnits);
        
        /* need consider how to handle clipping.  Presumably java does this efficiently, 
         * so there is no longer a need to worry about this.
         */
        
        
        double xSampleWidth;
        if (xds.getProperty("xTagWidth") != null) {
            Datum xSampleWidthDatum = (Datum)xds.getProperty("xTagWidth");
            xSampleWidth = xSampleWidthDatum.doubleValue(xUnits.getOffsetUnits());
        } else if (xds.getProperty("xSampleWidth") != null) {
            Datum xSampleWidthDatum = (Datum)xds.getProperty("xSampleWidth");   // this is not the correct property name, but we'll allow it for now
            xSampleWidth = xSampleWidthDatum.doubleValue(xUnits.getOffsetUnits());
        } else {
            //Try to load the legacy sample-width property.
            String xSampleWidthString = (String)xds.getProperty("x_sample_width");
            if (xSampleWidthString != null) {
                double xSampleWidthSeconds = Double.parseDouble(xSampleWidthString);
                xSampleWidth = Units.seconds.convertDoubleTo(xUnits.getOffsetUnits(), xSampleWidthSeconds);
            }
            else {
                xSampleWidth = 1e31;
            }
        }
        
        double t0 = -Double.MAX_VALUE;
        double x0 = -Double.MAX_VALUE;
        double y0 = -Double.MAX_VALUE;
        double i0 = -Double.MAX_VALUE;
        double j0 = -Double.MAX_VALUE;
        boolean skippedLast = true;        
        int n= xds.getXLength();
        for (int index = 0; index < n; index++) {
            double t= xds.getXTagDouble(index, xUnits);
            double x = xds.getDouble(index, yUnits);
            double y = yds.getDouble(index, yUnits);
            double i = xAxis.transform(x, xUnits);
            double j = yAxis.transform(y, yUnits);
            if ( yUnits.isFill(y) || Double.isNaN(y)) {
                skippedLast = true;
            }
            else if (skippedLast || (t - t0) > xSampleWidth) {
                newPath.moveTo((float)i, (float)j);
                skippedLast = false;
            }
            else {
                if (histogram) {
                    double i1 = (i0 + i)/2;
                    newPath.lineTo((float)i1, (float)j0);
                    newPath.lineTo((float)i1, (float)j);
                    newPath.lineTo((float)i, (float)j);
                }
                else {
                    newPath.lineTo((float)i, (float)j);
                }
                skippedLast = false;
            }
            t0= t;
            x0= x;
            y0= y;            
            i0= i;
            j0= j;
        }
        return newPath;

    }
    
    /**
     * calculates the AffineTransform between two sets of x and y axes, if possible.  
     * @returns null if no such transform exists, or the AffineTransform that transforms
     * data located with xaxis0 and yaxis0 on xaxis1 and yaxis1.
     */
    public static AffineTransform calculateAT( DasAxis xaxis0, DasAxis yaxis0, DasAxis xaxis1, DasAxis yaxis1 ) {
        AffineTransform at= new AffineTransform();
        
        double dmin0= xaxis1.transform(xaxis0.getDataMinimum());  // old axis in new axis space
        double dmax0= xaxis1.transform(xaxis0.getDataMaximum());
        double dmin1= xaxis1.transform(xaxis1.getDataMinimum());
        double dmax1= xaxis1.transform(xaxis1.getDataMaximum());
        
        double scalex= ( dmin0 - dmax0 ) / ( dmin1 - dmax1 );
        double transx= -1* dmin1 * scalex + dmin0;
                
        at.translate( transx, 0 );
        at.scale( scalex, 1. );
        
        if ( at.getDeterminant() == 0.000 ) {            
            return null;
        }
        
        dmin0= yaxis1.transform(yaxis0.getDataMinimum());  // old axis in new axis space
        dmax0= yaxis1.transform(yaxis0.getDataMaximum());
        dmin1= yaxis1.transform(yaxis1.getDataMinimum());
        dmax1= yaxis1.transform(yaxis1.getDataMaximum());
        
        double scaley= ( dmin0 - dmax0 ) / ( dmin1 - dmax1 );
        double transy= -1* dmin1 * scaley + dmin0;
        
        at.translate( 0, transy );
        at.scale( 1., scaley );
        
        return at;
    }
    
    
    public static DasAxis guessYAxis( DataSet dsz ) {
        boolean log= false;
        
        if ( dsz.getProperty( DataSet.PROPERTY_Y_SCALETYPE )!=null ) {
            if ( dsz.getProperty( DataSet.PROPERTY_Y_SCALETYPE ).equals("log") ) {
                log= true;
            }
        }
        
        DasAxis result;
        
        if ( dsz instanceof TableDataSet ) {
            TableDataSet ds= (TableDataSet)dsz;
            Units yunits= ds.getYUnits();
            Datum min, max;
            
            DatumRange yrange= DataSetUtil.yRange(dsz);
            Datum dy= TableUtil.guessYTagWidth( ds );
            if ( UnitsUtil.isRatiometric(dy.getUnits()) ) log=true;
            
            result= new DasAxis( yrange.min(), yrange.max(), DasAxis.LEFT, log );
            
        } else if ( dsz instanceof VectorDataSet ) {
            VectorDataSet ds= ( VectorDataSet ) dsz;
            Units yunits= ds.getYUnits();
            
            DatumRange range= DataSetUtil.yRange( dsz );
            if ( range.width().doubleValue(yunits.getOffsetUnits())==0. ) {
                range= range.include(yunits.createDatum(0));
                if ( range.width().doubleValue(yunits.getOffsetUnits())==0. ) {
                    range= new DatumRange( 0, 10, yunits );
                }
            }
            result= new DasAxis( range.min(), range.max(), DasAxis.LEFT, log );
            
        } else {
            throw new IllegalArgumentException( "not supported: "+dsz );
        }
        
        if ( dsz.getProperty( DataSet.PROPERTY_Y_LABEL )!=null ) {
            result.setLabel( (String)dsz.getProperty( DataSet.PROPERTY_Y_LABEL ) );
        }
        return result;
    }
    
    public static DasAxis guessXAxis( DataSet ds ) {
        Datum min= ds.getXTagDatum(0);
        Datum max= ds.getXTagDatum( ds.getXLength()-1 );
        return new DasAxis( min, max, DasAxis.BOTTOM );
    }
    
    public static DasAxis guessZAxis( DataSet dsz ) {
        if ( !(dsz instanceof TableDataSet) ) throw new IllegalArgumentException("only TableDataSet supported");
        
        TableDataSet ds= (TableDataSet)dsz;
        Units zunits= ds.getZUnits();
        
        DatumRange range= DataSetUtil.zRange(ds);
        
        boolean log= false;
        if ( dsz.getProperty( DataSet.PROPERTY_Z_SCALETYPE )!=null ) {
            if ( dsz.getProperty( DataSet.PROPERTY_Z_SCALETYPE ).equals("log") ) {
                log= true;
                if ( range.min().doubleValue( range.getUnits() ) <= 0 ) { // kludge for VALIDMIN
                    double max= range.max().doubleValue(range.getUnits());
                    range= new DatumRange( max/1000, max, range.getUnits() );
                }
            }
        }
        
        DasAxis result= new DasAxis( range.min(), range.max(), DasAxis.LEFT, log );
        if ( dsz.getProperty( DataSet.PROPERTY_Z_LABEL )!=null ) {
            result.setLabel( (String)dsz.getProperty( DataSet.PROPERTY_Z_LABEL ) );
        }
        return result;
    }

    public static Renderer guessRenderer( DataSet ds ) {
        Renderer rend=null;
        if ( ds instanceof VectorDataSet ) {
            if ( ds.getXLength() > 10000 ) {
                rend= new ImageVectorDataSetRenderer(new ConstantDataSetDescriptor(ds));
            } else {
                rend= new SymbolLineRenderer( ds );
                ((SymbolLineRenderer)rend).setPsym( Psym.DOTS );
                ((SymbolLineRenderer)rend).setSymSize( 2.0 );
            }
            
        } else if (ds instanceof TableDataSet ) {
            Units zunits= ((TableDataSet)ds).getZUnits();
            DasAxis zaxis= guessZAxis(ds);
            DasColorBar colorbar= new DasColorBar( zaxis.getDataMinimum(), zaxis.getDataMaximum(), zaxis.isLog() );
            colorbar.setLabel( zaxis.getLabel() );
            rend= new SpectrogramRenderer( new ConstantDataSetDescriptor(ds), colorbar );
        }
        return rend;
    }
    
    public static DasPlot guessPlot( DataSet ds ) {
        DasAxis xaxis= guessXAxis( ds );
        DasAxis yaxis= guessYAxis( ds );
        DasPlot plot= new DasPlot( xaxis, yaxis );
        plot.addRenderer( guessRenderer(ds) );
        return plot;
    }
     
    public static DasPlot visualize( DataSet ds ) {
        
        JFrame jframe= new JFrame("DataSetUtil.visualize");
        DasCanvas canvas= new DasCanvas(400,400);
        jframe.getContentPane().add( canvas );
        DasPlot result= guessPlot( ds );
        canvas.add( result, DasRow.create(canvas), DasColumn.create( canvas ) );
        
        jframe.pack();
        jframe.setVisible(true);
        jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        return result;
    }
    
    public static DasPlot visualize( DataSet ds, boolean ylog ) {
        DatumRange xRange= DataSetUtil.xRange( ds );
        DatumRange yRange= DataSetUtil.yRange( ds );
        JFrame jframe= new JFrame("DataSetUtil.visualize");
        DasCanvas canvas= new DasCanvas(400,400);
        jframe.getContentPane().add( canvas );
        DasPlot result= guessPlot( ds );
        canvas.add( result, DasRow.create(canvas), DasColumn.create( canvas ) );
        Units xunits= result.getXAxis().getUnits();
        result.getXAxis().setDatumRange(xRange.zoomOut(1.1));
        Units yunits= result.getYAxis().getUnits();
        if ( ylog ) {
            result.getYAxis().setDatumRange(yRange);
            result.getYAxis().setLog(true);
        } else {
            result.getYAxis().setDatumRange(yRange.zoomOut(1.1));
        }
        jframe.pack();
        jframe.setVisible(true);
        jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        return result;
    }

}