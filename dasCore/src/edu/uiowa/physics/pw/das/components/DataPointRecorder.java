/*
 * DataPointRecorder.java
 *
 * Created on October 6, 2003, 12:16 PM
 */

package edu.uiowa.physics.pw.das.components;

import edu.uiowa.physics.pw.das.*;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.datum.format.*;
import edu.uiowa.physics.pw.das.event.*;
import edu.uiowa.physics.pw.das.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;

/**
 *
 * @author  jbf
 */
public class DataPointRecorder extends JPanel implements edu.uiowa.physics.pw.das.event.DataPointSelectionListener {
    JTable textArea;
    DataPointReporter reporter;
    ArrayList dataPoints;
    Units[] unitsArray;
    DatumFormatter[] formatterArray;
    AbstractTableModel myTableModel;
    
    private class DataPoint {
        double[] data;
        DataPoint(double x1, double x2 ) {
            data= new double[] { x1,x2 };
        }
        double get(int i) {
            return data[i];
        }
        public String toString() {
            return ""+data[0]+" "+data[1];
        }
    }
    
    private class MyTableModel extends AbstractTableModel {
        
        public int getColumnCount() {
            return 2;
        }
        
        public String getColumnName(int i) {
            if ( unitsArray!=null ) {
                return unitsArray[i].toString();
            } else {
                return ""+i;
            }
        }
        
        public int getRowCount() {
            int nrow= dataPoints.size();
            nrow= nrow>0 ? nrow : 1 ;
            return dataPoints.size();
        }
        
        public Object getValueAt(int i, int j) {
            DataPoint x= (DataPoint)dataPoints.get(i);
            return formatterArray[j].format( Datum.create( x.get(j), unitsArray[j] ) );
        }
        
    }
    
    public void deleteRow( int row ) {
        dataPoints.remove(row);
        update();
    }
    
    
    public void saveToFile( File file ) throws IOException {
        FileOutputStream out= new FileOutputStream( file );
        BufferedWriter r= new BufferedWriter(new OutputStreamWriter(out));
        
        for ( int i=0; i<dataPoints.size(); i++ ) {
            DataPoint x= (DataPoint)dataPoints.get(i);
            StringBuffer s= new StringBuffer();
            for ( int j=0; j<unitsArray.length; j++ ) {
                s.append(formatterArray[j].format( Datum.create( x.get(j), unitsArray[j] ) ) + "\t");
            }            
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
            for ( int i=0;i<s.length;i++ ) {
                Datum x;
                if ( TimeUtil.isValidTime(s[0]) ) x= TimeUtil.createValid(s[0]); else x=DatumUtil.createValid(s[0]);
                Datum y;
                if ( TimeUtil.isValidTime(s[1]) ) y= TimeUtil.createValid(s[1]); else y=DatumUtil.createValid(s[1]);
                
                DataPointSelectionEvent e= new DataPointSelectionEvent(this, x, y );
                DataPointSelected(e);
            }            
        }
        DasProperties.getInstance().put("components.DataPointRecorder.lastFileLoad",  file.toString());
        
    }
    
    private class MyMouseAdapter extends MouseAdapter {
        JPopupMenu popup;
        final JTable parent;
        
        MyMouseAdapter(final JTable parent) {
            this.parent= parent;
            popup= new JPopupMenu("Options"); 
            popup.addSeparator();
            JMenuItem menuItem= new JMenuItem("Delete Row");
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
    
    
    /** Creates a new instance of DataPointRecorder */
    public DataPointRecorder() {
        super();
        dataPoints= new ArrayList();
        myTableModel= new MyTableModel();
        this.setLayout(new BorderLayout());
        textArea= new JTable(myTableModel);
        
        textArea.addMouseListener(new DataPointRecorder.MyMouseAdapter(textArea));
        
        JScrollPane p= new JScrollPane(textArea);
        this.add(p,BorderLayout.CENTER);
        
        final JPanel controlPanel= new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel,BoxLayout.X_AXIS));
        JButton saveButton= new JButton("Save...");
        saveButton.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser jj= new JFileChooser();
                String lastFileString= (String)DasProperties.getInstance().get("components.DataPointRecorder.lastFileSave");
                if ( lastFileString==null ) lastFileString="";
                File lastFile= null;
                if ( lastFileString!="" ) {
                        lastFile= new File( lastFileString );
                        jj.setSelectedFile(lastFile);
                }                                        
                
                int status= jj.showSaveDialog(controlPanel);
                if ( status==JFileChooser.APPROVE_OPTION ) {
                    try { 
                        saveToFile(jj.getSelectedFile());
                    } catch ( IOException e1 ) {
                        DasExceptionHandler.handle(e1);
                    }
                }
            }
        } );
        JButton loadButton= new JButton("Load...");
        loadButton.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser jj= new JFileChooser();
                String lastFileString= (String)DasProperties.getInstance().get("components.DataPointRecorder.lastFileLoad");
                if ( lastFileString==null ) lastFileString="";
                File lastFile= null;
                if ( lastFileString!="" ) {
                        lastFile= new File( lastFileString );
                        jj.setSelectedFile(lastFile);
                }                                        
                int status= jj.showOpenDialog(controlPanel);
                if ( status==JFileChooser.APPROVE_OPTION ) {
                    try {
                        loadFromFile(jj.getSelectedFile());
                    } catch ( IOException e1 ) {
                        DasExceptionHandler.handle(e1);
                    }
                }
            }
        } );
        
        controlPanel.add(saveButton);
        controlPanel.add(loadButton);
        
        
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
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        //DataPointRecorder p= DataPointRecorder.createFramed();
        //p.DataPointSelected(new DataPointSelectionEvent(p, Datum.create(2), Datum.create(6)));
        //p.DataPointSelected(new DataPointSelectionEvent(p, Datum.create(4), Datum.create(7)));
        //p.DataPointSelected(new DataPointSelectionEvent(p, Datum.create(1), Datum.create(72)));
        //edu.uiowa.physics.pw.das.graph.DasPlot plot= testNew.demoSpectrogram.createFramed();
        //CrossHairMouseModule ch= (CrossHairMouseModule)plot.getMouseAdapter().getModuleByLabel("Crosshair Digitizer");
        //ch.addDataPointSelectionListener(p);
    }
    
    private void update() {
        myTableModel.fireTableDataChanged();
    }
    
    public void DataPointSelected(edu.uiowa.physics.pw.das.event.DataPointSelectionEvent e) {
        if ( dataPoints.size()==0 ) {
            Datum[] datums= new Datum[] { e.getX(), e.getY() };
            unitsArray= new Units[] { e.getX().getUnits(), e.getY().getUnits() };
            formatterArray= new DatumFormatter[] { e.getX().getFormatter(), e.getY().getFormatter() };
            for ( int j=0; j<unitsArray.length; j++ ) {
                if ( unitsArray[j] instanceof TimeLocationUnits ) {
                    formatterArray[j]= TimeDatumFormatterFactory.getInstance().defaultFormatter();                    
                }
            }
            
        }
        dataPoints.add(new DataPoint(e.getX().doubleValue(unitsArray[0]),e.getY().doubleValue(unitsArray[1])));
        update();
    }
    
}
