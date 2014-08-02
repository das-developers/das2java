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
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeModel;

/**
 * present a filesystem as a TreeModel to display in JTrees.
 * @author jbf
 */
public class FSTreeModel implements TreeModel {

    FileSystem fs;
    String[] listCache;
    String listCacheFolder;

    public FSTreeModel(FileSystem fs) {
        this.fs = fs;
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
        String[] kids= getChildren(parent);
        return kids[index];
    }

    private String[] getChildren(Object parent) {
        synchronized (this) {
            if (parent.toString().equals(listCacheFolder.toString())) {  // so fs needn't implement equals...
                return listCache;
            } else {

//TODO: Resolve this on a different thread!!!
                
                try {
                    if ( parent instanceof FileSystem ) {
                        listCache = fs.listDirectory("/");
                    } else {
                        listCache = fs.listDirectory(parent.toString());
                        for ( int i=0; i<listCache.length; i++ ) {
                            listCache[i]= parent + listCache[i];
                        }
                    }
                    listCacheFolder= parent.toString();
                    return listCache;
                } catch (IOException ex) {
                    listCache = new String[]{"error: " + ex.getMessage()};
                    return listCache;
                }
            }
        }
    }


    public Object getRoot() {
        return fs;
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
        
    }

    public int getIndexOfChild(Object parent, Object child) {
        String[] cc= getChildren(parent);
        for ( int i=0; i<cc.length; i++ ) {
            if ( cc[i].equals(child) ) return i;
        }
        throw new IllegalArgumentException("no such child: "+child);
    }
    
    
    protected void fireTreeStructureChanged( ) {
        TreeModelEvent e = new TreeModelEvent(this,new Object[] {fs});
        for (TreeModelListener tml : listeners ) {
            tml.treeStructureChanged(e);
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
        FileSystem fs= FileSystem.create("file:///Users/jbf/tmp/");
        JTree mytree= new JTree( new FSTreeModel(fs) );
        mytree.setMinimumSize( new Dimension(400,400) );
        mytree.setPreferredSize( new Dimension(400,400) );
        JOptionPane.showMessageDialog( null, mytree, "Test FSTREE", JOptionPane.INFORMATION_MESSAGE );
        
    }
}

