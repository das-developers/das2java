/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.filters;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author jbf
 */
public class AddFilterDialog extends javax.swing.JPanel {

    /**
     * Creates new form AddFilterDialog
     */
    public AddFilterDialog() {
        initComponents();
        this.jTree1.setModel( new DefaultTreeModel( getTree() ) );
    }

    DefaultHandler createHandler( final DefaultMutableTreeNode root ) {
        final StringBuilder charsBuilder= new StringBuilder();
        
        final Deque<DefaultMutableTreeNode> stack = new ArrayDeque();
        
        stack.push(root);
        
        return new DefaultHandler() {

            /**
             * initialize the state to STATE_OPEN.
             */
            @Override
            public void startDocument() throws SAXException {
            }

            /**
             * As elements come in, we go through the state transitions to keep track of
             * whether we are reading FIELDS, Rows of the dataset, Individual columns, etc.
             */
            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                if ( localName.equals("bookmark") ) {
                    DefaultMutableTreeNode m= new DefaultMutableTreeNode();
                    m.setUserObject( new Bookmark() );
                    stack.peek().insert( m, stack.peek().getChildCount() );
                    stack.push(m);
                } else if ( localName.equals("bookmark-folder") ) {
                    DefaultMutableTreeNode m= new DefaultMutableTreeNode();
                    m.setUserObject( new Bookmark() );
                    stack.peek().insert( m, stack.peek().getChildCount() );
                    stack.push(m);
                } else if ( localName.equals("title") ) {
                    
                } else if ( localName.equals("filter") ) {
                    
                } else if ( localName.equals("description") ) {
                    
                }
            }

            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException {
                if ( localName.equals("bookmark") ) {
                    stack.pop();
                } else if ( localName.equals("bookmark-folder") ) {
                    stack.pop();
                } else if ( localName.equals("title") ) {
                    ((Bookmark)(stack.peek().getUserObject())).title= charsBuilder.toString();
                } else if ( localName.equals("filter") ) {
                    ((Bookmark)(stack.peek().getUserObject())).filter= charsBuilder.toString();
                } else if ( localName.equals("description") ) {
                    ((Bookmark)(stack.peek().getUserObject())).description= charsBuilder.toString();
                }
                charsBuilder.delete(0,charsBuilder.length());
            }


            @Override
            public void characters(char[] ch, int start, int length) throws SAXException {
                charsBuilder.append( ch, start, length );
            }
        };
    }

    private DefaultMutableTreeNode build( InputStream in )  {
        DefaultMutableTreeNode result= new DefaultMutableTreeNode("ff");
        DefaultHandler sax= createHandler(result);
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            SAXParser saxParser = spf.newSAXParser();
            XMLReader xmlReader = saxParser.getXMLReader();
            
            xmlReader.setContentHandler(sax);
            
            try {
                xmlReader.parse( new InputSource(in) );
                in.close();
                
            } catch ( RuntimeException ex ) {
                // this is expected.
            } catch (IOException ex) {
                Logger.getLogger(AddFilterDialog.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch ( ParserConfigurationException ex ) {       
            Logger.getLogger(AddFilterDialog.class.getName()).log(Level.SEVERE, null, ex);
            // this is expected.
        } catch (SAXException ex) {
            Logger.getLogger(AddFilterDialog.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }
    
    private static class Bookmark {
        String title;
        String filter;
        String description;
        public String toString() {
            return title;
        }
    }

    private TreeNode getTree() {
        return build( AddFilterDialog.class.getResourceAsStream("filters.xml") );                                    
    }
    
    /**
     * return the selected filter.
     * @return 
     */
    public String getValue() {
        Object o= this.jTree1.getSelectionPath().getLastPathComponent();
        DefaultMutableTreeNode tn= (DefaultMutableTreeNode)o;
        Bookmark b= (Bookmark)tn.getUserObject();
        return b.filter;
    }
    
    public static void main( String[] args ) {
        AddFilterDialog afd= new AddFilterDialog();
        if ( JOptionPane.OK_OPTION== JOptionPane.showConfirmDialog( null, afd ) ) {
            System.err.println(afd.getValue());
        }
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
        jTree1 = new javax.swing.JTree();

        jTree1.setRootVisible(false);
        jScrollPane1.setViewportView(jTree1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTree jTree1;
    // End of variables declaration//GEN-END:variables
}
