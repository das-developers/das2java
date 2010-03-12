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

        editableTypes = Collections.unmodifiableSet(set);
    }
    private JTable table;
    private JButton closeButton;
    private JDialog dialog;
    private Object bean;
    /* row of the last mouse click.  This object is the fellow who is edited when
     * applying properties to a group
     */
    private int focusRow = 0;
    private JPopupMenu popupMenu;
    private Logger logger = DasLogger.getLogger(DasLogger.GUI_LOG);

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
    }

    public PropertyEditor(Object bean) {
        this(new PropertyTreeNode(bean), bean);
        if (bean instanceof PropertyTreeNodeInterface) {
            throw new IllegalArgumentException("whoops!");
        }
    }

    public static PropertyEditor createPeersEditor(Object leader, Object[] peers) {
        Class leaderClass= leader.getClass();
        for ( int i=0; i<peers.length; i++ ) {
            if ( !leaderClass.isInstance(peers[i]) )
                throw new IllegalArgumentException( "child is not instance of leader class: "+peers[i].getClass().getName()+", should be "+leaderClass.getName() );
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

            public void valueChanged(ListSelectionEvent e) {
                focusRow = table.getSelectedRow(); // we could do a better job here

                logger.fine("focusRow=" + focusRow);
            }
        };
    }

    private KeyListener getKeyListener() {
        KeyAdapter ka;
        return new KeyAdapter() {

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

/*    private Action createSaveAction(final Object bean) {
        return new AbstractAction("Save") {

            public void actionPerformed(ActionEvent ev) {
                try {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {

                        public boolean accept(File f) {
                            return f.toString().matches(".*\\.das2PropertySheet");
                        }

                        public String getDescription() {
                            return "*.das2PropertySheet";
                        }
                    });
                    chooser.setSelectedFile(new File("default.das2PropertySheet"));
                    int result = chooser.showSaveDialog(PropertyEditor.this);
                    if (result == JFileChooser.APPROVE_OPTION) {
                        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
                        Element element = SerializeUtil.getDOMElement(document, bean);
                        document.appendChild(element);
                        OutputStream out = new FileOutputStream(chooser.getSelectedFile());

                        StringWriter writer = new StringWriter();
						DOMImplementation impl = document.getImplementation();
						DOMImplementationLS ls = (DOMImplementationLS)impl.getFeature("LS", "3.0");
						LSSerializer serializer = ls.createLSSerializer();
						LSOutput output = ls.createLSOutput();
						output.setEncoding("UTF-8");
						output.setByteStream(out);
						serializer.write(document, output);

                        //OutputFormat format = new OutputFormat(org.apache.xml.serialize.Method.XML, "UTF-8", true);
                        //XMLSerializer serializer = new XMLSerializer( new OutputStreamWriter(out), format);
                        //serializer.serialize(document);
                        out.close();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    private static Document readDocument(File file) throws IOException, ParserConfigurationException, SAXException {
        InputStream in = new FileInputStream(file);
        InputSource source = new InputSource();
        source.setCharacterStream(new InputStreamReader(in));
        DocumentBuilder builder;
        ErrorHandler eh = null;
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        builder = domFactory.newDocumentBuilder();
        builder.setErrorHandler(eh);
        Document document = builder.parse(source);
        return document;
    }

    private Action createLoadAction(final Object bean) {
        return new AbstractAction("Load") {

            public void actionPerformed(ActionEvent ev) {
                try {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {

                        public boolean accept(File f) {
                            return f.toString().matches(".*\\.das2PropertySheet");
                        }

                        public String getDescription() {
                            return "*.das2PropertySheet";
                        }
                    });
                    int result = chooser.showOpenDialog(PropertyEditor.this);
                    if (result == JFileChooser.APPROVE_OPTION) {
                        try {
                            Document document = readDocument(chooser.getSelectedFile());
                            Element element = document.getDocumentElement();
                            SerializeUtil.processElement(element, bean);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        } catch (ParserConfigurationException e) {
                            throw new RuntimeException(e);
                        } catch (SAXException e) {
                            throw new RuntimeException(e);
                        }

                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
*/
    private Action getEditSelectedAction() {
        return new AbstractAction("Edit Selected") {

            public void actionPerformed(ActionEvent e) {
                PropertyEditor p;
                TreeTableModel model = (TreeTableModel) table.getModel();
                PropertyTreeNodeInterface node = (PropertyTreeNodeInterface) model.getNodeForRow(focusRow);
                int[] selected = table.getSelectedRows();                
                if (selected.length == 1) {
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
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        if (saveLoadButton) {
            //JButton saveButton = new JButton(createSaveAction(this.bean));
            //buttonPanel.add(saveButton);
            //JButton loadButton = new JButton(createLoadAction(this.bean));
            //buttonPanel.add(loadButton);
        }
        final JButton apply = new JButton("Apply Changes");
        closeButton = new JButton("Dismiss");

        ActionListener al = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == apply) {
                    globalApplyChanges();
                    refresh();
                } else if (e.getSource() == closeButton) {
                    dismissDialog();
                }
            }
        };
        apply.addActionListener(al);
        closeButton.addActionListener(al);

        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                refresh();
            }
        });

        buttonPanel.add(refresh);
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(apply);
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void refresh() {
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

    private void dismissDialog() {
        PropertyTreeNodeInterface root = (PropertyTreeNodeInterface) ((TreeTableModel) table.getModel()).getRoot();
        if (root.isDirty()) {
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
            public void windowClosing(WindowEvent e) {
                dismissDialog();
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
        if (dialog == null) {
            Container top = (c == null ? null : SwingUtilities.getAncestorOfClass(Window.class, c));
            if (top instanceof JFrame) {
                dialog = new JDialog((JFrame) top);
            } else if (top instanceof JDialog) {
                dialog = new JDialog((JDialog) top);
            } else {
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

    /**
     * display the dialog, and use the given image for the icon.
     * @param c
     * @param icon
     */
    public void showDialog( Component c, String title, Image icon ) {
        showDialog(c);
        dialog.setTitle(title);
        dialog.setIconImage(icon);
    }

    public void doLayout() {
        if (SwingUtilities.isDescendingFrom(this, dialog)) {
            closeButton.setVisible(true);
        } else {
            closeButton.setVisible(false);
        }
        super.doLayout();
    }

    class PropertyTableMouseListener extends MouseAdapter {

        public void mouseClicked(MouseEvent event) {
            Point p = event.getPoint();
            int row = table.rowAtPoint(p);
            int column = table.columnAtPoint(p);
            TreeTableModel model = (TreeTableModel) table.getModel();
            PropertyTreeNodeInterface node = (PropertyTreeNodeInterface) model.getNodeForRow(row);

            focusRow = row;
            int modifiers = event.getModifiers() & (MouseEvent.SHIFT_MASK | MouseEvent.CTRL_MASK);

            if (event.getButton() == MouseEvent.BUTTON1 && modifiers == 0 && !node.isLeaf()) {
                model.toggleExpanded(row);
            }
        }

        public void mousePressed(MouseEvent event) {
            Point p = event.getPoint();
            focusRow = table.rowAtPoint(p);
            if (event.getButton() == MouseEvent.BUTTON3) {
                popupMenu.show(PropertyEditor.this.table, event.getX(), event.getY());
            }
        }
    }
}
