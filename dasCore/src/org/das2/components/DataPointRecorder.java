/*
 * DataPointRecorder.java
 *
 * Created on October 6, 2003, 12:16 PM
 */
package org.das2.components;

import java.util.Arrays;
import org.das2.event.CommentDataPointSelectionEvent;
import org.das2.event.DataPointSelectionListener;
import org.das2.event.DataPointSelectionEvent;
import org.das2.dataset.DataSetUpdateEvent;
import org.das2.dataset.VectorDataSetBuilder;
import org.das2.dataset.DataSetDescriptor;
import org.das2.dataset.DataSet;
import org.das2.dataset.VectorDataSet;
import org.das2.dataset.DataSetUpdateListener;
import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import org.das2.datum.Datum;
import org.das2.datum.DatumUtil;
import org.das2.datum.TimeUtil;
import org.das2.DasException;
import org.das2.util.monitor.NullProgressMonitor;
import org.das2.util.monitor.ProgressMonitor;
import org.das2.components.propertyeditor.PropertyEditor;
import org.das2.datum.format.DatumFormatter;
import org.das2.system.DasLogger;
import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import org.das2.DasApplication;
import org.das2.dataset.DataSetAdapter;
import org.das2.datum.DatumRangeUtil;
import org.das2.datum.EnumerationUnits;
import org.das2.datum.TimeLocationUnits;
import org.das2.datum.TimeParser;
import org.das2.datum.UnitsUtil;
import org.das2.qds.DataSetOps;
import org.das2.qds.DataSetUtil;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;
import org.das2.qds.ops.Ops;
import org.das2.qds.util.DataSetBuilder;

/**
 * DataPointRecorder is a GUI for storing data points selected by the user.  
 * The columns are set dynamically as the data arrives (via addDataPoint).
 * 
 * This replaces DataPointRecorderNew.  (DataPointRecorderNew replaced an older 
 * DataPointerRecorder whose name has been reclaimed.)
 * 
 * @author  jbf
 */
public class DataPointRecorder extends JPanel implements DataPointSelectionListener {

    /**
     * width of time column
     */
    private static final int TIME_WIDTH = 180;

    protected JTable table;
    protected JScrollPane scrollPane;
    protected JButton updateButton;
    final protected List dataPoints;
    
    /**
     * the row, of the data model, that needs to be selected after the update.  
     * Note with row sorting this may not be the same as the view row.
     */
    private int selectRow; 
    
    /**
     * units[index]==null if HashMap contains non-datum object.
     */
    protected Units[] unitsArray;
    
    /**
     * array of plane names that are also the column headers. planesArray[0]="x", planesArray[1]="y"
     */
    protected String[] planesArray;
    
    protected AbstractTableModel myTableModel;
    private File saveFile;
    private boolean modified;
    private JLabel messageLabel;
    private boolean active = true; // false means don't fire updates
    Preferences prefs = Preferences.userNodeForPackage(this.getClass());
    private static final Logger logger = DasLogger.getLogger(DasLogger.GUI_LOG);
    private final JButton clearSelectionButton;
    private final JButton deleteSelectionButton;
    private final JPanel accessoryPanel;
    
    /**
     * Note this is all pre-QDataSet.  QDataSet would be a much better way of implementing this.
     */
    public static class DataPoint implements Comparable {

        Datum[] data;
        private Map<String,Object> planes;

        public DataPoint(Datum x1, Datum x2, Map planes) {
            this(new Datum[]{x1, x2}, planes);
        }

        public DataPoint(Datum[] data, Map planes) {
            this.data = data;
            this.planes = planes;
        }

        /**
         * get the x or y Datum. 0 gets X, 1 gets Y.
         * TODO: redo this!
         */
        Datum get(int i) {
            return data[i];
        }

        /** 
         * get the Datum from the planes.  
         */
        Object getPlane(String name) {
            return planes.get(name.trim());
        }

        /**
         * When times are the independent parameter, we have to add a 
         * little fuzz because of rounding errors.
         * @param o
         * @return 
         */
        @Override
        public int compareTo(Object o) {
            DataPoint that = (DataPoint) o;
            Datum myt= this.data[0];
            Datum xt= that.data[0].convertTo( myt.getUnits() );
            Datum diff= myt.subtract(xt);
            if ( myt.getUnits() instanceof TimeLocationUnits ) {
                double micros= diff.doubleValue(Units.microseconds);
                if ( micros<-100 ) {
                    return -1;
                } else if ( micros>100 ) {
                    return 1;
                } else {
                    return 0;
                }
            } else {
                Units u= myt.getUnits().getOffsetUnits();
                double delta= diff.doubleValue(u);
                if ( delta < 0 ) {
                    return -1;
                } else if ( delta > 0 ) {
                    return 1;
                } else {
                    return 0;
                }
            }
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 97 * hash + Arrays.deepHashCode(this.data);
            hash = 97 * hash + (this.planes != null ? this.planes.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals( Object o ) {
            if ( !( o instanceof DataPoint ) ) return false;
            return compareTo(o)==0;
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder("" + data[0] + " " + data[1]);
            if (planes != null) {
                for ( Entry<String,Object> entry: planes.entrySet() ) {
                    result.append(" ").append(entry.getValue());
                }
            }
            return result.toString();
        }
    }

    private final Object planesArrayLock;
    
    private class MyTableModel extends AbstractTableModel {
        @Override
        public int getColumnCount() {
            //TODO: System.err.println("===> " + Thread.currentThread().getName());
            synchronized (planesArrayLock) {
                if (unitsArray == null) {
                    return 2;
                } else {
                    return planesArray.length;
                }
            }
        }

        @Override
        public String getColumnName(int j) {
            //System.err.println("===> " + Thread.currentThread().getName());
            synchronized (planesArrayLock) {
                String result = planesArray[j];
                if (unitsArray[j] != null) {
                    if ( unitsArray[j] instanceof EnumerationUnits ) {
                        result += "(ordinal)";
                    } else if ( UnitsUtil.isTimeLocation( unitsArray[j] ) ) {
                        result+= "(UTC)";
                    } else if ( unitsArray[j]!=Units.dimensionless ) {
                        result += "(" + unitsArray[j] + ")";
                    }
                }
                return result;
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return Datum.class;
        }

        
        @Override
        public int getRowCount() {
            int nrow = dataPoints.size();
            return nrow;
        }

        
        @Override
        public Object getValueAt(int i, int j) {
            DataPoint x;
            synchronized (dataPoints) {
                if ( i>=dataPoints.size() ) {
                    i= dataPoints.size()-1;
                }
                x = (DataPoint) dataPoints.get(i);
            }
            if (j < x.data.length) {
                Datum d = x.get(j);
                if ( j==0 && timeFormatter!=null && UnitsUtil.isTimeLocation( d.getUnits() ) ) {
                    return timeFormatter.format(d);
                } else {
                    if ( d.isFill() ) {
                        return "fill";
                    } else {
                        return d.getFormatter().format(d, unitsArray[j]);
                    }
                }
            } else {
                Object o = x.getPlane(planesArray[j]);
                if (o instanceof Datum) {
                    Datum d = (Datum) o;
                    if ( d.isFill() ) {
                        return "fill";
                    } else {
                        return d.getFormatter().format(d, unitsArray[j]);
                    }
                } else {
                    return (String) o;
                }
            }            
        }
    }

    /** 
     * delete all the points within the interval.  This was introduced to support the
     * case where we are going to reprocess an interval, as with the  
     * RBSP digitizer.
     * 
     * @param range range to delete, end time is exclusive.
     */
    public void deleteInterval( DatumRange range ) {
        if ( !sorted ) {
            throw new IllegalArgumentException("data must be sorted");
        } else {
            synchronized ( dataPoints ) {
                Comparator comp= new Comparator() {
                    @Override
                    public int compare(Object o1, Object o2) {
                        return ((DataPoint)o1).get(0).compareTo((Datum)o2);
                    }
                };
                int index1= Collections.binarySearch( dataPoints, range.min(), comp );
                if ( index1<0 ) index1= ~index1;
                int index2= Collections.binarySearch( dataPoints, range.max(), comp );
                if ( index2<0 ) index2= ~index2;
                if ( index1==index2 ) return;
                int[] arr= new int[ index2-index1 ];
                for ( int i=0; i<arr.length ; i++ ) arr[i]= index1+i;
                deleteRows( arr );
            }
        }
    }
    
    /**
     * delete the specified row.
     * @param row the row, where zero is the first element.
     */
    public void deleteRow(int row) {
        synchronized (dataPoints) {
            dataPoints.remove(row);
            modified = true;
            updateClients();
            updateStatus();
        }
        if ( active ) {
            fireDataSetUpdateListenerDataSetUpdated(new DataSetUpdateEvent(this));
        }
        myTableModel.fireTableDataChanged();
    }
    
    /**
     * delete the specified rows.
     * @param selectedRows the rows, where zero is the first element..
     */
    public void deleteRows(int[] selectedRows) {
        synchronized ( dataPoints ) {
            for ( int i = selectedRows.length-1; i>=0; i-- ) {
                int j= selectedRows[i];
                if ( j>=dataPoints.size() ) {
                    j= dataPoints.size()-1;
                    logger.fine("heres a bug to fix, having to do with synchronization.");
                }
                dataPoints.remove(j);
            }
            logger.log(Level.FINER, "dataPoints.size()={0}", dataPoints.size());            
            modified = true;
        }
        updateClients();
        updateStatus();
        if ( active ) {
            fireDataSetUpdateListenerDataSetUpdated(new DataSetUpdateEvent(this));
        }
        myTableModel.fireTableDataChanged();
    }
    

    private class MyDataSetDescriptor extends DataSetDescriptor {

        MyDataSetDescriptor() {
            super(null);
        }

        public void fireUpdate() {
            fireDataSetUpdateEvent(new DataSetUpdateEvent((Object) this));
        }

        @Override
        protected DataSet getDataSetImpl(Datum s1, Datum s2, Datum s3, ProgressMonitor monitor) throws DasException {
            synchronized ( dataPoints ) {
                if (dataPoints.isEmpty()) {
                    return null;
                } else {
                    VectorDataSetBuilder builder = new VectorDataSetBuilder(unitsArray[0], unitsArray[1]);
                    for (int irow = 0; irow < dataPoints.size(); irow++) {
                        DataPoint dp = (DataPoint) dataPoints.get(irow);
                        builder.insertY(dp.get(0), dp.get(1));
                    }
                    return builder.toVectorDataSet();
                }
            }
        }

        @Override
        public Units getXUnits() {
            return unitsArray[0];
        }
    }
    private MyDataSetDescriptor dataSetDescriptor;

    /**
     * returns a data set of the table data.  This will be 
     * ds[n,m] with n rows and m columns, where m is the number of columns
     * minus one, and with DEPEND_0 containing the X values.
     * @see #getBundleDataSet() 
     * @return  a data set of the table data. 
     * @deprecated see #getDataPoints
     */
    public QDataSet getDataSet( ) {
        if ( unitsArray[0]==null ) return null;
        QDataSet bds= getBundleDataSet();
        QDataSet xds= Ops.unbundle(bds,0);
        if ( xTagWidth != null && xTagWidth.value()>0 && !xTagWidth.isFill() ) {
            xds= Ops.putProperty( xds, QDataSet.CADENCE, DataSetUtil.asDataSet(xTagWidth) );
        }
        if ( bds.length(0)==2 ) {
            return Ops.link( xds, Ops.unbundle(bds,1) );
        } else {
            QDataSet ds= Ops.copy( Ops.trim1( bds, 1, bds.length(0) ) );
            return Ops.link( xds, ds  );
        }
    }
    
    /**
     * returns a entire set of data points as a rank 2 bundle.
     * This is the same as getBundleDataSet.
     * @return 
     */
    public QDataSet getDataPoints() {
        return getBundleDataSet();
    }
    
    /**
     * return the subset of the data points which are selected, as a rank 2 bundle.
     * @return 
     */
    public QDataSet getSelectedDataPoints() {
        if ( unitsArray[0]==null ) return null;
        DataSetBuilder builder= new DataSetBuilder( 2, dataPoints.size(), planesArray.length );
        builder.setName( 0, "x" );
        builder.setName( 1, "y" );
        builder.setFillValue( -1e31 );
        for ( int i=2; i<planesArray.length; i++ ) {
            builder.setName( i, planesArray[i] );
        }
        int[] selectedRows = getSelectedRowsInModel();        
        for ( int isrow = 0; isrow < selectedRows.length; isrow++) {
            int irow= selectedRows[isrow];
            DataPoint dp = (DataPoint) dataPoints.get(irow);
            builder.putValue( -1, 0, dp.get(0) );
            builder.putValue( -1, 1, dp.get(1) );
            for ( int i=2; i<planesArray.length; i++ ) {
                if ( unitsArray[i] instanceof EnumerationUnits ) {
                    EnumerationUnits eu= (EnumerationUnits)unitsArray[i];
                    if ( eu==unitsArray[i] ) {
                        builder.putValue( -1, i, eu.createDatum(((Datum)dp.getPlane(planesArray[i])).toString()) );
                    } else {
                        builder.putValue( -1, i, (Datum)dp.getPlane(planesArray[i]) );
                    }
                } else {
                    builder.putValue( -1, i, (Datum)dp.getPlane(planesArray[i] ) );
                }
            }
            builder.nextRecord();
        }
        return builder.getDataSet();
    }
    
    /**
     * return ds[n,m] with n rows and m columns, where ds[:,0] is the x, ds[:,1] 
     * is the y, and ds[:,2:] are the additional data.
     * @return a rank 2 dataset of the table data.
     */
    public QDataSet getBundleDataSet() {
        if ( unitsArray[0]==null ) return null;
        DataSetBuilder builder= new DataSetBuilder( 2, dataPoints.size(), planesArray.length );
        builder.setName( 0, "x" );
        builder.setName( 1, "y" );
        builder.setFillValue( -1e31 );
        for ( int i=2; i<planesArray.length; i++ ) {
            builder.setName( i, planesArray[i] );
        }
        for ( int irow = 0; irow < dataPoints.size(); irow++) {
            DataPoint dp = (DataPoint) dataPoints.get(irow);
            builder.putValue( -1, 0, dp.get(0) );
            builder.putValue( -1, 1, dp.get(1) );
            for ( int i=2; i<planesArray.length; i++ ) {
                builder.putValue( -1, i, (Datum)dp.getPlane(planesArray[i] ) );
            }
            builder.nextRecord();
        }
        return builder.getDataSet();
    }
    
    /**
     * returns a data set of the selected table data.  Warning: this used to
     * return a bundle dataset with Y,plane1,plane2,etc that had DEPEND_0 for X.
     * This now returns a bundle ds[n,m] where m is the number of columns and
     * n is the number of records.
     * @return a data set of the selected table data.
     * @see #select(org.das2.datum.DatumRange, org.das2.datum.DatumRange) which selects part of the dataset.
     */
    public synchronized QDataSet getSelectedDataSet() {
        int[] selectedRows = getSelectedRowsInModel();
        
        if (selectedRows.length == 0) {
            return null;
        } else {
            VectorDataSetBuilder builder = new VectorDataSetBuilder(unitsArray[0], unitsArray[1]);
            synchronized (dataPoints) {
                for (int j = 2; j < planesArray.length; j++) {
                    builder.addPlane(planesArray[j], unitsArray[j]);
                }
                for (int i = 0; i < selectedRows.length; i++) {
                    int irow = selectedRows[i];
                    if ( irow<dataPoints.size() ) {
                        DataPoint dp = (DataPoint) dataPoints.get(irow);
                        builder.insertY(dp.get(0), dp.get(1));
                        for (int j = 2; j < planesArray.length; j++) {
                            builder.insertY(dp.get(0).doubleValue(unitsArray[0]),
                                ((Datum) dp.getPlane(planesArray[j])).doubleValue(unitsArray[j]),
                                planesArray[j]);
                        }
                    }
                }
                if ( xTagWidth != null && xTagWidth.value()>0 && !xTagWidth.isFill() ) {
                    builder.setProperty("xTagWidth", xTagWidth);
                }
            }
            return DataSetAdapter.create( builder.toVectorDataSet() );
        }
    }

    /**
     * Selects all the points in the GUI where the first column is within xrange and
     * the second column is within yrange.  Returns the first of the selected 
     * indices, or -1 if no elements are found.
     * @param xrange the range constraint (non-null).
     * @param yrange the range constraint  or null if no constraint.
     * @return the first of the selected indices, or -1 if no elements are found.
     * @see #getSelectedDataSet() 
     */
    public int select( DatumRange xrange, DatumRange yrange ) {
        return select(xrange,yrange,false);
    }
    
    /**
     * Selects all the points in the GUI where the first column is within xrange and
     * the second column is within yrange.  Returns the first of the selected 
     * indices, or -1 if no elements are found.
     * @param xrange the x range
     * @param yrange the y range or null if no constraint
     * @param xOrY if true, then match if either yrange or xrange contains.
     * @return the first of the selected indices, or -1 if no elements are found.
     * @see #getSelectedDataSet() 
     */
    public int select(DatumRange xrange, DatumRange yrange, boolean xOrY ) {
        if ( xOrY && yrange==null ) throw new IllegalArgumentException("yrange is null with or condition--this would select all points.");
        Datum mid= xrange.middle();
        synchronized (dataPoints) {
            List<Integer> selectMe = new ArrayList();
            int iclosest= -1;
            Datum closestDist=null;
            for (int i = 0; i < dataPoints.size(); i++) {
                DataPoint p = (DataPoint) dataPoints.get(i);
                if ( xOrY ) {
                    assert yrange!=null;
                    if ( xrange.contains(p.data[0]) || ( yrange.contains(p.data[1]) ) ) {
                        selectMe.add( i );
                    }
                } else {
                    if ( xrange.contains(p.data[0]) && ( yrange==null || yrange.contains(p.data[1]) ) ) {
                        selectMe.add( i );
                    }
                }
                if ( closestDist==null || p.data[0].subtract(mid).abs().lt( closestDist ) ) {
                    iclosest= i;
                    closestDist= p.data[0].subtract(mid).abs();
                }
            }
            if ( iclosest!=-1 && selectMe.isEmpty() ) {
                assert closestDist!=null;
                if ( closestDist.gt( xrange.width() ) ) {
                    return -1;
                } else {
                    if ( sorted ) {
                        selectMe= Collections.singletonList(iclosest);
                    } else {
                        return -1;
                    }
                }
            }

            for ( Integer selectMe1 : selectMe ) {
                int iselect = selectMe1;
                table.getSelectionModel().addSelectionInterval(iselect, iselect);
            }

            final int fiselect;
            if ( selectMe.size()>0 ) {
                fiselect= selectMe.get(0);
            } else {
                fiselect= -1;
            }
            
            final List<Integer> fselectMe= selectMe;
                
            Runnable run= new Runnable() {
                public void run() {
                    showSelection( fselectMe );
                }
            };
            SwingUtilities.invokeLater(run);
            return fiselect;
        }
        
    }

    private int showSelection( List<Integer> selectMe ) {
        table.getSelectionModel().clearSelection();
        
        for ( Integer selectMe1 : selectMe ) {
            table.getSelectionModel().addSelectionInterval(selectMe1, selectMe1);
        }

        if ( selectMe.size()>0 ) {
            int iselect= selectMe.get(0);
            table.scrollRectToVisible(new Rectangle(table.getCellRect( iselect, 0, true)) );
            return iselect;
        } else {
            return -1;
        }        
    }
    
    public void saveToFile(File file) throws IOException {
        List<DataPoint> dataPoints1;
        synchronized (this.dataPoints) {
            dataPoints1= new ArrayList(this.dataPoints);
        }
        FileOutputStream out = new FileOutputStream(file);
        BufferedWriter r = new BufferedWriter(new OutputStreamWriter(out));

        try {
            StringBuilder header = new StringBuilder();
            //header.append("## "); // don't use comment characters so that labels and units are used in Autoplot's ascii parser.
            for (int j = 0; j < planesArray.length; j++) {
                String s= myTableModel.getColumnName(j);
                if ( !s.endsWith(")") ) {
                    s= s+"()"; // backward compatibility
                }
                header.append(s).append("\t");
            }
            r.write(header.toString());
            r.newLine();
            
            if ( dataPoints1.size()>0 ) {
                TimeParser ltimeFormatter;
                if ( getTimeFormat().length()>0 ) {
                    ltimeFormatter= TimeParser.create(timeFormat);
                } else {
                    ltimeFormatter = null;
                }
                for (int i = 0; i < dataPoints1.size(); i++) {
                    DataPoint x = (DataPoint) dataPoints1.get(i);
                    if ( ltimeFormatter!=null && !UnitsUtil.isTimeLocation( x.get(0).getUnits() ) ) {
                        ltimeFormatter= null;
                    }
                    StringBuilder s = new StringBuilder();
                    for (int j = 0; j < 2; j++) {
                        if ( j==0 && ltimeFormatter!=null ) { //TODO: this should be done by units.
                            s.append( ltimeFormatter.format( x.get(j) ) ).append("\t");
                        } else {
                            DatumFormatter formatter = x.get(j).getFormatter();
                            s.append(formatter.format(x.get(j), unitsArray[j])).append("\t");
                        }
                    }
                    for (int j = 2; j < planesArray.length; j++) {
                        Object o = x.getPlane(planesArray[j]);
                        if ( o==null ) {
                            //x.getPlane(planesArray[j]); // for debugging
                            throw new IllegalArgumentException("unable to find plane: "+planesArray[j]);
                        }
                        if (unitsArray[j] == null) {
                            s.append("\"").append(o).append("\"\t");
                        } else {
                            Datum d = (Datum) o;
                            DatumFormatter f = d.getFormatter();
                            s.append(f.format(d, unitsArray[j])).append("\t");
                        }
                    }
                    r.write(s.toString());
                    r.newLine();
                    prefs.put("components.DataPointRecorder.lastFileSave", file.toString());
                    prefs.put("components.DataPointRecorder.lastFileLoad", file.toString());
                }
            }
        } finally {
            r.close();
        }
        modified = false;
        updateStatus();

    }

    private int lineCount( File file ) throws IOException {
         BufferedReader r=null;
         int lineCount = 0;
         try {
            FileInputStream in = new FileInputStream(file);
            r = new BufferedReader(new InputStreamReader(in));


            for (String line = r.readLine(); line != null; line = r.readLine()) {
                lineCount++;
            }
        } catch ( IOException ex ) {
            throw ex;

        } finally {
            if ( r!=null ) r.close();
        }
        return lineCount;
        
    }

    public void loadFromFile(File file) throws IOException {

        ProgressMonitor mon= new NullProgressMonitor();

        BufferedReader r=null;

        boolean active0= active;
        boolean sorted0= sorted;
        
        try {
            active = false;
            sorted = false;

            int lineCount= lineCount( file );

            r = new BufferedReader( new FileReader( file ) );

            dataPoints.clear();
            String[] planesArray1 = null;
            Units[] unitsArray1 = null;

            Datum x;
            Datum y;
            Map planes;// = new LinkedHashMap();

            if (lineCount > 500) {
                mon = DasProgressPanel.createFramed("reading file");
            }

            // tabs detected in file.
            String delim= "\t";
            boolean delimCheck= true;
            
            mon.setTaskSize(lineCount);
            mon.started();
            int linenum = 0;
            for (String line = r.readLine(); line != null; line = r.readLine()) {
                linenum++;
                if (mon.isCancelled()) {
                    break;
                }
                String tline= line.trim();
                if ( tline.length()==0 ) {
                    continue;
                }
                
                if ( delimCheck && !line.contains("\t") ) {
                    Pattern p= Pattern.compile("([\\s\\,\\;])");
                    Matcher m= p.matcher(line);
                    if ( m.find() ) {
                        char cdelim= m.group(1).charAt(0);
                        if ( Character.isWhitespace(cdelim) ) {
                            delim= "\\s+";
                        } else {
                            delim= m.group(1);
                        }
                    }
                }
                delimCheck= false;
                
                mon.setTaskProgress(linenum);
                if (line.startsWith("## ") || line.length()>0 && Character.isJavaIdentifierStart( line.charAt(0) ) ) {
                    if ( unitsArray1!=null ) continue;
                    while ( line.startsWith("#") ) line = line.substring(1);
                    String[] s = line.split(delim,-2);
                    for ( int i=0; i<s.length; i++ ) {
                        s[i]= s[i].trim();
                    }
                    if ( s[s.length-1].length()==0 ) {
                        s= Arrays.copyOf(s,s.length-1);
                    }
                    Pattern p = Pattern.compile("([^\\(]+)\\((.*)\\)");
                    planesArray1 = new String[s.length];
                    unitsArray1 = new Units[s.length];
                    for (int i = 0; i < s.length; i++) {
                        Matcher m = p.matcher(s[i]);
                        if (m.matches()) {
                            //System.err.printf("%d %s\n", i, m.group(1) );
                            planesArray1[i] = m.group(1).trim();
                            switch (m.group(2).trim()) {
                                case "UTC":
                                    unitsArray1[i] = Units.cdfTT2000;
                                    break;
                                case "ordinal":
                                case "class java.lang.StringUnit(ordinal)":
                                    unitsArray1[i] = EnumerationUnits.create("default");
                                    break;
                                default:
                                    unitsArray1[i] = Units.lookupUnits(m.group(2).trim());
                                    break;
                            }
                        } else {
                            planesArray1[i] = s[i].trim();
                            unitsArray1[i] = Units.dimensionless;
                        }
                    }
                    continue;
                }
                String[] s = line.split(delim,-2);
                for ( int i=0; i<s.length; i++ ) {
                    s[i]= s[i].trim();
                }
                if (unitsArray1 == null) {
                    // support for legacy files
                    unitsArray1 = new Units[s.length];
                    for (int i = 0; i < s.length; i++) {
                        if (s[i].charAt(0) == '"') {
                            unitsArray1[i] = null;
                        } else if (TimeUtil.isValidTime(s[i])) {
                            unitsArray1[i] = Units.us2000;
                        } else {
                            unitsArray1[i] = DatumUtil.parseValid(s[i]).getUnits();
                        }
                    }
                    planesArray1 = new String[s.length];
                    System.arraycopy( new String[]{"X", "Y", "comment"}, 0, planesArray1, 0, planesArray1.length );
                    for ( int i=3; i<planesArray1.length; i++ ) {
                        planesArray1[i]= "comment"+i;
                    }
                }

                try {

                    planes = new LinkedHashMap();

                    for (int i = 2; i < unitsArray1.length; i++) {
                        if (unitsArray1[i] == null) {
                            Pattern p = Pattern.compile("\"(.*)\".*");
                            Matcher m = p.matcher(s[i]);
                            if (m.matches()) {
                                EnumerationUnits eu= EnumerationUnits.create("ordinal");
                                unitsArray1[i]= eu;
                                planes.put(planesArray1[i], eu.createDatum( m.group(1) ) );
                            } else {
                                throw new ParseException("parse error, expected \"\"", 0);
                            }
                        } else {
                            try {
                                if ( unitsArray1[i] instanceof EnumerationUnits ) {
                                    EnumerationUnits eu= (EnumerationUnits)unitsArray1[i];
                                    planes.put(planesArray1[i], eu.createDatum( s[i] ));
                                } else {
                                    planes.put(planesArray1[i], unitsArray1[i].parse(s[i]));
                                }
                            } catch (ParseException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }

                    x = unitsArray1[0].parse(s[0]);
                    try {
                        y = unitsArray1[1].parse(s[1]);
                    } catch ( ParseException ex ) {
                        if ( unitsArray1[1] instanceof EnumerationUnits ) {
                            y = ((EnumerationUnits)unitsArray1[1]).createDatum(s[1]);
                        } else {
                            throw ex;
                        }
                    }

                    DataPointSelectionEvent e;
                    e = new DataPointSelectionEvent(this, x, y, planes);
                    dataPointSelected(e);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }


            }

            r.close();

            saveFile= file;  // go ahead and set this in case client is going to do something with this.
            
            Runnable run= new Runnable() {
                @Override
                public void run() {
                    updateStatus();
                    updateClients();            
                    fireDataSetUpdateListenerDataSetUpdated(new DataSetUpdateEvent(this));
                }
            };
            SwingUtilities.invokeLater(run);
            prefs.put("components.DataPointRecorder.lastFileLoad", file.toString());
            
        } finally {

            mon.finished();

            if ( r!=null ) r.close();

            //active = true;
            active= active0;
            sorted= sorted0;
            
            modified = false;

            Runnable run= new Runnable() {
                @Override
                public void run() {
                    //table.getColumnModel();
                    myTableModel.fireTableStructureChanged();
                    table.repaint();
                }
            };
            SwingUtilities.invokeLater(run);      
        }

    }

    /**
     * active=true means fire off events on any change.  false= wait for update button.
     * @param active true means fire off events on any change
     */
    public void setActive( boolean active ) {
        this.active= active;
    }

    /**
     * return the index into the model for the selection
     * @return 
     */
    private int[] getSelectedRowsInModel() {
        int[] selectedRows = table.getSelectedRows();
        for ( int i=0; i<selectedRows.length; i++ ) {
            selectedRows[i]= table.convertRowIndexToModel(selectedRows[i]);
        }
        return selectedRows;
    }
    
    private class MyMouseAdapter extends MouseAdapter {

        JPopupMenu popup;
        JMenuItem menuItem;
        final JTable parent;

        MyMouseAdapter(final JTable parent) {
            this.parent = parent;
            popup = new JPopupMenu("Options");
            menuItem = new JMenuItem("Delete Row(s)");
            menuItem.setAction( getDeleteSelectedAction() );
            popup.add(menuItem);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON3) {
                int rowCount = parent.getSelectedRows().length;
                menuItem.setText("Delete " + rowCount + " Row" + (rowCount != 1 ? "s" : ""));
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
        // hide popup
        }
    }

    private Action getSaveAsAction() {
        return new AbstractAction("Save As...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveAs();
            }
        };
    }

    private Action getSaveAction() {
        return new AbstractAction("Save") {
            @Override
            public void actionPerformed(ActionEvent e) {
                save();
            }
        };
    }

    private Action getClearSelectionAction() {
        return new AbstractAction("Clear Selection") {
            @Override
            public void actionPerformed(ActionEvent e) {
                table.getSelectionModel().clearSelection();
                fireSelectedDataSetUpdateListenerDataSetUpdated(new DataSetUpdateEvent(this));  
            }
        };
    }

    
    /**
     * return true if the file was saved, false if cancel
     * @return true if the file was saved, false if cancel
     */
    public boolean saveAs() {
        JFileChooser jj = new JFileChooser();
        jj.setFileFilter( new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if ( pathname==null ) return false; //            rte_1178734273_20140402_133610_wsk, I think this happens on Windows.
                if ( pathname.isDirectory() ) return true;
                String fn= pathname.getName();
                return fn.endsWith(".dat") || fn.endsWith(".txt");
            }
            @Override
            public String getDescription() {
                return "Flat Ascii Tables";
            }
        });
        String lastFileString = prefs.get("components.DataPointRecorder.lastFileSave", "");
        if (lastFileString.length()>0) {
            File lastFile= new File(lastFileString);
            jj.setSelectedFile(lastFile);
        }

        int status = jj.showSaveDialog(DataPointRecorder.this);
        if (status == JFileChooser.APPROVE_OPTION) {
            try {
                File pathname= jj.getSelectedFile();
                if ( !( pathname.toString().endsWith(".dat") || pathname.toString().endsWith(".txt") ) ) {
                    pathname= new File( pathname.getAbsolutePath() + ".dat" );
                }
                DataPointRecorder.this.saveFile = pathname;
                saveToFile(saveFile);
            //messageLabel.setText("saved data to "+saveFile);
            } catch (IOException e1) {
                DasApplication.getDefaultApplication().getExceptionHandler().handle(e1);
                return false;
            }
        } else if ( status == JFileChooser.CANCEL_OPTION ) {
            return false;
        }
        return true;
    }

    public boolean save() {
        if (saveFile == null) {
            return saveAs();
        } else {
            try {
                saveToFile(saveFile);
                return true;
            } catch (IOException ex) {
                DasApplication.getDefaultApplication().getExceptionHandler().handle(ex);
                return false;
            }
        }
    }

    /**
     * shows the current name for the file.
     * @return the current name for the file.
     */
    public File getCurrentFile() {
        return this.saveFile;
    }


    /**
     * return true if the file was saved or "don't save" was pressed by the user.
     * @return true if the file was saved or "don't save" was pressed by the user.
     */
    public boolean saveBeforeExit( ) {
        if ( this.modified ) {
            int i= JOptionPane.showConfirmDialog( this, "Save changes before exiting?");
            switch (i) {
                case JOptionPane.OK_OPTION:
                    return save();
                case JOptionPane.CANCEL_OPTION:
                    return false;
                default:
                    return true;
            }
        } else {
            return true;
        }
    }

    private Action getLoadAction() {
        return new AbstractAction("Open...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (checkModified(e)) {
                    JFileChooser jj = new JFileChooser();
                    String lastFileString = prefs.get("components.DataPointRecorder.lastFileLoad", "");
                    if ( lastFileString.length()>0 ) {
                        File lastFile;
                        lastFile = new File(lastFileString);
                        jj.setSelectedFile(lastFile);
                    }

                    int status = jj.showOpenDialog(DataPointRecorder.this);
                    if (status == JFileChooser.APPROVE_OPTION) {
                        final File loadFile = jj.getSelectedFile();
                        prefs.put("components.DataPointRecorder.lastFileLoad", loadFile.toString());
                        Runnable run = new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    loadFromFile(loadFile);
                                    updateStatus();
                                } catch (IOException e) {
                                    DasApplication.getDefaultApplication().getExceptionHandler().handle(e);
                                }

                            }
                        };
                        new Thread(run).start();
                    }

                }
            }
        };
    }

    /**
     * returns true if the operation should continue, false
     * if not, meaning the user pressed cancel.
     */
    private boolean checkModified(ActionEvent e) {
        if (modified) {
            int n = JOptionPane.showConfirmDialog(
                    DataPointRecorder.this,
                    "Current work has not been saved.\n  Save first?",
                    "Save work first",
                    JOptionPane.YES_NO_CANCEL_OPTION);
            if (n == JOptionPane.YES_OPTION) {
                getSaveAction().actionPerformed(e);
            }

            return (n != JOptionPane.CANCEL_OPTION);
        } else {
            return true;
        }

    }

    private Action getNewAction() {
        return new AbstractAction("New") {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (checkModified(e)) {
                    dataPoints.clear();
                    saveFile =  null;
                    updateStatus();
                    updateClients();
                    fireDataSetUpdateListenerDataSetUpdated(new DataSetUpdateEvent(DataPointRecorder.this));
                    table.repaint();
                }

            }
        };
    }

    private Action getPropertiesAction() {
        return new AbstractAction("Properties") {
            @Override
            public void actionPerformed(ActionEvent e) {
                new PropertyEditor(DataPointRecorder.this).showDialog(DataPointRecorder.this);
            }
        };
    }

    private Action getUpdateAction() {
        return new AbstractAction("Update") {
            @Override
            public void actionPerformed(ActionEvent e) {
                update();
            }
        };
    }

    private Action getDeleteSelectedAction() {
        return new AbstractAction("Delete Selected") {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] selectedRows = getSelectedRowsInModel();
                deleteRows(selectedRows);                
            }            
        };
    }
    
    private Action getSetUnitsAction() {
        return new AbstractAction("Reset Units...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                editUnits();
            }            
        };
    }
    
    /**
     * show a dialog that allows each column's units to be reset.
     */
    private void editUnits() {
        JPanel p= new JPanel();
        p.setLayout( new BoxLayout( p, BoxLayout.Y_AXIS ) );
        
        int i=0;
        JComboBox[] cbs= new JComboBox[unitsArray.length];
        for ( Units u: unitsArray ) {
            JLabel l= new JLabel("column "+planesArray[i]+": "+u) ;
            
            p.add(l);
            
            JComboBox c= new JComboBox();
            c.setEditable(false);
            Units[] uu= u.getConvertibleUnits();
            String[] ss= new String[uu.length];
            for ( int j=0; j<uu.length; j++ ) {
                ss[j]= uu[j].toString();
            }
            DefaultComboBoxModel model= new DefaultComboBoxModel(ss);
            c.setModel(model);
            c.setSelectedItem(u);
            cbs[i]= c;
            p.add(c);
            i++;
        }
        
        if ( JOptionPane.showConfirmDialog( this, p, "Reset Units", JOptionPane.OK_CANCEL_OPTION )==JOptionPane.OK_OPTION ) {
            for ( i=0; i<unitsArray.length; i++ ) {
                if ( cbs[i].getSelectedItem()!=null ) {
                    unitsArray[i]= Units.lookupUnits(cbs[i].getSelectedItem().toString());
                } else {
                    logger.finest("ignoring non-selection...");
                }
            }
            myTableModel.fireTableDataChanged();
            myTableModel.fireTableStructureChanged();
        }
    }
    
    private Action getInsertFillAction() {
        return new AbstractAction("Insert Fill...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                if ( ( e.getModifiers() & KeyEvent.SHIFT_MASK )==KeyEvent.SHIFT_MASK ) {
                    insertFill(true);
                } else {
                    int index= table.getSelectedRow();
                    if ( index==-1 ) {
                        JOptionPane.showMessageDialog(DataPointRecorder.this,"Row must be selected");
                        return;
                    }
                    if ( JOptionPane.OK_OPTION==
                            JOptionPane.showConfirmDialog(DataPointRecorder.this,
                                    "Insert a fill record into in data?", 
                                    "Insert Fill", JOptionPane.OK_CANCEL_OPTION ) ) {
                        insertFill(false);
                    }
                }
            }            
        };
    }
    
    /**
     * insert a fill record at the selected cell.
     */
    private void insertFill( boolean appendToEnd ) { 
        Map planes= new LinkedHashMap<>();
        int icol=0;
        for ( String p: planesArray ) {
            planes.put( p, unitsArray[icol].getFillDatum() );
            icol++;
        }
        DataPoint fill= new DataPoint( unitsArray[0].getFillDatum(), unitsArray[1].getFillDatum(), planes );
        int index;
        if ( appendToEnd ) {
            index= table.getRowCount();
        } else {
            index= table.getSelectedRow();
            if ( index==-1 ) {
                throw new IllegalArgumentException("Row must be selected");
            }
            index= table.convertRowIndexToModel(index);
        }
        dataPoints.add( index, fill );
        modified = true;
        Runnable run= new Runnable() {
            public void run() {
                updateStatus();
                updateClients();
                table.repaint();
                if (active) {
                    fireDataSetUpdateListenerDataSetUpdated(new DataSetUpdateEvent(this));
                }
            }
        };   
        if ( SwingUtilities.isEventDispatchThread() ) {
            run.run();
        } else {
            SwingUtilities.invokeLater(run);
        }
    }
    
    
    /**
     * Notify listeners that the dataset has updated.  Pressing the "Update" 
     * button calls this.
     */
    public void update() {
        if (dataSetDescriptor != null) {
            dataSetDescriptor.fireUpdate();
        }

        fireDataSetUpdateListenerDataSetUpdated(new DataSetUpdateEvent(this));
        fireSelectedDataSetUpdateListenerDataSetUpdated(new DataSetUpdateEvent(this));        
    }
    
    /** Creates a new instance of DataPointRecorder */
    public DataPointRecorder() {
        super();
        this.planesArrayLock = new Object();
        dataPoints = new ArrayList();
        myTableModel = new MyTableModel();
        this.setLayout(new BorderLayout());

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        fileMenu.add(new JMenuItem(getNewAction()));
        fileMenu.add(new JMenuItem(getLoadAction()));
        fileMenu.add(new JMenuItem(getSaveAction()));
        fileMenu.add(new JMenuItem(getSaveAsAction()));
        menuBar.add(fileMenu);

        JMenu editMenu = new JMenu("Edit");
        editMenu.add(new JMenuItem(getPropertiesAction()));
        
        editMenu.add(new JMenuItem(getSetUnitsAction()));
        JMenuItem i= new JMenuItem(getInsertFillAction());
        i.setToolTipText("insert a fill record (interrupts data cadence) at the selected row, or hold shift to append a fill record.");
        editMenu.add(i);
        
        editMenu.add( new JMenuItem( new AbstractAction("Clear Table Sorting") {
            @Override
            public void actionPerformed(ActionEvent e) {
                table.setAutoCreateRowSorter(false);
                table.setAutoCreateRowSorter(true);
            }
        } ) );
        menuBar.add(editMenu);

        this.add(menuBar, BorderLayout.NORTH);

        planesArray = new String[]{"X", "Y"};
        unitsArray = new Units[]{null, null};

        table = new JTable(myTableModel);
        table.setAutoCreateRowSorter(true); // Java 1.6

        table.getTableHeader().setReorderingAllowed(true);
        table.setColumnModel( new DefaultTableColumnModel() {

            @Override
            public int getColumnCount() {
                synchronized ( planesArrayLock ) {
                    return super.getColumnCount(); //To change body of generated methods, choose Tools | Templates.
                }
            }

            @Override
            public TableColumn getColumn(int columnIndex) {
                synchronized ( planesArrayLock ) {
                    return super.getColumn(columnIndex); //To change body of generated methods, choose Tools | Templates.
                }
            }
            
        });
        table.setRowSelectionAllowed(true);
        table.addMouseListener(new DataPointRecorder.MyMouseAdapter(table));
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                fireSelectedDataSetUpdateListenerDataSetUpdated(new DataSetUpdateEvent(DataPointRecorder.this));
                int selected = table.getSelectedRow(); // we could do a better job here
                if (selected > -1) {
                    selected= table.convertRowIndexToModel(selected);
                    DataPoint dp = (DataPoint) dataPoints.get(selected);
                    Map planes= new HashMap();
                    for ( int i=2; i<planesArray.length; i++ ) {
                        planes.put( planesArray[i], dp.getPlane(planesArray[i]) );
                    }
                    DataPointSelectionEvent e2 = new DataPointSelectionEvent(DataPointRecorder.this, dp.get(0), dp.get(1), planes );
                    fireDataPointSelectionListenerDataPointSelected(e2);
                }
                Runnable run= new Runnable() {
                    @Override
                    public void run() {
                        deleteSelectionButton.setEnabled( table.getSelectedRowCount()>0 );
                        clearSelectionButton.setEnabled( table.getSelectedRowCount()>0 );                
                    }
                };
                if (SwingUtilities.isEventDispatchThread()) {
                    run.run();
                } else {
                    SwingUtilities.invokeLater(run);
                }
            }
        });

        scrollPane = new JScrollPane(table);
        this.add(scrollPane, BorderLayout.CENTER);

        JPanel controlStatusPanel = new JPanel();
        controlStatusPanel.setLayout(new BoxLayout(controlStatusPanel, BoxLayout.Y_AXIS));

        final JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.X_AXIS));

        updateButton = new JButton(getUpdateAction());
        updateButton.setVisible(false);
        updateButton.setEnabled(false);

        controlPanel.add(updateButton);

        clearSelectionButton = new JButton( getClearSelectionAction() );
        controlPanel.add( clearSelectionButton );

        deleteSelectionButton = new JButton( getDeleteSelectedAction() );
        controlPanel.add( deleteSelectionButton );
        
        controlPanel.add( Box.createGlue() );
        accessoryPanel= new JPanel( new BorderLayout() );
        controlPanel.add( accessoryPanel );
        
        messageLabel = new JLabel("ready");
        messageLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);

        controlStatusPanel.add(messageLabel);

        controlPanel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        controlStatusPanel.add(controlPanel);

        this.add(controlStatusPanel, BorderLayout.SOUTH);
    }

    public static DataPointRecorder createFramed() {
        DataPointRecorder result;
        JFrame frame = new JFrame("Data Point Recorder");
        result = new DataPointRecorder();
        frame.getContentPane().add(result);
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        return result;
    }

    /**
     * add a small component to the lower-right portion of the DataPointRecorder.  It should be roughly the size of a
     * button.
     * @param c the component.
     */
    public void addAccessory( JComponent c ) {
        accessoryPanel.add(c);
    }
    
    /**
     * update fires off the TableDataChanged, and sets the current selected
     * row if necessary.
     */
    private void updateClients() {
        if (active) {
            myTableModel.fireTableDataChanged();
            if (selectRow != -1 && table.getRowCount()>selectRow ) {
                int i= table.convertRowIndexToView(selectRow);
                table.setRowSelectionInterval(i, i);
                table.scrollRectToVisible(table.getCellRect(i, 0, true));
                selectRow = -1;
            }
            table.repaint();
        }
    }

    /**
     * update the status label "(modified)"
     */
    private void updateStatus() {
        String statusString = (saveFile == null ? "" : (String.valueOf(saveFile) + " ")) +
                (modified ? "(modified)" : "");
        String t= messageLabel.getText();
        if ( !statusString.equals(t) ) {
            messageLabel.setText(statusString);
        }
    }

    /**
     * insert the point into the data points.  If the dataset is sorted, then we
     * replace any point that is within 10milliseconds of the point.
     * @param newPoint 
     */
    private void insertInternal(DataPoint newPoint) {
        int newSelect;
        synchronized ( dataPoints ) {
            Set<String> keys= newPoint.planes.keySet();
            int ikey=2;
            for ( String key: keys ) {
                Object o= newPoint.planes.get(key);
                if ( o instanceof QDataSet ) {
                    QDataSet qds= (QDataSet)o;
                    if ( qds.rank()>0 ) {
                        throw new IllegalArgumentException("QDataSet rank must be zero: "+key+"="+o );
                    } else {
                        Datum d= DataSetUtil.asDatum((QDataSet)o);
                        if ( !d.getUnits().isConvertibleTo( unitsArray[ikey]) ) {
                            throw new IllegalArgumentException("Units are not convertible: "+key+"="+d + ", expected "+unitsArray[ikey]);
                        }
                        newPoint.planes.put( key, d );
                    }
                } else if ( o instanceof Datum ) {
                    Datum d= (Datum)o;
                    if ( !d.getUnits().isConvertibleTo( unitsArray[ikey]) ) {
                        if ( unitsArray[ikey] instanceof EnumerationUnits ) {
                            ((EnumerationUnits)unitsArray[ikey]).createDatum( d.toString() );
                        } else {
                            throw new IllegalArgumentException("Units are not convertible: "+key+"="+d+ ", expected "+unitsArray[ikey]);
                        }
                    }
                    // do nothing
                } else if ( o instanceof String ) {
                    newPoint.planes.put( key, ((EnumerationUnits)unitsArray[ikey]).createDatum(o) );
                } else if ( o instanceof Number ) {
                    newPoint.planes.put( key, (unitsArray[ikey]).createDatum(((Number)o) ) );
                }
                ikey++;
            }
            if (sorted) {
                if ( dataPoints.size()>2 ) { // this checks out, it is sorted...
                    Units off= ((DataPoint)(dataPoints.get(0))).data[0].getUnits().getOffsetUnits();
                    Datum doff= off.createDatum(0);
                    for ( int i=1; i<dataPoints.size(); i++ ) {
                        DataPoint p1= (DataPoint)(dataPoints.get(i));
                        DataPoint p0= (DataPoint)(dataPoints.get(i-1));
                        if ( p1.data[0].subtract(p0.data[0]).lt(doff) ) {
                            logger.fine("here not sorted");
                        }
                    }
                }
                int index = Collections.binarySearch( dataPoints, newPoint );
                if (index < 0) {
                    
                    DataPoint dp0= null;
                    if ( ~index<dataPoints.size() ) {
                        dp0= (DataPoint)dataPoints.get(~index);
                        keys= newPoint.planes.keySet();
                        for ( String key : keys ) {
                            if ( !dp0.planes.containsKey(key) ) {
                                logger.log(Level.FINE, "no place to put key: {0}", key);
                            }
                        }
                    }
                    DataPoint dp1= null;
                    if  ( (~index+1)<dataPoints.size() ) { // check for very close point.
                        dp1= (DataPoint)dataPoints.get(~index+1);
                    }
                    
                    Datum epsilon= Units.microseconds.createDatum(100);
//                    if ( dp0!=null ) { // if we can check for numerical resolution, use this.
//                        double eps= Math.abs( newPoint.data[0].doubleValue(((DataPoint)dataPoints.get(0)).data[0].getUnits()) );
//                        for ( int i=0; i<dataPoints.size(); i++ ) {
//                            double teps= Math.abs( ((DataPoint)dataPoints.get(i)).data[0].doubleValue(newPoint.data[0].getUnits()) );
//                            if ( teps>eps ) eps= teps;
//                        } 
//                        eps= eps * 0.00000000001;// something like numerical noise * 10.
//                        epsilon= newPoint.data[0].getUnits().getOffsetUnits().createDatum(eps);
//                    }
                    if ( newPoint.data[0].getUnits().getOffsetUnits().isConvertibleTo(Units.milliseconds) ) {
                        if ( dp0!=null && dp0.data[0].subtract(newPoint.data[0]).abs().lt(epsilon) ) {
                            dataPoints.set( ~index, newPoint );
                        } else if ( dp1!=null && dp1.data[0].subtract(newPoint.data[0]).abs().lt(epsilon) ) {
                            dataPoints.set( ~index+1, newPoint );
                        } else {
                            dataPoints.add(~index, newPoint);
                        }
                    } else {
                        dataPoints.add(~index, newPoint);
                    }
                    newSelect = ~index;
                } else {
                    dataPoints.set(index, newPoint);
                    newSelect = index;
                }
                logger.log(Level.FINER, "dataPoints.size()={0}", dataPoints.size());
            } else {
                boolean insertCheck = false; //TODO: consider how this can be done safely.
                if ( table.getSelectedRows().length==1 && insertCheck ) {
                    int[] isel= getSelectedRowsInModel();
                    newSelect= isel[0]+1;
                    if ( newSelect==dataPoints.size() ) {
                        dataPoints.add(newPoint);
                    } else {
                        dataPoints.add(newSelect,newPoint);
                    }
                } else {
                    dataPoints.add(newPoint);
                    newSelect = dataPoints.size() - 1;
                }
                logger.log(Level.FINER, "dataPoints.size()={0}", dataPoints.size());
            }

            selectRow = newSelect;
        }
        modified = true;
        if ( SwingUtilities.isEventDispatchThread() ) {
            updateStatus();
            updateClients();
            table.repaint();
        }
    }
    
    /**
     * add a record, which should be a rank 1 bundle.
     * @param ds 
     */
    public void addDataPoint( QDataSet ds ) {
        Datum x,y;
        x= DataSetUtil.asDatum( ds.slice(0) );
        y= DataSetUtil.asDatum( ds.slice(1) );
        Map<String,Datum> planes= new LinkedHashMap<>();
        String[] planeNames = DataSetUtil.bundleNames(ds);
        for ( int i=2; i<ds.length(); i++ ) {
            QDataSet d= ds.slice(i);
            planes.put( planeNames[i], DataSetUtil.asDatum(d) );
        }
        addDataPoint( x, y, planes );
    }
    
    /**
     * add just the x and y values.
     * @param x the x position
     * @param y the y position
     */
    public void addDataPoint( Datum x, Datum y ) {
        addDataPoint( x, y, null );
    }
    
    /**
     * add just the x and y values.
     * @param x the x position
     * @param y the y position
     */
    public void addDataPoint( double x, double y ) {
        addDataPoint( Units.dimensionless.createDatum(x), Units.dimensionless.createDatum(y), null );
    }
    
    /**
     * add just the x and y values.
     * @param x the x position
     * @param y the y position
     * @param planes additional planes, map from String name &rarr; String, Datum, or Number.
     */
    public void addDataPoint( double x, double y, Map planes ) {
        addDataPoint( Units.dimensionless.createDatum(x), Units.dimensionless.createDatum(y), planes );
    }
    
    /**
     * add the x and y values with unnamed metadata.
     * @param x the x position
     * @param y the y position
     * @param meta any metadata (String, Double, etc ) to be recorded along with the data point.
     */    
    public void addDataPoint( Datum x, Datum y, Object meta ) {
        addDataPoint( x, y, Collections.singletonMap("meta",meta) );
    }
    
    /**
     * add the data point, along with metadata such as the key press.
     * @param x the x position
     * @param y the y position
     * @param planes additional planes, map from String name &rarr; String, Datum, or Number.
     * @throws RuntimeException when the units are not convertible. 
     */
    public void addDataPoint( Datum x, Datum y, Map planes ) {
        synchronized (dataPoints) {
            if ( planes==null ) planes= new LinkedHashMap();
            if (dataPoints.isEmpty()) {
                unitsArray    = new Units[2 + planes.size()];
                unitsArray[0] = x.getUnits();
                unitsArray[1] = y.getUnits();
                planesArray    = new String[2 + planes.size()];
                planesArray[0] = "x";
                planesArray[1] = "y";
                int index = 2;
                for ( Iterator i = planes.entrySet().iterator(); i.hasNext();) {
                    Entry entry= (Entry)i.next();
                    Object key = entry.getKey();
                    planesArray[index] = String.valueOf(key).trim();
                    Object value = entry.getValue();
                    if (value instanceof String) {
                        unitsArray[index] = EnumerationUnits.create("default");
                    } else {
                        if ( value instanceof Datum ) {
                            unitsArray[index] = ((Datum) value).getUnits();
                        } else if ( value instanceof QDataSet ) {
                            QDataSet qds= (QDataSet)value;
                            if ( qds.rank()>0 ) {
                                throw new IllegalArgumentException("qdatasets in planes must be rank 0");
                            } else {
                                unitsArray[index] = SemanticOps.getUnits((QDataSet)value);
                            }
                        } else if ( value instanceof Number ) {
                            unitsArray[index]= Units.dimensionless;
                        } else {
                            throw new IllegalArgumentException("values must be rank 0 Datum or QDataSet, not " + value);
                        }
                    }
                    index++;
                }

                myTableModel.fireTableStructureChanged();
                for ( int i=0; i<1; i++ ) { //i<unitsArray.length
                    if ( UnitsUtil.isTimeLocation( unitsArray[i] ) ) {
                        table.getTableHeader().getColumnModel().getColumn(i).setMinWidth( TIME_WIDTH );   
                    }
                }

            }

            if (!x.getUnits().isConvertibleTo(unitsArray[0])) {
                throw new RuntimeException("inconvertible units: got \"" + x.getUnits() + "\", expected \"" + unitsArray[0] + "\"");
            }

            if (!y.getUnits().isConvertibleTo(unitsArray[1])) {
                throw new RuntimeException("inconvertible units: got \"" + y.getUnits() + "\", expected \"" + unitsArray[1] + "\"");
            }

            insertInternal(new DataPoint(x, y, new LinkedHashMap(planes)));
        }
        if (active) {
            fireDataSetUpdateListenerDataSetUpdated(new DataSetUpdateEvent(this));
        }
 
    }

    /**
     * @deprecated see #addDataPoints, which does not use DEPEND_0.
     * @see #addDataPoints(org.das2.qds.QDataSet) 
     * @param ds 
     */
    public void appendDataSet( QDataSet ds ) {
        throw new IllegalArgumentException("not supported");
    }
    
    /**
     * append the rank 2 data.  The data should be rank 2, without DEPEND_0.
     * Note earlier versions of this code assumed there would be a DEPEND_0.
     * 
     * @param ds rank 2 bundle dataset.
     * @see #addDataPoint(org.das2.qds.QDataSet) 
     */
    public void addDataPoints( QDataSet ds ) {
        
        boolean active0= this.active;
        
        Map planesMap = new LinkedHashMap();

        if ( ds.rank()!=2 ) throw new IllegalArgumentException("dataset should be rank 2");
        QDataSet dep0= (QDataSet) ds.property(QDataSet.DEPEND_0);
        if ( dep0!=null ) throw new IllegalArgumentException("dataset should not have DEPEND_0");
        
        dep0= Ops.slice1( ds,0  );
        if ( dep0.property(QDataSet.CADENCE) != null) {
            DataPointRecorder.this.xTagWidth = DataSetUtil.asDatum( (QDataSet)dep0.property(QDataSet.CADENCE) );
        } else {
            DataPointRecorder.this.xTagWidth = Datum.create(0);
        }

        String[] planes = DataSetUtil.bundleNames(ds);

        for (int i = 0; i < ds.length(); i++) {
            for (int j = 2; j < planes.length; j++) {
                if (!planes[j].equals("")) {
                    planesMap.put( planes[j], DataSetUtil.asDatum( DataSetOps.unbundle( ds, planes[j] ).slice(i) ) );
                }
            }
            addDataPoint( DataSetUtil.asDatum( ds.slice(i).slice(0) ), DataSetUtil.asDatum( ds.slice(i).slice(1) ), planesMap );
        }

        active= active0;
        
        updateClients();
                
    }
    
    /**
     * @deprecated uses old data model, use appendDataSet(QDataSet)
     * @param ds 
     * @see
     */
    public void appendDataSet(VectorDataSet ds) {

        Map planesMap = new LinkedHashMap();

        if (ds.getProperty("comment") != null) {
            planesMap.put("comment", ds.getProperty("comment"));
        }

        if (ds.getProperty("xTagWidth") != null) {
            DataPointRecorder.this.xTagWidth = (Datum) ds.getProperty("xTagWidth");
        } else {
            DataPointRecorder.this.xTagWidth = Datum.create(0);
        }

        String[] planes = ds.getPlaneIds();

        for (int i = 0; i<ds.getXLength(); i++) {
            for (String plane : planes) {
                if (!plane.equals("")) {
                    planesMap.put(plane, ((VectorDataSet) ds.getPlanarView(plane)).getDatum(i));
                }
            }
            addDataPoint(ds.getXTagDatum(i), ds.getDatum(i), planesMap);
        }

        updateClients();
        
    }

    /**
     * this adds all the points in the DataSet to the list.  This will also check the dataset for the special
     * property "comment" and add it as a comment.
     * @return the listener to receive data set updates
     * @see org.das2.dataset.DataSetUpdateEvent
     */
    public DataSetUpdateListener getAppendDataSetUpListener() {
        return new DataSetUpdateListener() {
            @Override
            public void dataSetUpdated(DataSetUpdateEvent e) {
                VectorDataSet ds = (VectorDataSet) e.getDataSet();
                if (ds == null) {
                    throw new RuntimeException("not supported, I need the DataSet in the update event");
                } else {
                    appendDataSet((VectorDataSet) e.getDataSet());
                }

            }
        };
    }

    @SuppressWarnings("deprecation")
    @Override
    public void dataPointSelected(org.das2.event.DataPointSelectionEvent e) {
        Map planesMap;

        if (e instanceof CommentDataPointSelectionEvent) {
            String comment;
            comment = ((CommentDataPointSelectionEvent) e).getComment();
            planesMap = new LinkedHashMap();
            planesMap.put("comment", comment);
        } else {
            String[] x = e.getPlaneIds();
            planesMap = new LinkedHashMap();
            for (String x1 : x) {
                planesMap.put(x1, e.getPlane(x1));
            }

        }

        synchronized (dataPoints) {
            // if a point exists within xTagWidth of the point, then have this point replace
            Datum x = e.getX();
            if (snapToGrid && xTagWidth != null && xTagWidth.value()>0 && !xTagWidth.isFill() && dataPoints.size() > 0) {
                QDataSet ds = getDataSet();
                QDataSet xds= (QDataSet)ds.property(QDataSet.DEPEND_0);
                Units xunits= SemanticOps.getUnits(xds);
                int i = DataSetUtil.closestIndex( xds, x );
                Datum diff = e.getX().subtract( xunits.createDatum(xds.value(i)) );
                if (Math.abs( diff.divide(xTagWidth).doubleValue(Units.dimensionless)) < 0.5 ) {
                    x = xunits.createDatum(xds.value(i));
                }

            }
            addDataPoint(x, e.getY(), planesMap);
        }
        updateClients();
    }

    /**
     * hide the update button if no one is listening.
     * @return true if it is now visible
     */
    boolean checkUpdateEnable() {
        int listenerList1Count;
        int selectedListenerListCount;
        synchronized (this) {
            listenerList1Count= listenerList1==null ? 0 : listenerList1.getListenerCount();
            selectedListenerListCount= selectedListenerList==null ? 0 : selectedListenerList.getListenerCount();
        }
        if ( listenerList1Count>0 || selectedListenerListCount>0 ) {
            updateButton.setEnabled(true);
            updateButton.setVisible(true);
            updateButton.setToolTipText(null);
            return true;
        } else {
            updateButton.setEnabled(false);
            updateButton.setToolTipText("no listeners. See File->Save to save table.");
            updateButton.setVisible(false);
            return false;
        }
    }
    private javax.swing.event.EventListenerList listenerList1 = new javax.swing.event.EventListenerList();

    public void addDataSetUpdateListener(org.das2.dataset.DataSetUpdateListener listener) {
        listenerList1.add(org.das2.dataset.DataSetUpdateListener.class, listener);
        checkUpdateEnable();
    }

    public void removeDataSetUpdateListener(org.das2.dataset.DataSetUpdateListener listener) {
        listenerList1.remove(org.das2.dataset.DataSetUpdateListener.class, listener);
        checkUpdateEnable();
    }

    private void fireDataSetUpdateListenerDataSetUpdated(org.das2.dataset.DataSetUpdateEvent event) {
        Object[] listeners= listenerList1.getListenerList();
        for (int i = listeners.length - 2; i >=0; i-= 2) {
            if (listeners[i] == org.das2.dataset.DataSetUpdateListener.class) {
                ((org.das2.dataset.DataSetUpdateListener) listeners[i + 1]).dataSetUpdated(event);
            }
        }

    }
    
    
    /**
     * the selection are the highlighted points in the table.  Listeners can grab this data and do something with the
     * dataset.
     */
    private javax.swing.event.EventListenerList selectedListenerList = new javax.swing.event.EventListenerList();

    public void addSelectedDataSetUpdateListener(org.das2.dataset.DataSetUpdateListener listener) {
        selectedListenerList.add(org.das2.dataset.DataSetUpdateListener.class, listener);
        checkUpdateEnable();
    }

    public void removeSelectedDataSetUpdateListener(org.das2.dataset.DataSetUpdateListener listener) {
        selectedListenerList.remove(org.das2.dataset.DataSetUpdateListener.class, listener);
        checkUpdateEnable();
    }

    
    private void fireSelectedDataSetUpdateListenerDataSetUpdated(org.das2.dataset.DataSetUpdateEvent event) {
        Object[] listeners= selectedListenerList.getListenerList();
        for ( int i = listeners.length - 2; i >=0; i-=2 ) {
            if (listeners[i] == org.das2.dataset.DataSetUpdateListener.class) {
                ((org.das2.dataset.DataSetUpdateListener) listeners[i + 1]).dataSetUpdated(event);
            }
        }

    }
    
    /**
     * Holds value of property sorted.  When the data is sorted, it means
     * that data points added will be inserted so that the order of the
     * x values is maintained.
     */
    private boolean sorted = true;

    /**
     * Getter for property sorted.
     * @return Value of property sorted.
     */
    public boolean isSorted() {

        return this.sorted;
    }

    /**
     * Setter for property sorted.
     * @param sorted New value of property sorted.
     */
    public void setSorted(boolean sorted) {

        this.sorted = sorted;
    }

    /**
     * Registers DataPointSelectionListener to receive events.
     * @param listener The listener to register.
     */
    public void addDataPointSelectionListener(org.das2.event.DataPointSelectionListener listener) {
        if (listenerList1 == null) {
            listenerList1 = new javax.swing.event.EventListenerList();
        }
        listenerList1.add(org.das2.event.DataPointSelectionListener.class, listener);
    }

    /**
     * Removes DataPointSelectionListener from the list of listeners.
     * @param listener The listener to remove.
     */
    public void removeDataPointSelectionListener(org.das2.event.DataPointSelectionListener listener) {
        listenerList1.remove(org.das2.event.DataPointSelectionListener.class, listener);
    }

    /**
     * Notifies all registered listeners about the event.
     *
     * @param event The event to be fired
     */
    private void fireDataPointSelectionListenerDataPointSelected(org.das2.event.DataPointSelectionEvent event) {
        Object[] listeners= listenerList1.getListenerList();
        logger.fine("firing data point selection event");
        for (int i = listeners.length - 2; i >= 0; i -= 2 ) {
            if (listeners[i] == org.das2.event.DataPointSelectionListener.class) {
                ((org.das2.event.DataPointSelectionListener) listeners[i + 1]).dataPointSelected(event);
            }
        }

    }
    
    /**
     * Holds value of property xTagWidth.
     */
    private Datum xTagWidth = Datum.create(0);

    /**
     * Getter for property xTagWidth.  When xTagWidth is zero,
     * this implies there is no binning.
     * @return Value of property xTagWidth.
     */
    public Datum getXTagWidth() {
        return this.xTagWidth;
    }

    /**
     * bins for the data, when xTagWidth is non-zero.
     * @param xTagWidth New value of property xTagWidth.
     */
    public void setXTagWidth(Datum xTagWidth) {
        this.xTagWidth = xTagWidth;
    }
    /**
     * Holds value of property snapToGrid.
     */
    private boolean snapToGrid = false;

    /**
     * Getter for property snapToGrid.
     * @return Value of property snapToGrid.
     */
    public boolean isSnapToGrid() {

        return this.snapToGrid;
    }

    /**
     * Setter for property snapToGrid.  true indicates the xtag will be reset
     * so that the tags are equally spaced, each xTagWidth apart.
     * @param snapToGrid New value of property snapToGrid.
     */
    public void setSnapToGrid(boolean snapToGrid) {

        this.snapToGrid = snapToGrid;
    }
    
    private String timeFormat = "$Y-$m-$dT$H:$M:$S.$(subsec,places=3)Z";
    private TimeParser timeFormatter= TimeParser.create(timeFormat);

    /**
     * Get the value of timeFormat
     *
     * @return the value of timeFormat
     */
    public String getTimeFormat() {
        return timeFormat;
    }

    /**
     * Set the value of timeFormat
     *
     * @param timeFormat new value of timeFormat
     */
    public void setTimeFormat(String timeFormat) {
        if ( timeFormat==null ) timeFormat= "";
        this.timeFormat = timeFormat;
        if ( timeFormat.length()==0 ) {
            this.timeFormatter= null;
        } else {
            this.timeFormatter= TimeParser.create(timeFormat);
            //TODO: 
        }
    }


    /**
     * return true when the data point recorder has been modified.
     * @return true when the data point recorder has been modified.
     */
    public boolean isModified() {
        return modified;
    }
    
}
