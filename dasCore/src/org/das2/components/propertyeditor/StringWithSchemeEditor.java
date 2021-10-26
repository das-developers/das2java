
package org.das2.components.propertyeditor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableCellEditor;
import javax.swing.text.BadLocationException;

/**
 * Experiments with adding an editor where you can click to get a special GUI.
 * @author jbf
 */
public class StringWithSchemeEditor extends AbstractCellEditor implements java.beans.PropertyEditor, TableCellEditor {

    private String value;

    public StringWithSchemeEditor() {
        this.pcs = new PropertyChangeSupport(this);
    }
    
    @Override
    public Object getCellEditorValue() {
        return value;
    }

    @Override
    public void setValue(Object value) {
        this.value= (String)value;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public boolean isPaintable() {
        return false;
    }

    @Override
    public void paintValue(Graphics gfx, Rectangle box) {
        
    }

    @Override
    public String getJavaInitializationString() {
        return "\""+value+"\"";
    }

    @Override
    public String getAsText() {
        return value;
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        this.value= text;
    }

    @Override
    public String[] getTags() {
        return null;
    }
    
    private StringSchemeEditor ssEditor= null;
    
    public void setCustomEditor( StringSchemeEditor edit ) {
        this.ssEditor= edit;
    }

    @Override
    public Component getCustomEditor() {
        if ( this.ssEditor != null )  {
            return this.ssEditor.getComponent();
        } else {
            return new JTextField();
        }
    }

    @Override
    public boolean supportsCustomEditor() {
        return true;
    }

    PropertyChangeSupport pcs;
    
    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        final JTextField f= new JTextField();
        f.setText( value.toString() );
        f.getDocument().addDocumentListener( new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                try {
                    setValue(e.getDocument().getText(0,e.getDocument().getLength()));
                } catch (BadLocationException ex) {
                    Logger.getLogger(StringWithSchemeEditor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                try {
                    setValue(e.getDocument().getText(0,e.getDocument().getLength()));
                } catch (BadLocationException ex) {
                    Logger.getLogger(StringWithSchemeEditor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                try {
                    setValue(e.getDocument().getText(0,e.getDocument().getLength()));
                } catch (BadLocationException ex) {
                    Logger.getLogger(StringWithSchemeEditor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        javax.swing.JPanel p= new javax.swing.JPanel(new BorderLayout());
        p.add( f, BorderLayout.CENTER );
        javax.swing.JButton extraButton= new javax.swing.JButton( new AbstractAction("?") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Component c;
                String svalue= (String)StringWithSchemeEditor.this.getValue();
                StringWithSchemeEditor.this.ssEditor.setValue(svalue);
                c= StringWithSchemeEditor.this.ssEditor.getComponent();
                if ( JOptionPane.OK_OPTION==JOptionPane.showConfirmDialog( table, c, "Edit Text", JOptionPane.OK_CANCEL_OPTION ) ) {
                    f.setText( ((StringSchemeEditor)c).getValue() );
                    ((Component)e.getSource()).getParent().repaint();
                }
            }
        } );
        extraButton.setPreferredSize( new Dimension(16,16) );
        extraButton.setMaximumSize( new Dimension(16,16) );
        p.add( extraButton, BorderLayout.EAST );
        return p;
    }
    
}
