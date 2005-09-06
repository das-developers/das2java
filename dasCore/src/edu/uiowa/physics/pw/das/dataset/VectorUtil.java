package edu.uiowa.physics.pw.das.dataset;

import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.stream.*;
import edu.uiowa.physics.pw.das.util.*;
import java.io.*;
import java.nio.channels.*;
import java.text.*;
import java.util.*;

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
    
    private static void dumpToDas2Stream( VectorDataSet vds, WritableByteChannel out, boolean asciiTransferTypes ) {
        if (vds.getXLength() == 0) {
            try {
                out.close();
            }
            catch (IOException ioe) {
                //Do nothing.
            }
            return;
        }
        try {
            StreamProducer producer = new StreamProducer(out);
            StreamDescriptor sd = new StreamDescriptor();
            sd.setProperty("start", vds.getXTagDatum(0).toString());
            sd.setProperty("end", vds.getXTagDatum(vds.getXLength()-1));
            
            DataTransferType xTransferType;
            DataTransferType yTransferType;
            
            if ( asciiTransferTypes ) {
                if ( vds.getXUnits().isConvertableTo(Units.us2000) ) {
                    xTransferType= DataTransferType.getByName("time24");
                } else {
                    xTransferType= DataTransferType.getByName("ascii24");
                }
                yTransferType= DataTransferType.getByName("ascii10");
            } else {
                xTransferType= DataTransferType.getByName("sun_real8");
                yTransferType= DataTransferType.getByName("sun_real4");
            }
            
            producer.streamDescriptor(sd);
            
            StreamXDescriptor xDescriptor = new StreamXDescriptor();
            xDescriptor.setDataTransferType(xTransferType);
            xDescriptor.setUnits(vds.getXUnits());
            
            PacketDescriptor pd = new PacketDescriptor();
            pd.setXDescriptor(xDescriptor);
            
            String[] planeIds= vds.getPlaneIds();
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
        }
        catch (StreamException se) {
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
}