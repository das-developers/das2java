/* File: CommandBlockEditor.java
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

package org.das2.dasml;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.TableCellEditor;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;
import java.util.List;

/**
 *
 * @author  eew
 */
public class CommandBlockEditor extends JButton implements TableCellEditor, java.beans.PropertyEditor {
    
    EventListenerList listenerList = new EventListenerList();

    JDialog dialog;
    
    JPanel contentPanel;
    
    JCheckBox enable;
    
    JTree commandTree;
    
    CommandBlockTreeModel commandBlockTreeModel;
    
    MultiPurposeListener listener;
    
    JButton commitChanges;
    
    JButton cancelEdit;
    
    JPanel editorPanel;
    
    CardLayout switcher;
    
    JTextField x1Field;
    
    JLabel x1FieldLabel;
    
    JButton x1Commit, x1Cancel;
    
    JTextField x2Field1, x2Field2;
    
    JLabel x2Field1Label, x2Field2Label;
    
    JButton x2Commit, x2Cancel;
    
    JButton newCommand;
    
    JButton editCommand;
    
    JButton removeCommand;
    
    JButton moveUpCommand;
    
    JButton moveDownCommand;
    
    /** Creates a new instance of CommandBlockEditor */
    public CommandBlockEditor() {
        super("edit");
        
        listener = new MultiPurposeListener();
        addActionListener(listener);
        
        commandBlockTreeModel = new CommandBlockTreeModel(new CommandBlock());
        commandTree = new JTree(commandBlockTreeModel);
        commandTree.setRootVisible(true);
        commandTree.setShowsRootHandles(false);
        commandTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        commandTree.setVisibleRowCount(10);
        commandTree.setCellRenderer(new CommandRenderer());
        
        contentPanel = new JPanel(new BorderLayout());
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        
        centerPanel.add(new JScrollPane(commandTree, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS), BorderLayout.CENTER);
        centerPanel.add(editorPanel = initEditorPanels(), BorderLayout.SOUTH);
        centerPanel.add(initButtonPanel(), BorderLayout.EAST);
        
        contentPanel.add(centerPanel, BorderLayout.CENTER);
        
        enable = new JCheckBox("Enabled", false);
        setTopEnabled(false);
        enable.addActionListener(listener);
        contentPanel.add(enable, BorderLayout.NORTH);
        
        JPanel dialogButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        commitChanges = new JButton("Commit Changes");
        commitChanges.addActionListener(listener);
        cancelEdit = new JButton("Cancel Changes");
        cancelEdit.addActionListener(listener);
        dialogButtons.add(commitChanges);
        dialogButtons.add(cancelEdit);
        
        contentPanel.add(dialogButtons, BorderLayout.SOUTH);
    }
    
    private JPanel initButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        newCommand = new JButton("new");
        editCommand = new JButton("edit");
        removeCommand = new JButton("remove");
        moveUpCommand = new JButton("move up");
        moveDownCommand = new JButton("move down");
        Dimension d = moveDownCommand.getPreferredSize();
        newCommand.setPreferredSize(d);
        newCommand.setMaximumSize(d);
        editCommand.setPreferredSize(d);
        editCommand.setMaximumSize(d);
        removeCommand.setPreferredSize(d);
        removeCommand.setMaximumSize(d);
        moveUpCommand.setPreferredSize(d);
        moveUpCommand.setMaximumSize(d);
        newCommand.addActionListener(listener);
        editCommand.addActionListener(listener);
        removeCommand.addActionListener(listener);
        moveUpCommand.addActionListener(listener);
        moveDownCommand.addActionListener(listener);
        panel.add(newCommand);
        panel.add(editCommand);
        panel.add(removeCommand);
        panel.add(moveUpCommand);
        panel.add(moveDownCommand);
        return panel;
    }
    
    void addCommand(CommandBlock.Command c) {
        TreePath selection = commandTree.getSelectionPath();
        CommandBlock parent;
        int index;
        if (selection == null) {
            parent = commandBlock;
            index = parent.commandList.size();
        }
        else if (selection.getLastPathComponent() instanceof CommandBlock) {
            parent = (CommandBlock)selection.getLastPathComponent();
            index = parent.commandList.size();
        }
        else {
            CommandBlock.Command item = (CommandBlock.Command)selection.getLastPathComponent();
            parent = item.getParent();
            index = parent.indexOf(item) + 1;
        }
        parent.insertCommand(c, index);
        Object[] path = commandBlockTreeModel.getPathToNode(parent);
        commandBlockTreeModel.fireTreeNodeInserted(path, index, c);
    }

    void removeCommands(TreePath[] selection) {
        HashMap map = new HashMap();
        for (int i = 0; i < selection.length; i++) {
            if (selection[i].getPathCount() == 1) {
                continue;
            }
            TreePath parent = selection[i].getParentPath();
            Object child = selection[i].getLastPathComponent();
            List l = (List)map.get(parent);
            if (l == null) {
                l = new ArrayList();
                map.put(parent, l);
            }
            l.add(child);
        }
        for (Iterator i = map.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            TreePath parent = (TreePath)entry.getKey();
            List list = (List)entry.getValue();
            CommandBlock.Command[] children = new CommandBlock.Command[list.size()];
            list.toArray(children);
            int[] indices = new int[children.length];
            for (int index = 0; index < children.length; index++) {
                indices[index] = children[index].getParent().indexOf(children[index]);
                children[index].getParent().removeCommand(children[index]);
            }
            commandBlockTreeModel.fireTreeNodesRemoved(parent, indices, children);
        }
    }
    
    private JPanel initEditorPanels() {
        switcher = new CardLayout();
        JPanel panel = new JPanel(switcher);
        panel.setBorder(new CompoundBorder(new CompoundBorder(new EmptyBorder(2, 2, 2, 2), new EtchedBorder()), new EmptyBorder(2, 2, 2, 2)));

        JPanel emptyPanel = new JPanel();
        panel.add(emptyPanel, "EMPTY");
        
        JPanel singlePanel = new JPanel();
        singlePanel.setLayout(new BoxLayout(singlePanel, BoxLayout.Y_AXIS));
        x1Field = new JTextField(30);
        x1Field.setMaximumSize(x1Field.getPreferredSize());
        x1Field.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        x1FieldLabel = new JLabel("field", JLabel.LEFT);
        x1FieldLabel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        singlePanel.add(x1FieldLabel);
        singlePanel.add(x1Field);
        JPanel x1ButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        x1ButtonPanel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        x1Commit = new JButton("Commit Changes");
        x1Commit.addActionListener(listener);
        x1Cancel = new JButton("Cancel Changes");
        x1Cancel.addActionListener(listener);
        x1ButtonPanel.add(x1Commit);
        x1ButtonPanel.add(x1Cancel);
        singlePanel.add(Box.createVerticalGlue());
        singlePanel.add(x1ButtonPanel);
        panel.add(singlePanel, "SINGLE");
        
        JPanel doublePanel = new JPanel();
        doublePanel.setLayout(new BoxLayout(doublePanel, BoxLayout.Y_AXIS));
        x2Field1 = new JTextField(30);
        x2Field1.setMaximumSize(x2Field1.getPreferredSize());
        x2Field1.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        x2Field1Label = new JLabel("field1");
        x2Field1Label.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        x2Field2 = new JTextField(30);
        x2Field2.setMaximumSize(x2Field2.getPreferredSize());
        x2Field2.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        x2Field2Label = new JLabel("field2");
        x2Field2Label.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        doublePanel.add(x2Field1Label);
        doublePanel.add(x2Field1);
        doublePanel.add(x2Field2Label);
        doublePanel.add(x2Field2);
        JPanel x2ButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        x2ButtonPanel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        x2Commit = new JButton("Commit Changes");
        x2Commit.addActionListener(listener);
        x2Cancel = new JButton("Cancel Changes");
        x2Cancel.addActionListener(listener);
        x2ButtonPanel.add(x2Commit);
        x2ButtonPanel.add(x2Cancel);
        doublePanel.add(Box.createVerticalGlue());
        doublePanel.add(x2ButtonPanel);
        panel.add(doublePanel, "DOUBLE");
        
        //switcher.show(panel, "EMPTY");
        return panel;
    }
    
    public void showDialog() {
        if (dialog == null) {
            Window w = SwingUtilities.windowForComponent(this);
            if (w instanceof Frame) {
                dialog = new JDialog((Frame)w, true);
            }
            else if (w instanceof Dialog) {
                dialog = new JDialog((Dialog)w, true);
            }
            else {
                dialog = new JDialog();
                dialog.setModal(true);
            }
            dialog.setContentPane(contentPanel);
            dialog.pack();
            dialog.setResizable(false);
            dialog.addWindowListener(listener);
        }
        dialog.setVisible(true);
    }
    
    CommandBlock commandBlock;
    
    public CommandBlock getCommandBlock() {
        if (enable.isSelected()) {
            return commandBlock;
        }
        return null;
    }
    
    public void setCommandBlock(CommandBlock commandBlock) {
        if (commandBlock == null) {
            commandBlock = new CommandBlock();
            enable.setSelected(false);
            setTopEnabled(false);
        }
        else {
            enable.setSelected(true);
            setTopEnabled(true);
        }
        this.commandBlock = commandBlock;
        commandBlockTreeModel.root = commandBlock;
        commandBlockTreeModel.fireTreeChanged();
    }
    
    public boolean stopCellEditing() {
        fireEditingStopped();
        dialog.setVisible(false);
        return true;
    }
    
    public void cancelCellEditing() {
        dialog.setVisible(false);
        fireEditingCanceled();
    }
    
    private class MultiPurposeListener extends WindowAdapter implements ActionListener {
        final JPopupMenu newCommandMenu = new JPopupMenu("new command");
        {
            newCommandMenu.add("SET").addActionListener(this);
            newCommandMenu.add("INVOKE").addActionListener(this);
            newCommandMenu.add("IF").addActionListener(this);
            newCommandMenu.add("ELSEIF").addActionListener(this);
            newCommandMenu.add("ELSE").addActionListener(this);
        }
        public void actionPerformed(ActionEvent e) {
            Object source = e.getSource();
            String command = e.getActionCommand();
            if (source == CommandBlockEditor.this) {
                showDialog();
            }
            else if (source == commitChanges) {
                stopCellEditing();
            }
            else if (source == cancelEdit) {
                cancelCellEditing();
            }
            else if (source == enable) {
                switcher.show(editorPanel, "EMPTY");
                setTopEnabled(enable.isSelected());
            }
            else if (source == newCommand) {
                newCommandMenu.show(newCommand, newCommand.getWidth(), 0);
            }
            else if (source == editCommand) {
                TreePath selection = commandTree.getSelectionPath();
                if (selection != null && selection.getPathCount() != 1) {
                    Object o = selection.getLastPathComponent();
                    if (o instanceof CommandBlock.SetCommand) {
                        CommandBlock.SetCommand c = (CommandBlock.SetCommand)o;
                        x2Field1Label.setText("Property");
                        x2Field1.setText(c.id);
                        x2Field2Label.setText("Value");
                        x2Field2.setText(c.value);
                        switcher.show(editorPanel, "DOUBLE");
                        setEditing(true);
                    }
                    else if (o instanceof CommandBlock.InvokeCommand) {
                        CommandBlock.InvokeCommand c = (CommandBlock.InvokeCommand)o;
                        x2Field1Label.setText("Method");
                        x2Field1.setText(c.target);
                        x2Field2Label.setText("Arguments (comma separated)");
                        if (c.args != null) {
                            String args = Arrays.asList(c.args).toString();
                            x2Field2.setText(args.substring(1, args.length() - 1));
                        }
                        else {
                            x2Field2.setText("");
                        }
                        switcher.show(editorPanel, "DOUBLE");
                        setEditing(true);
                    }
                    else if (o instanceof CommandBlock.ElseCommand) {
                        return;
                    }
                    else if (o instanceof CommandBlock.IfCommand) {
                        CommandBlock.IfCommand c = (CommandBlock.IfCommand)o;
                        x1FieldLabel.setText("Test");
                        x1Field.setText(c.test);
                        switcher.show(editorPanel, "SINGLE");
                        setEditing(true);
                    }
                }
            }
            else if (source == removeCommand) {
                TreePath[] selection = commandTree.getSelectionPaths();
                if (selection != null) {
                    removeCommands(selection);
                }
            }
            else if (source == moveUpCommand) {
                TreePath selection = commandTree.getSelectionPath();
                if (selection == null || selection.getPathCount() == 1) {
                    return;
                }
                CommandBlock.Command c = (CommandBlock.Command)selection.getLastPathComponent();
                CommandBlock parent = c.getParent();
                int index = parent.indexOf(c);
                if (index == 0) {
                    return;
                }
                else {
                    removeCommands(new TreePath[]{selection});
                    parent.insertCommand(c, index - 1);
                    commandBlockTreeModel.fireTreeNodeInserted(selection.getParentPath().getPath(), index - 1, c);
                }
            }
            else if (source == moveDownCommand) {
                TreePath selection = commandTree.getSelectionPath();
                if (selection == null || selection.getPathCount() == 1) {
                    return;
                }
                CommandBlock.Command c = (CommandBlock.Command)selection.getLastPathComponent();
                CommandBlock parent = c.getParent();
                int index = parent.indexOf(c);
                if (index == parent.commandList.size()-1) {
                    return;
                }
                else {
                    removeCommands(new TreePath[]{selection});
                    parent.insertCommand(c, index + 1);
                    commandBlockTreeModel.fireTreeNodeInserted(selection.getParentPath().getPath(), index + 1, c);
                }
            }
            else if (source == x1Commit) {
                TreePath selection = commandTree.getSelectionPath();
                Object node = selection.getLastPathComponent();
                CommandBlock.IfCommand c = (CommandBlock.IfCommand)node;
                c.test = x1Field.getText();
                setEditing(false);
            }
            else if (source == x2Commit) {
                TreePath selection = commandTree.getSelectionPath();
                Object node = selection.getLastPathComponent();
                if (node instanceof CommandBlock.SetCommand) {
                    CommandBlock.SetCommand c = (CommandBlock.SetCommand)node;
                    c.id = x2Field1.getText();
                    c.value = x2Field2.getText();
                    setEditing(false);
                }
                else {
                    CommandBlock.InvokeCommand c = (CommandBlock.InvokeCommand)node;
                    c.target = x2Field1.getText();
                    c.args = x2Field2.getText().split("\\s*,\\s*");
                    setEditing(false);
                }
            }
            else if (source == x1Cancel || source == x2Cancel) {
                setEditing(false);
            }
            else if (command.equals("SET")) {
                addCommand(new CommandBlock.SetCommand("property", "value"));
            }
            else if (command.equals("INVOKE")) {
                addCommand(new CommandBlock.InvokeCommand("object.method", null));
            }
            else if (command.equals("IF")) {
                addCommand(new CommandBlock.IfCommand("test"));
            }
            else if (command.equals("ELSEIF")) {
                addCommand(new CommandBlock.ElseIfCommand("test"));
            }
            else if (command.equals("ELSE")) {
                addCommand(new CommandBlock.ElseCommand());
            }
        }
        
        public void windowClosing(WindowEvent e) {
            cancelCellEditing();
        }
        private void setEditing(boolean b) {
            b = !b;
            if (b) {
                switcher.show(editorPanel, "EMPTY");
            }
            setTopEnabled(b);
            enable.setEnabled(b);
            TreePath selection = commandTree.getSelectionPath();
            CommandBlock.Command c = (CommandBlock.Command)selection.getLastPathComponent();
            int index = c.getParent().indexOf(c);
            commandBlockTreeModel.fireTreeNodesChanged(selection.getParentPath().getPath(), index, c);
        }
    }
    
    private void setTopEnabled(boolean b) {
        commandTree.setEnabled(b);
        newCommand.setEnabled(b);
        editCommand.setEnabled(b);
        removeCommand.setEnabled(b);
        moveUpCommand.setEnabled(b);
        moveDownCommand.setEnabled(b);
    }
    
    public static void main(String[] args) {
        
        CommandBlockEditor editor = new CommandBlockEditor();
        
        CommandBlock block = new CommandBlock();
        block.addCommand(new CommandBlock.SetCommand("property", "value"));
        block.addCommand(new CommandBlock.InvokeCommand("fred", new String[] {"arg1", "arg2"}));
        CommandBlock.IfCommand ifc = new CommandBlock.IfCommand("test");
        ifc.addCommand(new CommandBlock.SetCommand("fred", "larry"));
        block.addCommand(ifc);
        editor.setCommandBlock(block);
        
        JFrame frame = new JFrame();
        frame.setContentPane(editor);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
    
    public void addCellEditorListener(CellEditorListener l) {
        listenerList.add(CellEditorListener.class, l);
    }
    
    public Object getCellEditorValue() {
        return getCommandBlock();
    }
    
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        //this.table = table;
        setCommandBlock((CommandBlock)value);
        return this;
    }
    
    public boolean isCellEditable(EventObject anEvent) {
        return true;
    }
    
    public void removeCellEditorListener(CellEditorListener l) {
        listenerList.remove(CellEditorListener.class, l);
    }
    
    public boolean shouldSelectCell(EventObject anEvent) {
        return true;
    }
    
    private void fireEditingCanceled() {
        ChangeEvent e = null;
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==CellEditorListener.class) {
                if (e == null) {
                     e = new ChangeEvent(this);
                }
                ((CellEditorListener)listeners[i+1]).editingCanceled(e);
             }
         }
    }
    
    private void fireEditingStopped() {
        ChangeEvent e = null;
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==CellEditorListener.class) {
                if (e == null) {
                     e = new ChangeEvent(this);
                }
                ((CellEditorListener)listeners[i+1]).editingStopped(e);
             }
         }
    }
    
    /** Gets the property value as text.
     *
     * @return The property value as a human editable string.
     * <p>   Returns null if the value can't be expressed as an editable string.
     * <p>   If a non-null value is returned, then the PropertyEditor should
     * 	     be prepared to parse that string back in setAsText().
     */
    public String getAsText() {
        return "dflkjd";
    }
    
    /** A PropertyEditor may choose to make available a full custom Component
     * that edits its property value.  It is the responsibility of the
     * PropertyEditor to hook itself up to its editor Component itself and
     * to report property value changes by firing a PropertyChange event.
     * <P>
     * The higher-level code that calls getCustomEditor may either embed
     * the Component in some larger property sheet, or it may put it in
     * its own individual dialog, or ...
     *
     * @return A java.awt.Component that will allow a human to directly
     *      edit the current property value.  May be null if this is
     * 	    not supported.
     */
    public java.awt.Component getCustomEditor() {
        return this;
    }
    
    /** This method is intended for use when generating Java code to set
     * the value of the property.  It should return a fragment of Java code
     * that can be used to initialize a variable with the current property
     * value.
     * <p>
     * Example results are "2", "new Color(127,127,34)", "Color.orange", etc.
     *
     * @return A fragment of Java code representing an initializer for the
     *   	current value.
     */
    public String getJavaInitializationString() {
        return "???";
    }
    
    /** If the property value must be one of a set of known tagged values,
     * then this method should return an array of the tags.  This can
     * be used to represent (for example) enum values.  If a PropertyEditor
     * supports tags, then it should support the use of setAsText with
     * a tag value as a way of setting the value and the use of getAsText
     * to identify the current value.
     *
     * @return The tag values for this property.  May be null if this
     *   property cannot be represented as a tagged value.
     *
     */
    public String[] getTags() {
        return null;
    }
    
    /** Gets the property value.
     *
     * @return The value of the property.  Primitive types such as "int" will
     * be wrapped as the corresponding object type such as "java.lang.Integer".
     */
    public Object getValue() {
        return commandBlock;
    }
    
    /** Determines whether this property editor is paintable.
     *
     * @return  True if the class will honor the paintValue method.
     */
    public boolean isPaintable() {
        return false;
    }
    
    /** Paint a representation of the value into a given area of screen
     * real estate.  Note that the propertyEditor is responsible for doing
     * its own clipping so that it fits into the given rectangle.
     * <p>
     * If the PropertyEditor doesn't honor paint requests (see isPaintable)
     * this method should be a silent noop.
     * <p>
     * The given Graphics object will have the default font, color, etc of
     * the parent container.  The PropertyEditor may change graphics attributes
     * such as font and color and doesn't need to restore the old values.
     *
     * @param gfx  Graphics object to paint into.
     * @param box  Rectangle within graphics object into which we should paint.
     */
    public void paintValue(java.awt.Graphics gfx, java.awt.Rectangle box) {
    }
    
    /** Set the property value by parsing a given String.  May raise
     * java.lang.IllegalArgumentException if either the String is
     * badly formatted or if this kind of property can't be expressed
     * as text.
     * @param text  The string to be parsed.
     */
    public void setAsText(String text) throws java.lang.IllegalArgumentException {
    }
    
    /** Set (or change) the object that is to be edited.  Primitive types such
     * as "int" must be wrapped as the corresponding object type such as
     * "java.lang.Integer".
     *
     * @param value The new target object to be edited.  Note that this
     *     object should not be modified by the PropertyEditor, rather
     *     the PropertyEditor should create a new object to hold any
     *     modified value.
     */
    public void setValue(Object value) {
        setCommandBlock((CommandBlock)value);
    }
    
    /** Determines whether this property editor supports a custom editor.
     *
     * @return  True if the propertyEditor can provide a custom editor.
     */
    public boolean supportsCustomEditor() {
        return true;
    }
    
    private static class CommandBlockTreeModel implements TreeModel {
        
        private EventListenerList eventListenerList;
        private CommandBlock root;
        
        CommandBlockTreeModel(CommandBlock root) {
            this.root = root;
        }
        
        /** Adds a listener for the <code>TreeModelEvent</code>
         * posted after the tree changes.
         *
         * @param   l       the listener to add
         * @see     #removeTreeModelListener
         */
        public void addTreeModelListener(TreeModelListener l) {
            if (eventListenerList == null) {
                eventListenerList = new EventListenerList();
            }
            eventListenerList.add(TreeModelListener.class, l);
        }
        
        /** Returns the child of <code>parent</code> at index <code>index</code>
         * in the parent's
         * child array.  <code>parent</code> must be a node previously obtained
         * from this data source. This should not return <code>null</code>
         * if <code>index</code>
         * is a valid index for <code>parent</code> (that is <code>index >= 0 &&
         * index < getChildCount(parent</code>)).
         *
         * @param   parent  a node in the tree, obtained from this data source
         * @return  the child of <code>parent</code> at index <code>index</code>
         */
        public Object getChild(Object parent, int index) {
            if (parent instanceof CommandBlock) {
                return ((CommandBlock)parent).commandList.get(index);
            }
            else {
                return null;
            }
        }
        
        /** Returns the number of children of <code>parent</code>.
         * Returns 0 if the node
         * is a leaf or if it has no children.  <code>parent</code> must be a node
         * previously obtained from this data source.
         *
         * @param   parent  a node in the tree, obtained from this data source
         * @return  the number of children of the node <code>parent</code>
         */
        public int getChildCount(Object parent) {
            if (parent instanceof CommandBlock) {
                return ((CommandBlock)parent).commandList.size();
            }
            else {
                return 0;
            }
        }
        
        /** Returns the index of child in parent.  If <code>parent</code>
         * is <code>null</code> or <code>child</code> is <code>null</code>,
         * returns -1.
         *
         * @param parent a note in the tree, obtained from this data source
         * @param child the node we are interested in
         * @return the index of the child in the parent, or -1 if either
         *    <code>child</code> or <code>parent</code> are <code>null</code>
         */
        public int getIndexOfChild(Object parent, Object child) {
            if (parent instanceof CommandBlock) {
                return ((CommandBlock)parent).commandList.indexOf(child);
            }
            return -1;
        }
        
        /** Returns the root of the tree.  Returns <code>null</code>
         * only if the tree has no nodes.
         *
         * @return  the root of the tree
         */
        public Object getRoot() {
            return root;
        }
        
        /** Returns <code>true</code> if <code>node</code> is a leaf.
         * It is possible for this method to return <code>false</code>
         * even if <code>node</code> has no children.
         * A directory in a filesystem, for example,
         * may contain no files; the node representing
         * the directory is not a leaf, but it also has no children.
         *
         * @param   node  a node in the tree, obtained from this data source
         * @return  true if <code>node</code> is a leaf
         */
        public boolean isLeaf(Object node) {
            return !(node instanceof CommandBlock);
        }
        
        /** Removes a listener previously added with
         * <code>addTreeModelListener</code>.
         *
         * @see     #addTreeModelListener
         * @param   l       the listener to remove
         */
        public void removeTreeModelListener(TreeModelListener l) {
            if (eventListenerList != null) {
                eventListenerList.remove(TreeModelListener.class, l);
            }
        }
        
        protected void fireTreeChanged() {
            TreeModelEvent evt = null;
            Object[] listeners = eventListenerList.getListenerList();
            for (int i = listeners.length - 2; i >= 0; i-=2) {
                if (listeners[i] == TreeModelListener.class) {
                    if (evt == null) {
                        evt = new TreeModelEvent(this, new Object[]{root});
                    }
                    ((TreeModelListener)listeners[i+1]).treeStructureChanged(evt);
                }
            }
        }
        
        protected void fireTreeNodesChanged(Object[] path, int index, Object child) {
            TreeModelEvent evt = null;
            Object[] listeners = eventListenerList.getListenerList();
            for (int i = listeners.length - 2; i >= 0; i-=2) {
                if (listeners[i] == TreeModelListener.class) {
                    if (evt == null) {
                        evt = new TreeModelEvent(this, path, new int[]{index}, new Object[]{child});
                    }
                    ((TreeModelListener)listeners[i+1]).treeNodesChanged(evt);
                }
            }
        }
        
        protected void fireTreeNodeInserted(Object[] path, int index, Object child) {
            TreeModelEvent evt = null;
            Object[] listeners = eventListenerList.getListenerList();
            for (int i = listeners.length - 2; i >= 0; i-=2) {
                if (listeners[i] == TreeModelListener.class) {
                    if (evt == null) {
                        evt = new TreeModelEvent(this, path, new int[]{index}, new Object[]{child});
                    }
                    ((TreeModelListener)listeners[i+1]).treeNodesInserted(evt);
                }
            }
        }
        
        protected void fireTreeNodesRemoved(TreePath parent, int[] indices, Object[] children) {
            TreeModelEvent evt = null;
            Object[] listeners = eventListenerList.getListenerList();
            for (int i = listeners.length - 2; i >= 0; i-=2) {
                if (listeners[i] == TreeModelListener.class) {
                    if (evt == null) {
                        evt = new TreeModelEvent(this, parent, indices, children);
                    }
                    ((TreeModelListener)listeners[i+1]).treeNodesRemoved(evt);
                }
            }
        }
        
        protected void fireTreeStructureChanged(Object[] path) {
            TreeModelEvent evt = null;
            Object[] listeners = eventListenerList.getListenerList();
            for (int i = listeners.length - 2; i >= 0; i-=2) {
                if (listeners[i] == TreeModelListener.class) {
                    if (evt == null) {
                        evt = new TreeModelEvent(this, path);
                    }
                    ((TreeModelListener)listeners[i+1]).treeStructureChanged(evt);
                }
            }
        }
        
        Object[] getPathToNode(Object node) {
            if (!(node instanceof CommandBlock.Command)) {
                return new Object[]{node};
            }
            else {
                CommandBlock.Command command = (CommandBlock.Command)node;
                CommandBlock parent = command.getParent();
                int count = 2;
                while (parent instanceof CommandBlock.Command) {
                    parent = ((CommandBlock.Command)parent).getParent();
                    count ++;
                }
                Object[] path = new Object[count];
                parent = command.getParent();
                path[count - 1] = node;
                path[count - 2] = parent;
                int index = count - 2;
                while (parent instanceof CommandBlock.Command) {
                    index --;
                    parent = ((CommandBlock.Command)parent).getParent();
                    path[index] = parent;
                }
                System.out.println(Arrays.asList(path));
                return path;
            }
        }
        
        /** Messaged when the user has altered the value for the item identified
         * by <code>path</code> to <code>newValue</code>.
         * If <code>newValue</code> signifies a truly new value
         * the model should post a <code>treeNodesChanged</code> event.
         *
         * @param path path to the node that the user has altered
         * @param newValue the new value from the TreeCellEditor
         */
        public void valueForPathChanged(TreePath path, Object newValue) {
        }
        
    }
    
    private static class CommandRenderer extends JLabel implements TreeCellRenderer {
        
        Color textForeground;
        
        Color textBackground;
        
        Color selectionForeground;
        
        Color selectionBackground;
        
        Border focusedBorder;
        
        Border unfocusedBorder;
        
        CommandRenderer() {
            setOpaque(true);
            textForeground = UIManager.getColor("Tree.textForeground");
            textBackground = UIManager.getColor("Tree.textBackground");
            selectionForeground = UIManager.getColor("Tree.selectionForeground");
            selectionBackground = UIManager.getColor("Tree.selectionBackground");
            focusedBorder = new LineBorder(UIManager.getColor("Tree.selectionBorderColor"));
            unfocusedBorder = new LineBorder(textBackground);
        }
        
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            setForeground(selected ? selectionForeground : textForeground);
            setBackground(selected ? selectionBackground : textBackground);
            setBorder(hasFocus ? focusedBorder : unfocusedBorder);
            if (!(value instanceof CommandBlock.Command)) {
                setText("[Command Block]");
            }
            else {
                setText(value.toString());
            }
            setEnabled(tree.isEnabled());
            return this;
        }
        
    }
}
