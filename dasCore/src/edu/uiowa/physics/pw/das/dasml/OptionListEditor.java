/* File: OptionListEditor.java
 * Copyright (C) 2002-2003 University of Iowa
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

package edu.uiowa.physics.pw.das.dasml;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;

/**
 *
 * @author  eew
 */
public class OptionListEditor extends JButton implements OptionList, javax.swing.table.TableCellEditor, java.beans.PropertyEditor {

    EventListenerList listenerList = new EventListenerList();
    
    ListOption editing;
    
    JDialog dialog;
    
    JPanel editorPanel;
    
    JLabel labelLabel;
    
    JLabel valueLabel;
    
    JTextField labelField;
    
    JTextField valueField;
    
    JList optionList;
    
    AllPurposeListener listener = new AllPurposeListener();
    
    JButton add;
    JButton moveUp;
    JButton moveDown;
    JButton delete;
    JButton commitChanges;
    JButton cancelEdit;
    
    private final ListModel EMPTY_MODEL = new ListModel() {
        public void addListDataListener(ListDataListener l) {}
        public void removeListDataListener(ListDataListener l) {}
        public Object getElementAt(int index) {
            return "<no data>";
        }
        public int getSize() {
            return 1;
        }
    };
    
    /** Creates a new instance of FormChoiceEditor */
    public OptionListEditor() {
        super("edit");
        addActionListener(listener);
        
        labelLabel = new JLabel("label:", JLabel.LEFT);
        labelField = new JTextField(20);
        labelField.addFocusListener(listener);
        valueLabel = new JLabel("value:", JLabel.LEFT);
        valueField = new JTextField(20);
        valueField.addFocusListener(listener);
        optionList = new JList(EMPTY_MODEL);
        optionList.setVisibleRowCount(10);
        optionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        optionList.addListSelectionListener(listener);
        
        editorPanel = new JPanel(new BorderLayout());
        editorPanel.setBorder(new EmptyBorder(5,5,5,5));
        editorPanel.add(new JScrollPane(optionList,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
        
        JPanel rightPanel = new JPanel();
        rightPanel.setBorder(new EmptyBorder(0, 5, 0, 0));
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        add = new JButton("Add");
        moveUp = new JButton("Move up");
        moveDown = new JButton("Move down");
        delete = new JButton("Delete");
        add.addActionListener(listener);
        moveUp.addActionListener(listener);
        moveDown.addActionListener(listener);
        delete.addActionListener(listener);
        Dimension size = moveDown.getPreferredSize();
        add.setPreferredSize(size);
        add.setMaximumSize(size);
        moveUp.setPreferredSize(size);
        moveUp.setMaximumSize(size);
        moveDown.setPreferredSize(size);
        moveDown.setMaximumSize(size);
        delete.setPreferredSize(size);
        delete.setMaximumSize(size);
        rightPanel.add(add);
        rightPanel.add(moveUp);
        rightPanel.add(moveDown);
        rightPanel.add(delete);
        editorPanel.add(rightPanel, BorderLayout.EAST);
        
        JPanel buttonPanel = new JPanel();
        commitChanges = new JButton("Commit changes");
        commitChanges.addActionListener(listener);
        cancelEdit = new JButton("Cancel Edit");
        cancelEdit.addActionListener(listener);
        buttonPanel.add(commitChanges);
        buttonPanel.add(cancelEdit);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        labelLabel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        labelField.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        valueLabel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        valueField.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        buttonPanel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        bottomPanel.add(labelLabel);
        bottomPanel.add(labelField);
        bottomPanel.add(valueLabel);
        bottomPanel.add(valueField);
        bottomPanel.add(buttonPanel);
        editorPanel.add(bottomPanel, BorderLayout.SOUTH);
    }
    
    public void showDialog() {
        if (dialog == null) {
            Window w = SwingUtilities.windowForComponent(this);
            if (w instanceof Dialog) {
                dialog = new JDialog((Dialog)w, true);
            }
            else if (w instanceof Frame) {
                dialog = new JDialog((Frame)w, true);
            }
            else {
                dialog = new JDialog();
                dialog.setModal(true);
            }
            dialog.setContentPane(editorPanel);
            dialog.pack();
            dialog.setResizable(false);
            dialog.addWindowListener(listener);
        }
        dialog.show();
    }
    
    private void addOption() {
        DefaultListModel model = getListModel();
        model.addElement(new ListOption("label", "value"));
        optionList.setSelectedIndex(model.getSize() - 1);
    }
    
    private DefaultListModel getListModel() {
        return (DefaultListModel)optionList.getModel();
    }
    
    private void moveUp(int index) {
        if (index >= 0) {
            if (index == 0) {
                return;
            }
            DefaultListModel model = getListModel();
            Object option = model.getElementAt(index);
            model.removeElementAt(index);
            model.insertElementAt(option, index - 1);
            optionList.setSelectedIndex(index - 1);
        }
    }
    
    private void moveDown(int index) {
        if (index >= 0) {
            DefaultListModel model = getListModel();
            if (index == model.getSize()) {
                return;
            }
            Object option = model.getElementAt(index);
            model.removeElementAt(index);
            model.insertElementAt(option, index + 1);
            optionList.setSelectedIndex(index + 1);
        }
    }
    
    private void deleteOption(int index) {
        if (index >= 0) {
            DefaultListModel model = getListModel();
            model.removeElementAt(index);
            optionList.clearSelection();
        }
    }
    
    private class AllPurposeListener implements ActionListener, ListSelectionListener, FocusListener, WindowListener {
        
        /** Invoked when an action occurs.
         */
        public void actionPerformed(ActionEvent e) {
            Object source = e.getSource();
            if (source == OptionListEditor.this) {
                showDialog();
            }
            else if (source == add) {
                addOption();
            }
            else if (source == moveUp) {
                moveUp(optionList.getSelectedIndex());
            }
            else if (source == moveDown) {
                moveDown(optionList.getSelectedIndex());
            }
            else if (source == delete) {
                deleteOption(optionList.getSelectedIndex());
            }
            else if (source == commitChanges) {
                stopCellEditing();
            }
            else if (source == cancelEdit) {
                cancelCellEditing();
            }
        }
        
        /**
         * Called whenever the value of the selection changes.
         * @param e the event that characterizes the change.
         */
        public void valueChanged(ListSelectionEvent e) {
            if (e.getValueIsAdjusting()) {
                return;
            }
            int index = optionList.getSelectedIndex();
            if (index >= 0) {
                editing = (ListOption)getListModel().getElementAt(index);
                labelField.setText(editing.getLabel());
                valueField.setText(editing.getValue());
            }
            else {
                editing = null;
                labelField.setText("");
                valueField.setText("");
            }
        }
        
        /** Invoked when a component gains the keyboard focus.
         */
        public void focusGained(FocusEvent e) {}
        
        /** Invoked when a component loses the keyboard focus.
         */
        public void focusLost(FocusEvent e) {
            if (editing != null) {
                if (e.getComponent() == labelField) {
                    editing.setLabel(labelField.getText());
                    optionList.repaint();
                }
                else if (e.getComponent() == valueField) {
                    editing.setValue(valueField.getText());
                }
            }
        }

        public void windowClosing(WindowEvent e) {
            fireEditingCanceled();
        }

        public void windowClosed(WindowEvent e) {}
        public void windowActivated(WindowEvent e) {}
        public void windowDeactivated(WindowEvent e) {}
        public void windowDeiconified(WindowEvent e) {}
        public void windowIconified(WindowEvent e) {}
        public void windowOpened(WindowEvent e) {}
        
    }
    
    public static void main(String[] args) {
        
        JPanel content = new JPanel(new BorderLayout());
        
        FormBase form = new FormBase(true);
        FormChoice choice = new FormChoice("fred");
        choice.addOption(new ListOption("One", "1"));
        choice.addOption(new ListOption("Two", "2"));
        
        OptionListEditor editor = new OptionListEditor();
        editor.setOptions(choice.getOptions());
        
        content.add(choice, BorderLayout.CENTER);
        content.add(editor, BorderLayout.SOUTH);
        
        
        
        JFrame frame = new JFrame();
        frame.setContentPane(content);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
    
    public ListOption[] getOptions() {
        ListModel model = getListModel();
        ListOption[] options = new ListOption[model.getSize()];
        for (int index = 0; index < options.length; index++) {
            options[index] = (ListOption)model.getElementAt(index);
        }
        return options;
    }
    
    public void setOptions(ListOption[] options) {
        DefaultListModel model = new DefaultListModel();
        model.ensureCapacity(options.length);
        for (int index = 0; index < options.length; index++) {
            model.addElement(options[index]);
        }
        optionList.setModel(model);
    }
    
    /** Adds a listener to the list that's notified when the editor
     * stops, or cancels editing.
     *
     * @param	l		the CellEditorListener
     */
    public void addCellEditorListener(CellEditorListener l) {
        listenerList.add(CellEditorListener.class, l);
    }
    
    /** Tells the editor to cancel editing and not accept any partially
     * edited value.
     */
    public void cancelCellEditing() {
        fireEditingCanceled();
        dialog.setVisible(false);
    }
    
    /** Returns the value contained in the editor.
     * @return the value contained in the editor
     */
    public Object getCellEditorValue() {
        return getOptions();
    }
    
    /**  Sets an initial <code>value</code> for the editor.  This will cause
     *  the editor to <code>stopEditing</code> and lose any partially
     *  edited value if the editor is editing when this method is called. <p>
     *
     *  Returns the component that should be added to the client's
     *  <code>Component</code> hierarchy.  Once installed in the client's
     *  hierarchy this component will then be able to draw and receive
     *  user input.
     *
     * @param	table		the <code>JTable</code> that is asking the
     * 				editor to edit; can be <code>null</code>
     * @param	value		the value of the cell to be edited; it is
     * 				up to the specific editor to interpret
     * 				and draw the value.  For example, if value is
     * 				the string "true", it could be rendered as a
     * 				string or it could be rendered as a check
     * 				box that is checked.  <code>null</code>
     * 				is a valid value
     * @param	isSelected	true if the cell is to be rendered with
     * 				highlighting
     * @param	row     	the row of the cell being edited
     * @param	column  	the column of the cell being edited
     * @return	the component for editing
     */
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        ListOption[] options = (ListOption[])value;
        if (dialog != null && dialog.isVisible()) {
            dialog.setVisible(false);
        }
        setOptions(options);
        return this;
    }
    
    /** Asks the editor if it can start editing using <code>anEvent</code>.
     * <code>anEvent</code> is in the invoking component coordinate system.
     * The editor can not assume the Component returned by
     * <code>getCellEditorComponent</code> is installed.  This method
     * is intended for the use of client to avoid the cost of setting up
     * and installing the editor component if editing is not possible.
     * If editing can be started this method returns true.
     *
     * @param	anEvent		the event the editor should use to consider
     * 				whether to begin editing or not
     * @return	true if editing can be started
     * @see #shouldSelectCell
     */
    public boolean isCellEditable(java.util.EventObject anEvent) {
        return true;
    }
    
    /** Removes a listener from the list that's notified
     *
     * @param	l		the CellEditorListener
     */
    public void removeCellEditorListener(CellEditorListener l) {
        listenerList.remove(CellEditorListener.class, l);
    }
    
    /** Returns true if the editing cell should be selected, false otherwise.
     * Typically, the return value is true, because is most cases the editing
     * cell should be selected.  However, it is useful to return false to
     * keep the selection from changing for some types of edits.
     * eg. A table that contains a column of check boxes, the user might
     * want to be able to change those checkboxes without altering the
     * selection.  (See Netscape Communicator for just such an example)
     * Of course, it is up to the client of the editor to use the return
     * value, but it doesn't need to if it doesn't want to.
     *
     * @param	anEvent		the event the editor should use to start
     * 				editing
     * @return	true if the editor would like the editing cell to be selected;
     *    otherwise returns false
     * @see #isCellEditable
     */
    public boolean shouldSelectCell(java.util.EventObject anEvent) {
        return true;
    }
    
    /** Tells the editor to stop editing and accept any partially edited
     * value as the value of the editor.  The editor returns false if
     * editing was not stopped; this is useful for editors that validate
     * and can not accept invalid entries.
     *
     * @return	true if editing was stopped; false otherwise
     */
    public boolean stopCellEditing() {
        fireEditingStopped();
        dialog.setVisible(false);
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
        return null;
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
        return getOptions();
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
        throw new IllegalArgumentException();
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
        setOptions((ListOption[])value);
    }
    
    /** Determines whether this property editor supports a custom editor.
     *
     * @return  True if the propertyEditor can provide a custom editor.
     */
    public boolean supportsCustomEditor() {
        return true;
    }
    
}
