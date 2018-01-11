
package org.das2.qds.filters;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.das2.datum.LoggerManager;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Dialog for picking a new filter to add.  This uses a tree to sort the filters, based on
 * http://emfisis.physics.uiowa.edu/pub/jy/filters/filters.xml
 * at U. Iowa, and keeps track of opened nodes.
 * 
 * @author jbf
 */
public class AddFilterDialog extends javax.swing.JPanel {

    private static final Logger logger= LoggerManager.getLogger("qdataset.filters");
    
    private static final Preferences prefs= Preferences.userNodeForPackage(AddFilterDialog.class);
    
    private static final String PREF_EXPANSION_STATE = "expansionState";
    private static final String PREF_TAB = "tabPreference";
    private static final String PREF_INDEX = "indexPreference";

    private String expansionState= prefs.get(PREF_EXPANSION_STATE, null );
    
    DefaultMutableTreeNode root= null;
    
    /**
     * Creates new form AddFilterDialog
     */
    public AddFilterDialog() {
        initComponents();
        this.jTree1.setModel(new DefaultTreeModel(getTree()));
        if ( expansionState==null ) {
            for (int i = 0; i < jTree1.getRowCount(); i++) {
                jTree1.expandRow(i);
            }
        } else {
            TreeUtil.restoreExpanstionState( jTree1, 0, expansionState); 
        }
        this.jTree1.setRootVisible(false);
        this.jTree1.setSelectionModel( new RestrictedTreeSelectionModel() );        
        populateList();
        this.jTabbedPane1.setSelectedIndex( prefs.getInt(PREF_TAB, 0 ) );
        int selectedIndex= prefs.getInt(PREF_INDEX, 0 );
        if ( selectedIndex<0 ) selectedIndex=0;
        if ( selectedIndex>= this.jList1.getModel().getSize() ) selectedIndex= this.jList1.getModel().getSize()-1;
        //this.jList1.setCellRenderer( getListCellRenderer() );
        this.jList1.addListSelectionListener( new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                Bookmark b= (Bookmark) jList1.getSelectedValue();
                jLabel1.setText( b.description );
                jLabel2.setText( b.filter );
                setSelectedValue( b );
            }
        } );
        this.jList1.setSelectedIndex( selectedIndex );
        this.jList1.ensureIndexIsVisible( selectedIndex );
        this.jTree1.addTreeSelectionListener( new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                TreePath tp= jTree1.getSelectionPath();
                if ( tp!=null ) {
                    Object o = tp.getLastPathComponent();
                    DefaultMutableTreeNode tn = (DefaultMutableTreeNode) o;
                    Bookmark b = (Bookmark) tn.getUserObject();
                    jLabel1.setText( b.description );
                    jLabel2.setText( b.filter );
                    setSelectedValue( b );
                }
            }
        });
                        
        Bookmark b= (Bookmark)this.jList1.getSelectedValue(); 
        ensureFolderOpen( 0, this.root, b );

    }
    
    /**
     * Thanks http://stackoverflow.com/questions/8210630/how-to-search-a-particular-node-in-jtree-and-make-that-node-expanded
     * @param root the root of the tree
     * @param s the label to search for.
     * @return the path.
     */
    private TreePath find( DefaultMutableTreeNode root, String s ) {
        @SuppressWarnings("unchecked")
        Enumeration<DefaultMutableTreeNode> e = root.depthFirstEnumeration();
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode node = e.nextElement();
            if (node.toString().equalsIgnoreCase(s)) {
                return new TreePath(node.getPath());
            }
        }
        return null;
    }    
    
    private void setSelectedValue( Bookmark selected ) {
        jList1.setSelectedValue( selected, true );
        TreePath tp=find( (DefaultMutableTreeNode)this.jTree1.getModel().getRoot(), selected.title );
        this.jTree1.getSelectionModel().setSelectionPath(tp);
        this.jTree1.scrollPathToVisible(tp);
    }
    
    /**
     * simply alphabetize the tree elements to make a list more like the old list.
     */
    private void populateList( ) {
        List<Bookmark> elements= new ArrayList<>(100);
        getElementsFromTree( elements, (DefaultMutableTreeNode)this.jTree1.getModel().getRoot() );
        Collections.sort(elements, new Comparator<Bookmark>() {
            @Override
            public int compare(Bookmark o1, Bookmark o2) {
                return o1.filter.compareTo(o2.filter);
            }
        } );
        DefaultListModel model = new DefaultListModel();
        
        Bookmark last= null;
        for( Bookmark val : elements ) {
            if ( last==null || !last.title.equals(val.title) ) {
                model.addElement(val);
                last= val;
            }
        }
        this.jList1.setModel( model );
    }
    
    private void getElementsFromTree( List<Bookmark> list, DefaultMutableTreeNode node ) {
        if ( node.isLeaf() ) {
            Object o= node.getUserObject();
            if ( o instanceof Bookmark ) {
                list.add((Bookmark)node.getUserObject());
            } else {
                logger.fine("node is a lead but shouldn't be");
            }
        }        
        for ( int i=0; i<node.getChildCount(); i++ ) {
            getElementsFromTree( list, (DefaultMutableTreeNode)node.getChildAt(i) );
        }
    }
    
    private DefaultMutableTreeNode ensureFolderOpen( int depth, DefaultMutableTreeNode node, Bookmark filter ) {
        Object uo= node.getUserObject();
        if ( uo instanceof String ) {
            return ensureFolderOpen( depth+1, (DefaultMutableTreeNode)node.getFirstChild(), filter );
        } else {
            DefaultMutableTreeNode found= null;
            Enumeration n= node.children();
            String searchFor= filter.filter;
            int i= searchFor.indexOf("(");
            searchFor= searchFor.substring(0,i);
            DefaultMutableTreeNode aleaf= null;
            while ( n.hasMoreElements() ) {
                DefaultMutableTreeNode c= (DefaultMutableTreeNode)n.nextElement();
                if ( c.isLeaf() ) {
                    if ( ( (Bookmark)c.getUserObject() ).filter.startsWith(searchFor) ) {
                        found= c;
                        break;
                    } else {
                        if ( aleaf==null ) aleaf= c;
                    }
                } else {
                    DefaultMutableTreeNode aleaf1= ensureFolderOpen( depth+1, c, filter );
                    if ( aleaf1!=null ) {
                        found= aleaf1;
                        break;
                    }
                }
            }
            if ( found==null ) {
                return null;
            } else {
                TreePath tp= new TreePath( found.getPath() );
                this.jTree1.expandPath( tp.getParentPath() );
                this.jTree1.setSelectionPath( tp );
                this.jTree1.scrollPathToVisible( tp );
                return found;
            }
        }
    }
    
    static DefaultHandler createHandler(final DefaultMutableTreeNode root) {
        final StringBuilder charsBuilder = new StringBuilder();

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
             * As elements come in, we go through the state transitions to keep track of whether we are
             * reading FIELDS, Rows of the dataset, Individual columns, etc.
             */
            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                switch (localName) {
                    case "bookmark":
                        {
                            DefaultMutableTreeNode m = new DefaultMutableTreeNode();
                            m.setUserObject(new Bookmark());
                            stack.peek().insert(m, stack.peek().getChildCount());
                            stack.push(m);
                            break;
                        }
                    case "bookmark-folder":
                        {
                            DefaultMutableTreeNode m = new DefaultMutableTreeNode();
                            m.setUserObject(new Bookmark());
                            stack.peek().insert(m, stack.peek().getChildCount());
                            stack.push(m);
                            break;
                        }
                    case "title":
                        break;
                    case "filter":
                        break;
                    case "description":
                        break;
                    case "bookmark-list":
                        break;
                    default:
                        logger.log(Level.INFO, "unrecognized tag: {0}", localName);
                        break;
                }
            }

            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException {
                switch (localName) {
                    case "bookmark":
                        stack.pop();
                        break;
                    case "bookmark-folder":
                        stack.pop();
                        break;
                    case "title":
                        ((Bookmark) (stack.peek().getUserObject())).title = charsBuilder.toString().trim();
                        break;
                    case "filter":
                        ((Bookmark) (stack.peek().getUserObject())).filter = charsBuilder.toString().trim();
                        break;
                    case "description":
                        ((Bookmark) (stack.peek().getUserObject())).description = charsBuilder.toString().trim();
                        break;
                    case "bookmark-list":
                        break;
                    default:
                        logger.log(Level.INFO, "unrecognized tag: {0}", localName);
                        break;
                        
                }
                charsBuilder.delete(0, charsBuilder.length());
            }

            @Override
            public void characters(char[] ch, int start, int length) throws SAXException {
                charsBuilder.append(ch, start, length);
            }
        };
    }

    private static class RestrictedTreeSelectionModel extends DefaultTreeSelectionModel {

        @Override
        public void setSelectionPaths(final TreePath[] pPaths) {
            final ArrayList<TreePath> temp = new ArrayList<>();
            for ( int i = 0, n = pPaths != null ? pPaths.length : 0; i < n; i++ ) {
                final Object lastPathComponent= pPaths[i].getLastPathComponent();
                if (lastPathComponent instanceof TreeNode) {
                    if (((TreeNode) lastPathComponent).isLeaf()) {
                        temp.add(pPaths[i]);
                    }
                }
            }
            if (!temp.isEmpty()) {
                super.setSelectionPaths( temp.toArray(new TreePath[temp.size()]) );
            }
        }
    }

    /**
     * return the tree of filters in the file.
     * @param in
     * @return 
     */
    public static DefaultMutableTreeNode build( InputStream in ) {
        DefaultMutableTreeNode result = new DefaultMutableTreeNode("root");
        
        DefaultHandler sax = createHandler(result);
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            SAXParser saxParser = spf.newSAXParser();
            XMLReader xmlReader = saxParser.getXMLReader();

            xmlReader.setContentHandler(sax);

            try {
                logger.fine("done parsing filters.xml");
                xmlReader.parse(new InputSource(in));

            } catch (RuntimeException ex) {
                logger.log(Level.SEVERE, null, ex );
                throw ex;
                
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        } catch (ParserConfigurationException | SAXException ex) {
            logger.log(Level.SEVERE, null, ex);
            // this is expected.
        } finally {
            try {
                in.close();
            } catch ( IOException ex ) {
                logger.log( Level.WARNING, ex.getMessage(), ex );
            }
        }
        return result;
    }

    public static class Bookmark {
        String title="";
        String filter="";
        String description="";
        @Override
        public String toString() {
            return title;
        }
    }

    private TreeNode getTree() {
        InputStream in = AddFilterDialog.class.getResourceAsStream("filters.xml");
        DefaultMutableTreeNode result= build( in );
        root= result;
        return result;
    }

    /**
     * return the selected filter, such as "|smooth(5)"
     *
     * @return the selected filter.
     */
    public String getValue() {
        Bookmark result;
        int tabPreference;
        if ( jTabbedPane1.getSelectedIndex()==0 ) {
            Object o = this.jTree1.getSelectionPath().getLastPathComponent();
            DefaultMutableTreeNode tn = (DefaultMutableTreeNode) o;
            Bookmark b = (Bookmark) tn.getUserObject();
            expansionState= TreeUtil.getExpansionState( jTree1, 0 );
            tabPreference= 0;
            result= b;
        } else {
            Bookmark b= (Bookmark)jList1.getSelectedValue();
            expansionState= TreeUtil.getExpansionState( jTree1, 0 );
            tabPreference= 1;
            result= b;
        }
        prefs.put( PREF_EXPANSION_STATE, expansionState );
        prefs.putInt(PREF_TAB, tabPreference );
        prefs.putInt( PREF_INDEX, jList1.getSelectedIndex() );
        
        return result.filter;
    }

    public static void main(String[] args) {
        AddFilterDialog afd = new AddFilterDialog();
        if (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(null, afd)) {
            System.err.println(afd.getValue());
        }
        afd = new AddFilterDialog();
        if (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(null, afd)) {
            System.err.println(afd.getValue());
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this
     * code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTree1 = new javax.swing.JTree();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jList1 = new javax.swing.JList();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();

        jScrollPane1.setViewportView(jTree1);

        jTabbedPane1.addTab("By Category", jScrollPane1);

        jList1.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        jScrollPane2.setViewportView(jList1);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 392, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 204, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("Alphabetical", jPanel1);

        jLabel1.setFont(jLabel1.getFont().deriveFont(jLabel1.getFont().getSize()-2f));

        jLabel2.setFont(jLabel2.getFont().deriveFont(jLabel2.getFont().getSize()-2f));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1)
            .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 247, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 12, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {jLabel1, jLabel2});

    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JList jList1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTree jTree1;
    // End of variables declaration//GEN-END:variables
}
