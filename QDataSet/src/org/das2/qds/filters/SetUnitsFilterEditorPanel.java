/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.qds.filters;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.DefaultComboBoxModel;
import org.das2.datum.Units;
import static org.das2.datum.Units.getAllUnits;
import org.das2.qds.QDataSet;
import static org.das2.qds.SemanticOps.getUnits;

/**
 *
 * @author mmclouth
 * TODO ask about how to get all possible units for a dataset
 */
public class SetUnitsFilterEditorPanel extends AbstractFilterEditorPanel {

    /**
     * Creates new form SetUnitsFilterEditorPanel
     */
    public SetUnitsFilterEditorPanel() {
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
        unitsCB = new javax.swing.JComboBox();

        jLabel1.setText("Units:  ");

        List<Units> units = getAllUnits();
        Units[] array = units.toArray(new Units[units.size()]);
        unitsCB.setEditable(true);
        unitsCB.setModel(new javax.swing.DefaultComboBoxModel(array));
        unitsCB.setPreferredSize(new java.awt.Dimension(200, 27));

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel1)
                .add(2, 2, 2)
                .add(unitsCB, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(unitsCB, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    public javax.swing.JLabel jLabel1;
    public javax.swing.JComboBox unitsCB;
    // End of variables declaration//GEN-END:variables

    @Override
    public void setFilter(String filter) {
        Pattern p= Pattern.compile("\\|setUnits\\('?(\\w+)'?\\)");
        Matcher m= p.matcher(filter);
        if ( m.matches() ) {
            //System.out.println("M matches");
            unitsCB.setSelectedItem( Units.lookupUnits(m.group(1)) );
        }
        else {
            unitsCB.setSelectedItem( Units.lookupUnits("s") );
        }
    }

    @Override
    public void setInput(QDataSet ds) {
        
    }
        
        
    @Override
    public String getFilter() {
        return "|setUnits('" + unitsCB.getSelectedItem().toString() + "')";
    }
}
