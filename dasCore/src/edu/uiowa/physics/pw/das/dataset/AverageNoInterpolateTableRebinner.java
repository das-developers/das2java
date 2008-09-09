/*
 * AvergeNoInterpolateTableRebinner.java
 *
 * Created on September 19, 2005, 12:49 PM
 *
 *
 */

package edu.uiowa.physics.pw.das.dataset;

import org.das2.DasException;
import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.datum.DatumRange;
import edu.uiowa.physics.pw.das.datum.DatumVector;
import edu.uiowa.physics.pw.das.datum.Units;
import edu.uiowa.physics.pw.das.datum.UnitsUtil;
import edu.uiowa.physics.pw.das.system.DasLogger;
import org.das2.util.DasMath;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This rebinner will bin average elements that fall on the same bin, and will enlarge cells that
 * cover multiple bins.  This is done efficiently, and also does not introduce half-pixel aliasing because
 * input cells covering multiple output cells are averaged weighting by overlap.
 *
 * @author Jeremy
 */
public class AverageNoInterpolateTableRebinner implements DataSetRebinner {
    Logger logger;
    boolean nearestNeighbor= false;
    
    static class BinDescriptor {
        int length;
        int[] inputBins; // which bin to get
        int[] outputBins; // where to put it
        double[] weights; // what weight to apply
        public String toString() {
            StringBuffer result= new StringBuffer();
            int ll= length < 30 ? length : 30;
            for ( int i=0; i<ll; i++ ) {
                result.append( ""+inputBins[i]+" * "+weights[i]+" -> "+outputBins[i]+"\n" );
            }
            if ( length==0 ) {
                result.append( "(no rebinning)\n" );
            } else if ( length>30 ) {
                result.append( "("+(length-30)+" more)" );
            }
            return result.toString();
        }
    }
    
    private static DatumRange[] getXTagRanges( DataSet ds, int i0, int i1 ) {
        Datum tagWidth= DataSetUtil.guessXTagWidth(ds).divide(2);
        DatumRange[] result= new DatumRange[ i1-i0 ];
        for ( int i=0; i<i1-i0; i++ ) {
            Datum d= ds.getXTagDatum(i+i0);
            result[i]= new DatumRange( d.subtract(tagWidth), d.add( tagWidth ) );
        }
        return result;
    }
    
    private static DatumRange[] getLogYTagRanges( TableDataSet ds, int itable ) {
        Datum tagWidth= TableUtil.guessYTagWidth(ds,itable);
        double ratio= DasMath.exp10( tagWidth.doubleValue( Units.log10Ratio ) / 2. );
        Units units= ds.getYUnits();
        DatumRange[] result= new DatumRange[ ds.getYLength(itable) ];
        for ( int i=0; i<result.length; i++ ) {
            Datum d= ds.getYTagDatum(itable,i);
            double dd= d.doubleValue(d.getUnits());
            result[i]= new DatumRange( dd/ratio, dd*ratio, units );
        }
        return result;
    }
    
    private static DatumRange[] getYTagRanges( TableDataSet ds, int itable ) {
        Datum tagWidth= TableUtil.guessYTagWidth(ds, itable).divide(2);
        boolean isLog= UnitsUtil.isRatiometric(tagWidth.getUnits());
        if ( isLog ) return getLogYTagRanges( ds, itable );
        DatumRange[] result= new DatumRange[ ds.getYLength(itable) ];
        for ( int i=0; i<result.length; i++ ) {
            Datum d= ds.getYTagDatum(itable,i);
            result[i]= new DatumRange( d.subtract(tagWidth), d.add( tagWidth ) );
        }
        return result;
    }
    
    private static DatumRange[] getBinRanges( RebinDescriptor ddx ) {
        DatumRange[] result= new DatumRange[ ddx.numberOfBins() ];
        for ( int i=0; i<ddx.numberOfBins(); i++ ) {
            result[i]= new DatumRange( ddx.binStart(i), ddx.binStop(i) );
        }
        return result;
    }
    
    private static BinDescriptor getIdentityBinDescriptor( int size ) {
        int n= size;
        int[] inputBin= new int[ n ];
        int[] outputBin= new int[ n ];
        double[] weights= new double[ n ];
        for ( int i=0; i<n; i++ ) {
            inputBin[i]= i;
            outputBin[i]= i;
            weights[i]= 1.0;
        }
        BinDescriptor result= new BinDescriptor();
        result.inputBins= inputBin;
        result.outputBins= outputBin;
        result.length= n;
        result.weights= weights;
        return result;
    }
    
    private static BinDescriptor calcBinDescriptor( DatumRange[] inRanges, DatumRange[] outRanges ) {
        int guessCap= inRanges.length + outRanges.length;
        List inBinList= new ArrayList(guessCap);
        List outBinList= new ArrayList(guessCap);
        List weightList= new ArrayList(guessCap);
        
        int inIdx= 0;
        int outIdx= 0;
        DatumRange inRange= inRanges[inIdx];
        DatumRange outRange= outRanges[outIdx];
        
        boolean done= false;
        while ( !done ) {
            if ( inRanges[inIdx].intersects(outRanges[outIdx]) ) {
                inBinList.add( new Integer( inIdx ) );
                outBinList.add( new Integer( outIdx ) );
                DatumRange intersection= inRanges[inIdx].intersection(outRanges[outIdx]);
                weightList.add( intersection.width().divide( outRanges[outIdx].width()) );
            }
            if ( inRanges[inIdx].max().lt( outRanges[outIdx].max() ) ) {
                if ( inIdx < inRanges.length-1 ) {
                    inIdx++;
                } else {
                    done=true;
                }
            } else {
                if ( outIdx < outRanges.length-1 ) {
                    outIdx++;
                } else {
                    done= true;
                }
            }
        }
        
        int n= inBinList.size();
        int[] inputBin= new int[ n ];
        int[] outputBin= new int[ n ];
        double[] weights= new double[ n ];
        
        for ( int i=0; i<n; i++ ) {
            inputBin[i]= ((Integer)inBinList.get(i)).intValue();
            outputBin[i]= ((Integer)outBinList.get(i)).intValue();
            weights[i]= ((Datum)weightList.get(i)).doubleValue(Units.dimensionless);
        }
        BinDescriptor result= new BinDescriptor();
        result.inputBins= inputBin;
        result.outputBins= outputBin;
        result.weights= weights;
        result.length= weights.length;
        
        return result;
    }
    
    public DataSet rebin(DataSet ds, RebinDescriptor ddx, RebinDescriptor ddy) throws IllegalArgumentException, DasException {
        logger= DasLogger.getLogger( DasLogger.DATA_OPERATIONS_LOG );
        logger.finest("enter AverageNoInterpolateTableRebinner.rebin");
        
        logger.finest("get RebinDescriptor ranges");
        DatumRange[] xoutRanges= getBinRanges(ddx);
        
        TableDataSet tds= (TableDataSet)ds;
        TableDataSet wds = (TableDataSet)ds.getPlanarView(DataSet.PROPERTY_PLANE_WEIGHTS);
        
        Units units= tds.getZUnits();
        
        int nx= ddx.numberOfBins();
        int ny= (ddy == null ? tds.getYLength(0) : ddy.numberOfBins());
        
        double[][] sum= new double[nx][ny];
        double[][] weights= new double[nx][ny];
        
        HashMap ybinDescriptors= new HashMap();
        
        DatumRange[] youtRanges=null;
        
        if ( ddy!=null ) {
            logger.finest("get Y RebinDescriptor ranges");
            youtRanges= getBinRanges( ddy );            
        } 
        
        for ( int itable=0; itable<tds.tableCount(); itable++ ) {
            
            logger.finest("get xtag ranges");
            DatumRange[] inRanges= getXTagRanges( tds, tds.tableStart(itable), tds.tableEnd(itable) );
            
            if ( itable ==153 ) {
                System.err.println("itable="+itable);
            }
            logger.finest("calc X bin descriptor");
            BinDescriptor xbd= calcBinDescriptor( inRanges, xoutRanges );
            
            logger.finest("get YTag Ranges");
            BinDescriptor ybd;
            if ( ddy!=null ) {
                DatumVector dv= tds.getYTags(itable);
                if ( ybinDescriptors.containsKey( dv ) ) {
                    ybd= ( BinDescriptor ) ybinDescriptors.get(dv);
                } else {
                    inRanges= getYTagRanges( tds, itable );
                                
                    logger.finest("calc Y bin descriptor");
                    ybd= calcBinDescriptor( inRanges, youtRanges );
                }
                
            } else {
                if ( itable>1 ) throw new IllegalArgumentException( "null yRebinDescriptor not allowed for non-simple table datasets." );
                ybd= getIdentityBinDescriptor( tds.getYLength(itable) );
            }
            logger.finest("apply rebinning");
            
            logger.finest("ybd.length="+ybd.length);
            
            int x0= tds.tableStart(itable);
            if ( nearestNeighbor ) {
                for ( int i=0; i<xbd.length; i++ ) {
                    for ( int j=0; j<ybd.length; j++ ) {
                        double z= tds.getDouble( xbd.inputBins[i]+x0,ybd.inputBins[j],units );
                        double w= xbd.weights[i] * ybd.weights[j];
                        double w2 = wds == null
                                ? (units.isFill(z) ? 0. : 1.)
                                :  wds.getDouble( xbd.inputBins[i]+x0,ybd.inputBins[j],Units.dimensionless );
                        if ( w*w2 > weights[xbd.outputBins[i]][ybd.outputBins[j]] ) {
                            sum[xbd.outputBins[i]][ybd.outputBins[j]]= z;
                            weights[xbd.outputBins[i]][ybd.outputBins[j]]= w * w2;
                        }
                    }
                }
            } else {
                for ( int i=0; i<xbd.length; i++ ) {
                    for ( int j=0; j<ybd.length; j++ ) {
                        double z= tds.getDouble( xbd.inputBins[i]+x0,ybd.inputBins[j],units );
                        double w= xbd.weights[i] * ybd.weights[j];
                        double w2 = wds == null
                                ? (units.isFill(z) ? 0. : 1.)
                                :  wds.getDouble( xbd.inputBins[i]+x0,ybd.inputBins[j],Units.dimensionless );
                        try{
                            sum[xbd.outputBins[i]][ybd.outputBins[j]]+= w * w2 * z;
                            weights[xbd.outputBins[i]][ybd.outputBins[j]]+= w * w2;
                        } catch ( ArrayIndexOutOfBoundsException e ) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
        
        double fill= tds.getZUnits().getFillDouble();
        if ( !nearestNeighbor ) {
            logger.finest("normalize");
            for ( int i=0; i<nx; i++ ) {
                for ( int j=0; j<ny; j++ ) {
                    if ( weights[i][j]>0. ) {
                        sum[i][j]/= weights[i][j];
                    } else {
                        sum[i][j]= fill;
                    }
                }
            }
        }
        
        logger.finest("calculate dataset");
        double[][][] zValues = {sum,weights};
        
        int[] tableOffsets = {0};
        Units[] zUnits = {tds.getZUnits(), Units.dimensionless};
        String[] planeIDs = {"", DataSet.PROPERTY_PLANE_WEIGHTS };
        
        Map properties= new HashMap(ds.getProperties());
        
        if ( ddx!=null ) properties.put( DataSet.PROPERTY_X_TAG_WIDTH, ddx.binWidthDatum() );
        if ( ddy!=null ) properties.put( DataSet.PROPERTY_Y_TAG_WIDTH, ddy.binWidthDatum() );
        
        double[] xTags;
        if (ddx != null) {
            xTags = ddx.binCenters();
        } else {
            xTags = new double[nx];
            for (int i = 0; i < nx; i++) {
                xTags[i] = tds.getXTagDouble(i, tds.getXUnits());
            }
        }
        double[][] yTags;
        if (ddy != null) {
            yTags = new double[][]{ddy.binCenters()};
        } else {
            yTags = new double[1][ny];
            for (int j = 0; j < ny; j++) {
                yTags[0][j] = tds.getYTagDouble(0, j, tds.getYUnits());
            }
        }
        
        TableDataSet result= new DefaultTableDataSet( xTags, ddx.getUnits(), yTags, ddy.getUnits(), zValues, zUnits, planeIDs, tableOffsets, properties );
        
        logger.finest("done, exiting AverageNoInterpolateTableRebinner.rebin");
        return result;
    }
    
    public boolean isNearestNeighbor( ) {
        return this.nearestNeighbor;
    }
    
    public void setNearestNeighbor( boolean v ) {
        this.nearestNeighbor= v;
    }
    
    
}
