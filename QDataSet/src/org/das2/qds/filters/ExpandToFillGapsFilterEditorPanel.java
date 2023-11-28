
package org.das2.qds.filters;

import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.das2.datum.Datum;
import org.das2.qds.QDataSet;
import org.das2.qds.examples.Schemes;
import org.das2.qds.ops.Ops;
import static org.das2.qds.ops.Ops.datum;
import static org.das2.qds.ops.Ops.subtract;

/**
 *
 * @author jbf
 */
public class ExpandToFillGapsFilterEditorPanel extends AbstractFilterEditorPanel {

    public final static String PROP_REGEX= "\\|expandToFillGaps\\((.*)\\)";
    private QDataSet ds;
     
    /**
     * Creates new form ExpandToFillGapsFilterEditorPanel
     */
    public ExpandToFillGapsFilterEditorPanel() {
        initComponents();
        bindingGroup = new org.jdesktop.beansbinding.BindingGroup();
        org.jdesktop.beansbinding.Binding binding;
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding( org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, 
                autoPanel, org.jdesktop.beansbinding.ELProperty.create("${visible}"), 
                autoButton, org.jdesktop.beansbinding.BeanProperty.create("selected"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding( org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, 
                manualPanel, org.jdesktop.beansbinding.ELProperty.create("${visible}"), 
                manualButton, org.jdesktop.beansbinding.BeanProperty.create("selected"));
        bindingGroup.addBinding(binding);
        updateAutoMessage();
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        bindingGroup = new org.jdesktop.beansbinding.BindingGroup();

        buttonGroup1 = new javax.swing.ButtonGroup();
        jLabel1 = new javax.swing.JLabel();
        autoButton = new javax.swing.JRadioButton();
        manualButton = new javax.swing.JRadioButton();
        autoPanel = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        expandRatioTextField = new javax.swing.JTextField();
        aboutDataLabel = new javax.swing.JLabel();
        manualPanel = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        minimumCadenceTF = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        multiplierTF = new javax.swing.JFormattedTextField();

        jLabel1.setText("Expand To Fill Gaps");

        buttonGroup1.add(autoButton);
        autoButton.setSelected(true);
        autoButton.setText("Auto-Detect");

        buttonGroup1.add(manualButton);
        manualButton.setText("Manual");
        manualButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                manualButtonActionPerformed(evt);
            }
        });

        org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, autoButton, org.jdesktop.beansbinding.ELProperty.create("${selected}"), autoPanel, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        jLabel2.setText("Expand Ratio (1.0 is entire gap):");

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, autoPanel, org.jdesktop.beansbinding.ELProperty.create("${enabled}"), jLabel2, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        expandRatioTextField.setText("1.0");

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, autoPanel, org.jdesktop.beansbinding.ELProperty.create("${enabled}"), expandRatioTextField, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        expandRatioTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                expandRatioTextFieldFocusLost(evt);
            }
        });
        expandRatioTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                expandRatioTextFieldActionPerformed(evt);
            }
        });

        aboutDataLabel.setFont(aboutDataLabel.getFont().deriveFont((aboutDataLabel.getFont().getStyle() | java.awt.Font.ITALIC), aboutDataLabel.getFont().getSize()-1));
        aboutDataLabel.setText("jLabel3");

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, autoPanel, org.jdesktop.beansbinding.ELProperty.create("${enabled}"), aboutDataLabel, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        javax.swing.GroupLayout autoPanelLayout = new javax.swing.GroupLayout(autoPanel);
        autoPanel.setLayout(autoPanelLayout);
        autoPanelLayout.setHorizontalGroup(
            autoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(autoPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(autoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(aboutDataLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(autoPanelLayout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(expandRatioTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 60, Short.MAX_VALUE)))
                .addContainerGap())
        );
        autoPanelLayout.setVerticalGroup(
            autoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(autoPanelLayout.createSequentialGroup()
                .addGroup(autoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(expandRatioTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(aboutDataLabel))
        );

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, manualButton, org.jdesktop.beansbinding.ELProperty.create("${selected}"), manualPanel, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        jLabel3.setText("Minimum Cadence:");

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, manualPanel, org.jdesktop.beansbinding.ELProperty.create("${enabled}"), jLabel3, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        minimumCadenceTF.setText("1s");

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, manualPanel, org.jdesktop.beansbinding.ELProperty.create("${enabled}"), minimumCadenceTF, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        jLabel4.setText("Expansion Multiplier:");

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, manualPanel, org.jdesktop.beansbinding.ELProperty.create("${enabled}"), jLabel4, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        multiplierTF.setText("1.0");

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, manualPanel, org.jdesktop.beansbinding.ELProperty.create("${enabled}"), multiplierTF, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        javax.swing.GroupLayout manualPanelLayout = new javax.swing.GroupLayout(manualPanel);
        manualPanel.setLayout(manualPanelLayout);
        manualPanelLayout.setHorizontalGroup(
            manualPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(manualPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(manualPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(manualPanelLayout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(minimumCadenceTF))
                    .addGroup(manualPanelLayout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(multiplierTF, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        manualPanelLayout.setVerticalGroup(
            manualPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(manualPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(manualPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(minimumCadenceTF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(manualPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(multiplierTF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(12, 12, 12)
                                .addComponent(autoButton)
                                .addGap(18, 18, 18)
                                .addComponent(manualButton)))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(autoPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(manualPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(autoButton)
                    .addComponent(manualButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(autoPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(manualPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        bindingGroup.bind();
    }// </editor-fold>//GEN-END:initComponents

    private void manualButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_manualButtonActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_manualButtonActionPerformed

    private void expandRatioTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_expandRatioTextFieldActionPerformed
        updateAutoMessage();
    }//GEN-LAST:event_expandRatioTextFieldActionPerformed

    private void expandRatioTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_expandRatioTextFieldFocusLost
        updateAutoMessage();
    }//GEN-LAST:event_expandRatioTextFieldFocusLost


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel aboutDataLabel;
    private javax.swing.JRadioButton autoButton;
    private javax.swing.JPanel autoPanel;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JTextField expandRatioTextField;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JRadioButton manualButton;
    private javax.swing.JPanel manualPanel;
    private javax.swing.JTextField minimumCadenceTF;
    private javax.swing.JFormattedTextField multiplierTF;
    private org.jdesktop.beansbinding.BindingGroup bindingGroup;
    // End of variables declaration//GEN-END:variables

    @Override
    public void setFilter(String filter) {
        Pattern p= Pattern.compile(PROP_REGEX);
        Matcher m= p.matcher(filter);
        if ( m.matches() ) {
            String arg= m.group(1);
            String[] ss= arg.split(",");
            if ( ss.length==1 ) {
                this.expandRatioTextField.setText(ss[0]);
                this.autoButton.setSelected(true);
            } else {
                this.minimumCadenceTF.setText(ss[0]);
                this.multiplierTF.setText(ss[1]);
                this.manualButton.setSelected(true);
            }
        } else {
            this.expandRatioTextField.setText("0.8");
            this.autoButton.setSelected(true);
        }
    }

    @Override
    public String getFilter() {
        if ( autoButton.isSelected() ) {
            return "|expandToFillGaps("+this.expandRatioTextField.getText()+")";
        } else {
            return "|expandToFillGaps("+this.minimumCadenceTF.getText()+","+this.multiplierTF.getText()+")";
        }
    }

    private void updateAutoMessage() {
        if ( ds!=null ) {    
            QDataSet ttags= (QDataSet)ds.property(QDataSet.DEPEND_0);
            if ( ttags==null ) {
                aboutDataLabel.setText( "dataset didn't have timetags.");
                return;
            }
            QDataSet dts= Ops.abs( Ops.diff(ttags) );
            Datum cadenceMin= Ops.datum( Ops.reduceMin( dts, 0 ) );
            Datum twiceCadenceMin= cadenceMin.multiply(2);
            QDataSet r= Ops.where( Ops.gt( dts, twiceCadenceMin ) );
            if ( r.length()<1 ) {
                aboutDataLabel.setText("no gaps found.");
            } else {
                int[] startIndexes= new int[r.length()+1];
                startIndexes[0]= 0;
                Datum cadenceMax= null; // cadenceMax is the smallest of the big jumps.
                int count= 0;
                for ( int i=0; i<r.length(); i++ ) {
                    startIndexes[i+1]= (int)r.value(i);
                    Datum cadence= datum( subtract( ttags.slice(startIndexes[i+1]+1), ttags.slice(startIndexes[i]) ) );
                    if ( cadenceMax==null || ( cadence.value()>0 && cadence.lt(cadenceMax) ) ) {
                        cadenceMax= cadence;
                        count= startIndexes[i+1]+1 - startIndexes[i];
                    }
                }
                assert cadenceMax!=null;
                double stepFactor= cadenceMax.divide(cadenceMin).divide(count).value(); // step factor would fill the smallest gap.
                double factor;
                try {
                    factor = Double.parseDouble( this.expandRatioTextField.getText() );
                } catch ( NumberFormatException ex ) {
                    factor = 0.8;
                }
                double expansionMultiplier= stepFactor * factor ;
                aboutDataLabel.setText( "fine cadence is "+cadenceMin+" and expand to fill gaps of "+cadenceMax 
                        + " using an expansion multiplier of " + String.format("%.2f", expansionMultiplier ) );
                logger.log(Level.FINE, "expandToFillGaps: cadenceMin={0} cadenceMax={1}", new Object[]{cadenceMin, cadenceMax});
            }        
        } else {
            aboutDataLabel.setText( "no dataset available for context.");
        }
    }
    @Override
    public void setInput(QDataSet ds) {
        this.ds= ds;
        if ( Schemes.isRank2Waveform(ds) ) {
            aboutDataLabel.setText("data is rank 2 waveform.");
        } else if ( ds.rank()==2 ) {
            updateAutoMessage();
        }
    }
    
}
