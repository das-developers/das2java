/*
 * DataPointRecorder.java
 *
 * Created on October 6, 2003, 12:16 PM
 */

package edu.uiowa.physics.pw.das.components;

import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.event.*;
import java.awt.*;
import java.util.*;
import javax.swing.*;

/**
 *
 * @author  jbf
 */
public class DataPointRecorder extends JPanel implements edu.uiowa.physics.pw.das.event.DataPointSelectionListener {
    JTextArea textArea;
    DataPointReporter reporter;
    ArrayList dataPoints;    
    
    /** Creates a new instance of DataPointRecorder */
    public DataPointRecorder() {
        super();        
        dataPoints= new ArrayList();  // stored as strings        
        this.setLayout(new BorderLayout()); 
        reporter= new DataPointReporter();        
        this.add(reporter,BorderLayout.NORTH);
        reporter.addDataPointSelectionListener(this);
        textArea= new JTextArea(20,10);
        JScrollPane p= new JScrollPane(textArea);
        this.add(p,BorderLayout.CENTER);        
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
        DataPointRecorder p= DataPointRecorder.createFramed();
        p.DataPointSelected(new DataPointSelectionEvent(p, Datum.create(2), Datum.create(6)));
        p.DataPointSelected(new DataPointSelectionEvent(p, Datum.create(4), Datum.create(7)));
        p.DataPointSelected(new DataPointSelectionEvent(p, Datum.create(1), Datum.create(72)));
        //edu.uiowa.physics.pw.das.graph.DasPlot plot= testNew.demoSpectrogram.createFramed();
        //CrossHairMouseModule ch= (CrossHairMouseModule)plot.getMouseAdapter().getModuleByLabel("Crosshair Digitizer");
        //ch.addDataPointSelectionListener(p);
    }
    
    private void update() {
        int rows= dataPoints.size();
        StringBuffer text= new StringBuffer();
        for ( Iterator i= dataPoints.iterator(); i.hasNext(); ) {
            text.append(i.next()).append("\n");
        }
        textArea.setRows(rows);
        textArea.setText(text.toString());
    }
    
    public void DataPointSelected(edu.uiowa.physics.pw.das.event.DataPointSelectionEvent e) {        
        dataPoints.add(""+e.getX()+" "+e.getY());
        update();        
    }                    
    
}
