/*
 * MonthDatumRange.java
 *
 * Created on November 15, 2004, 4:28 PM
 */

package org.das2.datum;

/**
 *
 * @author  Jeremy
 */
public class MonthDatumRange extends DatumRange {
    
    int width;
    int widthDigit;
    int[] start;
    int[] end;
    
    public MonthDatumRange( int[] start, int[] end ) {
        super( TimeUtil.toDatum( start ),
        TimeUtil.toDatum( end ) );
        widthDigit= -1;
        int[] widthArr= new int[7];
        for ( int i=0; i<7; i++ ) {
            widthArr[i]= end[i]-start[i];
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
                    throw new IllegalArgumentException("MonthDatumRange must only vary in month or year, not both"); //todo: why?
                } else {
                    widthDigit=i;
                    width= widthArr[widthDigit];
                }
            }
        }
        this.start= start;
        this.end= end;
    }
    
    public DatumRange next() {
        int[] end1= new int[7];
        for ( int i=0; i<7; i++ ) {
            end1[i]= this.end[i];
        }
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
    
    public DatumRange previous() {
        int[] start1= new int[7];
        for ( int i=0; i<7; i++ ) {
            start1[i]= this.start[i];
        }
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
        
}
