/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import edu.uiowa.physics.pw.das.datum.Units;
import java.util.HashMap;
import javax.swing.JFrame;
import javax.swing.JTree;
import javax.swing.tree.TreeModel;
import org.virbo.dataset.DDataSet;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.QDataSet;
import org.virbo.dsops.Ops;
import org.virbo.dsutil.PropertiesTreeModel;

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
        
        TreeModel model= new PropertiesTreeModel(ds);
        JTree tree= new JTree(model);
        
        JFrame frame= new JFrame();
        frame.getContentPane().add(tree);
        
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
