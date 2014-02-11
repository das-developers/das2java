/*
 * MonthDatumRange.java
 *
 * Created on November 15, 2004, 4:28 PM
 */

package org.das2.datum;

import java.io.Serializable;
import java.util.Arrays;

/**
 * DatumRange implementation that preserves month and year boundaries in the next() and previous() implementations.  For example,
 *   dr= MonthDatumRange( [ 1999, 01, 01, 00, 00, 00, 0 ], [ 1999, 02, 01, 00, 00, 00, 0 ] )
 *   dr.toString() -> "Jan 1999"
 *   dr= dr.next()
 *   dr.toString() -> "Feb 1999"  ; a normal datumRange would simply advance 31 days.
 * @author  Jeremy
 */
public class MonthDatumRange extends DatumRange implements Serializable {
    
    int width;
    int widthDigit;
    final int[] start;
    final int[] end;
    
    public MonthDatumRange( int[] start, int[] end ) {

        super( TimeUtil.toDatum( start ), TimeUtil.toDatum( end ) );
        this.start= new int[7]; // make defensive copy to make findbugs happy.
        System.arraycopy( start, 0, this.start, 0, 7 );
        this.end= new int[7];
        System.arraycopy( end, 0, this.end, 0, 7 );

        widthDigit= -1;
        int[] widthArr= new int[7];
        for ( int i=0; i<7; i++ ) {
            widthArr[i]= this.end[i]-this.start[i];
        }
        while( widthArr[1]<0 ) {
            widthArr[1]+= 12;
            widthArr[0]--;
        }
        for ( int i=0; i<7; i++ ) {
            if ( widthArr[i]!=0 ) {
                if ( widthDigit==0 && i==1 ) {
                    widthDigit=i;
                    widthArr[1]+=widthArr[0]*12;
                    widthArr[0]= 0;
                } else if ( widthDigit!=-1 ) {
                    throw new IllegalArgumentException("MonthDatumRange must only vary in month or year, not both"); //TODO: why?
                } else {
                    widthDigit=i;
                    width= widthArr[widthDigit];
                }
            }
        }
    }
    
    @Override
    public DatumRange next() {
        int[] end1= new int[7];
        System.arraycopy(this.end, 0, end1, 0, 7);
        end1[widthDigit]= end1[widthDigit]+this.width;
        switch ( widthDigit ) {
            case 1: while( end1[1]>12 ) {
                end1[1]-= 12;
                end1[0]++;
            }
            case 0: break;
            default: throw new IllegalArgumentException("not implemented");
        }
        return new MonthDatumRange( this.end, end1 );
    }
    
    @Override
    public DatumRange previous() {
        int[] start1= new int[7];
        System.arraycopy(this.start, 0, start1, 0, 7);
        start1[widthDigit]= start1[widthDigit]-this.width;
        switch ( widthDigit ) {
            case 1: while( start1[1]<1 ) {
                start1[1]+= 12;
                start1[0]--;
            }
            case 0: break;
            default: throw new IllegalArgumentException("not implemented");
        }
        
        return new MonthDatumRange( start1, this.start );
    }

    @Override
    public boolean equals(Object o) {
        if ( o instanceof MonthDatumRange ) {
            MonthDatumRange m= (MonthDatumRange)o;
            return Arrays.equals( this.start, m.start ) && Arrays.equals(this.end,m.end);
        } else {
            return super.equals(o);
        }
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
    
}
