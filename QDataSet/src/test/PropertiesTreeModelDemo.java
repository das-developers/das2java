/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import org.das2.datum.Units;
import java.util.HashMap;
import javax.swing.JFrame;
import javax.swing.JTree;
import javax.swing.tree.TreeModel;
import org.das2.qds.DDataSet;
import org.das2.qds.DataSetUtil;
import org.das2.qds.QDataSet;
import org.das2.qds.ops.Ops;
import org.das2.qds.util.PropertiesTreeModel;

/**
 *
 * @author jbf
 */
public class PropertiesTreeModelDemo {

    public static void main(String[] args) {
        
        DDataSet ds = (DDataSet)Ops.dindgen(10, 20);
        ds.putProperty(DDataSet.NAME, "testData");
        ds.putProperty(QDataSet.UNITS, Units.meters);
        
        ds.putProperty(DDataSet.PLANE_0, Ops.dindgen(10, 20) );
                
        HashMap props= (HashMap) DataSetUtil.getProperties(ds);
        
        TreeModel model= new PropertiesTreeModel(ds,10);
        JTree tree= new JTree(model);
        
        JFrame frame= new JFrame();
        frame.getContentPane().add(tree);
        
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
