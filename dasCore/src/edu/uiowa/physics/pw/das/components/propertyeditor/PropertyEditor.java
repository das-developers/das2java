/* File: PropertyEditor.java
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

package edu.uiowa.physics.pw.das.components.propertyeditor;

import edu.uiowa.physics.pw.das.components.DatumEditor;
import edu.uiowa.physics.pw.das.components.treetable.TreeTableCellRenderer;
import edu.uiowa.physics.pw.das.components.treetable.TreeTableModel;
import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.graph.PsymConnector;
import edu.uiowa.physics.pw.das.util.DasExceptionHandler;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyEditorManager;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;



/**
 * This class implements a Hierarchical property editor
 *
 * @author Edward West
 */
public class PropertyEditor extends JComponent {
    
    static final Set editableTypes;
    static {
        HashSet set = new HashSet();
        
        //Primitives
        set.add(byte.class);
        set.add(short.class);
        set.add(int.class);
        set.add(long.class);
        set.add(float.class);
        set.add(double.class);
        set.add(boolean.class);
        
        //Object types
        set.add(String.class);
        set.add(Datum.class);
        set.add(Color.class);
        //set.add(PsymConnector.class);
        
        editableTypes = Collections.unmodifiableSet(set);
    }
    
    /*
     * Set up the custom editors.
     */
    static {
        PropertyEditorManager.registerEditor(Color.class, ColorEditor.class);
        PropertyEditorManager.registerEditor(Datum.class, DatumEditor.class);
        PropertyEditorManager.registerEditor(Boolean.TYPE, BooleanEditor.class);
        PropertyEditorManager.registerEditor(Boolean.class, BooleanEditor.class);
        PropertyEditorManager.registerEditor(PsymConnector.class, EnumerationEditor.class);
    }
    
    private JTable table;
    
    private JButton closeButton;
    
    private JDialog dialog;
    
    public PropertyEditor(Object bean) {
        setLayout(new BorderLayout());
        PropertyTreeNode root = new PropertyTreeNode(bean);
        DefaultTreeModel treeModel = new DefaultTreeModel(root, true);
        TreeTableCellRenderer tree = new TreeTableCellRenderer(treeModel);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        TreeTableModel model = new TreeTableModel(root, tree);
        table = new JTable(model);
        add(new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);

        initButtonPanel();

        PropertyCellRenderer valueRenderer = new PropertyCellRenderer();
        //PropertyCellEditor editor = new PropertyCellEditor(tree);
        PropertyEditorAdapter editor = new PropertyEditorAdapter();
        int cellHeight= 21;  // c.getPreferredSize().height;
        table.setRowHeight( cellHeight );
        tree.setRowHeight( cellHeight );
        tree.setCellRenderer(valueRenderer);
        table.getColumnModel().getColumn(0).setCellRenderer(tree);
        table.getColumnModel().getColumn(1).setCellRenderer(valueRenderer);
        table.setDefaultEditor(Object.class, editor);
        table.addMouseListener(new PropertyTableMouseListener());
    }
    
    private void initButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        final JButton apply = new JButton("Apply Changes");
        closeButton = new JButton("Dismiss");
        ActionListener al = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == apply) {
                    globalApplyChanges();
                }
                else if (e.getSource() == closeButton) {
                    dismissDialog();
                }
            }
        };
        apply.addActionListener(al);
        closeButton.addActionListener(al);
        buttonPanel.add(apply);
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void globalApplyChanges() {
        TreeTableModel model = (TreeTableModel)table.getModel();
        PropertyTreeNode root = (PropertyTreeNode)model.getRoot();
        try {
            root.flush();
        }
        catch (InvocationTargetException ite) {
            DasExceptionHandler.handle(ite.getCause());
        }
    }
    
    private void dismissDialog() {
        PropertyTreeNode root = (PropertyTreeNode)((TreeTableModel)table.getModel()).getRoot();
        if (root.isDirty()) {
            String[] message = new String[] {
                "You have unsaved changes",
                "Would you like to apply them?"
            };
            int result = JOptionPane.showConfirmDialog(this, message, "", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (result == JOptionPane.CANCEL_OPTION) {
                return;
            }
            if (result == JOptionPane.YES_OPTION) {
                globalApplyChanges();
            }
        }
        dialog.setVisible(false);
        dialog.dispose();
    }
    
    public void showDialog(Component c) {
        if (dialog == null) {
            Container top = ( c == null ? null : SwingUtilities.getAncestorOfClass(Window.class, c));
            if (top instanceof JFrame) {
                dialog = new JDialog((JFrame)top);
            }
            else if (top instanceof JDialog) {
                dialog = new JDialog((JDialog)top);
            }
            else { 
                dialog = new JDialog();
            }
            
            dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            dialog.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    dismissDialog();
                }
            });
            dialog.setContentPane(this);
            dialog.pack();
        }
        if (c != null) {
            dialog.setLocationRelativeTo(c);
        }
        dialog.setVisible(true);
    }
    
    public void doLayout() {
        if (SwingUtilities.isDescendingFrom(this, dialog)) {
            closeButton.setVisible(true);
        }
        else {
            closeButton.setVisible(false);
        }
        super.doLayout();
    }
    
    class PropertyTableMouseListener extends MouseAdapter {
        
        public void mouseClicked(MouseEvent e) {
            Point p = e.getPoint();
            int row = table.rowAtPoint(p);
            int column = table.columnAtPoint(p);
            TreeTableModel model = (TreeTableModel)table.getModel();
            PropertyTreeNode node = (PropertyTreeNode)model.getNodeForRow(row);
            if (!node.isLeaf()) {
                model.toggleExpanded(row);
            }
        }
        
    }
    
}
