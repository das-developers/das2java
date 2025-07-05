
package org.das2.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
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
    JButton myEntityButton;
    
    /**
     * Creates new form GrannyTextEditor
     */
    public GrannyTextEditor() {
        initComponents();
        //psymButton.setVisible(false);
        plotElementButton.setVisible(false);
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
        String[] misc= new String[] { "&rarr;", "&uarr;", "&#0229;", "&infin;", "&cong;", "&le;", "&ne;", "&ge;", 
            "&sup2;", "&sup3;", "&dagger;", "&deg;", "&int;", "", "More..." };
        for ( int i=0; i<15; i++ ) {
            if ( i<misc.length ) {
                JButton b= miscButton(misc[i]);
                if ( misc[i].length()==0 ) {
                    myEntityButton= b;
                }
                miscTab.add( b );
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
        } else if ( id.equals("plotElement") ) {
            plotElementButton.setVisible(true);
            plotElementButton.setEnabled(true);
        } else if ( id.equals("block") ) {
            // just ignore for this GUI, psym should be used.
        } else {
            System.err.println("not supported: "+id);
        }
        
    }
        
    /** 
     * add one button.  This rev just allows one button to be added.
     * @param tabName the name of the new tab
     * @param id 
     * @param insert text to insert
     */
    public void addButton( String tabName, String id, String insert ) {
        JPanel tabPanel=null;
        for ( int i=0; i<jTabbedPane1.getTabCount(); i++ ) {
            if ( jTabbedPane1.getTitleAt(i).equals(tabName) ) {
                tabPanel= (JPanel)jTabbedPane1.getComponentAt(i);
            }
        }
        if ( tabPanel==null ) {
            tabPanel= new JPanel();
            tabPanel.setLayout( new GridLayout( 3, 5 ) );
            for ( int i=0; i<15; i++ ) {
                tabPanel.add( new javax.swing.JLabel(" ") );
            }
            jTabbedPane1.add( tabName, tabPanel );
        }
        int i;
        for ( i=0; i<15; i++ ) {
            Component c= tabPanel.getComponent(i);
            if ( c instanceof javax.swing.JLabel ) {
                break;
            }
        }
        
        JButton customButton= new JButton(id);
        tabPanel.add(customButton,i);
        
        customButton.setVisible(true);
        customButton.setEnabled(true); 
        customButton.setAction( new AbstractAction(id) {
            @Override
            public void actionPerformed(ActionEvent e) {
                doInsert( insert, "" );
            }
        } );
        
    }
    
    
    private javax.swing.JButton miscButton( String s ) {
        javax.swing.JButton result= new javax.swing.JButton("<html>"+s);
        result.addActionListener((ActionEvent e) -> {
            String sel= result.getText().substring(6);
            if ( sel.equals("More...") ) {  // Yes, I coded this and I'm proud of it.
                sel= Entities.pickEntityGUI();
                if ( sel.isEmpty() ){
                    return;
                }
                if ( myEntityButton!=null ) {
                    myEntityButton.setText("<html>"+sel);
                    myEntityButton.repaint();
                }
            }
            if ( ( e.getModifiers() & ActionEvent.CTRL_MASK ) == ActionEvent.CTRL_MASK ) {
                String s1= Entities.decode(sel);
                if ( s1.length()==0 ) {
                    doInsert( sel, null );
                } else {
                    doInsert( s1, null );
                }
            } else {
                doInsert( sel, null );
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
        jPanel1 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        solidConnectRB = new javax.swing.JRadioButton();
        jLabel10 = new javax.swing.JLabel();
        lineThicknessTF = new javax.swing.JTextField();
        jRadioButton11 = new javax.swing.JRadioButton();
        noneConnectorRB = new javax.swing.JRadioButton();
        jPanel2 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        psymColorCB = new javax.swing.JCheckBox();
        psymColorTextField = new javax.swing.JTextField();
        jPanel3 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        sizeTextField = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jRadioButton9 = new javax.swing.JRadioButton();
        outlineFillStyleRB = new javax.swing.JRadioButton();
        noneFillStyleRB = new javax.swing.JRadioButton();
        jPanel4 = new javax.swing.JPanel();
        jRadioButton8 = new javax.swing.JRadioButton();
        jRadioButton5 = new javax.swing.JRadioButton();
        jRadioButton4 = new javax.swing.JRadioButton();
        jRadioButton3 = new javax.swing.JRadioButton();
        jRadioButton2 = new javax.swing.JRadioButton();
        jRadioButton1 = new javax.swing.JRadioButton();
        jRadioButton6 = new javax.swing.JRadioButton();
        jRadioButton7 = new javax.swing.JRadioButton();
        jLabel3 = new javax.swing.JLabel();
        plotSymbolButtonGroup = new javax.swing.ButtonGroup();
        connectButtonGroup = new javax.swing.ButtonGroup();
        fillStyleButtonGroup = new javax.swing.ButtonGroup();
        imagePanel = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        imageUrlTF = new javax.swing.JTextField();
        imageSizeCB = new javax.swing.JCheckBox();
        imageSizeTF = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        plotElementPanel = new javax.swing.JPanel();
        jLabel9 = new javax.swing.JLabel();
        peNumber = new javax.swing.JTextField();
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
        plotElementButton = new javax.swing.JButton();
        greekTab = new javax.swing.JPanel();
        miscTab = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        instructionalLabel = new javax.swing.JLabel();

        psymPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jLabel6.setText("Connector:");

        connectButtonGroup.add(solidConnectRB);
        solidConnectRB.setText("solid");

        jLabel10.setText("Line Thickness:");

        lineThicknessTF.setText("1.0");

        connectButtonGroup.add(jRadioButton11);
        jRadioButton11.setText("dots");

        connectButtonGroup.add(noneConnectorRB);
        noneConnectorRB.setSelected(true);
        noneConnectorRB.setText("none");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(lineThicknessTF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel10)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(jLabel6)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jRadioButton11)
                            .addComponent(solidConnectRB)
                            .addComponent(noneConnectorRB))))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(solidConnectRB)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(noneConnectorRB)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jRadioButton11, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel10)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lineThicknessTF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(8, Short.MAX_VALUE))
        );

        jPanel1Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {jRadioButton11, noneConnectorRB, solidConnectRB});

        jButton1.setText("Pick...");

        org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, psymColorCB, org.jdesktop.beansbinding.ELProperty.create("${selected}"), jButton1, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        psymColorCB.setText("Color:");
        psymColorCB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                psymColorCBActionPerformed(evt);
            }
        });

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, psymColorCB, org.jdesktop.beansbinding.ELProperty.create("${selected}"), psymColorTextField, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(psymColorCB)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(psymColorTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton1)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(psymColorCB)
                    .addComponent(psymColorTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 78, Short.MAX_VALUE))
        );

        jLabel4.setText("Size:");

        sizeTextField.setText("0.5em");

        jLabel5.setText("Fill Style:");

        fillStyleButtonGroup.add(jRadioButton9);
        jRadioButton9.setSelected(true);
        jRadioButton9.setText("solid");

        fillStyleButtonGroup.add(outlineFillStyleRB);
        outlineFillStyleRB.setText("outline");

        fillStyleButtonGroup.add(noneFillStyleRB);
        noneFillStyleRB.setText("none");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5)
                    .addComponent(jLabel4)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(noneFillStyleRB)
                            .addComponent(jRadioButton9)
                            .addComponent(outlineFillStyleRB)))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(sizeTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sizeTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(1, 1, 1)
                .addComponent(jRadioButton9)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(outlineFillStyleRB)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(noneFillStyleRB)
                .addContainerGap(9, Short.MAX_VALUE))
        );

        plotSymbolButtonGroup.add(jRadioButton8);
        jRadioButton8.setText("stars");

        plotSymbolButtonGroup.add(jRadioButton5);
        jRadioButton5.setText("exes");

        plotSymbolButtonGroup.add(jRadioButton4);
        jRadioButton4.setText("diamonds");

        plotSymbolButtonGroup.add(jRadioButton3);
        jRadioButton3.setText("crosses");
        jRadioButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButton3ActionPerformed(evt);
            }
        });

        plotSymbolButtonGroup.add(jRadioButton2);
        jRadioButton2.setText("circles");

        plotSymbolButtonGroup.add(jRadioButton1);
        jRadioButton1.setSelected(true);
        jRadioButton1.setText("boxes");
        jRadioButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButton1ActionPerformed(evt);
            }
        });

        plotSymbolButtonGroup.add(jRadioButton6);
        jRadioButton6.setText("triangles");

        plotSymbolButtonGroup.add(jRadioButton7);
        jRadioButton7.setText("none");
        jRadioButton7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButton7ActionPerformed(evt);
            }
        });

        jLabel3.setText("Plot Symbol:");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jRadioButton2)
                    .addComponent(jRadioButton1)
                    .addComponent(jRadioButton3)
                    .addComponent(jRadioButton4)
                    .addComponent(jRadioButton5)
                    .addComponent(jRadioButton8)
                    .addComponent(jRadioButton6)
                    .addComponent(jRadioButton7))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addComponent(jLabel3)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jRadioButton7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jRadioButton6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jRadioButton1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jRadioButton2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jRadioButton3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jRadioButton4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jRadioButton5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jRadioButton8)
                .addContainerGap(28, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout psymPanelLayout = new javax.swing.GroupLayout(psymPanel);
        psymPanel.setLayout(psymPanelLayout);
        psymPanelLayout.setHorizontalGroup(
            psymPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(psymPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(psymPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(psymPanelLayout.createSequentialGroup()
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(42, Short.MAX_VALUE))
        );
        psymPanelLayout.setVerticalGroup(
            psymPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(psymPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(psymPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(psymPanelLayout.createSequentialGroup()
                        .addGroup(psymPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
        );

        jLabel7.setText("URL of image:");

        imageUrlTF.setText("https://autoplot.org/Logo32.png");

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

        jLabel9.setText("Plot Element Number:");

        peNumber.setText(" ");

        javax.swing.GroupLayout plotElementPanelLayout = new javax.swing.GroupLayout(plotElementPanel);
        plotElementPanel.setLayout(plotElementPanelLayout);
        plotElementPanelLayout.setHorizontalGroup(
            plotElementPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(plotElementPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(plotElementPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(plotElementPanelLayout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(peNumber, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel9))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        plotElementPanelLayout.setVerticalGroup(
            plotElementPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(plotElementPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel9)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(peNumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(48, Short.MAX_VALUE))
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
        psymButton.setToolTipText("Add a symbol with given color and shape");
        psymButton.setEnabled(false);
        psymButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                psymButtonActionPerformed(evt);
            }
        });

        imageButton.setText("Image");
        imageButton.setToolTipText("Insert an image from a url");
        imageButton.setEnabled(false);
        imageButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                imageButtonActionPerformed(evt);
            }
        });

        plotElementButton.setText("PlotElement");
        plotElementButton.setToolTipText("insert a symbol based on a plot element");
        plotElementButton.setEnabled(false);
        plotElementButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                plotElementButtonActionPerformed(evt);
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
                        .addComponent(imageButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(plotElementButton)))
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
                    .addComponent(imageButton)
                    .addComponent(plotElementButton))
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
        String theText= jTextArea1.getText();
        String ext= getExtension();
        if ( ext!=null ) {
            int i= theText.indexOf(ext);
            if ( i==-1 ) i= jTextArea1.getCaretPosition();
            jTextArea1.replaceRange( text, i, i+ext.length() );
            if ( jTextArea1.getSelectionStart()!=jTextArea1.getSelectionEnd() && endt!=null ) {
                jTextArea1.insert(endt, jTextArea1.getSelectionEnd() );
            }
        } else {
            if ( jTextArea1.getSelectionStart()!=jTextArea1.getSelectionEnd() && endt!=null ) {
                jTextArea1.insert( endt, jTextArea1.getSelectionEnd() );
                jTextArea1.insert( text, jTextArea1.getSelectionStart() );
            } else {
                jTextArea1.insert( text, jTextArea1.getSelectionStart() );
            }
        }
    }
    
    /**
     * return the extension, or null if we are not within an extension.
     * @return 
     */
    private String getExtension( ) {
        String ss= jTextArea1.getText();
        int i1= ss.lastIndexOf("!(",jTextArea1.getCaretPosition());
        if ( i1==-1 ) {
            return null;
        }
        int i2= ss.indexOf(")",i1);
        if ( i2==-1 ) {
            return null;
        }
        if ( i2>=jTextArea1.getCaretPosition()-1 ) {
            return ss.substring(i1,i2+1);
        } else {
            return null;
        }
    }
    
    private void jTextArea1CaretPositionChanged(java.awt.event.InputMethodEvent evt) {//GEN-FIRST:event_jTextArea1CaretPositionChanged
        tickleTimer.tickle();
    }//GEN-LAST:event_jTextArea1CaretPositionChanged

    private void colorButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_colorButtonActionPerformed
        String extension= getExtension();
        JColorChooser chooser= new JColorChooser();
        if ( extension!=null ) {
            String[] ss= extension.substring(2,extension.length()-1).split(";");
            chooser.setColor( ColorUtil.decodeColor(ss[ss.length-1]) );
        }
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

    private void setPsymPanelSettings( String settings ) {
        settings= settings.substring(2,settings.length()-1);
        String[] ss= settings.split(";");
        psymColorCB.setSelected(false);
        psymColorTextField.setBackground( ColorUtil.BLACK );
        sizeTextField.setText("1.0");
        for ( String s : ss ) {
            int i= s.indexOf("=");
            String n,v;
            if ( i>-1 ) {
                n= s.substring(0,i);
                v= s.substring(i+1);
            } else {
                n= s;
                v= null;
            }
            switch ( n ) {
                case "painter":
                case "psym":
                    break;
                case "size":
                    sizeTextField.setText(v);
                    break;
                case "fillStyle":
                    if ( v.equals("outline") ) {
                        outlineFillStyleRB.setSelected(true);
                    } else if ( v.equals("none") ) {
                        noneFillStyleRB.setSelected(true);
                    } else {
                        jRadioButton9.setSelected(true);
                    }
                    break;
                case "connect":
                    if ( v.equals("solid") ) {
                        solidConnectRB.setSelected(true);
                    } else if ( v.equals("dots") ) {
                        jRadioButton11.setSelected(true);
                    } else {
                        noneConnectorRB.setSelected(true);
                    }
                    break;
                case "color":
                    psymColorTextField.setBackground( ColorUtil.decodeColor(v));
                    psymColorCB.setSelected(true);
                    break;
                case "lineThick":
                    lineThicknessTF.setText(v);
                    break;                    
                default:
                    Enumeration<AbstractButton> bbs= plotSymbolButtonGroup.getElements();
                    while ( bbs.hasMoreElements() ) {
                        javax.swing.AbstractButton b= bbs.nextElement();
                        if ( b.getText().equals(n) ) {
                            b.setSelected(true);
                        }
                    }
                    break;
            }
        }
    }
    

    private void setImagePanelSettings( String settings ) {
        settings= settings.substring(2,settings.length()-1);
        String[] ss= settings.split(";");
        String surl= ss[2];
        String scale= ss.length==4 ? ss[3] : "";
        
        imageSizeCB.setSelected(scale.length()>0);
        imageSizeTF.setText(scale);
        
        imageUrlTF.setText(surl);
    }

    private void psymButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_psymButtonActionPerformed
        String extension= getExtension();
        if ( extension!=null && extension.startsWith("!(painter;psym") ) {
            setPsymPanelSettings(extension);
        }
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
            if ( Double.parseDouble(lineThicknessTF.getText())!=1.0 ) {
                textb.append(";lineThick=").append(lineThicknessTF.getText());
            }
            textb.append(")");
            doInsert( textb.toString(), null );
        }
    }//GEN-LAST:event_psymButtonActionPerformed

    private void imageButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_imageButtonActionPerformed
        String extension= getExtension();
        if ( extension!=null && extension.startsWith("!(painter;img") ) {
            setImagePanelSettings(extension);
        }
        if ( JOptionPane.OK_OPTION==
                JOptionPane.showConfirmDialog( this, imagePanel, "Image Options", JOptionPane.OK_CANCEL_OPTION ) ) {
            StringBuilder textb= new StringBuilder( "!(painter;img" );
            textb.append(";").append(imageUrlTF.getText());
            if ( imageSizeCB.isSelected() ) {
                textb.append(";").append(imageSizeTF.getText().trim());
            }
            textb.append(")");
            doInsert( textb.toString(), null );
        }
    }//GEN-LAST:event_imageButtonActionPerformed

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

    private void jRadioButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButton3ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jRadioButton3ActionPerformed

    private void jRadioButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButton1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jRadioButton1ActionPerformed

    private void plotElementButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_plotElementButtonActionPerformed
         if ( JOptionPane.OK_OPTION==
                JOptionPane.showConfirmDialog( this, plotElementPanel, "Plot Element Options", JOptionPane.OK_CANCEL_OPTION ) ) {
            String textb= "!(painter;legend;plotElement="+peNumber.getText()+")";
            doInsert( textb, null );
        }
    }//GEN-LAST:event_plotElementButtonActionPerformed

    private void peNumberActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_peNumberActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_peNumberActionPerformed

    private void imageSizeCBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_imageSizeCBActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_imageSizeCBActionPerformed

    private void jRadioButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButton7ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jRadioButton7ActionPerformed

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
        edit.addButton( "Macros", "%{CONTEXT}", "%{CONTEXT,format=%d,id=}");
        edit.addButton( "Macros", "%{TIMERANGE}", "%{TIMERANGE,format=%d,id=}");
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
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JRadioButton jRadioButton1;
    private javax.swing.JRadioButton jRadioButton11;
    private javax.swing.JRadioButton jRadioButton2;
    private javax.swing.JRadioButton jRadioButton3;
    private javax.swing.JRadioButton jRadioButton4;
    private javax.swing.JRadioButton jRadioButton5;
    private javax.swing.JRadioButton jRadioButton6;
    private javax.swing.JRadioButton jRadioButton7;
    private javax.swing.JRadioButton jRadioButton8;
    private javax.swing.JRadioButton jRadioButton9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextField lineThicknessTF;
    private javax.swing.JPanel miscTab;
    private javax.swing.JButton nButton;
    private javax.swing.JRadioButton noneConnectorRB;
    private javax.swing.JRadioButton noneFillStyleRB;
    private javax.swing.JRadioButton outlineFillStyleRB;
    private javax.swing.JTextField peNumber;
    private javax.swing.JButton plotElementButton;
    private javax.swing.JPanel plotElementPanel;
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
