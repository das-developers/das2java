
package org.das2.qds.filters;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.DefaultComboBoxModel;
import org.das2.datum.Units;
import static org.das2.datum.Units.getAllUnits;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;

/**
 *
 * @author jbf
 */
public class SetDepend1CadenceFilterEditorPanel extends AbstractFilterEditorPanel {

    public static final String PROP_REGEX = "\\|setDepend1Cadence\\('?([\\+\\d.e]+)\\s*(\\w*)'?\\)";

    /**
     * Creates new form SetDepend1CadenceFilterEditorPanel
     */
    public SetDepend1CadenceFilterEditorPanel() {
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
        msgLabel = new javax.swing.JLabel();

        jLabel1.setText("Depend1 Cadence:  ");

        scalarTF.setText("10");
        scalarTF.setPreferredSize(new java.awt.Dimension(50, 27));

        List<Units> units = getAllUnits();
        Units[] array = units.toArray(new Units[units.size()]);
        unitsCB.setEditable(true);
        unitsCB.setModel(new javax.swing.DefaultComboBoxModel(array));
        unitsCB.setMinimumSize(new java.awt.Dimension(200, 27));
        unitsCB.setPreferredSize(new java.awt.Dimension(200, 27));

        msgLabel.setText("<html><i>Explicity set the cadence of the measurements, typically corresponding to vertical position.");

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(msgLabel)
                    .add(layout.createSequentialGroup()
                        .add(jLabel1)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(scalarTF, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 77, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(unitsCB, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 155, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(msgLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(scalarTF, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(unitsCB, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    public javax.swing.JLabel jLabel1;
    public javax.swing.JLabel msgLabel;
    public javax.swing.JTextField scalarTF;
    public javax.swing.JComboBox unitsCB;
    // End of variables declaration//GEN-END:variables

    @Override
    public void setFilter(String filter) {
        Pattern p= Pattern.compile(PROP_REGEX);
        Matcher m= p.matcher(filter);
        if ( m.matches() ) {
            scalarTF.setText( m.group(1) );
            unitsCB.setSelectedItem( Units.lookupUnits(m.group(2)) );
        } else {
            scalarTF.setText("1");
            unitsCB.setSelectedItem( Units.lookupUnits("") );
        }
    }
       

    @Override
    public String getFilter() {
        return "|setDepend1Cadence('" + scalarTF.getText() + unitsCB.getSelectedItem() + "')";
    }

    Units currentUnits= null;
    
    @Override
    public void setInput(QDataSet ds) {
        QDataSet dep= (QDataSet) ds.property(QDataSet.DEPEND_1);
        if ( dep==null ) {
            if ( SemanticOps.isJoin(ds) ) {
                dep= (QDataSet) ds.slice(0).property(QDataSet.DEPEND_1);
            }
        }
        if ( dep!=null ) {
            Units u= SemanticOps.getUnits(dep);
            if ( u!=currentUnits ) {
                Units[] uu= u.getOffsetUnits().getConvertibleUnits();
                Units oldu= (Units)unitsCB.getSelectedItem();
                unitsCB.setModel( new DefaultComboBoxModel<>(uu) );
                unitsCB.setSelectedItem(oldu);
                currentUnits= u;
            }
        } else {
            msgLabel.setText("<html><i>Dataset has no DEPEND_1");
        }
    }
    
}
