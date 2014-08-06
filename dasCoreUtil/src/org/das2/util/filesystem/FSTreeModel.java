/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.util.filesystem;

import java.awt.Dimension;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

/**
 * present a FileSystem as a TreeModel to display in JTrees.
 * @author jbf
 */
public class FSTreeModel implements TreeModel {

    FileSystem fs;
    List<TreePath> listCachePath= new ArrayList();
    TreeNode[] listCache;
    String listCacheFolder;
    String listCachePendingFolder= "";

    public FSTreeModel(FileSystem fs) {
        this.fs = fs;
    }
    
    private class FSTreeNode extends DefaultMutableTreeNode {
        String path;
        String label;
        FSTreeNode( String fileSystemPath, String label ) {
            this.path= fileSystemPath;
            this.label= label;
        }
        @Override
        public String toString() {
            return this.label;
        }
        public String getFileSystemPath() {
            return this.path;
        }
    }

    @Override
    public boolean isLeaf(Object node) {
        return !node.toString().endsWith("/");
    }

    @Override
    public int getChildCount(Object parent) {
        return getChildren(parent).length;
    }

    @Override
    public Object getChild(Object parent, int index) {
        Object[] kids= getChildren(parent);
        return kids[index];
    }

    private void listingImmediately() {
        try {
            String folder= listCachePendingFolder;
            final String[] folderKids= fs.listDirectory(folder);
            listCache = new DefaultMutableTreeNode[folderKids.length];
            //int[] nodes= new int[folderKids.length];
            for ( int i=0; i<listCache.length; i++ ) {
                final String ss= folder + folderKids[i];
                final String label= folderKids[i];
                DefaultMutableTreeNode dmtn= new FSTreeNode( ss, label );
                listCache[i]= dmtn;
                //nodes[i]= i;
            }
            listCachePendingFolder= "";
            //fireTreeNodesChanged( listCachePath.get( listCachePath.size()-1 ),nodes );
        } catch (IOException ex) {
            listCache = new DefaultMutableTreeNode[] { new DefaultMutableTreeNode( "error: " + ex.getMessage() ) };
        }
    }
    
    private void startListing() {
        Runnable run= new Runnable() {
            public void run() {
                listingImmediately();
            } 
        };
        new Thread( run ).start();

    }
    
    private TreeNode[] getChildren(Object parent) {
        synchronized (this) {
            String theFolder;
            if ( parent instanceof FileSystem ) {
                theFolder= "/";
            } else {
                theFolder= ((FSTreeNode)parent).getFileSystemPath();
            }
            
            if ( listCacheFolder!=null && theFolder.equals(listCacheFolder.toString())) {  // so fs needn't implement equals...
                if ( listCachePendingFolder.length()==0 ) {
                    return listCache;
                } else {
                    return new DefaultMutableTreeNode[] { new DefaultMutableTreeNode( "listing "+listCachePendingFolder+"..." ) };
                }
            } else {
                listCacheFolder= theFolder;
                listCache= null;
                listCachePendingFolder= listCacheFolder;
//                
//                if ( theFolder.equals("/") ) {
//                    listCachePath.clear();
//                    listCachePath.add( new TreePath( fs ) );
//                } else {
//                    listCachePath.add( new TreePath( parent ) );
//                }
//                startListing();
//                return new DefaultMutableTreeNode[] { new DefaultMutableTreeNode( "listing "+listCachePendingFolder+"..." ) };
                
                listingImmediately();
                return listCache;
                
            }
        }
    }

    public Object getRoot() {
        return fs;
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
        System.err.println("valueForPathChanged");
    }

    public int getIndexOfChild(Object parent, Object child) {
        Object[] cc= getChildren(parent);
        for ( int i=0; i<cc.length; i++ ) {
            if ( cc[i].equals(child) ) return i;
        }
        throw new IllegalArgumentException("no such child: "+child);
    }
    
    
    protected void fireTreeStructureChanged( TreePath path ) {
        TreeModelEvent e = new TreeModelEvent(this,path);
        for (TreeModelListener tml : listeners ) {
            tml.treeStructureChanged(e);
        }
    }
    
    protected void fireTreeStructureChanged( ) {
        TreeModelEvent e = new TreeModelEvent(this,new Object[] {fs});
        for (TreeModelListener tml : listeners ) {
            tml.treeStructureChanged(e);
        }
    }
    
    protected void fireTreeNodesChanged( TreePath parent, int[] nodes ) {
        TreeModelEvent e= new TreeModelEvent( this, parent, nodes, null );
        for (TreeModelListener tml : listeners ) {
            tml.treeNodesInserted(e);
        }
    }
    
    ArrayList<TreeModelListener> listeners = new ArrayList();

    @Override
    public void addTreeModelListener(TreeModelListener l) {
        listeners.add(l);
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        listeners.remove(l);
    }
        
    public static void main( String[] args ) throws FileNotFoundException, UnknownHostException, FileSystem.FileSystemOfflineException {
        //FileSystem fs= FileSystem.create("file:///home/jbf/tmp/");
        //FileSystem fs= FileSystem.create("http://autoplot.org/data/vap/");
        FileSystem fs= FileSystem.create("http://emfisis.physics.uiowa.edu/pub/jyds/");
        JTree mytree= new JTree( new FSTreeModel(fs) );
        mytree.setMinimumSize( new Dimension(400,600) );
        mytree.setPreferredSize( new Dimension(400,600) );
        JOptionPane.showMessageDialog( null, new JScrollPane(mytree), "Test FSTREE", JOptionPane.INFORMATION_MESSAGE );
        
    }
}

