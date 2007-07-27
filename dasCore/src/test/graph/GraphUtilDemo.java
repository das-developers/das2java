/*
 * GraphUtilDemo.java
 *
 * Created on June 29, 2007, 6:30 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package test.graph;

import edu.uiowa.physics.pw.das.graph.GraphUtil;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;

/**
 *
 * @author jbf
 */
public class GraphUtilDemo {
    
    private static String pathIteratorTypeString( int type ) {
        switch( type ) {
            case PathIterator.SEG_LINETO: return "line to";
            case PathIterator.SEG_MOVETO: return "move to";
            default: return ""+type;
        }
    }
    
    public static void testReducePath( ) {
        GeneralPath path= new GeneralPath();
        
       /*
        // 2D
        path.moveTo( 30, 30 );
        path.lineTo( 30, 30 );
        path.lineTo( 30.5f, 30 );
        path.lineTo( 30, 35.0f );
        path.moveTo( 30, 35.0f );
        path.lineTo( 30, 35.0f );
        path.lineTo( 30.1f, 35.1f );
        path.lineTo( 35, 35.1f );
        path.moveTo( 35, 35 );
        path.moveTo( 35, 35.2f );
        path.moveTo( 35, 35.5f );
        path.lineTo( 50, 40 );
        path.lineTo( 75, 90 );
        path.lineTo( 95, 95 );
        path.moveTo( 100,100 );
        path.lineTo( 100,100 ); */
        
        // 1D
        path.moveTo( 5, 0 );
        path.lineTo( 5, 0 );
        path.lineTo( 10, 0 );
        path.lineTo( 10, 0 );
        path.moveTo( 10, 0 );
        path.lineTo( 10, 0 );
        path.moveTo( 15, 0 );
        path.moveTo( 20, 0 );
        path.lineTo( 25,  0 );
        
        
        PathIterator pi= path.getPathIterator(null);
        float [] point= new float[6];
        
        while( !pi.isDone() ) {
            int type= pi.currentSegment( point );
            //System.err.println( pathIteratorTypeString(type)+"   "+String.format( "[ %f %f ] ", point[0], point[1] ) );
            pi.next();
        }
        
        GeneralPath path2= GraphUtil.reducePath( path.getPathIterator(null), new GeneralPath( ) );
        
        pi= path2.getPathIterator(null);
        
        //System.err.println( "---- reduces to ----" );
        
        while( !pi.isDone() ) {
            int type= pi.currentSegment( point );
            //System.err.println( pathIteratorTypeString(type)+"   "+String.format( "[ %f %f ] ", point[0], point[1] ) );
            pi.next();
        }
        
    }
    
    public static void main( String[] args ) {
        testReducePath();
    }
    
}
