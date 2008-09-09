/*
 * ContoursRendererDemo.java
 *
 * Created on December 7, 2007, 3:22 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package test.graph;

import org.das2.dataset.TableDataSet;
import org.das2.dataset.test.RipplesDataSet;
import edu.uiowa.physics.pw.das.event.AnnotatorMouseModule;
import edu.uiowa.physics.pw.das.event.MouseModule;
import edu.uiowa.physics.pw.das.event.PointSlopeDragRenderer;
import edu.uiowa.physics.pw.das.graph.GraphUtil;
import edu.uiowa.physics.pw.das.graph.Renderer;
import edu.uiowa.physics.pw.das.graph.ContoursRenderer;
import edu.uiowa.physics.pw.das.graph.DasPlot;

/**
 *
 * @author jbf
 */
public class ContoursRendererDemo  {
    
    /** Creates a new instance of ContoursRendererDemo */
    public ContoursRendererDemo() {
        super();
        Renderer rend= new ContoursRenderer();
        
        TableDataSet tds= new RipplesDataSet(50,50,20,70,70,30,100,100);
        
        DasPlot p= GraphUtil.visualize( tds );
        p.getXAxis().setAnimated(false);
        p.getYAxis().setAnimated(false);
        
	MouseModule mm= new MouseModule( p, new PointSlopeDragRenderer(p, p.getXAxis(), p.getYAxis() ), "Slope" );
	
	p.addMouseModule( mm );
	
        rend.setDataSet( tds );
        
        p.addRenderer( rend );
        
    }
    
    public static void main( String[] args ) {
        new ContoursRendererDemo();
    }
}
