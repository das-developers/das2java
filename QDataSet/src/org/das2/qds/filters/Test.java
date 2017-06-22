/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.qds.filters;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.das2.qds.QDataSet;
import org.das2.qds.ops.Ops;

/**
 *
 * @author mmclouth
 */
public class Test {
    
    public static void testAdd() {
        FilterEditorPanel p= new AddFilterEditorPanel();
        p.setFilter("|subtract(50)");
        JOptionPane.showMessageDialog( null, p );
        System.err.println( p.getFilter() );
    }
    
    public static void testSlice() throws Exception {
        final FilterEditorPanel p= new SliceFilterEditorPanel();
        Runnable run= new Runnable() {
            public void run() {
                p.setFilter("|slice1(50)");
            }
        };
        SwingUtilities.invokeAndWait(run);
        
        QDataSet ds= Ops.ripples(30,4,5,6);
        QDataSet dep0= Ops.linspace(50.,100.,30);
        dep0= Ops.putProperty( dep0, QDataSet.NAME, "time" );
        ds= Ops.putProperty( ds, QDataSet.DEPEND_0, dep0 );
        QDataSet dep1= Ops.linspace(50.,100.,4);
        dep1= Ops.putProperty( dep1, QDataSet.NAME, "energy" );
        ds= Ops.putProperty( ds, QDataSet.DEPEND_1, dep1 );
        p.setInput(ds);
        JOptionPane.showMessageDialog( null, p );
        System.err.println( p.getFilter() );
    }
    
    public static void main( String[] args ) throws Exception {
        //testAdd();
        testSlice();
    }
    
    
}
