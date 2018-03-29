/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.graph;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 *
 * @author leiffert
 */
public class DasSliceController extends DasCanvasComponent {

    enum MouseArea {
        LEFT, CENTER, RIGHT, NONE
    }
    
    MouseArea mouseArea;
    Point mousePressPt;
    
    double dataValueLeft;
    double dataValueRight;
    double dataValueLeftMouseUpdate;
    double dataValueRightMouseUpdate;
   
    Rectangle leftRect;
    Rectangle rightRect;
    Rectangle centerRect;
    
    private float widthFactor = 4.0f / 9.0f;
    
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
        
        g.drawString(Double.toString(dataValueLeft  + dataValueLeftMouseUpdate), leftRect.x + leftRect.width / 3, rowMidPt);
        g.drawString(Double.toString(dataValueRight + dataValueRightMouseUpdate), rightRect.x + rightRect.width / 3, rowMidPt);
        
        g.setColor(new Color(.5f, .5f, 0, .5f));
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
    
    public DasSliceController(double dataLeft, double dataRight){
        super();
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
                Point currentPoint = e.getPoint();
                int xDist = currentPoint.x - mousePressPt.x;
                if(mouseArea == MouseArea.LEFT){
                    dataValueLeftMouseUpdate = xDist;
                } else if ( mouseArea == MouseArea.RIGHT){
                    dataValueRightMouseUpdate = xDist;
                } else if ( mouseArea == MouseArea.CENTER){
                    dataValueLeftMouseUpdate = xDist;
                    dataValueRightMouseUpdate = xDist;
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                dataValueLeft += dataValueLeftMouseUpdate;
                dataValueRight += dataValueRightMouseUpdate;
                dataValueLeftMouseUpdate = 0;
                dataValueRightMouseUpdate = 0;
                if(!getBounds().contains(e.getPoint())){
                    mouseArea = MouseArea.NONE;
                    update();
                }
            }
            @Override
            public void mouseEntered(MouseEvent e){
                super.mouseEntered(e);
                
            }
            @Override
            public void mouseExited(MouseEvent e) {
                super.mouseExited(e); 
               
            }
            @Override
            public void mouseMoved(MouseEvent e) {
                super.mouseMoved(e); 
                Point eP = e.getPoint();
                if(leftRect.contains(eP)){
                    mouseArea = MouseArea.LEFT;
//                    System.err.println("in Left Rect");
                    update();
                } else if(rightRect.contains(eP)){
                    mouseArea = MouseArea.RIGHT;
//                    System.err.println("in Right Rect");
                    update();
                } else if(centerRect.contains(eP)){
                    mouseArea = MouseArea.CENTER;
//                    System.err.println("in Center Rect");
                    update();
                } else{
                    mouseArea = MouseArea.NONE;
//                    System.err.println("not in any Rect");
                    update();
                }
                
            }
            
        }; 
    }
}


