
package org.das2.qds.filters;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.DefaultComboBoxModel;

/**
 * Command takes a single argument.
 * @author jbf
 */
public class SingleArgumentEditorPanel extends AbstractFilterEditorPanel implements FilterEditorPanel {

    private final String cmd;
    
    /**
     * @param cmd the command, for example "setValidRange"
     * @param label the label for the argument, "Valid Range"
     * @param doc a documentation string, "The limits of valid data, inclusive"
     * @param examples a set of example inputs, [ "", "-1e30 to 1e30", "0 to 100" ]
     */
    @SuppressWarnings("unchecked")
    public SingleArgumentEditorPanel( String cmd, String label, String doc, String[] examples ) {
        initComponents();
        this.cmd= cmd;
        this.jLabel2.setText(doc);
        this.jLabel1.setText(label+":");
        this.jComboBox1.setModel( new DefaultComboBoxModel(examples) );
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
        jComboBox1 = new javax.swing.JComboBox<>();
        jLabel2 = new javax.swing.JLabel();

        jLabel1.setText("Label:");
        jLabel1.setToolTipText("Values within this range, inclusive of the min and max, are considered valid.");

        jComboBox1.setEditable(true);
        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "-1e30 to 1e30", "-1 to 101", "0 to 1e38" }));

        jLabel2.setFont(jLabel2.getFont().deriveFont(jLabel2.getFont().getSize()-2f));
        jLabel2.setText("This is the doc label that is passed into the constructor");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBox1, 0, 331, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    // End of variables declaration//GEN-END:variables

    @Override
    public void setFilter(String filter) {
        Pattern p= Pattern.compile("\\|"+cmd+"\\((.+)\\)");
        Matcher m= p.matcher(filter);
        if ( m.matches() ) {
            String s= m.group(1);
            if ( s.startsWith("'") && s.endsWith("'") ) {
                s= s.substring(1,s.length()-1);
            }
            s= s.trim();
            jComboBox1.setSelectedItem(s);
        }
    }

    @Override
    public String getFilter() {
        return "|"+cmd+"("+jComboBox1.getSelectedItem()+")";
    }
}
