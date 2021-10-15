/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.datum.format;

import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.DatumRangeUtil;
import org.das2.datum.DatumVector;
import org.das2.datum.TimeUtil;
import org.das2.datum.Units;

/**
 *
 * @author jbf
 */
public class DateTimeDatumFormatter extends DatumFormatter {

    boolean dayOfYear= false;

    public boolean isDayOfYear() {
        return dayOfYear;
    }

    public void setDayOfYear(boolean dayOfYear) {
        this.dayOfYear = dayOfYear;
    }

    public static final int OPT_DOY= 1;

    /**
     * create a formatter with the given options.  Options should be or'd together a|b.
     * @param opts
     */
    public DateTimeDatumFormatter( int opts ) {
        setDayOfYear( (opts&OPT_DOY)==OPT_DOY );
    }

    public DateTimeDatumFormatter() {
        this(0);
    }
    
    @Override
    public String format(Datum datum) {
        if ( !datum.getUnits().isConvertibleTo(Units.us2000 ) ) {
            return "!Ktime!C!kexpected";
        }        
        double ssm= TimeUtil.getSecondsSinceMidnight(datum);
        String date= null;
        String time= TimeDatumFormatter.MINUTES.format(datum);
        if ( ssm==0 ) {
            if ( dayOfYear ) {
                date= TimeDatumFormatter.DAY_OF_YEAR.format(datum);
            } else {
                date= TimeDatumFormatter.DAYS.format(datum);
            }
        }
        return date==null ? time : date + " " + time;
    }

    @Override
    public String grannyFormat(Datum datum) {
        double ssm= TimeUtil.getSecondsSinceMidnight(datum);
        String date= null;
        String time= TimeDatumFormatter.MINUTES.format(datum);
        if ( ssm==0 ) {
            if ( dayOfYear ) {
                date= TimeDatumFormatter.DAY_OF_YEAR.format(datum);
            } else {
                date= TimeDatumFormatter.DAYS.format(datum);
            }
        }
        return date==null ? time : time + "!c" + date;
    }

    /**
     * format the datums for axis, automatically picking the resolution needed to
     * correctly indicate tick values.
     * @param datums
     * @param context
     * @see TimeDatumFormatter#guessFormatter(org.das2.datum.DatumVector, org.das2.datum.DatumRange) 
     * @return 
     */
    @Override
    public String[] axisFormat( DatumVector datums, DatumRange context ) {
        boolean haveMidnight= false;
        boolean haveNonMidnight= false;
        
        int firstIndex= -1;
        String[] result= new String[datums.getLength()];
        
        if ( !datums.getUnits().isConvertibleTo(Units.us2000 ) || !context.getUnits().isConvertibleTo(Units.us2000 ) ) {
            for ( int i=0; i<result.length; i++ ) {
                result[i]= "!Ktime!C!kexpected";
            }
            return result;
        }
        
        // calculate the scale between successive datums.
        int scale;
        double width;
        if ( datums.getLength()<2 ) {
            scale= TimeUtil.MICRO;
        } else {
            width= datums.get(1).subtract(datums.get(0)).doubleValue(Units.microseconds);
            if ( width>=60e6 ) {
                scale= TimeUtil.MINUTE;
            } else if ( width>=1e6 ) {
                scale= TimeUtil.SECOND;
            } else if ( width>=1e3 ) {
                scale= TimeUtil.MILLI;
            } else {
                scale= TimeUtil.MICRO;
            }
        }
        
        TimeDatumFormatter delegate= TimeDatumFormatter.formatterForScale(scale, context);
        
        for ( int i=0; i<datums.getLength(); i++ ) {
            Datum datum= datums.get(i);
            String date= null;
            String time= delegate.format(datum);
            result[i]= time;
            if ( DatumRangeUtil.sloppyContains( context, datum) ) {
                if ( firstIndex==-1 ) firstIndex= i;
                double ssm= TimeUtil.getSecondsSinceMidnight(datum);
                if ( ssm==0 ) {
                    if ( dayOfYear ) {
                       date= TimeDatumFormatter.DAY_OF_YEAR.format(datum);
                    } else {
                       date= TimeDatumFormatter.DAYS.format(datum);
                    }
                    haveMidnight= true;
                    result[i]= result[i] + "!c" + date;
                } else {
                    haveNonMidnight= true;
                }
            }
        }
                
        if ( haveNonMidnight ) {            
            if ( !haveMidnight && firstIndex>-1 ) {
                Datum datum= datums.get(firstIndex);
                String date;
                if ( dayOfYear ) {
                    date= TimeDatumFormatter.DAY_OF_YEAR.format(datum);
                } else {
                    date= TimeDatumFormatter.DAYS.format(datum);
                }
                result[firstIndex]= result[firstIndex] + "!c" + date;
            }
        } else {
            for ( int i=0; i<datums.getLength(); i++ ) {
                Datum datum= datums.get(i);
                String date;
                if ( dayOfYear ) {
                    date= TimeDatumFormatter.DAY_OF_YEAR.format(datum);
                } else {
                    date= TimeDatumFormatter.DAYS.format(datum);
                }
                result[i]= date;
            }
        }            
            
        return result;
    }
    
    

}
