
package org.das2.qds.filters;

import org.das2.qds.QDataSet;
import static org.das2.qds.ops.Ops.abs;
import static org.das2.qds.ops.Ops.extent;

/**
 *
 * @author jbf
 */
public class NormalizeFilterEditorPanel extends AbstractFilterEditorPanel  {

    public NormalizeFilterEditorPanel() {
        initComponents();
    }

    private String s;
    
    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();

        jLabel1.setText("data normalized by dividing by...");

        jLabel2.setText("normalize()");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 375, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 9, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    @Override
    public void setFilter(String filter) {
        this.s = filter;
    }

    @Override
    public String getFilter() {
        return s;
    }

    @Override
    public void setInput(QDataSet ds) {
        QDataSet n= abs( extent(ds) );
        if ( n.value(0) > n.value(1) ) {
            n= n.slice(0);
        } else {
            n= n.slice(1);
        }
        jLabel1.setText( "data normalized by dividing by "+n+"." );
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    // End of variables declaration//GEN-END:variables
}
