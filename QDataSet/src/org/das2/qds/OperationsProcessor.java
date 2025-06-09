
package org.das2.qds;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.DatumUtil;
import org.das2.datum.Units;
import org.das2.util.LoggerManager;
import org.das2.util.StringTools;
import org.das2.util.monitor.NullProgressMonitor;
import org.das2.util.monitor.ProgressMonitor;
import static org.das2.qds.DataSetOps.grid;
import static org.das2.qds.DataSetOps.slice1;
import static org.das2.qds.DataSetOps.slice2;
import static org.das2.qds.DataSetOps.slice3;
import org.das2.qds.filters.ApplyIndexEditorPanel;
import org.das2.qds.ops.Ops;
import org.das2.qds.util.BinAverage;
import org.das2.qds.util.Reduction;

/**
 * Implement process chain like "|cleanData()|accum()", performing each command of the sequence.
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
     * @deprecated 
     * @see #process(org.das2.qds.QDataSet, java.lang.String, org.das2.util.monitor.ProgressMonitor) 
     * @param c process string like "slice0(9)|histogram()"
     * @param fillDs The dataset loaded from the data source controller, with initial filters (like fill) applied.
     * @param mon monitor for the processing.
     * @return
     * @throws Exception 
     */
    public static QDataSet sprocess( String c, QDataSet fillDs, ProgressMonitor mon ) throws Exception {
        return process( fillDs, c, mon );
    }
    
    
    /**
     * process implements the poorly-named filters string / process string of Autoplot, allowing
     * clients to "pipe" data through a chain of operations.  For example, the filters string 
     * "|slice0(9)|histogram()" will slice on the ninth index and then take a histogram of that
     * result.  See http://www.papco.org/wiki/index.php/DataReductionSpecs (TODO: wiki page was lost,
     * which could probably be recovered.)  There's a big problem here:
     * if the command is not recognized, then it is ignored.  We should probably change this,
     * but the change should be at a major version change in case it breaks things.
     * @param ds The dataset loaded from the data source controller, with initial filters (like fill) applied.
     * @param c process string like "slice0(9)|histogram()"
     * @param mon monitor for the processing.
     * @throws ParseException when the string cannot be parsed
     * @throws Exception when a function cannot be processed (e.g. index out of bounds)
     * @return the dataset after the process string is applied.
     * @see <a href="http://autoplot.org/developer.dataset.filters">http://autoplot.org/developer.dataset.filters</a>
     * @see <a href="http://autoplot.org/developer.panel_rank_reduction">http://autoplot.org/developer.panel_rank_reduction</a>
     * @see org.das2.qds.filters.FilterEditorPanel
     * @see https://sourceforge.net/p/autoplot/bugs/2378/
     */
    public static QDataSet process( QDataSet ds, String c, ProgressMonitor mon ) throws Exception {

        logger.log(Level.FINE, "process({0},{1})", new Object[] { c, ds } );

        boolean sprocessCache= "true".equals( System.getProperty("referenceCaching2","false") );
        
        if ( mon==null ) mon= new NullProgressMonitor();
        
        QDataSet ds0= ds;
                    
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
                    System.err.println( ds );
                    System.err.println( "dep0=" + ds.property(QDataSet.DEPEND_0) );
                    System.err.println( "bundle0=" + ds.property(QDataSet.BUNDLE_0) );
                    System.err.println( "dep1=" + ds.property(QDataSet.DEPEND_1) );
                    System.err.println( "bundle1=" + ds.property(QDataSet.BUNDLE_1) );
                    System.err.println( "  the next command is "+ cmd );
                }
                
                if ( cmd.startsWith("|slices") && cmd.length()==7 ) { // multi dimensional slice
                    int[] dims= DataSetUtil.qubeDims(ds);
                    Pattern skipPattern= Pattern.compile("\\'\\:?\\'");
                    Pattern skipPattern2= Pattern.compile("\\:");
                    List<Object> args= new ArrayList<>();
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
                    ds= Ops.slices( ds, args.toArray() );

                } else if(cmd.startsWith("|slice") && cmd.length()>6 ) {
                    int dim= cmd.charAt(6)-'0';
                    Object arg;
                    try {
                        arg= getArgumentIndex( s.next(),0 );
                    } catch ( IllegalArgumentException ex ) {
                        logger.log( Level.WARNING, ex.getLocalizedMessage(), ex );
                        arg= 0;
                    }
                    if ( arg instanceof Integer ) {
                        int idx= (Integer)arg;
                        switch (dim) {
                            case 0:
                                if ( idx>=ds.length() ) idx=ds.length()-1;
                                if ( idx<0 ) idx+= ds.length();
                                ds= ds.slice(idx);
                                break;
                            case 1:
                                if ( idx>=ds.length(0) ) idx=ds.length(0)-1;
                                if ( idx<0 ) idx=0;
                                ds= slice1(ds, idx);
                                break;
                            case 2:
                                if ( idx>=ds.length(0,0) ) idx=ds.length(0,0)-1;
                                if ( idx<0 ) idx=0;
                                ds= slice2(ds, idx);
                                break;
                            case 3:
                                if ( idx>=ds.length(0,0,0) ) idx=ds.length(0,0,0)-1;
                                if ( idx<0 ) idx=0;
                                ds= slice3(ds, idx);
                                break;
                            default:
                                throw new IllegalArgumentException("unsupported dim: "+cmd);
                        }
                    } else {
                        switch (dim) {
                            case 0:
                                ds= Ops.slice0( ds, (QDataSet)arg );
                                break;
                            case 1:
                                ds= Ops.slice1( ds, (QDataSet)arg );
                                break;
                            case 2:
                                ds= Ops.slice2( ds, (QDataSet)arg );
                                break;
                            case 3:
                                ds= Ops.slice3( ds, (QDataSet)arg );
                                break;
                            default:
                                throw new IllegalArgumentException("unsupported dim: "+cmd);
                        }
                    }
                } else if ( cmd.equals("|reducex") ) {
                    
                    ReferenceCache.ReferenceCacheEntry rcent=null;
                    boolean skip=false;
                    if ( sprocessCache ) {
                        String dsName= String.format( "%08d", ds.hashCode() );
                        String dsNameFilt= String.format( "%s%s", dsName, cmd );
                        ProgressMonitor mon1= mon.getSubtaskMonitor("reducex");
                        rcent= ReferenceCache.getInstance().getDataSetOrLock( dsNameFilt, mon1 );
                        if ( !rcent.shouldILoad( Thread.currentThread() ) ) {
                            logger.log(Level.FINER, "using cached data: {0}", dsNameFilt);
                            ds= rcent.park(mon1);
                            skip= true;
                        }
                    }
                    String arg= getStringArg( s.next() );
                    
                    if ( !skip ) {
                        try {
                            Datum r = DatumUtil.parse(arg);
                            ds= Reduction.reducex( ds, DataSetUtil.asDataSet(r) );

                            if ( sprocessCache ) {
                                assert rcent!=null;
                                rcent.finished( ds );
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
                    ds= Ops.normalize(ds);
                } else if ( cmd.equals("|diff") ) {
                    ds= Ops.diff(ds);
                } else if ( cmd.equals("|accum") ) {
                    ds= Ops.accum(ds);
                } else if ( cmd.equals("|log10") ) {
                    ds= Ops.log10(ds);
                } else if ( cmd.equals("|exp10") ) {
                    ds= Ops.exp10(ds);
                } else if ( cmd.equals("|log") ) {
                    ds= Ops.log(ds);
                } else if ( cmd.equals("|exp") ) {
                    ds= Ops.exp(ds);
                } else if ( cmd.equals("|trim") ) {
                    Object arg1= getArgumentIndex( s.next(),0 );
                    Object arg2= getArgumentIndex( s.next(),ds.length() );
                    if ( s.hasNext() && arg1 instanceof Integer && ((Integer)arg1)>0 ) {
                        int dim= (Integer)arg1;
                        arg1= arg2;
                        arg2= getArgumentIndex( s.next(),0 );
                        if ( arg1 instanceof Integer ) {
                            int d0= (Integer) arg1;
                            int d1= (Integer) arg2;
                            if ( d0<0 ) d0+= ds.length();
                            if ( d1<0 ) d1+= ds.length();
                            ds= Ops.trim( dim, ds, d0, d1 );
                        } else {
                            QDataSet d0= (QDataSet)arg1;
                            QDataSet d1= (QDataSet)arg2;
                            ds= Ops.trim( dim, ds, d0, d1 ); 
                        }	
                    } else {
                        if ( arg1 instanceof Integer ) {
                            int d0= (Integer) arg1;
                            int d1= (Integer) arg2;
                            if ( d0<0 ) d0+= ds.length();
                            if ( d1<0 ) d1+= ds.length();
                            ds= ds.trim(d0,d1);    
                        } else {
                            QDataSet d0= (QDataSet)arg1;
                            QDataSet d1= (QDataSet)arg2;
                            ds= Ops.trim( ds, d0, d1 ); 
                        }
                    }
                } else if ( cmd.equals("|trim1") ) {
                    Object arg1= getArgumentIndex( s.next(),0 );
                    Object arg2= getArgumentIndex( s.next(),ds.length(0) );
                    if ( arg1 instanceof Integer ) {
                        int d0= (Integer) arg1;
                        int d1= (Integer) arg2;
                        if ( d0<0 ) d0+= ds.length();
                        if ( d1<0 ) d1+= ds.length();
                        ds= Ops.trim1( ds, d0, d1 );
                    } else {
                        QDataSet d0= (QDataSet)arg1;
                        QDataSet d1= (QDataSet)arg2;
                        ds= Ops.trim1( ds, d0, d1 ); 
                    }					
                } else if ( cmd.equals("|trim") && cmd.length()==5) {
                    int dim= s.nextInt();
                    Object arg1= getArgumentIndex( s.next(),0 );
                    Object arg2= getArgumentIndex( s.next(),ds.length(0) );
                    if ( arg1 instanceof Integer ) {
                        int d0= (Integer) arg1;
                        int d1= (Integer) arg2;
                        if ( d0<0 ) d0+= ds.length();
                        if ( d1<0 ) d1+= ds.length();
                        ds= Ops.trim( dim, ds, d0, d1 );
                    } else {
                        QDataSet d0= (QDataSet)arg1;
                        QDataSet d1= (QDataSet)arg2;
                        ds= Ops.trim( dim, ds, d0, d1 ); 
                    }
                } else if ( cmd.startsWith("|applyIndex") && cmd.length()>11 ) {
                    Pattern p= Pattern.compile( ApplyIndexEditorPanel.PROP_REGEX );
                    Matcher m= p.matcher(command);
                    if ( m.matches() ) {
                        int len;
                        int idim= Integer.parseInt(m.group(1));
                        if ( idim==0 ) {
                            len= ds.length();
                        } else {
                            len= (Ops.size(ds))[idim];
                        }
                        int[] idx= SubsetDataSet.parseIndices(m.group(2),len);
                        ds= Ops.applyIndex( ds, idim, 
                                ArrayDataSet.wrap( idx, new int[] { idx.length }, false ) );
                    }
                } else if ( cmd.startsWith("|collapse") && cmd.length()>9 ) {
                    int dim= cmd.charAt(9)-'0';
                    if ( s.hasNextInt() ) {
                        if ( dim==0 ) {
                            int st= s.nextInt();
                            int en= s.nextInt();
                            if ( st<0 ) st+= ds.length();
                            if ( en<0 ) en+= ds.length();
                            ds= ds.trim(st,en);
                        } else {
                            throw new IllegalArgumentException("trim is only allowed with collapse0");
                        }
                    }
                    if ( ds.rank()==4 ) {
                        switch (dim) {
                            case 0:
                                ds= Ops.collapse0R4(ds, mon.getSubtaskMonitor("performing collapse") );
                                break;
                            case 1:
                                ds= Ops.collapse1R4(ds, mon.getSubtaskMonitor("performing collapse") );
                                break;
                            case 2:
                                ds= Ops.collapse2R4(ds, mon.getSubtaskMonitor("performing collapse") );
                                break;
                            case 3:
                                ds= Ops.collapse3R4(ds, mon.getSubtaskMonitor("performing collapse") );
                                break;
                            default:
                                break;
                        }
                    } else {
                        ds= Ops.reduceMean(ds,dim, mon.getSubtaskMonitor("performing collapse") );
                    }
                    
                } else if ( cmd.startsWith("|total") && cmd.length()>6 ) {
                    int dim= cmd.charAt(6)-'0';
                    if ( s.hasNextInt() ) {
                        if ( dim==0 ) {
                            int st= s.nextInt();
                            int en= s.nextInt();
                            if ( st<0 ) st+= ds.length();
                            if ( en<0 ) en+= ds.length();
                            ds= ds.trim(st,en);
                        } else {
                            throw new IllegalArgumentException("trim is only allowed with total0");
                        }
                    }
                    ds= Ops.total(ds,dim, mon.getSubtaskMonitor("performing total") );
                    
                } else if ( cmd.equals("|autoHistogram") ) {
                    ds= Ops.autoHistogram(ds);
                } else if ( cmd.equals("|histogram") ) { // 0=auto, 1=binsize                    
                    Pattern p= Pattern.compile("\\d.*"); 
                    if ( s.hasNextDouble() ) {
                        double binSize= s.nextDouble();
                        if ( s.hasNextDouble() ) {
                            double min= binSize;
                            double max= s.nextDouble();
                            binSize= s.nextDouble();
                            ds= Ops.histogram(ds,min,max,binSize);
                        } else {
                            ds= Ops.histogram(ds,-1,-1,binSize);
                        }
                    } else if ( s.hasNext(p) ) { // TODO: cheesy limited operation
                        String t1= s.next();
                        String t2= s.next();
                        String w= s.next();
                        Datum d1= DatumUtil.parse(t1);
                        Datum d2= DatumUtil.parse(t2);
                        Datum dw= d1.getUnits().getOffsetUnits().parse(w);
                        ds= Ops.histogram(ds,d1,d2,dw);
                    } else {
                        ds= Ops.autoHistogram(ds);
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
                    ds= Ops.histogram2d( x, y, bins, xrange, yrange );
                } else if ( cmd.equals("|binAverage2d") ) {
                    QDataSet bundle1;
                    if ( Ops.isBundle(ds0) && ds0.rank()==2 ) {
                        bundle1= ds0;
                    } else {
                        bundle1= Ops.flatten(ds0);
                    }
                    QDataSet x= Ops.unbundle( bundle1, 0 );
                    QDataSet y= Ops.unbundle( bundle1, 1 );
                    int [] bins= new int[] { 20, 20 };
                    DatumRange xrange=null;
                    DatumRange yrange=null;
                    if ( s.hasNextInt() ) {
                        bins[0]= s.nextInt();
                        bins[1]= s.nextInt();
                        if ( s.hasNext() ) {
                            xrange= Ops.datumRange(s.next());
                            yrange= Ops.datumRange(s.next());
                        }
                    }
                    if ( xrange==null ) {
                        xrange= DataSetUtil.asDatumRange(Ops.extent(x));
                        yrange= DataSetUtil.asDatumRange(Ops.extent(y));
                    }
                    assert yrange!=null;
                    QDataSet xtags= Ops.linspace( xrange.min(), xrange.max(), bins[0] );
                    QDataSet ytags= Ops.linspace( yrange.min(), yrange.max(), bins[1] );
                    
                    ds= BinAverage.binAverageBundle( bundle1, xtags, ytags );
                    
                } else if ( cmd.equals("|extent") ) {
                    ds= Ops.extent(ds);
                } else if ( cmd.equals("|logHistogram") ) {
                    ds= Ops.autoHistogram(Ops.log10(ds));
                    MutablePropertyDataSet dep0= DDataSet.copy( (QDataSet) ds.property(QDataSet.DEPEND_0));
                    QDataSet cadence= (QDataSet) dep0.property( QDataSet.CADENCE );
                    dep0= (MutablePropertyDataSet) Ops.pow( Ops.replicate(10,dep0.length()), dep0 );
                    dep0.putProperty( QDataSet.SCALE_TYPE, "log" );
                    dep0.putProperty( QDataSet.CADENCE, cadence );
                    ((MutablePropertyDataSet)ds).putProperty( QDataSet.DEPEND_0, dep0 );
                } else if ( cmd.equals("|transpose") ) {
                    if ( ds.rank()==2 ) {
                        ds= Ops.transpose(ds);
                    } else {
                        System.err.println("unable to transpose dataset, not rank 2"); //TODO: error handling
                    }
                } else if ( cmd.startsWith("|fftWindow" ) ) {
                    int size= s.nextInt();
                    ds= Ops.fftWindow(ds, size);
                } else if ( cmd.equals("|flatten" ) ) {
                    //if ( ds.rank()!=2 ) throw new IllegalArgumentException("only rank2 supported");
                    ds= Ops.flatten(ds);
                } else if ( cmd.equals("|grid" ) ) {
                    if ( ds.rank()!=2 ) throw new IllegalArgumentException("only rank2 supported");
                    ds= grid(ds);
                } else if ( cmd.equals("|magnitude") ) {
                    ds= Ops.magnitude(ds);
                } else if ( cmd.equals("|abs") ) {
                    ds= Ops.abs(ds);
                } else if ( cmd.equals("|pow")) {
                    double n= s.nextDouble();
                    ds= Ops.pow(ds,n);
                } else if ( cmd.equals("|total")) {
                    int idx= s.nextInt();
                    ds= Ops.total(ds, idx);
                } else if ( cmd.equals("|valid")) {
                    ds= Ops.valid(ds);
                } else if ( cmd.equals("|sqrt")) {
                    ds= Ops.sqrt(ds);
                } else if ( cmd.equals("|fftPower" ) ) {
                    if ( ds.length()>0 ) {
                        if ( s.hasNextInt() ) {
                            int len= s.nextInt();
                            if ( s.hasNextInt() ) {
                                int step= s.nextInt();
                                String window= getStringArg( s.next() );
                                if ( window.length()==0 ) window= "Unity";
                                Ops.FFTFilterType ff= Ops.FFTFilterType.valueOf(window);
                                QDataSet wqds= Ops.windowFunction( ff, len );
                                ds= Ops.fftPower( ds, wqds, step, mon.getSubtaskMonitor("fftPower"));
                            } else {
                                ds= Ops.fftPower(ds,len, mon.getSubtaskMonitor("fftPower"));
                            }
                        } else {
                            ds= Ops.fftPower(ds);
                        }
                    } else {
                        ds= Ops.fftPower(ds);
                    }
                } else if ( cmd.equals("|fftPowerSpectralDensity") 
                    || cmd.equals("|fftPowerSpectrum" ) 
                    || cmd.equals("|fftLinearSpectralDensity")
                    || cmd.equals("|fftLinearSpectrum" ) ) {
                    int len,step;
                    String window;
                    if ( s.hasNextInt() ) {
                        len= s.nextInt();
                    } else {
                        len= ds.length(0);
                    }
                    if ( s.hasNextInt() ) {
                        step= s.nextInt();
                    } else {
                        step= 1;
                    }
                    if ( s.hasNext() ) {
                        window= getStringArg(s.next());
                    } else {
                        window= "Unity";
                    }
                    Ops.FFTFilterType ff= Ops.FFTFilterType.valueOf(window);
                    QDataSet wqds= Ops.windowFunction( ff, len );
                    if ( cmd.startsWith("|fftPowerSpectrum") ) {
                        ds= Ops.fftPowerSpectrum(ds, wqds, step, mon.getSubtaskMonitor("fftPower"));
                    } else if ( cmd.startsWith("|fftPowerSpectralDensity") ) {
                        ds= Ops.fftPowerSpectralDensity(ds, wqds, step, mon.getSubtaskMonitor("fftPower"));
                    } else if ( cmd.startsWith("|fftLinearSpectrum") ) {
                        ds= Ops.fftLinearSpectrum( ds, wqds, step, mon);
                    } else if ( cmd.startsWith("|fftLinearSpectralDensity")) {
                        ds= Ops.fftLinearSpectralDensity( ds, wqds, step, mon);
                    }
                } else if ( cmd.equals("|fftPowerMultiThread" ) ) {
                    if ( ds.length()>0 ) {
                        if ( s.hasNextInt() ) {
                            int len= s.nextInt();
                            ds= Ops.fftPowerMultiThread(ds,len, mon.getSubtaskMonitor("fftPower"));
                        }
                    } 

                } else if ( cmd.startsWith("|fft" ) ) {
                    if ( ds.length()>0 ) {
                        if ( s.hasNextInt() ) {
                            int len= s.nextInt();
                            if ( s.hasNextInt() ) {
                                int step= s.nextInt();
                                String window= getStringArg( s.next() );
                                if ( window.length()==0 ) window= "Unity";
                                Ops.FFTFilterType ff= Ops.FFTFilterType.valueOf(window);
                                QDataSet wqds= Ops.windowFunction( ff, len );
                                ds= Ops.fft( ds, wqds, step, mon.getSubtaskMonitor("fft"));
                            } else {
                                ds= Ops.fft( ds, Ops.ones(len), 1, mon.getSubtaskMonitor("fft"));
                            }
                        } else {
                            ds= Ops.fft(ds);
                        }
                    } else {
                        ds= Ops.fft(ds); //TODO: this doesn't seem right.
                    }
                } else if ( cmd.equals("|expandWaveform") ) {
                    ds= Ops.expandWaveform(ds);
                } else if ( cmd.equals("|expandToFillGaps") ) {
                    if ( s.hasNextDouble() ) {
                        double d= s.nextDouble();
                        if ( s.hasNextDouble() ) {
                            Units tu= SemanticOps.getUnits( SemanticOps.xtagsDataSet(ds) );
                            Datum cadence= tu.getOffsetUnits().createDatum(d);
                            ds= Ops.expandToFillGaps( ds, cadence, s.nextDouble() );
                        } else {
                            ds= Ops.expandToFillGaps(ds,d);
                        }
                    } else if ( s.hasNext() ) {    
                        String scadence= s.next();
                        String sexpand= s.next();
                    
                        Units tu= SemanticOps.getUnits( SemanticOps.xtagsDataSet(ds) );
                        Datum cadence= tu.getOffsetUnits().parse(scadence);
                        double expand= Double.parseDouble(sexpand);
                        ds= Ops.expandToFillGaps( ds, cadence, expand );
                    } else {
                        ds= Ops.expandToFillGaps(ds);
                    }
                } else if ( cmd.equals("|hilbertEnvelope") ) {
                    QDataSet h= Ops.hilbertSciPy(ds);
                    ds= Ops.magnitude(h);
                } else if ( cmd.equals("|hilbertPhase") ) {
                    QDataSet h= Ops.hilbertSciPy(ds);
                    QDataSet dep0= (QDataSet)ds.property(QDataSet.DEPEND_0);
                    if (dep0==null ) throw new IllegalArgumentException("hilbertFrequency needs timetags");
                    ds= Ops.unwrap( Ops.atan2( Ops.slice1(h,1), Ops.slice1(h,0) ), 2*Ops.PI  );
                } else if ( cmd.equals("|hilbertFrequency") ) {
                    QDataSet h= Ops.hilbertSciPy(ds);
                    QDataSet dep0= (QDataSet)ds.property(QDataSet.DEPEND_0);
                    if (dep0==null ) throw new IllegalArgumentException("hilbertFrequency needs timetags");
                    QDataSet phase=  Ops.unwrap( Ops.atan2( Ops.slice1(h,1), Ops.slice1(h,0) ), 2*Ops.PI  );
                    QDataSet period= Ops.subtract( dep0.slice(1), dep0.slice(0) );
                    QDataSet fs= Ops.divide( 1 , period );
                    ds= Ops.multiply( Ops.divide( Ops.diff( phase ), 2*Ops.PI ), fs );
                } else if ( cmd.equals("|hanning") ) {
                    if ( ds.length()>0 ) {
                        if ( s.hasNextInt() ) {
                            int len= s.nextInt();
                            ds= Ops.fftFilter( ds, len, Ops.FFTFilterType.Hanning );
                        } else {
                            throw new IllegalArgumentException("expected argument to hanning filter");
                        }
                    }
                } else if ( cmd.equals("|butterworth") ) { //int order, Datum f, boolean lowp 
                    if ( ds.length()>0 ) {
                        if ( s.hasNextInt() ) {
                            int order= s.nextInt();
                            String f= s.next();
                            String arg= s.next();
                            if ( s.hasNext() ) {
                                String flow= f;
                                String fhigh= arg;
                                arg= s.next();
                                arg= arg.toLowerCase();
                                ds= Ops.butterworth( ds, order, Units.hertz.parse(flow), Units.hertz.parse(fhigh), arg.startsWith("t") ); 
                            } else {
                                arg= arg.toLowerCase();
                                ds= Ops.butterworth( ds, order, Units.hertz.parse(f), arg.startsWith("t") );
                            }
                        } else {
                            throw new IllegalArgumentException("expected argument to butterworth filter");
                        }
                    }
                } else if ( cmd.equals("|flattenWaveform") ) {
                    ds= DataSetOps.flattenWaveform( ds );
                    
                } else if ( cmd.equals("|unbundle" ) ) {
                    String comp= getStringArg( s.next() );
                    try {
                        int icomp= Integer.parseInt(comp);
                        ds= DataSetOps.unbundle( ds, icomp );
                    } catch ( NumberFormatException ex ) {
                        ds= DataSetOps.unbundle( ds, comp );
                    }
                } else if ( cmd.equals("|rebundle" ) ) {
                    if ( s.hasNextInt() ) {
                        List<Integer> args= new ArrayList<>();
                        args.add( s.nextInt() );
                        while ( s.hasNextInt() ) args.add( s.nextInt() );
                        int[] indeces= new int[args.size()];
                        for ( int ii=0; ii<indeces.length; ii++ ) indeces[ii]= args.get(ii);
                        ds= Ops.rebundle( ds, indeces );                        
                    } else {
                        List<String> args= new ArrayList<>();
                        while ( s.hasNext() ) args.add( s.next() );
                        ds= Ops.rebundle( ds, args.toArray(new String[args.size()]) );
                    }
                        
                } else if ( cmd.equals("|negate") ) {
                    ds= Ops.negate(ds);
                } else if ( cmd.equals("|cos") ) {
                    ds= Ops.cos(ds);
                } else if ( cmd.equals("|sin") ) {
                    ds= Ops.sin(ds);
                } else if ( cmd.equals("|toRadians") ) {
                    ds= Ops.toRadians(ds);
                } else if ( cmd.equals("|toDegrees") ) {
                    ds= Ops.toDegrees(ds);
                } else if ( cmd.equals("|smooth") ) {
                    String comp= s.next();
                    int icomp= Integer.parseInt(comp);
                    ds= Ops.smooth(ds, icomp);
                } else if ( cmd.equals("|smooth1") ) {
                    String comp= s.next();
                    int icomp= Integer.parseInt(comp);
                    ds= Ops.smooth1(ds, icomp);
                } else if ( cmd.equals("|detrend") ) {
                    String comp= s.next();
                    int icomp= Integer.parseInt(comp);
                    ds= Ops.detrend(ds, icomp);
                } else if ( cmd.equals("|detrend1") ) {
                    String comp= s.next();
                    int icomp= Integer.parseInt(comp);
                    ds= Ops.detrend1(ds, icomp);
                } else if ( cmd.equals("|smoothfit") ) {
                    String comp= s.next();
                    int icomp= Integer.parseInt(comp);
                    QDataSet x= SemanticOps.xtagsDataSet(ds);
                    ds= Ops.smoothFit(x,ds, icomp);
                } else if ( cmd.equals("|cleanData") ) {
                    if ( s.hasNext() ) {
                        String ssize= s.next();
                        if ( ssize.trim().length()>0 ) {
                            if ( s.hasNext() ) {
                                int size= Integer.parseInt(ssize);
                                double nsigma= Double.parseDouble(s.next());
                                ds= Ops.cleanData(ds,nsigma,size);
                            } else {
                                int size= Integer.parseInt(ssize);
                                ds= Ops.cleanData(ds,size);
                            }
                        } else {
                            ds= Ops.cleanData(ds);
                        }
                    } else {
                        ds= Ops.cleanData(ds);
                    }
                } else if ( cmd.equals("|medianFilter") ) {
                    String comp= s.next();
                    int icomp= Integer.parseInt(comp);
                    ds= Ops.medianFilter(ds, icomp);
                } else if ( cmd.equals("|contour") ) {
                    List<Double> args= new ArrayList<>();
                    args.add( s.nextDouble() );
                    while ( s.hasNextDouble() ) {
                        args.add( s.nextDouble() );
                    }
                    double[] aa= new double[args.size()];
                    for ( int j=0; j<aa.length; j++ ) aa[j]= args.get(j);
                    ds= Ops.contour( ds, DataSetUtil.asDataSet( aa ) );

                } else if ( cmd.equals("|dbAboveBackgroundDim1") ) { // remove the background across slices
                    String qrg= s.next();
                    int iarg= Integer.parseInt(qrg.trim());
                    if ( s.hasNext() ) {
                        String arg= s.next();
                        arg= arg.toLowerCase();
                        ds= DataSetOps.dbAboveBackgroundDim1( ds, iarg, arg.startsWith("t") ); 
                    } else {
                        ds= DataSetOps.dbAboveBackgroundDim1( ds, iarg );
                    }

                } else if ( cmd.equals("|dbAboveBackgroundDim0") ) { // remove the background within slices
                    String qrg= s.next();
                    int iarg= Integer.parseInt(qrg.trim());
                    ds= DataSetOps.dbAboveBackgroundDim0( ds, iarg );

                } else if ( cmd.equals("|setUnits" ) ) {
                    String arg= getStringArg( s.next() );
                    Units newu= Units.lookupUnits(arg);
                    ds= ArrayDataSet.copy(ds).setUnits(newu);
                } else if ( cmd.equals("|setDepend0Units") ) { //TODO: this causes strange errors with auto
                    String arg= getStringArg( s.next() );
                    Units newu= Units.lookupUnits(arg);
                    QDataSet dep0= (QDataSet) ds.property(QDataSet.DEPEND_0);
                    if ( dep0!=null ) {
                        dep0= ArrayDataSet.copy(dep0).setUnits(newu);
                        ds= Ops.putProperty( ds, QDataSet.DEPEND_0, dep0 );
                    }
                } else if ( cmd.equals("|setDepend0Cadence") ) {
                    String arg= getStringArg( s.next() );
                    ds= Ops.setDepend0Cadence( ds, arg );
                    
                } else if ( cmd.equals("|setDepend1Cadence" ) ) {
                    String arg= getStringArg( s.next() );
                    ds= Ops.setDepend1Cadence( ds, arg );

                } else if ( cmd.equals("|getProperty") ) {
                    String arg= getStringArg( s.next() );
                    if ( arg.startsWith("QDataSet.") ) {
                        arg= arg.substring(9);
                    }
                    ds= Ops.dataset( ds.property(arg) );
                } else if ( cmd.equals("|putProperty") ) {
                    String arg= getStringArg( s.next() );
                    if ( arg.startsWith("QDataSet.") ) {
                        arg= arg.substring(9);
                    }
                    if ( arg.startsWith("METADATA") ) {
                        String val= getStringArg( s.next() );
                        if ( arg.equals("METADATA.NOMINAL_RANGE") ) {
                            ds= Ops.setNominalRange( ds, val );
                        } else if ( arg.equals("METADATA.WARN_RANGE") ) {
                            ds= Ops.setWarnRange( ds, val );
                        }
                    } else {
                        String val= getStringArg( s.next() );
                        ds= Ops.putProperty( ds, arg, val );
                    }
                } else if ( cmd.equals("|setFillValue") ) {
                    String arg= getStringArg( s.next() );
                    double d= Double.parseDouble(arg);
                    ds= Ops.putProperty( ds, QDataSet.FILL_VALUE, d );
                    
                } else if ( cmd.equals("|setValidRange") ) {
                    String arg= getStringArg( s.next() );
                    ds= Ops.setValidRange( ds, arg );
                    
                } else if ( cmd.equals("|timeShift") ) {
                    Units u= SemanticOps.getUnits( SemanticOps.xtagsDataSet(ds) );
                    String arg= getStringArg( s.next() );
                    ds= Ops.timeShift( ds, u.getOffsetUnits().parse(arg) );
                    
                } else if ( cmd.equals("|monotonicSubset") ) {
                    WritableDataSet wds= Ops.copy(ds);
                    ds= Ops.monotonicSubset(wds);

                } else if ( cmd.equals("|sortInTime") ) {
                    ds= Ops.sortInTime(ds);
                    
                } else if ( cmd.equals("|decimate") ) {
                    if ( s.hasNext() ) {
                        String arg0= getStringArg( s.next() );
                        if ( !s.hasNext() ) {
                            ds= Ops.decimate(ds,Integer.parseInt(arg0) );
                        } else {
                            String arg1= getStringArg( s.next() );
                            ds= Ops.decimate(ds,Integer.parseInt(arg0),Integer.parseInt(arg1) );
                        }
                    } else {
                        ds= Ops.decimate(ds);
                    }
                } else if ( cmd.equals("|add") ) { 
                    String arg= getStringArg( s.next() );
                    Datum d= SemanticOps.getUnits(ds).parse(arg);
                    ds= Ops.add( ds, DataSetUtil.asDataSet(d) );
                    
                } else if ( cmd.equals("|subtract") ) { 
                    String arg= getStringArg( s.next() );
                    Datum d= SemanticOps.getUnits(ds).parse(arg);
                    ds= Ops.subtract( ds, DataSetUtil.asDataSet(d) );
                    
                } else if ( cmd.equals("|multiply") ) { 
                    String arg= getStringArg( s.next() );
                    Datum d= DatumUtil.parse(arg);
                    ds= Ops.multiply( ds, DataSetUtil.asDataSet(d) );

                } else if ( cmd.equals("|divide") ) { 
                    String arg= getStringArg( s.next() );
                    Datum d= DatumUtil.parse(arg);
                    ds= Ops.divide( ds, DataSetUtil.asDataSet(d) );

                } else if ( cmd.equals("|nop") ) { // no operation, for testing.
                    //fillDs= fillDs;

                } else if ( cmd.equals("|copy") ) { // force a copy of the dataset.
                    //fillDs= fillDs;
                    ds= Ops.copy(ds);

                } else if ( cmd.equals("|polarToCartesian") ) {
                    ds= Ops.polarToCartesian(ds);

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
            throw new IllegalArgumentException("process throws exception: "+c,ex);
        } finally {
            if ( mon.isFinished() ) {
                System.err.println("monitor was already finished, fix this...");
            } else {
                mon.finished();
            }
        }
        
        logger.log(Level.FINE, "{0}->sprocess(\"{1}\")->{2}", new Object[] { ds0, c, ds } );
        return ds;
    }
}
