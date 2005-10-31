/*
 * DataPointRecorder.java
 *
 * Created on October 6, 2003, 12:16 PM
 */

package edu.uiowa.physics.pw.das.components;

import edu.uiowa.physics.pw.das.*;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.datum.format.*;
import edu.uiowa.physics.pw.das.event.*;
import edu.uiowa.physics.pw.das.util.*;
import java.awt.BorderLayout;
import java.awt.event.*;
import java.io.*;
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
    protected DataPointReporter reporter;
    protected List dataPoints;
    protected Units[] unitsArray;
    protected DatumFormatter[] formatterArray;
    protected AbstractTableModel myTableModel;
    
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
                return 3;
            } else {
                return unitsArray.length+1;  // +1 is for the comment column
            }
        }
        
        public String getColumnName(int j) {
            if ( j+1 == getColumnCount() ) {
                return "comment";
            } else {
                if ( unitsArray!=null ) {
                    Units unit= unitsArray[j];
                    if ( unit instanceof TimeLocationUnits ) {
                        return ((TimeLocationUnits)unit).getTimeZone();
                    } else {
                        return unitsArray[j].toString();
                    }
                }  else {
                    return ""+j;
                }
            }
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
                return x.getPlane("comment");
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
            return builder.toVectorDataSet();
        }
    }
    
    public void saveToFile( File file ) throws IOException {
        FileOutputStream out= new FileOutputStream( file );
        BufferedWriter r= new BufferedWriter(new OutputStreamWriter(out));
        
        for ( int i=0; i<dataPoints.size(); i++ ) {
            DataPoint x= (DataPoint)dataPoints.get(i);
            StringBuffer s= new StringBuffer();
            for ( int j=0; j<unitsArray.length; j++ ) {
                s.append(formatterArray[j].format( x.get(j) ) + "\t");
            }
            String comment= (String)x.getPlane("comment");
            if ( comment==null ) comment="";
            s.append( "\""+comment+"\"" );
            r.write(s.toString());
            r.newLine();
            DasProperties.getInstance().put("components.DataPointRecorder.lastFileSave",  file.toString());
        }
        r.close();
        
    }
    
    public void loadFromFile( File file ) throws IOException {
        FileInputStream in= new FileInputStream( file );
        BufferedReader r= new BufferedReader(new InputStreamReader(in));
        dataPoints.clear();
        for ( String line= r.readLine(); line!=null; line= r.readLine() ) {
            String[] s= line.split("\t");
            
            Datum x;
            if ( TimeUtil.isValidTime(s[0]) ) x= TimeUtil.createValid(s[0]); else x=DatumUtil.parseValid(s[0]);
            Datum y;
            if ( TimeUtil.isValidTime(s[1]) ) y= TimeUtil.createValid(s[1]); else y=DatumUtil.parseValid(s[1]);
            String comment="";
            if ( s.length>2 ) {
                comment= s[2];
                Pattern p= Pattern.compile("\"(.*)\".*");
                Matcher m= p.matcher(comment);
                if ( m.matches() ) {
                    comment= m.group(1);
                }
            }
            
            DataPointSelectionEvent e;
            if ( comment.equals("") ) {
                e= new DataPointSelectionEvent(this, x, y );
            } else {
                e= CommentDataPointSelectionEvent.create( new DataPointSelectionEvent(this, x, y), comment );
            }
            DataPointSelected(e);
        }
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
                        saveToFile(jj.getSelectedFile());
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
                    try {
                        loadFromFile(jj.getSelectedFile());
                    } catch ( IOException e1 ) {
                        DasExceptionHandler.handle(e1);
                    }
                }
            }
        };
    }
    
    /** Creates a new instance of DataPointRecorder */
    public DataPointRecorder() {
        super();
        dataPoints= new ArrayList();
        myTableModel= new MyTableModel();
        this.setLayout(new BorderLayout());
        table= new JTable(myTableModel);
        
        table.getTableHeader().setReorderingAllowed(false);
        
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
        
        final JPanel controlPanel= new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel,BoxLayout.X_AXIS));
        JButton saveButton= new JButton(getSaveAction());
        JButton loadButton= new JButton(getLoadAction());
        
        controlPanel.add(saveButton);
        controlPanel.add(loadButton);
        
        JButton updateButton= new JButton("Update");
        updateButton.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // what if no one is listening?
                if ( dataSetDescriptor!=null ) {
                    dataSetDescriptor.fireUpdate();
                }
                fireDataSetUpdateListenerDataSetUpdated(new DataSetUpdateEvent(this));
                fireSelectedDataSetUpdateListenerDataSetUpdated(new DataSetUpdateEvent(this));
            }
        } );
        
        controlPanel.add(updateButton);
        
        this.add(controlPanel,BorderLayout.SOUTH);
    }
    
    public static DataPointRecorder createFramed() {
        DataPointRecorder result;
        JFrame frame= new JFrame("Data Point Recorder");
        result= new DataPointRecorder();
        frame.getContentPane().add(result);
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        return result;
    }
    
    
    protected void update() {
        myTableModel.fireTableDataChanged();
        JScrollBar scrollBar= scrollPane.getVerticalScrollBar();
        if ( scrollBar!=null ) scrollBar.setValue(scrollBar.getMaximum());
    }
    
    private void insertInternal( DataPoint newPoint ) {
        if ( sorted ) {
            int index = Collections.binarySearch(dataPoints, newPoint);
            if (index < 0) {
                dataPoints.add(~index, newPoint);
            } else {
                dataPoints.set(index, newPoint);
            }
        } else {
            dataPoints.add(newPoint);
        }
    }
    
    private void addDataPoint( Datum x, Datum y, HashMap planes ) {
        if ( dataPoints.size()==0 ) {
            Datum[] datums= new Datum[] { x, y };
            unitsArray= new Units[] { x.getUnits(), y.getUnits() };
            formatterArray= new DatumFormatter[] { x.getFormatter(), y.getFormatter() };
            for ( int j=0; j<unitsArray.length; j++ ) {
                if ( unitsArray[j] instanceof TimeLocationUnits ) {
                    formatterArray[j]= TimeDatumFormatterFactory.getInstance().defaultFormatter();
                }
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
        fireDataSetUpdateListenerDataSetUpdated( new DataSetUpdateEvent(this) );
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
                String[] planes= ds.getPlaneIds();
                if ( ds!=null ) {
                    for ( int i=0; i<ds.getXLength(); i++ ) {
                        HashMap planesMap= new HashMap();
                        for ( int j=0; j<planes.length; j++ ) {
                            planesMap.put( planes[j], ((VectorDataSet)ds.getPlanarView(planes[j])).getDatum(i) );
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
        if ( e instanceof CommentDataPointSelectionEvent ) {
            comment= ((CommentDataPointSelectionEvent)e).getComment();
        }
        HashMap planesMap= new HashMap();
        planesMap.put( "comment", comment );
        addDataPoint( e.getX(), e.getY(), planesMap );
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
        listenerList.add (edu.uiowa.physics.pw.das.event.DataPointSelectionListener.class, listener);
    }

    /**
     * Removes DataPointSelectionListener from the list of listeners.
     * @param listener The listener to remove.
     */
    public synchronized void removeDataPointSelectionListener(edu.uiowa.physics.pw.das.event.DataPointSelectionListener listener) {

        listenerList.remove (edu.uiowa.physics.pw.das.event.DataPointSelectionListener.class, listener);
    }

    /**
     * Notifies all registered listeners about the event.
     * 
     * @param event The event to be fired
     */
    private void fireDataPointSelectionListenerDataPointSelected(edu.uiowa.physics.pw.das.event.DataPointSelectionEvent event) {

        if (listenerList == null) return;
        Object[] listeners = listenerList.getListenerList ();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i]==edu.uiowa.physics.pw.das.event.DataPointSelectionListener.class) {
                ((edu.uiowa.physics.pw.das.event.DataPointSelectionListener)listeners[i+1]).DataPointSelected (event);
            }
        }
    }
}
