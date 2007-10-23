/*
 * DisplayDataMouseModule.java
 *
 * Created on October 23, 2007, 7:08 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.uiowa.physics.pw.das.event;

import edu.uiowa.physics.pw.das.dataset.ClippedTableDataSet;
import edu.uiowa.physics.pw.das.dataset.DataSet;
import edu.uiowa.physics.pw.das.dataset.TableDataSet;
import edu.uiowa.physics.pw.das.dataset.VectorDataSet;
import edu.uiowa.physics.pw.das.dataset.VectorDataSetBuilder;
import edu.uiowa.physics.pw.das.datum.DatumRange;
import edu.uiowa.physics.pw.das.datum.Units;
import edu.uiowa.physics.pw.das.datum.format.DatumFormatter;
import edu.uiowa.physics.pw.das.graph.DasPlot;
import edu.uiowa.physics.pw.das.graph.Renderer;
import edu.uiowa.physics.pw.das.util.DasExceptionHandler;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

/**
 *
 * @author jbf
 */
public class DisplayDataMouseModule extends MouseModule {
    
    final static String LABEL= "Display Data";
    
    DasPlot plot;
    
    static JFrame myFrame;
    static JPanel myPanel;
    static JEditorPane myEdit;
    
    /** Creates a new instance of DisplayDataMouseModule */
    public DisplayDataMouseModule( DasPlot parent ) {
        super( parent, new BoxRenderer(parent), LABEL );
        this.plot= parent;
    }
    
    private void maybeCreateFrame() {
        if ( myFrame==null ) {
            myFrame= new JFrame( LABEL );
            myPanel= new JPanel();
            myPanel.setPreferredSize( new Dimension( 300, 300 ) );
            myPanel.setLayout( new BorderLayout() );
            myEdit= new JEditorPane();
            myEdit.setFont( Font.decode("fixed-10" ) );
            JScrollPane scrollPane= new JScrollPane( myEdit, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
            myPanel.add( scrollPane, BorderLayout.CENTER );
            myFrame.getContentPane().add(myPanel);
            myFrame.pack();
        }
        return;
    }
    
    public void mouseRangeSelected(MouseDragEvent e0) {
        
        maybeCreateFrame();
        myFrame.setVisible(true);
        
        MouseBoxEvent e= (MouseBoxEvent)e0;
        
        DatumRange xrange;
        DatumRange yrange;
        
        xrange= new DatumRange( plot.getXAxis().invTransform(e.getXMinimum()), plot.getXAxis().invTransform(e.getXMaximum()) );
        yrange= new DatumRange( plot.getYAxis().invTransform(e.getYMaximum()), plot.getYAxis().invTransform(e.getYMinimum()) );
        
        Renderer[] rends= plot.getRenderers();
        
        Document doc= myEdit.getDocument();
        
        try {
            
            AttributeSet attrSet= null;
            
            doc.remove( 0, doc.getLength() ); // erase all
            
            for ( int irend=0; irend<rends.length; irend++ ) {
                
                doc.insertString( doc.getLength(), "Renderer #"+irend+"\n", attrSet );
                
                DataSet ds= rends[irend].getDataSet();
                
                DataSet outds;
                if ( ds instanceof TableDataSet ) {
                    TableDataSet tds= (TableDataSet)ds;
                    TableDataSet toutds= new ClippedTableDataSet( tds, xrange, yrange );
                    
                    StringBuffer buf= new StringBuffer();
                    
                    Units zunits= tds.getZUnits();
                    DatumFormatter df= tds.getDatum(0,0).getFormatter();
                    
                    buf.append( "TableDataSet "+toutds.getXLength()+"x"+toutds.getYLength(0)+" ("+zunits+")\n" );
                    for ( int i=0; i<toutds.getXLength(); i++ ) {
                        for ( int j=0; j<toutds.getYLength(0); j++ ) {
                            try {
                                buf.append( df.format(toutds.getDatum(i,j),zunits) );
                                buf.append( " " );
                            } catch ( IndexOutOfBoundsException ex ) {
                                System.err.println("here");
                            }
                        }
                        buf.append("\n");
                    }
                    doc.insertString( doc.getLength(), buf.toString(), attrSet );
                    
                } else {
                    VectorDataSet vds= (VectorDataSet)ds;
                    
                    Units units= vds.getYUnits();
                    Units xunits= vds.getXUnits();
                    
                    StringBuffer buf= new StringBuffer();
                    DatumFormatter df=  vds.getDatum(0).getFormatter();
                    DatumFormatter xdf= vds.getXTagDatum(0).getFormatter();
                    
                    VectorDataSetBuilder builder= new VectorDataSetBuilder(vds.getXUnits(),vds.getYUnits());
                    for ( int i=0; i<vds.getXLength(); i++ ) {
                        if ( xrange.contains( vds.getXTagDatum(i) ) && ( !yclip || yrange.contains(vds.getDatum(i)) ) ) {
                            buf.append( xdf.format( vds.getXTagDatum(i), xunits ) + "  " + df.format( vds.getDatum(i), units ) );
                            buf.append( "\n" );
                        }
                    }
                    doc.insertString( doc.getLength(), buf.toString(), attrSet );
                    
                }
                
            }
        } catch ( BadLocationException ex) {
            DasExceptionHandler.handle(ex);
        }
    }
    
    public String getListLabel() {
        return getLabel();
    }
    
    public Icon getListIcon() {
        ImageIcon icon;
        icon= new ImageIcon( this.getClass().getResource("/images/icons/showDataMouseModule.png" ) );
        return icon;
    }
    
    public String getLabel() {
        return LABEL;
    }
    
    /**
     * Holds value of property yclip.
     */
    private boolean yclip=false;
    
    /**
     * Utility field used by bound properties.
     */
    private java.beans.PropertyChangeSupport propertyChangeSupport =  new java.beans.PropertyChangeSupport(this);
    
    /**
     * Adds a PropertyChangeListener to the listener list.
     * @param l The listener to add.
     */
    public void addPropertyChangeListener(java.beans.PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(l);
    }
    
    /**
     * Removes a PropertyChangeListener from the listener list.
     * @param l The listener to remove.
     */
    public void removePropertyChangeListener(java.beans.PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener(l);
    }
    
    /**
     * Getter for property yclip.
     * @return Value of property yclip.
     */
    public boolean isYclip() {
        return this.yclip;
    }
    
    /**
     * Setter for property yclip.
     * @param yclip New value of property yclip.
     */
    public void setYclip(boolean yclip) {
        boolean oldYclip = this.yclip;
        this.yclip = yclip;
        propertyChangeSupport.firePropertyChange("yclip", new Boolean(oldYclip), new Boolean(yclip));
    }
    
    
}
