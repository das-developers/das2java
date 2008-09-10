/*
 * DumpToFileMouseModule.java
 *
 * Created on December 1, 2004, 10:31 PM
 */

package org.das2.event;

import org.das2.dataset.DataSetConsumer;
import org.das2.dataset.VectorDataSetBuilder;
import org.das2.dataset.VectorUtil;
import org.das2.dataset.TableDataSet;
import org.das2.dataset.DataSet;
import org.das2.dataset.ClippedTableDataSet;
import org.das2.dataset.TableUtil;
import org.das2.dataset.VectorDataSet;
import org.das2.datum.DatumRange;
import org.das2.util.DasExceptionHandler;
import edu.uiowa.physics.pw.das.graph.*;
import org.das2.system.UserMessageCenter;
import java.io.*;
import java.nio.channels.*;
import javax.swing.*;

/**
 *
 * @author  Jeremy
 */
public class DumpToFileMouseModule extends MouseModule {
    DasAxis xAxis;
    DasAxis yAxis;
    DataSetConsumer dsConsumer;
    
    public DumpToFileMouseModule(DasCanvasComponent parent, DataSetConsumer dsConsumer, DasAxis xAxis, DasAxis yAxis) {
        super( parent, new BoxRenderer(parent), "Dump to File" );
        if (!xAxis.isHorizontal()) {
            throw new IllegalArgumentException("X Axis orientation is not horizontal");
        }
        if (yAxis.isHorizontal()) {
            throw new IllegalArgumentException("Y Axis orientation is not vertical");
        }
        this.xAxis= xAxis;
        this.yAxis= yAxis;
        this.dsConsumer= dsConsumer;
    }
    
    public static DumpToFileMouseModule create( DasPlot parent, DataSetConsumer dsConsumer ) {
        DumpToFileMouseModule result=
        new DumpToFileMouseModule(parent, dsConsumer, parent.getXAxis(),parent.getYAxis());
        return result;
    }
    
    public void mouseRangeSelected(MouseDragEvent e0) {
        MouseBoxEvent e= (MouseBoxEvent)e0;
        
        DatumRange xrange;
        DatumRange yrange;
        
        xrange= new DatumRange( xAxis.invTransform(e.getXMinimum()), xAxis.invTransform(e.getXMaximum()) );
        yrange= new DatumRange( yAxis.invTransform(e.getYMaximum()), yAxis.invTransform(e.getYMinimum()) );
        
        DataSet ds= dsConsumer.getConsumedDataSet();
        
        if ( ds==null ) {
            UserMessageCenter.getDefault().notifyUser( this, "This renderer doesn't have a dataset loaded" );
            return;
        }
        
        DataSet outds;
        if ( ds instanceof TableDataSet ) {
            TableDataSet tds= (TableDataSet)ds;
            outds= new ClippedTableDataSet( tds, xrange, yrange );
            
        } else {
            VectorDataSet vds= (VectorDataSet)ds;
            VectorDataSetBuilder builder= new VectorDataSetBuilder(vds.getXUnits(),vds.getYUnits());
            for ( int i=0; i<vds.getXLength(); i++ ) {
                if ( yrange.contains(vds.getDatum(i)) & xrange.contains(vds.getXTagDatum(i) ) ) {
                    builder.insertY(vds.getXTagDouble(i,vds.getXUnits()),vds.getDouble(i,vds.getYUnits()));
                }
            }
            outds= builder.toVectorDataSet();
        }
        
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter( new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f ) { return f.toString().matches(".*\\.das2Stream"); }
            public String getDescription() { return "*.das2Stream"; }
        });
        int result = chooser.showSaveDialog(parent);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            try {
                FileChannel out = new FileOutputStream(selected).getChannel();
                if (outds instanceof TableDataSet) {
                    TableUtil.dumpToAsciiStream((TableDataSet)outds, out);
                }
                else if (outds instanceof VectorDataSet) {
                    VectorUtil.dumpToAsciiStream((VectorDataSet)outds, out);
                }
            }
            catch (IOException ioe) {
                DasExceptionHandler.handle(ioe);
            }
        }
        
    }
    
    /** Creates a new instance of DumpToFileMouseModule */
    public DumpToFileMouseModule() {
    }
    
}
