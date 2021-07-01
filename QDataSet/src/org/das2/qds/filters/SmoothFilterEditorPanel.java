/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.qds.filters;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author mmclouth
 */
public class SmoothFilterEditorPanel extends AbstractFilterEditorPanel {

    /**
     * Creates new form SmoothFilterEditorPanel
     */
    public SmoothFilterEditorPanel() {
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

        jCheckBox1 = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        sizeTF = new javax.swing.JTextField();
        fitCB = new javax.swing.JCheckBox();

        jCheckBox1.setText("jCheckBox1");

        jLabel1.setText("'Smooth' sliding boxcar size:  ");

        sizeTF.setText("3");
        sizeTF.setPreferredSize(new java.awt.Dimension(50, 27));

        fitCB.setText("Fit Ends");
        fitCB.setToolTipText("Use a linear fit for the end points.");

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel1)
                .add(2, 2, 2)
                .add(sizeTF, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(fitCB)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(sizeTF, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(fitCB))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    public javax.swing.JCheckBox fitCB;
    public javax.swing.JCheckBox jCheckBox1;
    public javax.swing.JLabel jLabel1;
    public javax.swing.JTextField sizeTF;
    // End of variables declaration//GEN-END:variables

    @Override
    public void setFilter(String filter) {
        Pattern p= Pattern.compile("\\|smooth\\((\\d+)\\)");
        Matcher m= p.matcher(filter);
        if ( m.matches() ) {
            sizeTF.setText( m.group(1) );
            fitCB.setSelected(false);
        } else {
            p= Pattern.compile("\\|smoothfit\\((\\d+)\\)");
            m= p.matcher(filter);
            if ( m.matches() ) {
                sizeTF.setText( m.group(1) );
                fitCB.setSelected(true);
            } else {
                sizeTF.setText( "3" );
                fitCB.setSelected(false);
            }
        }
        
    }

    @Override
    public String getFilter() {
        if ( fitCB.isSelected() ) {
            return "|smoothfit(" + sizeTF.getText() + ")";            
        } else {
            return "|smooth(" + sizeTF.getText() + ")";
        }
    }
}
