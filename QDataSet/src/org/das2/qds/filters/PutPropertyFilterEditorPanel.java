
package org.das2.qds.filters;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.DefaultComboBoxModel;

/**
 * Editor panel for the putProperty filter, which allows valid
 * ranges and missing metadata to be specified.
 * @author faden@cottagesystems.com
 */
public class PutPropertyFilterEditorPanel extends AbstractFilterEditorPanel {

    /**
     * Creates new form PutPropertyFilterEditorPanel
     */
    public PutPropertyFilterEditorPanel() {
        initComponents();
    }
    
    private void update() {
        String name= (String)jComboBox1.getSelectedItem();
        String[] vv=null;
        switch (name) {
            case "VALID_MIN":
                vv= new String[] { "-1e30", "-999", "0", "1" };
                documentationLabel.setText("the smallest valid value");
                break;
            case "VALID_MAX":
                vv= new String[] { "1e30", "999", "0" };
                documentationLabel.setText("the largest valid value");
                break;
            case "DELTA_MINUS":
                vv= new String[] { "None" };
                documentationLabel.setText("use None to remove error bars");
                break;
            case "DELTA_PLUS":
                vv= new String[] { "None" };
                documentationLabel.setText("use None to remove error bars");
                break;
            case "FILL_VALUE":
                vv= new String[] { "0", "-999", "-1e31" };
                documentationLabel.setText("values that indicate missing measurements");
                break;
            case "DEPEND_0":
                vv= new String[] { "None" };
                documentationLabel.setText("use None to remove the DEPEND_0 tags");
                break;
            case "FORMAT":
                vv= new String[] { "%5.2f", "%d", "%05d", "%x" };
                documentationLabel.setText("format specifier for digital display");
                break;
            case "UNITS":
                vv= new String[] { "None" };
                documentationLabel.setText("reset the units");
                break;
            case "NAME":
                vv= new String[] { "MyData" };
                documentationLabel.setText("set the name used for the data");
                break;
            default:
                vv= new String[] { "" };
                documentationLabel.setText(" ");
                break;
        }
        jComboBox2.setModel( new DefaultComboBoxModel(vv) );
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
        jComboBox2 = new javax.swing.JComboBox();
        documentationLabel = new javax.swing.JLabel();

        jLabel1.setText("Put Property");

        jComboBox1.setEditable(true);
        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "VALID_MIN", "VALID_MAX", "FILL_VALUE", "TITLE", "LABEL", "NAME", "DEPEND_0", "FORMAT", "DELTA_PLUS", "DELTA_MINUS", "UNITS" }));
        jComboBox1.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jComboBox1ItemStateChanged(evt);
            }
        });

        jComboBox2.setEditable(true);
        jComboBox2.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        documentationLabel.setText("jLabel2");
        documentationLabel.setToolTipText("");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(24, 24, 24)
                        .addComponent(documentationLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(documentationLabel)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jComboBox1ItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jComboBox1ItemStateChanged
        update();
    }//GEN-LAST:event_jComboBox1ItemStateChanged


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel documentationLabel;
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JComboBox jComboBox2;
    private javax.swing.JLabel jLabel1;
    // End of variables declaration//GEN-END:variables

    @Override
    public void setFilter(String filter) {
        Pattern p= Pattern.compile("\\|putProperty\\((\\S+),(\\S+)\\)");
        Matcher m= p.matcher(filter);
        if ( m.matches() ) {
            jComboBox1.setSelectedItem(m.group(1));
            update();
            jComboBox2.setSelectedItem(m.group(2));
        }
    }

    @Override
    public String getFilter() {
        return "|putProperty("+jComboBox1.getSelectedItem()+","+jComboBox2.getSelectedItem()+")";
    }
}
