
package org.das2.util.filesystem;

import java.awt.Dimension;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import org.das2.util.LoggerManager;

/**
 * present a FileSystem as a TreeModel to display in JTrees.
 * @author jbf
 */
public class FSTreeModel extends DefaultTreeModel {

    private static final Logger logger= LoggerManager.getLogger( "das2.filesystem.fstree");
    public static final String PENDING_NOTE = " PENDING";
    
    FileSystem fs;
    List<TreePath> listCachePath= new ArrayList();
    Map<String,String> listCachePendingFolders= new HashMap();
    Map<String,DefaultMutableTreeNode[]> listCache= new HashMap();

    public FSTreeModel(FileSystem fs) {
        super( new FSTreeNode( "/",fs.getRootURI().toString()) );
        this.fs = fs;
    }
    
    private static class FSTreeNode extends DefaultMutableTreeNode {
        String path;
        String label;
        boolean pending;
        FSTreeNode( String fileSystemPath, String label ) {
            this.path= fileSystemPath;
            this.label= label;
            this.pending= false;
        }
        @Override
        public String toString() {
            return this.label + ( pending ? PENDING_NOTE : "" );
        }
        public String getFileSystemPath() {
            return this.path;
        }
        
        public boolean isPending() {
            return pending;
        }
        public void setPending( boolean pending ) {
            this.pending= pending;
        }
    }

    @Override
    public boolean isLeaf(Object node) {
        boolean isFolder= ((FSTreeNode)node).path.endsWith("/");
        logger.log(Level.FINEST, "isLeaf({0}) -> {1}", new Object[] { node, !isFolder  } );
        return !isFolder;
    }

    @Override
    public int getChildCount(Object parent) {
        int count=  getChildren(parent).length;
        logger.log(Level.FINER, "getChildCount({0}) -> {1}", new Object[] { parent, count } ) ;
        return count;
    }

    @Override
    public Object getChild(Object parent, int index) {
        Object[] kids= getChildren(parent);
        logger.log(Level.FINEST, "getChild({0},{1}) -> {2}", new Object[]{parent, index,kids[index] } );
        return kids[index];
    }

    boolean stopTest= false;
    
    private void listingImmediately( final Object listCachePendingFolder ) {
        logger.log(Level.FINE, "listingImmediatey({0})", new Object[]{ listCachePendingFolder } );
        try {
            final String folder= folderForNode(listCachePendingFolder);

            long t0= System.currentTimeMillis();
            logger.log(Level.FINE, "listImmediately {0}", folder);
            final String[] folderKids= fs.listDirectory(folder);
            logger.fine( String.format( Locale.US, "done in %5.2f sec: listImmediately %s", (System.currentTimeMillis()-t0)/1000.0, folder ) );
            final DefaultMutableTreeNode[] listCache1 = new DefaultMutableTreeNode[folderKids.length];
            final int[] nodes= new int[folderKids.length];
            for ( int i=0; i<listCache1.length; i++ ) {
                final String ss= folder + folderKids[i];
                final String label= folderKids[i];
                DefaultMutableTreeNode dmtn= new FSTreeNode( ss, label );
                listCache1[i]= dmtn;
                nodes[i]= i;
            }
            String s= listCachePendingFolder.toString();
            ((FSTreeNode)listCachePendingFolder).setPending(false);
            listCachePendingFolders.put( s, "" );
            if ( s.endsWith(PENDING_NOTE) ) {
                s= s.substring(0,s.length()-PENDING_NOTE.length());
            }
            final MutableTreeNode[] rm= listCache.get( s );
            listCache.put( s, listCache1 );
            
            stopTest= true;
            
            Runnable run= new Runnable() {
                @Override
                public void run() {
                    //fireTreeNodesChanged( listCachePath.get(listCachePath.size()-1), nodes );
                    logger.log(Level.FINE, "listingImmediately({0}) -> array[{1}]", new Object[]{ listCachePath.get(listCachePath.size()-1), nodes.length } );
                    
                    for (MutableTreeNode rm1 : rm) {
                        FSTreeModel.this.removeNodeFromParent(rm1);
                    }
                    for ( int i=0; i<listCache1.length; i++ ) {
                        FSTreeModel.this.insertNodeInto( listCache1[i], (MutableTreeNode)listCachePendingFolder, i );
                    }
                    
                    //fireTreeNodesChanged( this, new Object[] { root }, nodes, listCache1 );
                    //if ( listPendingNode==null ) {
                    //    fireTreeNodesInserted( this, new Object[] { root }, nodes, listCache );
                    //} else {
                    //    fireTreeNodesInserted( this, listCachePath.toArray(), nodes, listCache );
                    //}
                }
            };
            SwingUtilities.invokeLater(run);
            
        } catch (final IOException ex) {

            Runnable run= new Runnable() {
                @Override
                public void run() {

                    logger.log(Level.SEVERE, null, ex );
                    ((FSTreeNode)listCachePendingFolder).setPending(false);

                }
            };
            SwingUtilities.invokeLater(run);
        }
    }
    
    private void startListing( final Object folder ) {
        final FSTreeNode fst= (FSTreeNode)folder;
        fst.setPending(true);
        Runnable run= new Runnable() {
            @Override
            public void run() {
                listingImmediately(folder);
                fst.setPending(false);
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
    
    private DefaultMutableTreeNode[] getChildren(Object parent) {
        
        synchronized (this) {
            String theFolder= folderForNode(parent);
                        
            String key= parent.toString();
            if ( key.endsWith(PENDING_NOTE) ) {
                key= key.substring(0,key.length()-PENDING_NOTE.length());
            }
            
            DefaultMutableTreeNode[] result= listCache.get( key );
            
            if ( result!=null ) { 
                logger.log( Level.FINEST, "getChildren({0}) -> {1}", new Object[]{parent, result});
                return result;

            } else {
                listCache.put( key, new DefaultMutableTreeNode[] { } );
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
                    result= listCache.get( key );
                    logger.log( Level.FINEST, "getChildren({0}) -> {1}", new Object[]{parent, result});                    
                    return result;
                } else {
                
                    listingImmediately(parent);
                    result= listCache.get( key );
                    logger.log( Level.FINEST, "getChildren({0}) -> {1}", new Object[]{parent, result});  
                    return result;
                }
                
            }
        }
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
    
    public static void main( String[] args ) throws FileNotFoundException, UnknownHostException, FileSystem.FileSystemOfflineException {
        logger.setLevel(Level.FINE);
        for ( Handler h: logger.getHandlers() ) {
            logger.removeHandler(h);
        }
        Handler h=  new ConsoleHandler();
        h.setLevel(Level.FINE);
        logger.addHandler(h );
        //FileSystem fs= FileSystem.create("file:///home/jbf/tmp/");
        //FileSystem fs= FileSystem.create("http://autoplot.org/data/vap/");
        //FileSystem fs= FileSystem.create("http://emfisis.physics.uiowa.edu/pub/jyds/");
        FileSystem fs= FileSystem.create("http://sarahandjeremy.net/~jbf/");
        TreeModel tm= new FSTreeModel(fs) ;
        JTree mytree= new JTree( tm );
        mytree.setMinimumSize( new Dimension(400,600) );
        mytree.setPreferredSize( new Dimension(400,600) );
        JOptionPane.showMessageDialog( null, new JScrollPane(mytree), "Test FSTREE", JOptionPane.INFORMATION_MESSAGE );
        
    }
}

