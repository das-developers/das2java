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
public class Util {
    
    public static DasPlot newDasPlot( DasCanvas canvas, DatumRange x, DatumRange y ) {
        DasAxis xaxis= new DasAxis( x.min(), x.max(), DasAxis.HORIZONTAL );
        DasAxis yaxis= new DasAxis( y.min(), y.max(), DasAxis.VERTICAL );
        DasRow row= DasRow.create(canvas);
        DasColumn col= DasColumn.create(canvas);
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
        }
        else {
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
        
        double x0 = -Double.MAX_VALUE;
        double y0 = -Double.MAX_VALUE;
        double i0 = -Double.MAX_VALUE;
        double j0 = -Double.MAX_VALUE;
        boolean skippedLast = true;        
        int n= xds.getXLength();
        for (int index = 0; index < n; index++) {
            double x = xds.getDouble(index, xUnits);
            double y = yds.getDouble(index, yUnits);
            double i = xAxis.transform(x, xUnits);
            double j = yAxis.transform(y, yUnits);
            if ( yUnits.isFill(y) || Double.isNaN(y)) {
                skippedLast = true;
            }
            else if (skippedLast || Math.abs(x - x0) > xSampleWidth) {
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
            x0= x;
            y0= y;
            i0= i;
            j0= j;
        }
        return newPath;

    }
}