/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.qds.filters;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JTextField;

/**
 *
 * @author mmclouth
 */
public class MedianFilterEditorPanel extends AbstractFilterEditorPanel {

    /**
     * Creates new form MedianFilterEditorPanel
     */
    public MedianFilterEditorPanel() {
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
        sizeTF = new javax.swing.JTextField();

        FormListener formListener = new FormListener();

        setToolTipText("The median boxcar size can be between 5 and n/2, where n is the length of the data");

        jLabel1.setText("Median Boxcar Size:  ");

        sizeTF.setText("5");
        sizeTF.setPreferredSize(new java.awt.Dimension(50, 27));
        sizeTF.addActionListener(formListener);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 156, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(sizeTF, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(46, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(sizeTF, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }

    // Code for dispatching events from components to event handlers.

    private class FormListener implements java.awt.event.ActionListener {
        FormListener() {}
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            if (evt.getSource() == sizeTF) {
                MedianFilterEditorPanel.this.sizeTFActionPerformed(evt);
            }
        }
    }// </editor-fold>//GEN-END:initComponents

    private void sizeTFActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sizeTFActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_sizeTFActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    public javax.swing.JLabel jLabel1;
    public javax.swing.JTextField sizeTF;
    // End of variables declaration//GEN-END:variables

    @Override
    public void setFilter(String filter) {
        Pattern p= Pattern.compile("\\|medianFilter\\((.*)\\)");
        Matcher m= p.matcher(filter);
        if ( m.matches() ) {
            sizeTF.setText( m.group(1) );
        } else {
            sizeTF.setText("5");
        }
        sizeTF.setInputVerifier( new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                String t= ((JTextField)input).getText();
                int i;
                try {
                    i=Integer.parseInt(t);
                } catch ( NumberFormatException ex ) {
                    return false;
                }
                return i>4 && i<10000;
            }
        } );
    }

    @Override
    public String getFilter() {
        return "|medianFilter(" + sizeTF.getText() + ")";
    }
}
