/*
 * FixedColumnTextPane.java
 *
 * Created on July 13, 2006, 4:43 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.uiowa.physics.pw.das.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.Collections;
import javax.swing.JTextArea;

/**
 *
 * @author Jeremy
 */
public class FixedColumnTextArea extends JTextArea {
    
    int[] cols;
    int[] colPixel;
    String spaces;
    
    /** Creates a new instance of FixedColumnTextPane */
    public FixedColumnTextArea() {
        super();
        setFont( Font.decode( "courier-12" ) );
        setLineWrap(false);
        StringBuffer spacesBuf= new StringBuffer(500);
        for ( int i=0; i<500; i++ ) spacesBuf.append(' ');
        spaces= spacesBuf.toString();
    }
    
    
    public void setColumnDivider( int col ) {
        setColumnDividers( new int[] { col } );
    }
    
    public void setColumnDividers( int[] col ) {
        this.cols= col; // TODO: defensive copy
        FontMetrics fm= this.getFontMetrics( getFont() );
        this.colPixel= new int[ col.length ];
        for ( int i=0; i<col.length; i++ ) {
            int ix= fm.stringWidth( spaces.substring(0,cols[i]) );
            this.colPixel[i]= ix;
        }
    }
    
    public int columnAt( int pixelX ) {
        for ( int i=1; i<cols.length; i++ ) {
            if ( pixelX < this.colPixel[i] ) return i-1;
        }
        return this.cols.length-1;
    }
    
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        FontMetrics fm= g.getFontMetrics();
        int leftMargin= 4;
        g.setColor( new Color( 1.f, 0.f, 0.f, 0.1f ) );
        if ( cols!=null ) {
            for ( int i=0; i<cols.length; i++ ) {
                int ix= this.colPixel[i];
                g.drawLine( ix, 0, ix, getHeight() );
            }
        }
        
    }
}
