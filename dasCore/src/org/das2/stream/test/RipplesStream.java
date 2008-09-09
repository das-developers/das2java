/* File: RipplesStream.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on March 29, 2004, 10:13 AM
 *      by Edward E. West <eew@space.physics.uiowa.edu>
 *
 * This file is part of the das2 library.
 *
 * das2 is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.das2.stream.test;

import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.stream.*;
import java.nio.channels.*;
import java.util.*;

/**
 *
 * @author  eew
 */
public class RipplesStream {
    
    private boolean compress;
    
    double x1, y1, p1, x2, y2, p2;
    
    /** Creates a new instance of RipplesStream */
    public RipplesStream( boolean compress ) {
        this( 14, 17, 10, 20, 60, 15, compress );
    }
    
    /** Creates a new instance of RipplesDataSetDescriptor */
    public RipplesStream( double x1, double y1, double p1, double x2, double y2, double p2, boolean compress ) {
        this.x1= x1;
        this.y1= y1;
        this.p1= p1;
        this.x2= x2;
        this.y2= y2;
        this.p2= p2;
        this.compress = compress;
    }
    
    public void produceRipplesStream(WritableByteChannel out) {
        try {
            StreamProducer producer = new StreamProducer(out);
            StreamDescriptor sd = new StreamDescriptor();
            if (compress) { sd.setCompression("deflate"); }
            producer.streamDescriptor(sd);

            int nx=100;
            int ny=100;

            double[] y= new double[ny];
            for (int j=0; j<y.length; j++) {
                y[j]= (double)j;
            }

            StreamXDescriptor xDescriptor = new StreamXDescriptor();
            xDescriptor.setDataTransferType(DataTransferType.getByName("ascii10"));
            xDescriptor.setUnits(Units.dimensionless);
            StreamYScanDescriptor yscan = new StreamYScanDescriptor();
            yscan.setDataTransferType(DataTransferType.getByName("ascii10"));
            yscan.setYCoordinates(y);
            PacketDescriptor pd = new PacketDescriptor();
            pd.setXDescriptor(xDescriptor);
            pd.addYDescriptor(yscan);

            producer.packetDescriptor(pd);

            double[] z = new double[ny];
            DatumVector[] scans = new DatumVector[1];
            for (int i = 0; i < nx; i++) {
                Datum xTag = Datum.create((double)i);
                for (int j = 0; j < y.length; j++) {
                    double rad1= Math.sqrt((i-x1)*(i-x1)+(j-y1)*(j-y1));
                    double exp1= Math.exp(-rad1/p1)*Math.cos(Math.PI*p1*rad1);
                    double rad2= Math.sqrt((i-nx*2/3)*(i-nx*2/3)+(j-ny*2/3)*(j-ny*2/3));
                    double exp2= Math.exp(-rad2/p2)*Math.cos(Math.PI*p2*rad2);
                    z[j] = (exp1+exp2);
                }
                scans[0] = DatumVector.newDatumVector(z, Units.dimensionless);
                producer.packet(pd, xTag, scans);
            }

            producer.streamClosed(sd);
        }
        catch (StreamException se) {
            se.printStackTrace();
            System.exit(-1);
        }
    }
    
    public static void main(String[] args) {
        List argList = Arrays.asList(args);
        boolean c = argList.contains("-c");
        WritableByteChannel out = Channels.newChannel(System.out);
        RipplesStream rs = new RipplesStream(c);
        rs.produceRipplesStream(out);
    }
        
}
