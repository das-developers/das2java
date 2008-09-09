/* File: DataSetBrowser.java
 * Copyright (C) 2002-2003 The University of Iowa
 * Created by: Jeremy Faden <jbf@space.physics.uiowa.edu>
 *             Jessica Swanner <jessica@space.physics.uiowa.edu>
 *             Edward E. West <eew@space.physics.uiowa.edu>
 *
 * This file is part of the das2 library.
 *
 * das2 is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.uiowa.physics.pw.das.components;

import org.das2.DasException;
import org.das2.DasIOException;
import edu.uiowa.physics.pw.das.client.DasServer;

import javax.swing.*;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;

/**
 * JTree veiw of a Das2Server's available data sets, with drag-n-drop of data set id's.
 */
public class DataSetBrowser extends JPanel implements DragSourceListener, DragGestureListener {
    
    DasServer dasServer;
    JTree tree;
    TreeModel dataSetListTreeModel;
    
    /** Creates a new instance of DataSetBrowser */
    public DataSetBrowser(DasServer dasServer) {
        super();
        this.dasServer= dasServer;
        try {
            dataSetListTreeModel= dasServer.getDataSetList();
        } catch ( DasIOException e ) {
            System.out.println(e);
        } catch ( DasException e ) {
            System.out.println(e);
        }
        
        this.setLayout(new java.awt.BorderLayout());
        
        tree= new JTree(dataSetListTreeModel);
        
        DragSource ds= DragSource.getDefaultDragSource();
        DragGestureRecognizer dgr=
                ds.createDefaultDragGestureRecognizer(tree,DnDConstants.ACTION_COPY_OR_MOVE,
                this );
        
        this.add(new JScrollPane(tree),java.awt.BorderLayout.CENTER);
        
    }
    
    public String getSelectedDataSetId() {
        TreePath tp= tree.getLeadSelectionPath();
        TreeNode tn = (TreeNode)tp.getLastPathComponent();        
        if (tn.isLeaf()) {
            String s = dasServer.getURL()+"?"+tp.getPathComponent(1);
            for (int index = 2; index < tp.getPathCount(); index++)
                s = s + "/" + tp.getPathComponent(index);
            return s;
        } else {
            return null;
        }        
    }
    
    public void dragDropEnd(java.awt.dnd.DragSourceDropEvent dragSourceDropEvent) {
    }
    
    public void dragEnter(java.awt.dnd.DragSourceDragEvent dragSourceDragEvent) {
    }
    
    public void dragExit(java.awt.dnd.DragSourceEvent dragSourceEvent) {
    }
    
    public void dragGestureRecognized(java.awt.dnd.DragGestureEvent dragGestureEvent) {
        String s= getSelectedDataSetId();
        if ( s!=null ) {
            Transferable t= new StringSelection(s);
            dragGestureEvent.startDrag(null,t,this);
        }
    }
    
    public void dragOver(java.awt.dnd.DragSourceDragEvent dragSourceDragEvent) {
    }
    
    public void dropActionChanged(java.awt.dnd.DragSourceDragEvent dragSourceDragEvent) {
    }
    
}
