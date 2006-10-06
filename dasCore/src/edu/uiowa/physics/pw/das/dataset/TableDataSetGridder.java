/*
 * TableDataSetGridder.java
 *
 * Created on July 15, 2005, 4:56 PM
 *
 *
 */

package edu.uiowa.physics.pw.das.dataset;

import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.datum.Units;
import edu.uiowa.physics.pw.das.datum.UnitsUtil;
import edu.uiowa.physics.pw.das.util.DasMath;

/**
 * calculate TableDataSets with tables that are gridded in linear or log space.
 * @author Jeremy
 */
public class TableDataSetGridder {
        
    private static Datum yTagGcd( TableDataSet tds, int itable, Datum error ) {        
        Units units= tds.getYUnits();
        double[] ytag= tds.getYTags(itable).toDoubleArray(units);        
        double gcd= DasMath.gcd( ytag, error.doubleValue( units.getOffsetUnits() ) );
        return units.getOffsetUnits().createDatum( gcd );
    }
       
    private static Datum yTagGcdLog( TableDataSet tds, int itable, Datum error ) {        
        Units units= tds.getYUnits();
        double[] ytag= new double[ tds.getYLength(itable) -1 ] ;
        
        if ( ! UnitsUtil.isRatiometric(error.getUnits()) ) throw new IllegalArgumentException("error units must be ratiometric");
        for ( int i=0; i<ytag.length; i++ ) ytag[i]= Math.log( tds.getYTagDouble( itable, i+1, units ) / tds.getYTagDouble( itable, i, units ) );        
        double gcd= DasMath.gcd( ytag, error.doubleValue( Units.logERatio ) );
        return Units.logERatio.createDatum(gcd);
    }

    private static Datum xTagGcd( TableDataSet ds, int itable, Datum error ) {        
        Units units= ds.getXUnits();
        int istart= ds.tableStart(itable);
        int iend= ds.tableEnd(itable);        
        double[] xtag= new double[ iend- istart-1 ];
        double base= ds.getXTagDouble( istart, units );
        for ( int i=0; i<xtag.length-1; i++ ) xtag[i]= ds.getXTagDouble( i+istart+1, units ) - ds.getXTagDouble( i+istart, units );
        double gcd= DasMath.gcd( xtag, error.doubleValue( units.getOffsetUnits() ) );
        return units.getOffsetUnits().createDatum(gcd);
    }

        
    /**
     * returns a TableDataSet where all the tables are gridded.  That is, each table has tags that are 
     * uniform in log or linear space.  Gaps are filled with getZUnits().getFill(), and cells are mapped
     * to a set of cells with the yTagWidth.
     */    
    public static TableDataSet gridLog( TableDataSet tds, Datum xerror, Datum yerror ) {
        if ( tds.tableCount()>1 ) throw new IllegalArgumentException("only simple tables for now");
        
        int itable=0;
        Datum xTagGcd= xTagGcd( tds, itable, xerror );
        Datum yTagGcd= yTagGcdLog( tds, itable, yerror );
        
        Units xunits= tds.getXUnits();
        Units yunits= tds.getYUnits();
        
        double tagWidth= DataSetUtil.guessXTagWidth(tds).doubleValue(xunits.getOffsetUnits());
        double dx= xTagGcd.doubleValue(xunits.getOffsetUnits());
        double xbase= tds.getXTagDouble( tds.tableStart(itable),  xunits ) -tagWidth/2 - dx / 2.;
        
        int nx= (int)(( tds.getXTagDouble(tds.tableEnd(itable)-1, xunits ) + tagWidth/2 - xbase ) / dx + 1 );
        
        int[] imap= new int[nx];        
        
                
        for ( int i=tds.tableStart(itable); i<tds.tableEnd(itable); i++ ) {
            double xtag= tds.getXTagDouble(i,xunits);
            int i0= (int)( ( xtag - tagWidth/2 - xbase ) / dx );
            int i1= (int)( ( xtag + tagWidth/2 - xbase ) / dx );
            for ( int k=i0; k<=i1; k++ ) {
                imap[k]= i;
            }
        }
        
        double dy= yTagGcd.doubleValue( Units.logERatio );
        tagWidth= TableUtil.guessYTagWidth(tds).doubleValue( Units.logERatio );
        double ybase= Math.log( tds.getYTagDouble( itable, 0, yunits ) ) - tagWidth / 2 - dy / 2;        
        
        int ny= (int)( ( Math.log( tds.getYTagDouble( itable, tds.getYLength(itable)-1, yunits ) ) + tagWidth/2 - ybase ) / dy + 1 );
        int[] jmap= new int[ny];
                     
        for ( int i=0; i<tds.getYLength(itable); i++ ) {
            double tag= Math.log( tds.getYTagDouble(itable,i,yunits) );
            int i0= (int)( ( tag - tagWidth/2 - ybase ) / dy );
            int i1= (int)( ( tag + tagWidth/2 - ybase ) / dy );
            for ( int k=i0; k<=i1; k++ ) {
                jmap[k]= i;
            }
        }
        
        int[] tableMap= new int[ nx ];
        for ( int j=0; j<nx; j++ ) tableMap[j]=0;
        
        return new TagMapTableDataSet( tds, imap, new int[][] { jmap }, tableMap );
        
    }
    
}
