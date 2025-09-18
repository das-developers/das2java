
package org.das2.util;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.das2.util.awt.PdfGraphicsOutput;

/**
 * Redo the font selector, which was an odd JComponent I found on-line years
 * ago, which isn't difficult to implement.  This code may be used freely.
 * @author jbf
 */
public class FontChooser extends javax.swing.JPanel {

    private List<String> sizes = Arrays.asList( new String[]{"2", "4", "6", "8", "10", "12", "14", "16", "18", "20", "22", "24", "30", "36", "48", "72"} );
    
    /**
     * Creates new form FontChooser
     */
    public FontChooser() {
        initComponents();
        DefaultListModel model= new DefaultListModel();
        for (String item : sizes) {
            model.addElement(item);
        }
        sizesList.setModel( model );
        sizesList.setSelectedIndex(7);
        resetFonts();
        fontList.addListSelectionListener( new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent lse) {
                sampleTextArea.setFont(getCurrentFont());
                if (fontCheck != null) {
                    updateFontCheck(getCurrentFont());
                }
//                if ( Entities.fontSupports(getCurrentFont(), txtSample.getText() ) ) {
//                    cbBold.setForeground(Color.green);
//                } else {
//                    cbBold.setForeground(Color.red);
//                }                
               }
            
        });
    }
    
    

    private void resetFonts() {
        String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        DefaultListModel model= new DefaultListModel();
        for (String item : fonts) {
            model.addElement(item);
        }
        fontList.setModel(model);
        fontList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }
    
    public void setExampleText(String text) {
        this.sampleTextArea.setText(text);
    }
    
    public String getExampleText() {
        return this.sampleTextArea.getText();
    }
    
    public FontCheck defaultFontCheck= new FontCheck() {
        @Override
        public String checkFont(Font f) {
            Object font= PdfGraphicsOutput.ttfFromNameInteractive(f);
            StringBuilder msg= new StringBuilder();
            if ( font==PdfGraphicsOutput.READING_FONTS ) {
                msg.append("Checking which fonts are embeddable...");
            } else if ( font!=null ) {
                msg.append("PDF okay");
            } else {                    
                msg.append("Cannot be embedded in PDF");
            }
            char missingCharacter=0;
            Font t= getFont();
            if ( t!=null ) {
                String text= getExampleText();
                for ( int i=0; missingCharacter==0 && i<text.length(); i++ ) {
                    char c= text.charAt(i);
                    if ( c!=10 ) {
                        if ( !t.canDisplay(c) ) {
                            missingCharacter= c;
                        }
                    }
                }
            }
            if ( missingCharacter!=0 ) 
                msg.append(". Missing ")
                        .append(missingCharacter)
                        .append(" 0x")
                        .append(Integer.toHexString(missingCharacter))
                        .append(".");
            return msg.toString();
        }
    };

    private FontCheck fontCheck = defaultFontCheck;

    private void updateFontCheck( final Font font ) {
        String s= fontCheck==null ? null : fontCheck.checkFont(font);
        if ( s==null ) {
            Timer t= new Timer(500,new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent e) {
                    updateFontCheck(getCurrentFont());
                }
            });
            t.setRepeats(false);
            t.start();
            if ( messageLabel!=null ) messageLabel.setText("");
        } else {
            if ( messageLabel!=null ) messageLabel.setText(s);
        }
    }

    public Font getCurrentFont() {
        String fontFamily = (String) fontList.getSelectedValue();
        int fontSize = Integer.parseInt((String) sizesList.getSelectedValue());

        int fontType = Font.PLAIN;

        if (boldCheckBox.isSelected()) {
            fontType += Font.BOLD;
        }
        if (italicCheckBox.isSelected()) {
            fontType += Font.ITALIC;
        }

        return new Font(fontFamily, fontType, fontSize);
    }
    
    /**
     * round up to the nearest available size.  Code from JFontChooser.
     * @param size
     * @return 
     */
    private int roundFontSize( int size ) {
        if ( size<=24 ) {
            size= ( size + 1 ) / 2 * 2 ;
            return size;
        } else {
            for ( int i=0; i<sizesList.getModel().getSize(); i++ ) {
                int ii= Integer.parseInt(sizesList.getModel().getElementAt(i));
                if ( ii>=size ) {
                    return ii;
                }
            }
            return 72;
        }
    }
    
    public void setCurrentFont( Font f ) {
        boldCheckBox.setSelected( f.isBold() );
        italicCheckBox.setSelected( f.isItalic() );
        sizesList.setSelectedValue( roundFontSize(f.getSize()), true);
        fontList.setSelectedValue( f.getName(), true);        
    }
    
    public interface FontCheck {
        String checkFont(Font c);
    }

    /**
     * allows an arbitrary string to be indicated for any font.  For example,
     * in Autoplot, we look to see if the font can be embedded.
     * @param c 
     */
    public void setFontCheck(FontCheck c) {
        this.fontCheck = c;
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        fontList = new javax.swing.JList<>();
        jScrollPane2 = new javax.swing.JScrollPane();
        sizesList = new javax.swing.JList<>();
        boldCheckBox = new javax.swing.JCheckBox();
        italicCheckBox = new javax.swing.JCheckBox();
        jScrollPane3 = new javax.swing.JScrollPane();
        sampleTextArea = new javax.swing.JTextArea();
        messageLabel = new javax.swing.JLabel();

        fontList.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        fontList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(fontList);

        sizesList.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        sizesList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane2.setViewportView(sizesList);

        boldCheckBox.setText("Bold");

        italicCheckBox.setText("Italic");

        sampleTextArea.setColumns(20);
        sampleTextArea.setRows(5);
        jScrollPane3.setViewportView(sampleTextArea);

        messageLabel.setText("jLabel1");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane3)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 262, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(boldCheckBox)
                    .addComponent(italicCheckBox))
                .addGap(0, 15, Short.MAX_VALUE))
            .addComponent(messageLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 204, Short.MAX_VALUE)
                        .addComponent(jScrollPane1))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(boldCheckBox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(italicCheckBox)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(messageLabel))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox boldCheckBox;
    private javax.swing.JList<String> fontList;
    private javax.swing.JCheckBox italicCheckBox;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JLabel messageLabel;
    private javax.swing.JTextArea sampleTextArea;
    private javax.swing.JList<String> sizesList;
    // End of variables declaration//GEN-END:variables
}
