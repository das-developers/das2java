package org.das2.graph;

import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import static javax.swing.JLayeredPane.LAYER_PROPERTY;
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

    private boolean inDebugMode = false;

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
        repaint();
    }
    
    private String dsLabel;

    public String getDsLabel() {
        return dsLabel;
    }

    public void setDsLabel(String dsLabel) {
        this.dsLabel = dsLabel;
        if(inSingleMode){
            dsLabelRect.text = dsLabel + " at ";
        }else{
            dsLabelRect.text = dsLabel + " averaged over ";
        }
        
    }
    
 
    class TextRect {

        String text;

        double lRatio;
        double rRatio;

        private int getWidthFromRatio() {
            return (int) ((rRatio - lRatio) * layoutWidth);
        }
        
        int lPixel;
        int rPixel;
        
        private int getPixelWidth(){
            return rPixel - lPixel;
        }
        
        int tPixel;
        int bPixel;
        
        private int getPixelHeight(){
            return bPixel - tPixel;
        }

        Cursor cursor = new Cursor(Cursor.DEFAULT_CURSOR);

        Rectangle rect;

        protected void setRect() {
            // Relative to canvas origin
//            this.rect.setBounds((int) (lRatio * layoutWidth) + colMin, rowMin , getWidthFromRatio(), layoutHeight);
            // Relative to canvas origin
            this.rect.setBounds(lPixel + colMin, rowMin , getPixelWidth(), layoutHeight);
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
                gtr.draw(g, this.rect.x , this.rect.y + this.rect.height / 2);
//                g.drawRect(this.rect.x, this.rect.y, this.rect.width, this.rect.height);
//                if(this.text.equals("scan>>") || this.text.equals("<<scan")){
//                    g.drawRect(this.rect.x, height / 2 - (int) gtr.getHeight(), this.rect.width, (int) gtr.getHeight());
//                }
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
    private TextRect toTextRect = new TextRect(" to ", new Rectangle());
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

    private int colMin;
    private int colMax;
    private int rowMin;
    private int rowMax;
    private int layoutWidth;
    private int layoutHeight;

    private void setSizingParams() {
        colMin = getColumn().getDMinimum();
        colMax = getColumn().getDMaximum();
        rowMin = getRow().getDMinimum();
        rowMax = getRow().getDMaximum();
        layoutWidth = colMax - colMin;
        layoutHeight = rowMax - rowMin;
    }
    
    private int floatRight = 0;

    public int getFloatRight() {
        return floatRight;
    }

    public void setFloatRight(int floatRight) {
        this.floatRight = floatRight;
        repaint();
    }
    
    private int floatDown = 0;
    public int getFloatDown(){
        return floatDown;
    }
    
    public void setFloatDown(int fDown){
        this.floatDown = fDown;
        repaint();
    }
    
    private void setLayout(boolean inSingle, GrannyTextRenderer gtr, Graphics g){
    
        if(inSingle){
            
            gtr.setString(g, lScan.text);
            int width = (int) gtr.getWidth();
            int height = (int) gtr.getHeight();
            lScan.lPixel = floatRight;
            // The plus 1 is safety from the cast to int on width
            lScan.rPixel = lScan.lPixel + width + 1;
            lScan.setRect();
            
            gtr.setString(g, dsLabelRect.text);
            width = (int) gtr.getWidth();
            height = (int) gtr.getHeight();
            dsLabelRect.lPixel = lScan.rPixel;
            dsLabelRect.rPixel = dsLabelRect.lPixel + width + 1;
            dsLabelRect.setRect();
            
//            gtr.setString(g, datumRange.min().getFormatter().format(datumRange.min(), datumRange.getUnits()) + datumRange.getUnits());
            gtr.setString(g, lDatumRect.text);
            width = (int) gtr.getWidth();
            height = (int) gtr.getHeight();
            lDatumRect.lPixel = dsLabelRect.rPixel;
            lDatumRect.rPixel = lDatumRect.lPixel + width + 1;
            lDatumRect.setRect();
            
            gtr.setString(g, addRDatum.text);
            width = (int) gtr.getWidth();
            height = (int) gtr.getHeight();
            addRDatum.lPixel = lDatumRect.rPixel;
            addRDatum.rPixel = addRDatum.lPixel + width + 1;
            addRDatum.setRect();
            
            gtr.setString(g, rScan.text);
            width = (int) gtr.getWidth();
            height = (int) gtr.getHeight();
            rScan.lPixel = addRDatum.rPixel;
            rScan.rPixel = rScan.lPixel + width + 1;
            rScan.setRect();
                 
        }else{
            gtr.setString(g, lScan.text);
            int width = (int) gtr.getWidth();
            int height = (int) gtr.getHeight();
            lScan.lPixel = floatRight;
            // The plus 1 is safety from the cast to int on width
            lScan.rPixel = lScan.lPixel + width + 1;
            lScan.setRect();
            
            gtr.setString(g, dsLabelRect.text);
            width = (int) gtr.getWidth();
            height = (int) gtr.getHeight();
            dsLabelRect.lPixel = lScan.rPixel;
            dsLabelRect.rPixel = dsLabelRect.lPixel + width + 1;
            dsLabelRect.setRect();
            
//            gtr.setString(g, datumRange.min().getFormatter().format(datumRange.min(), datumRange.getUnits()));
            gtr.setString(g, lDatumRect.text);
            width = (int) gtr.getWidth();
            height = (int) gtr.getHeight();
            lDatumRect.lPixel = dsLabelRect.rPixel;
            lDatumRect.rPixel = lDatumRect.lPixel + width + 1;
            lDatumRect.setRect();
            
            gtr.setString(g, toTextRect.text);
            width = (int) gtr.getWidth();
            height = (int) gtr.getHeight();
            toTextRect.lPixel = lDatumRect.rPixel;
            toTextRect.rPixel = toTextRect.lPixel + width + 1;
            toTextRect.setRect();
            
//            gtr.setString(g, datumRange.max().getFormatter().format(datumRange.max(), datumRange.getUnits()) + datumRange.getUnits());
            gtr.setString(g, rDatumRect.text);
            width = (int) gtr.getWidth();
            height = (int) gtr.getHeight();
            rDatumRect.lPixel = toTextRect.rPixel;
            rDatumRect.rPixel = rDatumRect.lPixel + width + 1;
            rDatumRect.setRect();
            
            gtr.setString(g, rScan.text);
            width = (int) gtr.getWidth();
            height = (int) gtr.getHeight();
            rScan.lPixel = rDatumRect.rPixel;
            rScan.rPixel = rScan.lPixel + width + 1;
            rScan.setRect();
                    
        }
    }

    private void setLayoutRatios(boolean inSingle) {

        if(inSingle){
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
        setLayout(this.inSingleMode, gtr, g);
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
    }

    private TextRect mouseRect;

    private void setMouseRect(int x, int y) {
        //Relative to canvas origin
        x += colMin;
        y += rowMin;
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
                    if (mouseRect == lDatumRect || mouseRect == rDatumRect) {
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
                        lDatumDrag(xUpdatingDrag);
                        
                    } else if (mouseRect == rDatumRect) {
                        rDatumDrag(xUpdatingDrag);
                    }
                }else if(buttonPress == 2){
                    if(mouseRect == lDatumRect || mouseRect == rDatumRect || mouseRect == toTextRect){
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
                }
                resetInSingleMode();
                setMouseRect(e.getX(), e.getY());
                repaint();
            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!isDragging) {
                    if (mouseRect != lDatumRect && mouseRect != toTextRect && mouseRect != rDatumRect && mouseRect != dsLabelRect && mouseRect != null) {
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
//        System.err.println("lDatumDragging, total dist = " + xTotalDrag);
        Datum dragDatum = Datum.create(xDrag, datumRange.getUnits());
        if (inSingleMode) {
            
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
}
