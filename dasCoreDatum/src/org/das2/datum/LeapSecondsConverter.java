/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.datum;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TT2000 converter that takes leap seconds into account from us2000.
 * @author jbf
 */
public class LeapSecondsConverter extends UnitsConverter {

    private static Logger logger= LoggerManager.getLogger("das2.datum.uc");

    public static final int T1972_LEAP = 10;

    private static List<Long> leapSeconds; // number of leap seconds is the index, value is in tt2000.
    private static List<Double> withoutLeapSeconds;
    private static long lastUpdateMillis=0;
    
    // the following six are a cache...
    private static double us2000_st= -1;
    private static double us2000_en= -1;
    private static int us2000_c=-1;
    private static long tt2000_st= -1;
    private static long tt2000_en= -1;
    private static int tt2000_c=-1;

    private static void updateLeapSeconds() throws IOException, MalformedURLException, NumberFormatException {
        URL url = LeapSecondsConverter.class.getResource("/orbits/CDFLeapSeconds.txt");
        logger.log(Level.FINE, "try reading leap seconds from {0}", url);
        InputStream in;
        try {
            in= url.openStream();
        } catch ( IOException ex ) {
            logger.log(Level.INFO,"unable to read internal leap seconds file: {0}", url);
            LoggerManager.getLogger("das2.url").log(Level.FINE, "openStream {0}", url);
            url= new URL("https://cdf.gsfc.nasa.gov/html/CDFLeapSeconds.txt");
            in= url.openStream();
            logger.log(Level.FINE, "Using remote copy of leap seconds file at {0}", url);
        }
        
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in))) {
            String s = "";
            leapSeconds = new ArrayList(50);
            withoutLeapSeconds = new ArrayList(50);
            String lastLine= s;
            boolean firstLine= true;
            while ( true ) {  
                s = r.readLine();
                if ( firstLine ) {
                    if ( s==null ) throw new RuntimeException("Leap seconds file is empty: "+url );
                    if ( !s.startsWith(";") ) throw new RuntimeException( "Leap seconds file should start with semicolon (;): "+url);
                    firstLine= false;
                }
                if (s == null) {
                    logger.log( Level.FINE, "Last leap second read from {0} {1}", new Object[]{url, lastLine});
                    break;
                }
                if (s.startsWith(";")) {
                    continue;
                }
                String[] ss = s.trim().split("\\s+", -2);
                if (ss[0].compareTo("1972") < 0) {
                    continue;
                }
                int iyear = Integer.parseInt(ss[0]);
                int imonth = Integer.parseInt(ss[1]);
                int iday = Integer.parseInt(ss[2]);
                int ileap = (int) (Double.parseDouble(ss[3])); // I thought these could only be whole numbers
                double us2000 = TimeUtil.createTimeDatum(iyear, imonth, iday, 0, 0, 0, 0).doubleValue(Units.us2000);
                leapSeconds.add( ((long) us2000) * 1000L - 43200000000000L + (long) ( ileap+32 ) * 1000000000 );
                withoutLeapSeconds.add( us2000 );
            }
            leapSeconds.add( Long.MAX_VALUE );
            withoutLeapSeconds.add( Double.MAX_VALUE );

            lastUpdateMillis = System.currentTimeMillis();
        }
    }

    boolean us2000ToTT2000;

    public LeapSecondsConverter( boolean us2000ToTT2000 ) {
        this.us2000ToTT2000= us2000ToTT2000;
        if ( us2000ToTT2000 ) {
            inverse= new LeapSecondsConverter( !us2000ToTT2000 );
            inverse.inverse= this;
        }
    }

    /**
     * calculate the number of leap seconds in the us2000 time.  For example,
     * <pre>
     * {@code
     * print getLeapSecondCountForUs2000( Units.us2000.parse('1972-01-01T00:00Z').doubleValue(Units.us2000) ) # results in 10
     * print getLeapSecondCountForUs2000( Units.us2000.parse('2017-01-01T00:00Z').doubleValue(Units.us2000) ) # results in 37
     * }
     * </pre>
     * This is intended to replicate the table https://cdf.gsfc.nasa.gov/html/CDFLeapSeconds.txt
     * @param us2000 the time in us2000, which include the leap seconds.
     * @return the number of leap seconds for the time.
     * @throws IOException
     */
    public synchronized static int getLeapSecondCountForUs2000( double us2000 ) throws IOException {

        if ( System.currentTimeMillis()-lastUpdateMillis > 86400000 ) {
            updateLeapSeconds();
        }

        if ( us2000 < withoutLeapSeconds.get(0) ) {
            return 0;
        }

        for ( int i=0; i<withoutLeapSeconds.size()-1; i++ ) {
            if ( withoutLeapSeconds.get(i) <= us2000 && ( i==withoutLeapSeconds.size()-1 || us2000 < withoutLeapSeconds.get(i+1) ) ) {
                us2000_st= withoutLeapSeconds.get(i);
                us2000_en= withoutLeapSeconds.get(i+1);
                us2000_c= i+T1972_LEAP;
                return i+10;
            }
        }
        logger.severe("code shouldn't get to this point: implementation error...");
        throw new RuntimeException("code shouldn't get to this point: implementation error...");
    }

    /**
     * calculate the number of leap seconds in the tt2000 time.  For example,
     * <pre>
     * {@code
     * print getLeapSecondCountForTT2000( Units.cdfTT2000.parse('1972-01-01T00:00Z').doubleValue(Units.cdfTT2000) ) # results in 10
     * print getLeapSecondCountForTT2000( 0 )   # results in 32
     * print getLeapSecondCountForTT2000( Units.cdfTT2000.parse('2017-01-01T00:00Z').doubleValue(Units.cdfTT2000) ) # results in 37
     * }
     * </pre>
     * This is intended to replicate the table https://cdf.gsfc.nasa.gov/html/CDFLeapSeconds.txt
     * @param tt2000 the time in tt2000, which include the leap seconds.
     * @return the number of leap seconds for the time.
     * @throws IOException
     */
    public synchronized static int getLeapSecondCountForTT2000( long tt2000 ) throws IOException {

        //System.err.println( "since 2015-06-30T23:58:00 (sec): " + ( ( tt2000 - 488980747184000000L ) / 1e9 ) );
        
        if ( System.currentTimeMillis()-lastUpdateMillis > 86400000 ) {
            updateLeapSeconds();
        }

        if ( tt2000 < leapSeconds.get(0) ) {
            return 0;
        }

        int i=0;
        for ( i=0; i<leapSeconds.size()-1; i++ ) {
            if ( leapSeconds.get(i) <= tt2000 && ( i==leapSeconds.size()-1 || tt2000 < leapSeconds.get(i+1) ) ) {
                tt2000_st= leapSeconds.get(i);
                tt2000_en= leapSeconds.get(i+1);
                tt2000_c= i+10;
                
                //System.err.println( "since 2015-06-30T23:58:00 (sec): " + ( ( tt2000 - 488980747184000000L ) / 1e9 ) + " result: "+tt2000_c );
                
                return i+10;
            }
        }

        return i+9; // this is when we receive Long.MAX_VALUE, presumably coming from a valid_max.
    }

    @Override
    public UnitsConverter getInverse() {
        return inverse;
    }

    @Override
    public synchronized double convert(double value) {
        try {
            int leapSec;
            if ( this.us2000ToTT2000 ) {
                if ( us2000_st <= value && value<us2000_en ) {
                    leapSec= us2000_c;
                } else {
                    leapSec= getLeapSecondCountForUs2000( value );
                }
                return ( value * 1000 - 43200000000000L ) + ( leapSec - 32 + 64.184 ) * 1000000000L;
            } else {
                if ( tt2000_st <= value && value<tt2000_en ) { // DANGER--rounding...
                    leapSec= tt2000_c;
                } else {
                    leapSec= getLeapSecondCountForTT2000( (long)value );
                }
                return ( ( value - (  leapSec - 32 + 64.184  ) * 1000000000L ) + 43200000000000L ) / 1000.; // tt = TAI + 32.184 s = UTC + 32 + 32.184 = UTC + 64.184s, see jbf@space.physics.uiowa.edu email at 2012-02-23 12:57 CST
            }
        } catch ( IOException ex ) {
            throw new RuntimeException("LeapSeconds file not available.  This should never happen since there is a leapSeconds file within code.",ex);
        }
        
    }


}
