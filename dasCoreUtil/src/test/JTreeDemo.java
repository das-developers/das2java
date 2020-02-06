/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/**
 *
 * @author jbf
 */
public class JTreeDemo {
    
    DefaultTreeModel treeModel1;
    DefaultTreeModel treeModel2;
    
    JTree jtree1;
    JTree jtree2;
    
    public static void main( String[] args ) {
        new JTreeDemo().init();
    }
    
    public void init() {
        JPanel panel=new JPanel();
        
        panel.setPreferredSize( new Dimension( 640,480 ) ) ;
        panel.setLayout(new BoxLayout(panel,BoxLayout.X_AXIS));

        jtree1=new JTree();
        jtree2=new JTree();
        
        treeModel1= (DefaultTreeModel)jtree1.getModel();
        treeModel2= (DefaultTreeModel)jtree2.getModel();
        
        jtree1.addMouseListener( getMouseListener() );
        jtree2.addMouseListener( getMouseListener() );

        JSplitPane sp= new JSplitPane();
        sp.setLeftComponent(jtree1);
        sp.setRightComponent(jtree2);

        panel.add( sp );
        
        JDialog d= new JDialog();
        d.getContentPane().add( panel );
        d.pack();
        sp.setDividerLocation(0.5);
        d.setVisible(true);
    }

    private MouseListener getMouseListener() {
        return new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if ( e.isPopupTrigger() ) {
                    showPopup( e.getSource(), e.getX(), e.getY() );
                }                    
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if ( e.isPopupTrigger() ) {
                    showPopup( e.getSource(), e.getX(), e.getY() );
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if ( e.isPopupTrigger() ) {
                    showPopup( e.getSource(), e.getX(), e.getY() );
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                
            }

            @Override
            public void mouseExited(MouseEvent e) {
                
            }
            
        };
    }
    
    public Action getDeleteAction( final JTree jtree ) {
        return new AbstractAction("Delete") {
            @Override
            public void actionPerformed(ActionEvent e) {
                TreePath tp= jtree.getSelectionPath();
                if ( tp==null ) return;
                MutableTreeNode n= (MutableTreeNode) tp.getLastPathComponent();
                if ( n.getParent()!=null ) {
                    ((DefaultTreeModel)jtree.getModel()).removeNodeFromParent(n);
                } else {
                    JOptionPane.showMessageDialog( jtree, "This root node cannot be deleted." );
                }
            }
        };
    }
    
    public static TreePath getPath(TreeNode treeNode) {
        List<Object> nodes = new ArrayList<>();
        if (treeNode != null) {
            nodes.add(treeNode);
            treeNode = treeNode.getParent();
            while (treeNode != null) {
                nodes.add(0, treeNode);
                treeNode = treeNode.getParent();
            }
        }
        return nodes.isEmpty() ? null : new TreePath(nodes.toArray());
    }

    
    
    public Action getReplaceAction( final JTree jtree ) {
        return new AbstractAction("Replace") {
            @Override
            public void actionPerformed(ActionEvent e) {
                TreePath tp= jtree.getSelectionPath();
                if ( tp==null ) return;
                MutableTreeNode n= (MutableTreeNode) tp.getLastPathComponent();
                MutableTreeNode parent= (MutableTreeNode) n.getParent();
                if ( parent==null ) {
                    JOptionPane.showMessageDialog( jtree, "This root node cannot be replaced." );
                } else {
                    int index= parent.getIndex(n);
                    ((DefaultTreeModel)jtree.getModel()).removeNodeFromParent(n);
                    DefaultTreeModel model= ((DefaultTreeModel)jtree.getModel());
                    MutableTreeNode nn= new DefaultMutableTreeNode("Instruments");
                    nn.insert( new DefaultMutableTreeNode("Violin"), 0 );
                    nn.insert( new DefaultMutableTreeNode("Viola"), 1 );
                    nn.insert( new DefaultMutableTreeNode("Cello"), 2 );
                    nn.insert( new DefaultMutableTreeNode("Bass"), 3 );
                    model.insertNodeInto( nn, parent, index );
                    jtree.expandPath( getPath(nn) );
                }
            }
        };
    }    
    
    public void showPopup( Object invoker, int x, int y ) {
        JPopupMenu popup= new JPopupMenu();
        JTree jtree= (JTree)invoker;
        popup.add( getDeleteAction(jtree) );
        popup.add( getReplaceAction(jtree) );
        popup.show( jtree, x, y );
    }

}
