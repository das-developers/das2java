/*
 * DataSetTreeModel.java
 *
 * Created on July 25, 2006, 2:17 PM
 *
 *
 */
package org.das2.util;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 * Forms a tree by connecting sub-trees.
 * @author Jeremy
 */
public class CombinedTreeModel implements TreeModel {

    List treeModels;
    List treeModelRoots;
    List<Float> treeModelSortIndexes;
    Object root = null;
    WeakHashMap sourceMap;
    List listeners;    // generally, the model should only be modified on the event thread.
    // this is useful for debugging.
    final private boolean checkEvent = false;

    public CombinedTreeModel(Object root) {
        this.root = root;
        treeModels = new ArrayList();
        treeModelRoots = new ArrayList();
        treeModelSortIndexes = new ArrayList<Float>();
        sourceMap = new WeakHashMap();
        listeners = new ArrayList();
    }

    class SubTreeModelListener implements TreeModelListener {

        private TreeModelEvent prependTreeModelEvent(TreeModelEvent e) {
            Object[] path = e.getPath();
            Object[] path2 = new Object[path.length + 1];
            path2[0] = root;
            System.arraycopy(path, 0, path2, 1, path.length);
            TreeModelEvent result = new TreeModelEvent(this, path2, e.getChildIndices(), e.getChildren());
            return result;
        }

        public void treeNodesChanged(TreeModelEvent e) {
            fireTreeNodesChanged(prependTreeModelEvent(e));
        }

        public void treeNodesInserted(TreeModelEvent e) {
            fireTreeNodesInserted(prependTreeModelEvent(e));
        }

        public void treeNodesRemoved(TreeModelEvent e) {
            fireTreeNodesRemoved(prependTreeModelEvent(e));
        }

        public void treeStructureChanged(TreeModelEvent e) {
            fireTreeStructureChanged(prependTreeModelEvent(e));
        }
    }
    SubTreeModelListener myListener = new SubTreeModelListener();

    public Object getRoot() {
        return root;
    }

    public void mountTree(TreeModel treeModel) {
        mountTree(treeModel, -1);
    }

    /**
     * mounts the tree.  Note each treeModel must have a unique root.
     */
    @SuppressWarnings("unchecked")
    public void mountTree(TreeModel treeModel, int sortIndex) {
        if (checkEvent && !EventQueue.isDispatchThread())
            throw new IllegalArgumentException("must be called from AWT thread"); // useful for debugging concurrent exception

        if (treeModelRoots.contains(treeModel.getRoot())) {
            int index = treeModelRoots.indexOf(treeModel.getRoot());
            treeModels.set(index, treeModel);
            treeModelRoots.set(index, treeModel.getRoot());
            treeModelSortIndexes.set(index, (float)sortIndex );
            TreePath path = new TreePath(root);
            treeModel.addTreeModelListener(myListener);
            fireTreeNodesChanged(path, new int[]{index}, new Object[]{treeModel.getRoot()});
        } else {
            int index= Collections.binarySearch( treeModelSortIndexes, sortIndex+0.5f );
            index= -1 - index;
            treeModels.add( index, treeModel );
            treeModelRoots.add( index, treeModel.getRoot() );
            treeModelSortIndexes.add( index, (float)sortIndex );
            TreePath path = new TreePath(root);
            treeModel.addTreeModelListener(myListener);
            fireTreeNodesInserted(path, new int[]{ index }, new Object[]{treeModel.getRoot()});
        }
    }

    public void unmountTree(TreeModel treeModel) {
        if (checkEvent && !EventQueue.isDispatchThread())
            throw new IllegalArgumentException("must be called from AWT thread"); // useful for debugging concurrent exception

        int index = treeModelRoots.indexOf(treeModel.getRoot());
        treeModels.remove(index);
        treeModelRoots.remove(index);
        TreePath path = new TreePath(root);
        fireTreeNodesRemoved(new TreeModelEvent(this, path));
    }

    public synchronized Object getChild(Object parent, int index) {
        if (checkEvent && !EventQueue.isDispatchThread())
            throw new IllegalArgumentException("must be called from AWT thread"); // useful for debugging concurrent exception
        Object result;
        TreeModel mt;
        if (parent == root) {
            mt = (TreeModel) treeModels.get(index);
            result = mt.getRoot();
        } else {
            mt = (TreeModel) sourceMap.get(parent);
            result = mt.getChild(parent, index);
        }
        sourceMap.put(result, mt);
        return result;
    }

    public int getChildCount(Object parent) {
        if (checkEvent && !EventQueue.isDispatchThread())
            throw new IllegalArgumentException("must be called from AWT thread"); // useful for debugging concurrent exception
        if (parent == root) {
            return this.treeModels.size();
        } else {
            TreeModel mt = (TreeModel) sourceMap.get(parent);
            return mt.getChildCount(parent);
        }
    }

    public boolean isLeaf(Object node) {
        if (node == root) {
            return treeModels.size() == 0;
        } else {
            TreeModel mt = (TreeModel) sourceMap.get(node);
            if ( mt==null ) {
                System.err.println("null on "+node);
                return true;
            }
            return mt.isLeaf(node);
        }
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
        Object parent = path.getPathComponent(path.getPathCount() - 2);
        Object child = path.getPathComponent(path.getPathCount() - 1);
        int index = getIndexOfChild(parent, child);
        fireTreeNodesChanged(path, new int[]{index}, new Object[]{newValue});
    }

    public int getIndexOfChild(Object parent, Object child) {
        if (parent == root) {
            for (int i = 0; i < treeModels.size(); i++) {
                if (child == ((TreeModel) treeModels.get(i)).getRoot())
                    return i;
            }
            return -1;
        } else {
            TreeModel mt = (TreeModel) sourceMap.get(parent);
            return mt.getIndexOfChild(parent, child);
        }
    }

    public void addTreeModelListener(TreeModelListener l) {
        listeners.add(l);
    }

    public void removeTreeModelListener(TreeModelListener l) {
        listeners.remove(l);
    }

    private void fireTreeNodesInserted(TreePath path,
            int[] insertedIndeces, Object[] children) {
        TreeModelEvent e = new TreeModelEvent(this, path, insertedIndeces, children);
        fireTreeNodesInserted(e);
    }

    private void fireTreeNodesChanged(TreePath path,
            int[] changedIndeces, Object[] children) {
        TreeModelEvent e = new TreeModelEvent(this, path, changedIndeces, children);
        fireTreeNodesChanged(e);
    }

    private void fireTreeNodesChanged(TreeModelEvent e) {
        for (Iterator i = listeners.iterator(); i.hasNext();) {
            ((TreeModelListener) i.next()).treeNodesChanged(e);
        }
    }

    private void fireTreeNodesInserted(TreeModelEvent e) {
        for (Iterator i = listeners.iterator(); i.hasNext();) {
            ((TreeModelListener) i.next()).treeNodesInserted(e);
        }
    }

    public void fireTreeNodesRemoved(TreeModelEvent e) {
        for (Iterator i = listeners.iterator(); i.hasNext();) {
            ((TreeModelListener) i.next()).treeNodesRemoved(e);
        }
    }

    public void fireTreeStructureChanged(TreeModelEvent e) {
        for (Iterator i = listeners.iterator(); i.hasNext();) {
            ((TreeModelListener) i.next()).treeStructureChanged(e);
        }
    }
}
