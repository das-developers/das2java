/*
 * DumpToFileMouseModule.java
 *
 * Created on December 1, 2004, 10:31 PM
 */

package org.das2.event;

import org.das2.graph.DasCanvasComponent;
import org.das2.graph.DasAxis;
import org.das2.graph.DasPlot;
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
import org.das2.system.UserMessageCenter;
import java.io.*;
import java.nio.channels.*;
import javax.swing.*;
import org.das2.dataset.DataSetAdapter;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SemanticOps;
import org.virbo.dsutil.DataSetBuilder;

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
        
        QDataSet ds= dsConsumer.getConsumedDataSet();
        
        if ( ds==null ) {
            UserMessageCenter.getDefault().notifyUser( this, "This renderer doesn't have a dataset loaded" );
            return;
        }
        
        QDataSet outds;
        if ( SemanticOps.isTableDataSet(ds) ) {
            QDataSet tds= (QDataSet)ds;
            outds= new ClippedTableDataSet( tds, xrange, yrange );
            
        } else {
            QDataSet vds= (QDataSet)ds;
            DataSetBuilder builder= new DataSetBuilder(2,100,2);
            QDataSet xds= SemanticOps.xtagsDataSet(vds);
            for ( int i=0; i<vds.length(); i++ ) {
                if ( yrange.contains( SemanticOps.getDatum(vds,vds.value(i)) ) & xrange.contains( SemanticOps.getDatum( xds,xds.value(i) ) ) ) {
                    builder.putValue( -1, 0, xds.value(i) );
                    builder.putValue( -1, 1, vds.value(i) );
                }
            }

            outds= builder.getDataSet();
        }
        
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter( new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f ) {
                if ( f.toString()==null ) return false;
                return f.toString().matches(".*\\.das2Stream");
            }
            public String getDescription() { return "*.das2Stream"; }
        });
        int result = chooser.showSaveDialog(parent);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            try {
                FileChannel out = new FileOutputStream(selected).getChannel();
                DataSet outds2= DataSetAdapter.createLegacyDataSet(outds);

                if ( outds2 instanceof TableDataSet ) {
                    TableUtil.dumpToAsciiStream( (TableDataSet)outds2, out);
                }
                else if (outds instanceof VectorDataSet) {
                    VectorUtil.dumpToAsciiStream((VectorDataSet)outds2, out);
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
