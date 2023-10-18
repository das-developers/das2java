
package org.das2.components.propertyeditor;

import java.awt.BorderLayout;
import java.awt.Component;
import org.das2.datum.UnitsUtil;
import org.das2.graph.DasAxis;
import org.das2.util.StringSchemeEditor;

/**
 * switches between time and decimal based on units
 * @author jbf
 */
public class AxisFormatStringSchemeEditor extends javax.swing.JPanel implements StringSchemeEditor {

    FormatSpecifierStringSchemeEditor decimalEdit;
    UriTemplatesStringSchemeEditor timeEdit;
    
    /**
     * Creates new form AxisFormatStringSchemeEditor
     */
    public AxisFormatStringSchemeEditor() {
        initComponents();
        this.timeEdit= new UriTemplatesStringSchemeEditor();
        this.decimalEdit= new FormatSpecifierStringSchemeEditor();
        timeFormatPanel.add(this.timeEdit.getComponent(), BorderLayout.CENTER );
        decimalFormatPanel.add(this.decimalEdit.getComponent(), BorderLayout.CENTER );
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane2 = new javax.swing.JTabbedPane();
        timeFormatPanel = new javax.swing.JPanel();
        decimalFormatPanel = new javax.swing.JPanel();

        timeFormatPanel.setLayout(new java.awt.BorderLayout());
        jTabbedPane2.addTab("Time Format", timeFormatPanel);

        decimalFormatPanel.setLayout(new java.awt.BorderLayout());
        jTabbedPane2.addTab("Decimal Format", decimalFormatPanel);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(0, 2, Short.MAX_VALUE)
                .addComponent(jTabbedPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 395, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 3, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 295, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel decimalFormatPanel;
    private javax.swing.JTabbedPane jTabbedPane2;
    private javax.swing.JPanel timeFormatPanel;
    // End of variables declaration//GEN-END:variables

    @Override
    public void setValue(String v) {
        this.timeEdit.setValue(v);
        this.decimalEdit.setValue(v);
    }

    @Override
    public String getValue() {
        if ( jTabbedPane2.getSelectedIndex()==0 ) {
            return timeEdit.getValue();
        } else {
            return decimalEdit.getValue();
        }
    }

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public void setContext(Object o) {
        if ( o==null ) {
            jTabbedPane2.setSelectedIndex(0);
            return;
        } 
        if ( o.getClass().getCanonicalName().equals("org.das2.graph.DasAxis") ) {
            if ( UnitsUtil.isTimeLocation(( (DasAxis)o).getUnits() ) ) {
                jTabbedPane2.setSelectedIndex(0);
            } else {
                jTabbedPane2.setSelectedIndex(1);
            }
        }
    }
}