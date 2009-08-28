package org.das2.dataset;

import org.das2.datum.LocationUnits;
import org.das2.datum.Units;
import org.das2.datum.DatumVector;
import org.das2.datum.Datum;
import org.das2.datum.UnitsUtil;
import org.das2.datum.TimeUtil;
import org.das2.stream.StreamProducer;
import org.das2.stream.DataTransferType;
import org.das2.stream.StreamMultiYDescriptor;
import org.das2.stream.StreamDescriptor;
import org.das2.stream.StreamXDescriptor;
import org.das2.stream.StreamException;
import org.das2.stream.PacketDescriptor;
import org.das2.util.FixedWidthFormatter;
import java.io.*;
import java.nio.channels.*;
import java.text.*;
import java.util.*;
import org.das2.datum.UnitsConverter;

/**
 *
 * @author  Owner
 */
public class VectorUtil {
    
    
    public static double[] getXTagArrayDouble( DataSet vds, Units units ) {
        
        int ixmax= vds.getXLength();
        double[] xx= new double[ixmax];
        for ( int i=0; i<ixmax; i++ ) {
            xx[i]= vds.getXTagDouble(i,units);
        }
        return xx;
    }
    
    private static int closest( double[] xx, double x ) {
        int result=0;
        while ( result<(xx.length-1) && xx[result]<x ) result++;
        while ( result>0 && xx[result]>x ) result--;
        if ( result<xx.length-2 ) {
            result= ( ( x-xx[result] ) / ( xx[result+1] - xx[result] ) < 0.5 ? result : result+1 );
        }
        return result;
    }
    
    public static int closestXTag( DataSet ds, Datum datum ) {
        return closestXTag( ds, datum.doubleValue(datum.getUnits()), datum.getUnits() );
    }
    
    public static int closestXTag( DataSet ds, double x, Units units ) {
        double [] xx= getXTagArrayDouble( ds, units );
        return closest( xx, x );
    }
    
    public static Datum median( VectorDataSet ds ) {
        double[] data= new double[ds.getXLength()];
        int idata=0;
        Units units= ds.getYUnits();
        for ( int i=0; i<ds.getXLength(); i++ ) {
            double zz= ds.getDouble(i,units);
            if ( !units.isFill(zz) ) {
                data[idata++]= zz;
            }
        }
        if ( idata==0 ) return Datum.create( units.getFillDouble(), units );
        Arrays.sort(data,0, idata);
        
        int n= idata/2;
        
        return Datum.create( data[n], units );
    }
    
    public static void dumpToAsciiStream( VectorDataSet vds, Datum xmin, Datum xmax, OutputStream out ) {
        PrintStream pout= new PrintStream(out);
        
        Datum base=null;
        Units offsetUnits= null;
        
        pout.print("[00]");
        pout.println("<stream start=\""+vds.getXTagDatum(0)+"\" end=\""+vds.getXTagDatum(vds.getXLength()-1)+"\" >");
        pout.println("<comment>Stream creation date: "+TimeUtil.now().toString()+"</comment>");
        pout.print("</stream>");
        
        if ( vds.getXUnits() instanceof LocationUnits ) {
            base= xmin;
            offsetUnits= ((LocationUnits)base.getUnits()).getOffsetUnits();
            if ( offsetUnits==Units.microseconds ) {
                offsetUnits= Units.seconds;
            }
        }
        
        
        pout.print("[01]<packet>\n");
        pout.print("<x type=\"asciiTab10\" ");
        if ( base!=null ) {
            pout.print("base=\""+base+"\" ");
            pout.print(" xUnits=\""+offsetUnits+"\" ");
        } else {
            pout.print(" xUnits=\""+vds.getXUnits()+"\"");
        }
        pout.println(" />");
        
        List planeIDs;
        if ( vds.getProperty("plane-list")!=null ) {
            planeIDs= (List)vds.getProperty("plane-list");
        } else {
            planeIDs= new ArrayList();
            planeIDs.add("");
        }
        
        for ( int i=0; i<planeIDs.size(); i++ ) {
            String plid= (String)planeIDs.get(i);
            pout.println("<y type=\"asciiTab10\" name=\""+plid+"\" yUnits=\""+vds.getPlanarView(plid).getYUnits()+"\" />");
        }
        pout.print("</packet>");
        
        NumberFormat xnf= new DecimalFormat("00000.000");
        NumberFormat ynf= new DecimalFormat("0.00E00");
        
        double dx= xmax.subtract(xmin).doubleValue(offsetUnits);
        for (int i=0; i<vds.getXLength(); i++) {
            double x;
            if ( base!=null ) {
                x= vds.getXTagDatum(i).subtract(base).doubleValue(offsetUnits);
            } else {
                x= vds.getXTagDouble(i,vds.getXUnits());
            }
            if ( x>=0 && x<dx ) {
                pout.print(":01:");
                pout.print(xnf.format(x)+" ");
                for ( int iplane=0; iplane<planeIDs.size(); iplane++ ) {
                    VectorDataSet vds1= (VectorDataSet)vds.getPlanarView((String)planeIDs.get(iplane));
                    pout.print(FixedWidthFormatter.format(ynf.format(vds1.getDouble(i,vds1.getYUnits())),9));
                    if ( iplane==planeIDs.size()-1) {
                        pout.print("\n");
                    } else {
                        pout.print(" ");
                    }
                }
            }
        }
        
        pout.close();
    }
    
    public static void dumpToAsciiStream( VectorDataSet vds, OutputStream out ) {
        dumpToAsciiStream(vds, Channels.newChannel(out));
    }
    
    public static void dumpToAsciiStream(VectorDataSet vds, WritableByteChannel out) {
        dumpToDas2Stream(vds, out, true);
    }
    
    public static void dumpToStream( VectorDataSet vds, OutputStream out ) {
        dumpToAsciiStream(vds, out);
    }
    
    public static void dumpToBinaryStream( VectorDataSet vds, OutputStream out ) {
        dumpToDas2Stream( vds, Channels.newChannel(out), false );
    }
    
    private static void dumpToDas2Stream( VectorDataSet vds, WritableByteChannel out, boolean asciiTransferTypes ) {
        if (vds.getXLength() == 0) {
            try {
                out.close();
            } catch (IOException ioe) {
                //Do nothing.
            }
            return;
        }
        try {
            StreamProducer producer = new StreamProducer(out);
            StreamDescriptor sd = new StreamDescriptor();
            
            Map properties= vds.getProperties();
            if ( properties!=null) {
                for ( Iterator i= properties.keySet().iterator(); i.hasNext(); ) {
                    String key= (String)i.next();
                    sd.setProperty(key, properties.get(key));
                }
            }
            
            DataTransferType xTransferType;
            DataTransferType yTransferType;
            
            if ( asciiTransferTypes ) {
                if ( UnitsUtil.isTimeLocation(vds.getXUnits()) ) {
                    xTransferType= DataTransferType.getByName("time24");
                } else {
                    xTransferType= DataTransferType.getByName("ascii14");
                }
                yTransferType= DataTransferType.getByName("ascii14");
            } else {
                xTransferType= DataTransferType.getByName("sun_real8");
                yTransferType= DataTransferType.getByName("sun_real4");
            }
            
            producer.streamDescriptor(sd);
            
            StreamXDescriptor xDescriptor = new StreamXDescriptor();
            xDescriptor.setUnits(vds.getXUnits());
            xDescriptor.setDataTransferType(xTransferType);
            
            PacketDescriptor pd = new PacketDescriptor();
            pd.setXDescriptor(xDescriptor);
            
            String[] planeIds= DataSetUtil.getAllPlaneIds(vds);
            
            DatumVector[] yValues = new DatumVector[planeIds.length];
            
            for ( int i=0; i<planeIds.length; i++ ) {
                StreamMultiYDescriptor yDescriptor = new StreamMultiYDescriptor();
                yDescriptor.setName(planeIds[i]);
                yDescriptor.setDataTransferType(yTransferType);
                yDescriptor.setUnits(((VectorDataSet)vds.getPlanarView(planeIds[i])).getYUnits());
                pd.addYDescriptor(yDescriptor);
            }
            
            producer.packetDescriptor(pd);
            for (int i = 0; i < vds.getXLength(); i++) {
                Datum xTag = vds.getXTagDatum(i);
                for ( int j=0; j<planeIds.length; j++ ) {
                    yValues[j] = toDatumVector(((VectorDataSet)vds.getPlanarView(planeIds[j])).getDatum(i));
                }
                producer.packet(pd, xTag, yValues);
            }
            producer.streamClosed(sd);
        } catch (StreamException se) {
            throw new RuntimeException(se);
        }
    }
    
    private static DatumVector toDatumVector(Datum d) {
        double[] array = { d.doubleValue(d.getUnits()) };
        return DatumVector.newDatumVector(array, d.getUnits());
    }
    
    public static String toString( VectorDataSet ds ) {
        return "[VectorDataSet "+ds.getXLength()+" xTags ]";
    }
    
    /**
     * Return the finite difference derivative of the dataset, between elements that
     * are n steps apart.
     * Because we don't have a general-purpose way to divide units, the units returned
     * are dimensionless.
     */
    public static VectorDataSet finiteDerivative( VectorDataSet ds, int n ) {
        VectorDataSetBuilder builder= new VectorDataSetBuilder( ds.getXUnits(), Units.dimensionless );
        Units xunits= ds.getXUnits();
        Units yunits= ds.getYUnits();
        for ( int i=n; i<ds.getXLength(); i++ ) {
            double dx= ds.getXTagDouble( i, xunits ) - ds.getXTagDouble( i-n, xunits );
            double dy= ds.getDouble( i, yunits ) - ds.getDouble( i-n, yunits );
            builder.insertY( ds.getXTagDouble(i-n, xunits) + dx / 2 , dy / dx );
        }
        
        for ( Iterator i=ds.getProperties().keySet().iterator(); i.hasNext(); ) {
            String key= (String)i.next();
            builder.setProperty( key, ds.getProperty(key) );
        }
        
        return builder.toVectorDataSet();
    }

    /**
     * return a converter for differences.  If dst units are specified,
     * then explicitly this is the target.
     * @param src
     * @param dst
     * @return
     */
    private static UnitsConverter getDifferencesConverter( Units unitsOut, Units unitsIn, Units dstUnits ) {
        UnitsConverter xuc;
        if ( dstUnits!=null ) {
            xuc= unitsOut.getConverter( dstUnits );
        } else {
            xuc= unitsOut.getConverter( unitsIn.getOffsetUnits() );
        }
        return xuc;
    }

    /**
     * produce a simpler version of the dataset by averaging adjecent data.
     * code taken from org.das2.graph.GraphUtil.reducePath.  Adjecent points are
     * averaged together until a point is found that is not in the bin, and then
     * a new bin is started.  The bin's lower bounds are integer multiples
     * of xLimit and yLimit.
     *
     * If yLimit is null, then averaging is done for all points in the x bin,
     * regardless of how close they are in Y.  This is similarly true when
     * xLimit is null.
     *
     * xLimit and yLimit are rank 0 datasets, so that they can indicate that binning
     * should be done in log space rather than linear.  In this case, a SCALE_TYPE
     * for the dataset should be "log" and its unit should be convertable to
     * Units.logERatio (for example, Units.log10Ratio or Units.percentIncrease).
     * Note when either is log, then averaging is done in the log space.
     *
     * @param ds
     * @param start first index.
     * @param end last (non-inclusive) index.
     * @param xLimit the size of the bins or null to indicate no limit.
     * @param yLimit the size of the bins or null to indicate no limit.
     * @return
     */
    public static VectorDataSet reduce2D( VectorDataSet ds, int start, int finish, Datum xLimit, Datum yLimit ) {

        double x0 = Float.MAX_VALUE;
        double y0 = Float.MAX_VALUE;
        double sx0 = 0;
        double sy0 = 0;
        double nn0 = 0;
        double ax0 = Float.NaN;
        double ay0 = Float.NaN;  // last averaged location

        final Units xunits= ds.getXUnits();
        final Units yunits= ds.getYUnits();

        VectorDataSetBuilder builder= new VectorDataSetBuilder( ds.getXUnits(), ds.getYUnits() );
        builder.addPlane( DataSet.PROPERTY_PLANE_WEIGHTS, Units.dimensionless );

        Units dxunits= xLimit!=null ? xLimit.getUnits() : null;

        boolean xlog= xLimit!=null && UnitsUtil.isRatiometric( xLimit.getUnits() );
        boolean ylog= yLimit!=null && UnitsUtil.isRatiometric( yLimit.getUnits() );

        UnitsConverter uc;
        double dxLimit, dyLimit;
        if ( xLimit!=null ) {
            uc= getDifferencesConverter( xLimit.getUnits(), ds.getXUnits().getOffsetUnits(), xlog ? Units.logERatio : null );
            dxLimit = uc.convert( xLimit.doubleValue(xLimit.getUnits()) );
        } else {
            dxLimit= Double.MAX_VALUE;
        }
        if ( yLimit!=null ) {
            uc= getDifferencesConverter( yLimit.getUnits(), ds.getYUnits().getOffsetUnits(), ylog ? Units.logERatio : null );
            dyLimit = uc.convert( yLimit.doubleValue(yLimit.getUnits()) );
        } else {
            dyLimit= Double.MAX_VALUE;
        }

        int points = 0;
        int inCount = 0;

        int i=start;

        while ( i<finish ) {
            inCount++;

            double xx= ds.getXTagDouble(i,xunits);
            double yy= ds.getDouble(i,yunits);
            double ww= yunits.isFill( yy ) ? 0. : 1.;

            if ( ww==0 ) {
                i++;
                continue;
            }

            double p0 = xlog ? Math.log(xx) : xx;
            double p1 = ylog ? Math.log(yy) : yy;

            if ( Double.isNaN(p0) || Double.isNaN(p1) ) continue;
            
            double dx = p0 - x0;
            double dy = p1 - y0;

            if ( Math.abs(dx) < dxLimit && Math.abs(dy) < dyLimit) {
                sx0 += p0;
                sy0 += p1;
                nn0 += ww;
                i++;
                continue;
            }

            if ( nn0>0 ) {
                ax0 = sx0 / nn0;
                ay0 = sy0 / nn0;

                builder.insertY( xlog ? Math.exp(ax0) : ax0, ylog ? Math.exp(ay0) : ay0, DataSet.PROPERTY_PLANE_WEIGHTS, nn0 );
                points++;
            }

            i++;

            x0 = dxLimit * ( 0.5 + (int) Math.floor(p0/dxLimit) );
            y0 = dyLimit * ( 0.5 + (int) Math.floor(p1/dyLimit) );
            sx0 = p0;
            sy0 = p1;
            nn0 = ww;
        }

        if ( nn0>0 ) {
            ax0 = sx0 / nn0;
            ay0 = sy0 / nn0;

            builder.insertY( xlog ? Math.exp(ax0) : ax0, ylog ? Math.exp(ay0) : ay0, DataSet.PROPERTY_PLANE_WEIGHTS, nn0 );
            points++;
        }

        Map props= ds.getProperties();
        builder.addProperties( props );
//        Datum xtw= (Datum) props.get(DataSet.PROPERTY_X_TAG_WIDTH);
//        if ( xtw!=null && dxunits!=null ) {
//            Datum nxtw= dxunits.createDatum(dxLimit);
//            if ( nxtw.gt(xtw) ) builder.setProperty( DataSet.PROPERTY_X_TAG_WIDTH, nxtw );
//        }
        builder.setProperty( DataSet.PROPERTY_X_TAG_WIDTH, null );
        VectorDataSet yds= builder.toVectorDataSet();

        return yds;

    }

}