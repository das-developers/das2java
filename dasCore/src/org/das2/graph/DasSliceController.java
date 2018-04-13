/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.graph;

import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import org.das2.datum.Datum;
import org.das2.datum.Units;
import org.das2.qds.QDataSet;

/**
 *
 * @author leiffert
 */

 public class DasSliceController extends DasCanvasComponent {

    Cursor resizeCursor = new Cursor(Cursor.E_RESIZE_CURSOR);
    Cursor handCursor = new Cursor(Cursor.HAND_CURSOR);
    Cursor defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);
    Cursor moveCursor = new Cursor(Cursor.MOVE_CURSOR);

    class TextRect {

        double spacingFactor = 1.5;
        String text;
        Rectangle rect;

        public TextRect(String text, Rectangle rect) {
            this.text = text;
            this.rect = rect;
        }

    }

    class DatumRect extends TextRect {

        private Datum datum;

        public void setDatum(Datum d) {
            this.datum = d;
            this.text = d.toString();
        }

        public DatumRect(Datum d, Rectangle r) {
            super(d.toString(), r);
            this.datum = d;
        }
    }
    /**
     * The Rank 1 slice Dataset
     */
    QDataSet qds;

    DatumRect lDatumRect;
    DatumRect rDatumRect;
    TextRect addRDatum = new TextRect("+", new Rectangle());

    int scanWidth = 100;
    TextRect rScan = new TextRect("scan>>", new Rectangle());
    TextRect lScan = new TextRect("<<scan", new Rectangle());

    public DasSliceController(QDataSet qds) {
        if (qds.rank() != 1) {
            throw new IllegalArgumentException("Dataset is not rank 1."
                    + "Slice the data in the dimension you want "
                    + "before calling DasSliceController().");
        }
        this.qds = qds;
        lDatumRect = new DatumRect(Datum.create(qds.value(0), (Units) qds.property(QDataSet.UNITS)), new Rectangle());
        rDatumRect = new DatumRect(Datum.create(qds.value(0), (Units) qds.property(QDataSet.UNITS)), new Rectangle());

        MouseAdapter ma = getMouseAdapter();
        addMouseMotionListener(ma);
        addMouseListener(ma);

    }

    private void layoutRects(Graphics g) {
        int colMin = getColumn().getDMinimum();
        int colMax = getColumn().getDMaximum();
        int rowMin = getRow().getDMinimum();
        int rowMax = getRow().getDMaximum();
        int width = colMax - colMin;
        int height = rowMax - rowMin;

        rScan.rect.setBounds(colMax - scanWidth, colMin, scanWidth, height);

        if (lDatumRect.datum.equals(rDatumRect.datum)) {

        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        layoutRects(g);
    }

    private MouseAdapter getMouseAdapter() {
        return new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {

            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseDragged(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }

            @Override
            public void mouseMoved(MouseEvent e) {

            }
            @Override
            public void mouseWheelMoved(MouseWheelEvent e){
            
            }

        };
    }
}
// 

//    
//     /** DatumRange containing the valid max and min of the QDataSet */
//     private DatumRange validDatumRange;   
// 
//     public DatumRange getValidDatumRange() {
//         return validDatumRange;
//     }
//     
//     public void setValidDatumRange(DatumRange validDatumRange) {
//         this.validDatumRange = validDatumRange;
//     }
// 
//     /** The current range displayed */
//     private DatumRange currentDatumRange = null;
// 
//     public static final String PROP_CURRENTDATUMRANGE = "currentDatumRange";
// 
//     public DatumRange getCurrentDatumRange() {
//         return currentDatumRange;
//     }
// 
//     public void setCurrentDatumRange(DatumRange currentDatumRange) {
//         DatumRange oldCurrentDatumRange = this.currentDatumRange;
//         this.currentDatumRange = currentDatumRange;
//         firePropertyChange(PROP_CURRENTDATUMRANGE, oldCurrentDatumRange, currentDatumRange);
//     }
//     
//     /** Amount to change currentDatumRange.min() on a click and drag */
//     private Datum datumLeftDragVal = null;
// 
//     public Datum getDatumLeftDragVal() {
//         return datumLeftDragVal;
//     }
// 
//     public void setDatumLeftDragVal(Datum datumLeftDragVal) {
//         this.datumLeftDragVal = datumLeftDragVal;
//     }
// 
//     /** Amount to change currentDatumRange.max() on a click and drag */
//     private Datum datumRightDragVal = null;
//     
//     public Datum getDatumRightDragVal() {
//         return datumRightDragVal;
//     }
// 
//     public void setDatumRightDragVal(Datum datumRightDragVal) {
//         this.datumRightDragVal = datumRightDragVal;
//     }
// 
//     
//     Cursor resizeCursor = new Cursor(Cursor.E_RESIZE_CURSOR);
//     Cursor handCursor = new Cursor(Cursor.HAND_CURSOR);
//     Cursor defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);
//     Cursor moveCursor = new Cursor(Cursor.MOVE_CURSOR);
//     
//     /* Possible areas the mouse cursor can be */
//     enum MouseArea {
//         LEFT, CENTER, RIGHT, NONE
//     }
//     
//     /* Used to highlight area mouse is in */
//     private MouseArea mouseArea = MouseArea.NONE;
// 
//     public MouseArea getMouseArea() {
//         return mouseArea;
//     }
// 
//     protected void setMouseArea(MouseArea mouseArea) {
//         if(this.mouseArea == mouseArea){ return; }
//         else{
//             switch(mouseArea){
//                 case LEFT:
//                     getCanvas().getGlassPane().setCursor(handCursor);
//                     update();
//                     break;
//                 case RIGHT:
//                     getCanvas().getGlassPane().setCursor(handCursor);
//                     update();
//                     break;
//                 case CENTER:
//                     getCanvas().getGlassPane().setCursor(moveCursor);
//                     update();
//                     break;
//                 case NONE:
//                     getCanvas().getGlassPane().setCursor(defaultCursor);
//                     update();
//                     break;
//                 default:
//                     break;
//                     
//             }
//         }
//         this.mouseArea = mouseArea;
//     }
//     
//     protected void setMouseArea(Point pt){
//      // Update mouse area for highlighting
//         if(leftRect.contains(pt)){
//             setMouseArea(MouseArea.LEFT);
//         } else if(rightRect.contains(pt)){
//             setMouseArea(MouseArea.RIGHT);
//         } else if(centerRect.contains(pt)){
//             setMouseArea(MouseArea.CENTER);
//         } else{
//             setMouseArea(MouseArea.NONE);
//         }
//     } 
//     
//     
//     /** 
//      * Stores point where the mouse is pressed and is used
//      * to determine how much a data value should change 
//      * when the mouse is released.
//      */
//     private Point mousePressPt;
//     
//     /**
//      * Used to keep an area highlighted if a mouse drag
//      * exits frame, but to stop highlighting if mouse exit
//      * wasn't during a drag.
//      */
//     private boolean mouseIsDragging;
//   
//     
//     /** Interactive area for changing left data value */
//     private Rectangle leftRect = new Rectangle(); 
//     /** Interactive area for changing right data value */
//     private Rectangle rightRect = new Rectangle();
//     /** Interactive area for changing both data values at the same time */
//     private Rectangle centerRect = new Rectangle();
//     
//     /** Portion of entire width used for leftRect and rightRect */
//     private float widthFactor = 4.0f / 9.0f;
//     
//     
//     /* ****** USEFUL PAINT PARAMS ****** */
//     private int colMin;
//     private int colMax; 
//     private int colWidth;
//     private int colMidPt;
//     
//     private int rowMin;  
//     private int rowMax; 
//     private int rowWidth;
//     private int rowMidPt; 
//     
//     private int dataCellWidth;
//     private int dataCellHeight;
//     
//     private int colLeftCellBegin;
//     private int colLeftCellEnd;
//     
//     private int colRightCellBegin;
//     private int colRightCellEnd;
//     /* *************************** */
//     
//     
//     /** Updates all the useful paint params */
//     private void setRects(){
//         
//         colMin = getColumn().getDMinimum() ;
//         colMax = getColumn().getDMaximum() ;
// //                colMin = 100; colMax = 900;
// //                colMin = 0; colMax = 1000;
// //        System.err.println("ColMin = " + colMin);
// //        System.err.println("ColMax = " + colMax);
// 
//         colWidth = colMax - colMin;
//         colMidPt = colMin + (colWidth / 2);
//         
//         rowMin = getRow().getDMinimum() ;
//         rowMax = getRow().getDMaximum() ;
// //                rowMin = 10; rowMax = 90;
// //                rowMin = 0; rowMax = 100;
// //        System.err.println("rowMin = " + rowMin);
// //        System.err.println("rowMax = " + rowMax);
// 
//         rowWidth = rowMax - rowMin;
//         rowMidPt = rowMin + (rowWidth / 2);
//         
//         dataCellWidth = (int) (colWidth * widthFactor);
//         dataCellHeight = rowWidth;
// //        System.err.println("row width = " + dataCellHeight);
//         colLeftCellBegin = colMin;
//         colLeftCellEnd = colLeftCellBegin + dataCellWidth;
//         
//         colRightCellEnd  = colMax;
//         colRightCellBegin = colRightCellEnd - dataCellWidth;
//         
//         leftRect.setBounds(colMin, rowMin, dataCellWidth, dataCellHeight);
//         rightRect.setBounds(colRightCellBegin, rowMin, dataCellWidth, dataCellHeight);
//         centerRect.setBounds(colLeftCellEnd, rowMin, colRightCellBegin - colLeftCellEnd, dataCellHeight);
//     }
//             
//     @Override
//     protected void paintComponent(Graphics g) {
//         
//         setRects();
//         g.setColor(Color.gray);
//         g.drawRect(leftRect.x, leftRect.y, leftRect.width, leftRect.height);
//         g.drawRect(rightRect.x, rightRect.y, rightRect.width, rightRect.height);
//         
//         g.setColor(Color.BLACK);
//        
//         g.setFont(g.getFont().deriveFont(g.getFont().getSize() * 2.5f));
//         
//         // Write data values
//         g.drawString((currentDatumRange.min().plus(datumLeftDragVal)).toString(), 
//                 leftRect.x + leftRect.width / 5, rowMidPt + g.getFont().getSize() / 4);
//         g.drawString((currentDatumRange.max().plus(datumRightDragVal)).toString(), 
//                 rightRect.x + rightRect.width / 5, rowMidPt + g.getFont().getSize() / 4);
//         
//         g.setColor(new Color(.5f, .5f, 0, .5f));
//         
//         // highlight area the mouse is in
//         switch (this.mouseArea){
//             case LEFT:
//                 g.fillRect(leftRect.x, leftRect.y, leftRect.width, leftRect.height);
//                 break;
//             case CENTER:
//                 g.fillRect(centerRect.x, centerRect.y, centerRect.width, centerRect.height);
//                 break;   
//             case RIGHT:
//                 g.fillRect(rightRect.x, rightRect.y, rightRect.width, rightRect.height);
//                 break;  
//             case NONE:
//                 break;
//             default:
//                 break;
//         }
//     }
// 
//     @Override
//     public Rectangle getBounds() {
//         
//         return DasDevicePosition.toRectangle( getRow(), getColumn() );
//         
//     }
//    
//     
//     public DasSliceController(QDataSet qds){
//         
//         super();
//         this.qds = qds;

//         
//         if(!DataSetUtil.isMonotonic(qds)){
//             throw new IllegalArgumentException("Dataset is not monotonic.");
//         }
//         
//         // Check if increasing or decreasing and set valid range accordingly
//         if( qds.value(0) > qds.value(qds.length() - 1)){
//             this.validDatumRange = new DatumRange(
//                 Datum.create(qds.value(qds.length() - 1), (Units) qds.property(QDataSet.UNITS)),
//                 Datum.create(qds.value(0), (Units) qds.property(QDataSet.UNITS)) );
//         } else{
//             
//             this.validDatumRange = new DatumRange(
//                 Datum.create(qds.value(0), (Units) qds.property(QDataSet.UNITS)),
//                 Datum.create(qds.value(qds.length() - 1), (Units) qds.property(QDataSet.UNITS)) );
//         }
//         
//         this.currentDatumRange = this.validDatumRange;
//         
//         datumLeftDragVal = Datum.create(0, (Units) qds.property(QDataSet.UNITS));
//         datumRightDragVal = Datum.create(0, (Units) qds.property(QDataSet.UNITS));
//         
//         MouseAdapter ma =  getMouseAdapter();
//         addMouseListener( ma );
//         addMouseMotionListener( ma );
//         
//     }
// 
//     
//     
//     private MouseAdapter getMouseAdapter() { 
//         return new MouseAdapter() {
//             
//             @Override
//             public void mouseClicked(MouseEvent e) {
//                
//             }
// 
//             @Override
//             public void mousePressed(MouseEvent e) { 
//                 mousePressPt = e.getPoint(); 
//             }
//             
//             @Override
//             public void mouseDragged(MouseEvent e) {
//                 
//                 mouseIsDragging = true;
//                 Point currentPoint = e.getPoint();
//                 
//                 // Create Datum based on distance dragged to manipulate the current range
//                 double xDist = currentPoint.x - mousePressPt.x;
//                 xDist = factorOfXDist(xDist);
//                 Datum xDatumDist = Datum.create(xDist, (Units) qds.property(QDataSet.UNITS));
//                 
//                 updateValues(xDatumDist);
//                 
//             }
//             
//             @Override
//             public void mouseReleased(MouseEvent e) {
//                 
//                 mouseIsDragging = false;
//                 // Update currentRange with final drag value
//                 setCurrentDatumRange(new DatumRange(
//                         currentDatumRange.min().add(datumLeftDragVal),
//                         currentDatumRange.max().add(datumRightDragVal)  ));
//                 
//                 // Reset drag vals
//                 setDatumLeftDragVal(Datum.create(0, (Units) qds.property(QDataSet.UNITS) ));
//                 setDatumRightDragVal(Datum.create(0, (Units) qds.property(QDataSet.UNITS)));
//                 
//                 // Stop highlighting if release is outside bounds
//                 if(!getBounds().contains(e.getPoint())){
//                     setMouseArea(MouseArea.NONE);
//                 }
//              
//                 // Fire event for current range on mouse release
//                 DataRangeSelectionEvent dataRangeEvent = new DataRangeSelectionEvent(
//                         this, currentDatumRange.min(), currentDatumRange.max());
// 
//                 fireDataRangeSelectionListenerDataRangeSelected(dataRangeEvent);
//             }
//             
//             @Override
//             public void mouseEntered(MouseEvent e){
//                
//             }
//             
//             @Override
//             public void mouseExited(MouseEvent e) {
//                 
//                 if(!mouseIsDragging){
//                     mouseArea = MouseArea.NONE;
//                     update();
//                 }
//                 
//             }
//             @Override
//             public void mouseMoved(MouseEvent e) {
//                
//                 Point eP = e.getPoint();
//                 setMouseArea(eP);
//                
//             } 
// 
//         }; 
//     }
//     
//     /**
//      * Get some factor of xDist
//      * @param xDist the distance from initial point
//      * @return factored distance
//      */
//     private double factorOfXDist(double xDist){
//         return Math.pow(xDist / 100, 3) ;
//     }
//     
//     /**
//      * Updates current range during a mouse drag, making sure not to allow 
//      * bounds to exceed the valid ranges
//      * 
//      * @param xDatumDist datum corresponding to how far the mouse pointer is
//      *          from its starting point during a drag
//      */
//     private void updateValues(Datum xDatumDist){
//         switch (mouseArea) {
//             case LEFT:
//                 
//                 if( currentDatumRange.min().add(xDatumDist). // Ensure data left never 
//                             ge(currentDatumRange.max())  ){  // gets larger than data right
//                     
//                     setDatumLeftDragVal(currentDatumRange.max().subtract(currentDatumRange.min()));
//                  
//                 }else if(currentDatumRange.min().add(xDatumDist).  // Ensure data left never
//                             le(validDatumRange.min())){            // drops below valid min
//                     
//                     setDatumLeftDragVal(validDatumRange.min().subtract(currentDatumRange.min()));
//                    
//                 }else{
//                     setDatumLeftDragVal(xDatumDist);
//                 }   break;
//             case RIGHT:
//                 if( currentDatumRange.max().add(xDatumDist). // Ensure data right never
//                             le(currentDatumRange.min())  ){  // dropw below data left
//                     
//                     setDatumRightDragVal(currentDatumRange.min().subtract(currentDatumRange.max()));
//                         
//                 }else if(currentDatumRange.max().add(xDatumDist). // Ensure data right never
//                             ge(validDatumRange.max())){          // increases above valid max
//                     
//                     setDatumRightDragVal(validDatumRange.max().subtract(currentDatumRange.max()));
//                             
//                 }else{
//                     setDatumRightDragVal(xDatumDist);
//                 }   break;
//             case CENTER:
//                 // Ensure currentDatumRange.max - currentDatumRange.min stays constant
//                 // while not allowing either side to change outside of valid range.
//                 if(currentDatumRange.max().add(xDatumDist).ge(validDatumRange.max())){
//                     xDatumDist = validDatumRange.max().subtract(currentDatumRange.max());
//                 }else if(currentDatumRange.min().add(xDatumDist).
//                         le(validDatumRange.min())){
//                     xDatumDist = validDatumRange.min().subtract(currentDatumRange.min());
//                 }
//                 setDatumLeftDragVal(xDatumDist);
//                 setDatumRightDragVal(xDatumDist);
//                 break;
//             default:
//                 break;
//         }
//     }
// 
//     /** Registers DataRangeSelectionListener to receive events.
//      * @param listener The listener to register.
//      */
//     public void addDataRangeSelectionListener(DataRangeSelectionListener listener){
//         listenerList.add(org.das2.event.DataRangeSelectionListener.class, listener);
// }
// 
// 
//     /** Removes DataRangeSelectionListener from the list of listeners.
//      * @param listener The listener to remove.
//      */
//     public void removeDataRangeSelectionListener(DataRangeSelectionListener listener){
//         listenerList.remove(org.das2.event.DataRangeSelectionListener.class, listener);
//     }
//     
//      /** Notifies all registered listeners about the event.
//      *
//      * @param event The event to be fired
//      */
//     private void fireDataRangeSelectionListenerDataRangeSelected(DataRangeSelectionEvent event){
//         Object[] listeners;
//         listeners = listenerList.getListenerList();
//         for (int i = listeners.length-2; i>=0; i-=2) {
//             if (listeners[i]==org.das2.event.DataRangeSelectionListener.class) {
//                 ((org.das2.event.DataRangeSelectionListener)listeners[i+1]).dataRangeSelected(event);
//             }
//         }
//     }
// }
// 
// 
// 
