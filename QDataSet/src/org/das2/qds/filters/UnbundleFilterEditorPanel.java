/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.qds.filters;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.DefaultComboBoxModel;
import org.das2.qds.DataSetOps;
import org.das2.qds.QDataSet;

/**
 *
 * @author mmclouth
 */
public class UnbundleFilterEditorPanel extends AbstractFilterEditorPanel {
    
    public static final String PROP_REGEX = "\\|unbundle\\((.+)\\)";

    /**
     * Creates new form UnbundleFilterEditorPanel
     */
    public UnbundleFilterEditorPanel() {
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jComboBox1 = new javax.swing.JComboBox();

        jLabel1.setText("Component to unbundle :");

        jComboBox1.setEditable(true);
        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel1)
                .add(2, 2, 2)
                .add(jComboBox1, 0, 150, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                .add(jLabel1)
                .add(jComboBox1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jComboBox1ItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jComboBox1ItemStateChanged
        firePropertyChange( PROP_FILTER, null, getFilter() );
    }//GEN-LAST:event_jComboBox1ItemStateChanged


    // Variables declaration - do not modify//GEN-BEGIN:variables
    public javax.swing.JComboBox jComboBox1;
    public javax.swing.JLabel jLabel1;
    // End of variables declaration//GEN-END:variables
    
    @Override
    public void setInput( QDataSet ds ) {
        if ( ds.rank()>1 ) {
            try {
                String name= (String)jComboBox1.getSelectedItem();
                String[] names= DataSetOps.bundleNames(ds);
                jComboBox1.setModel( new DefaultComboBoxModel<>(names) );
                jComboBox1.setSelectedItem(name);
            } catch ( IllegalArgumentException ex ) {
                jComboBox1.setModel( new DefaultComboBoxModel<>( new String[] { "ch0", "ch1", "ch2" } ) );
            }
        }
    }
    
    @Override
    public void setFilter(String filter) {
        Pattern p= Pattern.compile(PROP_REGEX);
        Matcher m= p.matcher(filter);
        if ( m.matches() ) {
            String s= m.group(1);
            if ( s.startsWith("'")&& s.endsWith("'") ) {
                s= s.substring(1,s.length()-1);
            }
            jComboBox1.setSelectedItem(s);
        } else {
            jComboBox1.setSelectedItem("'Bx'");
        }
    }

    @Override
    public String getFilter() {
        String s=  jComboBox1.getSelectedItem().toString().trim();
        if ( s.length()>0 && s.charAt(0)=='\'' && s.charAt(s.length()-1)=='\'') {
            s= s.substring(1,s.length()-1);
        }
        if ( s.length()>0 && ( s.charAt(0)>='0' && s.charAt(0)<='9' || s.charAt(0)=='-') ) {
            return "|unbundle("+s+")";
        } else {
            return "|unbundle('" + s + "')";
        }
    }
    
    /**
     * return just the component
     * @return the component, such "Bx"
     */
    public String getComponent() {
        return jComboBox1.getSelectedItem().toString();
    }
}