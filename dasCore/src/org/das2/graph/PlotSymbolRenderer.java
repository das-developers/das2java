/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.graph;

import org.das2.dataset.VectorDataSet;
import org.das2.datum.Units;
import org.das2.util.monitor.ProgressMonitor;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author jbf
 */
public class PlotSymbolRenderer extends Renderer {

    public PlotSymbolRenderer() {
        updatePsym();
    }
    Image psymImage;
    Image[] coloredPsyms;
    int cmx, cmy;  // center of psym image

    public void render(Graphics2D g, DasAxis xAxis, DasAxis yAxis ) {
        VectorDataSet vds = (VectorDataSet) getDataSet();

        if (vds == null) {
            return;
        }

        Units yunits = vds.getYUnits();
        Units xunits = vds.getXUnits();
        
        VectorDataSet colorByDataSet=null;
        if (colorByDataSetId != null) {
            colorByDataSet = (VectorDataSet) vds.getPlanarView(colorByDataSetId);
        }

        if (colorBar != null && colorByDataSet != null) {
            Units cunits = colorBar.getUnits();
            for (int i = 0; i < vds.getXLength(); i++) {
                double yy = vds.getDouble(i, yunits);
                if (yunits.isValid(yy)) {
                    double xx = xAxis.transform(vds.getXTagDouble(i, xunits), xunits);
                    yy = yAxis.transform(yy, yunits);
                    int icolor = colorBar.indexColorTransform(colorByDataSet.getDouble(i, cunits), cunits);
                    g.drawImage(coloredPsyms[icolor], (int) xx-cmx, (int) yy-cmy, getParent());
                }
            }
        } else {
            for (int i = 0; i < vds.getXLength(); i++) {
                double yy = vds.getDouble(i, yunits);
                if (yunits.isValid(yy)) {
                    double xx = xAxis.transform(vds.getXTagDouble(i, xunits), xunits);
                    yy = yAxis.transform(yy, yunits);
                    g.drawImage(psymImage, (int) xx-cmx, (int) yy-cmy, getParent());
                }
            }
        }
    }

    private void updatePsym() {
        BufferedImage image = new BufferedImage((int) symsize, (int) symsize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) image.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);
        cmx= (int)( symsize / 2 );
        cmy= (int)( symsize / 2 );
        psym.draw(g, symsize / 2, symsize / 2, (float) symsize, FillStyle.STYLE_SOLID);
        psymImage = image;

        if (colorBar != null) {
            IndexColorModel model = colorBar.getIndexColorModel();
            coloredPsyms = new Image[model.getMapSize()];
            for (int i = 0; i < model.getMapSize(); i++) {
                Color c = new Color(model.getRGB(i));
                image = new BufferedImage((int) symsize, (int) symsize, BufferedImage.TYPE_INT_ARGB);
                g = (Graphics2D) image.getGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(c);
                psym.draw(g, symsize / 2, symsize / 2, (float) symsize, FillStyle.STYLE_SOLID);
                coloredPsyms[i] = image;
            }
        }
        updateCacheImage();
    }
    
    private PlotSymbol psym = DefaultPlotSymbol.CIRCLES;
    public static final String PROP_PSYM = "psym";

    /**
     * Get the value of psym
     *
     * @return the value of psym
     */
    public PlotSymbol getPsym() {
        return this.psym;
    }

    /**
     * Set the value of psym
     *
     * @param newpsym new value of psym
     */
    public void setPsym(PlotSymbol newpsym) {
        PlotSymbol oldpsym = psym;
        this.psym = newpsym;
        updatePsym();
        propertyChangeSupport.firePropertyChange(PROP_PSYM, oldpsym, newpsym);
    }

    /**
     * Add PropertyChangeListener.
     *
     * @param listener
     */
    public void addPropertyChangeListener(java.beans.PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Remove PropertyChangeListener.
     *
     * @param listener
     */
    public void removePropertyChangeListener(java.beans.PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }
    private double symsize = 1.0;
    public static final String PROP_SYMSIZE = "symSize";

    /**
     * Get the value of symsize
     *
     * @return the value of symsize
     */
    public double getSymSize() {
        return this.symsize;
    }

    /**
     * Set the value of symsize
     *
     * @param newsymsize new value of symsize
     */
    public void setSymSize(double newsymsize) {
        double oldsymsize = symsize;
        this.symsize = newsymsize;
        updatePsym();
        propertyChangeSupport.firePropertyChange(PROP_SYMSIZE, new Double(oldsymsize), new Double(newsymsize));
    }
    private Color color = Color.BLACK;
    public static final String PROP_COLOR = "color";

    /**
     * Get the value of color
     *
     * @return the value of color
     */
    public Color getColor() {
        return this.color;
    }

    /**
     * Set the value of color
     *
     * @param newcolor new value of color
     */
    public void setColor(Color newcolor) {
        Color oldcolor = color;
        this.color = newcolor;
        updatePsym();
        propertyChangeSupport.firePropertyChange(PROP_COLOR, oldcolor, newcolor);
    }
    private String colorByDataSetId;
    public static final String PROP_COLORBYDATASETID = "colorByDataSetId";

    public String getColorByDataSetId() {
        return this.colorByDataSetId;
    }

    public void setColorByDataSetId(String newcolorByDataSetId) {
        String oldcolorByDataSetId = colorByDataSetId;
        this.colorByDataSetId = newcolorByDataSetId;
        propertyChangeSupport.firePropertyChange(PROP_COLORBYDATASETID, oldcolorByDataSetId, newcolorByDataSetId);
    }
    

    public void setColorBar(DasColorBar colorBar) {
        DasColorBar oldcolorBar= this.colorBar;
        this.colorBar = colorBar;
        colorBar.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (colorByDataSetId != null) {
                    updateCacheImage();
                }
            }
        });
        updatePsym();
        propertyChangeSupport.firePropertyChange(PROP_COLORBAR, oldcolorBar, colorBar);
    }

    protected void installRenderer() {

    }

    protected void uninstallRenderer() {

    }

    protected Element getDOMElement(Document document) {
        return null;
    }
}
