
package org.das2.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import javax.swing.JColorChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Graphical editor for "Granny" text strings on the canvas.
 * @see https://sourceforge.net/p/autoplot/feature-requests/758
 * @author jbf
 */
public class GrannyTextEditor extends javax.swing.JPanel {
    
    JPanel canvas;
    GrannyTextRenderer gtr;
    TickleTimer tickleTimer;
    
    /**
     * Creates new form GrannyTextEditor
     */
    public GrannyTextEditor() {
        initComponents();
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
                gtr.draw( g, 50, 50 );
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
        String[] greek= new String[] { "&epsilon;", "&omega;", "&Omega;", "&Tau;", "&tau;" };
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

    private javax.swing.JButton miscButton( String s ) {
        javax.swing.JButton result= new javax.swing.JButton("<html>"+s);
        result.addActionListener((ActionEvent e) -> {
            doInsert( s, null );
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
        greekTab = new javax.swing.JPanel();
        miscTab = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();

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

        underlineButton.setText("<html><u>underline</u>");
        underlineButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                underlineButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout extensionsTabLayout = new javax.swing.GroupLayout(extensionsTab);
        extensionsTab.setLayout(extensionsTabLayout);
        extensionsTabLayout.setHorizontalGroup(
            extensionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(extensionsTabLayout.createSequentialGroup()
                .addComponent(colorButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(boldButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(italicButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(underlineButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 139, Short.MAX_VALUE))
        );
        extensionsTabLayout.setVerticalGroup(
            extensionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(extensionsTabLayout.createSequentialGroup()
                .addGroup(extensionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(colorButton)
                    .addComponent(boldButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(italicButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(underlineButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(60, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Extensions", extensionsTab);

        greekTab.setLayout(new java.awt.GridLayout(3, 5));
        jTabbedPane1.addTab("Greek", greekTab);

        miscTab.setLayout(new java.awt.GridLayout(3, 5));
        jTabbedPane1.addTab("Misc", miscTab);

        jLabel1.setText("Preview:");

        jLabel2.setText("Granny Text:");

        jLabel3.setText("Press buttons from palette below to insert into text.");

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
                    .addComponent(jLabel3))
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
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 112, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
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

    private void updateImage() {
        gtr.setString( canvas.getFont(), getValue() );
        canvas.repaint();
    }

    public String getValue() {
        String text= jTextArea1.getText();
        text= text.replaceAll("\n","<br>");
        return text;
    }
    
    public void setValue( String text ) {
        jTextArea1.setText(text);
        updateImage();
    }
    
    public static void main( String[] args ) {
        GrannyTextEditor edit= new GrannyTextEditor();
        edit.setValue( "Happy !(color;Blue)Day!!");
        JOptionPane.showMessageDialog( null, edit );
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton aButton;
    private javax.swing.JButton bButton;
    private javax.swing.JButton boldButton;
    private javax.swing.JButton colorButton;
    private javax.swing.JButton exclaimationPointButton;
    private javax.swing.JPanel extensionsTab;
    private javax.swing.JPanel greekTab;
    private javax.swing.JPanel hersheyTab;
    private javax.swing.JButton italicButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JPanel miscTab;
    private javax.swing.JButton nButton;
    private javax.swing.JButton rButton;
    private javax.swing.JPanel renderPanel;
    private javax.swing.JButton sButton;
    private javax.swing.JButton underlineButton;
    // End of variables declaration//GEN-END:variables
}