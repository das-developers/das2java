package org.das2.graph;

import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import static javax.swing.JLayeredPane.LAYER_PROPERTY;
import org.das2.components.propertyeditor.Displayable;
import org.das2.components.propertyeditor.Enumeration;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import static org.das2.graph.DasCanvas.AXIS_LAYER;
import org.das2.util.GrannyTextRenderer;

/**
 *
 * @author leiffert
 */
public class DasSliceController extends DasCanvasComponent {

    private boolean inDebugMode = true;
    
    public static enum Alignment {
        LEFT, RIGHT, CENTER;
    }
    
    private Alignment alignment = Alignment.CENTER;

    public Alignment getAlignment() {
        return alignment;
    }

    public void setAlignment(Alignment alignment) {
        this.alignment = alignment;
        update();
//        repaint();
    }
    
    public static enum VerticalAlignment{
        TOP, BOTTOM
    }
    
    private VerticalAlignment verticalAlignment = VerticalAlignment.TOP;
    
    public VerticalAlignment getVerticalAlignment(){
        return this.verticalAlignment;
    }
    
    public void setVerticalAlignment(VerticalAlignment va){
        this.verticalAlignment = va;
        update();
    }
    
    public void setInDebugMode(boolean inDebug){
        this.inDebugMode = inDebug;
    }
    
    public boolean getInDebugMode(){
        return this.inDebugMode;
    }

    private DatumRange datumRange;
    public static final String PROP_DATUMRANGE = "datumRange";
    
    public DatumRange getDatumRange(){
        return datumRange;
    }
    
    public void setDatumRange(DatumRange dr){
        DatumRange oldDR = this.datumRange;
        this.datumRange = dr;
        lDatumRect.text = this.datumRange.min().toString();
        rDatumRect.text = this.datumRange.max().toString();
        firePropertyChange(PROP_DATUMRANGE, oldDR, dr);
        update();
//        repaint();
    }
    
    
    private String wordsAfterLabelSingleDatum = " at ";
    public void setWordsAfterLabelSingleDatum(String newWords){
        this.wordsAfterLabelSingleDatum = newWords;
        setDsLabel(dsLabel);
    }
    
    public String getWordsAfterLabelSingleDatum(){
        return wordsAfterLabelSingleDatum;
    }
    
    private String wordsAfterLabelDatumRange = " average over ";
    public void setWordsAfterLabelDatumRange(String newWords){
        wordsAfterLabelDatumRange = newWords;
        setDsLabel(dsLabel);
    }
    public String getWordsAfterLabelDatumRange(){
        return wordsAfterLabelDatumRange;
    }
    
    private String dsLabel;
    public String getDsLabel() {
        return dsLabel;
    }

    public void setDsLabel(String dsLabel) {
        this.dsLabel = dsLabel;
        if(inSingleMode){
            dsLabelRect.text = " " + dsLabel + wordsAfterLabelSingleDatum;
        }else{
            dsLabelRect.text = " " + dsLabel + wordsAfterLabelDatumRange ;
        }
        repaint();
    }
    
    
//    // Number of pixels left below the the text rects for animation. 
//    private int roomForAnimation = 10;
//
//    public int getRoomForAnimation() {
//        return roomForAnimation;
//    }
//
//    public void setRoomForAnimation(int roomForAnimation) {
//        this.roomForAnimation = roomForAnimation;
//        repaint();
//    }
    
    
    private int offsetFromPlot = 0;
    
    public int getOffsetFromPlot(){
        return offsetFromPlot;
    }
    
    public void setOffsetFromPlot(int newOffset){
        this.offsetFromPlot = newOffset;
        update();
    }
    
    
    class TextRect {

        String text;

//        double lRatio;
//        double rRatio;
//
//        private int getWidthFromRatio() {
//            return (int) ((rRatio - lRatio) * layoutWidth);
//        }
        
        int lPixel;
        int rPixel;
        
        private int getPixelWidth(){
            return rPixel - lPixel;
        }
        
//        int tPixel;
//        int bPixel;
//        
//        private int getPixelHeight(){
//            return bPixel - tPixel;
//        }

        Cursor cursor = new Cursor(Cursor.DEFAULT_CURSOR);

        Rectangle rect;

        protected void setRect() {
            // Relative to canvas origin
//            this.rect.setBounds(lPixel + colMin, rowMax -  ( bPixel - tPixel) - roomForAnimation , getPixelWidth(), getPixelHeight());
//            this.rect.setBounds(lPixel + colMin, rowMin - getPixelHeight() - (int) getEmSize(), getPixelWidth(), getPixelHeight() );
            this.rect.setBounds(lPixel + colMin, rowMin - (int) (2 * getEmSize()) - offsetFromPlot, getPixelWidth(), (int) (2 * getEmSize() ));
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
//            int txtWidth = (int) Math.ceil(gtr.getWidth());
//            int sparePixels = this.getWidthFromRatio() - txtWidth;
//            if (sparePixels < 0) {
//                throw new Exception(" Rectangle is too small for text. ");
//            }
//            sparePixels = sparePixels / 2;
            if (isShowing) {
                
//                gtr.draw(g, this.rect.x + sparePixels, this.rect.y + this.rect.height / 2);
                if(verticalAlignment == VerticalAlignment.TOP){
                    gtr.draw(g, this.rect.x, rowMin - (int) ( getEmSize()) - offsetFromPlot);
                }
//                gtr.draw(g, this.rect.x , this.rect.y + (2 * this.rect.height / 3 ) ); // This seems to center the text nicely
//                g.drawRect(this.rect.x, this.rect.y, this.rect.width, this.rect.height);
//                if(this.text.equals(" scan>> ") || this.text.equals(" <<scan ")){
                if(this == addRDatum || this == lScan || this == rScan){
                    g.drawRect(this.rect.x, this.rect.y, this.rect.width - 1, this.rect.height - (int)(0.6 * getEmSize()));
                }
            }

            if (inDebugMode) {
                g.drawRect(this.rect.x, this.rect.y, this.rect.width, this.rect.height -1);
                if (mouseRect == this) {
                    g.setColor(new Color(.5f, .5f, 0, .2f));
                    g.fillRect(mouseRect.rect.x, mouseRect.rect.y, mouseRect.rect.width, mouseRect.rect.height);
                    g.setColor(Color.black);
                }
            }
        }
    }
    
    

    private TextRect dsLabelRect = new TextRect(" Data: ", new Rectangle());
    private TextRect lDatumRect;
    private TextRect rDatumRect;
    private TextRect addRDatum = new TextRect(" + ", new Rectangle());
    private TextRect toTextRect = new TextRect(" to ", new Rectangle(), new Cursor(Cursor.MOVE_CURSOR));
    private TextRect rScan = new TextRect(" scan>> ", new Rectangle());
    private TextRect lScan = new TextRect(" <<scan ", new Rectangle());

    public DasSliceController(Datum lDatum, Datum rDatum) {

        this.datumRange = new DatumRange(lDatum, rDatum);
        lDatumRect = new TextRect(lDatum.toString(), new Rectangle(), new Cursor(Cursor.W_RESIZE_CURSOR));
        lDatumRect.isShowing = true;
        rDatumRect = new TextRect(rDatum.toString(), new Rectangle(), new Cursor(Cursor.E_RESIZE_CURSOR));
        rDatumRect.isShowing = true;
        toTextRect.isShowing = true;
        dsLabelRect.isShowing = true;
        if(this.datumRange.min().equals(this.datumRange.max())){
            inSingleMode = true;
        }
        MouseAdapter ma = getMouseAdapter();
        addMouseMotionListener(ma);
        addMouseWheelListener(ma);
        addMouseListener(ma);
        putClientProperty(LAYER_PROPERTY, AXIS_LAYER);
        
    }
    
    public DasSliceController(DatumRange dr){
        this(dr.min(), dr.max());
    }
    public DasSliceController(){
        this(Datum.create(0, Units.dimensionless), Datum.create(10, Units.dimensionless));
    }
    
    @Override
    public void resize(){
        setSizingParams();
        setLayout(this.inSingleMode, new GrannyTextRenderer());
        setlayoutAry(this.inSingleMode);
        Rectangle rect = new Rectangle(lScan.rect);
        rect.add(dsLabelRect.rect);
        rect.add(lDatumRect.rect);
        if(inSingleMode){
            rect.add(addRDatum.rect);
        }else{
            rect.add(toTextRect.rect);
            rect.add(rDatumRect.rect);
        }
        rect.add(rScan.rect);
        setBounds(rect);
        
    }

    private int colMin;
    private int colMax;
    private int rowMin;
    private int rowMax;
//    private int layoutWidth;
//    private int layoutHeight;

    private void setSizingParams() {
        colMin = getColumn().getDMinimum();
        colMax = getColumn().getDMaximum();
        rowMin = getRow().getDMinimum();
        rowMax = getRow().getDMaximum();
//        layoutWidth = colMax - colMin;
//        layoutHeight = rowMax - rowMin;
    }
//    
//    private int floatRight = 0;
//
//    public int getFloatRight() {
//        return floatRight;
//    }
//
//    public void setFloatRight(int floatRight) {
//        this.floatRight = floatRight;
//        repaint();
//    }
//    
//    private int floatDown = 0;
//    public int getFloatDown(){
//        return floatDown;
//    }
//    
//    public void setFloatDown(int fDown){
//        this.floatDown = fDown;
//        repaint();
//    }
//    
    private void setLayout(boolean inSingle, GrannyTextRenderer gtr){
    
        
        int totalWidth = 0;
        if(inSingle){
            
            gtr.setString(getFont(), lScan.text);
            int width = (int) gtr.getWidth();
            totalWidth += width;
//            int height = (int) gtr.getHeight();
//            lScan.lPixel = floatRight - width;
            lScan.lPixel =  - width;
            // The plus 1 is safety from the cast to int on width
            lScan.rPixel = lScan.lPixel + width + 1;
            
            
            gtr.setString(getFont(), dsLabelRect.text);
            width = (int) gtr.getWidth();
            totalWidth += width;
            // Find the largest height so all the text rects are the same height
//            height = (int) gtr.getHeight() > height ? (int) gtr.getHeight() : height;
            dsLabelRect.lPixel = lScan.rPixel;
            dsLabelRect.rPixel = dsLabelRect.lPixel + width + 1;
            
            
//            gtr.setString(g, datumRange.min().getFormatter().format(datumRange.min(), datumRange.getUnits()) + datumRange.getUnits());
            gtr.setString(getFont(), lDatumRect.text);
            width = (int) gtr.getWidth();
            totalWidth += width;
//            height = (int) gtr.getHeight() > height ? (int) gtr.getHeight() : height;
            lDatumRect.lPixel = dsLabelRect.rPixel;
            lDatumRect.rPixel = lDatumRect.lPixel + width + 1;
            
            
            gtr.setString(getFont(), addRDatum.text);
            width = (int) gtr.getWidth();
            totalWidth += width;
//            height = (int) gtr.getHeight() > height ? (int) gtr.getHeight() : height;
            addRDatum.lPixel = lDatumRect.rPixel;
            addRDatum.rPixel = addRDatum.lPixel + width + 1;
            
            
            gtr.setString(getFont(), rScan.text);
            width = (int) gtr.getWidth();
//            height = (int) gtr.getHeight() > height ? (int) gtr.getHeight() : height;
            rScan.lPixel = addRDatum.rPixel;
            rScan.rPixel = rScan.lPixel + width + 1;
            
            
            int xOffset = 0;
            switch(this.alignment){
                case LEFT:
                    xOffset = 0;
                    break;
                case CENTER:
                    xOffset = (colMax - colMin - totalWidth) / 2;
                    break;
                case RIGHT:
                    xOffset = (colMax - colMin - totalWidth);
                    break;
                default:
                    xOffset = 0;
                    break;
                    
            }
            
            lScan.lPixel += xOffset;
            lScan.rPixel += xOffset;
            dsLabelRect.lPixel += xOffset;
            dsLabelRect.rPixel += xOffset;
            lDatumRect.lPixel += xOffset;
            lDatumRect.rPixel += xOffset;
            addRDatum.lPixel += xOffset;
            addRDatum.rPixel += xOffset;
            rScan.lPixel += xOffset;
            rScan.rPixel += xOffset;
            
//            height *= 2; // Characters like 'g' would get cut off.
            
            // Setting top and bottom pixels after largest height was found
//            lScan.tPixel = floatDown;
//            lScan.bPixel = lScan.tPixel + height + 1;
            lScan.setRect();
            
//            dsLabelRect.tPixel = floatDown;
//            dsLabelRect.bPixel = dsLabelRect.tPixel + height + 1;
            dsLabelRect.setRect();
            
//            lDatumRect.tPixel = floatDown;
//            lDatumRect.bPixel = lDatumRect.tPixel + height + 1;
            lDatumRect.setRect();
            
//            addRDatum.tPixel = floatDown;
//            addRDatum.bPixel = addRDatum.tPixel + height + 1;
            addRDatum.setRect();
            
//            rScan.tPixel = floatDown;
//            rScan.bPixel = rScan.tPixel + height + 1;
            rScan.setRect();
                 
        }else{
            
            
            gtr.setString(getFont(), lScan.text);
            int width = (int) gtr.getWidth();
//            int height = (int) gtr.getHeight();
//            lScan.lPixel = floatRight - width;
            lScan.lPixel =  - width;
            // The plus 1 is safety from the cast to int on width
            lScan.rPixel = lScan.lPixel + width + 1;
            
            
            gtr.setString(getFont(), dsLabelRect.text);
            width = (int) gtr.getWidth();
            totalWidth += width;
//            height = (int) gtr.getHeight() > height ? (int) gtr.getHeight() : height;
//            dsLabelRect.lPixel = floatRight;
            dsLabelRect.lPixel = 0;
            dsLabelRect.rPixel = dsLabelRect.lPixel + width + 1;
            
            
//            gtr.setString(g, datumRange.min().getFormatter().format(datumRange.min(), datumRange.getUnits()));
            gtr.setString(getFont(), lDatumRect.text);
            width = (int) gtr.getWidth();
            totalWidth += width;
//            height = (int) gtr.getHeight() > height ? (int) gtr.getHeight() : height;
            lDatumRect.lPixel = dsLabelRect.rPixel;
            lDatumRect.rPixel = lDatumRect.lPixel + width + 1;
            
            
            gtr.setString(getFont(), toTextRect.text);
            width = (int) gtr.getWidth();
            totalWidth += width;
//            height = (int) gtr.getHeight() > height ? (int) gtr.getHeight() : height;
            toTextRect.lPixel = lDatumRect.rPixel;
            toTextRect.rPixel = toTextRect.lPixel + width + 1;
            
            
//            gtr.setString(g, datumRange.max().getFormatter().format(datumRange.max(), datumRange.getUnits()) + datumRange.getUnits());
            gtr.setString(getFont(), rDatumRect.text);
            width = (int) gtr.getWidth();
            totalWidth += width;
//            height = (int) gtr.getHeight() > height ? (int) gtr.getHeight() : height;
            rDatumRect.lPixel = toTextRect.rPixel;
            rDatumRect.rPixel = rDatumRect.lPixel + width + 1;
            
            
            gtr.setString(getFont(), rScan.text);
            width = (int) gtr.getWidth();
//            height = (int) gtr.getHeight() > height ? (int) gtr.getHeight() : height;
            rScan.lPixel = rDatumRect.rPixel;
            rScan.rPixel = rScan.lPixel + width + 1;
            
            int xOffset = 0;
            switch(this.alignment){
                case LEFT:
                    xOffset = 0;
                    break;
                case CENTER:
                    xOffset = (colMax - colMin - totalWidth) / 2;
                    break;
                case RIGHT:
                    xOffset = (colMax - colMin - totalWidth);
                    break;
                default:
                    xOffset = 0;
                    break;
                    
            }
//            int xOffset = colMax - colMin - totalWidth;
//            int xOffset = (colMax - colMin -totalWidth) / 2;
//            int xOffset = 0;
            lScan.lPixel += xOffset;
            lScan.rPixel += xOffset;
            dsLabelRect.lPixel += xOffset;
            dsLabelRect.rPixel += xOffset;
            lDatumRect.lPixel += xOffset;
            lDatumRect.rPixel += xOffset;
            toTextRect.lPixel += xOffset;
            toTextRect.rPixel += xOffset;
            rDatumRect.lPixel += xOffset;
            rDatumRect.rPixel += xOffset;
            rScan.lPixel += xOffset;
            rScan.rPixel += xOffset;
            
//            height *= 2; 
            
//            lScan.tPixel = floatDown;
//            lScan.bPixel = lScan.tPixel + height + 1;
            lScan.setRect();
            
//            dsLabelRect.tPixel = floatDown;
//            dsLabelRect.bPixel = dsLabelRect.tPixel + height + 1;
            dsLabelRect.setRect();
            
//            lDatumRect.tPixel = floatDown;
//            lDatumRect.bPixel = lDatumRect.tPixel + height + 1;
            lDatumRect.setRect();
            
//            toTextRect.tPixel = floatDown;
//            toTextRect.bPixel = toTextRect.tPixel + height + 1;
            toTextRect.setRect();
            
//            rDatumRect.tPixel = floatDown;
//            rDatumRect.bPixel = rDatumRect.tPixel + height + 1;
            rDatumRect.setRect();
            
//            rScan.tPixel = floatDown;
//            rScan.bPixel = rScan.tPixel + height + 1;
            rScan.setRect();
                    
        }
    }

    

    private TextRect[] layoutAry = new TextRect[6];

    private void setlayoutAry(boolean inSingle) {

        if(inSingle){
            layoutAry[0] = lScan;
            layoutAry[1] = dsLabelRect;
            layoutAry[2] = lDatumRect;
            layoutAry[3] = addRDatum;
            layoutAry[4] = rScan;
            layoutAry[5] = null;
        } else {
            layoutAry[0] = lScan;
            layoutAry[1] = dsLabelRect;
            layoutAry[2] = lDatumRect;
            layoutAry[3] = toTextRect;
            layoutAry[4] = rDatumRect;
            layoutAry[5] = rScan;
        }
    }
    
    private boolean inSingleMode;
    
    public void setInSingleMode(boolean inSingle){
        
        this.inSingleMode = inSingle;
        setDsLabel(dsLabel);
        setSizingParams();
        setlayoutAry(inSingle);
        repaint();
//        setLayoutRatios(inSingle);
        
    }
    
    public void resetInSingleMode(){
        if(datumRange.min().equals(datumRange.max())){
            setInSingleMode(true);
        }else{
            setInSingleMode(false);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        
        if(inDebugMode){
            Color cColor = g.getColor();
            g.setColor(Color.MAGENTA);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(cColor);
        }
        
        // Drawing coordinates should be relative to canvas origin
        g.translate(-getX(), -getY());
        
        float fontMultiplier = 1f;
        g.setFont(g.getFont().deriveFont(g.getFont().getSize() * fontMultiplier));


        GrannyTextRenderer gtr = new GrannyTextRenderer();
        setSizingParams();
//        setLayoutRatios(this.inSingleMode);
        setLayout(this.inSingleMode, gtr);
        setlayoutAry(this.inSingleMode);

        
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
        
        //Paint the animation
        if(showAnimation){
            switch(animationMode){
                case(LEFT_ANIMATION):
                    leftDatumClickAnimation(g);
                    break;
                case(RIGHT_ANIMATION):
                    rightDatumClickAnimation(g);
                    break;
                case(CENTER_ANIMATION):
                    centerAnimation(g);
                    break;
                default:
                    System.err.println("Animation not set to valid state");
                    break;
                    
            }
//            centerAnimation(g);
        }
        
    }

    private TextRect mouseRect;

    private void setMouseRect(int x, int y) {
        //Relative to canvas origin
        x += getX();
        y += getY();
        TextRect trBuf = null;
        for (TextRect tr : layoutAry) {
            if (tr != null && tr.rect.contains(x, y)) {
                trBuf = tr;
                trBuf.isShowing = true;
                break;
            }
        }
        if (mouseRect != trBuf && mouseRect != null) {
            if (mouseRect != lDatumRect && mouseRect != rDatumRect && mouseRect != toTextRect && mouseRect != dsLabelRect) {
                mouseRect.isShowing = false;
            }
            repaint();
        }
        mouseRect = trBuf;
        if (mouseRect != null) {
            getCanvas().getGlassPane().setCursor(mouseRect.cursor);
            repaint();
        }
    }
    private boolean isDragging = false;
    private boolean startedDragAsRange = false;
    
    // While dragging the event.getButton() always returns 0... So keep track of it here
    private int buttonPress = 0;
    private Point lastPoint;
    private Point pressedPoint;
    
    private boolean showAnimation = false;
    private static final int LEFT_ANIMATION = 0;
    private static final int CENTER_ANIMATION = 1;
    private static final int RIGHT_ANIMATION = 2;
    private int animationMode;
    
    
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
                pressedPoint = e.getPoint();
                lastPoint = e.getPoint();
                if(e.getButton() == 1){
                    buttonPress = 1;
                    if (mouseRect == lDatumRect || mouseRect == rDatumRect || mouseRect == toTextRect) {
                        isDragging = true;
                        if(!inSingleMode){
                            startedDragAsRange = true;
                        }else{
                            startedDragAsRange = false;
                        }
                    }
                } else if(e.getButton() == 2){
                    buttonPress = 2;
                    if(mouseRect == lDatumRect || mouseRect == rDatumRect || mouseRect == toTextRect){
                        isDragging = true;
                        getCanvas().getGlassPane().setCursor(new Cursor(Cursor.MOVE_CURSOR));
                        System.err.println("middle button pressed");
//                        if(!datumRange.min().equals(datumRange.max())){
                        if(!inSingleMode){
                            startedDragAsRange = true;
                        }else{
                            startedDragAsRange = false;
                        }
                    }
                }
                
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                
                int xUpdatingDrag = e.getX() - lastPoint.x;
                int xTotalDrag = e.getX() - pressedPoint.x;
                
                lastPoint = e.getPoint();
//                System.err.println("Button dragging is " + e.getButton());
                
                // mouseRect isn't updated during drag so its value is 
                // whatever it was on mousePress
                if(buttonPress == 1){
//                    System.err.println(" left clickckckc");
                    if (mouseRect == lDatumRect) {
                        showAnimation = true;
//                        animationMode = LEFT_ANIMATION;
                        lDatumDrag(xUpdatingDrag);
                    } else if (mouseRect == rDatumRect) {
                        showAnimation = true;
//                        animationMode = RIGHT_ANIMATION;
                        rDatumDrag(xUpdatingDrag);
                    } else if (mouseRect == toTextRect){
                        showAnimation = true;
//                        animationMode = CENTER_ANIMATION;
                        middleButtonDrag(xUpdatingDrag);
                    }
                    
                } else if(buttonPress == 2){
                    if(mouseRect == lDatumRect || mouseRect == rDatumRect || mouseRect == toTextRect){
                        animationMode = CENTER_ANIMATION;
                        middleButtonDrag(xUpdatingDrag);
                    }
                }
                
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (isDragging) {
                    setDatumRange(datumRange);
                    isDragging = false;
                    startedDragAsRange = false;
                    showAnimation = false;
                }
                resetInSingleMode();
                setMouseRect(e.getX(), e.getY());
                repaint();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
//                setMouseRect(e.getX(), e.getY());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!isDragging) {
                    if (mouseRect != lDatumRect && mouseRect != toTextRect && mouseRect != rDatumRect && mouseRect != dsLabelRect && mouseRect != null) {
                        mouseRect.isShowing = false;
                    }
                    showAnimation = false;
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
//        System.err.println("ScanLeft clicked");
        if (inSingleMode) {
            Datum singleScanDatum = Datum.create(1.0, datumRange.getUnits());

            setDatumRange(new DatumRange(datumRange.min().subtract(singleScanDatum), 
                                        datumRange.max().subtract(singleScanDatum)));
        } else {
            setDatumRange(datumRange.previous());
        }
    }

    private void scanRight() {
//        System.err.println("ScanRight clicked");
        if (inSingleMode) {
            Datum singleScanDatum = Datum.create(1.0, datumRange.getUnits());
            setDatumRange(new DatumRange(datumRange.min().add(singleScanDatum), 
                                        datumRange.max().add(singleScanDatum)));
            
        } else {
            setDatumRange(datumRange.next());
        }
    }

    private void addRDatum() {
//        System.err.println("AddRDatum clicked");
        Datum distFromLDatum = Datum.create(1, datumRange.getUnits());
        setDatumRange(new DatumRange(datumRange.min(), datumRange.min().add(distFromLDatum)));
        resetInSingleMode();
    }

    private void lDatumDrag(int xDrag) {
        showAnimation = true;
//        System.err.println("lDatumDragging, total dist = " + xTotalDrag);
        Datum dragDatum = Datum.create(xDrag, datumRange.getUnits());
        if (inSingleMode) {
            animationMode = CENTER_ANIMATION;
            // Make sure layout won't collapse to a single datum range
            // during drag. If a single datum range is a result of a drag
            // it can be reversed as long a the mouse isn't released.
            if(startedDragAsRange){
                if(datumRange.min().add(dragDatum).gt(datumRange.max())){
                    return;
                }else{
                    setDatumRange(new DatumRange(datumRange.min().add(dragDatum), datumRange.max()));
                    return;
                }
            }
            setDatumRange(new DatumRange(datumRange.min().add(dragDatum), datumRange.min().add(dragDatum)));
            
        } else {
//            animationMode = LEFT_ANIMATION;
            animationMode = CENTER_ANIMATION;
            Datum newLDatum = datumRange.min().add(dragDatum);
            // If left value exceeds right value make it stop at right value.
            if(newLDatum.gt(datumRange.max())){
                setDatumRange(new DatumRange(datumRange.max(), datumRange.max()));
            }else{
                setDatumRange(new DatumRange(newLDatum, datumRange.max()));
            }
        }
    }

    private void rDatumDrag(int xDrag) {
        showAnimation = true;
//        animationMode = RIGHT_ANIMATION;
        animationMode = CENTER_ANIMATION;
//        System.err.println("rDatumDragging, total dist = " + xTotalDrag);
        Datum dragDatum = Datum.create(xDrag, datumRange.getUnits());
        Datum newRDatum = datumRange.max().add(dragDatum);
        
        if (newRDatum.lt(datumRange.min())) {
            setDatumRange(new DatumRange(datumRange.min(), datumRange.min()));
        } else {
            setDatumRange(new DatumRange(datumRange.min(), newRDatum));
        }
    }
    
    private void middleButtonDrag(int xDrag){
        showAnimation = true;
        animationMode = CENTER_ANIMATION;
        Datum dragDatum = Datum.create(xDrag, datumRange.getUnits());
        setDatumRange(new DatumRange(datumRange.min().add(dragDatum), datumRange.max().add(dragDatum)));
    }

    private void wheelRotation(int amount) {
//        System.err.println("Wheel rotation amount = " + amount);
        Datum datumAmount = Datum.create(amount, datumRange.getUnits());
        if (inSingleMode) {
            setDatumRange(new DatumRange(datumRange.min().add(datumAmount), datumRange.min().add(datumAmount)));
        } else {
            if (amount > 0) {
                setDatumRange(new DatumRange(datumRange.min().subtract(datumAmount), datumRange.max().add(datumAmount)));
            } else if (amount < 0) {
                // Allow narrowing of range only via scroll, but never allow 
                // left and right datum to meet.  
                if(datumRange.max().add(datumAmount).gt(datumRange.min().subtract(datumAmount))
                        || datumRange.min().subtract(datumAmount).lt(datumRange.max().add(datumAmount))){
                    setDatumRange(new DatumRange(datumRange.min().subtract(datumAmount), datumRange.max().add(datumAmount)));
                }
            }
        }
    }
    
    private DatumRange validRange;

    public DatumRange getValidRange() {
        return validRange;
    }

    public void setValidRange(DatumRange validRange) {
        this.validRange = validRange;
    }
    
    
    private int ovalSize = 4;
    private int halfOvalSize = ovalSize / 2;
    
    private void centerAnimation(Graphics g){
        
        TextRect lRectForAnim = dsLabelRect;
        TextRect rRectForAnim = rScan;
        
        int textBaseLine = lDatumRect.rect.y + (int) getEmSize();
        
        int startX = lRectForAnim.rect.x;
        int endX = rRectForAnim.rect.x;
        
        g.drawOval(startX, textBaseLine, ovalSize, ovalSize);
        g.drawOval(endX, textBaseLine, ovalSize, ovalSize);
        g.drawLine(startX + halfOvalSize, textBaseLine + halfOvalSize, endX + halfOvalSize, textBaseLine + halfOvalSize); 
        
        int width = endX - startX;
        double dataWidth = validRange.width().value();
        double pixPerData =  width / dataWidth;
        
        double lDist =  datumRange.min().value() - validRange.min().value();
        int lDatumLineX = (int) ( lDist * pixPerData);
        lDatumLineX += (startX + halfOvalSize);
        
        double rDist = datumRange.max().value() - validRange.min().value();
        int rDatumLineX = (int) (rDist * pixPerData);
        rDatumLineX += (startX + halfOvalSize);
        
        int datIndicatorHeight = (int) (getEmSize() / 4 );
        
        g.drawLine(lDatumLineX, textBaseLine + halfOvalSize, lDatumLineX, textBaseLine + halfOvalSize + datIndicatorHeight);
        g.drawLine(rDatumLineX, textBaseLine + halfOvalSize, rDatumLineX, textBaseLine + halfOvalSize + datIndicatorHeight);
        
        Color curCol = g.getColor();
        g.setColor(Color.gray);
        g.fillRect(lDatumLineX, textBaseLine + halfOvalSize, (rDatumLineX - lDatumLineX), datIndicatorHeight);
        g.setColor(curCol);
    }
    
    private void leftDatumClickAnimation(Graphics g){

        int ovalPositionX = rDatumRect.rect.x + rDatumRect.rect.width / 2;
        int textBaseLine = lDatumRect.rect.y + (int) getEmSize();
        
        g.drawOval(ovalPositionX, textBaseLine, ovalSize, ovalSize);
        // relative to canvas origin
        int mouseX = lastPoint.x + getX();
        g.drawLine(ovalPositionX + halfOvalSize, textBaseLine + halfOvalSize, mouseX, textBaseLine + halfOvalSize);
    
        int datIndicatorHeight = (int) (getEmSize() / 4 );
        g.drawLine(mouseX, textBaseLine + halfOvalSize, mouseX, textBaseLine + halfOvalSize + datIndicatorHeight);
    }
    
    private void rightDatumClickAnimation(Graphics g){

        int ovalPositionX = lDatumRect.rect.x + lDatumRect.rect.width / 2;
        int textBaseLine = lDatumRect.rect.y + (int) getEmSize();
        g.drawOval(ovalPositionX, textBaseLine, ovalSize, ovalSize);
        // relative to canvas origin
        int mouseX = lastPoint.x + getX();
        g.drawLine(ovalPositionX + halfOvalSize, textBaseLine + halfOvalSize, mouseX, textBaseLine + halfOvalSize);
        
        int datIndicatorHeight = (int) (getEmSize() / 4 );
        g.drawLine(mouseX, textBaseLine + halfOvalSize, mouseX, textBaseLine + halfOvalSize + datIndicatorHeight);
    }
}
