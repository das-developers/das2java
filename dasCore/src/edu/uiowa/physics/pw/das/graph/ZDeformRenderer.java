/*
 * ZDeformRenderer.java
 *
 * Created on November 14, 2003, 8:18 PM
 */

package edu.uiowa.physics.pw.das.graph;

import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.datum.*;
import org.das2.util.monitor.ProgressMonitor;
import java.awt.*;
import java.awt.geom.*;

/**
 *
 * @author  Owner
 */
public class ZDeformRenderer extends Renderer {
    
    int dx= 20;
    int dy= 0;  // deform direction    
    
    /** Creates a new instance of ZDeformRenderer */
    public ZDeformRenderer( DataSetDescriptor dsd ) {
        super(dsd);
    }
    
    protected void installRenderer() {
    }
    
    public void render(java.awt.Graphics g1, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {
        Graphics2D g= (Graphics2D) g1;
        TableDataSet tds= (TableDataSet) getDataSet();
        double zmax= TableUtil.tableMax(tds,tds.getZUnits());
        for ( int itable=0; itable<tds.tableCount(); itable++ ) {
            int ny= tds.getYLength(itable);
            int nx= tds.tableEnd(itable)-tds.tableStart(itable);
            Datum x;            
            
            int[] iys= new int[tds.getYLength(itable)];            
            for (int j=0; j<tds.getYLength(itable); j++) {
               iys[j]= (int)yAxis.transform(tds.getYTagDatum(itable, j));
            }		
            for (int i=tds.tableStart(itable); i<tds.tableEnd(itable); i++) {
                x= tds.getXTagDatum(i);
                int ix= (int)xAxis.transform(x);
                if ( ix>(-1000) && ix<1000 ) {
                    double z0= tds.getDouble(i,0,tds.getZUnits());
                    Line2D.Double line= new Line2D.Double();
                    for ( int j=1; j<iys.length; j++ ) {                    
                        double z1= tds.getDouble(i,j, tds.getZUnits());
                        if ( z1>-1e30 && z0>-1e30 ) {                            
                            line.setLine(ix+z0/zmax*dx, iys[j-1]+z0/zmax*dy, ix+z1/zmax*dx, iys[j]+z0/zmax*dy );
                            g.draw(line);
                        }
                        z0= z1;
                    }
                }                
            }
        }
    }
    
    protected void uninstallRenderer() {
    }
            
    protected org.w3c.dom.Element getDOMElement(org.w3c.dom.Document document) {
        return null;
    }    
    
}
