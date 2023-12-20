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
public class FftOutputsFilterEditorPanel extends AbstractFilterEditorPanel {
    private String slide;

    /**
     * Creates new form FftPowerFilterEditorPanel
     */
    public FftOutputsFilterEditorPanel() {
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

        buttonGroup1 = new javax.swing.ButtonGroup();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        slideCB = new javax.swing.JComboBox();
        sizeTF = new javax.swing.JTextField();
        windowCB = new javax.swing.JComboBox();
        psdRB = new javax.swing.JRadioButton();
        lsdRB = new javax.swing.JRadioButton();
        lsRB = new javax.swing.JRadioButton();
        descriptionLabel = new javax.swing.JLabel();
        psRB = new javax.swing.JRadioButton();

        FormListener formListener = new FormListener();

        jLabel1.setText("Window Size: ");

        jLabel2.setText("Slide: ");

        jLabel3.setText("Window: ");

        slideCB.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "No Overlap", "1/2 Overlap", "2/3 Overlap", "3/4 Overlap", "7/8 Overlap" }));
        slideCB.setMinimumSize(new java.awt.Dimension(125, 27));
        slideCB.setPreferredSize(new java.awt.Dimension(125, 27));

        sizeTF.setText("512");
        sizeTF.setPreferredSize(new java.awt.Dimension(90, 27));

        windowCB.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Hanning (Hann)", "TenPercentEdgeCosine", "Unity (Boxcar)" }));
        windowCB.setPreferredSize(new java.awt.Dimension(125, 27));

        buttonGroup1.add(psdRB);
        psdRB.setSelected(true);
        psdRB.setText("PSD");
        psdRB.setToolTipText("Power Spectral Density");
        psdRB.addActionListener(formListener);

        buttonGroup1.add(lsdRB);
        lsdRB.setText("LSD");
        lsdRB.setToolTipText("Linear (Amplitude) Spectral Density");
        lsdRB.addActionListener(formListener);

        buttonGroup1.add(lsRB);
        lsRB.setText("LS");
        lsRB.setToolTipText("Linear (Amplitude) Spectrum");
        lsRB.addActionListener(formListener);

        descriptionLabel.setText("Power Spectral Density");

        buttonGroup1.add(psRB);
        psRB.setText("PS");
        psRB.setToolTipText("Power Spectrum");
        psRB.addActionListener(formListener);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel2)
                            .add(jLabel1)
                            .add(jLabel3))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(sizeTF, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(slideCB, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(windowCB, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 192, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                    .add(layout.createSequentialGroup()
                        .add(psdRB)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(psRB)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(lsdRB)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(lsRB))
                    .add(descriptionLabel))
                .addContainerGap(29, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(descriptionLabel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(psdRB)
                    .add(lsdRB)
                    .add(lsRB)
                    .add(psRB))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(sizeTF, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel2)
                    .add(slideCB, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(windowCB, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel3))
                .addContainerGap())
        );
    }

    // Code for dispatching events from components to event handlers.

    private class FormListener implements java.awt.event.ActionListener {
        FormListener() {}
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            if (evt.getSource() == psdRB) {
                FftOutputsFilterEditorPanel.this.psdRBActionPerformed(evt);
            }
            else if (evt.getSource() == lsdRB) {
                FftOutputsFilterEditorPanel.this.lsdRBActionPerformed(evt);
            }
            else if (evt.getSource() == lsRB) {
                FftOutputsFilterEditorPanel.this.lsRBActionPerformed(evt);
            }
            else if (evt.getSource() == psRB) {
                FftOutputsFilterEditorPanel.this.psRBActionPerformed(evt);
            }
        }
    }// </editor-fold>//GEN-END:initComponents

    private void psdRBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_psdRBActionPerformed
        if ( psdRB.isSelected() ) {
            descriptionLabel.setText("<html>Power Spectral Density  e.g. V&rarr;V**2/Hz");
        }
    }//GEN-LAST:event_psdRBActionPerformed

    private void lsdRBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lsdRBActionPerformed
        if ( lsdRB.isSelected() ) {
            descriptionLabel.setText("<html>Linear Spectral Density  e.g. V&rarr;V/sqrt(Hz)");
        }
    }//GEN-LAST:event_lsdRBActionPerformed

    private void lsRBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lsRBActionPerformed
        if ( lsRB.isSelected() ) {
            descriptionLabel.setText("<html>Linear Spectrum  e.g. V&rarr;V");
        }
    }//GEN-LAST:event_lsRBActionPerformed

    private void psRBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_psRBActionPerformed
        if ( psRB.isSelected() ) {
            descriptionLabel.setText("<html>Power Spectrum  e.g. V&rarr;V**2");
        }
    }//GEN-LAST:event_psRBActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    public javax.swing.ButtonGroup buttonGroup1;
    public javax.swing.JLabel descriptionLabel;
    public javax.swing.JLabel jLabel1;
    public javax.swing.JLabel jLabel2;
    public javax.swing.JLabel jLabel3;
    public javax.swing.JRadioButton lsRB;
    public javax.swing.JRadioButton lsdRB;
    public javax.swing.JRadioButton psRB;
    public javax.swing.JRadioButton psdRB;
    public javax.swing.JTextField sizeTF;
    public javax.swing.JComboBox slideCB;
    public javax.swing.JComboBox windowCB;
    // End of variables declaration//GEN-END:variables

    @Override
    public void setFilter(String filter) {
        Pattern p= Pattern.compile("\\|([a-zA-Z]+)\\((\\d+),(\\d),'?(\\w+)\\'?\\)");
        Matcher m= p.matcher(filter);
        if ( m.matches() ) {
            String f= m.group(1);
            if ( f.equals("fftPowerSpectralDensity" ) ) {
                psdRB.setSelected(true);
            } else if ( f.equals("fftLinearSpectralDensity") ) {
                lsdRB.setSelected(true);
            } else if ( f.equals("fftLinearSpectrum") ) {
                lsRB.setSelected(true);
            } else if ( f.equals("fftPowerSpectrum") ) {
                psRB.setSelected(true);
            }
            sizeTF.setText( m.group(2) );
            if (m.group(3).equals("1")) {
                slideCB.setSelectedIndex(0);
            } else if (m.group(3).equals("2")) {
                slideCB.setSelectedIndex(1);
            } else if (m.group(3).equals("3")) {
                slideCB.setSelectedIndex(2);
            } else if (m.group(3).equals("4")) {
                slideCB.setSelectedIndex(3);
            } else if (m.group(3).equals("8")) {
                slideCB.setSelectedIndex(4);
            } else {
                slideCB.setSelectedIndex(0);
            }
            windowCB.setSelectedItem( m.group(4) );
        }
        else {
            psdRB.setSelected(true);
            sizeTF.setText("512");
            slideCB.setSelectedIndex( 0 );
            windowCB.setSelectedIndex( 0 );
        }
        
    }

    @Override
    public String getFilter() {
        if (slideCB.getSelectedItem().equals("No Overlap"))  {
            slide = "1";
        } else if (slideCB.getSelectedItem().equals("1/2 Overlap")) {
            slide = "2";
        } else if (slideCB.getSelectedItem().equals("2/3 Overlap")) {
            slide = "3";
        } else if (slideCB.getSelectedItem().equals("3/4 Overlap")) {
            slide = "4";
        } else if (slideCB.getSelectedItem().equals("7/8 Overlap")) {
            slide = "8";
        }
        String window= (String)windowCB.getSelectedItem();
        if ( window.startsWith("Hanning") ) window= "Hanning"; // This is because of (Hann) in parenthesis.
        if ( window.startsWith("Unity") ) window= "Unity";
        String f;
        if ( psdRB.isSelected() ) {
            f = "fftPowerSpectralDensity";
        } else if ( lsdRB.isSelected() ) {
            f = "fftLinearSpectralDensity";
        } else if ( lsRB.isSelected() ) {
            f = "fftLinearSpectrum";
        } else if ( psRB.isSelected() ) {
            f = "fftPowerSpectrum";
        } else {
            throw new IllegalArgumentException("see code underimplemented line 192 FftOutputsFilterEditorPanel");
        }
        return "|"+f+"(" + sizeTF.getText() + "," + slide + ",'" + window + "')";
    }
}
