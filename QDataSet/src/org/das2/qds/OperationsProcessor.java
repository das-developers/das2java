
package org.das2.qds;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.DatumRangeUtil;
import org.das2.datum.DatumUtil;
import org.das2.datum.InconvertibleUnitsException;
import org.das2.datum.Units;
import org.das2.util.LoggerManager;
import org.das2.util.StringTools;
import org.das2.util.monitor.NullProgressMonitor;
import org.das2.util.monitor.ProgressMonitor;
import org.das2.qds.ReferenceCache;
import static org.das2.qds.DataSetOps.flattenRank2;
import static org.das2.qds.DataSetOps.grid;
import static org.das2.qds.DataSetOps.slice1;
import static org.das2.qds.DataSetOps.slice2;
import static org.das2.qds.DataSetOps.slice3;
import org.das2.qds.ops.Ops;
import org.das2.qds.util.Reduction;

/**
 * Extract the sprocess from DataSetOps.
 * @author jbf
 */
public class OperationsProcessor {

    private static final Logger logger= LoggerManager.getLogger("qdataset.ops");
    
    /**
     * pop off the single or double quotes delimiting a string, if found.
     * @param s a string argument, possibly surrounded with quotes.
     * @return the string without the quotes.
     */
    private static String getStringArg( String s ) {
        String comp= s.trim();
        if ( comp.startsWith("'") && comp.endsWith("'") ) {
            comp= comp.substring(1,comp.length()-1);
        } else if ( comp.startsWith("\"") && comp.endsWith("\"") ) {
            comp= comp.substring(1,comp.length()-1);
        }
        return comp;
    }
    
    /**
     * container for the logic for slicing at an index vs slicing at a datum.  If the string is
     * an integer, then we return the index.  If the index is a string, then we need to 
     * find the corresponding index to the rank 0 dataset elsewhere.
     * If the string argument is not parseable, then deft is returned.
     * @param arg String that encodes a datum position or index.
     * @param deft default value.
     * @return an integer index or a dataset indicating the index.
     */
    public static Object getArgumentIndex( String arg, int deft ) {
        try {
            int idx= Integer.parseInt(arg);
            return idx;
        } catch ( NumberFormatException ex ) {
            arg= arg.trim();
            if ( arg.length()>2 && arg.startsWith("'") && arg.endsWith("'") ) {
                arg= arg.substring(1,arg.length()-1);
            }
            if ( arg.length()>2 && arg.startsWith("\"") && arg.endsWith("\"") ) {
                arg= arg.substring(1,arg.length()-1);
            }
            try {
                QDataSet ds= Ops.dataset( arg );
                return ds;
            } catch ( IllegalArgumentException ex2 ) {
                return deft;
            }
        }
    }
    
    /**
     * sprocess implements the poorly-named filters string / process string of Autoplot, allowing
     * clients to "pipe" data through a chain of operations.  For example, the filters string 
     * "|slice0(9)|histogram()" will slice on the ninth index and then take a histogram of that
     * result.  See http://www.papco.org/wiki/index.php/DataReductionSpecs (TODO: wiki page was lost,
     * which could probably be recovered.)  There's a big problem here:
     * if the command is not recognized, then it is ignored.  We should probably change this,
     * but the change should be at a major version change in case it breaks things.
     * @param c process string like "slice0(9)|histogram()"
     * @param fillDs The dataset loaded from the data source controller, with initial filters (like fill) applied.
     * @param mon monitor for the processing.
     * @throws ParseException when the string cannot be parsed
     * @throws Exception when a function cannot be processed (e.g. index out of bounds)
     * @return the dataset after the process string is applied.
     * @see <a href="http://autoplot.org/developer.dataset.filters">http://autoplot.org/developer.dataset.filters</a>
     * @see <a href="http://autoplot.org/developer.panel_rank_reduction">http://autoplot.org/developer.panel_rank_reduction</a>
     */
    public static QDataSet sprocess( String c, QDataSet fillDs, ProgressMonitor mon ) throws Exception {

        logger.log(Level.FINE, "sprocess({0},{1})", new Object[] { c, fillDs } );

        boolean sprocessCache= "true".equals( System.getProperty("referenceCaching2","false") );
        
        if ( mon==null ) mon= new NullProgressMonitor();
        
        QDataSet ds0= fillDs;
                    
        int i=1;
        //Scanner s= new Scanner( c );
        //s.useDelimiter("[\\(\\),]");

        long t0= System.currentTimeMillis();
        
        String[] commands= StringTools.guardedSplit( c, "\\|", '\'' );
        
        String cmd="";
        try {
            mon.started();
            for ( String command : commands ) {
                if ( command.trim().length()==0 ) continue;
                Scanner s= new Scanner( command );
                s.useDelimiter("[\\(\\),]");
                cmd= "|"+s.next();
                cmd= cmd.replaceAll( "\\|\\s*", "|" ); // https://sourceforge.net/p/autoplot/feature-requests/288/
                i= c.indexOf(cmd,i);
                logger.log(Level.FINER, "  cmd \"{0}\"", cmd );

                if ( cmd.length()==0 ) continue;
                mon.setProgressMessage("performing "+cmd.substring(1));
                
                if ( logger.isLoggable(Level.FINEST) ) { // this has proved useful for debugging.
                    System.err.println( "---------------------" );
                    System.err.println( fillDs );
                    System.err.println( "dep0=" + fillDs.property(QDataSet.DEPEND_0) );
                    System.err.println( "bundle0=" + fillDs.property(QDataSet.BUNDLE_0) );
                    System.err.println( "dep1=" + fillDs.property(QDataSet.DEPEND_1) );
                    System.err.println( "bundle1=" + fillDs.property(QDataSet.BUNDLE_1) );
                    System.err.println( "  the next command is "+ cmd );
                }
                
                if ( cmd.startsWith("|slices") && cmd.length()==7 ) { // multi dimensional slice
                    int[] dims= DataSetUtil.qubeDims(fillDs);
                    Pattern skipPattern= Pattern.compile("\\'\\:?\\'");
                    Pattern skipPattern2= Pattern.compile("\\:");
                    List<Object> args= new ArrayList();
                    while ( s.hasNextInt() || s.hasNext( skipPattern ) || s.hasNext(skipPattern2) ) {
                        if ( s.hasNextInt() ) {
                            args.add( s.nextInt() );
                        } else {
                            args.add( s.next() );
                        }
                    }
                    if ( dims!=null ) {
                        for ( int j=0; j<dims.length; j++ ) {
                            if ( args.get(j) instanceof Integer ) {
                                int dim= ((Integer)args.get(j) );
                                if ( dim<0 ) dim=0;
                                if ( dim>=dims[j] ) dim= dims[j]-1;
                                args.set(j,dim);
                            }
                        }
                    }
                    fillDs= Ops.slices( fillDs, args.toArray() );

                } else if(cmd.startsWith("|slice") && cmd.length()>6 ) {
                    int dim= cmd.charAt(6)-'0';
                    Object arg;
                    try {
                        arg= getArgumentIndex( s.next(),0 );
                    } catch ( IllegalArgumentException ex ) {
                        ex.printStackTrace();
                        arg= 0;
                    }
                    if ( arg instanceof Integer ) {
                        int idx= (Integer)arg;
                        if ( dim==0 ) {
                            if ( idx>=fillDs.length() ) idx=fillDs.length()-1;
                            if ( idx<0 ) idx+= fillDs.length();
                            fillDs= fillDs.slice(idx);
                        } else if ( dim==1 ) {
                            if ( idx>=fillDs.length(0) ) idx=fillDs.length(0)-1;
                            if ( idx<0 ) idx=0;
                            fillDs= slice1(fillDs, idx);
                        } else if ( dim==2 ) {
                            if ( idx>=fillDs.length(0,0) ) idx=fillDs.length(0,0)-1;
                            if ( idx<0 ) idx=0;
                            fillDs= slice2(fillDs, idx);
                        } else if ( dim==3 ) {
                            if ( idx>=fillDs.length(0,0,0) ) idx=fillDs.length(0,0,0)-1;
                            if ( idx<0 ) idx=0;
                            fillDs= slice3(fillDs, idx);
                        } else {
                            throw new IllegalArgumentException("unsupported dim: "+cmd);
                        }
                    } else {
                        if ( dim==0 ) {
                            fillDs= Ops.slice0( fillDs, (QDataSet)arg );
                        } else if ( dim==1 ) {
                            fillDs= Ops.slice1( fillDs, (QDataSet)arg );
                        } else if ( dim==2 ) {
                            fillDs= Ops.slice2( fillDs, (QDataSet)arg );
                        } else if ( dim==3 ) {
                            fillDs= Ops.slice3( fillDs, (QDataSet)arg );
                        } else {
                            throw new IllegalArgumentException("unsupported dim: "+cmd);
                        }
                    }
                } else if ( cmd.equals("|reducex") ) {
                    
                    ReferenceCache.ReferenceCacheEntry rcent=null;
                    boolean skip=false;
                    if ( sprocessCache ) {
                        String dsName= String.format( "%08d", fillDs.hashCode() );
                        String dsNameFilt= String.format( "%s%s", dsName, cmd );
                        ProgressMonitor mon1= mon.getSubtaskMonitor("reducex");
                        rcent= ReferenceCache.getInstance().getDataSetOrLock( dsNameFilt, mon1 );
                        if ( !rcent.shouldILoad( Thread.currentThread() ) ) {
                            logger.log(Level.FINER, "using cached data: {0}", dsNameFilt);
                            fillDs= rcent.park(mon1);
                            skip= true;
                        }
                    }
                    String arg= getStringArg( s.next() );
                    
                    if ( !skip ) {
                        try {
                            Datum r = DatumUtil.parse(arg);
                            fillDs= Reduction.reducex( fillDs, DataSetUtil.asDataSet(r) );

                            if ( sprocessCache ) {
                                assert rcent!=null;
                                rcent.finished( fillDs );
                                //System.err.println("  Stor'n: " + fillDs + " in " + rcent.toString() );
                            }    
                        } catch (ParseException ex) {
                            logger.log(Level.SEVERE, ex.getMessage(), ex);
                            if ( sprocessCache ) {
                                assert rcent!=null;
                                rcent.exception(ex);
                            }
                        }
                    }
                } else if ( cmd.equals("|normalize") ) {
                    fillDs= Ops.normalize(fillDs);
                } else if ( cmd.equals("|diff") ) {
                    fillDs= Ops.diff(fillDs);
                } else if ( cmd.equals("|accum") ) {
                    fillDs= Ops.accum(fillDs);
                } else if ( cmd.equals("|log10") ) {
                    fillDs= Ops.log10(fillDs);
                } else if ( cmd.equals("|exp10") ) {
                    fillDs= Ops.exp10(fillDs);
                } else if ( cmd.equals("|trim") ) {
                    Object arg1= getArgumentIndex( s.next(),0 );
                    Object arg2= getArgumentIndex( s.next(),fillDs.length() );
                    if ( s.hasNext() && arg1 instanceof Integer && ((Integer)arg1)>0 ) {
                        int dim= (Integer)arg1;
                        arg1= arg2;
                        arg2= getArgumentIndex( s.next(),0 );
                        if ( arg1 instanceof Integer ) {
                            int d0= (Integer) arg1;
                            int d1= (Integer) arg2;
                            if ( d0<0 ) d0+= fillDs.length();
                            if ( d1<0 ) d1+= fillDs.length();
                            fillDs= Ops.trim( dim, fillDs, d0, d1 );
                        } else {
                            QDataSet d0= (QDataSet)arg1;
                            QDataSet d1= (QDataSet)arg2;
                            fillDs= Ops.trim( dim, fillDs, d0, d1 ); 
                        }	
                    } else {
                        if ( arg1 instanceof Integer ) {
                            int d0= (Integer) arg1;
                            int d1= (Integer) arg2;
                            if ( d0<0 ) d0+= fillDs.length();
                            if ( d1<0 ) d1+= fillDs.length();
                            fillDs= fillDs.trim(d0,d1);    
                        } else {
                            QDataSet d0= (QDataSet)arg1;
                            QDataSet d1= (QDataSet)arg2;
                            fillDs= Ops.trim( fillDs, d0, d1 ); 
                        }
                    }
                } else if ( cmd.equals("|trim1") ) {
                    Object arg1= getArgumentIndex( s.next(),0 );
                    Object arg2= getArgumentIndex( s.next(),fillDs.length(0) );
                    if ( arg1 instanceof Integer ) {
                        int d0= (Integer) arg1;
                        int d1= (Integer) arg2;
                        if ( d0<0 ) d0+= fillDs.length();
                        if ( d1<0 ) d1+= fillDs.length();
                        fillDs= Ops.trim1( fillDs, d0, d1 );
                    } else {
                        QDataSet d0= (QDataSet)arg1;
                        QDataSet d1= (QDataSet)arg2;
                        fillDs= Ops.trim1( fillDs, d0, d1 ); 
                    }					
                } else if ( cmd.equals("|trim") && cmd.length()==5) {
                    int dim= s.nextInt();
                    Object arg1= getArgumentIndex( s.next(),0 );
                    Object arg2= getArgumentIndex( s.next(),fillDs.length(0) );
                    if ( arg1 instanceof Integer ) {
                        int d0= (Integer) arg1;
                        int d1= (Integer) arg2;
                        if ( d0<0 ) d0+= fillDs.length();
                        if ( d1<0 ) d1+= fillDs.length();
                        fillDs= Ops.trim( dim, fillDs, d0, d1 );
                    } else {
                        QDataSet d0= (QDataSet)arg1;
                        QDataSet d1= (QDataSet)arg2;
                        fillDs= Ops.trim( dim, fillDs, d0, d1 ); 
                    }	
                } else if ( cmd.startsWith("|collapse") && cmd.length()>9 ) {
                    int dim= cmd.charAt(9)-'0';
                    if ( s.hasNextInt() ) {
                        if ( dim==0 ) {
                            int st= s.nextInt();
                            int en= s.nextInt();
                            if ( st<0 ) st+= fillDs.length();
                            if ( en<0 ) en+= fillDs.length();
                            fillDs= fillDs.trim(st,en);
                        } else {
                            throw new IllegalArgumentException("trim is only allowed with collapse0");
                        }
                    }
                    if ( fillDs.rank()==4 ) {
                        switch (dim) {
                            case 0:
                                fillDs= Ops.collapse0R4(fillDs, mon.getSubtaskMonitor("performing collapse") );
                                break;
                            case 1:
                                fillDs= Ops.collapse1R4(fillDs, mon.getSubtaskMonitor("performing collapse") );
                                break;
                            case 2:
                                fillDs= Ops.collapse2R4(fillDs, mon.getSubtaskMonitor("performing collapse") );
                                break;
                            case 3:
                                fillDs= Ops.collapse3R4(fillDs, mon.getSubtaskMonitor("performing collapse") );
                                break;
                            default:
                                break;
                        }
                    } else {
                        fillDs= Ops.reduceMean(fillDs,dim, mon.getSubtaskMonitor("performing collapse") );
                    }
                    
                } else if ( cmd.startsWith("|total") && cmd.length()>6 ) {
                    int dim= cmd.charAt(6)-'0';
                    if ( s.hasNextInt() ) {
                        if ( dim==0 ) {
                            int st= s.nextInt();
                            int en= s.nextInt();
                            if ( st<0 ) st+= fillDs.length();
                            if ( en<0 ) en+= fillDs.length();
                            fillDs= fillDs.trim(st,en);
                        } else {
                            throw new IllegalArgumentException("trim is only allowed with total0");
                        }
                    }
                    fillDs= Ops.total(fillDs,dim, mon.getSubtaskMonitor("performing total") );
                    
                } else if ( cmd.equals("|autoHistogram") ) {
                    fillDs= Ops.autoHistogram(fillDs);
                } else if ( cmd.equals("|histogram") ) { // 0=auto, 1=binsize                    
                    Pattern p= Pattern.compile("\\d.*"); 
                    if ( s.hasNextDouble() ) {
                        double binSize= s.nextDouble();
                        if ( s.hasNextDouble() ) {
                            double min= binSize;
                            double max= s.nextDouble();
                            binSize= s.nextDouble();
                            fillDs= Ops.histogram(fillDs,min,max,binSize);
                        } else {
                            fillDs= Ops.histogram(fillDs,-1,-1,binSize);
                        }
                    } else if ( s.hasNext(p) ) { // TODO: cheesy limited operation
                        String t1= s.next();
                        String t2= s.next();
                        String w= s.next();
                        Datum d1= DatumUtil.parse(t1);
                        Datum d2= DatumUtil.parse(t2);
                        Datum dw= d1.getUnits().getOffsetUnits().parse(w);
                        fillDs= Ops.histogram(fillDs,d1,d2,dw);
                    } else {
                        fillDs= Ops.autoHistogram(fillDs);
                    }
                } else if ( cmd.equals("|histogram2d") ) {
                    QDataSet x= SemanticOps.xtagsDataSet(ds0);
                    QDataSet y= SemanticOps.ytagsDataSet(ds0);
                    int [] bins= new int[] { 20, 20 };
                    QDataSet xrange=null;
                    QDataSet yrange=null;
                    if ( s.hasNextInt() ) {
                        bins[0]= s.nextInt();
                        bins[1]= s.nextInt();
                        if ( s.hasNext() ) {
                            xrange= Ops.dataset(Ops.datumRange(s.next()));
                            yrange= Ops.dataset(Ops.datumRange(s.next()));
                        }
                    }
                    if ( xrange==null ) {
                        xrange= Ops.extent(x);
                        yrange= Ops.extent(y);
                    }
                    fillDs= Ops.histogram2d( x, y, bins, xrange, yrange );
                } else if ( cmd.equals("|extent") ) {
                    fillDs= Ops.extent(fillDs);
                } else if ( cmd.equals("|logHistogram") ) {
                    fillDs= Ops.autoHistogram(Ops.log10(fillDs));
                    MutablePropertyDataSet dep0= DDataSet.copy( (QDataSet) fillDs.property(QDataSet.DEPEND_0));
                    QDataSet cadence= (QDataSet) dep0.property( QDataSet.CADENCE );
                    dep0= (MutablePropertyDataSet) Ops.pow( Ops.replicate(10,dep0.length()), dep0 );
                    dep0.putProperty( QDataSet.SCALE_TYPE, "log" );
                    dep0.putProperty( QDataSet.CADENCE, cadence );
                    ((MutablePropertyDataSet)fillDs).putProperty( QDataSet.DEPEND_0, dep0 );
                } else if ( cmd.equals("|transpose") ) {
                    if ( fillDs.rank()==2 ) {
                        fillDs= Ops.transpose(fillDs);
                    } else {
                        System.err.println("unable to transpose dataset, not rank 2"); //TODO: error handling
                    }
                } else if ( cmd.startsWith("|fftWindow" ) ) {
                    int size= s.nextInt();
                    fillDs= Ops.fftWindow(fillDs, size);
                } else if ( cmd.equals("|flatten" ) ) {
                    if ( fillDs.rank()!=2 ) throw new IllegalArgumentException("only rank2 supported");
                    fillDs= flattenRank2(fillDs);
                } else if ( cmd.equals("|grid" ) ) {
                    if ( fillDs.rank()!=2 ) throw new IllegalArgumentException("only rank2 supported");
                    fillDs= grid(fillDs);
                } else if ( cmd.equals("|magnitude") ) {
                    fillDs= Ops.magnitude(fillDs);
                } else if ( cmd.equals("|abs") ) {
                    fillDs= Ops.abs(fillDs);
                } else if ( cmd.equals("|pow")) {
                    double n= s.nextDouble();
                    fillDs= Ops.pow(fillDs,n);
                } else if ( cmd.equals("|total")) {
                    int idx= s.nextInt();
                    fillDs= Ops.total(fillDs, idx);
                } else if ( cmd.equals("|valid")) {
                    fillDs= Ops.valid(fillDs);
                } else if ( cmd.equals("|sqrt")) {
                    fillDs= Ops.sqrt(fillDs);
                } else if ( cmd.equals("|fftPower" ) ) {
                    if ( fillDs.length()>0 ) {
                        if ( s.hasNextInt() ) {
                            int len= s.nextInt();
                            if ( s.hasNextInt() ) {
                                int step= s.nextInt();
                                String window= getStringArg( s.next() );
                                if ( window.length()==0 ) window= "Unity";
                                Ops.FFTFilterType ff= Ops.FFTFilterType.valueOf(window);
                                QDataSet wqds= Ops.windowFunction( ff, len );
                                fillDs= Ops.fftPower( fillDs, wqds, step, mon.getSubtaskMonitor("fftPower"));
                            } else {
                                fillDs= Ops.fftPower(fillDs,len, mon.getSubtaskMonitor("fftPower"));
                            }
                        } else {
                            fillDs= Ops.fftPower(fillDs);
                        }
                    } else {
                        fillDs= Ops.fftPower(fillDs);
                    }
                } else if ( cmd.equals("|fftPowerMultiThread" ) ) {
                    if ( fillDs.length()>0 ) {
                        if ( s.hasNextInt() ) {
                            int len= s.nextInt();
                            fillDs= Ops.fftPowerMultiThread(fillDs,len, mon.getSubtaskMonitor("fftPower"));
                        }
                    } 

                } else if ( cmd.startsWith("|fft" ) ) {
                    if ( fillDs.length()>0 ) {
                        if ( s.hasNextInt() ) {
                            int len= s.nextInt();
                            if ( s.hasNextInt() ) {
                                int step= s.nextInt();
                                String window= getStringArg( s.next() );
                                if ( window.length()==0 ) window= "Unity";
                                Ops.FFTFilterType ff= Ops.FFTFilterType.valueOf(window);
                                QDataSet wqds= Ops.windowFunction( ff, len );
                                fillDs= Ops.fft( fillDs, wqds, step, mon.getSubtaskMonitor("fft"));
                            } else {
                                fillDs= Ops.fft( fillDs, Ops.ones(len), 1, mon.getSubtaskMonitor("fft"));
                            }
                        } else {
                            fillDs= Ops.fft(fillDs);
                        }
                    } else {
                        fillDs= Ops.fft(fillDs); //TODO: this doesn't seem right.
                    }
                } else if ( cmd.equals("|expandWaveform") ) {
                    fillDs= Ops.expandWaveform(fillDs);
                } else if ( cmd.equals("|hilbertEnvelope") ) {
                    QDataSet h= Ops.hilbertSciPy(fillDs);
                    fillDs= Ops.magnitude(h);
                } else if ( cmd.equals("|hilbertPhase") ) {
                    QDataSet h= Ops.hilbertSciPy(fillDs);
                    QDataSet dep0= (QDataSet)fillDs.property(QDataSet.DEPEND_0);
                    if (dep0==null ) throw new IllegalArgumentException("hilbertFrequency needs timetags");
                    fillDs= Ops.unwrap( Ops.atan2( Ops.slice1(h,1), Ops.slice1(h,0) ), 2*Ops.PI  );
                } else if ( cmd.equals("|hilbertFrequency") ) {
                    QDataSet h= Ops.hilbertSciPy(fillDs);
                    QDataSet dep0= (QDataSet)fillDs.property(QDataSet.DEPEND_0);
                    if (dep0==null ) throw new IllegalArgumentException("hilbertFrequency needs timetags");
                    QDataSet phase=  Ops.unwrap( Ops.atan2( Ops.slice1(h,1), Ops.slice1(h,0) ), 2*Ops.PI  );
                    QDataSet period= Ops.subtract( dep0.slice(1), dep0.slice(0) );
                    QDataSet fs= Ops.divide( 1 , period );
                    fillDs= Ops.multiply( Ops.divide( Ops.diff( phase ), 2*Ops.PI ), fs );
                } else if ( cmd.equals("|hanning") ) {
                    if ( fillDs.length()>0 ) {
                        if ( s.hasNextInt() ) {
                            int len= s.nextInt();
                            fillDs= Ops.fftFilter( fillDs, len, Ops.FFTFilterType.Hanning );
                        } else {
                            throw new IllegalArgumentException("expected argument to hanning filter");
                        }
                    }
                } else if ( cmd.equals("|butterworth") ) { //int order, Datum f, boolean lowp 
                    if ( fillDs.length()>0 ) {
                        if ( s.hasNextInt() ) {
                            int order= s.nextInt();
                            String f= s.next();
                            String arg= s.next();
                            if ( s.hasNext() ) {
                                String flow= f;
                                String fhigh= arg;
                                arg= s.next();
                                arg= arg.toLowerCase();
                                fillDs= Ops.butterworth( fillDs, order, Units.hertz.parse(flow), Units.hertz.parse(fhigh), arg.startsWith("t") ); 
                            } else {
                                arg= arg.toLowerCase();
                                fillDs= Ops.butterworth( fillDs, order, Units.hertz.parse(f), arg.startsWith("t") );
                            }
                        } else {
                            throw new IllegalArgumentException("expected argument to butterworth filter");
                        }
                    }
                } else if ( cmd.equals("|flattenWaveform") ) {
                    fillDs= DataSetOps.flattenWaveform( fillDs );
                    
                } else if ( cmd.equals("|unbundle" ) ) {
                    String comp= getStringArg( s.next() );
                    try {
                        int icomp= Integer.parseInt(comp);
                        fillDs= DataSetOps.unbundle( fillDs, icomp );
                    } catch ( NumberFormatException ex ) {
                        fillDs= DataSetOps.unbundle( fillDs, comp );
                    }
                } else if ( cmd.equals("|negate") ) {
                    fillDs= Ops.negate(fillDs);
                } else if ( cmd.equals("|cos") ) {
                    fillDs= Ops.cos(fillDs);
                } else if ( cmd.equals("|sin") ) {
                    fillDs= Ops.sin(fillDs);
                } else if ( cmd.equals("|toRadians") ) {
                    fillDs= Ops.toRadians(fillDs);
                } else if ( cmd.equals("|toDegrees") ) {
                    fillDs= Ops.toDegrees(fillDs);
                } else if ( cmd.equals("|smooth") ) {
                    String comp= s.next();
                    int icomp= Integer.parseInt(comp);
                    fillDs= Ops.smooth(fillDs, icomp);
                } else if ( cmd.equals("|smooth1") ) {
                    String comp= s.next();
                    int icomp= Integer.parseInt(comp);
                    fillDs= Ops.smooth1(fillDs, icomp);
                } else if ( cmd.equals("|detrend") ) {
                    String comp= s.next();
                    int icomp= Integer.parseInt(comp);
                    fillDs= Ops.detrend(fillDs, icomp);
                } else if ( cmd.equals("|smoothfit") ) {
                    String comp= s.next();
                    int icomp= Integer.parseInt(comp);
                    QDataSet x= SemanticOps.xtagsDataSet(fillDs);
                    fillDs= Ops.smoothFit(x,fillDs, icomp);
                } else if ( cmd.equals("|medianFilter") ) {
                    String comp= s.next();
                    int icomp= Integer.parseInt(comp);
                    fillDs= Ops.medianFilter(fillDs, icomp);
                } else if ( cmd.equals("|contour") ) {

                    List<Double> args= new ArrayList();

                    args.add( s.nextDouble() );
                    while ( s.hasNextDouble() ) {
                        args.add( s.nextDouble() );
                    }
                    double[] aa= new double[args.size()];
                    for ( int j=0; j<aa.length; j++ ) aa[j]= args.get(j);
                    fillDs= Ops.contour( fillDs, DataSetUtil.asDataSet( aa ) );

                } else if ( cmd.equals("|dbAboveBackgroundDim1") ) { // remove the background across slices
                    String qrg= s.next();
                    int iarg= Integer.parseInt(qrg.trim());
                    if ( s.hasNext() ) {
                        String arg= s.next();
                        arg= arg.toLowerCase();
                        fillDs= DataSetOps.dbAboveBackgroundDim1( fillDs, iarg, arg.startsWith("t") ); 
                    } else {
                        fillDs= DataSetOps.dbAboveBackgroundDim1( fillDs, iarg );
                    }

                } else if ( cmd.equals("|dbAboveBackgroundDim0") ) { // remove the background within slices
                    String qrg= s.next();
                    int iarg= Integer.parseInt(qrg.trim());
                    fillDs= DataSetOps.dbAboveBackgroundDim0( fillDs, iarg );

                } else if ( cmd.equals("|setUnits" ) ) {
                    String arg= getStringArg( s.next() );
                    Units newu= Units.lookupUnits(arg);
                    fillDs= ArrayDataSet.copy(fillDs).setUnits(newu);
                } else if ( cmd.equals("|setDepend0Units") ) { //TODO: this causes strange errors with auto
                    String arg= getStringArg( s.next() );
                    Units newu= Units.lookupUnits(arg);
                    QDataSet dep0= (QDataSet) fillDs.property(QDataSet.DEPEND_0);
                    if ( dep0!=null ) {
                        dep0= ArrayDataSet.copy(dep0).setUnits(newu);
                        fillDs= Ops.putProperty( fillDs, QDataSet.DEPEND_0, dep0 );
                    }
                } else if ( cmd.equals("|setDepend0Cadence") ) {
                    String arg= getStringArg( s.next() );
                    QDataSet dep0= (QDataSet) fillDs.property(QDataSet.DEPEND_0);
                    if ( dep0!=null ) {
                        Map<String,Object> props= DataSetUtil.getDimensionProperties( fillDs,null );
                        Units dep0units= SemanticOps.getUnits(dep0);
                        MutablePropertyDataSet mdep0= Ops.putProperty( dep0, QDataSet.CADENCE, DataSetUtil.asDataSet( dep0units.getOffsetUnits().parse(arg) ) );
                        fillDs= Ops.putProperty( fillDs, QDataSet.DEPEND_0, mdep0 );
                        DataSetUtil.putProperties( props,(MutablePropertyDataSet)fillDs );
                    } else if ( SemanticOps.isJoin(fillDs) ) {
                        JoinDataSet n= new JoinDataSet(fillDs.rank());
                        Map<String,Object> props= DataSetUtil.getDimensionProperties( fillDs,null );
                        for ( int ii=0; ii<fillDs.length(); ii++ ) {
                            QDataSet fillDs1= fillDs.slice(ii);
                            Map<String,Object> props1= DataSetUtil.getDimensionProperties( fillDs1,null );
                            dep0= (QDataSet) fillDs1.property(QDataSet.DEPEND_0);
                            Units dep0units= SemanticOps.getUnits(dep0);
                            MutablePropertyDataSet mdep0= Ops.putProperty( dep0, QDataSet.CADENCE, DataSetUtil.asDataSet( dep0units.getOffsetUnits().parse(arg) ) );
                            fillDs1= Ops.putProperty( fillDs1, QDataSet.DEPEND_0, mdep0 );
                            DataSetUtil.putProperties( props1,(MutablePropertyDataSet)fillDs1 );
                            n.join(fillDs1);
                        }
                        fillDs= n;
                        DataSetUtil.putProperties( props,(MutablePropertyDataSet)fillDs );
                    }
                    
                } else if ( cmd.equals("|setDepend1Cadence" ) ) {
                    String arg= getStringArg( s.next() );
                    Map<String,Object> props= DataSetUtil.getDimensionProperties( fillDs,null );
                    fillDs= Ops.copy(fillDs);
                    QDataSet dep1= (QDataSet) fillDs.property(QDataSet.DEPEND_1);
                    if ( dep1!=null ) {
                        Units dep1units= SemanticOps.getUnits(dep1);
                        Datum news;
                        try {
                            news= dep1units.getOffsetUnits().parse(arg);
                        } catch ( ParseException ex ) {
                            news= DatumUtil.parse(arg);
                        } catch ( InconvertibleUnitsException ex ) {
                            news= DatumUtil.parse(arg);
                        }
                        
                        MutablePropertyDataSet mdep0= Ops.putProperty( dep1, QDataSet.CADENCE, DataSetUtil.asDataSet( news ) );
                        fillDs= Ops.putProperty( fillDs, QDataSet.DEPEND_1, mdep0 );
                    } 
                    DataSetUtil.putProperties( props,(MutablePropertyDataSet)fillDs );
                } else if ( cmd.equals("|getProperty") ) {
                    String arg= getStringArg( s.next() );
                    if ( arg.startsWith("QDataSet.") ) {
                        arg= arg.substring(9);
                    }
                    fillDs= Ops.dataset( fillDs.property(arg) );
                } else if ( cmd.equals("|putProperty") ) {
                    String arg= getStringArg( s.next() );
                    if ( arg.startsWith("QDataSet.") ) {
                        arg= arg.substring(9);
                    }
                    String val= getStringArg( s.next() );
                    fillDs= Ops.putProperty( fillDs, arg, val );
                    
                } else if ( cmd.equals("|setFillValue") ) {
                    String arg= getStringArg( s.next() );
                    double d= Double.parseDouble(arg);
                    fillDs= Ops.putProperty( fillDs, QDataSet.FILL_VALUE, d );
                    
                } else if ( cmd.equals("|setValidRange") ) {
                    String arg= getStringArg( s.next() );
                    Units u= SemanticOps.getUnits(fillDs);
                    DatumRange d= DatumRangeUtil.parseDatumRange( arg, u );
                    fillDs= Ops.putProperty( fillDs, QDataSet.VALID_MIN, d.min().doubleValue(u) );
                    fillDs= Ops.putProperty( fillDs, QDataSet.VALID_MAX, d.max().doubleValue(u) );
                    
                } else if ( cmd.equals("|monotonicSubset") ) {
                    WritableDataSet ds= Ops.copy(fillDs);
                    fillDs= Ops.monotonicSubset(ds);
                } else if ( cmd.equals("|decimate") ) {
                    if ( s.hasNext() ) {
                        String arg0= getStringArg( s.next() );
                        if ( !s.hasNext() ) {
                            fillDs= Ops.decimate(fillDs,Integer.parseInt(arg0) );
                        } else {
                            String arg1= getStringArg( s.next() );
                            fillDs= Ops.decimate(fillDs,Integer.parseInt(arg0),Integer.parseInt(arg1) );
                        }
                    } else {
                        fillDs= Ops.decimate(fillDs);
                    }
                } else if ( cmd.equals("|add") ) { 
                    String arg= getStringArg( s.next() );
                    Datum d= SemanticOps.getUnits(fillDs).parse(arg);
                    fillDs= Ops.add( fillDs, DataSetUtil.asDataSet(d) );
                    
                } else if ( cmd.equals("|subtract") ) { 
                    String arg= getStringArg( s.next() );
                    Datum d= SemanticOps.getUnits(fillDs).parse(arg);
                    fillDs= Ops.subtract( fillDs, DataSetUtil.asDataSet(d) );
                    
                } else if ( cmd.equals("|multiply") ) { 
                    String arg= getStringArg( s.next() );
                    Datum d= DatumUtil.parse(arg);
                    fillDs= Ops.multiply( fillDs, DataSetUtil.asDataSet(d) );

                } else if ( cmd.equals("|divide") ) { 
                    String arg= getStringArg( s.next() );
                    Datum d= DatumUtil.parse(arg);
                    fillDs= Ops.divide( fillDs, DataSetUtil.asDataSet(d) );

                } else if ( cmd.equals("|nop") ) { // no operation, for testing.
                    //fillDs= fillDs;

                } else if ( cmd.equals("|copy") ) { // force a copy of the dataset.
                    //fillDs= fillDs;
                    fillDs= Ops.copy(fillDs);

                } else {
                    if ( !cmd.equals("") ) throw new ParseException( c + " (command not recognized: \""+cmd +"\")", i );
                }
                
                long t= System.currentTimeMillis() - t0;
                logger.log(Level.FINER, "sprocess {0}: {1}ms", new Object[]{cmd, t});             
                
            } // while ( s.hasNext() )
            
        } catch ( InputMismatchException ex ) {
            String msg= ex.getLocalizedMessage();
            if ( msg==null ) msg= ex.toString();
            ParseException ex2;
            if ( c.length()>cmd.length() ) {
                ex2= new ParseException( c + " at "+cmd+" ("+msg+")", i );
            } else {
                ex2= new ParseException( c + " ("+msg+")", i );
            }
            throw ex2;
        } catch ( Exception ex ) {
            throw new IllegalArgumentException("sprocess throws exception: "+c,ex);
        } finally {
            if ( mon.isFinished() ) {
                System.err.println("monitor was already finished, fix this...");
            } else {
                mon.finished();
            }
        }
        
        logger.log(Level.FINE, "{0}->sprocess(\"{1}\")->{2}", new Object[] { ds0, c, fillDs } );
        return fillDs;
    }
}
