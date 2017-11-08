/*
 * DataPointRecorderNew.java
 *
 * Created on Apr 18, 2014 5:57am 
 */
package org.das2.components;

import org.das2.dataset.DataSetUpdateEvent;
import org.das2.dataset.VectorDataSet;
import org.das2.dataset.DataSetUpdateListener;
import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import org.das2.datum.Datum;
import org.das2.datum.DatumUtil;
import org.das2.datum.TimeUtil;
import org.das2.util.DasExceptionHandler;
import org.das2.util.monitor.NullProgressMonitor;
import org.das2.util.monitor.ProgressMonitor;
import org.das2.components.propertyeditor.PropertyEditor;
import org.das2.datum.format.DatumFormatter;
import org.das2.system.DasLogger;
import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
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
import org.das2.datum.EnumerationUnits;
import org.das2.datum.InconvertibleUnitsException;
import org.das2.datum.UnitsUtil;
import org.das2.event.DataPointSelectionEvent;
import org.das2.qds.AbstractDataSet;
import org.das2.qds.ArrayDataSet;
import org.das2.qds.DDataSet;
import org.das2.qds.DataSetOps;
import org.das2.qds.DataSetUtil;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;
import org.das2.qds.SparseDataSetBuilder;
import org.das2.qds.ops.Ops;
import org.das2.qds.util.DataSetBuilder;

/**
 * DataPointRecorderNew is a GUI for storing data points selected by the user.  
 * This is the old recorder but:
 * 1. uses QDataSet to handle the data.  No more strange internal object.
 * 2. allows the columns to be declared explicitly by code, and data is merged in by name.
 * @deprecated use DataPointRecorder, which is this code.  DataPointRecorderNew is left because of use in Jython scripts.
 * @author  jbf
 */
public class DataPointRecorderNew extends JPanel {

    /**
     * width of time column
     */
    private static final int TIME_WIDTH = 180;

    protected JTable table;
    protected JScrollPane scrollPane;
    protected JButton updateButton;
    protected final transient List<QDataSet> dataPoints;
    private int selectRow; // this row needs to be selected after the update.
    
    /**
     * units[index]==null if HashMap contains non-datum object.
     */
    protected transient Units[] unitsArray;
    
    protected transient  Units[] defaultUnitsArray;
    
    /**
     * array of names that are also the column headers. 
     */
    protected transient  String[] namesArray;
    
    protected transient  String[] defaultNamesArray;
    
    private double[] defaultsArray;
            
    /**
     * bundleDescriptor for the dataset.
     */
    private transient QDataSet bundleDescriptor;
    
    protected AbstractTableModel myTableModel;
    private File saveFile;
    private boolean modified;
    private final JLabel messageLabel;
    private boolean active = true; // false means don't fire updates
    private transient Preferences prefs = Preferences.userNodeForPackage(this.getClass());
    private static final Logger logger = DasLogger.getLogger(DasLogger.GUI_LOG);
    private final JButton clearSelectionButton;


    private final Object namesArrayLock;
    
    private class MyTableModel extends AbstractTableModel {
        @Override
        public int getColumnCount() {
            synchronized (namesArrayLock) {
                return namesArray==null ? 0 : namesArray.length;
            }
        }

        @Override
        public String getColumnName(int j) {
            synchronized (namesArrayLock) {
                String result = namesArray[j];
                if (unitsArray[j] != null) {
                    if ( unitsArray[j] instanceof EnumerationUnits ) {
                        result += "(ordinal)";
                    } else if ( UnitsUtil.isTimeLocation( unitsArray[j] ) ) {
                        result += "(UTC)";
                    } else if ( unitsArray[j]==Units.dimensionless ) {
                        // add nothing.
                    } else {
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
            QDataSet x;
            synchronized (dataPoints) {
                 x= (QDataSet) dataPoints.get(i);
            }
            if (j < x.length()) {
                Datum d = unitsArray[j].createDatum(x.value(j));
                DatumFormatter format = d.getFormatter();
                return format.format(d, unitsArray[j]);
            } else {
                throw new IndexOutOfBoundsException("no such column");
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
                        Datum d1;
                        if ( o1 instanceof QDataSet ) {
                            d1= DataSetUtil.asDatum(((QDataSet)o1).slice(0));
                        } else if ( o1 instanceof Datum ) {
                            d1= (Datum)o1;
                        } else {
                            throw new IllegalArgumentException("expected Datum or QDataSet");
                        }
                        Datum d2;
                        if ( o2 instanceof QDataSet ) {
                            d2= DataSetUtil.asDatum(((QDataSet)o2).slice(0));
                        } else if ( o2 instanceof Datum ) {
                            d2= (Datum)o2;
                        } else {
                            throw new IllegalArgumentException("expected Datum or QDataSet");
                        }
                        return d1.compareTo(d2);
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
            fireDataSetUpdateListenerDataSetUpdated(new DataSetUpdateEvent(this,getDataSet()));
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
               dataPoints.remove(selectedRows[i]);
            }
            modified = true;
        }
        updateClients();
        updateStatus();
        if ( active ) {
            fireDataSetUpdateListenerDataSetUpdated(new DataSetUpdateEvent(this,getDataSet()));
        }
        myTableModel.fireTableDataChanged();
    }
    

    /**
     * returns a data set of the table data.
     * @return  a data set of the table data.
     */
    public QDataSet getDataSet() {
        DataSetBuilder b;
        synchronized ( dataPoints ) {
            if (dataPoints.isEmpty()) {
                return null;
            } else {
                b= new DataSetBuilder(2,dataPoints.size(),bundleDescriptor.length());
                b.putProperty( QDataSet.BUNDLE_1, bundleDescriptor );
                for (int irow = 0; irow < dataPoints.size(); irow++) {
                    QDataSet dp = dataPoints.get(irow);
                    b.putValues( -1, dp, dp.length() );
                    b.nextRecord();
                }
            }
        }
        return b.getDataSet();
    }
    
    /**
     * returns a data set of the selected table data.  Warning: this used to
     * return a bundle dataset with Y,plane1,plane2,etc that had DEPEND_0 for X.
     * This now returns a bundle ds[n,m] where m is the number of columns and
     * n is the number of records.
     * @return a data set of the selected table data.
     * @see #select(org.das2.datum.DatumRange, org.das2.datum.DatumRange) which selects part of the dataset.
     */
    public QDataSet getSelectedDataSet() {
        int[] selectedRows;
        List<QDataSet> ldataPoints;
        QDataSet lbundleDescriptor;
        synchronized (this) {
            selectedRows= getSelectedRowsInModel();
            ldataPoints= new ArrayList( dataPoints );
            lbundleDescriptor= bundleDescriptor;
        }
        DataSetBuilder b;
        if (selectedRows.length == 0) {
            return null;
        } else {
            b= new DataSetBuilder(2,selectedRows.length,lbundleDescriptor.length());
            b.putProperty( QDataSet.BUNDLE_1, lbundleDescriptor );
            for (int i = 0; i < selectedRows.length; i++) {
                int irow = selectedRows[i];
                if ( irow<ldataPoints.size() ) {
                    QDataSet dp = (QDataSet) ldataPoints.get(irow);
                    b.putValues( -1, dp, dp.length() );
                    b.nextRecord();
                }
            }
            return b.getDataSet();
        }
    }

    /**
     * Selects all the points where the first column is within xrange and
     * the second column is within yrange.
     * @param xrange the range constraint (non-null).
     * @param yrange the range constraint (non-null).
     * return the selected index, or -1 if no elements are found.
     */
    public void select(DatumRange xrange, DatumRange yrange) {
        Datum mid= xrange.rescale( 0.5,0.5 ).min();
        synchronized (dataPoints) {
            List selectMe = new ArrayList();
            int iclosest= -1;
            Datum closestDist=null;
            for (int i = 0; i < dataPoints.size(); i++) {
                QDataSet p = (QDataSet) dataPoints.get(i);
                if ( xrange.contains( DataSetUtil.asDatum(p.slice(0)) ) && yrange.contains(DataSetUtil.asDatum(p.slice(1))) ) {
                    selectMe.add( i );
                }
                if ( closestDist==null || DataSetUtil.asDatum((QDataSet)p.slice(0)).subtract(mid).abs().lt( closestDist ) ) {
                    iclosest= i;
                    closestDist= DataSetUtil.asDatum((QDataSet)p.slice(0)).subtract(mid).abs();
                }
            }
            if ( iclosest!=-1 && selectMe.isEmpty() ) {
                selectMe= Collections.singletonList(iclosest);
            }
            table.getSelectionModel().clearSelection();
            for (int i = 0; i < selectMe.size(); i++) {
                int iselect = ((Integer) selectMe.get(i)).intValue();
                table.getSelectionModel().addSelectionInterval(iselect, iselect);
            }

            if ( selectMe.size()>0 ) {
                int iselect= (Integer)selectMe.get(0);
                table.scrollRectToVisible(new Rectangle(table.getCellRect( iselect, 0, true)) );
            }
        }
    }

    /**
     * This should be called off the event thread.
     * @param file
     * @throws IOException 
     */
    public void saveToFile(File file) throws IOException {
        List<QDataSet> dataPoints1;
        String[] lnamesArray;
        Units[] lunitsArray;
        synchronized (this) {
            lnamesArray= Arrays.copyOf(namesArray,namesArray.length);
            lunitsArray= Arrays.copyOf(unitsArray,unitsArray.length);
            dataPoints1= new ArrayList( dataPoints );
        }
        FileOutputStream out = new FileOutputStream(file);
        BufferedWriter r = new BufferedWriter(new OutputStreamWriter(out));

        try {
            StringBuilder header = new StringBuilder();
            //header.append("## "); // don't use comment characters so that labels and units are used in Autoplot's ascii parser.
            for (int j = 0; j < lnamesArray.length; j++) {
                Units units= lunitsArray[j];
                String sunits;
                if ( UnitsUtil.isTimeLocation(units) ) {
                    sunits= "(UTC)";
                } else if ( UnitsUtil.isOrdinalMeasurement(units) ) {
                    sunits= "(ordinal)";
                } else if ( units==Units.dimensionless ) {
                    sunits= "";
                } else {
                    sunits= "("+units+")";
                }
                header.append( lnamesArray[j] ).append(sunits);
                if ( j<lnamesArray.length-1 ) header.append("\t");
            }
            r.write(header.toString());
            r.newLine();
            for (int i = 0; i < dataPoints1.size(); i++) {
                QDataSet x = (QDataSet) dataPoints1.get(i);
                StringBuilder s = new StringBuilder();
                for (int j = 0; j < x.length(); j++) {
                    Datum d= DataSetUtil.asDatum( x.slice(j) );
                    DatumFormatter formatter = d.getFormatter();
                    s.append( formatter.format( d, lunitsArray[j]).trim() );
                    if ( j<x.length()-1 ) s.append("\t");
                }
                r.write(s.toString());
                r.newLine();
                prefs.put("components.DataPointRecorder.lastFileSave", file.toString());
                prefs.put("components.DataPointRecorder.lastFileLoad", file.toString());
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

    /**
     * load the dataset from the file.  
     * TODO: this should be redone.
     * 
     * @param file
     * @throws IOException 
     */
    public void loadFromFile(File file) throws IOException {

        ProgressMonitor mon= new NullProgressMonitor();

        BufferedReader r=null;

        boolean active0= active;
        
        try {
            active = false;

            int lineCount= lineCount( file );

            r = new BufferedReader( new FileReader( file ) );

            dataPoints.clear();
            String[] namesArray1;
            Units[] unitsArray1 = null;
            QDataSet bundleDescriptor1= null;
            SparseDataSetBuilder bdsb= new SparseDataSetBuilder(2);
            
            if (lineCount > 500) {
                mon = DasProgressPanel.createFramed("reading file");
            }

            // tabs detected in file.
            String delim= ",";
            
            mon.setTaskSize(lineCount);
            mon.started();
            int linenum = 0;
            
            final List<QDataSet> records= new ArrayList<>(1440);
            
            for (String line = r.readLine(); line != null; line = r.readLine()) {
                linenum++;
                if (mon.isCancelled()) {
                    break;
                }
                line= line.trim();
                if ( line.length()==0 ) {
                    continue;
                }
                mon.setTaskProgress(linenum);
                if (line.startsWith("## ") || line.length()>0 && Character.isJavaIdentifierStart( line.charAt(0) ) ) {
                    if ( unitsArray1!=null ) continue;
                    while ( line.startsWith("#") ) line = line.substring(1);
                    if ( !line.contains(delim) ) delim= "\t";
                    if ( !line.contains(delim) ) delim= "\\s+";
                    String[] s = line.split(delim);
                    for ( int i=0; i<s.length; i++ ) {
                        s[i]= s[i].trim();
                    }                    
                    Pattern p = Pattern.compile("(.+)\\((.*)\\)");
                    namesArray1 = new String[s.length];
                    unitsArray1 = new Units[s.length];
                    for (int i = 0; i < s.length; i++) {
                        Matcher m = p.matcher(s[i]);
                        if (m.matches()) {
                            //System.err.printf("%d %s\n", i, m.group(1) );
                            namesArray1[i] = m.group(1).trim();
                            try {
                                if ( m.group(2).trim().equals("UTC") ) {
                                    unitsArray1[i] = Units.cdfTT2000;
                                } else if ( m.group(2).trim().equals("ordinal") ) {
                                    unitsArray1[i] = EnumerationUnits.create("default");
                                } else {
                                    unitsArray1[i] = Units.lookupUnits(m.group(2).trim());
                                }
                            } catch (IndexOutOfBoundsException e) {
                                throw e;
                            }
                        } else {
                            namesArray1[i] = s[i].trim();
                            unitsArray1[i] = Units.dimensionless;
                        }
                        bdsb.putProperty( QDataSet.NAME, i, namesArray1[i] );
                        bdsb.putProperty( QDataSet.UNITS, i, unitsArray1[i] );
                    }
                    bdsb.setLength( s.length );
                    continue;
                }
                String[] s = line.split(delim);
                for ( int i=0; i<s.length; i++ ) {
                    String s1= s[i];
                    s1= s1.trim();
                    if ( s1.startsWith("\"") && s1.endsWith("\"") ) { // pop off quotes used to delimit enumeration e.g. "fuh"
                        s1= s1.substring(1,s1.length()-1);
                    }
                    s[i]= s1;
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
                            if ( s[i].startsWith("0x") ) {
                                unitsArray1[i]= Units.dimensionless;
                            } else {
                                unitsArray1[i] = DatumUtil.parseValid(s[i]).getUnits();
                            }
                        }
                    }
                }

                try {
                    DDataSet rec= DDataSet.createRank1(s.length);
                    for (int i = 0; i < s.length; i++) {
                        if (unitsArray1[i] == null) {
                            Pattern p = Pattern.compile("\"(.*)\".*");
                            Matcher m = p.matcher(s[i]);
                            if (m.matches()) {
                                EnumerationUnits eu= EnumerationUnits.create("default");
                                unitsArray1[i]= eu;
                                rec.putValue(i,eu.createDatum( m.group(1) ).doubleValue(eu));
                            } else {
                                throw new ParseException("parse error, expected \"\"", 0);
                            }
                        } else {
                            try {
                                if ( unitsArray1[i] instanceof EnumerationUnits ) {
                                    EnumerationUnits eu= (EnumerationUnits)unitsArray1[i];
                                    rec.putValue( i,eu.createDatum(  s[i] ).doubleValue(eu)  );
                                } else {
                                    rec.putValue( i, unitsArray1[i].parse(s[i]).doubleValue(unitsArray1[i]) );
                                }
                            } catch (ParseException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }

                    if ( bundleDescriptor1==null ) {
                        bundleDescriptor1= bdsb.getDataSet();
                    }
                    rec.putProperty( QDataSet.BUNDLE_0, bundleDescriptor1 );

                    records.add(rec);
                    
                    //addDataPoint( rec );

                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }

            }

            r.close();

            Runnable run= new Runnable() {
                @Override
                public void run() {
                    for ( QDataSet rec: records ) {
                        addDataPoint( rec );
                    }
                    updateStatus();
                    updateClients();
                    fireDataSetUpdateListenerDataSetUpdated(new DataSetUpdateEvent(this));
                }
            };
            
            saveFile= file;  // go ahead and set this in case client is going to do something with this.

            prefs.put("components.DataPointRecorder.lastFileLoad", file.toString());
            
            if ( SwingUtilities.isEventDispatchThread() ) {
                run.run();
            } else {
                try {
                    SwingUtilities.invokeAndWait(run);
                } catch (InterruptedException | InvocationTargetException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
            
        } finally {

            mon.finished();

            if ( r!=null ) r.close();

            //active = true;
            active= active0;
            modified = false;

            //table.getColumnModel();
            myTableModel.fireTableStructureChanged();
            table.repaint();
        }
        
        if (active) {
            DataSetUpdateEvent ev= new DataSetUpdateEvent(this,getDataSet());
            fireDataSetUpdateListenerDataSetUpdated( ev );
        }

    }

    /**
     * active=true means fire off events on any change.  false= wait for update button.
     * @param active
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
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int[] selectedRows = getSelectedRowsInModel();
                    deleteRows(selectedRows);
                    //for (int i = 0; i < selectedRows.length; i++) {
                    //    deleteRow(selectedRows[i]);
                    //    for (int j = i + 1; j < selectedRows.length; j++) {
                    //        selectedRows[j]--; // indeces change because of deletion
                    //    }
                    //}
                }
            });
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
        final JFileChooser jj = new JFileChooser();
        final Map<String,Integer> statusHolder= new HashMap<>();
        Runnable run= new Runnable() {
            public void run() {
                jj.setFileFilter( new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        if ( pathname==null ) return false; //            rte_1178734273_20140402_133610_wsk, I think this happens on Windows.
                        if ( pathname.isDirectory() ) return true;
                        String fn= pathname.toString();
                        if ( fn==null ) return false; // rte_1178734275.  Bill is still seeing this strange error, which I believe happens on Windows.
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

                statusHolder.put( "status", jj.showSaveDialog(DataPointRecorderNew.this) );
                
            }
        };
        
        if ( SwingUtilities.isEventDispatchThread() ) {
            run.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(run);
            } catch (InterruptedException | InvocationTargetException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
        
        int status= statusHolder.get("status") ;
       
        if (status == JFileChooser.APPROVE_OPTION) {
            try {
                File pathname= jj.getSelectedFile();
                if ( !( pathname.toString().endsWith(".dat") || pathname.toString().endsWith(".txt") ) ) {
                    pathname= new File( pathname.getAbsolutePath() + ".dat" );
                }
                DataPointRecorderNew.this.saveFile = pathname;
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
            if ( i==JOptionPane.OK_OPTION ) {
                return save();
            } else if ( i==JOptionPane.CANCEL_OPTION ) {
                return false;
            } else {
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

                    int status = jj.showOpenDialog(DataPointRecorderNew.this);
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
                    DataPointRecorderNew.this,
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
                    table.repaint();
                }

            }
        };
    }

    private Action getPropertiesAction() {
        return new AbstractAction("Properties") {
            @Override
            public void actionPerformed(ActionEvent e) {
                new PropertyEditor(DataPointRecorderNew.this).showDialog(DataPointRecorderNew.this);
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

    /**
     * Notify listeners that the dataset has updated.  Pressing the "Update" 
     * button calls this.
     */
    public void update() {
        fireDataSetUpdateListenerDataSetUpdated(new DataSetUpdateEvent(this));
        fireSelectedDataSetUpdateListenerDataSetUpdated(new DataSetUpdateEvent(this));
    }
    
    /** Creates a new instance of DataPointRecorder */
    public DataPointRecorderNew() {
        super();
        this.namesArrayLock = new Object();
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
        editMenu.add( new JMenuItem( new AbstractAction("Clear Table Sorting") {
            public void actionPerformed(ActionEvent e) {
                table.setAutoCreateRowSorter(false);
                table.setAutoCreateRowSorter(true);
            }
        } ) );
        JMenuItem mi;
        mi= new JMenuItem( new AbstractAction("Delete Selected Items") {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] selectedRows = getSelectedRowsInModel();
                deleteRows(selectedRows);         
            }
        } );        
        editMenu.add( mi );        
        menuBar.add(editMenu);

        this.add(menuBar, BorderLayout.NORTH);
                
        table = new JTable(myTableModel);
        table.setAutoCreateRowSorter(true); // Java 1.6

        table.getTableHeader().setReorderingAllowed(true);
        table.setColumnModel( new DefaultTableColumnModel() {

            @Override
            public int getColumnCount() {
                synchronized ( namesArrayLock ) {
                    return super.getColumnCount(); //To change body of generated methods, choose Tools | Templates.
                }
            }

            @Override
            public TableColumn getColumn(int columnIndex) {
                synchronized ( namesArrayLock ) {
                    return super.getColumn(columnIndex); //To change body of generated methods, choose Tools | Templates.
                }
            }
            
        });
        table.setRowSelectionAllowed(true);
        table.addMouseListener(new DataPointRecorderNew.MyMouseAdapter(table));
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                fireSelectedDataSetUpdateListenerDataSetUpdated(new DataSetUpdateEvent(DataPointRecorderNew.this));
                int selected = table.getSelectedRow(); // we could do a better job here
                if (selected > -1) {
                    QDataSet dp = dataPoints.get(selected);
                    //System.err.println(dp);
                    Datum x= DataSetUtil.asDatum( dp.slice(0) );
                    Datum y= DataSetUtil.asDatum( dp.slice(1) );
                    DataPointSelectionEvent e2 = new DataPointSelectionEvent(DataPointRecorderNew.this, x, y );
                    e2.setDataSet(dp);
                    fireDataPointSelectionListenerDataPointSelected(e2);
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
        
        messageLabel = new JLabel("ready");
        messageLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);

        controlStatusPanel.add(messageLabel);

        controlPanel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        controlStatusPanel.add(controlPanel);

        this.add(controlStatusPanel, BorderLayout.SOUTH);
    }

    public static DataPointRecorderNew createFramed() {
        DataPointRecorderNew result;
        JFrame frame = new JFrame("Data Point Recorder");
        result = new DataPointRecorderNew();
        frame.getContentPane().add(result);
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        return result;
    }

    /**
     * update fires off the TableDataChanged, and sets the current selected
     * row if necessary.
     */
    private void updateClients() {
        if (active) {
            myTableModel.fireTableDataChanged();
            if (selectRow != -1 && table.getRowCount()>selectRow ) {
                table.setRowSelectionInterval(selectRow, selectRow);
                table.scrollRectToVisible(table.getCellRect(selectRow, 0, true));
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

    private transient Comparator comparator= new Comparator() {
        public int compare(Object o1, Object o2) {
            if ( o1 instanceof QDataSet && o2 instanceof QDataSet ) {
                QDataSet qds1= (QDataSet)o1;
                QDataSet qds2= (QDataSet)o2;
                return DataSetUtil.asDatum(qds1.slice(0)).gt( DataSetUtil.asDatum( qds2.slice(0) ) ) ? 1 : -1;
            } else {
                throw new IllegalArgumentException("expected qdatasets");
            }
        }
    };
    
    /**
     * explicitly declare the number of columns.  Call this and then 
     * setColumn to define each column.
     * @param count the number of columns.
     */
    public void setColumnCount( int count ) {
        namesArray= new String[count];
        unitsArray= new Units[count];
        defaultsArray= new double[count];
        for ( int i=0; i<count; i++ ) {
            namesArray[i]= "field"+i;
            unitsArray[i]= Units.dimensionless;
        }
    }
    
    /**
     * identify the name and unit for each column.
     * @param i the column number 
     * @param name a Java identifier for the column, e.g. "StartTime"
     * @param units units for the column, or null for dimensionless.
     * @param deft default value to use when data is not provided.
     */
    public void setColumn( int i, String name, Units units, Datum deft ) {
        if ( units==null ) units= Units.dimensionless;
        if ( namesArray==null ) {
            throw new IllegalArgumentException("call setColumnCount first.");
        }
        if ( i>=namesArray.length ) {
            throw new IndexOutOfBoundsException("column index is out of bounds (and 0 is the first column)");
        }
        namesArray[i]= name;
        unitsArray[i]= units;
        defaultsArray[i]= deft.doubleValue(units);
    }

    /**
     * identify the name and unit for each column.
     * @param i the column number
     * @param name a Java identifier for the column, e.g. "StartTime"
     * @param units  units units for the column, or null for dimensionless.
     * @param deft default value to use when data is not provided, which must be parseable by units.
     */
    public void setColumn( int i, String name, Units units, String deft ) throws ParseException {
        if ( units==null ) units= Units.dimensionless;
        if ( units instanceof EnumerationUnits ) {
            setColumn( i, name, units, ((EnumerationUnits)units).createDatum(deft) );            
        } else {
            setColumn( i, name, units, units.parse(deft) );
        }
    }
                
    /**
     * identify the name and unit for each column.
     * @param i the column number
     * @param name a Java identifier for the column, e.g. "StartTime"
     * @param units  units units for the column, or null for dimensionless.
     * @param deft default value to use when data is not provided.
     */
    public void setColumn( int i, String name, Units units, double deft ) throws ParseException {
        if ( units==null ) units= Units.dimensionless;
        setColumn( i, name, units, units.createDatum(deft) );
    }
                    
    /**
     * insert the point into the data points.  If the dataset is sorted, then we
     * replace any point that is within X_LIMIT of the point.
     * @param newPoint 
     */
    private void insertInternal( QDataSet newPoint ) {
        int newSelect;
        if ( newPoint.rank()==2 && newPoint.length()==1 ) {
            newPoint= newPoint.slice(0);
        }
        
        // make sure all the units are correct by converting them as they come in.
        ArrayDataSet mnp;
        if ( defaultsArray!=null ) {
            mnp= DDataSet.wrap( Arrays.copyOf(defaultsArray,defaultsArray.length) );
        } else {
            mnp= DDataSet.createRank1(namesArray.length);
        }
        
        QDataSet bds= (QDataSet) newPoint.property(QDataSet.BUNDLE_0);
        
        for ( int i=0; i<newPoint.length(); i++ ) {
            Datum d= DataSetUtil.asDatum( newPoint.slice(i) );
            
            int idx= -1;
            if ( i< bds.length() && i<namesArray.length && bds.property(QDataSet.NAME,i).equals(namesArray[i]) ) {
                idx= i;
            } else {
                for ( int j=0; j<namesArray.length; j++ ) {
                    if ( bds.property(QDataSet.NAME,i).equals(namesArray[j]) ) {
                        idx= j;
                    }
                }
            }
            if ( idx==-1 ) {
                logger.log(Level.FINEST, "unable to find column for {0}", bds.property(QDataSet.NAME,i));
                continue;
            }
            if ( unitsArray[idx].isConvertibleTo(d.getUnits() ) ) {
                mnp.putValue( idx,d.doubleValue( unitsArray[idx] ) );
            } else {
                if ( UnitsUtil.isOrdinalMeasurement(unitsArray[idx]) ) {
                    mnp.putValue( idx, ((EnumerationUnits)unitsArray[idx]).createDatum(d.toString()).doubleValue(unitsArray[idx]));
                } else {
                    throw new InconvertibleUnitsException(d.getUnits(),unitsArray[idx]);
                }
            }
            mnp.putProperty( QDataSet.BUNDLE_0, bundleDescriptor );
        }
        
        newPoint= mnp;
        
        synchronized ( dataPoints ) {
            String[] keys;
            if (sorted) {
                int index = Collections.binarySearch( dataPoints, newPoint, comparator );
                if (index < 0) {
                    QDataSet qds1= null;
                    if ( ~index<dataPoints.size() ) {
                        qds1= (QDataSet)dataPoints.get(~index);
                        keys= DataSetUtil.bundleNames(newPoint);
                        for ( String key : keys ) {
                            if ( DataSetOps.indexOfBundledDataSet( qds1, key )!=-1 ) {
                                logger.log(Level.FINE, "no place to put key: {0}", key);
                            }
                        }
                    }
                    QDataSet dp1= null;
                    if  ( (~index+1)<dataPoints.size() ) { // check for very close point.
                        dp1= (QDataSet)dataPoints.get(~index+1);
                    }
                    
                    Datum epsilon= Units.microseconds.createDatum(10000);
                    if ( SemanticOps.getUnits(newPoint.slice(0)).getOffsetUnits().isConvertibleTo(Units.milliseconds) ) {
                        if ( qds1!=null && Ops.lt( Ops.abs( Ops.subtract( qds1.slice(0), newPoint.slice(0) ) ), epsilon ).value()==1 ) {
                            dataPoints.set( ~index, newPoint );
                        } else if ( dp1!=null && Ops.lt( Ops.abs( Ops.subtract( dp1.slice(0), newPoint.slice(0) ) ), epsilon ).value()==1 ) {
                            dataPoints.set( ~index, newPoint );
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

            } else {
                dataPoints.add(newPoint);
                newSelect = dataPoints.size() - 1;
            }

            selectRow = newSelect;
        }
        modified = true;
        updateStatus();
        updateClients();
        table.repaint();
        myTableModel.fireTableDataChanged();
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
     * @param planes null or additional planes.  Note LinkedHashMap will keep the order of the tabs.
     */
    public void addDataPoint( Datum x, Datum y, Map<String,Object> planes ) {

        if ( planes==null ) planes= Collections.emptyMap();
        
        DDataSet rec= DDataSet.createRank1( 2 + planes.size() );        
        SparseDataSetBuilder bdsb= new SparseDataSetBuilder(2);
        
        int ii= 0;
        
        bdsb.putProperty( QDataSet.NAME, ii, "x" );
        bdsb.putProperty( QDataSet.UNITS, ii, x.getUnits() );
        rec.putValue( ii, x.doubleValue( x.getUnits() ));
        ii++;
        
        bdsb.putProperty( QDataSet.NAME, ii, "y" );
        bdsb.putProperty( QDataSet.UNITS, ii, y.getUnits() );
        rec.putValue( ii, y.doubleValue( y.getUnits() ));
        ii++;
        
        for ( Entry<String,Object> e : planes.entrySet() ) {
            bdsb.putProperty( QDataSet.NAME, ii, e.getKey() );
            Object o= e.getValue();
            Units theu;
            if ( o instanceof String ) {
                Units eu= EnumerationUnits.create("default"); 
                theu= eu;
                try {
                    rec.putValue( ii, theu.parse((String)o).doubleValue(theu) );
                } catch (ParseException ex) {
                    rec.putValue( ii, -1 ); // fill
                }
            } else if ( o instanceof Datum ) {
                theu= ((Datum)o).getUnits();
                rec.putValue( ii, ((Datum)o).doubleValue(theu) );
            } else if ( o instanceof Number ) {           
                theu= Units.dimensionless;
                rec.putValue( ii, ((Number)o).doubleValue() );
            } else if ( o instanceof QDataSet ) {
                theu= SemanticOps.getUnits((QDataSet)o);
                rec.putValue( ii, ((QDataSet)o).value() );
            } else {
                throw new IllegalArgumentException("value must be String, Datum, DataSet or Number");
            }
            bdsb.putProperty( QDataSet.UNITS, ii, theu );
            ii++;
        }
        
        bdsb.setLength(ii);
        
        QDataSet bds= bdsb.getDataSet();
        rec.putProperty(QDataSet.BUNDLE_0,bds);
        
        addDataPoint( rec );
    }
    
    /**
     * add the record to the collection of records.  This should be a
     * rank 1 bundle or 1-record rank 2 bundle.
     *<blockquote><pre>{@code
     *dpr=DataPointRecorder()
     *dpr.addDataPoint( createEvent( '2014-04-23/P1D', 0xFF0000, 'alert' ) )
     *}</pre></blockquote>
     * 
     * @param rec rank 1 qdataset, or 1-record rank 2 dataset (ds[1,n])
     */   
    public void addDataPoint( QDataSet rec ) {
        
        if ( rec.rank()==2 && rec.length()==1 ) {
            rec= rec.slice(0); // Jython createEvent produces rank 2 dataset.
        }
        
        synchronized (dataPoints) {
            if (dataPoints.isEmpty()) {
                QDataSet bds= (QDataSet) rec.property( QDataSet.BUNDLE_0 );
                
                if ( bds==null ) {
                    SparseDataSetBuilder bdsb= new SparseDataSetBuilder(2);
                    Units u= SemanticOps.getUnits(rec);
                    for ( int i=0;i<rec.length();i++ ) {
                        bdsb.putProperty( QDataSet.NAME,i,"ch_"+i );
                        bdsb.putProperty( QDataSet.UNITS,i,u );
                    }
                    bdsb.setLength(rec.length());
                    bds= bdsb.getDataSet();
                }
                
                if ( namesArray==null ) {
                    logger.fine("first record defines columns");
                    Units[] lunitsArray    = new Units[ rec.length() ];
                    String[] lnamesArray   = new String[ rec.length() ];
                
                    for ( int index=0; index<bds.length(); index++ ) {
                        lnamesArray[index] = (String) bds.property(QDataSet.NAME,index);
                        Units u= (Units) bds.property(QDataSet.UNITS,index);
                        lunitsArray[index] = u!=null ? u : Units.dimensionless;
                    }
                
                    unitsArray= lunitsArray;
                    namesArray= lnamesArray;
                }
                
                bundleDescriptor= new AbstractDataSet() {
                    @Override
                    public int rank() {
                        return 2;
                    }
                    @Override
                    public Object property(String name, int i) {
                        if ( name.equals(QDataSet.NAME) ) {
                            return namesArray[i];
                        } else if ( name.equals(QDataSet.UNITS) ) {
                            return unitsArray[i];
                        } else {
                            return null;
                        }
                    }
                    @Override
                    public int length() {
                        return namesArray.length;
                    }
                    @Override
                    public int length(int i) {
                        return 0;
                    }
                };

                myTableModel.fireTableStructureChanged();
                for ( int i=0; i<1; i++ ) { //i<unitsArray.length
                    if ( UnitsUtil.isTimeLocation( unitsArray[i] ) ) {
                        table.getTableHeader().getColumnModel().getColumn(i).setMinWidth( TIME_WIDTH );   
                    }
                }

            }

            insertInternal( rec );
        }
        if (active) {
            DataSetUpdateEvent ev= new DataSetUpdateEvent(this,getDataSet());
            fireDataSetUpdateListenerDataSetUpdated( ev );
        }
 
    }

    public void appendDataSet(VectorDataSet ds) {

        Map planesMap = new LinkedHashMap();

        if (ds.getProperty("comment") != null) {
            planesMap.put("comment", ds.getProperty("comment"));
        }

        if (ds.getProperty("xTagWidth") != null) {
            DataPointRecorderNew.this.xTagWidth = (Datum) ds.getProperty("xTagWidth");
        } else {
            DataPointRecorderNew.this.xTagWidth = Datum.create(0);
        }

        String[] planes = ds.getPlaneIds();

        for (int i = 0; i <
                ds.getXLength(); i++) {
            for (int j = 0; j <
                    planes.length; j++) {
                if (!planes[j].equals("")) {
                    planesMap.put(planes[j], ((VectorDataSet) ds.getPlanarView(planes[j])).getDatum(i));
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


    /**
     * hide the update button if no one is listening.
     * @return true if it is now visible
     */
    private boolean checkUpdateEnable() {
        int listenerList1Count;
        //int selectedListenerListCount;
        listenerList1Count= listenerList1.getListenerCount();

        if ( listenerList1Count>0 ) { // || selectedListenerListCount>0 ) {
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
     * Holds value of property sorted.
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
    public synchronized void addDataPointSelectionListener(org.das2.event.DataPointSelectionListener listener) {
        if (listenerList1 == null) {
            listenerList1 = new javax.swing.event.EventListenerList();
        }
        listenerList1.add(org.das2.event.DataPointSelectionListener.class, listener);
    }

    /**
     * Removes DataPointSelectionListener from the list of listeners.
     * @param listener The listener to remove.
     */
    public synchronized void removeDataPointSelectionListener(org.das2.event.DataPointSelectionListener listener) {
        listenerList1.remove(org.das2.event.DataPointSelectionListener.class, listener);
    }

    /**
     * Notifies all registered listeners about the event.
     *
     * @param event The event to be fired
     */
    private void fireDataPointSelectionListenerDataPointSelected(org.das2.event.DataPointSelectionEvent event) {
        Object[] listeners;
        synchronized (this) {
            if (listenerList1 == null) {
                return;
            }
            listeners = listenerList1.getListenerList();
        }

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

    /**
     * return true when the data point recorder has been modified.
     */
    public boolean isModified() {
        return modified;
    }
    
}
