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
import org.das2.event.DataRangeSelectionEvent;
import org.das2.event.DataRangeSelectionListener;
import org.das2.qds.QDataSet;

/**
 *
 * @author leiffert
 */
public class DasSliceController extends DasCanvasComponent {

    
    // Objects to recieve event firing
    private EventListenerList eListenerList = new EventListenerList();
   
    // Dataset to slice;
    QDataSet qds;
    Datum datumLeft;
    Datum datumRight;
    
    // Used to highlight area mouse is in
    enum MouseArea {
        LEFT, CENTER, RIGHT, NONE
    }
    
    MouseArea mouseArea;
    
    // Stores point where the mouse is pressed and is used
    // to determine how much a data value should change 
    // when the mouse is released.
    Point mousePressPt;
    
    // Used to keep an area highlighted if a mouse drag
    // exits frame, but to stop highlighting if mouse exit
    // wasn't during a drag.
    boolean mouseIsDragging;
    
    // The values stored in the left and right data cells
    double dataValueLeft;
    double dataValueRight;
    
    // How much to increase or decrease the data values 
    // on a click and drag
    double dataValueLeftMouseUpdate; 
    double dataValueRightMouseUpdate;
   
    
    // Interactive area for changing left data value
    Rectangle leftRect; 
    // Interactive area for changing right data value
    Rectangle rightRect;
    // Interactive area for changing both data values at the same time
    Rectangle centerRect;
    
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
        g.drawString(Double.toString(dataValueLeft  + dataValueLeftMouseUpdate), leftRect.x + leftRect.width / 5, rowMidPt + g.getFont().getSize() / 4);
        g.drawString(Double.toString(dataValueRight + dataValueRightMouseUpdate), rightRect.x + rightRect.width / 5, rowMidPt + g.getFont().getSize() / 4);
        
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
    
    public DasSliceController(QDataSet qds, double dataLeft, double dataRight){
        super();
        this.qds = qds;
        dataValueLeft = dataLeft;
        dataValueLeftMouseUpdate = 0;
        dataValueRight = dataRight;
        dataValueRightMouseUpdate = 0;
        leftRect = new Rectangle();
        rightRect = new Rectangle();
        centerRect = new Rectangle();
        mouseArea = MouseArea.NONE;
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
                
                switch (mouseArea) {
                    case LEFT:
                        //ensure data left never gets larger than data right
                        if(dataValueLeft + xDist >= dataValueRight){ 
                            dataValueLeftMouseUpdate = dataValueRight - dataValueLeft;
                        }else{
                            dataValueLeftMouseUpdate = xDist;
                        }   break;
                    case RIGHT:
                        //ensure data right never drops below data left
                        if(dataValueRight + xDist <= dataValueLeft){ 
                            dataValueRightMouseUpdate = dataValueLeft - dataValueRight;
                        } else{
                            dataValueRightMouseUpdate = xDist;
                        }   break;
                    case CENTER:
                        dataValueLeftMouseUpdate = xDist;
                        dataValueRightMouseUpdate = xDist;
                        break;
                    default:
                        break;
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                mouseIsDragging = false;
                dataValueLeft += dataValueLeftMouseUpdate;
                dataValueRight += dataValueRightMouseUpdate;
                dataValueLeftMouseUpdate = 0;
                dataValueRightMouseUpdate = 0;
                if(!getBounds().contains(e.getPoint())){
                    mouseArea = MouseArea.NONE;
                    update();
                }
                DataRangeSelectionEvent dataRangeEvent;
                dataRangeEvent = new DataRangeSelectionEvent(this, (Datum) datumLeft, (Datum) datumRight);
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
                    update();
                } else if(rightRect.contains(eP)){
                    mouseArea = MouseArea.RIGHT;
                    update();
                } else if(centerRect.contains(eP)){
                    mouseArea = MouseArea.CENTER;
                    update();
                } else{
                    mouseArea = MouseArea.NONE;
                    update();
                }
                
            }
            
        }; 
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


