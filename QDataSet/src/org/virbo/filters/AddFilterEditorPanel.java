/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.filters;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;

/**
 *
 * @author mmclouth
 */
public class AddFilterEditorPanel extends AbstractFilterEditorPanel implements FilterEditorPanel {

    /**
     * Creates new form AddFilterEditorPanel
     */
    public AddFilterEditorPanel() {
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

        scalar = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();

        scalar.setText("0");
        scalar.setPreferredSize(new java.awt.Dimension(75, 27));

        jLabel2.setText("Scalar to add:");

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(12, 12, 12)
                        .add(scalar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(jLabel2))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel2)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(scalar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    public javax.swing.JLabel jLabel2;
    public javax.swing.JTextField scalar;
    // End of variables declaration//GEN-END:variables

    @Override
    public String getFilter() {
        return "|add("+scalar.getText()+")";
    }

    @Override
    public void setFilter(String filter) {
        Pattern p= Pattern.compile("\\|add\\((\\-?\\d+\\.?\\d+)\\)");
        //Pattern p1= Pattern.compile("\\|butterworth\\((.*,.*,.*,.*)\\)");
        //Pattern p2= Pattern.compile("\\|butterworth\\((.*,.*,.*)\\)");
        Matcher m= p.matcher(filter);
        if ( m.matches() ) {
            scalar.setText(m.group(1));
        } else {
            scalar.setText("0");
        }
        
    }
    
    /**
     * demonstrate filter.
     * @param args 
     */
    public static void main(String[] args) {
        FilterEditorPanel filter= new AddFilterEditorPanel();
        filter.setFilter("|add(0)");
        JOptionPane.showMessageDialog( null, filter);
        System.err.println( filter.getFilter() );
    }
}
