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
    protected Units[] unitsArray;
    protected String[] planesArray;
    protected AbstractTableModel myTableModel;
    private File saveFile;
    private JLabel messageLabel;
    private boolean active= true; // false means don't fire updates
    
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
        
        Datum get(int i) {
            return data[i];
        }
        
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
        update();
        fireDataSetUpdateListenerDataSetUpdated( new DataSetUpdateEvent(this) );
    }
    
    class MyDataSetDescriptor extends DataSetDescriptor {
        MyDataSetDescriptor() {
            super(null);
        }
        public void fireUpdate() {
            fireDataSetUpdateEvent( new DataSetUpdateEvent(this) );
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
     * @depricated  use getDataSet() and getSelectedDataSet() instead
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
            for ( int irow= 0; irow<dataPoints.size(); irow++ ) {
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
            DasProperties.getInstance().put("components.DataPointRecorder.lastFileSave",  file.toString());
        }
        r.close();
        
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
                            System.err.println(e);
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
                DataPointSelected(e);
            } catch ( ParseException e ) {
                throw new RuntimeException(e);
            }
            
        }
        mon.finished();
        
        active= true;
        update();
        DasProperties.getInstance().put("components.DataPointRecorder.lastFileLoad",  file.toString());
        fireDataSetUpdateListenerDataSetUpdated( new DataSetUpdateEvent(this) );
    }
    
    private class MyMouseAdapter extends MouseAdapter {
        JPopupMenu popup;
        final JTable parent;
        
        MyMouseAdapter(final JTable parent) {
            this.parent= parent;
            popup= new JPopupMenu("Options");
            JMenuItem menuItem= new JMenuItem("Delete Row(s)");
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
                popup.show( e.getComponent(), e.getX(), e.getY());
            }
        }
        public void mouseReleased(MouseEvent e) {
            // hide popup
        }
    }
    
    private Action getSaveAction() {
        return new AbstractAction( "Save..." ) {
            public void actionPerformed(ActionEvent e) {
                JFileChooser jj= new JFileChooser();
                String lastFileString= (String)DasProperties.getInstance().get("components.DataPointRecorder.lastFileSave");
                if ( lastFileString==null ) lastFileString="";
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
    
    private Action getLoadAction() {
        return new AbstractAction( "Load..." ) {
            public void actionPerformed(ActionEvent e) {
                JFileChooser jj= new JFileChooser();
                String lastFileString= (String)DasProperties.getInstance().get("components.DataPointRecorder.lastFileLoad");
                if ( lastFileString==null ) lastFileString="";
                File lastFile= null;
                if ( lastFileString!="" ) {
                    lastFile= new File( lastFileString );
                    jj.setSelectedFile(lastFile);
                }
                int status= jj.showOpenDialog(DataPointRecorder.this);
                if ( status==JFileChooser.APPROVE_OPTION ) {
                    final File loadFile= jj.getSelectedFile();
                    
                    Runnable run= new Runnable() {
                        public void run( ) {
                            try {
                                loadFromFile(loadFile);
                            } catch ( IOException e ) {
                                DasExceptionHandler.handle(e);
                            }
                        }
                    };
                    new Thread( run ).start();
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
        JButton saveButton= new JButton(getSaveAction());
        JButton loadButton= new JButton(getLoadAction());
        
        controlPanel.add(saveButton);
        controlPanel.add(loadButton);
        
        controlPanel.add(new JButton(getPropertiesAction()));
        
        JButton updateButton= new JButton(getUpdateAction());
        
        controlPanel.add(updateButton);
        controlStatusPanel.add(controlPanel);
        //messageLabel= new JLabel("ready");
        //messageLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        //controlStatusPanel.add( messageLabel );
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
    private void update() {
        if ( active ) {
            myTableModel.fireTableDataChanged();
            if ( selectRow!=-1 ) {
                table.setRowSelectionInterval(selectRow,selectRow);
                table.scrollRectToVisible( table.getCellRect( selectRow, 0, true ) );
                selectRow= -1;
            }
        }
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
        insertInternal( new DataPoint( x, y, planes ) );
        if ( active ) fireDataSetUpdateListenerDataSetUpdated( new DataSetUpdateEvent(this) );
    }
    
    /**
     * this adds all the points in the DataSet to the list
     */
    public DataSetUpdateListener getAppendDataSetUpListener() {
        return new DataSetUpdateListener() {
            public void dataSetUpdated( DataSetUpdateEvent e ) {
                VectorDataSet ds= (VectorDataSet)e.getDataSet();
                String comment;
                if ( ds.getProperty("comment")!=null ) {
                    comment= (String)ds.getProperty("comment");
                } else {
                    comment= "";
                }
                if ( ds.getProperty("xTagWidth")!=null ) {
                    DataPointRecorder.this.xTagWidth= (Datum)ds.getProperty("xTagWidth");
                }
                String[] planes= ds.getPlaneIds();
                if ( ds!=null ) {
                    for ( int i=0; i<ds.getXLength(); i++ ) {
                        HashMap planesMap= new HashMap();
                        for ( int j=0; j<planes.length; j++ ) {
                            if ( !planes[j].equals("") ) {
                                planesMap.put( planes[j], ((VectorDataSet)ds.getPlanarView(planes[j])).getDatum(i) );
                            }
                        }
                        planesMap.put("comment", comment);
                        addDataPoint( ds.getXTagDatum(i), ds.getDatum(i), planesMap );
                    }
                    update();
                } else {
                    throw new RuntimeException("not supported, I need the DataSet in the update event");
                }
            }
        };
    }
    
    public void DataPointSelected(edu.uiowa.physics.pw.das.event.DataPointSelectionEvent e) {
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
        update();
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
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i]==edu.uiowa.physics.pw.das.event.DataPointSelectionListener.class) {
                ((edu.uiowa.physics.pw.das.event.DataPointSelectionListener)listeners[i+1]).DataPointSelected(event);
            }
        }
    }
    
    
    /**
     * Holds value of property xTagWidth.
     */
    private Datum xTagWidth;
    
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
     * Getter for property fuzzyOverwrite.
     * @return Value of property fuzzyOverwrite.
     */
    public boolean isSnapToGrid()  {
        
        return this.snapToGrid;
    }
    
    /**
     * Setter for property fuzzyOverwrite.
     * @param fuzzyOverwrite New value of property fuzzyOverwrite.
     */
    public void setSnapToGrid(boolean snapToGrid)  {
        
        this.snapToGrid = snapToGrid;
    }
}
