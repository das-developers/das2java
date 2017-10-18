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
package org.das2.components.propertyeditor;

import java.util.logging.Level;
import org.das2.components.treetable.TreeTableCellRenderer;
import org.das2.components.treetable.TreeTableModel;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.graph.DasCanvas;
import org.das2.system.DasLogger;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.DefaultTreeModel;
//import org.apache.xml.serialize.OutputFormat;
//import org.apache.xml.serialize.XMLSerializer;

/**
 * This class implements a Hierarchical property editor
 *
 * @author Edward West
 */
public class PropertyEditor extends JComponent {

    static final Set editableTypes;
    public final static Object MULTIPLE= new Object();

    

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
        set.add(DatumRange.class);
        set.add(Color.class);
        //set.add(PsymConnector.class);
        set.add(org.das2.graph.DasColorBar.Type.class);
        set.add(org.das2.graph.SpectrogramRenderer.RebinnerEnum.class);
        editableTypes = Collections.unmodifiableSet(set);
    }
    private JTable table;
    private JButton closeButton;
    private JPanel buttonPanel;
    private JDialog dialog;
    private Object bean;
    /* row of the last mouse click.  This object is the fellow who is edited when
     * applying properties to a group
     */
    private int focusRow = 0;
    private JPopupMenu popupMenu;
    private static final Logger logger = DasLogger.getLogger(DasLogger.GUI_LOG);

    private PropertyEditor(PropertyTreeNodeInterface root, Object bean) {
        this.bean = bean;
        setLayout(new BorderLayout());
        this.bean = bean;

        DefaultTreeModel treeModel = new DefaultTreeModel(root, true);
        root.setTreeModel(treeModel);
        TreeTableCellRenderer tree = new TreeTableCellRenderer(treeModel);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        TreeTableModel model = new TreeTableModel(root, tree);
        table = new JTable(model);
        table.setAutoCreateColumnsFromModel(false);
        table.putClientProperty("terminateEditOnFocusLost", true);

        add(new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);

        initButtonPanel(bean instanceof DasCanvas);
        initPopupMenu();

        PropertyCellRenderer valueRenderer = new PropertyCellRenderer();
        //PropertyCellEditor editor = new PropertyCellEditor(tree);
        PropertyEditorAdapter editor = new PropertyEditorAdapter();
        int cellHeight = 21;  // c.getPreferredSize().height;

        table.setRowHeight(cellHeight);
        tree.setRowHeight(cellHeight);
        tree.setCellRenderer(valueRenderer);
        table.getColumnModel().getColumn(0).setCellRenderer(tree);
        table.getColumnModel().getColumn(1).setCellRenderer(valueRenderer);
        for ( int i=2; i<table.getColumnCount(); i++ ) {
            table.getColumnModel().getColumn(i).setCellRenderer(valueRenderer);
        }
        table.getColumnModel().getColumn(0).setMaxWidth(250);
        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.setDefaultEditor(Object.class, editor);
        table.addMouseListener(new PropertyTableMouseListener());
        table.setSurrendersFocusOnKeystroke(true);
        table.addKeyListener(getKeyListener());
        addActions(table);
        table.getSelectionModel().addListSelectionListener(getListSelectionListener());

        if ( table.getColumnCount()>5 ) {
            this.setPreferredSize( new Dimension( (int)Math.min( Toolkit.getDefaultToolkit().getScreenSize().getWidth(), table.getColumnCount()*100 ), 500 ) );
        }
    }

    /**
     * create a PropertyEditor for the bean.
     * @param bean a java bean
     */
    public PropertyEditor(Object bean) {
        this(new PropertyTreeNode(bean), bean);
        if (bean instanceof PropertyTreeNodeInterface) { // this was some bug
            throw new IllegalArgumentException("whoops!");
        }
    }

    /**
     * create a PropertyEditor when we are editing several beans at once.
     * @param leader the leader bean, which is used to get settings for all components.
     * @param peers the peers, which must all be instances of the leader's class.
     * @return a PropertyEditor for editing several beans at once.
     */
    public static PropertyEditor createPeersEditor(Object leader, Object[] peers) {
        Class leaderClass= leader.getClass();
        for (Object peer : peers) {
            if (!leaderClass.isInstance(peer)) {
                throw new IllegalArgumentException("child is not instance of leader class: " + peer.getClass().getName() + ", should be " + leaderClass.getName());
            }
        }
        PropertyTreeNode[] peerNodes = new PropertyTreeNode[peers.length];
        for (int i = 0; i < peers.length; i++) {
            peerNodes[i] = new PropertyTreeNode(peers[i]);
        }
        PeerPropertyTreeNode root = new PeerPropertyTreeNode(null,
                new PropertyTreeNode(leader),
                peerNodes);
        return new PropertyEditor(root, null);
    }

    private void addActions(final JTable table) {
        table.getActionMap().put("MY_EDIT", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                table.editCellAt(focusRow, 1);
            }
        });
        table.getInputMap().put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false),
                "MY_EDIT");
    }

    private ListSelectionListener getListSelectionListener() {
        return new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                focusRow = table.getSelectedRow(); // we could do a better job here
                logger.log(Level.FINE, "focusRow={0}", focusRow);
            }
        };
    }

    private KeyListener getKeyListener() {
        return new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent event) {
                logger.fine(String.valueOf(event));
                if (event.getKeyCode() == KeyEvent.VK_RIGHT) {
                    TreeTableModel model = (TreeTableModel) table.getModel();
                    model.expand(focusRow);
                } else if (event.getKeyCode() == KeyEvent.VK_LEFT) {
                    TreeTableModel model = (TreeTableModel) table.getModel();
                    model.collapse(focusRow);
                }
            }
        };
    }

    private Action getEditSelectedAction() {
        return new AbstractAction("Edit Selected") {
            @Override
            public void actionPerformed(ActionEvent e) {
                PropertyEditor p;
                TreeTableModel model = (TreeTableModel) table.getModel();
                PropertyTreeNodeInterface node = (PropertyTreeNodeInterface) model.getNodeForRow(focusRow);
                int[] selected = table.getSelectedRows(); 
                if ( selected.length==0 ) {
                    JOptionPane.showMessageDialog( table, "No items selected" );
                    return;
                } else if (selected.length == 1) {
                    p = new PropertyEditor(node.getValue());
                } else {
                    int ileader= table.getSelectedRow();
                    java.util.List<Object> peers = new ArrayList(selected.length);
                    Object leader= ((PropertyTreeNode) model.getNodeForRow(ileader)).getValue();
                    for (int i = 0; i < selected.length; i++) {
                        Object peer= ((PropertyTreeNode) model.getNodeForRow(selected[i])).getValue();
                        if ( leader.getClass().isInstance(peer) ) peers.add(peer);
                    }
                    if ( peers.size()==1 && peers.get(0)==leader ) { //bug where hidden selected parent is leader
                        peers.remove(leader);
                        Object newLeader= ((PropertyTreeNode) model.getNodeForRow(selected[0])).getValue();
                        if ( newLeader==leader ) newLeader= ((PropertyTreeNode) model.getNodeForRow(selected[1])).getValue();
                        leader= newLeader;
                        for (int i = 0; i < selected.length; i++) {
                            Object peer= ((PropertyTreeNode) model.getNodeForRow(selected[i])).getValue();
                            if ( leader.getClass().isInstance(peer) ) peers.add(peer);
                        }
                    }
                    p = createPeersEditor( leader, peers.toArray() );
                }
                p.showDialog(PropertyEditor.this);

            }
        };
    }

    private void initPopupMenu() {
        popupMenu = new JPopupMenu();
        popupMenu.add(getEditSelectedAction());
    }

    private void initButtonPanel(boolean saveLoadButton) {
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        if (saveLoadButton) {
            //JButton saveButton = new JButton(createSaveAction(this.bean));
            //buttonPanel.add(saveButton);
            //JButton loadButton = new JButton(createLoadAction(this.bean));
            //buttonPanel.add(loadButton);
        }
        final JButton cancel = new JButton("Cancel");

        final JButton apply = new JButton("Apply");

        closeButton = new JButton("OK");

        ActionListener al = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ef) {
                SwingUtilities.invokeLater( new Runnable() { // allow focus event to occur first.
                    @Override
                    public void run() {
                        if (ef.getSource() == apply) {
                            globalApplyChanges();
                            refresh();
                        } else if (ef.getSource() == closeButton) {
                            globalApplyChanges();
                            dismissDialog(false);
                        } else if ( ef.getSource()==cancel ) {
                            dismissDialog(false);
                        }
                    }
                } );
            }
        };
        apply.addActionListener(al);
        closeButton.addActionListener(al);
        cancel.addActionListener(al);

        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refresh();
            }
        });

        buttonPanel.add(refresh);
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(cancel);
        buttonPanel.add(apply);
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * request a refresh, the same as if the "refresh" button was pressed.
     */
    public void refresh() {
        TreeTableModel model = (TreeTableModel) table.getModel();
        PropertyTreeNodeInterface root = (PropertyTreeNodeInterface) model.getRoot();
        root.refresh();
        model.fireTableDataChanged();
    }

    private void globalApplyChanges() {
        TreeTableModel model = (TreeTableModel) table.getModel();
        PropertyTreeNodeInterface root = (PropertyTreeNodeInterface) model.getRoot();
        root.flush();
    }

    private void dismissDialog(boolean checkDirty) {
        PropertyTreeNodeInterface root = (PropertyTreeNodeInterface) ((TreeTableModel) table.getModel()).getRoot();
        if (checkDirty && root.isDirty()) {
            String[] message = new String[]{
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

    public void showModalDialog(Component c) {
        Container top = (c == null ? null : SwingUtilities.getAncestorOfClass(Window.class, c));
        if (top instanceof JFrame) {
            dialog = new JDialog((JFrame) top);
        } else if (top instanceof JDialog) {
            dialog = new JDialog((JDialog) top);
        } else {
            dialog = new JDialog();
        }
        dialog.setModal(true);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        dismissDialog(true);
                    }
                } );
            }
        });
        dialog.setContentPane(this);
        dialog.pack();
        if (c != null) {
            dialog.setLocationRelativeTo(c);
        }
        dialog.setVisible(true);        
    }

    public void showDialog(Component c) {
        getDialog(c);
        if (c != null) {
            dialog.setLocationRelativeTo(c);
        }
        dialog.setVisible(true);
    }

    /**
     * allow clients to get at the dialog so it can be positioned.
     * @param c parent, if initializing.
     * @return the dialog.
     */
    public JDialog getDialog( Component c ) {
        if (dialog == null) {
            Container top=null;
            if ( c!=null ) {
                top = SwingUtilities.getAncestorOfClass(Window.class, c);
                if ( top==null && c instanceof JFrame ) top= (JFrame)c;
            }
            if (top instanceof JFrame) {
                dialog = new JDialog((JFrame) top);
            } else if (top instanceof JDialog) {
                dialog = new JDialog((JDialog) top);
            } else {
                dialog = new JDialog();
            }     
            if ( this.bean==null ) {
                dialog.setTitle("Property Editor");
            } else {
                dialog.setTitle("Property Editor for "+this.bean );
            }

            dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            dialog.addWindowListener(new WindowAdapter() {

                @Override
                public void windowClosing(WindowEvent e) {
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            dismissDialog(true);
                        }
                    } );
                }
            });
            dialog.setContentPane(this);
            dialog.pack();
            
        } else {
            if ( c!=dialog.getParent()) {
                logger.warning("properties dialog parent cannot change.");
            }
        }
        return dialog;
    }
    
    /**
     * display the dialog, and use the given image for the icon.
     * @param c the parent focus
     * @param title the dialog title.
     * @param icon the icon branding the application.
     */
    public void showDialog( Component c, String title, Image icon ) {
        showDialog(c);
        dialog.setTitle(title);
        //dialog.setIconImage(icon);  // java6
    }

    @Override
    public void doLayout() {
        if (SwingUtilities.isDescendingFrom(this, dialog)) {
            closeButton.setVisible(true);
        } else {
            closeButton.setVisible(false);
        }
        super.doLayout();
    }

    /**
     * add a save button, and perform the given action.
     * @param abstractAction the action to perform.
     */
    public void addSaveAction(final AbstractAction abstractAction) {
        buttonPanel.add( new JButton( new AbstractAction( "Save") {
            @Override
            public void actionPerformed(ActionEvent e) {
                globalApplyChanges();
                refresh();
                abstractAction.actionPerformed(e);
            }
        }));
    }

    class PropertyTableMouseListener extends MouseAdapter {

        @Override
        public void mouseClicked(MouseEvent event) {
            Point p = event.getPoint();
            int row = table.rowAtPoint(p);
            TreeTableModel model = (TreeTableModel) table.getModel();
            PropertyTreeNodeInterface node = (PropertyTreeNodeInterface) model.getNodeForRow(row);

            focusRow = row;
            int modifiers = event.getModifiers() & (MouseEvent.SHIFT_MASK | MouseEvent.CTRL_MASK);

            if (event.getButton() == MouseEvent.BUTTON1 && modifiers == 0 && !node.isLeaf()) {
                model.toggleExpanded(row);
            }
        }

        @Override
        public void mousePressed(MouseEvent event) {
            Point p = event.getPoint();
            focusRow = table.rowAtPoint(p);
            if (event.isPopupTrigger()) {
                popupMenu.show(PropertyEditor.this.table, event.getX(), event.getY());
            }
        }

        @Override
        public void mouseReleased(MouseEvent event) {
            Point p = event.getPoint();
            focusRow = table.rowAtPoint(p);
            if (event.isPopupTrigger()) {
                popupMenu.show(PropertyEditor.this.table, event.getX(), event.getY());
            }
        }
    }
}
