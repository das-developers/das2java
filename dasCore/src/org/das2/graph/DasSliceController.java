package org.das2.graph;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import org.das2.qds.QDataSet;
import org.das2.util.GrannyTextRenderer;

/**
 *
 * @author leiffert
 */
public class DasSliceController extends DasCanvasComponent {

    private boolean inDebugMode = false;

    private Datum lDatum;
    public static final String PROP_LDATUM = "lDatum";

    public Datum getlDatum() {
        return lDatum;
    }

    public void setlDatum(Datum lDatum) {
        Datum oldLDatum = this.lDatum;
        if (oldLDatum.equals(lDatum)) {
            return;
        }
        if(lDatum.gt(rDatum)){
            this.lDatum = rDatum;
        }else{
            this.lDatum = lDatum;
        }
        lDatumRect.text = this.lDatum.toString();
        firePropertyChange(PROP_LDATUM, oldLDatum, lDatum);
    }
    
    private boolean checklDatum(Datum oldLDatum, Datum newLDatum){
        if(oldLDatum.equals(newLDatum)){
            return false;
        }
        if(oldLDatum.equals(rDatum)){
            rDatum = newLDatum;
            this.lDatum = newLDatum;
            return true;
        }else if(newLDatum.gt(rDatum)){
            
            return false;
        }
        return false;
    }

    private Datum rDatum;
    public static final String PROP_RDATUM = "rDatum";

    public Datum getrDatum() {
        return rDatum;
    }

    public void setrDatum(Datum rDatum) {
        Datum oldRDatum = this.rDatum;
        if (oldRDatum.equals(rDatum)) {
            return;
        }
        if(rDatum.lt(lDatum)){
            this.rDatum = lDatum;
        }else{
            this.rDatum = rDatum;
        }
        rDatumRect.text = this.rDatum.toString();
        firePropertyChange(PROP_RDATUM, oldRDatum, rDatum);
    }

    class TextRect {

        String text;

        double lRatio;
        double rRatio;

        private int getWidth() {
            return (int) ((rRatio - lRatio) * width);
        }

        Cursor cursor = new Cursor(Cursor.DEFAULT_CURSOR);

        Rectangle rect;

        protected void setRect() {
            this.rect.setBounds((int) (lRatio * width), colMin, getWidth(), height);
        }

        boolean isShowing = false;

        public TextRect(String text, Rectangle rect) {
            this.text = text;
            this.rect = rect;
        }

        public TextRect(String text, Rectangle rect, Cursor c) {
            this(text, rect);
            this.cursor = c;
        }

        private void paint(GrannyTextRenderer gtr, Graphics g) throws Exception {

            gtr.setString(g, this.text);
            int txtWidth = (int) Math.ceil(gtr.getWidth());
            int sparePixels = this.getWidth() - txtWidth;
            if (sparePixels < 0) {
                throw new Exception(" Rectangle is too small for text. ");
            }
            sparePixels = sparePixels / 2;
            if (isShowing) {
                gtr.draw(g, this.rect.x + sparePixels, height / 2);
//                g.drawRect(this.rect.x, this.rect.y, this.rect.width, this.rect.height);
//                if(this.text.equals("scan>>") || this.text.equals("<<scan")){
//                    g.drawRect(this.rect.x, height / 2 - (int) gtr.getHeight(), this.rect.width, (int) gtr.getHeight());
//                }
            }

            if (inDebugMode) {
                g.drawRect(this.rect.x, this.rect.y, this.rect.width, this.rect.height);
                if (mouseRect == this) {
                    g.setColor(new Color(.5f, .5f, 0, .2f));
                    g.fillRect(mouseRect.rect.x, mouseRect.rect.y, mouseRect.rect.width, mouseRect.rect.height);
                    g.setColor(Color.black);
                }
            }
        }
    }

    private TextRect lDatumRect;
    private TextRect rDatumRect;
    private TextRect addRDatum = new TextRect("+", new Rectangle());
    private TextRect toTextRect = new TextRect("to", new Rectangle());
    private TextRect rScan = new TextRect("scan>>", new Rectangle());
    private TextRect lScan = new TextRect("<<scan", new Rectangle());

    public DasSliceController(Datum lDatum, Datum rDatum) {
        this.lDatum = lDatum;
        this.rDatum = rDatum;
        lDatumRect = new TextRect(lDatum.toString(), new Rectangle(), new Cursor(Cursor.W_RESIZE_CURSOR));
        lDatumRect.isShowing = true;
        rDatumRect = new TextRect(rDatum.toString(), new Rectangle(), new Cursor(Cursor.E_RESIZE_CURSOR));
        rDatumRect.isShowing = true;
        toTextRect.isShowing = true;
        MouseAdapter ma = getMouseAdapter();
        addMouseMotionListener(ma);
        addMouseWheelListener(ma);
        addMouseListener(ma);
    }

    private int colMin;
    private int colMax;
    private int rowMin;
    private int rowMax;
    private int width;
    private int height;

    private void setSizingParams() {
        colMin = getColumn().getDMinimum();
        colMax = getColumn().getDMaximum();
        rowMin = getRow().getDMinimum();
        rowMax = getRow().getDMaximum();
        width = colMax - colMin;
        height = rowMax - rowMin;
    }

    private void setLayoutRatios() {
        if (lDatum.equals(rDatum)) {
            lScan.lRatio = 0;
            lScan.rRatio = 0.15;
            lScan.setRect();
            lDatumRect.lRatio = 0.15;
            lDatumRect.rRatio = 0.75;
            lDatumRect.setRect();
            addRDatum.lRatio = 0.75;
            addRDatum.rRatio = 0.85;
            addRDatum.setRect();
            rScan.lRatio = 0.85;
            rScan.rRatio = 1;
            rScan.setRect();

        } else {
            lScan.lRatio = 0;
            lScan.rRatio = 0.1;
            lScan.setRect();
            lDatumRect.lRatio = 0.1;
            lDatumRect.rRatio = 0.45;
            lDatumRect.setRect();
            toTextRect.lRatio = 0.45;
            toTextRect.rRatio = 0.55;
            toTextRect.setRect();
            rDatumRect.lRatio = 0.55;
            rDatumRect.rRatio = 0.9;
            rDatumRect.setRect();
            rScan.lRatio = 0.9;
            rScan.rRatio = 1;
            rScan.setRect();
        }
    }

    private TextRect[] layoutAry = new TextRect[5];

    private void setlayoutAry() {

        if (lDatum.equals(rDatum)) {
            layoutAry[0] = lScan;
            layoutAry[1] = lDatumRect;
            layoutAry[2] = addRDatum;
            layoutAry[3] = rScan;
            layoutAry[4] = null;
        } else {
            layoutAry[0] = lScan;
            layoutAry[1] = lDatumRect;
            layoutAry[2] = toTextRect;
            layoutAry[3] = rDatumRect;
            layoutAry[4] = rScan;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {

        float fontMultiplier = 2f;
        g.setFont(g.getFont().deriveFont(g.getFont().getSize() * fontMultiplier));

        setSizingParams();
        setLayoutRatios();
        setlayoutAry();

        GrannyTextRenderer gtr = new GrannyTextRenderer();
        for (TextRect tr : layoutAry) {
            if (tr == null) {
                continue;
            }
            try {
                tr.paint(gtr, g);
            } catch (Exception ex) {
                Logger.getLogger(DasSliceController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private TextRect mouseRect;

    private void setMouseRect(int x, int y) {
        TextRect trBuf = null;
        for (TextRect tr : layoutAry) {
            if (tr != null && tr.rect.contains(x, y)) {
                trBuf = tr;
                trBuf.isShowing = true;
                break;
            }
        }
        if (mouseRect != trBuf && mouseRect != null) {
            if (mouseRect != lDatumRect && mouseRect != rDatumRect && mouseRect != toTextRect) {
                mouseRect.isShowing = false;
            }
            repaint(mouseRect.rect);
        }
        mouseRect = trBuf;
        if (mouseRect != null) {
            getCanvas().getGlassPane().setCursor(mouseRect.cursor);
            repaint(mouseRect.rect);
        }
    }
    private boolean isDragging;
    private Point pressedPoint;
    private Datum lDatumUpdating;
    private Datum rDatumUpdating;

    private MouseAdapter getMouseAdapter() {
        return new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
//                System.err.println("button clicked = " + e.getButton());
                setMouseRect(e.getX(), e.getY());
                if (e.getButton() == 1) {
                    if (mouseRect == lScan) {
                        scanLeft();
                    } else if (mouseRect == rScan) {
                        scanRight();
                    } else if (mouseRect == addRDatum) {
                        addRDatum();
                    }
                }

            }

            @Override
            public void mousePressed(MouseEvent e) {
                setMouseRect(e.getX(), e.getY());
                pressedPoint = new Point(e.getX(), e.getY());
                
                if (mouseRect == lDatumRect || mouseRect == rDatumRect) {
                    isDragging = true;
                    lDatumUpdating = lDatum;
                    rDatumUpdating = rDatum;
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                int xDrag = e.getX() - pressedPoint.x;
                int yDrag = e.getY() - pressedPoint.y;

                // mouseRect isn't updated during drag so its value is 
                // whatever it was on mousePress
                if (mouseRect == lDatumRect) {
                    lDatumDrag(xDrag);
                } else if (mouseRect == rDatumRect) {
                    rDatumDrag(xDrag);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (isDragging) {
                    setlDatum(lDatumUpdating);
                    setrDatum(rDatumUpdating);
                    isDragging = false;
                }

                setMouseRect(e.getX(), e.getY());
                repaint();
            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!isDragging) {
                    if (mouseRect != lDatumRect && mouseRect != toTextRect && mouseRect != rDatumRect) {
                        mouseRect.isShowing = false;
                    }
                    mouseRect = null;
                    repaint();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                if (!isDragging) {
                    setMouseRect(e.getX(), e.getY());
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (mouseRect == lDatumRect || mouseRect == toTextRect || mouseRect == rDatumRect) {
                    wheelRotation(e.getWheelRotation());
                    repaint();
                }
            }
        };
    }

    private void scanLeft() {
        System.err.println("ScanLeft clicked");
        if (lDatum.equals(rDatum)) {
            Datum singleScanDatum = Datum.create(1.0, lDatum.getUnits());
            //Also increment rDatum so the layout doesn't change.
            setrDatum(rDatum.subtract(singleScanDatum));
            setlDatum(lDatum.subtract(singleScanDatum));
        } else {
            Datum datumWidth = getrDatum().subtract(getlDatum());
            setrDatum(getlDatum());
            setlDatum(getlDatum().subtract(datumWidth));
        }
    }

    private void scanRight() {
        System.err.println("ScanRight clicked");
        if (lDatum.equals(rDatum)) {
            Datum singleScanDatum = Datum.create(1.0, lDatum.getUnits());
            setrDatum(rDatum.add(singleScanDatum));
            setlDatum(lDatum.add(singleScanDatum));
        } else {
            Datum datumWidth = getrDatum().subtract(getlDatum());
            setlDatum(getrDatum());
            setrDatum(getrDatum().add(datumWidth));
        }
    }

    private void addRDatum() {
        System.err.println("AddRDatum clicked");
        Datum distFromLDatum = Datum.create(1, lDatum.getUnits());
        setrDatum(getlDatum().add(distFromLDatum));
    }

    private void lDatumDrag(int xTotalDrag) {
        System.err.println("lDatumDragging, total dist = " + xTotalDrag);
        Datum dragDatum = Datum.create(xTotalDrag, lDatum.getUnits());
        if (lDatum.equals(rDatum)) {
            lDatumUpdating = lDatum.add(dragDatum);
            lDatumRect.text = lDatumUpdating.toString();
            rDatumUpdating = lDatumUpdating;
        } else {
            lDatumUpdating = lDatum.add(dragDatum);
            if (lDatumUpdating.gt(rDatum)) {
                lDatumUpdating = rDatum;
                lDatumRect.text = rDatum.toString();
            } else {
                lDatumRect.text = lDatumUpdating.toString();
            }

        }
    }

    private void rDatumDrag(int xTotalDrag) {
        System.err.println("rDatumDragging, total dist = " + xTotalDrag);
        Datum dragDatum = Datum.create(xTotalDrag, lDatum.getUnits());
        rDatumUpdating = rDatum.add(dragDatum);
        if (rDatumUpdating.lt(lDatum)) {
            rDatumUpdating = lDatum;
            rDatumRect.text = lDatum.toString();
        } else {
            rDatumRect.text = rDatumUpdating.toString();
        }
    }

    private void wheelRotation(int amount) {
        System.err.println("Wheel rotation amount = " + amount);
        Datum datumAmount = Datum.create(amount, lDatum.getUnits());
        if (lDatum.equals(rDatum)) {
            setrDatum(rDatum.add(datumAmount));
            setlDatum(lDatum.add(datumAmount));
        } else {
            if (amount > 0) {
                setrDatum(rDatum.add(datumAmount));
                setlDatum(lDatum.subtract(datumAmount));
            } else if (amount < 0) {
                // Allow narrowing of range only via scroll, but never allow 
                // left and right datum to meet.  
                if (rDatum.add(datumAmount).gt(lDatum.subtract(datumAmount))
                        || lDatum.subtract(datumAmount).lt(rDatum.add(datumAmount))) {

                    setrDatum(rDatum.add(datumAmount));
                    setlDatum(lDatum.subtract(datumAmount));
                }

            }
        }
    }

//    public DasSliceController(QDataSet qds) {
//        if (qds.rank() != 1) {
//            throw new IllegalArgumentException("Dataset is not rank 1."
//                    + "Slice the data in the dimension you want "
//                    + "before calling DasSliceController().");
//        }
//        this.qds = qds;
//        lDatumRect = new DatumRect(qds, 0, new Rectangle());
//        rDatumRect = new DatumRect(qds, 0, new Rectangle());
////        lDatumRect = new DatumRect(Datum.create(qds.value(0), (Units) qds.property(QDataSet.UNITS)), new Rectangle());
////        rDatumRect = new DatumRect(Datum.create(qds.value(1), (Units) qds.property(QDataSet.UNITS)), new Rectangle());
//
//        lDatumRect.cursor = wResizeCursor;
//        rDatumRect.cursor = eResizeCursor;
//        MouseAdapter ma = getMouseAdapter();
//        addMouseMotionListener(ma);
//        addMouseWheelListener(ma);
//        addMouseListener(ma);
//        setRecAry();
//
//    }
//    private void mouseClick(){
//        
//        if(mouseRect == addRDatum){
//            rDatumRect.setIndex(rDatumRect.getIndex() +2);
//            repaint();
//        }else if(mouseRect == lScan){
//            if(lDatumRect.getIndex() == rDatumRect.getIndex()){
//                if(lDatumRect.getIndex() > 0){
//                    rDatumRect.setIndex(rDatumRect.getIndex() - 1);
//                    lDatumRect.setIndex(lDatumRect.getIndex() - 1);
//                }
//                
//            }else{
//                int width = rDatumRect.getIndex() - lDatumRect.getIndex();
//                int lToZero = lDatumRect.getIndex();
//                if(lToZero < width){
//                    rDatumRect.setIndex(rDatumRect.getIndex() - lToZero);
//                    lDatumRect.setIndex(0);
//                }else{
//                    rDatumRect.setIndex(rDatumRect.getIndex() - width);
//                    lDatumRect.setIndex(lDatumRect.getIndex() - width);
//                }
//            }
//        }else if(mouseRect == rScan){
//            if(lDatumRect.getIndex() == rDatumRect.getIndex()){
//                if(rDatumRect.getIndex() < qds.length() - 1){
//                    rDatumRect.setIndex(rDatumRect.getIndex() + 1);
//                    lDatumRect.setIndex(lDatumRect.getIndex() + 1);
//                }
//                
//            }else{
//                int width = rDatumRect.getIndex() - lDatumRect.getIndex();
//                int rToMax = qds.length() - 1 - rDatumRect.getIndex();
//                if(width > rToMax){
//                    rDatumRect.setIndex(qds.length( ) - 1);
//                    lDatumRect.setIndex(lDatumRect.getIndex() + rToMax);
//                }else{
//                    rDatumRect.setIndex(rDatumRect.getIndex() + width);
//                    lDatumRect.setIndex(lDatumRect.getIndex() + width);
//                }
//            }
//        }
//    }
//    Point mousePressedPoint;
//    int pressIndex;
//    TextRect mousePressedRect;
//    private void setMousePressedParams(Point p){
//        isDragging = true;
//        mousePressedPoint = p;
//        mousePressedRect = null;
//        for(TextRect tr: layoutAry){
//            if(tr != null && tr.rect.contains(p)){
//                mousePressedRect = tr;
//                break;
//            }
//        }
//        if(mousePressedRect instanceof DatumRect){
//            pressIndex = ((DatumRect) mousePressedRect).getIndex();
//        }
//    }
//    
//    private void updateValuesViaDrag(Point p){
//        int dragX = p.x - mousePressedPoint.x;
//        int dragY = p.y - mousePressedPoint.y;
//        int newIndex = pressIndex + dragX / 100;
//        if(mousePressedRect == lDatumRect){
//            if(lDatumRect.index == rDatumRect.index && changeLayout){
//                rDatumRect.setIndex(newIndex);
//                lDatumRect.setIndex(newIndex);
////                repaint();
//            } else{
//                if(newIndex >= rDatumRect.getIndex()){
//                    changeLayout = false;
//                    lDatumRect.setIndex(rDatumRect.getIndex());
////                    repaint();
//                }else if(newIndex <= 0){
//                    lDatumRect.setIndex(0);
//                } else{
//                    lDatumRect.setIndex(newIndex);
//                }
//            }
//        } else if(mousePressedRect == rDatumRect){
//           if(newIndex <= lDatumRect.getIndex()){
//               changeLayout = false;
//               rDatumRect.setIndex(lDatumRect.getIndex());
//           }else if(newIndex >= qds.length() - 1){
//               rDatumRect.setIndex(qds.length() - 1);
//           }else{
//               rDatumRect.setIndex(newIndex);
//           }
//        }
//    }
//    
//    private void wheelRotated(int amt){
//        if(mouseRect == lDatumRect || mouseRect == toTextRect || mouseRect == rDatumRect){
//            if(lDatumRect.getIndex() == rDatumRect.getIndex()){
//                rDatumRect.setIndex(rDatumRect.getIndex() + amt);
//                lDatumRect.setIndex(lDatumRect.getIndex() + amt);
//                repaint();
//            }else if(lDatumRect.getIndex() - amt < rDatumRect.getIndex() &&
//                     rDatumRect.getIndex() + amt > lDatumRect.getIndex()){
//
//                rDatumRect.setIndex(rDatumRect.getIndex() + amt);
//                lDatumRect.setIndex(lDatumRect.getIndex() - amt);
//                repaint();
//                
//            }
//        }
//    }
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
