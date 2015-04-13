/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.filters;

import java.lang.ref.WeakReference;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.DefaultComboBoxModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import org.das2.util.LoggerManager;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.QDataSet;
import static org.virbo.filters.FilterEditorPanel.PROP_FILTER;

/**
 * Editor panel to replace the original GUI that was a part of the data panel.
 * @author jbf
 */
public class SliceFilterEditorPanel extends AbstractFilterEditorPanel implements FilterEditorPanel {

    static final long t0= System.currentTimeMillis();
    int[] qube= null;
    WeakReference<QDataSet> inputDs= null;
    
    /**
     * Creates new form SlicesFilterEditorPanel
     */
    public SliceFilterEditorPanel() {
        if ( !SwingUtilities.isEventDispatchThread() ) {
            logger.log(Level.WARNING, "called off event thread");
        }

        initComponents();
        setName("sliceFilterEditorPanel" + String.format( "%04d", (System.currentTimeMillis()-t0)/100 ));
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        sliceDimensionCB = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        sliceIndexSpinner = new javax.swing.JSpinner();
        jLabel2 = new javax.swing.JLabel();
        sliceFeedbackLabel = new javax.swing.JLabel();

        sliceDimensionCB.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        sliceDimensionCB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sliceDimensionCBActionPerformed(evt);
            }
        });

        jLabel1.setText("Slice Index:");

        sliceIndexSpinner.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(0), Integer.valueOf(0), null, Integer.valueOf(1)));
        sliceIndexSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sliceIndexSpinnerStateChanged(evt);
            }
        });
        sliceIndexSpinner.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                sliceIndexSpinnerMouseWheelMoved(evt);
            }
        });

        jLabel2.setText("Slice Dimension:");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(24, 24, 24)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sliceIndexSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sliceFeedbackLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sliceDimensionCB, 0, 291, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sliceDimensionCB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sliceFeedbackLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel1)
                        .addComponent(sliceIndexSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void sliceDimensionCBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sliceDimensionCBActionPerformed
        if ( !SwingUtilities.isEventDispatchThread() ) {
            logger.log(Level.WARNING, "called off event thread");
        }
        LoggerManager.logGuiEvent(evt);
        final String ff= getFilter();
        logger.log(Level.FINEST, "0: {0}{1}", new Object[]{ff, this.getName()});
        firePropertyChange( PROP_FILTER, null, ff );
        int idx= sliceDimensionCB.getSelectedIndex();
        if ( qube!=null ) {
            SpinnerNumberModel snm= ((SpinnerNumberModel)sliceIndexSpinner.getModel());
            snm.setMaximum(qube[idx]-1);
            if ( snm.getNumber().intValue()>=qube[idx] ) {
                snm.setValue(qube[idx]-1);
            }
        }
        updateFeedback();
    }//GEN-LAST:event_sliceDimensionCBActionPerformed

    private void sliceIndexSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sliceIndexSpinnerStateChanged
        if ( !SwingUtilities.isEventDispatchThread() ) {
            logger.log(Level.WARNING, "called off event thread");
        }

        LoggerManager.logGuiEvent(evt);
        final String ff= getFilter();
        logger.log(Level.FINEST, "1: {0}{1}", new Object[]{ff, this.getName()});
        firePropertyChange( PROP_FILTER, null, ff );
        updateFeedback();
    }//GEN-LAST:event_sliceIndexSpinnerStateChanged

    private void sliceIndexSpinnerMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_sliceIndexSpinnerMouseWheelMoved
        if ( !SwingUtilities.isEventDispatchThread() ) {
            logger.log(Level.WARNING, "called off event thread");
        }

        SpinnerNumberModel snm= ((SpinnerNumberModel)sliceIndexSpinner.getModel());
        int newIndex= snm.getNumber().intValue() - evt.getWheelRotation();
        if ( newIndex<0 ) newIndex= 0;
        Number nmax= (Number)snm.getMaximum();
        if ( nmax!=null ) {
            int maxIndex= nmax.intValue();
            if ( newIndex>maxIndex ) newIndex= maxIndex;
        }
        snm.setValue( newIndex );
        updateFeedback();
    }//GEN-LAST:event_sliceIndexSpinnerMouseWheelMoved

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JComboBox sliceDimensionCB;
    private javax.swing.JLabel sliceFeedbackLabel;
    private javax.swing.JSpinner sliceIndexSpinner;
    // End of variables declaration//GEN-END:variables

    private void updateFeedback() {
        Runnable run= new Runnable() {
            public void run() {
                int dim= sliceDimensionCB.getSelectedIndex();
                int index= (Integer)sliceIndexSpinner.getValue();
                boolean na= true;
                if ( inputDs!=null ) {
                    QDataSet ds= inputDs.get();
                    if ( ds!=null ) {
                        QDataSet dep= (QDataSet)ds.property("DEPEND_"+dim);
                        if ( dep!=null && dep.rank()==1 ) {
                            String s= DataSetUtil.asDatum( dep.slice(index) ).toString();
                            sliceFeedbackLabel.setText(s);
                            sliceFeedbackLabel.setToolTipText(null);
                            na= false;
                        }
                    }
                }        
                if ( na ) {
                    sliceFeedbackLabel.setText("N/A");
                    sliceFeedbackLabel.setToolTipText("value cannot be found easily");
                }
            }
        };
        SwingUtilities.invokeLater(run);
    }
    
    @Override
    public String getFilter() {
        logger.fine( "getFilter" );
        updateFeedback();
        return String.format( "|slice%d(%d)", sliceDimensionCB.getSelectedIndex(), (Integer)sliceIndexSpinner.getValue() );
    }

    @Override
    public void setFilter(String filter) {
        if ( !SwingUtilities.isEventDispatchThread() ) {
            logger.log(Level.WARNING, "called off event thread");
        }
        
        logger.log(Level.FINE, "setFilter {0}", filter);
        if ( getFilter().equals(filter) ) {
            logger.warning("redundant set filter");
        }
        Pattern p= Pattern.compile("\\|slice(\\d)\\((\\d+)\\)");
        Matcher m= p.matcher(filter);
        if ( m.matches() ) {
            int dim= Integer.parseInt(m.group(1));
            int index= Integer.parseInt(m.group(2));
            sliceDimensionCB.setSelectedIndex( dim );
            sliceIndexSpinner.setValue( index );
        }
    }

    @Override
    public void setInput(QDataSet ds) {
        if ( !SwingUtilities.isEventDispatchThread() ) {
            logger.log(Level.WARNING, "called off event thread");
        }
        logger.log(Level.FINE, "setInput {0}", ds.toString() );
        this.inputDs= new WeakReference(ds);
        String[] depNames1= FilterEditorPanelUtil.getDimensionNames(ds);
        int idx= sliceDimensionCB.getSelectedIndex();
        sliceDimensionCB.setModel(new DefaultComboBoxModel(depNames1));
        qube= DataSetUtil.qubeDims(ds);
        if ( qube!=null ) {
            if ( idx<qube.length ) { // transitions
                ((SpinnerNumberModel)sliceIndexSpinner.getModel()).setMaximum(qube[idx]-1);
            }
        }
        try {
            sliceDimensionCB.setSelectedIndex(idx);
        } catch ( IllegalArgumentException ex ) {
            sliceDimensionCB.setSelectedIndex(0);
        }
    }
    

}
