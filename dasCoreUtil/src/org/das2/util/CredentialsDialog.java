/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.util;

import java.awt.Dialog;
import java.awt.event.KeyEvent;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

/**
 *
 * @author cwp
 */
public class CredentialsDialog extends JDialog{

	protected int m_nRet;
	protected String m_sUser;
	protected String m_sPasswd;
	
	/** Creates new form CredentialsDialog */
	public CredentialsDialog(java.awt.Frame parent){
		super(parent, "Authentication Required", Dialog.ModalityType.APPLICATION_MODAL);
		m_nRet = 0;
		setResizable(true);
		if(parent != null){
			setLocationRelativeTo(parent);
		}
		initComponents();
	}
	
	/** Show the dialog, get user input, and then close */
   public int runDialog(String sDesc, Icon icon, String sUser, String sPasswd){
		m_nRet = JOptionPane.CANCEL_OPTION;
		
		// Code halts here until some other location calls setVisible(false) on this
		// dialog object
		if(icon != null) lblIcon.setIcon(icon);
		lblDesc.setText(sDesc);
		tfUser.setText(sUser);
		tfPasswd.setText(sPasswd);
		
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		
		pack();       // Recalculate internal size of sub components
		validate(); 
		
		setVisible(true);
		m_sUser = tfUser.getText();
		m_sPasswd = new String( tfPasswd.getPassword() );
		
		return m_nRet;
	};
	
	int getReturn(){return m_nRet;}
	String getUser(){return m_sUser;}
	String getPasswd(){return m_sPasswd;}
	
	/** This method is called from within the constructor to initialize the form. WARNING:
	 * Do NOT modify this code. The content of this method is always regenerated by the
	 * Form Editor.
	 */
	@SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        lblIcon = new javax.swing.JLabel();
        lblDesc = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        tfUser = new javax.swing.JTextField();
        tfPasswd = new javax.swing.JPasswordField();
        jSeparator1 = new javax.swing.JSeparator();
        btnOK = new javax.swing.JButton();
        btnCancel = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Authorization Required");
        setModalityType(java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        setName("Authorization"); // NOI18N
        getContentPane().setLayout(new java.awt.GridBagLayout());

        lblIcon.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/das2logo-64.png"))); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridheight = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 0);
        getContentPane().add(lblIcon, gridBagConstraints);

        lblDesc.setText("<html><center><h3>Some Long Complete Site Name</h3></center>Server: <b>some.server.org/das</b><br>Data Set: <b>Some Dataset</b>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(lblDesc, gridBagConstraints);

        jLabel3.setText("User name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        getContentPane().add(jLabel3, gridBagConstraints);

        jLabel4.setText("Password:");
        jLabel4.setToolTipText("passwords are stored in ~/.das2/keychain.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        getContentPane().add(jLabel4, gridBagConstraints);

        tfUser.setText("someone");
        tfUser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tfUserActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.RELATIVE;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.67;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(tfUser, gridBagConstraints);

        tfPasswd.setText("jPasswor");
        tfPasswd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tfPasswdActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(tfPasswd, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(jSeparator1, gridBagConstraints);

        btnOK.setText("OK");
        btnOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOKActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.ipadx = 10;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(btnOK, gridBagConstraints);

        btnCancel.setText("Cancel");
        btnCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(btnCancel, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

   private void btnOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOKActionPerformed
      m_nRet = JOptionPane.OK_OPTION;
		setVisible(false);
   }//GEN-LAST:event_btnOKActionPerformed

   private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelActionPerformed
		setVisible(false);
   }//GEN-LAST:event_btnCancelActionPerformed

   private void tfPasswdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tfPasswdActionPerformed
     btnOKActionPerformed(evt);
   }//GEN-LAST:event_tfPasswdActionPerformed

   private void tfUserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tfUserActionPerformed
     btnOKActionPerformed(evt);
   }//GEN-LAST:event_tfUserActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnOK;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JLabel lblDesc;
    private javax.swing.JLabel lblIcon;
    private javax.swing.JPasswordField tfPasswd;
    private javax.swing.JTextField tfUser;
    // End of variables declaration//GEN-END:variables
}
