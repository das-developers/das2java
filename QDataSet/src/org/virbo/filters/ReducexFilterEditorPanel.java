/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.filters;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.datum.Datum;
import org.das2.datum.DatumUtil;
import org.das2.datum.Units;
import static org.das2.datum.Units.getAllUnits;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SemanticOps;


/**
 *
 * @author mmclouth
 */
public class ReducexFilterEditorPanel extends AbstractFilterEditorPanel {

    /**
     * Creates new form ReducexFilterEditorPanel
     */
    public ReducexFilterEditorPanel() {
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
        scalarTF = new javax.swing.JTextField();
        unitsCB = new javax.swing.JComboBox();

        jLabel1.setText("Reduce data to intervals of:  ");

        scalarTF.setText("1");
        scalarTF.setPreferredSize(new java.awt.Dimension(30, 27));

        unitsCB.setEditable(true);
        List<Units> units = getAllUnits();
        Units[] array = units.toArray(new Units[units.size()]);
        unitsCB.setModel(new javax.swing.DefaultComboBoxModel(array));

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel1)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(scalarTF, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 48, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(unitsCB, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 136, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(scalarTF, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(unitsCB, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    public javax.swing.JLabel jLabel1;
    public javax.swing.JTextField scalarTF;
    public javax.swing.JComboBox unitsCB;
    // End of variables declaration//GEN-END:variables

    @Override
    public void setFilter(String filter) {
        Pattern p= Pattern.compile("\\|reducex\\('(.*)'\\)"); 
        Matcher m= p.matcher(filter);
        if ( m.matches() ) {
            try {
                Datum d= DatumUtil.parse(m.group(1));
                double dv= d.doubleValue(d.getUnits());
                if ( dv==(int)dv ) {
                    scalarTF.setText(String.valueOf((int)dv)); // this will often be the case.
                } else {
                    scalarTF.setText(String.valueOf(dv));
                }
                unitsCB.setSelectedItem(d.getUnits());
            } catch (ParseException ex) {
                Pattern p2= Pattern.compile("\\|reducex\\('?(\\d+)\\s*(\\S+)'?\\)");
                Matcher m2= p2.matcher(filter);
                if ( m2.matches() ) {
                    scalarTF.setText(m.group(1));
                    unitsCB.setSelectedItem(Units.lookupUnits(m.group(2)));
                } else {
                    scalarTF.setText("1");
                    unitsCB.setSelectedItem(Units.hours);
                }
            }
        } else {
            scalarTF.setText("1");
            unitsCB.setSelectedItem(Units.hours);
        }
    }

    @Override
    public String getFilter() {
        return "|reducex('" + scalarTF.getText() + "" + unitsCB.getSelectedItem() + "')";
    }

    @Override
    public void setInput(QDataSet ds) {
        QDataSet dep0= SemanticOps.xtagsDataSet(ds);
        Units tu= SemanticOps.getUnits(dep0);
        Units[] array= tu.getOffsetUnits().getConvertableUnits();
        Object u= unitsCB.getSelectedItem();
        unitsCB.setModel(new javax.swing.DefaultComboBoxModel(array));  
        unitsCB.setSelectedItem(u);
    }

}
