/*
 * ClippedTableDataSet.java
 *
 * Created on February 16, 2004, 12:19 PM
 */

package org.das2.dataset;

import org.das2.datum.DatumRange;
import org.das2.datum.Datum;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.SemanticOps;

/**
 *
 * @author  Jeremy
 */
public class ClippedTableDataSet extends org.virbo.dataset.AbstractDataSet {
    
    /*
     * clippedTableDataSet
     *
     * TableDataSet that is a view of a section of a TableDataSet including an X and Y range,
     * but not much more.
     */
    
    QDataSet source;
    
    int xoffset;
    int xlength;
    
    void calculateXOffset( Datum xmin, Datum xmax ) {
        QDataSet xds= SemanticOps.xtagsDataSet(source);
        xoffset= DataSetUtil.getPreviousIndex(xds, xmin);
        int ix1= DataSetUtil.getNextIndex(xds, xmax );
        xlength= ix1- xoffset+1;
    }
                
    public ClippedTableDataSet( QDataSet source, Datum xmin, Datum xmax, Datum ymin, Datum ymax ) {
        this( source, new DatumRange( xmin, xmax ), new DatumRange( ymin, ymax ) );
    }
    
    public ClippedTableDataSet ( QDataSet source, DatumRange xrange, DatumRange yrange ) {
        this.source= source;
        calculateXOffset( xrange.min(), xrange.max() );
    }
    
    // TODO: why is this public?
    public ClippedTableDataSet( QDataSet source, int xoffset, int xlength, int yoffset, int ylength ) {
        if ( source.rank() > 2 ) {
            throw new IllegalArgumentException( "this ClippedTableDataSet constructor requires that there be only one table" );
        }
        if ( source.length() < xoffset+xlength ) {
            throw new IllegalArgumentException( "xoffset + xlength greater than the number of XTags in source" );
        }
        if ( source.length(0) < yoffset+ylength ) {
            throw new IllegalArgumentException( "yoffset + ylength greater than the number of YTags in source" );
        }
        if ( yoffset<0 || xoffset<0 ) {
            throw new IllegalArgumentException( "yoffset("+yoffset+") or xoffset("+xoffset+") is negative" );
        }
        this.source= source;
        this.xoffset= xoffset;
        this.xlength= xlength;
    }
        
    public double value( int i, int j ) {
        return source.value( i+xoffset, j );
    }

    @Override
    public int rank() {
        return 2;
    }

    @Override
    public int length() {
        return this.xlength;
    }

    @Override
    public int length(int i) {
        return source.length(i+xoffset);
    }
    
    
}
