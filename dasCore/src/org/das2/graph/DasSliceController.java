/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.graph;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.event.EventListenerList;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.event.DataRangeSelectionEvent;
import org.das2.event.DataRangeSelectionListener;
import org.das2.qds.QDataSet;
import org.das2.datum.Units;
import org.das2.qds.ops.*;
import org.das2.qds.DataSetOps;
import org.das2.qds.DataSetUtil;

/**
 *
 * @author leiffert
 */
public class DasSliceController extends DasCanvasComponent {

    
    // Objects to recieve event firing
    private EventListenerList eListenerList = new EventListenerList();
   
    // Rank 1 Dataset;
    QDataSet qds;
    
    // DatumRange containing the valid max and min of the QDataSet
    DatumRange validDatumRange;
    // The current range displayed
    DatumRange currentDatumRange;
    
    // How much to increase or decrease the data values 
    // on a click and drag
    Datum datumLeftChange;
    Datum datumRightChange;
    
    
    // Used to highlight area mouse is in
    enum MouseArea {
        LEFT, CENTER, RIGHT, NONE
    }
    
    private MouseArea mouseArea = MouseArea.NONE;
    
    // Stores point where the mouse is pressed and is used
    // to determine how much a data value should change 
    // when the mouse is released.
    private Point mousePressPt;
    
    // Used to keep an area highlighted if a mouse drag
    // exits frame, but to stop highlighting if mouse exit
    // wasn't during a drag.
    private boolean mouseIsDragging;
  
    
    // Interactive area for changing left data value
    private Rectangle leftRect = new Rectangle(); 
    // Interactive area for changing right data value
    private Rectangle rightRect = new Rectangle();
    // Interactive area for changing both data values at the same time
    private Rectangle centerRect = new Rectangle();
    
    // Portion of entire width used for a data cell
    private float widthFactor = 4.0f / 9.0f;
    
    
    // Values to make painting the components easier
    private int colMin;
    private int colMax; 
    private int colWidth;
    private int colMidPt;
    
    private int rowMin;  
    private int rowMax; 
    private int rowWidth;
    private int rowMidPt; 
    
    private int dataCellWidth;
    private int dataCellHeight;
    
    private int colLeftCellBegin;
    private int colLeftCellEnd;
    
    private int colRightCellBegin;
    private int colRightCellEnd;
    
    
    // Called in paint method to update everything on a resize
    private void setRects(){
        
        colMin = getColumn().getDMinimum() ;
        colMax = getColumn().getDMaximum() ; 
        colWidth = colMax - colMin;
        colMidPt = colMin + (colWidth / 2);
        
        rowMin = getRow().getDMinimum() ;
        rowMax = getRow().getDMaximum() ;
        rowWidth = rowMax - rowMin;
        rowMidPt = rowMin + (rowWidth / 2);
        
        dataCellWidth = (int) (colWidth * widthFactor);
        dataCellHeight = rowWidth;
        
        colLeftCellBegin = colMin;
        colLeftCellEnd = colLeftCellBegin + dataCellWidth;
        
        colRightCellEnd  = colMax;
        colRightCellBegin = colRightCellEnd - dataCellWidth;
        
        leftRect.setBounds(colMin, rowMin, dataCellWidth, dataCellHeight);
        rightRect.setBounds(colRightCellBegin, rowMin, dataCellWidth, dataCellHeight);
        centerRect.setBounds(colLeftCellEnd, rowMin, colRightCellBegin - colLeftCellEnd, dataCellHeight);
    }
            
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); //To change body of generated methods, choose Tools | Templates.

        setRects();

        g.fillRect(leftRect.x, leftRect.y, leftRect.width, leftRect.height);
        g.fillRect(rightRect.x, rightRect.y, rightRect.width, rightRect.height);
        
        g.setColor(Color.BLACK);
       
        g.setFont(g.getFont().deriveFont(g.getFont().getSize() * 2.5f));
        
        // Write data values
        g.drawString((currentDatumRange.min().plus(datumLeftChange)).toString(), 
                leftRect.x + leftRect.width / 5, rowMidPt + g.getFont().getSize() / 4);
        g.drawString((currentDatumRange.max().plus(datumRightChange)).toString(), 
                rightRect.x + rightRect.width / 5, rowMidPt + g.getFont().getSize() / 4);
        
        g.setColor(new Color(.5f, .5f, 0, .5f));
        
        // highlight area the mouse is in
        switch (this.mouseArea){
            case LEFT:
                g.fillRect(leftRect.x, leftRect.y, leftRect.width, leftRect.height);
                break;
            case CENTER:
                g.fillRect(centerRect.x, centerRect.y, centerRect.width, centerRect.height);
                break;   
            case RIGHT:
                g.fillRect(rightRect.x, rightRect.y, rightRect.width, rightRect.height);
                break;  
            case NONE:
                break;
            default:
                break;
        }
    }

    @Override
    public Rectangle getBounds() {
        return DasDevicePosition.toRectangle( getRow(), getColumn() );
    }
    

    public DasSliceController(QDataSet qds){
        super();
        this.qds = qds;
        if(qds.rank() != 1){
            throw new IllegalArgumentException("Dataset is not rank 1." +
                    "Slice the data in the dimension you want "+ 
                    "before calling DasSliceController().");
        }
        
        if(!DataSetUtil.isMonotonic(qds)){
            throw new IllegalArgumentException("Dataset is not monotonic.");
        }
        
        // Check if increasing or decreasing and set valid range accordingly
        if( qds.value(0) > qds.value(qds.length() - 1)){
            this.validDatumRange = new DatumRange(
                Datum.create(qds.value(qds.length() - 1), (Units) qds.property(QDataSet.UNITS)),
                Datum.create(qds.value(0), (Units) qds.property(QDataSet.UNITS)) );
        } else{
            
            this.validDatumRange = new DatumRange(
                Datum.create(qds.value(0), (Units) qds.property(QDataSet.UNITS)),
                Datum.create(qds.value(qds.length() - 1), (Units) qds.property(QDataSet.UNITS)) );
        }
        
        this.currentDatumRange = this.validDatumRange;
        
        datumLeftChange = Datum.create(0, (Units) qds.property(QDataSet.UNITS));
        datumRightChange = Datum.create(0, (Units) qds.property(QDataSet.UNITS));
        
        MouseAdapter ma=  getMouseAdapter();
        addMouseListener( ma );
        addMouseMotionListener( ma );
        
    }

    
    private MouseAdapter getMouseAdapter() { 
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                mousePressPt = e.getPoint(); 
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
                super.mouseDragged(e); 
                mouseIsDragging = true;
                Point currentPoint = e.getPoint();
                int xDist = currentPoint.x - mousePressPt.x;
                Datum xDatumDist = Datum.create(xDist, (Units) qds.property(QDataSet.UNITS));
                updateValues(xDatumDist);
                
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                mouseIsDragging = false;
                currentDatumRange = new DatumRange(
                        currentDatumRange.min().add(datumLeftChange),
                        currentDatumRange.max().add(datumRightChange)  );
                datumLeftChange = Datum.create(0, (Units) qds.property(QDataSet.UNITS));
                datumRightChange = Datum.create(0, (Units) qds.property(QDataSet.UNITS));
                
                if(!getBounds().contains(e.getPoint())){
                    mouseArea = MouseArea.NONE;
                    update();
                }
             
                DataRangeSelectionEvent dataRangeEvent = new DataRangeSelectionEvent(
                        this, currentDatumRange.min(), currentDatumRange.max());

                fireDataRangeSelectionListenerDataRangeSelected(dataRangeEvent);
            }
            
            @Override
            public void mouseEntered(MouseEvent e){
                super.mouseEntered(e);
                
            }
            @Override
            public void mouseExited(MouseEvent e) {
                super.mouseExited(e); 
                if(!mouseIsDragging){
                    mouseArea = MouseArea.NONE;
                    update();
                }
                
            }
            @Override
            public void mouseMoved(MouseEvent e) {
                super.mouseMoved(e); 
                Point eP = e.getPoint();
                if(leftRect.contains(eP)){
                    mouseArea = MouseArea.LEFT;
//                    System.err.println("left");
                    update();
                } else if(rightRect.contains(eP)){
                    mouseArea = MouseArea.RIGHT;
//                    System.err.println("right");
                    update();
                } else if(centerRect.contains(eP)){
                    mouseArea = MouseArea.CENTER;
//                    System.err.println("center");
                    update();
                } else{
                    mouseArea = MouseArea.NONE;
//                    System.err.println("none");
                    update();
                }
                
            }
            
        }; 
    }
    
    private void updateValues(Datum xDatumDist){
        switch (mouseArea) {
            case LEFT:
                // Ensure data left never gets larger than data right
                if( currentDatumRange.min().add(xDatumDist).
                            ge(currentDatumRange.max())  ){ 
                    
                    datumLeftChange = 
                            currentDatumRange.max().subtract(currentDatumRange.min());   
                    
                }
                // Ensure data left never drops below valid min
                else if(currentDatumRange.min().add(xDatumDist).
                            le(validDatumRange.min())){
                    
                    datumLeftChange = 
                            validDatumRange.min().subtract(currentDatumRange.min());
                
                }else{
                    datumLeftChange = xDatumDist;
                }   break;
            case RIGHT:
                //ensure data right never drops below data left
                if( currentDatumRange.max().add(xDatumDist).
                            le(currentDatumRange.min())  ){ 
                    
                    datumRightChange = 
                            currentDatumRange.min().subtract(currentDatumRange.max()); 
                    
                }
                // Ensure data right never increases above valid max
                else if(currentDatumRange.max().add(xDatumDist).
                            ge(validDatumRange.max())){
                    
                    datumRightChange = 
                            validDatumRange.max().subtract(currentDatumRange.max());
                
                }else{
                    datumRightChange = xDatumDist;
                }   break;
            case CENTER:
                // Ensure currentDatumRange.max - currentDatumRange.min stays constant
                // while not allowing either side to change outside of valid range.
                if(currentDatumRange.max().add(xDatumDist).ge(validDatumRange.max())){
                    xDatumDist = validDatumRange.max().subtract(currentDatumRange.max());
                }else if(currentDatumRange.min().add(xDatumDist).
                        le(validDatumRange.min())){
                    xDatumDist = validDatumRange.min().subtract(currentDatumRange.min());
                }
                datumLeftChange = xDatumDist;
                datumRightChange = xDatumDist;
                break;
            default:
                break;
        }
    }
    
    /** Registers DataRangeSelectionListener to receive events.
     * @param listener The listener to register.
     */
    public void addDataRangeSelectionListener(DataRangeSelectionListener listener){
        eListenerList.add(org.das2.event.DataRangeSelectionListener.class, listener);
    }
    
    
    /** Removes DataRangeSelectionListener from the list of listeners.
     * @param listener The listener to remove.
     */
    public void removeDataRangeSelectionListener(DataRangeSelectionListener listener){
        eListenerList.remove(org.das2.event.DataRangeSelectionListener.class, listener);
    }
    
     /** Notifies all registered listeners about the event.
     *
     * @param event The event to be fired
     */
    private void fireDataRangeSelectionListenerDataRangeSelected(DataRangeSelectionEvent event){
        Object[] listeners;
        listeners = eListenerList.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==org.das2.event.DataRangeSelectionListener.class) {
                ((org.das2.event.DataRangeSelectionListener)listeners[i+1]).dataRangeSelected(event);
            }
        }
    }
}


