
package org.das2.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.util.Enumeration;
import javax.swing.AbstractButton;
import javax.swing.JColorChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.das2.util.ColorUtil;
import org.das2.util.DesktopColorChooserPanel;
import org.das2.util.Entities;
import org.das2.util.GrannyTextRenderer;
import org.das2.util.NamedColorChooserPanel;
import org.das2.util.StringSchemeEditor;
import org.das2.util.TickleTimer;

/**
 * Graphical editor for "Granny" text strings on the canvas.
 * @see https://sourceforge.net/p/autoplot/feature-requests/758
 * @author jbf
 */
public class GrannyTextEditor extends javax.swing.JPanel implements StringSchemeEditor {
    
    /**
     * suggested editor title
     */
    public static final String EDITOR_TITLE= "Granny Text Editor";
            
    JPanel canvas;
    GrannyTextRenderer gtr;
    TickleTimer tickleTimer;
    
    /**
     * Creates new form GrannyTextEditor
     */
    public GrannyTextEditor() {
        initComponents();
        //psymButton.setVisible(false);
        canvas= new JPanel() {
            @Override
            protected void paintComponent(Graphics g1) {
                super.paintComponent(g1);
                Graphics2D g= (Graphics2D)g1;
                g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
                Rectangle r= g.getClipBounds( renderPanel.getBounds() );
                g.setColor( Color.WHITE );
                g.fillRect( r.x, r.y, r.width, r.height );
                g.setColor( Color.BLACK );
                gtr.draw(g, 10, 14 );
                g.setColor( Color.LIGHT_GRAY );
                g.drawRect( 10, 14 - (int)gtr.getAscent(), (int)gtr.getWidth(), (int)gtr.getHeight() );
            }
        };
        
        renderPanel.setLayout( new BorderLayout() );
        renderPanel.add( canvas, BorderLayout.CENTER );
        gtr= new GrannyTextRenderer();
        gtr.setString( canvas.getFont(), "" );
        tickleTimer= new TickleTimer( 300, (PropertyChangeEvent evt) -> {
            SwingUtilities.invokeLater(() -> {
                updateImage();
            });
        });
        jTextArea1.getDocument().addDocumentListener( new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                tickleTimer.tickle();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                tickleTimer.tickle();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                tickleTimer.tickle();
            }
        } );
        String[] greek= new String[] { "&alpha;", "&beta;", "&Gamma;", "&Delta;", "&Theta;", 
            "&eta;", "&Lambda;", "&mu;", "&pi;", "&rho;",
            "&epsilon;", "&omega;", "&Phi;", "&tau;", "&chi;" };
        for ( int i=0; i<15; i++ ) {
            if ( i<greek.length ) {
                greekTab.add( miscButton(greek[i]) );
            } else {
                greekTab.add( new javax.swing.JLabel(" ") );
            }
        }
        String[] misc= new String[] { "&rarr;", "&uarr;", "&#0229;" };
        for ( int i=0; i<15; i++ ) {
            if ( i<misc.length ) {
                miscTab.add( miscButton(misc[i]) );
            } else {
                miscTab.add( new javax.swing.JLabel(" ") );
            }
        }
    }

    /**
     * add the application-specific painter to the GrannyTextRenderer used
     * to preview.
     * @param id id for the painter, where the id is found in the granny text string
     * @param p the painter code which draws on a graphics context.
     */
    public void addPainter( String id, GrannyTextRenderer.Painter p ) {
        gtr.addPainter( id, p );
        if ( id.equals("psym") ) {
            psymButton.setVisible(true);
            psymButton.setEnabled(true);
        } else if ( id.equals("img") ) {
            imageButton.setVisible(true);
            imageButton.setEnabled(true);
        }
        
    }
    
    private javax.swing.JButton miscButton( String s ) {
        javax.swing.JButton result= new javax.swing.JButton("<html>"+s);
        result.addActionListener((ActionEvent e) -> {
            if ( ( e.getModifiers() & ActionEvent.CTRL_MASK ) == ActionEvent.CTRL_MASK ) {
                String s1= Entities.decode(s);
                if ( s1.length()==0 ) {
                    doInsert( s, null );
                } else {
                    doInsert( s1, null );
                }
            } else {
                doInsert( s, null );
            }
        } );
        return result;
    }
    
    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        bindingGroup = new org.jdesktop.beansbinding.BindingGroup();

        psymPanel = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jRadioButton1 = new javax.swing.JRadioButton();
        jRadioButton2 = new javax.swing.JRadioButton();
        jRadioButton3 = new javax.swing.JRadioButton();
        jRadioButton4 = new javax.swing.JRadioButton();
        jRadioButton5 = new javax.swing.JRadioButton();
        jRadioButton6 = new javax.swing.JRadioButton();
        jRadioButton7 = new javax.swing.JRadioButton();
        jLabel4 = new javax.swing.JLabel();
        sizeTextField = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        outlineFillStyleRB = new javax.swing.JRadioButton();
        jRadioButton9 = new javax.swing.JRadioButton();
        jLabel6 = new javax.swing.JLabel();
        solidConnectRB = new javax.swing.JRadioButton();
        jRadioButton11 = new javax.swing.JRadioButton();
        noneConnectorRB = new javax.swing.JRadioButton();
        psymColorCB = new javax.swing.JCheckBox();
        jButton1 = new javax.swing.JButton();
        psymColorTextField = new javax.swing.JTextField();
        noneFillStyleRB = new javax.swing.JRadioButton();
        plotSymbolButtonGroup = new javax.swing.ButtonGroup();
        connectButtonGroup = new javax.swing.ButtonGroup();
        fillStyleButtonGroup = new javax.swing.ButtonGroup();
        imagePanel = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        imageUrlTF = new javax.swing.JTextField();
        imageSizeCB = new javax.swing.JCheckBox();
        imageSizeTF = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        renderPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        hersheyTab = new javax.swing.JPanel();
        aButton = new javax.swing.JButton();
        bButton = new javax.swing.JButton();
        sButton = new javax.swing.JButton();
        rButton = new javax.swing.JButton();
        exclaimationPointButton = new javax.swing.JButton();
        nButton = new javax.swing.JButton();
        extensionsTab = new javax.swing.JPanel();
        colorButton = new javax.swing.JButton();
        italicButton = new javax.swing.JButton();
        boldButton = new javax.swing.JButton();
        underlineButton = new javax.swing.JButton();
        psymButton = new javax.swing.JButton();
        imageButton = new javax.swing.JButton();
        greekTab = new javax.swing.JPanel();
        miscTab = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        instructionalLabel = new javax.swing.JLabel();

        jLabel3.setText("Plot Symbol:");

        plotSymbolButtonGroup.add(jRadioButton1);
        jRadioButton1.setSelected(true);
        jRadioButton1.setText("boxes");
        jRadioButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButton1ActionPerformed(evt);
            }
        });

        plotSymbolButtonGroup.add(jRadioButton2);
        jRadioButton2.setText("circles");

        plotSymbolButtonGroup.add(jRadioButton3);
        jRadioButton3.setText("crosses");
        jRadioButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButton3ActionPerformed(evt);
            }
        });

        plotSymbolButtonGroup.add(jRadioButton4);
        jRadioButton4.setText("diamonds");

        plotSymbolButtonGroup.add(jRadioButton5);
        jRadioButton5.setText("exes");

        plotSymbolButtonGroup.add(jRadioButton6);
        jRadioButton6.setText("triangles");

        plotSymbolButtonGroup.add(jRadioButton7);
        jRadioButton7.setText("none");

        jLabel4.setText("Size:");

        sizeTextField.setText("0.5em");

        jLabel5.setText("Fill Style:");

        fillStyleButtonGroup.add(outlineFillStyleRB);
        outlineFillStyleRB.setText("outline");

        fillStyleButtonGroup.add(jRadioButton9);
        jRadioButton9.setSelected(true);
        jRadioButton9.setText("solid");

        jLabel6.setText("Connector:");

        connectButtonGroup.add(solidConnectRB);
        solidConnectRB.setText("solid");

        connectButtonGroup.add(jRadioButton11);
        jRadioButton11.setText("dots");

        connectButtonGroup.add(noneConnectorRB);
        noneConnectorRB.setSelected(true);
        noneConnectorRB.setText("none");

        psymColorCB.setText("Color:");
        psymColorCB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                psymColorCBActionPerformed(evt);
            }
        });

        jButton1.setText("Pick...");

        org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, psymColorCB, org.jdesktop.beansbinding.ELProperty.create("${selected}"), jButton1, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, psymColorCB, org.jdesktop.beansbinding.ELProperty.create("${selected}"), psymColorTextField, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        fillStyleButtonGroup.add(noneFillStyleRB);
        noneFillStyleRB.setText("none");

        javax.swing.GroupLayout psymPanelLayout = new javax.swing.GroupLayout(psymPanel);
        psymPanel.setLayout(psymPanelLayout);
        psymPanelLayout.setHorizontalGroup(
            psymPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(psymPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(psymPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addGroup(psymPanelLayout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addGroup(psymPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jRadioButton2)
                            .addComponent(jRadioButton1)
                            .addComponent(jRadioButton3)
                            .addComponent(jRadioButton4)
                            .addComponent(jRadioButton5)
                            .addComponent(jRadioButton6)
                            .addComponent(jRadioButton7))))
                .addGap(12, 12, 12)
                .addGroup(psymPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(psymPanelLayout.createSequentialGroup()
                        .addGroup(psymPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel5)
                            .addComponent(jLabel4)
                            .addGroup(psymPanelLayout.createSequentialGroup()
                                .addGap(12, 12, 12)
                                .addGroup(psymPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(sizeTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jRadioButton9)
                                    .addComponent(noneFillStyleRB)
                                    .addComponent(outlineFillStyleRB))))
                        .addGap(25, 25, 25)
                        .addGroup(psymPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel6)
                            .addGroup(psymPanelLayout.createSequentialGroup()
                                .addGap(12, 12, 12)
                                .addGroup(psymPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jRadioButton11)
                                    .addComponent(solidConnectRB)
                                    .addComponent(noneConnectorRB)))))
                    .addGroup(psymPanelLayout.createSequentialGroup()
                        .addGap(1, 1, 1)
                        .addComponent(psymColorCB)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(psymColorTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton1)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        psymPanelLayout.setVerticalGroup(
            psymPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(psymPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(psymPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jLabel4)
                    .addComponent(jLabel6))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(psymPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jRadioButton1)
                    .addComponent(sizeTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(solidConnectRB))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(psymPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jRadioButton2)
                    .addComponent(jRadioButton11)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(psymPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(psymPanelLayout.createSequentialGroup()
                        .addComponent(jRadioButton3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(psymPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jRadioButton4)
                            .addComponent(outlineFillStyleRB))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jRadioButton5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jRadioButton6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jRadioButton7))
                    .addGroup(psymPanelLayout.createSequentialGroup()
                        .addGroup(psymPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jRadioButton9)
                            .addComponent(noneConnectorRB))
                        .addGap(23, 23, 23)
                        .addComponent(noneFillStyleRB)
                        .addGap(16, 16, 16)
                        .addGroup(psymPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButton1)
                            .addComponent(psymColorCB)
                            .addComponent(psymColorTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jLabel7.setText("URL of image:");

        imageUrlTF.setText("http://autoplot.org/wiki/images/Logo96.png");

        imageSizeCB.setText("Size:");

        imageSizeTF.setText("50%");

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, imageSizeCB, org.jdesktop.beansbinding.ELProperty.create("${selected}"), imageSizeTF, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        jLabel8.setText("<html><i>50% means half of original size; 2em means two em heights; 20 or 20px means 20 pixels");

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, imageSizeCB, org.jdesktop.beansbinding.ELProperty.create("${selected}"), jLabel8, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        javax.swing.GroupLayout imagePanelLayout = new javax.swing.GroupLayout(imagePanel);
        imagePanel.setLayout(imagePanelLayout);
        imagePanelLayout.setHorizontalGroup(
            imagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(imagePanelLayout.createSequentialGroup()
                .addGap(36, 36, 36)
                .addComponent(imageSizeTF, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(imagePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(imagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(imagePanelLayout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addContainerGap(603, Short.MAX_VALUE))
                    .addGroup(imagePanelLayout.createSequentialGroup()
                        .addGroup(imagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(imagePanelLayout.createSequentialGroup()
                                .addGap(21, 21, 21)
                                .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(imageSizeCB))
                        .addGap(0, 0, Short.MAX_VALUE))))
            .addGroup(imagePanelLayout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addComponent(imageUrlTF)
                .addContainerGap())
        );
        imagePanelLayout.setVerticalGroup(
            imagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(imagePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(imageUrlTF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(imageSizeCB)
                .addGap(5, 5, 5)
                .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(imageSizeTF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        renderPanel.setLayout(new java.awt.BorderLayout());

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jTextArea1.addInputMethodListener(new java.awt.event.InputMethodListener() {
            public void inputMethodTextChanged(java.awt.event.InputMethodEvent evt) {
            }
            public void caretPositionChanged(java.awt.event.InputMethodEvent evt) {
                jTextArea1CaretPositionChanged(evt);
            }
        });
        jScrollPane1.setViewportView(jTextArea1);

        jTabbedPane1.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jTabbedPane1StateChanged(evt);
            }
        });

        aButton.setText("A - Shift Up");
        aButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aButtonActionPerformed(evt);
            }
        });

        bButton.setText("B - Shift Down");
        bButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bButtonActionPerformed(evt);
            }
        });

        sButton.setText("S - Save Position");
        sButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sButtonActionPerformed(evt);
            }
        });

        rButton.setText("R - Restore Position");
        rButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rButtonActionPerformed(evt);
            }
        });

        exclaimationPointButton.setText("! Character");
        exclaimationPointButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exclaimationPointButtonActionPerformed(evt);
            }
        });

        nButton.setText("N - Normal");
        nButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout hersheyTabLayout = new javax.swing.GroupLayout(hersheyTab);
        hersheyTab.setLayout(hersheyTabLayout);
        hersheyTabLayout.setHorizontalGroup(
            hersheyTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(hersheyTabLayout.createSequentialGroup()
                .addGroup(hersheyTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(nButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(bButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(aButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addGroup(hersheyTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(rButton, javax.swing.GroupLayout.DEFAULT_SIZE, 186, Short.MAX_VALUE)
                    .addComponent(sButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(exclaimationPointButton)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        hersheyTabLayout.setVerticalGroup(
            hersheyTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(hersheyTabLayout.createSequentialGroup()
                .addGroup(hersheyTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(aButton)
                    .addComponent(sButton)
                    .addComponent(exclaimationPointButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(hersheyTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(bButton)
                    .addComponent(rButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(nButton))
        );

        jTabbedPane1.addTab("Granny", hersheyTab);

        colorButton.setText("Color");
        colorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                colorButtonActionPerformed(evt);
            }
        });

        italicButton.setText("<html><i>Italic");
        italicButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                italicButtonActionPerformed(evt);
            }
        });

        boldButton.setText("<html><b>Bold");
        boldButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                boldButtonActionPerformed(evt);
            }
        });

        underlineButton.setText("<html><u>Underline</u>");
        underlineButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                underlineButtonActionPerformed(evt);
            }
        });

        psymButton.setText("Psym");
        psymButton.setEnabled(false);
        psymButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                psymButtonActionPerformed(evt);
            }
        });

        imageButton.setText("Image");
        imageButton.setEnabled(false);
        imageButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                imageButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout extensionsTabLayout = new javax.swing.GroupLayout(extensionsTab);
        extensionsTab.setLayout(extensionsTabLayout);
        extensionsTabLayout.setHorizontalGroup(
            extensionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(extensionsTabLayout.createSequentialGroup()
                .addGroup(extensionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(extensionsTabLayout.createSequentialGroup()
                        .addComponent(colorButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(boldButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(italicButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(underlineButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(extensionsTabLayout.createSequentialGroup()
                        .addComponent(psymButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(imageButton)))
                .addGap(0, 199, Short.MAX_VALUE))
        );
        extensionsTabLayout.setVerticalGroup(
            extensionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(extensionsTabLayout.createSequentialGroup()
                .addGroup(extensionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(colorButton)
                    .addComponent(boldButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(italicButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(underlineButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(extensionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(psymButton)
                    .addComponent(imageButton))
                .addContainerGap(29, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Extensions", extensionsTab);

        greekTab.setLayout(new java.awt.GridLayout(3, 5));
        jTabbedPane1.addTab("Greek", greekTab);

        miscTab.setLayout(new java.awt.GridLayout(3, 5));
        jTabbedPane1.addTab("Misc", miscTab);

        jLabel1.setText("Preview:");

        jLabel2.setText("Granny Text:");

        instructionalLabel.setText("Press buttons from palette below to insert into text.");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(renderPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jScrollPane1)
            .addComponent(jTabbedPane1)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2)
                    .addComponent(instructionalLabel))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(renderPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 78, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(instructionalLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 112, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        bindingGroup.bind();
    }// </editor-fold>//GEN-END:initComponents

    private void doInsert( String text, String endt ) {
        if ( jTextArea1.getSelectionStart()!=jTextArea1.getSelectionEnd() && endt!=null ) {
            jTextArea1.insert( endt, jTextArea1.getSelectionEnd() );
            jTextArea1.insert( text, jTextArea1.getSelectionStart() );
        } else {
            jTextArea1.insert( text, jTextArea1.getSelectionStart() );
        }
    }
    
    private void jTextArea1CaretPositionChanged(java.awt.event.InputMethodEvent evt) {//GEN-FIRST:event_jTextArea1CaretPositionChanged
        tickleTimer.tickle();
    }//GEN-LAST:event_jTextArea1CaretPositionChanged

    private void colorButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_colorButtonActionPerformed
        JColorChooser chooser= new JColorChooser();
        chooser.addChooserPanel( new NamedColorChooserPanel() );
        chooser.addChooserPanel( new DesktopColorChooserPanel() );
        if ( JOptionPane.showConfirmDialog( this, chooser, "Color Chooser", JOptionPane.OK_CANCEL_OPTION )== JOptionPane.OK_OPTION ) {
            Color color= chooser.getColor();
            String colorName= ColorUtil.nameForColor(color);
            doInsert( "!(color;"+colorName+")", "!(color)" );
        }
    }//GEN-LAST:event_colorButtonActionPerformed

    private void bButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bButtonActionPerformed
        doInsert("!B","!n");
    }//GEN-LAST:event_bButtonActionPerformed

    private void aButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aButtonActionPerformed
        doInsert("!A","!n");
    }//GEN-LAST:event_aButtonActionPerformed

    private void boldButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_boldButtonActionPerformed
        doInsert("<b>","</b>");
    }//GEN-LAST:event_boldButtonActionPerformed

    private void italicButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_italicButtonActionPerformed
        doInsert("<i>","</i>");
    }//GEN-LAST:event_italicButtonActionPerformed

    private void underlineButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_underlineButtonActionPerformed
        doInsert("<u>","</u>");
    }//GEN-LAST:event_underlineButtonActionPerformed

    private void exclaimationPointButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exclaimationPointButtonActionPerformed
        doInsert("!!",null);
    }//GEN-LAST:event_exclaimationPointButtonActionPerformed

    private void sButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sButtonActionPerformed
        doInsert("!S",null);
    }//GEN-LAST:event_sButtonActionPerformed

    private void rButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rButtonActionPerformed
        doInsert("!R",null);
    }//GEN-LAST:event_rButtonActionPerformed

    private void nButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nButtonActionPerformed
        doInsert("!N",null);
    }//GEN-LAST:event_nButtonActionPerformed

    private void jTabbedPane1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jTabbedPane1StateChanged
        if ( jTabbedPane1.getSelectedIndex()>1 ) {
            instructionalLabel.setText( "Press to insert, and holding control will insert the actual character." );
        } else {
           instructionalLabel.setText( "Press buttons from palette below to insert into text." );
        }
    }//GEN-LAST:event_jTabbedPane1StateChanged

    private void psymButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_psymButtonActionPerformed
        if ( JOptionPane.OK_OPTION==
                JOptionPane.showConfirmDialog( this, psymPanel, "Psym Options", JOptionPane.OK_CANCEL_OPTION ) ) {
            StringBuilder textb= new StringBuilder( "!(painter;psym;" );
            Enumeration<AbstractButton> bbs= plotSymbolButtonGroup.getElements();
            while ( bbs.hasMoreElements() ) {
                javax.swing.AbstractButton b= bbs.nextElement();
                if ( b.isSelected() ) {
                    textb.append(b.getText()).append(";");
                }
            }
            textb.append("size=").append(sizeTextField.getText().trim());
            if ( outlineFillStyleRB.isSelected() ) {
                textb.append(";fillStyle=outline");
            } else if ( noneFillStyleRB.isSelected() ) {
                textb.append(";fillStyle=none");
            }
            if ( !noneConnectorRB.isSelected() )  {
                if ( solidConnectRB.isSelected() ) {
                    textb.append(";connect=solid");
                } else {
                    textb.append(";connect=dots");
                }
            }
            if ( psymColorCB.isSelected() ) {
                textb.append(";color=").append(psymColorTextField.getText().trim());
            }
            textb.append(")");
            doInsert( textb.toString(), null );
        }
    }//GEN-LAST:event_psymButtonActionPerformed

    private void jRadioButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButton3ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jRadioButton3ActionPerformed

    private void jRadioButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButton1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jRadioButton1ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        JColorChooser chooser= new JColorChooser();
        chooser.addChooserPanel( new NamedColorChooserPanel() );
        chooser.addChooserPanel( new DesktopColorChooserPanel() );
        if ( JOptionPane.showConfirmDialog( this, chooser, "Color Chooser", JOptionPane.OK_CANCEL_OPTION )== JOptionPane.OK_OPTION ) {
            Color color= chooser.getColor();
            String colorName= ColorUtil.nameForColor(color);
            psymColorTextField.setText( colorName );
            psymColorTextField.setBackground( color );
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void imageButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_imageButtonActionPerformed
        if ( JOptionPane.OK_OPTION==
                JOptionPane.showConfirmDialog( this, imagePanel, "Psym Options", JOptionPane.OK_CANCEL_OPTION ) ) {
            StringBuilder textb= new StringBuilder( "!(painter;img" );
            textb.append(";").append(imageUrlTF.getText());
            if ( imageSizeCB.isSelected() ) {
                textb.append(";").append(imageSizeTF.getText().trim());
            }
            textb.append(")");
            doInsert( textb.toString(), null );
        }
    }//GEN-LAST:event_imageButtonActionPerformed

    private void psymColorCBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_psymColorCBActionPerformed
        if ( !psymColorCB.isSelected() ) {
            JTextField s= new JTextField();
            psymColorTextField.setBackground(s.getBackground());
        } else {
            String s= psymColorTextField.getText();
            Color c= ColorUtil.decodeColor(s);
            psymColorTextField.setBackground(c);
        }
    }//GEN-LAST:event_psymColorCBActionPerformed

    private void updateImage() {
        String oldString= gtr.getString();
        try {
            gtr.setString( canvas.getFont(), getValue() );
            canvas.setBackground( Color.WHITE );
        } catch ( RuntimeException ex ) {
            gtr.setString( canvas.getFont(), oldString );
            canvas.setBackground( Color.RED );
        }
        canvas.repaint();
    }

    @Override
    public String getValue() {
        String text= jTextArea1.getText();
        text= text.replaceAll("\n","<br>");
        return text;
    }
    
    @Override
    public void setValue( String text ) {
        text= text.replaceAll("\\<br\\>","\n");
        text= text.replaceAll("\\![cC]", "\n");
        jTextArea1.setText(text);
        updateImage();
    }
    
    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public void setContext(Object o) {
        
    }    
    
    public static void main( String[] args ) {
        GrannyTextEditor edit= new GrannyTextEditor();
        edit.setValue( "Happy !(color;Blue)Day!!");
        edit.addPainter( "psym", null );
        edit.addPainter( "img", null );
        JOptionPane.showMessageDialog( null, edit );
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton aButton;
    private javax.swing.JButton bButton;
    private javax.swing.JButton boldButton;
    private javax.swing.JButton colorButton;
    private javax.swing.ButtonGroup connectButtonGroup;
    private javax.swing.JButton exclaimationPointButton;
    private javax.swing.JPanel extensionsTab;
    private javax.swing.ButtonGroup fillStyleButtonGroup;
    private javax.swing.JPanel greekTab;
    private javax.swing.JPanel hersheyTab;
    private javax.swing.JButton imageButton;
    private javax.swing.JPanel imagePanel;
    private javax.swing.JCheckBox imageSizeCB;
    private javax.swing.JTextField imageSizeTF;
    private javax.swing.JTextField imageUrlTF;
    private javax.swing.JLabel instructionalLabel;
    private javax.swing.JButton italicButton;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JRadioButton jRadioButton1;
    private javax.swing.JRadioButton jRadioButton11;
    private javax.swing.JRadioButton jRadioButton2;
    private javax.swing.JRadioButton jRadioButton3;
    private javax.swing.JRadioButton jRadioButton4;
    private javax.swing.JRadioButton jRadioButton5;
    private javax.swing.JRadioButton jRadioButton6;
    private javax.swing.JRadioButton jRadioButton7;
    private javax.swing.JRadioButton jRadioButton9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JPanel miscTab;
    private javax.swing.JButton nButton;
    private javax.swing.JRadioButton noneConnectorRB;
    private javax.swing.JRadioButton noneFillStyleRB;
    private javax.swing.JRadioButton outlineFillStyleRB;
    private javax.swing.ButtonGroup plotSymbolButtonGroup;
    private javax.swing.JButton psymButton;
    private javax.swing.JCheckBox psymColorCB;
    private javax.swing.JTextField psymColorTextField;
    private javax.swing.JPanel psymPanel;
    private javax.swing.JButton rButton;
    private javax.swing.JPanel renderPanel;
    private javax.swing.JButton sButton;
    private javax.swing.JTextField sizeTextField;
    private javax.swing.JRadioButton solidConnectRB;
    private javax.swing.JButton underlineButton;
    private org.jdesktop.beansbinding.BindingGroup bindingGroup;
    // End of variables declaration//GEN-END:variables

}
