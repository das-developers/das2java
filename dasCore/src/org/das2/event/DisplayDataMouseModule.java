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
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.logging.Level;
import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SemanticOps;
import org.virbo.dsutil.QDataSetTableModel;

/**
 *
 * @author jbf
 */
public class DisplayDataMouseModule extends MouseModule {

    private final static String LABEL = "Display Data";
    private final DasPlot plot;
    private JFrame myFrame;
    private JPanel myPanel;
    private JTable myEdit;
    private JComboBox comboBox;
    private JLabel messageLabel;
    private Renderer[] rends;
    private Renderer currentRenderer;
    private DatumRange xrange;
    private DatumRange yrange;

    /** Creates a new instance of DisplayDataMouseModule */
    public DisplayDataMouseModule(DasPlot parent) {
        super(parent, new BoxRenderer(parent), LABEL);
        this.plot = parent;
    }

    public static class CellTransferable implements Transferable {

        public static final DataFlavor CELL_DATA_FLAVOR = DataFlavor.stringFlavor;

        private Object cellValue;

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
      
    private class CopyAction extends AbstractAction {

        private JTable table;

        public CopyAction(JTable table) {
            this.table = table;
            putValue(NAME, "Copy");
        }

        @Override
        public void actionPerformed(ActionEvent e) {

            StringBuilder build= new StringBuilder();
            
            int[] rows= table.getSelectedRows();
            int[] cols= table.getSelectedColumns();
            if ( rows.length>1 ) {
                for ( int j=0; j<rows.length; j++ ) {
                    if ( j>0 ) build.append("\n");
                    for ( int i=0; i<cols.length; i++ ) {
                        if ( i>0 ) build.append(",");
                        build.append(table.getValueAt(rows[j], cols[i]));
                    }
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
            myPanel = new JPanel();
            myPanel.setPreferredSize(new Dimension(300, 300));
            myPanel.setLayout(new BorderLayout());
            myEdit = new JTable();
            myEdit.setFont(Font.decode("fixed-10"));
            myEdit.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
            myEdit.getTableHeader().setReorderingAllowed(false);
            myEdit.setCellSelectionEnabled(true);
            //myEdit.setRowSelectionAllowed(false);
            final JPopupMenu pm = new JPopupMenu();
            pm.add(new CopyAction(myEdit));
// See https://stackoverflow.com/questions/22622973/jtable-copy-and-paste-using-clipboard-and-abstractaction
            myEdit.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                       // highlightRow(e);
                        doPopup(e);
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                       // highlightRow(e);
                        doPopup(e);
                    }
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                       // highlightRow(e);
                        doPopup(e);
                    }
                }       

                protected void doPopup(MouseEvent e) {
                    pm.show(e.getComponent(), e.getX(), e.getY());
                }

                protected void highlightRow(MouseEvent e) {
                    JTable table = (JTable) e.getSource();
                    Point point = e.getPoint();
                    int row = table.rowAtPoint(point);
                    int col = table.columnAtPoint(point);

                    table.setRowSelectionInterval(row, row);
                    table.setColumnSelectionInterval(col, col);
                }

            });
             
            JScrollPane scrollPane = new JScrollPane( myEdit, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
            myPanel.add(scrollPane, BorderLayout.CENTER);
            
            messageLabel= new JLabel("");
            myPanel.add( messageLabel, BorderLayout.SOUTH );
            
            JPanel comboBoxArea= new JPanel();
            comboBoxArea.setLayout( new BoxLayout(comboBoxArea,BoxLayout.X_AXIS ) );
            comboBox= new JComboBox();
            comboBox.addItemListener( itemListener );
            comboBoxArea.add( new JLabel("Plotted Data:") );
            comboBoxArea.add(comboBox);
            
            myPanel.add( comboBoxArea, BorderLayout.NORTH );
            
            myFrame.getContentPane().add(myPanel);
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

        final DatumRange xrng;
        final DatumRange yrng;

        if (plot.getXAxis().isFlipped()) {
            xrng = new DatumRange(plot.getXAxis().invTransform(e.getXMaximum()), plot.getXAxis().invTransform(e.getXMinimum()));
        } else {
            xrng = new DatumRange(plot.getXAxis().invTransform(e.getXMinimum()), plot.getXAxis().invTransform(e.getXMaximum()));
        }
        if ( yclip ) {
            if (plot.getYAxis().isFlipped()) {
                yrng = new DatumRange(plot.getYAxis().invTransform(e.getYMinimum()), plot.getYAxis().invTransform(e.getYMaximum()));
            } else {
                yrng = new DatumRange(plot.getYAxis().invTransform(e.getYMaximum()), plot.getYAxis().invTransform(e.getYMinimum()));
            }
        } else {
            yrng= null;
        }

        final Renderer[] rends1 = plot.getRenderers();

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
        this.xrange= xrng;
        this.yrange= yrng;

        comboBox.setModel( new DefaultComboBoxModel( rlabels ) );
        comboBox.setSelectedIndex(icurrent);
        
        setDataSet(currentRenderer.getDataSet(),xrange,yrange);

    }

    private final ItemListener itemListener= new ItemListener() {
        public void itemStateChanged(ItemEvent e) {
            if ( rends==null ) return;
            int i= comboBox.getSelectedIndex();
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
        TableModel tm;

        if ( ds==null ) {
            showMessageInTable( myEdit, "no dataset" );
            return;
        }
        if ( ds.rank()>2 ) {
            QDataSet ds2= SemanticOps.getSimpleTableContaining( ds, xrange.min(), yrange.min() );
            if ( ds2==null ) {
                showMessageInTable( myEdit,"data cannot be displayed" );
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
                tds=SemanticOps.trim( ds, xrange, yrange );
            } else {
                tds=SemanticOps.trim( ds, xrange, null ); // this may cause problems else where...
            }
            tm= new QDataSetTableModel(tds);
            tcm= ((QDataSetTableModel)tm).getTableColumnModel();
            if ( dep1!=null && dep1.rank()==2 ) {
                myEdit.getTableHeader().setToolTipText("Column labels reported are from the first record");
            }
            if ( tds.rank()==1 ) {
                messageLabel.setText( tds.length() + " records" );
            } else {
                if ( tds.length()>0 ) {
                    int[] qube= DataSetUtil.qubeDims(tds );
                    int[] qube1= DataSetUtil.qubeDims(tds.slice(0) );
                    if ( qube!=null ) {
                        messageLabel.setText( tds.length() + " records, each is "+ DataSetUtil.toString(qube1) );
                    } else {
                        messageLabel.setText( tds.length() + " records, first is "+ DataSetUtil.toString(qube1) );
                    }
                } else {
                    messageLabel.setText( "no records" );
                }
            }
            
        } catch ( RuntimeException ex ) {
            logger.log( Level.SEVERE, ex.getMessage(), ex );
            tm= new QDataSetTableModel(ds);
            tcm= ((QDataSetTableModel)tm).getTableColumnModel();
            messageLabel.setText( ex.getMessage() );
        }
        myEdit.setModel(tm);
        myEdit.setColumnModel(tcm);
        myEdit.setRowSorter( new TableRowSorter<>(tm) );
        
        
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
