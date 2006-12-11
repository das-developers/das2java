/*
 * DataPointRecorder.java
 *
 * Created on October 6, 2003, 12:16 PM
 */

package edu.uiowa.physics.pw.das.components;

import edu.uiowa.physics.pw.das.*;
import edu.uiowa.physics.pw.das.components.propertyeditor.PropertyEditor;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.event.*;
import edu.uiowa.physics.pw.das.system.DasLogger;
import edu.uiowa.physics.pw.das.util.*;
import java.awt.BorderLayout;
import java.awt.event.*;
import java.io.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.*;

/**
 *
 * @author  jbf
 */
public class DataPointRecorder extends JPanel implements DataPointSelectionListener {
    protected JTable table;
    protected JScrollPane scrollPane;
    protected List dataPoints;
    private int selectRow; // this row needs to be selected after the update.
    
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
    
    private boolean active= true; // false means don't fire updates
    
    Preferences prefs= Preferences.userNodeForPackage( this.getClass() );
    
    static Logger logger= DasLogger.getLogger( DasLogger.GUI_LOG );
    
    protected class DataPoint implements Comparable {
        Datum[] data;
        HashMap planes;
        
        public DataPoint( Datum x1, Datum x2, HashMap planes ) {
            this( new Datum[] { x1,x2 }, planes );
        }
        
        public DataPoint( Datum[] data, HashMap planes ) {
            this.data= data;
            this.planes= planes;
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
        Object getPlane( String name ) {
            return planes.get(name);
        }
        
        public int compareTo(Object o) {
            DataPoint that = (DataPoint)o;
            return this.data[0].lt(that.data[0]) ? -1 : this.data[0].gt(that.data[0]) ? 1 : 0;
        }
        
        public String toString() {
            StringBuffer result= new StringBuffer( ""+data[0]+" "+data[1] );
            if ( planes!=null ) {
                for ( Iterator i=planes.keySet().iterator(); i.hasNext(); ) {
                    Object key= i.next();
                    result.append(" "+planes.get(key));
                }
            }
            return result.toString();
        }
    }
    
    private class MyTableModel extends AbstractTableModel {
        
        public int getColumnCount() {
            if ( unitsArray==null ) {
                return 2;
            } else {
                return planesArray.length;
            }
        }
        
        public String getColumnName(int j) {
            String result= planesArray[j];
            if ( unitsArray[j]!=null ) result+= "("+unitsArray[j]+")";
            return result;
        }
        
        public int getRowCount() {
            int nrow= dataPoints.size();
            nrow= nrow>0 ? nrow : 1 ;
            return dataPoints.size();
        }
        
        public Object getValueAt(int i, int j) {
            DataPoint x= (DataPoint)dataPoints.get(i);
            if ( j<x.data.length ) {
                return x.get(j).toString(); // formerly one formatter was use for the whole column
            } else {
                return x.getPlane(planesArray[j]);
            }
        }
        
    }
    
    public void deleteRow( int row ) {
        dataPoints.remove(row);
        modified= true;
        updateClients();
        updateStatus();
        fireDataSetUpdateListenerDataSetUpdated( new DataSetUpdateEvent(this) );
    }
    
    class MyDataSetDescriptor extends DataSetDescriptor {
        MyDataSetDescriptor() {
            super(null);
        }
        public void fireUpdate() {
            fireDataSetUpdateEvent( new DataSetUpdateEvent((Object)this) );
        }
        
        protected DataSet getDataSetImpl(Datum s1, Datum s2, Datum s3, DasProgressMonitor monitor) throws DasException {
            if ( dataPoints.size()==0 ) {
                return null;
            } else {
                VectorDataSetBuilder builder = new VectorDataSetBuilder(unitsArray[0],unitsArray[1]);
                for ( int irow= 0; irow<dataPoints.size(); irow++ ) {
                    DataPoint dp= (DataPoint)dataPoints.get(irow);
                    builder.insertY(dp.get(0), dp.get(1));
                }
                return builder.toVectorDataSet();
            }
        }
        
        public Units getXUnits() {
            return unitsArray[0];
        }
        
    }
    private MyDataSetDescriptor dataSetDescriptor;
    
    /**
     * @deprecated  use getDataSet() and getSelectedDataSet() instead
     */
    public DataSetDescriptor getDataSetDescriptor() {
        if ( dataSetDescriptor==null ) {
            dataSetDescriptor= new MyDataSetDescriptor();
        }
        return dataSetDescriptor;
    }
    
    /**
     * returns a data set of the table data.
     */
    public VectorDataSet getDataSet() {
        if ( dataPoints.size()==0 ) {
            return null;
        } else {
            VectorDataSetBuilder builder = new VectorDataSetBuilder(unitsArray[0],unitsArray[1]);
            for ( int i=2; i<planesArray.length; i++ ) {
                if ( unitsArray[i]!=null ) builder.addPlane( planesArray[i], unitsArray[i] );
            }
            for ( int irow= 0; irow<dataPoints.size(); irow++ ) {
                DataPoint dp= (DataPoint)dataPoints.get(irow);
                builder.insertY(dp.get(0), dp.get(1));
                for ( int i=2; i<planesArray.length; i++ ) {
                    if ( unitsArray[i]!=null ) builder.insertY( dp.get(0), (Datum)dp.getPlane(planesArray[i]), planesArray[i] );
                }
            }
            if ( this.xTagWidth!=null ) {
                builder.setProperty( "xTagWidth", xTagWidth );
            }
            return builder.toVectorDataSet();
        }
    }
    
    /**
     * returns a data set of the selected table data
     */
    public VectorDataSet getSelectedDataSet() {
        int[] selectedRows= table.getSelectedRows();
        if ( selectedRows.length==0 ) {
            return null;
        } else {
            VectorDataSetBuilder builder = new VectorDataSetBuilder(unitsArray[0],unitsArray[1]);
            for ( int i=0; i<selectedRows.length; i++ ) {
                int irow= selectedRows[i];
                DataPoint dp= (DataPoint)dataPoints.get(irow);
                builder.insertY(dp.get(0), dp.get(1));
            }
            if ( this.xTagWidth!=null ) {
                builder.setProperty( "xTagWidth", xTagWidth );
            }
            return builder.toVectorDataSet();
        }
    }
    
    /**
     * Selects all the points within the DatumRange
     */
    public void select( DatumRange xrange, DatumRange yrange ) {
        List selectMe= new ArrayList();
        for ( int i=0; i<dataPoints.size(); i++ ) {
            DataPoint p= (DataPoint)dataPoints.get(i);
            if ( xrange.contains( p.data[0] )
            && yrange.contains( p.data[1]  ) ) {
                selectMe.add(new Integer(i));
            }
        }
        table.getSelectionModel().clearSelection();
        for ( int i=0; i<selectMe.size(); i++ ) {
            int iselect= ((Integer)selectMe.get(i)).intValue();
            table.getSelectionModel().addSelectionInterval( iselect, iselect );
        }
    }
    
    public void saveToFile( File file ) throws IOException {
        FileOutputStream out= new FileOutputStream( file );
        BufferedWriter r= new BufferedWriter(new OutputStreamWriter(out));
        
        StringBuffer header= new StringBuffer();
        header.append("## ");
        for ( int j=0; j<planesArray.length; j++ ) {
            header.append( myTableModel.getColumnName(j) + "\t" );
        }
        r.write(header.toString());
        r.newLine();
        for ( int i=0; i<dataPoints.size(); i++ ) {
            DataPoint x= (DataPoint)dataPoints.get(i);
            StringBuffer s= new StringBuffer();
            for ( int j=0; j<2; j++ ) {
                s.append( String.valueOf(x.get(j)) + "\t");
            }
            for ( int j=2; j<planesArray.length; j++ ) {
                Object o= x.getPlane(planesArray[j]);
                if ( unitsArray[j]==null ) {
                    if ( o==null ) o="";
                    s.append( "\"" + o +"\"\t" );
                } else {
                    s.append( o+"\t" );
                }
            }
            r.write(s.toString());
            r.newLine();
            prefs.put( "components.DataPointRecorder.lastFileSave",  file.toString() );
            prefs.put( "components.DataPointRecorder.lastFileLoad",  file.toString() );
        }
        r.close();
        modified= false;
        updateStatus();
        
    }
    
    public void loadFromFile( File file ) throws IOException {
        active= false;
        
        FileInputStream in= new FileInputStream( file );
        BufferedReader r= new BufferedReader(new InputStreamReader(in));
        
        int lineCount=0;
        for ( String line= r.readLine(); line!=null; line= r.readLine() ) {
            lineCount++;
        }
        r.close();
        
        in= new FileInputStream( file );
        r= new BufferedReader(new InputStreamReader(in));
        
        dataPoints.clear();
        String[] planesArray= null;
        Units[] unitsArray= null;
        
        Datum x;
        Datum y;
        HashMap planes= new HashMap();
        
        DasProgressMonitor mon;
        if ( lineCount > 500 ) {
            mon= DasProgressPanel.createFramed("reading file");
        } else {
            mon= DasProgressMonitor.NULL;
        }
        
        mon.setTaskSize( lineCount );
        mon.started();
        int linenum=0;
        for ( String line= r.readLine(); line!=null; line= r.readLine() ) {
            linenum++;
            if ( mon.isCancelled() ) break;
            mon.setTaskProgress(linenum);
            if ( line.startsWith("## ") ) {
                line= line.substring(3);
                String[] s= line.split("\t");
                Pattern p= Pattern.compile("(.+)\\((.*)\\)");
                planesArray= new String[s.length];
                unitsArray= new Units[s.length];
                for ( int i=0; i<s.length; i++ ) {
                    Matcher m= p.matcher(s[i]);
                    if ( m.matches() ) {
                        planesArray[i]= m.group(1);
                        try {
                            unitsArray[i]= Units.getByName(m.group(2));
                        } catch ( IndexOutOfBoundsException e ) {
                            throw e;
                        }
                    } else {
                        planesArray[i]= s[i];
                        unitsArray[i]= null;
                    }
                }
                continue;
            }
            String[] s= line.split("\t");
            if ( unitsArray==null ) {
                // support for legacy files
                unitsArray= new Units[s.length];
                for ( int i=0; i<s.length; i++ ) {
                    if ( s[i].charAt(0)=='"' ) {
                        unitsArray[i]= null;
                    } else if ( TimeUtil.isValidTime(s[i]) ) {
                        unitsArray[i]= Units.us2000;
                    } else {
                        unitsArray[i]= DatumUtil.parseValid(s[i]).getUnits();
                    }
                }
                planesArray= new String[] { "X", "Y", "comment" };
            }
            
            try {
                
                x= unitsArray[0].parse(s[0]);
                y= unitsArray[1].parse(s[1]);
                
                planes= new HashMap();
                
                for ( int i=2; i<s.length; i++ ) {
                    if ( unitsArray[i]==null ) {
                        Pattern p= Pattern.compile("\"(.*)\".*");
                        Matcher m= p.matcher(s[i]);
                        if ( m.matches() ) {
                            planes.put( planesArray[i], m.group(1) );
                        } else {
                            throw new ParseException( "parse error, expected \"\"", 0 );
                        }
                    } else {
                        try {
                            planes.put( planesArray[i], unitsArray[i].parse(s[i]) );
                        } catch ( ParseException e ) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                
                DataPointSelectionEvent e;
                e= new DataPointSelectionEvent(this, x, y, planes );
                dataPointSelected(e);
            } catch ( ParseException e ) {
                throw new RuntimeException(e);
            }
            
        }
        mon.finished();
        
        active= true;
        
        modified= false;
        updateStatus();
        updateClients();
        
        prefs.put("components.DataPointRecorder.lastFileLoad",  file.toString());
        fireDataSetUpdateListenerDataSetUpdated( new DataSetUpdateEvent(this) );
        
        table.getColumnModel().getColumn(0).setPreferredWidth( 200 );
    }
    
    private class MyMouseAdapter extends MouseAdapter {
        JPopupMenu popup;
        JMenuItem menuItem;
        final JTable parent;
        
        MyMouseAdapter(final JTable parent) {
            this.parent= parent;
            popup= new JPopupMenu("Options");
            menuItem= new JMenuItem("Delete Row(s)");
            menuItem.addActionListener( new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int[] selectedRows= parent.getSelectedRows();
                    for ( int i=0; i<selectedRows.length; i++ ) {
                        deleteRow(selectedRows[i]);
                        for ( int j=i+1; j<selectedRows.length; j++ ) {
                            selectedRows[j]--; // indeces change because of deletion
                        }
                    }
                }
            });
            popup.add(menuItem);
        }
        
        MouseAdapter mm;
        public void mousePressed(MouseEvent e) {
            if ( e.getButton()==MouseEvent.BUTTON3 ) {
                int rowCount= parent.getSelectedRows().length;
                menuItem.setText( "Delete "+rowCount+" Row"+( rowCount!=1 ? "s" : "" ) );
                popup.show( e.getComponent(), e.getX(), e.getY());
            }
        }
        public void mouseReleased(MouseEvent e) {
            // hide popup
        }
    }
    
    private Action getSaveAsAction() {
        return new AbstractAction( "Save As..." ) {
            public void actionPerformed(ActionEvent e) {
                JFileChooser jj= new JFileChooser();
                String lastFileString= prefs.get("components.DataPointRecorder.lastFileSave","");
                File lastFile= null;
                if ( lastFileString!="" ) {
                    lastFile= new File( lastFileString );
                    jj.setSelectedFile(lastFile);
                }
                
                int status= jj.showSaveDialog(DataPointRecorder.this);
                if ( status==JFileChooser.APPROVE_OPTION ) {
                    try {
                        DataPointRecorder.this.saveFile= jj.getSelectedFile();
                        saveToFile(saveFile);
                        //messageLabel.setText("saved data to "+saveFile);
                    } catch ( IOException e1 ) {
                        DasExceptionHandler.handle(e1);
                    }
                }
            }
        };
    }
    
    private Action getSaveAction() {
        return new AbstractAction( "Save" ) {
            public void actionPerformed( ActionEvent e ) {
                if ( saveFile==null ) {
                    getSaveAsAction().actionPerformed(e);
                } else {
                    try {
                        saveToFile(saveFile);
                    } catch (IOException ex) {
                        DasExceptionHandler.handle( ex );
                    }
                }
            }
        };
    }
    
    private Action getLoadAction() {
        return new AbstractAction( "Open..." ) {
            public void actionPerformed(ActionEvent e) {
                if ( checkModified(e) ) {
                    JFileChooser jj= new JFileChooser();
                    String lastFileString= prefs.get("components.DataPointRecorder.lastFileLoad","");
                    File lastFile= null;
                    if ( lastFileString!="" ) {
                        lastFile= new File( lastFileString );
                        jj.setSelectedFile(lastFile);
                    }
                    int status= jj.showOpenDialog(DataPointRecorder.this);
                    if ( status==JFileChooser.APPROVE_OPTION ) {
                        final File loadFile= jj.getSelectedFile();
                        prefs.put("components.DataPointRecorder.lastFileLoad",loadFile.toString());
                        Runnable run= new Runnable() {
                            public void run( ) {
                                try {
                                    loadFromFile(loadFile);
                                    saveFile= loadFile;
                                    updateStatus();
                                } catch ( IOException e ) {
                                    DasExceptionHandler.handle(e);
                                }
                            }
                        };
                        new Thread( run ).start();
                    }
                }
            }
        };
    }
    
    /**
     * returns true if the operation should continue, false
     * if not, meaning the user pressed cancel.
     */
    private boolean checkModified( ActionEvent e ) {
        if ( modified ) {
            int n = JOptionPane.showConfirmDialog(
                    DataPointRecorder.this,
                    "Current work has not been saved.\n  Save first?",
                    "Save work first",
                    JOptionPane.YES_NO_CANCEL_OPTION );
            if ( n==JOptionPane.YES_OPTION ) {
                getSaveAction().actionPerformed(e);
            }
            return ( n != JOptionPane.CANCEL_OPTION );
        } else {
            return true;
        }
    }
    
    private Action getNewAction() {
        return new AbstractAction( "New" ) {
            public void actionPerformed( ActionEvent e ) {
                if ( checkModified( e ) ) {
                    dataPoints.removeAll(dataPoints);
                    saveFile= null;
                    updateStatus();
                    updateClients();
                }
            }
        };
    }
    
    private Action getPropertiesAction() {
        return new AbstractAction( "Properties" ) {
            public void actionPerformed(ActionEvent e) {
                new PropertyEditor( DataPointRecorder.this ).showDialog(DataPointRecorder.this);
            }
        };
    }
    
    private Action getUpdateAction() {
        return new AbstractAction("Update"){
            public void actionPerformed(ActionEvent e) {
                // what if no one is listening?
                if ( dataSetDescriptor!=null ) {
                    dataSetDescriptor.fireUpdate();
                }
                fireDataSetUpdateListenerDataSetUpdated(new DataSetUpdateEvent(this));
                fireSelectedDataSetUpdateListenerDataSetUpdated(new DataSetUpdateEvent(this));
            }
        };
    }
    
    /** Creates a new instance of DataPointRecorder */
    public DataPointRecorder() {
        super();
        dataPoints= new ArrayList();
        myTableModel= new MyTableModel();
        this.setLayout(new BorderLayout());
        
        JMenuBar menuBar= new JMenuBar();
        JMenu fileMenu= new JMenu( "File" );
        fileMenu.add( new JMenuItem( getNewAction() ) );
        fileMenu.add( new JMenuItem( getLoadAction() ) );
        fileMenu.add( new JMenuItem( getSaveAction() ) );
        fileMenu.add( new JMenuItem( getSaveAsAction() ) );
        menuBar.add( fileMenu );
        
        JMenu editMenu= new JMenu( "Edit" );
        editMenu.add( new JMenuItem( getPropertiesAction() ) );
        menuBar.add( editMenu );
        
        this.add( menuBar, BorderLayout.NORTH );
        
        planesArray= new String[] { "X", "Y" };
        unitsArray= new Units[] { null, null };
        
        table= new JTable(myTableModel);
        
        table.getTableHeader().setReorderingAllowed(false);
        
        table.setRowSelectionAllowed(true);
        table.addMouseListener(new DataPointRecorder.MyMouseAdapter(table));
        table.getSelectionModel().addListSelectionListener( new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                fireSelectedDataSetUpdateListenerDataSetUpdated( new DataSetUpdateEvent(DataPointRecorder.this) );
                int selected= table.getSelectedRow(); // we could do a better job here
                if ( selected>-1 ) {
                    DataPoint dp= (DataPoint)dataPoints.get(selected);
                    DataPointSelectionEvent e2= new DataPointSelectionEvent( DataPointRecorder.this, dp.get(0), dp.get(1) );
                    fireDataPointSelectionListenerDataPointSelected( e2 );
                }
            }
        });
        
        scrollPane= new JScrollPane(table);
        this.add(scrollPane,BorderLayout.CENTER);
        
        JPanel controlStatusPanel= new JPanel();
        controlStatusPanel.setLayout( new BoxLayout( controlStatusPanel, BoxLayout.Y_AXIS ) );
        
        final JPanel controlPanel= new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel,BoxLayout.X_AXIS));
        
        JButton updateButton= new JButton(getUpdateAction());
        
        controlPanel.add(updateButton);
        
        messageLabel= new JLabel("ready");
        messageLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        
        controlStatusPanel.add( messageLabel );
        
        controlPanel.setAlignmentX( JLabel.LEFT_ALIGNMENT );
        controlStatusPanel.add(controlPanel);
        
        this.add(controlStatusPanel,BorderLayout.SOUTH);
    }
    
    public static DataPointRecorder createFramed() {
        DataPointRecorder result;
        JFrame frame= new JFrame("Data Point Recorder");
        result= new DataPointRecorder();
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
        if ( active ) {
            myTableModel.fireTableDataChanged();
            if ( selectRow!=-1 ) {
                table.setRowSelectionInterval(selectRow,selectRow);
                table.scrollRectToVisible( table.getCellRect( selectRow, 0, true ) );
                selectRow= -1;
            }
        }
    }
    
    private void updateStatus() {
        String statusString= ( saveFile==null ? "" : ( String.valueOf( saveFile ) + " " )  )  +
                ( modified ? "(modified)" : "" );
        messageLabel.setText( statusString );
    }
    private void insertInternal( DataPoint newPoint ) {
        int newSelect;
        if ( sorted ) {
            int index = Collections.binarySearch(dataPoints, newPoint);
            if (index < 0) {
                dataPoints.add(~index, newPoint);
                newSelect= ~index;
            } else {
                dataPoints.set(index, newPoint);
                newSelect= index;
            }
        } else {
            dataPoints.add(newPoint);
            newSelect= dataPoints.size()-1;
        }
        selectRow= newSelect;
        modified= true;
        updateStatus();
    }
    
    private void addDataPoint( Datum x, Datum y, HashMap planes ) {
        if ( dataPoints.size()==0 ) {
            Datum[] datums= new Datum[] { x, y };
            unitsArray= new Units[2+planes.size()];
            unitsArray[0]= x.getUnits();
            unitsArray[1]= y.getUnits();
            planesArray= new String[2+planes.size()];
            planesArray[0]="x";
            planesArray[1]="y";
            int index=2;
            for ( Iterator i= planes.keySet().iterator(); i.hasNext(); ) {
                Object key= i.next();
                planesArray[index]= String.valueOf(key);
                Object value= planes.get(key);
                if ( value instanceof String ) {
                    unitsArray[index]= null;
                } else {
                    unitsArray[index]= ((Datum)value).getUnits();
                }
                index++;
            }
            myTableModel.fireTableStructureChanged();
        }
        if ( !x.getUnits().isConvertableTo(unitsArray[0]) ) {
            throw new RuntimeException( "inconvertable units: "+x+" expected "+unitsArray[0] );
        }
        if ( !y.getUnits().isConvertableTo(unitsArray[1]) ) {
            throw new RuntimeException( "inconvertable units: "+y+" expected "+unitsArray[1] );
        }
        insertInternal( new DataPoint( x, y, new HashMap(planes) ) );
        if ( active ) fireDataSetUpdateListenerDataSetUpdated( new DataSetUpdateEvent(this) );
    }
    
    public void appendDataSet( VectorDataSet ds ) {
        
        String comment;
        
        HashMap planesMap= new HashMap();
        
        if ( ds.getProperty("comment")!=null ) {
            planesMap.put( "comment", ds.getProperty("comment") );
        }
        
        if ( ds.getProperty("xTagWidth")!=null ) {
            DataPointRecorder.this.xTagWidth= (Datum)ds.getProperty("xTagWidth");
        }
        String[] planes= ds.getPlaneIds();
        
        for ( int i=0; i<ds.getXLength(); i++ ) {
            for ( int j=0; j<planes.length; j++ ) {
                if ( !planes[j].equals("") ) {
                    planesMap.put( planes[j], ((VectorDataSet)ds.getPlanarView(planes[j])).getDatum(i) );
                }
            }
            addDataPoint( ds.getXTagDatum(i), ds.getDatum(i), planesMap );
        }
        updateClients();
        
    }
    
    
    /**
     * this adds all the points in the DataSet to the list.  This will also check the dataset for the special
     * property "comment" and add it as a comment.
     */
    public DataSetUpdateListener getAppendDataSetUpListener() {
        return new DataSetUpdateListener() {
            public void dataSetUpdated( DataSetUpdateEvent e ) {
                VectorDataSet ds= (VectorDataSet)e.getDataSet();
                if ( ds==null ) {
                    throw new RuntimeException("not supported, I need the DataSet in the update event");
                } else {
                    appendDataSet( (VectorDataSet)e.getDataSet() );
                }
            }
        };
    }
    
    public void dataPointSelected(edu.uiowa.physics.pw.das.event.DataPointSelectionEvent e) {
        String comment="";
        HashMap planesMap;
        if ( e instanceof CommentDataPointSelectionEvent ) {
            comment= ((CommentDataPointSelectionEvent)e).getComment();
            planesMap= new HashMap();
            planesMap.put( "comment", comment );
        } else {
            String[] x= e.getPlaneIds();
            planesMap= new HashMap();
            for ( int i=0; i<x.length; i++ ) {
                planesMap.put( x[i], e.getPlane(x[i]) );
            }
        }
        
        // if a point exists within xTagWidth of the point, then have this point replace
        Datum x= e.getX();
        if ( snapToGrid && xTagWidth!=null && dataPoints.size()>0 ) {
            DataSet ds= getDataSet();
            int i= DataSetUtil.closestColumn( ds, e.getX() );
            Datum diff= e.getX().subtract(ds.getXTagDatum(i));
            if ( Math.abs(diff.divide(xTagWidth).doubleValue(Units.dimensionless))<0.5 ) {
                x= ds.getXTagDatum(i);
            }
        }
        addDataPoint( x, e.getY(), planesMap );
        updateClients();
    }
    
    private javax.swing.event.EventListenerList listenerList =  null;
    
    public synchronized void addDataSetUpdateListener(edu.uiowa.physics.pw.das.dataset.DataSetUpdateListener listener) {
        if (listenerList == null ) {
            listenerList = new javax.swing.event.EventListenerList();
        }
        listenerList.add(edu.uiowa.physics.pw.das.dataset.DataSetUpdateListener.class, listener);
    }
    
    public synchronized void removeDataSetUpdateListener(edu.uiowa.physics.pw.das.dataset.DataSetUpdateListener listener) {
        listenerList.remove(edu.uiowa.physics.pw.das.dataset.DataSetUpdateListener.class, listener);
    }
    
    private void fireDataSetUpdateListenerDataSetUpdated(edu.uiowa.physics.pw.das.dataset.DataSetUpdateEvent event) {
        if (listenerList == null) return;
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i]==edu.uiowa.physics.pw.das.dataset.DataSetUpdateListener.class) {
                ((edu.uiowa.physics.pw.das.dataset.DataSetUpdateListener)listeners[i+1]).dataSetUpdated(event);
            }
        }
    }
    
    private javax.swing.event.EventListenerList selectedListenerList =  null;
    
    public synchronized void addSelectedDataSetUpdateListener(edu.uiowa.physics.pw.das.dataset.DataSetUpdateListener listener) {
        if (selectedListenerList == null ) {
            selectedListenerList = new javax.swing.event.EventListenerList();
        }
        selectedListenerList.add(edu.uiowa.physics.pw.das.dataset.DataSetUpdateListener.class, listener);
    }
    
    public synchronized void removeSelectedDataSetUpdateListener(edu.uiowa.physics.pw.das.dataset.DataSetUpdateListener listener) {
        selectedListenerList.remove(edu.uiowa.physics.pw.das.dataset.DataSetUpdateListener.class, listener);
    }
    
    private void fireSelectedDataSetUpdateListenerDataSetUpdated(edu.uiowa.physics.pw.das.dataset.DataSetUpdateEvent event) {
        if (selectedListenerList == null) return;
        Object[] listeners = selectedListenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i]==edu.uiowa.physics.pw.das.dataset.DataSetUpdateListener.class) {
                ((edu.uiowa.physics.pw.das.dataset.DataSetUpdateListener)listeners[i+1]).dataSetUpdated(event);
            }
        }
    }
    
    /**
     * Holds value of property sorted.
     */
    private boolean sorted= true;
    
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
    public synchronized void addDataPointSelectionListener(edu.uiowa.physics.pw.das.event.DataPointSelectionListener listener) {
        
        if (listenerList == null ) {
            listenerList = new javax.swing.event.EventListenerList();
        }
        listenerList.add(edu.uiowa.physics.pw.das.event.DataPointSelectionListener.class, listener);
    }
    
    /**
     * Removes DataPointSelectionListener from the list of listeners.
     * @param listener The listener to remove.
     */
    public synchronized void removeDataPointSelectionListener(edu.uiowa.physics.pw.das.event.DataPointSelectionListener listener) {
        
        listenerList.remove(edu.uiowa.physics.pw.das.event.DataPointSelectionListener.class, listener);
    }
    
    /**
     * Notifies all registered listeners about the event.
     *
     * @param event The event to be fired
     */
    private void fireDataPointSelectionListenerDataPointSelected(edu.uiowa.physics.pw.das.event.DataPointSelectionEvent event) {
        if (listenerList == null) return;
        logger.fine("firing data point selection event");
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i]==edu.uiowa.physics.pw.das.event.DataPointSelectionListener.class) {
                ((edu.uiowa.physics.pw.das.event.DataPointSelectionListener)listeners[i+1]).dataPointSelected(event);
            }
        }
    }
    
    
    /**
     * Holds value of property xTagWidth.
     */
    private Datum xTagWidth= null;
    
    /**
     * Getter for property xTagWidth.
     * @return Value of property xTagWidth.
     */
    public Datum getXTagWidth() {
        return this.xTagWidth;
    }
    
    /**
     * Setter for property xTagWidth.
     * @param xTagWidth New value of property xTagWidth.
     */
    public void setXTagWidth(Datum xTagWidth) {
        this.xTagWidth = xTagWidth;
    }
    
    /**
     * Holds value of property snapToGrid.
     */
    private boolean snapToGrid= false;
    
    /**
     * Getter for property snapToGrid.
     * @return Value of property snapToGrid.
     */
    public boolean isSnapToGrid()  {
        
        return this.snapToGrid;
    }
    
    /**
     * Setter for property snapToGrid.  true indicates the xtag will be reset
     * so that the tags are equally spaced, each xTagWidth apart.
     * @param snapToGrid New value of property snapToGrid.
     */
    public void setSnapToGrid(boolean snapToGrid)  {
        
        this.snapToGrid = snapToGrid;
    }
}
