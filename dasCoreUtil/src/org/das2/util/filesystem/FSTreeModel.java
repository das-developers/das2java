
package org.das2.util.filesystem;

import java.awt.Dimension;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/**
 * present a FileSystem as a TreeModel to display in JTrees.
 * @author jbf
 */
public class FSTreeModel extends DefaultTreeModel {

    private static final Logger logger= Logger.getLogger( "gui.fstreemodel");
    
    FileSystem fs;
    List<TreePath> listCachePath= new ArrayList();
    Map<String,String> listCachePendingFolders= new HashMap();
    Map<String,TreeNode[]> listCache= new HashMap();

    public FSTreeModel(FileSystem fs) {
        super( new FSTreeNode(fs.getRootURI().toString(),"Root") );
        this.fs = fs;
    }
    
    private static class FSTreeNode extends DefaultMutableTreeNode {
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
        logger.log(Level.FINEST, "isLeaf({0}) -> {1}", new Object[] { node, !node.toString().endsWith("/")  } );
        return !node.toString().endsWith("/");
    }

    @Override
    public int getChildCount(Object parent) {
        int count=  getChildren(parent).length;
        logger.log(Level.FINEST, "getChildCount({0}) -> {1}", new Object[] { parent, count } ) ;
        return count;
    }

    @Override
    public Object getChild(Object parent, int index) {
        Object[] kids= getChildren(parent);
        logger.log(Level.FINEST, "getChild({0},{1}) -> {2}", new Object[]{parent, index,kids[index] } );
        return kids[index];
    }

    boolean stopTest= false;
    private void listingImmediately( Object listCachePendingFolder ) {
        try {
            String folder= folderForNode(listCachePendingFolder);

            System.err.println("listImmediately "+folder);
            final String[] folderKids= fs.listDirectory(folder);
            final TreeNode[] listCache1 = new DefaultMutableTreeNode[folderKids.length];
            final int[] nodes= new int[folderKids.length];
            for ( int i=0; i<listCache1.length; i++ ) {
                final String ss= folder + folderKids[i];
                final String label= folderKids[i];
                DefaultMutableTreeNode dmtn= new FSTreeNode( ss, label );
                listCache1[i]= dmtn;
                nodes[i]= i;
            }
            listCachePendingFolders.put( listCachePendingFolder.toString(), "" );
            listCache.put( listCachePendingFolder.toString(), listCache1 );
            
            stopTest= true;
            
            Runnable run= new Runnable() {
                public void run() {
                    logger.fine("== fireTreeNodesInserted ==");
                    fireTreeNodesChanged( listCachePath.get(listCachePath.size()-1), nodes );
                    //fireTreeNodesChanged( this, new Object[] { root }, nodes, listCache1 );
                    //if ( listPendingNode==null ) {
                    //    fireTreeNodesInserted( this, new Object[] { root }, nodes, listCache );
                    //} else {
                    //    fireTreeNodesInserted( this, listCachePath.toArray(), nodes, listCache );
                    //}
                }
            };
            SwingUtilities.invokeLater(run);
            
        } catch (IOException ex) {
            //listCache.put( listCachePendingFolder, DefaultMutableTreeNode[] { new DefaultMutableTreeNode( "error: " + ex.getMessage() ) } );
            throw new RuntimeException(ex);
        }
    }
    
    private void startListing( final Object folder ) {
        Runnable run= new Runnable() {
            public void run() {
                listingImmediately(folder);
            } 
        };
        new Thread( run ).start();

    }
    
    private static String folderForNode( Object parent ) {
        String theFolder;
        if ( parent instanceof FileSystem ) {
            theFolder= "/";
        } else {
            theFolder= ((FSTreeNode)parent).getFileSystemPath();
        }
        return theFolder;
    }
    
    private TreeNode[] getChildren(Object parent) {
        
        synchronized (this) {
            String theFolder= folderForNode(parent);
                        
            String key= parent.toString();
            TreeNode[] result= listCache.get( key );
            
            if ( result!=null ) { 
                return result;

            } else {
                listCache.put( key, new DefaultMutableTreeNode[] { new DefaultMutableTreeNode( "listing "+listCachePendingFolders+"..." ) } );
                listCachePendingFolders.put( key, theFolder );
                
                boolean async= true;
                if ( async ) {
                    if ( theFolder.equals("/") ) {
                        listCachePath.clear();
                        listCachePath.add( new TreePath( fs ) );
                    } else {
                        listCachePath.add( new TreePath( parent ) );
                    }
                
                    startListing(parent);
                    return listCache.get( key );
                } else {
                
                    listingImmediately(parent);
                    result= listCache.get( key );
                    return result;
                }
                
            }
        }
    }

    @Override
    public Object getRoot() {
        logger.log(Level.FINEST, "getRoot() -> {0}", fs);
        return fs;
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        logger.log(Level.FINEST, "valueForPathChanged({0},{1})", new Object[]{path, newValue});
        System.err.println("valueForPathChanged");
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        Object[] cc= getChildren(parent);
        int result= -1;
        for ( int i=0; i<cc.length; i++ ) {
            if ( cc[i].equals(child) ) result= i;
        }
        logger.log(Level.FINEST, "getIndexOfChild({0},{1}) -> {2}", new Object[]{parent, child, result });
        return result;
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
        //FileSystem fs= FileSystem.create("file:///home/jbf/tmp/");
        //FileSystem fs= FileSystem.create("http://autoplot.org/data/vap/");
        logger.setLevel(Level.ALL);
        Handler h=  new ConsoleHandler();
        h.setLevel(Level.ALL);
        logger.addHandler(h );
        FileSystem fs= FileSystem.create("http://emfisis.physics.uiowa.edu/pub/jyds/");
        JTree mytree= new JTree( new FSTreeModel(fs) );
        mytree.setMinimumSize( new Dimension(400,600) );
        mytree.setPreferredSize( new Dimension(400,600) );
        JOptionPane.showMessageDialog( null, new JScrollPane(mytree), "Test FSTREE", JOptionPane.INFORMATION_MESSAGE );
        
    }
}

