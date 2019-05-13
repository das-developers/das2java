/*
 * DisplayDataMouseModule.java
 *
 * Created on October 23, 2007, 7:08 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.das2.event;

import java.awt.event.ItemEvent;
import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import org.das2.graph.DasPlot;
import org.das2.graph.Renderer;
import java.awt.Component;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.logging.Level;
import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import org.das2.qds.DataSetUtil;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;
import org.das2.qds.util.QDataSetTableModel;

/**
 *
 * @author jbf
 */
public class DisplayDataMouseModule extends MouseModule {

    private final static String LABEL = "Display Data";
    private final DasPlot plot;
    private JFrame myFrame;
    private DisplayDataMouseModuleGUI myPanel2;
    private Renderer[] rends;
    private Renderer currentRenderer;
    private DatumRange xrange;
    private DatumRange yrange;

    /** 
     * Creates a new instance of DisplayDataMouseModule
     * @param parent 
     */
    public DisplayDataMouseModule(DasPlot parent) {
        super(parent, new BoxRenderer(parent), LABEL);
        this.plot = parent;
    }

    public static class CellTransferable implements Transferable {

        public static final DataFlavor CELL_DATA_FLAVOR = DataFlavor.stringFlavor;

        private final Object cellValue;

        public CellTransferable(Object cellValue) {
            this.cellValue = cellValue;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{CELL_DATA_FLAVOR};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return CELL_DATA_FLAVOR.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            if (!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return cellValue;
        }

    }
      
    private static final class CopyAction extends AbstractAction {

        private final JTable table;

        public CopyAction(JTable table) {
            this.table = table;
            putValue(NAME, "Copy");
        }

        @Override
        public void actionPerformed(ActionEvent e) {

            StringBuilder build= new StringBuilder();
            
            int[] rows= table.getSelectedRows();
            int[] cols= table.getSelectedColumns();
            for ( int j=0; j<rows.length; j++ ) {
                if ( j>0 ) build.append("\n");
                for ( int i=0; i<cols.length; i++ ) {
                    if ( i>0 ) build.append(",");
                    build.append(table.getValueAt(rows[j], cols[i]));
                }
            }
            
            Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
            cb.setContents(new CellTransferable( build.toString() ), null);

        }

    }
    

    private void maybeCreateFrame(Object source) {
        if (myFrame == null) {
            myFrame = new JFrame(LABEL);
            if ( source!=null && source instanceof JComponent ) {
                Window w=  SwingUtilities.getWindowAncestor((JComponent)source);
                if ( w instanceof JFrame ) {
                    myFrame.setIconImage( ((JFrame)w).getIconImage() );
                }
            }
            myPanel2= new DisplayDataMouseModuleGUI();

            final JPopupMenu pm = new JPopupMenu();
            pm.add(new CopyAction(myPanel2.getMyEdit()));
// See https://stackoverflow.com/questions/22622973/jtable-copy-and-paste-using-clipboard-and-abstractaction
            myPanel2.getMyEdit().addMouseListener(new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        doPopup(e);
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        doPopup(e);
                    }
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        doPopup(e);
                    }
                }       

                protected void doPopup(MouseEvent e) {
                    pm.show(e.getComponent(), e.getX(), e.getY());
                }

            });
            
            myPanel2.getRenderersComboBox().addItemListener(itemListener);
            myPanel2.getYClipCheckBox().setSelected(yclip);
            myPanel2.getYClipCheckBox().addActionListener( new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    DisplayDataMouseModule.this.yclip= myPanel2.getYClipCheckBox().isSelected();
                    int i= myPanel2.getRenderersComboBox().getSelectedIndex();
                    if ( i<rends.length ) setDataSet( rends[i].getDataSet(), xrange, yrange ); // thread safety
                }
            });
            
            myFrame.getContentPane().add(myPanel2);
            myFrame.pack();
            myFrame.setLocationRelativeTo(SwingUtilities.getWindowAncestor(plot));
        }
    }

    private String unitsStr(Units u) {
        return u == Units.dimensionless ? "" : "(" + u.toString() + ")";
    }

    @Override
    public void mouseRangeSelected(MouseDragEvent e0) {

       if ( !( e0 instanceof MouseBoxEvent ) ) {
            throw new IllegalArgumentException("Event should be MouseBoxEvent"); // findbugs
       }
       MouseBoxEvent e = (MouseBoxEvent) e0;
        if ( Point.distance( e.getXMaximum(), e.getYMinimum(), e.getXMaximum(), e.getYMaximum() ) < 5 ) {
            return;
        }
       
        maybeCreateFrame(e0.getSource());
        
        this.xrange= plot.getXAxis().invTransform( e.getXMaximum(), e.getXMinimum() );
        this.yrange= plot.getYAxis().invTransform( e.getYMinimum(),e.getYMaximum() );
        
        this.myPanel2.getYClipCheckBox().setText( "Show only data where Y is within "+this.yrange);
        
        final Renderer[] rends1 = plot.getRenderers();

        myPanel2.getInstructionsLabel().setText("The plot contains "+rends1.length+" renderer" + (rends1.length>1 ? "s" : "")+".");
        if ( rends1.length==0 ) return;
        myFrame.setVisible(true);

        String[] rlabels= new String[ rends1.length ];
        int firstActive= -1;
        for ( int i=0; i<rends1.length; i++ ) {
            String label= rends1[i].getLegendLabel();
            if ( label==null || label.equals("") ) {
                label= "Renderer "+i;
            }
            if ( !rends1[i].isActive() ) {
                label += " (not visible)";
            } else {
                if ( firstActive==-1 ) firstActive= i;
            }
            QDataSet ds= rends1[i].getDataSet();
            if ( ds!=null ) {
                label= label + ": "+ ds.toString();
            }
            rlabels[i]= label;
        }
        if ( firstActive==-1 ) firstActive=0;
        
        if ( currentRenderer==null ) {
            currentRenderer= rends1[firstActive];
        }
        
        int icurrent=-1;
        for ( int i=0; i<rends1.length; i++ ) {
            Renderer r= rends1[i];
            if ( currentRenderer==r ) {
                icurrent= i;
            }
        }
        
        if ( icurrent==-1 ) {
            currentRenderer= rends1[firstActive];
            icurrent= firstActive;
        }

        this.rends= rends1;

        myPanel2.getRenderersComboBox().setModel( new DefaultComboBoxModel( rlabels ) );
        myPanel2.getRenderersComboBox().setRenderer( new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                final javax.swing.JLabel label;
                label= (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if ( index>-1 && index<rends1.length ) {
                    label.setIcon(rends1[index].getListIcon());
                } else {
                    label.setIcon(null);
                }
                return label;
            }   
        });
        myPanel2.getRenderersComboBox().setSelectedIndex(icurrent);
        
        setDataSet(currentRenderer.getDataSet(),xrange,yrange);

    }

    private final ItemListener itemListener= new ItemListener() {
        @Override
        public void itemStateChanged(ItemEvent e) {
            if ( rends==null ) return;
            int i= myPanel2.getRenderersComboBox().getSelectedIndex();
            if ( i<rends.length ) setDataSet( rends[i].getDataSet(), xrange, yrange ); // thread safety
            currentRenderer= rends[i];
        }
    };

    private void showMessageInTable( final JTable t, final String message ) {
       TableModel result= new DefaultTableModel( 1, 1 ) {
            @Override
            public Object getValueAt( int row, int col ) {
                return message;
            }
        };
        t.setModel(result);

        DefaultTableColumnModel tcm = new DefaultTableColumnModel();

        TableColumn c = new TableColumn(0);
        c.setHeaderValue("");
        c.setPreferredWidth( 250 );
        tcm.addColumn(c);

        t.setColumnModel(tcm);

    }

    private void setDataSet( QDataSet ds, DatumRange xrange, DatumRange yrange ) {
        QDataSetTableModel tm;

        DatumRange yrng= isYclip() ? yrange : null;
        
        if ( ds==null ) {
            showMessageInTable( myPanel2.getMyEdit(), "no dataset" );
            return;
        }
        if ( ds.rank()>2 ) {
            QDataSet ds2= SemanticOps.getSimpleTableContaining( ds, xrange.min(), yrange.min() );
            if ( ds2==null ) {
                showMessageInTable( myPanel2.getMyEdit(),"data cannot be displayed" );
                return;
            } else {
                ds= ds2;
            }
        }
        
        TableColumnModel tcm;
        try {
            QDataSet tds;
            boolean isQube= DataSetUtil.isQube(ds);
            QDataSet dep1= (QDataSet) ds.property(QDataSet.DEPEND_1); // kludge code because isQube returns true.  It probably should return false.
            if ( dep1!=null && dep1.rank()==2 ) isQube= false;
            if ( isQube) {
                tds=SemanticOps.trim( ds, xrange, yrng );
            } else {
                tds=SemanticOps.trim( ds, xrange, null ); // this may cause problems else where...
            }
            tm= new QDataSetTableModel(tds);
            tcm= tm.getTableColumnModel();
            if ( dep1!=null && dep1.rank()==2 ) {
                myPanel2.getMyEdit().getTableHeader().setToolTipText("Column labels reported are from the first record");
            }
            if ( tds.rank()==1 ) {
                myPanel2.getMessageLabel().setText( tds.length() + " records.  Right-click to copy data to clipboard." );
            } else {
                if ( tds.length()>0 ) {
                    int[] qube= DataSetUtil.qubeDims(tds );
                    int[] qube1= DataSetUtil.qubeDims(tds.slice(0) );
                    if ( qube!=null ) {
                        myPanel2.getMessageLabel().setText( tds.length() + " records, each is "+ DataSetUtil.toString(qube1) );
                    } else {
                        myPanel2.getMessageLabel().setText( tds.length() + " records, first is "+ DataSetUtil.toString(qube1) );
                    }
                } else {
                    myPanel2.getMessageLabel().setText( "no records" );
                }
            }
            
        } catch ( RuntimeException ex ) {
            logger.log( Level.SEVERE, ex.getMessage(), ex );
            tm= new QDataSetTableModel(ds);
            tcm= tm.getTableColumnModel();
            myPanel2.getMessageLabel().setText( ex.getMessage() );
        }
        myPanel2.getMyEdit().setModel(tm);
        myPanel2.getMyEdit().setColumnModel(tcm);
        myPanel2.getMyEdit().setRowSorter( QDataSetTableModel.getRowSorter(tm) );
        
        
        //myEdit.setColumnModel(new DefaultTableColumnModel() );
        //myEdit.setColumnModel(tcm); // error with rank 1.

    }
    
    @Override
    public String getListLabel() {
        return getLabel();
    }

    @Override
    public Icon getListIcon() {
        ImageIcon icon;
        icon = new ImageIcon(this.getClass().getResource("/images/icons/showDataMouseModule.png"));
        return icon;
    }

    @Override
    public String getLabel() {
        return LABEL;
    }
    /**
     * Holds value of property yclip.
     */
    private boolean yclip = true;
    
    /**
     * Utility field used by bound properties.
     */
    private final java.beans.PropertyChangeSupport propertyChangeSupport = new java.beans.PropertyChangeSupport(this);

    /**
     * Adds a PropertyChangeListener to the listener list.
     * @param l The listener to add.
     */
    public void addPropertyChangeListener(java.beans.PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(l);
    }

    /**
     * Removes a PropertyChangeListener from the listener list.
     * @param l The listener to remove.
     */
    public void removePropertyChangeListener(java.beans.PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener(l);
    }

    /**
     * Getter for property yclip.
     * @return Value of property yclip.
     */
    public boolean isYclip() {
        return this.yclip;
    }

    /**
     * Setter for property yclip.
     * @param yclip New value of property yclip.
     */
    public void setYclip(boolean yclip) {
        boolean oldYclip = this.yclip;
        this.yclip = yclip;
        propertyChangeSupport.firePropertyChange("yclip", oldYclip, yclip);
    }
}
