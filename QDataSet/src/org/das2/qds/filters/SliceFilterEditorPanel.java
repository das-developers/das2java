/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.qds.filters;

import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import org.das2.datum.Datum;
import org.das2.datum.Units;
import org.das2.util.LoggerManager;
import org.das2.qds.DataSetUtil;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;
import static org.das2.qds.filters.FilterEditorPanel.PROP_FILTER;

/**
 * Editor panel to replace the original GUI that was a part of the data panel.
 * @author jbf
 */
public class SliceFilterEditorPanel extends AbstractFilterEditorPanel implements FilterEditorPanel {

    static final long t0= System.currentTimeMillis();
    int[] qube= null;
    int sliceByDatumIndex= -1; // the index we are slicing by datum.
    WeakReference<QDataSet> inputDs= null;
    
    /**
     * Creates new form SlicesFilterEditorPanel
     */
    public SliceFilterEditorPanel() {
        if ( !SwingUtilities.isEventDispatchThread() ) {
            logger.log(Level.WARNING, "called off event thread");
        }

        initComponents();
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(sliceIndexSpinner);
        editor.getFormat().setGroupingUsed(false);
        sliceIndexSpinner.setEditor(editor);
        
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
        bindingGroup = new org.jdesktop.beansbinding.BindingGroup();

        buttonGroup1 = new javax.swing.ButtonGroup();
        sliceDimensionCB = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        sliceIndexSpinner = new javax.swing.JSpinner();
        jLabel2 = new javax.swing.JLabel();
        sliceAtDatumTF = new javax.swing.JTextField();
        sliceAtIndexButton = new javax.swing.JRadioButton();
        sliceAtDatumButton = new javax.swing.JRadioButton();

        sliceDimensionCB.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        sliceDimensionCB.setName("sliceDimensionCB"); // NOI18N
        sliceDimensionCB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sliceDimensionCBActionPerformed(evt);
            }
        });

        jLabel1.setText("Index:");
        jLabel1.setToolTipText("Index of the slice");

        sliceIndexSpinner.setModel(new javax.swing.SpinnerNumberModel(0, 0, null, 1));
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

        sliceAtDatumTF.setText("2000-01-01T00:00:00.000Z");
        sliceAtDatumTF.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sliceAtDatumTFActionPerformed(evt);
            }
        });

        buttonGroup1.add(sliceAtIndexButton);

        org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, sliceIndexSpinner, org.jdesktop.beansbinding.ELProperty.create("${enabled}"), sliceAtIndexButton, org.jdesktop.beansbinding.BeanProperty.create("selected"));
        bindingGroup.addBinding(binding);

        buttonGroup1.add(sliceAtDatumButton);
        sliceAtDatumButton.setToolTipText("Slice at time or datum location");

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, sliceAtDatumTF, org.jdesktop.beansbinding.ELProperty.create("${enabled}"), sliceAtDatumButton, org.jdesktop.beansbinding.BeanProperty.create("selected"));
        bindingGroup.addBinding(binding);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sliceDimensionCB, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(sliceAtIndexButton)
                        .addGap(3, 3, 3)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sliceIndexSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sliceAtDatumButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sliceAtDatumTF, javax.swing.GroupLayout.DEFAULT_SIZE, 197, Short.MAX_VALUE)))
                .addGap(12, 12, 12))
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
                    .addComponent(sliceAtIndexButton)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel1)
                        .addComponent(sliceIndexSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(sliceAtDatumTF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sliceAtDatumButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        bindingGroup.bind();
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
        if ( sliceByDatumIndex!=idx ) {
            sliceAtIndexButton.setSelected(true);
            sliceByDatumIndex= idx;
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

    private void sliceAtDatumTFActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sliceAtDatumTFActionPerformed
        updateFeedback();
    }//GEN-LAST:event_sliceAtDatumTFActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JRadioButton sliceAtDatumButton;
    private javax.swing.JTextField sliceAtDatumTF;
    private javax.swing.JRadioButton sliceAtIndexButton;
    private javax.swing.JComboBox sliceDimensionCB;
    private javax.swing.JSpinner sliceIndexSpinner;
    private org.jdesktop.beansbinding.BindingGroup bindingGroup;
    // End of variables declaration//GEN-END:variables

    private void updateFeedback() {
        Runnable run= new Runnable() {
            public void run() {
                int dim= sliceDimensionCB.getSelectedIndex();
                boolean na= true;
                if ( inputDs!=null ) {
                    QDataSet ds= inputDs.get();
                    if ( ds!=null ) {
                        QDataSet dep= (QDataSet)ds.property("DEPEND_"+dim);
                        if ( dep!=null && dep.rank()==1 ) {
                            if ( getIndexMode() ) {
                                int index= (Integer)sliceIndexSpinner.getValue();
                                String s= DataSetUtil.asDatum( dep.slice(index) ).toString();
                                sliceAtDatumTF.setText(s);
                                na= false;
                            } else {
                                String t= sliceAtDatumTF.getText();
                                Units depu= SemanticOps.getUnits(dep);
                                try {
                                    if ( t!=null ) {
                                        Datum dt= depu.parse(t);
                                        int i= DataSetUtil.closestIndex( dep,dt );
                                        Object o= sliceIndexSpinner.getValue();
                                        if ( o!=null && o instanceof Integer ) {
                                            int i0= ((Integer)o);
                                            if ( i0!= i ) {
                                                sliceIndexSpinner.setValue(i);
                                            }
                                        } else {
                                            sliceIndexSpinner.setValue(i);
                                        }
                                        
                                    }
                                } catch (ParseException ex) {
                                    Logger.getLogger(SliceFilterEditorPanel.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        }
                    }
                }        
                if ( na ) {
                    if ( getIndexMode() ) {
                        sliceAtDatumTF.setText("N/A");
                        sliceAtDatumTF.setToolTipText("value is not available");
                    }
                }
            }
        };
        SwingUtilities.invokeLater(run);
    }
    
    private boolean getIndexMode() {
        return !sliceAtDatumButton.isSelected();
    }
    
    private static boolean checkIndexMode( String filter ) {
        Pattern p= Pattern.compile("\\|slice(\\d)\\((\\d+)\\)");
        Matcher m= p.matcher(filter);
        if ( m.matches() ) {
            return true;
        } else {
            return false;
        }
    }
    
    @Override
    public String getFilter() {
        logger.fine( "getFilter" );
        updateFeedback();
        String s;
        if ( getIndexMode() ) {
            s= String.format( "|slice%d(%d)", sliceDimensionCB.getSelectedIndex(), (Integer)sliceIndexSpinner.getValue() );
        } else {
            String pos= sliceAtDatumTF.getText().replaceAll("\\s+","");
            s= String.format("|slice%d('%s')", sliceDimensionCB.getSelectedIndex(), pos );
        }
        logger.log(Level.FINER, "getFilter() -> {0}", s);
        return s;
    }

    @Override
    public void setFilter(String filter) {
        if ( !SwingUtilities.isEventDispatchThread() ) {
            logger.log(Level.WARNING, "called off event thread");
        }
        logger.log(Level.FINE, "setFilter {0}", filter);
        Pattern p= Pattern.compile("\\|slice(\\d)\\((\\d+)\\)");
        Matcher m= p.matcher(filter);
        if ( m.matches() ) {
            int dim= Integer.parseInt(m.group(1));
            int index= Integer.parseInt(m.group(2));
            sliceDimensionCB.setSelectedIndex( dim );
            sliceIndexSpinner.setValue( index );
            sliceAtIndexButton.setSelected(true);
            sliceByDatumIndex= dim;
        } else {
            Pattern p2= Pattern.compile("\\|slice(\\d)\\(\\'(\\S+)\\'\\)");
            Matcher m2= p2.matcher(filter);
            if ( m2.matches() ) {
                int dim= Integer.parseInt(m2.group(1));
                String at= m2.group(2);
                sliceDimensionCB.setSelectedIndex( dim );
                sliceAtDatumTF.setText(at);
                sliceAtDatumTF.setEditable(true);
                sliceAtDatumButton.setSelected(true);
                sliceByDatumIndex= dim;
            }
        }
    }

    @Override
    public void setInput(QDataSet ds) {
        if ( !SwingUtilities.isEventDispatchThread() ) {
            logger.log(Level.WARNING, "called off event thread");
        }
        String oldFilter= getFilter();
        logger.log(Level.FINE, "setInput {0}", ds.toString() );
        this.inputDs= new WeakReference(ds);
        String[] depNames1= FilterEditorPanelUtil.getDimensionNames(ds);
        switch (depNames1.length) {
            case 2:
                logger.log(Level.FINE, "got depNames: {0},{1}", new Object[]{depNames1[0], depNames1[1]});
                break;
            case 3:
                logger.log(Level.FINE, "got depNames: {0},{1},{2}", new Object[]{depNames1[0], depNames1[1], depNames1[2]});
                break;
            case 4:
                logger.log(Level.FINE, "got depNames: {0},{1},{2},{3}", new Object[]{depNames1[0], depNames1[1], depNames1[2], depNames1[3]});
                break;
            default:
                logger.log(Level.FINE, "got depNames in setInput " );
                break;
        }
        int idx= sliceDimensionCB.getSelectedIndex();
        sliceDimensionCB.setModel(new DefaultComboBoxModel(depNames1));
        qube= DataSetUtil.qubeDims(ds);
        if ( qube!=null ) {
            if ( idx<qube.length ) { // transitions
                ((SpinnerNumberModel)sliceIndexSpinner.getModel()).setMaximum(qube[idx]-1);
                int index= ((Integer)sliceIndexSpinner.getValue());
                if ( index> qube[idx]-1 ) {
                    sliceIndexSpinner.setValue(qube[idx]-1);
                }
            }
        }
        try {
            sliceDimensionCB.setSelectedIndex(idx);
        } catch ( IllegalArgumentException ex ) {
            sliceDimensionCB.setSelectedIndex(0);
        }
        String newFilter= getFilter();
        if ( !oldFilter.equals(newFilter) ) {
            firePropertyChange( PROP_FILTER, oldFilter, newFilter );
        }
    }

    @Override
    public boolean validateFilter(String filter, QDataSet in) {
        if ( in==null ) return true;
        qube= DataSetUtil.qubeDims(in);
        logger.log(Level.FINE, "setFilter {0}", filter);
        Pattern p= Pattern.compile("\\|slice(\\d)\\((\\d+)\\)");
        Matcher m= p.matcher(filter);
        if ( m.matches() ) {
            int dim= Integer.parseInt(m.group(1));
            int index= Integer.parseInt(m.group(2));
            if ( dim==0 ) {
                return in.length()>index;
            } else {
                if ( dim>=qube.length ) {
                    return false;
                } else {
                    return qube[dim]>index;
                }
            }
        }
        return true;
    }

}
