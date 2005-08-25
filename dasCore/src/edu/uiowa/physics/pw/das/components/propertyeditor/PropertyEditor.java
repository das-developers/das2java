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

import edu.uiowa.physics.pw.das.beans.BeansUtil;
import edu.uiowa.physics.pw.das.components.treetable.TreeTableCellRenderer;
import edu.uiowa.physics.pw.das.components.treetable.TreeTableModel;
import edu.uiowa.physics.pw.das.dasml.SerializeUtil;
import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.graph.DasCanvas;
import edu.uiowa.physics.pw.das.util.DasExceptionHandler;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;



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
        BeansUtil.registerPropertyEditors();
    }
    
    private JTable table;
    
    private JButton closeButton;
    
    private JDialog dialog;
    
    private Object bean;
    
    public PropertyEditor( Object bean ) {
        setLayout(new BorderLayout());
        this.bean= bean;
        PropertyTreeNode root = new PropertyTreeNode(bean);
        DefaultTreeModel treeModel = new DefaultTreeModel(root, true);
        TreeTableCellRenderer tree = new TreeTableCellRenderer(treeModel);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        TreeTableModel model = new TreeTableModel(root, tree);
        table = new JTable(model);
        add(new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);
        
        initButtonPanel( bean instanceof DasCanvas );
        
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
    
    private Action createSaveAction( final Object bean ) {
        return new AbstractAction("Save") {
            public void actionPerformed( ActionEvent ev ) {
                try {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setFileFilter( new javax.swing.filechooser.FileFilter() {
                        public boolean accept(File f ) { return f.toString().matches(".*\\.das2PropertySheet"); }
                        public String getDescription() { return "*.das2PropertySheet"; }
                    });
                    chooser.setSelectedFile( new File( "default.das2PropertySheet" ) );
                    int result = chooser.showSaveDialog(PropertyEditor.this);
                    if (result == JFileChooser.APPROVE_OPTION) {
                        Document document= DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
                        Element element= SerializeUtil.getDOMElement(document, bean );
                        document.appendChild( element );
                        OutputStream out= new FileOutputStream( chooser.getSelectedFile() );
                        
                        StringWriter writer = new StringWriter();
                        OutputFormat format = new OutputFormat(org.apache.xml.serialize.Method.XML, "UTF-8", true);
                        XMLSerializer serializer = new XMLSerializer( new OutputStreamWriter(out), format);
                        serializer.serialize(document);
                        out.close();
                    }
                } catch ( Exception e ) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
    
    private static Document readDocument( File file ) throws IOException, ParserConfigurationException, SAXException {
        InputStream in= new FileInputStream(file);
        InputSource source = new InputSource();
        source.setCharacterStream(new InputStreamReader(in));
        DocumentBuilder builder;
        ErrorHandler eh= null;
        DocumentBuilderFactory domFactory= DocumentBuilderFactory.newInstance();
        builder = domFactory.newDocumentBuilder();
        builder.setErrorHandler(eh);
        Document document= builder.parse(source);
        return document;
    }
       
    private Action createLoadAction( final Object bean ) {
        return new AbstractAction("Load") {
            public void actionPerformed( ActionEvent ev ) {
                try {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setFileFilter( new javax.swing.filechooser.FileFilter() {
                        public boolean accept(File f ) { return f.toString().matches(".*\\.das2PropertySheet"); }
                        public String getDescription() { return "*.das2PropertySheet"; }
                    });
                    int result = chooser.showOpenDialog(PropertyEditor.this);
                    if (result == JFileChooser.APPROVE_OPTION) {
                        try {
                            Document document= readDocument( chooser.getSelectedFile() );
                            Element element= document.getDocumentElement();
                            SerializeUtil.processElement(element,bean );
                        } catch ( IOException e ) {
                            throw new RuntimeException(e);
                        } catch ( ParserConfigurationException e ) {
                            throw new RuntimeException(e);
                        } catch ( SAXException e ) {
                            throw new RuntimeException(e);
                        }
                        
                    }
                } catch ( Exception e ) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
    private void initButtonPanel( boolean saveLoadButton ) {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));        
        if ( false ) { //saveLoadButton ) {
            JButton saveButton=new JButton(createSaveAction(this.bean));
            buttonPanel.add(saveButton);
            JButton loadButton=new JButton(createLoadAction(this.bean));
            buttonPanel.add(loadButton);            
        }
        final JButton apply = new JButton("Apply Changes");
        closeButton = new JButton("Dismiss");
        ActionListener al = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == apply) {
                    globalApplyChanges();
                } else if (e.getSource() == closeButton) {
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
        } catch (InvocationTargetException ite) {
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
            } else if (top instanceof JDialog) {
                dialog = new JDialog((JDialog)top);
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
    
    public void doLayout() {
        if (SwingUtilities.isDescendingFrom(this, dialog)) {
            closeButton.setVisible(true);
        } else {
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
