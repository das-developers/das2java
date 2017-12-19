/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.datum;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.das2.datum.TimeUtil.TimeStruct;

/**
 * Orbits are a map of string identifiers to DatumRanges, typically used to enumerate the orbits a spacecraft makes.
 * For example, Cassini orbit "C" was from 2004-366T07:03 to 2005-032T03:27 and "33" was from  2006-318T23:34 to 2006-330T22:23.
 * There are two types of orbits: canonical, which have an identifier like "cassini" and can be used by the community, and user
 * which have identifiers like  http://das2.org/wiki/index.php/testorbits .  In either case, these refer to a file.
 * The canonical ones are stored on the das2 wiki at  http://das2.org/wiki/index.php/Orbits/&lt;id&gt;.  This file is a
 * three-column ascii file with the orbit id in either the first or last column.  Note any line not meeting this spec is ignored,
 * so that orbit files can contain additional documentation (and can sit within a wiki).
 *
 * @author jbf
 */
public class Orbits {

    private static final Logger logger= LoggerManager.getLogger("datum.orbits");

    private final String sc;

    private final LinkedHashMap<String,DatumRange> orbits;

    /**
     * the resource used to populate the orbits.
     */
    private URL url;

    /**
     * the last orbit
     */
    private String last;

    private static HashMap<String,Orbits> missions= new HashMap();

    /**
     * Read the orbits file.  Example files may be on the wiki page http://das2.org/wiki/index.php/Orbits.&lt;SC%gt;,
     * or on the classpath in /orbits/&lt;SC%gt;.dat  The orbits file will be read by ignoring any line
     * that does not contain three non-space blobs, and either the first two or last two should parse as an
     * ISO8601 string.  The ISO8601 strings must start with 4-digit years, either 19xx or 20xx.
     * Note the input can then be html, with a pre section containing the orbits.
     *
     * Note the wiki page is the source for cassini and crres, but other missions may come from special places encoded here.
     * Mediawiki introduced two problems: first, that typos were not identified clearly because a 200 (ok) code is returned
     * for any URL.  Second, it's not trivial to set up mirrors that put data into the wiki.  For this reason, the wiki
     * should only be used as a reference for humans and other use is discouraged.
     *
     * This now uses special code for rbspb-pp and rbspa-pp that looks at UIowa, LANL and at virbo.org.
     * 
     * @param sc the string identifier for the spacecraft, such as "rbspa-pp"
     * @return the list of orbits found for the spacecraft.
     */
    private static LinkedHashMap<String,DatumRange> readOrbits( String sc, List<URL> source ) throws IOException {
        List<URL> urls= new ArrayList<>();
        if ( SwingUtilities.isEventDispatchThread() ) {
            logger.warning("read orbits called on event thread");
        }
        try {
            if ( sc.contains(":") ) {
                urls.add( new URL( sc ) );  // orbit:http://das2.org/wiki/index.php/Orbits/crres:6 allowed.
            } else {
                switch (sc) {
                    case "rbspa-pp":
                    case "rbspb-pp":
                        String fsc= sc.replace("-","_");
                        urls.add( new URL( "http://www-pw.physics.uiowa.edu/rbsp/orbits/"+fsc ) );
                        urls.add( new URL( "ftp://stevens.lanl.gov/pub/projects/rbsp/autoplot/orbits/"+fsc ) );
                        urls.add( new URL( "ftp://virbo.org/mirror/stevens.lanl.gov/pub/projects/rbsp/autoplot/orbits/"+fsc ) );
                        URL lurl= Orbits.class.getResource("/orbits/"+fsc );
                        if ( lurl==null ) {
                            logger.warning("null found in orbits URLs indicates expected orbit was not found on classpath");
                        } else {
                            urls.add( lurl );
                        }   break;
                    case "crres":
                        urls.add( new URL( "http://www-pw.physics.uiowa.edu/das2/Orbits/crres.dat" ) );
                        urls.add( new URL( "ftp://virbo.org/mirror/das2.org/wiki/index.php/Oribts/crres/crres-orbits.dat" ) );
                        break;
                    case "cassini":
                        urls.add( new URL( "http://www-pw.physics.uiowa.edu/~jbf/cassini/cassiniOrbits.txt" ) );
                        break;
                    default:
                        urls.add( new URL( "http://www-pw.physics.uiowa.edu/das2/Orbits/"+sc+".dat" ) );
                        break;
                }
            }
        } catch ( MalformedURLException ex ) {
            throw new IllegalArgumentException(ex);
        }

        InputStream in=null;
        Exception exfirst=null;

        URL sourceUrl=null;

        for ( URL url: urls ) {
            URLConnection connect=null;
            try {
                logger.log(Level.FINE, "Orbits trying to connect to {0}", url);
                connect= url.openConnection();
                connect.setConnectTimeout(5000);
                in= connect.getInputStream();
                logger.log(Level.FINE, "  got input stream from {0}", url);
                sourceUrl= url;
                break;
            } catch ( IOException ex ) {
                if ( connect!=null && ( connect instanceof HttpURLConnection ) && ((HttpURLConnection)connect).getResponseCode()==401 ) {
                    logger.info("HTTP connection needs credentials, which must be in the URL.");
                }
                logger.log( Level.FINE, ex.getMessage(), ex );
                if ( exfirst==null ) exfirst= ex;
            }
        }

        if ( in==null ) {
            throw new IllegalArgumentException("I/O Exception prevents reading orbits from \""+urls.get(0)+"\"",exfirst );
        }

        LinkedHashMap<String,DatumRange> result= new LinkedHashMap();

        try (BufferedReader rin = new BufferedReader( new InputStreamReader( in ) )) {
            String s= rin.readLine();
            int col= -1; // the first time column, 0 is the first column.
            int labelColumn= -1;  // column of the identifiers.
            while ( s!=null ) {
                String[] ss= s.trim().split("\\s+");
                if ( ss.length>0 && ss[0].startsWith("#") ) {
                    s= rin.readLine();
                    continue;
                }
                if ( ss.length>2 && ( ss[1].startsWith("1") || ss[1].startsWith("2") ) ) { // quick checks
                    Datum d1;
                    Datum d2;
                    String s0;
                    try {
                        if ( col>-1 ) {
                            try {
                                d1= TimeUtil.create(ss[col]);
                                d2= TimeUtil.create(ss[col+1]);
                                if ( ss.length<=labelColumn ) {
                                    logger.info("number of columns changes, reverting to old logic");
                                    labelColumn= 2;
                                }
                                s0= ss[ col==0 ? labelColumn : 0 ];
                            } catch ( ParseException ex ) {
                                s= rin.readLine();
                                continue;
                            }
                        } else {
                            if ( ss[0].length()>4 ) {
                                d1= TimeUtil.create(ss[0]);
                                d2= TimeUtil.create(ss[1]);
                            } else {
                                throw new ParseException("time is too short",0); // nasty way to jump over to other branch
                            }
                            s0= ss[2];
                            col= 0;
                            labelColumn= ss.length-1;
                        }
                    } catch ( ParseException ex ) {
                        try {
                            d1= TimeUtil.create(ss[1]);
                            d2= TimeUtil.create(ss[2]);
                            s0= ss[0];
                            col= 1;
                            labelColumn= 0;
                        } catch ( ParseException ex1 ) {
                            s= rin.readLine();
                            continue;
                        }
                    }
                    if ( d1.gt(d2) ) {
                        logger.log(Level.WARNING, "dropping invalid orbit: {0}", s);
                    } else {
                        try {
                            result.put( trimOrbit(s0), new DatumRange(d1,d2) );
                        } catch ( IllegalArgumentException ex ) {
                            logger.log(Level.WARNING, "{0}: {1}", new Object[]{ex.getMessage(), s});
                        }
                    }
                }
                s= rin.readLine();
            }
        } finally {
            logger.log(Level.FINE, "read orbits for {0}", sc);
        }

        if ( result.isEmpty() ) {
            throw new IOException("no orbits found in files: "+urls);
        }
        
        source.add(sourceUrl);
        
        return result;

    }

    private Orbits( String sc, LinkedHashMap<String,DatumRange> orbits ) {
        this.sc= sc;
        this.orbits= orbits;
    }

    /**
     * return the DatumRange for this orbit number.  Note this IS NOT an OrbitDatumRange.
     * @param orbit
     * @return
     * @throws ParseException
     */
    public DatumRange getDatumRange( String orbit ) throws ParseException {
        orbit= trimOrbit( orbit );
        DatumRange s= orbits.get(orbit);
        if ( s==null ) {
            throw new ParseException("unable to find orbit: "+orbit+" for "+sc, 0 );
        } else {
            logger.log( Level.FINEST, "orbit {0} -> {1} to {2}", new Object[]{ s.min(), s.max() });
        }
        return s;
    }

    /**
     * returns the first orbit containing the time, or null if none do.
     * @param d
     * @return the orbit number or null
     */
    public String getOrbit( Datum d ) {
        for ( String s: orbits.keySet() ) {
            try {
                if (getDatumRange(s).contains(d)) {
                    return s;
                }
            } catch (ParseException ex) {
                logger.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
        return null;
    }

    /**
     * return the next orbit number, or null if there are no more orbit numbers.
     * @param orbit
     * @return the orbit number or null.
     */
    public String next( String orbit ) {//TODO: do this efficiently!
        boolean next= false;
        orbit= trimOrbit(orbit);
        for ( String s: orbits.keySet() ) {
            if ( next ) return s;
            if ( s.equals(orbit) ) {
                next= true;
            }
        }
        return null;
    }
    
    /**
     * return the previous orbit number, or null if there are no more orbit numbers.
     * @param orbit
     * @return the orbit number or null.
     */
    public String prev( String orbit ) { //TODO: do this efficiently!
        String prev= null;
        orbit= trimOrbit(orbit);
        for ( String s: orbits.keySet() ) {
            if ( s.equals(orbit) ) {
                return prev;
            }
            prev= s;
        }
        return null;
    }

    /**
     * Orbit numbers are typically just a number, but some missions like Cassini had letter names for orbits
     * as well.  This encapsulates the code to identify the canonical orbit from the string, by
     * removing trailing _'s and 0's.  
     * @param orbit
     * @return
     */
    public static String trimOrbit( String orbit ) {
        orbit= orbit.trim();
        int i=0;
        while ( i<orbit.length() && orbit.charAt(i)=='_' ) i++;
        while ( i<orbit.length() && orbit.charAt(i)=='0' ) i++;
        if ( i==orbit.length() ) return "0"; // cassini?
        orbit= orbit.substring(i);
        return orbit;
    }
    /**
     * return -1 if a is before b, 0 if they are equal, and 1 if a is after b.
     * @param a
     * @param b
     * @return
     */
    public int compare( String a, String b ) {
        if ( a.equals(b) ) return 0;
        int ia= -1;
        int ib= -1;
        int i=0;
        a= trimOrbit( a );
        b= trimOrbit( b );

        for ( String s: orbits.keySet() ) {
            if ( s.equals(a) ) ia= i;
            if ( s.equals(b) ) ib= i;
            i++;
        }
        if ( ia==-1 ) throw new IllegalArgumentException("not an orbit id: "+a );
        if ( ib==-1 ) throw new IllegalArgumentException("not an orbit id: "+b );
        return ia<ib ? -1 : ( ia==ib ? 0 : 1);
    }

    /**
     * return the first orbit id, so that we can iterate through all
     * @return
     */
    public String first() {
        return orbits.keySet().iterator().next();
    }

    public String last() {
        return last;
    }

    public String getSpacecraft() {
        return sc;
    }

    /**
     * reset the loaded missions.
     */
    public static synchronized void reset() {
        missions= new HashMap();
    }

    /**
     * Return the orbits for the named spacecraft, or those described in the file pointed to the URL when
     * the "sc" identifier is a URL.
     *
     * Example files may be on the wiki page http://das2.org/wiki/index.php/Orbits.&lt;SC%gt;,
     * or on the classpath in /orbits/&lt;SC%gt;.dat  The orbits file will be read by ignoring any line
     * that does not contain three non-space blobs, and either the first two or last two should parse as an
     * ISO8601 string.  The ISO8601 strings must start with 4-digit years, either
     * Note the input can then be html, with a pre section containing the orbits.
     *
     * Note the wiki page is the source for cassini and crres, but other missions may come from special places encoded here.
     * Mediawiki introduced two problems: first, that typos were not identified clearly because a 200 (ok) code is returned
     * for any URL.  Second, it's not trivial to set up mirrors that put data into the wiki.  For this reason, the wiki
     * should only be used as a reference for humans and other use is discouraged.
     *
     * This now uses special code for rbspb-pp and rbspa-pp that looks at UIowa, LANL and at virbo.org.
     *
     * This should not be called from the event thread, because it may block briefly while the orbits are loaded.
     * 
     * @param sc the string identifier for the spacecraft, such as "rbspa-pp", or URL to orbit file.
     * @return the Orbits file which can be used to query orbits.
     * @throws IllegalArgumentException when the orbits file cannot be read
     */
    public static synchronized Orbits getOrbitsFor( String sc ) {
        Orbits orbits= missions.get(sc);
        if ( orbits!=null && orbits.orbits.size()>0 ) {
            return orbits;
        } else {
            try {
                List<URL> source= new ArrayList();
                LinkedHashMap<String,DatumRange> lorbits= readOrbits(sc,source);
                orbits= new Orbits(sc,lorbits);
                Iterator<String> o= lorbits.keySet().iterator();
                String last= o.hasNext() ? o.next() : null;
                while ( o.hasNext() ) last= o.next();
                orbits.last= last;
                if ( source.size()==1 ) orbits.url= source.get(0);
                missions.put( sc, orbits );
            } catch ( IOException ex ) {
                throw new IllegalArgumentException( "Unable to read orbits file for "+sc, ex );
            }
            return orbits;
        }
    }

    /**
     * return the URL used to populate the orbits.
     * @return
     */
    public URL getURL() {
        return this.url;
    }

    /**
     * allow orbits to be used in file names
     */
    public static class OrbitFieldHandler implements TimeParser.FieldHandler {

        Orbits o;
        OrbitDatumRange orbitDatumRange;
        char pad='_';


        @Override
        public String configure(Map<String, String> args) {
            String id= args.get("id");
            if ( id==null ) {
                if ( args.get("sc")!=null ) {
                    throw new IllegalArgumentException("orbit should be specified with id, not sc");
                } else {
                    throw new IllegalArgumentException("orbit id not specified");
                }
            }
            o= getOrbitsFor( id );
            if ( args.containsKey("pad") ) {
                pad= TimeParser.getPad( args );
                if ( pad==0 ) pad=' ';
            }
            return null; // no errors
        }

        @Override
        public String getRegex() {
            return ".*";
        }

        @Override
        public void parse(String fieldContent, TimeStruct startTime, TimeStruct timeWidth, Map<String, String> extra) throws ParseException {

            // identify leading fill characters
            int i=0;
            while ( i<fieldContent.length() && fieldContent.charAt(i)==pad ) i++; // note we also trim

            DatumRange dr= o.getDatumRange( fieldContent.substring(i).trim() );
            TimeStruct tsmin= TimeUtil.toTimeStruct(dr.min());
            TimeStruct tsmax= TimeUtil.toTimeStruct(dr.max());
            
            startTime.year= tsmin.year;
            startTime.month= tsmin.month;
            startTime.day= tsmin.day;
            startTime.doy= tsmin.doy;
            startTime.hour= tsmin.hour;
            startTime.minute= tsmin.hour;
            startTime.seconds= tsmin.seconds;

            timeWidth.year= tsmax.year-tsmin.year;
            timeWidth.month= tsmax.month-tsmin.month;
            timeWidth.day= tsmax.day-tsmin.day;
            timeWidth.doy= tsmax.doy-tsmin.doy;
            timeWidth.hour= tsmax.hour-tsmin.hour;
            timeWidth.minute= tsmax.hour-tsmin.hour;
            timeWidth.seconds= tsmax.seconds-tsmin.seconds;
            timeWidth.micros= tsmax.micros-tsmin.micros;

            orbitDatumRange= new OrbitDatumRange( o.sc,fieldContent.substring(i).trim() );
        }

        public OrbitDatumRange getOrbitRange() {
            return orbitDatumRange;
        }

        @Override
        public String format( TimeStruct startTime, TimeStruct timeWidth, int length, Map<String, String> extra ) throws IllegalArgumentException {
            
            DatumRange seek= new DatumRange( TimeUtil.toDatum(startTime),TimeUtil.toDatum(TimeUtil.add(startTime,timeWidth)) );
            String result=null;
            for ( String s: o.orbits.keySet() ) {
                DatumRange dr;
                try {
                    dr= o.getDatumRange(s);
                } catch ( ParseException ex ) {
                    throw new RuntimeException(ex); // shouldn't happen
                }
                double nmin= DatumRangeUtil.normalize( dr, seek.min() );
                if ( seek.width().value()==0 ) {
                    if ( nmin>=-0.001 && nmin<1.001 ) {
                        result= s;
                        break;
                    }
                } else {
                    double nmax= DatumRangeUtil.normalize( dr, seek.max() );
                    if ( nmin>-0.8 && nmin<0.2 && ( nmax-nmin<0.01 || ( nmax>0.8 && nmax<1.2 ) ) ) {
                        result= s;
                        break;
                    }
                }
            }
            if ( result==null ) {
                DatumRange dr= new DatumRange( TimeUtil.toDatum( startTime ), TimeUtil.toDatum( startTime.add(timeWidth) ) );
                throw new IllegalArgumentException("unable to find orbit for timerange for range: "+dr);
            }

            if ( length<0 ) length= 5; // 5 by default.
            int n= length-result.length();

            StringBuilder ppad= new StringBuilder( "" );
            for ( int i=0; i<n; i++ ) {
                ppad.append(pad);
            }
            ppad.append(result);
            return ppad.toString();

        }

    }

    public static void main( String[] args ) throws ParseException {
        Orbits o= getOrbitsFor( "cassini" );
        System.err.println(o.getDatumRange("120")); //logger okay
        System.err.println(o.next("120") );

        {
            TimeParser tp= TimeParser.create( "$5(o,id=cassini)", "o", new OrbitFieldHandler() );
            DatumRange dr= tp.parse( "____C").getTimeRange();
            System.err.println( dr ); //logger okay
            System.err.println( tp.format(dr) ); //logger okay
        }

        {
            TimeParser tp= TimeParser.create( "$5(o,id=crres)", "o", new OrbitFieldHandler() );
            DatumRange dr= tp.parse( "__132").getTimeRange();
            System.err.println( dr ); //logger okay
            System.err.println( tp.format(dr) ); //logger okay
        }

        {
            // CRRES orbit 599 and 600 are the same interval.
            DatumRange dr= TimeParser.create("$(o,id=crres)").parse("599").getTimeRange();
            System.err.println( dr ); //logger okay
            DatumRange o600= dr.next();
            System.err.println( o600 ); //logger okay
            System.err.println( "-generate a list--" ); //logger okay
            List<DatumRange> drs= DatumRangeUtil.generateList( DatumRangeUtil.parseTimeRangeValid("1991-03-27 through 1991-03-29"), o600 );
            for ( DatumRange dr1: drs ) {
                System.err.println( dr1 ); //logger okay
            }

        }
    }

}
